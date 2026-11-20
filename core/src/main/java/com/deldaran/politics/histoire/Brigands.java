package com.deldaran.politics.histoire;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Phase 9 : les brigands, les batailles de bande, le butin, les prisonniers et le mercenariat.
 *
 * Autour des villes que l'on visite, des bandes de brigands établissent leur REPAIRE, à quelques centaines de mètres du bord.
 * La garde met leur tête à prix (mission CHASSE au tableau). L'écran montre le campement et ses brigands ; quand le joueur
 * approche, ils se lèvent et la rencontre s'ouvre : attaquer, les sommer de se rendre, payer le passage ou passer son chemin.
 * La bataille est résolue par Combat, puis rejouée à l'écran ; ensuite viennent le butin, les blessés, et le sort des prisonniers.
 * Une bande assez connue peut aussi signer un CONTRAT avec la garde : une paie chaque semaine, contre la chasse de tous les brigands
 * qui rôdent autour de la ville pendant la durée du contrat.
 */
public final class Brigands {
    public static final int ACTIF = 0, DETRUIT = 1;
    public static final int EN_VIE = 0, TUE = 1, PARTI = 2;
    /** Chance par jour qu'une bande s'installe près d'une ville sans repaire (en paix, en guerre, sous contrat). */
    public static final float CHANCE = 0.12f, CHANCE_GUERRE = 0.25f, CHANCE_CONTRAT = 0.5f;
    /** Jours sans nouveau repaire après qu'on en a détruit un (moins sous contrat). */
    public static final int ATTENTE = 20, ATTENTE_CONTRAT = 4;
    /** Effectif : EFFECTIF_MIN + hasard (EFFECTIF_HASARD + 3 par palier de renom du joueur), au plus EFFECTIF_MAX. Un de plus tous les CROISSANCE jours. */
    public static final int EFFECTIF_MIN = 3, EFFECTIF_HASARD = 4, EFFECTIF_MAX = 24, CROISSANCE = 10;
    /** Le campement est à BORD_MIN … BORD_MAX mètres au-delà du bord de la ville. */
    public static final float BORD_MIN = 110f, BORD_MAX = 200f;
    /** Prime de la garde par brigand (mission CHASSE), par prisonnier livré ; prix du passage par brigand ; part de la bourse qu'ils prennent au vaincu. */
    public static final int PRIME_PAR_TETE = 90, PRIME_PRISONNIER = 150, PASSAGE_PAR_TETE = 25;
    public static final float PILLAGE = 0.5f;
    /** Renom gagné dans la ville par victoire, plus par brigand vaincu. */
    public static final float RENOM_VICTOIRE = 8f, RENOM_PAR_TETE = 2f;
    /** Contrat de mercenaire : durée (jours), paie par jour et par tête (joueur compris), prime de fin. */
    public static final int CONTRAT_JOURS = 20, CONTRAT_PAIE = 10, CONTRAT_PRIME = 300;
    /** Sort des prisonniers. */
    public static final int LIVRER = 0, ENROLER = 1, RELACHER = 2, EXECUTER = 3;
    /** Embuscades sur les routes : une chance à chaque tirage (l'écran tire tous les 500 m parcourus loin des villes), plus en guerre, plus quand l'ennemi juré rôde. */
    public static final float EMBUSCADE = 0.08f, EMBUSCADE_GUERRE = 0.15f, EMBUSCADE_RIVAL = 0.1f;
    /** L'ennemi juré revient RETOUR_MIN … RETOUR_MIN + RETOUR_HASARD jours après sa fuite ; il prend la tête d'une nouvelle bande (RIVAL_CHANCE, moins pour une embuscade),
     *  avec RIVAL_FORCE de force en plus par niveau et autant de brigands en plus que son niveau. Le vaincre pour de bon rapporte RENOM_RIVAL. */
    public static final int RETOUR_MIN = 6, RETOUR_HASARD = 8;
    public static final float RIVAL_CHANCE = 0.6f, RIVAL_CHANCE_ROUTE = 0.35f, RIVAL_FORCE = 6f, RENOM_RIVAL = 12f;
    /** Part des brigands tués au combat qui ont de la famille dans la ville ; part des exécutés. */
    public static final float FAMILLE = 0.4f, FAMILLE_EXECUTE = 0.6f;
    private static final int DEUILS_MAX = 40;
    private static final String[] ARMES = { "BATON", "DAGUE", "LANCE", "HACHE_GUERRE", "ARC", "MASSE", "EPEE" };

    public static final class Repaire {
        public int id, ville, peuple, etat, cree, butin, passage = -1, arrivees, renfort;
        public long graine;
        public float angle, bord, force;
        public String chef = "";
        /** Par brigand : nom, femme, arme, armure, force, état (EN_VIE, TUE, PARTI). */
        public String[] noms;
        public boolean[] femmes;
        public int[] armes, armures, etats;
        public float[] forces;
        /** Une embuscade sur la route (pas de campement, pas de caisse) ; la graine du chef quand c'est l'ennemi juré (son corps reste le même). */
        public boolean route;
        public long graineChef;

        public int effectif() {
            int n = 0;
            if (etats != null) for (int e : etats) if (e == EN_VIE) n++;
            return n;
        }

        public long graineDe(int k) {
            if (k == 0 && graineChef != 0L) return graineChef;
            return graine * 1000003L + k * 7L + 1L;
        }

        Repaire copie() {
            Repaire r = new Repaire();
            r.id = id;
            r.ville = ville;
            r.peuple = peuple;
            r.etat = etat;
            r.cree = cree;
            r.butin = butin;
            r.passage = passage;
            r.arrivees = arrivees;
            r.renfort = renfort;
            r.graine = graine;
            r.angle = angle;
            r.bord = bord;
            r.force = force;
            r.chef = chef;
            r.noms = noms.clone();
            r.femmes = femmes.clone();
            r.armes = armes.clone();
            r.armures = armures.clone();
            r.etats = etats.clone();
            r.forces = forces.clone();
            r.route = route;
            r.graineChef = graineChef;
            return r;
        }
    }

