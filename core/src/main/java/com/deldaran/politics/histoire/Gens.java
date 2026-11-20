package com.deldaran.politics.histoire;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Lot H1 : la fiche de chaque habitant, comme celle du joueur.
 *
 * Un habitant, c'est son id dans la Foule : sa fiche (attributs, compétences, caractère, argent, sac) se déduit de cet id et de ce
 * que la Foule garde de lui (sexe, naissance, ville, caractère, talent, Maison). Le même id redonne toujours la même fiche : on ne
 * la garde pas. Seuls les gens importants (famille, proches, ouvriers du joueur…) gardent une fiche qui évolue ; c'est elle qu'on
 * sauvegarde, et elle seule.
 */
public final class Gens {
    /** L'âge (en années humaines) où l'on commence à travailler ; chaque année de métier donne GAIN_ANNEE points, jusqu'au plafond. */
    public static final int AGE_TRAVAIL = 14, GAIN_ANNEE = 4, ANNEES_MAX = 45;
    /** Les points d'attributs au-dessus du départ (2 partout) : POINTS_BASE, plus un par TALENT_POINT de talent. */
    public static final int POINTS_BASE = 3, TALENT_POINT = 34, ATTRIBUT_PNJ_MAX = 6;
    /** Un habitant sur LETTRE sait lire (les nobles, les maîtres d'école et les gens aisés davantage). */
    public static final float LETTRE = 0.15f;

    private final Joueur j;
    private final Population p;
    /** Les fiches qui évoluent, par id. */
    final Map<Long, Fiche> importants = new HashMap<>();
    /** Les lieux de travail fixés (un des points précis qu'on garde) : celui des parents vient de l'origine choisie à la création. */
    final Map<Long, Integer> lieux = new HashMap<>();

    Gens(Joueur j, Population p) {
        this.j = j;
        this.p = p;
    }

    /** La fiche d'un habitant : la sienne s'il est important, sinon tirée de son id. */
    public Fiche de(Foule f, int place) {
        Fiche x = importants.get(f.idDe(place));
        return x != null ? x : tirer(f, place);
    }

    /** L'habitant devient important : sa fiche est gardée, elle évoluera et sera sauvegardée. */
    public Fiche retenir(Foule f, int place) {
        long id = f.idDe(place);
        Fiche x = importants.get(id);
        if (x == null) {
            x = tirer(f, place);
            importants.put(id, x);
        }
        return x;
    }

    /** Fixe le lieu de travail d'un habitant (s'il n'en a pas déjà un) : sa fiche, et ce qu'on dit de lui, le suivent. */
    /** Lot I : l'année qu'il est, en jeu (l'année du monde, plus les jours passés depuis) : les gens vieillissent. */
    public int annee() {
        return Terre.annee(p, j.jourMaintenant());
    }

    /** Lot I2 : place un habitant à ce lieu de travail (un apprenti chez son maître), en remplaçant le précédent. */
    public void placer(long id, int lieu) {
        if (lieu >= 0) lieux.put(id, lieu);
    }

    public void fixerLieu(long id, int lieu) {
        if (lieu >= 0) lieux.putIfAbsent(id, lieu);
    }

    Integer lieuFixe(long id) {
        return lieux.get(id);
    }

    public boolean important(long id) {
        return importants.containsKey(id);
    }

    /** La Foule et la place d'un témoin, ou null s'il n'est plus dans le monde. */
    int[] place(Savoir.Temoin t) {
        if (t.peuple < 0 || t.region < 0 || t.peuple >= p.foules.length || t.region >= p.foules[t.peuple].length) return null;
        Foule f = p.foules[t.peuple][t.region];
        int q = f == null ? -1 : f.placeDe(t.id);
        return q < 0 ? null : new int[] { t.peuple, t.region, q };
    }

    /** L'âge en années humaines (les elfes et les nains vivent plus longtemps). */
    static float age(Foule f, int place, int annee) {
        return (annee - f.naissanceDe(place)) * f.peuple.rythme;
    }

