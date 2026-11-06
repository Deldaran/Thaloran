package com.deldaran.politics.planet;

import com.badlogic.gdx.utils.IntArray;

public class Rivieres {
    private static final float DEBIT_MIN = 1.5f;
    private static final float DEBIT_VISIBLE = 10f;
    private static final int NIVEAU_TOUTES_VISIBLES = 3;
    private static final int MORCEAUX = 8;
    private static final double MEANDRE = 0.25;

    private static final float VALLEE_BASE_KM = 4f * (float) Terrain.ECHELLE;
    private static final float VALLEE_PAR_DEBIT = 1.2f * (float) Terrain.ECHELLE;
    private static final float VALLEE_MAX_KM = 30f * (float) Terrain.ECHELLE;
    private static final float ELARGISSEMENT_KM = 25f * (float) Terrain.ECHELLE;
    private static final float ELARGISSEMENT_MAX_KM = 25f * (float) Terrain.ECHELLE;
    private static final float LARGEUR_PAR_DEBIT = 0.1f * (float) Terrain.ECHELLE;


    private static final int COTE_SEAUX = 256;
    private static final double TAILLE_SEAU_KM = 39.0 * Terrain.ECHELLE;

    private static final int OCTAVES_SINUOSITE = 5;
    private static final double LONGUEUR_SINUOSITE_KM = 30.0 * Terrain.ECHELLE;
    private static final double AMPLITUDE_SINUOSITE_KM = 1.5 * Terrain.ECHELLE;


    private final int graine;
    private int nombre = 0;
    private double[] ax, ay, az, bx, by, bz, inverse;
    private float[] hautA, hautB, debit, vallee, largeur;
    private final int[][] seaux = new int[Cube.FACES * COTE_SEAUX * COTE_SEAUX][];

    public Rivieres(Monde monde) {
        graine = (int) monde.seed + 200;
        Sphere s = monde.sphere;
        int[] amont = trouverAmonts(monde);

        int actives = 0;
        for (int i = 0; i < s.nombre; i++) {
            if (estActive(monde, i)) actives++;
        }
        reserver(actives * MORCEAUX);
        for (int i = 0; i < s.nombre; i++) {
            if (estActive(monde, i)) tracer(monde, i, amont[i]);
        }
        ranger();
    }

    private boolean estActive(Monde monde, int i) {
        return !monde.estSousLEau(i) && monde.debit[i] >= DEBIT_MIN && monde.receveur[i] >= 0;
    }

    private int[] trouverAmonts(Monde monde) {
        int[] amont = new int[monde.sphere.nombre];
        java.util.Arrays.fill(amont, -1);
        for (int i = 0; i < amont.length; i++) {
            if (!estActive(monde, i)) continue;
            int r = monde.receveur[i];
            if (amont[r] == -1 || monde.debit[i] > monde.debit[amont[r]]) amont[r] = i;
        }
        return amont;
    }

    private void reserver(int taille) {
        ax = new double[taille]; ay = new double[taille]; az = new double[taille];
        bx = new double[taille]; by = new double[taille]; bz = new double[taille];
        inverse = new double[taille];
        hautA = new float[taille]; hautB = new float[taille];
        debit = new float[taille]; vallee = new float[taille]; largeur = new float[taille];
    }

    private void tracer(Monde monde, int i, int amont) {
        Sphere s = monde.sphere;
        int r = monde.receveur[i];
        int suivant = monde.estSousLEau(r) || monde.receveur[r] < 0 ? r : monde.receveur[r];
        int avant = amont < 0 ? i : amont;

        float hA = monde.surfaceEau[i] - monde.niveauMer;
        float hB = Math.max(0f, monde.surfaceEau[r] - monde.niveauMer);
        float q = monde.debit[i];
        float racine = (float) Math.sqrt(q);
        float valleeKm = Math.min(VALLEE_MAX_KM, VALLEE_BASE_KM + VALLEE_PAR_DEBIT * racine);

        double[] precedent = new double[3];
        double[] courant = new double[3];
        point(s, avant, i, r, suivant, 0.0, precedent);
        for (int k = 1; k <= MORCEAUX; k++) {
            double t = (double) k / MORCEAUX;
            point(s, avant, i, r, suivant, t, courant);
            ajouterMorceau(precedent, courant, hA + (hB - hA) * (float) ((k - 1.0) / MORCEAUX), hA + (hB - hA) * (float) t,
                q, valleeKm, LARGEUR_PAR_DEBIT * racine);
            System.arraycopy(courant, 0, precedent, 0, 3);
        }
    }

