package com.deldaran.politics.planet;

public class Simulation {
    public enum Phase { TECTONIQUE, RIVIERES, TERMINEE }

    private static final int PERIODE_TEMPERATURE = 10;
    private static final int PERIODE_CLIMAT = 100;
    private static final double MA_PAR_PAS_RIVIERES = 0.01;
    private static final int PAS_APRES_GLACIERS = 20;

    public int dureeTectonique = 2500;
    public int pasRivieres = 1500;

    public final Tectonique tectonique;
    public final Climat climat;
    public Phase phase = Phase.TECTONIQUE;

    private final Monde monde;
    private Hydrologie hydrologie;
    private int pasHydrologie = 0;

    public Simulation(Monde monde) {
        this.monde = monde;
        tectonique = new Tectonique(monde);
        climat = new Climat(monde);
        climat.calculerTemperatures();
    }

    public void pas() {
        switch (phase) {
            case TECTONIQUE: pasTectonique(); break;
            case RIVIERES: pasRiviere(); break;
            default: break;
        }
    }

    private void pasTectonique() {
        tectonique.pas();
        if ((int) monde.temps % PERIODE_TEMPERATURE == 0) {
            climat.calculerTemperatures();
        }
        if (monde.temps >= dureeTectonique) {
            climat.calculer();
            hydrologie = new Hydrologie(monde);
            phase = Phase.RIVIERES;
        }
    }

    private void pasRiviere() {
        if (pasHydrologie == pasRivieres - PAS_APRES_GLACIERS) {
            hydrologie.creuserGlaciers();
        }
        hydrologie.pas();
        pasHydrologie++;
        if (pasHydrologie % PERIODE_CLIMAT == 0) {
            climat.calculer();
        }
        if (pasHydrologie >= pasRivieres) {
            climat.calculer();
            phase = Phase.TERMINEE;
        }
    }

    public String tempsTexte() {
        if (phase == Phase.TECTONIQUE) {
            return (int) monde.temps + " Ma";
        }
        return String.format("%d Ma + %.2f Ma", (int) monde.temps, pasHydrologie * MA_PAR_PAS_RIVIERES);
    }
}
