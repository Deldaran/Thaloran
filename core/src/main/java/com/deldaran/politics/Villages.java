package com.deldaran.politics;

import com.deldaran.politics.histoire.Population;
import com.deldaran.politics.histoire.Villes;
import com.deldaran.politics.planet.Sphere;
import com.deldaran.politics.planet.Terrain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

public class Villages {
    public static final int TERRE = 0, PAVE = 1, PONT = 2, PONTON = 3, TRAIT = 10;
    private static final double RAYON_METRES = Terrain.RAYON_KM * 1000.0;
    private static final double PORTEE = 4000.0;
    private static final double RECALCUL = 150.0;
    private static final double MARGE = 8.0;
    private static final double PAS_RECHERCHE = 50.0;
    private static final int ANNEAUX = 40;
    private static final int HABITANTS_PAR_MAISON = 6;
    private static final int MAISONS_MAX = 400;
    private static final float ALTITUDE_MIN = 0.006f;
    private static final double PAS_RUE = 4.0;
    private static final double PENTE_MAX_RUE = 0.28;
    private static final double JONCTION = 5.0;
    private static final double LARGEUR_GRANDE_RUE = 3.6, LARGEUR_RUELLE = 2.4;
    private static final double PAS_LOTS = 2.0;
    private static final double DENIVELE_MAX = 3.5;
    private static final double MARGE_GRILLE = 25.0;
    private static final double PENTE_TALUS = 0.45, TALUS_MIN = 1.5, TALUS_MAX = 7.0;
    private static final double MAISONS_PAR_METRE = 0.17;
    private static final double PONT_MAX = 60.0, PONT_RUELLE = 25.0;
    private static final double ECART_RUES = 14.0;
    private static final double RAYON_SITE = 400.0, PAS_SITE = 40.0, PLAT_SITE = 15.0;

    private static class Village {
        int maisons;
        float[] positions, sols, arbres, talus;
        int[] metiers;
        Grille grille;
        long[] graines;
        Batiments.Plan[] plans;
        float[] rues;
        double x, y, z, rayon;
    }

    private static class Rue {
        double[] points = new double[32];
        boolean[] ponts = new boolean[16];
        double[] niveaux = new double[16];
        double[] rive;
        int nombre;
        double largeur, longueur;
        int type;

        Rue(double largeur, int type) {
            this.largeur = largeur;
            this.type = type;
        }

        void ajouter(double a, double b) {
            ajouter(a, b, false);
        }

        void ajouter(double a, double b, boolean pont) {
            if (nombre * 2 == points.length) {
                points = Arrays.copyOf(points, points.length * 2);
                ponts = Arrays.copyOf(ponts, nombre * 2);
                niveaux = Arrays.copyOf(niveaux, nombre * 2);
            }
            ponts[nombre] = pont;
            niveaux[nombre] = Double.NaN;
            if (nombre > 0) longueur += Math.hypot(a - points[nombre * 2 - 2], b - points[nombre * 2 - 1]);
            points[nombre * 2] = a;
            points[nombre * 2 + 1] = b;
            nombre++;
        }
    }

    private static class Lot {
        double priorite, a, b, eau = Double.NaN, sol = Double.NaN, talus = TALUS_MIN;
        double ancreA, ancreB, normA, normB, distanceEau = -1.0;
        boolean place;
        int id = -1, rue = -1, metier;
        Batiments.Plan plan;
    }

    public int nombre, version;
    public float[] x = new float[256], y = new float[256], z = new float[256], altitude = new float[256], niveau = new float[256], talus = new float[256];
    public long[] graine = new long[256];
    public int[] metier = new int[256];
    public Batiments.Plan[] plans = new Batiments.Plan[256];
    public float[] rues = new float[1024];
    public int nombreRues;
    public float[] arbres = new float[1024];
    public int nombreArbres;
    private final Map<Integer, Village> connus = new HashMap<>();
    private Village[] proches = new Village[16];
    private int villages;
    private final double[] derniere = new double[3];
    private final double[] est = new double[3], nord = new double[3], p = new double[3];
    private double centreX, centreY, centreZ;
    private boolean place;
    private final Terrain.Point point = new Terrain.Point();
    private final Terrain.Point sol = new Terrain.Point();
    private final double[] coin = new double[3];
    private final double[] proche = new double[3];
    private Terrain terrain;
    private double rayonPlace;
    private Grille grille;
    private Population population;

    public void mettreAJour(Population population, Sphere sphere, Terrain terrain, GameCamera camera) {
        this.population = population;
        Villes villes = population.villes;
        double cosLat = Math.cos(camera.latitude);
        double cx = cosLat * Math.cos(camera.longitude), cy = cosLat * Math.sin(camera.longitude), cz = Math.sin(camera.latitude);
        double dx = cx - derniere[0], dy = cy - derniere[1], dz = cz - derniere[2];
        if (place && Math.sqrt(dx * dx + dy * dy + dz * dz) * RAYON_METRES < RECALCUL) return;
        place = true;
        version++;
        derniere[0] = cx;
        derniere[1] = cy;
        derniere[2] = cz;
        nombre = 0;
        villages = 0;
        nombreRues = 0;
        nombreArbres = 0;
        double seuil = Math.cos(PORTEE / RAYON_METRES);
        int total = villes.nombre;
        for (int v = 0; v < total; v++) {
            if (!villes.estVivante(v)) continue;
            int c = villes.caseDe[v];
            if (sphere.x[c] * cx + sphere.y[c] * cy + sphere.z[c] * cz < seuil) continue;
            int n = Math.min(MAISONS_MAX, Math.max(1, villes.habitants[v] / HABITANTS_PAR_MAISON));
            Village village = connus.get(v);
            if (village == null || village.maisons != n) {
                village = batir(v, n, sphere.x[c], sphere.y[c], sphere.z[c], terrain);
                connus.put(v, village);
            }
            if (village.positions == null) continue;
            if (villages == proches.length) proches = Arrays.copyOf(proches, villages * 2);
            proches[villages++] = village;
            for (int k = 0; k < village.graines.length; k++) {
                ajouter(village.positions, k, village.graines[k], village.plans[k]);
                niveau[nombre - 1] = village.sols[k];
                talus[nombre - 1] = village.talus[k];
                metier[nombre - 1] = village.metiers[k];
            }
            if (nombreRues + village.rues.length > rues.length) rues = Arrays.copyOf(rues, Math.max(rues.length * 2, nombreRues + village.rues.length));
            System.arraycopy(village.rues, 0, rues, nombreRues, village.rues.length);
            nombreRues += village.rues.length;
            if (nombreArbres + village.arbres.length > arbres.length) arbres = Arrays.copyOf(arbres, Math.max(arbres.length * 2, nombreArbres + village.arbres.length));
            System.arraycopy(village.arbres, 0, arbres, nombreArbres, village.arbres.length);
            nombreArbres += village.arbres.length;
        }
    }

