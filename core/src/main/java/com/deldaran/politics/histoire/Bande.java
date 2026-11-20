package com.deldaran.politics.histoire;

import com.deldaran.politics.Metiers;
import com.deldaran.politics.Relations;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Phase 8 : la bande du joueur.
 *
 * On recrute de vrais villageois en leur parlant (touche X). Chacun a des MOTIVATIONS (argent, promesse, amitié, cause,
 * gloire, besoin, peur), tirées de sa graine, de sa situation (sans travail, marié, village ravagé) et de son Lien avec le joueur.
 * Le joueur choisit un ARGUMENT ; s'il touche la bonne motivation (avec l'Éloquence ou le Commandement, le renom et l'estime),
 * le villageois pose ses conditions (une solde par semaine, une promesse) et rejoint la bande : il quitte son village et suit le joueur.
 * Chaque jour la bande mange (achetée au marché de la ville, ou prise dans le sac) ; chaque semaine on paie la solde.
 * Le moral monte quand tout va bien, chute quand on ne paie pas ou qu'on a faim ; trop bas, on déserte.
 */
public final class Bande {
    public static final int ARGENT = 0, PROMESSE = 1, AMITIE = 2, CAUSE = 3, GLOIRE = 4, BESOIN = 5, PEUR = 6, MOTIFS = 7;
    public static final String[] NOMS_MOTIFS = { "l'argent", "une promesse", "l'amitié", "une cause", "la gloire", "le besoin", "la peur" };
    public static final String[] ARGUMENTS = {
        "« Je te paierai bien. » (une solde)",
        "« Suis-moi, et je te promets mieux que ça. » (une promesse)",
        "« Fais-le pour moi, pour notre amitié. »",
        "« Il faut défendre les gens d'ici. Aide-moi. » (une cause)",
        "« Viens chercher la gloire avec moi. »",
        "« Avec moi, tu auras un toit et à manger. »",
        "« Viens. Ou tu le regretteras. » (une menace)" };
    public static final String[] PROMESSES = { "une part du butin", "une terre quand tu seras seigneur", "une place dans tes ateliers" };

    /** Taille maximale de la bande par palier de renom (inconnu, quartier, ville, comté, royaume, légende) ; +1 par COMMANDEMENT_PAR points de Commandement. */
    public static final int[] TAILLES = { 1, 3, 20, 60, 200, 500 };
    public static final int COMMANDEMENT_PAR = 25;
    /** Score à atteindre pour convaincre, et l'écart du hasard (± la moitié). */
    public static final float SEUIL = 60f, HASARD = 20f;
    /** Arguments qu'on peut essayer par jour sur la même personne. */
    public static final int ESSAIS = 2;
    /** Ce qui retient chez soi : un conjoint, un poste dans un atelier. */
    public static final float ATTACHE_FAMILLE = 20f, ATTACHE_METIER = 10f;
    /** Solde de base demandée par semaine (cuivre), plus jusqu'à SOLDE_AVIDE selon l'appétit d'argent. */
    public static final int SOLDE_BASE = 10, SOLDE_AVIDE = 25;
    /** Ration de pain par personne et par jour. */
    public static final int RATION = 2;
    /** Moral : gain par jour bien nourri, perte en cas de faim, de solde impayée ; seuil et chance par jour de désertion. */
    public static final float MORAL_JOUR = 0.6f, MORAL_FAIM = 8f, MORAL_IMPAYE = 15f, MORAL_PAYE = 2f, DESERTION = 20f, CHANCE_DESERTION = 0.25f;

    /** Ce que l'écran sait du villageois à qui l'on parle. */
    public static final class Rencontre {
        public long graine;
        public String nom = "", metierTexte = "";
        public boolean femme, enfant, marie, rival;
        public int peuple, ville = -1, travail = -1;
        public boolean ruines;
        public Relations.Lien lien;
    }

    public static final class Compagnon {
        public long graine;
        public String nom = "";
        public boolean femme;
        public int peuple, ville, motif, solde, promesse = -1, depuis, impayes;
        public float moral;
        /** Valeur au combat (0 à 100), tirée de la graine ; elle monte avec les victoires (entraînement). */
        public float combat;
        /** Phase 9 : l'arme et l'armure que le joueur lui a données (ids de Biens, ou -1), sa santé (1 : indemne), ses victoires. */
        public int arme = -1, armure = -1, victoires;
        public float sante = 1f;

        public float force() {
            return Combat.force(combat, arme) * (0.5f + 0.5f * sante);
        }
    }

    /** Promesses : tous les PATIENCE jours sans qu'elle soit tenue, le compagnon la rappelle et perd MORAL_PROMESSE ; part du butin promise. */
    public static final int PATIENCE = 30;
    public static final float MORAL_PROMESSE = 5f, PART_BUTIN = 0.1f, MORAL_TENUE = 6f;
    public static final int BUTIN = 0, TERRE = 1, ATELIERS = 2;

