package com.deldaran.politics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import java.io.Reader;
import java.io.Writer;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

/**
 * Lot K1 : les mondes du joueur. Chacun a son dossier (Thaloran/mondes/&lt;id&gt;/) avec sa fiche (monde.properties) et ses
 * sauvegardes : le monde (monde-&lt;graine&gt;.bin), son histoire, le joueur, les relations. L'ancien monde 42, rangé à la racine
 * de Thaloran/mondes, reste jouable.
 */
public final class Mondes {
    public static final String RACINE = Thaloran.DOSSIER + "mondes/";
    public static final int[] DUREES = { 1000, 2000, 3500, 5000 };
    private static final String[] DEBUTS = { "Les Terres", "Le Val", "Les Marches", "Le Royaume", "Les Plaines", "Les Îles", "Le Pays", "Les Contrées" };
    private static final String[] FINS = { " de Cendre", " d'Ombre", " du Couchant", " des Brumes", " de Fer", " des Anciens", " de Givre", " du Serment", " des Cloches", " de l'Aube grise", " des Rois morts", " du Sel" };

    public static final class Fiche {
        public String id, nom, dossier;
        public long graine;
        public int duree = 5000;
        public long cree, joue;
        /** L'ancien monde 42, d'avant les mondes multiples. */
        public boolean ancien;

        public FileHandle fichier(String nom) {
            return Gdx.files.external(dossier + nom);
        }

        public boolean forme() {
            return fichier("monde-" + graine + ".bin").exists();
        }

        public boolean histoireFaite() {
            return fichier("histoire-" + graine + ".bin").exists();
        }

        public boolean personnage() {
            return fichier("joueur-" + graine + ".bin").exists();
        }

        public String etat() {
            if (!forme()) return "Le monde n'est pas encore formé";
            if (!histoireFaite()) return "Son histoire n'est pas encore écrite";
            if (personnage()) return "Ton personnage t'y attend";
            return "Prêt à t'accueillir";
        }
    }

    private Mondes() {
    }

    /** Le monde 42 d'avant (les essais l'utilisent toujours). */
    public static Fiche ancien() {
        Fiche f = new Fiche();
        f.id = "monde-42";
        f.nom = "Le premier monde";
        f.dossier = RACINE;
        f.graine = 42L;
        f.duree = 5000;
        f.ancien = true;
        FileHandle m = f.fichier("monde-42.bin");
        f.cree = m.exists() ? m.lastModified() : 0L;
        FileHandle j = f.fichier("joueur-42.bin");
        f.joue = Math.max(f.cree, j.exists() ? j.lastModified() : 0L);
        return f;
    }

    /** Tous les mondes, le dernier joué en premier. */
    public static List<Fiche> tous() {
        List<Fiche> l = new ArrayList<>();
        FileHandle racine = Gdx.files.external(RACINE);
        if (racine.exists()) {
            for (FileHandle d : racine.list()) {
                if (!d.isDirectory()) continue;
                FileHandle fiche = d.child("monde.properties");
                if (!fiche.exists()) continue;
                Fiche f = lire(fiche);
                if (f != null) l.add(f);
            }
        }
        if (Gdx.files.external(RACINE + "monde-42.bin").exists()) l.add(ancien());
        l.sort((a, b) -> Long.compare(b.joue, a.joue));
        return l;
    }

    private static Fiche lire(FileHandle fichier) {
        Properties p = new Properties();
        try (Reader r = fichier.reader("UTF-8")) {
            p.load(r);
            Fiche f = new Fiche();
            f.id = fichier.parent().name();
            f.dossier = RACINE + f.id + "/";
            f.nom = p.getProperty("nom", f.id);
            f.graine = Long.parseLong(p.getProperty("graine", "42"));
            f.duree = Integer.parseInt(p.getProperty("duree", "5000"));
            f.cree = Long.parseLong(p.getProperty("cree", "0"));
            f.joue = Long.parseLong(p.getProperty("joue", "0"));
            return f;
        } catch (Exception e) {
            return null;
        }
    }

    public static void enregistrer(Fiche f) {
        if (f.ancien) return;
        Properties p = new Properties();
        p.setProperty("nom", f.nom);
        p.setProperty("graine", Long.toString(f.graine));
        p.setProperty("duree", Integer.toString(f.duree));
        p.setProperty("cree", Long.toString(f.cree));
        p.setProperty("joue", Long.toString(f.joue));
        try (Writer w = f.fichier("monde.properties").writer(false, "UTF-8")) {
            p.store(w, "Thaloran : un monde");
        } catch (Exception e) {
            Gdx.app.error("Mondes", "fiche non enregistrée", e);
        }
    }

    public static Fiche creer(String nom, long graine, int duree) {
        Fiche f = new Fiche();
        f.nom = nom.isBlank() ? nomAuHasard(new Random()) : nom.strip();
        String base = Normalizer.normalize(f.nom, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        if (base.isEmpty()) base = "monde";
        f.id = base + "-" + Long.toString(System.currentTimeMillis(), 36);
        f.dossier = RACINE + f.id + "/";
        f.graine = graine;
        f.duree = duree;
        f.cree = System.currentTimeMillis();
        f.joue = f.cree;
        Gdx.files.external(f.dossier).mkdirs();
        enregistrer(f);
        return f;
    }

    public static void marquerJoue(Fiche f) {
        f.joue = System.currentTimeMillis();
        enregistrer(f);
    }

    /** Efface un monde et toutes ses sauvegardes. */
    public static void supprimer(Fiche f) {
        if (f.ancien) {
            for (String n : new String[] { "monde-", "histoire-", "joueur-", "relations-" }) f.fichier(n + f.graine + ".bin").delete();
            return;
        }
        Gdx.files.external(f.dossier).deleteDirectory();
    }

    public static String nomAuHasard(Random r) {
        return DEBUTS[r.nextInt(DEBUTS.length)] + FINS[r.nextInt(FINS.length)];
    }
}
