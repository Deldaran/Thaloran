package com.deldaran.politics.histoire;

public class Etats {
    public static final short VIVANT = Short.MIN_VALUE;
    private static final int TAILLE_DEPART = 64;

    public static class Reglages {
        public int seuilFondation = 2500;
        public float partArmee = 0.03f;
        public float recrutement = 0.1f;
        public float impot = 0.1f;
        public float impotCommerce = 0.02f;
        public float solde = 1f;
        public float tresorMax = 50f;
        public int regionsDeBase = 2;
        public float regionsParMille = 1f;
        public float annexion = 0.1f;
        public float envieGuerre = 0.02f;
        public float rapportForce = 1.2f;
        public float bonusDefense = 1.2f;
        public float siege = 2f;
        public float perteBataille = 0.08f;
        public float sac = 0.2f;
        public int dureeGuerre = 20;
        public float armeeBrisee = 0.3f;
        public float legitimiteDepart = 60f;
        public float seuilSecession = 35f;
        public float seuilRevolution = 20f;
        public float chanceSecession = 0.02f;
        public float chanceRevolution = 0.1f;
        public float usurpation = 0.1f;

    }

    public final Reglages reglages;
    public final Chronique chronique = new Chronique();
    public final int[] regionEtat;

    public volatile int nombre = 0;
    public int chutes = 0;
    public int guerres = 0;
    public int revolutions = 0;
    public int crises = 0;
    public int regences = 0;
    public int usurpations = 0;
    public int heritages = 0;
    public long mortsAuCombat = 0;
    public long mortsDesSacs = 0;

    public byte[] peuple = new byte[TAILLE_DEPART];
    public int[] capitale = new int[TAILLE_DEPART];
    public int[] regionCapitale = new int[TAILLE_DEPART];
    public short[] fondation = new short[TAILLE_DEPART];
    public short[] fin = new short[TAILLE_DEPART];
    public long[] chef = new long[TAILLE_DEPART];
    public short[] debutRegne = new short[TAILLE_DEPART];
    public byte[] chefAmbition = new byte[TAILLE_DEPART];
    public byte[] chefCourage = new byte[TAILLE_DEPART];
    public byte[] chefLoi = new byte[TAILLE_DEPART];
    public byte[] chefBien = new byte[TAILLE_DEPART];
    public float[] legitimite = new float[TAILLE_DEPART];
    public float[] soldats = new float[TAILLE_DEPART];
    public float[] tresor = new float[TAILLE_DEPART];
    public int[] tenues = new int[TAILLE_DEPART];
    public int[] sujets = new int[TAILLE_DEPART];
    public int[] tenuesMax = new int[TAILLE_DEPART];
    public int[] ennemi = new int[TAILLE_DEPART];
    public int[] maison = new int[TAILLE_DEPART];
    public int[] regne = new int[TAILLE_DEPART];

    private int[] slotChef = new int[TAILLE_DEPART];
    private float[] revenu = new float[TAILLE_DEPART];
    private float[] armeeDeDepart = new float[TAILLE_DEPART];
    private short[] debutGuerre = new short[TAILLE_DEPART];
    private boolean[] attaquant = new boolean[TAILLE_DEPART];
    private int[] voisinFaible = new int[TAILLE_DEPART];
    private int[] cible = new int[TAILLE_DEPART];
    private int[] origine = new int[TAILLE_DEPART];

    private final Regions regions;
    private final int[] plusGrandeVille;
    private long hasard;

    public Etats(Regions regions, Reglages reglages, long graine) {
        this.regions = regions;
        this.reglages = reglages;
        regionEtat = new int[regions.nombre];
        java.util.Arrays.fill(regionEtat, -1);
        plusGrandeVille = new int[regions.nombre];
        hasard = graine * 0x9E3779B97F4A7C15L + 0x5851F42D4C957F2DL;
        if (hasard == 0L) hasard = 1L;
    }

