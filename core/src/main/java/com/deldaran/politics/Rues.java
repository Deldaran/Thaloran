package com.deldaran.politics;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.deldaran.politics.planet.Terrain;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Rues {
    public static final int TAILLE = 1024;
    public static final double TEXEL = 0.25;
    public static final int NIVEAUX = 5;
    public static final float METRES_PIXEL_MAX = 1.5f;
    public static final double ECHELLE_HAUTEUR = 64.0;
    public static final double DECALAGE_HAUTEUR = 512.0;
    private static final double RECENTRER = 0.15;
    private static final double FONDU = 0.5;
    private static final double PLAT = 0.6;
    private static final double TALUS = 2.0;
    private static final double MARGE_MAISON = 0.4;
    private static final double BRUIT_TALUS = 0.7;
    private static final double TALUS_PONT = 1.0;
    private static final double PENTE_RIGOLE = 0.05;
    private static final double RAYON_METRES = Terrain.RAYON_KM * 1000.0;

    private final byte[] terre = new byte[TAILLE * TAILLE], pave = new byte[TAILLE * TAILLE], pont = new byte[TAILLE * TAILLE], sens = new byte[TAILLE * TAILLE], rigole = new byte[TAILLE * TAILLE];
    private final float[] poids = new float[TAILLE * TAILLE], hauteur = new float[TAILLE * TAILLE], sommePoids = new float[TAILLE * TAILLE], sommeHauteur = new float[TAILLE * TAILLE];
    private final Pixmap image = new Pixmap(TAILLE, TAILLE, Pixmap.Format.RGBA8888);
    private final Pixmap imageSol = new Pixmap(TAILLE, TAILLE, Pixmap.Format.RGBA8888);
    private final Texture texture, textureSol;
    /** Lot J1 : les champs, par texel : rien, sorte * 32 + variante, sens des sillons (0 à 255 pour 0 à pi), distance au bord (en dm, 0 hors champ). */
    private final byte[] champs = new byte[TAILLE * TAILLE * 4];
    private final Pixmap imageChamps = new Pixmap(TAILLE, TAILLE, Pixmap.Format.RGBA8888);
    private final Texture textureChamps;
    /** Lot J1 : les sillons se dessinent par rapport à une ancre fixe : ils ne sautent pas quand la texture se recentre. */
    private final double[] ancre = new double[3];
    private boolean ancree;
    private static final double ANCRE_MAX = 600.0;
    public float decalageX, decalageY;
    private final double[] origine = new double[3], est = new double[3], nord = new double[3];
    private boolean placee;
    private int versionVue = -1, niveau = -1;
    private double texel = TEXEL;
    public float cameraX, cameraY, base;
    public boolean actif;

    public Rues() {
        texture = new Texture(image);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        textureSol = new Texture(imageSol);
        textureSol.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        textureChamps = new Texture(imageChamps);
        textureChamps.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }

    public Texture champs() {
        return textureChamps;
    }

    public Texture texture() {
        return texture;
    }

    public Texture sol() {
        return textureSol;
    }

    public float texel() {
        return (float) texel;
    }

    public void mettreAJour(GameCamera camera, Villages villages, double metresPixel, double demiVue) {
        actif = metresPixel < METRES_PIXEL_MAX;
        int voulu = NIVEAUX - 1;
        for (int k = 0; k < NIVEAUX; k++) {
            if (TAILLE * TEXEL * (1 << k) * 0.5 * (1.0 - RECENTRER) >= demiVue) {
                voulu = k;
                break;
            }
        }
        double cosLat = Math.cos(camera.latitude);
        double cx = cosLat * Math.cos(camera.longitude), cy = cosLat * Math.sin(camera.longitude), cz = Math.sin(camera.latitude);
        double mx = 0.0, my = 0.0;
        if (placee) {
            mx = (cx * est[0] + cy * est[1] + cz * est[2]) * RAYON_METRES;
            my = (cx * nord[0] + cy * nord[1] + cz * nord[2]) * RAYON_METRES;
        }
        double limite = TAILLE * texel * 0.5 * RECENTRER;
        if (!placee || Math.abs(mx) > limite || Math.abs(my) > limite || versionVue != villages.version || voulu != niveau) {
            niveau = voulu;
            texel = TEXEL * (1 << niveau);
            placer(cx, cy, cz);
            placee = true;
            versionVue = villages.version;
            dessiner(villages);
            mx = 0.0;
            my = 0.0;
        }
        cameraX = (float) mx;
        cameraY = (float) my;
    }

    public double aplanir(double x, double y, double z, double sol) {
        if (!actif || !placee || x * origine[0] + y * origine[1] + z * origine[2] <= 0.0) return sol;
        double a = (x * est[0] + y * est[1] + z * est[2]) * RAYON_METRES;
        double b = (x * nord[0] + y * nord[1] + z * nord[2]) * RAYON_METRES;
        double u = a / texel + TAILLE * 0.5 - 0.5, v = b / texel + TAILLE * 0.5 - 0.5;
        int i = (int) Math.floor(u), j = (int) Math.floor(v);
        if (i < 0 || j < 0 || i + 1 >= TAILLE || j + 1 >= TAILLE) return sol;
        double fu = u - i, fv = v - j, somme = 0.0, sommeH = 0.0;
        for (int c = 0; c < 4; c++) {
            int di = c & 1, dj = c >> 1;
            int k = (j + dj) * TAILLE + i + di;
            double w = (di == 0 ? 1.0 - fu : fu) * (dj == 0 ? 1.0 - fv : fv) * poids[k];
            somme += w;
            sommeH += w * hauteur[k];
        }
        if (somme <= 0.0) return sol;
        return sol + (sommeH / somme + base - sol) * somme;
    }

    private void placer(double x, double y, double z) {
        origine[0] = x;
        origine[1] = y;
        origine[2] = z;
        double n = Math.sqrt(x * x + y * y);
        est[0] = -y / n;
        est[1] = x / n;
        est[2] = 0.0;
        nord[0] = -z * est[1];
        nord[1] = z * est[0];
        nord[2] = x * est[1] - y * est[0];
        double dx = x - ancre[0], dy = y - ancre[1], dz = z - ancre[2];
        if (!ancree || Math.sqrt(dx * dx + dy * dy + dz * dz) * RAYON_METRES > ANCRE_MAX) {
            ancre[0] = x;
            ancre[1] = y;
            ancre[2] = z;
            ancree = true;
            dx = dy = dz = 0.0;
        }
        decalageX = (float) ((dx * est[0] + dy * est[1] + dz * est[2]) * RAYON_METRES);
        decalageY = (float) ((dx * nord[0] + dy * nord[1] + dz * nord[2]) * RAYON_METRES);
    }

    /** Lot J1 : les champs des villages proches dans la texture. */
    private void dessinerChamps(Villages villages) {
        Arrays.fill(champs, (byte) 0);
        double[] w0 = new double[3], w1 = new double[3];
        for (int v = 0; v < villages.nombreVillages(); v++) {
            Villages.Village village = villages.village(v);
            float[] c = village.champs;
            if (c.length == 0) continue;
            if (village.x * origine[0] + village.y * origine[1] + village.z * origine[2] <= 0.0) continue;
            for (int k = 0; k + Villages.CHAMP - 1 < c.length; k += Villages.CHAMP) {
                village.monde(c[k], c[k + 1], w0);
                village.monde(c[k] + c[k + 2], c[k + 1] + c[k + 3], w1);
                double a = (w0[0] * est[0] + w0[1] * est[1] + w0[2] * est[2]) * RAYON_METRES;
                double b = (w0[0] * nord[0] + w0[1] * nord[1] + w0[2] * nord[2]) * RAYON_METRES;
                double ua = (w1[0] * est[0] + w1[1] * est[1] + w1[2] * est[2]) * RAYON_METRES - a;
                double ub = (w1[0] * nord[0] + w1[1] * nord[1] + w1[2] * nord[2]) * RAYON_METRES - b;
                double l = Math.hypot(ua, ub);
                if (l < 1e-9) continue;
                double angle = Math.atan2(ub, ua);
                if (angle < 0.0) angle += Math.PI;
                if (angle >= Math.PI) angle -= Math.PI;
                int code = ((int) c[k + 6] & 7) * 32 + ((int) c[k + 7] & 31);
                remplirChamp(a, b, ua / l, ub / l, c[k + 4], c[k + 5], code, (int) Math.round(angle / Math.PI * 255.0) & 255);
            }
        }
        ByteBuffer pixels = imageChamps.getPixels();
        pixels.position(0);
        pixels.put(champs);
        pixels.position(0);
        textureChamps.draw(imageChamps, 0, 0);
    }

    private void remplirChamp(double a0, double b0, double ca, double sa, double demiL, double demiW, int code, int angle) {
        double portee = Math.hypot(demiL, demiW);
        double demiCote = TAILLE * texel * 0.5;
        if (Math.abs(a0) - portee > demiCote || Math.abs(b0) - portee > demiCote) return;
        int i0 = Math.max(0, (int) Math.floor((a0 - portee) / texel) + TAILLE / 2);
        int i1 = Math.min(TAILLE - 1, (int) Math.floor((a0 + portee) / texel) + TAILLE / 2);
        int j0 = Math.max(0, (int) Math.floor((b0 - portee) / texel) + TAILLE / 2);
        int j1 = Math.min(TAILLE - 1, (int) Math.floor((b0 + portee) / texel) + TAILLE / 2);
        for (int j = j0; j <= j1; j++) {
            double b = (j - TAILLE / 2 + 0.5) * texel - b0;
            for (int i = i0; i <= i1; i++) {
                double a = (i - TAILLE / 2 + 0.5) * texel - a0;
                double x = a * ca + b * sa, z = -a * sa + b * ca;
                double d = Math.min(demiL - Math.abs(x), demiW - Math.abs(z));
                if (d <= 0.0) continue;
                int bord = (int) Math.max(1, Math.min(255, Math.round(d * 10.0)));
                int k = (j * TAILLE + i) * 4;
                if (bord <= (champs[k + 3] & 255)) continue;
                champs[k + 1] = (byte) code;
                champs[k + 2] = (byte) angle;
                champs[k + 3] = (byte) bord;
            }
        }
    }

    private void dessiner(Villages villages) {
        Arrays.fill(terre, (byte) 0);
        Arrays.fill(pave, (byte) 0);
        Arrays.fill(pont, (byte) 0);
        Arrays.fill(sens, (byte) 0);
        Arrays.fill(rigole, (byte) 0);
        Arrays.fill(poids, 0f);
        Arrays.fill(sommePoids, 0f);
        Arrays.fill(sommeHauteur, 0f);
        float[] r = villages.rues;
        base = 0f;
        for (int t = 0; t + Villages.TRAIT - 1 < villages.nombreRues; t += Villages.TRAIT) {
            if (r[t] * origine[0] + r[t + 1] * origine[1] + r[t + 2] * origine[2] <= 0.0) continue;
            double a0 = (r[t] * est[0] + r[t + 1] * est[1] + r[t + 2] * est[2]) * RAYON_METRES;
            double b0 = (r[t] * nord[0] + r[t + 1] * nord[1] + r[t + 2] * nord[2]) * RAYON_METRES;
            if (Math.abs(a0) < TAILLE * texel && Math.abs(b0) < TAILLE * texel) {
                base = Math.round(r[t + 8]);
                break;
            }
        }
        for (int t = 0; t + Villages.TRAIT - 1 < villages.nombreRues; t += Villages.TRAIT) {
            if (r[t] * origine[0] + r[t + 1] * origine[1] + r[t + 2] * origine[2] <= 0.0) continue;
            double a0 = (r[t] * est[0] + r[t + 1] * est[1] + r[t + 2] * est[2]) * RAYON_METRES;
            double b0 = (r[t] * nord[0] + r[t + 1] * nord[1] + r[t + 2] * nord[2]) * RAYON_METRES;
            double a1 = (r[t + 3] * est[0] + r[t + 4] * est[1] + r[t + 5] * est[2]) * RAYON_METRES;
            double b1 = (r[t + 3] * nord[0] + r[t + 4] * nord[1] + r[t + 5] * nord[2]) * RAYON_METRES;
            int type = (int) r[t + 7];
            boolean surEau = type == Villages.PONT || type == Villages.PONTON;
            tracer(a0, b0, a1, b1, r[t + 6] * 0.5, type == Villages.PAVE ? pave : surEau ? pont : terre, r[t + 8] - base, r[t + 9] - base, surEau ? TALUS_PONT : TALUS);
            double longueur = Math.hypot(a1 - a0, b1 - b0);
            if (!surEau && longueur > 1.0 && Math.abs(r[t + 9] - r[t + 8]) / longueur > PENTE_RIGOLE) rigoler(a0, b0, a1, b1);
        }
        for (int k = 0; k < villages.nombre; k++) {
            double x = villages.x[k], y = villages.y[k], z = villages.z[k];
            if (villages.plans[k].style >= Batiments.ARCHE) continue;
            if (x * origine[0] + y * origine[1] + z * origine[2] <= 0.0) continue;
            double a = (x * est[0] + y * est[1] + z * est[2]) * RAYON_METRES;
            double b = (x * nord[0] + y * nord[1] + z * nord[2]) * RAYON_METRES;
            niveler(a, b, villages.plans[k], villages.niveau[k] - base, villages.talus[k]);
        }
        ByteBuffer pixels = image.getPixels();
        ByteBuffer pixelsSol = imageSol.getPixels();
        pixels.position(0);
        pixelsSol.position(0);
        for (int k = 0; k < TAILLE * TAILLE; k++) {
            pixels.put(terre[k]);
            pixels.put(pave[k]);
            pixels.put(pont[k]);
            pixels.put(sens[k]);
            int w = Math.round(poids[k] * 255f);
            poids[k] = w / 255f;
            int code = 0;
            if (w > 0) {
                hauteur[k] = sommePoids[k] > 0f ? sommeHauteur[k] / sommePoids[k] : 0f;
                code = (int) Math.round((hauteur[k] + DECALAGE_HAUTEUR) * ECHELLE_HAUTEUR);
                code = Math.max(0, Math.min(65535, code));
                hauteur[k] = (float) (code / ECHELLE_HAUTEUR - DECALAGE_HAUTEUR);
            }
            pixelsSol.put((byte) (code >> 8));
            pixelsSol.put((byte) code);
            pixelsSol.put((byte) w);
            pixelsSol.put(rigole[k]);
        }
        pixels.position(0);
        pixelsSol.position(0);
        texture.draw(image, 0, 0);
        textureSol.draw(imageSol, 0, 0);
        dessinerChamps(villages);
    }

    private void tracer(double a0, double b0, double a1, double b1, double demi, byte[] canal, double h0, double h1, double talus) {
        double bord = demi + PLAT + talus * (1.0 + BRUIT_TALUS * 0.5);
        double demiCote = TAILLE * texel * 0.5;
        if (Math.max(a0, a1) + bord < -demiCote || Math.min(a0, a1) - bord > demiCote) return;
        if (Math.max(b0, b1) + bord < -demiCote || Math.min(b0, b1) - bord > demiCote) return;
        int i0 = Math.max(0, (int) Math.floor((Math.min(a0, a1) - bord) / texel) + TAILLE / 2);
        int i1 = Math.min(TAILLE - 1, (int) Math.floor((Math.max(a0, a1) + bord) / texel) + TAILLE / 2);
        int j0 = Math.max(0, (int) Math.floor((Math.min(b0, b1) - bord) / texel) + TAILLE / 2);
        int j1 = Math.min(TAILLE - 1, (int) Math.floor((Math.max(b0, b1) + bord) / texel) + TAILLE / 2);
        double da = a1 - a0, db = b1 - b0, l2 = da * da + db * db, l = Math.sqrt(l2);
        double fondu = Math.max(FONDU, texel);
        for (int j = j0; j <= j1; j++) {
            double b = (j - TAILLE / 2 + 0.5) * texel;
            for (int i = i0; i <= i1; i++) {
                double a = (i - TAILLE / 2 + 0.5) * texel;
                double t = l2 < 1e-9 ? 0.0 : Math.max(0.0, Math.min(1.0, ((a - a0) * da + (b - b0) * db) / l2));
                double d = Math.hypot(a - a0 - da * t, b - b0 - db * t);
                if (d >= bord) continue;
                int k = j * TAILLE + i;
                if (canal == pont) {
                    int large = (int) (Math.max(0.0, Math.min(1.0, (bord - d) / fondu + 0.5)) * 255.0);
                    if (large > (sens[k] & 255)) sens[k] = (byte) large;
                }
                double cote = (a - a0) * db - (b - b0) * da >= 0.0 ? 13.0 : 0.0;
                double large = talus * (1.0 + (bruit(t * l * 0.3, cote) - 0.5) * BRUIT_TALUS);
                double s = Math.max(0.0, Math.min(1.0, (demi + PLAT + large - d) / large));
                cumuler(k, (float) (s * s * (3.0 - 2.0 * s)), d <= demi + PLAT, h0 + (h1 - h0) * t);
                double v = Math.max(0.0, Math.min(1.0, (demi - d) / fondu + 0.5));
                if (v <= 0.0) continue;
                int ancien = canal[k] & 255;
                int nouveau = (int) (v * 255.0);
                if (nouveau > ancien) canal[k] = (byte) nouveau;
            }
        }
    }

    private void rigoler(double a0, double b0, double a1, double b1) {
        double demi = 0.22, bord = demi + 0.3;
        int i0 = Math.max(0, (int) Math.floor((Math.min(a0, a1) - bord) / texel) + TAILLE / 2);
        int i1 = Math.min(TAILLE - 1, (int) Math.floor((Math.max(a0, a1) + bord) / texel) + TAILLE / 2);
        int j0 = Math.max(0, (int) Math.floor((Math.min(b0, b1) - bord) / texel) + TAILLE / 2);
        int j1 = Math.min(TAILLE - 1, (int) Math.floor((Math.max(b0, b1) + bord) / texel) + TAILLE / 2);
        double da = a1 - a0, db = b1 - b0, l2 = da * da + db * db;
        for (int j = j0; j <= j1; j++) {
            double b = (j - TAILLE / 2 + 0.5) * texel;
            for (int i = i0; i <= i1; i++) {
                double a = (i - TAILLE / 2 + 0.5) * texel;
                double t = Math.max(0.0, Math.min(1.0, ((a - a0) * da + (b - b0) * db) / l2));
                double d = Math.hypot(a - a0 - da * t, b - b0 - db * t);
                double v = Math.max(0.0, Math.min(1.0, (bord - d) / (bord - demi * 0.4)));
                int k = j * TAILLE + i;
                int nouveau = (int) (v * 255.0);
                if (nouveau > (rigole[k] & 255)) rigole[k] = (byte) nouveau;
            }
        }
    }

    private void cumuler(int k, float w, boolean dedans, double h) {
        if (w <= 0f) return;
        float p = w * w * w * w * (dedans ? 1000f : 1f);
        sommePoids[k] += p;
        sommeHauteur[k] += (float) (p * h);
        if (w > poids[k]) poids[k] = w;
    }

    private static double bruit(double x, double y) {
        int i = (int) Math.floor(x), j = (int) Math.floor(y);
        double fx = x - i, fy = y - j;
        fx = fx * fx * (3.0 - 2.0 * fx);
        fy = fy * fy * (3.0 - 2.0 * fy);
        double a = hasard(i, j), b = hasard(i + 1, j), c = hasard(i, j + 1), d = hasard(i + 1, j + 1);
        return a + (b - a) * fx + (c - a) * fy + (a - b - c + d) * fx * fy;
    }

    private static double hasard(int i, int j) {
        long h = i * 0x9E3779B97F4A7C15L ^ j * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (h >>> 11) * 0x1.0p-53;
    }

    private void niveler(double a0, double b0, Batiments.Plan plan, double h, double talus) {
        double portee = plan.rayon() + MARGE_MAISON + talus * (1.0 + BRUIT_TALUS * 0.5);
        double demiCote = TAILLE * texel * 0.5;
        if (Math.abs(a0) - portee > demiCote || Math.abs(b0) - portee > demiCote) return;
        int i0 = Math.max(0, (int) Math.floor((a0 - portee) / texel) + TAILLE / 2);
        int i1 = Math.min(TAILLE - 1, (int) Math.floor((a0 + portee) / texel) + TAILLE / 2);
        int j0 = Math.max(0, (int) Math.floor((b0 - portee) / texel) + TAILLE / 2);
        int j1 = Math.min(TAILLE - 1, (int) Math.floor((b0 + portee) / texel) + TAILLE / 2);
        double c = Math.cos(plan.angle), s = Math.sin(plan.angle);
        double demiX = plan.largeur * 0.5 + plan.avant() * 0.5, centreX = -plan.avant() * 0.5, demiZ = plan.profondeur * 0.5;
        for (int j = j0; j <= j1; j++) {
            double b = (j - TAILLE / 2 + 0.5) * texel - b0;
            for (int i = i0; i <= i1; i++) {
                double a = (i - TAILLE / 2 + 0.5) * texel - a0;
                double x = a * c + b * s, z = a * s - b * c;
                double d = Math.hypot(Math.max(Math.abs(x - centreX) - demiX, 0.0), Math.max(Math.abs(z) - demiZ, 0.0));
                double large = talus * (1.0 + (bruit(x * 0.35 + 17.0, z * 0.35) - 0.5) * BRUIT_TALUS);
                double v = Math.max(0.0, Math.min(1.0, (MARGE_MAISON + large - d) / large));
                if (v <= 0.0) continue;
                cumuler(j * TAILLE + i, (float) (v * v * (3.0 - 2.0 * v)), d <= MARGE_MAISON, h);
            }
        }
    }

    public void dispose() {
        image.dispose();
        imageSol.dispose();
        texture.dispose();
        textureSol.dispose();
        imageChamps.dispose();
        textureChamps.dispose();
    }
}
