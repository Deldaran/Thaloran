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
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;

public class RenduTuiles {
    public static final int NIVEAU_MAX = 11;

    private static final int FLOTTANTS_PAR_POINT = 8;
    private static final float TAILLE_DIVISION = 384f;
    private static final int ENVOIS_PAR_IMAGE = 8;
    private static final int TUILES_MAX = 500;
    private static final double MODULE = 289.0 * 4096.0;
    private static final double RAYON_SPHERE = 3000.0;
    private static final double RAYON_PLAT = 9000.0;
    private static final double MAREE = 0.5;
    private static final double PERIODE_MAREE = 180.0;
    private static final double CYCLE = 3600.0;


    private final Map<Long, Tuile> tuiles = new HashMap<>();
    private final LinkedBlockingQueue<Tuile> file = new LinkedBlockingQueue<>();
    private final ConcurrentLinkedQueue<Tuile> terminees = new ConcurrentLinkedQueue<>();
    private final AtomicInteger enCours = new AtomicInteger();
    private final List<Thread> ouvriers = new ArrayList<>();
    
    private final Terrain terrain;
    private final ShaderProgram shader;
    private final Mesh mesh;
    private final Texture palette;
    private final SpriteBatch pinceau = new SpriteBatch();
    public static final double PIXEL_MIN = 3.0;
    private final ShaderProgram agrandir;

    private final float[] sommets = new float[Tuile.POINTS * Tuile.POINTS * FLOTTANTS_PAR_POINT];

    private final double[] est = new double[3];
    private final double[] nord = new double[3];
    private final double[] centre = new double[3];
    private double rayon;
    private float largeur, hauteur;
    private long image = 0;
    private boolean biomes;

    private FrameBuffer cadre;
    private int largeurCadre, hauteurCadre;
    private int largeurMax, hauteurMax;
    private double echelle;
    private int niveauGrain;

    private float glisseX, glisseY;
    private float cameraX, cameraY;
    public double temps;

    private float minX, maxX, minY, maxY;
    private boolean devant;

    public RenduTuiles(Terrain terrain) {
        this.terrain = terrain;
        shader = new ShaderProgram(Gdx.files.internal("shaders/tuile.vert"), Gdx.files.internal("shaders/tuile.frag"));
        if (!shader.isCompiled()) {
            throw new IllegalStateException(shader.getLog());
        }
        mesh = new Mesh(false, Tuile.POINTS * Tuile.POINTS, Tuile.SEGMENTS * Tuile.SEGMENTS * 6,
            new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"),
            new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0"),
            new VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_hauteur"),
            new VertexAttribute(VertexAttributes.Usage.Generic, 3, "a_direction"));

        mesh.setIndices(creerIndices());
        palette = new Texture(Gdx.files.internal("palettes/sol.png"));
        palette.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        agrandir = new ShaderProgram(Gdx.files.internal("shaders/agrandir.vert"), Gdx.files.internal("shaders/agrandir.frag"));
        if (!agrandir.isCompiled()) {
            throw new IllegalStateException(agrandir.getLog());
        }
        pinceau.setShader(agrandir);

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
                atelier.generer(tuile);
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
        temps = (temps + Gdx.graphics.getDeltaTime()) % CYCLE;
        image++;
        preparerVue(camera);
        envoyerTextures();
        file.clear();

        cadre.begin();
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glViewport(0, 0, largeurCadre, hauteurCadre);

        palette.bind(2);
        shader.bind();
        shader.setUniformi("u_donnees", 0);
        shader.setUniformi("u_couverture", 1);
        shader.setUniformi("u_palette", 2);
        shader.setUniformf("u_centre", largeurCadre / 2f + glisseX, hauteurCadre / 2f + glisseY);
        shader.setUniformf("u_rayon", (float) (rayon / echelle));
        shader.setUniformf("u_camera", cameraX, cameraY);
        shader.setUniformf("u_metresPixel", (float) (Terrain.RAYON_KM * 1000.0 * echelle / rayon));
        shader.setUniformf("u_plaines", terrain.hauteurPlaines);
        shader.setUniformf("u_biomes", biomes ? 1f : 0f);
        shader.setUniformf("u_niveau", niveauGrain % 289);
        shader.setUniformf("u_densite", (float) (Math.pow(2.0, niveauGrain) / (Math.PI / 2.0)));
        shader.setUniformf("u_plat", (float) Math.max(0.0, Math.min(1.0, (rayon - RAYON_SPHERE) / (RAYON_PLAT - RAYON_SPHERE))));

        shader.setUniformf("u_fondu", (float) (Math.log(echelle / PIXEL_MIN) / Math.log(2.0)));
        shader.setUniformf("u_temps", (float) temps);
        shader.setUniformf("u_maree", (float) (MAREE * Math.sin(2.0 * Math.PI * temps / PERIODE_MAREE)));

        for (int face = 0; face < Cube.FACES; face++) {
            parcourir(obtenir(face, 0, 0, 0));
        }
        cadre.end();
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
        oublierVieillesTuiles();

        Texture rendu = cadre.getColorBufferTexture();
        pinceau.getProjectionMatrix().setToOrtho2D(0f, 0f, largeur, hauteur);
        pinceau.begin();
        agrandir.setUniformf("u_taille", largeurMax, hauteurMax);
        agrandir.setUniformf("u_echelle", (float) echelle);
        float e = (float) echelle;
        float x0 = largeur / 2f - (largeurCadre / 2f + glisseX) * e;
        float y0 = hauteur / 2f - (hauteurCadre / 2f + glisseY) * e;
        pinceau.draw(rendu, x0, y0, largeurCadre * e, hauteurCadre * e, 0, 0, largeurCadre, hauteurCadre, false, true);
        pinceau.end();

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
        
        int lm = 2 * ((int) Math.ceil(largeur / PIXEL_MIN / 2.0) + 1);
        int hm = 2 * ((int) Math.ceil(hauteur / PIXEL_MIN / 2.0) + 1);
        if (cadre == null || lm != largeurMax || hm != hauteurMax) {
            if (cadre != null) cadre.dispose();
            cadre = new FrameBuffer(Pixmap.Format.RGBA8888, lm, hm, false);
            cadre.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            largeurMax = lm;
            hauteurMax = hm;
        }
        double quart = Math.PI / 2.0;
        niveauGrain = (int) Math.floor(Math.log(quart * rayon / PIXEL_MIN) / Math.log(2.0));
        double cellule = quart / Math.pow(2.0, niveauGrain);
        echelle = cellule * rayon;
        largeurCadre = Math.min(largeurMax, 2 * ((int) Math.ceil(largeur / echelle / 2.0) + 1));
        hauteurCadre = Math.min(hauteurMax, 2 * ((int) Math.ceil(hauteur / echelle / 2.0) + 1));
        double cx = camera.glissementX / cellule;
        double cy = camera.glissementY / cellule;
        double px = Math.floor(cx), py = Math.floor(cy);
        double gx = px - largeurCadre / 2, gy = py - hauteurCadre / 2;
        glisseX = (float) (cx - px);
        glisseY = (float) (cy - py);
        cameraX = (float) (gx - Math.floor(gx / MODULE) * MODULE);
        cameraY = (float) (gy - Math.floor(gy / MODULE) * MODULE);

    }