    private float alea() {
        hasard ^= hasard << 13;
        hasard ^= hasard >>> 7;
        hasard ^= hasard << 17;
        return (hasard >>> 40) * 0x1.0p-24f;
    }

    public boolean estVivant(int e) {
        return fin[e] == VIVANT;
    }

    private void agrandir() {
        int n = peuple.length * 2;
        peuple = java.util.Arrays.copyOf(peuple, n);
        capitale = java.util.Arrays.copyOf(capitale, n);
        regionCapitale = java.util.Arrays.copyOf(regionCapitale, n);
        fondation = java.util.Arrays.copyOf(fondation, n);
        fin = java.util.Arrays.copyOf(fin, n);
        chef = java.util.Arrays.copyOf(chef, n);
        debutRegne = java.util.Arrays.copyOf(debutRegne, n);
        chefAmbition = java.util.Arrays.copyOf(chefAmbition, n);
        chefCourage = java.util.Arrays.copyOf(chefCourage, n);
        chefLoi = java.util.Arrays.copyOf(chefLoi, n);
        chefBien = java.util.Arrays.copyOf(chefBien, n);
        legitimite = java.util.Arrays.copyOf(legitimite, n);
        soldats = java.util.Arrays.copyOf(soldats, n);
        tresor = java.util.Arrays.copyOf(tresor, n);
        tenues = java.util.Arrays.copyOf(tenues, n);
        sujets = java.util.Arrays.copyOf(sujets, n);
        tenuesMax = java.util.Arrays.copyOf(tenuesMax, n);
        ennemi = java.util.Arrays.copyOf(ennemi, n);
        maison = java.util.Arrays.copyOf(maison, n);
        regne = java.util.Arrays.copyOf(regne, n);
        slotChef = java.util.Arrays.copyOf(slotChef, n);
        revenu = java.util.Arrays.copyOf(revenu, n);
        armeeDeDepart = java.util.Arrays.copyOf(armeeDeDepart, n);
        debutGuerre = java.util.Arrays.copyOf(debutGuerre, n);
        attaquant = java.util.Arrays.copyOf(attaquant, n);
        voisinFaible = java.util.Arrays.copyOf(voisinFaible, n);
        cible = java.util.Arrays.copyOf(cible, n);
        origine = java.util.Arrays.copyOf(origine, n);
    }

    public void avancer(Population p, int annee) {
        recenser(p);
        chercherVilles(p);
        fonderNouveaux(p, annee);
        for (int e = 0; e < nombre; e++) {
            if (!estVivant(e)) continue;
            verifierChef(p, e, annee);
            if (estVivant(e)) contester(p, e, annee);
        }
        explorerFrontieres(p);
        for (int e = 0; e < nombre; e++) {
            if (!estVivant(e)) continue;
            if (ennemi[e] < 0) declarer(e, annee, p);
        }
        for (int e = 0; e < nombre; e++) {
            if (estVivant(e) && ennemi[e] >= 0 && attaquant[e]) combattre(p, e, annee);
        }
    }

    private void recenser(Population p) {
        for (int e = 0; e < nombre; e++) {
            tenues[e] = 0;
            sujets[e] = 0;
            revenu[e] = 0f;
        }
        for (int r = 0; r < regions.nombre; r++) {
            int e = regionEtat[r];
            if (e < 0) continue;
            tenues[e]++;
            sujets[e] += p.habitants[r];
            revenu[e] += p.habitants[r] * reglages.impot + p.economie.trafic[r] * reglages.impotCommerce;
        }
        for (int e = 0; e < nombre; e++) {
            if (!estVivant(e)) continue;
            tenuesMax[e] = Math.max(tenuesMax[e], tenues[e]);
            boolean enGuerre = ennemi[e] >= 0;
            float voulus = sujets[e] * reglages.partArmee;
            soldats[e] += (voulus - soldats[e]) * reglages.recrutement * (enGuerre ? 0.3f : 1f);
            tresor[e] = Math.min(revenu[e] * reglages.tresorMax, tresor[e] + revenu[e] - soldats[e] * reglages.solde * (enGuerre ? 3f : 1f));
            float confiance = enGuerre ? -1f : 0.5f;
            float exces = (float) tenues[e] / limite(p, e) - 1f;
            if (exces > 0f) confiance -= 2f * exces;
            if (chefBien[e] < -30) confiance -= 0.2f;
            legitimite[e] = Math.max(0f, Math.min(100f, legitimite[e] + confiance));
        }
    }

