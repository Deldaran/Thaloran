package com.deldaran.politics.histoire;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class Recit {
    private static final String[][] RANGS = { { "le seigneur", "la dame" }, { "le thane", "la thane" }, { "le roi", "la reine" }, { "le chef de guerre", "la cheffe de guerre" } };
    private static final String[] SIGNATURES = { "Annales écrites par ", "Vie écrite par ", "Récit écrit par ", "Complainte écrite par ", "", "Généalogie écrite par " };
    private static final String[] FAMILLES = { "la lignée", "le clan", "la maison", "la horde" };
    private static final String[][] ANCIENS = {
        { "Les aînés se souviennent que ", "Il nous a été transmis que ", "Les feuilles murmurent encore que " },
        { "Les anciens de la forge disent que ", "Il est gravé dans nos mémoires que ", "Mon clan raconte que " },
        { "Les vieux disent que ", "On raconte que ", "Ma grand-mère disait que " },
        { "Les vieux crachent que ", "Au feu, on dit que ", "Les tambours racontent que " }
    };
    private static final String[] REFRAINS = {
        "Sous les feuilles d'argent, nous gardons leur nom.",
        "Frappe, marteau, que la pierre s'en souvienne !",
        "Buvons, compagnons, à ceux qui sont partis !",
        "Frappez, frappez, que tremble la terre !"
    };
    private static final String[] PLAINTES = {
        "Et nul ne sait plus où pleurer ses morts.",
        "Les hirondelles ne reviennent plus sur ces murs.",
        "Qui dira leurs noms quand nous ne serons plus ?"
    };

    private Recit() {
    }

    private static String de(String nom) {
        return ("AEIOUYaeiouy".indexOf(nom.charAt(0)) >= 0 ? "d'" : "de ") + nom;
    }
    private static String du(String famille) {
        return famille.startsWith("le ") ? "du " + famille.substring(3) : "de " + famille;
    }


    private static String majuscule(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static int degre(int stade) {
        return stade < 2 ? 0 : stade == 2 ? 1 : 3;
    }

    private static String choix(Savoir.Souvenir s, String... phrases) {
        return phrases[(int) (Savoir.tirage(s.graine, phrases.length) * phrases.length)];
    }

    private static String etat(Population p, int e, Savoir.Souvenir s) {
        String nom = Noms.ville(p.etats.peuple[e], p.graine, p.etats.capitale[e]);
        return Noms.deformer(nom, (s.graine & 3L) * 1000003L + e, degre(s.stade));
    }

    private static String chef(Population p, int e, long id, Savoir.Souvenir s) {
        String nom = Noms.personne(p.etats.peuple[e], p.graine, id, p.maisons.sexeDe(id));
        return Noms.deformer(nom, (s.graine & 3L) * 1000003L + id, degre(s.stade));
    }

    private static String lier(String debut, String phrase) {
        if (debut.endsWith(": ")) return debut + majuscule(phrase);
        boolean voyelle = "AEIOUYaeiouy".indexOf(phrase.charAt(0)) >= 0;
        if (voyelle && debut.endsWith(" que ")) return debut.substring(0, debut.length() - 2) + "'" + phrase;
        return debut + phrase;
    }

    private static String rang(Population p, int e, boolean femme) {
        return RANGS[p.etats.peuple[e]][femme ? 1 : 0];
    }

    public static String nomMaison(Population p, int m) {
        Maisons x = p.maisons;
        long fondateur = x.fondateur[m];
        return FAMILLES[x.peuple[m]] + " " + de(Noms.personne(x.peuple[m], p.graine, fondateur, x.sexeDe(fondateur)));
    }

    private static String heritage(Population p, int e, long id, Savoir.Souvenir s, String a) {
        Maisons x = p.maisons;
        String nom = chef(p, e, id, s);
        int r = x.regneDe(id);
        int avant = r < 0 ? -1 : x.precedent[r];
        if (avant < 0 || x.lien[r] == Maisons.ETRANGER) return choix(s, nom + " a pris le pouvoir à " + a, nom + " est monté" + (x.sexeDe(id) == 1 ? "e" : "") + " sur le trône " + de(a));
        String ancien = chef(p, e, x.personne[avant], s);
        if (x.lien[r] == Maisons.ENFANT) return nom + " a succédé à " + (x.sexe[avant] == 1 ? "sa mère " : "son père ") + ancien + " sur le trône " + de(a);
        return nom + ", de la famille " + de(ancien) + ", a hérité du trône " + de(a);

    }


    private static String morts(long nombre, float ampleur, int stade) {
        long n = Math.round(nombre * ampleur);
        if (stade >= 3) return "plus de gens qu'il n'y a d'étoiles";
        if (stade == 2) return "des milliers de gens";
        if (stade == 1) {
            long arrondi = n < 100 ? 100 : n < 1000 ? n / 100 * 100 : n / 1000 * 1000;
            return "quelque " + arrondi + " personnes";
        }
        return n + " personnes";
    }

    public static String fait(Population p, Savoir.Souvenir s, int ton) {
        Chronique c = p.etats.chronique;
        int i = s.fait;
        int sujet = c.sujet[i];
        int objet = c.objet[i];
        String a = etat(p, sujet, s);
        String b = objet >= 0 ? etat(p, objet, s) : "";
        int stade = s.stade;
        boolean femme = Maisons.porteRegnant(c.type[i]) && p.maisons.sexeDe(c.valeur[i]) == 1;
        String e1 = femme ? "e" : "";

        switch (c.type[i]) {
            case Chronique.FONDATION: {
                String nom = chef(p, sujet, c.valeur[i], s);
                if (stade >= 3) return choix(s, (femme ? "la déesse " : "le dieu ") + nom + " a tiré " + a + " du néant de ses propres mains", nom + (femme ? ", fille du ciel," : ", fils du ciel,") + " a dressé " + a + " en une nuit");
                if (stade == 2) return nom + (femme ? " la Grande" : " le Grand") + " a bâti " + a + " en une seule saison";
                return choix(s, nom + " a fondé " + a, a + " est née de la volonté de " + nom);
            }
            case Chronique.SUCCESSION:
                return heritage(p, sujet, c.valeur[i], s, a);
            case Chronique.CRISE: {
                String nom = chef(p, sujet, c.valeur[i], s);
                if (stade >= 2) return "le sang des anciens maîtres " + de(a) + " s'est tari, et " + nom + " a saisi la couronne";
                return choix(s, "sans héritier, le trône " + de(a) + " est passé à " + nom, "la lignée " + de(a) + " s'est éteinte, et " + nom + " a pris le pouvoir");
            }
            case Chronique.REGENCE: {
                String nom = chef(p, sujet, c.valeur[i], s);
                return choix(s, "l'enfant " + nom + " a hérité du trône " + de(a) + ", et d'autres ont gouverné à sa place", a + " a été confiée à des régents au nom " + (femme ? "de la jeune " : "du jeune ") + nom);
            }
            case Chronique.USURPATION: {
                String nom = chef(p, sujet, c.valeur[i], s);
                int r = p.maisons.regneDe(c.valeur[i]);
                int avant = r < 0 ? -1 : p.maisons.precedent[r];
                String victime = avant < 0 ? "l'héritier légitime" : chef(p, sujet, p.maisons.personne[avant], s);
                if (stade >= 3) return nom + " a égorgé les siens sous les yeux des dieux pour régner sur " + a;
                if (ton < 0) return nom + " a fait assassiner " + victime + ", de son propre sang, pour voler le trône " + de(a);
                return nom + " a écarté " + victime + " et pris le trône " + de(a);
            }

            case Chronique.GUERRE: {
                String cause = c.lieu[i] >= 0 && stade < 2 ? " pour son " + Economie.NOMS[c.lieu[i]] : "";
                if (stade >= 3) return "les dieux " + de(a) + " ont déclaré la guerre à ceux " + de(b);
                if (stade == 2) return "une guerre de cent ans a opposé " + a + " à " + b;
                if (ton > 0) return choix(s, a + " a porté une juste guerre à " + b + cause, a + " a marché sur " + b + cause);
                if (ton < 0) return choix(s, a + " a lâchement attaqué " + b + cause, "les armées " + de(a) + " ont envahi " + b + " sans raison");
                return choix(s, a + " a attaqué " + b + cause, "la guerre a éclaté entre " + a + " et " + b);
            }
            case Chronique.PAIX:
                return "la paix s'est faite entre " + a + " et " + b;
            case Chronique.CONQUETE:
                return a + " a pris une province à " + b;
            case Chronique.SAC: {
                if (stade >= 3) return "le sang a coulé cent jours quand " + a + " a écrasé " + b;
                String victimes = morts(c.valeur[i], s.ampleur, stade);
                if (ton > 0) return choix(s, "les soldats " + de(a) + " ont châtié une ville " + de(b) + ", où " + victimes + " ont payé", a + " a soumis une ville " + de(b) + " par le fer, et " + victimes + " y ont laissé la vie");
                if (ton < 0) return choix(s, "les bouchers " + de(a) + " ont mis à sac une ville " + de(b) + " et massacré " + victimes, "une ville " + de(b) + " a brûlé sous les torches " + de(a) + ", et " + victimes + " y ont laissé la vie");
                return choix(s, a + " a mis à sac une ville " + de(b) + ", et " + victimes + " y ont péri", "une ville " + de(b) + " a été pillée par " + a + ", et " + victimes + " y ont laissé la vie");
            }
            case Chronique.CHUTE: {
                if (stade >= 3) return choix(s, "les dieux ont englouti " + a + " pour punir son orgueil", a + " a sombré sous la terre en un seul jour");
                if (stade == 2) return choix(s, a + " la superbe a été rasée en une seule nuit par les armées " + de(b), de(a) + " la grande, il n'est resté que des pierres");
                if (ton > 0) return choix(s, b + " a abattu l'orgueil " + de(a), a + " a enfin plié le genou devant " + b);
                if (ton < 0) return choix(s, a + " est tombée sous les coups des barbares " + de(b), "les murs " + de(a) + " sont tombés, et " + b + " y règne");
                return choix(s, a + " est tombée devant " + b, b + " a pris " + a);
            }
            case Chronique.REVOLUTION: {
                String nom = chef(p, sujet, c.valeur[i], s);
                String titre = rang(p, sujet, femme) + " " + nom;
                if (stade >= 3) return choix(s, nom + " a été changé" + e1 + " en bête, et le peuple " + de(a) + " l'a chassé" + e1, nom + " a défié le ciel, et la foudre l'a brûlé" + e1 + " devant les portes " + de(a));
                if (stade == 2) return choix(s, nom + (femme ? " la Maudite a été jetée" : " le Maudit a été jeté") + " du haut des murs " + de(a), "on a pendu" + e1 + " " + nom + (femme ? " la Cruelle" : " le Cruel") + " aux portes " + de(a));
                if (ton < 0) return choix(s, "la canaille " + de(a) + " a assassiné " + titre, "une foule enragée a égorgé " + titre + " dans son palais " + de(a), "des traîtres ont poignardé " + titre + " à " + a, "le peuple " + de(a) + " s'est soulevé, et " + titre + " y a perdu la vie");
                return choix(s, "le peuple " + de(a) + " a renversé et tué " + titre, a + " s'est soulevée contre " + titre + ", qui n'y a pas survécu", titre + " a fini sous les coups de son propre peuple, à " + a);
            }
            case Chronique.SECESSION:
                if (stade >= 2) return a + " a brisé ses chaînes et chassé les maîtres venus " + de(b);
                if (ton < 0) return choix(s, a + " a trahi " + b, a + " a renié sa parole envers " + b);
                return choix(s, a + " s'est libérée " + de(b), a + " a rompu avec " + b);
            default:
                return a + " s'est éteinte, sans personne pour la gouverner";
        }
    }

    private static String quand(Population p, Savoir.Temoin t, Savoir.Souvenir s) {
        int age = p.annee - s.annee;
        float rythme = p.reglages.peuples[t.peuple].rythme;
        if (s.annee >= t.naissance) {
            if (age <= 1) return "l'an dernier";
            if (age * rythme < 8f) return "il y a quelques années";
            float alors = (s.annee - t.naissance) * rythme;
            if (alors < 14f) return "quand j'étais enfant";
            if (alors < 30f) return "quand j'étais jeune";
            return "il y a bien des années";
        }
        if (t.instruction >= 50 && s.stade < 2) {
            if (t.etat >= 0 && s.annee >= p.etats.fondation[t.etat]) {
                String ici = Noms.ville(p.etats.peuple[t.etat], p.graine, p.etats.capitale[t.etat]);
                return "en l'an " + (s.annee - p.etats.fondation[t.etat] + 1) + " " + de(ici);
            }
            return "il y a environ " + Math.max(10, age / 10 * 10) + " ans";
        }
        float generations = age * rythme / 25f;
        if (generations < 2f) return "du temps de mes parents";
        if (generations < 4f) return "du temps de mes grands-parents";
        if (generations < 15f) return "il y a bien longtemps";
        return "aux temps anciens";
    }

    private static String ouverture(Population p, Savoir.Temoin t, Savoir.Souvenir s) {
        Livres l = p.livres;
        if (s.vecu) return choix(s, "Je m'en souviens bien : ", "Je l'ai vécu : ", "J'y étais : ");
        if (s.source >= 0) {
            if (l.ecrit[s.source]) return "Il est écrit dans \"" + titre(p, s.source) + "\" que ";
            return "Comme dit \"" + titre(p, s.source) + "\", ";
        }
        if (s.annee >= t.naissance) return choix(s, "Des voyageurs racontent que ", "Un marchand m'a dit que ", "La nouvelle est venue que ");
        if (t.instruction < 20) return ANCIENS[t.peuple][(int) (Savoir.tirage(t.id, s.fait) * 3f)];
        return choix(s, "On dit que ", "J'ai entendu dire que ", "Il paraît que ");
    }

    private static String avis(Population p, Savoir.Temoin t, Savoir.Souvenir s, int ton) {
        byte quoi = p.etats.chronique.type[s.fait];
        if (quoi == Chronique.REVOLUTION && t.loi > 30) return " Le désordre ne mène à rien de bon.";
        if (quoi == Chronique.REVOLUTION && t.loi < -30) return " Bien fait.";
        if (ton < 0 && t.bien > 20) return " Que les morts reposent.";
        if (ton < 0 && t.bien < -20) return " Un jour, on le leur fera payer.";
        if (ton > 0 && t.bien < -20) return " Ils n'ont eu que ce qu'ils méritaient.";
        return "";
    }

    public static String parler(Population p, Savoir.Temoin t, List<Savoir.Souvenir> souvenirs, int nombre) {
        if (souvenirs.isEmpty()) return "Je ne sais pas grand-chose de tout ça. Ici, on s'occupe de ses bêtes et de sa récolte.";
                List<String> phrases = paroles(p, t, souvenirs);
        return String.join("\n\n", phrases.subList(0, Math.min(nombre, phrases.size())));
    }

    public static List<String> paroles(Population p, Savoir.Temoin t, List<Savoir.Souvenir> souvenirs) {
        List<String> phrases = new ArrayList<>();
        LinkedHashSet<Integer> sources = new LinkedHashSet<>();
        for (Savoir.Souvenir s : souvenirs) {
            if (s.source >= 0 && !sources.add(s.source)) continue;
            int ton = Savoir.ton(p, t.etat, t.peuple, s.fait);
            phrases.add(lier(ouverture(p, t, s), fait(p, s, ton)) + ", " + quand(p, t, s) + "." + avis(p, t, s, ton));
        }
        return phrases;
    }

    public static String nom(Population p, Savoir.Temoin t) {
        return Noms.personne(t.peuple, p.graine, t.id, t.sexe);
    }

    private static String lieuDe(Population p, Savoir.Temoin t) {
        return t.ville >= 0 ? Noms.ville(p.villes.peuple[t.ville], p.graine, t.ville) : "la campagne";
    }

    public static String decrire(Population p, Savoir.Temoin t) {
        StringBuilder texte = new StringBuilder(nom(p, t));
        if (t.etat >= 0 && p.etats.chef[t.etat] == t.id) texte.append(", ").append(rang(p, t.etat, t.sexe == 1));
        else if (t.maison >= 0) texte.append(", ").append(du(nomMaison(p, t.maison)));
        return texte.append(", ").append(p.annee - t.naissance).append(" ans").toString();
    }

    public static String sePresenter(Population p, Savoir.Temoin t) {
        StringBuilder texte = new StringBuilder("Je m'appelle ").append(nom(p, t));
        if (t.maison >= 0) texte.append(", ").append(du(nomMaison(p, t.maison)));
        texte.append(". J'ai ").append(p.annee - t.naissance).append(" ans et je vis à ").append(lieuDe(p, t)).append('.');
        if (t.instruction >= 50) texte.append(" J'ai passé ma vie parmi les livres.");
        else if (t.instruction >= p.livres.reglages.lecture) texte.append(" Je sais lire, et ça me suffit.");
        else if (t.instruction < 20) texte.append(" Les lettres, ce n'est pas pour moi.");
        return texte.toString();
    }

    public static String gouvernement(Population p, Savoir.Temoin t) {
        if (t.etat < 0) return "Ici, personne ne commande. On se débrouille entre nous.";
        Etats e = p.etats;
        int q = e.peuple[t.etat];
        long id = e.chef[t.etat];
        boolean femme = p.maisons.sexeDe(id) == 1;
        String nom = Noms.personne(q, p.graine, id, femme ? 1 : 0);
        String pays = Noms.ville(q, p.graine, e.capitale[t.etat]);
        int duree = p.annee - p.maisons.debut[e.regne[t.etat]];
        String depuis = duree <= 1 ? "depuis peu" : "depuis " + duree + " ans";
        if (id == t.id) return "Ici, c'est moi qui gouverne, " + depuis + ". " + majuscule(pays) + " m'obéit.";
        String texte = "Nous dépendons " + de(pays) + ". " + majuscule(rang(p, t.etat, femme)) + " " + nom + ", " + du(nomMaison(p, e.maison[t.etat])) + ", y règne " + depuis + ".";
        return q == t.peuple ? texte : texte + " Ce ne sont pas des nôtres.";
    }

    public static String saluer(Population p, Savoir.Temoin t) {
        if (t.etat >= 0 && p.etats.chef[t.etat] == t.id) return "Tu oses m'adresser la parole. Parle, et sois bref.";
        if (t.bien < -20) return "Qu'est-ce que tu me veux ?";
        if (t.loi > 30) return "Le bonjour. Que puis-je pour toi ?";
        return "Oui ? Je t'écoute.";
    }

    public static String presenter(Population p, Savoir.Temoin t) {
        String nom = nom(p, t);
        if (t.maison >= 0) nom += ", " + du(nomMaison(p, t.maison));
        int age = p.annee - t.naissance;
        String lieu = lieuDe(p, t);

        String e1 = t.sexe == 1 ? "e" : "";
        String culture = t.instruction >= 50 ? "lettré" + e1 : t.instruction >= p.livres.reglages.lecture ? "sait lire" : t.instruction >= 20 ? "peu instruit" + e1 : "illettré" + e1;
        return nom + ", " + p.reglages.peuples[t.peuple].nom + ", " + age + " ans, " + lieu + " (" + culture + ", instruction " + t.instruction + ")";
    }


    private static Savoir.Temoin chercher(Population p, int genre, long tirage) {
        java.util.Random hasard = new java.util.Random(tirage);
        Villes v = p.villes;
        int age = p.reglages.ageAdulte;
        Savoir.Temoin meilleur = null;
        for (int essai = 0; essai < 400; essai++) {
            int ville = -1;
            int region;
            int q;
            if (genre == 0) {
                region = hasard.nextInt(p.regions.nombre);
                q = hasard.nextInt(Peuple.NOMBRE);
            } else {
                ville = hasard.nextInt(v.nombre);
                if (!v.estVivante(ville)) continue;
                if (genre == 2 && p.livres.savoir[ville] < 40f) continue;
                region = v.region[ville];
                q = v.peuple[ville];
            }
            Foule f = p.foules[q][region];
            if (f == null || f.vivants == 0) continue;
            int place = f.habitantAuHasard(ville, p.annee, age, hasard.nextLong() >>> 1);
            if (place < 0) continue;
            Savoir.Temoin t = Savoir.temoin(p, f, place);
            if (genre < 2) return t;
            if (meilleur == null || t.instruction > meilleur.instruction) meilleur = t;
        }
        return meilleur;
    }

    public static String interroger(Population p, int genre, long tirage) {
        Savoir.Temoin t = chercher(p, genre, tirage);
        if (t == null) return "Personne à interroger.";
        return presenter(p, t) + "\n\n" + parler(p, t, Savoir.souvenirs(p, t, 60), 3);
    }

    public static String lireAuHasard(Population p, boolean ecrite, long tirage) {
        Livres l = p.livres;
        java.util.Random hasard = new java.util.Random(tirage);
        for (int essai = 0; essai < 4000 && l.nombre > 0; essai++) {
            int o = hasard.nextInt(l.nombre);
            if (l.nombreCopies[o] > 0 && l.ecrit[o] == ecrite) return titre(p, o) + "\n\n" + lire(p, o);
        }
        return "Aucune oeuvre trouvée.";
    }

    public static String lirePlusAncien(Population p) {
        Livres l = p.livres;
        for (int o = 0; o < l.nombre; o++) {
            if (l.nombreCopies[o] > 0 && l.ecrit[o]) return titre(p, o) + "\n\n" + lire(p, o);
        }
        return "Aucun livre n'a survécu.";
    }

    public static String grandesMaisons(Population p, int nombre) {
        Maisons x = p.maisons;
        Integer[] ordre = new Integer[x.nombre];
        for (int m = 0; m < x.nombre; m++) ordre[m] = m;
        java.util.Arrays.sort(ordre, (a, b) -> x.anneesRegnees[b] - x.anneesRegnees[a]);
        StringBuilder texte = new StringBuilder();
        for (int k = 0; k < Math.min(nombre, ordre.length); k++) {
            int m = ordre[k];
            int e = x.dernierEtat[m];
            String trone = Noms.ville(p.etats.peuple[e], p.graine, p.etats.capitale[e]);
            texte.append(majuscule(nomMaison(p, m))).append(" : ").append(x.regnes[m]).append(" règnes, ").append(x.anneesRegnees[m]).append(" ans de pouvoir, née en ").append(x.fondation[m]).append(", dernier trône ").append(trone).append(p.etats.estVivant(e) && p.etats.maison[e] == m ? " (règne encore)" : "").append('\n');
        }
        return texte.toString();
    }

    public static String verite(Population p, int nombre) {
        StringBuilder texte = new StringBuilder();
        Chronique c = p.etats.chronique;
        for (int rang = 0; rang < nombre; rang++) {
            int i = c.recent(rang);
            if (i < 0) break;
            texte.append(c.texte(p, i)).append('\n');
        }
        return texte.toString();
    }

    public static String titre(Population p, int o) {
        Livres l = p.livres;
        Chronique c = p.etats.chronique;
        Savoir.Souvenir s = l.souvenirDe(l.debutVersions[o], o);
        for (int k = 0; k < l.nombreVersions[o]; k++) {
            if (l.versionFait[l.debutVersions[o] + k] == l.sujet[o]) s = l.souvenirDe(l.debutVersions[o] + k, o);
        }
        int sujet = c.sujet[l.sujet[o]];
        String nom = etat(p, sujet, s);
        switch (l.type[o]) {
            case Livres.ANNALES: return "Annales " + de(l.etat[o] >= 0 ? etat(p, l.etat[o], s) : nom);
            case Livres.VIE: return "Vie " + de(chef(p, sujet, c.valeur[s.fait], s));
            case Livres.LIGNEE: return "La Généalogie " + de(nom);
            case Livres.GUERRE: return "Les Guerres " + de(nom);
            case Livres.COMPLAINTE: return "Complainte pour " + nom;
            default: return "Le Chant " + de(nom);
        }
    }

    private static String dateLivre(Population p, int o, Savoir.Souvenir s) {
        Livres l = p.livres;
        if (s.stade >= 3) return "Aux temps anciens";
        if (s.stade == 2) return "En des temps lointains";
        int e = l.etat[o];
        if (e >= 0 && s.annee >= p.etats.fondation[e]) return "An " + (s.annee - p.etats.fondation[e] + 1);
        int ecart = l.annee[o] - s.annee;
        return ecart <= 0 ? "Cette année" : "Il y a " + ecart + " ans";
    }

    private static String preface(Population p, int o) {
        Livres l = p.livres;
        String ville = Noms.ville(l.peuple[o], p.graine, l.ville[o]);
        switch (l.type[o]) {
            case Livres.ANNALES: return "Voici ce que les scribes " + de(ville) + " ont jugé digne d'être retenu.\n\n";
            case Livres.VIE: return "Que ceux qui viendront sachent ce que fut ce temps.\n\n";
            case Livres.LIGNEE: return "Voici, de mémoire de scribe, la suite de ceux qui ont tenu ce trône.\n\n";
            case Livres.GUERRE: return "J'écris ceci pour que ces guerres ne soient pas oubliées.\n\n";
            default: return "";
        }
    }

    private static String conclusion(Population p, int o) {
        Livres l = p.livres;
        if (l.type[o] == Livres.COMPLAINTE || l.type[o] == Livres.CHANSON) return "";
        StringBuilder fin = new StringBuilder();
        if (l.auteurBien[o] > 20) fin.append("\nPuissent nos enfants connaître la paix.");
        else if (l.auteurBien[o] < -20) fin.append("\nQue les faibles tremblent en lisant ces lignes.");
        else fin.append("\nAinsi s'achève ce récit.");
        LinkedHashSet<Integer> sources = new LinkedHashSet<>();
        for (int k = 0; k < l.nombreVersions[o]; k++) {
            int source = l.versionSource[l.debutVersions[o] + k];
            if (source >= 0 && source != o && l.ecrit[source]) sources.add(source);
        }
        for (int source : sources) {
            fin.append("\nL'auteur cite \"").append(titre(p, source)).append("\", de ").append(Noms.personne(l.peuple[source], p.graine, l.auteur[source], l.auteurSexe[source])).append('.');

        }
        return fin.toString();
    }

    public static String lire(Population p, int o) {
        Livres l = p.livres;
        StringBuilder texte = new StringBuilder();
        String auteur = Noms.personne(l.peuple[o], p.graine, l.auteur[o], l.auteurSexe[o]);
        String ville = Noms.ville(l.peuple[o], p.graine, l.ville[o]);
        if (l.ecrit[o]) texte.append(SIGNATURES[l.type[o]]).append(auteur).append(", à ").append(ville).append(".\n\n");
        else texte.append("Chant né à ").append(ville).append(", et que l'on doit à ").append(auteur).append(".\n\n");
        texte.append(preface(p, o));
        for (int k = 0; k < l.nombreVersions[o]; k++) {
            int v = l.debutVersions[o] + k;
            Savoir.Souvenir s = l.souvenirDe(v, o);
            int ton = Savoir.ton(p, l.etat[o], l.peuple[o], s.fait);
            String phrase = fait(p, s, ton);
            switch (l.type[o]) {
                case Livres.ANNALES:
                    texte.append(dateLivre(p, o, s)).append(" : ").append(majuscule(phrase)).append(".\n");
                    break;
                case Livres.COMPLAINTE:
                    texte.append(majuscule(phrase)).append(",\n").append(PLAINTES[k % PLAINTES.length]).append("\n\n");
                    break;
                case Livres.CHANSON:
                    texte.append(majuscule(phrase)).append(" !\n").append(ton > 0 ? "Honneur à ceux qui ont vaincu,\n" : ton < 0 ? "Malheur à ceux qui ont perdu,\n" : "Écoutez ce que disent les anciens,\n").append(REFRAINS[l.peuple[o]]).append("\n\n");
                    break;
                default:
                    texte.append(dateLivre(p, o, s)).append(", ").append(phrase).append(".\n\n");
                    break;
            }
        }
        texte.append(conclusion(p, o));
        return texte.toString();
    }
}
