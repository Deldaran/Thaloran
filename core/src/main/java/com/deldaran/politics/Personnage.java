package com.deldaran.politics;

import com.deldaran.politics.planet.Terrain;

public class Personnage {
    public static final int BAS = 0, HAUT = 1, DROITE = 2, GAUCHE = 3, BAS_DROITE = 4, BAS_GAUCHE = 5, HAUT_DROITE = 6, HAUT_GAUCHE = 7;
    private static final double RAYON_METRES = Terrain.RAYON_KM * 1000.0;
    private static final float CADENCE = 7f;

    public final Apparence apparence;
    public final double[] position = new double[3];
    public float altitude;
    public int direction = BAS;
    public boolean enMarche;
    public boolean dansHerbe;
    public boolean peint = true;
    public Tenue habits;
    public boolean nu;
    public Traits traits;
    private Tenue sousVetements;
    public float profondeur;
    public float dirEst, dirNord = -1f;
    public int dernierPas = -1;
    public static final float PROFONDEUR_MAX = 0.9f;
    private float marche;
    public Animations.Animation animation;
    public float tempsAnimation;
    public Personnage partenaire;
    public boolean meneur;
    public double hauteurPortee;
    public Villageois vie;
    public float sante = 1f;
    public String bulle;
    public float bulleReste;

    public Personnage(Apparence apparence, double x, double y, double z) {
        this.apparence = apparence;
        position[0] = x;
        position[1] = y;
        position[2] = z;
    }

    /** Image dans la planche LPC : directions haut, gauche, bas, droite ; image 0 à l'arrêt, 1 à 8 pour la marche. */
    public int image() {
        int vue = direction == GAUCHE ? 1 : direction == DROITE ? 3
                : direction == HAUT || direction == HAUT_DROITE || direction == HAUT_GAUCHE ? 0 : 2;
        int pas = enMarche ? 1 + (int) (marche * CADENCE * 2f) % (Assembleur.PAS - 1) : 0;
        return vue * Assembleur.PAS + pas;
    }

    public void jouer(Animations.Animation a) {
        if (partenaire != null) arreter();
        animation = a;
        tempsAnimation = 0f;
    }

    public void jouer(Animations.Animation a, Personnage autre) {
        jouer(a);
        if (a.partenaire == null || autre == null || autre == this) return;
        autre.jouer(a.partenaire);
        partenaire = autre;
        meneur = true;
        autre.partenaire = this;
        autre.meneur = false;
        placerPartenaire();
    }

    public void arreter() {
        animation = null;
        Personnage autre = partenaire;
        partenaire = null;
        meneur = false;
        hauteurPortee = 0.0;
        if (autre != null && autre.partenaire == this) autre.arreter();
    }

    public void montrer(String nom, float duree) {
        bulle = nom;
        bulleReste = duree;
    }

    public void avancer(float delta) {
        bulleReste -= delta;
        if (animation == null) return;
        if (partenaire != null && !meneur) return;
        if (!animation.mobile || enMarche) tempsAnimation += delta;
        if (partenaire != null) {
            partenaire.tempsAnimation = tempsAnimation;
            partenaire.enMarche = enMarche;
            placerPartenaire();
        }
        if (animation.fini(tempsAnimation)) arreter();
    }

    public int imageAnimation() {
        return animation.image(tempsAnimation);
    }

    private void placerPartenaire() {
        double d = animation.distance;
        Personnage p = partenaire;
        p.hauteurPortee = 0.0;
        switch (animation.placement) {
            case Animations.COTE:
                decaler(position, dirNord * d, -dirEst * d, p.position);
                p.orienter(dirEst, dirNord);
                break;
            case Animations.DESSUS:
                System.arraycopy(position, 0, p.position, 0, 3);
                p.hauteurPortee = d;
                p.orienter(dirEst, dirNord);
                break;
            default:
                decaler(position, dirEst * d, dirNord * d, p.position);
                p.orienter(-dirEst, -dirNord);
                break;
        }
        p.altitude = altitude;
        p.profondeur = profondeur;
        p.dansHerbe = dansHerbe;
    }

    public void orienter(double estMetres, double nordMetres) {
        double longueur = Math.sqrt(estMetres * estMetres + nordMetres * nordMetres);
        if (longueur < 1e-9) return;
        dirEst = (float) (estMetres / longueur);
        dirNord = (float) (nordMetres / longueur);
        double cr = Math.cos(rotationVue), sr = Math.sin(rotationVue);
        double ve = cr * estMetres - sr * nordMetres, vn = sr * estMetres + cr * nordMetres;
        double ae = Math.abs(ve), an = Math.abs(vn);
        if (ae > an * 2.0) direction = ve > 0 ? DROITE : GAUCHE;
        else if (an > ae * 2.0) direction = vn > 0 ? HAUT : BAS;
        else if (vn > 0) direction = ve > 0 ? HAUT_DROITE : HAUT_GAUCHE;
        else direction = ve > 0 ? BAS_DROITE : BAS_GAUCHE;
    }

