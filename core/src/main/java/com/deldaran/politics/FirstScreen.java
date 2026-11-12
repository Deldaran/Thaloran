package com.deldaran.politics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.deldaran.politics.planet.GenerateurMonde;
import com.deldaran.politics.planet.Monde;
import com.deldaran.politics.planet.Plaque;
import com.deldaran.politics.planet.Simulation;
import com.deldaran.politics.planet.Terrain;
import com.badlogic.gdx.files.FileHandle;
import com.deldaran.politics.planet.Sauvegarde;
import com.deldaran.politics.histoire.Regions;
import com.deldaran.politics.planet.Biome;
import java.util.function.Consumer;
import java.util.function.Supplier;
import com.deldaran.politics.histoire.Histoire;
import com.deldaran.politics.histoire.Joueur;
import com.deldaran.politics.histoire.Population;
import com.deldaran.politics.histoire.Peuple;
import com.deldaran.politics.histoire.Noms;
import com.deldaran.politics.histoire.Villes;
import com.deldaran.politics.histoire.Economie;
import com.deldaran.politics.histoire.Etats;
import com.deldaran.politics.histoire.Livres;
import com.deldaran.politics.histoire.Recit;
import java.util.function.Function;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FirstScreen implements Screen {

    GameCamera camera;
    CameraController cameraController;
    CarteCube carte;
    CarteCube fond;
    boolean fondPret;
    RenduGlobe rendu;
    int[] couleurs;
    float tempsCarte = 0f;
    int modeCarte = -1;

    Monde monde;
    Simulation simulation;
    FileHandle fichierMonde;
    boolean mondeEnregistre;
    FileHandle fichierHistoire;
    boolean reprendreHistoire = true;
    Regions regions;
    final Regions.Reglages reglagesRegions = new Regions.Reglages();
    final Population.Reglages reglagesPopulation = new Population.Reglages();

    float pasParSeconde = 10f;

    FenetreDebug fenetreDebug;
    int modeAffichage = 4;
    float dernierClimat = -1000f;

    RenduTuiles tuiles;
    Vegetation vegetation;
    Terrain terrain;
    Assembleur assembleur;
    Batiments batiments;
    final Villages villages = new Villages();
    Meteo meteo;
    Ciel ciel;
    Traces traces;
    Rues rues;
    Chargement chargement;
    final Obstacles obstacles = new Obstacles() {
        public boolean bloque(double x, double y, double z, double r) {
            return (villages != null && villages.bloque(x, y, z, r)) || (vegetation != null && vegetation.bloque(x, y, z, r));
        }

        public boolean surPont(double x, double y, double z) {
            return villages != null && villages.surPont(x, y, z);
        }
    };
    boolean arrive;
    final List<Personnage> personnages = new ArrayList<>();
    private final Terrain.Point point = new Terrain.Point();
    private final Random hasard = new Random();
    private float[] errance = new float[0];
    Personnage joueur;
    private static final double RAYON_JOUEUR = 2.0e7;
    private static final double ZOOM_JEU = 6.0e7;
    private static final double MARCHE = 1.4;
    private static final double COURSE = 4.0;

    private static final long GRAINE = 42L;


    Histoire histoire;
    boolean enPause = false;
    float tempsAccumule = 0f;
    private static final float SEUIL_RIVIERE = 10f;
    private static final String[] NOMS_BIOMES = { "Toundra", "Taiga", "Prairie", "Foret", "Desert", "Savane", "Jungle", "Montagne" };
    private static final byte[] BIOMES_FERTILES = { Biome.TOUNDRA, Biome.TAIGA, Biome.PRAIRIE, Biome.FORET, Biome.DESERT, Biome.SAVANE, Biome.JUNGLE, Biome.MONTAGNE };

    private static final float PERIODE_CARTE = 0.1f;
    private static final double RAYON_TUILES = 300.0;
    private static final int MODE_BIOMES = 4;
    private static final float OPACITE_CALQUE = 0.55f;
    private static final Color[] COULEURS_PEUPLES = {
        new Color(0.39f, 0.83f, 0.44f, 1f), new Color(0.85f, 0.40f, 0.23f, 1f), new Color(0.95f, 0.81f, 0.24f, 1f), new Color(0.64f, 0.23f, 0.82f, 1f)
    };


    private static final String[] NOMS_MODES = { "plaques", "relief", "temperature", "pluie", "biomes", "regions", "nourriture", "population", "villes", "commerce", "etats" };

    private final Color couleurTemp = new Color();

    @Override
    public void show() {
        camera = new GameCamera();
        cameraController = new CameraController(camera);


        monde = new GenerateurMonde().genererMonde(512, 256, GRAINE);

        simulation = new Simulation(monde);
        fichierMonde = Gdx.files.external("Politics/mondes/monde-" + GRAINE + ".bin");
        mondeEnregistre = Sauvegarde.charger(monde, simulation, fichierMonde);
        fichierHistoire = Gdx.files.external("Politics/mondes/histoire-" + GRAINE + ".bin");

        carte = new CarteCube(monde.sphere);
        fond = new CarteCube(monde.sphere);
        rendu = new RenduGlobe();
        couleurs = new int[monde.sphere.nombre];

        fenetreDebug = new FenetreDebug();
        remplirDebug();
        chargement = new Chargement();

        Gdx.input.setInputProcessor(new InputMultiplexer(fenetreDebug.getStage(), cameraController));
    }

    @Override
    public void render(float delta) {
        cameraController.update(delta);
        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            modeAffichage = (modeAffichage + 1) % NOMS_MODES.length;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.P) && terrain != null) {
            peupler();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.O) && meteo != null) {
            meteo.forcage = (meteo.forcage + 1) % Meteo.FORCAGES.length;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.T) && terrain != null) {
            teleporterSouris();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.H) && meteo != null) {
            meteo.decalageHeures += 3.0;
        }
        if (meteo != null) meteo.avancer(delta, accelerationMeteo());
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            enPause = !enPause;
        }

        if (!enPause) {
            tempsAccumule += delta;
            float periode = 1f / pasParSeconde;
            long limite = System.nanoTime() + 25000000L;
            while (tempsAccumule >= periode && System.nanoTime() < limite) {
                simulation.pas();
                tempsAccumule -= periode;
            }
            tempsAccumule = Math.min(tempsAccumule, 0.1f);
        }

        if (!mondeEnregistre && simulation.phase == Simulation.Phase.TERMINEE) {
            Sauvegarde.enregistrer(monde, simulation, fichierMonde);
            mondeEnregistre = true;
        }
        if (regions == null && simulation.phase == Simulation.Phase.TERMINEE) {
            regions = new Regions(monde, simulation.climat.hauteurPlaines, reglagesRegions);
        }
        if (histoire == null && regions != null) {
            histoire = new Histoire(monde, regions, reglagesPopulation, fichierHistoire, reprendreHistoire);
        }
        if (histoire != null) {
            histoire.enPause = enPause;
        }

        if (simulation.phase == Simulation.Phase.TECTONIQUE && modeAffichage >= 2 && monde.temps - dernierClimat >= 100f) {
            simulation.climat.calculer();
            dernierClimat = monde.temps;
        }


        tempsCarte += delta;
        if (modeCarte != modeAffichage || (!enPause && tempsCarte >= PERIODE_CARTE)) {
            rafraichirCarte();
        }



        ScreenUtils.clear(Color.BLACK);
        if (simulation.phase != Simulation.Phase.TERMINEE) {
            rendu.dessiner(carte, camera, 1f);
        } else {
            if (!fondPret) preparerFond();
            if (tuiles == null) initialiserMonde();
            if (!arrive && camera.rayonPixels >= RAYON_JOUEUR) {
                arrive = true;
                chargement.commencer();
            }
            if (camera.rayonPixels >= RAYON_TUILES) {
                dessinerTuiles();
            } else {
                rendu.dessiner(fond, camera, 1f);
            }
            ciel.ecrasement = camera.rayonPixels >= RAYON_TUILES ? tuiles.ecrasement() : 1f;
            ciel.rotation = camera.rayonPixels >= RAYON_TUILES ? tuiles.rotation() : 0.0;
            Personnage.rotationVue = ciel.rotation;
            camera.rotationVue = ciel.rotation;
            camera.ecrasementVue = ciel.ecrasement;
            ciel.dessiner(camera, meteo, delta);
            if (modeAffichage != MODE_BIOMES) {
                rendu.dessiner(carte, camera, OPACITE_CALQUE);
            }
        }

        fenetreDebug.dessiner(delta);
        dessinerChargement(delta);
    }

    private void dessinerChargement(float delta) {
        int sol = 0, forets = 0, gens = 0, ciel = 0;
        if (tuiles != null && camera.rayonPixels >= RAYON_TUILES) {
            sol = tuiles.enAttente();
            forets = vegetation.parcellesManquantes;
            gens = vegetation.personnagesManquants + vegetation.maisonsManquantes;
        }
        if (meteo != null && !meteo.pret()) ciel = 1;
        String etape = sol > 0 ? "La terre se forme" : forets > 0 ? "Les forets poussent" : gens > 0 ? "Les vivants s'eveillent" : ciel > 0 ? "Le ciel se couvre" : "";
        chargement.dessiner(sol + forets + gens + ciel, etape, delta);
    }

    private void couleurPlaque(int i) {
        Plaque plaque = monde.plaques.get(monde.plaque[i]);
        couleurTemp.set(plaque.couleur);
        if (!monde.isContinentale[i]) {
            couleurTemp.mul(0.7f, 0.7f, 0.7f, 1f);
        }
    }

    private void initialiserMonde() {
        terrain = new Terrain(monde, simulation.climat.hauteurPlaines);
        tuiles = new RenduTuiles(terrain);
        vegetation = new Vegetation(terrain);
        assembleur = new Assembleur();
        batiments = new Batiments();
        meteo = new Meteo(terrain);
        ciel = new Ciel(terrain);
        traces = new Traces();
        rues = new Rues();
        chargement.commencer();
    }

    private boolean accessible(double x, double y, double z) {
        terrain.echantillonner(x, y, z, 11, point);
        return Personnage.profondeur(point) == 0f;
    }

    void teleporter(double x, double y, double z) {
        double[] d = { x, y, z };
        Random r = new Random(Double.doubleToLongBits(x * 31.0 + y));
        double lon0 = Math.atan2(y, x), lat0 = Math.asin(Math.max(-1.0, Math.min(1.0, z)));
        boolean trouve = false;
        for (int k = 0; k < 6000 && !trouve; k++) {
            double ecart = k == 0 ? 0.0 : 0.00002 * (1 + k / 50);
            double lat = lat0 + (r.nextDouble() - 0.5) * ecart, lon = lon0 + (r.nextDouble() - 0.5) * ecart;
            d[0] = Math.cos(lat) * Math.cos(lon);
            d[1] = Math.cos(lat) * Math.sin(lon);
            d[2] = Math.sin(lat);
            trouve = accessible(d[0], d[1], d[2]);
        }
        if (!trouve) return;
        camera.longitude = Math.atan2(d[1], d[0]);
        camera.latitude = Math.asin(d[2]);
        camera.rayonPixels = ZOOM_JEU;
        traces.effacer();
        personnages.remove(joueur);
        joueur = null;
        placerJoueur();
        chargement.commencer();
    }

    void teleporterSouris() {
        double qx = (Gdx.input.getX() - Gdx.graphics.getWidth() / 2.0) / camera.rayonPixels;
        double qy = (Gdx.graphics.getHeight() / 2.0 - Gdx.input.getY()) / camera.rayonPixels / (tuiles != null && camera.rayonPixels >= RAYON_TUILES ? tuiles.ecrasement() : 1.0);
        double rv = tuiles != null && camera.rayonPixels >= RAYON_TUILES ? tuiles.rotation() : 0.0;
        double rx = Math.cos(rv) * qx + Math.sin(rv) * qy, ry = -Math.sin(rv) * qx + Math.cos(rv) * qy;
        qx = rx;
        qy = ry;
        double q2 = qx * qx + qy * qy;
        if (q2 >= 1.0) return;
        double cosLon = Math.cos(camera.longitude), sinLon = Math.sin(camera.longitude);
        double cosLat = Math.cos(camera.latitude), sinLat = Math.sin(camera.latitude);
        double qz = Math.sqrt(1.0 - q2);
        double x = -sinLon * qx - sinLat * cosLon * qy + cosLat * cosLon * qz;
        double y = cosLon * qx - sinLat * sinLon * qy + cosLat * sinLon * qz;
        double z = cosLat * qy + sinLat * qz;
        if (joueur != null && camera.rayonPixels >= RAYON_JOUEUR) {
            if (!accessible(x, y, z)) return;
            joueur.position[0] = x;
            joueur.position[1] = y;
            joueur.position[2] = z;
            joueur.altitude = point.altitude;
            joueur.profondeur = 0f;
            joueur.dansHerbe = Personnage.herbe(terrain, x, y, z, point);
            return;
        }
        teleporter(x, y, z);
    }

    void teleporterVille(boolean auHasard) {
        if (histoire == null || terrain == null) return;
        com.deldaran.politics.histoire.Villes v = histoire.population.villes;
        int[] grandes = v.plusGrandes(40);
        int choix = -1;
        for (int k = 0; k < grandes.length; k++) {
            int c = auHasard ? grandes[hasard.nextInt(grandes.length)] : grandes[k];
            if (c >= 0) {
                choix = c;
                break;
            }
        }
        if (choix < 0) return;
        int c = v.caseDe[choix];
        teleporter(monde.sphere.x[c], monde.sphere.y[c], monde.sphere.z[c]);
    }

    void teleporterBiome(int biome) {
        if (terrain == null) return;
        for (int k = 0; k < 40000; k++) {
            double z = hasard.nextDouble() * 2.0 - 1.0, a = hasard.nextDouble() * 2.0 * Math.PI, r = Math.sqrt(1.0 - z * z);
            double x = r * Math.cos(a), y = r * Math.sin(a);
            terrain.echantillonner(x, y, z, 9, point);
            if (point.altitude < 0.01f || point.riviere || point.lac) continue;
            if (biome >= 0 && terrain.biome(x, y, z, point) != biome) continue;
            teleporter(x, y, z);
            return;
        }
    }

    void teleporterRelief() {
        if (terrain == null) return;
        double meilleur = -1.0, bx = 0.0, by = 0.0, bz = 1.0, e = 80.0 / (Terrain.RAYON_KM * 1000.0);
        for (int k = 0; k < 3000; k++) {
            double z = hasard.nextDouble() * 2.0 - 1.0, a = hasard.nextDouble() * 2.0 * Math.PI, r = Math.sqrt(1.0 - z * z);
            double x = r * Math.cos(a), y = r * Math.sin(a);
            terrain.echantillonner(x, y, z, 11, point);
            if (point.altitude < 0.3f || point.riviere || point.lac) continue;
            float h = point.altitude;
            double n = Math.sqrt(x * x + y * y);
            terrain.echantillonner(x - y / n * e, y + x / n * e, z, 11, point);
            float he = point.altitude;
            terrain.echantillonner(x - z * x / n * e, y - z * y / n * e, z + n * e, 11, point);
            float hn = point.altitude;
            double pente = Math.hypot(he - h, hn - h);
            if (pente > meilleur) {
                meilleur = pente;
                bx = x;
                by = y;
                bz = z;
            }
        }
        if (meilleur > 0.0) teleporter(bx, by, bz);
    }

    private double accelerationMeteo() {
        double proche = Math.log(camera.rayonPixels / 1.0e5) / Math.log(RAYON_JOUEUR / 1.0e5);
        proche = Math.max(0.0, Math.min(1.0, proche));
        return Meteo.ACCELERATION + (Meteo.ACCELERATION_LOCALE - Meteo.ACCELERATION) * proche;
    }

    private void dessinerTuiles() {
        piloter(Gdx.graphics.getDeltaTime());
        errer(Gdx.graphics.getDeltaTime());
        traces.mollesse = 0.6f + ciel.locale.neigeSol * 3f + Math.max(0f, ciel.locale.mouille - 0.3f) * 2.5f;
        traces.comblement = ciel.locale.neige;
        traces.villages = villages;
        traces.mettreAJour(camera, personnages, Gdx.graphics.getDeltaTime());
        tuiles.traces = traces.texture();
        tuiles.traceX = traces.cameraX;
        tuiles.traceY = traces.cameraY;
        tuiles.mouille = ciel.locale.mouille;
        tuiles.neigeSol = ciel.locale.neigeSol;
        vegetation.vent = (float) Math.hypot(ciel.locale.ventEst, ciel.locale.ventNord) + ciel.locale.orage * 8f;
        vegetation.neige = ciel.locale.neigeSol;
        vegetation.ventEst = ciel.locale.ventEst;
        vegetation.ventNord = ciel.locale.ventNord;
        if (histoire != null) villages.mettreAJour(histoire.population, monde.sphere, terrain, camera);
        rues.mettreAJour(camera, villages, tuiles.metresPixel(), tuiles.demiVueMetres());
        tuiles.rues = rues;
        tuiles.dessiner(camera, true);
        vegetation.joueur = joueur;
        vegetation.dessiner(camera, tuiles, personnages, assembleur, villages, batiments);

    }

    void peupler() {
        double cosLat = Math.cos(camera.latitude);
        double x0 = cosLat * Math.cos(camera.longitude), y0 = cosLat * Math.sin(camera.longitude), z0 = Math.sin(camera.latitude);
        for (int k = 0; k < 30; k++) {
            Personnage p = new Personnage(assembleur.tirer(hasard.nextLong(), hasard.nextInt(Peuple.NOMBRE)), x0, y0, z0);
            terrain.echantillonner(x0, y0, z0, 11, point);
            p.altitude = point.altitude;
            p.deplacer((hasard.nextDouble() - 0.5) * 30.0, (hasard.nextDouble() - 0.5) * 20.0, 0f, terrain, point);
            p.enMarche = false;
            personnages.add(p);
        }
        errance = new float[personnages.size() * 3];
        chargement.commencer();
    }

    private void errer(float delta) {
         for (int k = 0; k < errance.length / 3; k++) {
            if (personnages.get(k) == joueur) continue;
            errance[k * 3] -= delta;
            if (errance[k * 3] <= 0f) {
                errance[k * 3] = 1f + hasard.nextFloat() * 3f;
                int choix = hasard.nextInt(6);
                errance[k * 3 + 1] = choix == 0 ? 1f : choix == 1 ? -1f : 0f;
                errance[k * 3 + 2] = choix == 2 ? 1f : choix == 3 ? -1f : 0f;
            }
            personnages.get(k).deplacer(errance[k * 3 + 1] * 1.4 * delta, errance[k * 3 + 2] * 1.4 * delta, delta, terrain, point, obstacles);
        }
    }
    private void piloter(float delta) {
        boolean proche = camera.rayonPixels >= RAYON_JOUEUR;
        if (proche && (joueur == null || Gdx.input.isKeyJustPressed(Input.Keys.J))) placerJoueur();
        cameraController.libre = !proche || joueur == null;
        if (cameraController.libre) return;
        double est = 0.0, nord = 0.0;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) nord += 1.0;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) nord -= 1.0;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) est += 1.0;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) est -= 1.0;
        double longueur = Math.sqrt(est * est + nord * nord);
        if (longueur > 0.0) {
            double vitesse = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) ? COURSE : MARCHE) * delta / longueur;
            est *= vitesse;
            nord *= vitesse;
        }
        double cr = Math.cos(Personnage.rotationVue), sr = Math.sin(Personnage.rotationVue);
        double versEst = cr * est + sr * nord, versNord = -sr * est + cr * nord;
        joueur.deplacer(versEst, versNord, delta, terrain, point, obstacles);
        suivre();
    }

    void basculerMain(int[] main) {
        if (joueur == null) return;
        Tenue t = joueur.tenue();
        int tenue = t.tenue(main);
        int debut = 0;
        if (tenue >= 0) {
            t.ranger(tenue);
            for (int k = 0; k < main.length; k++) if (main[k] == tenue) debut = k + 1;
        }
        for (int k = debut; k < main.length; k++) {
            if (t.porte(main[k])) {
                t.tenir(main[k]);
                return;
            }
        }
    }

    void placerJoueur() {
        double cosLat = Math.cos(camera.latitude);
        double x = cosLat * Math.cos(camera.longitude), y = cosLat * Math.sin(camera.longitude), z = Math.sin(camera.latitude);
        terrain.echantillonner(x, y, z, 11, point);
        if (point.altitude < 0f || point.riviere || point.lac) return;
        Joueur incarne = histoire != null ? histoire.joueur : null;
        int espece = incarne != null && incarne.peuple() >= 0 ? incarne.peuple() : Peuple.HUMAIN;
        long graine = incarne != null && incarne.identifiant() >= 0L ? incarne.identifiant() : 1L;
        personnages.remove(joueur);
        joueur = new Personnage(assembleur.tirer(graine, espece), x, y, z);
        joueur.altitude = point.altitude;
        joueur.dansHerbe = Personnage.herbe(terrain, x, y, z, point);
        personnages.add(joueur);
    }

    void suivre() {
        double cosLon = Math.cos(camera.longitude), sinLon = Math.sin(camera.longitude);
        double cosLat = Math.cos(camera.latitude), sinLat = Math.sin(camera.latitude);
        double[] p = joueur.position;
        double est = -sinLon * p[0] + cosLon * p[1];
        double nord = -sinLat * cosLon * p[0] - sinLat * sinLon * p[1] + cosLat * p[2];
        camera.deplacerPixels(est * camera.rayonPixels, nord * camera.rayonPixels);
    }

    private void preparerFond() {
        int[] teintes = new int[couleurs.length];
        for (int i = 0; i < teintes.length; i++) {
            Palette.biome(monde.biome[i], couleurTemp);
            ajouterEau(i);
            teintes[i] = Color.rgba8888(couleurTemp);
        }
        fond.mettreAJour(teintes);
        fondPret = true;
    }


    private void rafraichirCarte() {
        for (int i = 0; i < couleurs.length; i++) {
            choisirCouleur(i);
            couleurs[i] = Color.rgba8888(couleurTemp);
        }
        carte.mettreAJour(couleurs);
        tempsCarte = 0f;
        modeCarte = modeAffichage;
    }

    private void ajouterEau(int i) {
        if (monde.estSousLEau(i) || monde.estGele(i)) return;
        if (monde.lac[i]) {
            couleurTemp.set(Palette.LAC);
        } else if (monde.debit[i] > SEUIL_RIVIERE) {
            couleurTemp.set(Palette.RIVIERE);
        }
    }

    private void choisirCouleur(int i) {
        switch (modeAffichage) {
            case 0: couleurPlaque(i); break;
            case 1: Palette.relief(monde.altitude[i] - monde.niveauMer, monde.temperature[i], couleurTemp); ajouterEau(i); break;
            case 2: couleurTemperature(i); break;
            case 3: couleurPluie(i); break;
            case 5: couleurRegion(i); break;
            case 6: couleurNourriture(i); break;
            case 7: couleurPopulation(i); break;
            case 8: couleurVilles(i); break;
            case 9: couleurCommerce(i); break;
            case 10: couleurEtat(i); break;

            default: Palette.biome(monde.biome[i], couleurTemp); ajouterEau(i); break;

        }
    }

    private void couleurEtat(int i) {
        if (histoire == null) {
            couleurTemp.set(Color.DARK_GRAY);
            return;
        }
        Population p = histoire.population;
        int r = p.regions.regionDe[i];
        if (!p.regions.estTerre[r]) {
            couleurTemp.set(0.06f, 0.12f, 0.24f, 0f);
            return;
        }
        int e = p.etats.regionEtat[r];
        if (e < 0) {
            couleurTemp.set(0.29f, 0.29f, 0.26f, 0f);
            return;
        }
        couleurTemp.fromHsv((e * 47) % 360, 0.65f, 0.9f);
        couleurTemp.a = 1f;
    }


    private void couleurCommerce(int i) {
        if (histoire == null) {
            couleurTemp.set(Color.DARK_GRAY);
            return;
        }
        Population p = histoire.population;
        int r = p.regions.regionDe[i];
        float t = Math.min(1f, (float) Math.log10(1f + p.economie.trafic[r]) / 6f);
        if (p.regions.estTerre[r]) {
            couleurTemp.set(0.16f + 0.84f * t, 0.14f + 0.66f * t, 0.1f, 1f);
        } else {
            couleurTemp.set(0.04f, 0.08f + 0.5f * t, 0.18f + 0.8f * t, 1f);
        }
    }

    private void couleurVilles(int i) {
        couleurPopulation(i);
        couleurTemp.mul(0.4f, 0.4f, 0.4f, 1f);
        if (histoire == null) return;
        Population p = histoire.population;
        Villes villes = p.villes;
        int v = p.villeDe[i];
        if (v < 0 || v >= villes.nombre) return;
        if (!villes.estVivante(v)) {
            couleurTemp.set(0.55f, 0.5f, 0.45f, 1f);
            return;
        }
        float force = 0.3f + 0.7f * Math.min(1f, villes.habitants[v] / 3000f);
        couleurTemp.set(COULEURS_PEUPLES[villes.peuple[v]]).lerp(Color.WHITE, force);
    }

    private void couleurPopulation(int i) {
        if (histoire == null) {
            couleurTemp.set(Color.DARK_GRAY);
            return;
        }
        Population p = histoire.population;
        int r = p.regions.regionDe[i];
        if (!p.regions.estTerre[r]) {
            couleurTemp.set(0.06f, 0.12f, 0.24f, 0f);
            return;
        }
        int dominant = -1, nombre = 0;
        for (int q = 0; q < Peuple.NOMBRE; q++) {
            int vivants = p.foules[q][r].vivants;
            if (vivants > nombre) {
                nombre = vivants;
                dominant = q;
            }
        }
        if (dominant < 0) {
            couleurTemp.set(0.29f, 0.29f, 0.26f, 0f);
            return;
        }
        float d = 0.45f + 0.55f * Math.min(1f, p.habitants[r] / Math.max(1f, p.capacite[dominant][r]));
        couleurTemp.set(COULEURS_PEUPLES[dominant]).mul(d, d, d, 1f);
    }


    private void couleurTemperature(int i) {
        float t = MathUtils.clamp((monde.temperature[i] + 30f) / 60f, 0f, 1f);
        couleurTemp.set(t, 0.3f + 0.3f * (1f - Math.abs(t - 0.5f) * 2f), 1f - t, 1f);
    }

    private void couleurPluie(int i) {
        float t = MathUtils.clamp(monde.pluie[i] / 2000f, 0f, 1f);
        float base = monde.estSousLEau(i) ? 0.25f : 1f;
        couleurTemp.set(base * (1f - t), base * (1f - t * 0.6f), base * (1f - t * 0.1f), 1f);
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) return;
        fenetreDebug.redimensionner(width, height);
    }
    private void couleurRegion(int i) {
        if (regions == null) {
            couleurTemp.set(Color.DARK_GRAY);
            return;
        }
        int r = regions.regionDe[i];
        int h = r * 0x9E3779B1;
        couleurTemp.set(0.35f + ((h >>> 8) & 127) / 254f, 0.35f + ((h >>> 16) & 127) / 254f, 0.35f + ((h >>> 24) & 127) / 254f, 1f);
        if (!regions.estTerre[r]) {
            couleurTemp.mul(0.35f, 0.4f, 0.7f, 1f);
        }
    }

    private void couleurNourriture(int i) {
        if (regions == null) {
            couleurTemp.set(Color.DARK_GRAY);
            return;
        }
        int r = regions.regionDe[i];
        if (!regions.estTerre[r]) {
            couleurTemp.set(0.06f, 0.12f, 0.24f, 0f);
            return;
        }
        float f = regions.nourriture[r] / regions.cases[r];
        couleurTemp.set(0.1f + 0.6f * (1f - f), 0.25f + 0.7f * f, 0.2f, 1f);
    }

    private void remplirDebug() {
        FenetreDebug.Section affichage = fenetreDebug.ajouterSection("Affichage").ouvrir();
        affichage.ajouterInfo("FPS", () -> String.valueOf(Gdx.graphics.getFramesPerSecond()));
        affichage.ajouterInfo("Mode (TAB)", () -> NOMS_MODES[modeAffichage]);

        FenetreDebug.Section simulationDebug = fenetreDebug.ajouterSection("Simulation").ouvrir();
        simulationDebug.ajouterInfo("Etat (ESPACE)", () -> enPause ? "en pause" : "en cours");
        simulationDebug.ajouterInfo("Phase", () -> simulation.phase.name());
        simulationDebug.ajouterInfo("Temps", () -> simulation.tempsTexte());
        simulationDebug.ajouterSlider("Pas par seconde", 10f, 500f, 10f, 10f, valeur -> pasParSeconde = valeur);

        FenetreDebug.Section planete = fenetreDebug.ajouterSection("Planete");
        planete.ajouterInfo("Rayon", () -> String.format("%.0f km", Terrain.RAYON_KM));
        planete.ajouterInfo("Taille d'une case", () -> String.format("%.0f km2", surfaceCase()));
        planete.ajouterInfo("Niveau de la mer", () -> String.format("%.2f km", monde.niveauMer));

        FenetreDebug.Section plaques = planete.ajouterSection("Tectonique");
        plaques.ajouterInfo("Nombre de plaques", () -> String.valueOf(monde.plaques.size));
        plaques.ajouterInfo("Epaisseur maximale", () -> String.format("%.2f km", monde.epaisseurMax()));
        plaques.ajouterInfo("Fractures", () -> String.valueOf(simulation.tectonique.nombreFractures));
        plaques.ajouterInfo("Ruptures", () -> String.valueOf(simulation.tectonique.nombreRuptures));
        plaques.ajouterInfo("Soudures", () -> String.valueOf(simulation.tectonique.nombreSoudures));

        FenetreDebug.Section sectionRegions = fenetreDebug.ajouterSection("Regions");
        infoRegion(sectionRegions, "Regions de terre", () -> String.valueOf(regions.regionsTerre));
        infoRegion(sectionRegions, "Regions de mer", () -> String.valueOf(regions.regionsMer));
        infoRegion(sectionRegions, "Ports", () -> String.valueOf(regions.ports));
        infoRegion(sectionRegions, "Terres", () -> String.format("%.1f M km2", regions.casesTerre * surfaceCase() / 1e6));
        infoRegion(sectionRegions, "Terres fertiles", () -> String.format("%.1f M km2", regions.casesFertiles * surfaceCase() / 1e6));

        Regions.Reglages r = reglagesRegions;
        FenetreDebug.Section decoupe = sectionRegions.ajouterSection("Decoupage");
        reglerRegion(decoupe, "Ecart terre", 1f, 8f, 1f, r.ecartTerre, v -> r.ecartTerre = v.intValue());
        reglerRegion(decoupe, "Ecart mer", 2f, 16f, 1f, r.ecartMer, v -> r.ecartMer = v.intValue());

        FenetreDebug.Section ressources = sectionRegions.ajouterSection("Ressources");
        reglerRegion(ressources, "Debit d'un fleuve", 5f, 100f, 5f, r.debitFleuve, v -> r.debitFleuve = v);
        reglerRegion(ressources, "Bonus fleuve", 0f, 1f, 0.05f, r.bonusFleuve, v -> r.bonusFleuve = v);
        reglerRegion(ressources, "Peche cotiere", 0f, 1f, 0.05f, r.pecheCotiere, v -> r.pecheCotiere = v);
        reglerRegion(ressources, "Debut minerai", 0f, 2f, 0.1f, r.debutMinerai, v -> r.debutMinerai = v);
        reglerRegion(ressources, "Etendue minerai", 0.1f, 2f, 0.1f, r.etendueMinerai, v -> r.etendueMinerai = v);

        FenetreDebug.Section fertilite = sectionRegions.ajouterSection("Fertilite des biomes");
        for (int i = 0; i < BIOMES_FERTILES.length; i++) {
            byte biome = BIOMES_FERTILES[i];
            reglerRegion(fertilite, NOMS_BIOMES[i], 0f, 1f, 0.05f, r.fertilite[biome], v -> r.fertilite[biome] = v);
        }
        FenetreDebug.Section sectionMeteo = fenetreDebug.ajouterSection("Meteo");
        sectionMeteo.ajouterInfo("Mode (O)", () -> meteo == null ? "-" : Meteo.FORCAGES[meteo.forcage]);
        sectionMeteo.ajouterInfo("Temps", () -> meteo == null || !meteo.pret() ? "calcul..." : String.format("jour %d, %02dh%02d ici (H : +3h)", (int) (meteo.heures() / 24), (int) meteo.heureLocale(camera.longitude), (int) (meteo.heureLocale(camera.longitude) % 1.0 * 60.0)));
        sectionMeteo.ajouterInfo("Ici", () -> ciel == null ? "-" : String.format("%.0f C, nuages %.0f%%, pluie %.0f%%, neige %.0f%%, brouillard %.0f%%",
            ciel.locale.temperature, ciel.locale.nuage * 100f, ciel.locale.pluie * 100f, ciel.locale.neige * 100f, ciel.locale.brouillard * 100f));
        sectionMeteo.ajouterInfo("Sol", () -> ciel == null ? "-" : String.format("mouille %.0f%%, neige %.0f%%", ciel.locale.mouille * 100f, ciel.locale.neigeSol * 100f));
        sectionMeteo.ajouterInfo("Vent", () -> ciel == null ? "-" : String.format("%.1f m/s", Math.hypot(ciel.locale.ventEst, ciel.locale.ventNord)));

        FenetreDebug.Section sectionVoyage = fenetreDebug.ajouterSection("Teleportation");
        sectionVoyage.ajouterInfo("T", () -> "joueur sous la souris");
        sectionVoyage.ajouterBouton("Plus grande ville", () -> teleporterVille(false));
        sectionVoyage.ajouterBouton("Ville au hasard", () -> teleporterVille(true));
        sectionVoyage.ajouterBouton("Lieu au hasard", () -> teleporterBiome(-1));
        sectionVoyage.ajouterBouton("Foret", () -> teleporterBiome(Biome.FORET));
        sectionVoyage.ajouterBouton("Jungle", () -> teleporterBiome(Biome.JUNGLE));
        sectionVoyage.ajouterBouton("Taiga", () -> teleporterBiome(Biome.TAIGA));
        sectionVoyage.ajouterBouton("Prairie", () -> teleporterBiome(Biome.PRAIRIE));
        sectionVoyage.ajouterBouton("Savane", () -> teleporterBiome(Biome.SAVANE));
        sectionVoyage.ajouterBouton("Desert", () -> teleporterBiome(Biome.DESERT));
        sectionVoyage.ajouterBouton("Toundra", () -> teleporterBiome(Biome.TOUNDRA));
        sectionVoyage.ajouterBouton("Montagne", () -> teleporterBiome(Biome.MONTAGNE));
        sectionVoyage.ajouterBouton("Relief escarpe", () -> teleporterRelief());

        FenetreDebug.Section sectionGens = fenetreDebug.ajouterSection("Personnages");
        sectionGens.ajouterInfo("Nombre", () -> String.valueOf(personnages.size()));
        sectionGens.ajouterInfo("Joueur", () -> joueur == null ? "-" : joueur.profondeur > 0f ? String.format("dans l'eau (%.1f m)", joueur.profondeur) : joueur.dansHerbe ? "dans l'herbe" : "au sol");
        sectionGens.ajouterBouton("Ajouter 30 habitants", () -> { if (terrain != null) peupler(); });
        sectionGens.ajouterBouton("Effacer les habitants", () -> {
            personnages.clear();
            if (joueur != null) personnages.add(joueur);
            errance = new float[personnages.size() * 3];
        });
        sectionGens.ajouterBouton("Replacer le joueur (J)", () -> { if (terrain != null) placerJoueur(); });
        sectionGens.ajouterBouton("Habiller / deshabiller le joueur", () -> { if (joueur != null) joueur.nu = !joueur.nu; });
        sectionGens.ajouterInfo("Coiffure", () -> joueur == null ? "-" : Traits.NOMS_COUPES[joueur.traits().coupe] + String.format(" %.0f cm", joueur.traits().cheveux));
        sectionGens.ajouterInfo("Barbe", () -> joueur == null ? "-" : Traits.NOMS_BARBES[joueur.traits().barbe] + String.format(" %.1f cm", joueur.traits().longueurBarbe));
        sectionGens.ajouterInfo("Expression", () -> joueur == null ? "-" : Traits.NOMS_EXPRESSIONS[joueur.traits().expression]);
        sectionGens.ajouterBouton("Changer de coiffure", () -> { if (joueur != null) joueur.traits().coupe = (joueur.traits().coupe + 1) % Traits.COUPES; });
        sectionGens.ajouterBouton("Changer de barbe", () -> { if (joueur != null) joueur.traits().barbe = (joueur.traits().barbe + 1) % Traits.BARBES; });
        sectionGens.ajouterBouton("Laisser pousser 3 mois", () -> { if (joueur != null) joueur.traits().pousser(90.0, false); });
        sectionGens.ajouterBouton("Couper les cheveux / raser", () -> { if (joueur != null) { joueur.traits().couper(4.0); joueur.traits().raser(); } });
        sectionGens.ajouterBouton("Changer d'expression", () -> { if (joueur != null) joueur.traits().expression = (joueur.traits().expression + 1) % Traits.EXPRESSIONS; });
        sectionGens.ajouterBouton("Changer d'habits", () -> { if (joueur != null) joueur.habits = Tenue.tirer(hasard.nextLong(), joueur.apparence.femme, joueur.apparence.enfant); });
        sectionGens.ajouterBouton("Sortir / ranger arme droite", () -> basculerMain(Tenue.MAIN_DROITE));
        sectionGens.ajouterBouton("Sortir / ranger objet gauche", () -> basculerMain(Tenue.MAIN_GAUCHE));
        sectionGens.ajouterBouton("Effacer les traces", () -> { if (traces != null) traces.effacer(); });

        FenetreDebug.Section sectionJoueur = fenetreDebug.ajouterSection("Joueur");
        sectionJoueur.ajouterBouton("Entrer dans le monde", () -> {
            if (histoire != null) jouer(histoire.joueur::ouvrir);
        });

        FenetreDebug.Section sectionHistoire = fenetreDebug.ajouterSection("Histoire");
        infoHistoire(sectionHistoire, "Annee", () -> String.valueOf(histoire.population.annee));
        infoHistoire(sectionHistoire, "Vivants", () -> String.valueOf(histoire.population.vivants));
        infoHistoire(sectionHistoire, "Regions habitees", () -> String.valueOf(histoire.population.regionsHabitees));
        infoHistoire(sectionHistoire, "Ayant vecu", () -> String.valueOf(histoire.population.naissancesTotales));
        infoHistoire(sectionHistoire, "Age moyen au deces", () -> String.format("%.1f ans", histoire.population.ageMoyenAuDeces));
        infoHistoire(sectionHistoire, "Duree d'une annee", () -> String.format("%.1f ms", histoire.msParAn));
        sectionHistoire.ajouterBouton("Recommencer", this::arreterHistoire);
        infoHistoire(sectionHistoire, "Sauvegarde", () -> histoire.sauvegarde);
        sectionHistoire.ajouterBouton("Sauvegarder maintenant", () -> {
            if (histoire != null) histoire.demander(histoire::enregistrer);
        });


        Population.Reglages p = reglagesPopulation;
        FenetreDebug.Section depart = sectionHistoire.ajouterSection("Depart");
        reglerDepart(depart, "Vivants maximum", 1000000f, 20000000f, 1000000f, p.populationMax, v -> p.populationMax = v.intValue());
        reglerDepart(depart, "Habitants par km2 fertile", 0.5f, 10f, 0.5f, p.habitantsParKm2, v -> p.habitantsParKm2 = v);
        FenetreDebug.Section demographie = sectionHistoire.ajouterSection("Demographie");
        demographie.ajouterSlider("Natalite", 0.05f, 0.6f, 0.01f, p.natalite, v -> p.natalite = v);
        demographie.ajouterSlider("Frein natalite", 0.1f, 1f, 0.01f, p.freinNatalite, v -> p.freinNatalite = v);
        demographie.ajouterSlider("Famine", 0f, 5f, 0.1f, p.famine, v -> p.famine = v);

        FenetreDebug.Section migrations = sectionHistoire.ajouterSection("Migrations");
        migrations.ajouterSlider("Seuil", 0.1f, 1.5f, 0.05f, p.seuilMigration, v -> p.seuilMigration = v);
        migrations.ajouterSlider("Force", 0f, 1f, 0.05f, p.migration, v -> p.migration = v);
        migrations.ajouterSlider("Part par la mer", 0f, 0.3f, 0.01f, p.partMer, v -> p.partMer = v);
        FenetreDebug.Section sectionVilles = sectionHistoire.ajouterSection("Villes");
        infoHistoire(sectionVilles, "Villes vivantes", () -> String.valueOf(histoire.population.villes.nombre - histoire.population.villes.ruines));
        infoHistoire(sectionVilles, "Ruines", () -> String.valueOf(histoire.population.villes.ruines));
        sectionVilles.ajouterSlider("Seuil de fondation", 500f, 6000f, 100f, p.seuilVille, v -> p.seuilVille = v.intValue());
        sectionVilles.ajouterSlider("Colons", 20f, 500f, 10f, p.colons, v -> p.colons = v.intValue());
        sectionVilles.ajouterSlider("Exode rural", 0f, 0.3f, 0.01f, p.exode, v -> p.exode = v);
        FenetreDebug.Section grandes = sectionVilles.ajouterSection("Plus grandes");

        Etats.Reglages reglagesEtats = p.etats;
        FenetreDebug.Section sectionEtats = sectionHistoire.ajouterSection("Etats");
        infoHistoire(sectionEtats, "Etats vivants", () -> String.valueOf(histoire.population.etats.vivants()));
        infoHistoire(sectionEtats, "Chutes", () -> String.valueOf(histoire.population.etats.chutes));
        infoHistoire(sectionEtats, "Guerres", () -> String.valueOf(histoire.population.etats.guerres));
        infoHistoire(sectionEtats, "Revolutions", () -> String.valueOf(histoire.population.etats.revolutions));
        infoHistoire(sectionEtats, "Morts au combat", () -> String.valueOf(histoire.population.etats.mortsAuCombat));
        infoHistoire(sectionEtats, "Morts des sacs", () -> String.valueOf(histoire.population.etats.mortsDesSacs));
        sectionEtats.ajouterSlider("Envie de guerre", 0f, 0.1f, 0.005f, reglagesEtats.envieGuerre, v -> reglagesEtats.envieGuerre = v);
        sectionEtats.ajouterSlider("Siege", 1f, 4f, 0.1f, reglagesEtats.siege, v -> reglagesEtats.siege = v);
        sectionEtats.ajouterSlider("Part de l'armee", 0.005f, 0.1f, 0.005f, reglagesEtats.partArmee, v -> reglagesEtats.partArmee = v);
        sectionEtats.ajouterSlider("Seuil de revolution", 0f, 50f, 1f, reglagesEtats.seuilRevolution, v -> reglagesEtats.seuilRevolution = v);
        FenetreDebug.Section sectionChronique = sectionEtats.ajouterSection("Chronique");
        for (int rang = 0; rang < 6; rang++) {
            ajouterFait(sectionChronique, rang);
        }

        FenetreDebug.Section sectionFamilles = sectionHistoire.ajouterSection("Familles");
        infoHistoire(sectionFamilles, "Maisons", () -> String.valueOf(histoire.population.maisons.nombre));
        infoHistoire(sectionFamilles, "Regnes", () -> String.valueOf(histoire.population.maisons.nombreRegnes));
        infoHistoire(sectionFamilles, "Heritages", () -> String.valueOf(histoire.population.etats.heritages));
        infoHistoire(sectionFamilles, "Crises", () -> String.valueOf(histoire.population.etats.crises));
        infoHistoire(sectionFamilles, "Regences", () -> String.valueOf(histoire.population.etats.regences));
        infoHistoire(sectionFamilles, "Usurpations", () -> String.valueOf(histoire.population.etats.usurpations));
        sectionFamilles.ajouterBouton("Voir les grandes maisons", () -> montrerTexte("Maisons", q -> Recit.grandesMaisons(q, 25)));
        sectionFamilles.ajouterSlider("Chance d'usurpation", 0f, 0.5f, 0.01f, reglagesEtats.usurpation, v -> reglagesEtats.usurpation = v);


        Livres.Reglages reglagesLivres = p.livres;
        FenetreDebug.Section sectionSavoir = sectionHistoire.ajouterSection("Savoir");
        infoHistoire(sectionSavoir, "Oeuvres", () -> String.valueOf(histoire.population.livres.nombre));
        infoHistoire(sectionSavoir, "Oeuvres survivantes", () -> String.valueOf(histoire.population.livres.vivantes()));
        infoHistoire(sectionSavoir, "Chansons", () -> String.valueOf(histoire.population.livres.parType[Livres.CHANSON]));
        sectionSavoir.ajouterBouton("Interroger un paysan", () -> montrerTexte("Temoignage", q -> Recit.interroger(q, 0, MathUtils.random.nextLong())));
        sectionSavoir.ajouterBouton("Interroger un citadin", () -> montrerTexte("Temoignage", q -> Recit.interroger(q, 1, MathUtils.random.nextLong())));
        sectionSavoir.ajouterBouton("Interroger un lettre", () -> montrerTexte("Temoignage", q -> Recit.interroger(q, 2, MathUtils.random.nextLong())));
        sectionSavoir.ajouterBouton("Lire un livre", () -> montrerTexte("Livre", q -> Recit.lireAuHasard(q, true, MathUtils.random.nextLong())));
        sectionSavoir.ajouterBouton("Ecouter une chanson", () -> montrerTexte("Chanson", q -> Recit.lireAuHasard(q, false, MathUtils.random.nextLong())));
        sectionSavoir.ajouterBouton("Lire le plus ancien livre", () -> montrerTexte("Livre", Recit::lirePlusAncien));
        sectionSavoir.ajouterBouton("Voir la vraie chronique", () -> montrerTexte("Chronique", q -> Recit.verite(q, 60)));
        sectionSavoir.ajouterSlider("Portee des nouvelles", 0.2f, 5f, 0.1f, reglagesLivres.portee, v -> reglagesLivres.portee = v);
        sectionSavoir.ajouterSlider("Memoire orale", 10f, 200f, 5f, reglagesLivres.demiVie, v -> reglagesLivres.demiVie = v);
        sectionSavoir.ajouterSlider("Ecriture", 0f, 0.02f, 0.001f, reglagesLivres.ecriture, v -> reglagesLivres.ecriture = v);
        sectionSavoir.ajouterSlider("Perte des copies", 0f, 0.02f, 0.001f, reglagesLivres.perteCopie, v -> reglagesLivres.perteCopie = v);


        Economie.Reglages e = p.economie;
        FenetreDebug.Section sectionEconomie = sectionHistoire.ajouterSection("Economie");
        sectionEconomie.ajouterSlider("Rendement paysan", 1f, 3f, 0.05f, e.rendement, v -> e.rendement = v);
        sectionEconomie.ajouterSlider("Cout terre", 0.01f, 0.3f, 0.01f, e.coutTerre, v -> e.coutTerre = v);
        sectionEconomie.ajouterSlider("Cout mer", 0.01f, 0.3f, 0.01f, e.coutMer, v -> e.coutMer = v);
        sectionEconomie.ajouterSlider("Emplois du commerce", 0f, 3f, 0.1f, e.emplois, v -> e.emplois = v);
        sectionEconomie.ajouterSlider("Part urbaine du commerce", 0f, 0.9f, 0.05f, e.partCommerce, v -> e.partCommerce = v);

        for (int g = 0; g < Economie.NOMBRE; g++) {
            ajouterMarchandise(sectionEconomie, g);
        }


        for (int rang = 0; rang < 5; rang++) {
            ajouterGrandeVille(grandes, rang);
        }

        for (int q = 0; q < Peuple.NOMBRE; q++) {
            ajouterPeuple(sectionHistoire, q);
        }

    }

    private void jouer(Supplier<Joueur.Scene> suite) {
        if (histoire == null) return;
        Histoire courante = histoire;
        if (courante.population.annee < courante.dureeMax) {
            fenetreDebug.afficherTexte("Joueur", "L'histoire n'est pas encore finie.");
            return;
        }
        courante.demander(() -> {
            Joueur.Scene scene = suite.get();
            Gdx.app.postRunnable(() -> fenetreDebug.afficherChoix(scene.titre, scene.texte, scene.choix, i -> jouer(scene.suites.get(i))));
        });
    }

    private void montrerTexte(String titre, Function<Population, String> texte) {
        if (histoire == null) return;
        Histoire courante = histoire;
        courante.demander(() -> {
            String resultat = texte.apply(courante.population);
            Gdx.app.postRunnable(() -> fenetreDebug.afficherTexte(titre, resultat));
        });
    }


    private void ajouterFait(FenetreDebug.Section section, int rang) {
        infoHistoire(section, String.valueOf(rang + 1), () -> {
            Population p = histoire.population;
            int i = p.etats.chronique.recent(rang);
            return i < 0 ? "-" : p.etats.chronique.texte(p, i);
        });
    }


    private void ajouterMarchandise(FenetreDebug.Section parent, int g) {
        FenetreDebug.Section section = parent.ajouterSection(Economie.NOMS[g]);
        infoHistoire(section, "Production", () -> String.format("%.0f", histoire.population.economie.total(histoire.population.economie.production, g)));
        infoHistoire(section, "Besoin", () -> String.format("%.0f", histoire.population.economie.total(histoire.population.economie.besoin, g)));
        infoHistoire(section, "Stock", () -> String.format("%.0f", histoire.population.economie.stockTotal(g)));
        infoHistoire(section, "Prix moyen", () -> String.format("%.2f", histoire.population.economie.prixMoyen(g)));
    }


    private void ajouterGrandeVille(FenetreDebug.Section section, int rang) {
        infoHistoire(section, String.valueOf(rang + 1), () -> {
            Population p = histoire.population;
            int v = p.villes.plusGrandes(rang + 1)[rang];
            if (v < 0) return "-";
            return Noms.ville(p.villes.peuple[v], p.graine, v) + " " + p.villes.habitants[v];
        });
    }

    private void ajouterPeuple(FenetreDebug.Section parent, int q) {
        Peuple peuple = reglagesPopulation.peuples[q];
        FenetreDebug.Section section = parent.ajouterSection(peuple.nom);
        infoHistoire(section, "Vivants", () -> String.valueOf(histoire.population.vivantsPeuple[q]));
        infoHistoire(section, "Regions", () -> String.valueOf(histoire.population.regionsPeuple[q]));
        infoHistoire(section, "Urbains", () -> String.valueOf(histoire.population.urbainsPeuple[q]));
        infoHistoire(section, "Age au deces", () -> String.format("%.0f ans", histoire.population.ageAuDeces[q]));
        section.ajouterSlider("Mobilite", 0f, 2f, 0.1f, peuple.mobilite, v -> peuple.mobilite = v);
        section.ajouterSlider("Part urbaine", 0f, 1f, 0.05f, peuple.urbain, v -> peuple.urbain = v);
        reglerDepart(section, "Rythme de vie", 0.05f, 2f, 0.05f, peuple.rythme, v -> peuple.rythme = v);
        reglerDepart(section, "Annee d'eveil", 0f, 4000f, 100f, peuple.eveil, v -> peuple.eveil = v.intValue());
        reglerDepart(section, "Berceaux", 1f, 6f, 1f, peuple.berceaux, v -> peuple.berceaux = v.intValue());
        reglerDepart(section, "Fondateurs", 200f, 5000f, 100f, peuple.fondateurs, v -> peuple.fondateurs = v.intValue());
    }

    private void infoHistoire(FenetreDebug.Section section, String nom, Supplier<String> valeur) {
        section.ajouterInfo(nom, () -> histoire == null ? "-" : valeur.get());
    }

    private void reglerDepart(FenetreDebug.Section section, String nom, float min, float max, float pas, float depart, Consumer<Float> action) {
        section.ajouterSlider(nom, min, max, pas, depart, valeur -> {
            action.accept(valeur);
            arreterHistoire();
        });
    }

    private void arreterHistoire() {
        reprendreHistoire = false;
        if (histoire != null) {
            histoire.arreter();
            histoire = null;
        }
    }

    private void infoRegion(FenetreDebug.Section section, String nom, Supplier<String> valeur) {
        section.ajouterInfo(nom, () -> regions == null ? "-" : valeur.get());
    }

    private void reglerRegion(FenetreDebug.Section section, String nom, float min, float max, float pas, float depart, Consumer<Float> action) {
        section.ajouterSlider(nom, min, max, pas, depart, valeur -> {
            action.accept(valeur);
            arreterHistoire();
            regions = null;
            modeCarte = -1;
        });
    }

    private double surfaceCase() {
        return 4.0 * Math.PI * Terrain.RAYON_KM * Terrain.RAYON_KM / monde.plaque.length;
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        carte.dispose();
        fond.dispose();
        rendu.dispose();
        if (tuiles != null) tuiles.dispose();
        if (vegetation != null) vegetation.dispose();
        if (assembleur != null) assembleur.dispose();
        if (batiments != null) batiments.dispose();
        if (meteo != null) meteo.dispose();
        if (ciel != null) ciel.dispose();
        if (traces != null) traces.dispose();
        if (rues != null) rues.dispose();
        fenetreDebug.dispose();
        chargement.dispose();
        arreterHistoire();
    }
}