    /** La fiche tirée de l'id : toujours la même pour le même habitant. */
    Fiche tirer(Foule f, int place) {
        long id = f.idDe(place);
        Random r = new Random(Travail.melanger(id * 0x9E3779B97F4A7C15L + p.graine));
        Fiche x = new Fiche();
        x.id = id;
        x.apparence = id;
        x.sexe = f.sexeDe(place);
        x.peuple = f.indicePeuple;
        x.region = f.region;
        x.ville = f.villeDe(place);
        x.naissance = f.naissanceDe(place);
        x.nom = Noms.personne(f.indicePeuple, p.graine, id, x.sexe);
        boolean noble = f.maisonDe(place) >= 0;
        int talent = Math.max(0, Math.min(100, f.talentDe(place)));
        float age = age(f, place, annee());
        int annees = (int) Math.max(0f, Math.min(ANNEES_MAX, age - AGE_TRAVAIL));
        int lieu = age < AGE_TRAVAIL ? -1 : Emplois.lieu(j, p, x.ville, id);
        int comp = lieu < 0 ? -1 : Emplois.competence(lieu);

        // Le caractère : ce que la Foule garde, plus la générosité et l'honnêteté tirées de l'id.
        x.axes[Fiche.LOI] = f.loiDe(place);
        x.axes[Fiche.BIEN] = f.bienDe(place);
        x.axes[Fiche.AMBITION] = f.ambitionDe(place);
        x.axes[Fiche.COURAGE] = f.courageDe(place);
        x.axes[Fiche.FOI] = f.foiDe(place);
        x.axes[Fiche.GENEROSITE] = caractere(r);
        x.axes[Fiche.HONNETETE] = caractere(r);

        // Les attributs : 2 partout, puis des points, la moitié dans l'attribut du métier.
        int points = POINTS_BASE + talent / TALENT_POINT + (age >= AGE_TRAVAIL ? 1 : 0);
        for (int k = 0; k < points; k++) {
            int a = comp >= 0 && k % 2 == 0 ? Fiche.attributDe(comp) : r.nextInt(Fiche.ATTRIBUTS);
            if (x.attributs[a] < ATTRIBUT_PNJ_MAX) x.attributs[a]++;
        }

        // Les compétences : le métier, plus ce que tout adulte sait faire.
        if (comp >= 0) {
            x.focus[comp] = Math.min(Fiche.FOCUS_MAX, 1 + annees / 15 + (talent > 70 ? 1 : 0));
            x.competences[comp] = Math.min(x.plafond(comp), 15 + annees * GAIN_ANNEE + talent / 3 + r.nextInt(20));
        }
        if (age >= AGE_TRAVAIL) {
            monter(x, Fiche.LABEUR, 10 + annees + r.nextInt(15));
            monter(x, Fiche.ATHLETISME, 5 + r.nextInt(20));
            monter(x, Fiche.COMMERCE, 3 + r.nextInt(12));
            if (x.sexe == 0 && x.axes[Fiche.COURAGE] > 20) monter(x, Fiche.UNE_MAIN, 10 + r.nextInt(30));
            if (x.sexe == 0 && x.axes[Fiche.COURAGE] > 20) monter(x, Fiche.HAST, 10 + r.nextInt(25));
            monter(x, Fiche.SURVIE, r.nextInt(25));
            monter(x, Fiche.ELOQUENCE, Math.max(0, x.axes[Fiche.AMBITION] / 4) + r.nextInt(15));
        }
        boolean lettre = noble || lieu == com.deldaran.politics.Metiers.ECOLE || r.nextFloat() < LETTRE * (1f + talent / 100f);
        int lettres = lettre ? 80 + r.nextInt(70) : r.nextInt(25);
        monter(x, Fiche.LETTRES, Math.round(lettres * Math.min(1f, age / AGE_TRAVAIL)));

        // L'argent (en cuivre) : selon le métier, l'âge et le rang.
        int base = lieu == Biens.CHAMPS || lieu == Biens.FORET || lieu == Biens.MINE || lieu < 0 ? 150
                : lieu == Biens.MARCHE || lieu == com.deldaran.politics.Metiers.AUBERGE || lieu == com.deldaran.politics.Metiers.TAVERNE
                        || lieu == com.deldaran.politics.Metiers.ENTREPOT || lieu == Biens.DEHORS ? 600 : 250;
        float richesse = (0.5f + r.nextFloat()) * (0.4f + annees / 25f) * (noble ? 12f : 1f) * (age < AGE_TRAVAIL ? 0.05f : 1f);
        x.argent = Math.round(base * richesse);

        // Le sac : de quoi manger, l'outil de son métier.
        x.sac.ajouter(Biens.PAIN.id, 1 + r.nextInt(3));
        Biens.Bien outil = outil(lieu, comp);
        if (outil != null) x.sac.ajouter(outil.id, 1);
        if (x.argent > 1500) x.sac.ajouter(Biens.CAPE.id, 1);
        return x;
    }

