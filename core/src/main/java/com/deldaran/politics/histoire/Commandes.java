package com.deldaran.politics.histoire;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Les commandes : un atelier fabrique un objet qu'il n'a plus en réserve (une épée dans une petite ville) ; on le lui commande.
 * On paie un acompte, l'atelier se met à l'ouvrage (la commande compte dans sa demande), et l'objet est prêt quelques jours plus tard :
 * on revient le chercher et on paie le reste.
 */
public final class Commandes {
    /** Un objet fait sur commande coûte un peu plus ; l'acompte est une part du prix. */
    public static final float SUPPLEMENT = 1.15f, ACOMPTE = 0.3f;
    /** Délai : DELAI_MIN … DELAI_MAX jours, plus long pour un objet cher et un atelier qui a peu de bras. */
    public static final int DELAI_MIN = 2, DELAI_MAX = 12;
    /** On ne commande que des objets d'au moins ce prix (pas un pain). */
    public static final int PRIX_MIN = 15;

    public static final class Commande {
        public int ville, lieu, bien, quantite, prix, acompte, pret;
        public boolean annoncee;
    }

    private final Joueur j;
    private final Population p;
    public final List<Commande> liste = new ArrayList<>();

    Commandes(Joueur j, Population p) {
        this.j = j;
        this.p = p;
    }

    private String ville(int v) {
        return Noms.ville(p.villes.peuple[v], p.graine, v);
    }

    /** Les objets que cet atelier fabrique mais n'a plus : autant de choix « Commander … » dans la boutique ; et les commandes prêtes ici. */
    void choix(Joueur.Scene s, int lieu, EconomieVille.Agent a) {
        int v = j.villeIci();
        for (Commande c : liste) {
            if (c.ville != v || c.lieu != lieu) continue;
            Biens.Bien x = Biens.get(c.bien);
            if (j.jour() >= c.pret) s.ajouter("Retirer ta commande : " + x.quantite(c.quantite) + " (reste " + Fiche.argentTexte(c.prix - c.acompte) + ")", () -> retirer(c));
            else s.ajouter("Ta commande : " + x.quantite(c.quantite) + ", prête dans " + (c.pret - j.jour()) + " jour" + (c.pret - j.jour() > 1 ? "s" : ""), () -> attendre(c));
        }
        if (a.joueur) return;
        for (int b = 0; b < a.fabrique.length; b++) {
            if (!a.fabrique[b] || a.stock[b] >= 1f || Biens.get(b).prix < PRIX_MIN || !fabricable(a, b)) continue;
            int bien = b;
            s.ajouter("Commander : " + Biens.get(b).nom, () -> proposer(lieu, bien));
        }
    }

    /** Une recette en marche fabrique ce bien. */
    private static boolean fabricable(EconomieVille.Agent a, int b) {
        for (int k = 0; k < a.recettes.size(); k++) {
            if (a.arret[k]) continue;
            for (Biens.Bien x : a.recettes.get(k).sorties) if (x.id == b) return true;
        }
        return false;
    }

    static int prixUnite(EconomieVille.Agent a, int b) {
        return Math.max(1, Math.round(Math.max(a.prix[b], Biens.get(b).prix) * SUPPLEMENT));
    }

    static int delai(EconomieVille.Agent a, int b, int q) {
        float bras = Math.max(1f, a.ouvriers);
        int d = 1 + (int) Math.ceil(Biens.get(b).prix * q / (30f * bras));
        return Math.max(DELAI_MIN, Math.min(DELAI_MAX, d));
    }

    Joueur.Scene proposer(int lieu, int b) {
        EconomieVille.Agent a = j.agentIci(lieu);
        Fiche f = j.fiche();
        if (a == null || f == null) return j.boutiqueScene(lieu);
        Biens.Bien x = Biens.get(b);
        String M = j.travail().maitreNom(lieu);
        String Mm = Character.toUpperCase(M.charAt(0)) + M.substring(1);
        int pu = prixUnite(a, b);
        StringBuilder t = new StringBuilder(Mm).append(" n'a plus de ").append(x.nom).append(" en réserve, mais peut t'en faire.");
        t.append("\nSur commande : ").append(Fiche.argentTexte(pu)).append(" pièce, avec un acompte de ").append(Math.round(ACOMPTE * 100f)).append(" % aujourd'hui.");
        Joueur.Scene s = new Joueur.Scene("Commander", "");
        for (int q : new int[] { 1, 3, 5 }) {
            int total = pu * q, acompte = (int) Math.ceil(total * ACOMPTE);
            int d = delai(a, b, q);
            if (acompte > f.argent) continue;
            s.ajouter("En commander " + q + " : " + Fiche.argentTexte(total) + ", prêt" + (q > 1 ? "s" : "") + " dans " + d + " jour" + (d > 1 ? "s" : "") + " (acompte " + Fiche.argentTexte(acompte) + ")",
                    () -> commander(lieu, b, q, total, acompte, d));
        }
        if (s.choix.isEmpty()) t.append("\nTu n'as même pas de quoi payer l'acompte.");
        s.ajouter("« Revenir", () -> j.boutiqueScene(lieu));
        Joueur.Scene vraie = new Joueur.Scene("Commander : " + x.nom, t.toString());
        vraie.choix.addAll(s.choix);
        vraie.suites.addAll(s.suites);
        return vraie;
    }

