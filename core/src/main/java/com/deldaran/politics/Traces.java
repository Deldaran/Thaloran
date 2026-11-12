package com.deldaran.politics;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.deldaran.politics.planet.Terrain;
import java.nio.ByteBuffer;
import java.util.List;

public class Traces {
    public static final int TAILLE = 512;
    public static final double TEXEL = 0.09;
    private static final double RECENTRER = 12.0;
    private static final double RAYON_METRES = Terrain.RAYON_KM * 1000.0;
    private static final double ECART_PIEDS = 0.11;
    private static final float TASSEMENT = 0.05f;
    private static final float OUBLI_EMPREINTES = 0.985f;
    private static final float OUBLI_TASSEMENT = 0.9995f;
    private static final float OUBLI_CREUX = 0.9997f;
    private static final double RAYON_SILLON = 0.22;
    private static final float CREUSEMENT = 0.5f;

    private float[] tassement = new float[TAILLE * TAILLE];
    private float[] empreintes = new float[TAILLE * TAILLE];
    private float[] creux = new float[TAILLE * TAILLE];
    public float mollesse = 1f, comblement;
    private final Pixmap image = new Pixmap(TAILLE, TAILLE, Pixmap.Format.RGBA8888);
    private final Texture texture;
    private final double[] origine = new double[3], est = new double[3], nord = new double[3];
    private final double[] metres = new double[2];
    private boolean placee, modifiee;
    private float attenteOubli, attenteEnvoi;
    public float cameraX, cameraY;
    public Villages villages;