    /** Santé regagnée par jour ; sous SANTE_COMBAT, un blessé ne combat pas. */
    public static final float GUERISON = 0.15f, SANTE_COMBAT = 0.35f;
    /** Après une bataille : moral gagné (victoire) ou perdu (défaite, fuite) ; valeur au combat gagnée par victoire (au plus). */
    public static final float MORAL_VICTOIRE = 10f, MORAL_DEFAITE = 12f, ENTRAINEMENT = 4f;

    private final Joueur j;
    private final Population p;
    public final List<Compagnon> membres = new ArrayList<>();
    /** Refus par graine : jour * 8 + nombre d'essais ratés ce jour-là. */
    private final Map<Long, Integer> refus = new HashMap<>();
    private int dernierJour = -1;

    /** Phase 11 : après une régence, on ne rattrape pas les années sautées. */
    void sauter() {
        dernierJour = j.jourMaintenant();
    }

    Bande(Joueur j, Population p) {
        this.j = j;
        this.p = p;
    }

    private Fiche f() {
        return j.fiche();
    }

    public int taille() {
        return membres.size();
    }

    public int tailleMax() {
        Fiche f = f();
        int palier = Renommee.palier(j.renommee().meilleurRenom());
        return TAILLES[palier] + (f == null ? 0 : f.competences[Fiche.COMMANDEMENT] / COMMANDEMENT_PAR);
    }

    public Compagnon membre(long graine) {
        for (Compagnon c : membres) if (c.graine == graine) return c;
        return null;
    }

    /** Les graines des membres (lu par l'écran : ils ne sont plus dans leur village, ils suivent le joueur). */
    public long[] graines() {
        long[] g = new long[membres.size()];
        for (int k = 0; k < g.length; k++) g[k] = membres.get(k).graine;
        return g;
    }

    public int soldeSemaine() {
        int s = 0;
        for (Compagnon c : membres) s += c.solde;
        return s;
    }

    /** Les motivations d'un villageois (0 à 1), tirées de sa graine, de sa situation et de son Lien. */
    static float[] motivations(Rencontre r) {
        Random h = new Random(Travail.melanger(r.graine * 0x9E3779B97F4A7C15L + 17L));
        float[] m = new float[MOTIFS];
        boolean sansTravail = r.travail < 0;
        float estime = r.lien == null ? 0f : r.lien.estime, peur = r.lien == null ? 0f : r.lien.peur;
        m[ARGENT] = 0.3f + 0.35f * h.nextFloat() + (sansTravail ? 0.2f : 0f);
        m[PROMESSE] = 0.15f + 0.5f * h.nextFloat() + (sansTravail ? 0.1f : 0f);
        m[AMITIE] = Math.max(0f, Math.min(1f, estime * 1.2f));
        m[CAUSE] = h.nextFloat() < 0.3f ? 0.5f + 0.4f * h.nextFloat() : 0.1f * h.nextFloat();
        float g = h.nextFloat();
        m[GLOIRE] = 0.1f + 0.7f * g * g;
        m[BESOIN] = (sansTravail ? 0.45f : 0f) + (r.ruines ? 0.3f : 0f) + 0.1f * h.nextFloat();
        m[PEUR] = Math.max(0f, Math.min(1f, peur));
        return m;
    }

    /** Pour les essais. */
    public static float[] motivationsPour(Rencontre r) {
        return motivations(r);
    }

    private static float combat(long graine) {
        Random h = new Random(Travail.melanger(graine * 31L + 99L));
        return 10f + 50f * h.nextFloat() * h.nextFloat() + 20f * h.nextFloat();
    }

    // ---------------------------------------------------------------- Parler (touche X)

    /** Touche X devant un villageois : lui parler. Si c'est un compagnon, sa fiche. */
    public Joueur.Scene parler(Rencontre r) {
        avancer();
        Compagnon c = membre(r.graine);
        if (c != null) return compagnon(c, null);
        if (r.nom == null || r.nom.isEmpty()) {
            Joueur.Scene e = j.missions().quetes.escorteParle(r);
            if (e != null) return e;
        }
        String qui = r.nom + (r.metierTexte.isEmpty() ? "" : ", " + r.metierTexte);
        String parent = j.social().role(r.graine);
        if (parent != null) qui = r.nom + ", " + parent;
        StringBuilder t = new StringBuilder(qui).append(".");
        Relations.Lien l = r.lien;
        if (l != null) {
            if (l.rival) t.append(" Ton rival. Il te fixe sans un mot.");
            else if (l.estime > 0.4f) t.append(" Il te sourit : il t'aime bien.");
            else if (l.estime < -0.2f || l.rancune > 0.3f) t.append(" Il te regarde de travers.");
            else if (l.peur > 0.4f) t.append(" Il baisse les yeux : tu lui fais peur.");
            else t.append(" Il attend que tu parles.");
        }
        if (r.femme) t = new StringBuilder(t.toString().replace(" Il ", " Elle ").replace(" il ", " elle "));
        Joueur.Scene s = new Joueur.Scene(r.nom, "");
        j.missions().quetes.choix(s, r);
        j.social().choix(s, r);
        j.lignee().choix(s, r);
        if (!r.enfant && !r.rival) s.ajouter("« Rejoins-moi. »", () -> recruter(r));
        s.ajouter("« Ma bande »", this::scene);
        s.ajouter("« Au revoir. »", j::fermerScene);
        Joueur.Scene vraie = new Joueur.Scene(r.nom, t.toString());
        vraie.choix.addAll(s.choix);
        vraie.suites.addAll(s.suites);
        return vraie;
    }

