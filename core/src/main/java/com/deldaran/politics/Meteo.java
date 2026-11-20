package com.deldaran.politics;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.deldaran.politics.planet.Bruit;
import com.deldaran.politics.planet.Terrain;

public class Meteo {
    public static final int L = 256, H = 128;
    public static final String[] FORCAGES = { "reelle", "clair", "pluie", "orage", "brouillard", "neige" };
    public static final double ACCELERATION = 900.0;
    public static final double ACCELERATION_LOCALE = 60.0;
    private static final double DECLINAISON = 0.18;
    private static final double PAS = 300.0;
    private static final int PAS_DEMARRAGE = 600;
    private static final double RAYON_METRES = Terrain.RAYON_KM * 1000.0;
    private static final double SEUIL_PLUIE = 1.0;
    private static final double PLUIE_MAX = 3.0;
    private static final double NUAGE_MAX = 2.5;

    private final Terrain terrain;
    private final int graine;
    private final float[] temperature = new float[L * H];
    private final float[] climat = new float[L * H];
    private final float[] relief = new float[L * H];
    private final boolean[] ocean = new boolean[L * H];
    private float[] vapeur = new float[L * H];
    private float[] nuage = new float[L * H];
    private float[] suivantVapeur = new float[L * H];
    private float[] suivantNuage = new float[L * H];
    private final float[] pluie = new float[L * H];
    private final float[] brouillard = new float[L * H];
    private final float[] sol = new float[L * H];
    private final float[] neigeSol = new float[L * H];
    private final float[] ventX = new float[L * H];
    private final float[] ventY = new float[L * H];
    private final float[] tourbillon = new float[L * H];
    private final Pixmap image = new Pixmap(L, H, Pixmap.Format.RGBA8888);
    private Texture texture;
    private volatile boolean pret, nouvelle;
    private volatile boolean arret;
    /** Le fil qui calcule la météo ; dispose() l'attend avant de libérer l'image où il écrit. */
    private final Thread ouvrier;
    private double temps;
    private double avance;
    public int forcage;
    public double decalageHeures;

    public static final class Locale {
        public float nuage, pluie, orage, neige, brouillard, temperature, ventEst, ventNord, mouille, neigeSol;
    }

    public Meteo(Terrain terrain) {
        this.terrain = terrain;
        this.graine = terrain.graine + 9100;
        ouvrier = new Thread(this::travailler);
        ouvrier.setDaemon(true);
        ouvrier.start();
    }

    public boolean pret() {
        return pret;
    }

    public double heures() {
        return temps / 3600.0 + decalageHeures;
    }

    public double heureLocale(double longitude) {
        double h = heures() + longitude / (2.0 * Math.PI) * 24.0;
        return h - Math.floor(h / 24.0) * 24.0;
    }

    public void soleil(double[] s) {
        double angle = Math.PI - 2.0 * Math.PI * (heures() % 24.0) / 24.0;
        s[0] = Math.cos(DECLINAISON) * Math.cos(angle);
        s[1] = Math.cos(DECLINAISON) * Math.sin(angle);
        s[2] = Math.sin(DECLINAISON);
    }

    private void travailler() {
        Terrain.Point point = new Terrain.Point();
        for (int j = 0; j < H; j++) {
            double lat = latitude(j);
            for (int i = 0; i < L; i++) {
                double lon = longitude(i);
                terrain.echantillonner(Math.cos(lat) * Math.cos(lon), Math.cos(lat) * Math.sin(lon), Math.sin(lat), 6, point);
                int k = j * L + i;
                temperature[k] = point.temperature;
                climat[k] = point.pluie;
                ocean[k] = point.altitude < 0f;
                relief[k] = Math.max(point.altitude, 0f);
                vapeur[k] = (float) (0.7 * capacite(point.temperature));
                neigeSol[k] = point.temperature < -5f ? 1f : 0f;
            }
        }
        for (int n = 0; n < PAS_DEMARRAGE; n++) {
            if (arret) return;
            pas(PAS);
        }
        if (arret) return;
        ecrire();
        pret = true;
        long suivant = System.nanoTime();
        while (!arret) {
            if (avance >= PAS) {
                avance -= PAS;
                pas(PAS);
                if (arret) return;
                ecrire();
            } else {
                suivant += 20_000_000L;
                long attente = suivant - System.nanoTime();
                if (attente > 0) {
                    try {
                        Thread.sleep(attente / 1_000_000L);
                    } catch (InterruptedException fin) {
                        return;
                    }
                } else {
                    suivant = System.nanoTime();
                }
            }
        }
    }

    public void avancer(float delta, double acceleration) {
        if (pret) avance = Math.min(avance + delta * acceleration, PAS * 20);
    }

    private static double latitude(int j) {
        return ((j + 0.5) / H - 0.5) * Math.PI;
    }

