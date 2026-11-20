package com.deldaran.politics.histoire;

import com.deldaran.politics.Metiers;
import com.deldaran.politics.Relations;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Lot F : la vie sociale du joueur, jouée en parlant aux gens (touche X).
 * - les PROCHES (famille, amis de la création) se croisent en ville : un habitant du village les incarne, toujours le même ;
 * - les ENVIES tournées vers les proches : « boire un coup avec Garin », « prendre des nouvelles de ta mère » ;
 * - les DETTES : emprunter à qui t'estime, prêter à qui est à court ; les rendre, les réclamer ;
 * - les PROMESSES : une dette, une demande acceptée ; tenues, elles rapprochent ; rompues, elles fâchent ;
 * - l'AMOUR : faire la cour, offrir un présent, déclarer sa flamme, demander la main, célébrer les noces ;
 * - les BARDES, à la taverne : ils chantent tes exploits, et ton renom voyage plus vite ;
 * - les GRANDS MOMENTS (palier de renom, maîtrise, mariage, destin accompli) : on peut changer d'aspiration.
 */
public final class Social {
    /** Statuts amoureux. */
    public static final int AUCUN = 0, COUR = 1, AMOUREUX = 2, FIANCE = 3, MARIE = 4;
    public static final String[] STATUTS = { "", "tu lui fais la cour", "vous vous aimez", "fiancés", "marié" };
    /** Estime qu'il faut pour emprunter ; prêt maximal = PRET_BASE + PRET_ESTIME × estime ; délai pour rendre. */
    public static final float ESTIME_EMPRUNT = 0.4f;
    public static final int PRET_BASE = 20, PRET_ESTIME = 120, DELAI_DETTE = 10;
    /** Un villageois qui t'estime au moins ESTIME_PRET te demande parfois de l'argent ; sept sur dix rendent à temps. */
    public static final float ESTIME_PRET = 0.1f, HONNETES = 0.7f;
    /** Faire la cour : estime minimale ; amour pour déclarer sa flamme, pour demander la main ; jours d'amour avant la demande. */
    public static final float ESTIME_COUR = 0.15f, AMOUR_FLAMME = 0.7f, AMOUR_MAIN = 0.85f;
    public static final int JOURS_AVANT_MAIN = 5;
    /** Les noces coûtent une fête ; une tournée à la taverne. */
    public static final int PRIX_NOCES = 200, PRIX_TOURNEE = 6, PRIX_BARDE = 20, BARDE_JOURS = 30;

    public static final class Dette {
        public long graine;
        public String nom = "";
        public int ville, montant, limite;
        /** true : il te doit ; false : tu lui dois. */
        public boolean due;
        public boolean relancee;
    }

    public static final class Promesse {
        public long graine;
        public String nom = "", texte = "";
        public int ville, limite;
    }

    public static final class Amour {
        public long graine;
        public String nom = "";
        public boolean femme;
        public int ville, statut, depuis, vu = -1, courtise = -1;
        public float amour;
    }

    private final Joueur j;
    private final Population p;
    public final List<Dette> dettes = new ArrayList<>();
    public final List<Promesse> promesses = new ArrayList<>();
    public final Map<Long, Amour> amours = new HashMap<>();
    /** Les nouvelles de l'aspiration : un grand moment ouvre le droit d'en changer. */
    public String moment;
    /** L'envie du jour tournée vers un proche (Destin) : qui. */
    public long envieGraine;
    public String envieNom = "";
    /** Le jour de la dernière chanson payée, ville par ville ; le dernier jour où chacun a été vu (nouvelles, tournée). */
    private final Map<Integer, Integer> bardes = new HashMap<>();
    private final Map<Long, Integer> nouvelles = new HashMap<>();
    private int dernierJour = -1;
    /** La famille croisée dans le village où l'on est : graine du villageois → proche de la fiche. Publié pour l'écran (noms). */
    private Map<Long, Fiche.Proche> famille = Collections.emptyMap();
    public volatile Map<Long, String> nomsVue = Collections.emptyMap();
    public volatile Map<Long, Float> estimesVue = Collections.emptyMap();
    private int familleVille = -1;
    private long[] familleGens;

    Social(Joueur j, Population p) {
        this.j = j;
        this.p = p;
    }

    private String ville(int v) {
        return Noms.ville(p.villes.peuple[v], p.graine, v);
    }

    private static String il(boolean femme) {
        return femme ? "elle" : "il";
    }

    private static String Il(boolean femme) {
        return femme ? "Elle" : "Il";
    }

    private Relations.Lien lien(long graine) {
        Relations r = j.relations;
        return r == null ? null : r.existant(graine);
    }

    private void estime(Relations.Lien l, float d) {
        if (l == null) return;
        l.estime = Math.max(-1f, Math.min(1f, l.estime + d));
        if (j.relations != null) j.relations.modifie = true;
    }

    // ================================================================ La famille en ville

