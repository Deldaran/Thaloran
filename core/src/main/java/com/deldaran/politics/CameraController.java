package com.deldaran.politics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;

public class CameraController extends InputAdapter {
    private static final double FACTEUR_ZOOM = 1.15;

    private final GameCamera camera;

    public CameraController(GameCamera camera) {
        this.camera = camera;
    }

    public void update(float delta) {
        double pas = camera.vitessePixels * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) camera.deplacerPixels(0, pas);
        if (Gdx.input.isKeyPressed(Input.Keys.S)) camera.deplacerPixels(0, -pas);
        if (Gdx.input.isKeyPressed(Input.Keys.A)) camera.deplacerPixels(-pas, 0);
        if (Gdx.input.isKeyPressed(Input.Keys.D)) camera.deplacerPixels(pas, 0);
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        camera.deplacerPixels(-Gdx.input.getDeltaX(), Gdx.input.getDeltaY());
        return true;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        camera.zoomer(Math.pow(FACTEUR_ZOOM, -amountY));
        return true;
    }
}