    private Village batir(int v, int n, double vx, double vy, double vz, Terrain terrain) {
        this.terrain = terrain;
        Village village = new Village();
        village.maisons = n;
        repere(vx, vy, vz);
        if (!chercherSol(vx, vy, vz, terrain)) return village;
        choisirSite();
        repere(p[0], p[1], p[2]);
        centreX = p[0];
        centreY = p[1];
        centreZ = p[2];
        Random r = new Random(melanger(v * 31L + 7L));

        Batiments.Plan central = Batiments.planCentral(melanger(v * 1000003L), n);
        rayonPlace = central.rayon() + 3.0 + Math.min(4.0, Math.sqrt(n) * 0.3);
        double altCentre = hauteur(0.0, 0.0);

        List<Rue> reseau = new ArrayList<>();
        int principales = n < 6 ? 2 : 3 + (n > 40 ? 1 : 0) + (n > 120 ? 1 : 0);
        boolean pavee = n > 60;
        double depart = r.nextDouble() * 2.0 * Math.PI;
        double voulue = n / MAISONS_PAR_METRE + 30.0;
        double longueur = 0.0;
        double premierAngle = depart;
        for (int k = 0; k < principales; k++) {
            double angle = depart + k * 2.0 * Math.PI / principales + (r.nextDouble() - 0.5) * 0.6;
            if (k == 0) premierAngle = angle;
            Rue rue = tracer(Math.cos(angle) * rayonPlace * 0.9, Math.sin(angle) * rayonPlace * 0.9, angle, 25.0 + Math.sqrt(n) * 10.0,
                    pavee ? LARGEUR_GRANDE_RUE + 0.4 : LARGEUR_GRANDE_RUE, pavee ? PAVE : TERRE, reseau, null, r);
            if (rue.nombre < 2) continue;
            reseau.add(rue);
            longueur += rue.longueur;
        }
        int grandes = reseau.size();
        for (int essai = 0; essai < 300 && longueur < voulue && !reseau.isEmpty(); essai++) {
            Rue mere = reseau.get(r.nextInt(reseau.size()));
            if (mere.nombre < 3) continue;
            int i = 1 + r.nextInt(mere.nombre - 1);
            double a = mere.points[i * 2], b = mere.points[i * 2 + 1];
            double dir = Math.atan2(b - mere.points[i * 2 - 1], a - mere.points[i * 2 - 2]);
            double cap = dir + (r.nextBoolean() ? 1 : -1) * (Math.PI / 2.0 + (r.nextDouble() - 0.5) * 0.6);
            if (plusProche(a + Math.cos(cap) * ECART_RUES * 0.7, b + Math.sin(cap) * ECART_RUES * 0.7, reseau, mere) < ECART_RUES) continue;
            Rue rue = tracer(a, b, cap, 12.0 + r.nextDouble() * (15.0 + Math.sqrt(n) * 4.0), LARGEUR_RUELLE, TERRE, reseau, mere, r);
            if (rue.longueur < 8.0) continue;
            reseau.add(rue);
            longueur += rue.longueur;
        }

        List<Lot> objets = new ArrayList<>();
        List<double[]> pontons = new ArrayList<>();
        rivages(reseau, objets, pontons, n, v, r);
        ponts(reseau, objets, v);
        grille = quadriller(reseau, pontons);

        List<Lot> lots = new ArrayList<>();
        long compteur = 0;
        for (int ir = 0; ir < reseau.size(); ir++) {
            Rue rue = reseau.get(ir);
            for (int s = 0; s + 1 < rue.nombre; s++) {
                if (rue.ponts[s + 1]) continue;
                double a0 = rue.points[s * 2], b0 = rue.points[s * 2 + 1], a1 = rue.points[s * 2 + 2], b1 = rue.points[s * 2 + 3];
                double l = Math.hypot(a1 - a0, b1 - b0);
                if (l < 1e-6) continue;
                double ta = (a1 - a0) / l, tb = (b1 - b0) / l;
                for (double t = 0.0; t < l; t += PAS_LOTS) {
                    for (int cote = -1; cote <= 1; cote += 2) {
                        long g = melanger(v * 1000003L + (++compteur));
                        Batiments.Plan plan = Batiments.plan(g);
                        double recul = 0.9 + hasard(v, (int) compteur, 1) * 1.4;
                        double na = -tb * cote, nb = ta * cote;
                        double decal = rue.largeur * 0.5 + recul + plan.profondeur * 0.5;
                        Lot lot = new Lot();
                        lot.a = a0 + ta * t + na * decal;
                        lot.b = b0 + tb * t + nb * decal;
                        lot.ancreA = a0 + ta * t + na * (rue.largeur * 0.5 + recul);
                        lot.ancreB = b0 + tb * t + nb * (rue.largeur * 0.5 + recul);
                        lot.normA = na;
                        lot.normB = nb;
                        lot.rue = ir;
                        lot.plan = plan;
                        plan.orienter(Math.atan2(-na, nb));
                        double h = hauteur(lot.a, lot.b);
                        if (Double.isNaN(h)) continue;
                        lot.priorite = Math.hypot(lot.a, lot.b) + hasard(v, (int) compteur, 2) * 8.0 + Math.max(0.0, h - altCentre) * 1.5;
                        lots.add(lot);
                    }
                }
            }
        }
        int autour = (int) (2.0 * Math.PI * rayonPlace / 3.0);
        for (int k = 0; k < autour; k++) {
            long g = melanger(v * 1000003L + (++compteur));
            Batiments.Plan plan = Batiments.plan(g);
            double angle = 2.0 * Math.PI * k / autour;
            double d = rayonPlace + 0.8 + plan.profondeur * 0.5;
            Lot lot = new Lot();
            lot.a = Math.cos(angle) * d;
            lot.b = Math.sin(angle) * d;
            lot.ancreA = Math.cos(angle) * (rayonPlace + 0.8);
            lot.ancreB = Math.sin(angle) * (rayonPlace + 0.8);
            lot.normA = Math.cos(angle);
            lot.normB = Math.sin(angle);
            lot.place = true;
            lot.plan = plan;
            plan.orienter(Math.atan2(-Math.cos(angle), Math.sin(angle)));
            lot.priorite = d - 4.0 + hasard(v, (int) compteur, 2) * 3.0;
            lots.add(lot);
        }
        lots.sort((u, w) -> Double.compare(u.priorite, w.priorite));

        List<Lot> poses = new ArrayList<>();
        Lot centre = new Lot();
        centre.plan = central;
        central.orienter(Math.atan2(Math.cos(premierAngle), -Math.sin(premierAngle)));
        centre.id = 0;
        poses.add(centre);
        Metiers.Profil profil = Metiers.profil(population, v);
        profil.eau = !pontons.isEmpty() || grille.riviere || eauProche(0.0, 0.0, 120.0) < 120.0;
        profil.riviere = grille.riviere;
        int[] besoins = Metiers.besoins(profil);
        for (int q = 0; q < besoins.length; q++) placerMetier(besoins[q], q, lots, poses, grandes, v, r);
        for (Lot lot : lots) {
            if (poses.size() >= n + 1) break;
            lot.id = poses.size();
            if (!libre(lot)) continue;
            poses.add(lot);
        }

        List<double[]> plantes = new ArrayList<>();
        amenager(poses, reseau, objets, plantes, premierAngle, n, r);
        etals(Metiers.etals(profil), poses, reseau, objets, premierAngle, r);
        int maisons = poses.size();
        poses.addAll(objets);
        int total = poses.size();
        float[] positions = new float[total * 4];
        float[] sols = new float[total];
        float[] talus = new float[total];
        int[] metiers = new int[total];
        long[] graines = new long[total];
        Batiments.Plan[] lesPlans = new Batiments.Plan[total];
        double rayon = rayonPlace;
        for (int q = 0; q < total; q++) {
            Lot lot = poses.get(q);
            deplacer(centreX, centreY, centreZ, lot.a / RAYON_METRES, lot.b / RAYON_METRES);
            positions[q * 4] = (float) p[0];
            positions[q * 4 + 1] = (float) p[1];
            positions[q * 4 + 2] = (float) p[2];
            positions[q * 4 + 3] = Double.isNaN(lot.eau) ? RenduTuiles.altitudeSol(terrain, p[0], p[1], p[2], sol, coin) : (float) (lot.eau / 1000.0);
            sols[q] = q < maisons ? (float) (Double.isNaN(lot.sol) ? hauteurSol() : lot.sol) : 0f;
            talus[q] = (float) lot.talus;
            metiers[q] = lot.metier;
            graines[q] = lot.plan.graine;
            lesPlans[q] = lot.plan;
            rayon = Math.max(rayon, Math.hypot(lot.a, lot.b) + lot.plan.rayon());
        }
        float[] feuillage = new float[plantes.size() * 5];
        for (int q = 0; q < plantes.size(); q++) {
            double[] pl = plantes.get(q);
            deplacer(centreX, centreY, centreZ, pl[0] / RAYON_METRES, pl[1] / RAYON_METRES);
            feuillage[q * 5] = (float) p[0];
            feuillage[q * 5 + 1] = (float) p[1];
            feuillage[q * 5 + 2] = (float) p[2];
            feuillage[q * 5 + 3] = (float) pl[2];
            feuillage[q * 5 + 4] = RenduTuiles.altitudeSol(terrain, p[0], p[1], p[2], sol, coin);
        }
        int segments = 1 + pontons.size();
        for (Rue rue : reseau) {
            segments += rue.nombre - 1;
            for (int s = 0; s < rue.nombre; s++) rayon = Math.max(rayon, Math.hypot(rue.points[s * 2], rue.points[s * 2 + 1]));
        }
        float[] traits = new float[segments * TRAIT];
        int t = 0;
        t = segment(traits, t, 0, 0, 0, 0, rayonPlace * 2.0, PAVE, Double.NaN, Double.NaN);
        for (Rue rue : reseau) {
            for (int s = 0; s + 1 < rue.nombre; s++) {
                t = segment(traits, t, rue.points[s * 2], rue.points[s * 2 + 1], rue.points[s * 2 + 2], rue.points[s * 2 + 3], rue.largeur, rue.ponts[s + 1] ? PONT : rue.type, rue.niveaux[s], rue.niveaux[s + 1]);
            }
        }
        for (double[] o : pontons) t = segment(traits, t, o[0], o[1], o[2], o[3], o[4], (int) o[5], o[6], o[7]);
        village.positions = positions;
        village.arbres = feuillage;
        village.sols = sols;
        village.talus = talus;
        village.metiers = metiers;
        village.grille = grille;
        village.graines = graines;
        village.plans = lesPlans;
        village.rues = traits;
        village.x = centreX;
        village.y = centreY;
        village.z = centreZ;
        village.rayon = (rayon + MARGE) / RAYON_METRES;
        return village;
    }

