package com.deldaran.politics.histoire;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Phase 6 : ce que les gens savent de toi, ville par ville.
 *
 * - Le RENOM (la célébrité) est local : un exploit est connu tout de suite dans la ville où il a lieu, puis il voyage
 *   le long des routes ouvertes, chaque jour un peu, plus vite sur les grandes routes, moins loin sur les longues. Il s'efface lentement.
 * - La RÉPUTATION (de −100 à 100) dit si l'on t'apprécie : missions tenues, travail, vol… Elle voyage aussi, plus lentement.
 * - Les ÉTIQUETTES comptent les actes marquants ; elles donnent un SURNOM que les villageois emploient.
 */
public final class Renommee {
    public static final int[] PALIERS = { 0, 10, 50, 150, 400, 1000 };
    public static final String[] NOMS_PALIERS = { "inconnu", "connu du quartier", "connu en ville", "connu dans le comté", "célèbre dans le royaume", "légendaire" };
    /** Part de l'écart de renom qui passe chaque jour d'une ville à sa voisine, par niveau de route (sentier 1 … voie pavée 4). */
    public static final float DIFFUSION = 0.012f;
    /** Au-delà de cette longueur (km), une route transmet moins bien : part ÷ (1 + km ÷ PORTEE_KM). */
    public static final float PORTEE_KM = 40f;
    /** Chaque jour, le renom garde OUBLI de sa valeur, la réputation OUBLI_REPUTATION. */
    public static final float OUBLI = 0.998f, OUBLI_REPUTATION = 0.999f;

    public static final int FIABLE = 0, MANQUEES = 1, LETTRES = 2, VOLEUR = 3, JOURS = 4, MAITRE = 5, BRIGANDS = 6, ETIQUETTES = 7;

    private final Joueur j;
    private final Population p;
    /** ville → { renom, réputation }. Lu aussi par l'écran (fil de rendu) : table concurrente. */
    private final Map<Integer, float[]> villes = new ConcurrentHashMap<>();
    public final int[] etiquettes = new int[ETIQUETTES];
    /** Métier dans lequel on est passé maître (pour le surnom), -1 sinon. */
    public int metierMaitre = -1;
    private int dernierJour = -1;
    private boolean initialise;

    /** Lot F : la réputation auprès des groupes (de −100 à 100) : le royaume d'une ville, son peuple, la guilde d'un métier, les brigands. */
    public static final int ROYAUME = 0, PEUPLE = 1, GUILDE = 2, BRIGANDS_G = 3;
    /** Lot G2 : la réputation auprès d'une Maison noble (id de Maisons). */
    public static final int MAISON = 4;
    private final Map<Long, float[]> groupes = new ConcurrentHashMap<>();
    /** Les étiquettes ville par ville : chaque ville a son surnom pour toi. */
    private final Map<Integer, int[]> etiquettesVille = new ConcurrentHashMap<>();
    /** Les bardes payés : ville → dernier jour où ils chantent ; le renom en part deux fois plus vite. */
    private final Map<Integer, Integer> bardes = new ConcurrentHashMap<>();

    private static long cle(int type, int id) {
        return ((long) type << 32) | (id & 0xffffffffL);
    }

    public float groupe(int type, int id) {
        float[] g = groupes.get(cle(type, id));
        return g == null ? 0f : g[0];
    }

    void changer(int type, int id, float d) {
        if (id < 0 || d == 0f) return;
        float[] g = groupes.computeIfAbsent(cle(type, id), k -> new float[1]);
        g[0] = Math.max(-100f, Math.min(100f, g[0] + d));
    }

    public int royaumeDe(int v) {
        if (v < 0) return -1;
        int r = p.villes.region[v];
        return r >= 0 && r < p.etats.regionEtat.length ? p.etats.regionEtat[r] : -1;
    }

    /** Ce que pense de toi un habitant qui ne te connaît pas : la réputation dans la ville, un peu celle du royaume et du peuple. */
    public float reputationAupres(int v) {
        if (v < 0) return 0f;
        return Math.max(-100f, Math.min(100f, reputation(v) * 0.7f + 0.15f * (groupe(ROYAUME, royaumeDe(v)) + groupe(PEUPLE, p.villes.peuple[v]))));
    }

    void guilde(int metier, float d) {
        changer(GUILDE, metier, d);
    }

    /** La guilde qui t'apprécie te fait un prix sur la licence (jusqu'à moitié prix). */
    public int licence(int metier, int base) {
        return Math.max(0, Math.round(base * (1f - Math.max(0f, groupe(GUILDE, metier)) / 200f)));
    }

