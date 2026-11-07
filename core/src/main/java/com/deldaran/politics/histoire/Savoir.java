package com.deldaran.politics.histoire;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Savoir {
    private static final int LOIN = 9999;

    public static final class Temoin {
        public int peuple;
        public int region;
        public int ville;
        public int etat;
        public int naissance;
        public int instruction;
        public int loi;
        public int bien;
        public int sexe;
        public int maison;
        public int talent;
        public long id;
    }

    public static final class Souvenir {
        public int fait;
        public int annee;
        public float ampleur = 1f;
        public int stade;
        public long graine;
        public int source = -1;
        public boolean vecu;
        public float score;
    }

    private Savoir() {
    }

    public static float tirage(long a, long b) {
        long h = a * 0x9E3779B97F4A7C15L + b * 0xD1342543DE82EF95L + 0x632BE59BD9B4E019L;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (h >>> 40) * 0x1.0p-24f;
    }

    public static float importance(Population p, int i) {
        Chronique c = p.etats.chronique;
        float base;
        switch (c.type[i]) {
            case Chronique.CHUTE: base = 5f; break;
            case Chronique.FONDATION:
            case Chronique.REVOLUTION:
            case Chronique.USURPATION:
            case Chronique.CRISE:
            case Chronique.EXTINCTION: base = 3f; break;
            case Chronique.SUCCESSION:
            case Chronique.REGENCE: base = 1.5f; break;

            case Chronique.SAC: base = 2f + Math.min(3f, c.valeur[i] / 200f); break;
            case Chronique.GUERRE:
            case Chronique.SECESSION: base = 2f; break;
            default: base = 0.5f; break;
        }
        int taille = Math.max(1, p.etats.tenuesMax[c.sujet[i]]);
        return base * (1f + (float) (Math.log(taille) / Math.log(2)) / 3f);
    }

    public static int regionDu(Population p, int i) {
        Chronique c = p.etats.chronique;
        if (c.type[i] == Chronique.GUERRE || c.lieu[i] < 0) {
            int e = c.objet[i] >= 0 ? c.objet[i] : c.sujet[i];
            return p.etats.regionCapitale[e];
        }
        return c.lieu[i];
    }

    public static int[] distances(Regions regions, int depart) {
        int[] distance = new int[regions.nombre];
        Arrays.fill(distance, LOIN);
        int[] file = new int[regions.nombre];
        int debut = 0, fin = 0;
        file[fin++] = depart;
        distance[depart] = 0;
        while (debut < fin) {
            int r = file[debut++];
            for (int k = regions.debutVoisins[r]; k < regions.debutVoisins[r + 1]; k++) {
                int n = regions.voisins[k];
                if (distance[n] != LOIN) continue;
                distance[n] = distance[r] + 1;
                file[fin++] = n;
            }
        }
        return distance;
    }

    public static Temoin temoin(Population p, Foule f, int place) {
        Temoin t = new Temoin();
        t.peuple = f.indicePeuple;
        t.region = f.region;
        t.ville = f.villeDe(place);
        t.etat = p.etats.regionEtat[f.region];
        t.naissance = f.naissanceDe(place);
        t.id = f.idDe(place);
        t.loi = f.loiDe(place);
        t.bien = f.bienDe(place);
        t.sexe = f.sexeDe(place);
        t.maison = f.maisonDe(place);
        t.talent = f.talentDe(place);
        t.instruction = p.livres.instruction(p, t);
        return t;
    }

    public static int ton(Population p, int etat, int peuple, int i) {
        Chronique c = p.etats.chronique;
        int gagnant = -1, perdant = -1;
        switch (c.type[i]) {
            case Chronique.CHUTE: gagnant = c.objet[i]; perdant = c.sujet[i]; break;
            case Chronique.SAC:
            case Chronique.CONQUETE:
            case Chronique.GUERRE: gagnant = c.sujet[i]; perdant = c.objet[i]; break;
            case Chronique.SECESSION: gagnant = c.sujet[i]; perdant = c.objet[i]; break;
            case Chronique.FONDATION: gagnant = c.sujet[i]; break;
            case Chronique.REVOLUTION:
            case Chronique.USURPATION:
            case Chronique.EXTINCTION: perdant = c.sujet[i]; break;
            default: break;
        }
        if (etat >= 0 && etat == gagnant) return 1;
        if (etat >= 0 && etat == perdant) return -1;
        boolean ami = gagnant >= 0 && p.etats.peuple[gagnant] == peuple;
        boolean victime = perdant >= 0 && p.etats.peuple[perdant] == peuple;
        if (ami && !victime) return 1;
        if (victime && !ami) return -1;
        return 0;
    }

    private static int stade(int age, float rythme) {
        float generations = age * rythme / 25f;
        if (generations < 3f) return 0;
        if (generations < 10f) return 1;
        if (generations < 40f) return 2;
        return 3;
    }

    private static Souvenir oral(Population p, Temoin t, int i, boolean vecu, int distance) {
        Chronique c = p.etats.chronique;
        Souvenir s = new Souvenir();
        s.fait = i;
        s.vecu = vecu && distance <= 2;
        int age = p.annee - c.annee[i];
        float rythme = p.reglages.peuples[t.peuple].rythme;
        s.stade = vecu ? 0 : stade(age, rythme);
        if (!s.vecu && (distance > 4 || t.instruction < 20)) s.stade = Math.max(s.stade, 1);
        s.graine = i * 4L + t.peuple;
        float u = tirage(s.graine, 7L);
        s.ampleur = (float) Math.exp(s.stade * 0.7f * u);
        s.annee = c.annee[i] + Math.round((tirage(s.graine, 8L) - 0.5f) * age * 0.15f * s.stade);
        return s;
    }

    private static float chance(Population p, Temoin t, int i, int distance, boolean vecu, Livres.Reglages r) {
        Chronique c = p.etats.chronique;
        float poids = importance(p, i);
        int age = p.annee - c.annee[i];
        float rythme = p.reglages.peuples[t.peuple].rythme;
        float portee = poids * r.portee;
        float espace = distance <= portee ? 1f : (float) Math.exp((portee - distance) / 3f);
        boolean concerne = c.sujet[i] == t.etat || c.objet[i] == t.etat;
        boolean sien = p.etats.peuple[c.sujet[i]] == t.peuple;
        if (!concerne && !sien) espace *= 0.3f;
        float temps = vecu ? 1f : (float) Math.pow(0.5, age * rythme / (r.demiVie * poids));
        float tete = 0.15f + 0.85f * t.instruction / 100f;
        float reste = espace * temps * tete * (concerne ? 2f : 1f);
        if (vecu && distance <= 2 && poids >= 2f) reste = Math.max(reste, 0.8f);
        return Math.min(1f, reste);
    }

    public static List<Souvenir> souvenirs(Population p, Temoin t, int nombre) {
        Chronique c = p.etats.chronique;
        Livres l = p.livres;
        int total = c.nombre;
        int[] distance = distances(p.regions, t.region);
        Souvenir[] connus = new Souvenir[total];
        for (int i = 0; i < total; i++) {
            boolean vecu = c.annee[i] >= t.naissance;
            int loin = distance[regionDu(p, i)];
            float ch = chance(p, t, i, loin, vecu, l.reglages);
            if (tirage(t.id, i) < ch) connus[i] = oral(p, t, i, vecu, loin);
        }
        for (int o = 0; o < l.nombre; o++) {
            if (!l.accessible(p, o, t)) continue;
            float ch = l.ecrit[o] ? t.instruction / 100f : 0.6f;
            if (tirage(t.id, 0x10000000L + o) >= ch) continue;
            for (int k = l.debutVersions[o]; k < l.debutVersions[o] + l.nombreVersions[o]; k++) {
                int i = l.versionFait[k];
                Souvenir ancien = connus[i];
                if (ancien != null && (ancien.vecu || ancien.stade < l.versionStade[k])) continue;
                connus[i] = l.souvenirDe(k, o);
            }
        }
        List<Souvenir> liste = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            Souvenir s = connus[i];
            if (s == null) continue;
            int age = p.annee - c.annee[i];
            boolean concerne = c.sujet[i] == t.etat || c.objet[i] == t.etat;
            float legende = s.stade >= 2 ? 500f : 1f;
            s.score = importance(p, i) * (s.vecu ? 2f : 1f) * (concerne ? 2f : 1f) * (s.source >= 0 ? 1.5f : 1f) / (1f + age / (500f * legende)) / (1f + distance[regionDu(p, i)] / 8f) * (0.5f + tirage(t.id ^ 0x55L, i));
            liste.add(s);
        }
        liste.sort((a, b) -> Float.compare(b.score, a.score));
        return liste.size() > nombre ? new ArrayList<>(liste.subList(0, nombre)) : liste;
    }
}
