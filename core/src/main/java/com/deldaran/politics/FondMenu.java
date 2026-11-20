package com.deldaran.politics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.Random;

/**
 * Lot K1 : le fond du menu, une scène en pixel art peinte au lancement avec les couleurs de la palette du jeu (palettes/sol.png) :
 * un ciel de couchant, des montagnes aux crêtes taillées, un château lointain sur son piton, des ruines et des tombes, une arche
 * de pierre en ruine, un chemin de dalles, deux grands arbres pourpres qui encadrent la vue. Les dégradés passent d'une couleur
 * de la palette à la suivante par un tramage ordonné (Bayer 4 × 4), comme on le fait à la main.
 * Par-dessus, ce qui bouge : des rayons de lumière, une brume, des pétales qui tombent, des lucioles, les fenêtres du château.
 */
public final class FondMenu {
    /** La scène fait L × H pixels « d'art », étirés à l'écran sans lissage. */
    public static final int L = 480, H = 270;
    private static final int HORIZON = 156;
    private static final int SOLEIL_X = 238, SOLEIL_Y = 120;
    private static final int[] BAYER = { 0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5 };
    private static final int PETALES = 70, LUCIOLES = 28;

    private final Pixmap palette;
    private final Pixmap art = new Pixmap(L, H, Pixmap.Format.RGBA8888);
    private final Texture scene, rayons, brume, blanc;
    private final Random r = new Random(1337L);
    private final int[] fenetres = new int[12];
    private int nFenetres;
    private final float[] px = new float[PETALES], py = new float[PETALES], pv = new float[PETALES], pp = new float[PETALES];
    private final float[] lx = new float[LUCIOLES], ly = new float[LUCIOLES], lp = new float[LUCIOLES];
    private float temps;

    public FondMenu() {
        palette = new Pixmap(Gdx.files.internal("palettes/sol.png"));
        peindre();
        if (System.getenv("SORTIE") != null) com.badlogic.gdx.graphics.PixmapIO.writePNG(Gdx.files.absolute(System.getenv("SORTIE") + "/fond_menu.png"), art);
        scene = new Texture(art);
        scene.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        rayons = rayons();
        brume = brume();
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fill();
        blanc = new Texture(p);
        p.dispose();
        for (int k = 0; k < PETALES; k++) petale(k, true);
        for (int k = 0; k < LUCIOLES; k++) {
            lx[k] = 120 + r.nextFloat() * 260;
            ly[k] = 175 + r.nextFloat() * 80;
            lp[k] = r.nextFloat() * 6.28f;
        }
    }

    // ---------------------------------------------------------------- outils de peinture

    /** Une couleur de la palette (rangée, colonne 0 à 7). */
    private int c(int rangee, int colonne) {
        return palette.getPixel(Math.max(0, Math.min(7, colonne)), rangee) | 0xFF;
    }

    private static float bayer(int x, int y) {
        return (BAYER[(y & 3) * 4 + (x & 3)] + 0.5f) / 16f;
    }

    /** Entre deux couleurs, la proportion t, par tramage. */
    private static int trame(int a, int b, float t, int x, int y) {
        return t > bayer(x, y) ? b : a;
    }

    /** Le long d'une rampe de couleurs, à la position t (0 à 1), tramée. */
    private static int rampe(int[] r, float t, int x, int y) {
        float f = Math.max(0f, Math.min(1f, t)) * (r.length - 1);
        int i = Math.min(r.length - 2, (int) f);
        return trame(r[i], r[i + 1], f - i, x, y);
    }

    /** Comme rampe, mais en aplats : on ne trame qu'au milieu d'une transition (comme un pixel artiste). */
    private static int nette(int[] r, float t, int x, int y) {
        float f = Math.max(0f, Math.min(1f, t)) * (r.length - 1);
        int i = Math.min(r.length - 2, (int) f);
        float fr = f - i;
        if (fr < 0.38f) return r[i];
        if (fr > 0.62f) return r[i + 1];
        return ((x + y) & 1) == 0 ? r[i] : r[i + 1];
    }

    private void point(int x, int y, int couleur) {
        if (x >= 0 && y >= 0 && x < L && y < H) art.drawPixel(x, y, couleur);
    }