    private void point(Sphere s, int z, int a, int b, int c, double t, double[] sortie) {
        double t2 = t * t, t3 = t2 * t;
        double wz = -0.5 * t3 + t2 - 0.5 * t;
        double wa = 1.5 * t3 - 2.5 * t2 + 1.0;
        double wb = -1.5 * t3 + 2.0 * t2 + 0.5 * t;
        double wc = 0.5 * t3 - 0.5 * t2;
        double x = wz * s.x[z] + wa * s.x[a] + wb * s.x[b] + wc * s.x[c];
        double y = wz * s.y[z] + wa * s.y[a] + wb * s.y[b] + wc * s.y[c];
        double w = wz * s.z[z] + wa * s.z[a] + wb * s.z[b] + wc * s.z[c];

        double dx = s.x[b] - s.x[a], dy = s.y[b] - s.y[a], dz = s.z[b] - s.z[a];
        double longueur = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double px = dy * w - dz * y, py = dz * x - dx * w, pz = dx * y - dy * x;
        double norme = Math.max(1e-12, Math.sqrt(px * px + py * py + pz * pz));
        double ecart = MEANDRE * longueur * Math.sin(Math.PI * t) * Bruit.perlin(x * 60.0, y * 60.0, w * 60.0, graine) * 2.0;
        x += px / norme * ecart;
        y += py / norme * ecart;
        w += pz / norme * ecart;

        double total = Math.sqrt(x * x + y * y + w * w);
        sortie[0] = x / total;
        sortie[1] = y / total;
        sortie[2] = w / total;
    }

    private void ajouterMorceau(double[] a, double[] b, float hA, float hB, float q, float valleeKm, float largeurKm) {
        int n = nombre++;
        ax[n] = a[0]; ay[n] = a[1]; az[n] = a[2];
        bx[n] = b[0]; by[n] = b[1]; bz[n] = b[2];
        double vx = b[0] - a[0], vy = b[1] - a[1], vz = b[2] - a[2];
        inverse[n] = 1.0 / Math.max(1e-18, vx * vx + vy * vy + vz * vz);
        hautA[n] = hA;
        hautB[n] = hB;
        debit[n] = q;
        vallee[n] = valleeKm;
        largeur[n] = largeurKm;
    }

    private static int seau(double x, double y, double z) {
        int face = Cube.face(x, y, z);
        int cx = (int) ((Cube.coordonneeA(face, x, y, z) + 1.0) * 0.5 * COTE_SEAUX);
        int cy = (int) ((Cube.coordonneeB(face, x, y, z) + 1.0) * 0.5 * COTE_SEAUX);
        cx = Math.max(0, Math.min(COTE_SEAUX - 1, cx));
        cy = Math.max(0, Math.min(COTE_SEAUX - 1, cy));
        return (face * COTE_SEAUX + cy) * COTE_SEAUX + cx;
    }