    private void envoyerTextures() {
        for (int n = 0; n < ENVOIS_PAR_IMAGE; n++) {
            Tuile tuile = terminees.poll();
            if (tuile == null) return;
            tuile.donnees = new Texture(tuile.donneesPretes);
            tuile.donnees.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            tuile.donnees.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
            tuile.couverture = new Texture(tuile.couverturePrete);
            tuile.couverture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            tuile.couverture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
            tuile.couverturePrete.dispose();
            tuile.couverturePrete = null;
            tuile.donneesPretes = null;
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
                double ecranX = (x * est[0] + y * est[1] + z * est[2]) * rayon;
                double ecranY = (x * nord[0] + y * nord[1] + z * nord[2]) * rayon;
                float profondeur = (float) (x * centre[0] + y * centre[1] + z * centre[2]);

                sommets[n++] = (float) ((ecranX / echelle + glisseX) * 2.0 / largeurCadre);
                sommets[n++] = (float) ((ecranY / echelle + glisseY) * 2.0 / hauteurCadre);
                sommets[n++] = (float) i / Tuile.SEGMENTS;
                sommets[n++] = (float) j / Tuile.SEGMENTS;
                sommets[n++] = profondeur;
                sommets[n++] = (float) x;
                sommets[n++] = (float) y;
                sommets[n++] = (float) z;


                if (profondeur > 0f) {
                    devant = true;
                    minX = Math.min(minX, (float) ecranX);
                    maxX = Math.max(maxX, (float) ecranX);
                    minY = Math.min(minY, (float) ecranY);
                    maxY = Math.max(maxY, (float) ecranY);
                }
            }
        }
    }

    private boolean estVisible() {
        return devant && maxX >= -largeur / 2f && minX <= largeur / 2f && maxY >= -hauteur / 2f && minY <= hauteur / 2f;
    }

    private void tracer(Tuile tuile) {
        mesh.setVertices(sommets);
            tuile.donnees.bind(0);
        tuile.couverture.bind(1);
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
        palette.dispose();
        pinceau.dispose();
        agrandir.dispose();
        if (cadre != null) cadre.dispose();

    }
}