    private final Joueur j;
    private final Population p;
    public final List<Repaire> repaires = new ArrayList<>();
    private final Map<Integer, Integer> prochain = new HashMap<>();
    /** Prisonniers ligotés que la garde de chaque ville doit encore ramener (et payer). */
    private final Map<Integer, Integer> aLivrer = new HashMap<>();
    private int suivant = 1;
    private int dernierJour = -1;
    /** Contrat en cours : ville, prochain jour de paie. */
    public int contratVille = -1, contratPaie = -1;
    /** La dernière bataille, en attente de son dénouement (l'écran la rejoue d'abord). */
    private Combat.Resultat derniere;
    private Repaire derniereRepaire;
    private final List<Combat.Combattant> prisonniers = new ArrayList<>();
    /** Pour les essais. */
    public int batailles, victoires;
    public boolean forcerEmbuscade, forcerRival;

    /** Un brigand tué : sa famille vit dans la ville ; l'écran choisit qui, parmi les habitants, s'en souvient. */
    public static final class Deuil {
        public int ville;
        public long graine;
        public String nom;
        public boolean execute;
    }

    public final List<Deuil> deuils = new ArrayList<>();
    /** L'ennemi juré : un chef de brigands qui t'a échappé (null s'il n'y en a pas). Il mène peut-être une bande (rivalRepaire). */
    public String rivalNom;
    public boolean rivalFemme;
    public long rivalGraine;
    public int rivalPeuple, rivalArme, rivalArmure, rivalNiveau, rivalVictoires, rivalRetour, rivalRepaire = -1;
    public float rivalForce;
    private int derniereVille = -1, embuscades;

    Brigands(Joueur j, Population p) {
        this.j = j;
        this.p = p;
    }

    private Fiche f() {
        return j.fiche();
    }

    public Repaire repaire(int id) {
        for (Repaire r : repaires) if (r.id == id) return r;
        return null;
    }

    public boolean detruit(int id) {
        Repaire r = repaire(id);
        return r == null || r.etat == DETRUIT;
    }

    private Repaire actif(int ville) {
        for (Repaire r : repaires) if (r.ville == ville && r.etat == ACTIF && !r.route) return r;
        return null;
    }

    void publier() {
        List<Repaire> l = new ArrayList<>();
        for (Repaire r : repaires) if (r.etat == ACTIF) l.add(r.copie());
        j.repairesVue = l.toArray(new Repaire[0]);
        j.deuilsVue = deuils.toArray(new Deuil[0]);
    }

    static String direction(float angle) {
        String[] d = { "à l'est", "au nord-est", "au nord", "au nord-ouest", "à l'ouest", "au sud-ouest", "au sud", "au sud-est" };
        return d[(int) Math.floorMod(Math.round(angle / (Math.PI / 4.0)), 8)];
    }

    private boolean enGuerre(int v) {
        int region = p.villes.region[v];
        int etat = region >= 0 && region < p.etats.regionEtat.length ? p.etats.regionEtat[region] : -1;
        return etat >= 0 && p.etats.ennemi[etat] >= 0;
    }

    // ---------------------------------------------------------------- Naissance des repaires

    /** Chaque jour (au plus une fois) pour la ville où l'on est : une bande s'installe peut-être ; les bandes grossissent. */
    void actualiser(int v) {
        Fiche f = f();
        if (f == null || v < 0) return;
        derniereVille = v;
        avancer();
        int jour = j.jourMaintenant();
        Repaire r = actif(v);
        if (r != null) {
            // La bande grossit : un brigand de plus (les places en réserve du tableau) tous les CROISSANCE jours.
            boolean change = false;
            while (jour >= r.renfort && r.arrivees < r.etats.length) {
                r.etats[r.arrivees++] = EN_VIE;
                r.renfort += CROISSANCE;
                change = true;
            }
            if (change) publier();
            return;
        }
        Integer attente = prochain.get(v);
        if (attente != null && jour < attente) return;
        Random h = new Random(Travail.melanger(p.graine * 131L + v * 7919L + jour * 104729L));
        float chance = contratVille == v ? CHANCE_CONTRAT : enGuerre(v) ? CHANCE_GUERRE : CHANCE;
        prochain.put(v, jour + 1);
        if (h.nextFloat() >= chance) return;
        creer(v, jour, h);
    }

    Repaire creer(int v, int jour, Random h) {
        Repaire r = nouveau(v, jour, h, false);
        // La garde met leur tête à prix.
        Missions.Mission m = new Missions.Mission();
        m.type = Missions.CHASSE;
        m.ville = v;
        m.lieu = Biens.MARCHE;
        m.dest = r.id;
        m.quantite = 1;
        m.prime = r.effectif() * PRIME_PAR_TETE + Math.round(r.force * 2f);
        m.renom = 3;
        m.creee = jour;
        m.limite = jour + 25;
        m.donneur = "le capitaine de la garde";
        j.missions().liste.add(m);
        if (r.graineChef != 0L) {
            j.noter("En l'an " + p.annee + ", " + r.chef + ", ton ennemi juré, reparaît à la tête d'une bande " + direction(r.angle) + " " + Creation.de(nomVille(v)) + ".");
            j.annoncer("On dit que " + r.chef + " est revenu" + (rivalFemme ? "e" : "") + " avec une nouvelle bande, " + direction(r.angle) + " de la ville.");
        } else j.noter("En l'an " + p.annee + ", une bande de brigands menée par " + r.chef + " s'installe " + direction(r.angle) + " " + Creation.de(nomVille(v)) + ".");
        publier();
        return r;
    }

    private boolean rivalLibre(int jour) {
        return rivalNom != null && rivalRepaire < 0 && jour >= rivalRetour;
    }

    /** Le nom de l'ennemi juré et son titre (le Revenant, puis l'Increvable à partir du niveau 3). */
    public String rivalComplet() {
        if (rivalNom == null) return "";
        return rivalNom + " " + (rivalNiveau >= 3 ? "l'Increvable" : rivalFemme ? "la Revenante" : "le Revenant");
    }

