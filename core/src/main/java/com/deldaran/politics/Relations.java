package com.deldaran.politics;

import com.badlogic.gdx.files.FileHandle;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Relations {
    public static final int SALUT = 0, CADEAU = 1, TENDRESSE = 2, INSULTE = 3, COUP = 4, ASSOMME = 5, EPARGNE = 6, MEURTRE = 7, SOIN = 8, VICTOIRE = 9, TEMOIN_COUP = 10, TEMOIN_MEURTRE = 11, PROCHE_TUE = 12, RUMEUR = 13, REDDITION = 14;
    public static final String[] RECITS = { "m'a salue", "m'a fait un cadeau", "a ete tendre avec moi", "m'a insulte", "m'a frappe", "m'a mis a terre", "m'a epargne", "m'a tue", "m'a soigne", "a ete battu par moi", "a frappe quelqu'un devant moi", "a tue quelqu'un devant moi", "a tue un de mes proches", "on m'a parle de lui", "a eu raison de moi, je le sers" };
    private static final String[][] TITRES = {
        { "le Vengeur", "la Vengeresse" },
        { "le Balafre", "la Balafree" },
        { "le Briseur d'os", "la Briseuse d'os" },
        { "l'Increvable", "l'Increvable" },
        { "le Revenant", "la Revenante" },
        { "le Fleau de l'etranger", "le Fleau de l'etranger" }
    };
    public static final int TITRE_VENGEANCE = 0, TITRE_HUMILIATION = 1, TITRE_VICTOIRE = 2, TITRE_INCREVABLE = 3, TITRE_RETOUR = 4, TITRE_FLEAU = 5;

    public static class Souvenir {
        public final int type;
        public final double jour;
        public final String sujet;

        Souvenir(int type, double jour, String sujet) {
            this.type = type;
            this.jour = jour;
            this.sujet = sujet;
        }
    }

    public static class Lien {
        public final long graine;
        public final String nom;
        public final boolean femme;
        public float estime, peur, rancune;
        public boolean rival, mort, soumis;
        public int niveau, victoires, defaites, epargnes;
        public String titre;
        public double prochaineTraque;
        public final List<Souvenir> souvenirs = new ArrayList<>();

        Lien(long graine, String nom, boolean femme) {
            this.graine = graine;
            this.nom = nom;
            this.femme = femme;
        }

        public String nomComplet() {
            return titre == null ? nom : nom + " " + titre;
        }

        public void titrer(int sorte) {
            titre = TITRES[sorte][femme ? 1 : 0];
        }

        public String resume() {
            StringBuilder t = new StringBuilder(nomComplet());
            if (mort) return t.append(" (mort)").toString();
            if (rival) t.append(", rival niv. ").append(niveau).append(" (").append(victoires).append(" victoires, ").append(defaites).append(" defaites)");
            if (soumis) t.append(", soumis");
            t.append(String.format(" estime %+.0f%% peur %.0f%% rancune %.0f%%", estime * 100f, peur * 100f, rancune * 100f));
            if (!souvenirs.isEmpty()) t.append(", se souvient : ").append(RECITS[souvenirs.get(souvenirs.size() - 1).type]);
            return t.toString();
        }
    }

    private final Map<Long, Lien> liens = new HashMap<>();
    public final List<Lien> rivaux = new ArrayList<>();
    public boolean modifie, aRecaler;
    public double jourCourant;
    private static final int VERSION = 1;

    public Lien lien(Villageois w) {
        Lien l = liens.get(w.graine);
        if (l == null) {
            String nom = w.nomReel != null ? w.nomReel : com.deldaran.politics.histoire.Noms.personne(w.village.peuple, w.graine, 0, w.corps.apparence.femme ? 1 : 0);
            l = new Lien(w.graine, nom, w.corps.apparence.femme);
            liens.put(w.graine, l);
            modifie = true;
        }
        w.lien = l;
        return l;
    }

    public Lien existant(long graine) {
        return liens.get(graine);
    }

    public void oublier() {
        liens.clear();
        rivaux.clear();
        modifie = true;
    }

    public void enregistrer(FileHandle f) {
        double origine = aRecaler ? 0.0 : jourCourant;
        try (DataOutputStream o = new DataOutputStream(new BufferedOutputStream(f.write(false)))) {
            o.writeInt(VERSION);
            o.writeInt(liens.size());
            for (Lien l : liens.values()) {
                o.writeLong(l.graine);
                o.writeUTF(l.nom);
                o.writeBoolean(l.femme);
                o.writeFloat(l.estime);
                o.writeFloat(l.peur);
                o.writeFloat(l.rancune);
                o.writeBoolean(l.rival);
                o.writeBoolean(l.mort);
                o.writeBoolean(l.soumis);
                o.writeInt(l.niveau);
                o.writeInt(l.victoires);
                o.writeInt(l.defaites);
                o.writeInt(l.epargnes);
                o.writeUTF(l.titre == null ? "" : l.titre);
                o.writeDouble(l.prochaineTraque - origine);
                o.writeInt(l.souvenirs.size());
                for (Souvenir s : l.souvenirs) {
                    o.writeInt(s.type);
                    o.writeDouble(s.jour - origine);
                    o.writeUTF(s.sujet == null ? "" : s.sujet);
                }
            }
            o.writeInt(rivaux.size());
            for (Lien l : rivaux) o.writeLong(l.graine);
            modifie = false;
        } catch (Exception e) {
            System.out.println("[relations] sauvegarde impossible : " + e.getMessage());
        }
    }

    public void charger(FileHandle f) {
        liens.clear();
        rivaux.clear();
        modifie = false;
        if (!f.exists()) return;
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(f.read()))) {
            if (in.readInt() != VERSION) return;
            int n = in.readInt();
            for (int i = 0; i < n; i++) {
                Lien l = new Lien(in.readLong(), in.readUTF(), in.readBoolean());
                l.estime = in.readFloat();
                l.peur = in.readFloat();
                l.rancune = in.readFloat();
                l.rival = in.readBoolean();
                l.mort = in.readBoolean();
                l.soumis = in.readBoolean();
                l.niveau = in.readInt();
                l.victoires = in.readInt();
                l.defaites = in.readInt();
                l.epargnes = in.readInt();
                String titre = in.readUTF();
                l.titre = titre.isEmpty() ? null : titre;
                l.prochaineTraque = in.readDouble();
                int m = in.readInt();
                for (int k = 0; k < m; k++) {
                    int type = in.readInt();
                    double jour = in.readDouble();
                    String sujet = in.readUTF();
                    l.souvenirs.add(new Souvenir(type, jour, sujet.isEmpty() ? null : sujet));
                }
                liens.put(l.graine, l);
            }
            int r = in.readInt();
            for (int i = 0; i < r; i++) {
                Lien l = liens.get(in.readLong());
                if (l != null) rivaux.add(l);
            }
            aRecaler = true;
            System.out.println("[relations] " + liens.size() + " liens charges, " + rivaux.size() + " rivaux");
        } catch (Exception e) {
            System.out.println("[relations] lecture impossible : " + e.getMessage());
            liens.clear();
            rivaux.clear();
        }
    }

    public void recaler(double jour) {
        jourCourant = jour;
        if (!aRecaler) return;
        aRecaler = false;
        for (Lien l : liens.values()) {
            l.prochaineTraque += jour;
            for (int k = 0; k < l.souvenirs.size(); k++) {
                Souvenir s = l.souvenirs.get(k);
                l.souvenirs.set(k, new Souvenir(s.type, s.jour + jour, s.sujet));
            }
        }
    }

    public int nombre() {
        return liens.size();
    }

    public void retenir(Villageois w, int type, double jour) {
        retenir(w, type, jour, null);
    }

    public void retenir(Villageois w, int type, double jour, String sujet) {
        Lien l = lien(w);
        modifie = true;
        float e = 0f, p = 0f, r = 0f;
        switch (type) {
            case SALUT: e = 0.03f; break;
            case CADEAU: e = 0.2f; r = -0.15f; break;
            case TENDRESSE: e = l.estime > 0.3f ? 0.12f : -0.1f; break;
            case INSULTE: e = -0.15f; r = 0.12f; break;
            case COUP: e = -0.3f; p = 0.2f; r = 0.3f; break;
            case ASSOMME: e = -0.15f; p = 0.3f; r = 0.2f; break;
            case EPARGNE: e = 0.1f; p = 0.1f; r = l.rival ? 0.1f : -0.15f; break;
            case SOIN: e = 0.3f; r = -0.2f; break;
            case TEMOIN_COUP: e = -0.12f; p = 0.1f; r = 0.05f; break;
            case TEMOIN_MEURTRE: e = -0.5f; p = 0.45f; r = 0.25f; break;
            case PROCHE_TUE: e = -1f; r = 1f; break;
            default: break;
        }
        l.estime = borner(l.estime + e, -1f, 1f);
        l.peur = borner(l.peur + p, 0f, 1f);
        l.rancune = borner(l.rancune + r, 0f, 1f);
        if (type != SALUT || l.souvenirs.isEmpty() || l.souvenirs.get(l.souvenirs.size() - 1).type != SALUT) l.souvenirs.add(new Souvenir(type, jour, sujet));
        if (l.souvenirs.size() > 12) l.souvenirs.remove(0);
    }

    public boolean promouvoir(Villageois w, int sorte, double jour) {
        Lien l = lien(w);
        if (l.rival || l.mort || l.soumis || w.enfant || l.rancune < 0.75f) return false;
        l.rival = true;
        l.niveau = 1;
        l.titrer(sorte);
        l.prochaineTraque = jour + 0.02;
        rivaux.add(l);
        modifie = true;
        return true;
    }

    public void bavarder(Villageois a, Villageois b, double jour) {
        Lien la = a.lien, lb = b.lien;
        if (la == null && lb == null) return;
        if (poids(la) >= poids(lb)) transmettre(la, b, jour);
        else transmettre(lb, a, jour);
    }

    private void transmettre(Lien source, Villageois cible, double jour) {
        if (poids(source) < 0.15f) return;
        Lien l = lien(cible);
        modifie = true;
        l.estime = borner(l.estime + (source.estime - l.estime) * 0.3f, -1f, 1f);
        l.peur = borner(l.peur + (source.peur - l.peur) * 0.25f, 0f, 1f);
        l.rancune = borner(l.rancune + Math.max(0f, source.rancune - l.rancune) * 0.15f, 0f, 1f);
        if (l.souvenirs.isEmpty() || l.souvenirs.get(l.souvenirs.size() - 1).type != RUMEUR) l.souvenirs.add(new Souvenir(RUMEUR, jour, null));
    }

    private static float poids(Lien l) {
        return l == null ? 0f : Math.max(Math.abs(l.estime), Math.max(l.peur, l.rancune));
    }

    public static float borner(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
