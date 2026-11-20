package com.deldaran.politics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.deldaran.politics.planet.Biome;
import com.deldaran.politics.planet.Terrain;

public class Ciel {
    private static final double RAYON_METRES = Terrain.RAYON_KM * 1000.0;
    private static final double PERIODE = 8192.0;
    private static final float CYCLE = 3600f;

    private final Terrain terrain;
    private final ShaderProgram shader;
    private final SpriteBatch pinceau = new SpriteBatch();
    private final Texture blanc;
    private final Terrain.Point point = new Terrain.Point();
    public final Meteo.Locale locale = new Meteo.Locale();
    public float ecrasement = 1f;
    public double rotation;
    public float nuit;
    private float temps;
    private final double[] soleil = new double[3];
    private float attenteVie;
    private float lucioles, moucherons, pollen, papillons, brume;

    public Ciel(Terrain terrain) {
        this.terrain = terrain;
        shader = new ShaderProgram(Gdx.files.internal("shaders/agrandir.vert"), Gdx.files.internal("shaders/ciel.frag"));
        if (!shader.isCompiled()) {
            throw new IllegalStateException(shader.getLog());
        }
        pinceau.setShader(shader);
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(1f, 1f, 1f, 1f);
        p.fill();
        blanc = new Texture(p);
        p.dispose();
    }

    public void dessiner(GameCamera camera, Meteo meteo, float delta) {
        if (!meteo.pret()) return;
        temps = (temps + delta) % CYCLE;
        int largeur = Gdx.graphics.getBackBufferWidth(), hauteur = Gdx.graphics.getBackBufferHeight();
        double cosLon = Math.cos(camera.longitude), sinLon = Math.sin(camera.longitude);
        double cosLat = Math.cos(camera.latitude), sinLat = Math.sin(camera.latitude);
        double cx = cosLat * cosLon, cy = cosLat * sinLon, cz = sinLat;
        double mpp = RAYON_METRES / camera.rayonPixels;

        meteo.locale(cx, cy, cz, locale);
        attenteVie -= delta;
        if (attenteVie <= 0f) {
            attenteVie = 0.5f;
            vie(cx, cy, cz);
        }
        float calme = 1f - Math.min(1f, locale.pluie * 1.5f + locale.neige * 2f + locale.orage);
        meteo.soleil(soleil);
        double hauteurSoleil = cx * soleil[0] + cy * soleil[1] + cz * soleil[2];
        nuit = (float) lisse(0.1, -0.05, hauteurSoleil);
        float jour = 1f - nuit;

        double art = Math.max(Vegetation.METRES_SPRITE, mpp);
        double pixel = Math.max(art / mpp, 3.0);
        double ventEst = locale.ventEst, ventNord = locale.ventNord;

        pinceau.getProjectionMatrix().setToOrtho2D(0f, 0f, largeur, hauteur);
        pinceau.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
        pinceau.begin();
        meteo.texture().bind(1);
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
        shader.setUniformi("u_meteo", 1);
        shader.setUniformf("u_ecran", largeur, hauteur);
        shader.setUniformf("u_rayon", (float) camera.rayonPixels);
        shader.setUniformf("u_ecrasement", ecrasement);
        double cr = Math.cos(rotation), sr = Math.sin(rotation);
        double[] e = { -sinLon, cosLon, 0.0 }, n = { -sinLat * cosLon, -sinLat * sinLon, cosLat };
        shader.setUniformf("u_est", (float) (cr * e[0] - sr * n[0]), (float) (cr * e[1] - sr * n[1]), (float) (cr * e[2] - sr * n[2]));
        shader.setUniformf("u_nord", (float) (sr * e[0] + cr * n[0]), (float) (sr * e[1] + cr * n[1]), (float) (sr * e[2] + cr * n[2]));
        shader.setUniformf("u_rotation", (float) cr, (float) sr);
        shader.setUniformf("u_centre", (float) cx, (float) cy, (float) cz);
        shader.setUniformf("u_pixel", (float) pixel);
        shader.setUniformf("u_mpp", (float) mpp);
        shader.setUniformf("u_art", (float) art);
        shader.setUniformf("u_decalage", (float) modulo(camera.glissementX * RAYON_METRES), (float) modulo(camera.glissementY * RAYON_METRES));
        shader.setUniformf("u_temps", temps);
        shader.setUniformf("u_loin", (float) lisse(30.0, 300.0, mpp));
        shader.setUniformf("u_local", (float) (1.0 - lisse(0.6, 3.0, mpp)));
        shader.setUniformf("u_nuage", locale.nuage);
        shader.setUniformf("u_pluie", locale.pluie);
        shader.setUniformf("u_orage", locale.orage);
        shader.setUniformf("u_neige", locale.neige);
        shader.setUniformf("u_brouillard", locale.brouillard);
        shader.setUniformf("u_brume", brume);
        shader.setUniformf("u_vent", (float) ventEst, (float) ventNord);
        shader.setUniformf("u_lucioles", lucioles * calme * (0.1f + 0.9f * nuit));
        shader.setUniformf("u_moucherons", moucherons * calme);
        shader.setUniformf("u_pollen", pollen * calme * jour);
        shader.setUniformf("u_papillons", papillons * calme * jour);
        shader.setUniformf("u_soleil", (float) soleil[0], (float) soleil[1], (float) soleil[2]);
        pinceau.draw(blanc, 0f, 0f, largeur, hauteur);
        pinceau.end();
        pinceau.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void vie(double x, double y, double z) {
        terrain.echantillonner(x, y, z, 11, point);
        byte biome = terrain.biome(x, y, z, point);
        boolean terre = point.altitude >= 0f;
        float chaud = Math.max(0f, Math.min(1f, (point.temperature - 4f) / 10f));
        lucioles = moucherons = pollen = papillons = 0f;
        brume = 0.06f;
        if (!terre) return;
        switch (biome) {
            case Biome.FORET: lucioles = 0.6f; moucherons = 0.4f; pollen = 0.3f; papillons = 0.2f; brume = 0.25f; break;
            case Biome.JUNGLE: lucioles = 1f; moucherons = 0.7f; pollen = 0.2f; papillons = 0.5f; brume = 0.4f; break;
            case Biome.TAIGA: lucioles = 0.3f; moucherons = 0.5f; pollen = 0.15f; brume = 0.3f; break;
            case Biome.PRAIRIE: lucioles = 0.1f; moucherons = 0.2f; pollen = 0.8f; papillons = 0.6f; break;
            case Biome.SAVANE: moucherons = 0.3f; pollen = 0.5f; papillons = 0.4f; break;
            case Biome.TOUNDRA: case Biome.MONTAGNE: pollen = 0.2f; brume = 0.15f; break;
            default: break;
        }
        if (point.riviere || point.lac) moucherons = Math.min(1f, moucherons + 0.4f);
        lucioles *= chaud;
        moucherons *= chaud;
        papillons *= chaud;
    }

    private static double modulo(double v) {
        return v - Math.floor(v / PERIODE) * PERIODE;
    }

    private static double lisse(double a, double b, double x) {
        double t = Math.max(0.0, Math.min(1.0, (x - a) / (b - a)));
        return t * t * (3.0 - 2.0 * t);
    }

    public void dispose() {
        shader.dispose();
        pinceau.dispose();
        blanc.dispose();
    }
}
