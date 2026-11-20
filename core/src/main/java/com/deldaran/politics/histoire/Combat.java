package com.deldaran.politics.histoire;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Phase 9 : la résolution automatique d'un combat de groupe, à la manière de Bannerlord.
 *
 * Chaque tour, chaque combattant encore debout frappe un adversaire debout. La chance de toucher dépend de l'écart de force
 * (compétence, arme, Tactique du chef) ; les dégâts, de cet écart et de l'armure du défenseur. À zéro, on tombe : à terre
 * (assommé, blessé) le plus souvent, mort parfois ; le joueur, lui, n'est qu'assommé. Chaque perte fait baisser le moral du camp ;
 * sous MORAL_FUITE, les survivants de ce camp s'enfuient. Le combat garde la liste des coups : l'écran les rejoue avec les animations.
 */
public final class Combat {
    public static final int DEBOUT = 0, A_TERRE = 1, MORT = 2, FUI = 3;
    public static final int NOUS = 0, EUX = 1;
    /** Chance de toucher : 0,5 + écart de force ÷ ECART_TOUCHE, bornée entre TOUCHE_MIN et TOUCHE_MAX. */
    public static final float ECART_TOUCHE = 150f, TOUCHE_MIN = 0.15f, TOUCHE_MAX = 0.85f;
    /** Dégâts d'un coup (la santé va de 1 à 0) : DEGAT_BASE + DEGAT_HASARD × hasard, puis × (1 + écart ÷ 150) × (1 − PROTECTION × armure). */
    public static final float DEGAT_BASE = 0.22f, DEGAT_HASARD = 0.2f, PROTECTION = 0.05f;
    /** Chance de n'être qu'à terre (sinon mort) quand on tombe, pour la bande du joueur et pour l'adversaire. */
    public static final float SURVIE_NOUS = 0.75f, SURVIE_EUX = 0.6f;
    /** Perte de moral d'un camp à chaque homme tombé (proportionnelle à la taille du camp), seuil de fuite. */
    public static final float MORAL_PERTE = 45f, MORAL_FUITE = 22f;
    public static final int TOURS = 40;

    public static final class Combattant {
        public String nom = "";
        public int camp, etat = DEBOUT;
        public long graine;
        public boolean joueur, femme;
        public int peuple, arme = -1, armure = -1;
        public float force, sante = 1f;
        /** Index du brigand dans son repaire, ou -1. */
        public int brigand = -1;
    }

    public static final class Coup {
        public int attaquant, defenseur;
        public boolean touche;
        /** État du défenseur après le coup. */
        public int etat;
    }

    public static final class Resultat {
        public final List<Combattant> tous = new ArrayList<>();
        public final List<Coup> coups = new ArrayList<>();
        /** NOUS, EUX, ou -1 si personne n'a gagné (tours épuisés). */
        public int vainqueur = -1;
        /** Les ennemis se sont rendus sans combattre. */
        public boolean reddition;
        public int repaire = -1;

        public int compter(int camp, int etat) {
            int n = 0;
            for (Combattant c : tous) if (c.camp == camp && c.etat == etat) n++;
            return n;
        }
    }

    /** La force d'un combattant armé : sa valeur, plus 4 par point de force de l'arme. */
    public static float force(float base, int arme) {
        Biens.Bien b = arme >= 0 ? Biens.get(arme) : null;
        return base + (b != null ? 4f * b.valeur : 0f);
    }

    public static int protection(int armure) {
        Biens.Bien b = armure >= 0 ? Biens.get(armure) : null;
        return b != null ? b.valeur : 0;
    }

    /** La force du joueur : sa compétence dans l'arme qu'il porte, sa Vigueur, l'arme elle-même. */
    public static float forceJoueur(Fiche f, int arme) {
        Biens.Bien b = arme >= 0 ? Biens.get(arme) : null;
        int competence = b != null && b.competence >= 0 ? f.competences[b.competence] : f.competences[Fiche.ATHLETISME] / 2;
        return force(15f + competence / 4f + 2f * f.attributs[Fiche.VIGUEUR], arme);
    }