    private int segment(float[] traits, int t, double a0, double b0, double a1, double b1, double largeur, int type, double h0, double h1) {
        deplacer(centreX, centreY, centreZ, a0 / RAYON_METRES, b0 / RAYON_METRES);
        traits[t] = (float) p[0];
        traits[t + 1] = (float) p[1];
        traits[t + 2] = (float) p[2];
        traits[t + 8] = (float) (Double.isNaN(h0) ? hauteurSol() : h0);
        deplacer(centreX, centreY, centreZ, a1 / RAYON_METRES, b1 / RAYON_METRES);
        traits[t + 3] = (float) p[0];
        traits[t + 4] = (float) p[1];
        traits[t + 5] = (float) p[2];
        traits[t + 9] = (float) (Double.isNaN(h1) ? hauteurSol() : h1);
        traits[t + 6] = (float) largeur;
        traits[t + 7] = type;
        return t + TRAIT;
    }

    private double hauteurSol() {
        float altitude = RenduTuiles.altitudeSol(terrain, p[0], p[1], p[2], sol, coin);
        return Math.max(altitude * 1000.0, 0.0) + MicroRelief.hauteur(p[0], p[1], p[2], altitude, terrain.hauteurPlaines);
    }

    private Rue tracer(double a, double b, double cap, double longueurMax, double largeur, int type, List<Rue> reseau, Rue mere, Random r) {
        Rue rue = new Rue(largeur, type);
        double alt = hauteur(a, b);
        if (Double.isNaN(alt)) return rue;
        rue.ajouter(a, b);
        double courbe = (r.nextDouble() - 0.5) * 0.14;
        while (rue.longueur < longueurMax) {
            double meilleur = Double.MAX_VALUE, ma = 0, mb = 0, mc = 0, malt = 0;
            boolean mpont = false;
            for (int c = -2; c <= 2; c++) {
                double essai = cap + courbe + c * 0.22;
                double na = a + Math.cos(essai) * PAS_RUE, nb = b + Math.sin(essai) * PAS_RUE;
                if (Math.hypot(na, nb) < rayonPlace * 0.9) continue;
                double h = hauteur(na, nb);
                double cout;
                boolean pont = false;
                if (Double.isNaN(h)) {
                    double l = traverser(a, b, essai, alt, mere == null ? PONT_MAX : PONT_RUELLE);
                    if (l <= 0.0) continue;
                    na = a + Math.cos(essai) * l;
                    nb = b + Math.sin(essai) * l;
                    h = hauteur(na, nb);
                    pont = true;
                    cout = 1.5 + Math.abs(c) * 0.25 + r.nextDouble() * 0.35;
                } else {
                    double pente = Math.abs(h - alt) / PAS_RUE;
                    if (pente > PENTE_MAX_RUE) continue;
                    cout = pente * 6.0 + Math.max(0.0, h - alt) / PAS_RUE * 4.0 + Math.abs(c) * 0.25 + r.nextDouble() * 0.35;
                }
                if (cout < meilleur) {
                    meilleur = cout;
                    ma = na;
                    mb = nb;
                    mc = essai;
                    malt = h;
                    mpont = pont;
                }
            }
            if (meilleur == Double.MAX_VALUE) {
                if (Double.isNaN(hauteur(a + Math.cos(cap) * PAS_RUE, b + Math.sin(cap) * PAS_RUE))) rue.rive = new double[] { a, b, cap, hauteurPoint(a, b) };
                break;
            }
            rue.ajouter(ma, mb, mpont);
            a = ma;
            b = mb;
            cap = mc;
            alt = malt;
            if (rue.longueur > 6.0 && plusProche(a, b, reseau, mere) < (mere == null ? JONCTION : ECART_RUES * 0.6)) {
                rue.ajouter(proche[1], proche[2]);
                break;
            }
        }
        return rue;
    }

