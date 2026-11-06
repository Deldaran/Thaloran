package com.deldaran.politics.histoire;

public class Foule {
    private static final short MORT = Short.MIN_VALUE;
    private static final byte HOMME = 0;
    private static final byte FEMME = 1;
    private static final int TAILLE_DEPART = 16;
    private static final int AGE_MIGRATION_MAX = 35;
    private static final int ECART_CARACTERE = 12;

    public final int region;
    public final int indicePeuple;
    public final Peuple peuple;

    public int vivants = 0;
    public int naissances = 0;
    public int morts = 0;
    public long sommeAgesAuDeces = 0;

    private int taille = 0;
    private int prochainNumero = 0;
    private final long prefixe;
    private long[] id = new long[TAILLE_DEPART];
    private long[] pere = new long[TAILLE_DEPART];
    private long[] mere = new long[TAILLE_DEPART];
    private int[] conjoint = new int[TAILLE_DEPART];
    private short[] naissance = new short[TAILLE_DEPART];
    private byte[] sexe = new byte[TAILLE_DEPART];
    private byte[] loi = new byte[TAILLE_DEPART];
    private byte[] bien = new byte[TAILLE_DEPART];
    private byte[] ambition = new byte[TAILLE_DEPART];
    private byte[] courage = new byte[TAILLE_DEPART];
    private byte[] foi = new byte[TAILLE_DEPART];

    private int[] libres = new int[TAILLE_DEPART];
    private int nombreLibres = 0;

    private int[] meres = new int[TAILLE_DEPART];
    private int[] hommes = new int[TAILLE_DEPART];
    private int[] femmes = new int[TAILLE_DEPART];
    private int nombreMeres, nombreHommes, nombreFemmes;

    private long hasard;

    public int nombrePartants = 0;
    public int[] destination = new int[TAILLE_DEPART];
    public long[] partantId = new long[TAILLE_DEPART];
    public long[] partantPere = new long[TAILLE_DEPART];
    public long[] partantMere = new long[TAILLE_DEPART];
    public short[] partantNaissance = new short[TAILLE_DEPART];
    public byte[] partantSexe = new byte[TAILLE_DEPART];
    public byte[] partantCaractere = new byte[TAILLE_DEPART * 5];
    
    public Foule(int region, int indicePeuple, Peuple peuple, int nombreRegions) {
        this.region = region;
        this.indicePeuple = indicePeuple;
        this.peuple = peuple;
        prefixe = (long) indicePeuple * nombreRegions + region;
    }


    private void agrandir() {
        int n = id.length * 2;
        id = java.util.Arrays.copyOf(id, n);
        pere = java.util.Arrays.copyOf(pere, n);
        mere = java.util.Arrays.copyOf(mere, n);
        conjoint = java.util.Arrays.copyOf(conjoint, n);
        naissance = java.util.Arrays.copyOf(naissance, n);
        sexe = java.util.Arrays.copyOf(sexe, n);
        loi = java.util.Arrays.copyOf(loi, n);
        bien = java.util.Arrays.copyOf(bien, n);
        ambition = java.util.Arrays.copyOf(ambition, n);
        courage = java.util.Arrays.copyOf(courage, n);
        foi = java.util.Arrays.copyOf(foi, n);
        libres = java.util.Arrays.copyOf(libres, n);
        meres = java.util.Arrays.copyOf(meres, n);
        hommes = java.util.Arrays.copyOf(hommes, n);
        femmes = java.util.Arrays.copyOf(femmes, n);
    }

    private void agrandirPartants() {
        int n = destination.length * 2;
        destination = java.util.Arrays.copyOf(destination, n);
        partantId = java.util.Arrays.copyOf(partantId, n);
        partantPere = java.util.Arrays.copyOf(partantPere, n);
        partantMere = java.util.Arrays.copyOf(partantMere, n);
        partantNaissance = java.util.Arrays.copyOf(partantNaissance, n);
        partantSexe = java.util.Arrays.copyOf(partantSexe, n);
        partantCaractere = java.util.Arrays.copyOf(partantCaractere, n * 5);
    }

    private int nouvellePlace() {
        int place;
        if (nombreLibres > 0) {
            place = libres[--nombreLibres];
        } else {
            if (taille == id.length) agrandir();
            place = taille++;
        }
        vivants++;
        return place;
    }

    private long nouveauNumero() {
        return (prefixe << 40) | prochainNumero++;
    }

    private void liberer(int place) {
        naissance[place] = MORT;
        libres[nombreLibres++] = place;
        vivants--;
    }

    public void melanger(long graine, int annee) {
        long z = graine ^ (prefixe * 0x9E3779B97F4A7C15L) ^ (annee * 0xBF58476D1CE4E5B9L);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        hasard = z ^ (z >>> 27);
        if (hasard == 0L) hasard = 1L;
    }

    private float alea() {
        hasard ^= hasard << 13;
        hasard ^= hasard >>> 7;
        hasard ^= hasard << 17;
        return (hasard >>> 40) * 0x1.0p-24f;
    }

    private int entier(int borne) {
        return (int) (alea() * borne);
    }

    private byte heriter(int a, int b) {
        int moyenne = (a + b + (alea() < 0.5f ? 1 : 0)) >> 1;
        int valeur = moyenne + (int) ((alea() + alea() - 1f) * ECART_CARACTERE);
        return (byte) Math.max(-100, Math.min(100, valeur));
    }

