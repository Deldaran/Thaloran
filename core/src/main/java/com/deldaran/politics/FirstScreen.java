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
    float pasParSeconde = 10f;

    FenetreDebug fenetreDebug;
    int modeAffichage = 0;
    float dernierClimat = -1000f;
    
    RenduTuiles tuiles;

    boolean enPause = false;
    float tempsAccumule = 0f;
    private static final float SEUIL_RIVIERE = 10f;

    private static final float PERIODE_CARTE = 0.1f;
    private static final double RAYON_TUILES = 300.0;


    private static final String[] NOMS_MODES = { "plaques", "relief", "temperature", "pluie", "biomes" };
    private static final Color[] COULEURS_BIOMES = {
        new Color(0x123a7aff), new Color(0xcfe3f2ff), new Color(0xf4f8fbff), new Color(0x9aa58cff), new Color(0x3f6b55ff),
        new Color(0xa9c060ff), new Color(0x3c8a3aff), new Color(0xe3cf8cff), new Color(0xc2b24cff), new Color(0x116b2aff),
        new Color(0x8a8580ff), new Color(0xf6f9fcff)
    };

    private final Color couleurTemp = new Color();

    @Override
    public void show() {
        camera = new GameCamera();
        cameraController = new CameraController(camera);


        monde = new GenerateurMonde().genererMonde(512, 256, 42);

        simulation = new Simulation(monde);
        
        carte = new CarteCube(monde.sphere);
        rendu = new RenduGlobe();
        couleurs = new int[monde.sphere.nombre];

        fenetreDebug = new FenetreDebug();
        fenetreDebug.ajouterInfo("FPS", () -> String.valueOf(Gdx.graphics.getFramesPerSecond()));
        fenetreDebug.ajouterInfo("Mode (TAB)", () -> NOMS_MODES[modeAffichage]);
        fenetreDebug.ajouterInfo("Simulation (ESPACE)", () -> enPause ? "en pause" : "en cours");
        fenetreDebug.ajouterInfo("Phase", () -> simulation.phase.name());
        fenetreDebug.ajouterInfo("Temps", () -> simulation.tempsTexte());
        fenetreDebug.ajouterInfo("Nombre de plaques", () -> String.valueOf(monde.plaques.size));
        fenetreDebug.ajouterInfo("Niveau de la mer", () -> String.format("%.2f km", monde.niveauMer));
        fenetreDebug.ajouterInfo("épaisseur maximale", () -> String.format("%.2f km", monde.epaisseurMax()));
        fenetreDebug.ajouterInfo("Fractures", () -> String.valueOf(simulation.tectonique.nombreFractures));
        fenetreDebug.ajouterInfo("Ruptures", () -> String.valueOf(simulation.tectonique.nombreRuptures));
        fenetreDebug.ajouterInfo("Soudures", () -> String.valueOf(simulation.tectonique.nombreSoudures));

        fenetreDebug.ajouterSlider("Pas par seconde", 10f, 500f, 10f, 10f, valeur -> pasParSeconde = valeur);

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
            default: Palette.biome(monde.biome[i], couleurTemp); ajouterEau(i); break;

        }
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
    }
}