    private double traverser(double a, double b, double cap, double alt, double portee) {
        double ca = Math.cos(cap), cb = Math.sin(cap);
        for (double d = PAS_RUE + 1.0; d <= portee; d += 1.0) {
            double h = hauteur(a + ca * d, b + cb * d);
            if (Double.isNaN(h)) continue;
            double l = d + 1.5;
            double rive = hauteur(a + ca * l, b + cb * l);
            if (Double.isNaN(rive) || Math.abs(rive - alt) > l * 0.12 + 0.5) return -1.0;
            return l;
        }
        return -1.0;
    }

    private void choisirSite() {
        double bx = p[0], by = p[1], bz = p[2];
        repere(bx, by, bz);
        centreX = bx;
        centreY = by;
        centreZ = bz;
        double meilleur = Double.MAX_VALUE, ma = 0.0, mb = 0.0;
        for (int anneau = 0; anneau * PAS_SITE <= RAYON_SITE; anneau++) {
            int angles = Math.max(1, anneau * 6);
            for (int s = 0; s < angles; s++) {
                double t = 2.0 * Math.PI * (s + 0.5 * (anneau & 1)) / angles;
                double a = anneau * PAS_SITE * Math.cos(t), b = anneau * PAS_SITE * Math.sin(t);
                double h = hauteur(a, b);
                if (Double.isNaN(h)) continue;
                double ecart = 0.0;
                for (int k = 0; k < 8; k++) {
                    double hk = hauteur(a + Math.cos(k * Math.PI / 4.0) * PLAT_SITE, b + Math.sin(k * Math.PI / 4.0) * PLAT_SITE);
                    ecart = Double.isNaN(hk) ? ecart + 1.5 : Math.max(ecart, Math.abs(hk - h));
                }
                double note = ecart * 3.0 + h * 0.03 + anneau * PAS_SITE * 0.01;
                if (note < meilleur) {
                    meilleur = note;
                    ma = a;
                    mb = b;
                }
            }
        }
        deplacer(bx, by, bz, ma / RAYON_METRES, mb / RAYON_METRES);
    }

    private double hauteurPoint(double a, double b) {
        deplacer(centreX, centreY, centreZ, a / RAYON_METRES, b / RAYON_METRES);
        return hauteurSol();
    }

    private double niveauEau(double a, double b) {
        deplacer(centreX, centreY, centreZ, a / RAYON_METRES, b / RAYON_METRES);
        return Math.max(RenduTuiles.altitudeSol(terrain, p[0], p[1], p[2], sol, coin) * 1000.0, 0.0);
    }

    private void ponts(List<Rue> reseau, List<Lot> objets, long v) {
        for (Rue rue : reseau) {
            for (int s = 0; s + 1 < rue.nombre; s++) {
                if (!rue.ponts[s + 1]) continue;
                double a0 = rue.points[s * 2], b0 = rue.points[s * 2 + 1], a1 = rue.points[s * 2 + 2], b1 = rue.points[s * 2 + 3];
                double l = Math.hypot(a1 - a0, b1 - b0);
                double eau = niveauEau((a0 + a1) * 0.5, (b0 + b1) * 0.5);
                double tablier = Math.max((hauteurPoint(a0, b0) + hauteurPoint(a1, b1)) * 0.5, eau + 1.8);
                rue.niveaux[s] = tablier;
                rue.niveaux[s + 1] = tablier;
                int arches = Math.max(1, (int) Math.round(l / 7.0));
                double angle = Math.atan2(b1 - b0, a1 - a0);
                for (int k = 0; k < arches; k++) {
                    Lot o = new Lot();
                    o.a = a0 + (a1 - a0) * (k + 0.5) / arches;
                    o.b = b0 + (b1 - b0) * (k + 0.5) / arches;
                    o.eau = eau;
                    o.plan = Batiments.objet(Batiments.ARCHE, l / arches, rue.largeur + 0.8, tablier - eau, angle, (int) (v & 3));
                    objets.add(o);
                }
            }
        }
    }

    private boolean eau(double a, double b) {
        deplacer(centreX, centreY, centreZ, a / RAYON_METRES, b / RAYON_METRES);
        terrain.echantillonner(p[0], p[1], p[2], 11, point);
        return point.riviere || point.lac || RenduTuiles.altitudeSol(terrain, p[0], p[1], p[2], sol, coin) <= 0f;
    }

