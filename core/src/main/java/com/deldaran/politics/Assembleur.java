package com.deldaran.politics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.IntIntMap;
import com.deldaran.politics.histoire.Peuple;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Compose les personnages à partir des sprites LPC exportés par assets/lpc_export.py (marche : 4 directions x 9 images). */
public class Assembleur {
    public static final int L = 64;
    public static final int H = 64;
    public static final int DIRECTIONS = 4;
    public static final int PAS = 9;
    private static final int CACHE_MAX = 1024;
    private static final int OMBRE = 0x00000048;
    private static final int ENFOUI = 4;
    private static final int[] BRINS = { 0, 2, 1, 3, 1, 0, 2, 3, 0, 1, 3, 2, 0 };
    private static final int Z_CORPS = 10, Z_TETE = 100, Z_OREILLES = 126;

    private static final String[] NOMS = { "chapeaux", "hauts", "bas", "bottes", "coiffures", "barbes", "capes", "epaules", "bras", "cou" };
    private static final String[] MATERIAUX = { "body", "cloth", "metal", "hair" };
    private static final int PEAU = 0, TISSU = 1, METAL = 2, CHEVEUX = 3;
    /** Corps exportés : humain, elfe, orc, nain (homme puis femme), indexés par espèce, puis les enfants dans le même ordre. */
    private static final int[] CORPS = new int[Peuple.NOMBRE];
    private static final int ENFANTS = 8;
    private static final String[] RACES = new String[Peuple.NOMBRE];
    private static final String[] TYPES = { "male", "female", "male", "female", "male", "female", "teen", "teen", "child", "child", "child", "child" };
    private static final int[] VRAIES_BARBES = { 1, 2, 3, 4 };

    static {
        CORPS[Peuple.HUMAIN] = 0;
        CORPS[Peuple.ELFE] = 2;
        CORPS[Peuple.ORK] = 4;
        CORPS[Peuple.NAIN] = 6;
        RACES[Peuple.HUMAIN] = "humain";
        RACES[Peuple.ELFE] = "elfe";
        RACES[Peuple.ORK] = "orc";
        RACES[Peuple.NAIN] = "nain";
    }

    private static class Objet {
        int z;
        List<String> types;
        int[] materiaux;
        int[] bases;
    }

    private static class Calque {
        final int z;
        final Pixmap image;
        final IntIntMap teinte;

        Calque(int z, Pixmap image, IntIntMap teinte) {
            this.z = z;
            this.image = image;
            this.teinte = teinte;
        }
    }

    private final Pixmap[] palettes = new Pixmap[MATERIAUX.length];
    private final Map<String, int[]> choix = new HashMap<>();
    private final List<List<Objet>> objets = new ArrayList<>();
    private final Map<String, Pixmap> images = new HashMap<>();
    private final Map<Long, Texture> cache = new LinkedHashMap<>(64, 0.75f, true);

    public Assembleur() {
        for (int m = 0; m < MATERIAUX.length; m++) palettes[m] = new Pixmap(Gdx.files.internal("lpc/palettes/" + MATERIAUX[m] + ".png"));
        for (String ligne : Gdx.files.internal("lpc/teintes.txt").readString("UTF-8").split("\n")) {
            if (ligne.isBlank() || ligne.startsWith("#")) continue;
            String[] t = ligne.trim().split(" ");
            int[] lignes = new int[t.length - 2];
            for (int i = 0; i < lignes.length; i++) lignes[i] = Integer.parseInt(t[i + 2]);
            choix.put(t[0], lignes);
        }
        for (int c = 0; c < NOMS.length; c++) objets.add(new ArrayList<>());
        for (String ligne : Gdx.files.internal("lpc/objets.txt").readString("UTF-8").split("\n")) {
            if (ligne.isBlank() || ligne.startsWith("#")) continue;
            String[] t = ligne.trim().split(" ");
            Objet o = new Objet();
            o.z = Integer.parseInt(t[2]);
            o.types = List.of(t[4].split(","));
            int n = t[5].equals("-") ? 0 : t.length - 5;
            o.materiaux = new int[n];
            o.bases = new int[n];
            for (int i = 0; i < n; i++) {
                String[] s = t[5 + i].split(":");
                o.materiaux[i] = indice(MATERIAUX, s[0]);
                o.bases[i] = Integer.parseInt(s[1]);
            }
            objets.get(indice(NOMS, t[0])).add(o);
        }
    }