    private Joueur.Scene recruter(Rencontre r) {
        if (taille() >= tailleMax())
            return reponse(r, "Tu ne peux pas mener plus de " + tailleMax() + " personne" + (tailleMax() > 1 ? "s" : "") + " : il te faut plus de renom, ou de Commandement.");
        Integer deja = refus.get(r.graine);
        if (deja != null && deja / 8 == j.jourMaintenant() && deja % 8 >= ESSAIS) return reponse(r, "« Je t'ai déjà répondu. Reviens un autre jour. »");
        Joueur.Scene s = new Joueur.Scene("Rejoins-moi", "Que lui dis-tu pour le convaincre ?");
        for (int a = 0; a < ARGUMENTS.length; a++) {
            int arg = a;
            s.ajouter(ARGUMENTS[a], () -> argument(r, arg));
        }
        s.ajouter("« Laisse, ce n'est rien. »", () -> parler(r));
        return s;
    }

    private Joueur.Scene reponse(Rencontre r, String texte) {
        Joueur.Scene s = new Joueur.Scene(r.nom, texte);
        s.ajouter("« Revenir", () -> parler(r));
        return s;
    }

    /** Le score d'un argument : la motivation qu'il touche, la compétence, le renom, l'estime, moins ce qui retient. */
    private float score(Rencontre r, int arg) {
        Fiche f = f();
        float[] m = motivations(r);
        int palier = Renommee.palier(r.ville >= 0 ? j.renommee().renom(r.ville) : 0f);
        float estime = r.lien == null ? 0f : r.lien.estime;
        int competence;
        switch (arg) {
            case ARGENT: case BESOIN: competence = f.competences[Fiche.COMMERCE]; break;
            case GLOIRE: case PEUR: competence = f.competences[Fiche.COMMANDEMENT]; break;
            default: competence = f.competences[Fiche.ELOQUENCE];
        }
        float s = 100f * m[arg] + competence / 3f + palier * (arg == GLOIRE ? 14f : 7f) + estime * 30f;
        if (arg == PROMESSE) s += palier * 5f - 10f; // une promesse n'est crue que si l'on a du renom
        if (arg == CAUSE && r.ville >= 0 && j.renommee().reputation(r.ville) < 0f) s -= 30f;
        if (r.marie) s -= ATTACHE_FAMILLE;
        if (r.travail >= 0) s -= ATTACHE_METIER;
        Random h = new Random(Travail.melanger(r.graine * 7919L + j.jourMaintenant() * 31L + arg));
        return s + (h.nextFloat() - 0.5f) * HASARD;
    }

    private Joueur.Scene argument(Rencontre r, int arg) {
        float score = score(r, arg);
        float[] m = motivations(r);
        String il = r.femme ? "elle" : "il";
        if (score < SEUIL) {
            Integer deja = refus.get(r.graine);
            int jour = j.jourMaintenant(), rates = deja != null && deja / 8 == jour ? deja % 8 + 1 : 1;
            refus.put(r.graine, jour * 8 + rates);
            if (arg == PEUR && r.lien != null) {
                r.lien.rancune = Math.min(1f, r.lien.rancune + 0.15f);
                r.lien.estime = Math.max(-1f, r.lien.estime - 0.1f);
            }
            String rep;
            int fort = 0;
            for (int k = 1; k < MOTIFS; k++) if (m[k] > m[fort]) fort = k;
            if (arg == PEUR) rep = "« Tu crois me faire peur ? Va-t'en. »";
            else if (r.marie && score > SEUIL - ATTACHE_FAMILLE) rep = "« J'ai une famille ici. Je ne peux pas partir. »";
            else if (score > SEUIL - 25f && fort != arg) rep = "« Peut-être… mais pas comme ça. » " + (r.femme ? "Elle" : "Il") + " semble sensible à " + NOMS_MOTIFS[fort] + ".";
            else if (score > SEUIL - 25f) rep = "« Peut-être… mais il me faudrait plus. » Avec plus de renom, ou plus de talent pour convaincre, ça passerait.";
            else rep = "« Non. »";
            return reponse(r, rep);
        }
        Compagnon c = new Compagnon();
        c.graine = r.graine;
        c.nom = r.nom;
        c.femme = r.femme;
        c.peuple = r.peuple;
        c.ville = r.ville;
        c.motif = arg;
        c.depuis = j.jourMaintenant();
        c.combat = combat(r.graine);
        int voulu = Math.round((SOLDE_BASE + SOLDE_AVIDE * m[ARGENT]) * (1f + c.combat / 100f));
        switch (arg) {
            case ARGENT: c.solde = voulu; c.moral = 60f; break;
            case PROMESSE: c.solde = voulu / 2; c.moral = 65f; break;
            case AMITIE: c.solde = Math.max(3, voulu / 3); c.moral = 75f; break;
            case CAUSE: c.solde = Math.max(3, voulu / 3); c.moral = 70f; break;
            case GLOIRE: c.solde = voulu / 2; c.moral = 65f; break;
            case BESOIN: c.solde = 0; c.moral = 55f; break;
            default: c.solde = 0; c.moral = 25f;
        }
        String M = r.femme ? "Elle" : "Il";
        if (arg == PROMESSE) {
            Joueur.Scene s = new Joueur.Scene(r.nom, "« Et qu'est-ce que tu me promets ? »");
            for (int k = 0; k < PROMESSES.length; k++) {
                int pr = k;
                s.ajouter("« " + Character.toUpperCase(PROMESSES[k].charAt(0)) + PROMESSES[k].substring(1).replace("tu seras", "je serai").replace("tes ateliers", "mes ateliers") + ". »",
                        () -> {
                            c.promesse = pr;
                            return conditions(r, c, "« Je m'en souviendrai. »");
                        });
            }
            return s;
        }
        String phrase;
        switch (arg) {
            case ARGENT: phrase = "« Si tu paies, je te suis. »"; break;
            case AMITIE: phrase = "« Pour toi, d'accord. »"; break;
            case CAUSE: phrase = "« Enfin quelqu'un qui pense comme moi. »"; break;
            case GLOIRE: phrase = "« Ça, ça me plaît ! »"; break;
            case BESOIN: phrase = "« Un toit et du pain… ça me suffit. »"; break;
            default: phrase = M + " baisse la tête et te suit sans un mot.";
        }
        return conditions(r, c, phrase);
    }

