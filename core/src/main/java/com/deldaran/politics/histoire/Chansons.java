package com.deldaran.politics.histoire;

import com.deldaran.politics.Metiers;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Lot G2 : les chansons du joueur.
 * - une chanson SUR TOI naît quand un barde est payé pour chanter tes exploits, ou d'elle-même quand tu es connu dans le comté
 *   (renom 150) et que tu as des exploits ;
 * - une chanson DE TOI naît quand tu composes à la taverne ;
 * - chaque chanson voyage de ville en ville le long des routes ouvertes ; on la trouve sur l'étagère « Livres et chansons »
 *   des villes où elle est chantée, à côté des œuvres du monde ;
 * - on APPREND les chansons qu'on écoute (celles du monde comme les tiennes), on CHANTE en taverne, on COMPOSE ;
 * - le Destin mesure : apprendre dix chansons, jouer dans cinq tavernes, composer une chanson qui te survivra (chantée dans trois
 *   villes), qu'une chanson soit écrite sur toi.
 */
public final class Chansons {
    /** Une chanson naît d'elle-même sur toi dans une ville où ton renom atteint RENOM_CHANSON. */
    public static final float RENOM_CHANSON = 150f;
    /** Chance, par jour et par chanson, qu'elle gagne une ville voisine (× 2 tant qu'un barde payé la chante). */
    public static final float DIFFUSION = 0.08f;
    public static final int VILLES_MAX = 24;
    /** Pour composer : Éloquence et chansons connues ; une composition par saison. */
    public static final int ELOQUENCE_COMPOSER = 40, CONNUES_COMPOSER = 5;
    /** Une chanson « te survit » quand elle est chantée dans VILLES_SURVIE villes. */
    public static final int VILLES_SURVIE = 3;
    /** Le barde payé fait courir la chanson BARDE_JOURS jours. */
    public static final int BARDE_JOURS = 30;

    public static final class Chanson {
        public String titre = "", texte = "";
        /** De qui elle parle (id de fiche), ou 0 ; composée par le joueur ? */
        public long sujet;
        public boolean deToi;
        public int nee, barde = -1;
        public final List<Integer> villes = new ArrayList<>();
    }

    private final Joueur j;
    private final Population p;
    public final List<Chanson> liste = new ArrayList<>();
    /** Les chansons apprises : les œuvres du monde (id ≥ 0) et les nôtres (−1 − rang). */
    public final Set<Integer> apprises = new LinkedHashSet<>();
    /** Les villes où l'on a chanté en taverne. */
    public final Set<Integer> tavernes = new LinkedHashSet<>();
    private final Map<Integer, Integer> chante = new HashMap<>();
    private int dernierJour = -1, composeSaison = -1;

    Chansons(Joueur j, Population p) {
        this.j = j;
        this.p = p;
    }

    private String ville(int v) {
        return Noms.ville(p.villes.peuple[v], p.graine, v);
    }

    // ================================================================ Le temps

    void avancer() {
        Fiche f = j.fiche();
        if (f == null) return;
        int jour = j.jourMaintenant();
        if (dernierJour < 0) dernierJour = jour;
        for (int d = Math.max(dernierJour, jour - 60) + 1; d <= jour; d++) journee(d);
        dernierJour = Math.max(dernierJour, jour);
    }

    void sauter() {
        dernierJour = j.jourMaintenant();
    }

    /** L'héritier garde les chansons (elles parlent « des siens ») mais ne sait pas encore chanter celles de son parent. */
    void heriter() {
        apprises.clear();
        tavernes.clear();
        chante.clear();
        composeSaison = -1;
    }

    private void journee(int d) {
        for (int k = 0; k < liste.size(); k++) {
            Chanson c = liste.get(k);
            if (c.villes.isEmpty() || c.villes.size() >= VILLES_MAX) continue;
            Random h = new Random(Travail.melanger(p.graine * 31L + d * 7919L + k * 104729L));
            float chance = DIFFUSION * (c.barde >= d ? 2f : 1f);
            if (h.nextFloat() >= chance) continue;
            int de = c.villes.get(h.nextInt(c.villes.size()));
            int vers = voisine(de, h);
            if (vers < 0 || c.villes.contains(vers)) continue;
            c.villes.add(vers);
            if (c.deToi && c.villes.size() == VILLES_SURVIE) j.annoncer("On chante maintenant « " + c.titre + " » dans " + VILLES_SURVIE + " villes : ta chanson vit sans toi.");
        }
        spontanee();
        j.destin().verifierEtapes();
    }

