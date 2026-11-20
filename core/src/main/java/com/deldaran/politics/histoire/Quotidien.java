package com.deldaran.politics.histoire;

import com.deldaran.politics.Metiers;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Lot G1 (les petits restes du travail) :
 * - les HORAIRES par métier : le boulanger ouvre avant l'aube, la taverne ferme tard, les champs suivent le soleil ;
 * - l'APPRENTI est nourri (une soupe par jour de travail) et logé (le grenier du maître) ;
 * - les CONGÉS : quelques jours accordés par un maître content, une fois par saison ;
 * - le CHANTIER à la journée : déblayer une ruine pour la ville (payé par la bourse de la ville), ou mettre la main à son propre chantier
 *   (il avance d'un jour) ;
 * - les SERVIABLES : dans chaque ville, un habitant prend les missions que personne ne prend, et en gagne le renom.
 */
public final class Quotidien {
    /** Congés : jours possibles, estime du maître nécessaire, ce qu'il en coûte. */
    public static final int[] CONGES = { 3, 7 };
    public static final float ESTIME_CONGE = 40f, COUT_CONGE = 3f;
    /** La soupe de l'apprenti retire SOUPE points de faim. */
    public static final float SOUPE = 35f;
    /** Une journée de chantier : HEURES_CHANTIER heures, payée SALAIRE_CHANTIER + Labeur ÷ 25 cuivre. */
    public static final float HEURES_CHANTIER = 8f;
    public static final int SALAIRE_CHANTIER = 6;
    /** Un service rendu par le serviable de la ville vaut RENOM_SERVICE de renom. */
    public static final int RENOM_SERVICE = 5;

    private final Joueur j;
    private final Population p;
    private int congeSaison = -1, repasJour = -1, chantierJour = -1;
    /** Les serviables : ville → services rendus. */
    public final Map<Integer, Integer> services = new HashMap<>();
    private final Map<Integer, Boolean> depasse = new HashMap<>();

    Quotidien(Joueur j, Population p) {
        this.j = j;
        this.p = p;
    }

    // ================================================================ Les horaires

    public static int ouverture(int lieu) {
        switch (lieu) {
            case Metiers.BOULANGERIE: return 3;
            case Metiers.BOUCHERIE: case Metiers.MOULIN: case Biens.CHAMPS: return 5;
            case Metiers.AUBERGE: case Metiers.TAVERNE: return 10;
            case Biens.MARCHE: return 7;
            case Metiers.ECOLE: return 8;
            default: return Travail.OUVERTURE;
        }
    }

    public static int fermeture(int lieu) {
        switch (lieu) {
            case Metiers.BOULANGERIE: return 13;
            case Metiers.BOUCHERIE: return 17;
            case Metiers.AUBERGE: case Metiers.TAVERNE: return 23;
            case Biens.CHAMPS: return 20;
            case Biens.MARCHE: case Biens.FORET: case Biens.MINE: return 18;
            case Metiers.ECOLE: return 16;
            default: return Travail.FERMETURE;
        }
    }

    public static boolean ouvert(int lieu, double h) {
        return h >= ouverture(lieu) && h < fermeture(lieu);
    }

    // ================================================================ L'apprenti

    /** Une séance de travail : l'apprenti mange chez son maître (une fois par jour). */
    void seance(int rang, float heures) {
        if (rang != Travail.APPRENTI || heures < 1f || repasJour == j.jourMaintenant()) return;
        repasJour = j.jourMaintenant();
        j.corps().faim = Math.max(0f, j.corps().faim - SOUPE);
        j.annoncer("Ton maître te sert une écuelle de soupe : l'apprenti est nourri.");
    }

    // ================================================================ Les congés

    /** Dans « Travail » : demander un congé. */
    void choixTravail(Joueur.Scene s) {
        Travail t = j.travail();
        if (!t.employe()) return;
        int saison = Math.floorDiv(j.jourMaintenant(), Terre.JOURS_PAR_SAISON);
        if (congeSaison == saison) return;
        for (int n : CONGES) s.ajouter("Demander " + n + " jours de congé", () -> conge(n));
    }

    private Joueur.Scene conge(int n) {
        Travail t = j.travail();
        if (t.estime < ESTIME_CONGE) return t.accueil("« Un congé ? Travaille d'abord comme il faut. » Ton maître n'est pas assez content de toi.");
        congeSaison = Math.floorDiv(j.jourMaintenant(), Terre.JOURS_PAR_SAISON);
        // On fait comme si l'on avait travaillé jusqu'au dernier jour du congé : aucune absence ne sera comptée.
        t.dernierJour = Math.max(t.dernierJour, j.jourMaintenant() + n - 1);
        t.manquesComptes = 0;
        t.estime -= COUT_CONGE * (n / 3f);
        j.enregistrer();
        return t.accueil("« Va. Reviens dans " + n + " jours, pas un de plus. » Tu as congé jusqu'au " + Terre.date(p, j.jourMaintenant() + n) + ".");
    }

    // ================================================================ Le chantier à la journée

    private boolean peutTravailler() {
        double h = j.heureIci();
        return h >= Travail.OUVERTURE && h < Travail.FERMETURE && j.travail().fatigue <= Travail.FATIGUE_MAX - 30f && chantierJour != j.jourMaintenant();
    }

    /** Devant son chantier : y mettre la main. */
    void choixChantier(Joueur.Scene s, Affaires.Chantier c) {
        if (!peutTravailler() || c.fin - j.jour() <= 1) return;
        s.ajouter("Mettre la main à ton chantier (une journée, le chantier avance d'un jour)", () -> {
            journee("poser_poutre");
            c.fin--;
            j.enregistrer();
            Joueur.Scene r = new Joueur.Scene("Ton chantier", "Tu portes les pierres et gâches le mortier avec les maçons. Le chantier a pris un jour d'avance : il sera fini dans " + Math.max(1, c.fin - j.jour()) + " jours.");
            r.ajouter("Continuer", j::fermerScene);
            return r;
        });
    }

    /** Devant une ruine de la ville : la déblayer à la journée. */
    void choixRuine(Joueur.Scene s, int v) {
        if (!peutTravailler()) return;
        Fiche f = j.fiche();
        int paye = SALAIRE_CHANTIER + f.competences[Fiche.LABEUR] / 25;
        s.ajouter("Déblayer la ruine pour la ville, à la journée (" + Fiche.argentTexte(paye) + ")", () -> {
            journee("pelleter");
            f.argent += paye;
            EconomieVille ev = j.marcheVille();
            if (ev != null) ev.bourse -= paye;
            j.renommee().gagner(v, 1f, 1f, null);
            j.enregistrer();
            Joueur.Scene r = new Joueur.Scene("Le chantier de la ville", "Tu tries les pierres encore bonnes, sors les poutres brûlées, charges la charrette. Le soir, le bailli te paie " + Fiche.argentTexte(paye) + ".");
            r.ajouter("Continuer", j::fermerScene);
            return r;
        });
    }

    private void journee(String geste) {
        Fiche f = j.fiche();
        chantierJour = j.jourMaintenant();
        double h = j.heureIci();
        float heures = (float) Math.min(HEURES_CHANTIER, Travail.FERMETURE - h);
        Travail t = j.travail();
        t.reposer();
        t.fatigue = Math.min(Travail.FATIGUE_MAX, t.fatigue + heures * Travail.FATIGUE_HEURE * 0.8f);
        f.apprendre(Fiche.LABEUR, heures * 1.5f);
        f.apprendre(Fiche.ATHLETISME, heures * 0.3f);
        j.gesteJoueur = geste;
        j.passer(heures);
    }

    // ================================================================ Les serviables

    /** Le serviable d'une ville : { nom, féminin (1/0) }, toujours le même. */
    private String[] serviable(int v) {
        Random h = new Random(Travail.melanger(p.graine * 31L + v * 7919L + 17L));
        int sexe = h.nextInt(2);
        return new String[] { Noms.personne(p.villes.peuple[v], p.graine, h.nextLong(), sexe), "" + sexe };
    }

    /** Une mission que personne n'a prise : le serviable de la ville la fait (une fois sur deux). */
    void prise(Missions.Mission m) {
        Random h = new Random(Travail.melanger(m.creee * 13L + m.ville));
        if (h.nextBoolean()) return;
        services.merge(m.ville, 1, Integer::sum);
    }

    /** Au tableau des missions : on parle du serviable de la ville. */
    String texte(int v) {
        int n = services.getOrDefault(v, 0);
        if (n == 0) return "";
        String[] x = serviable(v);
        boolean femme = x[1].equals("1");
        int sien = n * RENOM_SERVICE, mien = Math.round(j.renommee().renom(v));
        String t = "\nSur la place, on parle de " + x[0] + ", qui a déjà rendu " + n + " service" + (n > 1 ? "s" : "") + " : " + (femme ? "elle" : "il") + " a " + sien + " de renom ici, toi " + mien + ".";
        if (mien > sien && n >= 3 && !depasse.getOrDefault(v, false)) {
            depasse.put(v, true);
            j.renommee().gagner(v, 5f, 0f, "tu passes devant " + x[0]);
            t += " On dit maintenant que tu fais mieux qu" + (x[0].matches("^[AEIOUYÉaeiouyé].*") ? "'" : "e ") + x[0] + ".";
        }
        return t;
    }

    // ================================================================ Pour les essais

    public void prisePourEssai(int v, int n) {
        services.merge(v, n, Integer::sum);
    }

    public String textePourEssai(int v) {
        return texte(v);
    }

    public Joueur.Scene ruinePourEssai(int v) {
        Joueur.Scene s = new Joueur.Scene("Essai", "");
        choixRuine(s, v);
        return s;
    }

    // ================================================================ Sauvegarde

    void ecrire(DataOutputStream s) throws IOException {
        s.writeInt(congeSaison);
        s.writeInt(repasJour);
        s.writeInt(chantierJour);
        s.writeInt(services.size());
        for (Map.Entry<Integer, Integer> e : services.entrySet()) {
            s.writeInt(e.getKey());
            s.writeInt(e.getValue());
            s.writeBoolean(depasse.getOrDefault(e.getKey(), false));
        }
    }

    void lire(DataInputStream s) throws IOException {
        congeSaison = s.readInt();
        repasJour = s.readInt();
        chantierJour = s.readInt();
        int n = s.readInt();
        for (int k = 0; k < n; k++) {
            int v = s.readInt();
            services.put(v, s.readInt());
            depasse.put(v, s.readBoolean());
        }
    }
}