    private static void monter(Fiche x, int comp, int valeur) {
        x.competences[comp] = Math.max(x.competences[comp], Math.min(x.plafond(comp), valeur));
    }

    private static int caractere(Random r) {
        return (int) Math.max(-100f, Math.min(100f, (r.nextFloat() + r.nextFloat() - 1f) * 60f));
    }

    private static Biens.Bien outil(int lieu, int comp) {
        if (lieu == Biens.CHAMPS) return Biens.FAUCILLE;
        if (lieu == Biens.FORET) return Biens.HACHE;
        if (lieu == Biens.MINE) return Biens.PIC;
        if (lieu == com.deldaran.politics.Metiers.PECHERIE) return Biens.FILET;
        if (lieu == com.deldaran.politics.Metiers.BOUCHERIE) return Biens.COUTEAU_BOUCHER;
        if (comp == Fiche.FORGE) return Biens.MARTEAU;
        if (comp == Fiche.BOIS) return Biens.TROUSSE_BOIS;
        return lieu < 0 ? null : Biens.COUTEAU;
    }

    /** Ce qu'on remarque d'un habitant : son métier, ce qu'il sait le mieux faire, s'il sait lire, s'il est aisé. */
    public String portrait(Fiche x) {
        int meilleure = -1;
        for (int c = 0; c < Fiche.COMPETENCES; c++) if (meilleure < 0 || x.competences[c] > x.competences[meilleure]) meilleure = c;
        StringBuilder t = new StringBuilder();
        String il = x.sexe == 1 ? "Elle" : "Il";
        if (meilleure >= 0 && x.competences[meilleure] > 0)
            t.append(il).append(" excelle en ").append(Fiche.NOMS_COMPETENCES[meilleure].toLowerCase()).append(" (").append(x.competences[meilleure]).append(")");
        t.append(t.length() == 0 ? il + " " : ", ").append(x.competences[Fiche.LETTRES] >= Fiche.LECTURE ? "sait lire" : "ne sait pas lire");
        t.append(x.argent >= 3000 ? " et vit dans l'aisance" : x.argent >= 600 ? " et ne manque de rien" : x.argent >= 100 ? " et vit chichement" : " et n'a presque rien").append('.');
        return t.toString();
    }

    /** La meilleure compétence, en court : « Forge 85 ». */
    public static String meilleure(Fiche x) {
        int m = 0;
        for (int c = 1; c < Fiche.COMPETENCES; c++) if (x.competences[c] > x.competences[m]) m = c;
        return x.competences[m] <= 0 ? "" : Fiche.NOMS_COMPETENCES[m] + " " + x.competences[m];
    }

    // ================================================================ Lot H2 : les habitants d'une ville répartis dans son village

    /** Pour l'écran : les habitants d'une ville, leur maison, leur lieu de travail (bâtiment ou étal), leur conjoint. */
    public static final class Foyers {
        public int n;
        public long[] id = new long[0];
        public boolean[] femme = new boolean[0], enfant = new boolean[0], important = new boolean[0];
        public int[] maison = new int[0], travail = new int[0], etal = new int[0], conjoint = new int[0];
        public String[] nom = new String[0];
        /** Lot H4 : sa bourse (cuivre), son salaire (cuivre par heure ; 0 pour les ouvriers du joueur, que le joueur paie), ses heures de travail, s'il est trop vieux pour travailler. */
        public int[] argent = new int[0], debut = new int[0], fin = new int[0];
        public float[] salaire = new float[0];
        public boolean[] vieux = new boolean[0];
        /** Lot H5 : un ouvrier du joueur (son ouvrage, vu à l'écran, fait tourner l'atelier). */
        public boolean[] ouvrier = new boolean[0];
        /** Lot I1 : la famille du joueur qui vit chez lui (son conjoint s'ils vivent ensemble, ses enfants non mariés). */
        public boolean[] famille = new boolean[0];
    }