    private void rivages(List<Rue> reseau, List<Lot> objets, List<double[]> pontons, int n, long v, Random r) {
        int maximum = 1 + n / 90;
        List<double[]> faits = new ArrayList<>();
        for (Rue rue : new ArrayList<>(reseau)) {
            if (rue.rive == null) continue;
            if (faits.size() >= maximum) break;
            double a = rue.rive[0], b = rue.rive[1], cap = rue.rive[2], h = rue.rive[3];
            double ca = Math.cos(cap), cb = Math.sin(cap), pa = -cb, pb = ca;
            double rivage = -1.0;
            for (double d = 1.0; d <= 30.0; d += 0.5) {
                if (eau(a + ca * d, b + cb * d)) {
                    rivage = d;
                    break;
                }
            }
            if (rivage < 0.0) continue;
            double sa = a + ca * rivage, sb = b + cb * rivage;
            boolean proche = Math.hypot(sa, sb) < rayonPlace + 4.0;
            for (double[] f : faits) proche |= Math.hypot(f[0] - sa, f[1] - sb) < 30.0;
            if (proche) continue;
            double recul = rivage - 1.6;
            double qa = a + ca * recul, qb = b + cb * recul;
            double eau = niveauEau(sa + ca * 3.0, sb + cb * 3.0);
            double hq = Math.max(hauteurPoint(qa, qb), eau + 0.8);
            if ((h - hq) / Math.max(recul, 1.0) > 0.35) continue;
            if (recul > 1.0) {
                Rue acces = new Rue(rue.largeur, rue.type);
                acces.ajouter(a, b);
                acces.ajouter(qa, qb);
                acces.niveaux[1] = hq;
                reseau.add(acces);
            }
            double demi = 5.0 + r.nextDouble() * 3.0;
            Rue quai = new Rue(4.0, PAVE);
            quai.ajouter(qa - pa * demi, qb - pb * demi);
            quai.ajouter(qa + pa * demi, qb + pb * demi);
            quai.niveaux[0] = hq;
            quai.niveaux[1] = hq;
            reseau.add(quai);
            faits.add(new double[] { sa, sb });
            double fin = rivage;
            for (double d = rivage + 1.0; d <= rivage + 16.0; d += 1.0) {
                if (!eau(a + ca * d, b + cb * d)) break;
                fin = d;
            }
            if (fin - rivage >= 7.0) {
                double debut = rivage - 0.2;
                int pieces = (int) Math.ceil((fin - debut) / 3.0);
                double longueur = (fin - debut) / pieces;
                for (int k = 0; k < pieces; k++) {
                    double d = debut + longueur * (k + 0.5);
                    Lot o = new Lot();
                    o.a = a + ca * d;
                    o.b = b + cb * d;
                    o.eau = eau;
                    o.plan = Batiments.objet(Batiments.PONTON, longueur, 2.2, hq - eau, cap, (int) (v & 3));
                    objets.add(o);
                }
                pontons.add(new double[] { a + ca * debut, b + cb * debut, a + ca * fin, b + cb * fin, 2.2, PONTON, hq, hq });
            }
            int caisses = 2 + r.nextInt(3);
            for (int k = 0; k < caisses; k++) {
                double u = (1.8 + r.nextDouble() * (demi - 2.8)) * (r.nextBoolean() ? 1 : -1);
                Lot o = new Lot();
                o.a = qa + pa * u - ca * 0.8;
                o.b = qb + pb * u - cb * 0.8;
                o.plan = Batiments.objet(r.nextBoolean() ? Batiments.TONNEAUX : Batiments.CAISSES, 1.4, 1.2, 0.0, cap + Math.PI / 2.0, r.nextInt(6));
                objets.add(o);
            }
        }
    }

    private void amenager(List<Lot> poses, List<Rue> reseau, List<Lot> objets, List<double[]> plantes, double premierAngle, int n, Random r) {
        deplacer(centreX, centreY, centreZ, 0.0, 0.0);
        terrain.echantillonner(p[0], p[1], p[2], 11, point);
        int arbre = point.temperature < 2f ? Vegetation.CONIFERE : Vegetation.FEUILLU;
        for (int q = 1; q < poses.size(); q++) {
            Lot lot = poses.get(q);
            if (r.nextFloat() < 0.55f) cour(lot, objets, plantes, arbre, r);
            if (r.nextFloat() < 0.45f) {
                Batiments.Plan pl = lot.plan;
                double c = Math.cos(pl.angle), s = Math.sin(pl.angle);
                double x = (r.nextBoolean() ? 1 : -1) * (pl.largeur * 0.5 + 0.45), z = pl.profondeur * 0.5 - 0.3;
                double a = lot.a + x * c + z * s, b = lot.b + x * s - z * c;
                if (libreAuSol(a, b, 0.5, lot.id, plantes)) planter(plantes, a, b, Vegetation.BUISSON * Vegetation.VARIANTES + r.nextInt(Vegetation.VARIANTES));
            }
        }
        Batiments.Plan central = poses.get(0).plan;
        double bord = rayonPlace - 1.3;
        int tour = (int) (2.0 * Math.PI * bord / 9.0);
        for (int k = 0; k < tour; k++) {
            double ang = 2.0 * Math.PI * k / tour + 0.35;
            double a = Math.cos(ang) * bord, b = Math.sin(ang) * bord;
            if (bord < central.rayon() + 2.0) break;
            if (pres(a, b, reseau, 1.4) || Double.isNaN(hauteur(a, b))) continue;
            planter(plantes, a, b, arbre * Vegetation.VARIANTES + r.nextInt(Vegetation.VARIANTES));
        }
        double ang = premierAngle + 0.9, d = (central.rayon() + rayonPlace) * 0.5;
        double pa = Math.cos(ang) * d, pb = Math.sin(ang) * d;
        if (d > central.rayon() + 1.2 && !pres(pa, pb, reseau, 1.2)) {
            Lot puits = new Lot();
            puits.a = pa;
            puits.b = pb;
            puits.plan = Batiments.objet(Batiments.PUITS, 1.6, 1.6, 0.0, 0.0, r.nextInt(4));
            objets.add(puits);
            grille.disque(pa, pb, 1.0, Grille.OBJET);
        }
        double portee = rayonPlace;
        for (Lot lot : poses) portee = Math.max(portee, Math.hypot(lot.a, lot.b));
        int voulus = n / 3 + 3, places = 0;
        for (int essai = 0; essai < voulus * 8 && places < voulus; essai++) {
            double t = r.nextDouble() * 2.0 * Math.PI, e = rayonPlace + 3.0 + r.nextDouble() * (portee + 6.0 - rayonPlace - 3.0);
            double a = Math.cos(t) * e, b = Math.sin(t) * e;
            if (Double.isNaN(hauteur(a, b)) || !libreAuSol(a, b, 1.2, -1, plantes)) continue;
            boolean seul = true;
            for (double[] pl : plantes) seul &= Math.hypot(pl[0] - a, pl[1] - b) > 4.0;
            if (!seul) continue;
            int espece = r.nextFloat() < 0.3f ? Vegetation.BUISSON : arbre;
            planter(plantes, a, b, espece * Vegetation.VARIANTES + r.nextInt(Vegetation.VARIANTES));
            places++;
        }
    }

    private boolean pres(double a, double b, List<Rue> reseau, double marge) {
        for (Rue rue : reseau) {
            for (int q = 0; q + 1 < rue.nombre; q++) {
                if (distance(a, b, rue.points[q * 2], rue.points[q * 2 + 1], rue.points[q * 2 + 2], rue.points[q * 2 + 3]) < rue.largeur * 0.5 + marge) return true;
            }
        }
        return false;
    }

    private void planter(List<double[]> plantes, double a, double b, int sorte) {
        plantes.add(new double[] { a, b, sorte });
        grille.disque(a, b, 0.5, Grille.ARBRE);
    }

    private boolean libreAuSol(double a, double b, double rayon, int moi, List<double[]> plantes) {
        if (Math.hypot(a, b) < rayonPlace + rayon) return false;
        if (!grille.libre(a, b, 1.0, 0.0, rayon, rayon, moi, false)) return false;
        for (double[] pl : plantes) {
            if (Math.hypot(pl[0] - a, pl[1] - b) < rayon + 0.8) return false;
        }
        return true;
    }