    private float limite(Population p, int e) {
        int habitantsCapitale = p.villes.habitants[capitale[e]];
        return reglages.regionsDeBase + habitantsCapitale / 1000f * reglages.regionsParMille * (1f + chefAmbition[e] / 200f);
    }

    private void chercherVilles(Population p) {
        java.util.Arrays.fill(plusGrandeVille, -1);
        Villes v = p.villes;
        for (int i = 0; i < v.nombre; i++) {
            if (!v.estVivante(i)) continue;
            int r = v.region[i];
            if (plusGrandeVille[r] < 0 || v.habitants[i] > v.habitants[plusGrandeVille[r]]) plusGrandeVille[r] = i;
        }
    }

    private void fonderNouveaux(Population p, int annee) {
        for (int r = 0; r < regions.nombre; r++) {
            int ville = plusGrandeVille[r];
            if (ville < 0 || regionEtat[r] >= 0 || p.villes.habitants[ville] < reglages.seuilFondation) continue;
            int e = creer(p, ville, annee);
            if (e >= 0) chronique.noter(annee, Chronique.FONDATION, e, -1, r, chef[e]);
        }
    }

    private int creer(Population p, int ville, int annee) {
        int r = p.villes.region[ville];
        int q = p.villes.peuple[ville];
        Foule f = p.foules[q][r];
        int place = f.choisirChef(ville, annee, p.reglages.ageAdulte, -1);
        if (place < 0) return -1;
        int e = nombre;
        if (e == peuple.length) agrandir();
        peuple[e] = (byte) q;
        capitale[e] = ville;
        regionCapitale[e] = r;
        fondation[e] = (short) annee;
        fin[e] = VIVANT;
        ennemi[e] = -1;
        legitimite[e] = reglages.legitimiteDepart;
        regne[e] = -1;
        introniser(p, f, e, place, annee);
        regionEtat[r] = e;
        tenues[e] = 1;
        sujets[e] = p.habitants[r];
        soldats[e] = p.habitants[r] * reglages.partArmee * 0.5f;
        nombre = e + 1;
        return e;
    }

    
    private void introniser(Population p, Foule f, int e, int place, int annee) {
        long ancien = chef[e];
        int avant = regne[e];
        p.maisons.finir(avant, annee);
        byte lien = Maisons.ETRANGER;
        int lignee = f.maisonDe(place);
        if (avant >= 0 && f.estEnfantDe(place, ancien)) lien = Maisons.ENFANT;
        else if (avant >= 0 && lignee >= 0 && lignee == maison[e]) lien = Maisons.PARENT;
        if (lignee < 0) lignee = p.maisons.fonder(peuple[e], f.idDe(place), annee, e);
        maison[e] = lignee;
        f.couronner(place, lignee);
        regne[e] = p.maisons.commencer(f.idDe(place), f.sexeDe(place), lignee, e, annee, avant, lien);
        slotChef[e] = place;
        chef[e] = f.idDe(place);

        debutRegne[e] = (short) annee;
        chefAmbition[e] = f.ambitionDe(place);
        chefCourage[e] = f.courageDe(place);
        chefLoi[e] = f.loiDe(place);
        chefBien[e] = f.bienDe(place);
    }

