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
    public static final double ANGLE_CAMERA = Math.toRadians(30.0);
    private static final double INCLINAISON = Math.cos(ANGLE_CAMERA) / Math.sin(ANGLE_CAMERA);
    public static final double ECRASEMENT = Math.sin(ANGLE_CAMERA);
    public static final double ROTATION = Math.toRadians(45.0);
    private static final int MARGE = 1024;
    private static final int PAS_MAILLAGE = 2;
    private static final double RELIEF_DEBUT = 2.0;
    private static final double RELIEF_PLEIN = 1.2;


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
    public static final double PIXEL_MIN = 1.9;
    private final ShaderProgram agrandir;
    private final ShaderProgram relief;
    private final SpriteBatch couvreur = new SpriteBatch();
    private final ShaderProgram surface;
    private FrameBuffer cadreSurface;
    private final Terrain.Point point = new Terrain.Point();
    private FrameBuffer cadreHauteurs, vue;
    private Mesh bande;
    private int rangeesBande, largeurVue, hauteurVue;
    private int marge;
    private double ecrasement = 1.0;
    private double fondu, rotation, cosR = 1.0, sinR = 0.0;
    private double reliefVue, referenceMetres, fractionReference;
    private final double[] estVue = new double[3];
    private final double[] nordVue = new double[3];
    private final List<Tuile> aTracer = new ArrayList<>();
    private final double[] sonde = new double[3];

    private final float[] sommets = new float[Tuile.POINTS * Tuile.POINTS * FLOTTANTS_PAR_POINT];

    private final double[] est = new double[3];
    private final double[] nord = new double[3];
    private final double[] centre = new double[3];
    private final float[] micro = new float[9];
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
    public float mouille, neigeSol;
    public Texture traces;
    public float traceX, traceY;
    public Rues rues;

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
        relief = new ShaderProgram(Gdx.files.internal("shaders/relief.vert"), Gdx.files.internal("shaders/relief.frag"));
        if (!relief.isCompiled()) {
            throw new IllegalStateException(relief.getLog());
        }
        surface = new ShaderProgram(Gdx.files.internal("shaders/agrandir.vert"), Gdx.files.internal("shaders/surface.frag"));
        if (!surface.isCompiled()) {
            throw new IllegalStateException(surface.getLog());
        }
        couvreur.setShader(surface);
        couvreur.disableBlending();

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

        if (traces != null) traces.bind(3);
        if (rues != null) {
            rues.texture().bind(4);
            rues.sol().bind(5);
        }
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
        MicroRelief.base(centre[0], centre[1], centre[2], micro);
        shader.setUniformf("u_microFrac", micro[0], micro[1], micro[2]);
        shader.setUniformf("u_microBase", micro[3], micro[4], micro[5]);
        shader.setUniformf("u_microBase2", micro[6], micro[7], micro[8]);
        shader.setUniformf("u_est", (float) estVue[0], (float) estVue[1], (float) estVue[2]);
        shader.setUniformf("u_nord", (float) nordVue[0], (float) nordVue[1], (float) nordVue[2]);
        shader.setUniformf("u_rotation", (float) cosR, (float) sinR);
        shader.setUniformf("u_mode", 0f);
        shader.setUniformf("u_referenceMetres", (float) referenceMetres);
        shader.setUniformf("u_biomes", biomes ? 1f : 0f);
        shader.setUniformf("u_niveau", niveauGrain % 289);
        shader.setUniformf("u_densite", (float) (Math.pow(2.0, niveauGrain) / (Math.PI / 2.0)));
        shader.setUniformf("u_plat", (float) Math.max(0.0, Math.min(1.0, (rayon - RAYON_SPHERE) / (RAYON_PLAT - RAYON_SPHERE))));

        shader.setUniformf("u_fondu", (float) (Math.log(echelle / PIXEL_MIN) / Math.log(2.0)));
        shader.setUniformf("u_temps", (float) temps);
        shader.setUniformf("u_maree", (float) (MAREE * Math.sin(2.0 * Math.PI * temps / PERIODE_MAREE)));
        shader.setUniformf("u_mouille", mouille);
        shader.setUniformf("u_neigeSol", neigeSol);
        shader.setUniformi("u_traces", 3);
        shader.setUniformf("u_traceCamera", traceX, traceY);
        shader.setUniformi("u_rues", 4);
        shader.setUniformi("u_rueSol", 5);
        shader.setUniformf("u_rueActif", rues != null && rues.actif ? 1f : 0f);
        if (rues != null) {
            shader.setUniformf("u_rueCamera", rues.cameraX, rues.cameraY);
            shader.setUniformf("u_rueTexel", rues.texel());
            shader.setUniformf("u_rueBase", rues.base);
        }

        aTracer.clear();
        for (int face = 0; face < Cube.FACES; face++) {
            parcourir(obtenir(face, 0, 0, 0));
        }
        cadre.end();
        if (fondu > 0.0) {
            cadreHauteurs.begin();
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            Gdx.gl.glViewport(0, 0, largeurCadre, hauteurCadre);
            shader.bind();
            shader.setUniformf("u_mode", 1f);
            for (Tuile tuile : aTracer) {
                projeter(tuile);
                mesh.setVertices(sommets);
                tuile.donnees.bind(0);
                tuile.couverture.bind(1);
                mesh.render(shader, GL20.GL_TRIANGLES);
            }
            cadreHauteurs.end();
            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
        }
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
        oublierVieillesTuiles();

        float metresPixel = (float) (Terrain.RAYON_KM * 1000.0 * echelle / rayon);
        Texture sol = cadre.getColorBufferTexture();
        if (traces != null && metresPixel < 0.3f) {
            cadreSurface.begin();
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            Gdx.gl.glViewport(0, 0, largeurCadre, hauteurCadre);
            traces.bind(1);
            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
            couvreur.getProjectionMatrix().setToOrtho2D(0f, 0f, largeurCadre, hauteurCadre);
            couvreur.begin();
            surface.setUniformi("u_traces", 1);
            surface.setUniformf("u_taille", largeurMax, hauteurMax);
            surface.setUniformf("u_centre", largeurCadre / 2f + glisseX, hauteurCadre / 2f + glisseY);
            surface.setUniformf("u_traceCamera", traceX, traceY);
            surface.setUniformf("u_rotation", (float) cosR, (float) sinR);
            surface.setUniformf("u_metresPixel", metresPixel);
            surface.setUniformf("u_actif", 1f);
            surface.setUniformf("u_mouille", mouille);
            surface.setUniformf("u_neigeSol", neigeSol);
            couvreur.draw(sol, 0f, 0f, largeurCadre, hauteurCadre);
            couvreur.end();
            cadreSurface.end();
            sol = cadreSurface.getColorBufferTexture();
        }

        vue.begin();
        Gdx.gl.glViewport(0, 0, largeurVue, hauteurVue);
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
        Gdx.gl.glClearDepthf(1f);
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        if (fondu > 0.0) Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        else Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthFunc(GL20.GL_LESS);
        Gdx.gl.glDisable(GL20.GL_BLEND);
        cadreHauteurs.getColorBufferTexture().bind(2);
        palette.bind(1);
        sol.bind(0);
        relief.bind();
        relief.setUniformi("u_texture", 0);
        relief.setUniformi("u_palette", 1);
        relief.setUniformi("u_hauteurs", 2);
        relief.setUniformf("u_taille", largeurMax, hauteurMax);
        relief.setUniformf("u_cadre", largeurCadre, hauteurCadre);
        relief.setUniformf("u_vue", largeurVue, hauteurVue);
        relief.setUniformf("u_pas", PAS_MAILLAGE);
        relief.setUniformf("u_ecrase", (float) ecrasement);
        relief.setUniformf("u_relief", fondu > 0.0 ? (float) reliefVue : 0f);
        relief.setUniformf("u_fraction", (float) fractionReference);
        relief.setUniformf("u_metresPixel", metresPixel);
        relief.setUniformf("u_cosinus", (float) Math.cos(ANGLE_CAMERA));
        relief.setUniformf("u_sinus", (float) Math.sin(ANGLE_CAMERA));
        relief.setUniformf("u_camera", cameraX, cameraY);
        relief.setUniformf("u_temps", (float) temps);
        for (int debut = 0; debut < hauteurCadre; debut += rangeesBande * PAS_MAILLAGE) {
            relief.setUniformf("u_bande", debut);
            bande.render(relief, GL20.GL_TRIANGLES);
        }
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        vue.end();

        Texture rendu = vue.getColorBufferTexture();

        pinceau.getProjectionMatrix().setToOrtho2D(0f, 0f, largeur, hauteur);
        pinceau.begin();
        agrandir.setUniformf("u_taille", largeurVue, hauteurVue);
        agrandir.setUniformf("u_echelle", (float) echelle);
        float e = (float) echelle;
        float x0 = largeur / 2f - (largeurVue / 2f + glisseX) * e;
        float k = (float) ecrasement;
        agrandir.setUniformf("u_ecrase", 1f);
        float y0 = hauteur / 2f - (glisseY * k + hauteurVue / 2f + 0.25f + (float) fractionReference) * e;
        pinceau.draw(rendu, x0, y0, largeurVue * e, hauteurVue * e, 0, 0, largeurVue, hauteurVue, false, true);
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
        int hm = 4 * ((int) Math.ceil(hauteur / PIXEL_MIN / ECRASEMENT / 4.0) + 1) + 2 * MARGE;

        if (cadre == null || lm != largeurMax || hm != hauteurMax) {
            if (cadre != null) cadre.dispose();
            if (cadreHauteurs != null) cadreHauteurs.dispose();
            if (cadreSurface != null) cadreSurface.dispose();
            if (vue != null) vue.dispose();
            cadreSurface = new FrameBuffer(Pixmap.Format.RGBA8888, lm, hm, false);
            cadreSurface.getColorBufferTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            cadre = new FrameBuffer(Pixmap.Format.RGBA8888, lm, hm, false);
            cadre.getColorBufferTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            cadreHauteurs = new FrameBuffer(Pixmap.Format.RGBA8888, lm, hm, false);
            cadreHauteurs.getColorBufferTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            largeurVue = lm;
            hauteurVue = 2 * ((int) Math.ceil(hauteur / PIXEL_MIN / 2.0) + 2);
            vue = new FrameBuffer(Pixmap.Format.RGBA8888, largeurVue, hauteurVue, true);
            vue.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            largeurMax = lm;
            hauteurMax = hm;
            creerBande();
        }

        double quart = Math.PI / 2.0;
        niveauGrain = (int) Math.floor(Math.log(quart * rayon / PIXEL_MIN) / Math.log(2.0));
        double cellule = quart / Math.pow(2.0, niveauGrain);
        echelle = cellule * rayon;
        preparerRelief();
        rotation = ROTATION * fondu;
        cosR = Math.cos(rotation);
        sinR = Math.sin(rotation);
        tourner(est, nord, estVue, nordVue);

        largeurCadre = Math.min(largeurMax, 2 * ((int) Math.ceil(largeur / echelle / 2.0) + 1));
        hauteurCadre = Math.min(hauteurMax, 4 * ((int) Math.ceil(hauteur / echelle / ecrasement / 4.0) + 1) + 2 * marge);
        double cx = (cosR * camera.glissementX - sinR * camera.glissementY) / cellule;
        double cy = (sinR * camera.glissementX + cosR * camera.glissementY) / cellule;
        double pasY = fondu >= 1.0 ? 2.0 : 1.0;
        double px = Math.floor(cx), py = Math.floor(cy / pasY) * pasY;
        double gx = px - largeurCadre / 2, gy = py - hauteurCadre / 2;
        glisseX = (float) (cx - px);
        glisseY = (float) (cy - py);
        cameraX = (float) (gx - Math.floor(gx / MODULE) * MODULE);
        cameraY = (float) (gy - Math.floor(gy / MODULE) * MODULE);

    }
    private void preparerRelief() {
        double metresPixel = Terrain.RAYON_KM * 1000.0 * echelle / rayon;
        fondu = Math.max(0.0, Math.min(1.0, (RELIEF_DEBUT - metresPixel) / (RELIEF_DEBUT - RELIEF_PLEIN)));
        if (rayon < RAYON_PLAT) fondu = 0.0;
        marge = fondu > 0.0 ? MARGE : 0;
        ecrasement = 1.0 - (1.0 - ECRASEMENT) * fondu;
        double reference = 0.0;
        if (fondu > 0.0) {
            float altitude = altitudeSol(centre[0], centre[1], centre[2]);
            reference = Math.max(altitude * 1000.0, 0.0) + MicroRelief.hauteur(centre[0], centre[1], centre[2], altitude, terrain.hauteurPlaines);
        }
        if (fondu > 0.0) {
            double portee = (hauteur / echelle / ecrasement / 2.0 + MARGE) * metresPixel;
            double ecart = 0.0;
            for (int j = -3; j <= 3; j++) {
                for (int i = -3; i <= 3; i++) {
                    Personnage.decaler(centre, portee * i / 3.0, portee * j / 3.0, sonde);
                    float altitude = altitudeSol(sonde[0], sonde[1], sonde[2]);
                    double h = Math.max(altitude * 1000.0, 0.0) + MicroRelief.hauteur(sonde[0], sonde[1], sonde[2], altitude, terrain.hauteurPlaines);
                    ecart = Math.max(ecart, Math.abs(h - reference));
                }
            }
            double rangees = (ecart + MicroRelief.AMPLITUDE) * Math.cos(ANGLE_CAMERA) * fondu / ecrasement / metresPixel + 16.0;
            marge = Math.min(MARGE, 4 * (int) Math.ceil(rangees / 4.0));
        }
        reliefVue = Math.cos(ANGLE_CAMERA) * fondu / metresPixel;
        referenceMetres = reference;
        fractionReference = reference * reliefVue - Math.floor(reference * reliefVue + 0.5);
    }

    private void creerBande() {
        if (bande != null) bande.dispose();
        int colonnes = (largeurMax + PAS_MAILLAGE - 1) / PAS_MAILLAGE;
        rangeesBande = Math.min(65535 / (colonnes + 1) - 1, (hauteurMax + PAS_MAILLAGE - 1) / PAS_MAILLAGE);
        int points = (colonnes + 1) * (rangeesBande + 1);
        float[] positions = new float[points * 2];
        int n = 0;
        for (int j = 0; j <= rangeesBande; j++) {
            for (int i = 0; i <= colonnes; i++) {
                positions[n++] = i * PAS_MAILLAGE + 0.5f;
                positions[n++] = j * PAS_MAILLAGE + 0.5f;
            }
        }
        short[] indices = new short[colonnes * rangeesBande * 6];
        n = 0;
        for (int j = 0; j < rangeesBande; j++) {
            for (int i = 0; i < colonnes; i++) {
                int a = j * (colonnes + 1) + i, b = a + 1, c = a + colonnes + 1, d = c + 1;
                indices[n++] = (short) a;
                indices[n++] = (short) b;
                indices[n++] = (short) d;
                indices[n++] = (short) a;
                indices[n++] = (short) d;
                indices[n++] = (short) c;
            }
        }
        bande = new Mesh(true, points, indices.length, new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"));
        bande.setVertices(positions);
        bande.setIndices(indices);
    }

    public double rotation() {
        return rotation;
    }

    public void tourner(double[] est, double[] nord) {
        tourner(est, nord, est, nord);
    }

    private void tourner(double[] est, double[] nord, double[] estSortie, double[] nordSortie) {
        for (int k = 0; k < 3; k++) {
            double e = est[k], n = nord[k];
            estSortie[k] = cosR * e - sinR * n;
            nordSortie[k] = sinR * e + cosR * n;
        }
    }

    public float ecrasement() {
        return (float) ecrasement;
    }

    public double metresPixel() {
        return Terrain.RAYON_KM * 1000.0 * echelle / rayon;
    }

    public float decalage(double x, double y, double z, float altitude) {
        double sol = altitude + MicroRelief.hauteur(x, y, z, altitude, terrain.hauteurPlaines) / 1000.0;
        if (rues != null) sol = rues.aplanir(x, y, z, Math.max(sol * 1000.0, 0.0)) / 1000.0;
        return decalage(sol);
    }

    public double demiVueMetres() {
        return Math.hypot(largeurCadre, hauteurCadre / Math.max(ecrasement, 0.1)) * 0.5 * metresPixel();
    }

    private final Terrain.Point pointSol = new Terrain.Point();
    private final double[] directionSol = new double[3];

    public float altitudeSol(double x, double y, double z) {
        return altitudeSol(terrain, x, y, z, pointSol, directionSol);
    }

    public static float altitudeSol(Terrain terrain, double x, double y, double z, Terrain.Point point, double[] d) {
        int face = Cube.face(x, y, z);
        double cote = (double) (1 << NIVEAU_MAX) * Tuile.COTE;
        double u = (Cube.coordonneeA(face, x, y, z) + 1.0) * 0.5 * cote - 0.5;
        double v = (Cube.coordonneeB(face, x, y, z) + 1.0) * 0.5 * cote - 0.5;
        double iu = Math.floor(u), iv = Math.floor(v);
        double fu = u - iu, fv = v - iv;
        float somme = 0f;
        for (int k = 0; k < 4; k++) {
            int di = k & 1, dj = k >> 1;
            double poids = (di == 0 ? 1.0 - fu : fu) * (dj == 0 ? 1.0 - fv : fv);
            if (poids <= 0.0) continue;
            double a = 2.0 * (iu + di + 0.5) / cote - 1.0, b = 2.0 * (iv + dj + 0.5) / cote - 1.0;
            Cube.direction(face, a, b, d);
            terrain.echantillonner(d[0], d[1], d[2], NIVEAU_MAX, point);
            somme += (float) (poids * point.altitude);
        }
        return somme;
    }

    public float decalage(double altitude) {
        return (float) ((Math.max(altitude * 1000.0, 0.0) - referenceMetres) * reliefVue * echelle);
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
                double ecranX = (x * estVue[0] + y * estVue[1] + z * estVue[2]) * rayon;
                double ecranY = (x * nordVue[0] + y * nordVue[1] + z * nordVue[2]) * rayon;
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
        float bord = (float) (hauteur / 2.0 / ecrasement) + marge * (float) echelle;
        return devant && maxX >= -largeur / 2f && minX <= largeur / 2f && maxY >= -bord && minY <= bord;

    }

    private void tracer(Tuile tuile) {
        aTracer.add(tuile);
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
        if (cadreHauteurs != null) cadreHauteurs.dispose();
        if (vue != null) vue.dispose();
        if (bande != null) bande.dispose();
        if (cadreSurface != null) cadreSurface.dispose();
        relief.dispose();
        surface.dispose();
        couvreur.dispose();

    }
}
