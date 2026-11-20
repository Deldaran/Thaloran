package com.deldaran.politics;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

public class Bulles {
    public static final int CASE = 11;
    private static final String[] NOMS = { "coeur", "colere", "zzz", "exclamation", "question", "larme", "note", "etoiles", "sueur", "points", "flocon", "malade", "eclair" };
    private static final int[] COULEURS = { 0xDC323C, 0xDC281E, 0xE6E6FF, 0xFAD23C, 0xFAFAFA, 0x5AA0F0, 0xFAFAFA, 0xFADC50, 0x78BEFA, 0xFAFAFA, 0xE6F0FF, 0x78BE50, 0xFADC3C };
    private static final String[][] DESSINS = {
        { "...........", "..oo...oo..", ".o##o.o##o.", "o#+##o####o", "o#########o", "o#########o", ".o#######o.", "..o#####o..", "...o###o...", "....o#o....", ".....o....." },
        { "...........", ".o#o...o#o.", ".o##o.o##o.", "..o##.##o..", "...o...o...", "...........", "...o...o...", "..o##.##o..", ".o##o.o##o.", ".o#o...o#o.", "..........." },
        { "......ooooo", "......o###o", "........o#o", ".......o#o.", "......o###o", "oooo..ooooo", "o##o.......", "..o#o......", ".o#o.......", "o####o.....", "oooooo....." },
        { "....ooo....", "....o#o....", "....o#o....", "....o#o....", "....o#o....", "....o#o....", "....ooo....", "...........", "....ooo....", "....o#o....", "....ooo...." },
        { "...ooooo...", "..o#####o..", "..o#ooo#o..", "...o..o#o..", ".....o#o...", "....o#o....", "....ooo....", "...........", "....ooo....", "....o#o....", "....ooo...." },
        { ".....o.....", ".....o.....", "....o#o....", "....o#o....", "...o###o...", "...o#+#o...", "..o##+##o..", "..o#####o..", "..o#####o..", "...o###o...", "....ooo...." },
        { ".....oooooo", ".....o####o", ".....o#ooo#", ".....o#..o#", ".....o#..o#", ".....o#..o#", "..ooo#o.oo#", ".o####o.o##", ".o####oo###", "..oooo.o###", "........oo." },
        { "..o........", ".o#o.......", "o###o...o..", ".o#o...o#o.", "..o...o###o", ".......o#o.", "...o....o..", "..o#o......", ".o###o.....", "..o#o......", "...o......." },
        { "...........", "o..........", "#o.....o...", "##o...o#o..", "o#o..o###o.", ".o...o###o.", "......ooo..", "...........", "...........", "...........", "..........." },
        { "...........", "...........", "...........", "...........", "ooo.ooo.ooo", "o#o.o#o.o#o", "ooo.ooo.ooo", "...........", "...........", "...........", "..........." },
        { ".....o.....", "...o.#.o...", "....###....", ".o..o#o..o.", "..#.o#o.#..", "o#########o", "..#.o#o.#..", ".o..o#o..o.", "....###....", "...o.#.o...", ".....o....." },
        { "...........", "...ooooo...", "..o#####o..", ".o#o###o#o.", ".o###o###o.", ".o#######o.", ".o#ooooo#o.", "..o#####o..", "...ooooo...", "...........", "..........." },
        { ".....oooo..", "....o##o...", "...o##o....", "..o####oo..", "...oo##o...", "....o#o....", "...o#o.....", "..o#o......", "..oo.......", "...........", "..........." },
    };

    public final Texture texture;

    public Bulles() {
        Pixmap p = new Pixmap(CASE * NOMS.length, CASE, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        for (int k = 0; k < NOMS.length; k++) {
            int c = COULEURS[k];
            int clair = ((Math.min(255, (c >> 16) + 60)) << 16) | ((Math.min(255, ((c >> 8) & 255) + 60)) << 8) | Math.min(255, (c & 255) + 60);
            for (int y = 0; y < CASE; y++) {
                String ligne = DESSINS[k][y];
                for (int x = 0; x < CASE; x++) {
                    char ch = ligne.charAt(x);
                    int rgb = ch == 'o' ? 0x1E191E : ch == '#' ? c : ch == '+' ? clair : -1;
                    if (rgb >= 0) p.drawPixel(k * CASE + x, y, (rgb << 8) | 0xFF);
                }
            }
        }
        texture = new Texture(p);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        p.dispose();
    }

    public int indice(String nom) {
        for (int k = 0; k < NOMS.length; k++) if (NOMS[k].equals(nom)) return k;
        return -1;
    }

    public void dispose() {
        texture.dispose();
    }
}
