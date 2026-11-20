package com.deldaran.politics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;

public class CameraController extends InputAdapter {
    private static final double FACTEUR_ZOOM = 1.15;

    private final GameCamera camera;
    public boolean libre = true, bloque;

    public CameraController(GameCamera camera) {
        this.camera = camera;
    }

    public void update(float delta) {
        if (!libre || bloque) return;
        double pas = camera.vitessePixels * delta;
        double x = 0.0, y = 0.0;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) y += 1.0;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) y -= 1.0;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) x -= 1.0;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) x += 1.0;
        double longueur = Math.hypot(x, y);
        if (longueur > 0.0) camera.glisserEcran(x / longueur * pas, y / longueur * pas);
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        camera.glisserEcran(-Gdx.input.getDeltaX(), Gdx.input.getDeltaY());
        return true;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        camera.zoomer(Math.pow(FACTEUR_ZOOM, -amountY));
        return true;
    }
}
