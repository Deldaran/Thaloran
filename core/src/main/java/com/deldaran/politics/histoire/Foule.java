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
    private short[] lieu = new short[TAILLE_DEPART];
    private int[] maison = new int[TAILLE_DEPART];
    private byte[] rang = new byte[TAILLE_DEPART];
    private byte[] talent = new byte[TAILLE_DEPART];

    private int nombreLieux = 1;
    private int[] villes = new int[4];
    private int[] compte = new int[4];
    private int[] tailles = new int[4];

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
    public byte[] partantCaractere = new byte[TAILLE_DEPART * 6];


    private static final int RANG_MAX = 3;
    private static final int BONUS_NOBLE = 15;
    private static final long CLE_TALENT = 0x7A1E47L;

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
        lieu = java.util.Arrays.copyOf(lieu, n);
        maison = java.util.Arrays.copyOf(maison, n);
        rang = java.util.Arrays.copyOf(rang, n);
        talent = java.util.Arrays.copyOf(talent, n);

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
        partantCaractere = java.util.Arrays.copyOf(partantCaractere, n * 6);

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
        lieu[place] = 0;

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
            maison[p] = -1;
            rang[p] = 0;
            talent[p] = (byte) (Savoir.tirage(id[p], CLE_TALENT) * 100f);
            compte[0]++;
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
        int[] echange = tailles;
        tailles = compte;
        compte = echange;
        java.util.Arrays.fill(compte, 0);
        int urbains = 0;
        for (int l = 1; l < nombreLieux; l++) {
            urbains += tailles[l];
        }
        
        int habitantsRegion = Math.max(1, monde.habitants[region]);
        float partUrbaine = peuple.urbain + monde.economie.importe[region] / habitantsRegion + monde.economie.partCommerce(region, habitantsRegion);
        float exode = urbains > 0 && urbains < partUrbaine * vivants ? rythme * r.exode : 0f;


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
            compte[lieu[p]]++;
            if (age < r.ageAdulte) continue;
            if (conjoint[p] >= 0) {
                if (sexe[p] == FEMME && age <= r.ageFertileMax && alea() < natalite) meres[nombreMeres++] = p;
            } else if (age <= AGE_MIGRATION_MAX && alea() < migration && maison[p] < 0) {
                partir(p, monde);
            } else {
                if (lieu[p] == 0 && alea() < exode) installerEnVille(p, urbains);
                if (sexe[p] == HOMME) {
                    hommes[nombreHommes++] = p;
                } else if (age <= r.ageFertileMax) {
                    femmes[nombreFemmes++] = p;
                }
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

    private void installerEnVille(int p, int urbains) {
        int tirage = entier(urbains);
        for (int l = 1; l < nombreLieux; l++) {
            tirage -= tailles[l];
            if (tirage < 0) {
                compte[0]--;
                compte[l]++;
                lieu[p] = (short) l;
                return;
            }
        }
    }

    private void naitre(int m, int c, int annee) {
        long idMere = id[m], idPere = id[c];
        byte l = heriter(loi[m], loi[c]), b = heriter(bien[m], bien[c]);
        byte a = heriter(ambition[m], ambition[c]), v = heriter(courage[m], courage[c]), f = heriter(foi[m], foi[c]);
        short foyer = lieu[m];
        int maisonEnfant = maison[c] >= 0 ? maison[c] : maison[m];
        int rangEnfant = (maison[c] >= 0 ? rang[c] : rang[m]) + 1;
        if (rangEnfant > RANG_MAX) maisonEnfant = -1;
        int talentParents = (talent[m] + talent[c]) / 2;
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
        lieu[p] = foyer;
        maison[p] = maisonEnfant;
        rang[p] = (byte) (maisonEnfant >= 0 ? rangEnfant : 0);
        int bonus = maisonEnfant >= 0 ? BONUS_NOBLE : 0;
        talent[p] = (byte) Math.min(100, talentParents / 2 + (int) (Savoir.tirage(id[p], CLE_TALENT) * 50f) + bonus);
        compte[foyer]++;
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
            compte[lieu[f]]--;
            compte[lieu[h]]++;
            lieu[f] = lieu[h];

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
        partantCaractere[n * 6] = loi[p];
        partantCaractere[n * 6 + 1] = bien[p];
        partantCaractere[n * 6 + 2] = ambition[p];
        partantCaractere[n * 6 + 3] = courage[p];
        partantCaractere[n * 6 + 4] = foi[p];
        partantCaractere[n * 6 + 5] = talent[p];

        liberer(p);
        compte[lieu[p]]--;
    }

    public void accueillir(Foule depart, int n) {
        int p = nouvellePlace();
        id[p] = depart.partantId[n];
        pere[p] = depart.partantPere[n];
        mere[p] = depart.partantMere[n];
        conjoint[p] = -1;
        naissance[p] = depart.partantNaissance[n];
        sexe[p] = depart.partantSexe[n];
        loi[p] = depart.partantCaractere[n * 6];
        bien[p] = depart.partantCaractere[n * 6 + 1];
        ambition[p] = depart.partantCaractere[n * 6 + 2];
        courage[p] = depart.partantCaractere[n * 6 + 3];
        foi[p] = depart.partantCaractere[n * 6 + 4];
        talent[p] = depart.partantCaractere[n * 6 + 5];
        maison[p] = -1;
        rang[p] = 0;

        compte[0]++;
    }
    
    public boolean doitFonder(int seuil) {
        int actives = 0;
        for (int l = 1; l < nombreLieux; l++) {
            if (compte[l] > 0) actives++;
        }
        return compte[0] > seuil * (1 + actives);
    }

    public void fonderVille(int ville, int colons) {
        if (nombreLieux == villes.length) {
            int n = villes.length * 2;
            villes = java.util.Arrays.copyOf(villes, n);
            compte = java.util.Arrays.copyOf(compte, n);
            tailles = java.util.Arrays.copyOf(tailles, n);
        }
        int l = nombreLieux++;
        villes[l] = ville;
        int restants = colons;
        for (int p = 0; p < taille && restants > 0; p++) {
            if (naissance[p] == MORT || lieu[p] != 0) continue;
            restants -= deplacer(p, l);
            int c = conjoint[p];
            if (c >= 0) restants -= deplacer(c, l);
        }
    }

    private int deplacer(int p, int l) {
        compte[lieu[p]]--;
        compte[l]++;
        lieu[p] = (short) l;
        return 1;
    }

    public void publierVilles(Villes liste, int annee) {
        for (int l = 1; l < nombreLieux; l++) {
            liste.actualiser(villes[l], compte[l], annee);
        }
    }

    public int choisirChef(int ville, int annee, int ageAdulte, int exclue) {

        int lieuVille = -1;
        for (int l = 1; l < nombreLieux; l++) {
            if (villes[l] == ville) lieuVille = l;
        }
        if (lieuVille < 0) return -1;
        int meilleur = -1, score = Integer.MIN_VALUE;
        for (int p = 0; p < taille; p++) {
            if (naissance[p] == MORT || lieu[p] != lieuVille) continue;
            if ((annee - naissance[p]) * peuple.rythme < ageAdulte) continue;
            if (exclue >= 0 && maison[p] == exclue) continue;
            int valeur = ambition[p] + courage[p];
            if (valeur > score) {
                score = valeur;
                meilleur = p;
            }
        }
        return meilleur;
    }

    public int habitantAuHasard(int ville, int annee, int ageAdulte, long tirage) {
        if (taille == 0) return -1;
        int lieuVoulu = 0;
        if (ville >= 0) {
            lieuVoulu = -1;
            for (int l = 1; l < nombreLieux; l++) {
                if (villes[l] == ville) lieuVoulu = l;
            }
            if (lieuVoulu < 0) return -1;
        }
        int depart = (int) ((tirage >>> 1) % taille);
        for (int k = 0; k < taille; k++) {
            int p = (depart + k) % taille;
            if (naissance[p] == MORT || lieu[p] != lieuVoulu) continue;
            if ((annee - naissance[p]) * peuple.rythme >= ageAdulte) return p;
        }
        return -1;
    }

    public int villeDe(int place) {
        return lieu[place] == 0 ? -1 : villes[lieu[place]];
    }

    public short naissanceDe(int place) {
        return naissance[place];
    }

    public int choisirHeritier(int lignee, int annee, int ageAdulte) {
        int meilleur = -1;
        long score = Long.MIN_VALUE;
        for (int p = 0; p < taille; p++) {
            if (naissance[p] == MORT || maison[p] != lignee) continue;
            int age = annee - naissance[p];
            long valeur;
            if (peuple.succession == Peuple.FORT) {
                if (age * peuple.rythme < ageAdulte) continue;
                valeur = ambition[p] + courage[p];
            } else {
                valeur = age - rang[p] * 100000L;
                if (peuple.succession == Peuple.MALE && sexe[p] == HOMME) valeur += 50000L;
            }
            if (valeur > score) {
                score = valeur;
                meilleur = p;
            }
        }
        return meilleur;
    }

    public int chercherPretendant(int lignee, int heritier, int annee, int ageAdulte) {
        int meilleur = -1, score = ambition[heritier];
        for (int p = 0; p < taille; p++) {
            if (p == heritier || naissance[p] == MORT || maison[p] != lignee) continue;
            if ((annee - naissance[p]) * peuple.rythme < ageAdulte) continue;
            if (ambition[p] > score) {
                score = ambition[p];
                meilleur = p;
            }
        }
        return meilleur;
    }

    public void couronner(int place, int lignee) {
        long numero = id[place];
        maison[place] = lignee;
        rang[place] = 0;
        for (int p = 0; p < taille; p++) {
            if (naissance[p] == MORT || (pere[p] != numero && mere[p] != numero)) continue;
            maison[p] = lignee;
            rang[p] = 1;
        }
    }

    public boolean estMineur(int place, int annee, int ageAdulte) {
        return (annee - naissance[place]) * peuple.rythme < ageAdulte;
    }

    public boolean estEnfantDe(int place, long numero) {
        return pere[place] == numero || mere[place] == numero;
    }

    public int maisonDe(int place) {
        return maison[place];
    }

    public int sexeDe(int place) {
        return sexe[place];
    }

    public int talentDe(int place) {
        return talent[place];
    }

    public int nobles() {
        int total = 0;
        for (int p = 0; p < taille; p++) {
            if (naissance[p] != MORT && maison[p] >= 0) total++;
        }
        return total;
    }

    public boolean estEnVie(int place, long numero) {
        return naissance[place] != MORT && id[place] == numero;
    }

    public long idDe(int place) {
        return id[place];
    }

    public byte ambitionDe(int place) {
        return ambition[place];
    }

    public byte courageDe(int place) {
        return courage[place];
    }

    public byte loiDe(int place) {
        return loi[place];
    }

    public byte bienDe(int place) {
        return bien[place];
    }

    private void mourir(int p, int annee) {
        int c = conjoint[p];
        if (c >= 0) conjoint[c] = -1;
        morts++;
        sommeAgesAuDeces += annee - naissance[p];
        compte[lieu[p]]--;
        liberer(p);
    }

    public void executer(int place, int annee) {
        if (naissance[place] != MORT) mourir(place, annee);
    }

    public int tuerSoldats(int nombre, int annee, int ageAdulte) {
        if (taille == 0 || nombre <= 0) return 0;
        int tues = 0;
        int depart = entier(taille);
        for (int k = 0; k < taille && tues < nombre; k++) {
            int p = (depart + k) % taille;
            if (naissance[p] == MORT || sexe[p] != HOMME) continue;
            if ((annee - naissance[p]) * peuple.rythme < ageAdulte) continue;
            mourir(p, annee);
            tues++;
        }
        return tues;
    }

    public int saccager(int ville, float part, int annee) {
        int lieuVille = -1;
        for (int l = 1; l < nombreLieux; l++) {
            if (villes[l] == ville) lieuVille = l;
        }
        if (lieuVille < 0) return 0;
        int tues = 0;
        for (int p = 0; p < taille; p++) {
            if (naissance[p] == MORT || lieu[p] != lieuVille || alea() >= part) continue;
            mourir(p, annee);
            tues++;
        }
        return tues;
    }

    public int urbains() {
        return vivants - compte[0];
    }
    
    public void archiver(Archive a) throws java.io.IOException {
        vivants = a.entier(vivants);
        taille = a.entier(taille);
        prochainNumero = a.entier(prochainNumero);
        nombreLieux = a.entier(nombreLieux);
        nombreLibres = a.entier(nombreLibres);
        id = a.longs(id, taille);
        pere = a.longs(pere, taille);
        mere = a.longs(mere, taille);
        conjoint = a.entiers(conjoint, taille);
        naissance = a.courts(naissance, taille);
        sexe = a.octets(sexe, taille);
        loi = a.octets(loi, taille);
        bien = a.octets(bien, taille);
        ambition = a.octets(ambition, taille);
        courage = a.octets(courage, taille);
        foi = a.octets(foi, taille);
        lieu = a.courts(lieu, taille);
        maison = a.entiers(maison, taille);
        rang = a.octets(rang, taille);
        talent = a.octets(talent, taille);
        villes = a.entiers(villes, villes.length);
        compte = a.entiers(compte, compte.length);
        tailles = a.entiers(tailles, tailles.length);
        libres = a.entiers(libres, nombreLibres);
        meres = java.util.Arrays.copyOf(meres, id.length);
        hommes = java.util.Arrays.copyOf(hommes, id.length);
        femmes = java.util.Arrays.copyOf(femmes, id.length);
    }

}
