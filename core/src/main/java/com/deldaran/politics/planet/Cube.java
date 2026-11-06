package com.deldaran.politics.planet;

public final class Cube {
    
    public static final int FACES = 6;

    private Cube() {
    }

    public static void direction(int face, double a, double b, double[] sortie) {
        double u = Math.tan(a * Math.PI / 4.0);
        double v = Math.tan(b * Math.PI / 4.0);
        double signe = face % 2 == 0 ? 1.0 : -1.0;
        double x, y, z;
        if (face < 2) {
            x = signe; y = u; z = v;
        } else if (face < 4) {
            x = u; y = signe; z = v;
        } else {
            x = u; y = v; z = signe;
        }
        double longueur = Math.sqrt(x * x + y * y + z * z);
        sortie[0] = x / longueur;
        sortie[1] = y / longueur;
        sortie[2] = z / longueur;
    }

    public static int face(double x, double y, double z) {
        double ax = Math.abs(x), ay = Math.abs(y), az = Math.abs(z);
        if (ax >= ay && ax >= az) return x > 0 ? 0 : 1;
        if (ay >= az) return y > 0 ? 2 : 3;
        return z > 0 ? 4 : 5;
    }

    public static double coordonneeA(int face, double x, double y, double z) {
        if (face < 2) return versFace(y / Math.abs(x));
        if (face < 4) return versFace(x / Math.abs(y));
        return versFace(x / Math.abs(z));
    }

    public static double coordonneeB(int face, double x, double y, double z) {
        if (face < 2) return versFace(z / Math.abs(x));
        if (face < 4) return versFace(z / Math.abs(y));
        return versFace(y / Math.abs(z));
    }

    private static double versFace(double valeur) {
        return Math.atan(valeur) * 4.0 / Math.PI;
    }
}
