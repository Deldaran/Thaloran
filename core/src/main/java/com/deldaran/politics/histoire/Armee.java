package com.deldaran.politics.histoire;

import com.deldaran.politics.Metiers;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Phase 10 (lot 10b) : l'armée, la guerre et la couronne.
 * - LES TROUPES : en plus des compagnons nommés de la bande, des soldats levés (piquiers, archers, cavaliers), recrutés à la caserne
 *   ou levés dans son fief, payés chaque jour ; sans solde, ils désertent ;
 * - LES STADES : bande (20 hommes), compagnie (60 : une base et un lieutenant), troupe (200 : renom 150 et deux officiers),
 *   armée (1 000 : être noble et tenir un fief) ;
 * - LA GUERRE : quand un royaume est en guerre, on rejoint l'ost de son suzerain, ou l'on se loue comme mercenaire (contrat de 30 jours) ;
 *   on marche sur les villes ennemies (siège) ; une bataille se résout comme les combats de la phase 9, chaque combattant valant
 *   plusieurs hommes quand les armées sont grandes ;
 * - LA COURONNE : noble, avec un fief, trois vassaux ralliés et cent hommes, on réclame le trône (par le sang si l'on a épousé la Maison
 *   régnante, par le peuple si l'on est célèbre dans la capitale) ; on bat l'armée royale et l'on est couronné. Roi ou reine, on lève
 *   l'impôt royal, on déclare la guerre et l'on fait la paix ; prendre la capitale d'un royaume ennemi en prend aussi la couronne.
 */
public final class Armee {
    public static final int PIQUIER = 0, ARCHER = 1, CAVALIER = 2, TYPES = 3;
    public static final String[][] NOMS = { { "piquier", "piquiers" }, { "archer", "archers" }, { "cavalier", "cavaliers" } };
    public static final int[] PRIX = { 30, 40, 120 }, SOLDE = { 3, 4, 8 };
    public static final float[] FORCE = { 24f, 25f, 34f };
    /** Les stades : nom et effectif maximal. */
    public static final String[] STADES = { "bande", "compagnie", "troupe", "armée" };
    public static final int[] LIMITES = { 20, 60, 200, 1000 };
    public static final String[] POSSESSIFS = { "ta bande", "ta compagnie", "ta troupe", "ton armée" };

    private static String majuscule(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
    /** Au-delà de COMBATTANTS par camp, chaque combattant de la bataille vaut plusieurs hommes. */
    public static final int COMBATTANTS = 50;
    /** Un contrat de mercenaire dure CONTRAT jours et paie GAGE cuivre par homme et par jour. */
    public static final int CONTRAT = 30, GAGE = 4;
    /** Rallier un vassal : prix ; réclamer le trône : hommes nécessaires. */
    public static final int PRIX_VASSAL = 2000, HOMMES_TRONE = 100, VASSAUX = 3;
    /** Après trois jours sans solde, une part des soldats déserte chaque jour. */
    public static final float DESERTION = 0.15f;

    public static final class Contrat {
        public int etat, fin, victoires;
    }

    private final Joueur j;
    private final Population p;
    public final int[] soldats = new int[TYPES];
    public int impayes, victoires, batailles, forts, vassaux, contrats, capitalesPrises, tentatives;
    public boolean grandeVictoire, reclame;
    public Contrat contrat;
    /** Les royaumes dont le joueur porte la couronne. */
    public final List<Integer> couronnes = new ArrayList<>();
    /** Recrues déjà prises par ville cette saison : ville → { saison, nombre }. */
    private final Map<Integer, int[]> recrues = new HashMap<>();
    private int dernierJour = -1, derniereBataille = -1;
    /** Pour les essais : la prochaine bataille est gagnée. */
    public boolean forcer;

    Armee(Joueur j, Population p) {
        this.j = j;
        this.p = p;
    }

    // ================================================================ L'effectif

    public int soldats() {
        int n = 0;
        for (int x : soldats) n += x;
        return n;
    }

    /** Toi, ta bande et tes soldats. */
    public int effectif() {
        return 1 + j.bande().taille() + soldats();
    }

    public int stade() {
        Noblesse n = j.noblesse();
        if (n.noble && !n.fiefs.isEmpty()) return 3;
        if (j.renommee().meilleurRenom() >= 150f && j.bande().taille() >= 2) return 2;
        if ((j.corps().aUneMaison() || !n.fiefs.isEmpty()) && j.bande().taille() >= 1) return 1;
        return 0;
    }

    public int limite() {
        return LIMITES[stade()];
    }

    private String exigences() {
        switch (stade()) {
            case 0: return "Pour une compagnie (60 hommes), il te faut une base (une maison ou un fief) et un lieutenant (un compagnon dans ta bande).";
            case 1: return "Pour une troupe (200 hommes), il te faut un renom de 150 et deux officiers (deux compagnons).";
            case 2: return "Pour une armée, il te faut être noble et tenir un fief.";
            default: return "";
        }
    }

    public int soldeJour() {
        int n = 0;
        for (int k = 0; k < TYPES; k++) n += soldats[k] * SOLDE[k];
        return n;
    }

    private String troupes() {
        List<String> l = new ArrayList<>();
        for (int k = 0; k < TYPES; k++) if (soldats[k] > 0) l.add(soldats[k] + " " + NOMS[k][soldats[k] > 1 ? 1 : 0]);
        return l.isEmpty() ? "aucun soldat" : String.join(", ", l);
    }

    private String ville(int v) {
        return Noms.ville(p.villes.peuple[v], p.graine, v);
    }

    private String royaume(int e) {
        return Noms.ville(p.etats.peuple[e], p.graine, p.etats.capitale[e]);
    }

    public boolean roi(int e) {
        return couronnes.contains(e);
    }

    // ================================================================ Le temps

    void avancer() {
        Fiche f = j.fiche();
        if (f == null) return;
        int jour = j.jourMaintenant();
        if (dernierJour < 0) dernierJour = jour;
        for (int d = Math.max(dernierJour, jour - 60) + 1; d <= jour; d++) journee(d);
        if (jour != dernierJour) j.enregistrer();
        dernierJour = Math.max(dernierJour, jour);
    }

    void sauter() {
        dernierJour = j.jourMaintenant();
        contrat = null;
    }

    private void journee(int d) {
        Fiche f = j.fiche();
        int solde = soldeJour();
        if (solde > 0) {
            if (f.argent >= solde) {
                f.argent -= solde;
                impayes = 0;
            } else if (++impayes >= 3) {
                int partis = 0;
                for (int k = 0; k < TYPES; k++) {
                    int x = Math.max(soldats[k] > 0 ? 1 : 0, Math.round(soldats[k] * DESERTION));
                    soldats[k] -= x;
                    partis += x;
                }
                if (partis > 0) j.annoncer("Sans solde depuis " + impayes + " jours, " + partis + " soldats désertent.");
            } else j.annoncer("Tu ne peux pas payer tes soldats (" + Fiche.argentTexte(solde) + " par jour) : ils grognent.");
        }
        if (contrat != null) {
            f.argent += soldats() * GAGE;
            if (!p.etats.estVivant(contrat.etat) || p.etats.ennemi[contrat.etat] < 0 || d >= contrat.fin) finirContrat();
        }
        if (Math.floorMod(d, Terre.JOURS_PAR_SAISON) == 0) for (int e : couronnes) {
            int impot = 0;
            for (int v = 0; v < p.villes.nombre; v++) if (p.villes.estVivante(v) && j.noblesse().etatDe(v) == e) impot += p.villes.habitants[v];
            f.argent += impot;
            j.annoncer("L'impôt royal de " + royaume(e) + " rentre : " + Fiche.argentTexte(impot) + ".");
        }
    }

    private void finirContrat() {
        Contrat c = contrat;
        contrat = null;
        if (c.victoires > 0) {
            contrats++;
            j.renommee().changer(Renommee.ROYAUME, c.etat, 10f);
            j.annoncer("Ton contrat avec " + royaume(c.etat) + " s'achève : " + c.victoires + " victoire" + (c.victoires > 1 ? "s" : "") + ". On se souviendra de ta compagnie.");
        } else j.annoncer("Ton contrat avec " + royaume(c.etat) + " s'achève sans gloire.");
        j.destin().verifierEtapes();
    }

    /** La couronne passe à l'héritier. */
    void heriter(long id) {
        for (int e : couronnes) p.etats.chef[e] = id;
    }

    /** Au chargement : le monde (qui ne garde pas la partie) reprend les couronnes du joueur. */
    void appliquer() {
        Fiche f = j.fiche();
        if (f != null) heriter(f.id);
    }

    // ================================================================ La caserne et la levée

    /** Devant une caserne (M) : recruter. */
    Joueur.Scene devant(float[] d) {
        int v = j.villeIci();
        if (v < 0 || d == null || Float.isNaN(d[Metiers.CASERNE]) || d[Metiers.CASERNE] > Joueur.PROCHE) return null;
        return caserne(null);
    }

    private int disponibles(int v, int part) {
        int saison = Math.floorDiv(j.jourMaintenant(), Terre.JOURS_PAR_SAISON);
        int[] r = recrues.get(v);
        int pris = r != null && r[0] == saison ? r[1] : 0;
        return Math.max(0, p.villes.habitants[v] / part - pris);
    }

    private void prendre(int v, int n) {
        int saison = Math.floorDiv(j.jourMaintenant(), Terre.JOURS_PAR_SAISON);
        int[] r = recrues.get(v);
        if (r == null || r[0] != saison) recrues.put(v, r = new int[] { saison, 0 });
        r[1] += n;
    }

    Joueur.Scene caserne(String message) {
        Fiche f = j.fiche();
        int v = j.villeIci();
        int dispo = disponibles(v, 10);
        StringBuilder t = new StringBuilder("La caserne " + Creation.de(ville(v)) + ". " + dispo + " hommes s'engageraient cette saison.");
        t.append("\n").append(majuscule(POSSESSIFS[stade()])).append(" : ").append(effectif()).append(" hommes sur ").append(limite()).append(" (").append(troupes()).append("), solde ").append(Fiche.argentTexte(soldeJour())).append(" par jour.");
        if (stade() < 3) t.append("\n").append(exigences());
        Joueur.Scene s = new Joueur.Scene("La caserne", Joueur.avec(message, t.toString()));
        int place = limite() - effectif();
        for (int k = 0; k < TYPES; k++) {
            int type = k, n = Math.min(5, Math.min(dispo, place));
            if (n <= 0) break;
            s.ajouter("Recruter " + n + " " + NOMS[k][n > 1 ? 1 : 0] + " (" + Fiche.argentTexte(PRIX[k] * n) + ", " + SOLDE[k] + " cuivre par jour chacun)", () -> recruter(v, type, n));
        }
        if (soldats() > 0) s.ajouter("Renvoyer cinq soldats", () -> {
            int n = 5;
            for (int k = TYPES - 1; k >= 0 && n > 0; k--) {
                int x = Math.min(n, soldats[k]);
                soldats[k] -= x;
                n -= x;
            }
            j.enregistrer();
            return caserne("Ils rendent leurs armes et rentrent chez eux.");
        });
        s.ajouter("« Revenir", j::fermerScene);
        return s;
    }

    private Joueur.Scene recruter(int v, int type, int n) {
        Fiche f = j.fiche();
        int prix = PRIX[type] * n;
        if (f.argent < prix) return caserne("Il faut " + Fiche.argentTexte(prix) + ".");
        f.argent -= prix;
        soldats[type] += n;
        prendre(v, n);
        j.destin().verifierEtapes();
        j.enregistrer();
        return caserne(n + " " + NOMS[type][1] + " signent leur engagement.");
    }

    /** Dans son fief : lever des hommes (des piquiers, sans prix de recrue, mais le village grogne). */
    void choixFief(Joueur.Scene s, Noblesse.Fief x) {
        int n = Math.min(disponibles(x.ville, 15), limite() - effectif());
        if (n <= 0) return;
        int levee = Math.min(10, n);
        s.ajouter("Lever " + levee + " hommes dans le fief (piquiers)", () -> {
            soldats[PIQUIER] += levee;
            prendre(x.ville, levee);
            j.renommee().gagner(x.ville, 0f, -2f, "levée");
            j.destin().verifierEtapes();
            j.enregistrer();
            return j.noblesse().fiefScene(x, levee + " hommes quittent leurs champs pour te suivre.");
        });
    }

    // ================================================================ La bataille

    public static final class Issue {
        public boolean victoire;
        public int nous, eux, mortsNous, mortsEux;
        public String texte = "";
    }

    /** Une bataille contre eux hommes de force forceEux. */
    Issue bataille(int eux, float forceEux, String contre, long graine) {
        Fiche f = j.fiche();
        Random h = new Random(graine);
        Issue r = new Issue();
        r.nous = effectif();
        r.eux = eux;
        int k = Math.max(1, (int) Math.ceil(Math.max(r.nous, eux) / (float) COMBATTANTS));
        List<Combat.Combattant> tous = new ArrayList<>();
        int[] eq = Combat.equipement(f.sac.portes());
        Combat.Combattant moi = new Combat.Combattant();
        moi.nom = f.nom;
        moi.joueur = true;
        moi.camp = Combat.NOUS;
        moi.graine = -1L;
        moi.arme = eq[0];
        moi.armure = eq[1];
        moi.force = Combat.forceJoueur(f, eq[0]);
        tous.add(moi);
        tous.addAll(j.bande().combattants());
        for (int t = 0; t < TYPES; t++) {
            int n = Math.round(soldats[t] / (float) k);
            if (n == 0 && soldats[t] > 0) n = 1;
            for (int q = 0; q < n; q++) {
                Combat.Combattant c = new Combat.Combattant();
                c.nom = NOMS[t][0];
                c.camp = Combat.NOUS;
                c.graine = -100L - t;
                c.force = FORCE[t];
                tous.add(c);
            }
        }
        int n = Math.max(1, Math.round(eux / (float) k));
        for (int q = 0; q < n; q++) {
            Combat.Combattant c = new Combat.Combattant();
            c.nom = "un soldat " + Creation.de(contre);
            c.camp = Combat.EUX;
            c.graine = h.nextLong();
            c.force = forceEux + h.nextInt(8);
            tous.add(c);
        }
        Combat.Resultat res = Combat.resoudre(tous, f.competences[Fiche.TACTIQUE] + f.competences[Fiche.COMMANDEMENT] / 2f, 30f, 65f, 55f, h.nextLong());
        r.victoire = forcer || res.vainqueur == Combat.NOUS;
        forcer = false;
        // Les pertes : un combattant tombé vaut k hommes ; à terre, un sur trois ne se relève pas.
        int[] morts = new int[TYPES];
        for (Combat.Combattant c : res.tous) {
            if (c.camp == Combat.NOUS && c.graine <= -100L) {
                int t = (int) (-100L - c.graine);
                int m = c.etat == Combat.MORT ? k : c.etat == Combat.A_TERRE ? Math.max(0, Math.round(k / 3f)) : 0;
                morts[t] += m;
            } else if (c.camp == Combat.EUX) r.mortsEux += c.etat == Combat.MORT ? k : c.etat == Combat.A_TERRE ? Math.round(k / 2f) : 0;
        }
        for (int t = 0; t < TYPES; t++) {
            morts[t] = Math.min(morts[t], soldats[t]);
            soldats[t] -= morts[t];
            r.mortsNous += morts[t];
        }
        if (!r.victoire) r.mortsEux = Math.min(r.mortsEux, eux / 3);
        else r.mortsEux = Math.max(r.mortsEux, eux / 4);
        r.mortsEux = Math.min(r.mortsEux, eux);
        String pertes = j.bande().apres(res);
        j.santeJoueur = Math.max(0.05f, res.tous.get(0).sante);
        if (!r.victoire && res.tous.get(0).etat == Combat.A_TERRE && res.tous.get(0).sante <= 0.2f) j.lignee().blessureMortelle("tombé" + f.e() + " à la bataille");
        batailles++;
        if (r.victoire) {
            victoires++;
            if (eux >= 2 * r.nous) grandeVictoire = true;
        }
        f.apprendre(Fiche.TACTIQUE, 3f + r.nous / 20f);
        f.apprendre(Fiche.COMMANDEMENT, 2f + r.nous / 20f);
        r.texte = r.nous + " contre " + eux + (k > 1 ? " (chaque combattant en vaut " + k + ")" : "") + ". "
                + (r.victoire ? "Victoire ! L'ennemi laisse " + r.mortsEux + " morts sur le terrain." : "Défaite : vous rompez et fuyez.")
                + " Tu perds " + r.mortsNous + " soldats." + (pertes == null || pertes.isEmpty() ? "" : " " + pertes);
        j.passer(8.0);
        derniereBataille = j.jourMaintenant();
        return r;
    }

    private boolean fatigue() {
        return derniereBataille == j.jourMaintenant();
    }

    /** À la cour d'un royaume e : la guerre (l'ost, le mercenariat) et la couronne. */
    void choixCour(Joueur.Scene s, int e) {
        Fiche f = j.fiche();
        Noblesse n = j.noblesse();
        int ennemi = p.etats.ennemi[e];
        if (roi(e)) {
            couronneChoix(s, e);
            return;
        }
        if (ennemi >= 0 && !fatigue()) {
            if (n.suzerain == e) s.ajouter("Rejoindre l'ost contre " + royaume(ennemi) + " (" + effectif() + " hommes)", () -> ost(e, false));
            else if (contrat != null && contrat.etat == e) s.ajouter("Combattre pour " + royaume(e) + " contre " + royaume(ennemi), () -> ost(e, true));
            else if (contrat == null && soldats() >= 10) s.ajouter("Louer ta compagnie (" + effectif() + " hommes) pour " + CONTRAT + " jours, à " + GAGE + " cuivre par homme et par jour", () -> {
                contrat = new Contrat();
                contrat.etat = e;
                contrat.fin = j.jourMaintenant() + CONTRAT;
                j.noter("En l'an " + Terre.annee(p, j.jourMaintenant()) + ", ta compagnie se loue à " + royaume(e) + " contre " + royaume(ennemi) + ".");
                j.enregistrer();
                return n.cour("Le maréchal signe ton contrat. « Tes hommes seront payés tant qu'ils se battent. »");
            });
        }
        if (n.noble && !n.fiefs.isEmpty() && vassaux < VASSAUX) s.ajouter("Rallier un noble à ta cause (" + Fiche.argentTexte(PRIX_VASSAL) + ")", () -> rallier(e));
        if (n.noble && !n.fiefs.isEmpty() && vassaux >= VASSAUX && !fatigue()) s.ajouter("Réclamer le trône " + Creation.de(royaume(e)), () -> reclamer(e));
    }

    private Joueur.Scene ost(int e, boolean mercenaire) {
        int ennemi = p.etats.ennemi[e];
        if (ennemi < 0) return j.noblesse().cour("La guerre est finie.");
        Random h = new Random(Travail.melanger(j.fiche().id * 89L + j.jourMaintenant() * 7L + batailles));
        int eux = Math.max(5, Math.min(Math.round(p.etats.soldats[ennemi]), Math.round(effectif() * (0.6f + 0.9f * h.nextFloat()))));
        Issue r = bataille(eux, 22f, royaume(ennemi), h.nextLong());
        p.etats.soldats[ennemi] = Math.max(0f, p.etats.soldats[ennemi] - r.mortsEux);
        if (r.victoire) {
            if (mercenaire) contrat.victoires++;
            else j.noblesse().changerFaveur(e, 10f);
            j.renommee().gagner(j.villeIci(), 10f, 3f, "victoire sur " + royaume(ennemi));
            j.fiche().argent += r.mortsEux * 5;
            String phrase = "En l'an " + Terre.annee(p, j.jourMaintenant()) + ", " + j.fiche().nom + " bat les troupes de " + royaume(ennemi) + ".";
            j.fiche().recit.add(phrase);
            j.noter(phrase);
        }
        j.destin().verifierEtapes();
        j.enregistrer();
        Joueur.Scene s = new Joueur.Scene("La bataille", r.texte + (r.victoire ? " Le butin : " + Fiche.argentTexte(r.mortsEux * 5) + "." : ""));
        s.ajouter("Continuer", () -> j.noblesse().cour(null));
        return s;
    }

    // ================================================================ Mon armée et les sièges

    /** « Mon armée » (Mon personnage). */
    Joueur.Scene accueil() {
        int compagnons = j.bande().taille();
        StringBuilder t = new StringBuilder(majuscule(POSSESSIFS[stade()]) + " : " + effectif() + " hommes sur " + limite() + " — toi, " + (compagnons == 0 ? "aucun compagnon" : compagnons == 1 ? "un compagnon" : compagnons + " compagnons") + ", " + troupes() + ".");
        t.append("\nSolde : ").append(Fiche.argentTexte(soldeJour())).append(" par jour.").append(impayes > 0 ? " Impayés depuis " + impayes + " jours !" : "");
        t.append("\nBatailles : ").append(batailles).append(", victoires : ").append(victoires).append(", places prises : ").append(forts).append('.');
        if (contrat != null) t.append("\nSous contrat avec ").append(royaume(contrat.etat)).append(" (encore ").append(Math.max(0, contrat.fin - j.jourMaintenant())).append(" jours, ").append(contrat.victoires).append(" victoires).");
        for (int e : couronnes) t.append("\nTu règnes sur ").append(royaume(e)).append('.');
        if (stade() < 3) t.append("\n").append(exigences());
        Joueur.Scene s = new Joueur.Scene("Mon armée", t.toString());
        if (!fatigue() && soldats() > 0) for (int c : cibles()) {
            int defenseurs = defenseurs(c);
            s.ajouter("Marcher sur " + ville(c) + " (" + royaume(j.noblesse().etatDe(c)) + ", " + defenseurs + " défenseurs)", () -> siege(c));
        }
        s.ajouter("« Revenir", j::accueilScene);
        return s;
    }

    /** Les royaumes contre qui l'on se bat : l'ennemi de son suzerain, de son employeur, de ses couronnes. */
    private List<Integer> adversaires() {
        List<Integer> l = new ArrayList<>();
        int s = j.noblesse().suzerain;
        if (s >= 0 && p.etats.ennemi[s] >= 0) l.add(p.etats.ennemi[s]);
        if (contrat != null && p.etats.ennemi[contrat.etat] >= 0 && !l.contains(p.etats.ennemi[contrat.etat])) l.add(p.etats.ennemi[contrat.etat]);
        for (int e : couronnes) if (p.etats.ennemi[e] >= 0 && !l.contains(p.etats.ennemi[e])) l.add(p.etats.ennemi[e]);
        return l;
    }

    /** Les villes que l'on peut assiéger : les trois plus petites de chaque adversaire, et sa capitale. */
    private List<Integer> cibles() {
        List<Integer> l = new ArrayList<>();
        for (int e : adversaires()) {
            List<Integer> villes = new ArrayList<>();
            for (int v = 0; v < p.villes.nombre; v++) if (p.villes.estVivante(v) && j.noblesse().etatDe(v) == e && p.etats.capitale[e] != v && j.noblesse().fief(v) == null) villes.add(v);
            villes.sort((a, b) -> Integer.compare(p.villes.habitants[a], p.villes.habitants[b]));
            l.addAll(villes.subList(0, Math.min(3, villes.size())));
            l.add(p.etats.capitale[e]);
        }
        return l;
    }

    private int defenseurs(int v) {
        int e = j.noblesse().etatDe(v);
        int d = Math.max(5, p.villes.habitants[v] / 20);
        if (e >= 0 && p.etats.capitale[e] == v) d += Math.round(p.etats.soldats[e] * 0.5f);
        return d;
    }

    private Joueur.Scene siege(int v) {
        Fiche f = j.fiche();
        int e = j.noblesse().etatDe(v);
        boolean capitale = e >= 0 && p.etats.capitale[e] == v;
        j.passer(16.0);
        Issue r = bataille(defenseurs(v), capitale ? 26f : 20f, ville(v), Travail.melanger(f.id * 61L + v * 7919L + j.jourMaintenant()));
        StringBuilder t = new StringBuilder("Tu mets le siège devant " + ville(v) + ". " + r.texte);
        if (r.victoire) {
            forts++;
            if (capitale) p.etats.soldats[e] = Math.max(0f, p.etats.soldats[e] - r.mortsEux);
            String phrase = "En l'an " + Terre.annee(p, j.jourMaintenant()) + ", " + f.nom + " prend " + ville(v) + ".";
            f.recit.add(phrase);
            j.noter(phrase);
            j.renommee().gagner(v, 20f, -10f, "conquête");
            j.renommee().changer(Renommee.MAISON, j.noblesse().maisonDe(e), -10f);
            f.argent += p.villes.habitants[v] * 2;
            t.append(" La ville ouvre ses portes ; le pillage rapporte ").append(Fiche.argentTexte(p.villes.habitants[v] * 2)).append('.');
            if (capitale && !couronnes.isEmpty() && p.etats.peuple[e] == f.peuple) {
                couronner(e, "par la conquête");
                capitalesPrises++;
                t.append(" Avec sa capitale, tu prends la couronne de ").append(royaume(e)).append(" !");
            } else if (j.noblesse().noble && !capitale) {
                Noblesse.Fief x = new Noblesse.Fief();
                x.ville = v;
                j.noblesse().fiefs.add(x);
                t.append(" Tu la gardes pour fief.");
            } else if (j.noblesse().suzerain >= 0) j.noblesse().changerFaveur(j.noblesse().suzerain, 15f);
        }
        j.destin().verifierEtapes();
        j.enregistrer();
        Joueur.Scene s = new Joueur.Scene("Le siège", t.toString());
        s.ajouter("Continuer", this::accueil);
        return s;
    }

    // ================================================================ La couronne

    private Joueur.Scene rallier(int e) {
        Fiche f = j.fiche();
        if (f.argent < PRIX_VASSAL) return j.noblesse().cour("Il faut " + Fiche.argentTexte(PRIX_VASSAL) + " pour acheter une loyauté.");
        f.argent -= PRIX_VASSAL;
        tentatives++;
        Random h = new Random(Travail.melanger(f.id * 211L + j.jourMaintenant() + tentatives * 7919L));
        float chance = 0.3f + f.competences[Fiche.ELOQUENCE] / 300f + j.renommee().meilleurRenom() / 1000f;
        j.passer(2.0);
        if (h.nextFloat() >= chance) {
            j.enregistrer();
            return j.noblesse().cour("Le seigneur empoche ton or, t'écoute poliment… et ne promet rien.");
        }
        vassaux++;
        String nom = Noms.personne(p.etats.peuple[e], p.graine, h.nextLong(), h.nextInt(2));
        j.noter("En l'an " + Terre.annee(p, j.jourMaintenant()) + ", " + nom + " se rallie à ta cause.");
        j.destin().verifierEtapes();
        j.enregistrer();
        return j.noblesse().cour(nom + " te serre la main dans un couloir sombre : « Le jour venu, mes lances seront avec toi. » (" + vassaux + (vassaux > 1 ? " vassaux)" : " vassal)"));
    }

    private Joueur.Scene reclamer(int e) {
        Fiche f = j.fiche();
        Noblesse n = j.noblesse();
        if (effectif() < HOMMES_TRONE) return n.cour("Il te faut au moins " + HOMMES_TRONE + " hommes pour réclamer une couronne (tu en as " + effectif() + ").");
        reclame = true;
        j.renommee().changer(Renommee.MAISON, j.noblesse().maisonDe(e), -50f);
        boolean sang = n.maisonEpousee >= 0 && n.maisonEpousee == p.etats.maison[e];
        boolean peuple = j.renommee().renom(p.etats.capitale[e]) >= Renommee.PALIERS[4];
        float part = 0.8f - (sang ? 0.3f : 0f) - (peuple ? 0.2f : 0f) - 0.05f * vassaux;
        int eux = Math.max(10, Math.round(p.etats.soldats[e] * Math.max(0.2f, part)));
        String motif = sang ? "par le sang" : peuple ? "par la voix du peuple" : "par l'épée";
        Issue r = bataille(eux, 26f, royaume(e), Travail.melanger(f.id * 997L + e + j.jourMaintenant()));
        StringBuilder t = new StringBuilder("Tu réclames le trône " + Creation.de(royaume(e)) + " " + motif + ". " + (sang ? "Une partie de la garde reconnaît ton droit. " : "") + (peuple ? "Le peuple de la capitale est avec toi. " : "") + r.texte);
        if (r.victoire) {
            p.etats.soldats[e] = Math.max(0f, p.etats.soldats[e] - r.mortsEux);
            if (n.suzerain == e) n.suzerain = -1;
            couronner(e, motif);
            t.append(" L'ancien souverain s'enfuit. On pose la couronne sur ta tête.");
        } else {
            n.changerFaveur(e, -80f);
            if (n.suzerain == e) n.suzerain = -1;
            n.fiefs.clear();
            t.append(" Ta rébellion est écrasée : la couronne te prend tes fiefs.");
        }
        j.destin().verifierEtapes();
        j.enregistrer();
        Joueur.Scene s = new Joueur.Scene("La couronne", t.toString());
        s.ajouter("Continuer", j::fermerScene);
        return s;
    }

    void couronner(int e, String motif) {
        Fiche f = j.fiche();
        if (!couronnes.contains(e)) couronnes.add(e);
        p.etats.chef[e] = f.id;
        p.etats.debutRegne[e] = (short) Terre.annee(p, j.jourMaintenant());
        p.etats.legitimite[e] = Math.max(p.etats.legitimite[e], 50f);
        j.noblesse().noble = true;
        String phrase = "En l'an " + Terre.annee(p, j.jourMaintenant()) + ", " + f.nom + " est couronné" + f.e() + " " + Creation.de(royaume(e)) + ", " + motif + ".";
        f.recit.add(phrase);
        j.noter(phrase);
        j.renommee().gagner(p.etats.capitale[e], 200f, 10f, "couronnement");
        j.renommee().changer(Renommee.ROYAUME, e, 30f);
        j.annoncer("Tu es couronné" + f.e() + " !");
        j.social().grandMoment("couronné" + f.e());
    }

    /** Roi ou reine, à sa propre cour. */
    private void couronneChoix(Joueur.Scene s, int e) {
        int ennemi = p.etats.ennemi[e];
        if (ennemi >= 0) {
            if (!fatigue()) s.ajouter("Mener l'ost contre " + royaume(ennemi), () -> ost(e, false));
            s.ajouter("Faire la paix avec " + royaume(ennemi), () -> {
                p.etats.ennemi[ennemi] = -1;
                p.etats.ennemi[e] = -1;
                j.noter("En l'an " + Terre.annee(p, j.jourMaintenant()) + ", " + royaume(e) + " fait la paix avec " + royaume(ennemi) + ".");
                j.enregistrer();
                return j.noblesse().cour("Les hérauts proclament la paix.");
            });
            return;
        }
        int montrees = 0;
        for (int x = 0; x < p.etats.nombre && montrees < 6; x++) {
            if (x == e || !p.etats.estVivant(x) || couronnes.contains(x) || p.etats.ennemi[x] >= 0 || p.etats.peuple[x] != p.etats.peuple[e]) continue;
            int cible = x;
            montrees++;
            s.ajouter("Déclarer la guerre à " + royaume(x) + " (" + Math.round(p.etats.soldats[x]) + " soldats)", () -> {
                p.etats.ennemi[e] = cible;
                p.etats.ennemi[cible] = e;
                j.noter("En l'an " + Terre.annee(p, j.jourMaintenant()) + ", " + royaume(e) + " déclare la guerre à " + royaume(cible) + ".");
                j.enregistrer();
                return j.noblesse().cour("Les hérauts partent porter le défi à " + royaume(cible) + ".");
            });
        }
    }

    /** Pour « Unifier son peuple » : les capitales de son peuple, et combien l'on en tient. */
    public int[] capitalesDuPeuple() {
        int q = j.fiche().peuple, total = 0, tenues = 0;
        for (int e = 0; e < p.etats.nombre; e++) {
            if (!p.etats.estVivant(e) || p.etats.peuple[e] != q) continue;
            total++;
            if (couronnes.contains(e)) tenues++;
        }
        return new int[] { tenues, total };
    }

    // ================================================================ Pour les essais

    public Joueur.Scene caserneEssai() {
        return caserne(null);
    }

    public Joueur.Scene accueilEssai() {
        return accueil();
    }

    // ================================================================ Sauvegarde

    void ecrire(DataOutputStream s) throws IOException {
        for (int x : soldats) s.writeInt(x);
        s.writeInt(impayes);
        s.writeInt(victoires);
        s.writeInt(batailles);
        s.writeInt(forts);
        s.writeInt(vassaux);
        s.writeInt(contrats);
        s.writeInt(capitalesPrises);
        s.writeInt(tentatives);
        s.writeBoolean(grandeVictoire);
        s.writeBoolean(reclame);
        s.writeBoolean(contrat != null);
        if (contrat != null) {
            s.writeInt(contrat.etat);
            s.writeInt(contrat.fin);
            s.writeInt(contrat.victoires);
        }
        s.writeInt(couronnes.size());
        for (int e : couronnes) s.writeInt(e);
        s.writeInt(recrues.size());
        for (Map.Entry<Integer, int[]> e : recrues.entrySet()) {
            s.writeInt(e.getKey());
            s.writeInt(e.getValue()[0]);
            s.writeInt(e.getValue()[1]);
        }
        s.writeInt(dernierJour);
        s.writeInt(derniereBataille);
    }

    void lire(DataInputStream s) throws IOException {
        for (int k = 0; k < TYPES; k++) soldats[k] = s.readInt();
        impayes = s.readInt();
        victoires = s.readInt();
        batailles = s.readInt();
        forts = s.readInt();
        vassaux = s.readInt();
        contrats = s.readInt();
        capitalesPrises = s.readInt();
        tentatives = s.readInt();
        grandeVictoire = s.readBoolean();
        reclame = s.readBoolean();
        if (s.readBoolean()) {
            contrat = new Contrat();
            contrat.etat = s.readInt();
            contrat.fin = s.readInt();
            contrat.victoires = s.readInt();
        }
        int n = s.readInt();
        for (int k = 0; k < n; k++) couronnes.add(s.readInt());
        n = s.readInt();
        for (int k = 0; k < n; k++) recrues.put(s.readInt(), new int[] { s.readInt(), s.readInt() });
        dernierJour = s.readInt();
        derniereBataille = s.readInt();
    }
}