    /** Le rôle d'un proche (« ta mère ») si ce villageois incarne quelqu'un de la famille, sinon null. */
    public String role(long graine) {
        Fiche.Proche q = famille.get(graine);
        if (q != null) return roleTexte(q);
        Amour a = amours.get(graine);
        if (a != null && a.statut == MARIE) return a.femme ? "ta femme" : "ton mari";
        if (a != null && a.statut == FIANCE) return a.femme ? "ta fiancée" : "ton fiancé";
        return null;
    }

    /** Lot G2 : le proche de la famille qu'incarne ce villageois, ou null. */
    Fiche.Proche procheDe(long graine) {
        return famille.get(graine);
    }

    String roleDe(Fiche.Proche q) {
        return roleTexte(q);
    }

    private String roleTexte(Fiche.Proche q) {
        String r = j.fiche().role(q);
        boolean voyelle = "aeéiouy".indexOf(r.charAt(0)) >= 0;
        return (q.sexe == 1 && q.role != Fiche.AMOUR && !voyelle ? "ta " : "ton ") + r;
    }

    /** Trouve, dans le village où l'on est, l'habitant qui incarne chaque proche vivant de cette ville (toujours le même). */
    void rafraichirFamille() {
        Fiche f = j.fiche();
        long[] gens = j.gensIci;
        boolean[] femmes = j.gensFemmes;
        int v = j.gensVille;
        if (f == null || gens == null || femmes == null || femmes.length != gens.length || v < 0) return;
        if (v == familleVille && gens == familleGens) return;
        familleVille = v;
        familleGens = gens;
        Map<Long, Fiche.Proche> m = new HashMap<>();
        Map<Long, String> noms = new HashMap<>();
        Map<Long, Float> estimes = new HashMap<>();
        Foule fl = p.foules[f.peuple][f.region];
        boolean[] pris = new boolean[gens.length];
        for (Fiche.Proche q : f.proches) {
            if (q.mort || fl == null || gens.length == 0) continue;
            int lieu = fl.placeDe(q.id);
            if (lieu < 0) continue;
            Savoir.Temoin t = Savoir.temoin(p, fl, lieu);
            if (t.ville != v) continue;
            // Lot H2 : le proche est lui-même un habitant du village (sa graine est son id) : c'est lui qu'on croise.
            int exact = -1;
            for (int k = 0; k < gens.length; k++) if (gens[k] == q.id) exact = k;
            if (exact >= 0) {
                pris[exact] = true;
                m.put(q.id, q);
                estimes.put(q.id, q.estime / 100f);
                continue;
            }
            // Les villageois sont les vrais habitants : un proche qui n'a pas de corps ici n'en emprunte pas un autre.
            if (fl.placeDe(gens[0]) >= 0) continue;
            int depart = (int) Math.floorMod(q.id, (long) gens.length);
            for (int k = 0; k < gens.length; k++) {
                int i = (depart + k) % gens.length;
                if (pris[i] || femmes[i] != (q.sexe == 1)) continue;
                pris[i] = true;
                m.put(gens[i], q);
                noms.put(gens[i], Noms.personne(f.peuple, p.graine, q.id, q.sexe));
                estimes.put(gens[i], q.estime / 100f);
                break;
            }
        }
        // Le conjoint épousé en jeu garde son nom (c'est un habitant).
        famille = m;
        nomsVue = noms;
        estimesVue = estimes;
    }

    /** Les lignes ajoutées à « Ma famille et mes proches » : amours, dettes, promesses. */
    List<String> lignesFamille() {
        List<String> l = new ArrayList<>();
        for (Amour a : amours.values()) {
            if (a.statut == AUCUN && a.amour < 0.2f) continue;
            String s = a.statut == MARIE ? (a.femme ? "épouse" : "époux") : a.statut == FIANCE ? (a.femme ? "fiancée" : "fiancé") : a.statut == AMOUREUX ? "ton amour" : "tu lui fais la cour";
            l.add(a.nom + " — " + s + ", à " + ville(a.ville) + " (" + coeur(a.amour) + ")");
        }
        for (Dette d : dettes) l.add(d.due ? d.nom + " te doit " + Fiche.argentTexte(d.montant) + " (" + delai(d.limite) + ")" : "Tu dois " + Fiche.argentTexte(d.montant) + " à " + d.nom + " (" + delai(d.limite) + ")");
        for (Promesse q : promesses) l.add("Promis à " + q.nom + " : " + q.texte + " (" + delai(q.limite) + ")");
        return l;
    }

    private String delai(int limite) {
        int r = limite - j.jourMaintenant();
        return r < 0 ? "en retard" : r == 0 ? "aujourd'hui" : r == 1 ? "demain" : "dans " + r + " jours";
    }

    private static String coeur(float a) {
        if (a >= 0.85f) return "fou d'amour";
        if (a >= 0.6f) return "très épris";
        if (a >= 0.35f) return "sous le charme";
        return "curieux de toi";
    }

    // ================================================================ Chaque jour