    private void cour(Lot lot, List<Lot> objets, List<double[]> plantes, int arbre, Random r) {
        Batiments.Plan pl = lot.plan;
        double c = Math.cos(pl.angle), s = Math.sin(pl.angle);
        double dx = pl.largeur * 0.5, dz = pl.profondeur * 0.5;
        double fond = 3.5 + r.nextDouble() * 2.5, hx = dx + 0.4, z0 = -dz - 0.3, z1 = z0 - fond, hz = fond * 0.5, zc = (z0 + z1) * 0.5;
        double ca = lot.a + zc * s, cb = lot.b - zc * c;
        if (!grille.libre(ca, cb, c, s, hx + 0.3, hz + 0.3, lot.id, false)) return;
        double[] mesure = grille.mesurer(ca, cb, c, s, hx, hz);
        if (mesure[1] - mesure[0] > 2.5) return;
        grille.marquer(ca, cb, c, s, hx + 0.3, hz + 0.3, Grille.COUR, lot.id);
        int variante = r.nextInt(2);
        int porte = r.nextBoolean() ? -1 : 1;
        cloture(lot, -hx, z1, hx, z1, variante, false, objets);
        for (int k = -1; k <= 1; k += 2) cloture(lot, k * hx, z0, k * hx, z1, variante, k == porte, objets);
        double t = r.nextDouble();
        if (t < 0.65) {
            double x = (r.nextDouble() * 2.0 - 1.0) * Math.max(0.0, hx - 1.2), z = zc + (r.nextDouble() * 2.0 - 1.0) * Math.max(0.0, hz - 1.2);
            planter(plantes, lot.a + x * c + z * s, lot.b + x * s - z * c, (r.nextFloat() < 0.25f ? Vegetation.BUISSON : arbre) * Vegetation.VARIANTES + r.nextInt(Vegetation.VARIANTES));
        }
        if (t > 0.45) {
            double x = (r.nextBoolean() ? 1 : -1) * Math.max(0.0, dx - 0.7), z = z0 - 0.8;
            Lot o = new Lot();
            o.a = lot.a + x * c + z * s;
            o.b = lot.b + x * s - z * c;
            o.plan = Batiments.objet(r.nextBoolean() ? Batiments.TONNEAUX : Batiments.CAISSES, 1.4, 1.2, 0.0, pl.angle, r.nextInt(6));
            objets.add(o);
        }
    }

    private void cloture(Lot lot, double x0, double z0, double x1, double z1, int variante, boolean portail, List<Lot> objets) {
        double c = Math.cos(lot.plan.angle), s = Math.sin(lot.plan.angle);
        double l = Math.hypot(x1 - x0, z1 - z0), ux = (x1 - x0) / l, uz = (z1 - z0) / l;
        double angle = Math.atan2(ux * s - uz * c, ux * c + uz * s);
        double[] coupes = portail && l > 2.6 ? new double[] { 0.0, l * 0.5 - 0.75, l * 0.5 + 0.75, l } : new double[] { 0.0, l };
        for (int k = 0; k + 1 < coupes.length; k++) {
            double debut = coupes[k], fin = coupes[k + 1];
            boolean grille = coupes.length == 4 && k == 1;
            int pieces = grille ? 1 : Math.max(1, (int) Math.ceil((fin - debut) / 3.0));
            double longueur = (fin - debut) / pieces;
            for (int q = 0; q < pieces; q++) {
                double m = debut + longueur * (q + 0.5);
                double x = x0 + ux * m, z = z0 + uz * m;
                Lot o = new Lot();
                o.a = lot.a + x * c + z * s;
                o.b = lot.b + x * s - z * c;
                o.plan = Batiments.objet(grille ? Batiments.PORTAIL : Batiments.BARRIERE, longueur, 0.15, 0.0, angle, variante);
                objets.add(o);
            }
        }
    }

    public boolean surPont(double px, double py, double pz) {
        return surTrait(px, py, pz, true);
    }

    public boolean surRue(double px, double py, double pz) {
        return dansVillage(px, py, pz) && surTrait(px, py, pz, false);
    }

    private boolean surTrait(double px, double py, double pz, boolean pontsSeuls) {
        for (int t = 0; t + TRAIT - 1 < nombreRues; t += TRAIT) {
            if (pontsSeuls && rues[t + 7] != PONT && rues[t + 7] != PONTON) continue;
            double ax = rues[t], ay = rues[t + 1], az = rues[t + 2];
            double dx = rues[t + 3] - ax, dy = rues[t + 4] - ay, dz = rues[t + 5] - az;
            double qx = px - ax, qy = py - ay, qz = pz - az;
            double l2 = dx * dx + dy * dy + dz * dz;
            double s = l2 < 1e-30 ? 0.0 : Math.max(0.0, Math.min(1.0, (qx * dx + qy * dy + qz * dz) / l2));
            double ex = qx - dx * s, ey = qy - dy * s, ez = qz - dz * s;
            if (Math.sqrt(ex * ex + ey * ey + ez * ez) * RAYON_METRES < rues[t + 6] * 0.5) return true;
        }
        return false;
    }

    private double plusProche(double a, double b, List<Rue> reseau, Rue mere) {
        proche[0] = Double.MAX_VALUE;
        for (Rue rue : reseau) {
            if (rue == mere) continue;
            for (int s = 0; s + 1 < rue.nombre; s++) {
                double a0 = rue.points[s * 2], b0 = rue.points[s * 2 + 1], a1 = rue.points[s * 2 + 2], b1 = rue.points[s * 2 + 3];
                double da = a1 - a0, db = b1 - b0, l2 = da * da + db * db;
                double t = l2 < 1e-9 ? 0.0 : Math.max(0.0, Math.min(1.0, ((a - a0) * da + (b - b0) * db) / l2));
                double qa = a0 + da * t, qb = b0 + db * t;
                double d = Math.hypot(a - qa, b - qb);
                if (d < proche[0]) {
                    proche[0] = d;
                    proche[1] = qa;
                    proche[2] = qb;
                }
            }
        }
        return proche[0];
    }

    private boolean libre(Lot lot) {
        Batiments.Plan pl = lot.plan;
        double c = Math.cos(pl.angle), s = Math.sin(pl.angle);
        double hx = pl.largeur * 0.5 + pl.annexe(), hz = pl.profondeur * 0.5;
        if (!grille.libre(lot.a, lot.b, c, s, hx + 0.1, hz + 0.1, -1, false)) return false;
        if (!grille.libre(lot.a, lot.b, c, s, hx + 0.9, hz + 0.5, -1, true)) return false;
        double[] mesure = grille.mesurer(lot.a, lot.b, c, s, hx, hz);
        if (mesure[1] - mesure[0] > DENIVELE_MAX) return false;
        lot.sol = mesure[2];
        double ecart = Math.max(lot.sol - mesure[0], mesure[1] - lot.sol);
        lot.talus = Math.max(TALUS_MIN, Math.min(TALUS_MAX, ecart / PENTE_TALUS + 0.5));
        grille.marquer(lot.a, lot.b, c, s, hx + 0.9, hz + 0.5, Grille.BATIMENT, lot.id);
        return true;
    }

