package com.deldaran.politics.histoire;

import com.deldaran.politics.Metiers;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Le corps et le foyer du joueur.
 *
 * LA FAIM monte avec le temps (moins vite en dormant). À FAIM_ALERTE, le bandeau prévient ; au-delà, le moral baisse chaque heure ;
 * à FAIM_FAIBLE, on avance moins vite. On mange ce qu'on a dans le sac (le pain, la viande, le fromage…) ou un repas chaud
 * à l'auberge et à la taverne.
 *
 * LE LOGEMENT : on loue (chaque semaine) ou on achète une maison de la ville, celle devant laquelle on se tient. Ses habitants
 * déménagent. On y dort gratuitement, mieux qu'à l'auberge, et on y garde un COFFRE. Un loyer impayé deux semaines de suite : expulsé.
 */
public final class Corps {
    /** Faim gagnée par heure éveillée, par heure de sommeil ; seuils d'alerte et de faiblesse. */
    public static final float FAIM_HEURE = 3.5f, FAIM_SOMMEIL = 1.2f, FAIM_ALERTE = 60f, FAIM_FAIBLE = 85f;
    /** Moral perdu par heure passée au-delà de l'alerte. */
    public static final float MORAL_FAIM = 0.4f;
    /** Repas chaud à l'auberge ou à la taverne : prix, moral gagné. */
    public static final int PRIX_REPAS = 6;
    public static final float MORAL_REPAS = 1f;
    /** Loyer par semaine et prix d'achat d'une maison : base + par habitant de la ville. */
    public static final int LOYER_BASE = 20, ACHAT_BASE = 1500;
    public static final float LOYER_PAR_HABITANT = 0.05f, ACHAT_PAR_HABITANT = 4f;
    /** Moral gagné en dormant chez soi (en plus d'un lit). */
    public static final float MORAL_MAISON = 2f;

    private final Joueur j;
    private final Population p;
    public float faim = 20f;

    /** Phase 11 : l'héritier arrive rassasié ; le temps sauté ne compte pas. */
    void sauter() {
        faim = 20f;
        alerte = 0;
        derniere = Double.NaN;
    }
    private double derniere = Double.NaN;
    private int alerte;
    /** La maison : ville, index du bâtiment dans le village, achetée ou louée, loyer, prochaine échéance, impayés. */
    public int maisonVille = -1, maisonBatiment = -1, loyer, echeance, impayes;
    public boolean proprietaire;
    public final Inventaire coffre = new Inventaire();

    Corps(Joueur j, Population p) {
        this.j = j;
        this.p = p;
    }

    /** Ce que nourrit une portion (0 : ne se mange pas). */
    public static int nourrit(Biens.Bien b) {
        if (b == Biens.PAIN) return 25;
        if (b == Biens.GALETTE) return 35;
        if (b == Biens.VIANDE || b == Biens.FROMAGE || b == Biens.LEGUMES) return 30;
        if (b == Biens.JAMBON) return 80;
        if (b == Biens.POISSON) return 25;
        if (b == Biens.MIEL) return 15;
        if (b == Biens.BIERE) return 8;
        if (b == Biens.VIN) return 5;
        return 0;
    }

    public boolean faible() {
        return faim >= FAIM_FAIBLE;
    }

    /** Le temps passe : la faim monte, le loyer tombe. */
    void avancer() {
        double h = j.maintenant();
        if (Double.isNaN(derniere) || h < derniere) {
            derniere = h;
            return;
        }
        double dt = Math.min(72.0, h - derniere);
        derniere = h;
        boolean faibleAvant = faible();
        faim = Math.min(100f, faim + (float) dt * (j.sommeil != Joueur.EVEILLE ? FAIM_SOMMEIL : FAIM_HEURE));
        if (faim >= FAIM_ALERTE) j.destin().moral = Math.max(0f, j.destin().moral - (float) dt * MORAL_FAIM * (faim - FAIM_ALERTE) / 40f);
        int niveau = faim >= FAIM_FAIBLE ? 2 : faim >= FAIM_ALERTE ? 1 : 0;
        if (niveau > alerte) j.annoncer(niveau == 2 ? "Tu es affamé" + j.fiche().e() + " : tu n'avances plus qu'à grand-peine. Mange (U)." : "Tu as faim. (U : ton sac, ou un repas à l'auberge)");
        alerte = niveau;
        if (faible() != faibleAvant) j.rafraichirSac();
        // Le loyer.
        if (maisonVille >= 0 && !proprietaire) {
            int jour = j.jourMaintenant();
            while (jour >= echeance) {
                echeance += 7;
                if (j.fiche().argent >= loyer) {
                    j.fiche().argent -= loyer;
                    impayes = 0;
                    j.annoncer("Tu paies ton loyer : " + Fiche.argentTexte(loyer) + ".");
                } else if (++impayes >= 2) {
                    j.annoncer("Loyer impayé deux semaines de suite : te voilà à la rue. Le propriétaire garde ce qu'il y avait dans ton coffre.");
                    j.noter("Expulsé" + j.fiche().e() + " de ta maison faute de loyer.");
                    coffre.piles.clear();
                    quitter();
                    break;
                } else j.annoncer("Tu n'as pas de quoi payer ton loyer (" + Fiche.argentTexte(loyer) + "). La semaine prochaine, c'est la porte.");
            }
        }
    }