    void avancer() {
        Fiche f = j.fiche();
        if (f == null) return;
        rafraichirFamille();
        int jour = j.jourMaintenant();
        if (dernierJour < 0) dernierJour = jour;
        if (jour == dernierJour) return;
        dernierJour = jour;
        // Les dettes arrivées à échéance : on te rend (ou pas) ce que tu as prêté.
        Iterator<Dette> it = dettes.iterator();
        while (it.hasNext()) {
            Dette d = it.next();
            if (!d.due || jour < d.limite) continue;
            boolean honnete = Math.floorMod(Travail.melanger(d.graine * 7L + d.limite), 100L) < Math.round(HONNETES * 100f);
            if (honnete) {
                f.argent += d.montant;
                j.annoncer(d.nom + " te rend les " + Fiche.argentTexte(d.montant) + " que tu lui avais prêtés.");
                estime(lien(d.graine), 0.05f);
                it.remove();
            } else if (!d.relancee) {
                d.relancee = true;
                j.annoncer(d.nom + " ne t'a pas rendu tes " + Fiche.argentTexte(d.montant) + ". Va le lui réclamer (X).");
            }
        }
        // Les promesses non tenues.
        Iterator<Promesse> ip = promesses.iterator();
        while (ip.hasNext()) {
            Promesse q = ip.next();
            if (jour <= q.limite) continue;
            ip.remove();
            Relations.Lien l = lien(q.graine);
            estime(l, -0.25f);
            if (l != null) l.rancune = Math.min(1f, l.rancune + 0.2f);
            j.renommee().gagner(q.ville, 0f, -3f, null);
            j.renommee().marquer(Renommee.MANQUEES);
            j.annoncer("Tu avais promis à " + q.nom + " : " + q.texte + ". Promesse rompue : " + q.nom + " s'en souviendra.");
            j.noter("Promesse rompue envers " + q.nom + " : " + q.texte + ".");
        }
        // L'amour s'use quand on ne se voit plus.
        for (Amour a : amours.values()) if (a.vu >= 0 && jour - a.vu > 10 && a.statut < MARIE) a.amour = Math.max(0f, a.amour - 0.01f);
        j.enregistrer();
    }

    void promettre(long graine, String nom, String texte, int limite, int ville) {
        Promesse q = new Promesse();
        q.graine = graine;
        q.nom = nom;
        q.texte = texte;
        q.limite = limite;
        q.ville = ville;
        promesses.add(q);
    }

    /** Une promesse tenue : elle rapproche. */
    void tenir(long graine, String debut) {
        Iterator<Promesse> it = promesses.iterator();
        while (it.hasNext()) {
            Promesse q = it.next();
            if (q.graine != graine || !q.texte.startsWith(debut)) continue;
            it.remove();
            estime(lien(graine), 0.05f);
            j.renommee().marquer(Renommee.FIABLE);
        }
    }

    // ================================================================ Grands moments

    void grandMoment(String raison) {
        if (j.fiche() == null) return;
        moment = raison;
        j.annoncer("Un grand moment de ta vie (" + raison + ") : tu peux changer d'aspiration (Mon destin).");
    }

    /** Dans « Mon destin » : changer d'aspiration, si un grand moment l'a permis. */
    void choixDestin(Joueur.Scene s) {
        if (moment == null) return;
        s.ajouter("Changer d'aspiration (" + moment + ")", this::aspirations);
    }

    private Joueur.Scene aspirations() {
        Fiche f = j.fiche();
        Joueur.Scene s = new Joueur.Scene("Changer d'aspiration", "Ce grand moment te fait voir ta vie autrement. Que veux-tu, désormais ?");
        for (int k = 0; k < Aspirations.TOUTES.size(); k++) {
            if (k == f.aspiration) continue;
            Aspirations.Aspiration a = Aspirations.TOUTES.get(k);
            int n = k;
            s.ajouter(a.nom + " — " + a.texte, () -> {
                String avant = f.aspiration >= 0 ? Aspirations.TOUTES.get(f.aspiration).nom : "rien";
                f.aspiration = n;
                f.etape = 0;
                moment = null;
                String phrase = "En l'an " + p.annee + ", tu renonces à « " + avant + " » : tu veux désormais « " + a.nom + " ».";
                f.recit.add(phrase);
                j.noter(phrase);
                j.destin().verifierEtapes();
                j.enregistrer();
                return j.destin().scene();
            });
        }
        s.ajouter("« Garder mon aspiration", () -> j.destin().scene());
        return s;
    }

    // ================================================================ Envies tournées vers les proches

    /** Une envie du jour tournée vers quelqu'un qu'on aime bien dans la ville : un parent, un ami, un amour. */
    Destin.Envie envieProche(Random h) {
        long[] gens = j.gensIci;
        String[] noms = j.gensNoms;
        if (gens == null || noms == null || noms.length != gens.length || j.gensVille != j.villeIci()) return null;
        List<Integer> l = new ArrayList<>();
        for (int k = 0; k < gens.length; k++) {
            Relations.Lien li = lien(gens[k]);
            boolean proche = famille.containsKey(gens[k]) || amours.containsKey(gens[k]) || li != null && li.estime >= 0.3f && !li.rival;
            if (proche) l.add(k);
        }
        if (l.isEmpty()) return null;
        int k = l.get(h.nextInt(l.size()));
        envieGraine = gens[k];
        Fiche.Proche q = famille.get(gens[k]);
        envieNom = q != null ? nomsVue.getOrDefault(gens[k], noms[k]) : noms[k];
        Destin.Envie e = new Destin.Envie();
        e.type = Destin.PROCHE;
        e.texte = q != null ? "Prendre des nouvelles de " + roleTexte(q) + ", " + envieNom : "Boire un coup avec " + envieNom;
        return e;
    }