    /** Une bande : en campement près de la ville, ou en embuscade sur la route (plus petite, sans renfort). L'ennemi juré la mène peut-être. */
    private Repaire nouveau(int v, int jour, Random h, boolean route) {
        Repaire r = new Repaire();
        r.id = suivant++;
        r.ville = v;
        r.peuple = p.villes.peuple[v];
        r.graine = h.nextLong();
        r.cree = jour;
        r.route = route;
        r.angle = (float) (h.nextFloat() * Math.PI * 2.0);
        r.bord = BORD_MIN + (BORD_MAX - BORD_MIN) * h.nextFloat();
        int palier = Renommee.palier(j.renommee().renom(v));
        boolean rival = rivalLibre(jour) && (forcerRival || h.nextFloat() < (route ? RIVAL_CHANCE_ROUTE : RIVAL_CHANCE));
        int n = route ? Math.min(EFFECTIF_MAX, 2 + h.nextInt(3 + 2 * palier)) : Math.min(EFFECTIF_MAX, EFFECTIF_MIN + h.nextInt(EFFECTIF_HASARD + 3 * palier));
        if (rival) {
            n = Math.min(EFFECTIF_MAX, n + rivalNiveau);
            r.peuple = rivalPeuple;
        }
        r.force = 15f + 25f * h.nextFloat() + 5f * palier;
        r.butin = route ? 20 + n * (5 + h.nextInt(20)) : 80 + n * (20 + h.nextInt(60));
        int total = route ? n : Math.min(EFFECTIF_MAX, n + 4);
        r.noms = new String[total];
        r.femmes = new boolean[total];
        r.armes = new int[total];
        r.armures = new int[total];
        r.etats = new int[total];
        r.forces = new float[total];
        for (int k = 0; k < total; k++) {
            Random b = new Random(r.graineDe(k));
            r.femmes[k] = b.nextFloat() < 0.2f;
            r.noms[k] = Noms.personne(r.peuple, p.graine, r.graineDe(k), r.femmes[k] ? 1 : 0);
            int a = Math.min(ARMES.length - 1, (int) (b.nextFloat() * b.nextFloat() * ARMES.length * (k == 0 ? 1.6f : 1f)));
            r.armes[k] = arme(ARMES[a]);
            float t = b.nextFloat();
            r.armures[k] = t < 0.5f ? -1 : t < 0.85f ? Biens.GAMBISON.id : Biens.CUIR_BOUILLI.id;
            r.forces[k] = r.force + (b.nextFloat() - 0.5f) * 16f + (k == 0 ? 12f : 0f);
            r.etats[k] = k < n ? EN_VIE : PARTI;
        }
        if (rival) {
            r.graineChef = rivalGraine;
            r.noms[0] = rivalNom;
            r.femmes[0] = rivalFemme;
            r.armes[0] = rivalArme;
            r.armures[0] = rivalArmure;
            r.forces[0] = rivalForce + RIVAL_FORCE * rivalNiveau;
            rivalRepaire = r.id;
        }
        r.chef = rival ? rivalComplet() : r.noms[0];
        r.arrivees = n;
        r.renfort = route ? Integer.MAX_VALUE : jour + CROISSANCE;
        repaires.add(r);
        return r;
    }

    /** L'écran : le joueur a parcouru un bout de chemin loin des villes. Peut-être une embuscade (v : la ville la plus proche, ou -1). */
    public void embuscade(int v) {
        Fiche f = f();
        if (f == null) return;
        if (v < 0) v = derniereVille >= 0 ? derniereVille : f.ville;
        if (v < 0 || v >= p.villes.nombre) return;
        for (Repaire r : repaires) if (r.route && r.etat == ACTIF) return;
        int jour = j.jourMaintenant();
        Random h = new Random(Travail.melanger(p.graine * 977L + jour * 31L + embuscades * 7919L));
        embuscades++;
        float chance = (enGuerre(v) ? EMBUSCADE_GUERRE : EMBUSCADE) + (rivalLibre(jour) ? EMBUSCADE_RIVAL : 0f)
                + (j.missions().quetes.escorte() ? Quetes.ESCORTE_EMBUSCADE : 0f);
        chance *= j.renommee().facteurBrigands() * j.noblesse().facteurBrigands(v);
        if (h.nextFloat() >= chance && !forcerEmbuscade) return;
        forcerEmbuscade = false;
        Repaire r = nouveau(v, jour, h, true);
        j.noter("En l'an " + p.annee + ", sur la route, des brigands menés par " + r.chef + " te tendent une embuscade.");
        publier();
    }

    /** Une embuscade finie (payée, fuie, gagnée ou perdue) : les brigands se dispersent. */
    private void disperser(Repaire r) {
        if (!r.route || r.etat != ACTIF) return;
        r.etat = DETRUIT;
        for (int k = 0; k < r.etats.length; k++) if (r.etats[k] == EN_VIE) r.etats[k] = PARTI;
        if (rivalRepaire == r.id) {
            rivalRepaire = -1;
            rivalRetour = j.jourMaintenant() + RETOUR_MIN;
        }
    }

    /** Le chef d'une bande défaite s'enfuit : il devient l'ennemi juré (ou, s'il l'était déjà, il revient plus fort). */
    private String fuiteChef(Repaire r) {
        int jour = j.jourMaintenant();
        Random h = new Random(Travail.melanger(r.graine * 13L + jour));
        int retour = jour + RETOUR_MIN + h.nextInt(RETOUR_HASARD + 1);
        if (r.graineChef != 0L && rivalRepaire == r.id) {
            rivalNiveau++;
            rivalForce += 2f;
            rivalRepaire = -1;
            rivalRetour = retour;
            j.noter(rivalComplet() + " t'échappe encore.");
            return rivalComplet() + " s'enfuit encore, en hurlant qu'" + (rivalFemme ? "elle" : "il") + " reviendra plus fort" + (rivalFemme ? "e." : ".");
        }
        if (rivalNom != null) return r.chef + " s'enfuit dans les bois.";
        rivalNom = r.noms[0];
        rivalFemme = r.femmes[0];
        rivalGraine = r.graineDe(0);
        rivalPeuple = r.peuple;
        rivalArme = r.armes[0];
        rivalArmure = r.armures[0];
        rivalForce = r.forces[0];
        rivalNiveau = 1;
        rivalVictoires = 0;
        rivalRetour = retour;
        rivalRepaire = -1;
        j.noter("En l'an " + p.annee + ", " + rivalNom + ", chef de brigands, t'échappe et jure ta perte. On l'appelle désormais " + rivalComplet() + ".");
        return rivalNom + " s'enfuit en jurant de se venger. On l'appellera désormais " + rivalComplet() + ".";
    }