    private byte hasardCaractere(float centre) {
        return (byte) Math.max(-100f, Math.min(100f, centre + (alea() + alea() - 1f) * 60f));
    }


    public void fonder(int nombre, short annee, int ageMax) {
        for (int k = 0; k < nombre; k++) {
            int p = nouvellePlace();
            id[p] = nouveauNumero();
            pere[p] = -1L;
            mere[p] = -1L;
            conjoint[p] = -1;
            naissance[p] = (short) (annee - entier((int) (ageMax / peuple.rythme)));
            sexe[p] = (k & 1) == 0 ? HOMME : FEMME;
            loi[p] = hasardCaractere(peuple.loi);
            bien[p] = hasardCaractere(peuple.bien);
            ambition[p] = hasardCaractere(0f);
            courage[p] = hasardCaractere(0f);
            foi[p] = hasardCaractere(0f);

        }
    }

    public void vivreAnnee(int annee, float pression, Population monde) {
        Population.Reglages r = monde.reglages;
        float[] mortalite = r.mortalite;
        float rythme = peuple.rythme;
        float surmortalite = rythme * (pression > 1f ? 1f + r.famine * (pression - 1f) : 1f);
        float natalite = rythme * r.natalite * Math.max(0f, 1f - r.freinNatalite * pression);
        float migration = pression > r.seuilMigration ? peuple.mobilite * r.migration * (pression - r.seuilMigration) : 0f;


        naissances = 0;
        morts = 0;
        sommeAgesAuDeces = 0;
        nombreMeres = nombreHommes = nombreFemmes = 0;
        nombrePartants = 0;

        for (int p = 0; p < taille; p++) {
            short ne = naissance[p];
            if (ne == MORT) continue;
            int vecu = annee - ne;
            int age = (int) (vecu * rythme);
            float risque = mortalite[Math.min(age, mortalite.length - 1)] * surmortalite;
            if (alea() < risque) {
                int c = conjoint[p];
                if (c >= 0) conjoint[c] = -1;
                morts++;
                sommeAgesAuDeces += vecu;
                liberer(p);
                continue;
            }
            if (age < r.ageAdulte) continue;
            if (conjoint[p] >= 0) {
                if (sexe[p] == FEMME && age <= r.ageFertileMax && alea() < natalite) meres[nombreMeres++] = p;
            } else if (age <= AGE_MIGRATION_MAX && alea() < migration) {
                partir(p, monde);
            } else if (sexe[p] == HOMME) {
                hommes[nombreHommes++] = p;
            } else if (age <= r.ageFertileMax) {
                femmes[nombreFemmes++] = p;
            }
        }

        for (int k = 0; k < nombreMeres; k++) {
            int m = meres[k];
            int c = conjoint[m];
            if (c < 0) continue;
            naitre(m, c, annee);
        }
        marier();
    }

    private void naitre(int m, int c, int annee) {
        long idMere = id[m], idPere = id[c];
        byte l = heriter(loi[m], loi[c]), b = heriter(bien[m], bien[c]);
        byte a = heriter(ambition[m], ambition[c]), v = heriter(courage[m], courage[c]), f = heriter(foi[m], foi[c]);
        int p = nouvellePlace();
        id[p] = nouveauNumero();
        pere[p] = idPere;
        mere[p] = idMere;
        conjoint[p] = -1;
        naissance[p] = (short) annee;
        sexe[p] = alea() < 0.5f ? HOMME : FEMME;
        loi[p] = l;
        bien[p] = b;
        ambition[p] = a;
        courage[p] = v;
        foi[p] = f;
        naissances++;
    }

    private void marier() {
        int couples = Math.min(nombreHommes, nombreFemmes);
        for (int k = 0; k < couples; k++) {
            int j = k + entier(nombreFemmes - k);
            int f = femmes[j];
            femmes[j] = femmes[k];
            int h = hommes[k];
            conjoint[h] = f;
            conjoint[f] = h;
        }
    }

    private void partir(int p, Population monde) {
        int vers = monde.choisirDestination(indicePeuple, region, alea(), alea());
        if (vers < 0) return;
        if (nombrePartants == destination.length) agrandirPartants();
        int n = nombrePartants++;
        destination[n] = vers;
        partantId[n] = id[p];
        partantPere[n] = pere[p];
        partantMere[n] = mere[p];
        partantNaissance[n] = naissance[p];
        partantSexe[n] = sexe[p];
        partantCaractere[n * 5] = loi[p];
        partantCaractere[n * 5 + 1] = bien[p];
        partantCaractere[n * 5 + 2] = ambition[p];
        partantCaractere[n * 5 + 3] = courage[p];
        partantCaractere[n * 5 + 4] = foi[p];
        liberer(p);
    }

    public void accueillir(Foule depart, int n) {
        int p = nouvellePlace();
        id[p] = depart.partantId[n];
        pere[p] = depart.partantPere[n];
        mere[p] = depart.partantMere[n];
        conjoint[p] = -1;
        naissance[p] = depart.partantNaissance[n];
        sexe[p] = depart.partantSexe[n];
        loi[p] = depart.partantCaractere[n * 5];
        bien[p] = depart.partantCaractere[n * 5 + 1];
        ambition[p] = depart.partantCaractere[n * 5 + 2];
        courage[p] = depart.partantCaractere[n * 5 + 3];
        foi[p] = depart.partantCaractere[n * 5 + 4];
    }
}
