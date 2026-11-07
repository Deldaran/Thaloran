package com.deldaran.politics.histoire;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;

import com.deldaran.politics.planet.Monde;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Histoire {
    private static final long ATTENTE_PAUSE_MS = 50L;

    public final Population population;
    public volatile int dureeMax = 5000;
    public volatile boolean enPause = false;
    public volatile float msParAn = 0f;
    public volatile String sauvegarde = "Aucune";
    private final FileHandle fichier;
    private boolean aEnregistrer = false;

    private volatile boolean arret = false;
    private final Thread fil;
    private final ConcurrentLinkedQueue<Runnable> demandes = new ConcurrentLinkedQueue<>();

    public Histoire(Monde monde, Regions regions, Population.Reglages reglages, FileHandle fichier, boolean reprendre) {
        this.fichier = fichier;
        Population depart = new Population(monde, regions, reglages);
        if (reprendre && fichier.exists()) {
            if (Archive.charger(depart, fichier)) {
                sauvegarde = "Chargee, an " + depart.annee;
            } else {
                depart = new Population(monde, regions, reglages);
                sauvegarde = "Illisible, nouvelle histoire";
            }
        }
        population = depart;

        fil = new Thread(this::tourner, "histoire");
        fil.setDaemon(true);
        fil.start();
    }

    private void tourner() {
        while (!arret) {
            Runnable demande;
            while ((demande = demandes.poll()) != null) {
                demande.run();
            }
            if (aEnregistrer && population.annee >= dureeMax) {
                enregistrer();
            }
            if (enPause || population.annee >= dureeMax) {
                attendre();
                continue;
            }
            long debut = System.nanoTime();
            population.avancer();
            aEnregistrer = true;
            msParAn = (System.nanoTime() - debut) / 1000000f;
        }
    }

    public void enregistrer() {
        sauvegarde = "En cours";
        try {
            Archive.enregistrer(population, fichier);
            sauvegarde = "Faite, an " + population.annee;
        } catch (GdxRuntimeException e) {
            sauvegarde = "Echec : " + e.getMessage();
        }
        aEnregistrer = false;
    }


    private void attendre() {
        try {
            Thread.sleep(ATTENTE_PAUSE_MS);
        } catch (InterruptedException e) {
            arret = true;
        }
    }

    public void demander(Runnable demande) {
        demandes.add(demande);
    }

    public void arreter() {
        arret = true;
        try {
            fil.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