        private void verifierChef(Population p, int e, int annee) {
        Foule f = p.foules[peuple[e]][regionCapitale[e]];
        if (f.estEnVie(slotChef[e], chef[e])) return;
        long ancien = chef[e];
        int adulte = p.reglages.ageAdulte;
        int heritier = f.choisirHeritier(maison[e], annee, adulte);
        if (heritier >= 0) {
            introniser(p, f, e, heritier, annee);
            heritages++;
            legitimite[e] = Math.max(0f, legitimite[e] - 2f);
            chronique.noter(annee, Chronique.SUCCESSION, e, -1, regionCapitale[e], chef[e]);
            if (f.estMineur(heritier, annee, adulte)) {
                regences++;
                legitimite[e] = Math.max(0f, legitimite[e] - 5f);
                chronique.noter(annee, Chronique.REGENCE, e, -1, regionCapitale[e], chef[e]);
            }
            usurper(p, f, e, heritier, annee);
            return;
        }
        int place = f.choisirChef(capitale[e], annee, adulte, -1);
        if (place < 0) {
            chronique.noter(annee, Chronique.EXTINCTION, e, -1, regionCapitale[e], ancien);
            p.maisons.finir(regne[e], annee);
            tomber(e, annee);
            return;
        }
        introniser(p, f, e, place, annee);
        crises++;
        legitimite[e] = Math.max(0f, legitimite[e] - 10f);
        chronique.noter(annee, Chronique.CRISE, e, -1, regionCapitale[e], chef[e]);
    }

    private void usurper(Population p, Foule f, int e, int heritier, int annee) {
        int rival = f.chercherPretendant(maison[e], heritier, annee, p.reglages.ageAdulte);
        if (rival < 0 || alea() >= reglages.usurpation) return;
        f.executer(heritier, annee);
        introniser(p, f, e, rival, annee);
        usurpations++;
        legitimite[e] = Math.max(0f, legitimite[e] - 15f);
        chronique.noter(annee, Chronique.USURPATION, e, -1, -1, chef[e]);
    }


    private void contester(Population p, int e, int annee) {
        if (legitimite[e] < reglages.seuilRevolution && alea() < reglages.chanceRevolution) {
            Foule f = p.foules[peuple[e]][regionCapitale[e]];
            long tyran = chef[e];
            f.executer(slotChef[e], annee);
            int place = f.choisirChef(capitale[e], annee, p.reglages.ageAdulte, maison[e]);
            chronique.noter(annee, Chronique.REVOLUTION, e, -1, regionCapitale[e], tyran);
            revolutions++;
            if (place < 0) {
                p.maisons.finir(regne[e], annee);
                tomber(e, annee);
                return;
            }
            introniser(p, f, e, place, annee);
            legitimite[e] = reglages.legitimiteDepart;
            return;
        }
        if (legitimite[e] >= reglages.seuilSecession) return;
        for (int r = 0; r < regions.nombre; r++) {
            if (regionEtat[r] != e || r == regionCapitale[e] || plusGrandeVille[r] < 0) continue;
            if (p.villes.habitants[plusGrandeVille[r]] < reglages.seuilFondation / 2 || alea() >= reglages.chanceSecession) continue;
            regionEtat[r] = -1;
            int nouveau = creer(p, plusGrandeVille[r], annee);
            if (nouveau >= 0) chronique.noter(annee, Chronique.SECESSION, nouveau, e, r, chef[nouveau]);
        }
    }

