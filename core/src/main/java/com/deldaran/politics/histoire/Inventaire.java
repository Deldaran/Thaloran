package com.deldaran.politics.histoire;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class Inventaire {
    public static final class Pile {
        public int bien, nombre;
        public boolean porte;

        Pile(int bien, int nombre, boolean porte) {
            this.bien = bien;
            this.nombre = nombre;
            this.porte = porte;
        }
    }

    public final List<Pile> piles = new ArrayList<>();

    public Pile pile(int bien) {
        for (Pile p : piles) if (p.bien == bien) return p;
        return null;
    }

    public int nombre(int bien) {
        Pile p = pile(bien);
        return p == null ? 0 : p.nombre;
    }

    public void ajouter(int bien, int n) {
        if (n <= 0 || Biens.get(bien) == null) return;
        Pile p = pile(bien);
        if (p != null) p.nombre += n;
        else piles.add(new Pile(bien, n, false));
    }

    public int retirer(int bien, int n) {
        Pile p = pile(bien);
        if (p == null || n <= 0) return 0;
        int pris = Math.min(n, p.nombre);
        p.nombre -= pris;
        if (p.nombre <= 0) piles.remove(p);
        return pris;
    }

    public float poids() {
        float total = 0f;
        for (Pile p : piles) {
            Biens.Bien b = Biens.get(p.bien);
            float unite = b.poids;
            if (p.porte && b.place != Biens.BETE && b.place != Biens.SAC_DOS) unite *= 0.5f;
            total += unite * p.nombre;
        }
        return total;
    }

    public int charge() {
        int bonus = 0;
        for (Pile p : piles) if (p.porte) bonus += Biens.get(p.bien).charge;
        return bonus;
    }

    public boolean porte(int bien) {
        Pile p = pile(bien);
        return p != null && p.porte;
    }

    public int porteA(int place) {
        for (Pile p : piles) if (p.porte && Biens.get(p.bien).place == place) return p.bien;
        return -1;
    }

    public boolean porter(int bien) {
        Pile p = pile(bien);
        Biens.Bien b = Biens.get(bien);
        if (p == null || b == null || b.place < 0) return false;
        for (Pile autre : piles) if (autre.porte && Biens.get(autre.bien).place == b.place) autre.porte = false;
        p.porte = true;
        return true;
    }

    public void ranger(int bien) {
        Pile p = pile(bien);
        if (p != null) p.porte = false;
    }

    /** Pour chaque place vide, porte l'objet le plus précieux qui s'y met. */
    public void porterLeMieux() {
        for (int place = 0; place < Biens.PLACES; place++) {
            if (porteA(place) >= 0) continue;
            Pile mieux = null;
            for (Pile p : piles) {
                Biens.Bien b = Biens.get(p.bien);
                if (b.place != place) continue;
                if (mieux == null || b.prix > Biens.get(mieux.bien).prix) mieux = p;
            }
            if (mieux != null) mieux.porte = true;
        }
    }

    public int[] portes() {
        List<Integer> l = new ArrayList<>();
        for (Pile p : piles) if (p.porte) l.add(p.bien);
        int[] t = new int[l.size()];
        for (int k = 0; k < t.length; k++) t[k] = l.get(k);
        return t;
    }

    public int valeur() {
        int total = 0;
        for (Pile p : piles) total += Biens.get(p.bien).prix * p.nombre;
        return total;
    }

    public String texte() {
        if (piles.isEmpty()) return "rien";
        List<String> l = new ArrayList<>();
        for (Pile p : piles) l.add(Biens.get(p.bien).quantite(p.nombre) + (p.porte ? " (" + Biens.NOMS_PLACES[Biens.get(p.bien).place] + ")" : ""));
        return String.join(", ", l);
    }

    public Inventaire copie() {
        Inventaire i = new Inventaire();
        for (Pile p : piles) i.piles.add(new Pile(p.bien, p.nombre, p.porte));
        return i;
    }

    public void ecrire(DataOutputStream s) throws IOException {
        s.writeInt(piles.size());
        for (Pile p : piles) {
            s.writeShort(p.bien);
            s.writeInt(p.nombre);
            s.writeBoolean(p.porte);
        }
    }

    public static Inventaire lire(DataInputStream s) throws IOException {
        Inventaire i = new Inventaire();
        int n = s.readInt();
        for (int k = 0; k < n; k++) {
            int bien = s.readShort();
            int nombre = s.readInt();
            boolean porte = s.readBoolean();
            if (Biens.get(bien) != null && nombre > 0) i.piles.add(new Pile(bien, nombre, porte));
        }
        return i;
    }
}