    /** Le villageois pose ses conditions ; on accepte ou non. */
    private Joueur.Scene conditions(Rencontre r, Compagnon c, String phrase) {
        StringBuilder t = new StringBuilder(phrase);
        if (c.solde > 0) t.append("\n").append(r.femme ? "Elle" : "Il").append(" demande ").append(Fiche.argentTexte(c.solde)).append(" par semaine.");
        else t.append("\n").append(r.femme ? "Elle" : "Il").append(" ne demande pas de solde, seulement à manger.");
        if (c.promesse >= 0) t.append(" Tu lui as promis ").append(PROMESSES[c.promesse]).append(" : ").append(r.femme ? "elle" : "il").append(" ne l'oubliera pas.");
        t.append("\nLa bande coûtera ").append(Fiche.argentTexte(soldeSemaine() + c.solde)).append(" par semaine, plus le pain.");
        Joueur.Scene s = new Joueur.Scene(r.nom, t.toString());
        s.ajouter("« Marché conclu. »", () -> engager(r, c));
        s.ajouter("« Non, finalement. »", () -> parler(r));
        return s;
    }

    private Joueur.Scene engager(Rencontre r, Compagnon c) {
        if (taille() >= tailleMax()) return reponse(r, "Ta bande est au complet.");
        membres.add(c);
        if (r.lien != null && c.motif == PEUR) r.lien.peur = Math.min(1f, r.lien.peur + 0.1f);
        j.recruter(c.graine);
        String phrase = "En l'an " + p.annee + ", " + c.nom + " rejoint ta bande.";
        j.noter(phrase);
        j.annoncer(c.nom + " te suit.");
        j.enregistrer();
        Joueur.Scene s = new Joueur.Scene(c.nom, c.nom + " rassemble ses affaires et se met en route derrière toi.");
        s.fermer = true;
        return s;
    }

    // ---------------------------------------------------------------- La vie de la bande

    /** Fait passer les jours : pain, solde, moral, désertions. */
    void avancer() {
        int jour = j.jourMaintenant();
        if (dernierJour < 0 || membres.isEmpty()) {
            dernierJour = jour;
            return;
        }
        int debut = Math.max(dernierJour, jour - 60);
        for (int d = debut + 1; d <= jour; d++) unJour(d);
        dernierJour = jour;
    }