    private void ranger() {
        IntArray[] listes = new IntArray[seaux.length];
        int[] dernier = new int[seaux.length];
        java.util.Arrays.fill(dernier, -1);
        double pas = TAILLE_SEAU_KM * 0.5 / Terrain.RAYON_KM;

        for (int n = 0; n < nombre; n++) {
            double mx = (ax[n] + bx[n]) * 0.5, my = (ay[n] + by[n]) * 0.5, mz = (az[n] + bz[n]) * 0.5;
            double norme = Math.sqrt(mx * mx + my * my + mz * mz);
            mx /= norme; my /= norme; mz /= norme;

            double ux = -my, uy = mx, uz = 0.0;
            double longueur = Math.max(1e-9, Math.sqrt(ux * ux + uy * uy));
            ux /= longueur; uy /= longueur;
            double wx = my * uz - mz * uy, wy = mz * ux - mx * uz, wz = mx * uy - my * ux;

            double demi = Math.sqrt(1.0 / inverse[n]) * 0.5;
            double portee = (vallee[n] + ELARGISSEMENT_MAX_KM + AMPLITUDE_SINUOSITE_KM * 2.0) / Terrain.RAYON_KM + demi;
            int cote = (int) Math.ceil(portee / pas) + 1;
            for (int j = -cote; j <= cote; j++) {
                for (int i = -cote; i <= cote; i++) {
                    double px = mx + (ux * i + wx * j) * pas;
                    double py = my + (uy * i + wy * j) * pas;
                    double pz = mz + (uz * i + wz * j) * pas;
                    int c = seau(px, py, pz);
                    if (dernier[c] == n) continue;
                    dernier[c] = n;
                    if (listes[c] == null) listes[c] = new IntArray(8);
                    listes[c].add(n);
                }
            }
        }
        for (int c = 0; c < listes.length; c++) {
            if (listes[c] != null) seaux[c] = listes[c].toArray();
        }
    }

    public void influencer(double x, double y, double z, int niveau, Terrain.Point point) {
        point.riviere = false;
        int[] morceaux = seaux[seau(x, y, z)];
        if (morceaux == null) return;

        int octaves = Math.min(OCTAVES_SINUOSITE, niveau + 1);
        double frequence = Terrain.RAYON_KM / LONGUEUR_SINUOSITE_KM;
        double amplitude = AMPLITUDE_SINUOSITE_KM / Terrain.RAYON_KM;
        double px = x, py = y, pz = z;
        for (int o = 0; o < octaves; o++) {
            double fx = x * frequence, fy = y * frequence, fz = z * frequence;
            px += amplitude * Bruit.perlin(fx, fy, fz, graine + 1 + o);
            py += amplitude * Bruit.perlin(fx, fy, fz, graine + 11 + o);
            pz += amplitude * Bruit.perlin(fx, fy, fz, graine + 21 + o);
            frequence *= 2.0;
            amplitude *= 0.5;
        }

        double radial = (px - x) * x + (py - y) * y + (pz - z) * z;
        px -= radial * x;
        py -= radial * y;
        pz -= radial * z;

        double seuil = Math.max(DEBIT_MIN, DEBIT_VISIBLE / (1 << Math.max(0, niveau - NIVEAU_TOUTES_VISIBLES)));
        double demiCase = TAILLE_SEAU_KM * 0.5 / (1 << niveau);
        double meilleur = 1.0;
        float niveauEau = 0f;

        for (int m : morceaux) {
            double vx = bx[m] - ax[m], vy = by[m] - ay[m], vz = bz[m] - az[m];
            double ex = px - ax[m], ey = py - ay[m], ez = pz - az[m];
            double t = (ex * vx + ey * vy + ez * vz) * inverse[m];
            t = t < 0.0 ? 0.0 : (t > 1.0 ? 1.0 : t);
            ex -= t * vx;
            ey -= t * vy;
            ez -= t * vz;
            double distance = Math.sqrt(ex * ex + ey * ey + ez * ez) * Terrain.RAYON_KM;

            float hauteur = hautA[m] + (hautB[m] - hautA[m]) * (float) t;
            float chute = Math.max(0f, point.altitude - hauteur);
            double rapport = distance / (vallee[m] + Math.min(ELARGISSEMENT_MAX_KM, ELARGISSEMENT_KM * chute));
            if (rapport < meilleur) {
                meilleur = rapport;
                niveauEau = hauteur;
            }
            if (debit[m] >= seuil && distance < Math.max(largeur[m] * 0.5, demiCase)) {
                point.riviere = true;
            }
        }

        if (meilleur < 1.0 && point.altitude > niveauEau) {
            float s = (float) (meilleur * meilleur * (3.0 - 2.0 * meilleur));
            point.altitude = niveauEau + (point.altitude - niveauEau) * s;
        }
    }
}
