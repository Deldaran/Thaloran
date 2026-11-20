package com.deldaran.politics;

import com.deldaran.politics.histoire.Population;
import com.deldaran.politics.histoire.Routes;
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
    private static final int VIVANT = 0, BRULE = 1, DELAISSE = 2;
    private static final int OUBLI_BRULE = 150, OUBLI_DELAISSE = 250, REOCCUPATION = 40;

    /** Lot J1 : un champ prend CHAMP nombres ; ses sortes. */
    public static final int CHAMP = 8;
    public static final int BLE = 0, LEGUMES = 1, PRE = 2, VERGER = 3, FRICHE = 4;
    /** Lot J1 : la surface de champs par habitant (m²), au plus fort de la ville. */
    private static final double SURFACE_PAR_HABITANT = 110.0;
    /** Lot J1 : les champs vont par anneaux (profondeur BANDE m, séparés par un chemin de ALLEE m), coupés en blocs d'environ BLOC m. */
    private static final double BANDE = 34.0, ALLEE = 4.0, BLOC = 46.0, ECART_BATI = 4.0;
    /** Lot J1 : autour des champs, DEGAGE m sans arbres sauvages (les chemins entre les blocs restent nus). */
    private static final double DEGAGE = 3.0;
    private static final int BANDES_MAX = 6;
    /** Lot J1 : un bloc se coupe en lanières de LANIERE_MIN à LANIERE_MAX m de large, séparées d'une bande d'herbe de BALK m. */
    private static final double LANIERE_MIN = 7.0, LANIERE_MAX = 13.0, BALK = 0.7;
    /** Lot J1 : au-delà de PENTE_CHAMP, ce ne sont que des prés ; au-delà de PENTE_PRE, rien. COULOIR : la place laissée aux routes qui partent. */
    private static final double PENTE_CHAMP = 0.16, PENTE_PRE = 0.32, COULOIR = 6.0;
    /** Lot J1 : les arbres des vergers, tous les PAS_VERGER m. */
    private static final double PAS_VERGER = 6.5;

    public static class Village {
        int maisons;
        float[] positions, sols, arbres, talus;
        public int[] metiers;
        public Grille grille;
        public float[] usure;
        public long[] graines;
        public Batiments.Plan[] plans;
        float[] rues;
        public double x, y, z, rayon;
        public int ville, peuple, habitables, ruines;
        /** Index (dans positions et plans) du tableau des annonces de la place, -1 s'il n'y en a pas. */
        public int tableau = -1;
        public float[] portes;
        public final double[] est = new double[3], nord = new double[3];
        /**
         * Lot J1 : les champs autour du village, par CHAMP : a, b (le centre, en mètres locaux), cos et sin du sens des sillons,
         * demi-longueur (le long des sillons), demi-largeur, sorte (BLE, LEGUMES, PRE, VERGER, FRICHE), variante (0 à 31).
         */
        public float[] champs = new float[0];
        /** Lot J1 : la portée des champs depuis le centre (en radians, comme rayon). */
        public double rayonChamps;

        public void local(double px, double py, double pz, double[] sortie) {
            double dx = px - x, dy = py - y, dz = pz - z;
            sortie[0] = (dx * est[0] + dy * est[1] + dz * est[2]) * RAYON_METRES;
            sortie[1] = (dx * nord[0] + dy * nord[1] + dz * nord[2]) * RAYON_METRES;
        }

        public void monde(double a, double b, double[] sortie) {
            double u = a / RAYON_METRES, v = b / RAYON_METRES;
            double px = x + est[0] * u + nord[0] * v, py = y + est[1] * u + nord[1] * v, pz = z + est[2] * u + nord[2] * v;
            double l = Math.sqrt(px * px + py * py + pz * pz);
            sortie[0] = px / l;
            sortie[1] = py / l;
            sortie[2] = pz / l;
        }
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

    /**
     * Lot G5 : un bâtiment du joueur sur un terrain libre. (a, b) est le point où il se tenait, devant la porte, en mètres locaux
     * du village ; angle est la direction où il regardait (le bâtiment est devant lui, sa façade tournée vers lui).
     */
    public static final class Ajout {
        public double a, b, angle;
        public int metier;
        public boolean chantier;
    }

    /** Lot G5 : les bâtiments du joueur d'une ville (fournis par l'écran, lus dans l'histoire) ; ils s'ajoutent à la fin du village. */
    public java.util.function.IntFunction<List<Ajout>> ajouts;
    /** Lot G5 : la porte est à PORTE_RECUL m du joueur. */
    public static final double PORTE_RECUL = 1.5;

    public static long graineAjout(int ville, Ajout x) {
        return melanger(ville * 1000003L + Math.round(x.a * 10.0) * 7919L + Math.round(x.b * 10.0) * 104729L + x.metier * 15485863L);
    }

    /** Lot G5 : le plan d'un bâtiment du joueur ; son centre (en mètres locaux) dans centre[0], centre[1]. */
    public static Batiments.Plan planAjout(int ville, Ajout x, double[] centre) {
        Batiments.Plan pl = Batiments.planMetier(graineAjout(ville, x), x.metier, false);
        double fa = Math.cos(x.angle), fb = Math.sin(x.angle);
        pl.orienter(Math.atan2(-fa, fb));
        double recul = pl.profondeur * 0.5 + PORTE_RECUL;
        centre[0] = x.a + fa * recul;
        centre[1] = x.b + fb * recul;
        return pl;
    }

    /** Lot G5 : le terrain est-il libre pour ce plan (rien de bâti, pas de rue sous les murs, pente modérée) ? Mêmes règles que les lots du village. */
    public static boolean terrainLibre(Grille g, Batiments.Plan pl, double ca, double cb) {
        double c = Math.cos(pl.angle), s = Math.sin(pl.angle);
        double hx = pl.largeur * 0.5 + pl.annexe(), hz = pl.profondeur * 0.5;
        if (!g.libre(ca, cb, c, s, hx + 0.1, hz + 0.1, -1, false)) return false;
        if (!g.libre(ca, cb, c, s, hx + 0.9, hz + 0.5, -1, true)) return false;
        double[] m = g.mesurer(ca, cb, c, s, hx, hz);
        return m[1] - m[0] <= DENIVELE_MAX;
    }

    /** Lot G5 : oublie le village d'une ville (il se refera, avec les bâtiments du joueur) ; rend l'ancien, ou null. */
    public Village oublier(int ville) {
        place = false;
        return connus.remove(ville);
    }

    private static class Lot {
        double priorite, a, b, eau = Double.NaN, sol = Double.NaN, talus = TALUS_MIN;
        double ancreA, ancreB, normA, normB, distanceEau = -1.0;
        boolean place;
        /** Lot G5 : un bâtiment du joueur (il garde son sol, comme une maison). */
        boolean joueur;
        int id = -1, rue = -1, metier, etat, annee;
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
        this.sphere = sphere;
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
        fixes = nombre;
    }

    /** Un plan de village a changé (un atelier bâti sur une ruine) : les tableaux se refont au prochain mettreAJour. */
    public void rafraichir() {
        place = false;
    }

    /** Le nombre de bâtiments des villages ; au-delà viennent des objets posés hors des villes (le mobilier des campements). */
    private int fixes;

    /** Pose les objets hors des villes (x, y, z, altitude par quatre), à la suite des bâtiments ; l'écran les repose à chaque image. */
    public void poserObjets(float[] positions, List<Batiments.Plan> objets, int n) {
        nombre = fixes;
        for (int k = 0; k < n; k++) {
            Batiments.Plan plan = objets.get(k);
            ajouter(positions, k, plan.graine, plan);
            niveau[nombre - 1] = 0f;
            talus[nombre - 1] = 0f;
            metier[nombre - 1] = -1;
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

        Metiers.Profil profil = Metiers.profil(population, v);
        double etendue = 30.0 + Math.sqrt(n) * 9.0;
        grille = sol(etendue);
        profil.eau = grille.riviere || eauProche(0.0, 0.0, 120.0) < 120.0;
        profil.riviere = grille.riviere;
        List<Rue> reseau = new ArrayList<>();
        List<Lot> objets = new ArrayList<>();
        List<double[]> pontons = new ArrayList<>();
        List<Lot> poses = new ArrayList<>();
        Lot centre = new Lot();
        centre.plan = central;
        centre.id = 0;
        poses.add(centre);
        double premierAngle = croitre(v, n, etendue, poses, profil, r);
        central.orienter(Math.atan2(Math.cos(premierAngle), -Math.sin(premierAngle)));
        tracerRues(reseau, n);
        if (profil.eau && n >= 12) port(reseau, objets, pontons, n, v, r);
        objets.addAll(objetsCroissance);
        for (Rue rue : reseau) {
            for (int q = 0; q + 1 < rue.nombre; q++) grille.segment(rue.points[q * 2], rue.points[q * 2 + 1], rue.points[q * 2 + 2], rue.points[q * 2 + 3], rue.largeur * 0.5 + 0.1, Grille.RUE, MARGE_GRILLE);
        }
        for (double[] o : pontons) grille.segment(o[0], o[1], o[2], o[3], o[4] * 0.5, Grille.RUE, 0.0);
        ponts(reseau, objets, v);

        List<double[]> plantes = new ArrayList<>();
        amenager(poses, reseau, objets, plantes, premierAngle, n, r);
        etals(Metiers.etals(profil), poses, reseau, objets, premierAngle, r);
        // Hasard à part : le tableau ne change rien au reste du village.
        tableau(poses, reseau, objets, plantes, premierAngle, new Random(melanger(v * 131L + 3L)));
        // Lot G5 : les bâtiments du joueur, après tout le reste : le village se génère comme avant et ses index ne bougent pas.
        List<Ajout> aj = ajouts == null ? null : ajouts.apply(v);
        if (aj != null) {
            for (Ajout x : aj) {
                double[] ctr = new double[2];
                Batiments.Plan pl = planAjout(v, x, ctr);
                if (x.chantier) {
                    pl = pl.ruiner(false, 0.0);
                    pl.chantier = true;
                }
                Lot lot = new Lot();
                lot.a = ctr[0];
                lot.b = ctr[1];
                lot.plan = pl;
                lot.metier = x.metier;
                lot.joueur = true;
                pl.joueur = true;
                double c = Math.cos(pl.angle), s = Math.sin(pl.angle);
                double hx = pl.largeur * 0.5 + pl.annexe(), hz = pl.profondeur * 0.5;
                double[] mesure = grille.mesurer(lot.a, lot.b, c, s, hx, hz);
                lot.sol = mesure[2];
                double ecart = Math.max(lot.sol - mesure[0], mesure[1] - lot.sol);
                lot.talus = Math.max(TALUS_MIN, Math.min(TALUS_MAX, ecart / PENTE_TALUS + 0.5));
                grille.marquer(lot.a, lot.b, c, s, hx + 0.1, hz + 0.1, Grille.BATIMENT, -1);
                // Les plantes du village sous les murs sont arrachées.
                plantes.removeIf(f -> {
                    double da = f[0] - lot.a, db = f[1] - lot.b;
                    return Math.abs(da * c + db * s) <= hx + 0.5 && Math.abs(da * s - db * c) <= hz + 0.5;
                });
                objets.add(lot);
            }
        }
        int maisons = poses.size();
        poses.addAll(objets);
        int total = poses.size();
        float[] positions = new float[total * 4];
        float[] portes = new float[maisons * 2];
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
            sols[q] = q < maisons || lot.plan.ruine || lot.joueur ? (float) (Double.isNaN(lot.sol) ? hauteurSol() : lot.sol) : 0f;
            if (lot.plan.ruine) village.ruines++;
            talus[q] = (float) lot.talus;
            metiers[q] = lot.metier;
            graines[q] = lot.plan.graine;
            if (q < maisons) {
                boolean face = lot.normA != 0.0 || lot.normB != 0.0;
                portes[q * 2] = (float) (face ? lot.ancreA - lot.normA * 1.2 : lot.a);
                portes[q * 2 + 1] = (float) (face ? lot.ancreB - lot.normB * 1.2 : lot.b);
            }
            lesPlans[q] = lot.plan;
            if (q >= maisons && lot.plan.style == Batiments.TABLEAU) village.tableau = q;
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
        village.portes = portes;
        village.habitables = maisons;
        village.ville = v;
        village.peuple = population.villes.peuple[v];
        System.arraycopy(est, 0, village.est, 0, 3);
        System.arraycopy(nord, 0, village.nord, 0, 3);
        village.arbres = feuillage;
        village.sols = sols;
        village.talus = talus;
        village.metiers = metiers;
        village.grille = grille;
        village.usure = usure;
        village.graines = graines;
        village.plans = lesPlans;
        village.rues = traits;
        village.x = centreX;
        village.y = centreY;
        village.z = centreZ;
        village.rayon = (rayon + MARGE) / RAYON_METRES;
        champs(v, village, rayon + MARGE, profil);
        return village;
    }

    /**
     * Lot J1 : les champs, prés et vergers autour du village. Leur surface suit le plus fort de la ville (pic) ; quand elle a décliné,
     * une part retourne à la friche. Ils évitent l'eau, les pentes, les routes qui partent et les autres villages. Hasard à part : le
     * village lui-même ne change pas.
     */
    private void champs(int v, Village village, double bati, Metiers.Profil profil) {
        Random r = new Random(melanger(v * 977L + 11L));
        int gens = Math.max(profil.habitants, profil.pic);
        double voulu = Math.max(12, gens) * SURFACE_PAR_HABITANT;
        double vivants = profil.pic > 0 ? Math.min(1.0, profil.habitants / (double) profil.pic) : 1.0;
        double[] sorties = new double[lesSorties == null ? 0 : lesSorties.length];
        for (int k = 0; k < sorties.length; k++) sorties[k] = Math.atan2(lesSorties[k][1], lesSorties[k][0]);
        List<float[]> liste = new ArrayList<>();
        List<double[]> arbres = new ArrayList<>();
        double fait = 0.0, portee = bati;
        double[] alt = new double[9];
        for (int bande = 0; bande < BANDES_MAX && fait < voulu; bande++) {
            double ri = bati + ECART_BATI + bande * (BANDE + ALLEE), rm = ri + BANDE * 0.5;
            int blocs = Math.max(4, (int) Math.round(2.0 * Math.PI * rm / BLOC));
            double pas = 2.0 * Math.PI / blocs, decalage = r.nextDouble() * pas;
            // Les blocs à la suite, depuis un angle au hasard : un anneau à moitié rempli fait un arc d'un seul tenant.
            int[] ordre = new int[blocs];
            int premier = r.nextInt(blocs);
            for (int k = 0; k < blocs; k++) ordre[k] = (premier + k) % blocs;
            for (int idx : ordre) {
                if (fait >= voulu) break;
                double th = decalage + idx * pas;
                double demiT = ri * Math.sin(pas * 0.5) - ALLEE * 0.5, demiR = BANDE * 0.5;
                if (demiT < 6.0) continue;
                double ca = Math.cos(th), sa = Math.sin(th), ma = rm * ca, mb = rm * sa;
                // Les routes qui partent du village.
                boolean route = false;
                for (double s : sorties) {
                    double d = Math.abs(Math.IEEEremainder(th - s, 2.0 * Math.PI));
                    if (d < Math.PI * 0.5 && Math.sin(d) * (rm - demiR) < demiT + COULOIR) route = true;
                }
                if (route) continue;
                // Le terrain : neuf points, ni eau ni mer, pente modérée.
                boolean bon = true;
                for (int q = 0; q < 9 && bon; q++) {
                    double fr = (q % 3 - 1) * 0.9 * demiR, ft = (q / 3 - 1) * 0.9 * demiT;
                    alt[q] = altitudeChamp(ma + ca * fr - sa * ft, mb + sa * fr + ca * ft);
                    bon = !Double.isNaN(alt[q]);
                }
                if (!bon || autreVillage(village, ma, mb, Math.hypot(demiR, demiT))) continue;
                double pente = 0.0;
                for (int q = 0; q < 9; q++) {
                    if (q % 3 < 2) pente = Math.max(pente, Math.abs(alt[q + 1] - alt[q]) / (0.9 * demiR));
                    if (q / 3 < 2) pente = Math.max(pente, Math.abs(alt[q + 3] - alt[q]) / (0.9 * demiT));
                }
                if (pente > PENTE_PRE) continue;
                // La sorte du bloc : les pentes en prés ; quelques prés et vergers ; le reste en lanières cultivées.
                double tirage = r.nextDouble();
                int sorteBloc = pente > PENTE_CHAMP ? PRE : tirage < 0.14 ? PRE : tirage < 0.22 && bande <= 1 ? VERGER : -1;
                boolean radial = r.nextBoolean();
                double longue = radial ? demiR : demiT, large = radial ? demiT : demiR;
                double angle = radial ? th : th + Math.PI * 0.5;
                double cl = Math.cos(angle), sl = Math.sin(angle);
                int lanieres = sorteBloc >= 0 ? 1 : Math.max(1, (int) Math.round(2.0 * large / (LANIERE_MIN + r.nextDouble() * (LANIERE_MAX - LANIERE_MIN))));
                double w = 2.0 * large / lanieres;
                for (int k = 0; k < lanieres; k++) {
                    double o = -large + w * (k + 0.5);
                    double a = ma - sl * o, b = mb + cl * o;
                    int sorte = sorteBloc;
                    if (sorte < 0) sorte = bande == 0 && r.nextDouble() < 0.35 ? LEGUMES : r.nextDouble() < 0.06 ? PRE : BLE;
                    // La ville a décliné : les champs les plus loin retournent les premiers à la friche.
                    if (r.nextDouble() < (1.0 - vivants) * (0.6 + bande * 0.5)) sorte = FRICHE;
                    float dl = (float) (longue - BALK * 0.5), dw = (float) (w * 0.5 - BALK * 0.5);
                    liste.add(new float[] { (float) a, (float) b, (float) cl, (float) sl, dl, dw, sorte, r.nextInt(32) });
                    if (sorte == VERGER) {
                        for (double x = -dl + 2.5; x <= dl - 2.5; x += PAS_VERGER) {
                            for (double z = -dw + 2.5; z <= dw - 2.5; z += PAS_VERGER) {
                                arbres.add(new double[] { a + cl * x - sl * z, b + sl * x + cl * z, Vegetation.FEUILLU * Vegetation.VARIANTES + r.nextInt(Vegetation.VARIANTES) });
                            }
                        }
                    }
                }
                fait += 4.0 * demiR * demiT;
                portee = Math.max(portee, rm + Math.hypot(demiR, demiT));
            }
        }
        float[] t = new float[liste.size() * CHAMP];
        for (int k = 0; k < liste.size(); k++) System.arraycopy(liste.get(k), 0, t, k * CHAMP, CHAMP);
        village.champs = t;
        village.rayonChamps = (portee + 2.0) / RAYON_METRES;
        if (!arbres.isEmpty()) {
            int n0 = village.arbres.length;
            float[] f = Arrays.copyOf(village.arbres, n0 + arbres.size() * 5);
            for (int q = 0; q < arbres.size(); q++) {
                double[] pl = arbres.get(q);
                deplacer(centreX, centreY, centreZ, pl[0] / RAYON_METRES, pl[1] / RAYON_METRES);
                f[n0 + q * 5] = (float) p[0];
                f[n0 + q * 5 + 1] = (float) p[1];
                f[n0 + q * 5 + 2] = (float) p[2];
                f[n0 + q * 5 + 3] = (float) pl[2];
                f[n0 + q * 5 + 4] = RenduTuiles.altitudeSol(terrain, p[0], p[1], p[2], sol, coin);
            }
            village.arbres = f;
        }
    }

    /** Lot J1 : l'altitude (m) d'un point hors du village (mètres locaux), ou NaN s'il est dans l'eau ou trop bas. */
    private double altitudeChamp(double a, double b) {
        deplacer(centreX, centreY, centreZ, a / RAYON_METRES, b / RAYON_METRES);
        terrain.echantillonner(p[0], p[1], p[2], 11, point);
        if (point.riviere || point.lac || point.altitude < ALTITUDE_MIN) return Double.NaN;
        float altitude = RenduTuiles.altitudeSol(terrain, p[0], p[1], p[2], sol, coin);
        if (altitude <= 0f) return Double.NaN;
        return altitude * 1000.0 + MicroRelief.hauteur(p[0], p[1], p[2], altitude, terrain.hauteurPlaines);
    }

    /** Lot J1 : ce point (mètres locaux) touche-t-il un autre village déjà connu ? */
    private boolean autreVillage(Village moi, double a, double b, double rayon) {
        deplacer(centreX, centreY, centreZ, a / RAYON_METRES, b / RAYON_METRES);
        for (Village w : connus.values()) {
            if (w == moi || w.ville == moi.ville || w.positions == null) continue;
            double dx = p[0] - w.x, dy = p[1] - w.y, dz = p[2] - w.z;
            double r = w.rayon + rayon / RAYON_METRES;
            if (dx * dx + dy * dy + dz * dz < r * r) return true;
        }
        return false;
    }

    private final double[] localChamp = new double[2];

    /** Lot J1 : ce point est-il dans un champ (hors friche) ? Les arbres sauvages n'y poussent pas. */
    public boolean dansChamp(double px, double py, double pz) {
        for (int v = 0; v < villages; v++) {
            Village w = proches[v];
            float[] c = w.champs;
            if (c.length == 0) continue;
            double dx = px - w.x, dy = py - w.y, dz = pz - w.z;
            if (dx * dx + dy * dy + dz * dz > w.rayonChamps * w.rayonChamps) continue;
            w.local(px, py, pz, localChamp);
            for (int k = 0; k + CHAMP - 1 < c.length; k += CHAMP) {
                if (c[k + 6] == FRICHE) continue;
                double da = localChamp[0] - c[k], db = localChamp[1] - c[k + 1];
                double x = da * c[k + 2] + db * c[k + 3], z = -da * c[k + 3] + db * c[k + 2];
                if (Math.abs(x) < c[k + 4] + DEGAGE && Math.abs(z) < c[k + 5] + DEGAGE) return true;
            }
        }
        return false;
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

    public int nombreVillages() {
        return villages;
    }

    public Village village(int i) {
        return proches[i];
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

    /** Le tableau des annonces (missions) : sur la place, du côté opposé au puits, face au centre. */
    private void tableau(List<Lot> poses, List<Rue> reseau, List<Lot> objets, List<double[]> plantes, double premierAngle, Random r) {
        Batiments.Plan central = poses.get(0).plan;
        double d = Math.max((central.rayon() + rayonPlace) * 0.5, central.rayon() + 1.0);
        for (int k = 0; k < 24; k++) {
            double ang = premierAngle - 0.9 + (k % 2 == 0 ? 1 : -1) * (k / 2) * 0.35;
            double a = Math.cos(ang) * d, b = Math.sin(ang) * d;
            if (pres(a, b, reseau, 1.0) || Double.isNaN(hauteur(a, b))) continue;
            boolean seul = true;
            for (Lot o : objets) seul &= Math.hypot(o.a - a, o.b - b) > 2.2;
            for (double[] pl : plantes) seul &= Math.hypot(pl[0] - a, pl[1] - b) > 1.5;
            if (!seul) continue;
            Lot t = new Lot();
            t.a = a;
            t.b = b;
            t.plan = Batiments.objet(Batiments.TABLEAU, 1.8, 0.5, 0.0, 0.0, r.nextInt(8)); // face à la caméra : les affiches se voient
            objets.add(t);
            grille.disque(a, b, 1.0, Grille.OBJET);
            return;
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

    private static final int PAS_ANS = 5;
    private static final int SEUIL_SENTIER = 2, SEUIL_CHEMIN = 5, SEUIL_RUE = 12, SEUIL_GRANDE = 28;
    private static final int EXPLORATION_MAX = 120000;
    private static final int RESEAU = 0, VERS_PLACE = 1, VERS_POINT = 2, VERS_EAU = 3;
    private Sphere sphere;
    private float[] usure;
    private final List<int[]> traces = new ArrayList<>();
    private final List<Integer> casesChemin = new ArrayList<>();
    private final List<Lot> objetsCroissance = new ArrayList<>();
    private final List<double[]> lesPuits = new ArrayList<>();
    private float[] coutA;
    private int[] venuA, marqueA;
    private int tourA;
    private double[] tasF = new double[1024];
    private int[] tasK = new int[1024];
    private int nTas;
    private double[][] lesSorties;
    private double[] rive;
    private double etendueVille;
    private double cibleA, cibleB, cibleRayon;

    private Grille sol(double etendue) {
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
        return g;
    }

    private double croitre(int v, int n, double etendue, List<Lot> poses, Metiers.Profil profil, Random r) {
        int total = grille.cote * grille.cote;
        usure = new float[total];
        coutA = new float[total];
        venuA = new int[total];
        marqueA = new int[total];
        tourA = 0;
        traces.clear();
        casesChemin.clear();
        objetsCroissance.clear();
        lesPuits.clear();
        rive = null;
        etendueVille = etendue;
        grille.disque(0.0, 0.0, rayonPlace, Grille.PLACE);
        lesSorties = sorties(v, etendue, r);
        double premierAngle = Math.atan2(lesSorties[0][1], lesSorties[0][0]);
        for (double[] s : lesSorties) {
            int[] c = chercher(grille.indice(s[0], s[1]), VERS_PLACE);
            if (c != null) deposer(c, 3f);
        }
        Villes villes = population.villes;
        int debut = villes.fondation[v], fin = population.annee;
        int etapes = Math.max(1, (fin - debut) / PAS_ANS + 1);
        int[] hab = new int[etapes];
        int maxi = 1;
        for (int k = 0; k < etapes; k++) {
            maxi = Math.max(maxi, villes.habitantsEn(v, Math.min(fin, debut + k * PAS_ANS)));
            hab[k] = maxi;
        }
        maxi = Math.max(maxi, villes.habitants[v]);
        int actuel = Math.max(1, villes.habitants[v]), plafond = Math.min(MAISONS_MAX, n * 3 + 20);
        List<int[]> sacs = new ArrayList<>();
        for (int e = 0; e < villes.nombreEvenements; e++) {
            if (villes.evVille[e] == v && villes.evType[e] == Villes.SACCAGEE) sacs.add(new int[] { villes.evAnnee[e], villes.evValeur[e] });
        }
        int[] places = new int[Metiers.NOMBRE];
        int dernierTrafic = 0, sac = 0;
        long compteur = 0;
        for (int k = 0; k < etapes; k++) {
            int annee = k == etapes - 1 ? fin : Math.min(fin, debut + k * PAS_ANS);
            while (sac < sacs.size() && sacs.get(sac)[0] <= annee) incendier(v, poses, sacs.get(sac++));
            int voulu = Math.min(plafond, Math.max(1, (int) Math.round((double) n * villes.habitantsEn(v, annee) / actuel)));
            if (k == etapes - 1) voulu = n;
            Metiers.Profil pt = copie(profil, hab[k], k == etapes - 1);
            int[] besoins = Metiers.besoins(pt);
            int[] vus = new int[Metiers.NOMBRE];
            for (int q = 0; q < besoins.length; q++) {
                int m = besoins[q];
                if (++vus[m] <= places[m]) continue;
                places[m] = vus[m];
                placerAtelier(m, q, v, poses, r);
            }
            int vivants = vivants(poses);
            int essais = 0;
            while (vivants < voulu && essais < 3 * (voulu - vivants) + 4) {
                essais++;
                if (relever(poses, annee) || placerMaison(v, ++compteur, poses, r)) vivants++;
            }
            if (vivants > voulu) delaisser(poses, vivants - voulu, annee, r);
            if (vivants >= dernierTrafic * 1.25 + 4) {
                trafic(poses, r);
                dernierTrafic = vivants;
            }
        }
        trafic(poses, r);
        trafic(poses, r);
        if (profil.eau && n >= 12) chercherRive();
        for (int q = poses.size() - 1; q >= 1; q--) {
            Lot lot = poses.get(q);
            if (lot.etat == VIVANT) continue;
            poses.remove(q);
            int age = fin - lot.annee;
            if (age > (lot.etat == BRULE ? OUBLI_BRULE : OUBLI_DELAISSE)) continue;
            lot.plan = lot.plan.ruiner(lot.etat == DELAISSE, age);
            objetsCroissance.add(lot);
        }
        return premierAngle;
    }

    private int vivants(List<Lot> poses) {
        int n = 0;
        for (int q = 1; q < poses.size(); q++) if (poses.get(q).etat == VIVANT) n++;
        return n;
    }

    private void incendier(int v, List<Lot> poses, int[] sac) {
        int annee = sac[0];
        Random r = new Random(melanger(v * 92821L + annee * 31L + 5L));
        List<Lot> proies = new ArrayList<>();
        for (int q = 1; q < poses.size(); q++) {
            Lot lot = poses.get(q);
            if (lot.etat != BRULE && lot.plan.style < Batiments.ARCHE) proies.add(lot);
        }
        if (proies.isEmpty()) return;
        int avant = Math.max(1, population.villes.habitantsEn(v, annee - Villes.PAS_MESURE));
        double part = Math.max(0.15, Math.min(0.9, 0.15 + 1.5 * sac[1] / (double) avant));
        Lot depart = proies.get(r.nextInt(proies.size()));
        double vent = r.nextDouble() * 2.0 * Math.PI, cv = Math.cos(vent), sv = Math.sin(vent);
        double[] distance = new double[proies.size()];
        Integer[] ordre = new Integer[proies.size()];
        for (int q = 0; q < proies.size(); q++) {
            Lot lot = proies.get(q);
            double da = lot.a - depart.a, db = lot.b - depart.b;
            double long_ = da * cv + db * sv, travers = -da * sv + db * cv;
            distance[q] = Math.hypot(long_ < 0.0 ? long_ * 1.6 : long_ * 0.7, travers) * (0.75 + 0.5 * r.nextDouble());
            ordre[q] = q;
        }
        Arrays.sort(ordre, (x, y) -> Double.compare(distance[x], distance[y]));
        int brules = Math.max(1, (int) Math.ceil(part * proies.size()));
        for (int q = 0; q < brules; q++) {
            Lot lot = proies.get(ordre[q]);
            lot.etat = BRULE;
            lot.annee = annee;
        }
    }

    private boolean relever(List<Lot> poses, int annee) {
        Lot meilleur = null;
        double proche = Double.MAX_VALUE;
        for (int q = 1; q < poses.size(); q++) {
            Lot lot = poses.get(q);
            if (lot.etat == VIVANT) continue;
            if (lot.etat == BRULE && annee - lot.annee < 4 + Math.floorMod(lot.plan.graine, 11L)) continue;
            double d = Math.hypot(lot.a, lot.b);
            if (d < proche) {
                proche = d;
                meilleur = lot;
            }
        }
        if (meilleur == null) return false;
        if (meilleur.etat == BRULE || annee - meilleur.annee > REOCCUPATION) {
            Batiments.Plan pl = meilleur.plan;
            pl.graine = melanger(pl.graine + annee);
            if (meilleur.etat == BRULE && pl.metier == Metiers.MAISON && (pl.style == Batiments.CHAUMIERE || pl.style == Batiments.RONDINS) && (pl.graine & 1L) == 0L) {
                pl.style = (pl.graine & 2L) == 0L ? Batiments.COLOMBAGES : Batiments.PIERRE;
            }
        }
        meilleur.etat = VIVANT;
        meilleur.annee = annee;
        return true;
    }

    private void delaisser(List<Lot> poses, int nombre, int annee, Random r) {
        List<Lot> liste = new ArrayList<>();
        for (int q = 1; q < poses.size(); q++) {
            Lot lot = poses.get(q);
            if (lot.etat == VIVANT && lot.metier == Metiers.MAISON) liste.add(lot);
        }
        double[] note = new double[liste.size()];
        Integer[] ordre = new Integer[liste.size()];
        for (int q = 0; q < liste.size(); q++) {
            note[q] = Math.hypot(liste.get(q).a, liste.get(q).b) + r.nextDouble() * 15.0;
            ordre[q] = q;
        }
        Arrays.sort(ordre, (x, y) -> Double.compare(note[y], note[x]));
        for (int q = 0; q < Math.min(nombre, liste.size()); q++) {
            Lot lot = liste.get(ordre[q]);
            lot.etat = DELAISSE;
            lot.annee = annee;
        }
    }

    private Metiers.Profil copie(Metiers.Profil p, int habitants, boolean dernier) {
        Metiers.Profil c = new Metiers.Profil();
        c.habitants = habitants;
        c.pic = p.pic;
        c.age = p.age;
        c.peuple = p.peuple;
        c.capitale = p.capitale && dernier;
        c.eau = p.eau;
        c.riviere = p.riviere;
        c.nourriture = p.nourriture;
        c.bois = p.bois;
        c.minerai = p.minerai;
        c.peche = p.peche;
        return c;
    }

    private double[][] sorties(int v, double etendue, Random r) {
        List<double[]> liste = new ArrayList<>();
        Routes rt = population.routes;
        int c = population.villes.caseDe[v];
        if (sphere != null) {
            for (int k = 0; k < rt.nombre; k++) {
                if (rt.fin[k] != Routes.OUVERTE || (rt.villeA[k] != v && rt.villeB[k] != v)) continue;
                int[] cs = rt.cases[k];
                if (cs.length < 2) continue;
                int voisin = rt.villeA[k] == v ? cs[1] : cs[cs.length - 2];
                double dx = sphere.x[voisin] - sphere.x[c], dy = sphere.y[voisin] - sphere.y[c], dz = sphere.z[voisin] - sphere.z[c];
                double angle = Math.atan2(dx * nord[0] + dy * nord[1] + dz * nord[2], dx * est[0] + dy * est[1] + dz * est[2]);
                boolean fusion = false;
                for (double[] s : liste) {
                    double ecart = Math.abs(Math.IEEEremainder(angle - s[3], 2.0 * Math.PI));
                    if (ecart < Math.toRadians(25.0)) {
                        s[2] = Math.max(s[2], rt.niveau[k]);
                        fusion = true;
                        break;
                    }
                }
                if (!fusion) liste.add(new double[] { 0.0, 0.0, rt.niveau[k], angle });
            }
        }
        if (liste.isEmpty()) liste.add(new double[] { 0.0, 0.0, 1.0, r.nextDouble() * 2.0 * Math.PI });
        List<double[]> valides = new ArrayList<>();
        for (double[] s : liste) {
            double ca = Math.cos(s[3]), cb = Math.sin(s[3]);
            for (double d = etendue - 2.0; d > rayonPlace + 6.0; d -= 2.0) {
                int i = grille.indice(ca * d, cb * d);
                if (i < 0 || !passable(i)) continue;
                s[0] = ca * d;
                s[1] = cb * d;
                valides.add(s);
                break;
            }
        }
        if (valides.isEmpty()) valides.add(new double[] { rayonPlace + 8.0, 0.0, 1.0, 0.0 });
        valides.sort((x, y) -> Double.compare(y[2], x[2]));
        return valides.toArray(new double[0][]);
    }

    private boolean passable(int k) {
        byte o = grille.occupation[k];
        return o == Grille.VIDE || o == Grille.RUE || o == Grille.PLACE || o == Grille.ARBRE;
    }

    private int[] chercher(int depart, int mode) {
        if (depart < 0) return null;
        int cote = grille.cote;
        tourA++;
        nTas = 0;
        coutA[depart] = 0f;
        venuA[depart] = -1;
        marqueA[depart] = tourA;
        pousser(heuristique(depart, mode), depart);
        int explores = 0, trouve = -1;
        while (nTas > 0 && explores < EXPLORATION_MAX) {
            int k = tasK[0];
            double f = tasF[0];
            retirer();
            if (f - heuristique(k, mode) > coutA[k] + 1e-4) continue;
            if (atteint(k, mode, depart)) {
                trouve = k;
                break;
            }
            explores++;
            int i = k % cote, j = k / cote;
            for (int dj = -1; dj <= 1; dj++) {
                for (int di = -1; di <= 1; di++) {
                    if (di == 0 && dj == 0) continue;
                    int ni = i + di, nj = j + dj;
                    if (ni < 0 || nj < 0 || ni >= cote || nj >= cote) continue;
                    int q = nj * cote + ni;
                    if (!passable(q)) continue;
                    double base = di != 0 && dj != 0 ? 1.4142 : 1.0;
                    double pente = Math.abs(grille.sol[q] - grille.sol[k]) / base;
                    if (pente > 0.6) continue;
                    double facteur = (1.0 + 5.0 * pente) * (usure[q] >= SEUIL_SENTIER ? 0.45 : 1.0);
                    float g = (float) (coutA[k] + base * facteur);
                    if (marqueA[q] == tourA && g >= coutA[q]) continue;
                    marqueA[q] = tourA;
                    coutA[q] = g;
                    venuA[q] = k;
                    pousser(g + heuristique(q, mode), q);
                }
            }
        }
        if (trouve < 0) return null;
        int longueur = 0;
        for (int k = trouve; k >= 0; k = venuA[k]) longueur++;
        int[] chemin = new int[longueur];
        int p = longueur;
        for (int k = trouve; k >= 0; k = venuA[k]) chemin[--p] = k;
        return chemin;
    }

    private boolean atteint(int k, int mode, int depart) {
        switch (mode) {
            case RESEAU:
                return k != depart && (usure[k] >= SEUIL_SENTIER || grille.occupation[k] == Grille.PLACE);
            case VERS_PLACE:
                return grille.occupation[k] == Grille.PLACE;
            case VERS_POINT:
                return Math.hypot(grille.centre(k % grille.cote) - cibleA, grille.centre(k / grille.cote) - cibleB) <= cibleRayon;
            default: {
                int cote = grille.cote, i = k % cote, j = k / cote;
                for (int d = 0; d < 4; d++) {
                    int ni = i + (d == 0 ? 1 : d == 1 ? -1 : 0), nj = j + (d == 2 ? 1 : d == 3 ? -1 : 0);
                    if (ni >= 0 && nj >= 0 && ni < cote && nj < cote && grille.occupation[nj * cote + ni] == Grille.EAU) return true;
                }
                return false;
            }
        }
    }

    private double heuristique(int k, int mode) {
        if (mode == VERS_POINT) return Math.max(0.0, Math.hypot(grille.centre(k % grille.cote) - cibleA, grille.centre(k / grille.cote) - cibleB) - cibleRayon) * 0.9;
        if (mode == VERS_PLACE) return Math.max(0.0, Math.hypot(grille.centre(k % grille.cote), grille.centre(k / grille.cote)) - rayonPlace) * 0.9;
        return 0.0;
    }

    private void pousser(double f, int k) {
        if (nTas == tasF.length) {
            tasF = Arrays.copyOf(tasF, nTas * 2);
            tasK = Arrays.copyOf(tasK, nTas * 2);
        }
        int i = nTas++;
        while (i > 0) {
            int parent = (i - 1) / 2;
            if (tasF[parent] <= f) break;
            tasF[i] = tasF[parent];
            tasK[i] = tasK[parent];
            i = parent;
        }
        tasF[i] = f;
        tasK[i] = k;
    }

    private void retirer() {
        nTas--;
        if (nTas == 0) return;
        double f = tasF[nTas];
        int k = tasK[nTas];
        int i = 0;
        while (true) {
            int g = i * 2 + 1;
            if (g >= nTas) break;
            if (g + 1 < nTas && tasF[g + 1] < tasF[g]) g++;
            if (tasF[g] >= f) break;
            tasF[i] = tasF[g];
            tasK[i] = tasK[g];
            i = g;
        }
        tasF[i] = f;
        tasK[i] = k;
    }

    private void deposer(int[] chemin, float quantite) {
        int cote = grille.cote;
        for (int k : chemin) {
            float avant = usure[k];
            usure[k] += quantite;
            double a = grille.centre(k % cote), b = grille.centre(k / cote);
            if (avant < SEUIL_SENTIER && usure[k] >= SEUIL_SENTIER) {
                casesChemin.add(k);
                grille.disque(a, b, quantite >= 3f ? 1.8 : 0.8, Grille.RUE);
            }
            if (avant < SEUIL_CHEMIN && usure[k] >= SEUIL_CHEMIN) grille.disque(a, b, 1.2, Grille.RUE);
            if (avant < SEUIL_RUE && usure[k] >= SEUIL_RUE) grille.disque(a, b, 1.6, Grille.RUE);
            if (avant < SEUIL_GRANDE && usure[k] >= SEUIL_GRANDE) grille.disque(a, b, 2.0, Grille.RUE);
        }
        traces.add(chemin);
    }

    private double largeur(float u) {
        if (u >= SEUIL_GRANDE) return LARGEUR_GRANDE_RUE;
        if (u >= SEUIL_RUE) return 3.0;
        if (u >= SEUIL_CHEMIN) return LARGEUR_RUELLE;
        return 1.4;
    }

    private Lot candidat(Batiments.Plan plan, int lieu, List<Lot> poses, Random r) {
        double ca, cb, fa, fb, ancreUsure = 0.0;
        double tirage = r.nextDouble();
        if (lieu == Metiers.PLACE || (tirage < 0.3 && poses.size() < 12) || (casesChemin.isEmpty() && tirage < 0.6)) {
            double t = r.nextDouble() * 2.0 * Math.PI;
            double d = rayonPlace + 0.8 + plan.profondeur * 0.5;
            ca = Math.cos(t) * d;
            cb = Math.sin(t) * d;
            fa = -Math.cos(t);
            fb = -Math.sin(t);
        } else if (!casesChemin.isEmpty()) {
            int k = casesChemin.get(r.nextInt(casesChemin.size()));
            double pa = grille.centre(k % grille.cote), pb = grille.centre(k / grille.cote);
            ancreUsure = usure[k];
            double sens = directionChemin(k);
            double t = Double.isNaN(sens) ? r.nextDouble() * 2.0 * Math.PI : sens + (r.nextBoolean() ? 0.5 : -0.5) * Math.PI;
            double d = 2.2 + r.nextDouble() * 0.8 + plan.profondeur * 0.5;
            if (lieu != Metiers.GRANDE_RUE && r.nextDouble() < 0.35) {
                d = 9.0 + r.nextDouble() * 14.0 + plan.profondeur * 0.5;
                ancreUsure = 0.0;
            }
            ca = pa + Math.cos(t) * d;
            cb = pb + Math.sin(t) * d;
            fa = -Math.cos(t);
            fb = -Math.sin(t);
        } else {
            Lot voisin = poses.get(poses.size() > 1 ? 1 + r.nextInt(poses.size() - 1) : 0);
            double t = r.nextDouble() * 2.0 * Math.PI, d = 7.0 + r.nextDouble() * 9.0;
            ca = voisin.a + Math.cos(t) * d;
            cb = voisin.b + Math.sin(t) * d;
            double l = Math.max(1e-6, Math.hypot(ca, cb));
            fa = -ca / l;
            fb = -cb / l;
        }
        Lot lot = new Lot();
        lot.a = ca;
        lot.b = cb;
        lot.normA = -fa;
        lot.normB = -fb;
        lot.ancreA = ca + fa * plan.profondeur * 0.5;
        lot.ancreB = cb + fb * plan.profondeur * 0.5;
        lot.place = lieu == Metiers.PLACE || Math.hypot(lot.ancreA, lot.ancreB) < rayonPlace + 1.5;
        lot.priorite = ancreUsure;
        lot.plan = plan;
        lot.metier = plan.metier;
        return lot;
    }

    private double directionChemin(int k) {
        int cote = grille.cote, i = k % cote, j = k / cote;
        double sx = 0.0, sy = 0.0;
        for (int dj = -2; dj <= 2; dj++) {
            for (int di = -2; di <= 2; di++) {
                int ni = i + di, nj = j + dj;
                if ((di == 0 && dj == 0) || ni < 0 || nj < 0 || ni >= cote || nj >= cote) continue;
                if (usure[nj * cote + ni] < SEUIL_SENTIER) continue;
                double angle = Math.atan2(dj, di);
                sx += Math.cos(2.0 * angle);
                sy += Math.sin(2.0 * angle);
            }
        }
        return sx == 0.0 && sy == 0.0 ? Double.NaN : 0.5 * Math.atan2(sy, sx);
    }

    private double noter(Lot lot, int lieu, Random r) {
        double d = Math.hypot(lot.a, lot.b);
        if (d > etendueVille - 5.0) return Double.NaN;
        if (eauProche(lot.a, lot.b, 4.0) < 4.0) return Double.NaN;
        Batiments.Plan pl = lot.plan;
        double c = Math.cos(pl.angle), s = Math.sin(pl.angle);
        double[] mesure = grille.mesurer(lot.a, lot.b, c, s, pl.largeur * 0.5, pl.profondeur * 0.5);
        double base = (mesure[1] - mesure[0]) * 2.0 + r.nextDouble() * 3.0;
        switch (lieu) {
            case Metiers.PLACE:
                return base;
            case Metiers.GRANDE_RUE:
                return base - Math.min(lot.priorite, 40.0) * 0.3 + d * 0.05;
            case Metiers.BORD:
                return base - d * 0.2;
            case Metiers.RIVE: {
                double e = eauProche(lot.a, lot.b, 25.0);
                return e > 25.0 ? Double.NaN : base + e;
            }
            case Metiers.HAUT:
                return base - grille.hauteur(lot.a, lot.b) + d * 0.03;
            default:
                return base + d * 0.08 - Math.min(lot.priorite, 10.0) * 0.1;
        }
    }

    private boolean essayer(Lot lot) {
        Batiments.Plan pl = lot.plan;
        pl.orienter(Math.atan2(-lot.normA, lot.normB));
        double c = Math.cos(pl.angle), s = Math.sin(pl.angle);
        double hx = pl.largeur * 0.5 + pl.annexe(), hz = pl.profondeur * 0.5;
        if (!grille.libre(lot.a, lot.b, c, s, hx + 0.1, hz + 0.1, -1, false)) return false;
        return grille.libre(lot.a, lot.b, c, s, hx + 0.9, hz + 0.5, -1, true);
    }

    private boolean placerMaison(int v, long numero, List<Lot> poses, Random r) {
        Batiments.Plan plan = Batiments.plan(melanger(v * 1000003L + numero));
        return placer(plan, Metiers.RUE, 24, poses, r);
    }

    private boolean placerAtelier(int m, int rang, int v, List<Lot> poses, Random r) {
        boolean roue = m == Metiers.MOULIN && grille.riviere;
        long g = melanger(v * 7777777L + rang * 31L + m);
        int lieu = m == Metiers.MOULIN ? (roue ? Metiers.RIVE : Metiers.BORD) : Metiers.LIEU[m];
        if (placer(Batiments.planMetier(g, m, roue), lieu, 40, poses, r)) return true;
        return roue && placer(Batiments.planMetier(g, m, false), Metiers.BORD, 40, poses, r);
    }

    private boolean placer(Batiments.Plan plan, int lieu, int essais, List<Lot> poses, Random r) {
        Lot meilleur = null;
        double note = Double.MAX_VALUE;
        for (int e = 0; e < essais; e++) {
            Lot lot = candidat(plan, lieu, poses, r);
            if (!essayer(lot)) continue;
            double s = noter(lot, lieu, r);
            if (Double.isNaN(s) || s >= note) continue;
            meilleur = lot;
            note = s;
        }
        if (meilleur == null) return false;
        meilleur.plan.orienter(Math.atan2(-meilleur.normA, meilleur.normB));
        meilleur.id = poses.size();
        if (!libre(meilleur)) return false;
        poses.add(meilleur);
        int[] c = chercher(grille.indice(meilleur.ancreA - meilleur.normA * 1.2, meilleur.ancreB - meilleur.normB * 1.2), RESEAU);
        if (c != null) deposer(c, meilleur.metier == Metiers.MAISON ? 2f : 4f);
        puits(meilleur, poses, r);
        return true;
    }

    private void puits(Lot lot, List<Lot> poses, Random r) {
        if (poses.size() < 5) return;
        double pa = lot.ancreA - lot.normA * 1.2, pb = lot.ancreB - lot.normB * 1.2;
        if (Math.hypot(pa, pb) < rayonPlace + 25.0) return;
        if (eauProche(pa, pb, 45.0) < 45.0) return;
        for (double[] p : lesPuits) {
            if (Math.hypot(p[0] - pa, p[1] - pb) < 45.0) return;
        }
        for (int e = 0; e < 12; e++) {
            double t = r.nextDouble() * 2.0 * Math.PI, d = 2.5 + r.nextDouble() * 3.0;
            double a = pa + Math.cos(t) * d, b = pb + Math.sin(t) * d;
            if (!grille.disqueLibre(a, b, 1.3)) continue;
            Lot o = new Lot();
            o.a = a;
            o.b = b;
            o.plan = Batiments.objet(Batiments.PUITS, 1.6, 1.6, 0.0, 0.0, r.nextInt(4));
            objetsCroissance.add(o);
            grille.disque(a, b, 1.0, Grille.OBJET);
            lesPuits.add(new double[] { a, b });
            return;
        }
    }

    private void trafic(List<Lot> poses, Random r) {
        int maisons = poses.size() - 1;
        if (maisons <= 0) return;
        int voyages = Math.min(30, maisons);
        for (int k = 0; k < voyages; k++) {
            Lot lot = poses.get(1 + r.nextInt(maisons));
            if (lot.etat != VIVANT) continue;
            int depart = grille.indice(lot.ancreA - lot.normA * 1.2, lot.ancreB - lot.normB * 1.2);
            int[] c = chercher(depart, VERS_PLACE);
            if (c != null) deposer(c, lot.metier == Metiers.MAISON ? 1f : 2f);
            if (!lesPuits.isEmpty() && r.nextFloat() < 0.4f) {
                double[] p = lesPuits.get(r.nextInt(lesPuits.size()));
                cibleA = p[0];
                cibleB = p[1];
                cibleRayon = 2.0;
                c = chercher(depart, VERS_POINT);
                if (c != null) deposer(c, 1f);
            }
        }
        for (double[] s : lesSorties) {
            double t = Math.atan2(s[1], s[0]);
            int depart = grille.indice(Math.cos(t) * (rayonPlace - 0.5), Math.sin(t) * (rayonPlace - 0.5));
            cibleA = s[0];
            cibleB = s[1];
            cibleRayon = 1.5;
            int[] c = chercher(depart, VERS_POINT);
            if (c != null) deposer(c, (float) (1.0 + maisons / 12.0 * s[2]));
        }
    }

    private void chercherRive() {
        int cote = grille.cote, meilleure = -1;
        double proche = Double.MAX_VALUE;
        for (int k = 0; k < cote * cote; k++) {
            if (grille.occupation[k] != Grille.EAU) continue;
            double d = Math.hypot(grille.centre(k % cote), grille.centre(k / cote));
            if (d > rayonPlace + 3.0 && d < proche) {
                proche = d;
                meilleure = k;
            }
        }
        if (meilleure < 0 || proche > etendueVille) return;
        double t = Math.atan2(grille.centre(meilleure / cote), grille.centre(meilleure % cote));
        int depart = grille.indice(Math.cos(t) * (rayonPlace - 0.5), Math.sin(t) * (rayonPlace - 0.5));
        int[] c = chercher(depart, VERS_EAU);
        if (c == null || c.length < 3) return;
        deposer(c, SEUIL_RUE + 2f);
        int fin = c[c.length - 1];
        double a = grille.centre(fin % cote), b = grille.centre(fin / cote);
        double cap = Math.atan2(b - grille.centre(c[c.length - 3] / cote), a - grille.centre(c[c.length - 3] % cote));
        rive = new double[] { a, b, cap, hauteurPoint(a, b) };
    }

    private void port(List<Rue> reseau, List<Lot> objets, List<double[]> pontons, int n, long v, Random r) {
        if (rive == null) return;
        Rue amorce = new Rue(3.0, TERRE);
        amorce.rive = rive;
        List<Rue> liste = new ArrayList<>();
        liste.add(amorce);
        rivages(liste, objets, pontons, n, v, r);
        for (Rue rue : liste) {
            if (rue != amorce) reseau.add(rue);
        }
    }

    private void tracerRues(List<Rue> reseau, int n) {
        int cote = grille.cote;
        boolean[] couvert = new boolean[cote * cote];
        List<int[]> ordre = new ArrayList<>(traces);
        double[] poids = new double[ordre.size()];
        for (int q = 0; q < ordre.size(); q++) {
            double somme = 0.0;
            for (int k : ordre.get(q)) somme += usure[k];
            poids[q] = somme / Math.max(1, ordre.get(q).length);
        }
        Integer[] indices = new Integer[ordre.size()];
        for (int q = 0; q < indices.length; q++) indices[q] = q;
        Arrays.sort(indices, (x, y) -> Double.compare(poids[y], poids[x]));
        for (int q : indices) {
            int[] c = ordre.get(q);
            int debut = -1;
            for (int k = 0; k <= c.length; k++) {
                boolean dedans = k < c.length && usure[c[k]] >= SEUIL_SENTIER && !couvert[c[k]];
                if (dedans && debut < 0) debut = k;
                if (!dedans && debut >= 0) {
                    int de = Math.max(0, debut - 1), a = Math.min(c.length - 1, k);
                    if (a - de >= 2) emettre(reseau, c, de, a, n);
                    for (int m = debut; m < k; m++) couvert[c[m]] = true;
                    debut = -1;
                }
            }
        }
    }

    private void emettre(List<Rue> reseau, int[] c, int de, int a, int n) {
        int cote = grille.cote;
        int nombre = a - de + 1;
        double[] xs = new double[nombre], ys = new double[nombre];
        float maxi = 0f;
        for (int k = 0; k < nombre; k++) {
            int q = c[de + k];
            xs[k] = grille.centre(q % cote);
            ys[k] = grille.centre(q / cote);
            maxi = Math.max(maxi, usure[q]);
        }
        boolean[] garde = new boolean[nombre];
        garde[0] = true;
        garde[nombre - 1] = true;
        simplifier(xs, ys, 0, nombre - 1, 0.8, garde);
        List<double[]> pts = new ArrayList<>();
        for (int k = 0; k < nombre; k++) {
            if (garde[k]) pts.add(new double[] { xs[k], ys[k] });
        }
        if (pts.size() >= 3) {
            List<double[]> lisse = new ArrayList<>();
            lisse.add(pts.get(0));
            for (int k = 0; k + 1 < pts.size(); k++) {
                double[] p0 = pts.get(k), p1 = pts.get(k + 1);
                if (k > 0) lisse.add(new double[] { p0[0] * 0.75 + p1[0] * 0.25, p0[1] * 0.75 + p1[1] * 0.25 });
                if (k + 2 < pts.size()) lisse.add(new double[] { p0[0] * 0.25 + p1[0] * 0.75, p0[1] * 0.25 + p1[1] * 0.75 });
            }
            lisse.add(pts.get(pts.size() - 1));
            pts = lisse;
        }
        double l = largeur(maxi);
        int type = maxi >= SEUIL_GRANDE && n > 60 ? PAVE : TERRE;
        Rue rue = new Rue(l, type);
        for (double[] p : pts) rue.ajouter(p[0], p[1]);
        reseau.add(rue);
    }

    private static void simplifier(double[] xs, double[] ys, int i0, int i1, double tolerance, boolean[] garde) {
        if (i1 <= i0 + 1) return;
        double pire = -1.0;
        int indice = -1;
        for (int k = i0 + 1; k < i1; k++) {
            double d = distance(xs[k], ys[k], xs[i0], ys[i0], xs[i1], ys[i1]);
            if (d > pire) {
                pire = d;
                indice = k;
            }
        }
        if (pire <= tolerance) return;
        garde[indice] = true;
        simplifier(xs, ys, i0, indice, tolerance, garde);
        simplifier(xs, ys, indice, i1, tolerance, garde);
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

    /** Clairières hors des villages (campements…) : x, y, z, rayon en mètres, par quatre. Les arbres n'y poussent pas. */
    public volatile double[] clairieres = new double[0];

    public boolean enClairiere(double px, double py, double pz) {
        double[] c = clairieres;
        for (int k = 0; k + 3 < c.length; k += 4) {
            double dx = (px - c[k]) * RAYON_METRES, dy = (py - c[k + 1]) * RAYON_METRES, dz = (pz - c[k + 2]) * RAYON_METRES;
            if (dx * dx + dy * dy + dz * dz < c[k + 3] * c[k + 3]) return true;
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