    /** L'ennemi juré est vaincu pour de bon. */
    private String finRival(String texte, int ville) {
        j.renommee().gagner(ville, RENOM_RIVAL, 2f, "Ennemi juré vaincu");
        j.noter("En l'an " + p.annee + ", " + texte);
        rivalNom = null;
        rivalRepaire = -1;
        rivalNiveau = 0;
        return texte;
    }

    /** Un brigand tué ou exécuté : peut-être a-t-il de la famille dans la ville (toujours la même réponse pour le même brigand). */
    private void deuil(Repaire r, Combat.Combattant c, boolean execute) {
        if (r.graineChef != 0L && c.brigand == 0) return;
        float u = (Travail.melanger(c.graine * 31L + 5L) >>> 40) / (float) (1L << 24);
        if (u >= (execute ? FAMILLE_EXECUTE : FAMILLE)) return;
        Deuil d = new Deuil();
        d.ville = r.ville;
        d.graine = c.graine;
        d.nom = c.nom;
        d.execute = execute;
        deuils.add(d);
        while (deuils.size() > DEUILS_MAX) deuils.remove(0);
    }

    private static int arme(String nom) {
        switch (nom) {
            case "BATON": return Biens.BATON.id;
            case "DAGUE": return Biens.DAGUE.id;
            case "LANCE": return Biens.LANCE.id;
            case "HACHE_GUERRE": return Biens.HACHE_GUERRE.id;
            case "ARC": return Biens.ARC.id;
            case "MASSE": return Biens.MASSE.id;
            default: return Biens.EPEE.id;
        }
    }

    String nomVille(int v) {
        return Noms.ville(p.villes.peuple[v], p.graine, v);
    }

    // ---------------------------------------------------------------- Le temps

    /** Les jours passent : paie du contrat. */
    void avancer() {
        int jour = j.jourMaintenant();
        if (dernierJour < 0) dernierJour = jour;
        Fiche f = f();
        if (f != null && contratVille >= 0 && contratPaie >= 0) {
            Missions.Mission m = contrat();
            if (m == null) {
                contratVille = -1;
                contratPaie = -1;
            } else while (jour >= contratPaie && contratPaie <= m.limite) {
                int paie = 7 * m.prixUnite;
                f.argent += paie;
                j.marche().ville(m.ville, j.jour()).bourse -= paie;
                j.annoncer("La garde " + Creation.de(nomVille(m.ville)) + " te verse la paie de la semaine : " + Fiche.argentTexte(paie) + ".");
                contratPaie += 7;
            }
        }
        dernierJour = jour;
    }

    private Missions.Mission contrat() {
        for (Missions.Mission m : j.missions().liste) if (m.type == Missions.MERCENAIRE && m.etat == Missions.ACCEPTEE) return m;
        return null;
    }

    /** Une offre de contrat au tableau : il faut une bande, et être connu dans la ville. */
    void offrirContrat(int v, List<Missions.Mission> liste, int jour) {
        if (j.bande().taille() < 2 || Renommee.palier(j.renommee().renom(v)) < 1 || j.renommee().reputation(v) < 0f) return;
        for (Missions.Mission m : liste) if (m.type == Missions.MERCENAIRE && (m.etat == Missions.OFFERTE || m.etat == Missions.ACCEPTEE)) return;
        Missions.Mission m = new Missions.Mission();
        m.type = Missions.MERCENAIRE;
        m.ville = v;
        m.lieu = Biens.MARCHE;
        m.quantite = CONTRAT_JOURS;
        m.prixUnite = CONTRAT_PAIE * (j.bande().taille() + 1);
        m.prime = CONTRAT_PRIME + 50 * j.bande().taille();
        m.renom = 4;
        m.creee = jour;
        m.limite = jour + 5;
        m.donneur = "le capitaine de la garde";
        liste.add(m);
    }

    void commencerContrat(Missions.Mission m) {
        int jour = j.jourMaintenant();
        m.limite = jour + m.quantite;
        contratVille = m.ville;
        contratPaie = jour + 7;
        prochain.put(m.ville, jour);
    }

    /** Fin du contrat : réussi s'il ne reste aucun brigand autour de la ville. */
    boolean finirContrat(Missions.Mission m) {
        contratVille = -1;
        contratPaie = -1;
        return actif(m.ville) == null;
    }

    /** En entrant dans une ville : la garde a ramené les prisonniers ligotés, et paie. */
    void visiter(int v) {
        Integer n = aLivrer.remove(v);
        Fiche f = f();
        if (n == null || n <= 0 || f == null) return;
        int prime = n * PRIME_PRISONNIER;
        f.argent += prime;
        j.marche().ville(v, j.jour()).bourse -= prime;
        j.renommee().gagner(v, n, 2f, null);
        j.annoncer("La garde a ramené tes " + n + " prisonnier" + (n > 1 ? "s" : "") + " : " + Fiche.argentTexte(prime) + " de prime.");
        j.enregistrer();
    }

    // ---------------------------------------------------------------- La rencontre

    private float forceJoueur() {
        Fiche f = f();
        int[] eq = Combat.equipement(f.sac.portes());
        return Combat.forceJoueur(f, eq[0]);
    }

    private float forceNous() {
        float s = forceJoueur();
        for (Combat.Combattant c : j.bande().combattants()) s += c.force * c.sante;
        return s;
    }

    private static float forceEux(Repaire r) {
        float s = 0f;
        for (int k = 0; k < r.etats.length; k++) if (r.etats[k] == EN_VIE) s += Combat.force(r.forces[k], r.armes[k]);
        return s;
    }

    private int passage(Repaire r) {
        return PASSAGE_PAR_TETE * r.effectif();
    }

