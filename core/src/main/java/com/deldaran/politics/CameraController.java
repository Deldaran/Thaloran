package com.deldaran.politics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;

public class CameraController extends InputAdapter {
    private final GameCamera camera;

    public CameraController(GameCamera camera) {
        this.camera = camera;
    }

    public void update(float delta) {
        float pas = camera.vitesse * camera.zoom * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) camera.deplacer(0, pas);
        if (Gdx.input.isKeyPressed(Input.Keys.S)) camera.deplacer(0, -pas);
        if (Gdx.input.isKeyPressed(Input.Keys.A)) camera.deplacer(-pas, 0);
        if (Gdx.input.isKeyPressed(Input.Keys.D)) camera.deplacer(pas, 0);
        camera.update();
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        camera.glisser(Gdx.input.getDeltaX(), Gdx.input.getDeltaY(), Gdx.graphics.getWidth());
        return true;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        camera.zoomer(amountY * 0.1f * camera.zoom);
        return true;
    }
}
