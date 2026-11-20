package com.deldaran.politics.histoire;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public final class Archive {
    private static final int FORMAT = 3;
    private static final long FIN = 0x46494E484953L;
    private static final int TAMPON = 1 << 16;

    private final DataOutputStream sortie;
    private final DataInputStream entree;

    private Archive(DataOutputStream sortie, DataInputStream entree) {
        this.sortie = sortie;
        this.entree = entree;
    }

    public static void enregistrer(Population p, FileHandle fichier) {
        FileHandle provisoire = fichier.sibling(fichier.name() + ".tmp");
        Deflater compression = new Deflater(Deflater.BEST_SPEED);
        try {
            try (DataOutputStream sortie = new DataOutputStream(new BufferedOutputStream(new DeflaterOutputStream(provisoire.write(false), compression, TAMPON), TAMPON))) {
                Archive a = new Archive(sortie, null);
                a.entier(FORMAT);
                a.grand(p.graine);
                a.entier(p.regions.nombre);
                p.archiver(a);
                a.grand(FIN);
            }
            Files.move(provisoire.file().toPath(), fichier.file().toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new GdxRuntimeException("Impossible d'enregistrer l'histoire", e);
        } finally {
            compression.end();
        }
    }

    public static boolean charger(Population p, FileHandle fichier) {
        if (!fichier.exists()) return false;
        try (DataInputStream entree = new DataInputStream(new BufferedInputStream(new InflaterInputStream(fichier.read()), TAMPON))) {
            Archive a = new Archive(null, entree);
            if (a.entier(0) != FORMAT || a.grand(0L) != p.graine || a.entier(0) != p.regions.nombre) return false;
            p.archiver(a);
            return a.grand(0L) == FIN;
        } catch (IOException | RuntimeException | OutOfMemoryError e) {
            return false;
        }
    }

    public boolean enLecture() {
        return entree != null;
    }

    public int entier(int v) throws IOException {
        if (entree != null) return entree.readInt();
        sortie.writeInt(v);
        return v;
    }

    public long grand(long v) throws IOException {
        if (entree != null) return entree.readLong();
        sortie.writeLong(v);
        return v;
    }

    public double reel(double v) throws IOException {
        if (entree != null) return entree.readDouble();
        sortie.writeDouble(v);
        return v;
    }

    public byte[] octets(byte[] t, int n) throws IOException {
        int longueur = entier(t.length), utile = entier(n);
        byte[] r = longueur == t.length ? t : new byte[longueur];
        if (entree != null) entree.readFully(r, 0, utile);
        else sortie.write(t, 0, utile);
        return r;
    }

    public boolean[] booleens(boolean[] t, int n) throws IOException {
        int longueur = entier(t.length), utile = entier(n);
        boolean[] r = longueur == t.length ? t : new boolean[longueur];
        for (int i = 0; i < utile; i++) {
            if (entree != null) r[i] = entree.readBoolean();
            else sortie.writeBoolean(t[i]);
        }
        return r;
    }

    public short[] courts(short[] t, int n) throws IOException {
        int longueur = entier(t.length), utile = entier(n);
        short[] r = longueur == t.length ? t : new short[longueur];
        for (int i = 0; i < utile; i++) {
            if (entree != null) r[i] = entree.readShort();
            else sortie.writeShort(t[i]);
        }
        return r;
    }

    public int[] entiers(int[] t, int n) throws IOException {
        int longueur = entier(t.length), utile = entier(n);
        int[] r = longueur == t.length ? t : new int[longueur];
        for (int i = 0; i < utile; i++) {
            if (entree != null) r[i] = entree.readInt();
            else sortie.writeInt(t[i]);
        }
        return r;
    }

    public long[] longs(long[] t, int n) throws IOException {
        int longueur = entier(t.length), utile = entier(n);
        long[] r = longueur == t.length ? t : new long[longueur];
        for (int i = 0; i < utile; i++) {
            if (entree != null) r[i] = entree.readLong();
            else sortie.writeLong(t[i]);
        }
        return r;
    }

    public float[] reels(float[] t, int n) throws IOException {
        int longueur = entier(t.length), utile = entier(n);
        float[] r = longueur == t.length ? t : new float[longueur];
        for (int i = 0; i < utile; i++) {
            if (entree != null) r[i] = entree.readFloat();
            else sortie.writeFloat(t[i]);
        }
        return r;
    }

    public double[] doubles(double[] t, int n) throws IOException {
        int longueur = entier(t.length), utile = entier(n);
        double[] r = longueur == t.length ? t : new double[longueur];
        for (int i = 0; i < utile; i++) {
            if (entree != null) r[i] = entree.readDouble();
            else sortie.writeDouble(t[i]);
        }
        return r;
    }
}