    /** L'écran : le joueur arrive au campement. */
    public Joueur.Scene rencontre(int id) {
        Repaire r = repaire(id);
        Fiche f = f();
        if (r == null || r.etat != ACTIF || f == null) return j.fermerScene();
        int n = r.effectif();
        float nous = forceNous(), eux = forceEux(r);
        boolean ennemi = r.graineChef != 0L && r.etats[0] == EN_VIE;
        boolean racket = ennemi || eux > nous * (r.route ? 0.85f : 1.3f);
        StringBuilder t = new StringBuilder();
        if (r.route) t.append("Des brigands surgissent des fourrés et te barrent la route : ").append(n).append(n > 1 ? " brigands armés, menés par " : " brigand armé, mené par ").append(r.chef).append('.');
        else t.append("Le campement de ").append(r.chef).append(", chef de brigands. ").append(n).append(n > 1 ? " brigands armés se lèvent" : " brigand armé se lève").append(" dans la clairière.");
        if (ennemi) t.append("\n« Toi ! » ").append(r.chef).append(rivalVictoires > 0 ? " ricane : « Tu reviens mordre la poussière ? »" : " t'a reconnu" + f.e() + ". « Je t'attendais. Cette fois, c'est toi qui vas fuir. »")
                .append(" Pas question de payer : c'est ta peau qu'").append(rivalFemme ? "elle" : "il").append(" veut.");
        else if (racket) t.append("\n« La bourse ou la vie ! » ").append(r.chef).append(" exige ").append(Fiche.argentTexte(passage(r))).append(" pour te laisser passer.");
        else if (r.route) t.append("\nIls hésitent en voyant ta troupe, et s'écartent à demi.");
        else t.append("\nIls te jaugent, la main sur leurs armes.");
        t.append("\n").append(jauge(nous, eux));
        int taille = j.bande().taille();
        Joueur.Scene s = new Joueur.Scene("Brigands", "");
        s.ajouter(taille > 0 ? "« À l'attaque ! »" : "Attaquer", () -> attaquer(r, 0f));
        if (taille > 0 || Renommee.palier(j.renommee().meilleurRenom()) >= 2) s.ajouter("« Rendez-vous, et vous aurez la vie sauve ! »", () -> sommer(r));
        if (f.argent >= passage(r) && !ennemi) s.ajouter("Payer le passage (" + Fiche.argentTexte(passage(r)) + ")", () -> payer(r));
        s.ajouter(racket ? "Tenter de fuir" : "Passer ton chemin", () -> partir(r, racket));
        Joueur.Scene vraie = new Joueur.Scene("Brigands", t.toString());
        vraie.choix.addAll(s.choix);
        vraie.suites.addAll(s.suites);
        return vraie;
    }

    private static String jauge(float nous, float eux) {
        float rapport = nous / Math.max(1f, eux);
        if (rapport > 2f) return "Ils sont bien moins forts que vous.";
        if (rapport > 1.25f) return "Vous avez l'avantage.";
        if (rapport > 0.8f) return "Le combat serait serré.";
        if (rapport > 0.5f) return "Ils sont plus forts que vous.";
        return "Ils sont bien trop forts pour vous.";
    }

    private Joueur.Scene payer(Repaire r) {
        Fiche f = f();
        int prix = passage(r);
        f.argent -= prix;
        r.butin += prix;
        r.passage = j.jourMaintenant();
        disperser(r);
        j.gesteJoueur = "donner";
        j.noter("Tu paies " + Fiche.argentTexte(prix) + " aux brigands de " + r.chef + " pour passer.");
        j.enregistrer();
        publier();
        Joueur.Scene s = new Joueur.Scene("Brigands", r.chef + " empoche l'argent avec un sourire mauvais. « Bon voyage. »");
        s.ajouter("Partir", j::fermerScene);
        return s;
    }

    private Joueur.Scene partir(Repaire r, boolean racket) {
        if (!racket) {
            disperser(r);
            publier();
            return j.fermerScene();
        }
        Fiche f = f();
        Random h = new Random(Travail.melanger(r.graine + j.jourMaintenant()));
        float chance = 0.3f + f.competences[Fiche.ATHLETISME] / 300f + f.competences[Fiche.DISCRETION] / 400f - 0.03f * j.bande().taille();
        if (h.nextFloat() < chance) {
            r.passage = j.jourMaintenant();
            disperser(r);
            publier();
            f.apprendre(Fiche.ATHLETISME, 2f);
            Joueur.Scene s = new Joueur.Scene("Brigands", "Tu détales. Ils te poursuivent un moment, puis renoncent en jurant.");
            s.ajouter("Partir", j::fermerScene);
            return s;
        }
        Joueur.Scene s = new Joueur.Scene("Brigands", "Ils te coupent la route. Il faut se battre !");
        s.ajouter("Se battre", () -> attaquer(r, 10f));
        return s;
    }

    private Joueur.Scene sommer(Repaire r) {
        Fiche f = f();
        float rapport = forceNous() / Math.max(1f, forceEux(r));
        Random h = new Random(Travail.melanger(r.graine * 3L + j.jourMaintenant()));
        float score = 40f * rapport + f.competences[Fiche.COMMANDEMENT] / 3f + 8f * Renommee.palier(j.renommee().renom(r.ville)) + (h.nextFloat() - 0.5f) * 20f
                - (r.graineChef != 0L && r.etats[0] == EN_VIE ? 25f : 0f);
        f.apprendre(Fiche.COMMANDEMENT, 2f);
        if (score < 75f) {
            Joueur.Scene s = new Joueur.Scene("Brigands", r.chef + " éclate de rire. « Viens nous prendre ! » Ils chargent.");
            s.ajouter("Se battre", () -> attaquer(r, 5f));
            return s;
        }
        // Ils se rendent : tous prisonniers, sans un coup.
        Combat.Resultat res = new Combat.Resultat();
        res.repaire = r.id;
        res.reddition = true;
        res.vainqueur = Combat.NOUS;
        res.tous.add(joueurCombattant());
        res.tous.addAll(j.bande().combattants());
        for (Combat.Combattant c : brigands(r)) {
            c.etat = Combat.A_TERRE;
            res.tous.add(c);
        }
        derniere = res;
        derniereRepaire = r;
        j.bataille = res;
        return j.fermerScene();
    }

    private Combat.Combattant joueurCombattant() {
        Fiche f = f();
        int[] eq = Combat.equipement(f.sac.portes());
        Combat.Combattant moi = new Combat.Combattant();
        moi.nom = f.nom;
        moi.joueur = true;
        moi.camp = Combat.NOUS;
        moi.graine = -1L;
        moi.arme = eq[0];
        moi.armure = eq[1];
        moi.force = Combat.forceJoueur(f, eq[0]);
        return moi;
    }

    private List<Combat.Combattant> brigands(Repaire r) {
        List<Combat.Combattant> l = new ArrayList<>();
        for (int k = 0; k < r.etats.length; k++) {
            if (r.etats[k] != EN_VIE) continue;
            Combat.Combattant c = new Combat.Combattant();
            c.nom = r.noms[k];
            c.camp = Combat.EUX;
            c.graine = r.graineDe(k);
            c.femme = r.femmes[k];
            c.peuple = r.peuple;
            c.arme = r.armes[k];
            c.armure = r.armures[k];
            c.force = Combat.force(r.forces[k], r.armes[k]);
            c.brigand = k;
            l.add(c);
        }
        return l;
    }