    public Traces() {
        texture = new Texture(image);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    public Texture texture() {
        return texture;
    }

    public void effacer() {
        java.util.Arrays.fill(tassement, 0f);
        java.util.Arrays.fill(empreintes, 0f);
        java.util.Arrays.fill(creux, 0f);
        placee = false;
        modifiee = true;
    }

    public void mettreAJour(GameCamera camera, List<Personnage> personnages, float delta) {
        double cosLat = Math.cos(camera.latitude);
        double cx = cosLat * Math.cos(camera.longitude), cy = cosLat * Math.sin(camera.longitude), cz = Math.sin(camera.latitude);
        if (!placee) {
            placer(cx, cy, cz);
            placee = true;
        }
        versMetres(cx, cy, cz);
        if (Math.abs(metres[0]) > RECENTRER || Math.abs(metres[1]) > RECENTRER) {
            decaler((int) Math.round(metres[0] / TEXEL), (int) Math.round(metres[1] / TEXEL));
            versMetres(cx, cy, cz);
        }
        cameraX = (float) metres[0];
        cameraY = (float) metres[1];

        for (Personnage p : personnages) {
            if (!p.enMarche || p.profondeur > 0f) continue;
            if (villages != null && villages.surRue(p.position[0], p.position[1], p.position[2])) continue;
            versMetres(p.position[0], p.position[1], p.position[2]);
            double mx = metres[0], my = metres[1];
            tasser(mx, my, delta);
            creuser(mx, my, delta);
            int pas = p.pas();
            if (pas != p.dernierPas) {
                p.dernierPas = pas;
                double cote = (pas % 2 == 0 ? ECART_PIEDS : -ECART_PIEDS);
                empreinte(mx - p.dirNord * cote, my + p.dirEst * cote, p.dirEst, p.dirNord);
            }
        }

        attenteOubli -= delta;
        if (attenteOubli <= 0f) {
            attenteOubli = 1f;
            float oubliCreux = Math.max(0.9f, OUBLI_CREUX - comblement * 0.005f);
            for (int k = 0; k < TAILLE * TAILLE; k++) {
                empreintes[k] *= OUBLI_EMPREINTES;
                tassement[k] *= OUBLI_TASSEMENT;
                creux[k] *= oubliCreux;
            }
            modifiee = true;
        }
        attenteEnvoi -= delta;
        if (modifiee && attenteEnvoi <= 0f) {
            attenteEnvoi = 0.1f;
            modifiee = false;
            envoyer();
        }
    }

    private void placer(double x, double y, double z) {
        origine[0] = x;
        origine[1] = y;
        origine[2] = z;
        double n = Math.sqrt(x * x + y * y);
        est[0] = -y / n;
        est[1] = x / n;
        est[2] = 0.0;
        nord[0] = -z * est[1];
        nord[1] = z * est[0];
        nord[2] = x * est[1] - y * est[0];
    }

    private void versMetres(double x, double y, double z) {
        metres[0] = (x * est[0] + y * est[1] + z * est[2]) * RAYON_METRES;
        metres[1] = (x * nord[0] + y * nord[1] + z * nord[2]) * RAYON_METRES;
    }

    private void decaler(int dx, int dy) {
        float[] t = new float[TAILLE * TAILLE], e = new float[TAILLE * TAILLE], c = new float[TAILLE * TAILLE];
        for (int j = 0; j < TAILLE; j++) {
            int sj = j + dy;
            if (sj < 0 || sj >= TAILLE) continue;
            for (int i = 0; i < TAILLE; i++) {
                int si = i + dx;
                if (si < 0 || si >= TAILLE) continue;
                t[j * TAILLE + i] = tassement[sj * TAILLE + si];
                e[j * TAILLE + i] = empreintes[sj * TAILLE + si];
                c[j * TAILLE + i] = creux[sj * TAILLE + si];
            }
        }
        tassement = t;
        empreintes = e;
        creux = c;
        double ax = dx * TEXEL / RAYON_METRES, ay = dy * TEXEL / RAYON_METRES;
        double x = origine[0] + est[0] * ax + nord[0] * ay;
        double y = origine[1] + est[1] * ax + nord[1] * ay;
        double z = origine[2] + est[2] * ax + nord[2] * ay;
        double l = Math.sqrt(x * x + y * y + z * z);
        placer(x / l, y / l, z / l);
        modifiee = true;
    }

    private int indice(double mx, double my) {
        int i = (int) Math.floor(mx / TEXEL) + TAILLE / 2, j = (int) Math.floor(my / TEXEL) + TAILLE / 2;
        if (i < 0 || j < 0 || i >= TAILLE || j >= TAILLE) return -1;
        return j * TAILLE + i;
    }

    private void tasser(double mx, double my, float delta) {
        for (int dj = -2; dj <= 2; dj++) {
            for (int di = -2; di <= 2; di++) {
                if (di * di + dj * dj > 5) continue;
                int k = indice(mx + di * TEXEL, my + dj * TEXEL);
                if (k >= 0) tassement[k] = Math.min(1f, tassement[k] + TASSEMENT * delta);
            }
        }
        modifiee = true;
    }

    private void creuser(double mx, double my, float delta) {
        int r = (int) Math.ceil(RAYON_SILLON / TEXEL);
        float quantite = CREUSEMENT * mollesse * delta;
        for (int dj = -r; dj <= r; dj++) {
            for (int di = -r; di <= r; di++) {
                double d = Math.hypot(di, dj) * TEXEL / RAYON_SILLON;
                if (d >= 1.0) continue;
                int k = indice(mx + di * TEXEL, my + dj * TEXEL);
                if (k >= 0) creux[k] = Math.min(1f, creux[k] + quantite * (float) (1.0 - d * d));
            }
        }
    }

    private void empreinte(double mx, double my, double dirEst, double dirNord) {
        for (int l = -1; l <= 1; l++) {
            int k = indice(mx + dirEst * l * TEXEL * 0.9, my + dirNord * l * TEXEL * 0.9);
            if (k >= 0) empreintes[k] = 1f;
        }
        modifiee = true;
    }

    private void envoyer() {
        ByteBuffer pixels = image.getPixels();
        pixels.position(0);
        for (int j = 0; j < TAILLE; j++) {
            for (int i = 0; i < TAILLE; i++) {
                int k = j * TAILLE + i;
                pixels.put((byte) (int) (tassement[k] * 255f));
                pixels.put((byte) (int) (empreintes[k] * 255f));
                pixels.put((byte) (int) (creux[k] * 255f));
                pixels.put((byte) 255);
            }
        }
        pixels.position(0);
        texture.draw(image, 0, 0);
    }

    public void dispose() {
        image.dispose();
        texture.dispose();
    }
}