    // ================================================================ Parler aux gens (X)

    void choix(Joueur.Scene s, Bande.Rencontre r) {
        Fiche f = j.fiche();
        if (f == null || r.enfant || r.ville < 0) return;
        Relations.Lien l = r.lien;
        float est = l == null ? 0f : l.estime;
        Fiche.Proche q = famille.get(r.graine);
        int jour = j.jourMaintenant();
        // Les proches et les amis.
        if (q != null) {
            Integer vu = nouvelles.get(r.graine);
            if (vu == null || vu != jour) s.ajouter("« Comment vas-tu ? »", () -> nouvelles(r, q));
            if (f.argent >= 20) s.ajouter("Lui donner un peu d'argent (" + Fiche.argentTexte(20) + ")", () -> soutenir(r, q));
        }
        if (!r.rival && (q != null || est >= 0.2f || amours.containsKey(r.graine)) && f.argent >= PRIX_TOURNEE) {
            Integer vu = nouvelles.get(r.graine);
            if (vu == null || vu != jour) s.ajouter("« Viens boire un coup avec moi. »", () -> tournee(r));
        }
        // Les dettes.
        Dette d = dette(r.graine);
        if (d != null && !d.due && f.argent >= d.montant) s.ajouter("Rendre les " + Fiche.argentTexte(d.montant) + " que tu lui dois", () -> rendre(r, d));
        if (d != null && d.due && jour >= d.limite) s.ajouter("Réclamer les " + Fiche.argentTexte(d.montant) + " qu'" + il(r.femme) + " te doit", () -> reclamer(r, d));
        if (d == null && !r.rival && (est >= ESTIME_EMPRUNT || q != null && q.estime >= 30)) s.ajouter("« Tu pourrais me prêter de l'argent ? »", () -> emprunter(r, q));
        if (d == null && !r.rival && est >= ESTIME_PRET && demandePret(r)) s.ajouter("« Tu as l'air soucieu" + (r.femme ? "se" : "x") + ". »", () -> preter(r));
        // L'amour (pas avec la famille ni les gens mariés).
        if (q == null && !r.marie && !r.rival) {
            Amour a = amours.get(r.graine);
            boolean ailleurs = engage(r.graine);
            if ((a == null || a.statut <= AMOUREUX) && est >= ESTIME_COUR && !ailleurs && (a == null || a.courtise != jour)) s.ajouter("Lui faire la cour", () -> courtiser(r));
            if (a != null && a.statut >= COUR && a.statut < MARIE) s.ajouter("Lui offrir un présent…", () -> presents(r, a));
            if (a != null && a.statut == COUR && a.amour >= AMOUR_FLAMME) s.ajouter("Lui déclarer ta flamme", () -> flamme(r, a));
            if (a != null && a.statut == AMOUREUX && a.amour >= AMOUR_MAIN && jour - a.depuis >= JOURS_AVANT_MAIN) s.ajouter("Lui demander sa main", () -> main(r, a));
            if (a != null && a.statut == FIANCE) s.ajouter("Célébrer les noces (une fête : " + Fiche.argentTexte(PRIX_NOCES) + ")", () -> noces(r, a));
        }
    }

    private Joueur.Scene reponse(Bande.Rencontre r, String texte) {
        Joueur.Scene s = new Joueur.Scene(r.nom, texte);
        s.ajouter("Continuer", j::fermerScene);
        return s;
    }

    private void envie(long graine) {
        if (graine == envieGraine && envieGraine != 0L) j.destin().signaler(Destin.PROCHE, -1, 0f);
    }

    private static final String[][] NOUVELLES = {
        { "« Tu manges assez, au moins ? Tu as maigri. »", "« Ici, rien ne change : le toit fuit toujours. »", "« Viens donc souper un de ces soirs. »" },
        { "« Ça va, ça va. On parle de toi, tu sais. »", "« La saison est dure, mais on tient. »", "« Content de te voir, vraiment. »" },
    };

    private Joueur.Scene nouvelles(Bande.Rencontre r, Fiche.Proche q) {
        nouvelles.put(r.graine, j.jourMaintenant());
        q.estime = Math.min(100, q.estime + 2);
        j.destin().moral = Math.min(100f, j.destin().moral + 3f);
        envie(r.graine);
        boolean parent = q.role == Fiche.PERE || q.role == Fiche.MERE;
        String[] t = NOUVELLES[parent ? 0 : 1];
        j.gesteJoueur = "saluer_main";
        j.enregistrer();
        return reponse(r, t[(int) Math.floorMod(r.graine + j.jourMaintenant(), (long) t.length)]);
    }