    private Lot poser(Lot modele, Batiments.Plan plan) {
        Lot lot = new Lot();
        lot.ancreA = modele.ancreA;
        lot.ancreB = modele.ancreB;
        lot.normA = modele.normA;
        lot.normB = modele.normB;
        lot.place = modele.place;
        lot.rue = modele.rue;
        lot.a = modele.ancreA + modele.normA * plan.profondeur * 0.5;
        lot.b = modele.ancreB + modele.normB * plan.profondeur * 0.5;
        lot.plan = plan;
        lot.metier = plan.metier;
        plan.orienter(Math.atan2(-modele.normA, modele.normB));
        return lot;
    }

    private double eauProche(double a, double b, double portee) {
        for (double d = 1.0; d <= portee; d += 1.0) {
            int n = Math.max(8, (int) (d * 2.0));
            for (int k = 0; k < n; k++) {
                double t = 2.0 * Math.PI * k / n;
                int i = grille.indice(a + Math.cos(t) * d, b + Math.sin(t) * d);
                if (i >= 0 && grille.occupation[i] == Grille.EAU) return d;
            }
        }
        return Double.MAX_VALUE;
    }

    private double note(Lot lot, int lieu, int grandes, Random r) {
        double distance = Math.hypot(lot.ancreA, lot.ancreB);
        switch (lieu) {
            case Metiers.PLACE:
                return lot.place ? r.nextDouble() : Double.NaN;
            case Metiers.GRANDE_RUE:
                if (lot.place || lot.rue < 0 || lot.rue >= grandes) return Double.NaN;
                return Math.abs(distance - rayonPlace - 14.0) + r.nextDouble() * 8.0;
            case Metiers.BORD:
                return lot.place ? Double.NaN : -distance + r.nextDouble() * 15.0;
            case Metiers.RIVE: {
                if (lot.place) return Double.NaN;
                if (lot.distanceEau < 0.0) lot.distanceEau = eauProche(lot.ancreA, lot.ancreB, 25.0);
                return lot.distanceEau > 25.0 ? Double.NaN : lot.distanceEau + r.nextDouble() * 4.0;
            }
            case Metiers.HAUT:
                return lot.place ? Double.NaN : -grille.hauteur(lot.ancreA, lot.ancreB) + distance * 0.03 + r.nextDouble() * 2.0;
            default:
                return lot.place ? Double.NaN : distance * 0.5 + r.nextDouble() * 25.0;
        }
    }

    private void placerMetier(int m, int rang, List<Lot> lots, List<Lot> poses, int grandes, long v, Random r) {
        boolean roue = m == Metiers.MOULIN && grille.riviere;
        for (int essai = 0; essai < 2; essai++) {
            int lieu = m == Metiers.MOULIN ? (roue ? Metiers.RIVE : Metiers.BORD) : Metiers.LIEU[m];
            int nombre = lots.size();
            double[] notes = new double[nombre];
            Integer[] ordre = new Integer[nombre];
            for (int k = 0; k < nombre; k++) {
                notes[k] = note(lots.get(k), lieu, grandes, r);
                ordre[k] = k;
            }
            Arrays.sort(ordre, (u, w) -> Double.compare(Double.isNaN(notes[u]) ? Double.MAX_VALUE : notes[u], Double.isNaN(notes[w]) ? Double.MAX_VALUE : notes[w]));
            long g = melanger(v * 7777777L + rang * 31L + m);
            for (int q = 0; q < Math.min(nombre, 120); q++) {
                int k = ordre[q];
                if (Double.isNaN(notes[k])) break;
                Lot lot = poser(lots.get(k), Batiments.planMetier(g, m, roue));
                lot.id = poses.size();
                if (!libre(lot)) continue;
                poses.add(lot);
                return;
            }
            if (!roue) return;
            roue = false;
        }
    }

    private void etals(int nombre, List<Lot> poses, List<Rue> reseau, List<Lot> objets, double premierAngle, Random r) {
        if (nombre <= 0) return;
        Batiments.Plan central = poses.get(0).plan;
        double d = (central.rayon() + rayonPlace) * 0.5 + 0.6;
        if (d < central.rayon() + 1.5) return;
        int places = 0;
        for (int k = 0; k < 40 && places < nombre; k++) {
            double ang = premierAngle + Math.PI + (k % 2 == 0 ? 1 : -1) * (k / 2) * 2.6 / d;
            double a = Math.cos(ang) * d, b = Math.sin(ang) * d;
            if (pres(a, b, reseau, 1.4)) continue;
            boolean seul = true;
            for (Lot o : objets) seul &= Math.hypot(o.a - a, o.b - b) > 2.2;
            if (!seul) continue;
            Lot etal = new Lot();
            etal.a = a;
            etal.b = b;
            etal.plan = Batiments.objet(Batiments.ETAL, 1.8, 1.2, 0.0, ang + Math.PI / 2.0, r.nextInt(8));
            objets.add(etal);
            grille.disque(a, b, 1.1, Grille.OBJET);
            places++;
        }
    }

