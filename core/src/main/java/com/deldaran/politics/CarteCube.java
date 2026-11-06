package com.deldaran.politics;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.deldaran.politics.planet.Sphere;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import com.deldaran.politics.planet.Cube;

public class CarteCube {
    public static final int COTE = 256;

    public final Texture texture;

    private final Pixmap image;
    private final IntBuffer pixels;
    private final int[] caseTexel;

    public CarteCube(Sphere sphere) {
        int largeur = COTE * Cube.FACES;
        image = new Pixmap(largeur, COTE, Pixmap.Format.RGBA8888);
        pixels = image.getPixels().order(ByteOrder.BIG_ENDIAN).asIntBuffer();
        caseTexel = new int[largeur * COTE];

        double[] d = new double[3];
        for (int face = 0; face < Cube.FACES; face++) {
            for (int y = 0; y < COTE; y++) {
                for (int x = 0; x < COTE; x++) {
                    Cube.direction(face, 2.0 * (x + 0.5) / COTE - 1.0, 2.0 * (y + 0.5) / COTE - 1.0, d);
                    int texel = y * largeur + face * COTE + x;
                    caseTexel[texel] = sphere.plusProche((float) d[0], (float) d[1], (float) d[2]);
                }
            }
        }


        texture = new Texture(image);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }

    public void mettreAJour(int[] couleurs) {
        for (int texel = 0; texel < caseTexel.length; texel++) {
            pixels.put(texel, couleurs[caseTexel[texel]]);
        }
        texture.draw(image, 0, 0);
    }

    public void dispose() {
        texture.dispose();
        image.dispose();
    }
}
