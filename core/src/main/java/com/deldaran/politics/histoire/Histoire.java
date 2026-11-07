package com.deldaran.politics.histoire;

import com.deldaran.politics.planet.Monde;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Histoire {
    private static final long ATTENTE_PAUSE_MS = 50L;

    public final Population population;
    public volatile int dureeMax = 5000;
    public volatile boolean enPause = false;
    public volatile float msParAn = 0f;

    private volatile boolean arret = false;
    private final Thread fil;
    private final ConcurrentLinkedQueue<Runnable> demandes = new ConcurrentLinkedQueue<>();

    public Histoire(Monde monde, Regions regions, Population.Reglages reglages) {
        population = new Population(monde, regions, reglages);
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
            if (enPause || population.annee >= dureeMax) {
                attendre();
                continue;
            }
            long debut = System.nanoTime();
            population.avancer();
            msParAn = (System.nanoTime() - debut) / 1000000f;
        }
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