    /** Lot H4 : le salaire horaire (cuivre) : SALAIRE_BASE, plus la compétence du métier divisée par SALAIRE_COMPETENCE. */
    public static final float SALAIRE_BASE = 0.6f, SALAIRE_COMPETENCE = 200f;
    /** Lot H4 : ce que coûtent les repas d'une journée résumée (cuivre), et les heures d'une journée de travail. */
    public static final int REPAS_JOUR = 2, HEURES_JOUR = 8;

    /** Au-delà de cet âge (en années humaines), on ne travaille plus. */
    public static final int AGE_RETRAITE = 65;
    /** Les codes de lieux que l'écran passe pour chaque bâtiment : un étal de la place ; JOUEUR + métier pour un atelier du joueur. */
    public static final int LIEU_ETAL = -2, JOUEUR = 1000;

    /**
     * Lot H2 : répartit les habitants vivants de la ville dans le village.
     * - Le foyer : un couple marié et leurs enfants vivent sous le même toit ; chaque foyer tombe dans une maison (« logeable »)
     *   tirée de son id (toujours la même).
     * - Le travail : le lieu tiré de l'économie (Emplois) devient un bâtiment du village de ce métier ; les ouvriers du joueur vont
     *   à son atelier ; les marchands de la place tiennent les étals. Les bâtiments sans personne prennent des adultes sans emploi.
     * lieux[q] : métier du bâtiment q (≥ 1), JOUEUR + métier, LIEU_ETAL, ou -1.
     */
    public Foyers foyers(int ville, int[] lieux, boolean[] logeable) {
        Foyers r = new Foyers();
        if (ville < 0 || !p.villes.estVivante(ville)) return r;
        int q0 = p.villes.peuple[ville], r0 = p.villes.region[ville];
        Foule f = q0 >= 0 && r0 >= 0 && q0 < p.foules.length && r0 < p.foules[q0].length ? p.foules[q0][r0] : null;
        java.util.List<Integer> maisons = new java.util.ArrayList<>();
        for (int k = 0; k < logeable.length; k++) if (logeable[k]) maisons.add(k);
        if (f == null || maisons.isEmpty()) return r;
        java.util.List<Integer> places = new java.util.ArrayList<>();
        long moi = j.identifiant();
        for (int q = 0; q < f.taille(); q++) {
            long id = f.idDe(q);
            if (f.estEnVie(q, id) && f.villeDe(q) == ville && id != moi) places.add(q);
        }
        // Lot I1 : la famille qui vit sous le toit du joueur, dans sa maison de cette ville.
        java.util.Set<Long> chezMoi = famille(f);
        int maisonMoi = j.corps().maisonVille == ville ? j.corps().maisonBatiment : -1;
        if (maisonMoi >= logeable.length) maisonMoi = -1;
        if (maisonMoi >= 0) for (long id : chezMoi) {
            int q = f.placeDe(id);
            if (q >= 0 && f.estEnVie(q, id) && !places.contains(q)) places.add(q);
        }
        int n = places.size();
        r.n = n;
        r.id = new long[n];
        r.femme = new boolean[n];
        r.enfant = new boolean[n];
        r.important = new boolean[n];
        r.maison = new int[n];
        r.travail = new int[n];
        r.etal = new int[n];
        r.conjoint = new int[n];
        r.nom = new String[n];
        r.argent = new int[n];
        r.debut = new int[n];
        r.fin = new int[n];
        r.salaire = new float[n];
        r.vieux = new boolean[n];
        r.ouvrier = new boolean[n];
        r.famille = new boolean[n];
        Map<Integer, Integer> indice = new HashMap<>();
        for (int i = 0; i < n; i++) indice.put(places.get(i), i);
        java.util.Set<Long> proches = new java.util.HashSet<>();
        if (j.fiche() != null) for (Fiche.Proche x : j.fiche().proches) proches.add(x.id);
        // Les bâtiments du village, par code de lieu.
        Map<Integer, java.util.List<Integer>> batiments = new HashMap<>();
        java.util.List<Integer> etals = new java.util.ArrayList<>();
        for (int k = 0; k < lieux.length; k++) {
            if (lieux[k] == LIEU_ETAL) etals.add(k);
            else if (lieux[k] > 0) batiments.computeIfAbsent(lieux[k], c -> new java.util.ArrayList<>()).add(k);
        }
        java.util.List<Integer> marchands = new java.util.ArrayList<>(), sansEmploi = new java.util.ArrayList<>();
        int[] occupes = new int[lieux.length];
        for (int i = 0; i < n; i++) {
            int q = places.get(i);
            long id = f.idDe(q);
            r.id[i] = id;
            r.femme[i] = f.sexeDe(q) == 1;
            float age = age(f, q, annee());
            r.enfant[i] = age < AGE_TRAVAIL;
            r.nom[i] = Noms.personne(f.indicePeuple, p.graine, id, f.sexeDe(q));
            int c = f.conjointDe(q);
            Integer ci = c >= 0 ? indice.get(c) : null;
            r.conjoint[i] = ci == null ? -1 : ci;
            r.maison[i] = maisons.get((int) Long.remainderUnsigned(Travail.melanger(foyer(f, q, indice)), maisons.size()));
            r.famille[i] = maisonMoi >= 0 && chezMoi.contains(id);
            if (r.famille[i]) r.maison[i] = maisonMoi;
            r.travail[i] = -1;
            r.etal[i] = -1;
            Entreprise.Atelier employeur = Emplois.employeur(j, id);
            r.important[i] = important(id) || proches.contains(id) || employeur != null;
            // Lot H4 : sa bourse et ses heures.
            Fiche x = de(f, q);
            r.argent[i] = x.argent;
            r.vieux[i] = age > AGE_RETRAITE;
            r.debut[i] = Quotidien.ouverture(-1);
            r.fin[i] = Quotidien.fermeture(-1);
            if (r.enfant[i] || age > AGE_RETRAITE) continue;
            int lieu = Emplois.lieu(j, p, ville, id);
            r.debut[i] = Quotidien.ouverture(lieu);
            r.fin[i] = Quotidien.fermeture(lieu);
            r.salaire[i] = employeur != null ? 0f : SALAIRE_BASE + x.competences[Emplois.competence(lieu)] / SALAIRE_COMPETENCE;
            java.util.List<Integer> ou = employeur != null && employeur.ville == ville ? batiments.get(JOUEUR + lieu) : null;
            // Lot H5 : l'ouvrier du joueur travaille dans le bâtiment de son atelier.
            r.ouvrier[i] = employeur != null && employeur.ville == ville;
            if (r.ouvrier[i] && employeur.batiment >= 0 && employeur.batiment < lieux.length) ou = java.util.Collections.singletonList(employeur.batiment);
            if (ou == null) ou = batiments.get(lieu);
            if (ou != null) {
                r.travail[i] = ou.get((int) Long.remainderUnsigned(Travail.melanger(id + 7L), ou.size()));
                occupes[r.travail[i]]++;
            } else if (lieu == Biens.MARCHE) marchands.add(i);
            else sansEmploi.add(i);
        }
        // Les étals : les marchands de la place d'abord, puis des adultes sans emploi.
        sansEmploi.sort(java.util.Comparator.comparingLong(i -> Travail.melanger(r.id[i])));
        int e = 0;
        for (int i : marchands) {
            if (e < etals.size()) r.etal[i] = etals.get(e++);
            else sansEmploi.add(i);
        }
        int libre = 0;
        while (e < etals.size() && libre < sansEmploi.size()) r.etal[sansEmploi.get(libre++)] = etals.get(e++);
        // Les bâtiments où personne ne travaille : la caserne et le château en veulent trois, les autres un.
        for (Map.Entry<Integer, java.util.List<Integer>> b : batiments.entrySet()) {
            int m = b.getKey() >= JOUEUR ? b.getKey() - JOUEUR : b.getKey();
            for (int k : b.getValue()) {
                int voulus = (m == com.deldaran.politics.Metiers.CASERNE || m == com.deldaran.politics.Metiers.CHATEAU ? 3 : 1) - occupes[k];
                while (voulus-- > 0 && b.getKey() < JOUEUR && libre < sansEmploi.size()) {
                    int i = sansEmploi.get(libre++);
                    r.travail[i] = k;
                    r.debut[i] = Quotidien.ouverture(m);
                    r.fin[i] = Quotidien.fermeture(m);
                }
            }
        }
        return r;
    }

