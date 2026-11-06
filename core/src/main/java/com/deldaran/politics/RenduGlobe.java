package com.deldaran.politics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class RenduGlobe {
    private final ShaderProgram shader;
    private final Mesh ecran;

    public RenduGlobe() {
        shader = new ShaderProgram(Gdx.files.internal("shaders/globe.vert"), Gdx.files.internal("shaders/globe.frag"));
        if (!shader.isCompiled()) {
            throw new IllegalStateException(shader.getLog());
        }
        ecran = new Mesh(true, 4, 0, new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"));
        ecran.setVertices(new float[] { -1f, -1f, 1f, -1f, 1f, 1f, -1f, 1f });
    }

    public void dessiner(CarteCube carte, GameCamera camera) {
        carte.texture.bind(0);
        shader.bind();
        shader.setUniformi("u_carte", 0);
        shader.setUniformMatrix("u_repere", camera.repere());
        shader.setUniformf("u_centre", Gdx.graphics.getBackBufferWidth() / 2f, Gdx.graphics.getBackBufferHeight() / 2f);
        shader.setUniformf("u_rayon", (float) camera.rayonPixels);
        ecran.render(shader, GL20.GL_TRIANGLE_FAN);
    }

    public void dispose() {
        shader.dispose();
        ecran.dispose();
    }
}