    /** Les brigands qui te respectent (tu as volé, fait de la contrebande) t'attaquent moins ; ceux que tu as décimés, plus. */
    public float facteurBrigands() {
        float b = groupe(BRIGANDS_G, 0);
        return b >= 30f ? 0.5f : b <= -30f ? 1.3f : 1f;
    }

    void barde(int v, int fin) {
        bardes.put(v, fin);
    }

    /** Des voyageurs (marchands, caravanes) partis de « de » racontent ce qu'on dit de toi à « vers ». */
    void porter(int de, int vers, float part) {
        float[] a = villes.get(de);
        if (a == null || de == vers || vers < 0) return;
        float[] b = entree(vers);
        if (a[0] > b[0]) b[0] += (a[0] - b[0]) * part;
    }

    /** « Ce qu'on dit de moi » : les groupes. */
    String groupesTexte(int v) {
        StringBuilder t = new StringBuilder();
        int e = royaumeDe(v);
        if (e >= 0) t.append("\n  Le royaume de ").append(Noms.ville(p.etats.peuple[e], p.graine, p.etats.capitale[e])).append(" : ").append(reputationTexte(groupe(ROYAUME, e)));
        if (v >= 0) t.append("\n  Le peuple de cette ville : ").append(reputationTexte(groupe(PEUPLE, p.villes.peuple[v])));
        for (int m = 0; m < com.deldaran.politics.Metiers.NOMBRE; m++) {
            float g = groupe(GUILDE, m);
            if (Math.abs(g) >= 1f) t.append("\n  La guilde (").append(com.deldaran.politics.Metiers.NOMS[m]).append(") : ").append(reputationTexte(g)).append(g > 0f ? ", licence à " + Math.round(100f - g / 2f) + " %" : "");
        }
        for (Map.Entry<Long, float[]> x : groupes.entrySet()) {
            if ((int) (x.getKey() >>> 32) != MAISON || Math.abs(x.getValue()[0]) < 1f) continue;
            String m = Recit.nomMaison(p, (int) (long) x.getKey());
            t.append("\n  ").append(Character.toUpperCase(m.charAt(0))).append(m.substring(1)).append(" : ").append(reputationTexte(x.getValue()[0]));
        }
        float b = groupe(BRIGANDS_G, 0);
        t.append("\n  Les brigands : ").append(b >= 30f ? "ils te respectent et t'attaquent moins" : b <= -30f ? "ils te haïssent et te cherchent" : "ils ne te connaissent pas");
        return t.toString();
    }

    void ecrireSuite(DataOutputStream s) throws IOException {
        s.writeInt(groupes.size());
        for (Map.Entry<Long, float[]> e : groupes.entrySet()) {
            s.writeLong(e.getKey());
            s.writeFloat(e.getValue()[0]);
        }
        s.writeInt(etiquettesVille.size());
        for (Map.Entry<Integer, int[]> e : etiquettesVille.entrySet()) {
            s.writeInt(e.getKey());
            for (int x : e.getValue()) s.writeInt(x);
        }
        s.writeInt(bardes.size());
        for (Map.Entry<Integer, Integer> e : bardes.entrySet()) {
            s.writeInt(e.getKey());
            s.writeInt(e.getValue());
        }
    }

    void lireSuite(DataInputStream s) throws IOException {
        int n = s.readInt();
        for (int k = 0; k < n; k++) groupes.put(s.readLong(), new float[] { s.readFloat() });
        n = s.readInt();
        for (int k = 0; k < n; k++) {
            int v = s.readInt();
            int[] et = new int[ETIQUETTES];
            for (int q = 0; q < ETIQUETTES; q++) et[q] = s.readInt();
            etiquettesVille.put(v, et);
        }
        n = s.readInt();
        for (int k = 0; k < n; k++) bardes.put(s.readInt(), s.readInt());
    }

    Renommee(Joueur j, Population p) {
        this.j = j;
        this.p = p;
    }

    /** Phase 11 : l'héritier porte le nom de la famille. Il garde la moitié du renom et la réputation des groupes ; ses actes restent à faire. */
    void heriter() {
        for (float[] r : villes.values()) {
            r[0] *= 0.5f;
            r[1] *= 0.5f;
        }
        for (float[] g : groupes.values()) g[0] *= 0.5f;
        java.util.Arrays.fill(etiquettes, 0);
        etiquettesVille.clear();
    }

    private void initialiser() {
        if (initialise) return;
        initialise = true;
        Fiche f = j.fiche();
        if (f != null && f.ville >= 0 && !villes.containsKey(f.ville)) villes.put(f.ville, new float[] { f.renom, 10f });
    }