    // ================================================================ Lot H4 : la journée de chacun

    /** Ce qu'un habitant important a gagné ou dépensé sous les yeux du joueur (l'écran le rapporte) : sa fiche le garde. */
    public void depenser(long id, int cuivre) {
        Fiche x = importants.get(id);
        if (x != null) x.argent = Math.max(0, x.argent + cuivre);
    }

    private int dernierJour = -1;

    /**
     * Chaque jour, la journée RÉSUMÉE des gens importants qui ne sont pas sous les yeux du joueur : le salaire d'une journée de
     * travail, moins les repas. (Les notables ont la leur ; ceux qui ont un corps vivent la leur à l'écran.)
     */
    void avancer() {
        int jour = j.jourMaintenant();
        if (dernierJour < 0) dernierJour = jour;
        for (int d = Math.max(dernierJour, jour - 60) + 1; d <= jour; d++) journee(d);
        dernierJour = Math.max(dernierJour, jour);
    }

    void journee(int d) {
        java.util.Set<Long> visibles = new java.util.HashSet<>();
        long[] ici = j.gensIci;
        if (ici != null) for (long g : ici) visibles.add(g);
        java.util.Set<Long> notables = new java.util.HashSet<>();
        for (java.util.List<Long> l : j.notables().villes.values()) notables.addAll(l);
        for (Fiche x : importants.values()) {
            if (notables.contains(x.id)) continue;
            int[] q = place(x);
            if (q == null) continue;
            Foule f = p.foules[q[0]][q[1]];
            float age = age(f, q[2], annee());
            boolean employe = Emplois.employeur(j, x.id) != null;
            // Un ouvrier du joueur est payé par le joueur (Affaires) : on lui verse ici son salaire ; à l'écran, il ne gagne rien d'autre.
            if (visibles.contains(x.id) && !employe) continue;
            int gain = 0;
            if (age >= AGE_TRAVAIL && age <= AGE_RETRAITE) {
                int lieu = Emplois.lieu(j, p, x.ville, x.id);
                gain = Math.round(HEURES_JOUR * (SALAIRE_BASE + x.competences[Emplois.competence(lieu)] / SALAIRE_COMPETENCE));
            }
            int repas = visibles.contains(x.id) || age < AGE_TRAVAIL ? 0 : REPAS_JOUR;
            x.argent = Math.max(0, x.argent + gain - repas);
        }
    }

