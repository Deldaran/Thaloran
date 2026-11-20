package com.deldaran.politics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lot K1 : la musique et les bruitages. Une musique est un dossier de assets/musics (main_menu, creation_monde,
 * select_personnage_history) : ses morceaux s'enchaînent dans un ordre mêlé, et l'on passe d'un dossier à l'autre en fondu.
 * Les bruitages sont des fichiers de assets/sons (clic.wav…) ; s'il manque un fichier, on ne joue rien.
 */
public final class Sons {
    /** Durée du fondu entre deux musiques, en secondes. */
    private static final float FONDU = 2.0f;
    private static String dossier;
    private static List<FileHandle> pistes = new ArrayList<>();
    private static int index;
    private static Music courante, sortante;
    private static float entree = 1f, sortie = 1f;
    private static final Map<String, Sound> effets = new HashMap<>();
    /** Les essais tournent en silence. */
    public static boolean muet = System.getenv("SORTIE") != null && System.getenv("SON") == null;

    private Sons() {
    }

    /** Joue la musique de ce dossier (rien ne change si c'est déjà elle) ; null pour s'arrêter en fondu. */
    public static void musique(String nouveau) {
        if (muet || java.util.Objects.equals(nouveau, dossier)) return;
        if (System.getenv("SON") != null) System.out.println("MUSIQUE " + nouveau);
        dossier = nouveau;
        eteindre();
        pistes = nouveau == null ? new ArrayList<>() : lister("musics/" + nouveau);
        Collections.shuffle(pistes);
        index = 0;
        jouerPiste();
    }

    public static String musique() {
        return dossier;
    }

    private static void eteindre() {
        if (sortante != null) sortante.dispose();
        sortante = courante;
        sortie = 1f;
        courante = null;
    }

    private static void jouerPiste() {
        if (pistes.isEmpty()) return;
        try {
            Music m = Gdx.audio.newMusic(pistes.get(index % pistes.size()));
            m.setVolume(0f);
            m.setOnCompletionListener(fini -> Gdx.app.postRunnable(() -> {
                if (fini != courante) return;
                index++;
                eteindre();
                jouerPiste();
            }));
            m.play();
            if (System.getenv("SON") != null) System.out.println("PISTE " + pistes.get(index % pistes.size()).path());
            courante = m;
            entree = 0f;
        } catch (Exception e) {
            Gdx.app.error("Sons", "musique illisible : " + pistes.get(index % pistes.size()), e);
        }
    }

    /** Les fondus ; à appeler à chaque image. */
    public static void mettreAJour(float delta) {
        if (courante != null) {
            entree = Math.min(1f, entree + delta / FONDU);
            courante.setVolume(entree * Parametres.musique);
        }
        if (sortante != null) {
            sortie -= delta / FONDU;
            if (sortie <= 0f) {
                sortante.dispose();
                sortante = null;
            } else sortante.setVolume(sortie * Parametres.musique);
        }
    }

    /** Un bruitage de assets/sons/&lt;nom&gt;.wav (ou .ogg). */
    public static void effet(String nom) {
        effet(nom, 1f, 1f);
    }

    public static void effet(String nom, float volume, float hauteur) {
        if (muet || Parametres.sons <= 0f) return;
        Sound s = effets.get(nom);
        if (s == null && !effets.containsKey(nom)) {
            FileHandle f = Gdx.files.internal("sons/" + nom + ".wav");
            if (!f.exists()) f = Gdx.files.internal("sons/" + nom + ".ogg");
            try {
                s = f.exists() ? Gdx.audio.newSound(f) : null;
            } catch (Exception e) {
                s = null;
            }
            effets.put(nom, s);
        }
        if (s != null) s.play(volume * Parametres.sons, hauteur, 0f);
    }

    /** Les fichiers audio d'un dossier des assets (par le disque, sinon par assets.txt quand le jeu est empaqueté). */
    static List<FileHandle> lister(String chemin) {
        List<FileHandle> l = new ArrayList<>();
        FileHandle d = Gdx.files.internal(chemin);
        if (d.isDirectory()) {
            for (FileHandle f : d.list()) if (audio(f.name())) l.add(f);
        }
        if (l.isEmpty()) {
            FileHandle index = Gdx.files.internal("assets.txt");
            if (index.exists()) {
                for (String ligne : index.readString("UTF-8").split("\\R")) {
                    String s = ligne.replace('\\', '/').strip();
                    if (s.startsWith(chemin + "/") && audio(s)) l.add(Gdx.files.internal(s));
                }
            }
        }
        return l;
    }

    private static boolean audio(String nom) {
        String n = nom.toLowerCase();
        return n.endsWith(".mp3") || n.endsWith(".ogg") || n.endsWith(".wav");
    }

    public static void liberer() {
        if (courante != null) courante.dispose();
        if (sortante != null) sortante.dispose();
        courante = sortante = null;
        for (Sound s : effets.values()) if (s != null) s.dispose();
        effets.clear();
        dossier = null;
    }
}