    private void unJour(int jour) {
        Fiche f = f();
        if (f == null || membres.isEmpty()) return;
        int avant = painsManques;
        boolean mange = nourrir(membres.size() * RATION);
        painsManquesRecents = painsManques - avant;
        List<Compagnon> partis = new ArrayList<>();
        for (Compagnon c : membres) {
            c.sante = Math.min(1f, c.sante + GUERISON);
            float plafond = 70f + 30f * (c.motif == AMITIE ? 1f : c.motif == CAUSE ? 0.7f : 0.3f) + Math.min(15f, c.victoires * 3f);
            if (mange) c.moral = Math.min(plafond, c.moral + MORAL_JOUR);
            else c.moral -= MORAL_FAIM;
            if ((jour - c.depuis) % 7 == 0 && c.solde > 0) {
                if (f.argent >= c.solde) {
                    f.argent -= c.solde;
                    c.moral = Math.min(plafond, c.moral + MORAL_PAYE);
                    c.impayes = 0;
                } else {
                    c.impayes++;
                    c.moral -= MORAL_IMPAYE;
                    j.annoncer(c.nom + " n'a pas reçu sa solde.");
                }
            }
            if (c.promesse == TERRE || c.promesse == ATELIERS) {
                int attente = jour - c.depuis;
                boolean tenable = c.promesse == ATELIERS && j.entreprise().nombre() > 0;
                if (attente > 0 && attente % PATIENCE == 0) {
                    c.moral -= MORAL_PROMESSE;
                    j.annoncer(c.nom + " te rappelle ta promesse : " + PROMESSES[c.promesse] + "." + (tenable ? " (Ma bande : tu peux la tenir.)" : ""));
                }
            }
            float seuil = c.motif == PEUR ? DESERTION + 15f : DESERTION;
            Random h = new Random(Travail.melanger(c.graine * 131L + jour));
            if (c.moral < seuil && h.nextFloat() < CHANCE_DESERTION) partis.add(c);
        }
        if (!mange) j.annoncer("La bande a faim.");
        for (Compagnon c : partis) {
            membres.remove(c);
            j.renvoyerCompagnon(c.graine);
            j.annoncer(c.nom + " a déserté" + (c.impayes > 0 ? " : on ne " + (c.femme ? "la" : "le") + " payait plus." : " : le moral était trop bas."));
            j.noter(c.nom + " quitte ta bande.");
        }
    }

    /** Achète le pain du jour au marché de la ville (ça fait monter les prix), ou le prend dans le sac. Faux si on n'a pas pu. */
    private boolean nourrir(int pains) {
        Fiche f = f();
        int restants = pains, achetes = 0;
        // Dans le sac d'abord : galettes et pain
        for (Biens.Bien b : new Biens.Bien[] { Biens.PAIN, Biens.GALETTE }) {
            int n = Math.min(restants, f.sac.nombre(b.id));
            if (n > 0) {
                f.sac.retirer(b.id, n);
                restants -= n;
            }
        }
        int v = j.villeIci();
        if (restants > 0 && v >= 0) {
            EconomieVille e = j.marche().ville(v, j.jour());
            for (EconomieVille.Agent a : e.agents) {
                if (restants <= 0) break;
                if (!a.vend[Biens.PAIN.id] || a.joueur) continue;
                int prix = Math.max(1, j.marche().prixAchat(f, e, a, Biens.PAIN.id));
                int n = Math.min(restants, Math.min(j.marche().stock(a, Biens.PAIN.id), f.argent / prix));
                if (n <= 0) continue;
                f.argent -= n * prix;
                e.venteAuJoueur(a, Biens.PAIN.id, n, n * prix);
                achetes += n;
                restants -= n;
            }
        }
        painsAchetes += achetes;
        painsManques += restants;
        return restants <= pains / 2;
    }

    /** Pour les essais : pains achetés au marché et pains qui ont manqué, depuis le début. */
    public int painsAchetes, painsManques, painsManquesRecents;

    public static String humeur(float moral) {
        if (moral >= 75f) return "dévoué";
        if (moral >= 55f) return "content";
        if (moral >= 35f) return "las";
        if (moral >= 20f) return "mécontent";
        return "prêt à partir";
    }

    /** « Ma bande ». */
    public Joueur.Scene scene() {
        avancer();
        StringBuilder t = new StringBuilder();
        if (membres.isEmpty()) t.append("Personne ne te suit. Parle aux gens (X) et demande-leur de te rejoindre.");
        else t.append(membres.size()).append(membres.size() > 1 ? " personnes te suivent" : " personne te suit").append(" (au plus ").append(tailleMax()).append("). Solde : ")
                .append(Fiche.argentTexte(soldeSemaine())).append(" par semaine, plus ").append(membres.size() * RATION).append(" pains par jour.");
        if (membres.isEmpty()) t.append("\nTu peux mener ").append(tailleMax()).append(tailleMax() > 1 ? " personnes." : " personne.");
        Joueur.Scene s = new Joueur.Scene("", "");
        for (Compagnon c : membres) s.ajouter(c.nom + " — " + humeur(c.moral) + (c.solde > 0 ? ", " + Fiche.argentTexte(c.solde) + " par semaine" : ""), () -> compagnon(c, null));
        s.ajouter("« Revenir", j::accueilScene);
        Joueur.Scene vraie = new Joueur.Scene("Ma bande", t.toString());
        vraie.choix.addAll(s.choix);
        vraie.suites.addAll(s.suites);
        return vraie;
    }

