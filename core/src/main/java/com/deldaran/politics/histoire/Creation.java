package com.deldaran.politics.histoire;

import com.deldaran.politics.Metiers;
import com.deldaran.politics.histoire.Joueur.Scene;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public final class Creation {
    private static final int PAGE = 8;
    private static final int[] AGES = { 20, 30, 40, 50 };
    private static final int AUCUNE = Integer.MIN_VALUE;

    public static final class Ajout {
        public long id, pere = -1L, mere = -1L, conjoint = -1L;
        public int naissance, sexe, talent, ville = -1, maison = -1, rang;
        public int[] caractere = new int[5];

        public void ecrire(DataOutputStream s) throws IOException {
            s.writeLong(id);
            s.writeLong(pere);
            s.writeLong(mere);
            s.writeLong(conjoint);
            s.writeInt(naissance);
            s.writeByte(sexe);
            s.writeByte(talent);
            s.writeInt(ville);
            s.writeInt(maison);
            s.writeByte(rang);
            for (int c : caractere) s.writeByte(c);
        }

        public static Ajout lire(DataInputStream s) throws IOException {
            Ajout a = new Ajout();
            a.id = s.readLong();
            a.pere = s.readLong();
            a.mere = s.readLong();
            a.conjoint = s.readLong();
            a.naissance = s.readInt();
            a.sexe = s.readByte();
            a.talent = s.readByte();
            a.ville = s.readInt();
            a.maison = s.readInt();
            a.rang = s.readByte();
            for (int k = 0; k < 5; k++) a.caractere[k] = s.readByte();
            return a;
        }

        public int inserer(Foule f) {
            return f.inserer(id, pere, mere, naissance, sexe, caractere, talent, ville, maison, rang);
        }
    }

    static final class Effet {
        final List<int[]> gains = new ArrayList<>();
        final List<Integer> focus = new ArrayList<>();
        final List<String> objets = new ArrayList<>();
        final int[] axes = new int[Fiche.AXES];
        int attribut = -1, argent, renom;

        Effet c(int competence, int points) {
            gains.add(new int[] { competence, points });
            return this;
        }

        Effet f(int competence) {
            focus.add(competence);
            return this;
        }

        Effet a(int a) {
            attribut = a;
            return this;
        }

        Effet or(int cuivre) {
            argent += cuivre;
            return this;
        }

        Effet o(String... noms) {
            for (String n : noms) objets.add(n);
            return this;
        }

        Effet x(int axe, int valeur) {
            axes[axe] += valeur;
            return this;
        }

        Effet r(int points) {
            renom += points;
            return this;
        }

        void appliquer(Fiche fiche, float part) {
            for (int[] g : gains) fiche.gagner(g[0], Math.round(g[1] * part));
            for (int c : focus) fiche.concentrer(c);
            if (attribut >= 0) fiche.augmenter(attribut, 1);
            for (int k = 0; k < axes.length; k++) fiche.pencher(k, Math.round(axes[k] * part));
            fiche.argent += Math.round(argent * part);
            fiche.renom += Math.round(renom * part);
            for (String o : objets) fiche.recevoir(o);
        }

        String apport() {
            List<String> l = new ArrayList<>();
            for (int[] g : gains) l.add(Fiche.NOMS_COMPETENCES[g[0]] + " +" + g[1]);
            if (attribut >= 0) l.add(Fiche.NOMS_ATTRIBUTS[attribut] + " +1");
            return String.join(", ", l);
        }
    }

    private static final class Origine {
        final String nom, phrase, apport;
        final int metier, competence;
        final Effet effet;

        Origine(String nom, String phrase, String apport, int metier, int competence, Effet effet) {
            this.nom = nom;
            this.phrase = phrase;
            this.apport = apport;
            this.metier = metier;
            this.competence = competence;
            this.effet = effet;
        }
    }

    private static final int PAYSANS = 0, BUCHERONS = 1, MINEURS = 2, PECHEURS = 3, FORGERONS = 4, MENUISIERS = 5, TISSERANDS = 6, POTIERS = 7, TANNEURS = 8,
            BOULANGERS = 9, BOUCHERS = 10, MEUNIERS = 11, AUBERGISTES = 12, MARCHANDS = 13, SOLDATS = 14, LETTRES = 15, NOBLES = 16, HORS_LA_LOI = 17, ORPHELIN = 18;
    /** Lot G4 : le métier d'un père ou d'une mère (masculin, féminin), dans l'ordre des ORIGINES ; null pour l'orphelin. */
    static final String[][] METIERS_PARENTS = {
        { "paysan", "paysanne" }, { "bûcheron", "bûcheronne" }, { "mineur", "mineuse" }, { "pêcheur", "pêcheuse" },
        { "forgeron", "forgeronne" }, { "menuisier", "menuisière" }, { "tisserand", "tisserande" }, { "potier", "potière" },
        { "tanneur", "tanneuse" }, { "boulanger", "boulangère" }, { "boucher", "bouchère" }, { "meunier", "meunière" },
        { "aubergiste", "aubergiste" }, { "marchand", "marchande" }, { "soldat", "soldate" }, { "lettré", "lettrée" },
        { "de petite noblesse", "de petite noblesse" }, { "hors-la-loi", "hors-la-loi" }, null,
    };

    /** Lot H1 : le lieu de travail des parents, selon l'origine (-1 : aucun lieu fixe). */
    static int lieuOrigine(int o) {
        if (o < 0 || o >= ORIGINES.length) return -1;
        if (ORIGINES[o].metier >= 0) return ORIGINES[o].metier;
        return o == 0 ? Biens.CHAMPS : o == 1 ? Biens.FORET : o == 2 ? Biens.MINE : -1;
    }

    private static final Origine[] ORIGINES = {
        new Origine("Paysans", "travaillaient la terre", "la terre et les bêtes, peu d'argent", -1, Fiche.LABEUR,
            new Effet().c(Fiche.LABEUR, 20).c(Fiche.SURVIE, 10).or(30).o("une faucille").x(Fiche.HONNETETE, 5)),
        new Origine("Bûcherons", "vivaient du bois de la forêt", "la hache et la forêt", -1, Fiche.DEUX_MAINS,
            new Effet().c(Fiche.LABEUR, 15).c(Fiche.DEUX_MAINS, 15).or(30).o("une hache")),
        new Origine("Mineurs", "descendaient chaque jour à la mine", "le pic, le dos solide", -1, Fiche.LABEUR,
            new Effet().c(Fiche.LABEUR, 20).c(Fiche.ATHLETISME, 10).or(40).o("un pic")),
        new Origine("Pêcheurs", "vivaient de la pêche", "les filets et l'eau froide", Metiers.PECHERIE, Fiche.SURVIE,
            new Effet().c(Fiche.SURVIE, 15).c(Fiche.ATHLETISME, 15).or(30).o("des filets")),
        new Origine("Forgerons", "tenaient la forge", "le fer, quelques pièces, des outils", Metiers.FORGE, Fiche.FORGE,
            new Effet().c(Fiche.FORGE, 30).c(Fiche.DEUX_MAINS, 5).or(150).o("un marteau de forge")),
        new Origine("Menuisiers", "tenaient un atelier de menuiserie", "le bois travaillé, des outils", Metiers.MENUISERIE, Fiche.BOIS,
            new Effet().c(Fiche.BOIS, 30).or(120).o("des outils de menuisier")),
        new Origine("Tisserands", "tissaient la laine et le lin", "le métier à tisser, de bons habits", Metiers.TISSERAND, Fiche.METIERS_FINS,
            new Effet().c(Fiche.METIERS_FINS, 30).or(120).o("une bonne cape")),
        new Origine("Potiers", "tournaient l'argile", "le tour et le four", Metiers.POTIER, Fiche.METIERS_FINS,
            new Effet().c(Fiche.METIERS_FINS, 25).c(Fiche.COMMERCE, 5).or(100)),
        new Origine("Tanneurs", "tannaient les peaux au bord de l'eau", "le cuir, l'odeur qui ne part pas", Metiers.TANNERIE, Fiche.METIERS_FINS,
            new Effet().c(Fiche.METIERS_FINS, 25).c(Fiche.SURVIE, 5).or(100).o("des bottes de cuir")),
        new Origine("Boulangers", "cuisaient le pain de la ville", "le four, levés avant l'aube", Metiers.BOULANGERIE, Fiche.METIERS_FINS,
            new Effet().c(Fiche.METIERS_FINS, 20).c(Fiche.COMMERCE, 10).or(120)),
        new Origine("Bouchers", "tenaient la boucherie", "le couteau, la viande, les clients", Metiers.BOUCHERIE, Fiche.METIERS_FINS,
            new Effet().c(Fiche.METIERS_FINS, 15).c(Fiche.UNE_MAIN, 10).c(Fiche.COMMERCE, 5).or(120).o("un couteau de boucher")),
        new Origine("Meuniers", "faisaient tourner le moulin", "le grain de toute la ville passe chez vous", Metiers.MOULIN, Fiche.INTENDANCE,
            new Effet().c(Fiche.INTENDANCE, 15).c(Fiche.COMMERCE, 10).c(Fiche.LABEUR, 5).or(150)),
        new Origine("Aubergistes", "tenaient l'auberge", "les voyageurs, les rumeurs, la bière", Metiers.AUBERGE, Fiche.ELOQUENCE,
            new Effet().c(Fiche.ELOQUENCE, 20).c(Fiche.COMMERCE, 15).or(200)),
        new Origine("Marchands", "achetaient et revendaient au marché", "le commerce, les comptes, de l'argent", Metiers.ENTREPOT, Fiche.COMMERCE,
            new Effet().c(Fiche.COMMERCE, 30).c(Fiche.LETTRES, 30).c(Fiche.ELOQUENCE, 10).or(500).o("une bourse de cuir")),
        new Origine("Soldats", "servaient dans la garde", "la lance, la discipline, une arme", Metiers.CASERNE, Fiche.HAST,
            new Effet().c(Fiche.HAST, 20).c(Fiche.UNE_MAIN, 15).c(Fiche.ATHLETISME, 10).or(100).o("une lance").x(Fiche.LOI, 10)),
        new Origine("Lettrés", "servaient au temple et à l'école", "les livres, les prières, l'instruction", Metiers.ECOLE, Fiche.LETTRES,
            new Effet().c(Fiche.LETTRES, 60).c(Fiche.MEDECINE, 15).or(150).o("un livre de prières").x(Fiche.FOI, 15)),
        new Origine("Petite noblesse", "appartenaient à une Maison noble", "un nom, un cheval, des ennemis", Metiers.CHATEAU, Fiche.COMMANDEMENT,
            new Effet().c(Fiche.EQUITATION, 25).c(Fiche.COMMANDEMENT, 20).c(Fiche.INTENDANCE, 15).c(Fiche.LETTRES, 40).or(2000).o("un cheval", "une épée", "une chevalière").r(30).x(Fiche.AMBITION, 10)),
        new Origine("Hors-la-loi", "vivaient en marge de la loi", "les bois, la débrouille, la garde vous connaît", -1, Fiche.DISCRETION,
            new Effet().c(Fiche.DISCRETION, 25).c(Fiche.SURVIE, 15).or(20).o("un couteau").x(Fiche.LOI, -20)),
        new Origine("Orphelin", "", "aucune attache, la rue pour école", -1, Fiche.DISCRETION,
            new Effet().c(Fiche.DISCRETION, 15).c(Fiche.ELOQUENCE, 15).c(Fiche.SURVIE, 5))
    };

    private static final int MILICE = 0, MERCENAIRE = 1, CARAVANIER = 2, COMPAGNON = 3, ETUDES = 4, BRIGAND = 5, MARIN = 6, TERRE = 7, VAGABOND = 8;
    private static final String[] VOIES = { "La milice de la ville", "Une compagnie de mercenaires", "Les caravanes", "Compagnon dans un atelier", "Les études",
            "Brigand dans les bois", "Marin ou pêcheur", "Rester travailler la terre", "Vagabond" };
    private static final String[] VOIES_RECIT = { "t'es engagé%s dans la milice", "as vendu ton épée à une compagnie de mercenaires", "as suivi les caravanes de ville en ville",
            "es devenu%s compagnon dans un atelier", "as étudié auprès des lettrés", "as pris le chemin des bois avec une bande de brigands", "as vécu sur l'eau, avec les pêcheurs",
            "es resté%s travailler la terre", "as erré de région en région" };
    private static final Effet[] EFFETS_VOIES = {
        new Effet().c(Fiche.HAST, 30).c(Fiche.UNE_MAIN, 20).c(Fiche.COMMANDEMENT, 10).f(Fiche.HAST).o("une lance", "un gambison").or(100).x(Fiche.LOI, 10),
        new Effet().c(Fiche.UNE_MAIN, 30).c(Fiche.ARBALETE, 20).c(Fiche.TACTIQUE, 15).f(Fiche.UNE_MAIN).o("une épée", "une arbalète", "une broigne").or(300).r(10).x(Fiche.LOI, -5),
        new Effet().c(Fiche.COMMERCE, 25).c(Fiche.EQUITATION, 20).c(Fiche.SURVIE, 15).f(Fiche.COMMERCE).o("une mule").or(400),
        new Effet().c(Fiche.ELOQUENCE, 5).or(200).o("tes outils"),
        new Effet().c(Fiche.LETTRES, 40).c(Fiche.MEDECINE, 20).c(Fiche.INTENDANCE, 20).f(Fiche.LETTRES).o("trois livres").or(100),
        new Effet().c(Fiche.DISCRETION, 25).c(Fiche.ARC, 25).c(Fiche.SURVIE, 20).f(Fiche.ARC).o("un arc", "un couteau").or(250).x(Fiche.LOI, -25).x(Fiche.HONNETETE, -10),
        new Effet().c(Fiche.ATHLETISME, 25).c(Fiche.SURVIE, 15).c(Fiche.JET, 15).f(Fiche.ATHLETISME).or(150),
        new Effet().c(Fiche.LABEUR, 35).c(Fiche.INTENDANCE, 10).f(Fiche.LABEUR).o("un lopin de terre", "deux chèvres").or(100),
        new Effet().c(Fiche.SURVIE, 20).c(Fiche.ELOQUENCE, 20).c(Fiche.DISCRETION, 10).f(Fiche.SURVIE).or(30)
    };
    private static final String[] VOIES_APPORT = { "Hast, Une main, Commandement ; une lance", "Une main, Arbalète, Tactique ; de l'équipement, un peu de renom",
            "Commerce, Équitation, Survie ; une mule, des contacts", "le métier appris ; tes outils", "Lettres, Médecine, Intendance ; des livres",
            "Discrétion, Arc, Survie ; mauvaise réputation", "Athlétisme, Survie, Jet", "Labeur ; un lopin, des bêtes", "Survie, Éloquence ; tu connais du pays" };

    private static final class Frere {
        long id;
        int sexe, naissance;
    }

    private static final class Brouillon {
        Fiche f = new Fiche();
        int ageDepart = 20, prochain;
        boolean sexeChoisi;
        long pere = -1L, mere = -1L;
        int nePere = AUCUNE, neMere = AUCUNE, mortPere = AUCUNE, mortMere = AUCUNE;
        int[] caracPere = new int[5], caracMere = new int[5];
        int maisonFamille = -1, rangFamille;
        int metierAppris = -1;
        long conjoint = -1L;
        int neConjoint = AUCUNE;
        List<Frere> fratrie = new ArrayList<>();
        List<Ajout> ajouts = new ArrayList<>();

        Brouillon copie() {
            Brouillon b = new Brouillon();
            b.f = f.copie();
            b.ageDepart = ageDepart;
            b.prochain = prochain;
            b.sexeChoisi = sexeChoisi;
            b.pere = pere;
            b.mere = mere;
            b.nePere = nePere;
            b.neMere = neMere;
            b.mortPere = mortPere;
            b.mortMere = mortMere;
            b.caracPere = caracPere.clone();
            b.caracMere = caracMere.clone();
            b.maisonFamille = maisonFamille;
            b.rangFamille = rangFamille;
            b.metierAppris = metierAppris;
            b.conjoint = conjoint;
            b.neConjoint = neConjoint;
            b.fratrie = new ArrayList<>(fratrie);
            b.ajouts = new ArrayList<>(ajouts);
            return b;
        }
    }

    private static final class Pas {
        final Brouillon etat;
        final Supplier<Scene> scene;

        Pas(Brouillon etat, Supplier<Scene> scene) {
            this.etat = etat;
            this.scene = scene;
        }
    }

    private final Population p;
    private final Joueur joueur;
    private final long graine;
    private final Deque<Pas> pile = new ArrayDeque<>();
    private Brouillon b = new Brouillon();
    private Supplier<Scene> courante = this::peuples;
    /** Le titre de l'étape en cours (la vignette en tire l'âge qu'elle montre). */
    private String titreScene = "";

    Creation(Population p, Joueur joueur, long graine, int premierNumero) {
        this.p = p;
        this.joueur = joueur;
        this.graine = graine;
        b.prochain = premierNumero;
    }

    /** Lot K3 : peut-on déjà montrer le personnage ? (on connaît son peuple, son sexe et son identifiant) */
    public boolean apercu() {
        return b.f.peuple >= 0 && b.f.id >= 0L && b.sexeChoisi;
    }

    public int peuple() {
        return b.f.peuple;
    }

    public boolean femme() {
        return b.f.femme();
    }

    public long graineApercu() {
        return Aspect.graine(b.f.id, b.f.aspect.visage);
    }

    public Aspect aspect() {
        return b.f.aspect;
    }

    /** Lot K3 : la fiche qui se remplit au fil des choix, en paires (libellé, valeur). À appeler dans le fil de l'histoire. */
    public List<String[]> resume() {
        List<String[]> l = new ArrayList<>();
        Fiche f = b.f;
        if (f.peuple < 0) return l;
        l.add(new String[] { "Peuple", p.reglages.peuples[f.peuple].nom });
        if (f.ville >= 0) l.add(new String[] { "Né" + f.e() + " à", nomVille() });
        if (b.sexeChoisi) l.add(new String[] { "Sexe", f.femme() ? "Femme" : "Homme" });
        if (f.naissance > 0) l.add(new String[] { "Âge", Math.round(b.ageDepart / rythme()) + " ans" });
        if (!f.nom.isEmpty()) l.add(new String[] { "Nom", f.nom });
        if (f.origine >= 0) l.add(new String[] { "Famille", ORIGINES[f.origine].nom });
        if (b.pere >= 0L) l.add(new String[] { "Père", nom(b.pere, 0) + (b.mortPere != AUCUNE ? " †" : "") });
        if (b.mere >= 0L) l.add(new String[] { "Mère", nom(b.mere, 1) + (b.mortMere != AUCUNE ? " †" : "") });
        if (!b.fratrie.isEmpty()) {
            StringBuilder s = new StringBuilder();
            for (Frere x : b.fratrie) s.append(s.length() > 0 ? ", " : "").append(nom(x.id, x.sexe));
            l.add(new String[] { b.fratrie.size() > 1 ? "Fratrie" : (b.fratrie.get(0).sexe == 1 ? "Sœur" : "Frère"), s.toString() });
        }
        if (b.conjoint >= 0L) l.add(new String[] { f.femme() ? "Époux" : "Épouse", nom(b.conjoint, f.femme() ? 0 : 1) });
        return l;
    }

    /** Lot K4 : une personne de la vignette (la graine est celle qu'elle a dans le monde : son id). */
    public static final class Figure {
        public final long graine;
        public final int espece;
        public final boolean femme, enfant, toi;
        public final String role, legende;

        Figure(long graine, int espece, boolean femme, boolean enfant, boolean toi, String role, String legende) {
            this.graine = graine;
            this.espece = espece;
            this.femme = femme;
            this.enfant = enfant;
            this.toi = toi;
            this.role = role;
            this.legende = legende;
        }
    }

    /** Lot K4 : la vignette d'une étape : qui est là (dans l'ordre, de gauche à droite), où et quand. */
    public static final class Tableau {
        public int ville = -1, annee;
        public String lieu = "";
        public final List<Figure> figures = new ArrayList<>();
    }

    /** L'âge (en années humaines) que montre chaque étape : l'enfance montre l'enfant, la suite l'âge d'entrée dans le monde. */
    private float ageEtape() {
        switch (titreScene) {
            case "Petite enfance": return 4f;
            case "Enfance": return 9f;
            case "Adolescence": return 14f;
            case "Jeunesse": return 18f;
            default: return b.ageDepart;
        }
    }

    /** Lot K4 : qui se tient sur la vignette de l'étape en cours. À appeler dans le fil de l'histoire. */
    public Tableau vignette() {
        Tableau t = new Tableau();
        Fiche f = b.f;
        if (!apercu() || f.ville < 0) return t;
        t.ville = f.ville;
        int espece = f.peuple;
        boolean connu = f.naissance > 0;
        float age = connu ? ageEtape() : b.ageDepart;
        int annee = connu ? f.naissance + Math.round(age / rythme()) : p.annee;
        t.annee = annee;
        t.lieu = nomVille() + ", an " + annee;
        if (connu) {
            if (b.pere >= 0L && (b.mortPere == AUCUNE || b.mortPere > annee)) t.figures.add(proche(b.pere, espece, 0, b.nePere, annee, "Père", "ton père"));
            if (b.mere >= 0L && (b.mortMere == AUCUNE || b.mortMere > annee)) t.figures.add(proche(b.mere, espece, 1, b.neMere, annee, "Mère", "ta mère"));
        }
        String toi = f.nom.isEmpty() ? "Toi" : f.nom + ", toi";
        t.figures.add(new Figure(Aspect.graine(f.id, f.aspect.visage), espece, f.femme(), age < AGE_ENFANT, true, "Toi",
            toi + (connu ? ", " + Math.round(age / rythme()) + " ans" : "")));
        if (connu && b.conjoint >= 0L && age >= 17f) {
            int sexe = f.femme() ? 0 : 1;
            t.figures.add(proche(b.conjoint, espece, sexe, b.neConjoint, annee, f.femme() ? "Époux" : "Épouse", f.femme() ? "ton époux" : "ton épouse"));
        }
        if (connu) {
            List<Frere> freres = new ArrayList<>(b.fratrie);
            freres.sort((x, y) -> Integer.compare(x.naissance, y.naissance));
            for (Frere x : freres) {
                if (t.figures.size() >= FIGURES_MAX) break;
                if (x.naissance > annee) continue;
                boolean aine = x.naissance < f.naissance;
                String role = x.sexe == 1 ? (aine ? "Sœur aînée" : "Petite sœur") : (aine ? "Frère aîné" : "Petit frère");
                t.figures.add(proche(x.id, espece, x.sexe, x.naissance, annee, role, x.sexe == 1 ? "ta sœur" : "ton frère"));
            }
        }
        return t;
    }

    private static final float AGE_ENFANT = 13f;
    private static final int FIGURES_MAX = 8;

    private Figure proche(long id, int espece, int sexe, int ne, int annee, String role, String lien) {
        String n = nom(id, sexe);
        float age = ne == AUCUNE ? 30f : (annee - ne) * rythme();
        String legende = n + ", " + lien + (ne == AUCUNE ? "" : ", " + Math.max(0, Math.round(age / rythme())) + " ans");
        return new Figure(id, espece, sexe == 1, age < AGE_ENFANT, false, role, legende);
    }

    Scene reprendre() {
        return courante.get();
    }

    private Scene scene(String titre, String texte, Supplier<Scene> ici) {
        courante = ici;
        titreScene = titre;
        return new Scene(titre, texte);
    }

    private void option(Scene s, String libelle, Supplier<Scene> ici, Runnable effet, Supplier<Scene> suite) {
        s.ajouter(libelle, () -> {
            pile.push(new Pas(b.copie(), ici));
            effet.run();
            return suite.get();
        });
    }

    private void retour(Scene s) {
        if (pile.isEmpty()) return;
        s.ajouter("« Revenir", () -> {
            Pas pas = pile.pop();
            b = pas.etat;
            return pas.scene.get();
        });
    }

    private Random hasard(int etape) {
        return new Random(graine * 1000003L + etape * 7919L + b.f.ville * 31L + b.f.naissance);
    }

    private float rythme() {
        return p.reglages.peuples[b.f.peuple].rythme;
    }

    private int ans(float age) {
        return Math.max(1, Math.round(age / rythme()));
    }

    private String tranche(int debut, int fin) {
        return "De " + (debut == 0 ? "ta naissance" : ans(debut) + "") + " à " + (debut == 0 ? "tes " : "") + ans(fin) + " ans";
    }

    private int ageEn(int ne, int annee) {
        return (int) ((annee - ne) * rythme());
    }

    private Foule foule() {
        return p.foules[b.f.peuple][b.f.region];
    }

    private String nom(long id, int sexe) {
        return Noms.personne(b.f.peuple, p.graine, id, sexe);
    }

    private String nomVille() {
        return Noms.ville(p.villes.peuple[b.f.ville], p.graine, b.f.ville);
    }

    private String nomEtat(int e) {
        return Noms.ville(p.etats.peuple[e], p.graine, p.etats.capitale[e]);
    }

    static String de(String nom) {
        return ("AEIOUYaeiouyÉ".indexOf(nom.charAt(0)) >= 0 ? "d'" : "de ") + nom;
    }

    private static String a(String nom) {
        return "à " + nom;
    }

    private String e() {
        return b.f.e();
    }

    private String il() {
        return b.f.femme() ? "elle" : "il";
    }

    private String tu(String masculin, String feminin) {
        return b.f.femme() ? feminin : masculin;
    }

    private void raconter(String phrase) {
        b.f.recit.add(phrase);
    }

    private long nouvelId() {
        return foule().idReserve(b.prochain++);
    }

    private Metiers.Profil profil() {
        return Metiers.profil(p, b.f.ville);
    }

    private boolean present(int metier) {
        for (int m : Metiers.besoins(profil())) if (m == metier) return true;
        return false;
    }

    private int habitants() {
        return p.villes.habitants[b.f.ville];
    }

    private static final String[] PEUPLES_TEXTES = {
        "Les elfes vivent des siècles : à deux cents ans, un elfe est encore jeune. Peuple des forêts, lettré, attaché à la loi et au bien.",
        "Les nains vivent sous les montagnes, autour de leurs forges. Têtus, loyaux et durs à la tâche.",
        "Les humains sont partout, nombreux et changeants. Ils apprennent vite et s'adaptent à tout.",
        "Les orks vivent vite et brûlent fort. Chez eux, la force fait la loi et le plus fort commande."
    };
    private static final int[] PEUPLES_ATTRIBUT = { Fiche.ESPRIT, Fiche.ENDURANCE, Fiche.PRESENCE, Fiche.VIGUEUR };
    private static final int[] PEUPLES_FOCUS = { Fiche.ARC, Fiche.FORGE, Fiche.COMMERCE, Fiche.DEUX_MAINS };

    Scene peuples() {
        Scene s = scene("Créer ton personnage", "Nous sommes en l'an " + p.annee + ". Le monde a vécu cinq mille ans d'histoire sans toi. Tu vas y naître.\n\nDe quel peuple es-tu ?", this::peuples);
        int[] villes = new int[Peuple.NOMBRE];
        for (int v = 0; v < p.villes.nombre; v++) if (p.villes.estVivante(v)) villes[p.villes.peuple[v]]++;
        int[] ordre = { Peuple.HUMAIN, Peuple.NAIN, Peuple.ELFE, Peuple.ORK };
        for (int q : ordre) {
            if (villes[q] == 0) continue;
            String libelle = p.reglages.peuples[q].nom + " (" + villes[q] + (villes[q] > 1 ? " villes) — " : " ville) — ") + PEUPLES_TEXTES[q];
            option(s, libelle, this::peuples, () -> {
                b.f.peuple = q;
                b.f.augmenter(PEUPLES_ATTRIBUT[q], 1);
                b.f.concentrer(PEUPLES_FOCUS[q]);
                Peuple peuple = p.reglages.peuples[q];
                b.f.pencher(Fiche.LOI, Math.round(peuple.loi / 2f));
                b.f.pencher(Fiche.BIEN, Math.round(peuple.bien / 2f));
            }, this::royaumes);
        }
        return s;
    }

    private int[] habitantsParRegion() {
        int[] h = new int[p.regions.nombre];
        Villes v = p.villes;
        for (int ville = 0; ville < v.nombre; ville++) {
            if (v.estVivante(ville) && v.peuple[ville] == b.f.peuple && v.region[ville] >= 0 && v.region[ville] < h.length) h[v.region[ville]] += v.habitants[ville];
        }
        return h;
    }

    /** Lot K3 : les royaumes où vivent des gens de ton peuple, tous à la fois (on les choisit sur la carte ou dans la liste). */
    private Scene royaumes() {
        Etats e = p.etats;
        int[] h = habitantsParRegion();
        int[] comtes = new int[e.nombre], gens = new int[e.nombre];
        int libres = 0, gensLibres = 0;
        for (int r = 0; r < h.length; r++) {
            if (h[r] == 0) continue;
            int etat = e.regionEtat[r];
            if (etat >= 0 && e.estVivant(etat)) {
                comtes[etat]++;
                gens[etat] += h[r];
            } else {
                libres++;
                gensLibres += h[r];
            }
        }
        List<Integer> liste = new ArrayList<>();
        for (int k = 0; k < e.nombre; k++) if (e.estVivant(k) && comtes[k] > 0) liste.add(k);
        liste.sort((x, y) -> Integer.compare(gens[y], gens[x]));
        Scene s = scene("Lieu de naissance", "Dans quel royaume vas-tu naître ? Choisis-le sur la carte ou dans la liste.", this::royaumes);
        s.carte = Scene.CARTE_ROYAUMES;
        for (int etat : liste) {
            String peuple = e.peuple[etat] != b.f.peuple ? ", sous un roi " + p.reglages.peuples[e.peuple[etat]].nom.toLowerCase() : "";
            s.ajouter("Royaume " + de(nomEtat(etat)) + " (" + comtes[etat] + (comtes[etat] > 1 ? " comtés, " : " comté, ") + gens[etat] + " habitants" + peuple + ")", () -> villes(etat));
            s.lieux.add(etat);
        }
        if (libres > 0) {
            s.ajouter("Terres sans maître (" + libres + (libres > 1 ? " comtés, " : " comté, ") + gensLibres + " habitants)", () -> villes(-1));
            s.lieux.add(-1);
        }
        retour(s);
        s.lieux.add(Scene.HORS_CARTE);
        return s;
    }

    /** Les villes de ton peuple dans ce royaume (etat -1 : les terres sans maître), toutes, choisies sur la carte ou dans la liste. */
    private Scene villes(int etat) {
        Villes v = p.villes;
        Etats e = p.etats;
        List<Integer> liste = new ArrayList<>();
        for (int ville = 0; ville < v.nombre; ville++) {
            if (!v.estVivante(ville) || v.peuple[ville] != b.f.peuple || v.region[ville] < 0) continue;
            int maitre = e.regionEtat[v.region[ville]];
            boolean libre = maitre < 0 || !e.estVivant(maitre);
            if (etat < 0 ? libre : maitre == etat) liste.add(ville);
        }
        liste.sort((x, y) -> Integer.compare(v.habitants[y], v.habitants[x]));
        Supplier<Scene> ici = () -> villes(etat);
        Scene s = scene(etat < 0 ? "Terres sans maître" : "Royaume " + de(nomEtat(etat)), "Dans quelle ville ? Choisis-la sur la carte ou dans la liste.", ici);
        s.carte = Scene.CARTE_VILLES;
        s.etatCarte = etat;
        for (int ville : liste) {
            boolean capitale = etat >= 0 && e.capitale[etat] == ville;
            String histoire = histoireVille(ville);
            int region = v.region[ville];
            option(s, Noms.ville(v.peuple[ville], p.graine, ville) + (capitale ? ", capitale" : "") + " (" + v.habitants[ville] + " habitants, fondée il y a " + (p.annee - v.fondation[ville]) + " ans" + histoire + ")", ici, () -> {
                b.f.ville = ville;
                b.f.region = region;
                b.f.id = nouvelId();
            }, this::sexe);
            s.lieux.add(ville);
        }
        s.ajouter("« Revenir", this::royaumes);
        s.lieux.add(Scene.HORS_CARTE);
        return s;
    }

    private String histoireVille(int ville) {
        Villes v = p.villes;
        int sacs = 0, dernier = AUCUNE;
        for (int k = 0; k < v.nombreEvenements; k++) {
            if (v.evVille[k] != ville || v.evType[k] != Villes.SACCAGEE) continue;
            sacs++;
            dernier = v.evAnnee[k];
        }
        if (sacs == 0) return "";
        return sacs == 1 ? ", saccagée en " + dernier : ", saccagée " + sacs + " fois, la dernière en " + dernier;
    }

    private Scene sexe() {
        Scene s = scene("Identité", "Tu vas naître à " + nomVille() + ".\n\nQui es-tu ?", this::sexe);
        option(s, "Un homme", this::sexe, () -> { b.f.sexe = 0; b.sexeChoisi = true; }, this::age);
        option(s, "Une femme", this::sexe, () -> { b.f.sexe = 1; b.sexeChoisi = true; }, this::age);
        retour(s);
        return s;
    }

    private Scene age() {
        Peuple peuple = p.reglages.peuples[b.f.peuple];
        Scene s = scene("Identité", "Quel âge as-tu le jour où tu entres dans le monde ?\n\nPlus tu es âgé" + e() + ", plus tu as appris et amassé, mais moins il te reste d'années. "
                + (peuple.rythme < 0.5f ? "Chez les " + peuple.nom.toLowerCase() + ", une vie est longue : l'âge compte en années de ton peuple." : ""), this::age);
        String[] notes = { "toute la vie devant toi", "un conjoint possible, un peu d'argent de côté", "des enfants déjà grands, plus de savoir-faire", "beaucoup d'expérience, mais il faudra préparer l'héritier tôt" };
        for (int k = 0; k < AGES.length; k++) {
            int age = AGES[k];
            int reels = Math.round(age / peuple.rythme);
            int ne = p.annee - reels;
            option(s, reels + " ans" + (reels != age ? " (comme un humain de " + age + ")" : "") + " — " + notes[k], this::age, () -> {
                b.ageDepart = age;
                b.f.naissance = ne;
            }, this::nom);
        }
        retour(s);
        return s;
    }

    private Scene nom() {
        Random r = hasard(1000 + (int) b.f.apparence);
        Scene s = scene("Identité", "Comment t'appelles-tu ?", this::nom);
        for (int k = 0; k < 6; k++) {
            String n = Noms.personne(b.f.peuple, p.graine, r.nextLong(), b.f.sexe);
            option(s, n, this::nom, () -> b.f.nom = n, this::origines);
        }
        s.ajouter("Écrire un autre nom…", () -> {
            Scene saisie = scene("Identité", "Écris ton nom, puis valide avec Entrée.", this::nom);
            saisie.saisie = "";
            saisie.valider = texte -> {
                String n = texte.trim();
                if (n.isEmpty()) return nom();
                pile.push(new Pas(b.copie(), this::nom));
                b.f.nom = Character.toUpperCase(n.charAt(0)) + n.substring(1);
                return origines();
            };
            saisie.annuler = this::nom;
            return saisie;
        });
        s.ajouter("D'autres noms", () -> {
            b.f.apparence++;
            return nom();
        });
        retour(s);
        return s;
    }

    private int[] bornesMere() {
        return new int[] { b.f.naissance - ans(38), b.f.naissance - ans(17) };
    }

    private boolean possible(int o) {
        Metiers.Profil pr = profil();
        int region = b.f.region;
        switch (o) {
            case BUCHERONS: return p.regions.bois[region] > 0f;
            case MINEURS: return p.regions.minerai[region] > 0f;
            case PECHEURS: return p.regions.peche[region] > 0f;
            case AUBERGISTES: return present(Metiers.AUBERGE) || present(Metiers.TAVERNE);
            case MARCHANDS: return present(Metiers.ENTREPOT) || Metiers.etals(pr) > 0;
            case SOLDATS: return present(Metiers.CASERNE);
            case LETTRES: return present(Metiers.ECOLE) || habitants() >= 300;
            case NOBLES: {
                int[] m = bornesMere();
                return !foule().couples(b.f.ville, m[0], m[1], m[1], true).isEmpty();
            }
            default:
                return ORIGINES[o].metier < 0 || present(ORIGINES[o].metier);
        }
    }

    private Scene origines() {
        Scene s = scene("Ta famille", "Tu t'appelles " + b.f.nom + ".\n\nQue faisaient tes parents ? On ne te propose que les métiers qu'on trouve vraiment à " + nomVille() + ".", this::origines);
        for (int o = 0; o < ORIGINES.length; o++) {
            if (!possible(o)) continue;
            int origine = o;
            Origine x = ORIGINES[o];
            option(s, x.nom + " — " + x.apport, this::origines, () -> {
                b.f.origine = origine;
                x.effet.appliquer(b.f, 1f);
            }, origine == ORPHELIN ? this::orphelin : this::parents);
        }
        retour(s);
        return s;
    }

    private int sacPendant(int debut, int fin) {
        Villes v = p.villes;
        int meilleur = -1;
        for (int k = 0; k < v.nombreEvenements; k++) {
            if (v.evVille[k] != b.f.ville || v.evType[k] != Villes.SACCAGEE) continue;
            if (v.evAnnee[k] < debut || v.evAnnee[k] > fin) continue;
            if (meilleur < 0 || v.evValeur[k] > v.evValeur[meilleur]) meilleur = k;
        }
        return meilleur;
    }

    private int pillard(int annee) {
        Chronique c = p.etats.chronique;
        int region = b.f.region, objet = -1;
        for (int i = 0; i < c.nombre; i++) {
            if (c.annee[i] != annee) continue;
            if (c.type[i] == Chronique.SAC && c.lieu[i] == region) return c.sujet[i];
            if ((c.type[i] == Chronique.SAC || c.type[i] == Chronique.CONQUETE || c.type[i] == Chronique.CHUTE) && objet < 0) objet = c.sujet[i];
        }
        return objet;
    }

    private int maitreEn(int annee) {
        Villes v = p.villes;
        int ville = b.f.ville;
        int i = (annee - v.fondation[ville]) / Villes.PAS_MESURE;
        if (i < 0 || v.mesures[ville] == 0) return -1;
        return v.maitre[ville][Math.min(i, v.mesures[ville] - 1)];
    }

    private int[] caractereHasard(Random r) {
        Peuple q = p.reglages.peuples[b.f.peuple];
        int[] c = new int[5];
        c[0] = Math.round(q.loi + (r.nextFloat() + r.nextFloat() - 1f) * 60f);
        c[1] = Math.round(q.bien + (r.nextFloat() + r.nextFloat() - 1f) * 60f);
        for (int k = 2; k < 5; k++) c[k] = Math.round((r.nextFloat() + r.nextFloat() - 1f) * 60f);
        for (int k = 0; k < 5; k++) c[k] = Math.max(-100, Math.min(100, c[k]));
        return c;
    }

    private int[] caractereDe(int place) {
        Foule f = foule();
        return new int[] { f.loiDe(place), f.bienDe(place), f.ambitionDe(place), f.courageDe(place), f.foiDe(place) };
    }

    private boolean nomPris(String nom) {
        if (nom.equals(b.f.nom)) return true;
        for (Ajout x : b.ajouts) if (nom(x.id, x.sexe).equals(nom)) return true;
        for (Frere x : b.fratrie) if (nom(x.id, x.sexe).equals(nom)) return true;
        for (Fiche.Proche x : b.f.proches) if (nom(x.id, x.sexe).equals(nom)) return true;
        return false;
    }

    private Ajout creer(Random r, int sexe, int ne, long pere, long mere, int[] caractere) {
        Ajout a = new Ajout();
        a.id = nouvelId();
        for (int essai = 0; essai < 8 && nomPris(nom(a.id, sexe)); essai++) a.id = nouvelId();
        a.pere = pere;
        a.mere = mere;
        a.sexe = sexe;
        a.naissance = ne;
        a.caractere = caractere;
        a.talent = r.nextInt(70) + 10;
        a.ville = b.f.ville;
        b.ajouts.add(a);
        return a;
    }

    private void parentReel(int place, boolean pere) {
        Foule f = foule();
        if (pere) {
            b.pere = f.idDe(place);
            b.nePere = f.naissanceDe(place);
            b.caracPere = caractereDe(place);
        } else {
            b.mere = f.idDe(place);
            b.neMere = f.naissanceDe(place);
            b.caracMere = caractereDe(place);
        }
        if (f.maisonDe(place) >= 0) {
            b.maisonFamille = f.maisonDe(place);
            b.rangFamille = Math.min(3, f.rangDe(place) + 1);
        }
    }

    private void parentCree(Random r, boolean pere, int mort) {
        int[] m = bornesMere();
        int ne = mort == AUCUNE ? m[1] - r.nextInt(Math.max(1, (m[1] - m[0]) / 3)) : m[0] + r.nextInt(Math.max(1, m[1] - m[0]));
        if (pere) ne -= r.nextInt(Math.max(1, ans(8)));
        int[] c = caractereHasard(r);
        long id;
        if (mort == AUCUNE) {
            Ajout a = creer(r, pere ? 0 : 1, ne, -1L, -1L, c);
            id = a.id;
        } else id = nouvelId();
        if (pere) {
            b.pere = id;
            b.nePere = ne;
            b.mortPere = mort;
            b.caracPere = c;
        } else {
            b.mere = id;
            b.neMere = ne;
            b.mortMere = mort;
            b.caracMere = c;
        }
    }

    private void unirCrees() {
        Ajout pere = null, mere = null;
        for (Ajout a : b.ajouts) {
            if (a.id == b.pere) pere = a;
            if (a.id == b.mere) mere = a;
        }
        if (pere != null && mere != null) {
            pere.conjoint = mere.id;
            mere.conjoint = pere.id;
        }
    }

    private String nomPere() {
        return nom(b.pere, 0);
    }

    private String nomMere() {
        return nom(b.mere, 1);
    }

    private Scene parents() {
        Origine x = ORIGINES[b.f.origine];
        Random r = hasard(2);
        int[] m = bornesMere();
        boolean nobles = b.f.origine == NOBLES;
        Foule f = foule();
        List<int[]> couples = f.couples(b.f.ville, m[0], m[1], m[1], nobles);
        int vieux = ageEn(m[1], p.annee);
        int enfance = b.f.naissance + ans(16);
        int sac = sacPendant(b.f.naissance, Math.min(p.annee, enfance));
        Scene s = scene("Ta famille", "Tes parents " + x.phrase + " à " + nomVille() + ".\n\nQue sont-ils devenus ?", this::parents);
        if (!couples.isEmpty() || (!nobles && vieux <= 75)) {
            option(s, couples.isEmpty() ? "Ils sont toujours en vie" : "Ils sont toujours en vie, et vivent encore à " + nomVille(), this::parents, () -> {
                if (!couples.isEmpty()) {
                    int[] c = couples.get(r.nextInt(couples.size()));
                    parentReel(c[0], true);
                    parentReel(c[1], false);
                } else {
                    parentCree(r, true, AUCUNE);
                    parentCree(r, false, AUCUNE);
                    unirCrees();
                }
                raconter(tu("Fils", "Fille") + " " + de(nomPere()) + " et " + de(nomMere()) + ", qui " + x.phrase + ", tu es né" + e() + " à " + nomVille() + " en l'an " + b.f.naissance + ".");
            }, this::fratrie);
        }
        if (nobles) {
            retour(s);
            return s;
        }
        int anneeMort = Math.min(p.annee - 1, b.f.naissance + ans(3 + r.nextInt(12)));
        int ageMort = anneeMort - b.f.naissance;
        List<Integer> veuves = f.gens(b.f.ville, m[0], m[1], 1, true);
        List<Integer> veufs = f.gens(b.f.ville, m[0] - ans(8), m[1], 0, true);
        option(s, "Ton père est mort quand tu avais " + ageMort + " ans ; ta mère t'a élevé" + e() + " seule", this::parents, () -> {
            if (!veuves.isEmpty()) parentReel(veuves.get(r.nextInt(veuves.size())), false);
            else parentCree(r, false, AUCUNE);
            parentCree(r, true, anneeMort);
            raconter("Tu es né" + e() + " à " + nomVille() + " en l'an " + b.f.naissance + ". Tes parents " + x.phrase + ". Ton père, " + nomPere() + ", est mort quand tu avais " + ageMort + " ans, et ta mère, " + nomMere() + ", t'a élevé" + e() + " seule.");
        }, this::fratrie);
        option(s, "Ta mère est morte quand tu avais " + ageMort + " ans ; ton père t'a élevé" + e() + " seul", this::parents, () -> {
            if (!veufs.isEmpty()) parentReel(veufs.get(r.nextInt(veufs.size())), true);
            else parentCree(r, true, AUCUNE);
            parentCree(r, false, anneeMort);
            raconter("Tu es né" + e() + " à " + nomVille() + " en l'an " + b.f.naissance + ". Tes parents " + x.phrase + ". Ta mère, " + nomMere() + ", est morte quand tu avais " + ageMort + " ans, et ton père, " + nomPere() + ", t'a élevé" + e() + " seul.");
        }, this::fratrie);
        if (sac >= 0) {
            int annee = p.villes.evAnnee[sac], morts = p.villes.evValeur[sac];
            int ennemi = pillard(annee);
            int age = annee - b.f.naissance;
            String qui = ennemi >= 0 ? "les soldats " + de(nomEtat(ennemi)) : "des pillards";
            option(s, "Ils ont été tués lors du sac de " + nomVille() + ", en l'an " + annee + " ; tu avais " + age + " ans", this::parents, () -> {
                parentCree(r, true, annee);
                parentCree(r, false, annee);
                b.f.ennemi = ennemi;
                b.f.pencher(Fiche.COURAGE, 10);
                b.f.pencher(Fiche.AMBITION, 10);
                raconter("Tu es né" + e() + " à " + nomVille() + " en l'an " + b.f.naissance + ". Tes parents, " + nomPere() + " et " + nomMere() + ", " + x.phrase + ". En l'an " + annee + ", " + qui
                        + " ont mis la ville à sac. " + morts + " personnes y sont mortes, et tes parents avec elles. Tu avais " + age + " ans.");
            }, this::fratrie);
        }
        option(s, "Tu as perdu tes deux parents quand tu étais enfant", this::parents, () -> {
            parentCree(r, true, anneeMort);
            parentCree(r, false, Math.min(p.annee - 1, anneeMort + r.nextInt(Math.max(1, ans(4)))));
            raconter("Tu es né" + e() + " à " + nomVille() + " en l'an " + b.f.naissance + ". Tes parents, " + nomPere() + " et " + nomMere() + ", " + x.phrase + ". Tu les as perdus tous les deux quand tu étais enfant.");
        }, this::fratrie);
        retour(s);
        return s;
    }

    private Scene orphelin() {
        Random r = hasard(3);
        int sac = sacPendant(b.f.naissance, Math.min(p.annee, b.f.naissance + ans(6)));
        Scene s = scene("Ta famille", "Tu n'as jamais connu tes parents. Comment as-tu grandi ?", this::orphelin);
        option(s, "Recueilli" + e() + " par le temple : on t'a appris à prier et à te taire", this::orphelin, () -> {
            b.f.gagner(Fiche.LETTRES, 20);
            b.f.pencher(Fiche.FOI, 15);
            raconter("Tu es né" + e() + " à " + nomVille() + " en l'an " + b.f.naissance + ", de parents que tu n'as jamais connus. Le temple t'a recueilli" + e() + ".");
        }, this::petiteEnfance);
        option(s, "Élevé" + e() + " par la rue : tu as appris à voler avant de savoir parler", this::orphelin, () -> {
            b.f.gagner(Fiche.DISCRETION, 15);
            b.f.pencher(Fiche.HONNETETE, -10);
            raconter("Tu es né" + e() + " à " + nomVille() + " en l'an " + b.f.naissance + ", de parents que tu n'as jamais connus. Tu as grandi dans la rue.");
        }, this::petiteEnfance);
        option(s, "Placé" + e() + " dans une ferme où l'on avait besoin de bras", this::orphelin, () -> {
            b.f.gagner(Fiche.LABEUR, 15);
            raconter("Tu es né" + e() + " à " + nomVille() + " en l'an " + b.f.naissance + ", de parents que tu n'as jamais connus. On t'a placé" + e() + " dans une ferme, où l'on avait besoin de bras.");
        }, this::petiteEnfance);
        if (sac >= 0) {
            int annee = p.villes.evAnnee[sac];
            int ennemi = pillard(annee);
            option(s, "Trouvé" + e() + " dans les ruines après le sac de l'an " + annee, this::orphelin, () -> {
                b.f.ennemi = ennemi;
                b.f.pencher(Fiche.COURAGE, 10);
                raconter("Tu es né" + e() + " à " + nomVille() + " en l'an " + b.f.naissance + ". On t'a trouvé" + e() + " dans les ruines, après le sac de l'an " + annee
                        + (ennemi >= 0 ? " par les soldats " + de(nomEtat(ennemi)) : "") + ". Personne n'a jamais su qui étaient tes parents.");
            }, this::petiteEnfance);
        }
        retour(s);
        return s;
    }

    private int[] bornesFratrie() {
        int debut = b.neMere != AUCUNE ? b.neMere + ans(17) : b.f.naissance - ans(10);
        int fin = b.neMere != AUCUNE ? b.neMere + ans(42) : b.f.naissance + ans(10);
        if (b.mortPere != AUCUNE) fin = Math.min(fin, b.mortPere);
        if (b.mortMere != AUCUNE) fin = Math.min(fin, b.mortMere);
        fin = Math.min(fin, p.annee - 1);
        return new int[] { debut, fin };
    }

    private void fratrieReelle() {
        b.fratrie.clear();
        Foule f = foule();
        boolean reels = b.pere >= 0 && (f.placeDe(b.pere) >= 0 || f.placeDe(b.mere) >= 0);
        if (!reels) return;
        for (int place : f.enfantsDe(f.placeDe(b.pere) >= 0 ? b.pere : -1L, f.placeDe(b.mere) >= 0 ? b.mere : -1L)) {
            Frere x = new Frere();
            x.id = f.idDe(place);
            x.sexe = f.sexeDe(place);
            x.naissance = f.naissanceDe(place);
            b.fratrie.add(x);
        }
    }

    private String decrireFratrie() {
        if (b.fratrie.isEmpty()) return "";
        StringBuilder t = new StringBuilder();
        for (Frere x : b.fratrie) {
            if (t.length() > 0) t.append(", ");
            t.append(nom(x.id, x.sexe)).append(" (").append(x.sexe == 1 ? "sœur" : "frère").append(", ").append(p.annee - x.naissance).append(" ans)");
        }
        return t.toString();
    }

    private Scene fratrie() {
        fratrieReelle();
        int[] bornes = bornesFratrie();
        String deja = decrireFratrie();
        String texte = deja.isEmpty() ? "As-tu des frères et sœurs ?" : "Tu as déjà des frères et sœurs bien réels à " + nomVille() + " : " + deja + ".\n\nEn as-tu d'autres ?";
        Scene s = scene("Frères et sœurs", texte, this::fratrie);
        option(s, deja.isEmpty() ? "Non, tu es enfant unique" : "Non, c'est tout", this::fratrie, () -> {}, this::liensFratrie);
        if (bornes[1] > bornes[0]) {
            int[][] tailles = { { 1, 1 }, { 2, 3 }, { 4, 6 } };
            String[] libelles = { "Un frère ou une sœur", "Deux ou trois", "Une famille nombreuse (quatre à six)" };
            for (int k = 0; k < tailles.length; k++) {
                int[] t = tailles[k];
                option(s, libelles[k], this::fratrie, () -> {
                    Random r = hasard(4);
                    int n = t[0] + r.nextInt(t[1] - t[0] + 1);
                    for (int i = 0; i < n; i++) {
                        int ne = bornes[0] + r.nextInt(bornes[1] - bornes[0] + 1);
                        if (Math.abs(ne - b.f.naissance) < ans(1)) ne = b.f.naissance + (r.nextBoolean() ? 1 : -1) * ans(1 + r.nextInt(3));
                        if (ne > p.annee - 1) ne = p.annee - 1;
                        int[] c = new int[5];
                        for (int j = 0; j < 5; j++) c[j] = Math.max(-100, Math.min(100, (b.caracPere[j] + b.caracMere[j]) / 2 + Math.round((r.nextFloat() + r.nextFloat() - 1f) * 24f)));
                        Ajout a = creer(r, r.nextBoolean() ? 1 : 0, ne, b.pere, b.mere, c);
                        a.maison = b.maisonFamille;
                        a.rang = b.rangFamille;
                    }
                }, this::liensFratrie);
            }
        }
        retour(s);
        return s;
    }

    private List<Frere> tousFreres() {
        List<Frere> l = new ArrayList<>(b.fratrie);
        for (Ajout a : b.ajouts) {
            if (a.id == b.pere || a.id == b.mere || a.pere != b.pere || a.mere != b.mere || b.pere < 0) continue;
            Frere x = new Frere();
            x.id = a.id;
            x.sexe = a.sexe;
            x.naissance = a.naissance;
            l.add(x);
        }
        return l;
    }

    private Scene liensFratrie() {
        List<Frere> freres = tousFreres();
        if (freres.isEmpty()) {
            b.f.aine = true;
            return petiteEnfance();
        }
        Frere proche = freres.get(0);
        boolean aine = true;
        for (Frere x : freres) {
            if (Math.abs(x.naissance - b.f.naissance) < Math.abs(proche.naissance - b.f.naissance)) proche = x;
            if (x.naissance < b.f.naissance) aine = false;
        }
        Frere choisi = proche;
        boolean plusVieux = choisi.naissance < b.f.naissance;
        String lui = nom(choisi.id, choisi.sexe);
        String sonRole = (choisi.sexe == 1 ? (plusVieux ? "ta grande sœur" : "ta petite sœur") : (plusVieux ? "ton grand frère" : "ton petit frère"));
        boolean estAine = aine;
        String rang = estAine ? "Tu es l'aîné" + e() + " : la maison et le métier de tes parents te reviendront." : "Tu n'es pas l'aîné" + e() + " : la maison et le métier iront à un autre. Il te faudra faire ta vie toi-même.";
        Scene s = scene("Frères et sœurs", rang + "\n\nAvec " + lui + ", " + sonRole + ", tu étais…", this::liensFratrie);
        Runnable commun = () -> {
            b.f.aine = estAine;
            if (!estAine) b.f.pencher(Fiche.AMBITION, 10);
            for (Frere x : freres) b.f.lier(x.id, x.sexe == 1 ? Fiche.SOEUR : Fiche.FRERE, x.sexe, 30, false);
        };
        option(s, "Inséparables : vous faisiez les quatre cents coups ensemble", this::liensFratrie, () -> {
            commun.run();
            estime(choisi.id, 70);
            b.f.gagner(Fiche.DISCRETION, 5);
            raconter("Avec " + lui + ", " + sonRole + ", vous étiez inséparables.");
        }, this::petiteEnfance);
        option(s, "Rivaux : chacun voulait être le préféré", this::liensFratrie, () -> {
            commun.run();
            estime(choisi.id, -30);
            b.f.pencher(Fiche.AMBITION, 10);
            b.f.gagner(Fiche.UNE_MAIN, 5);
            raconter("Entre " + lui + ", " + sonRole + ", et toi, ce fut toujours la rivalité.");
        }, this::petiteEnfance);
        if (plusVieux) {
            option(s, "Protégé" + e() + " : " + (choisi.sexe == 1 ? "elle" : "il") + " veillait sur toi", this::liensFratrie, () -> {
                commun.run();
                estime(choisi.id, 60);
                raconter(lui + ", " + sonRole + ", a toujours veillé sur toi.");
            }, this::petiteEnfance);
        } else {
            option(s, "Protect" + tu("eur", "rice") + " : tu veillais sur " + (choisi.sexe == 1 ? "elle" : "lui"), this::liensFratrie, () -> {
                commun.run();
                estime(choisi.id, 60);
                b.f.gagner(Fiche.COMMANDEMENT, 5);
                b.f.pencher(Fiche.BIEN, 10);
                raconter("Tu as toujours veillé sur " + lui + ", " + sonRole + ".");
            }, this::petiteEnfance);
        }
        retour(s);
        return s;
    }

    private void estime(long id, int valeur) {
        for (Fiche.Proche x : b.f.proches) if (x.id == id) x.estime = valeur;
    }

    private void lierParents() {
        if (b.pere >= 0 && b.f.proche(Fiche.PERE) == null) b.f.lier(b.pere, Fiche.PERE, 0, 50, b.mortPere != AUCUNE);
        if (b.mere >= 0 && b.f.proche(Fiche.MERE) == null) b.f.lier(b.mere, Fiche.MERE, 1, 50, b.mortMere != AUCUNE);
    }

    private static final String[][] PETITE_ENFANCE = {
        { "Un enfant robuste, qui ne tombait jamais malade", "Tu étais un enfant robuste." },
        { "Tu grimpais partout et attrapais tout ce qu'on te lançait", "Tu grimpais partout." },
        { "Tu courais du matin au soir sans te fatiguer", "Tu courais du matin au soir." },
        { "Tu démontais et réparais les objets de la maison", "Tu démontais tout ce qui te tombait sous la main." },
        { "Tu posais sans cesse des questions", "Tu posais sans cesse des questions." },
        { "Les autres enfants te suivaient partout", "Les autres enfants te suivaient partout." },
        { "Tu savais toujours où étaient cachés les gâteaux", "Tu savais toujours où étaient cachés les gâteaux." }
    };

    private Scene petiteEnfance() {
        lierParents();
        Scene s = scene("Petite enfance", tranche(0, 6) + ". Quel enfant étais-tu ?", this::petiteEnfance);
        for (int a = 0; a < PETITE_ENFANCE.length; a++) {
            int attribut = a;
            option(s, PETITE_ENFANCE[a][0] + " (" + Fiche.NOMS_ATTRIBUTS[a] + " +1)", this::petiteEnfance, () -> {
                b.f.augmenter(attribut, 1);
                raconter(PETITE_ENFANCE[attribut][1]);
            }, this::enfance);
        }
        retour(s);
        return s;
    }

    private Scene enfance() {
        Origine x = ORIGINES[b.f.origine];
        Scene s = scene("Enfance", tranche(7, 12) + ". Que faisais-tu de tes journées ?", this::enfance);
        if (b.f.origine != ORPHELIN && b.f.origine != HORS_LA_LOI) {
            Effet ef = new Effet().c(x.competence, 15);
            option(s, "Tu aidais tes parents, qui " + x.phrase + " (" + ef.apport() + ")", this::enfance, () -> {
                ef.appliquer(b.f, 1f);
                for (Fiche.Proche q : b.f.proches) if (q.role == Fiche.PERE || q.role == Fiche.MERE) q.estime += 20;
                raconter("Enfant, tu aidais tes parents.");
            }, this::adolescence);
        }
        enfanceOption(s, "Tu gardais les bêtes dans les collines", new Effet().c(Fiche.SURVIE, 15).c(Fiche.ARC, 10), "Enfant, tu gardais les bêtes dans les collines.");
        if (b.f.origine == LETTRES || b.f.origine == NOBLES || habitants() >= 150) {
            enfanceOption(s, "Tu suivais un lettré qui t'apprenait à lire", new Effet().c(Fiche.LETTRES, 40).c(Fiche.ELOQUENCE, 5), "Enfant, un lettré t'a appris à lire.");
        }
        Effet bagarre = new Effet().c(Fiche.UNE_MAIN, 15).c(Fiche.ATHLETISME, 10);
        option(s, "Tu te battais avec les gamins du quartier (" + bagarre.apport() + ")", this::enfance, () -> {
            bagarre.appliquer(b.f, 1f);
            raconter("Enfant, tu te battais avec les gamins du quartier.");
        }, this::bagarre);
        if (Metiers.etals(profil()) > 0 || habitants() >= 120) {
            enfanceOption(s, "Tu chapardais au marché", new Effet().c(Fiche.DISCRETION, 20).c(Fiche.ATHLETISME, 5).x(Fiche.HONNETETE, -15), "Enfant, tu chapardais au marché.");
        }
        if (present(Metiers.AUBERGE) || present(Metiers.TAVERNE)) {
            enfanceOption(s, "Tu traînais à la taverne pour écouter les voyageurs", new Effet().c(Fiche.ELOQUENCE, 15).c(Fiche.COMMERCE, 5), "Enfant, tu traînais à la taverne pour écouter les voyageurs.");
        }
        enfanceOption(s, "Tu soignais les animaux blessés", new Effet().c(Fiche.MEDECINE, 15).x(Fiche.BIEN, 15), "Enfant, tu soignais les animaux blessés.");
        if (p.regions.peche[b.f.region] > 0f) {
            enfanceOption(s, "Tu pêchais avec les vieux sur la rive", new Effet().c(Fiche.SURVIE, 10).c(Fiche.ATHLETISME, 10).c(Fiche.JET, 5), "Enfant, tu pêchais avec les vieux sur la rive.");
        }
        retour(s);
        return s;
    }

    private void enfanceOption(Scene s, String libelle, Effet ef, String phrase) {
        option(s, libelle + " (" + ef.apport() + ")", this::enfance, () -> {
            ef.appliquer(b.f, 1f);
            raconter(phrase);
        }, this::adolescence);
    }

    private List<Integer> pairs(int ecart, int sexe, boolean seuls) {
        List<Integer> l = foule().gens(b.f.ville, b.f.naissance - ans(ecart), b.f.naissance + ans(ecart), sexe, seuls);
        l.removeIf(place -> foule().idDe(place) == b.pere || foule().idDe(place) == b.mere || estFrere(foule().idDe(place)));
        return l;
    }

    private boolean estFrere(long id) {
        for (Frere x : tousFreres()) if (x.id == id) return true;
        return false;
    }

    private Scene bagarre() {
        Random r = hasard(5);
        List<Integer> gamins = pairs(2, -1, false);
        if (gamins.isEmpty()) return adolescence();
        int a = gamins.get(r.nextInt(gamins.size()));
        int c = gamins.get(r.nextInt(gamins.size()));
        Foule f = foule();
        String nomA = nom(f.idDe(a), f.sexeDe(a)), nomC = nom(f.idDe(c), f.sexeDe(c));
        Scene s = scene("Enfance", "Dans ces bagarres, il y avait toujours…", this::bagarre);
        option(s, nomA + ", qui est devenu" + (f.sexeDe(a) == 1 ? "e" : "") + " ton ami" + (f.sexeDe(a) == 1 ? "e" : "") + " pour la vie", this::bagarre, () -> {
            b.f.lier(f.idDe(a), Fiche.AMI, f.sexeDe(a), 60, false);
            raconter("C'est là que tu t'es lié" + e() + " d'amitié avec " + nomA + ".");
        }, this::adolescence);
        option(s, nomC + ", qui ne t'a jamais pardonné une bagarre de trop", this::bagarre, () -> {
            b.f.lier(f.idDe(c), Fiche.RIVAL, f.sexeDe(c), -50, false);
            b.f.pencher(Fiche.COURAGE, 5);
            raconter("C'est là que " + nomC + " est devenu" + (f.sexeDe(c) == 1 ? "e" : "") + " ton rival" + (f.sexeDe(c) == 1 ? "e" : "") + ".");
        }, this::adolescence);
        retour(s);
        return s;
    }

    private static final class Maitre {
        final String libelle, phrase, avecNom, metierNom;
        final int competence, metier;

        Maitre(String libelle, String phrase, String avecNom, String metierNom, int competence, int metier) {
            this.libelle = libelle;
            this.phrase = phrase;
            this.avecNom = avecNom;
            this.metierNom = metierNom;
            this.competence = competence;
            this.metier = metier;
        }
    }

    private List<Maitre> maitres() {
        List<Maitre> l = new ArrayList<>();
        int[][] ateliers = { { Metiers.FORGE, Fiche.FORGE }, { Metiers.MENUISERIE, Fiche.BOIS }, { Metiers.TISSERAND, Fiche.METIERS_FINS }, { Metiers.POTIER, Fiche.METIERS_FINS },
                { Metiers.TANNERIE, Fiche.METIERS_FINS }, { Metiers.BOULANGERIE, Fiche.METIERS_FINS }, { Metiers.BOUCHERIE, Fiche.METIERS_FINS }, { Metiers.MOULIN, Fiche.INTENDANCE } };
        String[] noms = { "forgeron", "menuisier", "tisserand", "potier", "tanneur", "boulanger", "boucher", "meunier" };
        for (int k = 0; k < ateliers.length; k++) {
            if (present(ateliers[k][0])) l.add(new Maitre("Apprenti" + e() + " chez le " + noms[k], "apprenti" + e() + " chez le " + noms[k], "apprenti" + e() + " chez le " + noms[k] + " %s", noms[k], ateliers[k][1], ateliers[k][0]));
        }
        if (present(Metiers.CASERNE)) l.add(new Maitre("Au service du capitaine de la garde", "au service du capitaine de la garde", "au service de %s, capitaine de la garde", "capitaine de la garde", Fiche.UNE_MAIN, Metiers.CASERNE));
        if (Metiers.etals(profil()) > 0 || present(Metiers.ENTREPOT)) l.add(new Maitre("Commis d'un marchand", "commis d'un marchand", "commis de %s, qui tenait un étal au marché", "marchand", Fiche.COMMERCE, Metiers.ENTREPOT));
        if (present(Metiers.ECOLE) || habitants() >= 200) l.add(new Maitre("Élève d'un lettré", "élève d'un lettré", "élève de %s, qui t'a appris les lettres", "lettré", Fiche.LETTRES, Metiers.ECOLE));
        if (present(Metiers.AUBERGE) || present(Metiers.TAVERNE)) l.add(new Maitre("Servant" + tu("", "e") + " à l'auberge", "servant" + tu("", "e") + " à l'auberge", "servant" + tu("", "e") + " à l'auberge de %s", "aubergiste", Fiche.ELOQUENCE, Metiers.AUBERGE));
        l.add(new Maitre("Avec un chasseur, dans les bois", "aux côtés d'un chasseur", "aux côtés de %s, à chasser dans les bois", "chasseur", Fiche.ARC, -1));
        l.add(new Maitre("Avec une guérisseuse", "aux côtés d'une guérisseuse", "aux côtés de %s, qui soignait les malades", "guérisseur", Fiche.MEDECINE, -1));
        l.add(new Maitre("Aux champs, comme tout le monde", "aux champs", "aux champs", "", Fiche.LABEUR, -1));
        return l;
    }

    private Scene adolescence() {
        Random r = hasard(6);
        Scene s = scene("Adolescence", tranche(13, 16) + ". Chez qui as-tu fait ton apprentissage ?", this::adolescence);
        Foule f = foule();
        List<Integer> anciens = f.gens(b.f.ville, b.f.naissance - ans(45), b.f.naissance - ans(18), -1, false);
        for (Maitre m : maitres()) {
            int place = anciens.isEmpty() ? -1 : anciens.get(r.nextInt(anciens.size()));
            String nomMaitre = place >= 0 ? nom(f.idDe(place), f.sexeDe(place)) : null;
            String libelle = m.libelle + (nomMaitre != null && !m.libelle.startsWith("Aux champs") ? " : " + nomMaitre : "") + " (" + Fiche.NOMS_COMPETENCES[m.competence] + " +30, focus)";
            option(s, libelle, this::adolescence, () -> {
                b.f.gagner(m.competence, 30);
                b.f.concentrer(m.competence);
                b.metierAppris = m.competence;
                if (m.competence == Fiche.UNE_MAIN) b.f.gagner(Fiche.HAST, 10);
                if (m.competence == Fiche.ARC) b.f.gagner(Fiche.SURVIE, 10);
                if (m.competence == Fiche.LETTRES) b.f.gagner(Fiche.LETTRES, 10);
                if (place >= 0 && !m.libelle.startsWith("Aux champs")) {
                    Fiche.Proche q = b.f.lier(f.idDe(place), Fiche.MAITRE, f.sexeDe(place), 50, false);
                    q.note = m.metierNom;
                    raconter("Adolescent" + e() + ", tu as été " + String.format(m.avecNom, nomMaitre) + ".");
                } else raconter("Adolescent" + e() + ", tu as " + (m.phrase.equals("aux champs") ? "travaillé " : "été ") + m.phrase + ".");
            }, this::evenement);
        }
        retour(s);
        return s;
    }

    private static final int K_SAC = 0, K_CHUTE = 1, K_GUERRE = 2, K_POUVOIR = 3, K_RIEN = 4;

    private Scene evenement() {
        int debut = b.f.naissance + ans(11), fin = Math.min(p.annee, b.f.naissance + ans(18));
        int sac = sacPendant(debut, fin);
        int kind = K_RIEN, annee = debut + (fin - debut) / 2, ennemi = -1, chef = -1, etat = -1;
        long nouveau = -1L;
        if (sac >= 0) {
            kind = K_SAC;
            annee = p.villes.evAnnee[sac];
            ennemi = pillard(annee);
        } else {
            Chronique c = p.etats.chronique;
            int meilleur = -1, priorite = 99;
            for (int i = 0; i < c.nombre; i++) {
                if (c.annee[i] < debut || c.annee[i] > fin) continue;
                int e = maitreEn(c.annee[i]);
                if (e < 0) continue;
                int k = 99;
                switch (c.type[i]) {
                    case Chronique.CHUTE:
                    case Chronique.CONQUETE: if (c.objet[i] == e) k = K_CHUTE; break;
                    case Chronique.GUERRE:
                    case Chronique.SAC: if (c.sujet[i] == e || c.objet[i] == e) k = K_GUERRE; break;
                    case Chronique.SUCCESSION:
                    case Chronique.USURPATION:
                    case Chronique.REVOLUTION:
                    case Chronique.REGENCE:
                    case Chronique.CRISE: if (c.sujet[i] == e) k = K_POUVOIR; break;
                    default: break;
                }
                if (k < priorite) {
                    priorite = k;
                    meilleur = i;
                }
            }
            if (meilleur >= 0) {
                kind = priorite;
                annee = c.annee[meilleur];
                etat = maitreEn(annee);
                if (kind == K_CHUTE) ennemi = c.sujet[meilleur];
                if (kind == K_GUERRE) ennemi = c.sujet[meilleur] == etat ? c.objet[meilleur] : c.sujet[meilleur];
                if ((kind == K_GUERRE || kind == K_CHUTE) && ennemi < 0) kind = K_RIEN;
                if (kind == K_POUVOIR) {
                    nouveau = c.valeur[meilleur];
                    chef = c.type[meilleur];
                }
            }
        }
        int age = Math.max(1, annee - b.f.naissance);
        int an = annee, adversaire = ennemi;
        Supplier<Scene> ici = this::evenement;
        String ville = nomVille();
        String texte;
        Scene s;
        switch (kind) {
            case K_SAC: {
                String qui = ennemi >= 0 ? "les soldats " + de(nomEtat(ennemi)) : "des pillards";
                texte = "Tu avais " + age + " ans quand " + qui + " ont pris " + ville + ". La ville a brûlé, et " + p.villes.evValeur[sac] + " personnes y sont mortes.\n\nCe jour-là, tu…";
                s = scene("L'année où tout a changé", texte, ici);
                reaction(s, "as pris une arme pour défendre la ville", new Effet().c(Fiche.UNE_MAIN, 20).c(Fiche.HAST, 10).x(Fiche.COURAGE, 25), adversaire,
                        "En l'an " + an + ", quand " + qui + " ont saccagé " + ville + ", tu as pris une arme pour la défendre.");
                reaction(s, "as caché ta famille dans la cave", new Effet().c(Fiche.DISCRETION, 20).x(Fiche.BIEN, 10), adversaire,
                        "En l'an " + an + ", quand " + qui + " ont saccagé " + ville + ", tu as caché les tiens dans la cave.");
                reaction(s, "as fui dans les bois", new Effet().c(Fiche.SURVIE, 20).x(Fiche.COURAGE, -20), -1,
                        "En l'an " + an + ", quand " + qui + " ont saccagé " + ville + ", tu as fui dans les bois.");
                reaction(s, "as pillé les maisons abandonnées", new Effet().c(Fiche.DISCRETION, 10).or(300).x(Fiche.HONNETETE, -20).x(Fiche.GENEROSITE, -15), -1,
                        "En l'an " + an + ", pendant le sac de " + ville + ", tu as pillé les maisons abandonnées.");
                reaction(s, "as soigné les blessés jusqu'à la nuit", new Effet().c(Fiche.MEDECINE, 20).x(Fiche.BIEN, 20), adversaire,
                        "En l'an " + an + ", après le sac de " + ville + ", tu as soigné les blessés jusqu'à la nuit.");
                break;
            }
            case K_CHUTE: {
                String vainqueur = nomEtat(ennemi);
                texte = "Tu avais " + age + " ans quand " + ville + " est tombée sous la coupe " + de(vainqueur) + ". De nouveaux maîtres sont entrés dans la ville.\n\nEt toi, tu…";
                s = scene("L'année où tout a changé", texte, ici);
                reaction(s, "as juré de ne jamais plier", new Effet().c(Fiche.UNE_MAIN, 10).x(Fiche.COURAGE, 10).x(Fiche.LOI, -10), adversaire,
                        "En l'an " + an + ", quand " + ville + " est tombée sous la coupe " + de(vainqueur) + ", tu as juré de ne jamais plier.");
                reaction(s, "t'es fait" + e() + " aux nouveaux maîtres", new Effet().c(Fiche.ELOQUENCE, 15).x(Fiche.LOI, 10), -1,
                        "En l'an " + an + ", " + ville + " est tombée sous la coupe " + de(vainqueur) + ", et tu t'es fait" + e() + " aux nouveaux maîtres.");
                reaction(s, "as profité du désordre", new Effet().c(Fiche.DISCRETION, 15).or(150).x(Fiche.HONNETETE, -15), -1,
                        "En l'an " + an + ", quand " + ville + " est tombée, tu as profité du désordre.");
                break;
            }
            case K_GUERRE: {
                String a = nomEtat(etat), b2 = nomEtat(ennemi);
                texte = "Tu avais " + age + " ans quand la guerre a éclaté entre " + a + " et " + b2 + ". Les hommes partaient, les prix montaient.\n\nEt toi, tu…";
                s = scene("L'année où tout a changé", texte, ici);
                reaction(s, "as suivi l'armée comme porteur", new Effet().c(Fiche.ATHLETISME, 15).c(Fiche.HAST, 10).x(Fiche.COURAGE, 10), adversaire,
                        "En l'an " + an + ", pendant la guerre contre " + b2 + ", tu as suivi l'armée comme porteur.");
                reaction(s, "as vendu du pain et des bottes aux soldats", new Effet().c(Fiche.COMMERCE, 20).or(100), -1,
                        "En l'an " + an + ", pendant la guerre contre " + b2 + ", tu as vendu du pain et des bottes aux soldats.");
                reaction(s, "t'es caché" + e() + " pour échapper à la levée", new Effet().c(Fiche.DISCRETION, 15).x(Fiche.LOI, -10).x(Fiche.COURAGE, -10), -1,
                        "En l'an " + an + ", quand la guerre contre " + b2 + " a éclaté, tu t'es caché" + e() + " pour échapper à la levée.");
                reaction(s, "as prié chaque soir pour les soldats", new Effet().c(Fiche.ELOQUENCE, 10).x(Fiche.FOI, 20), -1,
                        "En l'an " + an + ", pendant la guerre contre " + b2 + ", tu as prié chaque soir pour les soldats.");
                break;
            }
            case K_POUVOIR: {
                String pays = nomEtat(etat);
                String maitre = Noms.personne(p.etats.peuple[etat], p.graine, nouveau, p.maisons.sexeDe(nouveau));
                String ee = p.maisons.sexeDe(nouveau) == 1 ? "e" : "";
                String comment = chef == Chronique.USURPATION ? "a pris le trône par la force" : chef == Chronique.REVOLUTION ? "a été renversé" + ee + " par le peuple" : "est monté" + ee + " sur le trône";
                if (chef == Chronique.REVOLUTION) texte = "Tu avais " + age + " ans quand " + maitre + ", qui régnait sur " + pays + ", " + comment + ".\n\nEt toi, tu…";
                else texte = "Tu avais " + age + " ans quand " + maitre + " " + comment + " " + de(pays) + ".\n\nEt toi, tu…";
                s = scene("L'année où tout a changé", texte, ici);
                reaction(s, "as crié avec la foule et fêté le changement", new Effet().c(Fiche.ELOQUENCE, 10).x(Fiche.LOI, 10), -1,
                        "En l'an " + an + ", tu as fêté avec la foule le nouveau maître " + de(pays) + ".");
                reaction(s, "t'en méfiais, et tu observais les puissants", new Effet().c(Fiche.TACTIQUE, 15).x(Fiche.AMBITION, 10), -1,
                        "En l'an " + an + ", quand le pouvoir a changé de mains à " + pays + ", tu as appris à observer les puissants.");
                reaction(s, "écoutais les mécontents à la taverne", new Effet().c(Fiche.ELOQUENCE, 10).c(Fiche.DISCRETION, 5).x(Fiche.LOI, -15), -1,
                        "En l'an " + an + ", quand le pouvoir a changé de mains à " + pays + ", tu écoutais les mécontents à la taverne.");
                break;
            }
            default: {
                texte = "Tu avais " + age + " ans quand un hiver terrible a frappé " + ville + ". Le grain manquait, les vieux mouraient.\n\nEt toi, tu…";
                s = scene("L'année où tout a changé", texte, ici);
                reaction(s, "partageais le peu que vous aviez", new Effet().c(Fiche.ELOQUENCE, 5).x(Fiche.BIEN, 15).x(Fiche.GENEROSITE, 20), -1,
                        "L'hiver de tes " + age + " ans, quand la faim est venue, tu as partagé le peu que vous aviez.");
                reaction(s, "volais pour que les tiens mangent", new Effet().c(Fiche.DISCRETION, 15).x(Fiche.HONNETETE, -15), -1,
                        "L'hiver de tes " + age + " ans, quand la faim est venue, tu as volé pour que les tiens mangent.");
                reaction(s, "chassais dans la neige", new Effet().c(Fiche.SURVIE, 15).c(Fiche.ARC, 10), -1,
                        "L'hiver de tes " + age + " ans, quand la faim est venue, tu as chassé dans la neige.");
                reaction(s, "travaillais deux fois plus", new Effet().c(Fiche.LABEUR, 15).a(Fiche.ENDURANCE), -1,
                        "L'hiver de tes " + age + " ans, quand la faim est venue, tu as travaillé deux fois plus.");
                break;
            }
        }
        retour(s);
        return s;
    }

    private void reaction(Scene s, String libelle, Effet ef, int rancune, String phrase) {
        option(s, "Tu " + libelle + " (" + ef.apport() + ")", this::evenement, () -> {
            ef.appliquer(b.f, 1f);
            if (rancune >= 0) b.f.ennemi = rancune;
            raconter(phrase);
        }, this::premierAmour);
    }

    private Scene premierAmour() {
        Random r = hasard(7);
        Foule f = foule();
        List<Integer> amours = pairs(3, 1 - b.f.sexe, false);
        List<Integer> amis = pairs(3, -1, false);
        Scene s = scene("Adolescence", "À " + ans(16) + " ans, ton cœur appartenait à…", this::premierAmour);
        if (!amours.isEmpty()) {
            int a = amours.get(r.nextInt(amours.size()));
            String n = nom(f.idDe(a), f.sexeDe(a));
            boolean marie = f.conjointDe(a) >= 0;
            option(s, n + ", ton premier amour" + (marie ? " (qui en a épousé un" + (f.sexeDe(a) == 1 ? "" : "e") + " autre depuis)" : ""), this::premierAmour, () -> {
                b.f.lier(f.idDe(a), Fiche.AMOUR, f.sexeDe(a), 60, false);
                raconter("Ton premier amour fut " + n + (marie ? ", qui en a épousé un" + (f.sexeDe(a) == 1 ? "" : "e") + " autre depuis." : "."));
            }, this::jeunesse);
        }
        if (!amis.isEmpty()) {
            int a = amis.get(r.nextInt(amis.size()));
            String n = nom(f.idDe(a), f.sexeDe(a));
            String fidele = f.sexeDe(a) == 1 ? "ton amie la plus fidèle" : "ton ami le plus fidèle";
            option(s, n + ", " + fidele, this::premierAmour, () -> {
                b.f.lier(f.idDe(a), Fiche.AMI, f.sexeDe(a), 70, false);
                raconter(Character.toUpperCase(fidele.charAt(0)) + fidele.substring(1) + " était " + n + ".");
            }, this::jeunesse);
        }
        option(s, "Personne : tu restais seul" + e() + " avec tes pensées (Lettres +5, Survie +5)", this::premierAmour, () -> {
            b.f.gagner(Fiche.LETTRES, 5);
            b.f.gagner(Fiche.SURVIE, 5);
            raconter("Tu as grandi seul" + e() + " avec tes pensées.");
        }, this::jeunesse);
        retour(s);
        return s;
    }

    private boolean voiePossible(int v) {
        switch (v) {
            case COMPAGNON: return b.metierAppris == Fiche.FORGE || b.metierAppris == Fiche.BOIS || b.metierAppris == Fiche.METIERS_FINS || b.metierAppris == Fiche.INTENDANCE;
            case ETUDES: return b.f.competences[Fiche.LETTRES] >= 30;
            case MARIN: return p.regions.peche[b.f.region] > 0f;
            default: return true;
        }
    }

    private void appliquerVoie(int v, float part) {
        EFFETS_VOIES[v].appliquer(b.f, part);
        if (v == COMPAGNON) {
            b.f.gagner(b.metierAppris, Math.round(30 * part));
            b.f.concentrer(b.metierAppris);
        }
    }

    private Scene jeunesse() {
        Scene s = scene("Jeunesse", tranche(17, 20) + ". Quelle voie as-tu prise ?", this::jeunesse);
        for (int v = 0; v < VOIES.length; v++) {
            if (!voiePossible(v)) continue;
            int voie = v;
            option(s, VOIES[v] + " (" + VOIES_APPORT[v] + ")", this::jeunesse, () -> {
                b.f.voie = voie;
                appliquerVoie(voie, 1f);
                if (voie == MILICE || voie == MERCENAIRE || voie == BRIGAND) camarade(voie);
                raconter(String.format("Jeune, tu " + VOIES_RECIT[voie] + ".", e()));
            }, () -> b.ageDepart > 20 ? decennie(1) : aspirations());
        }
        retour(s);
        return s;
    }

    private void camarade(int voie) {
        Random r = hasard(8);
        List<Integer> l = pairs(4, -1, false);
        if (l.isEmpty()) return;
        int c = l.get(r.nextInt(l.size()));
        Foule f = foule();
        Fiche.Proche q = b.f.lier(f.idDe(c), Fiche.CAMARADE, f.sexeDe(c), 40, false);
        q.note = voie == MILICE ? "de la milice" : voie == MERCENAIRE ? "de la compagnie" : "de la bande des bois";
    }

    private Scene decennie(int k) {
        int debutAge = 10 + 10 * k, finAge = debutAge + 10;
        Supplier<Scene> ici = () -> decennie(k);
        int ages = Math.round(debutAge / rythme()), agesFin = Math.round(finAge / rythme());
        Scene s = scene("Les années suivantes", "De " + ages + " à " + agesFin + " ans. Qu'as-tu fait de ces années ?", ici);
        for (int v = 0; v < VOIES.length; v++) {
            if (!voiePossible(v)) continue;
            int voie = v;
            boolean meme = v == b.f.voie;
            option(s, (meme ? "Continuer : " : "Changer : ") + VOIES[v].toLowerCase(), ici, () -> {
                b.f.voie = voie;
                appliquerVoie(voie, 0.8f);
                b.f.argent += EFFETS_VOIES[voie].argent * 2;
                raconter("De " + ages + " à " + agesFin + " ans, tu " + (meme ? "as continué dans cette voie" : String.format(VOIES_RECIT[voie], e())) + ".");
            }, () -> k == 1 ? mariage() : k == 2 && b.ageDepart > 40 ? decennie(3) : aspirations());
        }
        retour(s);
        return s;
    }

    private Scene mariage() {
        Random r = hasard(9);
        Foule f = foule();
        List<Integer> l = pairs(8, 1 - b.f.sexe, true);
        Scene s = scene("Les années suivantes", "As-tu pris époux ou épouse ?", this::mariage);
        Supplier<Scene> suite = this::enfants;
        for (int k = 0; k < Math.min(3, l.size()); k++) {
            int c = l.remove(r.nextInt(l.size()));
            String n = nom(f.idDe(c), f.sexeDe(c));
            int age = Math.round(ageEn(f.naissanceDe(c), p.annee) / rythme());
            option(s, "Tu as épousé " + n + ", " + age + " ans", this::mariage, () -> {
                b.conjoint = f.idDe(c);
                b.neConjoint = f.naissanceDe(c);
                b.f.lier(f.idDe(c), Fiche.CONJOINT, f.sexeDe(c), 60, false);
                raconter("Tu as épousé " + n + ".");
            }, suite);
        }
        if (l.isEmpty()) {
            option(s, "Tu t'es marié" + e() + " (une personne de la ville)", this::mariage, () -> {
                Random h = hasard(10);
                int ne = b.f.naissance + (h.nextBoolean() ? 1 : -1) * h.nextInt(Math.max(1, ans(6)));
                Ajout x = creer(h, 1 - b.f.sexe, ne, -1L, -1L, caractereHasard(h));
                x.conjoint = b.f.id;
                b.conjoint = x.id;
                b.neConjoint = ne;
                b.f.lier(x.id, Fiche.CONJOINT, x.sexe, 60, false);
                raconter("Tu as épousé " + nom(x.id, x.sexe) + ".");
            }, suite);
        }
        option(s, "Tu as aimé quelqu'un, mais la mort te l'a pris", this::mariage, () -> {
            long id = nouvelId();
            b.f.lier(id, Fiche.CONJOINT, 1 - b.f.sexe, 60, true);
            b.f.pencher(Fiche.FOI, 10);
            raconter("Tu as épousé " + nom(id, 1 - b.f.sexe) + ", que la mort t'a pris" + (b.f.sexe == 0 ? "e" : "") + " trop tôt.");
        }, suite);
        option(s, "Tu es resté" + e() + " seul" + e(), this::mariage, () -> raconter("Tu ne t'es jamais marié" + e() + "."), suite);
        retour(s);
        return s;
    }

    private Scene enfants() {
        Fiche.Proche conjoint = b.f.proche(Fiche.CONJOINT);
        Supplier<Scene> suite = () -> b.ageDepart > 30 ? decennie(2) : aspirations();
        if (conjoint == null) return suite.get();
        Scene s = scene("Les années suivantes", "As-tu eu des enfants ?", this::enfants);
        int[][] tailles = { { 0, 0 }, { 1, 1 }, { 2, 3 }, { 4, 5 } };
        String[] libelles = { "Aucun", "Un enfant", "Deux ou trois", "Quatre ou plus" };
        for (int k = 0; k < tailles.length; k++) {
            int[] t = tailles[k];
            option(s, libelles[k], this::enfants, () -> {
                Random r = hasard(11);
                int n = t[0] + r.nextInt(t[1] - t[0] + 1);
                int debut = b.f.naissance + ans(22), fin = Math.min(p.annee - 1, b.f.naissance + ans(b.ageDepart - 1));
                if (fin <= debut) n = 0;
                List<String> noms = new ArrayList<>();
                for (int i = 0; i < n; i++) {
                    int ne = debut + r.nextInt(fin - debut + 1);
                    int[] c = new int[5];
                    for (int j = 0; j < 5; j++) c[j] = Math.max(-100, Math.min(100, b.f.axes[j] + Math.round((r.nextFloat() + r.nextFloat() - 1f) * 30f)));
                    long pere = b.f.sexe == 0 ? b.f.id : conjoint.id, mere = b.f.sexe == 0 ? conjoint.id : b.f.id;
                    Ajout a = creer(r, r.nextBoolean() ? 1 : 0, ne, pere, mere, c);
                    a.maison = b.maisonFamille;
                    a.rang = Math.min(3, b.rangFamille + 1);
                    b.f.lier(a.id, Fiche.ENFANT, a.sexe, 60, false);
                    noms.add(nom(a.id, a.sexe));
                }
                if (n == 0) raconter("Tu n'as pas eu d'enfant.");
                else raconter(n == 1 ? "Tu as eu un enfant : " + noms.get(0) + "." : "Tu as eu " + n + " enfants : " + String.join(", ", noms) + ".");
            }, suite);
        }
        retour(s);
        return s;
    }

    private Scene aspirations() {
        String texte = "Que veux-tu faire de ta vie ? C'est ton aspiration : le grand but que tu poursuis. Tu pourras en changer plus tard, aux grands moments de ta vie.";
        if (b.f.ennemi >= 0) texte += "\n\nTu n'as pas oublié " + nomEtat(b.f.ennemi) + ".";
        Scene s = scene("Aspiration", texte, this::aspirations);
        if (b.f.ennemi >= 0) {
            s.ajouter("Vengeance contre " + nomEtat(b.f.ennemi), () -> aspiration(Aspirations.VENGEANCE));
        }
        for (int g = 0; g < Aspirations.GROUPES; g++) {
            int groupe = g;
            s.ajouter(Aspirations.GROUPES_NOMS[g] + " — " + Aspirations.GROUPES_TEXTES[g], () -> groupe(groupe));
        }
        retour(s);
        return s;
    }

    private Scene groupe(int g) {
        Scene s = scene("Aspiration", Aspirations.GROUPES_NOMS[g] + " : " + Aspirations.GROUPES_TEXTES[g], () -> groupe(g));
        for (int k : Aspirations.du(g)) {
            Aspirations.Aspiration x = Aspirations.TOUTES.get(k);
            s.ajouter(x.nom + " — " + x.texte, () -> aspiration(k));
        }
        s.ajouter("« Revenir", this::aspirations);
        return s;
    }

    private Scene aspiration(int k) {
        Aspirations.Aspiration x = Aspirations.TOUTES.get(k);
        String texte = Aspirations.decrire(k, 0);
        if (k == Aspirations.VENGEANCE && b.f.ennemi >= 0) texte = "Contre " + nomEtat(b.f.ennemi) + ". " + texte;
        texte += "\n\nBonus : " + Fiche.NOMS_COMPETENCES[x.competence] + " +10 et un point de focus.";
        Scene s = scene(x.nom, texte, () -> aspiration(k));
        option(s, "Choisir cette aspiration", () -> aspiration(k), () -> {
            b.f.aspiration = k;
            b.f.etape = 0;
            b.f.gagner(x.competence, 10);
            b.f.concentrer(x.competence);
            if (x.penchant != 0) b.f.pencher(x.axe, x.penchant / 2);
            raconter(k == Aspirations.VENGEANCE && b.f.ennemi >= 0 ? "Aujourd'hui, une seule chose compte : faire payer " + nomEtat(b.f.ennemi) + "."
                    : "Aujourd'hui, tu n'as qu'un but : « " + x.nom + " ».");
        }, this::recit);
        s.ajouter("« Revenir", () -> groupe(x.groupe));
        return s;
    }

    private Scene recit() {
        String texte = String.join(" ", b.f.recit) + "\n\n" + b.f.resume();
        Scene s = scene(b.f.nom, texte, this::recit);
        s.ajouter("Entrer dans le monde", this::entrer);
        retour(s);
        return s;
    }

    private Scene entrer() {
        Foule f = foule();
        Ajout moi = new Ajout();
        moi.id = b.f.id;
        moi.pere = b.pere;
        moi.mere = b.mere;
        moi.conjoint = b.conjoint;
        moi.naissance = b.f.naissance;
        moi.sexe = b.f.sexe;
        moi.talent = Math.min(100, 20 + b.f.attributs[Fiche.ESPRIT] * 8);
        moi.ville = b.f.ville;
        moi.maison = b.maisonFamille;
        moi.rang = b.rangFamille;
        for (int k = 0; k < 5; k++) moi.caractere[k] = b.f.axes[k];
        List<Ajout> tous = new ArrayList<>();
        tous.add(moi);
        tous.addAll(b.ajouts);
        for (Ajout a : tous) a.inserer(f);
        Joueur.unir(f, tous);
        b.f.apparence = Aspect.graine(b.f.id, b.f.aspect.visage);
        b.f.sac.porterLeMieux();
        joueur.adopter(b.f, tous, b.prochain);
        return joueur.ouvrir();
    }
}
