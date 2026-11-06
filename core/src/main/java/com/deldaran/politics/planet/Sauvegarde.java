package com.deldaran.politics.planet;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class Sauvegarde {
    private static final int FORMAT = 1;
    private static final int TAILLE_ENTETE = 32;
    private static final int TAILLE_CASE = 35;

    private Sauvegarde() {
    }

    public static void enregistrer(Monde monde, Simulation simulation, FileHandle fichier) {
        try (DataOutputStream sortie = new DataOutputStream(new BufferedOutputStream(fichier.write(false)))) {
            sortie.writeInt(FORMAT);
            sortie.writeLong(monde.seed);
            sortie.writeInt(monde.plaque.length);
            sortie.writeFloat(monde.niveauMer);
            sortie.writeFloat(monde.volumeEau);
            sortie.writeFloat(monde.temps);
            sortie.writeFloat(simulation.climat.hauteurPlaines);
            for (int i = 0; i < monde.plaque.length; i++) {
                sortie.writeFloat(monde.altitude[i]);
                sortie.writeFloat(monde.epaisseur[i]);
                sortie.writeFloat(monde.temperature[i]);
                sortie.writeFloat(monde.pluie[i]);
                sortie.writeFloat(monde.debit[i]);
                sortie.writeFloat(monde.surfaceEau[i]);
                sortie.writeInt(monde.plaque[i]);
                sortie.writeInt(monde.receveur[i]);
                sortie.writeByte(monde.biome[i]);
                sortie.writeBoolean(monde.lac[i]);
                sortie.writeBoolean(monde.isContinentale[i]);
            }
        } catch (IOException e) {
            throw new GdxRuntimeException("Impossible d'enregistrer le monde", e);
        }
    }

    public static boolean charger(Monde monde, Simulation simulation, FileHandle fichier) {
        int n = monde.plaque.length;
        if (!fichier.exists() || fichier.length() != TAILLE_ENTETE + (long) n * TAILLE_CASE) {
            return false;
        }
        try (DataInputStream entree = new DataInputStream(new BufferedInputStream(fichier.read()))) {
            if (entree.readInt() != FORMAT || entree.readLong() != monde.seed || entree.readInt() != n) {
                return false;
            }
            monde.niveauMer = entree.readFloat();
            monde.volumeEau = entree.readFloat();
            monde.temps = entree.readFloat();
            simulation.climat.hauteurPlaines = entree.readFloat();
            for (int i = 0; i < n; i++) {
                monde.altitude[i] = entree.readFloat();
                monde.epaisseur[i] = entree.readFloat();
                monde.temperature[i] = entree.readFloat();
                monde.pluie[i] = entree.readFloat();
                monde.debit[i] = entree.readFloat();
                monde.surfaceEau[i] = entree.readFloat();
                monde.plaque[i] = entree.readInt();
                monde.receveur[i] = entree.readInt();
                monde.biome[i] = entree.readByte();
                monde.lac[i] = entree.readBoolean();
                monde.isContinentale[i] = entree.readBoolean();
            }
        } catch (IOException e) {
            throw new GdxRuntimeException("Impossible de charger le monde", e);
        }
        simulation.phase = Simulation.Phase.TERMINEE;
        return true;
    }
}