    private Joueur.Scene compagnon(Compagnon c, String message) {
        StringBuilder t = new StringBuilder();
        String il = c.femme ? "Elle" : "Il";
        t.append(c.nom).append(", ").append(c.ville >= 0 ? "venu" + (c.femme ? "e" : "") + " " + Creation.de(Noms.ville(p.villes.peuple[c.ville], p.graine, c.ville)) : "")
                .append(". ").append(il).append(" t'a suivi").append(" pour ").append(NOMS_MOTIFS[c.motif]).append('.');
        t.append("\nMoral : ").append(humeur(c.moral)).append(" (").append(Math.round(c.moral)).append("). Solde : ")
                .append(c.solde > 0 ? Fiche.argentTexte(c.solde) + " par semaine" : "aucune").append(". Au combat : ").append(c.combat < 30f ? "novice" : c.combat < 55f ? "solide" : "redoutable").append('.');
        if (c.promesse >= 0) t.append("\nTu lui as promis ").append(PROMESSES[c.promesse]).append('.');
        if (c.impayes > 0) t.append("\nTu lui dois ").append(c.impayes).append(c.impayes > 1 ? " semaines de solde." : " semaine de solde.");
        t.append("\nArme : ").append(c.arme >= 0 ? Biens.get(c.arme).nom : "aucune").append(". Armure : ").append(c.armure >= 0 ? Biens.get(c.armure).nom : "aucune").append('.');
        if (c.victoires > 0) t.append(" Victoires : ").append(c.victoires).append('.');
        if (c.sante < 1f) t.append("\n").append(il).append(c.sante < SANTE_COMBAT ? " est gravement blessé" + (c.femme ? "e" : "") + " et ne peut pas se battre." : " est blessé" + (c.femme ? "e" : "") + ".");
        Joueur.Scene s = new Joueur.Scene("", "");
        s.ajouter("Lui donner une arme ou une armure…", () -> equiper(c));
        if (f().argent >= c.solde + 5) s.ajouter("Lui donner " + Fiche.argentTexte(Math.max(5, c.solde)) + " de plus", () -> {
            int don = Math.max(5, c.solde);
            f().argent -= don;
            c.moral = Math.min(100f, c.moral + 8f);
            c.impayes = 0;
            j.enregistrer();
            return compagnon(c, il + " empoche l'argent avec un hochement de tête.");
        });
        if (c.promesse == ATELIERS && j.entreprise().nombre() > 0) s.ajouter("Tenir ta promesse : lui donner sa place dans ton atelier", () -> {
            Entreprise.Atelier x = j.entreprise().ateliers.get(0);
            EconomieVille.Agent a = j.entreprise().agent(x);
            if (a != null) a.ouvriers += 1f;
            membres.remove(c);
            j.renvoyerCompagnon(c.graine);
            j.renommee().marquer(Renommee.FIABLE);
            f().pencher(Fiche.HONNETETE, 3);
            j.noter("Tu tiens ta promesse : " + c.nom + " entre dans ton atelier.");
            j.enregistrer();
            Joueur.Scene fin = new Joueur.Scene(c.nom, "« Tu as tenu parole. » " + il + " te serre la main et part prendre sa place à l'atelier.");
            fin.ajouter("« Revenir", this::scene);
            return fin;
        });
        s.ajouter("Le renvoyer", () -> {
            membres.remove(c);
            j.renvoyerCompagnon(c.graine);
            if (c.promesse >= 0) {
                f().pencher(Fiche.HONNETETE, -4);
                Relations rel = j.relations;
                Relations.Lien l = rel == null ? null : rel.existant(c.graine);
                if (l != null) {
                    l.rancune = Math.min(1f, l.rancune + 0.4f);
                    l.estime = Math.max(-1f, l.estime - 0.3f);
                }
            }
            j.noter("Tu renvoies " + c.nom + (c.promesse >= 0 ? ", sans tenir ta promesse." : "."));
            j.enregistrer();
            Joueur.Scene fin = new Joueur.Scene(c.nom, c.promesse >= 0 ? "« Et ta promesse ? » " + il + " crache par terre et s'en va. Il ne l'oubliera pas." : il + " hausse les épaules et s'en va.");
            fin.ajouter("« Revenir", this::scene);
            return fin;
        });
        s.ajouter("« Revenir", this::scene);
        Joueur.Scene vraie = new Joueur.Scene(c.nom, Joueur.avec(message, t.toString()));
        vraie.choix.addAll(s.choix);
        vraie.suites.addAll(s.suites);
        return vraie;
    }