    private Joueur.Scene soutenir(Bande.Rencontre r, Fiche.Proche q) {
        Fiche f = j.fiche();
        f.argent -= 20;
        q.estime = Math.min(100, q.estime + 5);
        envie(r.graine);
        j.gesteJoueur = "donner_objet";
        j.enregistrer();
        return reponse(r, Il(r.femme) + " refuse d'abord, puis glisse les pièces dans sa poche : « Tu es bon" + (f.femme() ? "ne" : "") + " avec nous. »");
    }

    private Joueur.Scene tournee(Bande.Rencontre r) {
        Fiche f = j.fiche();
        f.argent -= PRIX_TOURNEE;
        nouvelles.put(r.graine, j.jourMaintenant());
        estime(r.lien, 0.08f);
        Amour a = amours.get(r.graine);
        if (a != null) {
            a.amour = Math.min(1f, a.amour + 0.05f);
            a.vu = j.jourMaintenant();
        }
        Fiche.Proche q = famille.get(r.graine);
        if (q != null) q.estime = Math.min(100, q.estime + 3);
        j.destin().moral = Math.min(100f, j.destin().moral + 4f);
        j.destin().signaler(Destin.BIERE, -1, 0f);
        envie(r.graine);
        j.passer(1.0);
        j.enregistrer();
        return reponse(r, "Vous allez boire une bière ensemble. " + Il(r.femme) + " te raconte les nouvelles du quartier, et vous riez comme de vieux amis. (1 heure, " + Fiche.argentTexte(PRIX_TOURNEE) + ")");
    }

    // ---------------------------------------------------------------- Dettes

    public Dette dette(long graine) {
        for (Dette d : dettes) if (d.graine == graine) return d;
        return null;
    }

    private Joueur.Scene emprunter(Bande.Rencontre r, Fiche.Proche q) {
        float est = r.lien == null ? 0f : r.lien.estime;
        if (q != null) est = Math.max(est, q.estime / 100f);
        int montant = PRET_BASE + Math.round(PRET_ESTIME * est) * (q != null ? 2 : 1);
        Joueur.Scene s = new Joueur.Scene(r.nom, Il(r.femme) + " réfléchit : « Je peux te prêter " + Fiche.argentTexte(montant) + ", pas plus. Tu me les rends dans " + DELAI_DETTE + " jours ? »");
        s.ajouter("« Promis. »", () -> {
            Dette d = new Dette();
            d.graine = r.graine;
            d.nom = r.nom;
            d.ville = r.ville;
            d.montant = montant;
            d.limite = j.jourMaintenant() + DELAI_DETTE;
            dettes.add(d);
            j.fiche().argent += montant;
            promettre(r.graine, r.nom, "lui rendre " + Fiche.argentTexte(montant), d.limite, r.ville);
            j.gesteJoueur = "recevoir";
            j.enregistrer();
            return reponse(r, Il(r.femme) + " compte les pièces dans ta main.");
        });
        s.ajouter("« Non, laisse. »", j::fermerScene);
        return s;
    }

    private Joueur.Scene rendre(Bande.Rencontre r, Dette d) {
        Fiche f = j.fiche();
        f.argent -= d.montant;
        dettes.remove(d);
        boolean aTemps = j.jourMaintenant() <= d.limite;
        tenir(r.graine, "lui rendre");
        if (aTemps) estime(r.lien, 0.05f);
        j.gesteJoueur = "donner_objet";
        j.enregistrer();
        return reponse(r, aTemps ? "« Merci. On peut compter sur toi. »" : "« Enfin ! Je n'y croyais plus. »");
    }

    /** Une fois sur quatre (le même jour pour la même personne), un villageois qui t'estime est à court d'argent. */
    private boolean demandePret(Bande.Rencontre r) {
        int n = 0;
        for (Dette d : dettes) if (d.due) n++;
        return n < 2 && Math.floorMod(Travail.melanger(r.graine * 5L + j.jourMaintenant()), 4L) == 0L;
    }

    private Joueur.Scene preter(Bande.Rencontre r) {
        int montant = 15 + (int) Math.floorMod(Travail.melanger(r.graine), 50L);
        Joueur.Scene s = new Joueur.Scene(r.nom, "« Je suis à court… Tu pourrais me prêter " + Fiche.argentTexte(montant) + " ? Je te les rends dans " + DELAI_DETTE + " jours, c'est juré. »");
        if (j.fiche().argent >= montant) s.ajouter("Lui prêter " + Fiche.argentTexte(montant), () -> {
            Dette d = new Dette();
            d.graine = r.graine;
            d.nom = r.nom;
            d.ville = r.ville;
            d.montant = montant;
            d.limite = j.jourMaintenant() + DELAI_DETTE;
            d.due = true;
            dettes.add(d);
            j.fiche().argent -= montant;
            estime(r.lien, 0.1f);
            j.gesteJoueur = "donner_objet";
            j.enregistrer();
            return reponse(r, "« Merci. Je n'oublierai pas. »");
        });
        s.ajouter("« Désolé" + j.fiche().e() + ", je ne peux pas. »", () -> {
            estime(r.lien, -0.02f);
            return j.fermerScene();
        });
        return s;
    }