    private int voisine(int v, Random h) {
        Routes r = p.routes;
        List<Integer> l = new ArrayList<>();
        for (int k = 0; k < r.nombre; k++) {
            if (r.fin[k] != Routes.OUVERTE) continue;
            int autre = r.villeA[k] == v ? r.villeB[k] : r.villeB[k] == v ? r.villeA[k] : -1;
            if (autre >= 0 && p.villes.estVivante(autre)) l.add(autre);
        }
        return l.isEmpty() ? -1 : l.get(h.nextInt(l.size()));
    }

    // ================================================================ Les chansons sur toi

    public boolean surToi() {
        Fiche f = j.fiche();
        if (f == null) return false;
        for (Chanson c : liste) if (c.sujet == f.id) return true;
        return false;
    }

    private boolean surToiA(int v) {
        Fiche f = j.fiche();
        for (Chanson c : liste) if (c.sujet == f.id && c.villes.contains(v)) return true;
        return false;
    }

    /** Une ville où l'on est connu dans le comté fait d'elle-même une chanson sur toi. */
    private void spontanee() {
        Fiche f = j.fiche();
        int v = j.villeIci() >= 0 ? j.villeIci() : f.ville;
        if (v < 0 || j.renommee().renom(v) < RENOM_CHANSON || surToiA(v)) return;
        List<String> ex = j.social().exploits();
        if (ex.size() < 2) return;
        String nom = j.renommee().surnomEn(v).isEmpty() ? f.nom : j.renommee().surnomEn(v);
        Chanson c = nouvelle("La geste " + Creation.de(nom), couplets(ex), f.id, false, v);
        j.annoncer("À " + ville(v) + ", quelqu'un a fait une chanson sur toi : « " + c.titre + " ».");
        j.noter("En l'an " + Terre.annee(p, j.jourMaintenant()) + ", on chante à " + ville(v) + " « " + c.titre + " ».");
    }

    /** Le barde payé (Social) : la chanson de tes exploits part sur les routes. */
    void barde(String nom, int v, List<String> ex) {
        Fiche f = j.fiche();
        Chanson c = null;
        for (Chanson x : liste) if (x.sujet == f.id && x.villes.contains(v)) c = x;
        if (c == null) c = nouvelle("La chanson " + Creation.de(nom), couplets(ex), f.id, false, v);
        else c.texte = couplets(ex);
        c.barde = j.jourMaintenant() + BARDE_JOURS;
        j.destin().verifierEtapes();
    }

    private static String couplets(List<String> ex) {
        StringBuilder t = new StringBuilder();
        for (String e : ex) {
            String x = e.replaceAll("\\.$", "");
            if (t.length() > 0) t.append('\n');
            t.append("« ").append(Character.toUpperCase(x.charAt(0))).append(x.substring(1)).append("… »");
        }
        t.append("\n(et la salle reprend le refrain)");
        return t.toString();
    }

    private Chanson nouvelle(String titre, String texte, long sujet, boolean deToi, int v) {
        Chanson c = new Chanson();
        c.titre = titre;
        c.texte = texte;
        c.sujet = sujet;
        c.deToi = deToi;
        c.nee = j.jourMaintenant();
        c.villes.add(v);
        liste.add(c);
        apprises.add(-1 - (liste.size() - 1));
        j.enregistrer();
        return c;
    }

    // ================================================================ L'étagère

    public int connues() {
        return apprises.size();
    }

    /** Une œuvre du monde écoutée (une chanson, pas un livre) : on l'apprend. */
    void entendre(int oeuvre) {
        if (apprises.add(oeuvre)) {
            if (apprises.size() == 10) j.annoncer("Tu connais maintenant dix chansons par cœur.");
            j.destin().verifierEtapes();
        }
    }

    /** Les chansons du joueur chantées dans la ville. */
    int ici(int v) {
        int n = 0;
        for (Chanson c : liste) if (c.villes.contains(v)) n++;
        return n;
    }

    void choixEtagere(Joueur.Scene s, int v, int debut) {
        if (v < 0) return;
        for (int k = 0; k < liste.size(); k++) {
            Chanson c = liste.get(k);
            if (!c.villes.contains(v)) continue;
            int rang = k;
            s.ajouter("Chanson : « " + c.titre + " »" + (c.deToi ? " (de toi)" : " (sur " + (c.sujet == j.fiche().id ? "toi" : "les tiens") + ")"), () -> ecouter(rang, debut));
        }
    }