    /** Les armes et armures du sac que l'on peut donner (sans prendre celles que l'on porte). */
    private Joueur.Scene equiper(Compagnon c) {
        Fiche f = f();
        String il = c.femme ? "elle" : "il";
        Joueur.Scene s = new Joueur.Scene("", "");
        for (Inventaire.Pile pile : new ArrayList<>(f.sac.piles)) {
            Biens.Bien b = Biens.get(pile.bien);
            boolean arme = b.categorie == Biens.ARME && b.place == Biens.MAIN && b.valeur > 0;
            boolean armure = b.categorie == Biens.ARMURE && b.place == Biens.CORPS;
            if (!arme && !armure || pile.nombre - (pile.porte ? 1 : 0) < 1) continue;
            s.ajouter(b.majuscule() + (arme ? " (force " : " (protection ") + b.valeur + ")", () -> {
                f.sac.retirer(b.id, 1);
                int ancien = arme ? c.arme : c.armure;
                if (ancien >= 0) f.sac.ajouter(ancien, 1);
                if (arme) c.arme = b.id;
                else c.armure = b.id;
                c.moral = Math.min(100f, c.moral + 2f);
                j.gesteJoueur = "donner_objet";
                j.publierBande();
                j.rafraichirSac();
                j.enregistrer();
                return compagnon(c, Character.toUpperCase(il.charAt(0)) + il.substring(1) + " prend " + (b.feminin ? "la " : "le ") + b.nom + (ancien >= 0 ? " et te rend " + (Biens.get(ancien).feminin ? "la " : "le ") + Biens.get(ancien).nom : "") + ".");
            });
        }
        String texte = s.choix.isEmpty() ? "Tu n'as rien à lui donner : achète des armes et des armures (forge, tannerie, tisserand), ou prends-les aux brigands."
                : "Que lui donnes-tu ? Ce qu'" + il + " portait revient dans ton sac.";
        s.ajouter("« Revenir", () -> compagnon(c, null));
        Joueur.Scene vraie = new Joueur.Scene(c.nom, texte);
        vraie.choix.addAll(s.choix);
        vraie.suites.addAll(s.suites);
        return vraie;
    }

    /** Les membres en état de se battre, pour une bataille. */
    List<Combat.Combattant> combattants() {
        List<Combat.Combattant> l = new ArrayList<>();
        for (Compagnon c : membres) {
            if (c.sante < SANTE_COMBAT) continue;
            Combat.Combattant x = new Combat.Combattant();
            x.nom = c.nom;
            x.camp = Combat.NOUS;
            x.graine = c.graine;
            x.femme = c.femme;
            x.peuple = c.peuple;
            x.arme = c.arme;
            x.armure = c.armure;
            x.force = Combat.force(c.combat, c.arme);
            x.sante = c.sante;
            l.add(x);
        }
        return l;
    }

    /** Après une bataille : morts, blessés, moral, entraînement. Renvoie le récit des pertes. */
    String apres(Combat.Resultat r) {
        StringBuilder t = new StringBuilder();
        boolean victoire = r.vainqueur == Combat.NOUS;
        for (Combat.Combattant x : r.tous) {
            if (x.camp != Combat.NOUS || x.joueur) continue;
            Compagnon c = membre(x.graine);
            if (c == null) continue;
            if (x.etat == Combat.MORT) {
                membres.remove(c);
                j.noter(c.nom + " est mort" + (c.femme ? "e" : "") + " au combat, en l'an " + p.annee + ".");
                t.append(c.nom).append(" est mort").append(c.femme ? "e" : "").append(". ");
                for (Compagnon o : membres) o.moral -= 5f;
                continue;
            }
            c.sante = x.etat == Combat.A_TERRE ? 0.2f : Math.max(0.3f, x.sante);
            if (x.etat == Combat.A_TERRE) t.append(c.nom).append(" est grièvement blessé").append(c.femme ? "e" : "").append(". ");
            if (victoire) {
                c.victoires++;
                c.moral = Math.min(100f, c.moral + MORAL_VICTOIRE);
                Random h = new Random(Travail.melanger(c.graine + 7L * c.victoires));
                c.combat = Math.min(95f, c.combat + 1f + (ENTRAINEMENT - 1f) * h.nextFloat());
            } else c.moral -= x.etat == Combat.FUI ? MORAL_DEFAITE * 0.7f : MORAL_DEFAITE;
        }
        j.publierBande();
        return t.toString();
    }

    /** Le butin promis : chacun de ceux à qui on l'a promis reçoit sa part. Renvoie le récit, et ce qu'il reste au joueur. */
    String partager(int butin, int[] reste) {
        StringBuilder t = new StringBuilder();
        int garde = butin;
        for (Compagnon c : membres) {
            if (c.promesse != BUTIN) continue;
            int part = Math.round(butin * PART_BUTIN);
            if (part <= 0) continue;
            garde -= part;
            c.moral = Math.min(100f, c.moral + MORAL_TENUE);
            t.append(t.length() == 0 ? "Comme promis, tu partages le butin : " : ", ").append(c.nom).append(" reçoit ").append(Fiche.argentTexte(part));
        }
        if (t.length() > 0) t.append('.');
        reste[0] = Math.max(0, garde);
        return t.toString();
    }

    // ---------------------------------------------------------------- Ce qu'ils disent en chemin

    public static final int EN_ROUTE = 0, AFFAME = 1, IMPAYE = 2, CONTENT = 3, BLESSE = 4, PROMESSE_DUE = 5;

