package com.deldaran.politics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.deldaran.politics.planet.Cube;
import com.deldaran.politics.planet.Terrain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class RenduTuiles {
    public static final int NIVEAU_MAX = 11;

    private static final int FLOTTANTS_PAR_POINT = 5;
    private static final float TAILLE_DIVISION = 384f;
    private static final int ENVOIS_PAR_IMAGE = 8;
    private static final int TUILES_MAX = 500;

    private final Map<Long, Tuile> tuiles = new HashMap<>();
    private final LinkedBlockingQueue<Tuile> file = new LinkedBlockingQueue<>();
    private final ConcurrentLinkedQueue<Tuile> terminees = new ConcurrentLinkedQueue<>();
    private final AtomicInteger enCours = new AtomicInteger();
    private final List<Thread> ouvriers = new ArrayList<>();

    private final ShaderProgram shader;
    private final Mesh mesh;
    private final float[] sommets = new float[Tuile.POINTS * Tuile.POINTS * FLOTTANTS_PAR_POINT];

    private final double[] est = new double[3];
    private final double[] nord = new double[3];
    private final double[] centre = new double[3];
    private double rayon;
    private float largeur, hauteur;
    private long image = 0;
    private boolean biomes;

    private float minX, maxX, minY, maxY;
    private boolean devant;

    public RenduTuiles(Terrain terrain) {
        shader = new ShaderProgram(Gdx.files.internal("shaders/tuile.vert"), Gdx.files.internal("shaders/tuile.frag"));
        if (!shader.isCompiled()) {
            throw new IllegalStateException(shader.getLog());
        }
        mesh = new Mesh(false, Tuile.POINTS * Tuile.POINTS, Tuile.SEGMENTS * Tuile.SEGMENTS * 6,
            new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"),
            new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0"),
            new VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_hauteur"));
        mesh.setIndices(creerIndices());

        int nombre = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
        for (int i = 0; i < nombre; i++) {
            Thread ouvrier = new Thread(() -> travailler(new AtelierTuile(terrain)));
            ouvrier.setDaemon(true);
            ouvrier.start();
            ouvriers.add(ouvrier);
        }
    }

    private short[] creerIndices() {
        short[] indices = new short[Tuile.SEGMENTS * Tuile.SEGMENTS * 6];
        int n = 0;
        for (int j = 0; j < Tuile.SEGMENTS; j++) {
            for (int i = 0; i < Tuile.SEGMENTS; i++) {
                short a = (short) (j * Tuile.POINTS + i);
                short b = (short) (a + 1);
                short c = (short) (a + Tuile.POINTS);
                short d = (short) (c + 1);
                indices[n++] = a;
                indices[n++] = b;
                indices[n++] = d;
                indices[n++] = a;
                indices[n++] = d;
                indices[n++] = c;
            }
        }
        return indices;
    }

    private void travailler(AtelierTuile atelier) {
        try {
            while (true) {
                Tuile tuile = file.take();
                if (!tuile.etat.compareAndSet(Tuile.VIDE, Tuile.EN_COURS)) continue;
                enCours.incrementAndGet();
                tuile.image = atelier.generer(tuile);
                tuile.etat.set(Tuile.CALCULEE);
                terminees.add(tuile);
                enCours.decrementAndGet();
            }
        } catch (InterruptedException fin) {
            Thread.currentThread().interrupt();
        }
    }

    public int nombreTuiles() {
        return tuiles.size();
    }

    public int enAttente() {
        return file.size() + enCours.get() + terminees.size();
    }

    public void dessiner(GameCamera camera, boolean biomes) {
        this.biomes = biomes;
        image++;
        preparerVue(camera);
        envoyerTextures();
        file.clear();

        shader.bind();
        shader.setUniformi("u_carte", 0);
        shader.setUniformf("u_centre", largeur / 2f, hauteur / 2f);
        shader.setUniformf("u_rayon", (float) rayon);
        for (int face = 0; face < Cube.FACES; face++) {
            parcourir(obtenir(face, 0, 0, 0));
        }
        oublierVieillesTuiles();
    }

    private void preparerVue(GameCamera camera) {
        double cosLon = Math.cos(camera.longitude), sinLon = Math.sin(camera.longitude);
        double cosLat = Math.cos(camera.latitude), sinLat = Math.sin(camera.latitude);
        est[0] = -sinLon; est[1] = cosLon; est[2] = 0;
        nord[0] = -sinLat * cosLon; nord[1] = -sinLat * sinLon; nord[2] = cosLat;
        centre[0] = cosLat * cosLon; centre[1] = cosLat * sinLon; centre[2] = sinLat;
        rayon = camera.rayonPixels;
        largeur = Gdx.graphics.getBackBufferWidth();
        hauteur = Gdx.graphics.getBackBufferHeight();
    }

    private void envoyerTextures() {
        for (int n = 0; n < ENVOIS_PAR_IMAGE; n++) {
            Tuile tuile = terminees.poll();
            if (tuile == null) return;
            Pixmap pixels = tuile.image;
            tuile.texture = new Texture(pixels);
            tuile.texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            pixels.dispose();
            tuile.image = null;
            tuile.etat.set(Tuile.PRETE);
        }
    }

    private Tuile obtenir(int face, int niveau, int x, int y) {
        long cle = Tuile.cle(face, niveau, x, y, biomes);
        Tuile tuile = tuiles.get(cle);
        if (tuile == null) {
            tuile = new Tuile(face, niveau, x, y, biomes);
            tuiles.put(cle, tuile);
        }
        tuile.dernierUsage = image;
        return tuile;
    }

    private void parcourir(Tuile tuile) {
        projeter(tuile);
        if (!estVisible()) return;
        float taille = Math.max(maxX - minX, maxY - minY);
        demander(tuile);

        if (tuile.niveau < NIVEAU_MAX && taille > TAILLE_DIVISION) {
            Tuile[] enfants = enfants(tuile);
            boolean prets = true;
            for (Tuile enfant : enfants) {
                projeter(enfant);
                if (estVisible() && enfant.etat.get() != Tuile.PRETE) {
                    demander(enfant);
                    prets = false;
                }
            }
            if (prets) {
                for (Tuile enfant : enfants) parcourir(enfant);
                return;
            }
            projeter(tuile);
        }
        if (tuile.etat.get() == Tuile.PRETE) {
            tracer(tuile);
        }
    }

    private Tuile[] enfants(Tuile tuile) {
        int niveau = tuile.niveau + 1, x = tuile.x * 2, y = tuile.y * 2;
        return new Tuile[] {
            obtenir(tuile.face, niveau, x, y), obtenir(tuile.face, niveau, x + 1, y),
            obtenir(tuile.face, niveau, x, y + 1), obtenir(tuile.face, niveau, x + 1, y + 1)
        };
    }

    private void demander(Tuile tuile) {
        if (tuile.etat.get() == Tuile.VIDE) file.add(tuile);
    }

    private void projeter(Tuile tuile) {
        minX = minY = Float.MAX_VALUE;
        maxX = maxY = -Float.MAX_VALUE;
        devant = false;
        double[] g = tuile.grille;
        int n = 0;
        for (int j = 0; j < Tuile.POINTS; j++) {
            for (int i = 0; i < Tuile.POINTS; i++) {
                int p = (j * Tuile.POINTS + i) * 3;
                double x = g[p], y = g[p + 1], z = g[p + 2];
                float ecranX = (float) ((x * est[0] + y * est[1] + z * est[2]) * rayon);
                float ecranY = (float) ((x * nord[0] + y * nord[1] + z * nord[2]) * rayon);
                float profondeur = (float) (x * centre[0] + y * centre[1] + z * centre[2]);

                sommets[n++] = ecranX * 2f / largeur;
                sommets[n++] = ecranY * 2f / hauteur;
                sommets[n++] = (float) i / Tuile.SEGMENTS;
                sommets[n++] = (float) j / Tuile.SEGMENTS;
                sommets[n++] = profondeur;

                if (profondeur > 0f) {
                    devant = true;
                    minX = Math.min(minX, ecranX);
                    maxX = Math.max(maxX, ecranX);
                    minY = Math.min(minY, ecranY);
                    maxY = Math.max(maxY, ecranY);
                }
            }
        }
    }

    private boolean estVisible() {
        return devant && maxX >= -largeur / 2f && minX <= largeur / 2f && maxY >= -hauteur / 2f && minY <= hauteur / 2f;
    }

    private void tracer(Tuile tuile) {
        mesh.setVertices(sommets);
        tuile.texture.bind(0);
        mesh.render(shader, GL20.GL_TRIANGLES);
    }

    private void oublierVieillesTuiles() {
        if (tuiles.size() <= TUILES_MAX) return;
        List<Tuile> liste = new ArrayList<>(tuiles.values());
        liste.sort((a, b) -> Long.compare(a.dernierUsage, b.dernierUsage));
        int aRetirer = tuiles.size() - TUILES_MAX;
        for (Tuile tuile : liste) {
            if (aRetirer == 0 || tuile.dernierUsage == image) break;
            int etat = tuile.etat.get();
            if (etat == Tuile.EN_COURS || etat == Tuile.CALCULEE) continue;
            tuile.etat.set(Tuile.EN_COURS);
            tuile.liberer();
            tuiles.remove(Tuile.cle(tuile.face, tuile.niveau, tuile.x, tuile.y, tuile.biomes));
            aRetirer--;
        }
    }

    public void dispose() {
        for (Thread ouvrier : ouvriers) ouvrier.interrupt();
        for (Tuile tuile : tuiles.values()) tuile.liberer();
        shader.dispose();
        mesh.dispose();
    }
}
