package com.deldaran.politics.histoire;

import com.deldaran.politics.Metiers;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Le joueur face à l'économie des villes : garde une EconomieVille par ville visitée, la fait avancer jusqu'au jour présent,
 * et calcule ce que le joueur paie ou reçoit.
 */
public final class Marche {
    /** Rabais maximal (10 %) obtenu avec 150 en Commerce, en achat comme en vente. */
    public static final float GAIN_COMMERCE = 0.1f;
    public static final float ETRANGER = 1.1f;
    /** Un marchand rachète au joueur à ce taux de ce que la marchandise vaut pour lui. */
    public static final float RACHAT = 0.8f;

    private final Population p;
    private final Map<Integer, EconomieVille> villes = new LinkedHashMap<>();
    private final float moyenneNourriture, moyenneBois, moyenneMinerai;

    public Marche(Population p) {
        this.p = p;
        double n = 0, bo = 0, mi = 0;
        int cn = 0, cb = 0, cm = 0;
        for (int r = 0; r < p.regions.nombre; r++) {
            if (p.regions.nourriture[r] > 0f) { n += p.regions.nourriture[r]; cn++; }
            if (p.regions.bois[r] > 0f) { bo += p.regions.bois[r]; cb++; }
            if (p.regions.minerai[r] > 0f) { mi += p.regions.minerai[r]; cm++; }
        }
        moyenneNourriture = cn == 0 ? 1f : (float) (n / cn);
        moyenneBois = cb == 0 ? 1f : (float) (bo / cb);
        moyenneMinerai = cm == 0 ? 1f : (float) (mi / cm);
    }

    public Metiers.Profil profil(int ville) {
        Metiers.Profil pr = Metiers.profil(p, ville);
        pr.eau = pr.peche > 0f;
        return pr;
    }

    private static float relatif(float v, float moyenne) {
        if (v <= 0f) return 0f;
        return Math.max(0.3f, Math.min(2f, v / moyenne));
    }

    private EconomieVille construire(int ville) {
        Metiers.Profil pr = profil(ville);
        float fert = 0.5f + 0.5f * relatif(pr.nourriture, moyenneNourriture);
        return new EconomieVille(p, ville, pr, fert, relatif(pr.bois, moyenneBois), relatif(pr.minerai, moyenneMinerai));
    }

    /** L'économie de la ville, avancée jusqu'au jour donné. La première fois, elle tourne 60 jours pour trouver son équilibre. */
    public EconomieVille ville(int ville, int jour) {
        EconomieVille e = villes.get(ville);
        if (e == null) {
            e = construire(ville);
            e.roder();
            e.jour = jour;
            villes.put(ville, e);
        }
        e.avancerJusqua(jour);
        return e;
    }

    /** Les économies des villes déjà connues (pour les caravanes du pays). */
    public java.util.Collection<EconomieVille> economies() {
        return villes.values();
    }

    private float habilete(Fiche fiche) {
        return Math.min(1f, fiche.competences[Fiche.COMMERCE] / 150f);
    }

    public int prixAchat(Fiche fiche, EconomieVille e, EconomieVille.Agent a, int b) {
        float m = 1f - GAIN_COMMERCE * habilete(fiche);
        if (p.villes.peuple[e.ville] != fiche.peuple) m *= ETRANGER;
        return Math.max(1, Math.round(a.prix[b] * m));
    }

    /** Ce que la marchandise vaut pour ce marchand : il en a besoin (matière, revente), il la fabrique lui-même, ou il l'exporte. */
    public int prixVente(Fiche fiche, EconomieVille e, EconomieVille.Agent a, int b) {
        float valeur;
        if (a.achete[b]) valeur = Math.min(a.prix[b], e.meilleurPrixGros(b, a));
        else if (a.fabrique[b]) valeur = a.cout[b] * 0.75f;
        else if (a.lieu == Metiers.ENTREPOT || a.lieu == Biens.MARCHE) valeur = e.prixExport(b);
        else return 0;
        if (a.stock[b] > 2f * a.cible[b] + 2f) valeur *= 0.5f;
        float m = RACHAT + GAIN_COMMERCE * habilete(fiche);
        if (p.villes.peuple[e.ville] != fiche.peuple) m /= ETRANGER;
        return (int) Math.floor(valeur * m);
    }

    public int stock(EconomieVille.Agent a, int b) {
        return (int) Math.floor(Math.max(0f, a.stock[b]));
    }

    public String acheter(Fiche fiche, EconomieVille e, EconomieVille.Agent a, int b, int n) {
        Biens.Bien x = Biens.get(b);
        int dispo = stock(a, b);
        int prix = prixAchat(fiche, e, a, b);
        n = Math.min(n, Math.min(dispo, fiche.argent / prix));
        if (n <= 0) return dispo <= 0 ? "Il n'y en a plus." : "Tu n'as pas assez d'argent.";
        int total = prix * n;
        fiche.argent -= total;
        fiche.sac.ajouter(b, n);
        e.venteAuJoueur(a, b, n, total);
        fiche.apprendre(Fiche.COMMERCE, Math.min(4f, 0.3f + total / 300f));
        return "Tu achètes " + x.quantite(n) + " pour " + Fiche.argentTexte(total) + ".";
    }

    public String vendre(Fiche fiche, EconomieVille e, EconomieVille.Agent a, int b, int n) {
        Biens.Bien x = Biens.get(b);
        int prix = prixVente(fiche, e, a, b);
        if (prix <= 0) return "Ça ne l'intéresse pas.";
        int caisse = (int) Math.max(0, Math.floor(a.caisse));
        n = Math.min(n, Math.min(fiche.sac.nombre(b), caisse / prix));
        if (n <= 0) return caisse < prix ? a.nom + " n'a plus assez d'argent en caisse." : "Tu n'en as pas.";
        int total = prix * n;
        fiche.sac.retirer(b, n);
        fiche.argent += total;
        e.achatAuJoueur(a, b, n, total);
        fiche.apprendre(Fiche.COMMERCE, Math.min(4f, 0.3f + total / 300f));
        return "Tu vends " + x.quantite(n) + " pour " + Fiche.argentTexte(total) + ".";
    }

    public void ecrire(DataOutputStream s) throws IOException {
        s.writeInt(villes.size());
        for (EconomieVille e : villes.values()) e.ecrire(s);
    }

    public void lire(DataInputStream s) throws IOException {
        int n = s.readInt();
        for (int k = 0; k < n; k++) {
            int ville = s.readInt();
            EconomieVille e = construire(ville);
            e.lire(s);
            villes.put(ville, e);
        }
    }
}
