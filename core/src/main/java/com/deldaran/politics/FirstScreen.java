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
import com.deldaran.politics.histoire.Population;
import com.deldaran.politics.histoire.Peuple;


public class FirstScreen implements Screen {

    GameCamera camera;
    CameraController cameraController;
    CarteCube carte;
    RenduGlobe rendu;
    int[] couleurs;
    float tempsCarte = 0f;
    int modeCarte = -1;

    Monde monde;
    Simulation simulation;
    FileHandle fichierMonde;
    boolean mondeEnregistre;
    Regions regions;
    final Regions.Reglages reglagesRegions = new Regions.Reglages();
    final Population.Reglages reglagesPopulation = new Population.Reglages();
    
    float pasParSeconde = 10f;

    FenetreDebug fenetreDebug;
    int modeAffichage = 0;
    float dernierClimat = -1000f;
    
    RenduTuiles tuiles;
    private static final long GRAINE = 42L;

    
    Histoire histoire;
    boolean enPause = false;
    float tempsAccumule = 0f;
    private static final float SEUIL_RIVIERE = 10f;
    private static final String[] NOMS_BIOMES = { "Toundra", "Taiga", "Prairie", "Foret", "Desert", "Savane", "Jungle", "Montagne" };
    private static final byte[] BIOMES_FERTILES = { Biome.TOUNDRA, Biome.TAIGA, Biome.PRAIRIE, Biome.FORET, Biome.DESERT, Biome.SAVANE, Biome.JUNGLE, Biome.MONTAGNE };

    private static final float PERIODE_CARTE = 0.1f;
    private static final double RAYON_TUILES = 300.0;
    private static final Color[] COULEURS_PEUPLES = {
        new Color(0.39f, 0.83f, 0.44f, 1f), new Color(0.85f, 0.40f, 0.23f, 1f), new Color(0.95f, 0.81f, 0.24f, 1f), new Color(0.64f, 0.23f, 0.82f, 1f)
    };


    private static final String[] NOMS_MODES = { "plaques", "relief", "temperature", "pluie", "biomes", "regions", "nourriture", "population" };

    private final Color couleurTemp = new Color();

    @Override
    public void show() {
        camera = new GameCamera();
        cameraController = new CameraController(camera);


        monde = new GenerateurMonde().genererMonde(512, 256, GRAINE);

        simulation = new Simulation(monde);
        fichierMonde = Gdx.files.external("Politics/mondes/monde-" + GRAINE + ".bin");
        mondeEnregistre = Sauvegarde.charger(monde, simulation, fichierMonde);

        
        carte = new CarteCube(monde.sphere);
        rendu = new RenduGlobe();
        couleurs = new int[monde.sphere.nombre];

        fenetreDebug = new FenetreDebug();
        remplirDebug();

        Gdx.input.setInputProcessor(new InputMultiplexer(fenetreDebug.getStage(), cameraController));
    }

    @Override
    public void render(float delta) {
        cameraController.update(delta);
        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            modeAffichage = (modeAffichage + 1) % NOMS_MODES.length;
        }
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
            histoire = new Histoire(monde, regions, reglagesPopulation);
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
        rendu.dessiner(carte, camera);
        dessinerTuiles();

        fenetreDebug.dessiner(delta);
    }

    private void couleurPlaque(int i) {
        Plaque plaque = monde.plaques.get(monde.plaque[i]);
        couleurTemp.set(plaque.couleur);
        if (!monde.isContinentale[i]) {
            couleurTemp.mul(0.7f, 0.7f, 0.7f, 1f);
        }
    }

    private void dessinerTuiles() {
        if (simulation.phase != Simulation.Phase.TERMINEE) return;
        if (tuiles == null) {
            tuiles = new RenduTuiles(new Terrain(monde, simulation.climat.hauteurPlaines));
        }
        boolean carteFine = modeAffichage == 1 || modeAffichage == 4;
        if (carteFine && camera.rayonPixels >= RAYON_TUILES) {
            tuiles.dessiner(camera, modeAffichage == 4);
        }
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

            default: Palette.biome(monde.biome[i], couleurTemp); ajouterEau(i); break;

        }
    }

    private void couleurPopulation(int i) {
        if (histoire == null) {
            couleurTemp.set(Color.DARK_GRAY);
            return;
        }
        Population p = histoire.population;
        int r = p.regions.regionDe[i];
        if (!p.regions.estTerre[r]) {
            couleurTemp.set(0.06f, 0.12f, 0.24f, 1f);
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
            couleurTemp.set(0.29f, 0.29f, 0.26f, 1f);
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
            couleurTemp.set(0.06f, 0.12f, 0.24f, 1f);
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
        FenetreDebug.Section sectionHistoire = fenetreDebug.ajouterSection("Histoire");
        infoHistoire(sectionHistoire, "Annee", () -> String.valueOf(histoire.population.annee));
        infoHistoire(sectionHistoire, "Vivants", () -> String.valueOf(histoire.population.vivants));
        infoHistoire(sectionHistoire, "Regions habitees", () -> String.valueOf(histoire.population.regionsHabitees));
        infoHistoire(sectionHistoire, "Ayant vecu", () -> String.valueOf(histoire.population.naissancesTotales));
        infoHistoire(sectionHistoire, "Age moyen au deces", () -> String.format("%.1f ans", histoire.population.ageMoyenAuDeces));
        infoHistoire(sectionHistoire, "Duree d'une annee", () -> String.format("%.1f ms", histoire.msParAn));
        sectionHistoire.ajouterBouton("Recommencer", this::arreterHistoire);

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
        for (int q = 0; q < Peuple.NOMBRE; q++) {
            ajouterPeuple(sectionHistoire, q);
        }

    }

    private void ajouterPeuple(FenetreDebug.Section parent, int q) {
        Peuple peuple = reglagesPopulation.peuples[q];
        FenetreDebug.Section section = parent.ajouterSection(peuple.nom);
        infoHistoire(section, "Vivants", () -> String.valueOf(histoire.population.vivantsPeuple[q]));
        infoHistoire(section, "Regions", () -> String.valueOf(histoire.population.regionsPeuple[q]));
        infoHistoire(section, "Age au deces", () -> String.format("%.0f ans", histoire.population.ageAuDeces[q]));
        section.ajouterSlider("Mobilite", 0f, 2f, 0.1f, peuple.mobilite, v -> peuple.mobilite = v);
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
        rendu.dispose();
        if (tuiles != null) tuiles.dispose();
        fenetreDebug.dispose();
        arreterHistoire();
    }
}