    private Joueur.Scene ecouter(int rang, int debut) {
        Chanson c = liste.get(rang);
        apprises.add(-1 - rang);
        j.destin().verifierEtapes();
        String qui = c.deToi ? "Tu l'as composée toi-même. On la chante dans " + c.villes.size() + (c.villes.size() > 1 ? " villes." : " ville.")
                : "On la chante dans " + c.villes.size() + (c.villes.size() > 1 ? " villes." : " ville.");
        Joueur.Scene s = new Joueur.Scene(c.titre, c.texte + "\n\n" + qui);
        s.ajouter("Reposer", () -> j.etagere(debut));
        return s;
    }

    // ================================================================ La taverne

    void choixBoutique(Joueur.Scene s, int lieu) {
        if (lieu != Metiers.TAVERNE && lieu != Metiers.AUBERGE) return;
        int v = j.villeIci();
        Fiche f = j.fiche();
        if (v < 0 || f == null) return;
        Integer jour = chante.get(v);
        if (connues() >= 1 && (jour == null || jour != j.jourMaintenant())) s.ajouter("Chanter pour la salle (1 h)", () -> chanter(lieu, v));
        int saison = Math.floorDiv(j.jourMaintenant(), Terre.JOURS_PAR_SAISON);
        if (composeSaison != saison && connues() >= CONNUES_COMPOSER && f.competences[Fiche.ELOQUENCE] >= ELOQUENCE_COMPOSER)
            s.ajouter("Composer une chanson (une soirée)", () -> composer(lieu, v));
    }

    private Joueur.Scene chanter(int lieu, int v) {
        Fiche f = j.fiche();
        chante.put(v, j.jourMaintenant());
        boolean nouvelle = tavernes.add(v);
        int elo = f.competences[Fiche.ELOQUENCE];
        int gain = Math.min(60, 2 + elo / 8 + p.villes.habitants[v] / 400);
        f.argent += gain;
        EconomieVille ev = j.marcheVille();
        if (ev != null) ev.bourse -= gain;
        f.apprendre(Fiche.ELOQUENCE, 3f);
        j.renommee().gagner(v, elo >= 50 ? 2f : 1f, 0.5f, null);
        j.destin().moral = Math.min(100f, j.destin().moral + 3f);
        j.gesteJoueur = "saluer_main";
        j.passer(1.0);
        j.destin().verifierEtapes();
        j.enregistrer();
        String salle = elo < 20 ? "Quelques rires polis, un ivrogne qui chante faux avec toi." : elo < 50 ? "La salle tape du pied et reprend le refrain." : "On te réclame encore, et la tavernière t'offre à boire.";
        return j.boutiqueScene(lieu, "Tu chantes ce que tu sais. " + salle + " Le chapeau revient avec " + Fiche.argentTexte(gain) + "." + (nouvelle && tavernes.size() > 1 ? " (" + tavernes.size() + " tavernes où tu as chanté)" : ""));
    }

    private static final String[][] THEMES = {
        { "La ballade %s", "Sur la route de %s, la pluie m'a pris la main", "J'ai laissé là-bas une porte et un chemin", "Qui m'attend encore au bout du matin" },
        { "Le chant des remparts %s", "Sonnez, les cors de %s, les lances sont levées", "Nos pères ont tenu ici, nos fils tiendront après", "Et la pierre se souvient des noms qu'on a gravés" },
        { "La complainte %s", "À %s le blé est mûr et personne pour le faucher", "Les hommes sont partis, les femmes ont chanté", "Et le vent dans les seigles a tout emporté" },
        { "La chanson à boire %s", "À %s on boit sec, à %s on boit bien", "Le tavernier nous jure qu'il ne mouille pas son vin", "Et l'on rentre en chantant quand se lève le matin" },
    };

