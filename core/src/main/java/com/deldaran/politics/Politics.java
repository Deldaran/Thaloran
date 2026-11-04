package com.deldaran.politics;

import com.badlogic.gdx.Game;

public class Politics extends Game {
    @Override
    public void create() {
        setScreen(new FirstScreen());
    }
}