    private Joueur.Scene attaquer(Repaire r, float bonusEux) {
        Fiche f = f();
        List<Combat.Combattant> tous = new ArrayList<>();
        tous.add(joueurCombattant());
        tous.addAll(j.bande().combattants());
        tous.addAll(brigands(r));
        float moralNous = 55f + 5f * Renommee.palier(j.renommee().renom(r.ville));
        if (j.bande().taille() > 0) {
            float m = 0f;
            for (Bande.Compagnon c : j.bande().membres) m += c.moral;
            moralNous = Math.max(moralNous, m / j.bande().taille());
        }
        Combat.Resultat res = Combat.resoudre(tous, f.competences[Fiche.TACTIQUE], 15f + r.force / 3f, moralNous, 50f + bonusEux,
                Travail.melanger(r.graine * 17L + j.jourMaintenant() * 31L + batailles));
        res.repaire = r.id;
        derniere = res;
        derniereRepaire = r;
        batailles++;
        j.bataille = res;
        return j.fermerScene();
    }

    // ---------------------------------------------------------------- Après la bataille

    /** Appelé par l'écran quand il a fini de rejouer la bataille. */
    public Joueur.Scene apresBataille() {
        Combat.Resultat res = derniere;
        Repaire r = derniereRepaire;
        derniere = null;
        j.bataille = null;
        Fiche f = f();
        if (res == null || r == null || f == null) return j.fermerScene();
        int tues = 0, aTerre = 0, enFuite = 0;
        prisonniers.clear();
        Combat.Combattant chef = null;
        boolean ennemi = r.graineChef != 0L && rivalRepaire == r.id;
        for (Combat.Combattant c : res.tous) {
            if (c.camp != Combat.EUX) continue;
            if (c.brigand == 0) chef = c;
            if (c.etat == Combat.MORT) {
                tues++;
                deuil(r, c, false);
                r.etats[c.brigand] = TUE;
            } else if (c.etat == Combat.A_TERRE) {
                aTerre++;
                r.etats[c.brigand] = TUE;
                prisonniers.add(c);
            } else if (c.etat == Combat.FUI) enFuite++;
        }
        String pertes = j.bande().apres(res);
        boolean victoire = res.vainqueur == Combat.NOUS;
        StringBuilder t = new StringBuilder();
        // Le joueur lui-même : un peu d'expérience dans son arme, et des coups reçus.
        Combat.Combattant moi = res.tous.get(0);
        int[] eq = Combat.equipement(f.sac.portes());
        Biens.Bien arme = eq[0] >= 0 ? Biens.get(eq[0]) : null;
        if (!res.reddition) {
            f.apprendre(arme != null && arme.competence >= 0 ? arme.competence : Fiche.ATHLETISME, 4f + tues + aTerre);
            f.apprendre(Fiche.TACTIQUE, 1f + j.bande().taille() * 0.3f);
        }
        j.santeJoueur = moi.joueur ? Math.max(0.05f, moi.sante) : 1f;
        if (moi.joueur && !victoire && moi.etat == Combat.A_TERRE && moi.sante <= 0.2f) j.lignee().blessureMortelle("tombé" + f.e() + " sous les coups des brigands");
        if (victoire) {
            victoires++;
            j.noblesse().victoire(r.ville);
            int restants = r.effectif();
            if (restants == 0 || enFuite < 2) {
                r.etat = DETRUIT;
                prochain.put(r.ville, j.jourMaintenant() + (contratVille == r.ville ? ATTENTE_CONTRAT : ATTENTE));
                for (int k = 0; k < r.etats.length; k++) if (r.etats[k] == EN_VIE) r.etats[k] = PARTI;
            }
            // Le butin : leur caisse et une partie de leurs armes.
            int butin = r.butin;
            r.butin = 0;
            int[] reste = { butin };
            String partage = j.bande().partager(butin, reste);
            f.argent += reste[0];
            Random h = new Random(Travail.melanger(r.graine + 99L * batailles));
            List<String> objets = new ArrayList<>();
            for (Combat.Combattant c : res.tous) {
                if (c.camp != Combat.EUX || c.etat == Combat.FUI || c.etat == Combat.DEBOUT) continue;
                if (c.arme >= 0 && h.nextFloat() < 0.5f) {
                    f.sac.ajouter(c.arme, 1);
                    objets.add(Biens.get(c.arme).nom);
                }
                if (c.armure >= 0 && h.nextFloat() < 0.3f) {
                    f.sac.ajouter(c.armure, 1);
                    objets.add(Biens.get(c.armure).nom);
                }
            }
            float renom = RENOM_VICTOIRE + RENOM_PAR_TETE * (tues + aTerre);
            j.renommee().gagner(r.ville, res.reddition ? renom * 0.7f : renom, 3f, "Brigands vaincus");
            j.renommee().marquer(Renommee.BRIGANDS);
            if (res.reddition) t.append("Les brigands de ").append(r.chef).append(" jettent leurs armes et se rendent sans combattre. ");
            else t.append("Victoire ! Les brigands de ").append(r.chef).append(" sont vaincus : ")
                    .append(tues).append(tues > 1 ? " morts, " : " mort, ").append(aTerre).append(" à terre").append(enFuite > 0 ? ", " + enFuite + " en fuite. " : ". ");
            t.append("\nDans leur campement : ").append(Fiche.argentTexte(butin)).append(objets.isEmpty() ? "." : ", et " + String.join(", ", objets) + ".");
            if (!partage.isEmpty()) t.append("\n").append(partage);
            if (r.etat == ACTIF && !r.route) t.append("\nLes fuyards se regroupent plus loin : la bande n'est pas finie.");
            if (chef != null && !res.reddition) {
                if (chef.etat == Combat.MORT && ennemi) t.append("\n").append(finRival(r.chef + ", ton ennemi juré, est tombé" + (rivalFemme ? "e" : "") + " sous tes coups. On ne le reverra plus.", r.ville));
                else if (chef.etat == Combat.FUI && (r.etat == DETRUIT || r.route)) t.append("\n").append(fuiteChef(r));
            }
            j.noter("En l'an " + p.annee + ", tu " + (res.reddition ? "obtiens la reddition des" : "bats les") + " brigands de " + r.chef + ", près " + Creation.de(nomVille(r.ville)) + ".");
        } else {
            boolean assomme = moi.etat == Combat.A_TERRE;
            int pris = assomme ? Math.round(f.argent * PILLAGE) : 0;
            f.argent -= pris;
            r.butin += pris;
            for (Combat.Combattant c : res.tous) if (c.camp == Combat.EUX && c.etat == Combat.FUI) r.etats[c.brigand] = EN_VIE;
            if (assomme) {
                t.append("Tout devient noir. Tu reprends conscience bien plus tard, la tête en sang. Ils t'ont pris ").append(Fiche.argentTexte(pris)).append('.');
                j.passer(3.0);
            } else t.append("Vous rompez le combat et fuyez. Les brigands ne vous poursuivent pas loin.");
            prisonniers.clear();
            j.noter("En l'an " + p.annee + ", les brigands de " + r.chef + " te battent, près " + Creation.de(nomVille(r.ville)) + ".");
            if (ennemi) {
                rivalVictoires++;
                t.append("\n").append(r.chef).append(" crache par terre : « La prochaine fois, je ne te laisserai pas la vie. »");
            }
        }
        disperser(r);
        if (!pertes.isEmpty()) t.append("\n").append(pertes);
        if (moi.sante < 0.5f && victoire) t.append("\nTu es blessé" + (f.femme() ? "e" : "") + ".");
        publier();
        j.missions().majCible();
        j.rafraichirSac();
        j.enregistrer();
        Joueur.Scene s = new Joueur.Scene(victoire ? "Victoire" : "Défaite", "");
        if (!prisonniers.isEmpty()) {
            int n = prisonniers.size();
            t.append("\n").append(n).append(n > 1 ? " brigands sont à ta merci. Que fais-tu d'eux ?" : " brigand est à ta merci. Que fais-tu de lui ?");
            s.ajouter("Les ligoter pour la garde (" + Fiche.argentTexte(PRIME_PRISONNIER) + " par tête)", () -> sort(r, LIVRER));
            if (j.bande().taille() < j.bande().tailleMax()) s.ajouter("Les enrôler de force dans ta bande", () -> sort(r, ENROLER));
            s.ajouter("Les relâcher", () -> sort(r, RELACHER));
            s.ajouter("Les exécuter", () -> sort(r, EXECUTER));
        } else s.ajouter("Continuer", j::fermerScene);
        Joueur.Scene vraie = new Joueur.Scene(s.titre, t.toString());
        vraie.choix.addAll(s.choix);
        vraie.suites.addAll(s.suites);
        return vraie;
    }