    private float[] entree(int v) {
        return villes.computeIfAbsent(v, k -> new float[2]);
    }

    public float renom(int v) {
        float[] r = villes.get(v);
        return r == null ? 0f : r[0];
    }

    public float reputation(int v) {
        float[] r = villes.get(v);
        return r == null ? 0f : r[1];
    }

    public static int palier(float renom) {
        int k = 0;
        while (k + 1 < PALIERS.length && renom >= PALIERS[k + 1]) k++;
        return k;
    }

    public int palierEn(int v) {
        return v < 0 ? 0 : palier(renom(v));
    }

    public static String reputationTexte(float r) {
        if (r >= 60f) return "très appréciée";
        if (r >= 25f) return "appréciée";
        if (r > -25f) return "sans histoire";
        if (r > -60f) return "mauvaise";
        return "détestable";
    }

    /** Un acte : du renom et de la réputation dans la ville où il a eu lieu. Le renom total de la fiche augmente aussi. */
    void gagner(int v, float renom, float reputation, String pourquoi) {
        initialiser();
        if (v < 0) return;
        float[] e = entree(v);
        int avant = palier(e[0]);
        e[0] = Math.max(0f, e[0] + renom);
        e[1] = Math.max(-100f, Math.min(100f, e[1] + reputation));
        changer(ROYAUME, royaumeDe(v), reputation * 0.3f);
        changer(PEUPLE, p.villes.peuple[v], reputation * 0.2f);
        Fiche f = j.fiche();
        if (f != null && renom > 0f) f.renom += Math.round(renom);
        String ville = Noms.ville(p.villes.peuple[v], p.graine, v);
        if (renom >= 1f && pourquoi != null) j.annoncer(pourquoi + " : on parle de toi à " + ville + " (renom +" + Math.round(renom) + ").");
        int apres = palier(e[0]);
        if (apres > avant) j.annoncer("Te voilà " + NOMS_PALIERS[apres] + (f != null ? f.e() : "") + " à " + ville + " !");
        if (apres > avant && apres >= 2 && j.social() != null) j.social().grandMoment(NOMS_PALIERS[apres] + (f != null ? f.e() : "") + " à " + ville);
    }

    void marquer(int etiquette) {
        etiquettes[etiquette]++;
        int v = j.villeIci() >= 0 ? j.villeIci() : j.fiche() != null ? j.fiche().ville : -1;
        if (v >= 0) etiquettesVille.computeIfAbsent(v, k -> new int[ETIQUETTES])[etiquette]++;
        if (etiquette == VOLEUR) {
            changer(BRIGANDS_G, 0, 3f);
            changer(ROYAUME, royaumeDe(v), -3f);
        }
        if (etiquette == BRIGANDS) {
            changer(BRIGANDS_G, 0, -6f);
            changer(ROYAUME, royaumeDe(v), 2f);
        }
    }

    /** Le surnom qu'on te donne dans cette ville : celui de tes actes ici (il en faut deux fois moins), sinon ta renommée, si elle est arrivée jusque-là. */
    public String surnomEn(int v) {
        if (v < 0) return surnom();
        int[] et = etiquettesVille.get(v);
        String s = et == null ? "" : surnom(et, true);
        if (!s.isEmpty()) return s;
        return palierEn(v) >= 2 ? surnom() : "";
    }

    /** Le surnom, d'après l'acte le plus marquant ; vide s'il n'y en a pas encore. */
    public String surnom() {
        return surnom(etiquettes, false);
    }

    private String surnom(int[] etiquettes, boolean ville) {
        Fiche f = j.fiche();
        boolean femme = f != null && f.femme();
        int d = ville ? 2 : 1;
        if (etiquettes[VOLEUR] > 0) return "Main-Leste";
        if (metierMaitre >= 0 && (!ville || etiquettes[MAITRE] > 0)) {
            String r = Missions.role(metierMaitre);
            if (r.startsWith("le ")) r = r.substring(3);
            return (femme ? "Maîtresse " : "Maître ") + r;
        }
        if (etiquettes[BRIGANDS] >= (3 + d - 1) / d) return "Fléau-des-Brigands";
        if (etiquettes[LETTRES] >= (3 + d - 1) / d) return femme ? "la Messagère" : "le Messager";
        if (etiquettes[FIABLE] >= 8 / d) return "Rend-Service";
        if (etiquettes[MANQUEES] >= (3 + d - 1) / d) return "Promet-Tout";
        if (etiquettes[JOURS] >= 40 / d) return "l'Infatigable";
        return "";
    }