    private Joueur.Scene commander(int lieu, int b, int q, int total, int acompte, int d) {
        EconomieVille.Agent a = j.agentIci(lieu);
        Fiche f = j.fiche();
        if (a == null || f.argent < acompte) return j.boutiqueScene(lieu);
        f.argent -= acompte;
        a.caisse += acompte;
        // La commande compte dans la demande de l'atelier : il achète ses matières et se met à l'ouvrage.
        a.demandeJour[b] += q;
        Commande c = new Commande();
        c.ville = j.villeIci();
        c.lieu = lieu;
        c.bien = b;
        c.quantite = q;
        c.prix = total;
        c.acompte = acompte;
        c.pret = j.jour() + d;
        liste.add(c);
        j.gesteJoueur = "donner";
        j.noter("Tu commandes " + Biens.get(b).quantite(q) + " à " + ville(c.ville) + ", prêt" + (q > 1 ? "s" : "") + " dans " + d + " jours.");
        j.enregistrer();
        return j.boutiqueScene(lieu, "Marché conclu : reviens dans " + d + " jour" + (d > 1 ? "s" : "") + ".");
    }

    private Joueur.Scene attendre(Commande c) {
        Joueur.Scene s = new Joueur.Scene("Ta commande", "On y travaille. Reviens dans " + Math.max(1, c.pret - j.jour()) + " jour" + (c.pret - j.jour() > 1 ? "s" : "") + ".");
        s.ajouter("Annuler la commande (l'acompte est perdu)", () -> {
            liste.remove(c);
            j.enregistrer();
            return j.boutiqueScene(c.lieu, "Tu annules ta commande. L'acompte reste au maître.");
        });
        s.ajouter("« Revenir", () -> j.boutiqueScene(c.lieu));
        return s;
    }

    private Joueur.Scene retirer(Commande c) {
        Fiche f = j.fiche();
        EconomieVille.Agent a = j.agentIci(c.lieu);
        if (a == null) return j.boutiqueScene(c.lieu);
        int reste = c.prix - c.acompte;
        if (f.argent < reste) return j.boutiqueScene(c.lieu, "Il te manque " + Fiche.argentTexte(reste - f.argent) + " pour payer le reste.");
        f.argent -= reste;
        a.caisse += reste;
        f.sac.ajouter(c.bien, c.quantite);
        liste.remove(c);
        j.gesteJoueur = "donner";
        j.rafraichirSac();
        j.enregistrer();
        return j.boutiqueScene(c.lieu, "Tu paies le reste et repars avec " + Biens.get(c.bien).quantite(c.quantite) + ", tout neuf.");
    }

    /** Chaque jour : une commande prête s'annonce une fois. */
    void avancer() {
        for (Commande c : liste) {
            if (c.annoncee || j.jour() < c.pret) continue;
            c.annoncee = true;
            j.annoncer("Ta commande est prête : " + Biens.get(c.bien).quantite(c.quantite) + " t'attend" + (c.quantite > 1 ? "ent" : "") + " à " + ville(c.ville) + ".");
        }
    }

    /** Pour les essais. */
    public Joueur.Scene proposerPourEssai(int lieu, int b) {
        return proposer(lieu, b);
    }

    void ecrire(DataOutputStream s) throws IOException {
        s.writeInt(liste.size());
        for (Commande c : liste) {
            s.writeInt(c.ville);
            s.writeInt(c.lieu);
            s.writeInt(c.bien);
            s.writeInt(c.quantite);
            s.writeInt(c.prix);
            s.writeInt(c.acompte);
            s.writeInt(c.pret);
            s.writeBoolean(c.annoncee);
        }
    }

    void lire(DataInputStream s) throws IOException {
        int n = s.readInt();
        for (int k = 0; k < n; k++) {
            Commande c = new Commande();
            c.ville = s.readInt();
            c.lieu = s.readInt();
            c.bien = s.readInt();
            c.quantite = s.readInt();
            c.prix = s.readInt();
            c.acompte = s.readInt();
            c.pret = s.readInt();
            c.annoncee = s.readBoolean();
            liste.add(c);
        }
    }
}
