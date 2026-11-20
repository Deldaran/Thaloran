package com.deldaran.politics.histoire;

import com.badlogic.gdx.files.FileHandle;
import com.deldaran.politics.Metiers;
import com.deldaran.politics.Relations;
import com.badlogic.gdx.utils.GdxRuntimeException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class Joueur {
    private static final int FORMAT = 26;
    /** Index du tableau distances : un par atelier (Metiers), puis la place du marché, puis la sortie de la ville. */
    public static final int LIEU_MARCHE = Metiers.NOMBRE, LIEU_DEHORS = Metiers.NOMBRE + 1, LIEUX = Metiers.NOMBRE + 2;
    /** Distance (m) en dessous de laquelle on est « devant » un atelier ou un étal. */
    public static final float PROCHE = 4f;
    private static final int VOISINS = 6;
    private static final int ETAGERE = 7;
    private static final int PAROLES = 2;
    private static final int SOUVENIRS = 60;

    public static final class Scene {
        public final String titre;
        public final String texte;
        public final List<String> choix = new ArrayList<>();
        public final List<Supplier<Scene>> suites = new ArrayList<>();
        public String saisie;
        /** La scène ne s'affiche pas : la fenêtre se ferme et le jeu reprend (travail à la main…). */
        public boolean fermer;
        public Function<String, Scene> valider;
        public Supplier<Scene> annuler;
        /** Lot K3 : une étape qui se choisit sur la carte (royaumes, puis villes) ; pour chaque choix, l'État ou la ville qu'il désigne. */
        public static final int SANS_CARTE = 0, CARTE_ROYAUMES = 1, CARTE_VILLES = 2, HORS_CARTE = -2;
        public int carte = SANS_CARTE;
        public int etatCarte = -1;
        public final List<Integer> lieux = new ArrayList<>();

        public Scene(String titre, String texte) {
            this.titre = titre;
            this.texte = texte;
        }

        void ajouter(String nom, Supplier<Scene> suite) {
            choix.add(nom);
            suites.add(suite);
        }
    }

    private final Population p;
    private final FileHandle fichier;
    private final LinkedHashSet<String> carnet = new LinkedHashSet<>();
    private int peuple = -1;
    private int region = -1;
    private int place = -1;
    private long id = -1L;
    private int quartier = 0;
    private int numeroReserve = 0;
    private Fiche fiche;
    private List<Creation.Ajout> ajouts = new ArrayList<>();
    private Creation creation;
    public volatile int villeDemandee = -1;
    /** Lot G3 : le nombre de loups sortis autour du joueur (mission des loups, la nuit, hors des murs) ; l'écran les fait rôder. */
    public volatile int loupsDehors;
    public volatile boolean nouvelleApparence;
    /** Heures de jeu vécues depuis le début de cette vie (sauvegardées) : l'économie avance d'un jour toutes les 24. */
    public volatile double heuresVecues;
    private double derniereHeure = Double.NaN;
    /** Écrits par l'écran : le village où se trouve le joueur (-1 si aucun) et la distance à chaque lieu (NaN = pas de bâtiment). */
    public volatile int villeVue = -1;
    public volatile float[] distances;
    /** Débogage : commercer avec tout le monde sans se déplacer. */
    public volatile boolean partout;
    /** Vitesse de marche selon la charge (1 = normale). */
    public volatile float allure = 1f;
    /** Biens portés (armes, armures…), lus par l'écran pour habiller le sprite. */
    public volatile int[] portes;
    public volatile int versionTenue;
    private Marche marche;
    /** Heure locale (0 à 24) là où est le joueur, écrite par l'écran à chaque image. */
    public volatile double heureLocale = 12.0;
    /** Heures que les scènes demandent de faire passer (travail, sommeil) : l'écran les ajoute à la météo. */
    private double heuresAPasser;
    /** Écrits par l'écran : pour chaque atelier proche (index Metiers), le Lien du villageois qui y travaille, et les relations à enregistrer. */
    public volatile Relations.Lien[] maitres;
    public volatile Relations relations;
    private Travail travail;
    private Missions missions;
    private Renommee renommee;
    private Destin destin;
    /** Geste que le personnage du joueur doit jouer (payer, recevoir, lire…), et atelier dont le marchand doit réagir : lus par l'écran. */
    public volatile String gesteJoueur;
    public volatile int reactionLieu = -1;
    /** Geste que fera ce marchand (null : montrer sa marchandise ou saluer). */
    public volatile String reactionGeste;
    /** Mission à suivre : ville et lieu (index de distances) où la rendre, et ce qu'elle demande ; lus par l'écran pour le repère. */
    public volatile int cibleVille = -1, cibleLieu = -1;
    public volatile String cibleTexte = "";

    /** On se tient devant le tableau des annonces (posé par l'écran) : Q le lit directement. */
    public volatile boolean devantTableau;

    // ---------------------------------------------------------------- Dormir (en action)

    public static final int EVEILLE = 0, BELLE_ETOILE = 1, CHAMBRE = 2;
    /** Prix d'une chambre à l'auberge pour la nuit. */
    public static final int PRIX_CHAMBRE = 8;
    /** Moral gagné après une nuit dans un lit, perdu après une nuit dehors. */
    public static final float MORAL_CHAMBRE = 3f, MORAL_DEHORS = 1f;
    /** Dehors, on se repose moins bien : cette fatigue par heure dormie est rendue au réveil (le repos normal en ôte Travail.REPOS_HEURE). */
    public static final float FATIGUE_DEHORS = 1.5f;
    /** EVEILLE, BELLE_ETOILE ou CHAMBRE ; lu par l'écran qui accélère le temps, couche ou cache le personnage. */
    public volatile int sommeil;
    /** Heures vécues au coucher et à l'heure prévue du réveil. */
    public volatile double couche, reveil;

    /** Touche Z : où et combien de temps dormir. À l'auberge, une chambre ; ailleurs, la belle étoile. */
    public Scene dormir() {
        if (fiche == null) return accueil();
        travail.reposer();
        double h = heureIci();
        float matin = (float) (((Quotidien.ouverture(travail.employe() ? travail.lieu : -1) - h) % 24.0 + 24.0) % 24.0);
        if (matin < 1f) matin += 24f;
        final float m = matin;
        StringBuilder t = new StringBuilder("Il est ").append((int) h).append(" h. Fatigue : ").append(Math.round(travail.fatigue)).append(" %.");
        Scene s = new Scene("Dormir", "");
        dortChezSoi = false;
        if (corps.chezSoi()) s.ajouter("Dormir chez toi jusqu'au matin (" + Math.round(m) + " h)", () -> {
            dortChezSoi = true;
            return coucher(CHAMBRE, m);
        });
        if (travail.rang == Travail.APPRENTI && villeIci() == travail.ville && !corps.chezSoi())
            s.ajouter("Dormir dans le grenier de ton maître (l'apprenti est logé) jusqu'au matin (" + Math.round(m) + " h)", () -> coucher(CHAMBRE, m));
        EconomieVille.Agent aub = agentIci(Metiers.AUBERGE);
        if (aub != null) {
            if (fiche.argent >= PRIX_CHAMBRE) s.ajouter("Prendre une chambre jusqu'au matin (" + Fiche.argentTexte(PRIX_CHAMBRE) + ", " + Math.round(m) + " h)", () -> {
                fiche.argent -= PRIX_CHAMBRE;
                aub.caisse += PRIX_CHAMBRE;
                return coucher(CHAMBRE, m);
            });
            else t.append(" Une chambre coûte ").append(Fiche.argentTexte(PRIX_CHAMBRE)).append(" : tu n'as pas de quoi.");
        } else t.append(" Pour un vrai lit, va à l'auberge.");
        s.ajouter("Dormir à la belle étoile jusqu'au matin (" + Math.round(m) + " h)", () -> coucher(BELLE_ETOILE, m));
        s.ajouter("Faire une sieste (2 h)", () -> coucher(BELLE_ETOILE, 2f));
        s.ajouter("« Revenir", this::accueilScene);
        Scene vraie = new Scene("Dormir", t.toString());
        vraie.choix.addAll(s.choix);
        vraie.suites.addAll(s.suites);
        return vraie;
    }

    private Scene coucher(int ou, float heures) {
        couche = maintenant();
        reveil = couche + heures;
        sommeil = ou;
        annoncer(ou == CHAMBRE ? "Tu montes te coucher. (Bouge pour te lever.)" : "Tu t'allonges. (Bouge pour te lever.)");
        Scene s = new Scene("", "");
        s.fermer = true;
        return s;
    }

    /** L'heure prévue du réveil est passée (lu par l'écran). */
    public boolean reveilPasse() {
        return maintenant() >= reveil;
    }

    /** Réveil : à l'heure (reposé) ou plus tôt parce qu'on a bougé. */
    public void reveiller() {
        int s = sommeil;
        if (s == EVEILLE || fiche == null) return;
        boolean tot = !reveilPasse();
        travail.reposer();
        float dormi = (float) Math.max(0.0, maintenant() - couche);
        if (s == CHAMBRE) {
            if (!tot) {
                travail.fatigue = 0f;
                destin.moral = Math.min(100f, destin.moral + MORAL_CHAMBRE + (dortChezSoi ? Corps.MORAL_MAISON : 0f));
                annoncer(dortChezSoi ? "Tu te réveilles reposé" + fiche.e() + ", chez toi, dans ton lit." : "Tu te réveilles reposé" + fiche.e() + " dans un vrai lit.");
            } else annoncer("Tu te lèves avant l'heure.");
        } else {
            travail.fatigue = Math.min(Travail.FATIGUE_MAX, travail.fatigue + dormi * FATIGUE_DEHORS);
            if (dormi >= 4f) destin.moral = Math.max(0f, destin.moral - MORAL_DEHORS);
            annoncer(tot ? "Tu te relèves." : "Tu te réveilles, courbatu" + fiche.e() + ", à la belle étoile.");
        }
        sommeil = EVEILLE;
        enregistrer();
    }

    /** Fin du travail à la main (appelée par l'écran). */
    public Scene finirTravailAMain() {
        return travail.finirAMain();
    }

    /** Messages courts pour le bandeau du jeu (envie accomplie, renom gagné…), lus par l'écran. */
    private final java.util.concurrent.ConcurrentLinkedQueue<String> annonces = new java.util.concurrent.ConcurrentLinkedQueue<>();

    public Joueur(Population p, FileHandle fichier) {
        this.p = p;
        this.fichier = fichier;
        this.travail = new Travail(this, p);
        this.missions = new Missions(this, p);
        this.renommee = new Renommee(this, p);
        this.destin = new Destin(this, p);
        this.entreprise = new Entreprise(this, p);
        this.bande = new Bande(this, p);
        this.brigands = new Brigands(this, p);
        this.corps = new Corps(this, p);
        this.commandes = new Commandes(this, p);
        this.affaires = new Affaires(this, p);
        this.social = new Social(this, p);
        this.terre = new Terre(this, p);
        this.famille = new Famille(this, p);
        this.noblesse = new Noblesse(this, p);
        this.armee = new Armee(this, p);
        this.quotidien = new Quotidien(this, p);
        this.chansons = new Chansons(this, p);
        this.gens = new Gens(this, p);
        this.notables = new Notables(this, p);
    }

    // ---- Lot G4 : les livres du sac sont de vrais livres du monde (l'oeuvre de Livres, ou -1 pour un livre sans titre connu)
    final List<Integer> bibliotheque = new ArrayList<>();

    // ---- Lot H1 : la fiche de chaque habitant
    private Gens gens;

    public Gens gens() {
        return gens;
    }

    // ---- Lot H3 : les notables (rivaux) de chaque ville
    private Notables notables;

    public Notables notables() {
        return notables;
    }

    // ---- Lot G2 : les chansons
    private Chansons chansons;

    public Chansons chansons() {
        return chansons;
    }

    // ---- Lot G1 : le quotidien du travail
    private Quotidien quotidien;

    public Quotidien quotidien() {
        return quotidien;
    }

    // ---- Phase 10 : l'armée
    private Armee armee;

    public Armee armee() {
        return armee;
    }

    // ---- Phase 10 : la noblesse
    private Noblesse noblesse;

    public Noblesse noblesse() {
        return noblesse;
    }

    // ---- Phase 11 : la famille et la lignée
    private Famille famille;

    public Famille lignee() {
        return famille;
    }

    /** Un nouvel identifiant de personne, pris dans la réserve du joueur (enfants). */
    long nouvelId() {
        return foule().idReserve(numeroReserve++);
    }

    /** Fait entrer une personne nouvelle dans le monde (un enfant) ; elle sera remise à chaque chargement. */
    /** Lot I2 : l'ajout (l'habitant que le joueur a fait naître) de cet id, ou null. */
    Creation.Ajout ajout(long id) {
        for (Creation.Ajout a : ajouts) if (a.id == id) return a;
        return null;
    }

    void inserer(Creation.Ajout a) {
        ajouts.add(a);
        a.inserer(foule());
    }

    /** L'héritier reprend la partie : même monde, mêmes biens ; le reste de la vie recommence. jours : la régence (saut dans le temps). */
    void succeder(Fiche n, int jours, String phrase) {
        boolean mort = famille.mort != null;
        if (mort && enVie()) foule().executer(place, Terre.annee(p, jourMaintenant()));
        synchronized (this) {
            heuresVecues += jours * 24.0;
        }
        id = n.id;
        place = foule().placeDe(id);
        fiche = n;
        noter(phrase);
        travail = new Travail(this, p);
        missions = new Missions(this, p);
        destin = new Destin(this, p);
        Social ancien = social;
        social = new Social(this, p);
        social.dettes.addAll(ancien.dettes);
        renommee.heriter();
        famille = new Famille(this, p);
        corps.sauter();
        noblesse.tache = null;
        armee.heriter(n.id);
        chansons.heriter();
        if (jours > 0) {
            armee.sauter();
            chansons.sauter();
            noblesse.sauter();
            terre.sauter();
            bande.sauter();
        }
        enregistrer();
        nouvelleApparence = true;
        rafraichir();
    }

    // ---- Phase 11 : la terre
    private Terre terre;

    public Terre terre() {
        return terre;
    }

    // ---- Lot F : la vie sociale
    private Social social;

    public Social social() {
        return social;
    }

    java.util.Collection<String> carnet() {
        return carnet;
    }

    // ---- Lot D : économie
    private Commandes commandes;
    private Affaires affaires;

    public Commandes commandes() {
        return commandes;
    }

    public Affaires affaires() {
        return affaires;
    }

    /** Pour l'écran et Affaires : la ruine la plus proche dans le village (index du bâtiment) et sa distance ;
     *  les ateliers bâtis sur des ruines (ville, bâtiment, métier, par trois) que l'écran dessine à la place des ruines. */
    public volatile int ruineProche = -1;
    public volatile float ruineDistance = Float.MAX_VALUE;
    public volatile int[] batisVue = new int[0];
    /** Lot G5 : le terrain libre devant le joueur (ville, a, b, angle, métiers qui y tiennent en bits), posé par l'écran ; null sinon. */
    public volatile double[] terrainLibre;
    /** Lot G5 : les bâtiments du joueur sur des terrains libres, pour l'écran (ville, a, b, angle, métier, chantier par six). */
    public volatile float[] constructionsVue = new float[0];
    public volatile int constructionsVersion;

    private Corps corps;

    public Corps corps() {
        return corps;
    }

    /** Pour l'écran : la maison la plus proche du joueur (index du bâtiment) et sa distance ; le joueur est devant sa propre porte ;
     *  sa maison (ville, bâtiment) pour faire déménager ses habitants ; il dort chez lui. */
    public volatile int maisonProche = -1;
    public volatile float maisonDistance = Float.MAX_VALUE;
    public volatile boolean devantMaison, dortChezSoi;
    public volatile int[] maisonVue;

    public Scene objetScene(int bien, String message) {
        return objet(bien, message);
    }

    private Brigands brigands;

    public Brigands brigands() {
        return brigands;
    }

    /** Phase 9, pour l'écran : la bataille à rejouer (posée par Brigands, remise à null après), le sort choisi pour les prisonniers,
     *  les repaires à montrer, la santé du joueur après une bataille (-1 : rien à faire). */
    public volatile Combat.Resultat bataille;
    public volatile int sortPrisonniers = -1;
    public volatile Brigands.Repaire[] repairesVue = new Brigands.Repaire[0];
    /** Les brigands tués dont la famille vit en ville (l'écran choisit l'habitant qui s'en souvient). */
    public volatile Brigands.Deuil[] deuilsVue = new Brigands.Deuil[0];
    public volatile int cibleCamp = -1;
    public volatile float santeJoueur = -1f;
    /** Lot E, pour l'écran : le villageois à qui la mission demande d'aller parler (cibleLieu = -3), 0 si n'importe qui.
     *  Et, publiés par l'écran : les adultes du village (graine, nom, femme), les ruines (index des bâtiments). */
    public volatile long cibleGraine;
    public volatile long[] gensIci;
    public volatile String[] gensNoms;
    public volatile boolean[] gensFemmes;
    public volatile int gensVille = -1;
    public volatile int[] ruinesIci;
    public volatile int ruinesVille = -1;

    /** Hors des murs : au-delà des maisons, dans les champs (la distance « dehors » est nulle). */
    boolean horsLesMurs() {
        float[] d = distances;
        return d != null && villeVue >= 0 && d[LIEU_DEHORS] <= 0.5f;
    }

    private Entreprise entreprise;
    private Bande bande;

    public Bande bande() {
        return bande;
    }

    /** Les membres de la bande, pour l'écran (copie : l'écran les fait marcher derrière le joueur). */
    public volatile Bande.Compagnon[] bandeVue = new Bande.Compagnon[0];

    void publierBande() {
        List<Bande.Compagnon> l = new ArrayList<>(bande.membres);
        // Lot E : le marchand qu'on escorte suit le joueur comme la bande.
        if (missions != null) l.addAll(missions.quetes.escortes());
        bandeVue = l.toArray(new Bande.Compagnon[0]);
    }

    /** Un villageois vient de rejoindre la bande : l'écran le retire de son village et le fait suivre. */
    void recruter(long graine) {
        publierBande();
    }

    /** Un membre quitte la bande (renvoyé, déserteur) : l'écran le laisse repartir. */
    void renvoyerCompagnon(long graine) {
        publierBande();
    }

    /** Appelé par l'écran de temps en temps : les jours passent pour la bande (pain, solde, moral). */
    public void avancerBande() {
        if (fiche == null) return;
        bande.avancer();
        brigands.avancer();
        corps.avancer();
        commandes.avancer();
        affaires.avancer();
        missions.quetes.avancer();
        social.avancer();
        terre.avancer();
        famille.avancer();
        noblesse.avancer();
        armee.avancer();
        chansons.avancer();
        notables.avancer();
        gens.avancer();
    }

    /** Une scène qui ferme la fenêtre. */
    Scene fermerScene() {
        Scene s = new Scene("", "");
        s.fermer = true;
        return s;
    }

    /** L'économie de la ville où l'on est (pour les essais). */
    public EconomieVille marcheVille() {
        return marche().ville(villeIci(), jour());
    }

    public Entreprise entreprise() {
        return entreprise;
    }

    /** Par métier, l'index du bâtiment le plus proche dans le village où l'on est (posé par l'écran) : pour savoir quel bâtiment on rachète. */
    public volatile int[] batimentsIci;

    /** La boutique de ce lieu (pour revenir d'une scène d'achat d'atelier). */
    Scene boutiqueScene(int lieu) {
        return boutique(lieu, null);
    }

    Scene boutiqueScene(int lieu, String message) {
        return boutique(lieu, message);
    }

    /** Mon atelier de ce métier, ici, si le bâtiment le plus proche de ce métier est bien le mien. */
    Entreprise.Atelier atelierDevant(int lieu) {
        int v = villeIci();
        Entreprise.Atelier x = entreprise.atelier(v, lieu);
        if (x == null) return null;
        int[] bat = batimentsIci;
        EconomieVille e = marche().ville(v, jour());
        boolean seul = e.agent(lieu) == e.agentJoueur(lieu);
        if (seul || x.batiment < 0 || (bat != null && lieu < bat.length && bat[lieu] == x.batiment)) return x;
        return null;
    }

    public Renommee renommee() {
        return renommee;
    }

    public Destin destin() {
        return destin;
    }

    void annoncer(String texte) {
        annonces.add(texte);
    }

    /** Appelé par l'écran : le prochain message pour le bandeau, ou null. */
    public String prendreAnnonce() {
        return annonces.poll();
    }

    /** L'écran signale que le joueur entre dans un village (fil de l'histoire). */
    public void entrer(int ville) {
        if (fiche == null || ville < 0) return;
        renommee.avancer(jourMaintenant());
        affaires.entrer(ville);
        missions.quetes.entrer(ville);
        noblesse.entrer(ville);
        int r = p.villes.region[ville];
        if (r >= 0) destin.regions.add(r);
        destin.actualiser();
        entreprise.visiter(ville);
        brigands.actualiser(ville);
        brigands.visiter(ville);
        missions.majCible();
    }

    /** L'écran signale que le joueur arrive devant un lieu (index Metiers, ou LIEU_MARCHE) (fil de l'histoire). */
    public void devant(int lieu) {
        if (fiche == null) return;
        destin.actualiser();
        destin.signaler(Destin.VOIR, lieu, 0f);
        entreprise.visiter(villeIci());
    }

    public Missions missions() {
        return missions;
    }

    /** La scène « Missions » (touche Q). */
    public Scene allerAuxMissions() {
        if (fiche == null) return accueil();
        if (devantTableau) return missions.tableau(null);
        return missions.accueil();
    }

    void rafraichirSac() {
        rafraichir();
    }

    /** Fait passer le temps (travail, sommeil) : l'écran l'appliquera à la météo à la prochaine image. */
    synchronized void passer(double heures) {
        if (heures > 0.0) heuresAPasser += heures;
    }

    /** Appelé par l'écran : les heures à ajouter à la météo, remises à zéro. */
    public synchronized double prendreHeures() {
        double h = heuresAPasser;
        heuresAPasser = 0.0;
        return h;
    }

    /** Heures vécues, y compris celles que l'écran n'a pas encore appliquées. */
    public synchronized double maintenant() {
        return heuresVecues + heuresAPasser;
    }

    synchronized double heureIci() {
        double h = heureLocale + heuresAPasser;
        return ((h % 24.0) + 24.0) % 24.0;
    }

    int jourMaintenant() {
        return (int) Math.floor(maintenant() / 24.0);
    }

    public Travail travail() {
        return travail;
    }

    /** La scène « Travail » (touche L). */
    public Scene allerTravailler() {
        if (fiche == null) return accueil();
        return travail.accueil();
    }

    Scene accueilScene() {
        return accueil();
    }

    private Foule foule() {
        return p.foules[peuple][region];
    }

    private boolean enVie() {
        return id >= 0L && foule().estEnVie(place, id);
    }

    private Savoir.Temoin moi() {
        Savoir.Temoin t = Savoir.temoin(p, foule(), place);
        if (fiche != null) t.instruction = fiche.instruction();
        return t;
    }

    public int peuple() {
        return peuple;
    }

    public long identifiant() {
        return id;
    }

    public long apparence() {
        return fiche != null ? fiche.apparence : id;
    }

    public int sexe() {
        return fiche != null ? fiche.sexe : -1;
    }

    public Fiche fiche() {
        return fiche;
    }

    public Scene ouvrir() {
        if (!enVie()) {
            if (creation == null) creation = new Creation(p, this, System.nanoTime(), numeroReserve);
            return creation.reprendre();
        }
        if (fiche != null && famille.mort != null) return famille.succession(false);
        int ville = moi().ville;
        if (ville >= 0) villeDemandee = ville;
        return accueil();
    }

    /** Lot K3 : la création en cours (null quand on joue déjà un personnage) ; l'écran de création la lit. */
    public Creation creationEnCours() {
        return creation;
    }

    public Scene nouvelleCreation() {
        creation = new Creation(p, this, System.nanoTime(), numeroReserve);
        return creation.reprendre();
    }

    private Scene recommencer() {
        Scene s = new Scene("Commencer une autre vie", "Tu vas créer un nouveau personnage. " + (fiche != null ? fiche.nom : "Celui-ci") + " restera dans le monde, comme n'importe quel habitant, mais tu ne le joueras plus.");
        s.ajouter("Créer un nouveau personnage", this::nouvelleCreation);
        s.ajouter("« Revenir", this::accueil);
        return s;
    }

    void adopter(Fiche f, List<Creation.Ajout> nouveaux, int prochain) {
        Foule fl = p.foules[f.peuple][f.region];
        peuple = f.peuple;
        region = f.region;
        id = f.id;
        place = fl.placeDe(id);
        quartier = 0;
        numeroReserve = prochain;
        fiche = f;
        ajouts = new ArrayList<>(nouveaux);
        creation = null;
        carnet.clear();
        travail = new Travail(this, p);
        missions = new Missions(this, p);
        renommee = new Renommee(this, p);
        destin = new Destin(this, p);
        entreprise = new Entreprise(this, p);
        bande = new Bande(this, p);
        brigands = new Brigands(this, p);
        corps = new Corps(this, p);
        commandes = new Commandes(this, p);
        affaires = new Affaires(this, p);
        social = new Social(this, p);
        terre = new Terre(this, p);
        famille = new Famille(this, p);
        noblesse = new Noblesse(this, p);
        armee = new Armee(this, p);
        quotidien = new Quotidien(this, p);
        chansons = new Chansons(this, p);
        batisVue = new int[0];
        maisonVue = null;
        publierBande();
        brigands.publier();
        enregistrer();
        villeDemandee = f.ville;
        nouvelleApparence = true;
        rafraichir();
    }

    static void unir(Foule f, List<Creation.Ajout> liste) {
        for (Creation.Ajout a : liste) {
            if (a.conjoint < 0) continue;
            f.unir(f.placeDe(a.id), f.placeDe(a.conjoint));
        }
    }

    private String presentation(Savoir.Temoin t) {
        if (fiche == null) return Recit.sePresenter(p, t);
        int age = Terre.annee(p, jour()) - fiche.naissance;
        StringBuilder b = new StringBuilder("Tu es ").append(fiche.nom).append(", ").append(age).append(" ans, ");
        b.append(fiche.femme() ? "née" : "né").append(" à ").append(Noms.ville(p.villes.peuple[fiche.ville], p.graine, fiche.ville)).append('.');
        if (fiche.aspiration >= 0) {
            Aspirations.Aspiration a = Aspirations.TOUTES.get(fiche.aspiration);
            if (fiche.etape >= a.etapes.length) b.append(" Ton but : « ").append(a.nom).append(" » — accompli !");
            else b.append(" Ton but : « ").append(a.nom).append(" » (étape ").append(fiche.etape + 1).append(" sur ").append(a.etapes.length).append(" : ").append(a.etapes[Math.min(fiche.etape, a.etapes.length - 1)].toLowerCase()).append(").");
        }
        if (noblesse.noble) b.append(" On t'appelle ").append(noblesse.titre()).append('.');
        b.append(" Tu as ").append(Fiche.argentTexte(fiche.argent)).append(" en poche.");
        if (fiche.allure() < 1f) b.append(" Ton sac est trop lourd (").append(charge()).append(").");
        return b.toString();
    }

    private Scene accueil() {
        Savoir.Temoin t = moi();
        Scene s = new Scene(fiche != null ? fiche.nom : Recit.nom(p, t), (fiche != null ? "Nous sommes au " + Terre.date(p, jour()) + ".\n\n" : "") + presentation(t) + "\n\n" + Recit.gouvernement(p, t));
        if (fiche != null) {
            s.ajouter("Ma fiche", this::voirFiche);
            s.ajouter("Ma famille et mes proches", this::famille);
            s.ajouter("Les notables d'ici", this::notablesIci);
            s.ajouter("Mon destin", this::sceneDestin);
            s.ajouter("Ma vie jusqu'ici", this::vie);
            s.ajouter("Mon sac (" + charge() + ")", () -> sac(null));
            s.ajouter("Aller au marché", () -> allerAuMarche(null));
            s.ajouter(travail.employe() ? "Mon travail" : "Chercher du travail", travail::accueil);
            s.ajouter("Missions (" + missions.acceptees() + " en cours)", missions::accueil);
            s.ajouter("Mes ateliers (" + entreprise.nombre() + ")", () -> entreprise.accueil(null));
            s.ajouter("Mes terres (" + terre.lopins() + " lopin" + (terre.lopins() > 1 ? "s" : "") + ")", terre::accueil);
            if (armee.soldats() > 0 || !armee.couronnes.isEmpty() || armee.contrat != null) s.ajouter("Mon armée (" + armee.effectif() + " hommes)", armee::accueil);
            if (noblesse.servi || noblesse.noble) s.ajouter(noblesse.fiefs.isEmpty() ? "Mon service" : "Mes fiefs (" + noblesse.fiefs.size() + ")", noblesse::accueil);
            s.ajouter(corps.aUneMaison() ? "Ma maison" : "Chercher un logement", () -> corps.logement(null));
            s.ajouter("Ma bande (" + bande.taille() + ")", bande::scene);
        }
        s.ajouter("Ce que je sais", this::memoire);
        s.ajouter("Regarder autour de moi", this::autour);
        s.ajouter("Chercher des livres et des chansons", () -> etagere(0));
        s.ajouter("Relire mon carnet (" + carnet.size() + ")", this::relire);
        if (fiche != null) s.ajouter("Changer d'apparence", () -> {
            fiche.apparence = fiche.apparence * 6364136223846793005L + 1442695040888963407L;
            enregistrer();
            nouvelleApparence = true;
            return accueil();
        });
        s.ajouter("Commencer une autre vie", this::recommencer);
        return s;
    }

    private Scene voirFiche() {
        Scene s = new Scene(fiche.nom, fiche.resume());
        s.ajouter("« Revenir", this::accueil);
        return s;
    }

    private Scene vie() {
        Scene s = new Scene("Ma vie jusqu'ici", String.join(" ", fiche.recit));
        s.ajouter("« Revenir", this::accueil);
        return s;
    }

    private Scene sceneDestin() {
        if (fiche != null) return destin.scene();
        if (fiche.aspiration < 0) {
            Scene s = new Scene("Mon destin", "Tu n'as pas encore d'aspiration.");
            s.ajouter("« Revenir", this::accueil);
            return s;
        }
        Aspirations.Aspiration a = Aspirations.TOUTES.get(fiche.aspiration);
        String texte = Aspirations.decrire(fiche.aspiration, fiche.etape);
        if (fiche.aspiration == Aspirations.VENGEANCE && fiche.ennemi >= 0) {
            texte = "Contre " + Noms.ville(p.etats.peuple[fiche.ennemi], p.graine, p.etats.capitale[fiche.ennemi]) + (p.etats.estVivant(fiche.ennemi) ? "" : ", qui n'existe plus : il faudra retrouver les héritiers") + ". " + texte;
        }
        Scene s = new Scene(a.nom, texte + "\n\nLes envies du jour et le suivi des étapes arriveront avec le renom et le Destin (phase 6).");
        s.ajouter("« Revenir", this::accueil);
        return s;
    }

    private Scene famille() {
        Foule f = foule();
        StringBuilder texte = new StringBuilder();
        Scene s = new Scene("Ma famille et mes proches", "");
        List<String> lignes = new ArrayList<>();
        for (Fiche.Proche q : fiche.proches) {
            int lieu = q.mort ? -1 : f.placeDe(q.id);
            String nom = famille.nom(q.id, Noms.personne(peuple, p.graine, q.id, q.sexe));
            String role = fiche.role(q) + (q.note.isEmpty() ? "" : ", " + q.note);
            // Lot G4 : le métier — celui de l'origine pour les parents, sinon tiré de la ville où l'on vit.
            String metier = q.role == Fiche.PERE || q.role == Fiche.MERE ? Emplois.parent(fiche.origine, q.sexe) : null;
            String talent = "";
            String etat;
            if (lieu < 0) etat = q.mort ? (q.sexe == 1 ? "morte" : "mort") : "disparu" + (q.sexe == 1 ? "e" : "");
            else {
                Savoir.Temoin autre = Savoir.temoin(p, f, lieu);
                int ville = autre.ville;
                etat = Famille.ans(Terre.annee(p, jour()) - autre.naissance) + (ville >= 0 && ville != fiche.ville ? ", vit à " + Noms.ville(p.villes.peuple[ville], p.graine, ville) : "");
                if (metier == null && !(q.role == Fiche.MAITRE && !q.note.isEmpty())) metier = Emplois.metier(this, p, ville, q.id);
                if (q.role == Fiche.PERE || q.role == Fiche.MERE) gens.fixerLieu(q.id, Creation.lieuOrigine(fiche.origine));
                talent = Gens.meilleure(gens.retenir(f, lieu));
                s.ajouter("Parler à " + nom + " (" + role + ")", () -> rencontrer(autre));
            }
            lignes.add(nom + " — " + role + (metier == null ? "" : ", " + metier) + (talent.isEmpty() ? "" : " [" + talent + "]") + " (" + etat + ")" + (q.estime >= 50 ? ", très proche" : q.estime < 0 ? ", en froid" : ""));
        }
        lignes.addAll(social.lignesFamille());
        texte.append(lignes.isEmpty() ? "Tu n'as personne." : String.join("\n", lignes));
        Scene vraie = new Scene("Ma famille et mes proches", texte.toString());
        vraie.choix.addAll(s.choix);
        vraie.suites.addAll(s.suites);
        vraie.ajouter("« Revenir", this::accueil);
        return vraie;
    }

    private Scene memoire() {
        Savoir.Temoin t = moi();
        Scene s = new Scene("Ce que je sais", Recit.parler(p, t, Savoir.souvenirs(p, t, SOUVENIRS), 8));
        s.ajouter("Revenir", this::accueil);
        return s;
    }

    private List<Integer> entourage(Savoir.Temoin t) {
        Foule f = foule();
        List<Integer> places = new ArrayList<>();
        Etats e = p.etats;
        if (t.etat >= 0 && e.capitale[t.etat] == t.ville && e.peuple[t.etat] == peuple && e.placeChef(t.etat) != place) {
            places.add(e.placeChef(t.etat));
        }
        for (int k = 0; k < VOISINS * 4 && places.size() < VOISINS; k++) {
            long tirage = (long) (Savoir.tirage(id + quartier, k) * Long.MAX_VALUE);
            int voisin = f.habitantAuHasard(t.ville, p.annee, p.reglages.ageAdulte, tirage);
            if (voisin >= 0 && voisin != place && !places.contains(voisin)) places.add(voisin);
        }
        return places;
    }

    private Scene autour() {
        Savoir.Temoin t = moi();
        Foule f = foule();
        List<Integer> places = entourage(t);
        Scene s = new Scene("Autour de moi", places.isEmpty() ? "Il n'y a personne ici." : "À qui veux-tu parler ?");
        for (int voisin : places) {
            Savoir.Temoin autre = Savoir.temoin(p, f, voisin);
            s.ajouter(Recit.decrire(p, autre), () -> rencontrer(autre));
        }
        s.ajouter("Aller dans un autre quartier", () -> {
            quartier++;
            return autour();
        });
        s.ajouter("Revenir", this::accueil);
        return s;
    }

    private Scene rencontrer(Savoir.Temoin t) {
        return conversation(t, Recit.saluer(p, t), 0);
    }

    private Scene conversation(Savoir.Temoin t, String texte, int rang) {
        Scene s = new Scene(Recit.nom(p, t), texte);
        s.ajouter("Qui es-tu ?", () -> conversation(t, Recit.sePresenter(p, t) + portrait(t), rang));
        s.ajouter("Qui gouverne ici ?", () -> conversation(t, Recit.gouvernement(p, t), rang));
        s.ajouter("Raconte-moi le passé", () -> raconter(t, rang));
        s.ajouter("Au revoir", this::accueil);
        return s;
    }

    /** Lot H1 : ce qu'on remarque de quelqu'un (son métier, ce qu'il sait faire, sa fortune), d'après sa fiche. */
    private String portrait(Savoir.Temoin t) {
        int[] q = gens.place(t);
        if (q == null) return "";
        Foule f = p.foules[q[0]][q[1]];
        Fiche x = gens.de(f, q[2]);
        String metier = Gens.age(f, q[2], p.annee) < Gens.AGE_TRAVAIL ? "est encore un enfant" : Emplois.metier(this, p, f.villeDe(q[2]), x.id);
        return "\n\n(" + x.nom.split(" ")[0] + " " + metier + ". " + gens.portrait(x) + notables.role(x.id) + gens.journee(f, q[2], x) + ")";
    }

    /** Lot H3 : les notables de la ville où l'on est (ou de celle où l'on vit), leur but, où ils en sont, et qui est ton rival. */
    private Scene notablesIci() {
        int v = villeIci() >= 0 ? villeIci() : fiche.ville;
        notables.suivre(v);
        List<String> l = notables.lignes(v);
        String ville = v >= 0 ? Noms.ville(p.villes.peuple[v], p.graine, v) : "ici";
        Scene s = new Scene("Les notables " + Creation.de(ville), l.isEmpty() ? "Personne ici ne compte plus que les autres." : String.join("\n", l));
        s.ajouter("« Revenir", this::accueil);
        return s;
    }

    /** Essai du lot H3 : la page des notables. */
    public String notablesPourEssai() {
        return notablesIci().texte;
    }

    /** Essai du lot H1 : le portrait d'un habitant de la ville, tel que « Qui es-tu ? » le montre. */
    public String portraitPourEssai(int ville, int k) {
        Foule f = p.foules[p.villes.peuple[ville]][p.villes.region[ville]];
        int q = f.habitantAuHasard(ville, p.annee, 0, Travail.melanger(k * 104729L + 5L));
        return q < 0 ? "" : Recit.sePresenter(p, Savoir.temoin(p, f, q)) + portrait(Savoir.temoin(p, f, q));
    }

    private Scene raconter(Savoir.Temoin t, int rang) {
        List<String> phrases = Recit.paroles(p, t, Savoir.souvenirs(p, t, SOUVENIRS));
        if (phrases.isEmpty()) return conversation(t, "Je ne sais pas grand-chose de tout ça. Ici, on s'occupe de ses bêtes et de sa récolte.", rang);
        if (rang >= phrases.size()) return conversation(t, "Je t'ai dit tout ce que je sais.", rang);
        int fin = Math.min(phrases.size(), rang + PAROLES);
        String nom = Recit.nom(p, t);
        StringBuilder texte = new StringBuilder();
        for (int k = rang; k < fin; k++) {
            if (k > rang) texte.append("\n\n");
            texte.append(phrases.get(k));
            noter(nom + " : " + phrases.get(k));
        }
        enregistrer();
        return conversation(t, texte.toString(), fin);
    }

    Scene etagere(int debut) {
        Savoir.Temoin t = moi();
        Livres l = p.livres;
        List<Integer> oeuvres = new ArrayList<>();
        for (int o = 0; o < l.nombre; o++) {
            if (l.accessible(p, o, t)) oeuvres.add(o);
        }
        boolean lecteur = t.instruction >= l.reglages.lecture;
        String texte = oeuvres.isEmpty() && chansons.ici(villeIci()) == 0 ? "Il n'y a ici rien que tu puisses lire ou chanter." : lecteur ? "Voici ce que tu trouves ici." : "Tu ne sais pas lire. Il te reste les chansons d'ici.";
        Scene s = new Scene("Livres et chansons", texte);
        int fin = Math.min(oeuvres.size(), debut + ETAGERE);
        for (int k = debut; k < fin; k++) {
            int o = oeuvres.get(k);
            s.ajouter(Recit.titre(p, o), () -> lire(o, debut));
        }
        if (debut == 0) chansons.choixEtagere(s, villeIci(), debut);
        if (oeuvres.size() > ETAGERE) s.ajouter("Autres titres", () -> etagere(fin >= oeuvres.size() ? 0 : fin));
        s.ajouter("Revenir", this::accueil);
        return s;
    }

    private Scene lire(int o, int debut) {
        String titre = Recit.titre(p, o);
        noter((p.livres.ecrit[o] ? "Lu : " : "Entendu : ") + titre);
        if (fiche != null && p.livres.ecrit[o]) destin.signaler(Destin.LIRE, o, 0f);
        if (fiche != null && !p.livres.ecrit[o]) chansons.entendre(o);
        enregistrer();
        Scene s = new Scene(titre, Recit.lire(p, o));
        s.ajouter("Reposer", () -> etagere(debut));
        return s;
    }

    private Scene relire() {
        Scene s = new Scene("Mon carnet", carnet.isEmpty() ? "Ton carnet est vide." : String.join("\n\n", carnet));
        s.ajouter("Revenir", this::accueil);
        return s;
    }

    // ---------------------------------------------------------------- Phase 2 : sac, marché

    int villeIci() {
        int v = villeVue;
        if (v >= 0) return v;
        if (partout) {
            v = moi().ville;
            return v >= 0 ? v : fiche.ville;
        }
        return -1;
    }

    /** Appelé par l'écran à chaque image avec l'heure de la météo : ajoute le temps écoulé (les sauts en arrière sont ignorés). */
    public void horloge(double heures) {
        if (!Double.isNaN(derniereHeure) && heures > derniereHeure) heuresVecues += Math.min(48.0, heures - derniereHeure);
        derniereHeure = heures;
    }

    public int jour() {
        return (int) Math.floor(heuresVecues / 24.0);
    }

    /** L'économie de la ville où l'on est (pour l'affichage de débogage). */
    public EconomieVille economieIci() {
        int v = villeIci();
        return v < 0 ? null : marche().ville(v, jour());
    }

    Marche marche() {
        if (marche == null) marche = new Marche(p);
        return marche;
    }

    /** À appeler après tout changement du sac : met à jour l'allure et ce que le sprite porte. */
    private void rafraichir() {
        rangerLivres();
        if (fiche == null) {
            allure = 1f;
            portes = null;
        } else {
            allure = fiche.allure() * (corps != null && corps.faible() ? 0.75f : 1f);
            portes = fiche.sac.portes();
        }
        versionTenue++;
    }

    /**
     * Lot G4 : tient la liste des livres au même nombre que les livres (Biens.OUVRAGE) du sac. Un livre de plus prend le titre d'une
     * oeuvre écrite dont la ville a une copie (sinon une oeuvre du peuple du joueur) ; un livre de moins (vendu, jeté) enlève le dernier.
     * Rend les titres des livres nouveaux.
     */
    private List<String> rangerLivres() {
        List<String> nouveaux = new ArrayList<>();
        if (fiche == null) return nouveaux;
        int n = fiche.sac.nombre(Biens.OUVRAGE.id);
        while (bibliotheque.size() > n) bibliotheque.remove(bibliotheque.size() - 1);
        while (bibliotheque.size() < n) {
            int o = livreAuHasard();
            bibliotheque.add(o);
            if (o >= 0) nouveaux.add("« " + Recit.titre(p, o) + " »");
        }
        return nouveaux;
    }

    private int livreAuHasard() {
        Livres l = p.livres;
        int ville = villeIci();
        List<Integer> ici = new ArrayList<>(), duPeuple = new ArrayList<>();
        for (int o = 0; o < l.nombre; o++) {
            if (!l.ecrit[o] || bibliotheque.contains(o)) continue;
            if (l.peuple[o] == peuple) duPeuple.add(o);
            for (int k = 0; k < l.nombreCopies[o]; k++) {
                if (l.copies[o][k] == ville) {
                    ici.add(o);
                    break;
                }
            }
        }
        List<Integer> choix = !ici.isEmpty() ? ici : duPeuple;
        if (choix.isEmpty()) return -1;
        long g = Travail.melanger(id * 31L + jour() * 7919L + bibliotheque.size() * 104729L);
        return choix.get((int) Long.remainderUnsigned(g, choix.size()));
    }

    /** Lot G4 : lire un des livres du sac. */
    private Scene lireLivre(int o, int bien) {
        if (o < 0 || o >= p.livres.nombre) return objet(bien, null);
        if (moi().instruction < p.livres.reglages.lecture) return objet(bien, "Tu ne sais pas lire : les lignes ne te disent rien.");
        String titre = Recit.titre(p, o);
        noter("Lu : " + titre);
        destin.signaler(Destin.LIRE, o, 0f);
        enregistrer();
        Scene s = new Scene(titre, Recit.lire(p, o));
        s.ajouter("Refermer le livre", () -> objet(bien, null));
        return s;
    }

    /** Essai du lot G4 : ajoute des livres au sac ; rend leurs titres, la fiche du livre (texte et choix) et la famille. */
    public String livresPourEssai(int n) {
        fiche.sac.ajouter(Biens.OUVRAGE.id, n);
        List<String> t = rangerLivres();
        rafraichir();
        Scene o = objet(Biens.OUVRAGE.id, null);
        String lu = "";
        if (!bibliotheque.isEmpty() && bibliotheque.get(0) >= 0) {
            Scene l = lireLivre(bibliotheque.get(0), Biens.OUVRAGE.id);
            lu = l.titre + " : " + l.texte.substring(0, Math.min(200, l.texte.length()));
        }
        return "TITRES " + t + "\nOBJET " + o.texte + "\nCHOIX " + o.choix + "\nLU " + lu + "\nFAMILLE\n" + famille().texte;
    }

    private String charge() {
        return Biens.poids(fiche.sac.poids()) + " sur " + Biens.poids(fiche.capacite());
    }

    static String avec(String message, String texte) {
        return message == null ? texte : message + "\n\n" + texte;
    }

    public Scene sac() {
        return sac(null);
    }

    private Scene sac(String message) {
        if (fiche == null) return accueil();
        StringBuilder t = new StringBuilder("Argent : ").append(Fiche.argentTexte(fiche.argent)).append(". Charge : ").append(charge()).append('.');
        if (fiche.allure() < 1f) t.append(" Tu es trop chargé").append(fiche.e()).append(" : tu avances moins vite.");
        if (!fiche.objets.isEmpty()) t.append("\nTes biens : ").append(String.join(", ", fiche.objets)).append('.');
        if (fiche.sac.piles.isEmpty()) t.append("\n\nTon sac est vide.");
        Scene s = new Scene("Mon sac", avec(message, t.toString()));
        for (Inventaire.Pile pile : fiche.sac.piles) {
            Biens.Bien b = Biens.get(pile.bien);
            int bien = pile.bien;
            String libelle = b.majuscule() + (pile.nombre > 1 ? " ×" + pile.nombre : "") + (pile.porte ? "  (" + Biens.NOMS_PLACES[b.place] + ")" : "");
            s.ajouter(libelle, () -> objet(bien, null));
        }
        s.ajouter("« Revenir", this::accueil);
        return s;
    }

    private Scene objet(int bien, String message) {
        Biens.Bien b = Biens.get(bien);
        int n = fiche.sac.nombre(bien);
        if (n <= 0) return sac(message);
        Scene s = new Scene(b.majuscule(), avec(message, Biens.description(b) + "\nTu en as " + n + "."));
        if (b.place >= 0) {
            if (fiche.sac.porte(bien)) s.ajouter(b.place == Biens.BETE ? "Laisser à l'écurie" : "Ranger dans le sac", () -> {
                fiche.sac.ranger(bien);
                rafraichir();
                enregistrer();
                return objet(bien, null);
            });
            else {
                int avant = fiche.sac.porteA(b.place);
                String remplace = avant >= 0 ? " (à la place " + de(Biens.get(avant).un()) + ")" : "";
                s.ajouter((b.place == Biens.BETE ? "Prendre avec moi" : "Porter") + remplace, () -> {
                    fiche.sac.porter(bien);
                    rafraichir();
                    enregistrer();
                    return objet(bien, null);
                });
            }
        }
        if (Corps.nourrit(b) > 0) s.ajouter(b == Biens.BIERE || b == Biens.VIN ? "En boire" : "En manger", () -> corps.manger(bien));
        if (bien == Biens.OUVRAGE.id) {
            List<Integer> vus = new ArrayList<>();
            for (int o : bibliotheque) {
                if (o < 0 || vus.contains(o)) continue;
                vus.add(o);
                s.ajouter("Lire « " + Recit.titre(p, o) + " »", () -> lireLivre(o, bien));
            }
        }
        s.ajouter(n > 1 ? "En jeter un" : "Le jeter", () -> {
            fiche.sac.retirer(bien, 1);
            rafraichir();
            enregistrer();
            return objet(bien, "Tu jettes " + b.un() + ".");
        });
        if (n > 1) s.ajouter("Tout jeter", () -> {
            fiche.sac.retirer(bien, n);
            rafraichir();
            enregistrer();
            return sac("Tu jettes " + b.quantite(n) + ".");
        });
        s.ajouter("« Revenir", () -> sac(null));
        return s;
    }

    private static String de(String groupe) {
        if (groupe.startsWith("un ")) return "d'" + groupe;
        if (groupe.startsWith("une ")) return "d'" + groupe;
        return "de " + groupe;
    }

    // ---------------------------------------------------------------- Phase 3 : commerce sur place

    /** Distance (m) à un lieu de commerce. Les fermes, bûcherons et mine sont hors des murs ; un atelier sans bâtiment dans le village tient un étal sur la place. */
    private float distance(int lieu) {
        float[] d = distances;
        if (d == null) return Float.NaN;
        if (lieu == Biens.CHAMPS || lieu == Biens.FORET || lieu == Biens.MINE) return d[LIEU_DEHORS];
        if (lieu == Biens.MARCHE) return d[LIEU_MARCHE];
        if (lieu >= 0 && lieu < Metiers.NOMBRE && !Float.isNaN(d[lieu])) return d[lieu];
        return d[LIEU_MARCHE];
    }

    boolean proche(EconomieVille.Agent a) {
        if (partout) return true;
        float d = distance(a.lieu);
        return !Float.isNaN(d) && d <= PROCHE;
    }

    String ouEst(EconomieVille.Agent a) {
        float d = distance(a.lieu);
        if (a.source()) return Float.isNaN(d) ? "hors de la ville" : "hors de la ville, à " + Math.round(d) + " m";
        boolean surPlace = a.lieu != Biens.MARCHE && distances != null && a.lieu < Metiers.NOMBRE && Float.isNaN(distances[a.lieu]);
        if (Float.isNaN(d)) return "loin";
        return "à " + Math.round(d) + " m" + (surPlace ? " (un étal sur la place)" : "");
    }

    /** Touche M : la boutique du marchand devant lequel on se tient ; sinon, la liste des marchands de la ville et leur distance. */
    public Scene allerAuMarche() {
        if (fiche != null && corps.chezSoi()) return corps.coffre(null);
        if (fiche != null && !partout) {
            Scene noble = noblesse.devant(distances);
            if (noble != null) return noble;
            Scene caserne = armee.devant(distances);
            if (caserne != null) return caserne;
            EconomieVille.Agent a = agentDevant();
            if (a != null) {
                Entreprise.Atelier mien = a.lieu < Metiers.NOMBRE ? atelierDevant(a.lieu) : null;
                if (mien != null) return entreprise.gerer(mien, null);
                return boutique(a.lieu, null);
            }
            // Devant une ruine : y bâtir un atelier.
            if (villeIci() >= 0 && ruineProche >= 0 && ruineDistance <= Affaires.RUINE_PROCHE) return affaires.ruine(null);
            // Lot G5 : devant un terrain libre : y bâtir un atelier.
            double[] t = terrainLibre;
            if (t != null && (int) t[0] == villeIci()) return affaires.terrain(null);
        }
        return allerAuMarche(null);
    }

    /** Le marchand le plus proche à moins de PROCHE mètres, ou null. */
    EconomieVille.Agent agentDevant() {
        int v = villeIci();
        if (v < 0) return null;
        EconomieVille.Agent mieux = null;
        float dm = PROCHE;
        for (EconomieVille.Agent a : marche().ville(v, jour()).agents) {
            if (a.lieu == Biens.DEHORS) continue;
            float d = distance(a.lieu);
            if (Float.isNaN(d) || d > dm) continue;
            // Un atelier sans bâtiment tient un étal sur la place : devant la place, c'est le marché qu'on voit d'abord.
            if (a.lieu != Biens.MARCHE && !a.source() && distances != null && a.lieu < Metiers.NOMBRE && Float.isNaN(distances[a.lieu])) continue;
            if (mieux == null || d < dm) {
                dm = d;
                mieux = a;
            }
        }
        return mieux;
    }

    private Scene allerAuMarche(String message) {
        if (fiche == null) return accueil();
        int v = villeIci();
        if (v < 0) {
            Scene s = new Scene("Commerce", avec(message, "Tu n'es dans aucune ville. Approche-toi d'un village pour commercer."));
            s.ajouter("Mon sac", () -> sac(null));
            s.ajouter("« Revenir", this::accueil);
            return s;
        }
        EconomieVille e = marche().ville(v, jour());
        String ville = Noms.ville(p.villes.peuple[v], p.graine, v);
        StringBuilder t = new StringBuilder("Les marchands ").append(Creation.de(ville)).append(". Tu as ").append(Fiche.argentTexte(fiche.argent)).append('.');
        if (p.villes.peuple[v] != fiche.peuple) t.append(" Ici, tu es étranger").append(fiche.e()).append(" : on te fait payer un peu plus.");
        t.append("\nOn ne commerce qu'avec ce qui est devant soi : va jusqu'à l'atelier, l'étal ou la ferme.");
        Scene s = new Scene("Commerce", avec(message, t.toString()));
        for (int passe = 0; passe < 2; passe++) {
            for (EconomieVille.Agent a : e.agents) {
                if (a.lieu == Biens.DEHORS || a.joueur || proche(a) != (passe == 0)) continue;
                if (passe == 0) s.ajouter(a.nom + " — ici", () -> boutique(a.lieu, null));
                else s.ajouter(a.nom + " — " + ouEst(a), () -> loin(a.lieu));
            }
        }
        s.ajouter("Mon sac", () -> sac(null));
        s.ajouter("« Revenir", this::accueil);
        return s;
    }

    private Scene loin(int lieu) {
        EconomieVille e = marche().ville(villeIci(), jour());
        EconomieVille.Agent a = e.agent(lieu);
        if (a == null) return allerAuMarche(null);
        StringBuilder t = new StringBuilder(a.nom).append(" : ").append(ouEst(a)).append(". Va sur place pour voir ce qu'on y vend et à quel prix.\n\n");
        if (a.lieu == Biens.MARCHE) t.append("Les étals revendent ce qu'ils achètent aux artisans, aux fermes et aux colporteurs, avec leur marge : c'est plus cher, mais tout est au même endroit et tout le monde y passe.");
        else if (a.acces() < 0.3f) t.append("Peu de gens font le chemin. On y paie le prix du producteur, sans marchand entre les deux : c'est moins cher qu'au marché.");
        else t.append("Un atelier de la ville : il vend ce qu'il fabrique au prix de l'artisan.");
        Scene s = new Scene(a.nom, t.toString());
        s.ajouter("« Revenir", () -> allerAuMarche(null));
        return s;
    }

    EconomieVille.Agent agentIci(int lieu) {
        int v = villeIci();
        if (v < 0) return null;
        EconomieVille e = marche().ville(v, jour());
        EconomieVille.Agent a = e.agent(lieu);
        // Devant mon propre atelier, c'est lui (et non celui du maître du même métier) qu'on trouve.
        if (lieu >= 0 && lieu < Metiers.NOMBRE && entreprise.atelier(v, lieu) != null && atelierDevant(lieu) != null && e.agentJoueur(lieu) != null) a = e.agentJoueur(lieu);
        return a != null && proche(a) ? a : null;
    }

    /** Une boutique : chaque marchandise du jour est un choix (prix et réserve dans le libellé), puis « Vendre… ». */
    private Scene boutique(int lieu, String message) {
        EconomieVille.Agent a = agentIci(lieu);
        if (a == null) return allerAuMarche("Tu n'es plus assez près.");
        EconomieVille e = marche().ville(villeIci(), jour());
        if (message == null) reactionLieu = lieu;
        Scene vraie = new Scene(a.nom, "");
        int presents = 0;
        for (int b = 0; b < a.vend.length; b++) {
            if (!a.vend[b]) continue;
            int n = marche().stock(a, b);
            if (n <= 0) continue;
            presents++;
            Biens.Bien x = Biens.get(b);
            int prix = marche().prixAchat(fiche, e, a, b);
            int bien = b;
            vraie.ajouter(x.majuscule() + " — " + Fiche.argentTexte(prix) + "  (" + n + ")" + (prix > fiche.argent ? "  trop cher" : ""), () -> combienAcheter(lieu, bien));
        }
        StringBuilder t = new StringBuilder(presents == 0 ? "Il n'y a plus rien à acheter aujourd'hui." : "Que veux-tu acheter ? Entre parenthèses, ce qui reste en réserve.");
        if (!a.recettes.isEmpty()) t.append(" ").append(Math.round(a.ouvriers)).append(a.ouvriers >= 1.5f ? " personnes travaillent ici." : " personne travaille ici.");
        t.append("\nTu as ").append(Fiche.argentTexte(fiche.argent)).append(". Charge : ").append(charge()).append('.');
        Scene s = new Scene(a.nom, avec(message, t.toString()));
        s.choix.addAll(vraie.choix);
        s.suites.addAll(vraie.suites);
        commandes.choix(s, lieu, a);
        missions.quetes.choixBoutique(s, lieu, a);
        social.choixBoutique(s, lieu, a);
        chansons.choixBoutique(s, lieu);
        terre.choixBoutique(s, lieu, a);
        s.ajouter("Vendre…", () -> ventes(lieu, null));
        if ((lieu == Metiers.AUBERGE || lieu == Metiers.TAVERNE) && fiche.argent >= Corps.PRIX_REPAS)
            s.ajouter("Prendre un repas chaud (" + Fiche.argentTexte(Corps.PRIX_REPAS) + ")", () -> corps.repas(a, lieu));
        if (a.joueur) s.ajouter("Gérer l'atelier", () -> {
            Entreprise.Atelier x = entreprise.atelier(villeIci(), lieu);
            return x == null ? entreprise.accueil(null) : entreprise.gerer(x, null);
        });
        else if (lieu < Metiers.NOMBRE && !a.recettes.isEmpty()) s.ajouter("Proposer de racheter l'atelier…", () -> entreprise.proposer(lieu));
        if (a.joueur && !a.recettes.isEmpty()) s.ajouter("Travailler toi-même à l'atelier (à la main)", () -> travail.commencerAMain(lieu, Travail.PATRON));
        s.ajouter("« Revenir", () -> allerAuMarche(null));
        return s;
    }

    private Scene combienAcheter(int lieu, int bien) {
        EconomieVille.Agent a = agentIci(lieu);
        if (a == null) return allerAuMarche("Tu n'es plus assez près.");
        EconomieVille e = marche().ville(villeIci(), jour());
        Biens.Bien x = Biens.get(bien);
        int prix = marche().prixAchat(fiche, e, a, bien);
        int stock = marche().stock(a, bien);
        int max = Math.min(stock, fiche.argent / prix);
        String texte = Biens.description(x) + "\n\nIci : " + Fiche.argentTexte(prix) + " pièce. " + stock + " en réserve. Tu peux en payer " + max + ".";
        Scene s = new Scene(x.majuscule(), texte);
        for (int q : new int[] { 1, 5, 10 }) {
            if (q > max || (q > 1 && q == max)) continue;
            s.ajouter("En acheter " + q + " (" + Fiche.argentTexte(prix * q) + ")", () -> acheter(lieu, bien, q));
        }
        if (max > 1) s.ajouter("En acheter " + max + " (" + Fiche.argentTexte(prix * max) + ")", () -> acheter(lieu, bien, max));
        if (max >= 1 && x.place >= 0) s.ajouter("L'acheter et le porter tout de suite", () -> {
            Scene suite = acheter(lieu, bien, 1);
            if (fiche.sac.nombre(bien) > 0) {
                fiche.sac.porter(bien);
                rafraichir();
                enregistrer();
            }
            return suite;
        });
        s.ajouter("« Revenir", () -> boutique(lieu, null));
        return s;
    }

    private Scene acheter(int lieu, int bien, int n) {
        EconomieVille.Agent a = agentIci(lieu);
        if (a == null) return allerAuMarche("Tu n'es plus assez près.");
        String m = marche().acheter(fiche, marche().ville(villeIci(), jour()), a, bien, n);
        if (m.startsWith("Tu achètes")) {
            gesteJoueur = "donner";
            destin.signaler(Destin.OBJET, bien, 0f);
            if (bien == Biens.BIERE.id && (lieu == Metiers.TAVERNE || lieu == Metiers.AUBERGE)) destin.signaler(Destin.BIERE, bien, 0f);
            List<String> titres = rangerLivres();
            if (!titres.isEmpty()) m += (titres.size() == 1 ? " C'est " : " Ce sont ") + String.join(", ", titres) + ".";
        }
        rafraichir();
        enregistrer();
        return boutique(lieu, m);
    }

    private Scene ventes(int lieu, String message) {
        EconomieVille.Agent a = agentIci(lieu);
        if (a == null) return allerAuMarche("Tu n'es plus assez près.");
        EconomieVille e = marche().ville(villeIci(), jour());
        Scene s = new Scene(a.nom + " : vendre", avec(message, a.nom + " a " + Fiche.argentTexte((int) Math.max(0, a.caisse)) + " en caisse. Que veux-tu vendre ?"));
        int offres = 0;
        for (Inventaire.Pile pile : fiche.sac.piles) {
            int prix = marche().prixVente(fiche, e, a, pile.bien);
            if (prix <= 0) continue;
            Biens.Bien x = Biens.get(pile.bien);
            int bien = pile.bien;
            offres++;
            s.ajouter(x.majuscule() + (pile.nombre > 1 ? " ×" + pile.nombre : "") + " — on t'en offre " + Fiche.argentTexte(prix), () -> combienVendre(lieu, bien));
        }
        if (offres == 0) s = new Scene(a.nom + " : vendre", avec(message, "Tu n'as rien qui intéresse " + a.nom.toLowerCase() + "."));
        s.ajouter("« Revenir", () -> boutique(lieu, null));
        return s;
    }

    private Scene combienVendre(int lieu, int bien) {
        EconomieVille.Agent a = agentIci(lieu);
        if (a == null) return allerAuMarche("Tu n'es plus assez près.");
        EconomieVille e = marche().ville(villeIci(), jour());
        Biens.Bien x = Biens.get(bien);
        int prix = marche().prixVente(fiche, e, a, bien);
        int n = fiche.sac.nombre(bien);
        int max = Math.min(n, prix <= 0 ? 0 : (int) Math.max(0, a.caisse) / prix);
        String porte = fiche.sac.porte(bien) ? " Tu le portes en ce moment." : "";
        Scene s = new Scene(x.majuscule(), "On t'en offre " + Fiche.argentTexte(prix) + " pièce. Tu en as " + n + ", on peut t'en prendre " + max + "." + porte);
        for (int q : new int[] { 1, 5, 10 }) {
            if (q > max || (q > 1 && q == max)) continue;
            s.ajouter("En vendre " + q + " (" + Fiche.argentTexte(prix * q) + ")", () -> vendre(lieu, bien, q));
        }
        if (max > 1) s.ajouter("En vendre " + max + " (" + Fiche.argentTexte(prix * max) + ")", () -> vendre(lieu, bien, max));
        s.ajouter("« Revenir", () -> ventes(lieu, null));
        return s;
    }

    private Scene vendre(int lieu, int bien, int n) {
        EconomieVille.Agent a = agentIci(lieu);
        if (a == null) return allerAuMarche("Tu n'es plus assez près.");
        String m = marche().vendre(fiche, marche().ville(villeIci(), jour()), a, bien, n);
        if (m.startsWith("Tu vends")) gesteJoueur = "recevoir";
        rafraichir();
        enregistrer();
        return ventes(lieu, m);
    }

    void noter(String phrase) {
        carnet.add(phrase);
    }

    public void enregistrer() {
        try (DataOutputStream sortie = new DataOutputStream(new BufferedOutputStream(fichier.write(false)))) {
            sortie.writeInt(FORMAT);
            sortie.writeInt(peuple);
            sortie.writeInt(region);
            sortie.writeInt(place);
            sortie.writeLong(id);
            sortie.writeInt(quartier);
            sortie.writeInt(carnet.size());
            for (String phrase : carnet) {
                sortie.writeUTF(phrase);
            }
            sortie.writeInt(numeroReserve);
            sortie.writeBoolean(fiche != null);
            if (fiche != null) fiche.ecrire(sortie);
            sortie.writeInt(ajouts.size());
            for (Creation.Ajout a : ajouts) a.ecrire(sortie);
            sortie.writeDouble(maintenant());
            marche().ecrire(sortie);
            travail.ecrire(sortie);
            missions.ecrire(sortie);
            renommee.ecrire(sortie);
            destin.ecrire(sortie);
            entreprise.ecrire(sortie);
            bande.ecrire(sortie);
            brigands.ecrire(sortie);
            corps.ecrire(sortie);
            commandes.ecrire(sortie);
            affaires.ecrire(sortie);
            missions.ecrireSuite(sortie);
            renommee.ecrireSuite(sortie);
            social.ecrire(sortie);
            terre.ecrire(sortie);
            famille.ecrire(sortie);
            noblesse.ecrire(sortie);
            armee.ecrire(sortie);
            quotidien.ecrire(sortie);
            chansons.ecrire(sortie);
            sortie.writeInt(bibliotheque.size());
            for (int o : bibliotheque) sortie.writeInt(o);
            affaires.ecrireConstructions(sortie);
            gens.ecrire(sortie);
            notables.ecrire(sortie);
            sortie.writeBoolean(fiche != null);
            if (fiche != null) fiche.aspect.ecrire(sortie);
        } catch (IOException | GdxRuntimeException e) {
            System.err.println("Impossible d'enregistrer le joueur : " + e.getMessage());
        }
    }

    public void charger() {
        if (!fichier.exists()) return;
        try (DataInputStream entree = new DataInputStream(new BufferedInputStream(fichier.read()))) {
            int format = entree.readInt();
            if (format < 1 || format > FORMAT) return;
            int q = entree.readInt(), r = entree.readInt(), lieu = entree.readInt();
            long numero = entree.readLong();
            int coin = entree.readInt(), notes = entree.readInt();
            List<String> lues = new ArrayList<>();
            for (int k = 0; k < notes; k++) {
                lues.add(entree.readUTF());
            }
            Fiche lue = null;
            List<Creation.Ajout> liste = new ArrayList<>();
            int reserve = 0;
            if (format >= 2) {
                reserve = entree.readInt();
                if (entree.readBoolean()) lue = Fiche.lire(entree, format >= 3);
                int n = entree.readInt();
                for (int k = 0; k < n; k++) liste.add(Creation.Ajout.lire(entree));
            }
            double vecues = 0.0;
            Marche marcheLu = null;
            if (format >= 4) {
                vecues = entree.readDouble();
                marcheLu = new Marche(p);
                marcheLu.lire(entree);
            }
            Travail travailLu = new Travail(this, p);
            if (format >= 5) travailLu.lire(entree);
            Missions missionsLues = new Missions(this, p);
            if (format >= 6) missionsLues.lire(entree);
            Renommee renommeeLue = new Renommee(this, p);
            Destin destinLu = new Destin(this, p);
            if (format >= 7) {
                renommeeLue.lire(entree);
                destinLu.lire(entree);
            }
            Entreprise entrepriseLue = new Entreprise(this, p);
            if (format >= 8) entrepriseLue.lire(entree, format);
            Bande bandeLue = new Bande(this, p);
            if (format >= 9) bandeLue.lire(entree, format);
            Brigands brigandsLus = new Brigands(this, p);
            if (format >= 10) brigandsLus.lire(entree, format);
            Corps corpsLu = new Corps(this, p);
            if (format >= 11) corpsLu.lire(entree);
            Commandes commandesLues = new Commandes(this, p);
            Affaires affairesLues = new Affaires(this, p);
            if (format >= 13) {
                commandesLues.lire(entree);
                affairesLues.lire(entree);
            }
            if (format >= 14) missionsLues.lireSuite(entree);
            Social socialLu = new Social(this, p);
            if (format >= 15) {
                renommeeLue.lireSuite(entree);
                socialLu.lire(entree);
            }
            Terre terreLue = new Terre(this, p);
            if (format >= 16) terreLue.lire(entree);
            Famille familleLue = new Famille(this, p);
            if (format >= 17) familleLue.lire(entree);
            Noblesse noblesseLue = new Noblesse(this, p);
            if (format >= 18) noblesseLue.lire(entree);
            Armee armeeLue = new Armee(this, p);
            if (format >= 19) armeeLue.lire(entree);
            Quotidien quotidienLu = new Quotidien(this, p);
            if (format >= 20) quotidienLu.lire(entree);
            Chansons chansonsLues = new Chansons(this, p);
            if (format >= 21) chansonsLues.lire(entree);
            List<Integer> livresLus = new ArrayList<>();
            if (format >= 22) {
                int nb = entree.readInt();
                for (int k = 0; k < nb; k++) livresLus.add(entree.readInt());
            }
            if (format >= 23) affairesLues.lireConstructions(entree);
            Gens gensLus = new Gens(this, p);
            if (format >= 24) gensLus.lire(entree);
            Notables notablesLus = new Notables(this, p);
            if (format >= 25) notablesLus.lire(entree);
            if (format >= 26 && entree.readBoolean()) {
                Aspect aspectLu = Aspect.lire(entree);
                if (lue != null) lue.aspect = aspectLu;
            }
            Foule f = p.foules[q][r];
            if (f == null) return;
            boolean manquants = false;
            for (Creation.Ajout a : liste) {
                if (f.placeDe(a.id) >= 0) continue;
                a.inserer(f);
                manquants = true;
            }
            if (manquants) unir(f, liste);
            if (!liste.isEmpty()) lieu = f.placeDe(numero);
            if (lieu < 0 || !f.estEnVie(lieu, numero)) return;
            peuple = q;
            region = r;
            place = lieu;
            id = numero;
            quartier = coin;
            numeroReserve = reserve;
            fiche = lue;
            ajouts = liste;
            carnet.addAll(lues);
            heuresVecues = vecues;
            marche = marcheLu;
            travail = travailLu;
            missions = missionsLues;
            renommee = renommeeLue;
            destin = destinLu;
            entreprise = entrepriseLue;
            bande = bandeLue;
            brigands = brigandsLus;
            corps = corpsLu;
            commandes = commandesLues;
            affaires = affairesLues;
            social = socialLu;
            terre = terreLue;
            famille = familleLue;
            noblesse = noblesseLue;
            armee = armeeLue;
            quotidien = quotidienLu;
            chansons = chansonsLues;
            gens = gensLus;
            notables = notablesLus;
            bibliotheque.clear();
            bibliotheque.addAll(livresLus);
            armee.appliquer();
            affaires.publier();
            maisonVue = corps.aUneMaison() ? new int[] { corps.maisonVille, corps.maisonBatiment } : null;
            publierBande();
            brigands.publier();
            rafraichir();
        } catch (IOException | RuntimeException e) {
            id = -1L;
            fiche = null;
            carnet.clear();
        }
    }
}