    private static double longitude(int i) {
        return ((i + 0.5) / L) * 2.0 * Math.PI - Math.PI;
    }

    private static double capacite(double t) {
        return 4.0 * Math.exp(0.06 * t);
    }

    private void vents() {
        double t = temps / 3600.0 / 40.0;
        for (int j = 0; j < H; j++) {
            double lat = latitude(j), cosLat = Math.cos(lat), sinLat = Math.sin(lat);
            for (int i = 0; i < L; i++) {
                double lon = longitude(i);
                double x = cosLat * Math.cos(lon) * 2.2, y = cosLat * Math.sin(lon) * 2.2, z = sinLat * 2.2;
                tourbillon[j * L + i] = Bruit.perlin(x + t, y - t * 0.6, z + t * 0.4, graine);
            }
        }
        for (int j = 0; j < H; j++) {
            double lat = latitude(j);
            double a = Math.abs(Math.toDegrees(lat));
            double est = a < 30.0 ? -6.0 : a < 60.0 ? 10.0 * Math.sin((a - 30.0) / 30.0 * Math.PI) : -4.0;
            double nord = a < 30.0 ? -Math.signum(lat) * 2.0 * Math.sin(a / 30.0 * Math.PI) : 0.0;
            int jh = Math.min(H - 1, j + 1), jb = Math.max(0, j - 1);
            for (int i = 0; i < L; i++) {
                int ig = (i + L - 1) % L, id = (i + 1) % L;
                double dy = tourbillon[jh * L + i] - tourbillon[jb * L + i];
                double dx = tourbillon[j * L + id] - tourbillon[j * L + ig];
                ventX[j * L + i] = (float) (est - dy * 40.0);
                ventY[j * L + i] = (float) (nord + dx * 40.0);
            }
        }
    }

    private void pas(double dt) {
        temps += dt;
        vents();
        double cellY = Math.PI * RAYON_METRES / H;
        for (int j = 0; j < H; j++) {
            double cellX = 2.0 * Math.PI * RAYON_METRES * Math.max(0.05, Math.cos(latitude(j))) / L;
            for (int i = 0; i < L; i++) {
                int k = j * L + i;
                double di = Math.max(-3.0, Math.min(3.0, ventX[k] * dt / cellX));
                double dj = Math.max(-3.0, Math.min(3.0, ventY[k] * dt / cellY));
                suivantVapeur[k] = echantillon(vapeur, i - di, j - dj);
                suivantNuage[k] = echantillon(nuage, i - di, j - dj);
            }
        }
        float[] e = vapeur;
        vapeur = suivantVapeur;
        suivantVapeur = e;
        e = nuage;
        nuage = suivantNuage;
        suivantNuage = e;

        double h = dt / 3600.0;
        for (int k = 0; k < L * H; k++) {
            double t = temperature[k];
            double c = capacite(t);
            double q = vapeur[k], n = nuage[k];
            double humide = Math.max(0.03, Math.min(1.2, climat[k] / 1500.0));
            double evaporation = ocean[k] ? h / 24.0 * Math.max(0.0, 0.92 * c - q) : h / 48.0 * humide * Math.max(0.0, 0.8 * c - q);
            q += evaporation;
            double lat = Math.toDegrees(latitude(k / L));
            double depression = Math.min(1.0, Math.max(0.0, tourbillon[k]) * 2.5);
            double seuil = c * (1.0 - 0.2 * Math.min(1.0, climat[k] / 2500.0)) * (1.0 - 0.15 * Math.min(1.0, relief[k] / 2.0))
                * (1.0 - 0.3 * depression) * (1.0 - 0.15 * Math.exp(-lat * lat / 100.0));
            if (q > seuil) {
                double condense = (q - seuil) * Math.min(1.0, h);
                q -= condense;
                n += condense;
            } else if (n > 0.0 && q < 0.7 * c) {
                double seche = Math.min(n, 0.1 * h * n);
                n -= seche;
                q += seche;
            }
            double precipitation = Math.max(0.0, n - SEUIL_PLUIE) * Math.min(1.0, 0.4 * h);
            n -= precipitation;
            pluie[k] = (float) (precipitation / h);
            if (t < 0.0) {
                neigeSol[k] = (float) Math.min(1.0, neigeSol[k] + pluie[k] * h * 0.15);
            } else {
                sol[k] = (float) Math.min(1.0, sol[k] + pluie[k] * h * 0.3);
                neigeSol[k] = (float) Math.max(0.0, neigeSol[k] - h * 0.01 * t);
            }
            sol[k] = (float) Math.max(0.0, sol[k] - h * (0.03 + 0.003 * Math.max(0.0, t)));
            vapeur[k] = (float) q;
            nuage[k] = (float) n;
            double vent = Math.sqrt(ventX[k] * ventX[k] + ventY[k] * ventY[k]);
            double froid = ocean[k] && t > 12.0 ? 0.3 : 1.0;
            brouillard[k] = (float) (Math.max(0.0, Math.min(1.0, (q / c - 0.85) * 8.0)) * Math.max(0.0, Math.min(1.0, 1.0 - vent / 7.0)) * froid);
        }
    }