    private static int indice(String[] noms, String nom) {
        for (int i = 0; i < noms.length; i++) if (noms[i].equals(nom)) return i;
        throw new IllegalArgumentException("inconnu : " + nom);
    }

    public Apparence tirer(long graine, int espece) {
        return tirer(graine, espece, -1);
    }

    public Apparence tirer(long graine, int espece, int sexe) {
        Random r = new Random(graine);
        Apparence a = new Apparence(graine, espece);
        a.femme = r.nextBoolean();
        a.enfant = r.nextFloat() < 0.2f;
        if (sexe >= 0) {
            a.femme = sexe == 1;
            a.enfant = false;
        }
        a.peau = choisir(r, choix.get("peau." + RACES[espece]));
        int cheveux = choisir(r, choix.get("cheveux"));
        int metal = choisir(r, choix.get("metal"));
        String type = TYPES[corps(a)];

        ajouter(a, r, Apparence.BAS, -1, type, cheveux, metal);
        ajouter(a, r, Apparence.BOTTES, -1, type, cheveux, metal);
        ajouter(a, r, Apparence.HAUT, -1, type, cheveux, metal);
        if (!a.femme && !a.enfant) {
            if (espece == Peuple.NAIN) ajouter(a, r, Apparence.BARBE, choisir(r, VRAIES_BARBES), type, cheveux, metal);
            else if (espece == Peuple.HUMAIN && r.nextFloat() < 0.5f) ajouter(a, r, Apparence.BARBE, -1, type, cheveux, metal);
            else if (espece == Peuple.ORK && r.nextFloat() < 0.3f) ajouter(a, r, Apparence.BARBE, -1, type, cheveux, metal);
        }
        ajouter(a, r, Apparence.COIFFURE, -1, type, cheveux, metal);
        if (r.nextFloat() < 0.45f) ajouter(a, r, Apparence.CHAPEAU, -1, type, cheveux, metal);
        if (r.nextFloat() < 0.3f) ajouter(a, r, Apparence.CAPE, -1, type, cheveux, metal);
        if (r.nextFloat() < 0.25f) ajouter(a, r, Apparence.EPAULES, -1, type, cheveux, metal);
        if (r.nextFloat() < 0.35f) ajouter(a, r, Apparence.BRAS, -1, type, cheveux, metal);
        if (r.nextFloat() < 0.3f) ajouter(a, r, Apparence.COU, -1, type, cheveux, metal);
        return a;
    }

    private static int corps(Apparence a) {
        return a.enfant ? ENFANTS + CORPS[a.espece] / 2 : CORPS[a.espece] + (a.femme ? 1 : 0);
    }

    /** Ajoute l'objet numero de la catégorie, ou un objet au hasard parmi ceux qui existent pour ce corps si numero < 0. */
    private void ajouter(Apparence a, Random r, int categorie, int numero, String type, int cheveux, int metal) {
        if (numero < 0) {
            List<Integer> possibles = new ArrayList<>();
            for (int i = 0; i < objets.get(categorie).size(); i++) if (objets.get(categorie).get(i).types.contains(type)) possibles.add(i);
            if (possibles.isEmpty()) return;
            numero = possibles.get(r.nextInt(possibles.size()));
        }
        Objet o = objets.get(categorie).get(numero);
        int tissu = choisir(r, choix.get("tissu"));
        int[] couleurs = new int[o.materiaux.length];
        for (int i = 0; i < couleurs.length; i++) {
            int m = o.materiaux[i];
            couleurs[i] = m == CHEVEUX ? cheveux : m == METAL ? metal : m == PEAU ? a.peau : tissu;
        }
        a.ajouter(categorie, numero, couleurs);
    }

    private static int choisir(Random r, int[] liste) {
        return liste[r.nextInt(liste.length)];
    }

