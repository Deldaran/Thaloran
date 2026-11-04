package com.deldaran.politics.planet;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;

public class Plaque {
    public final int id;
    public Vector2 vitesse = new Vector2();
    public Color couleur = new Color();
    
    public float decalageX , decalageY;

    public Plaque(int id) {
        this.id = id;
    }
}