    private Joueur.Scene sort(Repaire r, int choix) {
        Fiche f = f();
        int n = prisonniers.size();
        boolean ennemi = false;
        for (Combat.Combattant c : prisonniers) ennemi |= c.brigand == 0 && r.graineChef != 0L && rivalNom != null && rivalGraine == r.graineChef;
        String texte;
        switch (choix) {
            case LIVRER:
                aLivrer.merge(r.ville, n, Integer::sum);
                f.pencher(Fiche.LOI, 3);
                texte = "Tu les ligotes solidement. La garde " + Creation.de(nomVille(r.ville)) + " viendra les chercher : la prime t'attend en ville.";
                j.noter("Tu livres " + n + " brigand" + (n > 1 ? "s" : "") + " à la garde.");
                break;
            case ENROLER: {
                int pris = 0;
                for (Combat.Combattant c : prisonniers) if (j.bande().enroler(c, r.ville)) pris++;
                f.pencher(Fiche.BIEN, -2);
                texte = pris + " d'entre eux rejoignent ta bande, la corde au cou. Ils te suivront par peur, sans solde : surveille leur moral.";
                if (pris < n) texte += " Les autres, faute de place, sont relâchés.";
                j.noter("Tu enrôles " + pris + " brigand" + (pris > 1 ? "s" : "") + " de force.");
                break;
            }
            case RELACHER:
                f.pencher(Fiche.BIEN, 4);
                j.renommee().gagner(r.ville, 0f, 1f, null);
                texte = "Tu les laisses partir. Ils s'enfuient sans demander leur reste.";
                break;
            default:
                f.pencher(Fiche.BIEN, -8);
                j.renommee().gagner(r.ville, 2f * n, -3f, null);
                for (Bande.Compagnon c : j.bande().membres) if (c.motif == Bande.CAUSE || c.motif == Bande.AMITIE) c.moral -= 6f;
                for (Combat.Combattant c : prisonniers) deuil(r, c, true);
                texte = "Tu les fais exécuter. Ta bande détourne les yeux ; on en parlera en ville.";
                j.noter("Tu fais exécuter " + n + " brigand" + (n > 1 ? "s" : "") + ".");
        }
        if (ennemi) {
            String nom = rivalComplet();
            if (choix == RELACHER) {
                rivalNiveau++;
                rivalRepaire = -1;
                rivalRetour = j.jourMaintenant() + RETOUR_MIN;
                texte += "\n" + nom + " s'éloigne sans un merci. Tu le reverras.";
            } else texte += "\n" + finRival(choix == LIVRER ? nom + ", ton ennemi juré, finira au gibet de la garde."
                    : choix == ENROLER ? nom + ", ton ennemi juré, te sert désormais, la rage au ventre."
                    : nom + ", ton ennemi juré, n'est plus.", r.ville);
        }
        j.sortPrisonniers = choix;
        prisonniers.clear();
        j.publierBande();
        j.enregistrer();
        Joueur.Scene s = new Joueur.Scene("Les prisonniers", texte);
        s.ajouter("Continuer", j::fermerScene);
        return s;
    }

    /** Pour les essais : un repaire tout de suite près de la ville, et sa mission acceptée. */
    public Repaire creerPourEssai(int v, long graine, float force) {
        Repaire r = creer(v, j.jourMaintenant(), new Random(graine));
        if (force > 0f) for (int k = 0; k < r.forces.length; k++) r.forces[k] = force;
        for (Missions.Mission m : j.missions().liste) if (m.type == Missions.CHASSE && m.dest == r.id) m.etat = Missions.ACCEPTEE;
        j.missions().majCible();
        publier();
        return r;
    }