    private static float bruit(float[] g, float x) {
        int i = (int) Math.floor(x);
        float f = x - i;
        f = f * f * (3f - 2f * f);
        int n = g.length;
        return g[Math.floorMod(i, n)] + (g[Math.floorMod(i + 1, n)] - g[Math.floorMod(i, n)]) * f;
    }

    private float[] grille(int n) {
        float[] g = new float[n];
        for (int k = 0; k < n; k++) g[k] = r.nextFloat();
        return g;
    }

    /** Une crête taillée par déplacement du point milieu (des pics, pas des bosses). */
    private float[] crete(float gauche, float droite, float ampleur, float rugosite, int facette, float min, float max) {
        int n = 512;
        float[] h = new float[n + 1];
        h[0] = gauche;
        h[n] = droite;
        float a = ampleur;
        // On s'arrête aux segments de « facette » points : les pentes restent droites, comme taillées.
        for (int pas = n; pas > facette; pas /= 2) {
            for (int i = pas / 2; i < n; i += pas) h[i] = (h[i - pas / 2] + h[i + pas / 2]) * 0.5f + (r.nextFloat() - 0.5f) * a;
            a *= rugosite;
        }
        for (int i = 0; i < n; i++) {
            int a0 = (i / facette) * facette, a1 = Math.min(n, a0 + facette);
            if (i == a0) continue;
            h[i] = h[a0] + (h[a1] - h[a0]) * (i - a0) / (float) (a1 - a0);
        }
        float[] l = new float[L];
        for (int x = 0; x < L; x++) {
            float v = h[x * n / L];
            // Un col où le soleil se couche.
            float col = Math.min(1f, Math.abs(x - SOLEIL_X) / 120f);
            v *= 0.35f + 0.65f * col;
            l[x] = Math.max(min, Math.min(max, v));
        }
        return l;
    }

    // ---------------------------------------------------------------- la scène

    private void peindre() {
        art.setBlending(Pixmap.Blending.None);
        ciel();
        montagnes(crete(78, 64, 110, 0.55f, 16, 14f, 92f), HORIZON + 2, new int[] { c(19, 4), c(19, 5), c(19, 6), c(17, 6), c(17, 7) }, c(19, 7), 0.7f);
        montagnes(crete(46, 40, 80, 0.5f, 8, 6f, 58f), HORIZON + 4, new int[] { c(19, 2), c(19, 3), c(19, 4), c(17, 4), c(17, 5) }, c(19, 5), 0.8f);
        chateau(SOLEIL_X - 6, 58);
        collines();
        ruines();
        chemin();
        arche(318, 404);
        arbre(34, true);
        arbre(462, false);
        buissons();
        palette.dispose();
    }