    public Tenue tenue() {
        if (nu) {
            if (sousVetements == null) sousVetements = Tenue.nue(apparence.graine, apparence.femme && !apparence.enfant);
            return sousVetements;
        }
        if (habits == null) habits = Tenue.tirer(apparence.graine, apparence.femme, apparence.enfant);
        return habits;
    }

    public Traits traits() {
        if (traits == null) traits = Traits.tirer(apparence.graine, apparence.espece, apparence.femme && !apparence.enfant, apparence.enfant);
        return traits;
    }

    public boolean imberbe() {
        return apparence.enfant || apparence.femme || apparence.espece == com.deldaran.politics.histoire.Peuple.ELFE;
    }

    public int pas() {
        return (int) (marche * CADENCE / 2f);
    }

    public static final double RAYON_CORPS = 0.28;
    public static double rotationVue;

    public void deplacer(double estMetres, double nordMetres, float delta, Terrain terrain, Terrain.Point point) {
        deplacer(estMetres, nordMetres, delta, terrain, point, null);
    }

    public void deplacer(double estMetres, double nordMetres, float delta, Terrain terrain, Terrain.Point point, Obstacles obstacles) {
        if (partenaire != null && !meneur) return;
        if (animation != null && !animation.mobile && (estMetres != 0.0 || nordMetres != 0.0)) arreter();
        if (obstacles != null && (estMetres != 0.0 || nordMetres != 0.0) && !obstacles.bloque(position[0], position[1], position[2], RAYON_CORPS)) {
            if (bloque(estMetres, nordMetres, obstacles)) {
                if (!bloque(estMetres, 0.0, obstacles)) nordMetres = 0.0;
                else if (!bloque(0.0, nordMetres, obstacles)) estMetres = 0.0;
                else {
                    estMetres = 0.0;
                    nordMetres = 0.0;
                }
                if (estMetres == 0.0 && nordMetres == 0.0) {
                    enMarche = false;
                    marche = 0f;
                    return;
                }
            }
        }
        enMarche = estMetres != 0.0 || nordMetres != 0.0;
        if (!enMarche) {
            marche = 0f;
            return;
        }
        marche += delta;
        orienter(estMetres, nordMetres);

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
        float fond = profondeur(point);
        if (fond > 0f && obstacles != null && obstacles.surPont(px, py, pz)) fond = 0f;
        if (fond > PROFONDEUR_MAX) return;
        position[0] = px;
        position[1] = py;
        position[2] = pz;
        altitude = point.altitude;
        profondeur = fond;
        dansHerbe = fond == 0f && herbe(terrain, px, py, pz, point);
    }

    private final double[] essai = new double[3];

    private boolean bloque(double estMetres, double nordMetres, Obstacles obstacles) {
        decaler(position, estMetres, nordMetres, essai);
        return obstacles.bloque(essai[0], essai[1], essai[2], RAYON_CORPS);
    }

    public static void decaler(double[] p, double estMetres, double nordMetres, double[] sortie) {
        double x = p[0], y = p[1], z = p[2];
        double ex = -y, ey = x, n = Math.sqrt(ex * ex + ey * ey);
        ex /= n;
        ey /= n;
        double nx = -z * ey, ny = z * ex, nz = x * ey - y * ex;
        double a = estMetres / RAYON_METRES, b = nordMetres / RAYON_METRES;
        double px = x + ex * a + nx * b, py = y + ey * a + ny * b, pz = z + nz * b;
        double l = Math.sqrt(px * px + py * py + pz * pz);
        sortie[0] = px / l;
        sortie[1] = py / l;
        sortie[2] = pz / l;
    }

    public static float profondeur(Terrain.Point point) {
        if (point.riviere || point.lac) return 0.6f;
        return point.altitude < 0f ? -point.altitude * 150f : 0f;
    }

    public static boolean herbe(Terrain terrain, double x, double y, double z, Terrain.Point point) {
        terrain.biome(x, y, z, point);
        float t = point.temperature, p = point.pluie;
        if (t < -8f) return false;
        if (t < 8f) return true;
        if (t < 20f) return p >= 250f && p < 550f;
        return p >= 250f;
    }
}
