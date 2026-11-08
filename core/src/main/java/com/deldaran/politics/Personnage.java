package com.deldaran.politics;

import com.deldaran.politics.planet.Terrain;

public class Personnage {
    public static final int BAS = 0, HAUT = 1, DROITE = 2, GAUCHE = 3;
    private static final double RAYON_METRES = Terrain.RAYON_KM * 1000.0;
    private static final float CADENCE = 7f;
    private static final int[] PAS = { 1, 0, 2, 0 };

    public final Apparence apparence;
    public final double[] position = new double[3];
    public float altitude;
    public int direction = BAS;
    public boolean enMarche;
    private float marche;

    public Personnage(Apparence apparence, double x, double y, double z) {
        this.apparence = apparence;
        position[0] = x;
        position[1] = y;
        position[2] = z;
    }

    public int image() {
        int colonne = direction == BAS ? 0 : direction == HAUT ? 1 : 2;
        int pas = enMarche ? PAS[(int) (marche * CADENCE) % PAS.length] : 0;
        return colonne * 3 + pas;
    }

    public boolean miroir() {
        return direction == GAUCHE;
    }

    public void deplacer(double estMetres, double nordMetres, float delta, Terrain terrain, Terrain.Point point) {
        enMarche = estMetres != 0.0 || nordMetres != 0.0;
        if (!enMarche) {
            marche = 0f;
            return;
        }
        marche += delta;
        if (Math.abs(estMetres) > Math.abs(nordMetres)) direction = estMetres > 0 ? DROITE : GAUCHE;
        else direction = nordMetres > 0 ? HAUT : BAS;

        double x = position[0], y = position[1], z = position[2];
        double ex = -y, ey = x, n = Math.sqrt(ex * ex + ey * ey);
        ex /= n;
        ey /= n;
        double nx = -z * ey, ny = z * ex, nz = x * ey - y * ex;
        double a = estMetres / RAYON_METRES, b = nordMetres / RAYON_METRES;
        double px = x + ex * a + nx * b, py = y + ey * a + ny * b, pz = z + nz * b;
        double l = Math.sqrt(px * px + py * py + pz * pz);
        px /= l;
        py /= l;
        pz /= l;
        terrain.echantillonner(px, py, pz, 11, point);
        if (point.altitude < 0f || point.riviere || point.lac) return;
        position[0] = px;
        position[1] = py;
        position[2] = pz;
        altitude = point.altitude;
    }
}