    private Joueur.Scene composer(int lieu, int v) {
        Fiche f = j.fiche();
        composeSaison = Math.floorDiv(j.jourMaintenant(), Terre.JOURS_PAR_SAISON);
        Random h = new Random(Travail.melanger(f.id * 13L + j.jourMaintenant()));
        String[] t = THEMES[h.nextInt(THEMES.length)];
        String nom = ville(v);
        String titre = String.format(t[0], Creation.de(nom));
        StringBuilder x = new StringBuilder();
        for (int k = 1; k < t.length; k++) x.append(k > 1 ? "\n" : "").append("« ").append(t[k].replace("%s", nom)).append(" »");
        boolean belle = f.competences[Fiche.ELOQUENCE] + h.nextInt(40) >= 70;
        Chanson c = nouvelle(titre, x.toString(), 0L, true, v);
        if (belle) {
            // Une belle chanson part d'elle-même vers une ville voisine.
            int w = voisine(v, h);
            if (w >= 0) c.villes.add(w);
        }
        f.apprendre(Fiche.ELOQUENCE, 10f);
        j.renommee().gagner(v, 3f, 1f, null);
        j.passer(4.0);
        String phrase = "En l'an " + Terre.annee(p, j.jourMaintenant()) + ", " + f.nom + " compose « " + titre + " ».";
        f.recit.add(phrase);
        j.noter(phrase);
        j.destin().verifierEtapes();
        j.enregistrer();
        return j.boutiqueScene(lieu, "Tu passes la soirée à chercher tes rimes au coin du feu, puis tu la chantes :\n" + x + "\n"
                + (belle ? "La salle la reprend aussitôt ; un colporteur la note pour la chanter plus loin." : "On applaudit poliment. Il faudra qu'on la chante ailleurs pour qu'elle vive."));
    }

    /** Pour le Destin : la plus répandue de tes chansons. */
    public int meilleureDeToi() {
        int n = 0;
        for (Chanson c : liste) if (c.deToi) n = Math.max(n, c.villes.size());
        return n;
    }

    // ================================================================ Pour les essais

    public Joueur.Scene choixPourEssai(int lieu) {
        Joueur.Scene s = new Joueur.Scene("Essai", "");
        choixBoutique(s, lieu);
        return s;
    }

    public int jourPourEssai() {
        return j.jourMaintenant();
    }

    public void noterPourEssai(String phrase) {
        j.noter(phrase);
    }

    public List<Missions.Mission> missionsPourEssai() {
        return j.missions().liste;
    }

    public void entendrePourEssai(int oeuvre) {
        entendre(oeuvre);
    }

    public void journeePourEssai(int d) {
        journee(d);
    }

    /** La faveur d'un royaume change (et la Maison qui règne s'en souvient) ; renvoie la réputation auprès de cette Maison. */
    public float maisonPourEssai(int e, float faveur) {
        j.noblesse().changerFaveur(e, faveur);
        return j.renommee().groupe(Renommee.MAISON, j.noblesse().maisonDe(e));
    }

    public Joueur.Scene renomPourEssai() {
        Joueur.Scene s = new Joueur.Scene("Groupes", j.renommee().groupesTexte(j.villeIci()));
        return s;
    }

    // ================================================================ Sauvegarde

    void ecrire(DataOutputStream s) throws IOException {
        s.writeInt(dernierJour);
        s.writeInt(composeSaison);
        s.writeInt(liste.size());
        for (Chanson c : liste) {
            s.writeUTF(c.titre);
            s.writeUTF(c.texte);
            s.writeLong(c.sujet);
            s.writeBoolean(c.deToi);
            s.writeInt(c.nee);
            s.writeInt(c.barde);
            s.writeInt(c.villes.size());
            for (int v : c.villes) s.writeInt(v);
        }
        s.writeInt(apprises.size());
        for (int a : apprises) s.writeInt(a);
        s.writeInt(tavernes.size());
        for (int v : tavernes) s.writeInt(v);
        s.writeInt(chante.size());
        for (Map.Entry<Integer, Integer> e : chante.entrySet()) {
            s.writeInt(e.getKey());
            s.writeInt(e.getValue());
        }
    }

    void lire(DataInputStream s) throws IOException {
        dernierJour = s.readInt();
        composeSaison = s.readInt();
        int n = s.readInt();
        for (int k = 0; k < n; k++) {
            Chanson c = new Chanson();
            c.titre = s.readUTF();
            c.texte = s.readUTF();
            c.sujet = s.readLong();
            c.deToi = s.readBoolean();
            c.nee = s.readInt();
            c.barde = s.readInt();
            int m = s.readInt();
            for (int i = 0; i < m; i++) c.villes.add(s.readInt());
            liste.add(c);
        }
        n = s.readInt();
        for (int k = 0; k < n; k++) apprises.add(s.readInt());
        n = s.readInt();
        for (int k = 0; k < n; k++) tavernes.add(s.readInt());
        n = s.readInt();
        for (int k = 0; k < n; k++) chante.put(s.readInt(), s.readInt());
    }
}
