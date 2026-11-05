package com.deldaran.politics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.deldaran.politics.planet.GenerateurMonde;
import com.deldaran.politics.planet.Monde;
import com.deldaran.politics.planet.Plaque;
import com.deldaran.politics.planet.Tectonique;
import com.deldaran.politics.planet.Sphere;
public class FirstScreen implements Screen {
    private static final Color OCEAN_PROFOND = new Color(0.05f, 0.15f, 0.40f, 1f);
    private static final Color OCEAN_COTIER  = new Color(0.20f, 0.45f, 0.80f, 1f);
    private static final Color TERRE_BASSE   = new Color(0.30f, 0.60f, 0.25f, 1f);
    private static final Color TERRE_HAUTE   = new Color(0.55f, 0.45f, 0.30f, 1f);

    GameCamera camera;
    CameraController cameraController;
    ExtendViewport viewport;
    ShapeRenderer shapeRenderer;
    Monde monde;
    Tectonique tectonique;
    FenetreDebug fenetreDebug;
    boolean modePlaques = true;

    boolean enPause = false;
    float tempsAccumule = 0f;

    private final Color couleurTemp = new Color();

    @Override
    public void show() {
        camera = new GameCamera();
        viewport = new ExtendViewport(40, 22.5f, camera);
        cameraController = new CameraController(camera);
        shapeRenderer = new ShapeRenderer();

        monde = new GenerateurMonde().genererMonde(512, 256, 42);

        tectonique = new Tectonique(monde);

        camera.position.set(monde.largeur / 2f, monde.hauteur / 2f, 0);

        fenetreDebug = new FenetreDebug();
        fenetreDebug.ajouterInfo("FPS", () -> String.valueOf(Gdx.graphics.getFramesPerSecond()));
        fenetreDebug.ajouterInfo("Mode (TAB)", () -> modePlaques ? "plaques" : "relief");
        fenetreDebug.ajouterInfo("Simulation (ESPACE)", () -> enPause ? "en pause" : "en cours");
        fenetreDebug.ajouterInfo("Temps", () -> (int) monde.temps + " Millions d'années");
        fenetreDebug.ajouterInfo("Nombre de plaques", () -> String.valueOf(monde.plaques.size));
        fenetreDebug.ajouterInfo("Niveau de la mer", () -> String.format("%.2f km", monde.niveauMer));
        fenetreDebug.ajouterInfo("épaisseur maximale", () -> String.format("%.2f km", monde.epaisseurMax()));
        fenetreDebug.ajouterInfo("Fractures", () -> String.valueOf(tectonique.nombreFractures));
        fenetreDebug.ajouterInfo("Ruptures", () -> String.valueOf(tectonique.nombreRuptures));
        fenetreDebug.ajouterInfo("Soudures", () -> String.valueOf(tectonique.nombreSoudures));

        Gdx.input.setInputProcessor(new InputMultiplexer(fenetreDebug.getStage(), cameraController));
    }

    @Override
    public void render(float delta) {
        cameraController.update(delta);
        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            modePlaques = !modePlaques;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            enPause = !enPause;
        }  

        if (!enPause) {
            tempsAccumule += delta;
            while (tempsAccumule >= 0.1f) {
                tectonique.pas();
                tempsAccumule -= 0.1f;
            }
        }

        ScreenUtils.clear(Color.BLACK);

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        int debutX = camera.getDebutX();
        int finX   = camera.getFinX(monde.largeur);
        int debutY = camera.getDebutY();
        int finY   = camera.getFinY(monde.hauteur);

        for (int y = debutY; y < finY; y++) {
            for (int x = debutX; x < finX; x++) {
                int i = monde.index(x, y);
                if (modePlaques) {
                    couleurPlaque(i);
                } else {
                    couleurRelief(i);
                }
                shapeRenderer.setColor(couleurTemp);
                shapeRenderer.rect(x, y, 1, 1);
            }
        }

        
        shapeRenderer.end();

        fenetreDebug.dessiner(delta);
    }

    private void couleurPlaque(int i) {
        Plaque plaque = monde.plaques.get(monde.plaque[i]);
        couleurTemp.set(plaque.couleur);
        if (!monde.isContinentale[i]) {
            couleurTemp.mul(0.7f, 0.7f, 0.7f, 1f);
        }
    }

    private void couleurRelief(int i) {
        float hauteurRelative = monde.altitude[i] - monde.niveauMer;
        if (hauteurRelative < 0) {
            float t = MathUtils.clamp(-hauteurRelative / 7f, 0f, 1f);
            couleurTemp.set(OCEAN_COTIER).lerp(OCEAN_PROFOND, t);
        } else {
            float t = MathUtils.clamp(hauteurRelative / 4f, 0f, 1f);
            couleurTemp.set(TERRE_BASSE).lerp(TERRE_HAUTE, t);
        }
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) return;
        viewport.update(width, height);
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
        shapeRenderer.dispose();
        fenetreDebug.dispose();
    }
}