    private Joueur.Scene reclamer(Bande.Rencontre r, Dette d) {
        Fiche f = j.fiche();
        Random h = new Random(Travail.melanger(r.graine * 3L + j.jourMaintenant()));
        int jet = 5 * f.attributs[Fiche.PRESENCE] + f.competences[Fiche.ELOQUENCE] / 3 + h.nextInt(50);
        f.apprendre(Fiche.ELOQUENCE, 1f);
        if (jet >= 40) {
            f.argent += d.montant;
            dettes.remove(d);
            j.gesteJoueur = "recevoir";
            j.enregistrer();
            return reponse(r, Il(r.femme) + " baisse les yeux et te rend les " + Fiche.argentTexte(d.montant) + ". « Pardon pour le retard. »");
        }
        estime(r.lien, -0.05f);
        j.enregistrer();
        return reponse(r, "« Je n'ai rien, je te dis ! Laisse-moi un peu de temps. »");
    }

    // ---------------------------------------------------------------- L'amour

    private boolean engage(long sauf) {
        for (Amour a : amours.values()) if (a.graine != sauf && a.statut >= AMOUREUX) return true;
        return false;
    }

    /** Le conjoint épousé en jeu, ou null. */
    public Amour conjoint() {
        for (Amour a : amours.values()) if (a.statut == MARIE) return a;
        return null;
    }

    public boolean marie() {
        for (Amour a : amours.values()) if (a.statut == MARIE) return true;
        return false;
    }

    public float meilleurAmour() {
        float m = 0f;
        for (Amour a : amours.values()) m = Math.max(m, a.amour);
        return m;
    }

    public int meilleurStatut() {
        int m = AUCUN;
        for (Amour a : amours.values()) m = Math.max(m, a.statut);
        return m;
    }

    private Amour amour(Bande.Rencontre r) {
        return amours.computeIfAbsent(r.graine, g -> {
            Amour a = new Amour();
            a.graine = g;
            a.nom = r.nom;
            a.femme = r.femme;
            a.ville = r.ville;
            return a;
        });
    }

    private static final String[] COUR_OUI = { "rougit et sourit", "rit à ta plaisanterie", "te regarde un peu plus longtemps", "accepte de marcher un moment avec toi" };
    private static final String[] COUR_NON = { "hausse les épaules", "a mieux à faire", "trouve tes compliments un peu lourds" };

    private Joueur.Scene courtiser(Bande.Rencontre r) {
        Fiche f = j.fiche();
        Amour a = amour(r);
        int jour = j.jourMaintenant();
        a.courtise = jour;
        a.vu = jour;
        Random h = new Random(Travail.melanger(r.graine * 11L + jour));
        float est = r.lien == null ? 0f : r.lien.estime;
        int jet = f.competences[Fiche.ELOQUENCE] / 2 + 4 * f.attributs[Fiche.PRESENCE] + Math.round(40f * est) + h.nextInt(50);
        f.apprendre(Fiche.ELOQUENCE, 2f);
        String t;
        if (jet >= 45) {
            a.amour = Math.min(1f, a.amour + 0.12f + h.nextFloat() * 0.08f);
            estime(r.lien, 0.03f);
            if (a.statut == AUCUN) {
                a.statut = COUR;
                j.noter("En l'an " + p.annee + ", tu commences à faire la cour à " + r.nom + ".");
            }
            t = r.nom + " " + COUR_OUI[h.nextInt(COUR_OUI.length)] + ". (" + coeur(a.amour) + ")";
        } else {
            a.amour = Math.max(0f, a.amour - 0.05f);
            t = r.nom + " " + COUR_NON[h.nextInt(COUR_NON.length)] + ". Reviens un autre jour.";
        }
        j.destin().verifierEtapes();
        j.enregistrer();
        return reponse(r, t);
    }

    private Joueur.Scene presents(Bande.Rencontre r, Amour a) {
        Fiche f = j.fiche();
        Joueur.Scene s = new Joueur.Scene("Un présent pour " + r.nom, "Que lui offres-tu ?");
        for (Biens.Bien b : Biens.TOUS) {
            if (b.place == Biens.BETE || f.sac.nombre(b.id) <= 0 || b.prix < 2) continue;
            s.ajouter(b.majuscule() + " (" + Fiche.argentTexte(Math.round(b.prix)) + ")", () -> {
                f.sac.retirer(b.id, 1);
                float gain = Math.min(0.25f, 0.03f + b.prix / 200f);
                a.amour = Math.min(1f, a.amour + gain);
                a.vu = j.jourMaintenant();
                estime(r.lien, gain * 0.5f);
                j.gesteJoueur = "donner_objet";
                j.rafraichirSac();
                j.destin().verifierEtapes();
                j.enregistrer();
                return reponse(r, r.nom + " prend " + b.un() + " et le garde contre " + (r.femme ? "elle" : "lui") + ". (" + coeur(a.amour) + ")");
            });
            if (s.choix.size() >= 10) break;
        }
        if (s.choix.isEmpty()) return reponse(r, "Tu n'as rien dans ton sac qui vaille la peine d'être offert.");
        s.ajouter("« Rien, finalement. »", j::fermerScene);
        return s;
    }

