package com.deldaran.politics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.Arrays;

public class Flammes {
    static final int CHALEUR_MAX = 36;
    private static final float CADENCE = 1f / 30f, PIXEL_MIN = 2f;
    private static final int MARGE_BAS = 60, MARGE_COTE = 20;
    private static final int[] PERTES = { 0, 0, 1, 1 };

    private final Incendies inc;
    private final Front front;
    private Texture palette;
    private byte[] chaleur = new byte[0];
    private short[] bande = new short[0];
    private byte[] tampon = new byte[0];
    private short[] tamponBande = new short[0];
    private int largeur, hauteur, cx0, cy0;
    private float cote = -1f;
    private long derniere;
    private float reste;
    private int alea = 0x2545F491;

    private int[] listes = new int[4096];
    private int[] cles = new int[64], debuts = new int[64], fins = new int[64];
    private int[] comptes = new int[64];
    public int bandes;

    Flammes(Incendies inc, Front front) {
        this.inc = inc;
        this.front = front;
    }

    private int hasard() {
        alea ^= alea << 13;
        alea ^= alea >>> 17;
        alea ^= alea << 5;
        return alea & 0x7FFFFFFF;
    }

    private void preparerPalette() {
        if (palette != null) return;
        Pixmap p = new Pixmap(CHALEUR_MAX + 1, 1, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        float[][] cles = { { 0f, 0.25f, 0.04f, 0.02f, 0f }, { 4f, 0.55f, 0.08f, 0.03f, 0.75f }, { 10f, 0.85f, 0.2f, 0.04f, 1f }, { 17f, 0.98f, 0.45f, 0.07f, 1f },
                { 24f, 1f, 0.68f, 0.16f, 1f }, { 30f, 1f, 0.86f, 0.38f, 1f }, { 36f, 1f, 0.98f, 0.82f, 1f } };
        for (int h = 0; h <= CHALEUR_MAX; h++) {
            int k = 0;
            while (k + 2 < cles.length && cles[k + 1][0] < h) k++;
            float t = Math.max(0f, Math.min(1f, (h - cles[k][0]) / (cles[k + 1][0] - cles[k][0])));
            float r = cles[k][1] + (cles[k + 1][1] - cles[k][1]) * t, g = cles[k][2] + (cles[k + 1][2] - cles[k][2]) * t;
            float b = cles[k][3] + (cles[k + 1][3] - cles[k][3]) * t, a = cles[k][4] + (cles[k + 1][4] - cles[k][4]) * t;
            p.drawPixel(h, 0, h < 2 ? 0 : Color.rgba8888(r, g, b, a));
        }
        palette = new Texture(p);
        palette.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        p.dispose();
    }

    public Texture palette() {
        return palette;
    }

    public float cote() {
        return cote;
    }

    void vider() {
        Arrays.fill(chaleur, (byte) 0);
        bandes = 0;
    }

    void mettreAJour() {
        preparerPalette();
        float s = Math.max(PIXEL_MIN, front.echelleFlamme());
        int l = (int) Math.ceil(inc.largeur / s) + MARGE_COTE * 2, h = (int) Math.ceil(inc.hauteur / s) + MARGE_BAS + 10;
        int nx0 = (int) Math.floor((-MARGE_COTE * s - inc.origineX) / s), ny0 = (int) Math.floor((-MARGE_BAS * s - inc.origineY) / s);
        if (Math.abs(s - cote) > 0.01f || l != largeur || h != hauteur) {
            cote = s;
            largeur = l;
            hauteur = h;
            chaleur = new byte[l * h];
            bande = new short[l * h];
            tampon = new byte[l * h];
            tamponBande = new short[l * h];
            cx0 = nx0;
            cy0 = ny0;
        } else if (nx0 != cx0 || ny0 != cy0) {
            decaler(nx0 - cx0, ny0 - cy0);
            cx0 = nx0;
            cy0 = ny0;
        }
        long maintenant = System.nanoTime();
        if (derniere == 0) derniere = maintenant;
        reste += Math.min(0.2f, (maintenant - derniere) / 1e9f) * Math.max(0.5f, Math.min(2f, inc.vitesse));
        derniere = maintenant;
        boolean change = false;
        while (reste >= CADENCE) {
            reste -= CADENCE;
            propager();
            sources();
            change = true;
        }
        if (change || bandes == 0) trier();
    }

    private void decaler(int dx, int dy) {
        Arrays.fill(tampon, (byte) 0);
        for (int y = 0; y < hauteur; y++) {
            int sy = y + dy;
            if (sy < 0 || sy >= hauteur) continue;
            for (int x = 0; x < largeur; x++) {
                int sx = x + dx;
                if (sx < 0 || sx >= largeur) continue;
                tampon[y * largeur + x] = chaleur[sy * largeur + sx];
                tamponBande[y * largeur + x] = bande[sy * largeur + sx];
            }
        }
        byte[] t = chaleur;
        chaleur = tampon;
        tampon = t;
        short[] u = bande;
        bande = tamponBande;
        tamponBande = u;
    }

    private void propager() {
        float derive = front.derive();
        int seuilVent = (int) (Math.min(1f, Math.abs(derive)) * 0.5f * 1024f);
        int sens = derive >= 0f ? 1 : -1;
        for (int y = hauteur - 2; y >= 0; y--) {
            int ligne = y * largeur, dessus = ligne + largeur;
            for (int x = 0; x < largeur; x++) {
                int c = chaleur[ligne + x];
                if (c == 0) {
                    chaleur[dessus + x] = 0;
                    continue;
                }
                int r = hasard();
                int dx = (r & 3) - 1;
                if (dx == 2) dx = 0;
                if (((r >>> 2) & 1023) < seuilVent) dx += sens;
                int cible = x + dx;
                if (cible < 0 || cible >= largeur) continue;
                int perte = PERTES[(r >>> 12) & 3];
                int nouvelle = c - perte;
                chaleur[dessus + cible] = (byte) Math.max(0, nouvelle);
                bande[dessus + cible] = bande[ligne + x];
            }
        }
        Arrays.fill(chaleur, 0, largeur, (byte) 0);
    }

    private void poser(int px, int py, int valeur, int profondeur) {
        if (px < 0 || px >= largeur || py < 0 || py >= hauteur || valeur <= 0) return;
        int i = py * largeur + px;
        if (valeur >= chaleur[i]) {
            chaleur[i] = (byte) Math.min(CHALEUR_MAX, valeur);
            bande[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, profondeur));
        }
    }

