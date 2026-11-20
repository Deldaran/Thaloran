package com.deldaran.politics;

import java.util.Random;

public class Repliques {
    public static final int DEFI = 0, VICTOIRE = 1, SUPPLIQUE = 2, REDDITION = 3, VENGEANCE = 4, HUMILIATION = 5, SALUT_HOSTILE = 6, SALUT_AMI = 7, PEUR = 8, CADEAU = 9;
    public static final String[] NOMS = { "defi", "victoire", "supplique", "reddition", "vengeance", "humiliation", "salut hostile", "salut ami", "peur", "cadeau" };
    private static final String[] PEUPLES = { "elfe", "naine", "humaine", "orque" };
    private static final String[] PEUPLES_M = { "elfe", "nain", "humain", "orque" };
    private static final String[] RECITS = { "il t'a salué", "il t'a fait un cadeau", "il a été tendre avec toi", "il t'a insulté", "il t'a frappé", "il t'a mis à terre",
        "il t'a épargné", "il t'a tué", "il t'a soigné", "tu l'as mis à terre", "il a frappé quelqu'un devant toi", "il a tué quelqu'un devant toi", "il a tué un de tes proches",
        "on t'a parlé de lui", "il a eu raison de toi et tu t'es soumis" };
    private static final String[] SITUATIONS = {
        "Tu retrouves enfin l'étranger. Tu le défies juste avant de te battre contre lui.",
        "Tu viens de mettre l'étranger à terre à coups de poing. Tu te moques de lui.",
        "L'étranger vient de te mettre à terre. Tu es à sa merci et tu le supplies de t'épargner.",
        "L'étranger t'a vaincu encore et encore, et t'a épargné chaque fois. Tu renonces à ta vengeance et tu te soumets à lui.",
        "L'étranger vient de tuer un membre de ta famille sous tes yeux. Tu jures de le venger.",
        "L'étranger t'a battu puis t'a laissé partir. Tu te relèves, humilié, et tu jures qu'il le paiera.",
        "L'étranger passe près de toi. Tu lui en veux et tu le lui fais sentir.",
        "L'étranger passe près de toi. Tu l'apprécies et tu le salues chaleureusement.",
        "L'étranger s'approche de toi et il te fait peur.",
        "L'étranger vient de t'offrir un présent."
    };
    private static final String[][] SECOURS = {
        { "Je n'ai pas oublie ce que tu m'as fait !", "Te voila enfin. Cette fois, tu ne t'en tireras pas.", "Leve les poings, etranger." },
        { "Encore toi ? Tu n'as pas retenu la lecon.", "Reste a terre, c'est ta place.", "Va raconter ca a tes amis, si tu en as." },
        { "Pitie ! Ne me tue pas !", "Arrete, je t'en supplie !", "J'ai une famille... epargne-moi." },
        { "Tu as gagne. Je ne leverai plus la main sur toi.", "Je m'incline. Tu es plus fort que moi.", "C'est fini. Je te sers, si tu le veux." },
        { "Tu as tue les miens. Je jure de les venger !", "Tu paieras pour ce sang.", "Je te retrouverai, ou que tu ailles." },
        { "Tu aurais du me tuer. Tu le regretteras.", "Ce n'est pas fini entre nous.", "Je reviendrai, et je serai pret." },
        { "Passe ton chemin.", "Toi... ne m'adresse pas la parole.", "On sait ce que tu as fait." },
        { "Le bonjour, ami !", "Content de te revoir !", "Que les dieux te gardent." },
        { "Ne me fais pas de mal...", "Reste loin de moi !", "Je n'ai rien fait, laisse-moi." },
        { "Merci, c'est genereux.", "Pour moi ? Merci bien.", "Je m'en souviendrai." }
    };
    private static final String[] EXEMPLES = {
        "Te voilà enfin. J'ai rêvé de ce moment chaque nuit.",
        "Alors, l'étranger, la poussière a bon goût ?",
        "Pitié... j'ai des enfants, laisse-moi vivre !",
        "Assez. Tu as gagné, je ne lèverai plus la main sur toi.",
        "Tu as versé le sang des miens. Je te traquerai jusqu'au bout du monde.",
        "Tu aurais dû m'achever. Tu le regretteras.",
        "Passe ton chemin, on sait tous ce que tu as fait.",
        "Ah, te revoilà ! Que les dieux te gardent, ami.",
        "Ne m'approche pas... je n'ai rien fait !",
        "Pour moi ? Je n'oublierai pas ta bonté."
    };
    public static final String SYSTEME = "Tu joues un personnage d'un monde médiéval sombre. Tu réponds en français correct, à la première personne, par UNE seule phrase courte "
            + "(quinze mots au plus), vivante et pleine d'émotion, en tutoyant l'étranger. Tu ne récites pas tes souvenirs et tu ne répètes aucun mot : tu réagis. "
            + "Tu ne décris aucun geste, tu ne mets ni guillemets ni nom devant ta phrase. Tu n'inventes aucun nom de personne ou de lieu.";

