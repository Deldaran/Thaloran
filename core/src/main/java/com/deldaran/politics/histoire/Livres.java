package com.deldaran.politics.histoire;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class Livres {
    public static final byte ANNALES = 0, VIE = 1, GUERRE = 2, COMPLAINTE = 3, CHANSON = 4;
    public static final int TYPES = 5;
    private static final int TAILLE_DEPART = 256;
    private static final int COPIES_MAX = 48;

    public static class Reglages {
        public float portee = 1.2f;
        public float demiVie = 40f;
        public int habitantsEcole = 500;
        public int habitantsPleins = 5000;
        public float progresEcole = 0.01f;
        public float seuilLettres = 15f;
        public float lecture = 35f;
        public float ecriture = 0.004f;
        public float chant = 0.002f;
        public float diffusion = 0.01f;
        public float perteCopie = 0.002f;
        public float perteSac = 0.7f;
        public float effetSac = 0.5f;
    }

    public final Reglages reglages;
    public volatile int nombre = 0;
    public int perdues = 0;
    public final int[] parType = new int[TYPES];

    public float[] savoir = new float[TAILLE_DEPART];

    public byte[] type = new byte[TAILLE_DEPART];
    public boolean[] ecrit = new boolean[TAILLE_DEPART];
    public short[] annee = new short[TAILLE_DEPART];
    public long[] auteur = new long[TAILLE_DEPART];
    public byte[] peuple = new byte[TAILLE_DEPART];
    public int[] ville = new int[TAILLE_DEPART];
    public int[] etat = new int[TAILLE_DEPART];
    public int[] sujet = new int[TAILLE_DEPART];
    public byte[] auteurBien = new byte[TAILLE_DEPART];
    public int[] debutVersions = new int[TAILLE_DEPART];
    public int[] nombreVersions = new int[TAILLE_DEPART];
    public int[][] copies = new int[TAILLE_DEPART][];
    public int[] nombreCopies = new int[TAILLE_DEPART];

    public int totalVersions = 0;
    public int[] versionFait = new int[TAILLE_DEPART];
    public int[] versionAnnee = new int[TAILLE_DEPART];
    public float[] versionAmpleur = new float[TAILLE_DEPART];
    public byte[] versionStade = new byte[TAILLE_DEPART];
    public long[] versionGraine = new long[TAILLE_DEPART];
    public int[] versionSource = new int[TAILLE_DEPART];

    private final HashSet<Long> traites = new HashSet<>();
    private int curseur = 0;
    private long hasard;
    private int[] plusGrande = new int[0];
    private boolean[] capitale = new boolean[0];

    public Livres(Reglages reglages, long graine) {
        this.reglages = reglages;
        hasard = graine * 0xD1342543DE82EF95L + 0x2545F4914F6CDD1DL;
        if (hasard == 0L) hasard = 1L;
    }

    private long suivant() {
        hasard ^= hasard << 13;
        hasard ^= hasard >>> 7;
        hasard ^= hasard << 17;
        return hasard;
    }

    private float alea() {
        return (suivant() >>> 40) * 0x1.0p-24f;
    }

    private void agrandirOeuvres() {
        int n = type.length * 2;
        type = Arrays.copyOf(type, n);
        ecrit = Arrays.copyOf(ecrit, n);
        annee = Arrays.copyOf(annee, n);
        auteur = Arrays.copyOf(auteur, n);
        peuple = Arrays.copyOf(peuple, n);
        ville = Arrays.copyOf(ville, n);
        etat = Arrays.copyOf(etat, n);
        sujet = Arrays.copyOf(sujet, n);
        auteurBien = Arrays.copyOf(auteurBien, n);
        debutVersions = Arrays.copyOf(debutVersions, n);
        nombreVersions = Arrays.copyOf(nombreVersions, n);
        copies = Arrays.copyOf(copies, n);
        nombreCopies = Arrays.copyOf(nombreCopies, n);
    }

    private void agrandirVersions() {
        int n = versionFait.length * 2;
        versionFait = Arrays.copyOf(versionFait, n);
        versionAnnee = Arrays.copyOf(versionAnnee, n);
        versionAmpleur = Arrays.copyOf(versionAmpleur, n);
        versionStade = Arrays.copyOf(versionStade, n);
        versionGraine = Arrays.copyOf(versionGraine, n);
        versionSource = Arrays.copyOf(versionSource, n);
    }

    public void avancer(Population p, int an) {
        Villes v = p.villes;
        if (savoir.length < v.nombre) savoir = Arrays.copyOf(savoir, v.nombre * 2);
        chercherVilles(p);
        subirSacs(p);
        eduquer(p);
        for (int c = 0; c < v.nombre; c++) {
            if (!v.estVivante(c) || v.habitants[c] < reglages.habitantsEcole) continue;
            float taille = v.habitants[c] / 1000f;
            boolean lettree = savoir[c] >= reglages.seuilLettres;
            float chance = lettree ? reglages.ecriture * savoir[c] / 100f * taille : reglages.chant * taille;
            if (alea() < chance) composer(p, c, an, lettree);
        }
        recopier(p);
    }

    private void chercherVilles(Population p) {
        Villes v = p.villes;
        if (plusGrande.length != p.regions.nombre) plusGrande = new int[p.regions.nombre];
        if (capitale.length < v.nombre) capitale = new boolean[v.nombre * 2];
        Arrays.fill(plusGrande, -1);
        Arrays.fill(capitale, false);
        for (int c = 0; c < v.nombre; c++) {
            if (!v.estVivante(c)) continue;
            int r = v.region[c];
            if (plusGrande[r] < 0 || v.habitants[c] > v.habitants[plusGrande[r]]) plusGrande[r] = c;
        }
        Etats e = p.etats;
        for (int i = 0; i < e.nombre; i++) {
            if (e.estVivant(i)) capitale[e.capitale[i]] = true;
        }
    }

    private void subirSacs(Population p) {
        Chronique c = p.etats.chronique;
        int fin = c.nombre;
        for (; curseur < fin; curseur++) {
            if (c.type[curseur] != Chronique.SAC) continue;
            int cible = plusGrande[c.lieu[curseur]];
            if (cible < 0) continue;
            savoir[cible] *= reglages.effetSac;
            for (int o = 0; o < nombre; o++) {
                if (!ecrit[o]) continue;
                for (int k = nombreCopies[o] - 1; k >= 0; k--) {
                    if (copies[o][k] == cible && alea() < reglages.perteSac) retirerCopie(o, k);
                }
            }
        }
    }

    private void eduquer(Population p) {
        Villes v = p.villes;
        for (int c = 0; c < v.nombre; c++) {
            if (!v.estVivante(c)) continue;
            float taille = Math.min(1f, (float) v.habitants[c] / reglages.habitantsPleins);
            float cible = 100f * p.reglages.peuples[v.peuple[c]].lettres * taille * (capitale[c] ? 1.3f : 1f);
            savoir[c] += (Math.min(100f, cible) - savoir[c]) * reglages.progresEcole;
        }
    }

    public int instruction(Population p, Savoir.Temoin t) {
        float base;
        if (t.ville >= 0) {
            base = savoir[t.ville];
        } else {
            int proche = t.region < plusGrande.length ? plusGrande[t.region] : -1;
            base = proche >= 0 ? savoir[proche] * 0.15f : 0f;
        }
        float u = Savoir.tirage(t.id, 0x1E77L);
        return Math.min(100, Math.round(base * 2.2f * u * u));
    }

    public boolean accessible(Population p, int o, Savoir.Temoin t) {
        if (nombreCopies[o] == 0) return false;
        if (ecrit[o] && t.instruction < reglages.lecture) return false;
        if (!ecrit[o] && peuple[o] != t.peuple) return false;
        for (int k = 0; k < nombreCopies[o]; k++) {
            int c = copies[o][k];
            if (c == t.ville || (!ecrit[o] && p.villes.region[c] == t.region)) return true;
        }
        return false;
    }

    public Savoir.Souvenir souvenirDe(int version, int oeuvre) {
        Savoir.Souvenir s = new Savoir.Souvenir();
        s.fait = versionFait[version];
        s.annee = versionAnnee[version];
        s.ampleur = versionAmpleur[version];
        s.stade = versionStade[version];
        s.graine = versionGraine[version];
        s.source = oeuvre;
        return s;
    }

    private void composer(Population p, int c, int an, boolean lettree) {
        Villes v = p.villes;
        int r = v.region[c];
        int q = v.peuple[c];
        Foule f = p.foules[q][r];
        if (f == null) return;
        int place = f.habitantAuHasard(c, an, p.reglages.ageAdulte, suivant());
        if (place < 0) return;
        Savoir.Temoin t = Savoir.temoin(p, f, place);
        t.instruction = Math.max(t.instruction, Math.round(savoir[c]));
        List<Savoir.Souvenir> memoire = Savoir.souvenirs(p, t, 60);
        if (memoire.isEmpty()) return;
        Chronique ch = p.etats.chronique;
        Savoir.Souvenir principal = memoire.get((int) (alea() * Math.min(8, memoire.size())));
        byte genre;
        List<Savoir.Souvenir> choisis = new ArrayList<>();
        if (!lettree) {
            genre = CHANSON;
            choisis.add(principal);
        } else if (t.etat >= 0 && alea() < 0.3f) {
            genre = ANNALES;
            for (Savoir.Souvenir s : memoire) {
                if (choisis.size() < 12 && (ch.sujet[s.fait] == t.etat || ch.objet[s.fait] == t.etat)) choisis.add(s);
            }
            if (choisis.size() < 3) {
                choisis.clear();
                genre = GUERRE;
                choisis.add(principal);
            }
        } else {
            byte quoi = ch.type[principal.fait];
            int ton = Savoir.ton(p, t.etat, q, principal.fait);
            if ((quoi == Chronique.SAC || quoi == Chronique.CHUTE) && ton <= 0) genre = COMPLAINTE;
            else if (quoi == Chronique.FONDATION || quoi == Chronique.REVOLUTION || quoi == Chronique.SUCCESSION) genre = VIE;
            else genre = GUERRE;
            choisis.add(principal);
            int epoque = Math.round(40f / p.reglages.peuples[q].rythme);
            for (Savoir.Souvenir s : memoire) {
                if (s == principal || choisis.size() >= 5 || ch.sujet[s.fait] != ch.sujet[principal.fait]) continue;
                if (genre != VIE || Math.abs(ch.annee[s.fait] - ch.annee[principal.fait]) <= epoque) choisis.add(s);
            }
        }
        long cle = ((long) c << 32) | ((long) principal.fait << 3) | genre;
        if (!traites.add(cle)) return;
        choisis.sort((a, b) -> ch.annee[a.fait] - ch.annee[b.fait]);
        enregistrer(genre, lettree, an, t, q, c, principal.fait, choisis);
    }

    private void enregistrer(byte genre, boolean lettree, int an, Savoir.Temoin t, int q, int c, int fait, List<Savoir.Souvenir> choisis) {
        int o = nombre;
        if (o == type.length) agrandirOeuvres();
        type[o] = genre;
        ecrit[o] = lettree;
        annee[o] = (short) an;
        auteur[o] = t.id;
        peuple[o] = (byte) q;
        ville[o] = c;
        etat[o] = t.etat;
        sujet[o] = fait;
        auteurBien[o] = (byte) t.bien;
        debutVersions[o] = totalVersions;
        nombreVersions[o] = choisis.size();
        copies[o] = new int[4];
        copies[o][0] = c;
        nombreCopies[o] = 1;
        for (Savoir.Souvenir s : choisis) {
            if (totalVersions == versionFait.length) agrandirVersions();
            int k = totalVersions++;
            versionFait[k] = s.fait;
            versionAnnee[k] = s.annee;
            versionAmpleur[k] = s.ampleur;
            versionStade[k] = (byte) s.stade;
            versionGraine[k] = s.graine;
            versionSource[k] = s.source;
        }
        parType[genre]++;
        nombre = o + 1;
    }

    private void retirerCopie(int o, int k) {
        copies[o][k] = copies[o][--nombreCopies[o]];
        if (nombreCopies[o] == 0) perdues++;
    }

    private void ajouterCopie(int o, int c) {
        for (int k = 0; k < nombreCopies[o]; k++) {
            if (copies[o][k] == c) return;
        }
        if (nombreCopies[o] >= COPIES_MAX) return;
        if (nombreCopies[o] == copies[o].length) copies[o] = Arrays.copyOf(copies[o], nombreCopies[o] * 2);
        copies[o][nombreCopies[o]++] = c;
    }

    private void recopier(Population p) {
        Villes v = p.villes;
        Regions regions = p.regions;
        for (int o = 0; o < nombre; o++) {
            for (int k = nombreCopies[o] - 1; k >= 0; k--) {
                int c = copies[o][k];
                if (!v.estVivante(c) || (ecrit[o] && alea() < reglages.perteCopie)) {
                    retirerCopie(o, k);
                    continue;
                }
                if (alea() >= reglages.diffusion) continue;
                int r = v.region[c];
                int pas = 1 + (int) (alea() * 4f);
                for (int n = 0; n < pas; n++) {
                    int debut = regions.debutVoisins[r];
                    int degre = regions.debutVoisins[r + 1] - debut;
                    if (degre == 0) break;
                    r = regions.voisins[debut + (int) (alea() * degre)];
                }
                int cible = plusGrande[r];
                if (cible < 0 || cible == c) continue;
                if (ecrit[o] && savoir[cible] < reglages.seuilLettres) continue;
                if (v.peuple[cible] != peuple[o] && (!ecrit[o] || alea() >= 0.2f)) continue;
                ajouterCopie(o, cible);
            }
        }
    }

    public int vivantes() {
        return nombre - perdues;
    }
}