    /** La Foule et la place d'une fiche, ou null si l'habitant n'est plus. */
    private int[] place(Fiche x) {
        if (x.peuple < 0 || x.region < 0 || x.peuple >= p.foules.length || x.region >= p.foules[x.peuple].length) return null;
        Foule f = p.foules[x.peuple][x.region];
        int q = f == null ? -1 : f.placeDe(x.id);
        return q < 0 || !f.estEnVie(q, x.id) ? null : new int[] { x.peuple, x.region, q };
    }

    /** « Ses journées : debout vers 4 h, il travaille à la boulangerie de 3 h à 13 h, mange chez lui et passe ses soirées à la taverne. » */
    public String journee(Foule f, int q, Fiche x) {
        float age = age(f, q, annee());
        boolean femme = x.femme();
        String il = femme ? "elle" : "il";
        if (age < AGE_TRAVAIL) return " Ses journées : " + il + " joue dans les rues et aide à la maison.";
        Random r = new Random(Travail.melanger(x.id * 13L + 3L));
        String repas = x.argent >= 400 ? "mange à la taverne" : x.argent >= 60 ? "achète son pain à la boulangerie" : "mange chez " + (femme ? "elle" : "lui");
        String soir = x.argent >= 200 && r.nextFloat() < 0.6f ? "passe ses soirées à la taverne" : "passe ses soirées chez " + (femme ? "elle" : "lui");
        if (age > AGE_RETRAITE) return " Ses journées : " + il + " ne travaille plus, " + repas + " et " + soir + ".";
        int lieu = Emplois.lieu(j, p, x.ville, x.id);
        int debut = Quotidien.ouverture(lieu), fin = Quotidien.fermeture(lieu);
        int lever = Math.max(3, Math.min(8, debut - 1));
        return " Ses journées : debout vers " + lever + " h, " + il + " " + Emplois.phrase(lieu, Travail.melanger(x.id)) + " de " + debut + " h à " + fin + " h, " + repas
                + " et " + soir + ".";
    }