    public static String secours(int situation, Villageois w) {
        String[] l = SECOURS[situation];
        String t = l[new Random(w.graine * 31 + situation * 7 + (w.lien != null ? w.lien.souvenirs.size() : 0)).nextInt(l.length)];
        if (situation == VENGEANCE && w.lien != null) {
            String mort = mort(w.lien);
            if (mort != null) t = "Tu as tue " + mort + ". Je jure de le venger !";
        }
        return t;
    }

    private static String mort(Relations.Lien l) {
        for (int k = l.souvenirs.size() - 1; k >= 0; k--) {
            Relations.Souvenir s = l.souvenirs.get(k);
            if (s.type == Relations.PROCHE_TUE && s.sujet != null && !s.sujet.isEmpty()) return s.sujet;
        }
        return null;
    }

    public static String consigne(int situation, Villageois w, double jour) {
        Relations.Lien l = w.lien;
        boolean femme = w.corps.apparence.femme;
        String e = femme ? "e" : "";
        StringBuilder t = new StringBuilder();
        t.append("Tu es ").append(l != null ? l.nomComplet() : "un villageois").append(", ");
        if (w.enfant) t.append(femme ? "une fillette " : "un garçon ");
        else t.append(femme ? "une femme " : "un homme ");
        t.append((femme ? PEUPLES : PEUPLES_M)[Math.max(0, Math.min(3, w.village.peuple))]);
        String metier = w.travail >= 0 ? Metiers.NOMS[w.village.metiers[w.travail]] : "maison";
        if (!w.enfant) t.append(metier.equals("maison") ? ", qui tient sa maison" : ", qui travaille à : " + metier);
        t.append(".\n");
        if (l != null && l.rival) {
            t.append("Tu es devenu").append(e).append(" l'ennemi").append(femme ? "e" : "").append(" juré").append(e).append(" de l'étranger");
            if (l.victoires + l.defaites > 0) t.append(" : tu l'as battu ").append(l.victoires).append(" fois et il t'a battu").append(e).append(" ").append(l.defaites).append(" fois");
            t.append(".\n");
        }
        if (l != null && !l.souvenirs.isEmpty()) {
            t.append("Ce que tu sais de l'étranger :\n");
            int debut = Math.max(0, l.souvenirs.size() - 8);
            for (int k = debut; k < l.souvenirs.size(); k++) {
                Relations.Souvenir s = l.souvenirs.get(k);
                int fois = 1;
                while (k + 1 < l.souvenirs.size() && l.souvenirs.get(k + 1).type == s.type) {
                    k++;
                    fois++;
                }
                String recit = RECITS[s.type];
                if (s.type == Relations.PROCHE_TUE && s.sujet != null && !s.sujet.isEmpty()) recit = "il a tué " + s.sujet + ", de ta famille";
                if (s.type == Relations.RUMEUR) recit = l.estime >= 0f ? "on t'a dit du bien de lui" : "on t'a dit du mal de lui";
                t.append("- ").append(recit).append(fois > 1 ? " (" + fois + " fois, " : " (").append(quand(jour - l.souvenirs.get(k).jour)).append(")\n");
            }
        }
        if (l != null) t.append("Ce que tu ressens pour lui : ").append(sentiment(l)).append(".\n");
        t.append("Situation : ").append(SITUATIONS[situation]).append("\n");
        t.append("Exemple de ton, à ne pas recopier : ").append(EXEMPLES[situation]).append("\nTa phrase :");
        return t.toString();
    }

    private static String quand(double jours) {
        if (jours < 0.05) return "à l'instant";
        if (jours < 0.6) return "aujourd'hui";
        if (jours < 1.5) return "hier";
        return "il y a " + Math.round(jours) + " jours";
    }

    private static String sentiment(Relations.Lien l) {
        StringBuilder s = new StringBuilder();
        if (l.rancune > 0.7f) s.append("une haine profonde");
        else if (l.rancune > 0.35f) s.append("de la rancune");
        if (l.peur > 0.5f) s.append(s.length() > 0 ? " et " : "").append("de la peur");
        if (l.estime > 0.4f) s.append(s.length() > 0 ? " et " : "").append("de l'affection");
        else if (l.estime < -0.4f && s.length() == 0) s.append("du mépris");
        return s.length() == 0 ? "de l'indifférence" : s.toString();
    }
}
