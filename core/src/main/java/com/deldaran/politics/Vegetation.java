package com.deldaran.politics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.deldaran.politics.planet.Biome;
import com.deldaran.politics.planet.Bruit;
import com.deldaran.politics.planet.Cube;
import com.deldaran.politics.planet.Terrain;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class Vegetation {
    public static final double METRES_SPRITE = 0.0936;
    public static final double METRES_ECRAN_MAX = 0.4;

    private static final int NIVEAU_PARCELLE = 15;
    private static final int CASES = 24;
    private static final int CASE_L = 48;
    private static final int CASE_H = 72;
    private static final int ANCRE = 4;
    private static final int VARIANTES = 4;
    private static final int PAS_ECRAN = 48;
    private static final int PARCELLES_MAX = 600;
    private static final double RAYON_METRES = Terrain.RAYON_KM * 1000.0;

    private static final int FEUILLU = 0, SOMBRE = 1, CONIFERE = 2, MORT = 3, BUISSON = 4, ROCHER = 5;

    private final Terrain terrain;
    private final Texture planche;
    private final ShaderProgram agrandir;
    private final SpriteBatch pinceau = new SpriteBatch();

    private final Map<Long, float[]> parcelles = new ConcurrentHashMap<>();
    private final Map<Long, Long> usages = new HashMap<>();
    private final Set<Long> demandees = ConcurrentHashMap.newKeySet();
    private final LinkedBlockingQueue<Long> file = new LinkedBlockingQueue<>();
    private final Thread ouvrier;
    private long image;

    private final double[] est = new double[3], nord = new double[3], centre = new double[3];
    private final Set<Long> visibles = new HashSet<>();
    private float[] xs = new float[4096], ys = new float[4096];
    private int[] sortes = new int[4096];
    private long[] ordre = new long[4096];

    public Vegetation(Terrain terrain) {
        this.terrain = terrain;
        planche = new Texture(Gdx.files.internal("vegetation/vegetation.png"));
        planche.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        agrandir = new ShaderProgram(Gdx.files.internal("shaders/agrandir.vert"), Gdx.files.internal("shaders/agrandir.frag"));
        if (!agrandir.isCompiled()) {
            throw new IllegalStateException(agrandir.getLog());
        }
        pinceau.setShader(agrandir);
        ouvrier = new Thread(this::travailler, "vegetation");
        ouvrier.setDaemon(true);
        ouvrier.start();
    }

    private void travailler() {
        Terrain.Point point = new Terrain.Point();
        double[] d = new double[3];
        try {
            while (true) {
                long cle = file.take();
                parcelles.put(cle, planter(cle, point, d));
                demandees.remove(cle);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private float[] planter(long cle, Terrain.Point point, double[] d) {
        int face = (int) (cle >>> 40);
        int px = (int) ((cle >>> 20) & 0xFFFFF);
        int py = (int) (cle & 0xFFFFF);
        double cote = 2.0 / (1 << NIVEAU_PARCELLE);
        float[] arbres = new float[CASES * CASES * 5];
        int n = 0;
        for (int j = 0; j < CASES; j++) {
            for (int i = 0; i < CASES; i++) {
                long h = melanger(cle * 1000003L + j * CASES + i);
                double a = -1.0 + (px + (i + hasard(h, 1)) / CASES) * cote;
                double b = -1.0 + (py + (j + hasard(h, 2)) / CASES) * cote;
                Cube.direction(face, a, b, d);
                terrain.echantillonner(d[0], d[1], d[2], 11, point);
                if (point.altitude * 1000f < 6f || point.riviere || point.lac) continue;
                byte biome = terrain.biome(d[0], d[1], d[2], point);
                double f = RAYON_METRES / 180.0;
                double clairiere = 0.55 + 0.9 * Bruit.perlin(d[0] * f, d[1] * f, d[2] * f, terrain.graine + 700);
                double densite = densite(biome) * Math.max(0.0, Math.min(1.0, clairiere));
                if (hasard(h, 3) >= densite) continue;
                int sorte = choisir(biome, hasard(h, 4));
                if (sorte < 0) continue;
                arbres[n++] = (float) d[0];
                arbres[n++] = (float) d[1];
                arbres[n++] = (float) d[2];
                arbres[n++] = sorte * VARIANTES + (int) (hasard(h, 5) * VARIANTES);
                arbres[n++] = point.altitude;

            }
        }
        return Arrays.copyOf(arbres, n);
    }

    private static double densite(byte biome) {
        switch (biome) {
            case Biome.FORET: return 0.35;
            case Biome.JUNGLE: return 0.45;
            case Biome.TAIGA: return 0.3;
            case Biome.PRAIRIE: return 0.03;
            case Biome.SAVANE: return 0.02;
            case Biome.TOUNDRA: return 0.015;
            case Biome.DESERT: return 0.008;
            case Biome.MONTAGNE: return 0.03;
            default: return 0.0;
        }
    }

    private static int choisir(byte biome, double t) {
        switch (biome) {
            case Biome.FORET: return t < 0.45 ? FEUILLU : t < 0.8 ? SOMBRE : t < 0.95 ? BUISSON : MORT;
            case Biome.JUNGLE: return t < 0.7 ? SOMBRE : t < 0.9 ? FEUILLU : BUISSON;
            case Biome.TAIGA: return t < 0.85 ? CONIFERE : t < 0.9 ? MORT : t < 0.95 ? ROCHER : BUISSON;
            case Biome.PRAIRIE: return t < 0.5 ? FEUILLU : t < 0.9 ? BUISSON : ROCHER;
            case Biome.SAVANE: return t < 0.4 ? FEUILLU : t < 0.8 ? BUISSON : MORT;
            case Biome.TOUNDRA: return t < 0.5 ? ROCHER : t < 0.8 ? MORT : BUISSON;
            case Biome.DESERT: return t < 0.7 ? ROCHER : MORT;
            case Biome.MONTAGNE: return t < 0.7 ? ROCHER : CONIFERE;
            default: return -1;
        }
    }

    private static long melanger(long x) {
        x ^= x >>> 33;
        x *= 0xff51afd7ed558ccdL;
        x ^= x >>> 33;
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= x >>> 33;
        return x;
    }

    private static double hasard(long h, int k) {
        return (melanger(h + k * 0x9E3779B97F4A7C15L) >>> 11) * 0x1.0p-53;
    }

    public void dessiner(GameCamera camera, RenduTuiles tuiles) {
        double rayon = camera.rayonPixels;
        double metresEcran = RAYON_METRES / rayon;
        if (metresEcran > METRES_ECRAN_MAX) return;
        image++;
        preparerVue(camera);
        int largeur = Gdx.graphics.getBackBufferWidth(), hauteur = Gdx.graphics.getBackBufferHeight();
        chercherParcelles(rayon, largeur, hauteur);

        int n = 0;
        for (long cle : visibles) {
            float[] arbres = parcelles.get(cle);
            if (arbres == null) continue;
            for (int k = 0; k < arbres.length; k += 5) {
                double x = arbres[k], y = arbres[k + 1], z = arbres[k + 2];
                if (x * centre[0] + y * centre[1] + z * centre[2] <= 0.0) continue;
                float sx = (float) (largeur / 2.0 + (x * est[0] + y * est[1] + z * est[2]) * rayon);
                float sy = (float) (hauteur / 2.0 + (x * nord[0] + y * nord[1] + z * nord[2]) * rayon);
                if (sx < -200 || sx > largeur + 200 || sy < -200 || sy > hauteur + 200) continue;
                if (n == xs.length) agrandirTableaux();
                xs[n] = sx;
                ys[n] = sy + tuiles.decalage(arbres[k + 4]);
                sortes[n] = (int) arbres[k + 3];
                ordre[n] = ((long) (100000 - (int) sy) << 32) | n;
                n++;
            }
        }
        Arrays.sort(ordre, 0, n);

        float e = (float) (METRES_SPRITE / metresEcran);
        pinceau.getProjectionMatrix().setToOrtho2D(0f, 0f, largeur, hauteur);
        pinceau.begin();
        agrandir.setUniformf("u_taille", planche.getWidth(), planche.getHeight());
        agrandir.setUniformf("u_echelle", e);
        for (int k = 0; k < n; k++) {
            int i = (int) ordre[k];
            int sorte = sortes[i];
            int cx = (sorte % VARIANTES) * CASE_L, cy = (sorte / VARIANTES) * CASE_H;
            float x = Math.round(xs[i] - CASE_L / 2f * e);
            float y = Math.round(ys[i] - ANCRE * e);
            pinceau.draw(planche, x, y, CASE_L * e, CASE_H * e, cx, cy, CASE_L, CASE_H, false, false);
        }
        pinceau.end();
        oublier();
    }

    private void agrandirTableaux() {
        int t = xs.length * 2;
        xs = Arrays.copyOf(xs, t);
        ys = Arrays.copyOf(ys, t);
        sortes = Arrays.copyOf(sortes, t);
        ordre = Arrays.copyOf(ordre, t);
    }

    private void preparerVue(GameCamera camera) {
        double cosLon = Math.cos(camera.longitude), sinLon = Math.sin(camera.longitude);
        double cosLat = Math.cos(camera.latitude), sinLat = Math.sin(camera.latitude);
        est[0] = -sinLon; est[1] = cosLon; est[2] = 0;
        nord[0] = -sinLat * cosLon; nord[1] = -sinLat * sinLon; nord[2] = cosLat;
        centre[0] = cosLat * cosLon; centre[1] = cosLat * sinLon; centre[2] = sinLat;
    }

    private void chercherParcelles(double rayon, int largeur, int hauteur) {
        visibles.clear();
        int cote = 1 << NIVEAU_PARCELLE;
        for (int sy = -PAS_ECRAN; sy <= hauteur + PAS_ECRAN; sy += PAS_ECRAN) {
            for (int sx = -PAS_ECRAN; sx <= largeur + PAS_ECRAN; sx += PAS_ECRAN) {
                double qx = (sx - largeur / 2.0) / rayon, qy = (sy - hauteur / 2.0) / rayon;
                double q2 = qx * qx + qy * qy;
                if (q2 >= 1.0) continue;
                double qz = Math.sqrt(1.0 - q2);
                double x = est[0] * qx + nord[0] * qy + centre[0] * qz;
                double y = est[1] * qx + nord[1] * qy + centre[1] * qz;
                double z = est[2] * qx + nord[2] * qy + centre[2] * qz;
                int face = Cube.face(x, y, z);
                int px = Math.min(cote - 1, (int) ((Cube.coordonneeA(face, x, y, z) + 1.0) * 0.5 * cote));
                int py = Math.min(cote - 1, (int) ((Cube.coordonneeB(face, x, y, z) + 1.0) * 0.5 * cote));
                long cle = ((long) face << 40) | ((long) px << 20) | py;
                if (visibles.add(cle)) {
                    usages.put(cle, image);
                    if (!parcelles.containsKey(cle) && demandees.add(cle)) file.add(cle);
                }
            }
        }
    }

    private void oublier() {
        if (usages.size() <= PARCELLES_MAX) return;
        Iterator<Map.Entry<Long, Long>> it = usages.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, Long> e = it.next();
            if (image - e.getValue() > 120) {
                parcelles.remove(e.getKey());
                it.remove();
            }
        }
    }

    public void dispose() {
        ouvrier.interrupt();
        planche.dispose();
        agrandir.dispose();
        pinceau.dispose();
    }
}