    /** Une réplique d'un membre de la bande : { nom, réplique de secours, consigne pour l'IA locale } ; null si personne. */
    public String[] parole(long graine, long hasard) {
        Compagnon c = membre(graine);
        Fiche f = f();
        if (c == null || f == null) return null;
        Random h = new Random(hasard);
        int situation = c.sante < 0.5f ? BLESSE : c.impayes > 0 ? IMPAYE : painsManquesRecents > 0 ? AFFAME
                : c.promesse >= 0 && j.jourMaintenant() - c.depuis > PATIENCE ? PROMESSE_DUE : c.moral > 70f ? CONTENT : EN_ROUTE;
        String[][] secours = {
            { "La route est longue, chef.", "On va où, au juste ?", "J'ai vu des traces de loups, par là.", "Il fera beau demain.", "Mes pieds me font souffrir." },
            { "On mange quand ?", "J'ai le ventre vide depuis hier.", "Un quignon de pain, c'est trop demander ?" },
            { "Et ma solde ?", "On ne vit pas d'air, chef.", "Paie-nous, ou on s'en va." },
            { "Avec toi, on ira loin !", "Je ne regrette pas de t'avoir suivi.", "Qu'ils viennent, les brigands !" },
            { "Ma blessure me lance...", "J'ai besoin de repos.", "Je ne tiendrai pas un autre combat." },
            { "Tu n'as pas oublié ta promesse ?", "Ma terre, c'est pour quand ?", "Tu m'avais promis quelque chose." } };
        String[] situations = { "vous marchez sur la route", "la bande a faim", "tu n'as pas reçu ta solde", "tout va bien, tu es fier de ton chef",
                "tu es blessé" + (c.femme ? "e" : ""), "ton chef ne t'a pas encore donné " + (c.promesse >= 0 ? PROMESSES[c.promesse] : "ce qu'il t'avait promis") };
        String s = secours[situation][h.nextInt(secours[situation].length)];
        String consigne = "Tu es " + c.nom + ", " + (c.femme ? "une femme" : "un homme") + " qui suit " + f.nom + " dans sa bande, par " + NOMS_MOTIFS[c.motif]
                + ". Ton humeur : " + humeur(c.moral) + ". Situation : " + situations[situation] + ". Dis à " + f.nom + " une seule courte phrase (moins de 15 mots).";
        return new String[] { c.nom, s, consigne };
    }

    /** Pour les essais : ce villageois rejoint la bande sans discuter. */
    public void ajouterPourEssai(Rencontre r) {
        Compagnon c = new Compagnon();
        c.graine = r.graine;
        c.nom = r.nom;
        c.femme = r.femme;
        c.peuple = r.peuple;
        c.ville = r.ville;
        c.motif = AMITIE;
        c.moral = 75f;
        c.solde = 3;
        c.depuis = j.jourMaintenant();
        c.combat = combat(r.graine);
        membres.add(c);
        j.publierBande();
    }

    /** Un prisonnier enrôlé de force : il suit par peur, sans solde, de mauvais gré. */
    boolean enroler(Combat.Combattant x, int ville) {
        if (taille() >= tailleMax()) return false;
        Compagnon c = new Compagnon();
        c.graine = x.graine;
        c.nom = x.nom;
        c.femme = x.femme;
        c.peuple = x.peuple;
        c.ville = ville;
        c.motif = PEUR;
        c.moral = 30f;
        c.depuis = j.jourMaintenant();
        c.combat = Math.max(10f, x.force - (x.arme >= 0 ? 4f * Biens.get(x.arme).valeur : 0f));
        c.arme = x.arme;
        c.armure = x.armure;
        c.sante = 0.3f;
        membres.add(c);
        return true;
    }

    void ecrire(DataOutputStream s) throws IOException {
        s.writeInt(dernierJour);
        s.writeInt(membres.size());
        for (Compagnon c : membres) {
            s.writeLong(c.graine);
            s.writeUTF(c.nom);
            s.writeBoolean(c.femme);
            s.writeInt(c.peuple);
            s.writeInt(c.ville);
            s.writeInt(c.motif);
            s.writeInt(c.solde);
            s.writeInt(c.promesse);
            s.writeInt(c.depuis);
            s.writeInt(c.impayes);
            s.writeFloat(c.moral);
            s.writeFloat(c.combat);
            s.writeInt(c.arme);
            s.writeInt(c.armure);
            s.writeInt(c.victoires);
            s.writeFloat(c.sante);
        }
    }

    void lire(DataInputStream s, int format) throws IOException {
        dernierJour = s.readInt();
        int n = s.readInt();
        for (int k = 0; k < n; k++) {
            Compagnon c = new Compagnon();
            c.graine = s.readLong();
            c.nom = s.readUTF();
            c.femme = s.readBoolean();
            c.peuple = s.readInt();
            c.ville = s.readInt();
            c.motif = s.readInt();
            c.solde = s.readInt();
            c.promesse = s.readInt();
            c.depuis = s.readInt();
            c.impayes = s.readInt();
            c.moral = s.readFloat();
            c.combat = s.readFloat();
            if (format >= 10) {
                c.arme = s.readInt();
                c.armure = s.readInt();
                c.victoires = s.readInt();
                c.sante = s.readFloat();
            }
            membres.add(c);
        }
    }
}
