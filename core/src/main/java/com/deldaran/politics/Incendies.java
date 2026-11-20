package com.deldaran.politics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.deldaran.politics.planet.Terrain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Incendies {
    static final double RAYON_METRES = Terrain.RAYON_KM * 1000.0;
    static final double COS_CAMERA = Math.cos(RenduTuiles.ANGLE_CAMERA);
    static final float PAS = 0.1f;
    private static final float PORTEE_CHALEUR = 30f, PORTEE_PARTICULES = 600f;
    private static final float REFROIDISSEMENT = 0.06f;
    private static final int PARTICULES_MAX = 12000, RAYONS = 16;
    static final float UNITE = (float) (Peintre.METRES_PIXEL * 2.0);

    public static final int FEU = 1, BRAISES = 2, RUINE = 3;
    private static final int FLAMME = 0, FUMEE = 1, ETINCELLE = 2, BRANDON = 3;
    private static final String[] ETATS = { "", "en feu", "braises", "ruine" };

    public static class Foyer {
        public long cle;
        public int etat = FEU;
        public float x, y, z, altitude;
        public Batiments.Plan plan;
        float e, n;
        float aire, hauteur, demi;
        float croissance, duree, seuil, reception, brandons;
        public float intensite = 0.03f, initial, combustible, braise, age;
        public float brule, effondre, lueur;
        float flammes, fumees, etincelles, envols;
        float baseX, baseY, planY;
        boolean vu;
        private Batiments.Plan ruine;

        public float reste() {
            return initial <= 0f ? 0f : combustible / initial;
        }

        public boolean abattu() {
            return etat != FEU || effondre > 0.6f;
        }

        public Batiments.Plan planRuine() {
            if (ruine == null) ruine = plan.ruiner();
            return ruine;
        }
    }

    public final Front front;
    private final Map<Long, Foyer> foyers = new HashMap<>();
    private final List<Foyer> actifs = new ArrayList<>();
    private final Map<Long, float[]> chaleurs = new HashMap<>();
    final Random hasard = new Random(7321);
    final double[] ancre = new double[3], ancreEst = new double[3], ancreNord = new double[3];
    boolean ancree;
    private float accumule;
    float horloge;
    public float vitesse = 1f, ventForce = -1f;
    public float ventEst, ventNord, pluie, mouille, orage;
    public int allumages, parBrandon, parChaleur, parFoudre;

    private final int[] sorte = new int[PARTICULES_MAX];
    private final Foyer[] source = new Foyer[PARTICULES_MAX];
    private final float[] pe = new float[PARTICULES_MAX], pn = new float[PARTICULES_MAX], ph = new float[PARTICULES_MAX], sol = new float[PARTICULES_MAX];
    private final float[] ve = new float[PARTICULES_MAX], vn = new float[PARTICULES_MAX], vh = new float[PARTICULES_MAX];
    private final float[] age = new float[PARTICULES_MAX], vie = new float[PARTICULES_MAX], taille = new float[PARTICULES_MAX], teinte = new float[PARTICULES_MAX];
    private int particules;

    private Texture disques, langues, halo;
    private final TextureRegion[] disque = new TextureRegion[RAYONS + 1], langue = new TextureRegion[RAYONS + 1];
    SpriteBatch pinceau;
    float m00, m01, m10, m11, pixelsMetre, origineX, origineY;
    double[] est, nord;
    double rayon;
    float ecrasement;
    int largeur, hauteur;
    RenduTuiles tuiles;
    float temps;

    public Incendies(Terrain terrain) {
        front = new Front(this, terrain);
    }

    public boolean ancre() {
        return ancree && est != null;
    }

    private final float[] coins = new float[20], coque = new float[40];

    void sourcesBatiments(Flammes flammes) {
        for (Foyer f : foyers.values()) {
            if (!f.vu) continue;
            boolean braises = f.etat == BRAISES;
            if (f.etat != FEU && !(braises && f.braise > 0.15f)) continue;
            if (f.etat == FEU && f.intensite < 0.02f) continue;
            Batiments.Plan p = f.plan;
            double c = Math.cos(p.angle), s = Math.sin(p.angle);
            float reste = 1f - f.effondre * 0.72f;
            float murs = (float) p.murs * reste, faite = f.hauteur * reste;
            if (braises) {
                murs = 0.6f;
                faite = 0.8f;
            }
            int n = 0;
            for (int k = 0; k < 4; k++) {
                double lx = ((k & 1) == 0 ? -0.5 : 0.5) * p.largeur, lz = ((k & 2) == 0 ? -0.5 : 0.5) * p.profondeur;
                float de = (float) (lx * c + lz * s), dn = (float) (lx * s - lz * c);
                float x = f.baseX + m00 * de + m01 * dn, y = f.baseY + m10 * de + m11 * dn;
                coins[n++] = x;
                coins[n++] = y;
                coins[n++] = x;
                coins[n++] = y + murs * pixelsMetre;
            }
            for (int k = 0; k < 2; k++) {
                double lx = (k == 0 ? -0.5 : 0.5) * p.largeur;
                float de = (float) (lx * c), dn = (float) (lx * s);
                coins[n++] = f.baseX + m00 * de + m01 * dn;
                coins[n++] = f.baseY + m10 * de + m11 * dn + faite * pixelsMetre;
            }
            int points = enveloppe(coins, n / 2, coque);
            float niveau = braises ? 1f : Math.min(1f, 0.12f + f.intensite * 1.3f);
            flammes.batiment(coque, points, f.baseY, niveau, braises ? f.braise : f.intensite, braises, (int) Math.floor(f.planY), (int) f.cle);
        }
    }

    private static int enveloppe(float[] p, int n, float[] sortie) {
        int depart = 0;
        for (int k = 1; k < n; k++) if (p[k * 2] < p[depart * 2] || (p[k * 2] == p[depart * 2] && p[k * 2 + 1] < p[depart * 2 + 1])) depart = k;
        int courant = depart, m = 0;
        do {
            sortie[m * 2] = p[courant * 2];
            sortie[m * 2 + 1] = p[courant * 2 + 1];
            m++;
            int suivant = (courant + 1) % n;
            for (int k = 0; k < n; k++) {
                float ax = p[suivant * 2] - p[courant * 2], ay = p[suivant * 2 + 1] - p[courant * 2 + 1];
                float bx = p[k * 2] - p[courant * 2], by = p[k * 2 + 1] - p[courant * 2 + 1];
                if (ax * by - ay * bx < 0f) suivant = k;
            }
            courant = suivant;
        } while (courant != depart && m < n);
        return m;
    }

    public boolean vide() {
        return foyers.isEmpty() && !front.actif();
    }

    public int nombre(int etat) {
        int n = 0;
        for (Foyer f : foyers.values()) if (f.etat == etat) n++;
        return n;
    }

    public int brandonsEnVol() {
        int n = 0;
        for (int i = 0; i < particules; i++) if (sorte[i] == BRANDON) n++;
        return n;
    }

    public int particules() {
        return particules;
    }

    public static long cle(float x, float y, float z) {
        long h = Float.floatToIntBits(x) * 0x9E3779B97F4A7C15L;
        h ^= Float.floatToIntBits(y) * 0xC2B2AE3D27D4EB4FL;
        h ^= Float.floatToIntBits(z) * 0x165667B19E3779F9L;
        h ^= h >>> 31;
        return h * 0xff51afd7ed558ccdL;
    }

    public Foyer foyer(Villages villages, int k) {
        if (foyers.isEmpty()) return null;
        return foyers.get(cle(villages.x[k], villages.y[k], villages.z[k]));
    }

    Foyer foyer(long cle) {
        return foyers.get(cle);
    }

    public void eteindre() {
        foyers.clear();
        actifs.clear();
        chaleurs.clear();
        front.vider();
        particules = 0;
        ancree = false;
    }

    static boolean combustible(Batiments.Plan p) {
        return p.style != Batiments.ARCHE && p.style != Batiments.PUITS && !p.ruine;
    }

    static float reception(Batiments.Plan p) {
        switch (p.style) {
            case Batiments.CHAUMIERE:
            case Batiments.LONGERE: return 0.9f;
            case Batiments.RONDINS: return 0.35f;
            case Batiments.COLOMBAGES: return 0.08f;
            case Batiments.PIERRE:
            case Batiments.HALLE: return 0.05f;
            default: return 0.4f;
        }
    }

    private void regler(Foyer f) {
        Batiments.Plan p = f.plan;
        float densite;
        switch (p.style) {
            case Batiments.CHAUMIERE:
            case Batiments.LONGERE:
                f.croissance = 0.12f; f.duree = 150f; f.seuil = 30f; f.brandons = 1f; densite = 1f;
                break;
            case Batiments.RONDINS:
                f.croissance = 0.06f; f.duree = 260f; f.seuil = 55f; f.brandons = 0.6f; densite = 1.4f;
                break;
            case Batiments.COLOMBAGES:
                f.croissance = 0.05f; f.duree = 240f; f.seuil = 70f; f.brandons = 0.5f; densite = 1f;
                break;
            case Batiments.PIERRE:
            case Batiments.HALLE:
                f.croissance = 0.035f; f.duree = 200f; f.seuil = 120f; f.brandons = 0.3f; densite = 0.5f;
                break;
            default:
                f.croissance = 0.1f; f.duree = 90f; f.seuil = 35f; f.brandons = 0.2f; densite = 0.6f;
                break;
        }
        f.reception = reception(p);
        boolean maison = p.style < Batiments.ARCHE;
        f.aire = (float) (p.largeur * p.profondeur * (p.etage ? 1.6 : 1.0)) * densite;
        f.hauteur = maison ? (float) (p.murs + p.profondeur * 0.5 * Math.tan(p.pente)) : (float) Math.max(0.8, p.murs);
        f.demi = (float) ((p.largeur + p.profondeur) * 0.25);
        f.initial = f.aire;
        f.combustible = f.aire;
    }

    private static float seuil(Batiments.Plan p) {
        switch (p.style) {
            case Batiments.CHAUMIERE:
            case Batiments.LONGERE: return 30f;
            case Batiments.RONDINS: return 55f;
            case Batiments.COLOMBAGES: return 70f;
            case Batiments.PIERRE:
            case Batiments.HALLE: return 120f;
            default: return 35f;
        }
    }

    void ancrer(double x, double y, double z) {
        if (ancree) return;
        double l = Math.sqrt(x * x + y * y + z * z);
        ancre[0] = x / l;
        ancre[1] = y / l;
        ancre[2] = z / l;
        double ex = -ancre[1], ey = ancre[0], el = Math.sqrt(ex * ex + ey * ey);
        ancreEst[0] = ex / el;
        ancreEst[1] = ey / el;
        ancreEst[2] = 0.0;
        ancreNord[0] = -ancre[2] * ancreEst[1];
        ancreNord[1] = ancre[2] * ancreEst[0];
        ancreNord[2] = ancre[0] * ancreEst[1] - ancre[1] * ancreEst[0];
        ancree = true;
    }

    float versEst(double x, double y, double z) {
        return (float) (((x - ancre[0]) * ancreEst[0] + (y - ancre[1]) * ancreEst[1] + (z - ancre[2]) * ancreEst[2]) * RAYON_METRES);
    }

    float versNord(double x, double y, double z) {
        return (float) (((x - ancre[0]) * ancreNord[0] + (y - ancre[1]) * ancreNord[1] + (z - ancre[2]) * ancreNord[2]) * RAYON_METRES);
    }

    void versMonde(double e, double n, double[] sortie) {
        double x = ancre[0] + (e * ancreEst[0] + n * ancreNord[0]) / RAYON_METRES;
        double y = ancre[1] + (e * ancreEst[1] + n * ancreNord[1]) / RAYON_METRES;
        double z = ancre[2] + (e * ancreEst[2] + n * ancreNord[2]) / RAYON_METRES;
        double l = Math.sqrt(x * x + y * y + z * z);
        sortie[0] = x / l;
        sortie[1] = y / l;
        sortie[2] = z / l;
    }

    public Foyer allumer(Villages villages, int k) {
        Batiments.Plan p = villages.plans[k];
        if (!combustible(p)) return null;
        long c = cle(villages.x[k], villages.y[k], villages.z[k]);
        if (foyers.containsKey(c)) return null;
        ancrer(villages.x[k], villages.y[k], villages.z[k]);
        Foyer f = new Foyer();
        f.cle = c;
        f.x = villages.x[k];
        f.y = villages.y[k];
        f.z = villages.z[k];
        f.altitude = villages.altitude[k];
        f.plan = p;
        f.e = versEst(f.x, f.y, f.z);
        f.n = versNord(f.x, f.y, f.z);
        regler(f);
        foyers.put(c, f);
        actifs.add(f);
        chaleurs.remove(c);
        allumages++;
        return f;
    }

    Foyer allumerCle(Villages villages, long c) {
        if (foyers.containsKey(c)) return null;
        for (int k = 0; k < villages.nombre; k++) if (cle(villages.x[k], villages.y[k], villages.z[k]) == c) return allumer(villages, k);
        return null;
    }

    public Foyer allumerPres(Villages villages, double x, double y, double z, double distanceMax) {
        int meilleur = -1;
        double mieux = distanceMax / RAYON_METRES;
        for (int k = 0; k < villages.nombre; k++) {
            if (!combustible(villages.plans[k]) || villages.plans[k].style >= Batiments.ARCHE) continue;
            double dx = villages.x[k] - x, dy = villages.y[k] - y, dz = villages.z[k] - z;
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz) - villages.plans[k].rayon() / RAYON_METRES;
            if (d < mieux && !foyers.containsKey(cle(villages.x[k], villages.y[k], villages.z[k]))) {
                mieux = d;
                meilleur = k;
            }
        }
        return meilleur < 0 ? null : allumer(villages, meilleur);
    }

    public boolean allumerSol(Villages villages, double x, double y, double z) {
        ancrer(x, y, z);
        float e = versEst(x, y, z), n = versNord(x, y, z);
        for (int essai = 0; essai < 40; essai++) {
            float r = essai * 0.6f, a = essai * 2.4f;
            if (front.allumer(villages, e + r * (float) Math.cos(a), n + r * (float) Math.sin(a))) {
                allumages++;
                return true;
            }
        }
        return false;
    }

    float vent() {
        return (float) Math.hypot(ventEst, ventNord);
    }

    public void avancer(float delta, Villages villages, Meteo.Locale locale, double cx, double cy, double cz) {
        temps = (temps + delta) % 3600f;
        if (ventForce >= 0f) {
            double v = Math.hypot(locale.ventEst, locale.ventNord);
            ventEst = v > 0.1 ? (float) (locale.ventEst / v * ventForce) : ventForce;
            ventNord = v > 0.1 ? (float) (locale.ventNord / v * ventForce) : 0f;
        } else {
            ventEst = locale.ventEst;
            ventNord = locale.ventNord;
        }
        pluie = locale.pluie;
        mouille = Math.max(locale.mouille, locale.neigeSol);
        orage = locale.orage;
        foudre(delta, villages, cx, cy, cz);
        if (vide() && particules == 0) return;
        accumule += Math.min(delta, 0.25f) * vitesse;
        while (accumule >= PAS) {
            accumule -= PAS;
            horloge += PAS;
            pas(villages, cx, cy, cz);
        }
    }

    private void foudre(float delta, Villages villages, double cx, double cy, double cz) {
        if (orage < 0.3f || villages.nombre == 0) return;
        if (hasard.nextFloat() > orage * 0.004f * delta * vitesse) return;
        int k = hasard.nextInt(villages.nombre);
        Batiments.Plan p = villages.plans[k];
        if (p.style >= Batiments.ARCHE) return;
        double dx = villages.x[k] - cx, dy = villages.y[k] - cy, dz = villages.z[k] - cz;
        if (Math.sqrt(dx * dx + dy * dy + dz * dz) * RAYON_METRES > 400.0) return;
        float chance = p.style == Batiments.CHAUMIERE || p.style == Batiments.LONGERE ? 0.6f : p.style == Batiments.RONDINS ? 0.3f : 0.1f;
        if (hasard.nextFloat() < chance * (1f - pluie * 0.5f) && allumer(villages, k) != null) parFoudre++;
    }

    float humide() {
        return Math.max(mouille, pluie);
    }

    private void pas(Villages villages, double cx, double cy, double cz) {
        float v = vent();
        float humide = humide();
        for (java.util.Iterator<Map.Entry<Long, float[]>> it = chaleurs.entrySet().iterator(); it.hasNext(); ) {
            float[] h = it.next().getValue();
            h[0] *= (float) Math.exp(-(REFROIDISSEMENT + pluie * 0.08f) * PAS);
            if (h[0] < 0.05f) it.remove();
        }
        float ce = ancree ? versEst(cx, cy, cz) : 0f, cn = ancree ? versNord(cx, cy, cz) : 0f;
        for (int a = actifs.size() - 1; a >= 0; a--) {
            Foyer f = actifs.get(a);
            f.age += PAS;
            if (f.etat == FEU) {
                float reste = f.reste();
                float plafond = Math.min(1f, reste / 0.35f);
                float pousse = f.croissance * (1f - 0.6f * pluie) * (f.intensite + 0.03f) * (1f - f.intensite / Math.max(plafond, 0.01f));
                f.intensite += pousse * PAS;
                f.intensite -= pluie * 0.05f * Math.max(0f, 1.1f - f.intensite) * PAS;
                f.intensite = Math.max(0f, Math.min(plafond, f.intensite));
                f.combustible = Math.max(0f, f.combustible - f.intensite * f.initial / f.duree * PAS);
                float consume = 1f - f.reste();
                f.brule = Math.min(1f, consume * 1.8f + f.intensite * 0.15f);
                f.effondre = lisse(0.45f, 0.95f, consume);
                f.lueur = f.intensite;
                if (f.intensite < 0.004f && f.age > 5f) {
                    f.etat = consume > 0.25f ? BRAISES : RUINE;
                    f.braise = consume > 0.25f ? 1f : 0f;
                    if (f.etat == RUINE) {
                        foyers.remove(f.cle);
                        actifs.remove(a);
                    }
                    continue;
                }
                if (f.reste() <= 0.01f) {
                    f.etat = BRAISES;
                    f.braise = 1f;
                    f.intensite = 0f;
                }
                rayonner(f, villages, v, humide);
                envoyer(f, v);
                embraserSol(f, villages, v);
            } else if (f.etat == BRAISES) {
                f.braise -= PAS / (240f * (1f + f.initial / 200f)) * (1f + pluie * 4f);
                f.lueur = Math.max(0f, f.braise) * 0.5f;
                if (f.braise <= 0f) {
                    f.braise = 0f;
                    f.lueur = 0f;
                    f.etat = RUINE;
                    actifs.remove(a);
                }
            }
            boolean proche = Math.abs(f.e - ce) < PORTEE_PARTICULES && Math.abs(f.n - cn) < PORTEE_PARTICULES;
            if (proche) emettre(f, v);
        }
        front.pas(villages, PAS, ce, cn);
        animer(villages, v, humide);
    }

    private void embraserSol(Foyer f, Villages villages, float v) {
        if (f.intensite < 0.15f) return;
        for (int k = 0; k < 2; k++) {
            float a = hasard.nextFloat() * 6.2831855f;
            float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
            float cosinus = v > 0.1f ? (ca * ventEst + sa * ventNord) / v : 0f;
            float souffle = Math.max(0.2f, Math.min(4f, (float) Math.exp(0.2f * v * cosinus)));
            float r = f.demi + hasard.nextFloat() * (1.5f + f.intensite * 2.5f) * souffle;
            if (hasard.nextFloat() < f.intensite * 0.25f * souffle) front.allumer(villages, f.e + ca * r, f.n + sa * r);
        }
    }

    private void rayonner(Foyer f, Villages villages, float v, float humide) {
        if (f.intensite < 0.05f) return;
        float puissance = f.intensite * f.aire;
        double portee = (PORTEE_CHALEUR + f.demi) / RAYON_METRES;
        for (int k = 0; k < villages.nombre; k++) {
            double dx = villages.x[k] - f.x, dy = villages.y[k] - f.y, dz = villages.z[k] - f.z;
            if (Math.abs(dx) > portee || Math.abs(dy) > portee || Math.abs(dz) > portee) continue;
            Batiments.Plan p = villages.plans[k];
            if (p == f.plan || !combustible(p)) continue;
            long c = cle(villages.x[k], villages.y[k], villages.z[k]);
            if (foyers.containsKey(c)) continue;
            float te = versEst(villages.x[k], villages.y[k], villages.z[k]) - f.e;
            float tn = versNord(villages.x[k], villages.y[k], villages.z[k]) - f.n;
            float centre = (float) Math.sqrt(te * te + tn * tn);
            float demiCible = (float) ((p.largeur + p.profondeur) * 0.25);
            float d = Math.max(0.5f, centre - f.demi - demiCible);
            if (d > PORTEE_CHALEUR) continue;
            float cosinus = centre > 0.01f && v > 0.1f ? (te * ventEst + tn * ventNord) / (centre * v) : 0f;
            float souffle = Math.max(0.3f, Math.min(5f, (float) Math.exp(0.15f * v * cosinus)));
            float flux = puissance * souffle / (d * d + 1f);
            float[] h = chaleurs.get(c);
            if (h == null) {
                if (flux < 0.05f) continue;
                h = new float[1];
                chaleurs.put(c, h);
            }
            h[0] += flux * PAS;
            if (h[0] > seuil(p) * (1f + 2.5f * humide + 2f * pluie) && allumer(villages, k) != null) parChaleur++;
        }
    }

    private void envoyer(Foyer f, float v) {
        float taux = f.brandons * f.intensite * f.intensite * f.aire / 50f * (0.4f + v / 8f) * 0.6f;
        f.envols += taux * PAS;
        while (f.envols >= 1f) {
            f.envols -= 1f;
            int i = nouvelle(BRANDON, f, f.altitude);
            if (i < 0) return;
            depart(f, i, f.hauteur * (1f - f.effondre * 0.6f));
            lancer(i, f.intensite);
        }
    }

    private void lancer(int i, float force) {
        ve[i] = ventEst * (0.5f + hasard.nextFloat() * 0.5f) + (float) hasard.nextGaussian();
        vn[i] = ventNord * (0.5f + hasard.nextFloat() * 0.5f) + (float) hasard.nextGaussian();
        vh[i] = 3f + hasard.nextFloat() * 4f * force;
        vie[i] = 3f + hasard.nextFloat() * 9f;
        taille[i] = 1f;
    }

    void brandon(float e, float n, float h, float altitude) {
        int i = nouvelle(BRANDON, null, altitude);
        if (i < 0) return;
        pe[i] = e;
        pn[i] = n;
        ph[i] = h;
        lancer(i, 0.7f);
    }

    void fumee(float e, float n, float h, float altitude, float grosseur, float sombre) {
        int i = nouvelle(FUMEE, null, altitude);
        if (i < 0) return;
        pe[i] = e;
        pn[i] = n;
        ph[i] = h;
        ve[i] = (float) hasard.nextGaussian() * 0.4f;
        vn[i] = (float) hasard.nextGaussian() * 0.4f;
        vh[i] = 1.5f + hasard.nextFloat() * 1.5f;
        vie[i] = 8f + hasard.nextFloat() * 7f;
        taille[i] = grosseur;
        teinte[i] = Math.min(0.75f, sombre + pluie * 0.4f);
    }

    void etincelle(float e, float n, float h, float altitude) {
        int i = nouvelle(ETINCELLE, null, altitude);
        if (i < 0) return;
        pe[i] = e;
        pn[i] = n;
        ph[i] = h;
        ve[i] = ventEst * 0.6f + (float) hasard.nextGaussian() * 1.5f;
        vn[i] = ventNord * 0.6f + (float) hasard.nextGaussian() * 1.5f;
        vh[i] = 3f + hasard.nextFloat() * 4f;
        vie[i] = 0.8f + hasard.nextFloat() * 1.4f;
        taille[i] = 1f;
    }

    private void emettre(Foyer f, float v) {
        float etendue = f.aire / 50f;
        float haut = f.hauteur * (1f - f.effondre * 0.72f);
        if (f.etat == FEU) {
            float echelle = Math.max(0.45f, Math.min(1.6f, (float) Math.sqrt(f.aire) / 7f));
            f.flammes += 0f;
            while (f.flammes >= 1f) {
                f.flammes -= 1f;
                int i = nouvelle(FLAMME, f, f.altitude);
                if (i < 0) break;
                float monte = hasard.nextFloat();
                depart(f, i, haut * (0.55f + 0.45f * (float) Math.sqrt(monte)) - 0.2f);
                ve[i] = ventEst * 0.45f + (float) hasard.nextGaussian() * 0.25f;
                vn[i] = ventNord * 0.45f + (float) hasard.nextGaussian() * 0.25f;
                vh[i] = 0.2f + hasard.nextFloat() * 0.5f;
                vie[i] = (0.8f + hasard.nextFloat() * 0.9f) * (0.5f + 0.5f * f.intensite);
                taille[i] = (1.0f + hasard.nextFloat() * 1.3f) * (0.35f + 0.65f * f.intensite) * echelle;
            }
            f.etincelles += f.intensite * f.intensite * Math.max(0.3f, etendue) * 14f * PAS;
            while (f.etincelles >= 1f) {
                f.etincelles -= 1f;
                int i = nouvelle(ETINCELLE, f, f.altitude);
                if (i < 0) break;
                depart(f, i, haut);
                ve[i] = ventEst * 0.6f + (float) hasard.nextGaussian() * 1.5f;
                vn[i] = ventNord * 0.6f + (float) hasard.nextGaussian() * 1.5f;
                vh[i] = 4f + hasard.nextFloat() * 5f;
                vie[i] = 0.8f + hasard.nextFloat() * 1.4f;
                taille[i] = 1f;
            }
        }
        float fumee = f.etat == FEU ? 0.25f + f.intensite * 0.9f : Math.max(0f, f.braise) * 0.35f;
        f.fumees += fumee * Math.max(0.35f, etendue) * 4f * PAS;
        while (f.fumees >= 1f) {
            f.fumees -= 1f;
            int i = nouvelle(FUMEE, f, f.altitude);
            if (i < 0) break;
            depart(f, i, f.etat == FEU ? haut : 0.4f);
            ve[i] = (float) hasard.nextGaussian() * 0.4f;
            vn[i] = (float) hasard.nextGaussian() * 0.4f;
            vh[i] = f.etat == FEU ? 2f + f.intensite * 2.5f : 0.8f + f.braise;
            vie[i] = 9f + hasard.nextFloat() * 7f;
            taille[i] = f.etat == FEU ? 1.2f + f.intensite : 0.8f;
            float sombre = f.etat == FEU ? 0.1f + (1f - f.intensite) * 0.2f : 0.38f;
            teinte[i] = Math.min(0.75f, sombre + pluie * 0.4f + (f.plan.style == Batiments.CHAUMIERE || f.plan.style == Batiments.LONGERE ? 0.08f : 0f));
        }
    }

    private int nouvelle(int s, Foyer f, float altitude) {
        if (particules >= PARTICULES_MAX) return -1;
        int i = particules++;
        sorte[i] = s;
        source[i] = f;
        sol[i] = altitude;
        age[i] = 0f;
        teinte[i] = hasard.nextFloat();
        return i;
    }

    private void depart(Foyer f, int i, float h) {
        double c = Math.cos(f.plan.angle), s = Math.sin(f.plan.angle);
        double lx = (hasard.nextFloat() - 0.5f) * f.plan.largeur * 0.9, lz = (hasard.nextFloat() - 0.5f) * f.plan.profondeur * 0.9;
        pe[i] = f.e + (float) (lx * c + lz * s);
        pn[i] = f.n + (float) (lx * s - lz * c);
        ph[i] = Math.max(0.1f, h);
    }

    private void animer(Villages villages, float v, float humide) {
        for (int i = particules - 1; i >= 0; i--) {
            age[i] += PAS;
            boolean mort = age[i] >= vie[i];
            switch (sorte[i]) {
                case FLAMME:
                    vh[i] += 0.3f * PAS;
                    break;
                case FUMEE: {
                    float monte = (float) Math.exp(-age[i] / 5f);
                    vh[i] = Math.max(0.3f, vh[i] * (1f - 0.12f * PAS)) * (0.4f + 0.6f * monte) + 0.2f;
                    float porte = 1f + ph[i] / 25f;
                    ve[i] += (ventEst * porte - ve[i]) * 0.5f * PAS;
                    vn[i] += (ventNord * porte - vn[i]) * 0.5f * PAS;
                    taille[i] += (0.32f + v * 0.03f) * PAS;
                    break;
                }
                case ETINCELLE:
                    vh[i] -= 3f * PAS;
                    ve[i] += (ventEst - ve[i]) * 0.3f * PAS;
                    vn[i] += (ventNord - vn[i]) * 0.3f * PAS;
                    break;
                default:
                    vh[i] = Math.max(-2.5f, vh[i] - 2.5f * PAS);
                    ve[i] += (ventEst - ve[i]) * 0.4f * PAS + (float) hasard.nextGaussian() * 0.3f * PAS;
                    vn[i] += (ventNord - vn[i]) * 0.4f * PAS + (float) hasard.nextGaussian() * 0.3f * PAS;
                    break;
            }
            pe[i] += ve[i] * PAS;
            pn[i] += vn[i] * PAS;
            ph[i] += vh[i] * PAS;
            if (sorte[i] == BRANDON && !mort) {
                Foyer f = source[i];
                if (ph[i] <= 0f) {
                    mort = true;
                    atterrir(villages, i, humide);
                } else if (ph[i] < 6f) {
                    int k = toucher(villages, pe[i], pn[i], ph[i], f);
                    if (k >= 0) {
                        mort = true;
                        if (hasard.nextFloat() < reception(villages.plans[k]) * 0.3f * (1f - 0.9f * humide) && allumer(villages, k) != null) parBrandon++;
                    }
                }
            }
            if (sorte[i] == ETINCELLE && ph[i] < 0f) mort = true;
            if (mort) retirer(i);
        }
    }

    private void atterrir(Villages villages, int i, float humide) {
        int k = toucher(villages, pe[i], pn[i], 0f, null);
        if (k >= 0) {
            if (hasard.nextFloat() < reception(villages.plans[k]) * 0.15f * (1f - 0.9f * humide) && allumer(villages, k) != null) parBrandon++;
            return;
        }
        if (hasard.nextFloat() < 0.4f * (1f - 0.9f * humide) && front.allumer(villages, pe[i], pn[i])) parBrandon++;
    }

    private int toucher(Villages villages, float e, float n, float h, Foyer depart) {
        double px = ancre[0] + (e * ancreEst[0] + n * ancreNord[0]) / RAYON_METRES;
        double py = ancre[1] + (e * ancreEst[1] + n * ancreNord[1]) / RAYON_METRES;
        double pz = ancre[2] + (e * ancreEst[2] + n * ancreNord[2]) / RAYON_METRES;
        double portee = 14.0 / RAYON_METRES;
        for (int k = 0; k < villages.nombre; k++) {
            double dx = px - villages.x[k], dy = py - villages.y[k], dz = pz - villages.z[k];
            if (Math.abs(dx) > portee || Math.abs(dy) > portee || Math.abs(dz) > portee) continue;
            Batiments.Plan p = villages.plans[k];
            if ((depart != null && p == depart.plan) || !combustible(p)) continue;
            float te = e - versEst(villages.x[k], villages.y[k], villages.z[k]);
            float tn = n - versNord(villages.x[k], villages.y[k], villages.z[k]);
            if (h > p.murs + p.profondeur * 0.5 * Math.tan(p.pente) + 0.5) continue;
            if (p.bloque(te, tn, 0.1)) return k;
        }
        return -1;
    }

    private void retirer(int i) {
        int d = --particules;
        sorte[i] = sorte[d];
        source[i] = source[d];
        pe[i] = pe[d];
        pn[i] = pn[d];
        ph[i] = ph[d];
        sol[i] = sol[d];
        ve[i] = ve[d];
        vn[i] = vn[d];
        vh[i] = vh[d];
        age[i] = age[d];
        vie[i] = vie[d];
        taille[i] = taille[d];
        teinte[i] = teinte[d];
        source[d] = null;
    }

    static float lisse(float a, float b, float x) {
        float t = Math.max(0f, Math.min(1f, (x - a) / (b - a)));
        return t * t * (3f - 2f * t);
    }

    void preparerTextures() {
        if (disques != null) return;
        int l = 0;
        for (int r = 1; r <= RAYONS; r++) l += r * 2 + 1;
        Pixmap p = new Pixmap(l, RAYONS * 2 + 1, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        p.setColor(0f, 0f, 0f, 0f);
        p.fill();
        int x0 = 0;
        int[] debut = new int[RAYONS + 1];
        for (int r = 1; r <= RAYONS; r++) {
            debut[r] = x0;
            for (int j = -r; j <= r; j++) {
                for (int i = -r; i <= r; i++) {
                    double d = Math.sqrt(i * i + j * j) / (r + 0.35);
                    if (d > 1.0) continue;
                    p.drawPixel(x0 + r + i, r + j, Color.rgba8888(1f, 1f, 1f, 1f));
                }
            }
            x0 += r * 2 + 1;
        }
        disques = new Texture(p);
        disques.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        p.dispose();
        for (int r = 1; r <= RAYONS; r++) disque[r] = new TextureRegion(disques, debut[r], 0, r * 2 + 1, r * 2 + 1);
        Pixmap f = new Pixmap(l, RAYONS * 4 + 1, Pixmap.Format.RGBA8888);
        f.setBlending(Pixmap.Blending.None);
        f.setColor(0f, 0f, 0f, 0f);
        f.fill();
        for (int r = 1; r <= RAYONS; r++) {
            int h = r * 4 + 1;
            for (int j = 0; j < h; j++) {
                double demi;
                if (j <= r) demi = Math.sqrt(Math.max(0.0, (r + 0.35) * (r + 0.35) - (r - j) * (r - j)));
                else demi = (r + 0.35) * Math.pow(1.0 - (j - r) / (double) (h - r), 1.3);
                for (int i = -r; i <= r; i++) if (Math.abs(i) <= demi) f.drawPixel(debut[r] + r + i, h - 1 - j, Color.rgba8888(1f, 1f, 1f, 1f));
            }
        }
        langues = new Texture(f);
        langues.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        f.dispose();
        for (int r = 1; r <= RAYONS; r++) langue[r] = new TextureRegion(langues, debut[r], 0, r * 2 + 1, r * 4 + 1);
        int t = 64;
        Pixmap q = new Pixmap(t, t, Pixmap.Format.RGBA8888);
        q.setBlending(Pixmap.Blending.None);
        for (int j = 0; j < t; j++) {
            for (int i = 0; i < t; i++) {
                double dx = (i + 0.5) / t * 2.0 - 1.0, dy = (j + 0.5) / t * 2.0 - 1.0;
                double d = Math.sqrt(dx * dx + dy * dy);
                float a = (float) Math.max(0.0, 1.0 - d);
                q.drawPixel(i, j, Color.rgba8888(1f, 1f, 1f, a * a));
            }
        }
        halo = new Texture(q);
        halo.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        q.dispose();
        pinceau = new SpriteBatch(6000);
    }

    public void masquer() {
        est = null;
    }

    public void cadrer(double[] est, double[] nord, double rayon, float ecrasement, int largeur, int hauteur, double metresEcran, RenduTuiles tuiles) {
        if (!ancree) return;
        preparerTextures();
        this.est = est;
        this.nord = nord;
        this.rayon = rayon;
        this.ecrasement = ecrasement;
        this.largeur = largeur;
        this.hauteur = hauteur;
        this.tuiles = tuiles;
        float k = (float) (rayon / RAYON_METRES);
        m00 = (float) (ancreEst[0] * est[0] + ancreEst[1] * est[1] + ancreEst[2] * est[2]) * k;
        m01 = (float) (ancreNord[0] * est[0] + ancreNord[1] * est[1] + ancreNord[2] * est[2]) * k;
        m10 = (float) (ancreEst[0] * nord[0] + ancreEst[1] * nord[1] + ancreEst[2] * nord[2]) * k * ecrasement;
        m11 = (float) (ancreNord[0] * nord[0] + ancreNord[1] * nord[1] + ancreNord[2] * nord[2]) * k * ecrasement;
        origineX = (float) (largeur / 2.0 + (ancre[0] * est[0] + ancre[1] * est[1] + ancre[2] * est[2]) * rayon);
        origineY = (float) (hauteur / 2.0 + (ancre[0] * nord[0] + ancre[1] * nord[1] + ancre[2] * nord[2]) * rayon * ecrasement);
        pixelsMetre = (float) (COS_CAMERA / metresEcran);
        for (Foyer f : foyers.values()) {
            f.vu = false;
            if (f.etat == RUINE) continue;
            f.baseX = (float) (largeur / 2.0 + (f.x * est[0] + f.y * est[1] + f.z * est[2]) * rayon);
            f.planY = (float) (hauteur / 2.0 + (f.x * nord[0] + f.y * nord[1] + f.z * nord[2]) * rayon * ecrasement);
            f.baseY = f.planY + tuiles.decalage(f.x, f.y, f.z, f.altitude);
            f.vu = f.baseX > -800 && f.baseX < largeur + 800 && f.baseY > -800 && f.baseY < hauteur + 1600;
        }
        front.cadrer();
    }

    float planX(float e, float n) {
        return origineX + m00 * e + m01 * n;
    }

    float planY(float e, float n) {
        return origineY + m10 * e + m11 * n;
    }

    private float ecranX(int i) {
        return planX(pe[i], pn[i]);
    }

    private float ecranY(int i) {
        return planY(pe[i], pn[i]) + tuiles.decalage(sol[i]) + ph[i] * pixelsMetre;
    }

    private boolean visible(float x, float y) {
        return x > -300 && x < largeur + 300 && y > -300 && y < hauteur + 600;
    }

    private void tache(SpriteBatch b, float x, float y, float rayonMetres) {
        float unite = UNITE * pixelsMetre / (float) COS_CAMERA;
        int r = Math.max(1, Math.round(rayonMetres / UNITE));
        float cote = (r * 2 + 1) * unite;
        if (r > RAYONS) r = RAYONS;
        b.draw(disque[r], Math.round(x - cote / 2f), Math.round(y - cote / 2f), cote, cote);
    }

    private void flamme(SpriteBatch b, float x, float y, float rayonMetres) {
        float unite = UNITE * pixelsMetre / (float) COS_CAMERA;
        int r = Math.max(1, Math.round(rayonMetres / UNITE));
        float l = (r * 2 + 1) * unite, h = (r * 4 + 1) * unite;
        if (r > RAYONS) r = RAYONS;
        b.draw(langue[r], Math.round(x - l / 2f), Math.round(y - l / 2f), l, h);
    }

    private void couleurFlamme(SpriteBatch b, float t, float alpha) {
        if (t < 0.3f) b.setColor(1f, 0.56f, 0.12f, alpha);
        else if (t < 0.65f) b.setColor(0.9f, 0.3f, 0.06f, alpha);
        else b.setColor(0.5f, 0.1f, 0.04f, alpha * 0.8f);
    }

    private boolean garder(int i, Foyer f) {
        if (sorte[i] != FLAMME) return false;
        return f != null ? source[i] == f : source[i] != null && source[i].vu;
    }

    private void brasier(SpriteBatch b, Foyer f, boolean lumiere, float alpha) {
        if (f.etat != FEU || f.intensite < 0.03f) return;
        float haut = f.hauteur * (1f - f.effondre * 0.72f);
        int colonnes = Math.max(3, Math.min(24, Math.round(f.aire / 4f)));
        double c = Math.cos(f.plan.angle), s = Math.sin(f.plan.angle);
        float horizontal = (float) Math.sqrt(m00 * m00 + m01 * m01);
        int graine = (int) f.cle;
        for (int q = 0; q < colonnes; q++) {
            float allume = Front.hache(graine, q, 7);
            if (allume > f.intensite * 1.4f + 0.1f) continue;
            double lx = (Front.hache(graine, q, 8) - 0.5) * f.plan.largeur * 0.9, lz = (Front.hache(graine, q, 9) - 0.5) * f.plan.profondeur * 0.9;
            float de = (float) (lx * c + lz * s), dn = (float) (lx * s - lz * c);
            float x = f.baseX + m00 * de + m01 * dn;
            float y = f.baseY + m10 * de + m11 * dn + haut * (0.7f + 0.3f * Front.hache(graine, q, 10)) * pixelsMetre;
            float force = f.intensite;
            front.flamme(b, x, y, 1.8f * horizontal, (1.2f + 3.6f * force) * pixelsMetre, Math.round(20f + 60f * force), graine * 31 + q, lumiere, alpha);
        }
    }

    public void flammes(SpriteBatch b, Foyer f) {
        if (!ancree || est == null) return;
        brasier(b, f, false, 1f);
    }

    public void dessinerFumee(float nuit) {
        if (!ancree || particules == 0 || est == null) return;
        pinceau.getProjectionMatrix().setToOrtho2D(0f, 0f, largeur, hauteur);
        pinceau.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        pinceau.begin();
        for (int i = 0; i < particules; i++) {
            if (sorte[i] != FUMEE) continue;
            float x = ecranX(i), y = ecranY(i);
            if (!visible(x, y)) continue;
            float t = age[i] / vie[i];
            float entree = Math.min(1f, age[i] / 0.8f);
            float a = 0.38f * entree * (float) Math.pow(1f - t, 1.1f) * (1f - 0.55f * nuit);
            float g = (teinte[i] + t * 0.22f) * (1f - 0.5f * nuit);
            Foyer f = source[i];
            float braise = f != null && f.etat == FEU ? f.intensite * Math.max(0f, 1f - ph[i] / (f.hauteur + 6f)) : Math.max(0f, 1f - ph[i] / 4f) * 0.5f;
            pinceau.setColor(Math.min(1f, g + braise * 0.45f), g + braise * 0.15f, g * 1.02f, a);
            tache(pinceau, x, y, taille[i] * 0.5f);
        }
        pinceau.end();
    }

    public void dessinerLumieres(float nuit) {
        if (!ancree || est == null) return;
        pinceau.getProjectionMatrix().setToOrtho2D(0f, 0f, largeur, hauteur);
        pinceau.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        pinceau.begin();
        float jour = 0.35f + 0.65f * nuit;
        for (Foyer f : foyers.values()) {
            if (!f.vu || f.lueur <= 0.01f) continue;
            float vacille = 0.85f + 0.15f * (float) Math.sin(temps * 9.0 + f.cle % 17) * (float) Math.sin(temps * 5.3 + f.cle % 7);
            float r = (6f + f.demi * 2.2f) * (0.4f + 0.6f * f.lueur) * pixelsMetre / (float) COS_CAMERA;
            float y = f.baseY + f.hauteur * 0.4f * pixelsMetre;
            pinceau.setColor(1f, 0.48f, 0.14f, 0.32f * f.lueur * jour * vacille);
            pinceau.draw(halo, f.baseX - r, y - r * ecrasement, r * 2f, r * 2f * ecrasement);
            float r2 = r * 0.45f;
            pinceau.setColor(1f, 0.7f, 0.3f, 0.35f * f.lueur * jour * vacille);
            pinceau.draw(halo, f.baseX - r2, y - r2, r2 * 2f, r2 * 2f);
        }
        front.lumieres(pinceau, halo, jour);
        if (nuit > 0.05f) {
            front.flammesLumineuses(pinceau, 0.45f * nuit);
        }
        for (int i = 0; i < particules; i++) {
            if (sorte[i] != ETINCELLE && sorte[i] != BRANDON) continue;
            float x = ecranX(i), y = ecranY(i);
            if (!visible(x, y)) continue;
            float t = age[i] / vie[i];
            float clignote = 0.6f + 0.4f * (float) Math.sin(temps * 20f + teinte[i] * 40f);
            float a = (sorte[i] == BRANDON ? 1f : 1f - t) * clignote;
            if (sorte[i] == BRANDON) pinceau.setColor(1f, 0.55f, 0.15f, a);
            else pinceau.setColor(1f, 0.8f, 0.35f, a);
            float u = UNITE * pixelsMetre / (float) COS_CAMERA * (sorte[i] == BRANDON ? 1.5f : 1f);
            pinceau.draw(disque[1], Math.round(x - u), Math.round(y - u), u * 2f, u * 2f);
        }
        pinceau.end();
        pinceau.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    public String resume() {
        return String.format("%d en feu, %d braises, %d ruines | front : %d cases en feu, %d m2 brules | %d brandons", nombre(FEU), nombre(BRAISES), nombre(RUINE), front.enFeu(), front.brulees(), brandonsEnVol());
    }

    public void dispose() {
        if (disques != null) disques.dispose();
        if (langues != null) langues.dispose();
        if (halo != null) halo.dispose();
        if (pinceau != null) pinceau.dispose();
        front.dispose();
    }
}