    private int colonne(float x) {
        return (int) Math.floor((x - inc.origineX) / cote) - cx0;
    }

    private int rangee(float y) {
        return (int) Math.floor((y - inc.origineY) / cote) - cy0;
    }

    private void sources() {
        for (int j = 0; j < front.visibles; j++) {
            int classe = front.fc[j];
            float force = front.fa[j];
            float rx = (classe == 2 ? 0.9f : 0.6f) * front.pixelsHorizontal() / cote;
            float ry = Math.max(1f, rx * inc.ecrasement);
            int x0 = colonne(front.fx[j]), y0 = rangee(front.fy[j] + front.fd[j]);
            int haut = classe == 2 ? (int) (3.5f * inc.pixelsMetre / cote) : 0;
            int valeur = (int) ((classe == 0 ? 7 : classe == 1 ? 12 : 22) * (0.5f + 0.5f * force));
            int profondeur = (int) Math.floor(front.fy[j]);
            for (int dy = (int) -ry; dy <= ry + haut; dy++) {
                float largeurLigne = dy <= ry ? rx * (float) Math.sqrt(Math.max(0f, 1f - (dy * dy) / (ry * ry))) : rx * 0.8f * (1f - (dy - ry) / (float) (haut + 1)) + 1f;
                if (dy < -ry) continue;
                for (int dx = (int) -largeurLigne; dx <= largeurLigne; dx++) {
                    int r = hasard();
                    if ((r & 7) < 5) continue;
                    poser(x0 + dx, y0 + dy, valeur - ((r >>> 3) % 6), profondeur);
                }
            }
        }
        inc.sourcesBatiments(this);
    }