    /** La meilleure arme et la meilleure armure parmi les objets portés (ou -1). */
    public static int[] equipement(int[] portes) {
        int arme = -1, armure = -1;
        if (portes != null) for (int id : portes) {
            Biens.Bien b = Biens.get(id);
            if (b == null) continue;
            if (b.categorie == Biens.ARME && b.valeur > 0 && b.place == Biens.MAIN && (arme < 0 || b.valeur > Biens.get(arme).valeur)) arme = id;
            if (b.categorie == Biens.ARMURE && b.place == Biens.CORPS && (armure < 0 || b.valeur > Biens.get(armure).valeur)) armure = id;
        }
        return new int[] { arme, armure };
    }

    public static Resultat resoudre(List<Combattant> combattants, float tactiqueNous, float tactiqueEux, float moralNous, float moralEux, long graine) {
        Resultat r = new Resultat();
        r.tous.addAll(combattants);
        Random h = new Random(graine);
        float[] tactique = { tactiqueNous / 10f, tactiqueEux / 10f };
        float[] moral = { moralNous, moralEux };
        int[] effectif = new int[2];
        for (Combattant c : r.tous) effectif[c.camp]++;
        if (effectif[NOUS] == 0 || effectif[EUX] == 0) {
            r.vainqueur = effectif[NOUS] > 0 ? NOUS : EUX;
            return r;
        }
        List<Integer> ordre = new ArrayList<>();
        List<Integer> cibles = new ArrayList<>();
        for (int tour = 0; tour < TOURS && r.vainqueur < 0; tour++) {
            ordre.clear();
            for (int k = 0; k < r.tous.size(); k++) if (r.tous.get(k).etat == DEBOUT) ordre.add(k);
            Collections.shuffle(ordre, h);
            for (int k : ordre) {
                Combattant a = r.tous.get(k);
                if (a.etat != DEBOUT) continue;
                cibles.clear();
                for (int q = 0; q < r.tous.size(); q++) if (r.tous.get(q).camp != a.camp && r.tous.get(q).etat == DEBOUT) cibles.add(q);
                if (cibles.isEmpty()) break;
                int qd = cibles.get(h.nextInt(cibles.size()));
                Combattant d = r.tous.get(qd);
                float ecart = (a.force + tactique[a.camp]) - (d.force + tactique[d.camp]);
                float p = Math.max(TOUCHE_MIN, Math.min(TOUCHE_MAX, 0.5f + ecart / ECART_TOUCHE));
                Coup c = new Coup();
                c.attaquant = k;
                c.defenseur = qd;
                c.touche = h.nextFloat() < p;
                if (c.touche) {
                    float degat = (DEGAT_BASE + DEGAT_HASARD * h.nextFloat()) * Math.max(0.4f, 1f + ecart / 150f) * Math.max(0.3f, 1f - PROTECTION * protection(d.armure));
                    d.sante -= degat;
                    if (d.sante <= 0f) {
                        d.sante = 0f;
                        float survie = d.camp == NOUS ? SURVIE_NOUS : SURVIE_EUX;
                        d.etat = d.joueur || h.nextFloat() < survie ? A_TERRE : MORT;
                        moral[d.camp] -= MORAL_PERTE / effectif[d.camp] * (d.joueur ? 2f : 1f);
                    }
                }
                c.etat = d.etat;
                r.coups.add(c);
            }
            // Le moral d'un camp s'effondre : ses survivants s'enfuient.
            for (int camp = 0; camp < 2; camp++) {
                if (moral[camp] >= MORAL_FUITE) continue;
                for (int q = 0; q < r.tous.size(); q++) {
                    Combattant c = r.tous.get(q);
                    if (c.camp != camp || c.etat != DEBOUT) continue;
                    c.etat = FUI;
                    Coup fuite = new Coup();
                    fuite.attaquant = q;
                    fuite.defenseur = q;
                    fuite.etat = FUI;
                    r.coups.add(fuite);
                }
            }
            int debout0 = 0, debout1 = 0;
            for (Combattant c : r.tous) if (c.etat == DEBOUT) {
                if (c.camp == NOUS) debout0++;
                else debout1++;
            }
            if (debout1 == 0) r.vainqueur = NOUS;
            else if (debout0 == 0) r.vainqueur = EUX;
        }
        return r;
    }
}
