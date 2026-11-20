package com.deldaran.politics;

/**
 * Lot H4 : la journée de chacun, choisie par ses besoins, à la Sims.
 *
 * Chaque villageois a des besoins qui montent avec le temps (0 = comblé, 1 = pressant) :
 * - la FAIM monte en FAIM_HEURES heures (trois fois moins vite en dormant) ; un repas la comble ;
 * - le SOMMEIL monte en SOMMEIL_EVEIL heures d'éveil ; une nuit chez soi le comble en SOMMEIL_NUIT heures ;
 * - l'ENVIE (de se distraire) monte en ENVIE_TRAVAIL heures de travail, plus lentement sinon ; les loisirs la comblent ;
 * - le BESOIN D'ARGENT : la bourse comparée à RESERVE_JOURS jours de salaire.
 * Chaque activité reçoit un score d'après ces besoins et l'heure (ses heures de travail, sa nuit) ; la plus forte l'emporte, avec un
 * petit avantage (GARDER) pour celle qu'on fait déjà. Personne n'a d'horaire écrit : le boulanger, qui commence à 3 h, se couche tôt
 * parce que son sommeil monte plus tôt ; un pauvre travaille plus tard ; un riche mange à la taverne.
 */
final class Besoins {
    static final float FAIM_HEURES = 8f, FAIM_SOMMEIL = 0.33f, SOMMEIL_EVEIL = 16f, SOMMEIL_NUIT = 7f, ENVIE_TRAVAIL = 8f, ENVIE_REPOS = 24f, ENVIE_LOISIR = 0.8f;
    /** On ne va manger qu'au-dessus de FAIM_MIN de faim. */
    static final float FAIM_MIN = 0.5f;
    /** L'avantage de l'activité en cours (évite d'en changer sans cesse). */
    static final float GARDER = 0.15f;
    /** Ce que coûtent un repas à la boulangerie, un repas à la taverne, une bière (en cuivre). Chez soi, on mange ses vivres. */
    static final int REPAS_BOUTIQUE = 1, REPAS_TAVERNE = 3, BIERE = 2;
    /** La réserve qui rassure : RESERVE_JOURS journées de HEURES_JOUR heures de salaire. */
    static final float RESERVE_JOURS = 10f, HEURES_JOUR = 8f;
    /** Assez riche pour la taverne quand l'envie dépasse ENVIE_TAVERNE. */
    static final float ENVIE_TAVERNE = 0.5f;

    private Besoins() {
    }

    static boolean dans(double h, double debut, double fin) {
        if (debut <= fin) return h >= debut && h < fin;
        return h >= debut || h < fin;
    }

    /** 0 quand la bourse tient RESERVE_JOURS jours, 1 quand elle est vide. */
    static float pauvrete(Villageois w) {
        float reserve = Math.max(5f, w.salaire * HEURES_JOUR * RESERVE_JOURS);
        return Math.max(0f, Math.min(1f, 1f - w.bourse / reserve));
    }

    /** L'activité que ses besoins choisissent, à l'heure h (déjà décalée pour lui). */
    static int choisir(Villageois w, double h) {
        int prec = w.activite;
        boolean nuit = !dans(h, w.lever, w.coucher);
        boolean heuresTravail = !w.enfant && !w.vieux && dans(h, w.debut, w.fin);
        float pauvre = pauvrete(w);
        float[] s = new float[Villageois.MORT + 1];
        java.util.Arrays.fill(s, -9f);
        s[Villageois.DORMIR] = nuit ? w.sommeil * 1.2f + 1f : w.sommeil * w.sommeil * 1.2f - 0.6f;
        if (w.faim >= FAIM_MIN) s[Villageois.MANGER] = w.faim * 1.3f;
        if (w.enfant) s[Villageois.JOUER] = nuit ? -1f : 0.55f;
        else if (!w.vieux) s[Villageois.TRAVAIL] = heuresTravail ? 0.75f + pauvre * 0.3f - w.fatigue * 0.15f : pauvre > 0.8f && !nuit ? 0.3f : -1f;
        s[Villageois.LOISIR] = (w.vieux ? 0.35f : 0.15f) + w.envie * 0.6f + w.social * 0.3f + (h >= 18.0 && h < 22.0 ? 0.2f : 0f);
        if (prec == Villageois.TRAVAIL && w.fatigue > 0.85f) s[Villageois.PAUSE] = 1.2f;
        if (prec == Villageois.REVEIL) prec = w.enfant ? Villageois.JOUER : Villageois.TRAVAIL;
        if (prec >= 0 && prec < s.length && s[prec] > -9f) s[prec] += GARDER;
        int meilleure = Villageois.LOISIR;
        for (int a = 0; a < s.length; a++) if (s[a] > s[meilleure]) meilleure = a;
        return meilleure;
    }

    /** Les besoins qui montent ou descendent pendant ces heures de jeu ; le salaire qui s'accumule au travail. */
    static void user(Villageois w, float heures) {
        if (w.dedans && w.activite == Villageois.DORMIR) w.sommeil = Math.max(0f, w.sommeil - heures / SOMMEIL_NUIT);
        else w.sommeil = Math.min(1f, w.sommeil + heures / SOMMEIL_EVEIL);
        boolean aLOuvrage = w.activite == Villageois.TRAVAIL && !w.enRoute && !w.dedans;
        if (aLOuvrage) w.envie = Math.min(1f, w.envie + heures / ENVIE_TRAVAIL);
        else if (w.activite == Villageois.LOISIR || w.activite == Villageois.JOUER || w.activite == Villageois.DISCUTER) w.envie = Math.max(0f, w.envie - heures * ENVIE_LOISIR);
        else w.envie = Math.min(1f, w.envie + heures / ENVIE_REPOS);
        if (aLOuvrage) w.gain += heures * w.salaire;
        if (aLOuvrage && w.ouvrier) w.ouvrage += heures;
    }

    /** L'état d'un habitant qui prend corps à l'heure h : levé depuis un moment, ayant mangé à ses heures. */
    static void initialiser(Villageois w, double h) {
        h = ((h % 24.0) + 24.0) % 24.0;
        double eveil = h - w.lever;
        if (eveil < 0) eveil += 24.0;
        w.sommeil = dans(h, w.lever, w.coucher) ? (float) Math.min(1.0, eveil / SOMMEIL_EVEIL) : 0.5f;
        double depuisRepas = Math.min(Math.min(ecart(h, w.lever + 1.0), ecart(h, 12.5)), ecart(h, 19.0));
        w.faim = (float) Math.min(1.0, depuisRepas / FAIM_HEURES);
        w.envie = 0.2f + w.hasard.nextFloat() * 0.3f;
    }

    private static double ecart(double h, double repas) {
        double e = h - repas;
        return e < 0 ? e + 24.0 : e;
    }

    /** Les heures d'un habitant selon son travail : il se lève une heure avant d'ouvrir (entre 3 h et 8 h) et veille seize heures. */
    static void horaires(Villageois w, int debut, int fin) {
        w.debut = debut;
        w.fin = fin;
        w.lever = w.enfant || w.vieux ? 7f : Math.max(3f, Math.min(8f, debut - 1f));
        w.coucher = w.enfant ? 20f : w.lever + 16f >= 24f ? w.lever + 16f - 24f : w.lever + 16f;
    }
}