    /** Royaumes où l'on est au moins « connu en ville » quelque part. */
    public int royaumesOuConnu() {
        Set<Integer> etats = new HashSet<>();
        for (Map.Entry<Integer, float[]> e : villes.entrySet()) {
            if (e.getValue()[0] < PALIERS[2]) continue;
            int r = p.villes.region[e.getKey()];
            int etat = r >= 0 && r < p.etats.regionEtat.length ? p.etats.regionEtat[r] : -1;
            if (etat >= 0) etats.add(etat);
        }
        return etats.size();
    }

    public float meilleurRenom() {
        initialiser();
        float m = 0f;
        for (float[] r : villes.values()) m = Math.max(m, r[0]);
        return m;
    }

    public float meilleureReputation() {
        initialiser();
        float m = -100f;
        for (float[] r : villes.values()) m = Math.max(m, r[1]);
        return villes.isEmpty() ? 0f : m;
    }

    /** Fait voyager le renom et la réputation le long des routes, jour après jour (60 jours au plus d'un coup). */
    void avancer(int jour) {
        initialiser();
        if (dernierJour < 0) dernierJour = jour;
        int debut = Math.max(dernierJour, jour - 60);
        Routes r = p.routes;
        for (int d = debut; d < jour; d++) {
            for (int k = 0; k < r.nombre; k++) {
                if (r.fin[k] != Routes.OUVERTE) continue;
                float[] a = villes.get(r.villeA[k]), b = villes.get(r.villeB[k]);
                if (a == null && b == null) continue;
                if (a == null) a = entree(r.villeA[k]);
                if (b == null) b = entree(r.villeB[k]);
                float ecart = a[0] - b[0];
                // Lot F : là où un barde chante tes exploits, le renom part deux fois plus vite.
                Integer ba = bardes.get(ecart > 0f ? r.villeA[k] : r.villeB[k]);
                float part = DIFFUSION * r.niveau[k] / (1f + r.longueurKm[k] / PORTEE_KM) * (ba != null && ba >= d ? 2f : 1f);
                if (ecart > 0f) b[0] += ecart * part;
                else a[0] -= ecart * part;
                float[] fort = Math.abs(a[1]) >= Math.abs(b[1]) ? a : b, faible = fort == a ? b : a;
                faible[1] += (fort[1] - faible[1]) * part * 0.5f;
            }
            for (float[] v : villes.values()) {
                v[0] *= OUBLI;
                v[1] *= OUBLI_REPUTATION;
            }
            for (float[] g : groupes.values()) g[0] *= OUBLI_REPUTATION;
            villes.values().removeIf(v -> v[0] < 0.05f && Math.abs(v[1]) < 0.05f);
        }
        dernierJour = Math.max(dernierJour, jour);
    }

    /** Les villes où l'on te connaît, de la plus à la moins connue. */
    String villesConnues(int max) {
        StringBuilder t = new StringBuilder();
        villes.entrySet().stream().filter(e -> e.getValue()[0] >= 1f).sorted((x, y) -> Float.compare(y.getValue()[0], x.getValue()[0])).limit(max).forEach(e -> {
            float[] v = e.getValue();
            t.append("\n  ").append(Noms.ville(p.villes.peuple[e.getKey()], p.graine, e.getKey())).append(" : ").append(NOMS_PALIERS[palier(v[0])])
                    .append(" (").append(Math.round(v[0])).append("), réputation ").append(reputationTexte(v[1]));
        });
        return t.length() == 0 ? "\n  Personne ne te connaît encore." : t.toString();
    }

    void ecrire(DataOutputStream s) throws IOException {
        s.writeBoolean(initialise);
        s.writeInt(dernierJour);
        s.writeInt(metierMaitre);
        s.writeInt(ETIQUETTES);
        for (int x : etiquettes) s.writeInt(x);
        s.writeInt(villes.size());
        for (Map.Entry<Integer, float[]> e : villes.entrySet()) {
            s.writeInt(e.getKey());
            s.writeFloat(e.getValue()[0]);
            s.writeFloat(e.getValue()[1]);
        }
    }

    void lire(DataInputStream s) throws IOException {
        initialise = s.readBoolean();
        dernierJour = s.readInt();
        metierMaitre = s.readInt();
        int n = s.readInt();
        for (int k = 0; k < n; k++) {
            int x = s.readInt();
            if (k < ETIQUETTES) etiquettes[k] = x;
        }
        int v = s.readInt();
        for (int k = 0; k < v; k++) {
            int ville = s.readInt();
            villes.put(ville, new float[] { s.readFloat(), s.readFloat() });
        }
    }
}