    private void explorerFrontieres(Population p) {
        for (int e = 0; e < nombre; e++) {
            voisinFaible[e] = -1;
            cible[e] = -1;
            origine[e] = -1;
        }
        for (int r = 0; r < regions.nombre; r++) {
            int e = regionEtat[r];
            if (e < 0) continue;
            for (int k = regions.debutVoisins[r]; k < regions.debutVoisins[r + 1]; k++) {
                int n = regions.voisins[k];
                if (!regions.estTerre[n]) continue;
                int autre = regionEtat[n];
                if (autre == e) continue;
                if (autre < 0) {
                    if (p.habitants[n] > 0 && ennemi[e] < 0 && tenues[e] < limite(p, e) && alea() < reglages.annexion) {
                        regionEtat[n] = e;
                        tenues[e]++;
                    }
                    continue;
                }
                if (voisinFaible[e] < 0 || soldats[autre] < soldats[voisinFaible[e]]) voisinFaible[e] = autre;
                if (ennemi[e] == autre && (cible[e] < 0 || n != regionCapitale[autre])) {
                    cible[e] = n;
                    origine[e] = r;
                }
            }
        }
    }

    private void declarer(int e, int annee, Population p) {
        int proie = voisinFaible[e];
        if (proie < 0 || ennemi[proie] >= 0) return;
        if (soldats[e] < reglages.rapportForce * soldats[proie]) return;
        int cause = marchandiseManquante(p, e);
        float envie = reglages.envieGuerre * (1f + chefAmbition[e] / 100f) * (1f - chefBien[e] / 200f) * (1f - chefLoi[e] / 300f);
        if (cause >= 0) envie *= 2f;
        if (peuple[e] == peuple[proie]) envie *= 0.3f;
        if (alea() >= envie) return;
        ennemi[e] = proie;
        ennemi[proie] = e;
        attaquant[e] = true;
        attaquant[proie] = false;
        debutGuerre[e] = (short) annee;
        armeeDeDepart[e] = soldats[e];
        armeeDeDepart[proie] = soldats[proie];
        guerres++;
        chronique.noter(annee, Chronique.GUERRE, e, proie, cause, chef[e]);
    }

    private int marchandiseManquante(Population p, int e) {
        int r = regionCapitale[e];
        int pire = -1;
        float prix = p.economie.reglages.prixMax * 0.5f;
        for (int g = Economie.BOIS; g < Economie.NOMBRE; g++) {
            if (p.economie.prix[g][r] > prix) {
                prix = p.economie.prix[g][r];
                pire = g;
            }
        }
        return pire;
    }

    private void combattre(Population p, int a, int annee) {
        int d = ennemi[a];
        if (annee == debutGuerre[a]) return;
        if (cible[a] < 0) {
            conclure(a, d, annee);
            return;
        }
        float forceA = soldats[a] * (1f + chefCourage[a] / 200f) * (0.7f + 0.6f * alea());
        float forceD = soldats[d] * (1f + chefCourage[d] / 200f) * (0.7f + 0.6f * alea()) * reglages.bonusDefense;
        float pertesA = Math.min(soldats[a], reglages.perteBataille * forceD);
        float pertesD = Math.min(soldats[d], reglages.perteBataille * forceA);
        soldats[a] -= pertesA;
        soldats[d] -= pertesD;
        mortsAuCombat += tuer(p, a, origine[a], (int) pertesA, annee);
        mortsAuCombat += tuer(p, d, cible[a], (int) pertesD, annee);
        float avantage = cible[a] == regionCapitale[d] ? reglages.siege : reglages.rapportForce;
        if (forceA > forceD * avantage) {
            conquerir(p, a, d, cible[a], annee);
            if (!estVivant(d)) return;
        }
        boolean brisee = soldats[a] < armeeDeDepart[a] * reglages.armeeBrisee || soldats[d] < armeeDeDepart[d] * reglages.armeeBrisee;
        if (brisee || tresor[a] < 0f || annee - debutGuerre[a] >= reglages.dureeGuerre) conclure(a, d, annee);
    }

    private int tuer(Population p, int e, int region, int nombreMorts, int annee) {
        Foule f = p.foules[peuple[e]][region];
        if (f == null || f.vivants == 0) f = p.foules[peuple[e]][regionCapitale[e]];
        f.melanger(p.graine ^ 0x5DEECE66DL, annee);
        return f.tuerSoldats(nombreMorts, annee, p.reglages.ageAdulte);
    }

