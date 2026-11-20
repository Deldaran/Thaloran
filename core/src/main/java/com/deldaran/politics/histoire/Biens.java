package com.deldaran.politics.histoire;

import com.deldaran.politics.Metiers;
import com.deldaran.politics.Tenue;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Biens {
    public static final int VIVRES = 0, MATIERE = 1, OUTIL = 2, ARME = 3, ARMURE = 4, VETEMENT = 5, MONTURE = 6, BETAIL = 7, LIVRE = 8, LUXE = 9, CATEGORIES = 10;
    public static final String[] NOMS_CATEGORIES = { "Vivres", "Matières", "Outils", "Armes", "Armures", "Vêtements", "Montures", "Bétail", "Livres", "Luxe" };

    public static final int MAIN = 0, GAUCHE = 1, CORPS = 2, TETE = 3, PIEDS = 4, DOS = 5, SAC_DOS = 6, CARQUOIS = 7, BETE = 8, PLACES = 9;
    public static final String[] NOMS_PLACES = { "en main", "main gauche", "sur le corps", "sur la tête", "aux pieds", "sur le dos", "sac au dos", "à la ceinture", "monture" };

    /** Lieux de commerce qui ne sont pas des ateliers de Metiers : la place du marché, les fermes, les bûcherons, la mine, les colporteurs (importations). */
    public static final int MARCHE = 100, CHAMPS = 101, FORET = 102, MINE = 103, DEHORS = 104;

    public static final int[] PIECES_EQUIPEMENT = { Tenue.EPEE, Tenue.OUTIL, Tenue.ARC, Tenue.CARQUOIS, Tenue.BOUCLIER, Tenue.LANTERNE, Tenue.HACHE, Tenue.LANCE, Tenue.DAGUE,
            Tenue.CASQUE, Tenue.MAILLE, Tenue.BATON, Tenue.PLASTRON, Tenue.SAC };

    public static final class Bien {
        public final int id;
        public final String nom, pluriel;
        public final boolean feminin;
        public final int categorie, prix;
        public final float poids;
        public final int groupe;
        public final float part;
        public final int[] vendeurs;
        public int piece = -1, couleur = -1, place = -1, competence = -1, valeur, charge;

        Bien(int id, String nom, String pluriel, boolean feminin, int categorie, int prix, float poids, int groupe, float part, int... vendeurs) {
            this.id = id;
            this.nom = nom;
            this.pluriel = pluriel;
            this.feminin = feminin;
            this.categorie = categorie;
            this.prix = prix;
            this.poids = poids;
            this.groupe = groupe;
            this.part = part;
            this.vendeurs = vendeurs;
        }

        Bien porte(int place, int piece, int couleur) {
            this.place = place;
            this.piece = piece;
            this.couleur = couleur;
            return this;
        }

        Bien comp(int competence) {
            this.competence = competence;
            return this;
        }

        Bien val(int valeur) {
            this.valeur = valeur;
            return this;
        }

        Bien charge(int kilos) {
            this.charge = kilos;
            return this;
        }

        public boolean venduPar(int metier) {
            for (int v : vendeurs) if (v == metier) return true;
            return false;
        }

        public String un() {
            return (feminin ? "une " : "un ") + nom;
        }

        public String quantite(int n) {
            if (n == 1) return un();
            return n + " " + pluriel;
        }

        public String majuscule() {
            return Character.toUpperCase(nom.charAt(0)) + nom.substring(1);
        }
    }

    private static final List<Bien> LISTE = new ArrayList<>();
    public static final List<Bien> TOUS = Collections.unmodifiableList(LISTE);

    private static Bien b(String nom, String pluriel, boolean f, int cat, int prix, float poids, int groupe, float part, int... vendeurs) {
        Bien x = new Bien(LISTE.size(), nom, pluriel, f, cat, prix, poids, groupe, part, vendeurs);
        LISTE.add(x);
        return x;
    }

    private static final int N = Economie.NOURRITURE, BO = Economie.BOIS, MI = Economie.MINERAI;
    private static final int BOUL = Metiers.BOULANGERIE, BOUC = Metiers.BOUCHERIE, MOUL = Metiers.MOULIN, GREN = Metiers.GRENIER, FORG = Metiers.FORGE,
            MENU = Metiers.MENUISERIE, TANN = Metiers.TANNERIE, TISS = Metiers.TISSERAND, POT = Metiers.POTIER, AUB = Metiers.AUBERGE, TAV = Metiers.TAVERNE,
            ENTR = Metiers.ENTREPOT, PECH = Metiers.PECHERIE, ECOL = Metiers.ECOLE;

    // Vivres
    public static final Bien PAIN = b("pain", "pains", false, VIVRES, 1, 0.5f, N, 1f, BOUL, MARCHE);
    public static final Bien GALETTE = b("galette de voyage", "galettes de voyage", true, VIVRES, 2, 0.3f, N, 1f, BOUL);
    public static final Bien FARINE = b("sac de farine", "sacs de farine", false, VIVRES, 12, 10f, N, 1f, MOUL, GREN, MARCHE);
    public static final Bien GRAIN = b("sac de grain", "sacs de grain", false, VIVRES, 8, 10f, N, 1f, GREN, MARCHE);
    public static final Bien VIANDE = b("morceau de viande salée", "morceaux de viande salée", false, VIVRES, 4, 0.5f, N, 1f, BOUC, MARCHE);
    public static final Bien JAMBON = b("jambon", "jambons", false, VIVRES, 25, 4f, N, 1f, BOUC);
    public static final Bien POISSON = b("poisson séché", "poissons séchés", false, VIVRES, 3, 0.4f, N, 1f, PECH, MARCHE);
    public static final Bien FROMAGE = b("fromage", "fromages", false, VIVRES, 6, 1f, N, 1f, MARCHE);
    public static final Bien LEGUMES = b("panier de légumes", "paniers de légumes", false, VIVRES, 3, 3f, N, 1f, MARCHE);
    public static final Bien MIEL = b("pot de miel", "pots de miel", false, VIVRES, 10, 1f, N, 0.6f, MARCHE);
    public static final Bien BIERE = b("cruche de bière", "cruches de bière", true, VIVRES, 2, 2f, N, 0.8f, TAV, AUB);
    public static final Bien VIN = b("bouteille de vin", "bouteilles de vin", true, VIVRES, 8, 1.5f, N, 0.6f, AUB, TAV, ENTR);
    public static final Bien SEL = b("sac de sel", "sacs de sel", false, VIVRES, 20, 5f, -1, 0f, ENTR, MARCHE);
    // Matières
    public static final Bien FAGOT = b("fagot de bois", "fagots de bois", false, MATIERE, 2, 10f, BO, 1f, MENU, MARCHE);
    public static final Bien PLANCHE = b("planche", "planches", true, MATIERE, 4, 6f, BO, 1f, MENU);
    public static final Bien FER = b("lingot de fer", "lingots de fer", false, MATIERE, 30, 3f, MI, 1f, FORG, ENTR);
    public static final Bien CHARBON = b("sac de charbon", "sacs de charbon", false, MATIERE, 10, 15f, BO, 0.8f, FORG);
    public static final Bien PEAU = b("peau tannée", "peaux tannées", true, MATIERE, 20, 2f, -1, 0f, TANN);
    public static final Bien DRAP = b("rouleau de drap", "rouleaux de drap", false, MATIERE, 40, 4f, -1, 0f, TISS);
    public static final Bien LAINE = b("pelote de laine", "pelotes de laine", true, MATIERE, 5, 0.5f, -1, 0f, TISS, MARCHE);
    public static final Bien POT_TERRE = b("pot de terre", "pots de terre", false, MATIERE, 3, 1.5f, -1, 0f, POT, MARCHE);
    public static final Bien CORDE = b("corde", "cordes", true, MATIERE, 6, 2f, -1, 0f, MARCHE, ENTR);
    public static final Bien CHANDELLE = b("chandelle", "chandelles", true, MATIERE, 1, 0.1f, -1, 0f, MARCHE);
    // Outils
    public static final Bien COUTEAU = b("couteau", "couteaux", false, OUTIL, 12, 0.3f, MI, 0.4f, FORG, MARCHE).porte(MAIN, Tenue.DAGUE, 5).comp(Fiche.UNE_MAIN).val(1);
    public static final Bien COUTEAU_BOUCHER = b("couteau de boucher", "couteaux de boucher", false, OUTIL, 15, 0.4f, MI, 0.4f, BOUC, FORG).porte(MAIN, Tenue.DAGUE, 5).comp(Fiche.UNE_MAIN).val(1);
    public static final Bien HACHE = b("hache", "haches", true, OUTIL, 40, 2f, MI, 0.4f, FORG).porte(MAIN, Tenue.HACHE, 9).comp(Fiche.UNE_MAIN).val(3);
    public static final Bien MARTEAU = b("marteau de forge", "marteaux de forge", false, OUTIL, 35, 2f, MI, 0.4f, FORG).porte(MAIN, Tenue.OUTIL, 9).comp(Fiche.FORGE).val(2);
    public static final Bien PIC = b("pic", "pics", false, OUTIL, 30, 3f, MI, 0.4f, FORG).porte(MAIN, Tenue.OUTIL, 9).comp(Fiche.LABEUR).val(2);
    public static final Bien FAUCILLE = b("faucille", "faucilles", true, OUTIL, 15, 0.5f, MI, 0.4f, FORG, MARCHE).porte(MAIN, Tenue.OUTIL, 9).comp(Fiche.LABEUR).val(1);
    public static final Bien TROUSSE_BOIS = b("trousse de menuisier", "trousses de menuisier", true, OUTIL, 60, 4f, MI, 0.3f, MENU, FORG).porte(MAIN, Tenue.OUTIL, 9).comp(Fiche.BOIS);
    public static final Bien FILET = b("filet de pêche", "filets de pêche", false, OUTIL, 25, 3f, -1, 0f, PECH, MARCHE).comp(Fiche.SURVIE);
    public static final Bien LANTERNE = b("lanterne", "lanternes", true, OUTIL, 20, 1f, MI, 0.3f, FORG, ENTR).porte(GAUCHE, Tenue.LANTERNE, 5);
    public static final Bien SAC_A_DOS = b("sac à dos", "sacs à dos", false, OUTIL, 25, 1f, -1, 0f, TANN).porte(SAC_DOS, Tenue.SAC, 9).charge(15);
    public static final Bien BOURSE = b("bourse de cuir", "bourses de cuir", true, OUTIL, 8, 0.1f, -1, 0f, TANN);
    public static final Bien SOINS = b("trousse de soins", "trousses de soins", true, OUTIL, 30, 1f, -1, 0f, ECOL, MARCHE).comp(Fiche.MEDECINE);
    // Armes
    public static final Bien DAGUE = b("dague", "dagues", true, ARME, 40, 0.4f, MI, 0.4f, FORG).porte(MAIN, Tenue.DAGUE, 5).comp(Fiche.UNE_MAIN).val(3);
    public static final Bien EPEE = b("épée", "épées", true, ARME, 600, 1.5f, MI, 0.4f, FORG).porte(MAIN, Tenue.EPEE, 5).comp(Fiche.UNE_MAIN).val(6);
    public static final Bien EPEE_LONGUE = b("épée longue", "épées longues", true, ARME, 1200, 2.5f, MI, 0.4f, FORG).porte(MAIN, Tenue.EPEE, 5).comp(Fiche.DEUX_MAINS).val(8);
    public static final Bien HACHE_GUERRE = b("hache de guerre", "haches de guerre", true, ARME, 300, 2.5f, MI, 0.4f, FORG).porte(MAIN, Tenue.HACHE, 5).comp(Fiche.DEUX_MAINS).val(7);
    public static final Bien MASSE = b("masse d'armes", "masses d'armes", true, ARME, 250, 2.5f, MI, 0.4f, FORG).porte(MAIN, Tenue.BATON, 5).comp(Fiche.UNE_MAIN).val(5);
    public static final Bien BATON = b("bâton", "bâtons", false, ARME, 3, 1.5f, BO, 1f, MENU, MARCHE).porte(MAIN, Tenue.BATON, 9).comp(Fiche.HAST).val(2);
    public static final Bien LANCE = b("lance", "lances", true, ARME, 80, 2.5f, BO, 0.5f, FORG, MENU).porte(MAIN, Tenue.LANCE, 9).comp(Fiche.HAST).val(5);
    public static final Bien HALLEBARDE = b("hallebarde", "hallebardes", true, ARME, 400, 3.5f, MI, 0.4f, FORG).porte(MAIN, Tenue.LANCE, 5).comp(Fiche.HAST).val(8);
    public static final Bien JAVELOTS = b("lot de javelots", "lots de javelots", false, ARME, 60, 3f, BO, 0.5f, FORG, MENU).porte(MAIN, Tenue.LANCE, 9).comp(Fiche.JET).val(4);
    public static final Bien ARC = b("arc", "arcs", false, ARME, 120, 1f, BO, 0.5f, MENU).porte(GAUCHE, Tenue.ARC, 9).comp(Fiche.ARC).val(5);
    public static final Bien ARC_LONG = b("arc long", "arcs longs", false, ARME, 300, 1.2f, BO, 0.5f, MENU).porte(GAUCHE, Tenue.ARC, 9).comp(Fiche.ARC).val(7);
    public static final Bien ARBALETE = b("arbalète", "arbalètes", true, ARME, 500, 4f, MI, 0.4f, FORG).porte(GAUCHE, Tenue.ARC, 5).comp(Fiche.ARBALETE).val(8);
    public static final Bien FLECHES = b("carquois de flèches", "carquois de flèches", false, ARME, 30, 1f, BO, 0.5f, MENU).porte(CARQUOIS, Tenue.CARQUOIS, 9);
    public static final Bien BOUCLIER = b("bouclier rond", "boucliers ronds", false, ARME, 80, 4f, BO, 0.5f, MENU).porte(GAUCHE, Tenue.BOUCLIER, 9).val(3);
    public static final Bien ECU = b("écu ferré", "écus ferrés", false, ARME, 250, 6f, MI, 0.4f, FORG).porte(GAUCHE, Tenue.BOUCLIER, 5).val(5);
    // Armures et vêtements
    public static final Bien GAMBISON = b("gambison", "gambisons", false, ARMURE, 150, 4f, -1, 0f, TISS).porte(CORPS, Tenue.PLASTRON, 13).val(3);
    public static final Bien CUIR_BOUILLI = b("cuirasse de cuir", "cuirasses de cuir", true, ARMURE, 250, 5f, -1, 0f, TANN).porte(CORPS, Tenue.PLASTRON, 2).val(4);
    public static final Bien BROIGNE = b("broigne", "broignes", true, ARMURE, 600, 9f, MI, 0.3f, FORG, TANN).porte(CORPS, Tenue.MAILLE, 2).val(5);
    public static final Bien MAILLES = b("cotte de mailles", "cottes de mailles", true, ARMURE, 1500, 12f, MI, 0.4f, FORG).porte(CORPS, Tenue.MAILLE, 5).val(7);
    public static final Bien CUIRASSE = b("cuirasse d'acier", "cuirasses d'acier", true, ARMURE, 4000, 15f, MI, 0.4f, FORG).porte(CORPS, Tenue.PLASTRON, 5).val(9);
    public static final Bien CALOTTE = b("calotte de cuir", "calottes de cuir", true, ARMURE, 30, 0.8f, -1, 0f, TANN).porte(TETE, Tenue.CASQUE, 2).val(1);
    public static final Bien CASQUE = b("casque", "casques", false, ARMURE, 200, 2f, MI, 0.4f, FORG).porte(TETE, Tenue.CASQUE, 5).val(3);
    public static final Bien BOTTES = b("paire de bottes", "paires de bottes", true, VETEMENT, 30, 1.5f, -1, 0f, TANN).porte(PIEDS, Tenue.BOTTES, 9).val(1);
    public static final Bien CAPE = b("cape", "capes", true, VETEMENT, 40, 1.5f, -1, 0f, TISS).porte(DOS, Tenue.CAPE, 4);
    public static final Bien CAPE_DOUBLEE = b("cape doublée", "capes doublées", true, VETEMENT, 120, 2f, -1, 0f, TISS).porte(DOS, Tenue.CAPE, 13);
    // Montures et bétail
    public static final Bien ANE = b("âne", "ânes", false, MONTURE, 300, 0f, N, 0.3f, MARCHE).porte(BETE, -1, -1).comp(Fiche.EQUITATION).charge(50);
    public static final Bien MULE = b("mule", "mules", true, MONTURE, 800, 0f, N, 0.3f, ENTR, MARCHE).porte(BETE, -1, -1).comp(Fiche.EQUITATION).charge(80);
    public static final Bien CHEVAL = b("cheval de selle", "chevaux de selle", false, MONTURE, 2500, 0f, N, 0.3f, ENTR).porte(BETE, -1, -1).comp(Fiche.EQUITATION).charge(50);
    public static final Bien DESTRIER = b("destrier", "destriers", false, MONTURE, 12000, 0f, N, 0.3f, ENTR).porte(BETE, -1, -1).comp(Fiche.EQUITATION).charge(60).val(5);
    public static final Bien CHEVRE = b("chèvre", "chèvres", true, BETAIL, 60, 0f, N, 0.5f, MARCHE);
    public static final Bien POULE = b("poule", "poules", true, BETAIL, 8, 0f, N, 0.5f, MARCHE);
    // Livres et luxe
    public static final Bien OUVRAGE = b("livre", "livres", false, LIVRE, 300, 1f, -1, 0f, ECOL).comp(Fiche.LETTRES);
    public static final Bien PRIERES = b("livre de prières", "livres de prières", false, LIVRE, 200, 0.8f, -1, 0f, ECOL).comp(Fiche.LETTRES);
    public static final Bien ECRITOIRE = b("nécessaire d'écriture", "nécessaires d'écriture", false, LIVRE, 40, 0.5f, -1, 0f, ECOL);
    public static final Bien CHEVALIERE = b("chevalière", "chevalières", true, LUXE, 500, 0.05f, -1, 0f, ENTR);
    public static final Bien BIJOU = b("bijou", "bijoux", false, LUXE, 400, 0.1f, -1, 0f, ENTR);
    public static final Bien EPICES = b("sachet d'épices", "sachets d'épices", false, LUXE, 150, 0.2f, -1, 0f, ENTR);
    // Biens intermédiaires de la phase 3 (ajoutés à la fin pour garder les numéros des sauvegardes)
    public static final Bien MINERAI = b("sac de minerai", "sacs de minerai", false, MATIERE, 12, 20f, MI, 1f, ENTR);
    public static final Bien BILLE = b("bille de bois", "billes de bois", true, MATIERE, 6, 30f, BO, 1f);
    public static final Bien PORC = b("porc", "porcs", false, BETAIL, 120, 0f, N, 1f, MARCHE);
    public static final Bien PEAU_BRUTE = b("peau brute", "peaux brutes", true, MATIERE, 6, 3f, N, 0.5f);
    // Phase 11 : la ferme.
    public static final Bien OEUFS = b("douzaine d'œufs", "douzaines d'œufs", true, VIVRES, 2, 0.7f, N, 0.6f, MARCHE);
    public static final Bien MOUTON = b("mouton", "moutons", false, BETAIL, 50, 0f, N, 0.5f, MARCHE);

    public static Bien get(int id) {
        return id >= 0 && id < LISTE.size() ? LISTE.get(id) : null;
    }

    private static final Map<String, Bien> ALIAS = new HashMap<>();
    private static final Map<String, Integer> NOMBRES = new HashMap<>();

    static {
        for (Bien x : LISTE) {
            ALIAS.put(normaliser(x.nom), x);
            ALIAS.put(normaliser(x.pluriel), x);
        }
        ALIAS.put("bottes de cuir", BOTTES);
        ALIAS.put("bottes", BOTTES);
        ALIAS.put("filets", FILET);
        ALIAS.put("filet", FILET);
        ALIAS.put("outils de menuisier", TROUSSE_BOIS);
        ALIAS.put("bonne cape", CAPE_DOUBLEE);
        ALIAS.put("cheval", CHEVAL);
        String[] mots = { "un", "une", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf", "dix" };
        for (int k = 0; k < mots.length; k++) NOMBRES.put(mots[k], Math.max(1, k));
        NOMBRES.put("des", 1);
        NOMBRES.put("le", 1);
        NOMBRES.put("la", 1);
        NOMBRES.put("l'", 1);
    }

    static String normaliser(String s) {
        return Normalizer.normalize(s.toLowerCase(Locale.ROOT).trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    /** Traduit un objet écrit en toutes lettres (« trois livres », « une mule ») en { bien, quantité }, ou null si ce n'est pas un bien transportable. */
    public static int[] reconnaitre(String texte) {
        String t = normaliser(texte);
        int n = 1;
        int espace = t.indexOf(' ');
        if (espace > 0) {
            Integer q = NOMBRES.get(t.substring(0, espace));
            if (q != null) {
                n = q;
                t = t.substring(espace + 1).trim();
            }
        }
        Bien x = ALIAS.get(t);
        return x == null ? null : new int[] { x.id, n };
    }

    /** Remplace les armes, armures et sacs d'une tenue par ce que le personnage porte vraiment. */
    public static Tenue habiller(Tenue t, int[] portes) {
        for (int piece : PIECES_EQUIPEMENT) t.enlever(piece);
        if (portes == null) return t;
        for (int id : portes) {
            Bien x = get(id);
            if (x == null || x.piece < 0) continue;
            t.mettre(x.piece, x.couleur);
        }
        return t;
    }

    public static String description(Bien x) {
        StringBuilder s = new StringBuilder(x.majuscule()).append(". ").append(NOMS_CATEGORIES[x.categorie]);
        if (x.poids > 0f) s.append(", ").append(poids(x.poids));
        s.append(". Vaut d'ordinaire ").append(Fiche.argentTexte(x.prix)).append('.');
        if (x.place >= 0) s.append("\nSe porte ").append(NOMS_PLACES[x.place]).append('.');
        if (x.categorie == ARME && x.valeur > 0) s.append(" Force au combat : ").append(x.valeur).append('.');
        if (x.categorie == ARMURE && x.valeur > 0) s.append(" Protection : ").append(x.valeur).append('.');
        if (x.charge > 0) s.append(" Permet de porter ").append(x.charge).append(" kg de plus.");
        if (x.competence >= 0) s.append(" Compétence : ").append(Fiche.NOMS_COMPETENCES[x.competence]).append('.');
        return s.toString();
    }

    public static String poids(float kg) {
        if (kg < 1f) return Math.round(kg * 1000f) + " g";
        return (kg == Math.rint(kg) ? String.valueOf((int) kg) : String.format(Locale.FRANCE, "%.1f", kg)) + " kg";
    }

    public static String nomBoutique(int metier) {
        switch (metier) {
            case MARCHE: return "Le marché";
            case CHAMPS: return "Les fermes";
            case FORET: return "Les bûcherons";
            case MINE: return "La mine";
            case DEHORS: return "Les colporteurs";
            case Metiers.BOULANGERIE: return "La boulangerie";
            case Metiers.BOUCHERIE: return "La boucherie";
            case Metiers.MOULIN: return "Le moulin";
            case Metiers.GRENIER: return "Le grenier";
            case Metiers.FORGE: return "La forge";
            case Metiers.MENUISERIE: return "La menuiserie";
            case Metiers.TANNERIE: return "La tannerie";
            case Metiers.TISSERAND: return "Le tisserand";
            case Metiers.POTIER: return "Le potier";
            case Metiers.AUBERGE: return "L'auberge";
            case Metiers.TAVERNE: return "La taverne";
            case Metiers.ENTREPOT: return "L'entrepôt du marchand";
            case Metiers.PECHERIE: return "La pêcherie";
            case Metiers.ECOLE: return "L'école";
            default: return Metiers.NOMS[metier];
        }
    }

    private Biens() {
    }
}