    public Texture texture(Apparence a) {
        Texture t = cache.get(a.cle());
        if (t != null) return t;
        Pixmap p = assembler(a);
        t = new Texture(p);
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        p.dispose();
        cache.put(a.cle(), t);
        if (cache.size() > CACHE_MAX) {
            Iterator<Map.Entry<Long, Texture>> it = cache.entrySet().iterator();
            it.next().getValue().dispose();
            it.remove();
        }
        return t;
    }

    private Pixmap image(String chemin) {
        if (images.containsKey(chemin)) return images.get(chemin);
        FileHandle f = Gdx.files.internal(chemin);
        Pixmap p = f.exists() ? new Pixmap(f) : null;
        images.put(chemin, p);
        return p;
    }

    private IntIntMap teinte(int materiau, int base, int variante) {
        IntIntMap m = new IntIntMap();
        Pixmap pal = palettes[materiau];
        for (int x = 0; x < pal.getWidth(); x++) m.put(pal.getPixel(x, base) >>> 8, pal.getPixel(x, variante) >>> 8);
        return m;
    }

    private Pixmap assembler(Apparence a) {
        int corps = corps(a);
        String type = TYPES[corps];
        IntIntMap peau = teinte(PEAU, 0, a.peau);
        List<Calque> calques = new ArrayList<>();
        calques.add(new Calque(Z_CORPS, image("lpc/corps/" + corps + ".png"), peau));
        calques.add(new Calque(Z_TETE, image("lpc/tetes/" + corps + ".png"), peau));
        Pixmap oreilles = image("lpc/oreilles/" + corps + ".png");
        if (oreilles != null) calques.add(new Calque(Z_OREILLES, oreilles, peau));
        for (int k = 0; k < a.nombre; k++) {
            Objet o = objets.get(a.categories[k]).get(a.objets[k]);
            IntIntMap t = new IntIntMap();
            for (int i = 0; i < o.materiaux.length; i++) t.putAll(teinte(o.materiaux[i], o.bases[i], a.couleurs[k][i]));
            String base = "lpc/" + NOMS[a.categories[k]] + "/" + a.objets[k] + "_" + type;
            Pixmap derriere = image(base + "_derriere.png");
            if (derriere != null) calques.add(new Calque(0, derriere, t));
            calques.add(new Calque(o.z, image(base + ".png"), t));
        }
        calques.sort((c1, c2) -> Integer.compare(c1.z, c2.z));

        Pixmap p = new Pixmap(L * DIRECTIONS * PAS, H * 2, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        for (int image = 0; image < DIRECTIONS * PAS; image++) {
            for (int x = 22; x < 42; x++) p.drawPixel(image * L + x, H - 3, OMBRE);
            for (int x = 24; x < 40; x++) p.drawPixel(image * L + x, H - 2, OMBRE);
        }
        p.setBlending(Pixmap.Blending.SourceOver);
        for (Calque c : calques) copier(p, c);
        p.setBlending(Pixmap.Blending.None);
        for (int image = 0; image < DIRECTIONS * PAS; image++) {
            for (int x = 0; x < L; x++) {
                int fond = H - ENFOUI - BRINS[x % BRINS.length];
                for (int y = 0; y < fond; y++) p.drawPixel(image * L + x, H + y, p.getPixel(image * L + x, y));
            }
        }
        return p;
    }

    /** Recopie une planche LPC (lignes = directions, colonnes = pas) en une seule rangée d'images. */
    private static void copier(Pixmap p, Calque c) {
        for (int d = 0; d < DIRECTIONS; d++) {
            for (int f = 0; f < PAS; f++) {
                int dx = (d * PAS + f) * L;
                for (int y = 0; y < H; y++) {
                    for (int x = 0; x < L; x++) {
                        int v = c.image.getPixel(f * L + x, d * H + y);
                        if ((v & 0xff) == 0) continue;
                        int rgb = c.teinte.get(v >>> 8, -1);
                        p.drawPixel(dx + x, y, rgb < 0 ? v : (rgb << 8) | (v & 0xff));
                    }
                }
            }
        }
    }

    public void dispose() {
        for (Texture t : cache.values()) t.dispose();
        cache.clear();
        for (Pixmap m : palettes) m.dispose();
        for (Pixmap m : images.values()) if (m != null) m.dispose();
        images.clear();
    }
}
