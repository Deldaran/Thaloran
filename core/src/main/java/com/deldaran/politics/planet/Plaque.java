package com.deldaran.politics.planet;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class Plaque {
    public final int id;
    public final Vector3 rotation = new Vector3();
    public final Vector3 rotationAccumulee = new Vector3();

    public Color couleur = new Color();

    public Plaque(int id) {
        this.id = id;
    }
}