    /** Lot I1 : les ids de ceux qui vivent sous le toit du joueur : son conjoint s'ils vivent ensemble, ses enfants qui ne sont pas mariés. */
    java.util.Set<Long> famille(Foule f) {
        java.util.Set<Long> l = new java.util.HashSet<>();
        Famille fa = j.lignee();
        Social.Amour c = j.social().conjoint();
        if (c != null && fa.ensemble) l.add(c.graine);
        for (Famille.Enfant e : fa.enfants) {
            int q = f.placeDe(e.id);
            if (q >= 0 && f.conjointDe(q) < 0) l.add(e.id);
        }
        return l;
    }

    /** La clé du foyer : un couple marié partage la sienne ; un enfant vit chez ses parents. */
    private static long foyer(Foule f, int q, Map<Integer, Integer> indice) {
        int c = f.conjointDe(q);
        if (c >= 0 && indice.containsKey(c)) return Math.min(f.idDe(q), f.idDe(c));
        for (long parent : new long[] { f.pereDe(q), f.mereDe(q) }) {
            int pq = parent < 0 ? -1 : f.placeDe(parent);
            if (pq < 0 || !indice.containsKey(pq) || f.conjointDe(q) >= 0) continue;
            int pc = f.conjointDe(pq);
            return pc >= 0 && indice.containsKey(pc) ? Math.min(f.idDe(pq), f.idDe(pc)) : f.idDe(pq);
        }
        return f.idDe(q);
    }

    void ecrire(DataOutputStream s) throws IOException {
        s.writeInt(importants.size());
        for (Fiche x : importants.values()) x.ecrire(s);
        s.writeInt(lieux.size());
        for (Map.Entry<Long, Integer> e : lieux.entrySet()) {
            s.writeLong(e.getKey());
            s.writeInt(e.getValue());
        }
    }

    void lire(DataInputStream s) throws IOException {
        importants.clear();
        int n = s.readInt();
        for (int k = 0; k < n; k++) {
            Fiche x = Fiche.lire(s, true);
            importants.put(x.id, x);
        }
        lieux.clear();
        n = s.readInt();
        for (int k = 0; k < n; k++) lieux.put(s.readLong(), s.readInt());
    }

    /** Essai du lot H1 : les fiches gardées. */
    public java.util.Collection<Fiche> importantsPourEssai() {
        return importants.values();
    }

    /** Essai du lot H1 : la fiche de quelques habitants de la ville (tirée deux fois : elle doit être la même). */
    public String essai(int ville, int n) {
        Foule f = p.foules[p.villes.peuple[ville]][p.villes.region[ville]];
        StringBuilder t = new StringBuilder();
        for (int k = 0; k < n; k++) {
            int q = f.habitantAuHasard(ville, p.annee, 0, Travail.melanger(k * 7919L + 13L));
            if (q < 0) continue;
            Fiche a = tirer(f, q), b = tirer(f, q);
            boolean pareil = a.argent == b.argent && java.util.Arrays.equals(a.competences, b.competences) && java.util.Arrays.equals(a.attributs, b.attributs);
            t.append(a.nom).append(", ").append(Math.round(age(f, q, p.annee))).append(" ans (humains) — ").append(meilleure(a)).append(" ; attributs ")
                    .append(java.util.Arrays.toString(a.attributs)).append(" ; ").append(Fiche.argentTexte(a.argent)).append(" ; sac ").append(a.sac.piles.size())
                    .append(" piles ; ").append(Emplois.metier(j, p, ville, a.id)).append(pareil ? " ; même fiche au second tirage" : " ; FICHE DIFFÉRENTE").append('\n');
        }
        return t.toString();
    }
}