    // ---------------------------------------------------------------- Manger

    /** « Manger » dans le sac. */
    Joueur.Scene manger(int bien) {
        Biens.Bien b = Biens.get(bien);
        int n = nourrit(b);
        if (n <= 0 || j.fiche().sac.nombre(bien) <= 0) return null;
        avancer();
        j.fiche().sac.retirer(bien, 1);
        float avant = faim;
        faim = Math.max(0f, faim - n);
        alerte = faim >= FAIM_FAIBLE ? 2 : faim >= FAIM_ALERTE ? 1 : 0;
        j.gesteJoueur = b.categorie == Biens.VIVRES && (b == Biens.BIERE || b == Biens.VIN) ? "boire" : "manger_debout";
        j.rafraichirSac();
        j.enregistrer();
        String etat = faim < 15f ? "Tu es rassasié" + j.fiche().e() + "." : faim < FAIM_ALERTE ? "Ça va mieux." : "Tu as encore faim.";
        return j.objetScene(bien, "Tu manges " + b.un() + ". " + etat + (avant < 10f ? " (Tu n'avais pas vraiment faim.)" : ""));
    }

    /** Un repas chaud à l'auberge ou à la taverne (dans la boutique). */
    Joueur.Scene repas(EconomieVille.Agent a, int lieu) {
        Fiche f = j.fiche();
        if (f.argent < PRIX_REPAS) return j.boutiqueScene(lieu);
        avancer();
        f.argent -= PRIX_REPAS;
        a.caisse += PRIX_REPAS;
        a.ventes += PRIX_REPAS;
        faim = 0f;
        alerte = 0;
        j.destin().moral = Math.min(100f, j.destin().moral + MORAL_REPAS);
        j.gesteJoueur = "manger_assis";
        j.passer(0.5);
        j.rafraichirSac();
        j.enregistrer();
        j.annoncer("Un bol de soupe, du pain, de la viande : te voilà rassasié" + f.e() + ".");
        Joueur.Scene s = new Joueur.Scene("", "");
        s.fermer = true;
        return s;
    }

    // ---------------------------------------------------------------- Le logement

    public boolean aUneMaison() {
        return maisonVille >= 0;
    }

    private int loyerIci(int v) {
        return Math.round(LOYER_BASE + LOYER_PAR_HABITANT * p.villes.habitants[v]);
    }

    private int prixIci(int v) {
        return Math.round(ACHAT_BASE + ACHAT_PAR_HABITANT * p.villes.habitants[v]);
    }

    /** « Chercher un logement » / « Ma maison ». */
    Joueur.Scene logement(String message) {
        Fiche f = j.fiche();
        int v = j.villeIci();
        StringBuilder t = new StringBuilder();
        Joueur.Scene s = new Joueur.Scene("", "");
        if (aUneMaison()) {
            t.append("Ta maison est à ").append(Noms.ville(p.villes.peuple[maisonVille], p.graine, maisonVille))
                    .append(proprietaire ? ". Elle est à toi." : ". Tu la loues " + Fiche.argentTexte(loyer) + " par semaine.");
            if (!proprietaire && impayes > 0) t.append(" Tu as un loyer de retard.");
            t.append("\nTu y dors (Z devant la porte) et tu y gardes un coffre (M devant la porte) : ").append(coffre.piles.isEmpty() ? "il est vide." : coffre.piles.size() + " sortes d'objets.");
            if (!proprietaire && v == maisonVille && f.argent >= prixIci(v) - loyer * 4) s.ajouter("L'acheter (" + Fiche.argentTexte(prixIci(v) - loyer * 4) + ")", () -> {
                f.argent -= prixIci(v) - loyer * 4;
                proprietaire = true;
                j.noter("Tu achètes ta maison.");
                j.enregistrer();
                return logement("La maison est à toi. Plus de loyer.");
            });
            s.ajouter(proprietaire ? "La vendre (" + Fiche.argentTexte(prixIci(maisonVille) * 7 / 10) + ")" : "Rendre les clés", () -> {
                if (proprietaire) f.argent += prixIci(maisonVille) * 7 / 10;
                for (Inventaire.Pile pile : coffre.piles) f.sac.ajouter(pile.bien, pile.nombre);
                coffre.piles.clear();
                quitter();
                j.rafraichirSac();
                j.enregistrer();
                return logement("Tu rends la maison. Ce qu'il y avait dans le coffre est dans ton sac.");
            });
        } else if (v < 0) t.append("Tu n'as pas de maison. Il faut être en ville pour en chercher une.");
        else {
            int q = j.maisonProche;
            t.append("Tu n'as pas de maison. Une maison ").append(Creation.de(Noms.ville(p.villes.peuple[v], p.graine, v))).append(" se loue ").append(Fiche.argentTexte(loyerIci(v)))
                    .append(" par semaine, ou s'achète ").append(Fiche.argentTexte(prixIci(v))).append('.');
            if (q < 0 || j.maisonDistance > Joueur.PROCHE * 2f) t.append("\nPlace-toi devant la maison que tu veux : ses habitants déménageront.");
            else {
                t.append("\nLa maison devant toi est à prendre.");
                if (f.argent >= loyerIci(v)) s.ajouter("La louer (" + Fiche.argentTexte(loyerIci(v)) + " par semaine, la première d'avance)", () -> prendre(v, q, false));
                if (f.argent >= prixIci(v)) s.ajouter("L'acheter (" + Fiche.argentTexte(prixIci(v)) + ")", () -> prendre(v, q, true));
            }
        }
        s.ajouter("« Revenir", j::accueilScene);
        Joueur.Scene vraie = new Joueur.Scene(aUneMaison() ? "Ma maison" : "Un logement", Joueur.avec(message, t.toString()));
        vraie.choix.addAll(s.choix);
        vraie.suites.addAll(s.suites);
        return vraie;
    }

