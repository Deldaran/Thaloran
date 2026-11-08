package com.deldaran.politics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.deldaran.politics.histoire.Peuple;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class Assembleur {
    public static final int L = 24;
    public static final int H = 34;
    public static final int CASES = 9;
    private static final int CACHE_MAX = 512;
    private static final int OMBRE = 0x0000003C;

    private final Pixmap pieces;
    private final Pixmap teintes;
    private final int[] nombres;
    private final Map<Long, Texture> cache = new LinkedHashMap<>(64, 0.75f, true);

    public Assembleur() {
        pieces = new Pixmap(Gdx.files.internal("personnages/pieces.png"));
        teintes = new Pixmap(Gdx.files.internal("personnages/teintes.png"));
        nombres = new int[teintes.getHeight()];
        for (int ligne = 0; ligne < nombres.length; ligne++) {
            int n = 0;
            while (n * 4 < teintes.getWidth() && (teintes.getPixel(n * 4, ligne) & 0xff) != 0) n++;
            nombres[ligne] = n;
        }
    }

    public Apparence tirer(long graine, int espece) {
        Random r = new Random(graine);
        Apparence a = new Apparence(graine, espece);
        int peau = r.nextInt(nombres[espece]);
        a.cheveux = r.nextInt(nombres[Apparence.CHEVEUX]);
        a.yeux = r.nextInt(nombres[Apparence.YEUX]);
        int tissu = r.nextInt(nombres[Apparence.TISSU]);
        int tissu2 = r.nextInt(nombres[Apparence.TISSU]);
        int tissu3 = r.nextInt(nombres[Apparence.TISSU]);
        int cuir = r.nextInt(nombres[Apparence.CUIR]);
        int habit = Apparence.MANTEAU + r.nextInt(4);

        if (r.nextFloat() < 0.4f) a.ajouter(Apparence.BATON, Apparence.BOIS, 0);
        a.ajouter(Apparence.CORPS, espece, peau);
        a.ajouter(Apparence.VISAGE, espece, peau);
        if (habit != Apparence.ROBE) a.ajouter(Apparence.PANTALON, Apparence.TISSU, tissu2);
        a.ajouter(Apparence.BOTTES, Apparence.CUIR, cuir);
        if (habit == Apparence.ARMURE) {
            a.ajouter(Apparence.TUNIQUE, Apparence.TISSU, tissu);
            a.ajouter(Apparence.ARMURE, Apparence.METAL, 0);
        } else {
            a.ajouter(habit, Apparence.TISSU, tissu);
        }
        if (habit != Apparence.ROBE || r.nextBoolean()) a.ajouter(Apparence.CEINTURE, Apparence.CUIR, cuir);

        if (espece == Peuple.NAIN) a.ajouter(Apparence.BARBE_LONGUE, Apparence.CHEVEUX, a.cheveux);
        else if (espece == Peuple.HUMAIN && r.nextFloat() < 0.45f) a.ajouter(r.nextBoolean() ? Apparence.BARBE_COURTE : Apparence.BARBE_LONGUE, Apparence.CHEVEUX, a.cheveux);

        int chapeau = r.nextInt(6);
        int[] coiffes = espece == Peuple.ORK ? new int[] { Apparence.CRETE, -1, Apparence.LONG }
            : new int[] { Apparence.COURT, Apparence.CHIGNON, Apparence.LONG, Apparence.MECHE, -1 };
        int coiffe = coiffes[r.nextInt(coiffes.length)];
        if (chapeau == 3) {
            a.ajouter(Apparence.CAPUCHE, Apparence.TISSU, tissu3);
        } else if (chapeau == 4) {
            a.ajouter(Apparence.CASQUE, Apparence.METAL, 0);
        } else {
            if (coiffe >= 0) a.ajouter(coiffe, Apparence.CHEVEUX, a.cheveux);
            if (chapeau == 5) a.ajouter(Apparence.POINTU, Apparence.TISSU, tissu3);
        }
        return a;
    }

    public Texture texture(Apparence a) {
        Texture t = cache.get(a.cle());
        if (t != null) return t;
        Pixmap p = assembler(a);
        t = new Texture(p);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        p.dispose();
        cache.put(a.cle(), t);
        if (cache.size() > CACHE_MAX) {
            Iterator<Map.Entry<Long, Texture>> it = cache.entrySet().iterator();
            it.next().getValue().dispose();
            it.remove();
        }
        return t;
    }

    private Pixmap assembler(Apparence a) {
        Pixmap p = new Pixmap(L * CASES, H, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        for (int c = 0; c < CASES; c++) {
            for (int x = 8; x < 18; x++) p.drawPixel(c * L + x, 32, OMBRE);
        }
        int yeux = couleur(Apparence.YEUX, a.yeux, 2);
        int sourcils = couleur(Apparence.CHEVEUX, a.cheveux, 1);
        for (int k = 0; k < a.nombre; k++) {
            int piece = a.pieces[k], ligne = a.lignes[k], rampe = a.rampes[k];
            for (int c = 0; c < CASES; c++) {
                for (int y = 0; y < H; y++) {
                    for (int x = 0; x < L; x++) {
                        int v = pieces.getPixel((a.espece * CASES + c) * L + x, piece * H + y);
                        if ((v & 0xff) == 0) continue;
                        int rouge = v >>> 24, vert = (v >>> 16) & 0xff, bleu = (v >>> 8) & 0xff;
                        int sortie;
                        if (rouge == vert && vert == bleu) sortie = couleur(ligne, rampe, rouge / 64);
                        else if (rouge == 255 && vert == 0 && bleu == 255) sortie = yeux;
                        else if (rouge == 0 && vert == 255 && bleu == 255) sortie = sourcils;
                        else sortie = v;
                        p.drawPixel(c * L + x, y, sortie);
                    }
                }
            }
        }
        return p;
    }

    private int couleur(int ligne, int rampe, int ton) {
        return teintes.getPixel(rampe * 4 + ton, ligne);
    }

    public void dispose() {
        for (Texture t : cache.values()) t.dispose();
        cache.clear();
        pieces.dispose();
        teintes.dispose();
    }
}
