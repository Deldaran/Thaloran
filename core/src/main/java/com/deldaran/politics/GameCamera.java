package com.deldaran.politics;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;

public class GameCamera extends OrthographicCamera {
    public float vitesse = 20f;
    public float zoomMin = 0.1f;
    public float zoomMax = 500f; 

    public void deplacer(float dx, float dy) {
        position.x += dx;
        position.y += dy;
    }

    public void zoomer(float quantite) {
        zoom = MathUtils.clamp(zoom + quantite, zoomMin, zoomMax);
    }

    public void glisser(float pixelsX, float pixelsY, float largeurEcranPixels) {
        float tuilesParPixel = viewportWidth * zoom / largeurEcranPixels;
        deplacer(-pixelsX * tuilesParPixel, pixelsY * tuilesParPixel);
    }

    public int getDebutX() {
        return Math.max(0, MathUtils.floor(position.x - viewportWidth * zoom / 2f));
    }

    public int getFinX(int largeurCarte) {
        return Math.min(largeurCarte, MathUtils.ceil(position.x + viewportWidth * zoom / 2f));
    }

    public int getDebutY() {
        return Math.max(0, MathUtils.floor(position.y - viewportHeight * zoom / 2f));
    }

    public int getFinY(int hauteurCarte) {
        return Math.min(hauteurCarte, MathUtils.ceil(position.y + viewportHeight * zoom / 2f));
    }
}