    /** Pour les essais : la bande est défaite et son chef s'enfuit. */
    public String fuitePourEssai(int id) {
        Repaire r = repaire(id);
        r.etat = DETRUIT;
        for (int k = 0; k < r.etats.length; k++) if (r.etats[k] == EN_VIE) r.etats[k] = PARTI;
        String t = fuiteChef(r);
        publier();
        return t;
    }

    /** Pour les essais : un brigand mort qui a de la famille dans cette ville. */
    public void deuilPourEssai(int ville, long graine, String nom, boolean execute) {
        Deuil d = new Deuil();
        d.ville = ville;
        d.graine = graine;
        d.nom = nom;
        d.execute = execute;
        deuils.add(d);
        publier();
    }

    /** Pour les essais : le choix sur les prisonniers. */
    public Joueur.Scene sortPourEssai(int choix) {
        return sort(derniereRepaire, choix);
    }

    // ---------------------------------------------------------------- Sauvegarde

    void ecrire(DataOutputStream s) throws IOException {
        s.writeInt(suivant);
        s.writeInt(dernierJour);
        s.writeInt(contratVille);
        s.writeInt(contratPaie);
        s.writeInt(batailles);
        s.writeInt(victoires);
        s.writeInt(prochain.size());
        for (Map.Entry<Integer, Integer> e : prochain.entrySet()) {
            s.writeInt(e.getKey());
            s.writeInt(e.getValue());
        }
        s.writeInt(aLivrer.size());
        for (Map.Entry<Integer, Integer> e : aLivrer.entrySet()) {
            s.writeInt(e.getKey());
            s.writeInt(e.getValue());
        }
        s.writeInt(repaires.size());
        for (Repaire r : repaires) {
            s.writeInt(r.id);
            s.writeInt(r.ville);
            s.writeInt(r.peuple);
            s.writeInt(r.etat);
            s.writeInt(r.cree);
            s.writeInt(r.butin);
            s.writeInt(r.passage);
            s.writeInt(r.arrivees);
            s.writeInt(r.renfort);
            s.writeLong(r.graine);
            s.writeFloat(r.angle);
            s.writeFloat(r.bord);
            s.writeFloat(r.force);
            s.writeUTF(r.chef);
            s.writeInt(r.etats.length);
            for (int k = 0; k < r.etats.length; k++) {
                s.writeUTF(r.noms[k]);
                s.writeBoolean(r.femmes[k]);
                s.writeInt(r.armes[k]);
                s.writeInt(r.armures[k]);
                s.writeInt(r.etats[k]);
                s.writeFloat(r.forces[k]);
            }
            s.writeBoolean(r.route);
            s.writeLong(r.graineChef);
        }
        s.writeInt(derniereVille);
        s.writeInt(embuscades);
        s.writeBoolean(rivalNom != null);
        if (rivalNom != null) {
            s.writeUTF(rivalNom);
            s.writeBoolean(rivalFemme);
            s.writeLong(rivalGraine);
            s.writeInt(rivalPeuple);
            s.writeInt(rivalArme);
            s.writeInt(rivalArmure);
            s.writeInt(rivalNiveau);
            s.writeInt(rivalVictoires);
            s.writeInt(rivalRetour);
            s.writeInt(rivalRepaire);
            s.writeFloat(rivalForce);
        }
        s.writeInt(deuils.size());
        for (Deuil d : deuils) {
            s.writeInt(d.ville);
            s.writeLong(d.graine);
            s.writeUTF(d.nom);
            s.writeBoolean(d.execute);
        }
    }

    void lire(DataInputStream s, int format) throws IOException {
        suivant = s.readInt();
        dernierJour = s.readInt();
        contratVille = s.readInt();
        contratPaie = s.readInt();
        batailles = s.readInt();
        victoires = s.readInt();
        int n = s.readInt();
        for (int k = 0; k < n; k++) prochain.put(s.readInt(), s.readInt());
        n = s.readInt();
        for (int k = 0; k < n; k++) aLivrer.put(s.readInt(), s.readInt());
        n = s.readInt();
        for (int i = 0; i < n; i++) {
            Repaire r = new Repaire();
            r.id = s.readInt();
            r.ville = s.readInt();
            r.peuple = s.readInt();
            r.etat = s.readInt();
            r.cree = s.readInt();
            r.butin = s.readInt();
            r.passage = s.readInt();
            r.arrivees = s.readInt();
            r.renfort = s.readInt();
            r.graine = s.readLong();
            r.angle = s.readFloat();
            r.bord = s.readFloat();
            r.force = s.readFloat();
            r.chef = s.readUTF();
            int t = s.readInt();
            r.noms = new String[t];
            r.femmes = new boolean[t];
            r.armes = new int[t];
            r.armures = new int[t];
            r.etats = new int[t];
            r.forces = new float[t];
            for (int k = 0; k < t; k++) {
                r.noms[k] = s.readUTF();
                r.femmes[k] = s.readBoolean();
                r.armes[k] = s.readInt();
                r.armures[k] = s.readInt();
                r.etats[k] = s.readInt();
                r.forces[k] = s.readFloat();
            }
            if (format >= 12) {
                r.route = s.readBoolean();
                r.graineChef = s.readLong();
            }
            repaires.add(r);
        }
        if (format >= 12) {
            derniereVille = s.readInt();
            embuscades = s.readInt();
            if (s.readBoolean()) {
                rivalNom = s.readUTF();
                rivalFemme = s.readBoolean();
                rivalGraine = s.readLong();
                rivalPeuple = s.readInt();
                rivalArme = s.readInt();
                rivalArmure = s.readInt();
                rivalNiveau = s.readInt();
                rivalVictoires = s.readInt();
                rivalRetour = s.readInt();
                rivalRepaire = s.readInt();
                rivalForce = s.readFloat();
            }
            n = s.readInt();
            for (int k = 0; k < n; k++) {
                Deuil d = new Deuil();
                d.ville = s.readInt();
                d.graine = s.readLong();
                d.nom = s.readUTF();
                d.execute = s.readBoolean();
                deuils.add(d);
            }
        }
        // Une embuscade ne survit pas à la sauvegarde : les brigands se sont dispersés.
        for (Repaire r : repaires) disperser(r);
    }
}
