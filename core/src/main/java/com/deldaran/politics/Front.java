package com.deldaran.politics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.deldaran.politics.planet.Biome;
import com.deldaran.politics.planet.Terrain;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Front {
    static final int C = 32, PX = 4, MORCEAUX_MAX = 900, VISIBLES_MAX = 8000;
    static final byte INTACT = 0, EN_FEU = 1, CENDRE = 2;
    static final byte VIDE = 0, HERBE = 1, BROUSSAILLE = 2, ARBRE = 3, BATI = 4;
    static final int COULEURS = 6, BUDGET = 45000;
    static final int[] PIXELS = { 22, 40, 90 };
    static final float[] LARGEUR_M = { 1.0f, 1.3f, 2.2f }, HAUTEUR_M = { 0.8f, 1.6f, 4.0f };
    private static final int BRAISE_VIVE = Color.rgba8888(1f, 0.55f, 0.12f, 0.95f), BRAISE = Color.rgba8888(0.85f, 0.25f, 0.05f, 0.92f);
    private static final int BRAISE_SOMBRE = Color.rgba8888(0.35f, 0.07f, 0.03f, 0.9f), TISON = Color.rgba8888(1f, 0.42f, 0.08f, 1f);
    private static final int[][] VOISINS = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 }, { 1, 1 }, { 1, -1 }, { -1, 1 }, { -1, -1 } };

    static final class Morceau {
        final int ci, cj;
        final byte[] etat = new byte[C * C], sorte = new byte[C * C];
        final float[] combustible = new float[C * C], initial = new float[C * C], temps = new float[C * C];
        final float[] facilite = new float[C * C], duree = new float[C * C], intensite = new float[C * C], fin = new float[C * C];
        long[] bati;
        final float[] coins = new float[4];
        Texture texture;
        Pixmap image;
        boolean sale;
        float braisesJusqua, prochaineMaj;
        int enFeu;
        float sommeE, sommeN;

        Morceau(int ci, int cj) {
            this.ci = ci;
            this.cj = cj;
        }
    }

    private final Incendies inc;
    private final Terrain terrain;
    public Vegetation vegetation;
    public final Flammes flammes;
    private final Map<Long, Morceau> morceaux = new HashMap<>();
    private int[] feuI = new int[1024], feuJ = new int[1024];
    private int nombreFeu, brulees;
    private final Terrain.Point point = new Terrain.Point();
    private final double[] monde = new double[3], coin = new double[3];
    private final float[] valeurs = new float[(C + 2) * (C + 2)], feux = new float[(C + 2) * (C + 2)];

    float[] fx = new float[256], fy = new float[256], fd = new float[256], fa = new float[256];
    int[] fc = new int[256], fi = new int[256];
    public int visibles;
    private float pixelsHorizontal, derive, reduction = 1f;
    private Texture atlas;
    private final float[] sommets = new float[20];

    Front(Incendies inc, Terrain terrain) {
        this.inc = inc;
        this.terrain = terrain;
        flammes = new Flammes(inc, this);
    }

    float derive() {
        return derive;
    }

    float pixelsHorizontal() {
        return pixelsHorizontal;
    }

    public boolean actif() {
        return nombreFeu > 0;
    }

    public boolean present() {
        return !morceaux.isEmpty();
    }

    public int enFeu() {
        return nombreFeu;
    }

    public int brulees() {
        return brulees;
    }

    void vider() {
        for (Morceau m : morceaux.values()) liberer(m);
        morceaux.clear();
        nombreFeu = 0;
        brulees = 0;
        visibles = 0;
        flammes.vider();
    }

    private static void liberer(Morceau m) {
        if (m.texture != null) m.texture.dispose();
        if (m.image != null) m.image.dispose();
    }

    private static long cleMorceau(int ci, int cj) {
        return ((long) ci << 32) ^ (cj & 0xFFFFFFFFL);
    }

    private Morceau morceau(int gi, int gj, Villages villages) {
        int ci = Math.floorDiv(gi, C), cj = Math.floorDiv(gj, C);
        long c = cleMorceau(ci, cj);
        Morceau m = morceaux.get(c);
        if (m != null || villages == null || morceaux.size() >= MORCEAUX_MAX) return m;
        m = new Morceau(ci, cj);
        remplir(m, villages);
        morceaux.put(c, m);
        return m;
    }

    private static float herbe(byte biome) {
        switch (biome) {
            case Biome.PRAIRIE: return 1.0f;
            case Biome.SAVANE: return 1.2f;
            case Biome.FORET: return 0.8f;
            case Biome.TAIGA: return 0.7f;
            case Biome.JUNGLE: return 0.45f;
            case Biome.TOUNDRA: return 0.35f;
            case Biome.MONTAGNE: return 0.2f;
            case Biome.DESERT: return 0.05f;
            default: return 0f;
        }
    }

    static float hache(int a, int b, int c) {
        long h = a * 0x9E3779B97F4A7C15L ^ b * 0xC2B2AE3D27D4EB4FL ^ c * 0x165667B19E3779F9L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (h >>> 40) / (float) (1 << 24);
    }

    private static float bruit(float x, float y) {
        int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y);
        float fx = x - x0, fy = y - y0;
        fx = fx * fx * (3f - 2f * fx);
        fy = fy * fy * (3f - 2f * fy);
        float a = hache(x0, y0, 7), b = hache(x0 + 1, y0, 7), c = hache(x0, y0 + 1, 7), d = hache(x0 + 1, y0 + 1, 7);
        return (a + (b - a) * fx) + ((c + (d - c) * fx) - (a + (b - a) * fx)) * fy;
    }

    private void remplir(Morceau m, Villages villages) {
        int gi0 = m.ci * C, gj0 = m.cj * C;
        for (int q = 0; q < 4; q++) {
            inc.versMonde(gi0 + (q & 1) * C, gj0 + (q >> 1) * C, monde);
            m.coins[q] = RenduTuiles.altitudeSol(terrain, monde[0], monde[1], monde[2], point, coin);
        }
        for (int j = 0; j < C; j++) {
            for (int i = 0; i < C; i++) {
                int k = j * C + i;
                float e = gi0 + i + 0.5f, n = gj0 + j + 0.5f;
                inc.versMonde(e, n, monde);
                terrain.echantillonner(monde[0], monde[1], monde[2], 11, point);
                if (point.altitude <= 0f || Personnage.profondeur(point) > 0f) continue;
                float base = herbe(terrain.biome(monde[0], monde[1], monde[2], point));
                float charge = base * (0.35f + 0.9f * bruit(e / 11f, n / 11f) + 0.25f * hache(gi0 + i, gj0 + j, 3));
                if (charge < 0.12f) continue;
                if (villages.surRue(monde[0], monde[1], monde[2])) continue;
                definir(m, k, HERBE, charge, 0.3f + 0.3f * Math.min(1.3f, charge), 4f + 5f * charge);
            }
        }
        float ce = gi0 + C * 0.5f, cn = gj0 + C * 0.5f;
        inc.versMonde(ce, cn, monde);
        if (vegetation != null) {
            float[] arbres = vegetation.arbresAutour(monde[0], monde[1], monde[2], C * 0.75 + 3.0);
            for (int a = 0; a + 3 < arbres.length; a += 4) planter(m, arbres[a], arbres[a + 1], arbres[a + 2], (int) arbres[a + 3]);
        }
        for (int a = 0; a + 4 < villages.nombreArbres; a += 5) {
            float[] p = villages.arbres;
            double dx = p[a] - monde[0], dy = p[a + 1] - monde[1], dz = p[a + 2] - monde[2];
            if (Math.sqrt(dx * dx + dy * dy + dz * dz) * Incendies.RAYON_METRES > C) continue;
            planter(m, p[a], p[a + 1], p[a + 2], (int) p[a + 3]);
        }
        for (int b = 0; b < villages.nombre; b++) {
            Batiments.Plan p = villages.plans[b];
            double r = p.rayon();
            double dx = villages.x[b] - monde[0], dy = villages.y[b] - monde[1], dz = villages.z[b] - monde[2];
            if (Math.sqrt(dx * dx + dy * dy + dz * dz) * Incendies.RAYON_METRES > C * 0.75 + r) continue;
            float be = inc.versEst(villages.x[b], villages.y[b], villages.z[b]), bn = inc.versNord(villages.x[b], villages.y[b], villages.z[b]);
            boolean brule = Incendies.combustible(p) && p.style != Batiments.PONTON;
            long cle = Incendies.cle(villages.x[b], villages.y[b], villages.z[b]);
            float facilite = 0.04f + 0.25f * Incendies.reception(p);
            for (int j = (int) Math.floor(bn - r) - gj0; j <= (int) Math.ceil(bn + r) - gj0; j++) {
                if (j < 0 || j >= C) continue;
                for (int i = (int) Math.floor(be - r) - gi0; i <= (int) Math.ceil(be + r) - gi0; i++) {
                    if (i < 0 || i >= C) continue;
                    if (!p.bloque(gi0 + i + 0.5 - be, gj0 + j + 0.5 - bn, 0.0)) continue;
                    int k = j * C + i;
                    if (!brule) {
                        definir(m, k, VIDE, 0f, 0f, 0f);
                        continue;
                    }
                    definir(m, k, BATI, 1f, facilite, 1f);
                    if (m.bati == null) m.bati = new long[C * C];
                    m.bati[k] = cle;
                }
            }
        }
        m.sale = true;
    }

    private static void definir(Morceau m, int k, byte sorte, float charge, float facilite, float duree) {
        m.sorte[k] = sorte;
        m.combustible[k] = charge;
        m.initial[k] = charge;
        m.facilite[k] = facilite;
        m.duree[k] = duree;
    }

    private void planter(Morceau m, float x, float y, float z, int code) {
        int type = code / Vegetation.VARIANTES;
        if (type == Vegetation.ROCHER) return;
        float e = inc.versEst(x, y, z), n = inc.versNord(x, y, z);
        boolean buisson = type == Vegetation.BUISSON;
        float r = buisson ? 0.9f : 1.7f;
        int gi0 = m.ci * C, gj0 = m.cj * C;
        for (int j = (int) Math.floor(n - r) - gj0; j <= (int) Math.ceil(n + r) - gj0; j++) {
            if (j < 0 || j >= C) continue;
            for (int i = (int) Math.floor(e - r) - gi0; i <= (int) Math.ceil(e + r) - gi0; i++) {
                if (i < 0 || i >= C) continue;
                float de = gi0 + i + 0.5f - e, dn = gj0 + j + 0.5f - n;
                if (de * de + dn * dn > r * r) continue;
                int k = j * C + i;
                if (m.sorte[k] == BATI) continue;
                if (m.sorte[k] == VIDE && m.combustible[k] == 0f) {
                    inc.versMonde(gi0 + i + 0.5f, gj0 + j + 0.5f, monde);
                    terrain.echantillonner(monde[0], monde[1], monde[2], 11, point);
                    if (point.altitude <= 0f || Personnage.profondeur(point) > 0f) continue;
                }
                if (buisson) {
                    if (m.sorte[k] != ARBRE) definir(m, k, BROUSSAILLE, 2f, 0.4f, 14f);
                } else {
                    definir(m, k, ARBRE, 6f, type == Vegetation.MORT ? 0.5f : 0.28f, type == Vegetation.CONIFERE ? 35f : 45f);
                }
            }
        }
    }

    public boolean allumer(Villages villages, float e, float n) {
        int gi = (int) Math.floor(e), gj = (int) Math.floor(n);
        Morceau m = morceau(gi, gj, villages);
        if (m == null) return false;
        int k = Math.floorMod(gj, C) * C + Math.floorMod(gi, C);
        if (m.etat[k] != INTACT || m.facilite[k] <= 0f) return false;
        embraser(m, k, gi, gj, villages);
        return true;
    }

    private void embraser(Morceau m, int k, int gi, int gj, Villages villages) {
        m.etat[k] = EN_FEU;
        m.temps[k] = 0f;
        m.sale = true;
        if (nombreFeu == feuI.length) {
            feuI = Arrays.copyOf(feuI, nombreFeu * 2);
            feuJ = Arrays.copyOf(feuJ, nombreFeu * 2);
        }
        feuI[nombreFeu] = gi;
        feuJ[nombreFeu] = gj;
        nombreFeu++;
        if (m.sorte[k] == BATI && m.bati != null && inc.foyer(m.bati[k]) == null) inc.allumerCle(villages, m.bati[k]);
    }

    public int etat(double x, double y, double z) {
        if (morceaux.isEmpty() || !inc.ancree) return -1;
        int gi = (int) Math.floor(inc.versEst(x, y, z)), gj = (int) Math.floor(inc.versNord(x, y, z));
        Morceau m = morceaux.get(cleMorceau(Math.floorDiv(gi, C), Math.floorDiv(gj, C)));
        if (m == null) return -1;
        return m.etat[Math.floorMod(gj, C) * C + Math.floorMod(gi, C)];
    }

    void pas(Villages villages, float dt, float ce, float cn) {
        if (nombreFeu == 0) return;
        float v = inc.vent(), ve = inc.ventEst, vn = inc.ventNord;
        float sec = 1f - 0.85f * inc.humide();
        float pluie = inc.pluie;
        for (Morceau m : morceaux.values()) {
            m.enFeu = 0;
            m.sommeE = 0f;
            m.sommeN = 0f;
        }
        int total = nombreFeu;
        for (int b = total - 1; b >= 0; b--) {
            int gi = feuI[b], gj = feuJ[b];
            Morceau m = morceau(gi, gj, null);
            int k = Math.floorMod(gj, C) * C + Math.floorMod(gi, C);
            if (m == null || m.etat[k] != EN_FEU) {
                retirer(b);
                continue;
            }
            m.temps[k] += dt;
            float force;
            boolean eteint;
            if (m.sorte[k] == BATI) {
                Incendies.Foyer f = m.bati == null ? null : inc.foyer(m.bati[k]);
                force = f != null && f.etat == Incendies.FEU ? Math.max(0.3f, f.intensite) : 0f;
                eteint = force <= 0f && m.temps[k] > 1f;
            } else {
                float reste = m.combustible[k] / m.initial[k];
                force = Math.min(1f, m.temps[k] / 1.2f) * (float) Math.sqrt(Math.max(0f, reste)) * (1f - 0.5f * pluie);
                m.combustible[k] -= m.initial[k] / m.duree[k] * dt * (1f + pluie);
                eteint = m.combustible[k] <= 0f;
            }
            m.intensite[k] = force;
            if (eteint) {
                m.etat[k] = CENDRE;
                m.fin[k] = inc.horloge;
                m.intensite[k] = 0f;
                m.sale = true;
                m.braisesJusqua = inc.horloge + 30f;
                brulees++;
                retirer(b);
                continue;
            }
            m.enFeu++;
            m.sommeE += gi + 0.5f;
            m.sommeN += gj + 0.5f;
            for (int[] d : VOISINS) {
                int ni = gi + d[0], nj = gj + d[1];
                Morceau mm = (Math.floorDiv(ni, C) == m.ci && Math.floorDiv(nj, C) == m.cj) ? m : morceau(ni, nj, villages);
                if (mm == null) continue;
                int kk = Math.floorMod(nj, C) * C + Math.floorMod(ni, C);
                if (mm.etat[kk] != INTACT || mm.facilite[kk] <= 0f) continue;
                float longueur = d[0] != 0 && d[1] != 0 ? 1.4142f : 1f;
                float cosinus = v > 0.1f ? (d[0] * ve + d[1] * vn) / (longueur * v) : 0f;
                float souffle = Math.max(0.06f, Math.min(10f, (float) Math.exp(0.3f * v * cosinus)));
                float taux = mm.facilite[kk] * force * souffle * sec / longueur;
                if (inc.hasard.nextFloat() < 1f - (float) Math.exp(-taux * dt)) embraser(mm, kk, ni, nj, villages);
            }
            if (Math.abs(gi - ce) > 500f || Math.abs(gj - cn) > 500f) continue;
            byte s = m.sorte[k];
            if (s == BATI) continue;
            float alt = altitude(m, gi, gj);
            float taux = (s == HERBE ? 0.05f : s == BROUSSAILLE ? 0.2f : 0.5f) * force * Math.max(0.15f, Math.min(1f, 600f / nombreFeu));
            if (inc.hasard.nextFloat() < taux * dt) inc.fumee(gi + inc.hasard.nextFloat(), gj + inc.hasard.nextFloat(), s == ARBRE ? 4f : 0.5f, alt, s == ARBRE ? 1.6f : 0.9f, s == HERBE ? 0.34f : 0.16f);
            if (s == ARBRE && inc.hasard.nextFloat() < 1.5f * force * dt) inc.etincelle(gi + 0.5f, gj + 0.5f, 5f, alt);
            if (s == ARBRE && inc.hasard.nextFloat() < 0.06f * force * (0.4f + v / 8f) * dt) inc.brandon(gi + 0.5f, gj + 0.5f, 6f, alt);
        }
    }

    private void retirer(int b) {
        nombreFeu--;
        feuI[b] = feuI[nombreFeu];
        feuJ[b] = feuJ[nombreFeu];
    }

    private static float altitude(Morceau m, float gi, float gj) {
        float u = (gi - m.ci * C) / C, w = (gj - m.cj * C) / C;
        float bas = m.coins[0] + (m.coins[1] - m.coins[0]) * u, haut = m.coins[2] + (m.coins[3] - m.coins[2]) * u;
        return bas + (haut - bas) * w;
    }

    private void preparerAtlas() {
        if (atlas != null) return;
        Pixmap p = new Pixmap(COULEURS, 1, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        float[][] teintes = { { 1f, 0.97f, 0.8f }, { 1f, 0.85f, 0.32f }, { 1f, 0.6f, 0.12f }, { 0.95f, 0.36f, 0.06f }, { 0.72f, 0.16f, 0.04f }, { 0.4f, 0.07f, 0.03f } };
        for (int c = 0; c < COULEURS; c++) p.drawPixel(c, 0, Color.rgba8888(teintes[c][0], teintes[c][1], teintes[c][2], 1f));
        atlas = new Texture(p);
        atlas.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        p.dispose();
    }

    public Texture atlas() {
        return atlas;
    }

    void cadrer() {
        visibles = 0;
        preparerAtlas();
        float horizontal = (float) Math.sqrt(inc.m00 * inc.m00 + inc.m01 * inc.m01);
        pixelsHorizontal = horizontal;
        derive = horizontal > 0f ? Math.max(-1.2f, Math.min(1.2f, 0.08f * (inc.m00 * inc.ventEst + inc.m01 * inc.ventNord) / horizontal)) : 0f;
        if (morceaux.isEmpty()) {
            flammes.mettreAJour();
            return;
        }
        int majs = 0;
        for (Morceau m : morceaux.values()) {
            if (m.braisesJusqua > inc.horloge && inc.horloge >= m.prochaineMaj) m.sale = true;
            if (m.sale && majs < 5) {
                peindre(m);
                majs++;
            }
        }
        int demande = 0;
        for (int b = 0; b < nombreFeu && visibles < VISIBLES_MAX; b++) {
            int gi = feuI[b], gj = feuJ[b];
            Morceau m = morceau(gi, gj, null);
            if (m == null) continue;
            int k = Math.floorMod(gj, C) * C + Math.floorMod(gi, C);
            byte s = m.sorte[k];
            float force = m.intensite[k];
            if (s == BATI || force < 0.05f) continue;
            float e = gi + 0.5f, n = gj + 0.5f;
            float x = inc.planX(e, n), y = inc.planY(e, n);
            if (x < -100 || x > inc.largeur + 100 || y < -200 || y > inc.hauteur + 400) continue;
            if (visibles == fx.length) {
                int t = fx.length * 2;
                fx = Arrays.copyOf(fx, t);
                fy = Arrays.copyOf(fy, t);
                fd = Arrays.copyOf(fd, t);
                fa = Arrays.copyOf(fa, t);
                fc = Arrays.copyOf(fc, t);
                fi = Arrays.copyOf(fi, t);
            }
            int classe = s == ARBRE ? 2 : s == BROUSSAILLE || m.initial[k] > 0.8f ? 1 : 0;
            fx[visibles] = x;
            fy[visibles] = y;
            fd[visibles] = inc.tuiles.decalage(altitude(m, e, n));
            fc[visibles] = classe;
            fi[visibles] = gi * 73856093 ^ gj * 19349663;
            fa[visibles] = Math.min(1f, force * 1.3f);
            demande += PIXELS[classe];
            visibles++;
        }
        reduction = demande > BUDGET ? BUDGET / (float) demande : 1f;
        flammes.mettreAJour();
    }

    public float echelleFlamme() {
        return Incendies.UNITE * inc.pixelsMetre / (float) Incendies.COS_CAMERA;
    }

    public void tracerFlamme(SpriteBatch b, int j, boolean lumiere, float alpha) {
        int c = fc[j];
        float force = fa[j];
        int nombre = Math.max(3, Math.round(PIXELS[c] * reduction * (0.4f + 0.6f * force)));
        flamme(b, fx[j], fy[j] + fd[j], LARGEUR_M[c] * pixelsHorizontal, HAUTEUR_M[c] * (0.45f + 0.55f * force) * inc.pixelsMetre, nombre, fi[j], lumiere, alpha);
    }

    void flamme(SpriteBatch b, float x0, float y0, float largeurPx, float hauteurPx, int nombre, int graine, boolean lumiere, float alpha) {
        if (atlas == null) return;
        float s = Math.max(1f, echelleFlamme());
        float t0 = inc.temps;
        b.setColor(lumiere ? 1f : 0f, lumiere ? 1f : 0f, lumiere ? 1f : 0f, alpha);
        for (int k = 0; k < nombre; k++) {
            float h1 = hache(graine, k, 1), h2 = hache(graine, k, 2), h3 = hache(graine, k, 3);
            float periode = 0.4f + 0.6f * h3;
            float t = t0 / periode + h1;
            t -= (float) Math.floor(t);
            int c = t < 0.1f ? 0 : t < 0.28f ? 1 : t < 0.5f ? 2 : t < 0.7f ? 3 : t < 0.86f ? 4 : t < 0.95f ? 5 : -1;
            if (c < 0) continue;
            float monte = t * hauteurPx * (0.5f + 0.5f * h3);
            float cote = (h2 - 0.5f) * largeurPx * (1f - 0.7f * t) + derive * monte + (float) Math.sin(t0 * 8f + k * 1.7f) * s * 1.5f * t;
            float px = Math.round((x0 + cote) / s) * s, py = Math.round((y0 + monte) / s) * s;
            b.draw(atlas, px, py, s, s, c, 0, 1, 1, false, false);
        }
    }

    void flammesLumineuses(SpriteBatch b, float alpha) {
        if (flammes.palette() == null) return;
        b.setColor(1f, 1f, 1f, alpha);
        for (int j = 0; j < flammes.bandes; j++) flammes.tracer(b, j);
    }

    void lumieres(SpriteBatch b, Texture halo, float jour) {
        for (Morceau m : morceaux.values()) {
            if (m.enFeu == 0) continue;
            float e = m.sommeE / m.enFeu, n = m.sommeN / m.enFeu;
            float x = inc.planX(e, n), y = inc.planY(e, n) + inc.tuiles.decalage(altitude(m, e, n));
            float r = (5f + (float) Math.sqrt(m.enFeu) * 2.2f) * inc.pixelsMetre / (float) Incendies.COS_CAMERA;
            if (x < -r || x > inc.largeur + r || y < -r || y > inc.hauteur + r) continue;
            float vacille = 0.85f + 0.15f * (float) Math.sin(inc.temps * 8f + m.ci * 3 + m.cj);
            b.setColor(1f, 0.5f, 0.15f, Math.min(0.45f, 0.04f * (float) Math.sqrt(m.enFeu)) * jour * vacille);
            b.draw(halo, x - r, y - r * inc.ecrasement, r * 2f, r * 2f * inc.ecrasement);
        }
    }

    private int etatGlobal(int gi, int gj) {
        Morceau m = morceaux.get(cleMorceau(Math.floorDiv(gi, C), Math.floorDiv(gj, C)));
        return m == null ? 0 : m.etat[Math.floorMod(gj, C) * C + Math.floorMod(gi, C)];
    }

    private void peindre(Morceau m) {
        m.sale = false;
        m.prochaineMaj = inc.horloge + 1f;
        int gi0 = m.ci * C, gj0 = m.cj * C, t = C + 2;
        for (int j = -1; j <= C; j++) {
            for (int i = -1; i <= C; i++) {
                int e;
                if (i >= 0 && i < C && j >= 0 && j < C) e = m.etat[j * C + i];
                else e = etatGlobal(gi0 + i, gj0 + j);
                valeurs[(j + 1) * t + i + 1] = e == INTACT ? 0f : 1f;
                feux[(j + 1) * t + i + 1] = e == EN_FEU ? 1f : 0f;
            }
        }
        if (m.image == null) {
            m.image = new Pixmap(C * PX, C * PX, Pixmap.Format.RGBA8888);
            m.image.setBlending(Pixmap.Blending.None);
        }
        Pixmap p = m.image;
        ByteBuffer tampon = p.getPixels().duplicate().order(ByteOrder.BIG_ENDIAN);
        int cote = C * PX;
        for (int py = 0; py < cote; py++) {
            float cn = (cote - 0.5f - py) / PX - 0.5f;
            int j0 = (int) Math.floor(cn);
            float wj = cn - j0;
            for (int px = 0; px < cote; px++) {
                float ce = (px + 0.5f) / PX - 0.5f;
                int i0 = (int) Math.floor(ce);
                float wi = ce - i0;
                int a = (j0 + 1) * t + i0 + 1;
                float vb = mix(valeurs, a, t, wi, wj), vf = mix(feux, a, t, wi, wj);
                int gx = gi0 * PX + px, gy = gj0 * PX + (cote - 1 - py);
                float h1 = hache(gx, gy, 11), h2 = hache(gx >> 1, gy >> 1, 13);
                float grain = (h2 - 0.5f) * 0.5f + (h1 - 0.5f) * 0.2f;
                int o = (py * cote + px) * 4;
                if (vb + grain <= 0.5f) {
                    tampon.putInt(o, 0);
                    continue;
                }
                if (vf + grain * 0.6f > 0.45f) {
                    tampon.putInt(o, h1 > 0.55f ? BRAISE_VIVE : h1 > 0.2f ? BRAISE : BRAISE_SOMBRE);
                    continue;
                }
                int ci = Math.max(0, Math.min(C - 1, Math.round(ce))), cj = Math.max(0, Math.min(C - 1, Math.round(cn)));
                float age = inc.horloge - m.fin[cj * C + ci];
                if (m.etat[cj * C + ci] == CENDRE && age < 30f && h1 > 1f - 0.14f * (1f - age / 30f)) {
                    tampon.putInt(o, TISON);
                    continue;
                }
                int g = (int) ((0.05f + 0.08f * h2 + 0.03f * h1) * 255f);
                tampon.putInt(o, (Math.min(255, g * 112 / 100) << 24) | (g << 16) | ((g * 88 / 100) << 8) | 219);
            }
        }
        if (m.texture == null) {
            m.texture = new Texture(p);
            m.texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        } else {
            m.texture.draw(p, 0, 0);
        }
    }

    private static float mix(float[] v, int a, int t, float wi, float wj) {
        float b = v[a] + (v[a + 1] - v[a]) * wi;
        float h = v[a + t] + (v[a + t + 1] - v[a + t]) * wi;
        return b + (h - b) * wj;
    }

    void dessinerSol() {
        if (morceaux.isEmpty() || inc.est == null) return;
        SpriteBatch b = inc.pinceau;
        b.getProjectionMatrix().setToOrtho2D(0f, 0f, inc.largeur, inc.hauteur);
        b.begin();
        float blanc = Color.WHITE.toFloatBits();
        for (Morceau m : morceaux.values()) {
            if (m.texture == null) continue;
            float e0 = m.ci * C, n0 = m.cj * C, e1 = e0 + C, n1 = n0 + C;
            float x0 = inc.planX(e0, n0), y0 = inc.planY(e0, n0) + inc.tuiles.decalage(m.coins[0]);
            float x1 = inc.planX(e0, n1), y1 = inc.planY(e0, n1) + inc.tuiles.decalage(m.coins[2]);
            float x2 = inc.planX(e1, n1), y2 = inc.planY(e1, n1) + inc.tuiles.decalage(m.coins[3]);
            float x3 = inc.planX(e1, n0), y3 = inc.planY(e1, n0) + inc.tuiles.decalage(m.coins[1]);
            float minX = Math.min(Math.min(x0, x1), Math.min(x2, x3)), maxX = Math.max(Math.max(x0, x1), Math.max(x2, x3));
            float minY = Math.min(Math.min(y0, y1), Math.min(y2, y3)), maxY = Math.max(Math.max(y0, y1), Math.max(y2, y3));
            if (maxX < 0 || minX > inc.largeur || maxY < 0 || minY > inc.hauteur) continue;
            sommet(0, x0, y0, blanc, 0f, 1f);
            sommet(1, x1, y1, blanc, 0f, 0f);
            sommet(2, x2, y2, blanc, 1f, 0f);
            sommet(3, x3, y3, blanc, 1f, 1f);
            b.draw(m.texture, sommets, 0, 20);
        }
        b.end();
    }

    private void sommet(int q, float x, float y, float couleur, float u, float v) {
        sommets[q * 5] = x;
        sommets[q * 5 + 1] = y;
        sommets[q * 5 + 2] = couleur;
        sommets[q * 5 + 3] = u;
        sommets[q * 5 + 4] = v;
    }

    void dispose() {
        vider();
        if (atlas != null) atlas.dispose();
        flammes.dispose();
    }
}