    private Joueur.Scene flamme(Bande.Rencontre r, Amour a) {
        Random h = new Random(Travail.melanger(r.graine * 13L + j.jourMaintenant()));
        if (a.amour + h.nextFloat() * 0.3f < 0.8f) {
            a.amour = Math.max(0f, a.amour - 0.1f);
            j.enregistrer();
            return reponse(r, r.nom + " détourne les yeux : « C'est trop tôt… Laisse-moi un peu de temps. »");
        }
        a.statut = AMOUREUX;
        a.depuis = j.jourMaintenant();
        String phrase = "En l'an " + p.annee + ", " + r.nom + " et toi vous avouez votre amour.";
        j.fiche().recit.add(phrase);
        j.noter(phrase);
        j.destin().moral = Math.min(100f, j.destin().moral + 15f);
        j.destin().verifierEtapes();
        j.enregistrer();
        return reponse(r, r.nom + " te prend la main : « Moi aussi. Depuis le premier jour. »");
    }

    private Joueur.Scene main(Bande.Rencontre r, Amour a) {
        a.statut = FIANCE;
        String phrase = "En l'an " + p.annee + ", tu demandes la main de " + r.nom + ", qui dit oui.";
        j.fiche().recit.add(phrase);
        j.noter(phrase);
        j.annoncer("Fiançailles : " + r.nom + " a dit oui ! Les noces se célèbrent quand tu veux (X).");
        j.enregistrer();
        return reponse(r, "« Oui. Oui, bien sûr ! » Toute la rue l'a entendu.");
    }

    private Joueur.Scene noces(Bande.Rencontre r, Amour a) {
        Fiche f = j.fiche();
        if (f.argent < PRIX_NOCES) return reponse(r, "« Il nous faut de quoi faire la fête, tout de même : " + Fiche.argentTexte(PRIX_NOCES) + ". »");
        f.argent -= PRIX_NOCES;
        j.marche().ville(r.ville, j.jour()).bourse += PRIX_NOCES;
        a.statut = MARIE;
        a.depuis = j.jourMaintenant();
        a.amour = 1f;
        String phrase = "En l'an " + p.annee + ", tu épouses " + r.nom + " " + Creation.de(ville(r.ville)) + ". Toute la ville danse jusqu'à l'aube.";
        f.recit.add(phrase);
        j.noter(phrase);
        j.renommee().gagner(r.ville, 5f, 5f, "Tes noces");
        j.destin().moral = 100f;
        j.passer(4.0);
        grandMoment("ton mariage");
        j.destin().verifierEtapes();
        j.enregistrer();
        return reponse(r, "Les noces ont lieu sur la place : musique, bière, danses jusqu'à l'aube. " + r.nom + " est désormais " + (r.femme ? "ta femme." : "ton mari."));
    }

    // ================================================================ Les bardes

    void choixBoutique(Joueur.Scene s, int lieu, EconomieVille.Agent a) {
        if (lieu != Metiers.TAVERNE && lieu != Metiers.AUBERGE) return;
        int v = j.villeIci();
        if (v < 0) return;
        s.ajouter("Écouter le barde", () -> barde(lieu, v));
    }

    /** Les exploits du joueur, tirés de son carnet, pour la chanson. */
    List<String> exploits() {
        List<String> l = new ArrayList<>();
        for (String c : j.carnet()) {
            if (c.startsWith("Mission réussie : ") && !c.contains("(X)") && !c.contains("aimerait")) l.add(c.substring(18));
            else if (c.startsWith("En l'an ") && (c.contains("loups") || c.contains("brigands") || c.contains("maître") || c.contains("épouses"))) l.add(c.substring(c.indexOf(',') + 2));
        }
        return l.size() > 3 ? l.subList(l.size() - 3, l.size()) : l;
    }

    private static final String[] VIEILLES = {
        "une vieille complainte sur un roi noyé dans les marais, que la salle reprend en chœur",
        "la ballade des sept frères partis à la guerre, dont un seul revint",
        "une chanson à boire si leste que la tavernière lui jette son torchon",
    };