    private void ciel() {
        int[] rampe = { c(20, 1), c(19, 2), c(19, 3), c(19, 4), c(19, 5), c(17, 5), c(17, 6), c(17, 7), c(16, 6), c(16, 7) };
        float[] g = grille(64), g2 = grille(64);
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < L; x++) {
                float t = (float) Math.pow(y / (float) HORIZON, 1.1);
                float dx = (x - SOLEIL_X) / 170f, dy = (y - SOLEIL_Y) / 80f;
                float lueur = Math.max(0f, 1f - (float) Math.sqrt(dx * dx + dy * dy));
                t = t * 0.78f + lueur * lueur * 0.45f;
                // Des nuages en longues bandes, éclairés par-dessous.
                float n = bruit(g, x / 38f + y * 0.03f) * 0.6f + bruit(g2, x / 13f - y * 0.05f) * 0.4f;
                float bande = (float) Math.sin(y / 6.5f + n * 2.2f);
                if (y < HORIZON - 10 && y > 20 && bande > 0.72f && n > 0.45f) t += bande > 0.9f ? 0.12f : -0.08f;
                point(x, y, rampe(rampe, t, x, y));
            }
        }
        // Le soleil, bas, à demi caché par les montagnes.
        for (int y = SOLEIL_Y - 14; y <= SOLEIL_Y + 14; y++) {
            for (int x = SOLEIL_X - 14; x <= SOLEIL_X + 14; x++) {
                float d = (float) Math.hypot(x - SOLEIL_X, y - SOLEIL_Y);
                if (d < 12.5f) point(x, y, d < 10f ? c(16, 7) : trame(c(16, 6), c(16, 7), 0.5f, x, y));
            }
        }
    }

    /** Un rang de montagnes. Les faces tournées vers le soleil sont claires ; la neige tient sur les plus hauts sommets. */
    private void montagnes(float[] h, int base, int[] tons, int neige, float seuilNeige) {
        float max = 0f;
        for (float v : h) max = Math.max(max, v);
        for (int x = 0; x < L; x++) {
            int haut = base - Math.round(h[x]);
            for (int y = Math.max(0, haut); y < base + 6 && y < H; y++) {
                float d = y - haut;
                // La pente qu'on regarde descend en diagonale depuis la crête : les faces font des bandes obliques.
                float pente0 = h[Math.min(L - 1, x + 1)] - h[Math.max(0, x - 1)];
                int xs = Math.max(1, Math.min(L - 2, x + Math.round(d * 0.7f * Math.signum(-pente0))));
                float pente = h[xs + 1] - h[xs - 1];
                boolean eclaire = x < SOLEIL_X ? pente < 0f : pente > 0f;
                float t = (eclaire ? 0.8f : 0.3f) - d / 70f;
                int couleur = rampe(tons, t, x, y);
                if (h[x] > max * seuilNeige + 30f && d < 4f + (h[x] - max * seuilNeige - 30f) * 0.12f && eclaire) couleur = neige;
                point(x, y, couleur);
            }
        }
    }

    /** Le château, sur un piton de roche, contre le ciel le plus clair. */
    private void chateau(int cx, int hauteur) {
        int sombre = c(19, 1), mur = c(19, 2), clair = c(17, 4), feu = c(16, 6);
        int pied = HORIZON + 6, sommet = pied - hauteur;
        // Le piton.
        float[] g = grille(32);
        for (int y = sommet; y < pied; y++) {
            float t = (y - sommet) / (float) hauteur;
            int gauche = Math.round(10 + t * t * t * 26 + bruit(g, y / 4f) * 4f), droite = Math.round(10 + t * t * 20 + bruit(g, y / 3f + 9f) * 4f);
            for (int x = cx - gauche; x <= cx + droite; x++) {
                int couleur = x > cx + droite - 2 ? clair : x < cx - gauche / 2 ? sombre : mur;
                if (((x * 5 + y * 3) % 17) == 0) couleur = sombre;
                point(x, y, couleur);
            }
        }
        // Les tours : largeur, hauteur, décalage.
        int[][] tours = { { -14, 26, 7 }, { -4, 44, 8 }, { 6, 32, 7 }, { 15, 22, 6 } };
        for (int[] t : tours) {
            int x0 = cx + t[0], h = t[1], l = t[2], y0 = sommet - h;
            for (int y = y0; y < sommet + 3; y++) for (int x = x0; x < x0 + l; x++) point(x, y, x == x0 + l - 1 ? clair : mur);
            // Un toit pointu.
            for (int k = 0; k <= l / 2 + 2; k++) for (int x = x0 - 1 + k; x <= x0 + l - k; x++) point(x, y0 - k, x > x0 + l / 2 ? clair : sombre);
            point(x0 + l / 2, y0 - l / 2 - 4, sombre);
            point(x0 + l / 2, y0 - l / 2 - 5, sombre);
            if (nFenetres < fenetres.length) {
                int fy = y0 + 6 + r.nextInt(Math.max(1, h - 14));
                fenetres[nFenetres++] = (x0 + l / 2) | fy << 16;
                point(x0 + l / 2, fy, feu);
            }
        }
        // Une muraille entre les tours.
        for (int y = sommet - 10; y < sommet + 2; y++) for (int x = cx - 14; x < cx + 20; x++) point(x, y, y == sommet - 10 && x % 2 == 0 ? c(19, 3) : mur);
    }

    /** Les collines du milieu, sombres, avec des touffes d'herbe et une brume au pied des montagnes. */
    private void collines() {
        float[] g = grille(40);
        int[] tons = { c(20, 0), c(10, 1), c(19, 1), c(19, 2), c(19, 3), c(17, 4) };
        for (int x = 0; x < L; x++) {
            int haut = HORIZON + Math.round(bruit(g, x / 45f) * 12f);
            for (int y = haut; y < H; y++) {
                float t = Math.max(0.18f, 0.62f - (y - haut) / 150f) + (bruit(g, x / 6f + y * 0.7f) - 0.5f) * 0.12f;
                if (y - haut < 2) t = 0.9f;
                point(x, y, nette(tons, t, x, y));
                // Des touffes d'herbe, éclairées par le haut.
                if (y > haut + 8 && ((x * 7 + y * 13) % 29) == 0) {
                    point(x, y - 1, tons[Math.min(tons.length - 1, (int) (t * (tons.length - 1)) + 1)]);
                    point(x, y - 2, tons[Math.min(tons.length - 1, (int) (t * (tons.length - 1)) + 1)]);
                }
            }
        }
    }

    /** Des tombes, des colonnes brisées, un pan de mur, entre les arbres. */
    private void ruines() {
        int[] pierre = { c(5, 1), c(5, 2), c(23, 2), c(23, 3), c(23, 4) };
        // Les tombes : des pierres dressées, arrondies, un peu penchées.
        int[][] tombes = { { 150, 196, 6, 11 }, { 166, 200, 5, 9 }, { 136, 203, 7, 12 }, { 286, 198, 5, 10 }, { 182, 205, 6, 8 } };
        for (int[] t : tombes) {
            for (int y = 0; y < t[3]; y++) {
                for (int x = 0; x < t[2]; x++) {
                    if (y < 2 && (x == 0 || x == t[2] - 1)) continue;
                    float l = x == 0 ? 0.9f : x == t[2] - 1 ? 0.15f : 0.55f - y / (float) t[3] * 0.3f;
                    point(t[0] + x + (t[3] - y) / 6, t[1] + y - t[3], rampe(pierre, l, x, y));
                }
            }
        }
        // Deux colonnes brisées à gauche.
        colonne(104, 214, 9, 46, pierre);
        colonne(124, 210, 7, 28, pierre);
    }

    private void colonne(int x0, int pied, int large, int haut, int[] pierre) {
        float[] g = grille(16);
        int sommet = pied - haut;
        for (int x = 0; x < large; x++) {
            int brise = sommet + Math.round(bruit(g, x * 0.7f) * 7f);
            for (int y = brise; y < pied; y++) {
                float l = x == 1 ? 0.95f : x < large / 2 ? 0.7f : x == large - 1 ? 0.05f : 0.35f;
                if ((y - sommet) % 13 == 0) l -= 0.3f;
                point(x0 + x, y, rampe(pierre, l, x0 + x, y));
            }
        }
        // La base.
        for (int x = -2; x < large + 2; x++) for (int y = pied - 3; y < pied; y++) point(x0 + x, y, rampe(pierre, y == pied - 3 ? 0.8f : 0.3f, x0 + x, y));
    }

    /** Le chemin de dalles, qui s'élargit vers nous, éclairé au milieu par le soleil. */
    private void chemin() {
        int[] dalle = { c(4, 1), c(4, 2), c(4, 3), c(23, 3), c(23, 4), c(23, 5) };
        int joint = c(20, 0);
        int haut = 182;
        float[] g = grille(64);
        for (int y = haut; y < H; y++) {
            float t = (y - haut) / (float) (H - haut);
            float centre = 258 - t * 30;
            float demi = 10 + t * 92;
            int rangee = (int) Math.floor(Math.pow(t, 0.7) * 14);
            float hauteurRangee = 1f / 14f;
            float v = (float) Math.pow(t, 0.7) / hauteurRangee - rangee;
            for (int x = Math.round(centre - demi); x <= Math.round(centre + demi); x++) {
                float u = (x - centre) / demi;
                if (Math.abs(u) > 0.92f + bruit(g, y * 0.5f + x * 0.1f) * 0.1f) continue;
                int colonne = (int) Math.floor((u * 3.2f + (rangee % 2) * 0.5f + 10f));
                float w = (u * 3.2f + (rangee % 2) * 0.5f + 10f) - colonne;
                if (v < 0.12f || w < 0.06f + 0.02f / Math.max(0.2f, t)) {
                    point(x, y, joint);
                    continue;
                }
                float lumiere = 0.42f + (1f - Math.abs(u)) * 0.25f - (0.5f - (v - 0.5f)) * 0.08f;
                // La flaque de soleil qui tombe par la trouée des arbres.
                float sx = (x - 236 - (y - 200) * 0.45f) / 26f, sy = (y - 230) / 26f;
                if (sx * sx + sy * sy < 1f) lumiere += 0.22f;
                lumiere += (r.nextFloat() - 0.5f) * 0.1f + (Math.floorMod(colonne * 7 + rangee * 13, 5) - 2) * 0.04f;
                point(x, y, nette(dalle, lumiere, x, y));
            }
        }
    }

    /** L'arche gothique en ruine : des pierres taillées une à une, la mousse sur les arêtes, la lumière du couchant dans l'ouverture. */
    private void arche(int gauche, int droite) {
        int pied = 222, ouvertureG = gauche + 30, ouvertureD = droite - 26, naissance = 132, centre = (ouvertureG + ouvertureD) / 2;
        float demiOuv = (ouvertureD - ouvertureG) / 2f;
        // À contre-jour : la pierre prend les violets du soir ; seul le bord tourné vers le soleil s'allume.
        int[] pierre = { c(20, 0), c(20, 1), c(19, 1), c(19, 2), c(5, 2), c(14, 3), c(23, 4) };
        int lisere = c(16, 6), lisereDoux = c(17, 6);
        int mortier = c(20, 0), mousse = c(21, 2), mousseClaire = c(21, 3);
        float[] g = grille(48);
        int[] sommet = new int[droite - gauche + 1];
        for (int x = gauche; x <= droite; x++) {
            // Le haut brisé : des marches de pierres tombées.
            float n = bruit(g, (x - gauche) / 9f);
            sommet[x - gauche] = 34 + Math.round(n * 40f) + (x < gauche + 10 ? (gauche + 10 - x) * 4 : 0) + (x > droite - 8 ? (x - droite + 8) * 5 : 0);
            sommet[x - gauche] = (sommet[x - gauche] / 6) * 6;
        }
        int hRang = 7;
        for (int y = 0; y < pied; y++) {
            int rang = y / hRang;
            int decal = (rang * 7) % 13;
            for (int x = gauche; x <= droite; x++) {
                if (y < sommet[x - gauche]) continue;
                // L'ouverture en ogive : on y voit la scène derrière.
                float ox = Math.abs(x - centre + 0.5f);
                boolean ouvert;
                if (y >= naissance) ouvert = ox < demiOuv;
                else {
                    // Deux arcs de cercle qui se croisent en pointe.
                    float rayon = demiOuv * 1.7f;
                    float cxArc = x < centre ? centre - demiOuv + rayon : centre + demiOuv - rayon;
                    float dx = x - cxArc, dy = y - naissance;
                    ouvert = dx * dx + dy * dy < rayon * rayon && Math.abs(dx) <= rayon;
                    if (x < centre ? x < centre - demiOuv : x > centre + demiOuv) ouvert = false;
                }
                if (ouvert) continue;
                // Les claveaux autour de l'arc : plus clairs, rayonnants.
                int k = x - gauche + decal;
                int largePierre = 11 + Math.floorMod(rang * 5 + k / 12, 4);
                int dansPierre = Math.floorMod(k, largePierre);
                int dansRang = y % hRang;
                boolean joint = dansRang == hRang - 1 || dansPierre == 0;
                if (joint) {
                    point(x, y, mortier);
                    continue;
                }
                float l = 0.4f + ((rang * 31 + k / largePierre * 17) % 7 - 3) * 0.035f;
                if (dansRang == 0) l += 0.14f;
                if (dansPierre == 1) l += 0.08f;
                if (dansRang == hRang - 2 || dansPierre == largePierre - 1) l -= 0.16f;
                // Plus on s'éloigne du soleil (à gauche), plus la pierre s'enfonce dans l'ombre.
                l -= (x - gauche) / (float) (droite - gauche) * 0.12f;
                // L'embrasure de l'ouverture, dans l'ombre ; le flanc droit du mur, face au soleil couchant.
                if (x >= ouvertureG - 4 && x < ouvertureG && y > naissance - 30) l -= 0.3f;
                if (x > ouvertureD && x < ouvertureD + 3 && y > naissance - 30) l += 0.25f;
                if (x == gauche || y == sommet[x - gauche]) l += 0.2f;
                l -= Math.max(0f, (y - 150) / 300f);
                int couleur = nette(pierre, l, x, y);
                // Le liseré du couchant : le bord gauche du mur et le bord droit de l'ouverture, et le haut des pierres.
                if (x - gauche < 2 && y > sommet[0]) couleur = (y & 1) == 0 ? lisere : lisereDoux;
                else if (x > ouvertureD && x <= ouvertureD + 1 && y > naissance - 10) couleur = lisereDoux;
                else if (y == sommet[x - gauche] || y == sommet[x - gauche] + 1 && (x & 1) == 0) couleur = lisereDoux;
                // La mousse sur le haut des pierres et au pied du mur.
                float m = bruit(g, x / 5f + y * 0.37f);
                if ((y - sommet[x - gauche] < 3 && m > 0.45f) || (y > pied - 14 && m > 0.62f - (y - pied + 14) * 0.03f)) couleur = trame(mousse, mousseClaire, m - 0.4f, x, y);
                point(x, y, couleur);
            }
        }
        // Le lierre qui pend du haut, à gauche.
        for (int k = 0; k < 9; k++) {
            int x = gauche + 3 + r.nextInt(30), y0 = sommet[x - gauche], l = 10 + r.nextInt(40);
            for (int y = y0; y < y0 + l; y++) {
                int xx = x + Math.round((float) Math.sin(y * 0.3f + k) * 1.2f);
                point(xx, y, (y + k) % 3 == 0 ? mousseClaire : mousse);
                if ((y + k) % 5 == 0) point(xx + 1, y, c(10, 2));
            }
        }
    }

    /** Un grand arbre aux feuilles pourpres qui encadre la vue, son tronc dans l'ombre, éclairé d'un côté par le couchant. */
    private void arbre(int base, boolean aGauche) {
        int ecorce = c(20, 0), ecorce2 = c(19, 1), bord = c(17, 3);
        int sens = aGauche ? 1 : -1;
        // Le tronc, qui s'incline vers le centre, et ses racines.
        for (int y = 0; y < H; y++) {
            float t = y / (float) H;
            int cx = base + Math.round(sens * (1f - t) * (1f - t) * 26f);
            int demi = Math.round(9 + t * t * 10 + (y > H - 22 ? (y - H + 22) * 1.3f : 0));
            for (int x = cx - demi; x <= cx + demi; x++) {
                boolean cote = sens > 0 ? x >= cx + demi - 2 : x <= cx - demi + 2;
                int couleur = cote ? bord : ((x * 3 + y / 4) % 7 == 0 ? ecorce2 : ecorce);
                if (!cote && (x - cx + 100) % 5 == 0 && (y / 3) % 4 != 0) couleur = ecorce2;
                point(x, y, couleur);
            }
        }
        // Une branche vers le centre.
        for (int k = 0; k < 70; k++) {
            int x = base + sens * (20 + k), y = 70 - Math.round(k * 0.55f) + Math.round((float) Math.sin(k * 0.12f) * 4f);
            for (int e = 0; e < 4 - k / 25; e++) point(x, y + e, e == 0 ? bord : ecorce);
        }
        // Le feuillage : des touffes rondes, ombrées par-dessous, éclairées en haut et vers le couchant.
        int[] feuilles = { c(19, 0), c(19, 1), c(17, 1), c(17, 2), c(17, 3), c(17, 4), c(17, 5), c(17, 6) };
        int touffes = 38;
        for (int k = 0; k < touffes; k++) {
            float fond = k / (float) touffes;
            int tx = base + sens * Math.round(-30 + r.nextFloat() * 160 * (0.35f + fond * 0.65f));
            int ty = Math.round(-14 + r.nextFloat() * (36 + fond * 46));
            int rayon = 16 + r.nextInt(18);
            for (int y = ty - rayon; y <= ty + rayon; y++) {
                for (int x = tx - rayon; x <= tx + rayon; x++) {
                    float dx = (x - tx) / (float) rayon, dy = (y - ty) / (float) rayon;
                    // Un bord dentelé de feuilles.
                    float bordure = 1f - 0.08f * Math.abs((float) Math.sin(Math.atan2(dy, dx) * 9f + k)) - (((x * 7 + y * 3) & 3) == 0 ? 0.06f : 0f);
                    if (dx * dx + dy * dy > bordure * bordure) continue;
                    float l = 0.25f + fond * 0.4f - dy * 0.35f + (-sens) * dx * 0.18f;
                    if (((x * 7 + y * 13 + k) & 7) == 0) l += 0.18f;
                    if (((x * 5 + y * 3 + k) % 11) == 0) l -= 0.2f;
                    point(x, y, nette(feuilles, l, x, y));
                }
            }
        }
    }

    /** Des buissons sombres en bas, piqués de fleurs. */
    private void buissons() {
        int[] tons = { c(20, 0), c(10, 0), c(10, 1), c(10, 2), c(21, 2), c(21, 3) };
        int[] fleurs = { c(17, 6), c(16, 6), c(17, 7) };
        int[][] tas = { { 70, 268, 60 }, { 150, 272, 36 }, { 420, 270, 70 }, { 330, 274, 30 }, { 0, 262, 50 } };
        for (int[] b : tas) {
            for (int y = b[1] - b[2]; y < H; y++) {
                for (int x = b[0] - b[2] * 2; x <= b[0] + b[2] * 2; x++) {
                    float dx = (x - b[0]) / (b[2] * 2f), dy = (y - b[1]) / (float) b[2];
                    float bordure = 1f - 0.15f * Math.abs((float) Math.sin(x * 0.5f) + (float) Math.sin(x * 0.23f + y));
                    if (dx * dx + dy * dy > bordure * bordure) continue;
                    float l = 0.35f - dy * 0.55f + (((x * 3 + y * 5) % 9) == 0 ? 0.15f : 0f);
                    int couleur = nette(tons, l, x, y);
                    point(x, y, couleur);
                    // Des fleurs en petites grappes de deux ou trois pixels.
                    if (((x / 2 * 13 + y / 2 * 7) % 41) == 0 && dy < -0.25f && ((x + y) & 1) == 0) {
                        int f = fleurs[(x / 2 + y / 2) % 3];
                        point(x, y, f);
                        point(x + 1, y, f);
                        point(x, y - 1, fleurs[2]);
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- ce qui bouge

    private Texture rayons() {
        int l = 256, h = 256;
        Pixmap p = new Pixmap(l, h, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        float[] largeur = { 0.07f, 0.04f, 0.1f, 0.05f };
        float[] place = { 0.38f, 0.5f, 0.6f, 0.72f };
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < l; x++) {
                float u = x / (float) l - (1f - y / (float) h) * 0.45f;
                float a = 0f;
                for (int k = 0; k < 4; k++) {
                    float d = Math.abs(u - place[k]) / largeur[k];
                    a += Math.max(0f, 1f - d) * 0.5f;
                }
                a *= (float) Math.sin(Math.PI * y / h);
                p.setColor(1f, 0.72f, 0.42f, Math.min(1f, a) * 0.5f);
                p.drawPixel(x, y);
            }
        }
        Texture t = new Texture(p);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        p.dispose();
        return t;
    }

    private Texture brume() {
        int t = 128;
        Pixmap p = new Pixmap(t, 32, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        float[] g = grille(16), g2 = grille(16);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < t; x++) {
                float v = bruit(g, x / 8f) * 0.6f + bruit(g2, x / 3f + y * 0.2f) * 0.4f;
                float bord = (float) Math.sin(Math.PI * y / 32f);
                p.setColor(1f, 1f, 1f, Math.max(0f, v - 0.3f) * bord);
                p.drawPixel(x, y);
            }
        }
        Texture tx = new Texture(p);
        tx.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        tx.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.ClampToEdge);
        p.dispose();
        return tx;
    }

    private void petale(int k, boolean partout) {
        px[k] = r.nextFloat() * L;
        py[k] = partout ? r.nextFloat() * H : -2f;
        pv[k] = 6f + r.nextFloat() * 10f;
        pp[k] = r.nextFloat() * 6.28f;
    }

    public void dessiner(SpriteBatch pinceau, float delta, float largeur, float hauteur, float sourisX) {
        temps += delta;
        // L'échelle qui couvre l'écran, sans déformer la scène.
        float e = Math.max(largeur / L, hauteur / H);
        float ox = (largeur - L * e) / 2f, oy = (hauteur - H * e) / 2f;
        pinceau.setColor(Color.WHITE);
        pinceau.draw(scene, ox, oy, L * e, H * e);
        // Les fenêtres du château qui vacillent.
        for (int k = 0; k < nFenetres; k++) {
            int fx = fenetres[k] & 0xFFFF, fy = fenetres[k] >>> 16;
            float a = 0.55f + 0.45f * (float) Math.sin(temps * (1.7f + k * 0.4f) + k * 2f);
            pinceau.setColor(1f, 0.85f, 0.45f, a);
            pinceau.draw(blanc, ox + fx * e, oy + (H - 1 - fy) * e, e, e);
        }
        // La brume qui passe au pied des montagnes.
        pinceau.setColor(0.95f, 0.6f, 0.5f, 0.18f);
        float u = temps * 0.01f;
        pinceau.draw(brume, ox, oy + (H - HORIZON - 14) * e, L * e, 22 * e, u, 1f, u + 2.5f, 0f);
        pinceau.setColor(0.7f, 0.4f, 0.45f, 0.14f);
        pinceau.draw(brume, ox, oy + (H - 210) * e, L * e, 26 * e, -u * 1.6f, 1f, -u * 1.6f + 2f, 0f);
        // Les rayons du couchant, qui respirent.
        pinceau.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        pinceau.setColor(1f, 1f, 1f, 0.22f + 0.08f * (float) Math.sin(temps * 0.5f));
        pinceau.draw(rayons, ox + 120 * e, oy, 240 * e, 230 * e);
        // Les lucioles.
        for (int k = 0; k < LUCIOLES; k++) {
            float x = lx[k] + (float) Math.sin(temps * 0.6f + lp[k]) * 8f, y = ly[k] + (float) Math.sin(temps * 0.9f + lp[k] * 2f) * 4f;
            float a = Math.max(0f, (float) Math.sin(temps * 1.3f + lp[k] * 3f));
            pinceau.setColor(1f, 0.82f, 0.4f, a * 0.9f);
            pinceau.draw(blanc, ox + Math.round(x) * e, oy + (H - 1 - Math.round(y)) * e, e, e);
            pinceau.setColor(1f, 0.6f, 0.25f, a * 0.25f);
            pinceau.draw(blanc, ox + (Math.round(x) - 1) * e, oy + (H - 2 - Math.round(y)) * e, e * 3, e * 3);
        }
        pinceau.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        // Les pétales qui tombent des arbres.
        for (int k = 0; k < PETALES; k++) {
            py[k] += pv[k] * delta;
            px[k] += (float) Math.sin(temps * 1.4f + pp[k]) * 6f * delta + 3f * delta;
            if (py[k] > H + 2) petale(k, false);
            boolean clair = (k & 1) == 0;
            pinceau.setColor(clair ? 0.85f : 0.66f, clair ? 0.36f : 0.2f, clair ? 0.32f : 0.2f, 0.95f);
            pinceau.draw(blanc, ox + Math.round(px[k]) * e, oy + (H - 1 - Math.round(py[k])) * e, e, e);
        }
        pinceau.setColor(Color.WHITE);
    }

    public void dispose() {
        art.dispose();
        scene.dispose();
        rayons.dispose();
        brume.dispose();
        blanc.dispose();
    }
}