    void batiment(float[] coque, int points, float base, float niveau, float force, boolean braises, int profondeur, int graine) {
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (int k = 0; k < points; k++) {
            minY = Math.min(minY, coque[k * 2 + 1]);
            maxY = Math.max(maxY, coque[k * 2 + 1]);
        }
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        for (int k = 0; k < points; k++) {
            minX = Math.min(minX, coque[k * 2]);
            maxX = Math.max(maxX, coque[k * 2]);
        }
        float limite = minY + (maxY - minY) * niveau;
        int valeurMax = braises ? 9 : (int) (16 + 18 * force);
        float seuilFoyer = 0.975f - force * 0.06f;
        int temps = (int) (inc.temps * 2.5f);
        for (int c = Math.max(0, colonne(minX)); c <= Math.min(largeur - 1, colonne(maxX)); c++) {
            float x = inc.origineX + (cx0 + c + 0.5f) * cote;
            float bas = Float.MAX_VALUE, haut = -Float.MAX_VALUE;
            for (int k = 0; k < points; k++) {
                float ax = coque[k * 2], ay = coque[k * 2 + 1];
                float bx = coque[((k + 1) % points) * 2], by = coque[((k + 1) % points) * 2 + 1];
                if ((ax <= x && bx > x) || (bx <= x && ax > x)) {
                    float y = ay + (x - ax) / (bx - ax) * (by - ay);
                    bas = Math.min(bas, y);
                    haut = Math.max(haut, y);
                }
            }
            if (bas > haut) continue;
            int rb = rangee(bas), rh = rangee(haut);
            if (braises) {
                for (int r = rb; r <= Math.min(rh, rb + 4); r++) {
                    int alea = hasard();
                    if ((alea & 7) < 5) continue;
                    poser(c, r, valeurMax - ((alea >>> 3) % 5), profondeur);
                }
                continue;
            }
            int ligne = Math.min(rh, rangee(limite) + (int) (Front.hache(Math.floorDiv(c + cx0, 3), graine, 3) * 6f) - 3);
            boolean toit = limite >= haut;
            for (int r = ligne - 3; r <= ligne; r++) {
                int alea = hasard();
                if ((alea & 7) < (toit ? 2 : 3)) continue;
                poser(c, r, valeurMax - ((alea >>> 3) % 7), profondeur);
            }
        }
    }

    private void trier() {
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE, chauds = 0;
        for (int i = 0; i < chaleur.length; i++) {
            if (chaleur[i] < 2) continue;
            int cle = bande[i] >> 2;
            if (cle < min) min = cle;
            if (cle > max) max = cle;
            chauds++;
        }
        bandes = 0;
        if (chauds == 0) return;
        int etendue = max - min + 1;
        if (comptes.length < etendue + 1) comptes = new int[etendue + 1];
        Arrays.fill(comptes, 0, etendue + 1, 0);
        for (int i = 0; i < chaleur.length; i++) if (chaleur[i] >= 2) comptes[(bande[i] >> 2) - min]++;
        if (listes.length < chauds) listes = new int[chauds * 2];
        if (cles.length < etendue) {
            cles = new int[etendue];
            debuts = new int[etendue];
            fins = new int[etendue];
        }
        int position = 0;
        for (int k = 0; k < etendue; k++) {
            int n = comptes[k];
            if (n == 0) {
                comptes[k] = -1;
                continue;
            }
            cles[bandes] = (k + min) << 2;
            debuts[bandes] = position;
            fins[bandes] = position;
            comptes[k] = bandes;
            position += n;
            bandes++;
        }
        for (int i = 0; i < chaleur.length; i++) {
            if (chaleur[i] < 2) continue;
            int b = comptes[(bande[i] >> 2) - min];
            if (b < 0) continue;
            listes[fins[b]++] = i;
        }
    }

    public int profondeur(int b) {
        return cles[b];
    }

    public void tracer(SpriteBatch pinceau, int b) {
        float s = cote;
        for (int q = debuts[b]; q < fins[b]; q++) {
            int i = listes[q];
            int x = i % largeur, y = i / largeur;
            pinceau.draw(palette, inc.origineX + (cx0 + x) * s, inc.origineY + (cy0 + y) * s, s, s, chaleur[i], 0, 1, 1, false, false);
        }
    }

    void dispose() {
        if (palette != null) palette.dispose();
    }
}
