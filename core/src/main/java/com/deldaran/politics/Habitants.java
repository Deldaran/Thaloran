package com.deldaran.politics;

import com.deldaran.politics.planet.Terrain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Habitants {
    private static final double RAYON_METRES = Terrain.RAYON_KM * 1000.0;
    private static final double PORTEE = 180.0, OUBLI = 260.0;
    private static final int MAX_VILLAGE = 48, MAX_TOTAL = 120, RECHERCHES_PAR_IMAGE = 3;
    private static final float VITESSE = 1.4f;
    private static final String[][] GESTES = new String[Metiers.NOMBRE][];
    /** Lot G3 : le marchand d'étal crie sa marchandise, la montre, compte ses pièces ; il se tient à ETAL_RECUL m derrière l'étal. */
    private static final String[] ETAL = { "crier_marchandise", "montrer_marchandise", "compter_pieces", "crier_marchandise" };
    static final double ETAL_OUVERTURE = 7.0, ETAL_FERMETURE = 18.0, ETAL_RECUL = 1.1;
    /** Le destinataire d'une lettre (ou d'une mission sur la place) vient de moins de ACCUEIL_PORTEE m. */
    public static final double ACCUEIL_PORTEE = 60.0;
    private static final String[] DOMESTIQUE = { "balayer", "laver_linge", "etendre_linge", "puiser_eau", "couper_legumes", "remuer_marmite", "fendre_buche" };
    private static final String[] REPAS = { "manger_debout", "manger_bol", "boire" };
    private static final String[] REPAS_MAISON = { "manger_assis", "manger_bol" };
    private static final String[] REVEIL = { "s_etirer", "bailler", "se_laver_visage" };
    private static final String[] LOISIRS = { "attendre", "regarder_autour", "croiser_bras", "mains_sur_hanches", "assis_banc", "rire" };
    private static final String[] JEUX = { "jouer_enfant", "cache_cache", "ricochets", "sauter_de_joie", "danser" };
    private static final String[] PAUSES = { "s_eponger_front", "s_etirer", "boire_gourde", "assis_sol" };
    private static final String[] PAROLE = { "parler", "parler_anime", "rire", "hausser_epaules" };
    private static final String[] ECOUTE = { "ecouter", "acquiescer", "rire", "croiser_bras" };
    private static final String[] HOSTILE = { "regard_noir", "cracher", "menacer_doigt", "detourner_regard" };
    /** Ce que disent les passants qui te reconnaissent (phase 6). {qui} : ton surnom, sinon ton nom. */
    private static final String[] ACCUEIL_RENOM = { "Tiens, {qui} ! On parle de toi, ici.", "Salut, {qui}. Content de te voir.", "C'est bien toi, {qui} ? On m'a dit du bien de toi.",
            "Le bonjour, {qui} !", "Regarde, c'est {qui}." };
    private static final String[] MEFIANCE_RENOM = { "Voilà {qui}... Gardez vos bourses.", "Je ne veux pas d'ennuis avec toi, {qui}.", "Passe ton chemin, {qui}.",
            "On sait qui tu es, {qui}." };
    /** Écrits par l'écran : ton renom (palier) et ta réputation dans ce village, ton nom et ton surnom. */
    public volatile int joueurPalier;
    public volatile float joueurReputation;
    public volatile String joueurNom = "", joueurSurnom = "";
    public static final int SALUER = 0, FRAPPER = 1, OFFRIR = 2, EPARGNER = 3, ACHEVER = 4;

    static {
        GESTES[Metiers.MAISON] = DOMESTIQUE;
        GESTES[Metiers.MAIRIE] = new String[] { "signer", "proclamation", "recevoir_audience", "lire" };
        GESTES[Metiers.BOULANGERIE] = new String[] { "petrir", "faconner_pain", "enfourner", "recharger_four", "tamiser" };
        GESTES[Metiers.BOUCHERIE] = new String[] { "decouper_viande", "suspendre_viande", "aiguiser_lame" };
        GESTES[Metiers.MOULIN] = new String[] { "verser_grain", "ouvrir_vanne", "empiler_buches" };
        GESTES[Metiers.GRENIER] = new String[] { "verser_grain", "compter_pieces", "peser" };
        GESTES[Metiers.FORGE] = new String[] { "forger", "actionner_soufflet", "attiser_feu", "tremper_metal", "aiguiser_lame" };
        GESTES[Metiers.MENUISERIE] = new String[] { "raboter", "clouer", "scier", "poser_poutre" };
        GESTES[Metiers.TANNERIE] = new String[] { "gratter_peau", "tendre_peau", "remuer_cuve" };
        GESTES[Metiers.TISSERAND] = new String[] { "tisser", "filer_rouet", "filer_quenouille", "coudre" };
        GESTES[Metiers.POTIER] = new String[] { "tourner_poterie", "modeler", "enfourner_poterie" };
        GESTES[Metiers.AUBERGE] = new String[] { "tirer_biere", "servir_chope", "porter_plateau", "essuyer_comptoir", "encaisser" };
        GESTES[Metiers.TAVERNE] = GESTES[Metiers.AUBERGE];
        GESTES[Metiers.ENTREPOT] = new String[] { "empiler_buches", "compter_pieces", "peser" };
        GESTES[Metiers.PECHERIE] = new String[] { "vider_poisson", "lancer_filet", "pecher_ligne" };
        GESTES[Metiers.CASERNE] = new String[] { "entrainement_mannequin", "garde_a_vous", "aiguiser_lame" };
        GESTES[Metiers.ECOLE] = new String[] { "ecrire_tableau", "faire_reciter" };
        GESTES[Metiers.BAINS] = new String[] { "frotter_bains", "verser_eau_chaude" };
        GESTES[Metiers.CHATEAU] = new String[] { "garde_a_vous", "proclamation", "signer" };
    }

    public final List<Villageois> tous = new ArrayList<>();

    /** Les gestes du métier, pour le joueur qui travaille à la main (phase « en action ») : ceux des villageois, plus les champs, les bois, la mine et le marché. */
    public static Animations.Animation[] gestesJoueur(int lieu) {
        String[] noms;
        switch (lieu) {
            case com.deldaran.politics.histoire.Biens.CHAMPS: noms = new String[] { "faucher", "moissonner", "labourer", "semer", "desherber", "nourrir_betes", "traire" }; break;
            case com.deldaran.politics.histoire.Biens.FORET: noms = new String[] { "abattre_arbre", "couper_bois", "fendre_buche", "empiler_buches" }; break;
            case com.deldaran.politics.histoire.Biens.MINE: noms = new String[] { "piocher", "pelleter", "tailler_pierre" }; break;
            case com.deldaran.politics.histoire.Biens.MARCHE: noms = new String[] { "crier_marchandise", "montrer_marchandise", "marchander", "peser", "compter_pieces" }; break;
            default: noms = lieu >= 0 && lieu < Metiers.NOMBRE && GESTES[lieu] != null ? GESTES[lieu] : DOMESTIQUE;
        }
        List<Animations.Animation> r = new ArrayList<>();
        for (String n : noms) {
            Animations.Animation a = Animations.toutes().get(n);
            if (a != null && !a.mobile && a.partenaire == null) r.add(a);
        }
        return r.toArray(new Animations.Animation[0]);
    }

    /** Un travailleur vient servir s'il est à moins de SERVIR_PORTEE mètres ; il s'arrête à SERVIR_DISTANCE mètres du joueur. */
    public static final double SERVIR_PORTEE = 40.0, SERVIR_DISTANCE = 1.3;

    /**
     * Le travailleur de ce bâtiment vient servir le joueur : s'il est à l'intérieur, il sort par la porte ; il marche
     * jusqu'au joueur, se tourne vers lui et fait le geste demandé. Renvoie le villageois, ou null s'il n'y a personne (ou qu'il dort).
     */
    public Villageois servir(Villages.Village v, int batiment, Personnage joueur, String geste) {
        if (v == null || batiment < 0 || joueur == null || batiment * 2 + 1 >= v.portes.length) return null;
        v.local(joueur.position[0], joueur.position[1], joueur.position[2], local);
        double ja = local[0], jb = local[1];
        double porteA = v.portes[batiment * 2], porteB = v.portes[batiment * 2 + 1];
        Villageois mieux = null;
        double dm = SERVIR_PORTEE;
        for (Villageois w : tous) {
            if (w.village != v || w.enfant || w.travail != batiment || w.activite == Villageois.MORT || w.activite == Villageois.DORMIR
                    || w.activite >= Villageois.FUIR || w.corps.partenaire != null) continue;
            double d;
            if (w.dedans) d = Math.hypot(porteA - ja, porteB - jb) + 5.0;
            else {
                v.local(w.corps.position[0], w.corps.position[1], w.corps.position[2], autre);
                d = Math.hypot(autre[0] - ja, autre[1] - jb);
            }
            if (d < dm) {
                dm = d;
                mieux = w;
            }
        }
        if (mieux == null) return null;
        return amener(v, mieux, porteA, porteB, ja, jb, geste);
    }

    /** Lot G3 : le marchand d'étal le plus proche vient servir le joueur (touche M sur la place). */
    public Villageois servirEtal(Villages.Village v, Personnage joueur, String geste) {
        if (v == null || joueur == null) return null;
        v.local(joueur.position[0], joueur.position[1], joueur.position[2], local);
        double ja = local[0], jb = local[1];
        Villageois mieux = null;
        double dm = SERVIR_PORTEE;
        for (Villageois w : tous) {
            if (w.village != v || w.etal < 0 || w.dedans || w.activite != Villageois.TRAVAIL || w.corps.partenaire != null) continue;
            v.local(w.corps.position[0], w.corps.position[1], w.corps.position[2], autre);
            double d = Math.hypot(autre[0] - ja, autre[1] - jb);
            if (d < dm) {
                dm = d;
                mieux = w;
            }
        }
        return mieux == null ? null : amener(v, mieux, 0.0, 0.0, ja, jb, geste);
    }

    /**
     * Lot G3 : quelqu'un vient attendre le joueur sur la place (le destinataire d'une lettre) : d'abord le travailleur du bâtiment
     * donné (la mairie : le bailli), sinon l'adulte éveillé le plus proche.
     */
    public Villageois accueillir(Villages.Village v, int batiment, Personnage joueur, String geste) {
        Villageois w = batiment >= 0 ? servir(v, batiment, joueur, geste) : null;
        if (w != null || v == null || joueur == null) return w;
        v.local(joueur.position[0], joueur.position[1], joueur.position[2], local);
        double ja = local[0], jb = local[1];
        Villageois mieux = null;
        double dm = ACCUEIL_PORTEE;
        for (Villageois o : tous) {
            if (o.village != v || o.enfant || o.dedans || o.activite == Villageois.MORT || o.activite == Villageois.DORMIR || o.activite >= Villageois.FUIR
                    || o.corps.partenaire != null) continue;
            v.local(o.corps.position[0], o.corps.position[1], o.corps.position[2], autre);
            double d = Math.hypot(autre[0] - ja, autre[1] - jb);
            if (d < dm) {
                dm = d;
                mieux = o;
            }
        }
        return mieux == null ? null : amener(v, mieux, 0.0, 0.0, ja, jb, geste);
    }

    private Villageois amener(Villages.Village v, Villageois w, double porteA, double porteB, double ja, double jb, String geste) {
        finirDiscussion(w);
        if (w.dedans) {
            w.dedans = false;
            poser(w, porteA, porteB);
        }
        w.corps.arreter();
        w.servir = geste;
        w.salutAttente = Math.max(w.salutAttente, 60f);
        w.reste = Math.max(w.reste, 30f);
        v.local(w.corps.position[0], w.corps.position[1], w.corps.position[2], autre);
        double da = autre[0] - ja, db = autre[1] - jb, d = Math.hypot(da, db);
        if (d <= SERVIR_DISTANCE + 0.4) {
            w.enRoute = false;
            return w;
        }
        w.cibleA = ja + da / d * SERVIR_DISTANCE;
        w.cibleB = jb + db / d * SERVIR_DISTANCE;
        w.regardA = Double.NaN;
        w.enRoute = true;
        w.attentChemin = true;
        w.chemin = null;
        return w;
    }

    /** Le villageois le plus proche du joueur à qui l'on peut parler (debout, dehors), ou null. */
    public Villageois aQuiParler(Personnage joueur, double portee) {
        Villageois w = cible(joueur, portee, false);
        if (w == null || w.activite >= Villageois.FUIR || w.activite == Villageois.DORMIR) return null;
        return w;
    }

    /** Le villageois s'arrête et se tourne vers le joueur qui lui parle. */
    public void retenir(Villageois w, Personnage joueur) {
        finirDiscussion(w);
        w.corps.arreter();
        w.enRoute = false;
        w.chemin = null;
        regarderJoueur(w, joueur);
        w.salutAttente = Math.max(w.salutAttente, 30f);
        w.reste = Math.max(w.reste, 20f);
    }

    /** Le villageois quitte son village pour suivre le joueur : il n'est plus mené par la vie du village. Renvoie son corps (déjà affiché), ou null. */
    public Personnage retirer(long graine) {
        partis.add(graine);
        for (Villageois w : tous) {
            if (w.graine != graine) continue;
            finirDiscussion(w);
            for (Villageois o : tous) if (o.interlocuteur == w) finirDiscussion(o);
            if (w.conjoint != null && w.conjoint.conjoint == w) w.conjoint.conjoint = null;
            tous.remove(w);
            List<Villageois> l = parVillage.get(w.village);
            if (l != null) l.remove(w);
            w.corps.arreter();
            w.corps.enMarche = false;
            return w.corps;
        }
        return null;
    }

    /** Un membre de la bande parle : l'IA locale si elle est prête, sinon la réplique de secours. */
    public void direBande(String nom, String secours, String consigne) {
        if (ia == null || !ia.pret()) {
            parolesSecours++;
            annoncer(nom + " : \"" + secours + "\"");
            return;
        }
        Parole p = new Parole();
        p.nom = nom;
        p.secours = secours;
        p.attente = 2.5f;
        paroles.add(p);
        ia.demander(Repliques.SYSTEME, consigne, "bande | " + nom, t -> p.reponse = t);
    }

    /** Le nom de ce villageois tel que le joueur le connaît. */
    /** Lot F : les habitants qui incarnent la famille du joueur portent le nom de ses proches (graine → nom). */
    public volatile java.util.Map<Long, String> nomsForces = java.util.Collections.emptyMap();

    public String nom(Villageois w) {
        String force = nomsForces.get(w.graine);
        return force != null ? force : relations.lien(w).nomComplet();
    }

    /** Le villageois qui travaille dans ce bâtiment se tourne vers le joueur et fait un geste (le marchand qui montre sa marchandise). */
    public boolean reagir(Villages.Village v, int batiment, Personnage joueur, String geste) {
        for (Villageois w : tous) {
            if (w.village != v || w.enfant || w.travail != batiment || w.dedans || w.activite == Villageois.MORT) continue;
            regarderJoueur(w, joueur);
            jouerNom(w.corps, geste);
            Animations.Animation a = w.corps.animation;
            w.gesteReste = a != null && a.mode == Animations.UNE_FOIS ? a.duree + 0.5f : 3f;
            return true;
        }
        return false;
    }
    private final Map<Villages.Village, List<Villageois>> parVillage = new HashMap<>();
    private final Map<String, Animations.Animation[]> listes = new HashMap<>();
    private final Chemins chemins = new Chemins();
    private final Terrain.Point point = new Terrain.Point();
    private final double[] local = new double[2], autre = new double[2], monde = new double[3];
    private Terrain terrain;
    private int recherches;
    public int recherchees, echecs;
    public final Relations relations = new Relations();
    /** Graines des villageois partis avec le joueur (sa bande) : on ne les recrée pas dans leur village. */
    public final java.util.Set<Long> partis = new java.util.HashSet<>();
    /** Les maisons du joueur (« ville:bâtiment ») : personne n'y habite. */
    public final java.util.Set<String> maisonsJoueur = new java.util.HashSet<>();

    /** Les habitants d'une maison que le joueur vient de prendre déménagent : ils disparaissent du village. */
    public void vider(Villages.Village v, int maison, List<Personnage> personnages) {
        maisonsJoueur.add(v.ville + ":" + maison);
        List<Villageois> l = parVillage.get(v);
        for (Villageois w : new ArrayList<>(tous)) {
            if (w.village != v || w.maison != maison) continue;
            finirDiscussion(w);
            for (Villageois o : tous) if (o.interlocuteur == w) finirDiscussion(o);
            tous.remove(w);
            if (l != null) l.remove(w);
            w.corps.arreter();
            personnages.remove(w.corps);
        }
    }
    public double heures;
    public float joueurATerre;
    public String annonce;
    public float annonceReste;
    public IaLocale ia;
    public int parolesIa, parolesSecours;
    private float paroleAttente;
    private final List<Parole> paroles = new ArrayList<>();

    private static class Parole {
        String nom, secours;
        volatile String reponse;
        float attente;
    }

    // ================================================================ Lot H2 : les vrais habitants, chargés selon la distance

    /** Les habitants de chaque village, fournis par l'histoire (null tant qu'ils ne sont pas prêts) ; sans fournisseur, l'ancien peuplement. */
    public java.util.function.Function<Villages.Village, com.deldaran.politics.histoire.Gens.Foyers> foyers;
    /** Les gens à faire paraître en priorité (la cible d'une mission…), en plus des gens importants. */
    public volatile java.util.Set<Long> prioritaires = java.util.Collections.emptySet();
    /** Un habitant prend corps à VIE m (de sa maison ou de son travail) ; il le quitte au-delà d'OUBLI_CORPS m. CORPS_VILLAGE corps au plus par village. */
    public static final double VIE = 120.0, OUBLI_CORPS = 150.0;
    public static final int CORPS_VILLAGE = 80;
    /** On revoit qui doit avoir un corps toutes les INCARNATION s. */
    public static final float INCARNATION = 1f;
    private final Map<Villages.Village, com.deldaran.politics.histoire.Gens.Foyers> foyersVus = new HashMap<>();
    private final Map<Villages.Village, float[]> incarnations = new HashMap<>();

    /** Le nombre d'habitants d'un village et le nombre de corps (pour les essais et le débogage). */
    public String etatFoyers(Villages.Village v) {
        com.deldaran.politics.histoire.Gens.Foyers fo = foyersVus.get(v);
        List<Villageois> l = parVillage.get(v);
        int imp = 0, ouv = 0;
        if (fo != null) for (int i = 0; i < fo.n; i++) { if (fo.important[i]) imp++; if (i < fo.ouvrier.length && fo.ouvrier[i]) ouv++; }
        return (fo == null ? "-" : fo.n + " habitants (" + imp + " importants, " + ouv + " ouvriers du joueur)") + ", " + (l == null ? 0 : l.size()) + " corps";
    }

    private double distanceLocale(Villages.Village v, int q, double pa, double pb) {
        if (q < v.habitables && v.portes != null && q * 2 + 1 < v.portes.length) return Math.hypot(v.portes[q * 2] - pa, v.portes[q * 2 + 1] - pb);
        if (q * 4 + 2 >= v.positions.length) return Double.MAX_VALUE;
        v.local(v.positions[q * 4], v.positions[q * 4 + 1], v.positions[q * 4 + 2], autre);
        return Math.hypot(autre[0] - pa, autre[1] - pb);
    }

    private boolean occupe(Villageois w, List<Villageois> liste) {
        if (w.interlocuteur != null || w.corps.partenaire != null || w.servir != null || w.presente || w.garde || w.activite >= Villageois.FUIR) return true;
        for (Villageois o : liste) if (o.interlocuteur == w) return true;
        return false;
    }

    private void retirerCorps(Villageois w, List<Villageois> liste, List<Personnage> personnages) {
        finirDiscussion(w);
        if (w.conjoint != null && w.conjoint.conjoint == w) w.conjoint.conjoint = null;
        tous.remove(w);
        liste.remove(w);
        w.corps.arreter();
        personnages.remove(w.corps);
    }

    /**
     * Lot H2 : qui, parmi les habitants du village, a un corps. Les plus proches du centre de la vue (par leur maison ou leur
     * travail) et les gens importants d'abord, dans la limite de CORPS_VILLAGE ; ceux qui s'éloignent sont oubliés (s'ils ne sont
     * pas occupés), ceux qui approchent prennent corps, déjà à leur place (chez eux la nuit, au travail le jour).
     */
    private void incarner(Villages.Village v, com.deldaran.politics.histoire.Gens.Foyers fo, List<Personnage> personnages, Assembleur assembleur, double heure,
            double cx, double cy, double cz) {
        List<Villageois> liste = parVillage.get(v);
        if (liste == null) return;
        v.local(cx, cy, cz, local);
        double pa = local[0], pb = local[1];
        int n = fo.n;
        double[] score = new double[n];
        Integer[] ordre = new Integer[n];
        Map<Long, Integer> indice = new HashMap<>();
        java.util.Set<Long> prio = prioritaires;
        for (int i = 0; i < n; i++) {
            ordre[i] = i;
            long id = fo.id[i];
            indice.put(id, i);
            Relations.Lien l = relations.existant(id);
            boolean famille = i < fo.famille.length && fo.famille[i];
            if (partis.contains(id) || (l != null && l.mort) || (maisonsJoueur.contains(v.ville + ":" + fo.maison[i]) && !famille)) {
                score[i] = Double.MAX_VALUE;
                continue;
            }
            double d = distanceLocale(v, fo.maison[i], pa, pb);
            int t = fo.travail[i] >= 0 ? fo.travail[i] : fo.etal[i];
            if (t >= 0) d = Math.min(d, distanceLocale(v, t, pa, pb));
            if (fo.important[i] || prio.contains(id)) d = Math.min(d, 0.0) - 1000.0;
            score[i] = d;
        }
        java.util.Arrays.sort(ordre, (a, b) -> Double.compare(score[a], score[b]));
        int place = Math.max(0, Math.min(CORPS_VILLAGE, MAX_TOTAL - (tous.size() - liste.size())));
        java.util.Set<Long> voulus = new java.util.HashSet<>();
        for (int k = 0; k < n && voulus.size() < place; k++) {
            int i = ordre[k];
            if (score[i] > VIE) break;
            voulus.add(fo.id[i]);
        }
        Map<Long, Villageois> presents = new HashMap<>();
        // On garde d'abord les voulus et les occupés, puis, s'il reste de la place, ceux qui ne sont pas encore assez loin pour être oubliés.
        List<Villageois> sursis = new ArrayList<>();
        for (Villageois w : new ArrayList<>(liste)) {
            Integer i = indice.get(w.graine);
            if (i != null && (voulus.contains(w.graine) || occupe(w, liste))) presents.put(w.graine, w);
            else if (i != null && score[i] < OUBLI_CORPS) sursis.add(w);
            else retirerCorps(w, liste, personnages);
        }
        // Lot I : une maison ou un travail qui change (un mariage, un apprentissage) : il y va, sans disparaître.
        for (Villageois w : presents.values()) {
            int i = indice.get(w.graine);
            if (w.maison == fo.maison[i] && w.travail == fo.travail[i] && w.etal == fo.etal[i]) continue;
            w.maison = fo.maison[i];
            w.travail = fo.travail[i];
            w.etal = fo.etal[i];
            if (i < fo.debut.length) Besoins.horaires(w, fo.debut[i], fo.fin[i]);
            if (!occupe(w, liste)) w.reste = 0f;
        }
        // Les voulus qui n'ont pas encore de corps passent avant ceux qui sont en sursis.
        int attendus = 0;
        for (long id : voulus) if (!presents.containsKey(id)) attendus++;
        for (Villageois w : sursis) {
            if (presents.size() + attendus < place) presents.put(w.graine, w);
            else retirerCorps(w, liste, personnages);
        }
        List<Villageois> nouveaux = new ArrayList<>();
        for (int k = 0; k < n && presents.size() < place; k++) {
            long id = fo.id[ordre[k]];
            if (!voulus.contains(id)) break;
            if (presents.containsKey(id)) continue;
            int i = indice.get(id);
            int m = fo.maison[i] < v.metiers.length ? v.metiers[fo.maison[i]] : Metiers.MAISON;
            Villageois w = creer(v, fo.maison[i], fo.travail[i], m, fo.femme[i], fo.enfant[i], id, assembleur);
            w.nomReel = fo.nom[i];
            w.etal = fo.etal[i];
            // Lot H4 : sa bourse, son salaire, ses heures ; il paraît avec les besoins de l'heure qu'il est.
            if (i < fo.argent.length) {
                w.important = fo.important[i];
                w.bourse = fo.argent[i];
                w.salaire = fo.salaire[i];
                w.vieux = fo.vieux[i];
                w.ouvrier = i < fo.ouvrier.length && fo.ouvrier[i];
                Besoins.horaires(w, fo.debut[i], fo.fin[i]);
                Besoins.initialiser(w, heure + w.decalage);
            }
            if (w.lien != null && w.lien.mort) continue;
            nouveaux.add(w);
            presents.put(id, w);
        }
        for (Villageois w : nouveaux) {
            int c = fo.conjoint[indice.get(w.graine)];
            Villageois o = c < 0 ? null : presents.get(fo.id[c]);
            if (o != null) {
                w.conjoint = o;
                o.conjoint = w;
            }
        }
        for (Villageois w : nouveaux) {
            choisir(w, heure);
            if (w.enRoute) {
                w.enRoute = false;
                w.attentChemin = false;
                if (w.activite == Villageois.DORMIR) w.dedans = true;
                else poser(w, w.cibleA, w.cibleB);
            }
        }
        liste.addAll(nouveaux);
        tous.addAll(nouveaux);
    }

    /** Lot G5 : un village est refait (un bâtiment du joueur) : ses habitants s'en vont, ils reviennent avec le nouveau village. */
    public void oublier(Villages.Village v, List<Personnage> personnages) {
        foyersVus.remove(v);
        incarnations.remove(v);
        List<Villageois> l = parVillage.remove(v);
        if (l == null) return;
        for (Villageois w : l) {
            w.corps.arreter();
            personnages.remove(w.corps);
            tous.remove(w);
        }
    }

    public void mettreAJour(float delta, double heure, double acceleration, Villages villages, List<Personnage> personnages, Assembleur assembleur, Terrain terrain, Personnage joueur, double cx, double cy, double cz) {
        this.terrain = terrain;
        Iterator<Map.Entry<Villages.Village, List<Villageois>>> it = parVillage.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Villages.Village, List<Villageois>> e = it.next();
            if (distance(e.getKey(), cx, cy, cz) < OUBLI) continue;
            for (Villageois w : e.getValue()) {
                w.corps.arreter();
                personnages.remove(w.corps);
                tous.remove(w);
            }
            foyersVus.remove(e.getKey());
            incarnations.remove(e.getKey());
            it.remove();
        }
        for (int i = 0; i < villages.nombreVillages() && tous.size() < MAX_TOTAL; i++) {
            Villages.Village v = villages.village(i);
            if (parVillage.containsKey(v) || v.portes == null || v.grille == null || distance(v, cx, cy, cz) > PORTEE) continue;
            if (foyers == null) {
                peupler(v, assembleur, heure);
                continue;
            }
            // Lot H2 : on attend que l'histoire ait réparti les habitants, puis ils prennent corps près de la vue.
            com.deldaran.politics.histoire.Gens.Foyers fo = foyers.apply(v);
            if (fo == null) continue;
            foyersVus.put(v, fo);
            parVillage.put(v, new ArrayList<>());
            incarnations.put(v, new float[] { 0f });
        }
        for (Map.Entry<Villages.Village, float[]> e : incarnations.entrySet()) {
            float[] t = e.getValue();
            t[0] -= delta;
            if (t[0] > 0f) continue;
            t[0] = INCARNATION;
            com.deldaran.politics.histoire.Gens.Foyers fo = foyers == null ? null : foyers.apply(e.getKey());
            if (fo != null) foyersVus.put(e.getKey(), fo);
            fo = foyersVus.get(e.getKey());
            if (fo != null) incarner(e.getKey(), fo, personnages, assembleur, heure, cx, cy, cz);
        }
        recherches = RECHERCHES_PAR_IMAGE;
        relations.recaler(jour());
        annonceReste -= delta;
        paroleAttente -= delta;
        ecouter(delta);
        soigner(joueur, delta);
        double heuresJeu = delta * acceleration / 3600.0;
        for (int k = 0; k < tous.size(); k++) vivre(tous.get(k), delta, (float) heuresJeu, heure, joueur);
        for (Villageois w : tous) {
            boolean voulu = !w.dedans;
            if (voulu && !w.affiche) personnages.add(w.corps);
            else if (!voulu && w.affiche) personnages.remove(w.corps);
            w.affiche = voulu;
        }
    }

    public Villageois proche(double[] p) {
        Villageois meilleur = null;
        double d = Double.MAX_VALUE;
        for (Villageois w : tous) {
            if (w.dedans || w.activite == Villageois.MORT) continue;
            double dx = w.corps.position[0] - p[0], dy = w.corps.position[1] - p[1], dz = w.corps.position[2] - p[2];
            double e = dx * dx + dy * dy + dz * dz;
            if (e < d) {
                d = e;
                meilleur = w;
            }
        }
        return meilleur;
    }

    public int dehors() {
        int n = 0;
        for (Villageois w : tous) if (!w.dedans) n++;
        return n;
    }

    private static double distance(Villages.Village v, double cx, double cy, double cz) {
        double dx = v.x - cx, dy = v.y - cy, dz = v.z - cz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz) * RAYON_METRES;
    }

    static boolean logement(int m) {
        return m == Metiers.MAISON || m == Metiers.BOULANGERIE || m == Metiers.BOUCHERIE || m == Metiers.FORGE || m == Metiers.MENUISERIE || m == Metiers.TANNERIE
                || m == Metiers.TISSERAND || m == Metiers.POTIER || m == Metiers.AUBERGE || m == Metiers.TAVERNE || m == Metiers.PECHERIE;
    }

    private void peupler(Villages.Village v, Assembleur assembleur, double heure) {
        List<Villageois> liste = new ArrayList<>();
        List<Integer> postes = new ArrayList<>();
        for (int k = 0; k < v.habitables; k++) {
            int m = v.metiers[k];
            if (!logement(m)) {
                postes.add(k);
                continue;
            }
            if (liste.size() >= MAX_VILLAGE || maisonsJoueur.contains(v.ville + ":" + k)) continue;
            Random r = new Random(v.graines[k] * 31L + 5L);
            boolean couple = r.nextFloat() < 0.75f;
            int enfants = couple ? r.nextInt(3) : 0;
            boolean femme = !couple && r.nextBoolean();
            int poste = m == Metiers.MAISON ? -1 : k;
            Villageois chef = creer(v, k, poste, m, femme, false, r.nextLong(), assembleur);
            liste.add(chef);
            if (couple) {
                Villageois conjoint = creer(v, k, m != Metiers.MAISON && r.nextBoolean() ? k : -1, m, !femme, false, r.nextLong(), assembleur);
                chef.conjoint = conjoint;
                conjoint.conjoint = chef;
                liste.add(conjoint);
            }
            for (int e = 0; e < enfants; e++) liste.add(creer(v, k, -1, m, r.nextBoolean(), true, r.nextLong(), assembleur));
        }
        liste.removeIf(w -> (w.lien != null && w.lien.mort) || partis.contains(w.graine));
        for (Villageois w : liste) if (w.conjoint != null && ((w.conjoint.lien != null && w.conjoint.lien.mort) || partis.contains(w.conjoint.graine))) w.conjoint = null;
        for (int p : postes) {
            int voulus = v.metiers[p] == Metiers.CASERNE || v.metiers[p] == Metiers.CHATEAU ? 3 : v.metiers[p] == Metiers.MAISON ? 0 : 1;
            for (Villageois w : liste) {
                if (voulus == 0) break;
                if (w.enfant || w.travail >= 0 || v.metiers[w.maison] != Metiers.MAISON) continue;
                w.travail = p;
                voulus--;
            }
        }
        // Lot G3 : un habitant sans emploi tient chaque étal de la place, de l'ouverture à la fermeture du marché.
        int objets = Math.min(v.plans.length, v.positions.length / 4);
        for (int q = v.habitables; q < objets; q++) {
            Batiments.Plan pl = v.plans[q];
            if (pl == null || pl.style != Batiments.ETAL) continue;
            for (Villageois w : liste) {
                if (w.enfant || w.travail >= 0 || w.etal >= 0 || v.metiers[w.maison] != Metiers.MAISON) continue;
                w.etal = q;
                break;
            }
        }
        for (Villageois w : liste) {
            choisir(w, heure);
            if (w.enRoute) {
                w.enRoute = false;
                w.attentChemin = false;
                if (w.activite == Villageois.DORMIR) w.dedans = true;
                else poser(w, w.cibleA, w.cibleB);
            }
        }
        parVillage.put(v, liste);
        tous.addAll(liste);
    }

    private Villageois creer(Villages.Village v, int maison, int travail, int metier, boolean femme, boolean enfant, long graine, Assembleur assembleur) {
        Apparence a = null;
        for (int s = 0; s < 64; s++) {
            a = assembleur.tirer(graine + s * 7919L, v.peuple);
            if (a.femme == femme && a.enfant == enfant) break;
        }
        v.monde(v.portes[maison * 2], v.portes[maison * 2 + 1], monde);
        Personnage p = new Personnage(a, monde[0], monde[1], monde[2]);
        terrain.echantillonner(monde[0], monde[1], monde[2], 11, point);
        p.altitude = point.altitude;
        Villageois w = new Villageois(p, v, maison, travail, metier, a.enfant, graine);
        w.lien = relations.existant(graine);
        return w;
    }

    private void poser(Villageois w, double a, double b) {
        w.village.monde(a, b, monde);
        System.arraycopy(monde, 0, w.corps.position, 0, 3);
        terrain.echantillonner(monde[0], monde[1], monde[2], 11, point);
        w.corps.altitude = point.altitude;
    }

    private void choisir(Villageois w, double heure) {
        double h = heure + w.decalage;
        h -= Math.floor(h / 24.0) * 24.0;
        int precedente = w.activite;
        int a;
        // Lot H4 : ses besoins choisissent ; au réveil, il sort d'abord de chez lui.
        a = Besoins.choisir(w, h);
        if (a != Villageois.DORMIR && precedente == Villageois.DORMIR) a = Villageois.REVEIL;
        if (a == Villageois.DORMIR && precedente == Villageois.DORMIR && w.dedans) {
            w.reste = 5f;
            return;
        }
        w.activite = a;
        w.interlocuteur = null;
        Villages.Village v = w.village;
        double ma = v.portes[w.maison * 2], mb = v.portes[w.maison * 2 + 1];
        double ta = ma, tb = mb;
        int regard = w.maison;
        switch (a) {
            case Villageois.DORMIR:
                w.reste = 5f;
                break;
            case Villageois.REVEIL:
                if (w.dedans) {
                    w.dedans = false;
                    poser(w, ma, mb);
                }
                w.reste = 10f;
                break;
            case Villageois.TRAVAIL:
                if (w.travail >= 0) {
                    ta = v.portes[w.travail * 2];
                    tb = v.portes[w.travail * 2 + 1];
                    regard = w.travail;
                } else if (w.etal >= 0 && h >= ETAL_OUVERTURE && h < ETAL_FERMETURE) {
                    // Derrière son étal, côté extérieur de la place, tourné vers les chalands.
                    v.local(v.positions[w.etal * 4], v.positions[w.etal * 4 + 1], v.positions[w.etal * 4 + 2], autre);
                    double r = Math.max(0.5, Math.hypot(autre[0], autre[1]));
                    ta = autre[0] + autre[0] / r * ETAL_RECUL;
                    tb = autre[1] + autre[1] / r * ETAL_RECUL;
                    regard = w.etal;
                    w.reste = 60f + w.hasard.nextFloat() * 60f;
                    break;
                }
                ta += (w.hasard.nextDouble() - 0.5) * 2.0;
                tb += (w.hasard.nextDouble() - 0.5) * 2.0;
                w.reste = 40f + w.hasard.nextFloat() * 50f;
                break;
            case Villageois.MANGER: {
                // Lot H4 : la taverne si l'on a de quoi et envie de compagnie, la boulangerie si l'on a un sou, sinon chez soi.
                int lieu = w.bourse >= Besoins.REPAS_TAVERNE && w.envie > Besoins.ENVIE_TAVERNE ? chercher(v, w, Metiers.TAVERNE, Metiers.AUBERGE, -1)
                        : w.bourse >= Besoins.REPAS_BOUTIQUE && w.hasard.nextFloat() < 0.6f ? chercher(v, w, Metiers.BOULANGERIE, -1, -1) : -1;
                if (lieu >= 0) {
                    ta = v.portes[lieu * 2] + (w.hasard.nextDouble() - 0.5) * 2.5;
                    tb = v.portes[lieu * 2 + 1] + (w.hasard.nextDouble() - 0.5) * 2.5;
                    regard = lieu;
                }
                w.reste = 25f;
                break;
            }
            case Villageois.LOISIR: {
                int lieu = w.bourse >= Besoins.BIERE && w.hasard.nextFloat() < 0.3f + w.envie * 0.5f ? chercher(v, w, Metiers.TAVERNE, Metiers.AUBERGE, -1) : -1;
                if (lieu >= 0) {
                    ta = v.portes[lieu * 2] + (w.hasard.nextDouble() - 0.5) * 3.0;
                    tb = v.portes[lieu * 2 + 1] + (w.hasard.nextDouble() - 0.5) * 3.0;
                    regard = lieu;
                } else {
                    double t = w.hasard.nextDouble() * Math.PI * 2.0, r = 3.0 + w.hasard.nextDouble() * 5.0;
                    ta = Math.cos(t) * r;
                    tb = Math.sin(t) * r;
                    regard = -1;
                }
                w.reste = 40f + w.hasard.nextFloat() * 40f;
                break;
            }
            case Villageois.JOUER: {
                double t = w.hasard.nextDouble() * Math.PI * 2.0, r = 2.0 + w.hasard.nextDouble() * 8.0;
                ta += Math.cos(t) * r;
                tb += Math.sin(t) * r;
                regard = -1;
                w.reste = 25f + w.hasard.nextFloat() * 30f;
                break;
            }
            case Villageois.PAUSE:
                w.village.local(w.corps.position[0], w.corps.position[1], w.corps.position[2], local);
                ta = local[0];
                tb = local[1];
                regard = -1;
                w.reste = 12f;
                break;
            default:
                break;
        }
        w.lieu = regard;
        if (regard >= 0) {
            v.local(v.positions[regard * 4], v.positions[regard * 4 + 1], v.positions[regard * 4 + 2], autre);
            w.regardA = autre[0];
            w.regardB = autre[1];
        } else {
            w.regardA = Double.NaN;
        }
        w.cibleA = ta;
        w.cibleB = tb;
        w.corps.arreter();
        w.gesteReste = 0f;
        if (w.dedans) return;
        v.local(w.corps.position[0], w.corps.position[1], w.corps.position[2], local);
        w.enRoute = Math.hypot(ta - local[0], tb - local[1]) > 0.8;
        w.attentChemin = w.enRoute;
        w.chemin = null;
        if (a == Villageois.DORMIR && !w.enRoute) w.dedans = true;
    }

    public void oublier(List<Personnage> personnages) {
        for (Villageois w : tous) {
            w.corps.arreter();
            personnages.remove(w.corps);
        }
        tous.clear();
        parVillage.clear();
    }

    private int chercher(Villages.Village v, Villageois w, int m1, int m2, int m3) {
        int meilleur = -1;
        double d = Double.MAX_VALUE;
        double ma = v.portes[w.maison * 2], mb = v.portes[w.maison * 2 + 1];
        for (int k = 0; k < v.habitables; k++) {
            int m = v.metiers[k];
            if (m != m1 && m != m2 && m != m3) continue;
            double e = Math.hypot(v.portes[k * 2] - ma, v.portes[k * 2 + 1] - mb);
            if (e < d) {
                d = e;
                meilleur = k;
            }
        }
        return meilleur;
    }

    private void vivre(Villageois w, float delta, float heures, double heure, Personnage joueur) {
        w.faim = Math.min(1f, w.faim + heures / Besoins.FAIM_HEURES * (w.dedans && w.activite == Villageois.DORMIR ? Besoins.FAIM_SOMMEIL : 1f));
        if (w.activite == Villageois.TRAVAIL && !w.enRoute) w.fatigue = Math.min(1f, w.fatigue + heures / 5f);
        else w.fatigue = Math.max(0f, w.fatigue - heures / 3f);
        if (w.activite == Villageois.DISCUTER || (w.activite == Villageois.LOISIR && !w.enRoute)) w.social = Math.max(0f, w.social - heures * 1.5f);
        else w.social = Math.min(1f, w.social + heures / 6f);
        // Lot H4 : le sommeil, l'envie, le salaire.
        Besoins.user(w, heures);
        if (w.gain >= 1f) {
            int g = (int) w.gain;
            w.gain -= g;
            payer(w, g);
        }
        // Lot H5 : chaque heure d'ouvrage d'un ouvrier du joueur fait tourner l'atelier ; on dit au joueur ce qui s'est fait s'il est tout près.
        if (w.ouvrage >= 1f) {
            w.ouvrage -= 1f;
            boolean proche = joueur != null && metres(w.corps.position, joueur.position) < PROCHE_OUVRAGE;
            ouvrages.add(new long[] { w.graine, proche ? 1L : 0L });
        }
        w.salutAttente -= delta;
        w.reste -= delta;
        w.coupAttente -= delta;
        if (w.dedans) {
            w.fatigue = 0f;
            if (w.reste <= 0f) choisir(w, heure);
            return;
        }
        if (w.corps.partenaire != null && !w.corps.meneur) return;
        double dj = joueur != null ? metres(w.corps.position, joueur.position) : Double.MAX_VALUE;
        if (w.activite >= Villageois.FUIR) {
            conflit(w, delta, heure, joueur, dj);
            return;
        }
        w.sante = Math.min(1f, w.sante + delta / 90f);
        if (traquer(w, joueur, dj)) return;
        if (w.enRoute) {
            marcher(w, delta);
            return;
        }
        if (w.servir != null) {
            // Arrivé près du joueur : il se tourne vers lui et fait son geste (montrer, saluer, recevoir).
            if (joueur != null) regarderJoueur(w, joueur);
            jouerNom(w.corps, w.servir);
            Animations.Animation an = w.corps.animation;
            w.gesteReste = an != null && an.mode == Animations.UNE_FOIS ? an.duree + 2f : 5f;
            w.servir = null;
            return;
        }
        if (w.activite == Villageois.MANGER && w.reste <= 0f) {
            w.faim = 0f;
            // Lot H4 : le repas se paie à la boulangerie ou à la taverne ; chez soi, on mange ses vivres.
            int m = w.lieu >= 0 && w.lieu < w.village.metiers.length && w.lieu != w.maison ? w.village.metiers[w.lieu] : -1;
            payer(w, m == Metiers.BOULANGERIE ? -Besoins.REPAS_BOUTIQUE : m == Metiers.TAVERNE || m == Metiers.AUBERGE ? -Besoins.REPAS_TAVERNE : 0);
        }
        if (w.activite == Villageois.LOISIR && w.reste <= 0f && w.lieu >= 0 && w.lieu < w.village.metiers.length && w.lieu != w.maison
                && (w.village.metiers[w.lieu] == Metiers.TAVERNE || w.village.metiers[w.lieu] == Metiers.AUBERGE)) payer(w, -Besoins.BIERE);
        if (w.activite == Villageois.PAUSE && w.reste <= 0f) w.fatigue *= 0.3f;
        if (w.activite == Villageois.DISCUTER && w.reste <= 0f) w.social = 0f;
        if (w.reste <= 0f) {
            finirDiscussion(w);
            choisir(w, heure);
            return;
        }
        if (accueillir(w, joueur, dj)) return;
        if (w.activite == Villageois.LOISIR && w.social > 0.25f && w.interlocuteur == null) chercherCompagnie(w);
        w.gesteReste -= delta;
        if (w.corps.animation == null || w.gesteReste <= 0f) geste(w);
    }

    /** Lot H5 : les heures d'ouvrage des ouvriers du joueur ({id, 1 si le joueur est tout près}), une par heure de jeu. */
    public final java.util.concurrent.ConcurrentLinkedQueue<long[]> ouvrages = new java.util.concurrent.ConcurrentLinkedQueue<>();
    /** Lot H5 : en deçà (m), le joueur voit ce qu'a fait l'ouvrier. */
    public static final double PROCHE_OUVRAGE = 15.0;

    /** Lot H4 : les bourses des gens importants qui ont changé ({id, cuivre}), que l'écran reporte sur leur fiche. */
    public final java.util.concurrent.ConcurrentLinkedQueue<long[]> bourses = new java.util.concurrent.ConcurrentLinkedQueue<>();

    /** Lot H4 : un gain (positif) ou une dépense (négative) ; on ne dépense pas plus qu'on n'a. */
    private void payer(Villageois w, int d) {
        if (d < 0 && w.bourse < -d) d = -w.bourse;
        if (d == 0) return;
        w.bourse += d;
        if (w.important) bourses.add(new long[] { w.graine, d });
    }

    private boolean accueillir(Villageois w, Personnage joueur, double dj) {
        if (joueur == null || w.salutAttente > 0f || w.activite == Villageois.DISCUTER || dj > 3.5 || joueurATerre > 0f) return false;
        Relations.Lien l = w.lien;
        boolean neutre = l == null || (Math.abs(l.estime) < 0.2f && l.peur < 0.3f && l.rancune < 0.25f && !l.soumis && !l.rival);
        if (neutre && w.enfant && w.hasard.nextBoolean()) {
            w.salutAttente = 60f;
            return false;
        }
        w.salutAttente = 90f + w.hasard.nextFloat() * 60f;
        String nom = "saluer_main", bulle = null;
        // Un inconnu qui a entendu parler de toi : il te salue par ton surnom, ou se méfie si ta réputation est mauvaise.
        String renom = null;
        int palier = joueurPalier;
        if (neutre && !w.enfant && palier >= 1 && paroleAttente <= 0f && w.hasard.nextFloat() < 0.25f * palier && !joueurNom.isEmpty()) {
            String qui = joueurSurnom.isEmpty() ? joueurNom : joueurSurnom;
            if (joueurReputation <= -20f) {
                nom = HOSTILE[w.hasard.nextInt(HOSTILE.length)];
                bulle = "colere";
                renom = MEFIANCE_RENOM[w.hasard.nextInt(MEFIANCE_RENOM.length)];
            } else {
                if (joueurReputation >= 40f) bulle = "coeur";
                renom = ACCUEIL_RENOM[w.hasard.nextInt(ACCUEIL_RENOM.length)];
            }
            renom = renom.replace("{qui}", qui);
        }
        if (!neutre) {
            if (l.soumis) nom = "salut_bas";
            else if (l.peur > 0.55f && l.peur >= l.rancune) {
                if (dj < 2.5) {
                    fuir(w);
                    w.corps.montrer("sueur", 2f);
                    return true;
                }
                nom = "se_proteger";
                bulle = "sueur";
            } else if (l.rival || l.rancune > 0.35f || l.estime < -0.3f) {
                nom = l.rival ? "jurer_vengeance" : HOSTILE[w.hasard.nextInt(HOSTILE.length)];
                bulle = "colere";
            } else if (l.estime > 0.45f) bulle = "coeur";
        }
        regarderJoueur(w, joueur);
        jouerNom(w, nom);
        if (bulle != null) w.corps.montrer(bulle, 2.5f);
        if (renom != null) {
            paroleAttente = 6f;
            boolean nouveau = relations.existant(w.graine) == null;
            Relations.Lien ln = relations.lien(w);
            // Ce qu'on raconte de toi colore la première impression.
            if (nouveau) ln.estime = Math.max(-1f, Math.min(1f, joueurReputation / 400f));
            annoncer(ln.nomComplet() + " : \"" + renom + "\"");
        } else if (!neutre && !l.rival && paroleAttente <= 0f) {
            paroleAttente = 6f;
            parler(w, l.soumis || "coeur".equals(bulle) ? Repliques.SALUT_AMI : "sueur".equals(bulle) ? Repliques.PEUR : Repliques.SALUT_HOSTILE);
        }
        Animations.Animation a = w.corps.animation;
        w.gesteReste = a != null && a.mode == Animations.UNE_FOIS ? a.duree + 0.3f : 2.5f;
        return true;
    }

    private boolean traquer(Villageois w, Personnage joueur, double dj) {
        Relations.Lien l = w.lien;
        if (l == null || !l.rival || joueur == null || joueurATerre > 0f || dj > 45.0 || jour() < l.prochaineTraque) return false;
        finirDiscussion(w);
        commencer(w, Villageois.TRAQUER);
        w.presente = false;
        annoncer(l.nomComplet() + (l.victoires + l.defaites == 0 ? " t'a reconnu et vient vers toi" : " revient pour t'affronter (niveau " + l.niveau + ")"));
        return true;
    }

    private void commencer(Villageois w, int activite) {
        w.activite = activite;
        w.enRoute = false;
        w.attentChemin = false;
        w.chemin = null;
        w.reste = 0f;
        w.cheminAttente = 0f;
        w.corps.arreter();
    }

    private void conflit(Villageois w, float delta, double heure, Personnage joueur, double dj) {
        Relations.Lien l = w.lien;
        boolean tenace = l != null && l.rival || w.garde;
        switch (w.activite) {
            case Villageois.MORT:
                if (w.corps.animation == null) jouerNom(w, "mourir");
                return;
            case Villageois.A_TERRE:
                if (w.corps.animation == null) jouerNom(w, "supplier");
                if (w.reste <= 0f || joueur == null || dj > 12.0) relever(w);
                return;
            case Villageois.FUIR:
                if (w.corps.animation != null && !w.corps.animation.mobile) return;
                if (!w.enRoute) {
                    arriver(w);
                    return;
                }
                if (w.corps.animation == null) jouerNom(w, "fuir");
                marcher(w, delta);
                return;
            case Villageois.TRAQUER: {
                if (joueur == null || joueurATerre > 0f || dj > (l != null && l.rival ? 70.0 : w.garde ? 40.0 : 25.0)) {
                    abandonner(w, heure);
                    return;
                }
                if (w.reste > 0f) return;
                if (!w.presente && dj < 5.0) {
                    w.presente = true;
                    w.enRoute = false;
                    w.chemin = null;
                    w.corps.arreter();
                    regarderJoueur(w, joueur);
                    String intro = w.garde ? "cri_de_guerre" : l == null || !l.rival ? "colere_poing" : l.victoires > 0 ? "se_moquer" : l.defaites > 0 ? "jurer_vengeance" : "defier";
                    jouerNom(w, intro);
                    w.corps.montrer(w.garde ? "exclamation" : "colere", 2.5f);
                    w.reste = 2.2f;
                    if (l != null && l.rival) parler(w, Repliques.DEFI);
                    return;
                }
                if (dj < 1.0) {
                    commencer(w, Villageois.COMBATTRE);
                    w.reste = tenace ? 1e9f : 12f;
                    w.coupAttente = 0.5f;
                    return;
                }
                poursuivre(w, joueur, delta);
                return;
            }
            case Villageois.COMBATTRE: {
                if (joueur == null) {
                    abandonner(w, heure);
                    return;
                }
                if (joueurATerre > 0f) {
                    if (w.corps.animation == null || w.corps.animation.mode == Animations.UNE_FOIS && !w.corps.animation.nom.equals("se_moquer")) {
                        jouerNom(w, "se_moquer");
                        w.coupAttente = 3f;
                    } else if (w.coupAttente <= 0f) abandonner(w, heure);
                    return;
                }
                if (w.reste <= 0f) {
                    abandonner(w, heure);
                    return;
                }
                if (dj > 1.4) {
                    commencer(w, Villageois.TRAQUER);
                    w.presente = true;
                    return;
                }
                if (w.corps.animation == null || w.corps.animation.mode != Animations.UNE_FOIS) regarderJoueur(w, joueur);
                if (w.coupAttente <= 0f && w.corps.partenaire == null) {
                    regarderJoueur(w, joueur);
                    jouerNom(w, w.hasard.nextFloat() < 0.7f ? "coup_de_poing" : "coup_de_pied");
                    int niveau = l != null && l.rival ? l.niveau : 0;
                    w.coupAttente = Math.max(0.7f, 1.5f - 0.12f * niveau) + w.hasard.nextFloat() * 0.4f;
                    blesserJoueur(w, joueur, (w.garde ? 0.1f : 0.07f) + 0.03f * niveau);
                }
                return;
            }
            default:
                abandonner(w, heure);
        }
    }

    private void blesserJoueur(Villageois w, Personnage joueur, float degat) {
        joueur.sante -= degat;
        if (joueur.sante > 0f) {
            if (joueur.animation == null || joueur.animation.mode != Animations.UNE_FOIS) jouerNom(joueur, "etre_touche");
            return;
        }
        joueur.sante = 0f;
        joueurATerre = 6f;
        joueur.arreter();
        jouerNom(joueur, "a_terre_assomme");
        Relations.Lien l = w.lien;
        if (l != null && l.rival) {
            l.victoires++;
            l.niveau++;
            if (l.victoires >= 3) l.titrer(Relations.TITRE_FLEAU);
            else if (l.victoires == 1) l.titrer(Relations.TITRE_VICTOIRE);
            l.prochaineTraque = jour() + 0.75;
            relations.retenir(w, Relations.VICTOIRE, jour());
            annoncer(l.nomComplet() + " t'a mis a terre et passe au niveau " + l.niveau);
            parler(w, Repliques.VICTOIRE);
        } else annoncer((l != null ? l.nom : "Un villageois") + " t'a mis a terre");
        if (l != null) l.rancune = Math.max(l.rival ? 0.75f : 0f, l.rancune - 0.3f);
    }

    private void soigner(Personnage joueur, float delta) {
        if (joueur == null) return;
        if (joueurATerre > 0f) {
            joueurATerre -= delta;
            if (joueur.animation == null) jouerNom(joueur, "a_terre_assomme");
            if (joueurATerre <= 0f) {
                joueur.arreter();
                jouerNom(joueur, "se_relever");
                joueur.sante = 0.5f;
            }
            return;
        }
        joueur.sante = Math.min(1f, joueur.sante + delta / 120f);
    }

    private void poursuivre(Villageois w, Personnage joueur, float delta) {
        Villages.Village v = w.village;
        w.cheminAttente -= delta;
        if (w.chemin == null || w.cheminAttente <= 0f) {
            v.local(w.corps.position[0], w.corps.position[1], w.corps.position[2], local);
            v.local(joueur.position[0], joueur.position[1], joueur.position[2], autre);
            w.cibleA = autre[0];
            w.cibleB = autre[1];
            float[] c = null;
            if (!Chemins.direct(v.grille, local[0], local[1], autre[0], autre[1]) && recherches > 0) {
                recherches--;
                c = chemins.trouver(v.grille, local[0], local[1], autre[0], autre[1]);
                recherchees++;
            }
            w.chemin = c != null && c.length >= 4 ? c : new float[] { (float) local[0], (float) local[1], (float) autre[0], (float) autre[1] };
            w.etape = 1;
            w.enRoute = true;
            w.attentChemin = false;
            w.cheminAttente = 1f;
        }
        if (w.corps.animation == null) jouerNom(w, "courir");
        marcher(w, delta);
    }

    private void abandonner(Villageois w, double heure) {
        w.presente = false;
        w.garde = false;
        w.corps.arreter();
        w.activite = Villageois.LOISIR;
        choisir(w, heure);
    }

    private void fuir(Villageois w) {
        finirDiscussion(w);
        commencer(w, Villageois.FUIR);
        Villages.Village v = w.village;
        w.cibleA = v.portes[w.maison * 2];
        w.cibleB = v.portes[w.maison * 2 + 1];
        w.enRoute = true;
        w.attentChemin = true;
    }

    private void relever(Villageois w) {
        w.sante = 0.4f;
        Relations.Lien l = relations.lien(w);
        if (l.rival) l.prochaineTraque = jour() + 0.5;
        else if (relations.promouvoir(w, Relations.TITRE_HUMILIATION, jour())) {
            l.prochaineTraque = jour() + 0.5;
            annoncer(l.nom + " se releve en jurant de se venger. On l'appelle desormais " + l.nomComplet());
            parler(w, Repliques.HUMILIATION);
        }
        fuir(w);
        jouerNom(w, "se_relever");
    }

    private void aTerre(Villageois w) {
        commencer(w, Villageois.A_TERRE);
        w.sante = 0f;
        w.reste = 15f;
        jouerNom(w, "se_rendre");
        relations.retenir(w, Relations.ASSOMME, jour());
        Relations.Lien l = w.lien;
        if (l.rival) {
            l.defaites++;
            if (l.defaites >= 2) l.titrer(Relations.TITRE_INCREVABLE);
        }
        annoncer(l.nomComplet() + " est a terre : E pour epargner, K pour achever");
        parler(w, Repliques.SUPPLIQUE);
    }

    public void agir(int action, Personnage joueur) {
        if (joueur == null || joueurATerre > 0f) return;
        Villageois c = cible(joueur, action == SALUER ? 5.0 : 1.9, action == EPARGNER || action == ACHEVER);
        joueur.arreter();
        if (c != null) {
            c.village.local(c.corps.position[0], c.corps.position[1], c.corps.position[2], local);
            c.village.local(joueur.position[0], joueur.position[1], joueur.position[2], autre);
            joueur.orienter(local[0] - autre[0], local[1] - autre[1]);
        }
        switch (action) {
            case SALUER:
                jouerNom(joueur, "saluer_main");
                if (c == null) return;
                relations.retenir(c, Relations.SALUT, jour());
                if (c.activite < Villageois.FUIR && !c.enRoute && c.corps.partenaire == null) {
                    c.salutAttente = 0f;
                    accueillir(c, joueur, 0.0);
                }
                return;
            case FRAPPER:
                jouerNom(joueur, "coup_de_poing");
                if (c != null) frapper(c);
                return;
            case OFFRIR: {
                if (c == null || c.activite >= Villageois.FUIR || c.corps.partenaire != null) {
                    annoncer("Personne a qui offrir quelque chose");
                    return;
                }
                finirDiscussion(c);
                c.corps.arreter();
                Animations.Animation a = Animations.toutes().get("donner_objet");
                if (a != null) joueur.jouer(a, c.corps);
                relations.retenir(c, Relations.CADEAU, jour());
                Relations.Lien l = c.lien;
                c.corps.montrer(l.rival ? "colere" : l.estime > 0.3f ? "coeur" : "points", 3f);
                c.gesteReste = 1.5f;
                if (l.rival) annoncer(l.nom + " prend ton present sans un mot");
                else parler(c, Repliques.CADEAU);
                return;
            }
            case EPARGNER: {
                if (c == null) {
                    annoncer("Personne a epargner");
                    return;
                }
                jouerNom(joueur, "epargner");
                relations.retenir(c, Relations.EPARGNE, jour());
                temoins(c, -1);
                Relations.Lien l = c.lien;
                if (l.rival) {
                    l.epargnes++;
                    if (l.defaites >= 3 && l.epargnes >= 2) {
                        l.rival = false;
                        l.soumis = true;
                        l.rancune = 0f;
                        l.peur = 0.5f;
                        l.estime = Math.max(l.estime, 0.5f);
                        relations.retenir(c, Relations.REDDITION, jour());
                        annoncer(l.nomComplet() + " s'incline et ne te combattra plus");
                        parler(c, Repliques.REDDITION);
                    } else {
                        l.niveau++;
                        if (l.defaites < 2) l.titrer(Relations.TITRE_RETOUR);
                        annoncer(l.nomComplet() + (l.femme ? " fuit, humiliee. Elle reviendra plus forte" : " fuit, humilie. Il reviendra plus fort") + " (niveau " + l.niveau + ")");
                    }
                }
                relever(c);
                return;
            }
            case ACHEVER:
                if (c == null) annoncer("Personne a achever");
                else tuer(c, joueur);
                return;
            default:
        }
    }

    private Villageois cible(Personnage joueur, double portee, boolean aTerre) {
        Villageois meilleur = null;
        double d = portee;
        for (Villageois w : tous) {
            if (w.dedans || w.activite == Villageois.MORT || (w.activite == Villageois.A_TERRE) != aTerre) continue;
            double e = metres(w.corps.position, joueur.position);
            if (e < d) {
                d = e;
                meilleur = w;
            }
        }
        return meilleur;
    }

    private void frapper(Villageois c) {
        if (c.activite == Villageois.A_TERRE || c.activite == Villageois.MORT) return;
        finirDiscussion(c);
        relations.retenir(c, Relations.COUP, jour());
        temoins(c, Relations.TEMOIN_COUP);
        Relations.Lien l = c.lien;
        float degat = c.enfant ? 0.5f : 0.34f / (1f + 0.3f * Math.max(0, (l.rival ? l.niveau : 1) - 1));
        c.sante -= degat;
        if (c.sante <= 0f) {
            aTerre(c);
            return;
        }
        boolean courage = !c.enfant && (l.rival || c.garde || (l.rancune > 0.4f && l.peur < 0.6f) || c.hasard.nextFloat() < 0.25f);
        if (courage) {
            if (c.activite != Villageois.COMBATTRE) {
                commencer(c, Villageois.COMBATTRE);
                c.reste = l.rival || c.garde ? 1e9f : 10f + c.hasard.nextFloat() * 10f;
            }
            c.corps.arreter();
            c.coupAttente = 0.6f;
            jouerNom(c, "etre_touche");
            c.corps.montrer("colere", 2f);
        } else {
            fuir(c);
            jouerNom(c, "etre_touche");
            c.corps.montrer("larme", 2.5f);
        }
    }

    private void tuer(Villageois c, Personnage joueur) {
        commencer(c, Villageois.MORT);
        Animations.Animation a = Animations.toutes().get("achever");
        if (a != null) joueur.jouer(a, c.corps);
        Relations.Lien l = relations.lien(c);
        l.mort = true;
        if (l.rival) relations.rivaux.remove(l);
        l.rival = false;
        relations.retenir(c, Relations.MEURTRE, jour());
        temoins(c, Relations.TEMOIN_MEURTRE);
        StringBuilder texte = new StringBuilder(l.nomComplet()).append(l.femme ? " est morte" : " est mort");
        Villageois vengeur = null;
        List<Villageois> liste = parVillage.get(c.village);
        if (liste != null) {
            for (Villageois o : liste) {
                if (o == c || o.maison != c.maison || o.activite == Villageois.MORT) continue;
                relations.retenir(o, Relations.PROCHE_TUE, jour(), l.nom);
                if (relations.promouvoir(o, Relations.TITRE_VENGEANCE, jour())) {
                    texte.append(". ").append(o.lien.nom).append(l.femme ? " jure de la venger : " : " jure de le venger : ").append(o.lien.nomComplet());
                    o.lien.prochaineTraque = jour() + 0.1;
                    if (vengeur == null) vengeur = o;
                }
            }
        }
        annoncer(texte.toString());
        if (vengeur != null) parler(vengeur, Repliques.VENGEANCE);
    }

    private void temoins(Villageois c, int type) {
        List<Villageois> liste = parVillage.get(c.village);
        if (liste == null) return;
        for (Villageois o : liste) {
            if (o == c || o.dedans || o.activite == Villageois.MORT) continue;
            if (metres(o.corps.position, c.corps.position) > 15.0) continue;
            if (type < 0) {
                Relations.Lien l = relations.lien(o);
                l.estime = Relations.borner(l.estime + 0.05f, -1f, 1f);
                continue;
            }
            boolean famille = o.maison == c.maison;
            relations.retenir(o, type, jour());
            Relations.Lien l = o.lien;
            if (famille) l.rancune = Relations.borner(l.rancune + 0.25f, 0f, 1f);
            if (o.activite >= Villageois.FUIR || o.corps.partenaire != null) continue;
            boolean garde = !o.enfant && o.travail >= 0 && (o.village.metiers[o.travail] == Metiers.CASERNE || o.village.metiers[o.travail] == Metiers.CHATEAU);
            if (garde || (famille && !o.enfant && l.peur < 0.7f && type != Relations.TEMOIN_MEURTRE)) {
                finirDiscussion(o);
                commencer(o, Villageois.TRAQUER);
                o.garde = garde;
                o.presente = false;
                if (garde) annoncer("Un garde accourt !");
            } else if (o.enfant || l.peur > 0.4f || type == Relations.TEMOIN_MEURTRE) {
                fuir(o);
                o.corps.montrer("exclamation", 2f);
            } else {
                o.corps.montrer("colere", 2.5f);
                o.salutAttente = 0f;
            }
        }
    }

    private void parler(Villageois w, int situation) {
        String secours = Repliques.secours(situation, w);
        String nom = w.lien != null ? w.lien.nomComplet() : "Un villageois";
        if (ia == null || !ia.pret()) {
            parolesSecours++;
            annoncer(nom + " : \"" + secours + "\"");
            return;
        }
        Parole p = new Parole();
        p.nom = nom;
        p.secours = secours;
        p.attente = 2.5f;
        paroles.add(p);
        ia.demander(Repliques.SYSTEME, Repliques.consigne(situation, w, jour()), Repliques.NOMS[situation] + " | " + nom, t -> p.reponse = t);
    }

    private void ecouter(float delta) {
        for (Iterator<Parole> it = paroles.iterator(); it.hasNext();) {
            Parole p = it.next();
            String r = p.reponse;
            p.attente -= delta;
            if (r == null && p.attente > 0f) continue;
            if (r != null) parolesIa++;
            else parolesSecours++;
            annoncer(p.nom + " : \"" + (r != null ? IaLocale.affichable(r) : p.secours) + "\"");
            it.remove();
        }
    }

    public void annoncer(String texte) {
        System.out.println("[relations] " + texte);
        relations.modifie = true;
        if (annonce != null && annonceReste > 3f) {
            int saut = annonce.lastIndexOf('\n');
            texte = (saut >= 0 ? annonce.substring(saut + 1) : annonce) + "\n" + texte;
        }
        annonce = texte;
        annonceReste = 8f;
    }

    private double jour() {
        return heures / 24.0;
    }

    private static double metres(double[] a, double[] b) {
        double dx = a[0] - b[0], dy = a[1] - b[1], dz = a[2] - b[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz) * RAYON_METRES;
    }

    private void regarderJoueur(Villageois w, Personnage joueur) {
        w.village.local(joueur.position[0], joueur.position[1], joueur.position[2], autre);
        regarder(w, autre[0], autre[1]);
    }

    private static void jouerNom(Personnage p, String nom) {
        Animations.Animation a = Animations.toutes().get(nom);
        if (a != null) p.jouer(a);
    }

    private static void jouerNom(Villageois w, String nom) {
        jouerNom(w.corps, nom);
    }

    public float reputation(Villages.Village v) {
        List<Villageois> liste = v == null ? null : parVillage.get(v);
        if (liste == null || liste.isEmpty()) return 0f;
        float s = 0f;
        for (Villageois w : liste) if (w.lien != null) s += w.lien.estime;
        return s / liste.size();
    }

    private void chercherCompagnie(Villageois w) {
        w.village.local(w.corps.position[0], w.corps.position[1], w.corps.position[2], local);
        for (Villageois o : parVillage.get(w.village)) {
            if (o == w || o.dedans || o.enRoute || o.interlocuteur != null || (o.activite != Villageois.LOISIR && o.activite != Villageois.TRAVAIL) || o.enfant != w.enfant) continue;
            if (o.activite == Villageois.TRAVAIL && o.hasard.nextFloat() < 0.9f) continue;
            o.village.local(o.corps.position[0], o.corps.position[1], o.corps.position[2], autre);
            double d = Math.hypot(autre[0] - local[0], autre[1] - local[1]);
            if (d > 6.0 || !Chemins.direct(w.village.grille, local[0], local[1], autre[0], autre[1])) continue;
            w.interlocuteur = o;
            o.interlocuteur = w;
            relations.bavarder(w, o, jour());
            w.activite = Villageois.DISCUTER;
            o.activite = Villageois.DISCUTER;
            float duree = 20f + w.hasard.nextFloat() * 20f;
            w.reste = duree;
            o.reste = duree;
            o.corps.arreter();
            w.corps.arreter();
            if (d > 1.4) {
                double ux = (local[0] - autre[0]) / d, uy = (local[1] - autre[1]) / d;
                w.cibleA = autre[0] + ux * 1.1;
                w.cibleB = autre[1] + uy * 1.1;
                w.chemin = new float[] { (float) local[0], (float) local[1], (float) w.cibleA, (float) w.cibleB };
                w.etape = 1;
                w.enRoute = true;
                w.attentChemin = false;
            }
            w.regardA = autre[0];
            w.regardB = autre[1];
            o.regardA = local[0];
            o.regardB = local[1];
            w.gesteReste = 0f;
            o.gesteReste = 0f;
            return;
        }
    }

    private void finirDiscussion(Villageois w) {
        Villageois o = w.interlocuteur;
        w.interlocuteur = null;
        if (o != null && o.interlocuteur == w) {
            o.interlocuteur = null;
            o.reste = Math.min(o.reste, 0.1f);
        }
    }

    private void geste(Villageois w) {
        String[] noms;
        Villages.Village v = w.village;
        switch (w.activite) {
            case Villageois.REVEIL: noms = REVEIL; break;
            case Villageois.TRAVAIL: noms = w.travail >= 0 ? GESTES[v.metiers[w.travail]] : w.etal >= 0 && w.lieu == w.etal ? ETAL : DOMESTIQUE; break;
            case Villageois.MANGER: noms = w.lieu < 0 || w.lieu == w.maison ? REPAS_MAISON : REPAS; break;
            case Villageois.LOISIR: noms = LOISIRS; break;
            case Villageois.JOUER: noms = JEUX; break;
            case Villageois.PAUSE: noms = PAUSES; break;
            case Villageois.DISCUTER: noms = w.interlocuteur != null && w.hasard.nextBoolean() ? PAROLE : ECOUTE; break;
            default: return;
        }
        if (noms == null) noms = DOMESTIQUE;
        Animations.Animation[] choix = liste(noms);
        if (choix.length == 0) return;
        Animations.Animation a = choix[w.hasard.nextInt(choix.length)];
        if (w.activite == Villageois.DISCUTER && w.interlocuteur != null) {
            w.village.local(w.interlocuteur.corps.position[0], w.interlocuteur.corps.position[1], w.interlocuteur.corps.position[2], autre);
            regarder(w, autre[0], autre[1]);
            if (w.interlocuteur == w.conjoint && w.hasard.nextFloat() < 0.25f && w.interlocuteur.corps.partenaire == null) {
                Animations.Animation calin = Animations.toutes().get(w.hasard.nextBoolean() ? "calin" : "bisou_joue");
                if (calin != null) {
                    w.corps.jouer(calin, w.interlocuteur.corps);
                    w.gesteReste = 4f;
                    return;
                }
            }
        } else if (!Double.isNaN(w.regardA)) {
            regarder(w, w.regardA, w.regardB);
        }
        w.corps.jouer(a);
        w.gesteReste = a.mode == Animations.UNE_FOIS ? a.duree + 0.5f : 5f + w.hasard.nextFloat() * 8f;
    }

    private void regarder(Villageois w, double a, double b) {
        w.village.local(w.corps.position[0], w.corps.position[1], w.corps.position[2], local);
        double da = a - local[0], db = b - local[1];
        if (Math.hypot(da, db) > 0.05) w.corps.orienter(da, db);
    }

    private Animations.Animation[] liste(String[] noms) {
        String cle = String.join(",", noms);
        Animations.Animation[] l = listes.get(cle);
        if (l != null) return l;
        List<Animations.Animation> r = new ArrayList<>();
        for (String n : noms) {
            Animations.Animation a = Animations.toutes().get(n);
            if (a == null) System.out.println("animation inconnue pour les villageois : " + n);
            else if (!a.mobile && a.partenaire == null) r.add(a);
        }
        l = r.toArray(new Animations.Animation[0]);
        listes.put(cle, l);
        return l;
    }

    private void marcher(Villageois w, float delta) {
        Villages.Village v = w.village;
        if (w.attentChemin) {
            if (recherches <= 0) return;
            recherches--;
            w.attentChemin = false;
            v.local(w.corps.position[0], w.corps.position[1], w.corps.position[2], local);
            w.chemin = chemins.trouver(v.grille, local[0], local[1], w.cibleA, w.cibleB);
            recherchees++;
            if (w.chemin == null) echecs++;
            if (w.chemin == null || w.chemin.length < 4) w.chemin = new float[] { (float) local[0], (float) local[1], (float) w.cibleA, (float) w.cibleB };
            w.etape = 1;
        }
        if (w.chemin == null) {
            w.enRoute = false;
            return;
        }
        v.local(w.corps.position[0], w.corps.position[1], w.corps.position[2], local);
        float vitesse = w.activite == Villageois.FUIR ? 3.2f : w.activite == Villageois.TRAQUER ? 2.6f + (w.lien != null && w.lien.rival ? 0.1f * w.lien.niveau : 0f) : w.enfant ? 1.6f : VITESSE;
        float pas = vitesse * delta;
        while (true) {
            if (w.etape * 2 >= w.chemin.length) {
                arriver(w);
                return;
            }
            double da = w.chemin[w.etape * 2] - local[0], db = w.chemin[w.etape * 2 + 1] - local[1];
            double d = Math.hypot(da, db);
            if (d < 0.2) {
                w.etape++;
                continue;
            }
            double s = Math.min(d, pas);
            double avant0 = w.corps.position[0], avant1 = w.corps.position[1];
            w.corps.deplacer(da / d * s, db / d * s, delta, terrain, point);
            if (avant0 == w.corps.position[0] && avant1 == w.corps.position[1]) poser(w, w.chemin[w.etape * 2], w.chemin[w.etape * 2 + 1]);
            return;
        }
    }

    private void arriver(Villageois w) {
        w.enRoute = false;
        w.chemin = null;
        w.corps.deplacer(0.0, 0.0, 0f, terrain, point);
        if (w.activite == Villageois.FUIR) {
            w.corps.arreter();
            w.activite = Villageois.DORMIR;
            w.dedans = true;
            w.reste = 40f + w.hasard.nextFloat() * 40f;
            return;
        }
        if (w.activite == Villageois.TRAQUER) {
            w.cheminAttente = 0f;
            return;
        }
        if (w.activite == Villageois.DORMIR) {
            w.corps.arreter();
            w.dedans = true;
            w.reste = 5f;
            return;
        }
        w.gesteReste = 0f;
    }
}