    private Joueur.Scene prendre(int v, int q, boolean achat) {
        Fiche f = j.fiche();
        if (achat) f.argent -= prixIci(v);
        else {
            loyer = loyerIci(v);
            f.argent -= loyer;
            echeance = j.jourMaintenant() + 7;
            impayes = 0;
        }
        maisonVille = v;
        maisonBatiment = q;
        proprietaire = achat;
        j.maisonVue = new int[] { v, q };
        j.noter((achat ? "Tu achètes" : "Tu loues") + " une maison " + Creation.de(Noms.ville(p.villes.peuple[v], p.graine, v)) + ".");
        j.destin().moral = Math.min(100f, j.destin().moral + 5f);
        j.enregistrer();
        return logement(achat ? "La maison est à toi. Ses anciens habitants emportent leurs affaires." : "Le propriétaire te tend la clé. Les anciens occupants s'en vont.");
    }

    private void quitter() {
        maisonVille = -1;
        maisonBatiment = -1;
        proprietaire = false;
        impayes = 0;
        j.maisonVue = null;
    }

    /** Devant sa porte : on est chez soi. */
    public boolean chezSoi() {
        return aUneMaison() && j.devantMaison;
    }

    /** Le coffre (M devant sa porte). */
    Joueur.Scene coffre(String message) {
        Fiche f = j.fiche();
        Joueur.Scene s = new Joueur.Scene("", "");
        for (Inventaire.Pile pile : new ArrayList<>(coffre.piles)) {
            Biens.Bien b = Biens.get(pile.bien);
            s.ajouter("Prendre : " + b.majuscule() + (pile.nombre > 1 ? " ×" + pile.nombre : ""), () -> {
                int n = coffre.retirer(b.id, coffre.nombre(b.id));
                f.sac.ajouter(b.id, n);
                j.rafraichirSac();
                j.enregistrer();
                return coffre("Tu prends " + b.quantite(n) + ".");
            });
        }
        for (Inventaire.Pile pile : new ArrayList<>(f.sac.piles)) {
            Biens.Bien b = Biens.get(pile.bien);
            int libres = pile.nombre - (pile.porte ? 1 : 0);
            if (libres <= 0) continue;
            s.ajouter("Ranger : " + b.majuscule() + (libres > 1 ? " ×" + libres : ""), () -> {
                int n = f.sac.retirer(b.id, libres);
                coffre.ajouter(b.id, n);
                j.rafraichirSac();
                j.enregistrer();
                return coffre("Tu ranges " + b.quantite(n) + " dans le coffre.");
            });
        }
        j.lignee().choixMaison(s);
        s.ajouter("« Revenir", j::accueilScene);
        j.gesteJoueur = "fouiller_coffre";
        Joueur.Scene vraie = new Joueur.Scene("Mon coffre", Joueur.avec(message, coffre.piles.isEmpty() ? "Le coffre est vide." : "Ce que tu gardes chez toi."));
        vraie.choix.addAll(s.choix);
        vraie.suites.addAll(s.suites);
        return vraie;
    }

    void ecrire(DataOutputStream s) throws IOException {
        s.writeFloat(faim);
        s.writeInt(maisonVille);
        s.writeInt(maisonBatiment);
        s.writeBoolean(proprietaire);
        s.writeInt(loyer);
        s.writeInt(echeance);
        s.writeInt(impayes);
        s.writeInt(coffre.piles.size());
        for (Inventaire.Pile pile : coffre.piles) {
            s.writeInt(pile.bien);
            s.writeInt(pile.nombre);
        }
    }

    void lire(DataInputStream s) throws IOException {
        faim = s.readFloat();
        maisonVille = s.readInt();
        maisonBatiment = s.readInt();
        proprietaire = s.readBoolean();
        loyer = s.readInt();
        echeance = s.readInt();
        impayes = s.readInt();
        int n = s.readInt();
        for (int k = 0; k < n; k++) coffre.ajouter(s.readInt(), s.readInt());
    }
}