    private Grille quadriller(List<Rue> reseau, List<double[]> pontons) {
        double etendue = rayonPlace;
        for (Rue rue : reseau) {
            for (int s = 0; s < rue.nombre; s++) etendue = Math.max(etendue, Math.max(Math.abs(rue.points[s * 2]), Math.abs(rue.points[s * 2 + 1])));
        }
        int cote = 2 * (int) Math.ceil(etendue + MARGE_GRILLE);
        Grille g = new Grille(cote);
        boolean[] rivieres = new boolean[cote];
        Terrain t = terrain;
        double cx = centreX, cy = centreY, cz = centreZ;
        double[] e = est.clone(), nd = nord.clone();
        IntStream.range(0, cote).parallel().forEach(j -> {
            Terrain.Point pt = new Terrain.Point(), sl = new Terrain.Point();
            double[] cn = new double[3];
            double b = g.centre(j) / RAYON_METRES;
            for (int i = 0; i < cote; i++) {
                double a = g.centre(i) / RAYON_METRES;
                double px = cx + e[0] * a + nd[0] * b, py = cy + e[1] * a + nd[1] * b, pz = cz + e[2] * a + nd[2] * b;
                double l = Math.sqrt(px * px + py * py + pz * pz);
                px /= l;
                py /= l;
                pz /= l;
                t.echantillonner(px, py, pz, 11, pt);
                float alt = RenduTuiles.altitudeSol(t, px, py, pz, sl, cn);
                int k = j * cote + i;
                g.sol[k] = (float) (Math.max(alt * 1000.0, 0.0) + MicroRelief.hauteur(px, py, pz, alt, t.hauteurPlaines));
                if (pt.riviere) rivieres[j] = true;
                if (pt.riviere || pt.lac || alt <= 0f) g.occupation[k] = Grille.EAU;
                else if (pt.altitude < ALTITUDE_MIN) g.occupation[k] = Grille.INTERDIT;
            }
        });
        for (boolean b : rivieres) g.riviere |= b;
        g.disque(0.0, 0.0, rayonPlace, Grille.PLACE);
        for (Rue rue : reseau) {
            for (int s = 0; s + 1 < rue.nombre; s++) g.segment(rue.points[s * 2], rue.points[s * 2 + 1], rue.points[s * 2 + 2], rue.points[s * 2 + 3], rue.largeur * 0.5 + 0.1, Grille.RUE, MARGE_GRILLE);
        }
        for (double[] o : pontons) g.segment(o[0], o[1], o[2], o[3], o[4] * 0.5, Grille.RUE, 0.0);
        return g;
    }

    private static boolean chevauche(double a1, double b1, double c1, double s1, double hx1, double hz1, double a2, double b2, double c2, double s2, double hx2, double hz2) {
        double[][] axes = { { c1, s1 }, { s1, -c1 }, { c2, s2 }, { s2, -c2 } };
        double da = a2 - a1, db = b2 - b1;
        for (double[] ax : axes) {
            double r1 = hx1 * Math.abs(c1 * ax[0] + s1 * ax[1]) + hz1 * Math.abs(s1 * ax[0] - c1 * ax[1]);
            double r2 = hx2 * Math.abs(c2 * ax[0] + s2 * ax[1]) + hz2 * Math.abs(s2 * ax[0] - c2 * ax[1]);
            if (Math.abs(da * ax[0] + db * ax[1]) > r1 + r2) return false;
        }
        return true;
    }

    private static double distance(double a, double b, double a0, double b0, double a1, double b1) {
        double da = a1 - a0, db = b1 - b0, l2 = da * da + db * db;
        double t = l2 < 1e-9 ? 0.0 : Math.max(0.0, Math.min(1.0, ((a - a0) * da + (b - b0) * db) / l2));
        return Math.hypot(a - a0 - da * t, b - b0 - db * t);
    }

    private double hauteur(double a, double b) {
        deplacer(centreX, centreY, centreZ, a / RAYON_METRES, b / RAYON_METRES);
        terrain.echantillonner(p[0], p[1], p[2], 11, point);
        if (point.altitude < ALTITUDE_MIN || point.riviere || point.lac) return Double.NaN;
        return RenduTuiles.altitudeSol(terrain, p[0], p[1], p[2], sol, coin) * 1000.0;
    }

    private void repere(double vx, double vy, double vz) {
        double h = Math.sqrt(vx * vx + vy * vy);
        est[0] = -vy / h;
        est[1] = vx / h;
        est[2] = 0.0;
        nord[0] = -vz * est[1];
        nord[1] = vz * est[0];
        nord[2] = vx * est[1] - vy * est[0];
    }

    private boolean chercherSol(double vx, double vy, double vz, Terrain terrain) {
        for (int anneau = 0; anneau <= ANNEAUX; anneau++) {
            int angles = Math.max(1, anneau * 6);
            double r = anneau * PAS_RECHERCHE / RAYON_METRES;
            for (int s = 0; s < angles; s++) {
                double t = 2.0 * Math.PI * s / angles;
                deplacer(vx, vy, vz, r * Math.cos(t), r * Math.sin(t));
                terrain.echantillonner(p[0], p[1], p[2], 11, point);
                if (point.altitude >= ALTITUDE_MIN && !point.riviere && !point.lac) return true;
            }
        }
        return false;
    }

    private void deplacer(double vx, double vy, double vz, double a, double b) {
        double px = vx + est[0] * a + nord[0] * b, py = vy + est[1] * a + nord[1] * b, pz = vz + est[2] * a + nord[2] * b;
        double l = Math.sqrt(px * px + py * py + pz * pz);
        p[0] = px / l;
        p[1] = py / l;
        p[2] = pz / l;
    }

    private void ajouter(float[] positions, int k, long g, Batiments.Plan plan) {
        if (nombre == x.length) {
            int t = nombre * 2;
            x = Arrays.copyOf(x, t);
            y = Arrays.copyOf(y, t);
            z = Arrays.copyOf(z, t);
            altitude = Arrays.copyOf(altitude, t);
            niveau = Arrays.copyOf(niveau, t);
            talus = Arrays.copyOf(talus, t);
            metier = Arrays.copyOf(metier, t);
            graine = Arrays.copyOf(graine, t);
            plans = Arrays.copyOf(plans, t);
        }
        x[nombre] = positions[k * 4];
        y[nombre] = positions[k * 4 + 1];
        z[nombre] = positions[k * 4 + 2];
        altitude[nombre] = positions[k * 4 + 3];
        graine[nombre] = g;
        plans[nombre] = plan;
        nombre++;
    }

    public boolean bloque(double px, double py, double pz, double rayon) {
        double portee = 22.0 / RAYON_METRES;
        for (int k = 0; k < nombre; k++) {
            double dx = px - x[k], dy = py - y[k], dz = pz - z[k];
            if (dx * dx + dy * dy + dz * dz > portee * portee) continue;
            double ex = -y[k], ey = x[k], n = Math.sqrt(ex * ex + ey * ey);
            ex /= n;
            ey /= n;
            double nx = -z[k] * ey, ny = z[k] * ex, nz = x[k] * ey - y[k] * ex;
            double est = (dx * ex + dy * ey) * RAYON_METRES;
            double nord = (dx * nx + dy * ny + dz * nz) * RAYON_METRES;
            if (plans[k].bloque(est, nord, rayon)) return true;
        }
        return false;
    }

    public boolean dansVillage(double px, double py, double pz) {
        for (int v = 0; v < villages; v++) {
            Village village = proches[v];
            double dx = px - village.x, dy = py - village.y, dz = pz - village.z;
            if (dx * dx + dy * dy + dz * dz < village.rayon * village.rayon) return true;
        }
        return false;
    }

    private static double hasard(int v, int i, int j) {
        return (melanger(v * 7919L + i * 104729L + j * 1299709L) >>> 11) * 0x1.0p-53;
    }

    private static long melanger(long x) {
        x ^= x >>> 33;
        x *= 0xff51afd7ed558ccdL;
        x ^= x >>> 33;
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= x >>> 33;
        return x;
    }
}