    private Joueur.Scene barde(int lieu, int v) {
        Fiche f = j.fiche();
        Renommee r = j.renommee();
        List<String> ex = exploits();
        int jour = j.jourMaintenant();
        String nom = r.surnomEn(v).isEmpty() ? f.nom : r.surnomEn(v);
        StringBuilder t = new StringBuilder();
        if (r.palierEn(v) >= 1 && !ex.isEmpty()) {
            t.append("Le barde accorde son luth et, les yeux sur toi, chante la geste ").append(Creation.de(nom)).append(" :");
            for (String e : ex) t.append("\n  « ").append(Character.toUpperCase(e.charAt(0))).append(e.substring(1).replaceAll("\\.$", "")).append("… »");
            t.append("\nDes têtes se tournent vers toi.");
            j.destin().moral = Math.min(100f, j.destin().moral + 2f);
        } else t.append("Le barde chante ").append(VIEILLES[(int) Math.floorMod(jour + v, VIEILLES.length)]).append(". Personne ici ne chante encore tes exploits.");
        Joueur.Scene s = new Joueur.Scene("Le barde", t.toString());
        Integer der = bardes.get(v);
        boolean libre = der == null || jour - der >= BARDE_JOURS;
        if (!ex.isEmpty() && libre && f.argent >= PRIX_BARDE) s.ajouter("Le payer pour qu'il chante tes exploits sur les routes (" + Fiche.argentTexte(PRIX_BARDE) + ")", () -> {
            f.argent -= PRIX_BARDE;
            bardes.put(v, jour);
            r.barde(v, jour + BARDE_JOURS);
            r.gagner(v, 3f, 0f, "Le barde chante tes exploits");
            String chanson = "La chanson de " + nom + ", par le barde " + Creation.de(ville(v)) + " : " + String.join(" ; ", ex) + ".";
            j.noter(chanson);
            j.chansons().barde(nom, v, ex);
            j.enregistrer();
            return j.boutiqueScene(lieu, "Le barde empoche les pièces : « D'ici un mois, on chantera ton nom dans toutes les tavernes de la région. »");
        });
        else if (!libre) t.append("\nIl chante déjà ta chanson sur les routes.");
        s.ajouter("« Revenir", () -> j.boutiqueScene(lieu));
        return s;
    }

    // ================================================================ Essais

    public Joueur.Scene bardePourEssai(int lieu) {
        return barde(lieu, j.villeIci());
    }

    public Joueur.Scene destinPourEssai() {
        return j.destin().scene();
    }

    public Joueur.Scene renomPourEssai() {
        return j.destin().renom();
    }

    public void marquerPourEssai(int etiquette) {
        j.renommee().marquer(etiquette);
    }

    public void guildePourEssai(int metier, float d) {
        j.renommee().guilde(metier, d);
    }

    // ================================================================ Sauvegarde

    void ecrire(DataOutputStream s) throws IOException {
        s.writeInt(dettes.size());
        for (Dette d : dettes) {
            s.writeLong(d.graine);
            s.writeUTF(d.nom);
            s.writeInt(d.ville);
            s.writeInt(d.montant);
            s.writeInt(d.limite);
            s.writeBoolean(d.due);
            s.writeBoolean(d.relancee);
        }
        s.writeInt(promesses.size());
        for (Promesse q : promesses) {
            s.writeLong(q.graine);
            s.writeUTF(q.nom);
            s.writeUTF(q.texte);
            s.writeInt(q.ville);
            s.writeInt(q.limite);
        }
        s.writeInt(amours.size());
        for (Amour a : amours.values()) {
            s.writeLong(a.graine);
            s.writeUTF(a.nom);
            s.writeBoolean(a.femme);
            s.writeInt(a.ville);
            s.writeInt(a.statut);
            s.writeInt(a.depuis);
            s.writeInt(a.vu);
            s.writeInt(a.courtise);
            s.writeFloat(a.amour);
        }
        s.writeUTF(moment == null ? "" : moment);
        s.writeLong(envieGraine);
        s.writeUTF(envieNom);
        s.writeInt(dernierJour);
        s.writeInt(bardes.size());
        for (Map.Entry<Integer, Integer> e : bardes.entrySet()) {
            s.writeInt(e.getKey());
            s.writeInt(e.getValue());
        }
    }

    void lire(DataInputStream s) throws IOException {
        int n = s.readInt();
        for (int k = 0; k < n; k++) {
            Dette d = new Dette();
            d.graine = s.readLong();
            d.nom = s.readUTF();
            d.ville = s.readInt();
            d.montant = s.readInt();
            d.limite = s.readInt();
            d.due = s.readBoolean();
            d.relancee = s.readBoolean();
            dettes.add(d);
        }
        n = s.readInt();
        for (int k = 0; k < n; k++) {
            Promesse q = new Promesse();
            q.graine = s.readLong();
            q.nom = s.readUTF();
            q.texte = s.readUTF();
            q.ville = s.readInt();
            q.limite = s.readInt();
            promesses.add(q);
        }
        n = s.readInt();
        for (int k = 0; k < n; k++) {
            Amour a = new Amour();
            a.graine = s.readLong();
            a.nom = s.readUTF();
            a.femme = s.readBoolean();
            a.ville = s.readInt();
            a.statut = s.readInt();
            a.depuis = s.readInt();
            a.vu = s.readInt();
            a.courtise = s.readInt();
            a.amour = s.readFloat();
            amours.put(a.graine, a);
        }
        String m = s.readUTF();
        moment = m.isEmpty() ? null : m;
        envieGraine = s.readLong();
        envieNom = s.readUTF();
        dernierJour = s.readInt();
        n = s.readInt();
        for (int k = 0; k < n; k++) bardes.put(s.readInt(), s.readInt());
    }
}