    private static float echantillon(float[] champ, double x, double y) {
        y = Math.max(0.0, Math.min(H - 1.0, y));
        int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y);
        double fx = x - x0, fy = y - y0;
        int y1 = Math.min(H - 1, y0 + 1);
        int a = Math.floorMod(x0, L), b = Math.floorMod(x0 + 1, L);
        double haut = champ[y0 * L + a] * (1.0 - fx) + champ[y0 * L + b] * fx;
        double bas = champ[y1 * L + a] * (1.0 - fx) + champ[y1 * L + b] * fx;
        return (float) (haut * (1.0 - fy) + bas * fy);
    }

    private void ecrire() {
        synchronized (image) {
            for (int j = 0; j < H; j++) {
                for (int i = 0; i < L; i++) {
                    int k = j * L + i;
                    int r = octet(nuage[k] / NUAGE_MAX), g = octet(pluie[k] / PLUIE_MAX), b = octet(brouillard[k]), a = octet((temperature[k] + 30.0) / 60.0);
                    image.drawPixel(i, H - 1 - j, (r << 24) | (g << 16) | (b << 8) | a);
                }
            }
        }
        nouvelle = true;
    }

    private static int octet(double v) {
        return (int) Math.round(Math.max(0.0, Math.min(1.0, v)) * 255.0);
    }

    public Texture texture() {
        if (texture == null) {
            texture = new Texture(L, H, Pixmap.Format.RGBA8888);
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.ClampToEdge);
        }
        if (nouvelle) {
            nouvelle = false;
            synchronized (image) {
                texture.draw(image, 0, 0);
            }
        }
        return texture;
    }

    public void locale(double x, double y, double z, Locale sortie) {
        double lat = Math.asin(Math.max(-1.0, Math.min(1.0, z))), lon = Math.atan2(y, x);
        double fi = (lon + Math.PI) / (2.0 * Math.PI) * L - 0.5, fj = (lat / Math.PI + 0.5) * H - 0.5;
        float n = echantillon(nuage, fi, fj), p = echantillon(pluie, fi, fj);
        float t = echantillon(temperature, fi, fj);
        double km = RAYON_METRES / 1000.0 / 6.0, derive = temps / 3600.0 * 0.4;
        double tache = 0.5 + 0.9 * Bruit.perlin(x * km + derive, y * km, z * km - derive, graine + 1);
        double pluieLocale = Math.min(1.0, p / PLUIE_MAX) * Math.max(0.0, Math.min(1.0, tache + 0.3));
        sortie.nuage = (float) Math.min(1.0, n / NUAGE_MAX + pluieLocale * 0.5);
        sortie.temperature = t;
        sortie.neige = t < 0f ? (float) pluieLocale : 0f;
        sortie.pluie = t < 0f ? 0f : (float) pluieLocale;
        sortie.orage = t < 0f ? 0f : (float) Math.max(0.0, Math.min(1.0, (p - 1.5) / 2.0 * tache));
        sortie.brouillard = echantillon(brouillard, fi, fj);
        sortie.ventEst = echantillon(ventX, fi, fj);
        sortie.ventNord = echantillon(ventY, fi, fj);
        sortie.mouille = echantillon(sol, fi, fj);
        sortie.neigeSol = echantillon(neigeSol, fi, fj);
        switch (forcage) {
            case 1: sortie.nuage = 0f; sortie.pluie = 0f; sortie.orage = 0f; sortie.neige = 0f; sortie.brouillard = 0f; sortie.mouille = 0f; sortie.neigeSol = 0f; break;
            case 2: sortie.nuage = 0.8f; sortie.pluie = 0.6f; sortie.orage = 0f; sortie.neige = 0f; sortie.mouille = 0.7f; sortie.neigeSol = 0f; break;
            case 3: sortie.nuage = 1f; sortie.pluie = 1f; sortie.orage = 1f; sortie.neige = 0f; sortie.mouille = 1f; sortie.neigeSol = 0f; break;
            case 4: sortie.nuage = 0.5f; sortie.pluie = 0f; sortie.orage = 0f; sortie.neige = 0f; sortie.brouillard = 0.9f; break;
            case 5: sortie.nuage = 0.7f; sortie.pluie = 0f; sortie.orage = 0f; sortie.neige = 0.8f; sortie.mouille = 0f; sortie.neigeSol = 0.8f; break;
            default: break;
        }
    }

    public void dispose() {
        arret = true;
        ouvrier.interrupt();
        try {
            ouvrier.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        image.dispose();
        if (texture != null) texture.dispose();
    }
}