    private void conquerir(Population p, int a, int d, int r, int annee) {
        regionEtat[r] = a;
        tenues[a]++;
        tenues[d]--;
        legitimite[a] = Math.min(100f, legitimite[a] + 3f);
        legitimite[d] = Math.max(0f, legitimite[d] - 10f);
        int ville = plusGrandeVille[r];
        int tues = 0;
        if (ville >= 0) {
            Foule f = p.foules[p.villes.peuple[ville]][r];
            float part = reglages.sac * (1f - chefBien[a] / 100f) * 0.5f;
            f.melanger(p.graine ^ 0x2545F4914F6CDD1DL, annee);
            tues = f.saccager(ville, part, annee);
            mortsDesSacs += tues;
        }
        chronique.noter(annee, tues > 0 ? Chronique.SAC : Chronique.CONQUETE, a, d, r, tues);
        if (r == regionCapitale[d]) {
            chronique.noter(annee, Chronique.CHUTE, d, a, r, chef[a]);
            p.maisons.finir(regne[d], annee);
            tomber(d, annee);
            ennemi[a] = -1;
        }
    }

    private void conclure(int a, int d, int annee) {
        ennemi[a] = -1;
        ennemi[d] = -1;
        chronique.noter(annee, Chronique.PAIX, a, d, -1, 0L);
    }

    private void tomber(int e, int annee) {
        fin[e] = (short) annee;
        chutes++;
        if (ennemi[e] >= 0) {
            ennemi[ennemi[e]] = -1;
            ennemi[e] = -1;
        }
        for (int r = 0; r < regions.nombre; r++) {
            if (regionEtat[r] == e) regionEtat[r] = -1;
        }
    }

    public int vivants() {
        return nombre - chutes;
    }
    public void archiver(Archive a) throws java.io.IOException {
        nombre = a.entier(nombre);
        chutes = a.entier(chutes);
        guerres = a.entier(guerres);
        revolutions = a.entier(revolutions);
        crises = a.entier(crises);
        regences = a.entier(regences);
        usurpations = a.entier(usurpations);
        heritages = a.entier(heritages);
        mortsAuCombat = a.grand(mortsAuCombat);
        mortsDesSacs = a.grand(mortsDesSacs);
        hasard = a.grand(hasard);
        a.entiers(regionEtat, regionEtat.length);
        peuple = a.octets(peuple, nombre);
        capitale = a.entiers(capitale, nombre);
        regionCapitale = a.entiers(regionCapitale, nombre);
        fondation = a.courts(fondation, nombre);
        fin = a.courts(fin, nombre);
        chef = a.longs(chef, nombre);
        debutRegne = a.courts(debutRegne, nombre);
        chefAmbition = a.octets(chefAmbition, nombre);
        chefCourage = a.octets(chefCourage, nombre);
        chefLoi = a.octets(chefLoi, nombre);
        chefBien = a.octets(chefBien, nombre);
        legitimite = a.reels(legitimite, nombre);
        soldats = a.reels(soldats, nombre);
        tresor = a.reels(tresor, nombre);
        tenues = a.entiers(tenues, nombre);
        sujets = a.entiers(sujets, nombre);
        tenuesMax = a.entiers(tenuesMax, nombre);
        ennemi = a.entiers(ennemi, nombre);
        maison = a.entiers(maison, nombre);
        regne = a.entiers(regne, nombre);
        slotChef = a.entiers(slotChef, nombre);
        revenu = a.reels(revenu, nombre);
        armeeDeDepart = a.reels(armeeDeDepart, nombre);
        debutGuerre = a.courts(debutGuerre, nombre);
        attaquant = a.booleens(attaquant, nombre);
        voisinFaible = a.entiers(voisinFaible, nombre);
        cible = a.entiers(cible, nombre);
        origine = a.entiers(origine, nombre);
        chronique.archiver(a);
    }

}
