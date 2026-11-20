package com.deldaran.politics.histoire;

import com.deldaran.politics.Metiers;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * L'économie d'une ville, simulée jour par jour.
 *
 * Chaque « agent » est un lieu de commerce : une ferme, le moulin, la forge, les étals du marché…
 * Il a des ouvriers, une caisse, un stock, un coût de revient et un prix de vente pour chaque bien.
 * Chaque jour :
 *   1. les ateliers produisent avec leurs recettes (ils paient les salaires aux foyers) ;
 *   2. ils rachètent leurs matières et les marchands se réapprovisionnent chez le fournisseur le moins cher
 *      (ou chez les colporteurs, qui importent au prix de la région plus le transport) ;
 *   3. les foyers achètent ce qu'ils consomment, surtout là où c'est facile d'accès (la place) et pas cher ;
 *   4. les surplus partent à l'export, les prix suivent le manque ou l'excès, les ateliers embauchent ou renvoient.
 * Plus il y a d'intermédiaires, plus c'est cher : le moulin vend sa farine près du coût, l'étal du marché la revend avec sa marge.
 */
public final class EconomieVille {
    public static final float GROS = 0.9f;
    public static final float IMPORT = 1.35f, EXPORT = 0.7f;
    public static final float JOURS_STOCK = 4f, JOURS_MATIERE = 5f;
    public static final float LISSAGE_PRIX = 0.25f, LISSAGE_DEMANDE = 0.15f;
    public static final float BUDGET_JOUR = 0.15f, REVENU_AUTRE = 0.6f;
    public static final int RODAGE = 60, RATTRAPAGE_MAX = 60;
    public static final float PART_PAYSANS = 0.12f, PART_BUCHERONS = 0.03f, PART_MINEURS = 0.02f, OUVRIERS_PAR_ATELIER = 2f, MAX_PAR_ATELIER = 15f;
    /** Atelier du joueur (phase 7) : rendement quand le patron est absent sans intendant, part de la caisse volée chaque jour dans ce cas. */
    public static final float ABSENT_RENDEMENT = 0.8f, VOL_ABSENT = 0.01f;
    /** L'intendant renvoie un ouvrier par jour tant que l'ouvrage est sous OUVRAGE_MIN, et embauche au-dessus d'OUVRAGE_MAX (moins, s'il est habile). */
    public static final float OUVRAGE_MIN = 0.7f, OUVRAGE_MAX = 0.92f;
    /** Chance par jour qu'un ouvrier mal payé (salaire bas) s'en aille. */
    public static final float DEPART_MAL_PAYES = 0.04f;
    /** Salaire de l'intendant, par jour (cuivre). */
    public static final int INTENDANT_SALAIRE = 12;
    /** L'intendant garde en caisse de quoi payer ce nombre de jours de salaires avant d'embaucher. */
    public static final int JOURS_PRUDENCE = 10;

    public final class Agent {
        public final int lieu;
        public final String nom;
        public final List<Recettes.Recette> recettes;
        public final boolean[] vend, achete, fabrique;
        public final float[] stock, prix, cout, demande, cible;
        /** Ce que l'agent fait venir des colporteurs chaque jour (moyenne lissée) : c'est là que naissent les missions de livraison. */
        public final float[] importe;
        final float[] demandeJour, importeJour;
        public int batiments;
        public float ouvriers, emploi = 0.5f;
        public double caisse;

        // ---- Atelier du joueur (phase 7)
        /** L'atelier appartient au joueur : il paie ses ouvriers chaque jour, la ville ne le renfloue pas, il n'embauche que sur ordre (ou par l'intendant). */
        public boolean joueur;
        /** Multiplicateurs choisis par le patron : marge sur les prix, salaire des ouvriers. */
        public float margeJoueur = 1f, salaireJoueur = 1f;
        /** Compétence Intendance de l'intendant (0 : pas d'intendant). */
        public float intendance;
        /** Jusqu'à ce jour, le patron est considéré comme présent (il passe régulièrement). */
        public int presenceJusqua;
        /** Recettes arrêtées par le patron (même ordre que recettes). */
        public final boolean[] arret;
        /** Comptes depuis la dernière lecture : ventes, achats de matières, salaires, vols. */
        public double ventes, achats, salaires, pertes;
        /** Jours de suite sans pouvoir payer, et ouvriers partis depuis la dernière lecture. */
        public int impayes, partis;
        /** Lot D : fiabilité moyenne des ouvriers nommés (multiplie l'ouvrage), grève (rien ne se fait, personne n'est payé). */
        public float fiabilite = 1f;
        public boolean greve;
        /** Lot H5 : les journées d'ouvrage déjà faites aujourd'hui sous les yeux du joueur (retirées de la journée de l'atelier). */
        public float faitVu;

        Agent(int lieu, int batiments) {
            this.lieu = lieu;
            this.nom = Biens.nomBoutique(lieu);
            this.batiments = batiments;
            this.recettes = Recettes.de(lieu);
            this.arret = new boolean[recettes.size()];
            int n = Biens.TOUS.size();
            vend = new boolean[n];
            achete = new boolean[n];
            fabrique = new boolean[n];
            stock = new float[n];
            prix = new float[n];
            cout = new float[n];
            demande = new float[n];
            demandeJour = new float[n];
            importe = new float[n];
            importeJour = new float[n];
            cible = new float[n];
            for (Recettes.Recette r : recettes) {
                for (Biens.Bien b : r.sorties) {
                    fabrique[b.id] = true;
                    vend[b.id] = true;
                }
                for (Biens.Bien b : r.entrees) achete[b.id] = true;
            }
            for (Biens.Bien b : Biens.TOUS) {
                if (!b.venduPar(lieu)) continue;
                vend[b.id] = true;
                if (!fabrique[b.id]) achete[b.id] = true;
            }
            for (int b = 0; b < n; b++) {
                if (!vend[b] && !achete[b]) continue;
                float ref = reference(b);
                cout[b] = ref * 0.85f;
                prix[b] = ref * (1f + marge());
            }
        }

        public float marge() {
            return joueur ? margeBase() * margeJoueur : margeBase();
        }

        float margeBase() {
            switch (lieu) {
                case Biens.CHAMPS: case Biens.FORET: case Biens.MINE: return 0.10f;
                case Biens.MARCHE: return 0.35f;
                case Metiers.GRENIER: return 0.12f;
                case Metiers.ENTREPOT: return 0.30f;
                case Metiers.AUBERGE: case Metiers.TAVERNE: return 0.40f;
                default: return 0.15f;
            }
        }

        /** Facilité pour un habitant d'aller acheter là : 1 sur la place, peu pour un moulin au bord de l'eau ou une ferme hors des murs. */
        public float acces() {
            switch (lieu) {
                case Biens.MARCHE: return 1f;
                case Biens.CHAMPS: case Biens.FORET: case Biens.MINE: return 0.06f;
                case Biens.DEHORS: return 0f;
                case Metiers.BOULANGERIE: case Metiers.BOUCHERIE: return 0.7f;
                default:
                    switch (Metiers.LIEU[lieu]) {
                        case Metiers.PLACE: return 0.9f;
                        case Metiers.GRANDE_RUE: return 0.6f;
                        case Metiers.RUE: return 0.5f;
                        case Metiers.BORD: return 0.15f;
                        case Metiers.RIVE: return 0.2f;
                        default: return 0.05f;
                    }
            }
        }

        public boolean source() {
            return lieu == Biens.CHAMPS || lieu == Biens.FORET || lieu == Biens.MINE;
        }

        public float maxOuvriers() {
            return source() ? habitants * 0.5f : Math.max(1, batiments) * MAX_PAR_ATELIER;
        }

        float reserve() {
            return ouvriers * Recettes.SALAIRE * 20f + 300f;
        }

        /** Avance que l'atelier peut prendre pour payer ses ouvriers avant de vendre. */
        float credit() {
            return ouvriers * Recettes.SALAIRE * 10f;
        }

        /** Ce qu'il peut dépenser en matières sans toucher aux salaires des trois prochains jours. */
        double disponible() {
            return caisse - ouvriers * Recettes.SALAIRE * 3f;
        }

        void ajouter(int b, float q, float coutUnitaire) {
            if (q <= 0f) return;
            float s = Math.max(0f, stock[b]);
            cout[b] = (s * cout[b] + q * coutUnitaire) / (s + q);
            stock[b] = s + q;
        }
    }

    private final Population p;
    public final int ville;
    public final int habitants;
    public final boolean garnison;
    public final List<Agent> agents = new ArrayList<>();
    public double bourse;
    public int jour, renflouements;
    private final float[] referenceLocale;

    EconomieVille(Population p, int ville, Metiers.Profil profil, float fertilite, float boise, float mines) {
        this.p = p;
        this.ville = ville;
        this.habitants = Math.max(10, p.villes.habitants[ville]);
        int n = Biens.TOUS.size();
        referenceLocale = new float[n];
        int r = p.villes.region[ville];
        for (Biens.Bien b : Biens.TOUS) {
            float f = 1f;
            if (b.groupe >= 0 && r >= 0 && r < p.economie.prix[b.groupe].length) {
                float moyen = p.economie.prixMoyen(b.groupe);
                if (moyen > 0f) f = 1f + b.part * (Math.max(0.6f, Math.min(2f, p.economie.prix[b.groupe][r] / moyen)) - 1f);
            }
            referenceLocale[b.id] = b.prix * f;
        }
        int[] besoins = Metiers.besoins(profil);
        boolean caserne = false;
        agents.add(new Agent(Biens.MARCHE, Math.max(1, Metiers.etals(profil))));
        for (int m = 0; m < Metiers.NOMBRE; m++) {
            int nb = 0;
            for (int x : besoins) if (x == m) nb++;
            if (m == Metiers.CASERNE && nb > 0) caserne = true;
            if (nb == 0) continue;
            Agent a = new Agent(m, nb);
            boolean utile = false;
            for (boolean v : a.vend) utile |= v;
            if (utile) agents.add(a);
        }
        garnison = caserne;
        Agent champs = new Agent(Biens.CHAMPS, 0);
        champs.ouvriers = habitants * PART_PAYSANS * fertilite;
        agents.add(champs);
        if (boise > 0f) {
            Agent bois = new Agent(Biens.FORET, 0);
            bois.ouvriers = habitants * PART_BUCHERONS * boise;
            agents.add(bois);
        }
        if (mines > 0f) {
            Agent mine = new Agent(Biens.MINE, 0);
            mine.ouvriers = habitants * PART_MINEURS * mines;
            agents.add(mine);
        }
        for (Agent a : agents) {
            if (!a.source()) a.ouvriers = a.lieu == Biens.MARCHE ? a.batiments : a.batiments * OUVRIERS_PAR_ATELIER;
            a.ouvriers = Math.max(0.5f, a.ouvriers);
            a.caisse = a.ouvriers * Recettes.SALAIRE * 30f + 200f;
        }
        bourse = habitants * 15.0;
    }

    public float reference(int b) {
        return referenceLocale[b];
    }

    public float prixImport(int b) {
        return referenceLocale[b] * IMPORT;
    }

    public float prixExport(int b) {
        return referenceLocale[b] * EXPORT;
    }

    /** L'agent de la ville pour ce lieu (celui du joueur seulement s'il n'y en a pas d'autre). */
    public Agent agent(int lieu) {
        Agent j = null;
        for (Agent a : agents) {
            if (a.lieu != lieu) continue;
            if (!a.joueur) return a;
            if (j == null) j = a;
        }
        return j;
    }

    /** L'atelier du joueur pour ce lieu, ou null. */
    public Agent agentJoueur(int lieu) {
        for (Agent a : agents) if (a.joueur && a.lieu == lieu) return a;
        return null;
    }

    /** Un atelier neuf pour le joueur : pas d'ouvriers, caisse vide. */
    public Agent ajouterJoueur(int lieu) {
        Agent a = new Agent(lieu, 1);
        a.joueur = true;
        a.ouvriers = 0f;
        a.caisse = 0.0;
        agents.add(a);
        return a;
    }

    /** Ce que les clients achètent à cet atelier par jour (moyenne lissée), et ce qu'il en tire une fois les coûts payés. */
    public double ventesJour(Agent a) {
        double v = 0.0;
        for (int b = 0; b < a.stock.length; b++) if (a.fabrique[b] || a.vend[b]) v += a.demande[b] * a.prix[b];
        return v;
    }

    public double beneficeJour(Agent a) {
        double v = 0.0;
        for (int b = 0; b < a.stock.length; b++) if (a.fabrique[b] || a.vend[b]) v += a.demande[b] * (a.prix[b] - a.cout[b]);
        return v;
    }

    /**
     * Ce que vaut un bâtiment de cet atelier : les murs (prixBatiment), « joursBenefice » jours de bénéfice,
     * plus sa part du stock (au coût) et de la caisse.
     */
    public double valeur(Agent a, int prixBatiment, int joursBenefice) {
        float part = a.joueur ? 1f : 1f / Math.max(1, a.batiments);
        double v = prixBatiment + Math.max(0.0, beneficeJour(a)) * part * joursBenefice;
        for (int b = 0; b < a.stock.length; b++) v += Math.max(0f, a.stock[b]) * a.cout[b] * part;
        return v + Math.max(0.0, a.caisse) * part;
    }

    /**
     * Le joueur rachète un bâtiment de cet atelier. S'il n'y en a qu'un, l'atelier entier (ouvriers, stock, caisse) devient le sien ;
     * sinon il en prend une part : un bâtiment, et autant d'ouvriers, de stock et de caisse.
     */
    public Agent racheter(Agent a) {
        if (a.joueur) return a;
        if (a.batiments <= 1) {
            a.joueur = true;
            a.ouvriers = Math.max(1f, Math.round(a.ouvriers));
            a.presenceJusqua = jourCourant;
            return a;
        }
        Agent n = ajouterJoueur(a.lieu);
        float part = 1f / a.batiments;
        n.ouvriers = Math.max(1f, Math.round(a.ouvriers * part));
        a.ouvriers = Math.max(1f, a.ouvriers - n.ouvriers);
        for (int b = 0; b < a.stock.length; b++) {
            float q = Math.max(0f, a.stock[b]) * part;
            n.stock[b] = q;
            a.stock[b] -= q;
            n.cout[b] = a.cout[b];
            n.prix[b] = a.prix[b];
            n.demande[b] = a.demande[b] * part;
            a.demande[b] -= n.demande[b];
        }
        n.emploi = a.emploi;
        n.caisse = Math.max(0.0, a.caisse * part);
        a.caisse -= n.caisse;
        a.batiments--;
        n.presenceJusqua = jourCourant;
        return n;
    }

    /** Le joueur revend son atelier à la ville : il rejoint l'atelier du même métier, ou redevient celui d'un patron du pays. */
    public void rendre(Agent a) {
        if (!a.joueur) return;
        Agent pnj = null;
        for (Agent x : agents) if (x != a && x.lieu == a.lieu && !x.joueur) pnj = x;
        if (pnj == null) {
            a.joueur = false;
            a.margeJoueur = 1f;
            a.salaireJoueur = 1f;
            a.intendance = 0f;
            java.util.Arrays.fill(a.arret, false);
            a.ouvriers = Math.max(1f, a.ouvriers);
            a.caisse = Math.max(a.caisse, a.ouvriers * Recettes.SALAIRE * 30f + 200f);
            return;
        }
        pnj.batiments += a.batiments;
        pnj.ouvriers += a.ouvriers;
        for (int b = 0; b < a.stock.length; b++) {
            if (a.stock[b] > 0f) pnj.ajouter(b, a.stock[b], a.cout[b]);
            pnj.demande[b] += a.demande[b];
        }
        agents.remove(a);
    }

    /** Le fournisseur le moins cher pour un bien (null = les colporteurs, au prix d'import). */
    Agent fournisseur(int b, Agent acheteur) {
        Agent mieux = null;
        float prixMieux = prixImport(b);
        for (Agent a : agents) {
            if (a == acheteur || !a.vend[b] || a.stock[b] < 1f) continue;
            if (acheteur != null && !acheteur.source() && a.lieu == Biens.MARCHE && acheteur.lieu != Metiers.ENTREPOT) continue;
            float px = a.prix[b] * GROS;
            if (px < prixMieux) {
                prixMieux = px;
                mieux = a;
            }
        }
        return mieux;
    }

    /** Ce qu'un acheteur en gros paierait aujourd'hui pour ce bien. */
    public float meilleurPrixGros(int b, Agent acheteur) {
        Agent f = fournisseur(b, acheteur);
        return f == null ? prixImport(b) : f.prix[b] * GROS;
    }

    public void avancerJusqua(int cible) {
        if (cible <= jour) return;
        int debut = Math.max(jour, cible - RATTRAPAGE_MAX);
        for (int j = debut; j < cible; j++) {
            jourCourant = j;
            unJour();
        }
        jour = cible;
        jourCourant = cible;
    }

    /** Le jour en cours de simulation. */
    private int jourCourant;

    void roder() {
        for (int k = 0; k < RODAGE; k++) unJour();
    }

    void unJour() {
        bourse += habitants * REVENU_AUTRE;
        for (Agent a : agents) cibles(a);
        for (Agent a : agents) produire(a);
        for (Agent a : agents) approvisionner(a);
        foyers();
        for (Agent a : agents) {
            exporter(a);
            fixerPrix(a);
            gerer(a);
        }
    }

    private void cibles(Agent a) {
        int n = a.stock.length;
        for (int b = 0; b < n; b++) {
            a.demande[b] += (a.demandeJour[b] - a.demande[b]) * LISSAGE_DEMANDE;
            a.demandeJour[b] = 0f;
            a.importe[b] += (a.importeJour[b] - a.importe[b]) * LISSAGE_DEMANDE;
            a.importeJour[b] = 0f;
            float minimum = Biens.get(b).prix < 50 ? 4f : 1f;
            a.cible[b] = a.vend[b] ? Math.max(minimum, JOURS_STOCK * a.demande[b]) : 0f;
        }
        for (int k0 = 0; k0 < a.recettes.size(); k0++) {
            if (a.arret[k0]) continue;
            Recettes.Recette r = a.recettes.get(k0);
            int principal = r.sorties.get(0).id;
            float lots = a.demande[principal] / r.qs.get(0);
            float manque = Math.max(0f, a.cible[principal] - a.stock[principal]) / r.qs.get(0);
            if (lots <= 0.001f && manque <= 0f) continue;
            // Sans commande, on ne prépare un objet pour la vitrine que si on peut se le permettre.
            if (lots <= 0.001f && r.valeurEntrees() > a.disponible() * 0.3) continue;
            for (int k = 0; k < r.entrees.size(); k++) a.cible[r.entrees.get(k).id] += (JOURS_MATIERE * lots + Math.min(manque, 1f)) * r.qe.get(k);
        }
    }

    private void produire(Agent a) {
        if (a.joueur) {
            produireJoueur(a);
            return;
        }
        if (a.recettes.isEmpty() || (a.caisse < -a.credit() && !a.source())) return;
        float utilise = fabriquer(a, a.ouvriers, true);
        a.emploi += (utilise / Math.max(0.01f, a.ouvriers) - a.emploi) * 0.2f;
    }

    /** Vrai si le patron n'est pas passé depuis un moment. */
    public boolean absent(Agent a) {
        return jourCourant > a.presenceJusqua;
    }

    /** Rendement de l'atelier du joueur : salaire (mal payés : ×0,9, bien payés : ×1,1), intendant, absence du patron. */
    public float rendementJoueur(Agent a) {
        float r = a.salaireJoueur >= 1.2f ? 1.1f : a.salaireJoueur < 0.9f ? 0.9f : 1f;
        if (a.intendance > 0f) r *= 1f + a.intendance / 500f;
        else if (absent(a)) r *= ABSENT_RENDEMENT;
        return r * a.fiabilite;
    }

    /** L'atelier du joueur paie tous ses ouvriers chaque jour, qu'ils aient de l'ouvrage ou non (plus le supplément s'il paie bien). */
    private void produireJoueur(Agent a) {
        float utilise = 0f;
        // Lot H5 : ce qui s'est fait sous les yeux du joueur compte dans la journée (et a déjà été payé à l'ouvrage).
        float capacite = Math.max(0f, a.ouvriers * rendementJoueur(a) - a.faitVu);
        if (!a.recettes.isEmpty() && a.caisse >= 0.0 && capacite > 0f && !a.greve) utilise = fabriquer(a, capacite, true);
        utilise += a.faitVu;
        a.faitVu = 0f;
        double du = a.greve ? 0.0 : a.ouvriers * Recettes.SALAIRE * a.salaireJoueur - utilise * Recettes.SALAIRE;
        if (du > 0.0) {
            a.caisse -= du;
            bourse += du;
            a.salaires += du;
        }
        if (a.intendance > 0f) {
            a.caisse -= INTENDANT_SALAIRE;
            bourse += INTENDANT_SALAIRE;
            a.salaires += INTENDANT_SALAIRE;
        }
        a.emploi += (utilise / Math.max(0.01f, a.ouvriers) - a.emploi) * 0.2f;
    }

    /**
     * Le joueur travaille dans l'atelier : des journées de travail en plus de celles des ouvriers, sans salaire versé aux foyers
     * (le joueur est payé à part, par son contrat). Renvoie les journées vraiment employées : 0 si l'atelier n'avait rien à faire.
     */
    public float travailJoueur(Agent a, float jours) {
        if (a.recettes.isEmpty() || jours <= 0f) return 0f;
        return fabriquer(a, jours, false);
    }

    /**
     * Lot H5 : des ouvriers du joueur ont travaillé sous ses yeux : leur ouvrage se fait tout de suite (la matière baisse, le produit
     * monte) et sera retiré de la journée de l'atelier. Renvoie les journées vraiment employées.
     */
    public float ouvrageVu(Agent a, float jours) {
        if (!a.joueur || a.greve || a.recettes.isEmpty() || a.caisse < 0.0 || jours <= 0f) return 0f;
        float reste = Math.max(0f, a.ouvriers * rendementJoueur(a) - a.faitVu);
        float u = fabriquer(a, Math.min(jours * rendementJoueur(a), reste), true);
        a.faitVu += u;
        return u;
    }

    /** Produit avec « capacite » journées de travail ; renvoie celles qui ont servi. */
    private float fabriquer(Agent a, float capacite, boolean salaires) {
        float utilise = 0f;
        List<Recettes.Recette> ordre = new ArrayList<>(a.recettes);
        // D'abord les produits intermédiaires (le fer avant l'épée), puis les plus rentables.
        ordre.sort((x, y) -> {
            int ix = intermediaire(a, x) ? 0 : 1, iy = intermediaire(a, y) ? 0 : 1;
            return ix != iy ? Integer.compare(ix, iy) : Float.compare(rentabilite(a, y), rentabilite(a, x));
        });
        for (Recettes.Recette r : ordre) {
            if (capacite - utilise <= 1e-3f) break;
            if (a.arret[a.recettes.indexOf(r)]) continue;
            int principal = r.sorties.get(0).id;
            float manque = a.cible[principal] - a.stock[principal];
            if (manque <= 0f) continue;
            float lots = Math.min(manque / r.qs.get(0), (capacite - utilise) / r.jours);
            for (int k = 0; k < r.entrees.size(); k++) lots = Math.min(lots, Math.max(0f, a.stock[r.entrees.get(k).id]) / r.qe.get(k));
            if (lots <= 1e-3f) continue;
            float coutTotal = 0f;
            for (int k = 0; k < r.entrees.size(); k++) {
                int e = r.entrees.get(k).id;
                float q = lots * r.qe.get(k);
                a.stock[e] -= q;
                coutTotal += q * a.cout[e];
            }
            float jours = lots * r.jours;
            utilise += jours;
            if (salaires) {
                float paye = jours * Recettes.SALAIRE;
                a.caisse -= paye;
                bourse += paye;
                coutTotal += paye;
                if (a.joueur) a.salaires += paye;
            }
            float valeur = r.valeurSorties();
            for (int k = 0; k < r.sorties.size(); k++) {
                Biens.Bien s = r.sorties.get(k);
                float q = lots * r.qs.get(k);
                float part = s.prix * r.qs.get(k) / valeur;
                a.ajouter(s.id, q, coutTotal * part / q);
            }
        }
        return utilise;
    }

    private static boolean intermediaire(Agent a, Recettes.Recette r) {
        Biens.Bien s = r.sorties.get(0);
        for (Recettes.Recette autre : a.recettes) if (autre != r && autre.entrees.contains(s)) return true;
        return false;
    }

    private float rentabilite(Agent a, Recettes.Recette r) {
        float v = 0f;
        for (int k = 0; k < r.sorties.size(); k++) v += a.prix[r.sorties.get(k).id] * r.qs.get(k);
        for (int k = 0; k < r.entrees.size(); k++) v -= a.cout[r.entrees.get(k).id] * r.qe.get(k);
        return (v - r.jours * Recettes.SALAIRE) / Math.max(0.01f, r.jours);
    }

    private void approvisionner(Agent a) {
        if (a.lieu == Biens.DEHORS) return;
        for (int b = 0; b < a.stock.length; b++) {
            if (!a.achete[b] || a.fabrique[b]) continue;
            float manque = a.cible[b] - a.stock[b];
            // La commande est connue du producteur local même s'il n'a plus rien : c'est ce qui le fait produire et embaucher.
            Agent producteur = null;
            for (Agent x : agents) if (x != a && x.fabrique[b] && (producteur == null || x.prix[b] < producteur.prix[b])) producteur = x;
            if (producteur != null && manque > 0f) producteur.demandeJour[b] += manque;
            for (int essai = 0; essai < 3 && manque > 0.05f && a.disponible() > 1.0; essai++) {
                Agent f = fournisseur(b, a);
                float px = f == null ? prixImport(b) : f.prix[b] * GROS;
                float q = Math.min(manque, (float) (a.disponible() / px));
                if (f != null) {
                    if (f != producteur) f.demandeJour[b] += manque;
                    q = Math.min(q, f.stock[b]);
                }
                if (q <= 0.01f) break;
                if (f == null) a.importeJour[b] += q;
                transferer(f, a, b, q, px);
                manque -= q;
            }
        }
    }

    private void transferer(Agent vendeur, Agent acheteur, int b, float q, float px) {
        double total = q * (double) px;
        if (vendeur != null) {
            vendeur.stock[b] -= q;
            vendeur.caisse += total;
            if (vendeur.joueur) vendeur.ventes += total;
        }
        acheteur.caisse -= total;
        if (acheteur.joueur) acheteur.achats += total;
        acheteur.ajouter(b, q, px);
    }

    private void foyers() {
        int n = Biens.TOUS.size();
        float[] voulu = new float[n];
        double depense = 0.0;
        for (int b = 0; b < n; b++) {
            float c = Recettes.CONSO[b] + (garnison ? Recettes.CONSO_GARNISON[b] : 0f);
            if (c <= 0f) continue;
            float attrait = 0f, prixMoyen = 0f;
            for (Agent a : agents) {
                if (!a.vend[b] || a.acces() <= 0f) continue;
                float w = attrait(a, b);
                attrait += w;
                prixMoyen += w * a.prix[b];
            }
            if (attrait <= 0f) continue;
            prixMoyen /= attrait;
            float elasticite = (float) Math.pow(reference(b) / Math.max(0.01f, prixMoyen), 0.6);
            voulu[b] = habitants * c * Math.max(0.3f, Math.min(1.5f, elasticite));
            depense += voulu[b] * prixMoyen;
        }
        double budget = Math.max(0.0, bourse * BUDGET_JOUR);
        float echelle = depense > budget ? (float) (budget / depense) : 1f;
        for (int b = 0; b < n; b++) {
            if (voulu[b] <= 0f) continue;
            float q = voulu[b] * echelle;
            float attrait = 0f;
            for (Agent a : agents) if (a.vend[b] && a.acces() > 0f) attrait += attrait(a, b);
            float reste = 0f;
            for (Agent a : agents) {
                if (!a.vend[b] || a.acces() <= 0f) continue;
                float part = q * attrait(a, b) / attrait;
                a.demandeJour[b] += part;
                float pris = Math.min(part, Math.max(0f, a.stock[b]));
                vendre(a, b, pris);
                reste += part - pris;
            }
            for (Agent a : agents) {
                if (reste <= 0.01f) break;
                if (!a.vend[b] || a.acces() <= 0f || a.stock[b] < 0.01f) continue;
                float pris = Math.min(reste, a.stock[b]);
                vendre(a, b, pris);
                reste -= pris;
            }
        }
    }

    private float attrait(Agent a, int b) {
        float relatif = a.prix[b] / Math.max(0.01f, reference(b));
        return a.acces() / (relatif * relatif * relatif);
    }

    private void vendre(Agent a, int b, float q) {
        if (q <= 0f) return;
        double total = q * (double) a.prix[b];
        a.stock[b] -= q;
        a.caisse += total;
        bourse -= total;
        if (a.joueur) a.ventes += total;
    }

    private void exporter(Agent a) {
        for (int b = 0; b < a.stock.length; b++) {
            if (!a.fabrique[b]) continue;
            float trop = a.stock[b] - 3f * a.cible[b] - 5f;
            if (trop <= 0f) continue;
            float q = a.stock[b] - 2f * a.cible[b];
            a.stock[b] -= q;
            a.caisse += q * (double) prixExport(b);
            if (a.joueur) a.ventes += q * (double) prixExport(b);
        }
    }

    private void fixerPrix(Agent a) {
        for (int b = 0; b < a.stock.length; b++) {
            if (!a.vend[b]) continue;
            float base = Math.max(0.01f, a.cout[b]) * (1f + a.marge());
            float pression = (float) Math.pow((a.cible[b] + 1f) / (Math.max(0f, a.stock[b]) + 1f), 0.35);
            pression = Math.max(0.6f, Math.min(2.5f, pression));
            float voulu = Math.min(base * pression, prixImport(b) * (1f + a.marge()));
            boolean encombre = a.stock[b] > 2f * a.cible[b] + 2f;
            if (!encombre) voulu = Math.max(voulu, a.cout[b] * 1.05f / GROS);
            if (a.fabrique[b]) voulu = Math.max(voulu, prixExport(b));
            a.prix[b] += (voulu - a.prix[b]) * LISSAGE_PRIX;
        }
    }

    /**
     * L'atelier du joueur : s'il ne peut pas payer, un quart des ouvriers s'en va ; mal payés, ils partent un à un ;
     * sans patron ni intendant, la caisse fond ; l'intendant embauche et renvoie selon l'ouvrage.
     */
    private void gererJoueur(Agent a) {
        if (a.caisse < 0.0) {
            a.impayes++;
            int partent = (int) Math.ceil(a.ouvriers * 0.25f);
            a.ouvriers = Math.max(0f, a.ouvriers - partent);
            a.partis += partent;
        } else a.impayes = 0;
        long h = Travail.melanger(ville * 7919L + jourCourant * 31L + a.lieu);
        float tirage = (h >>> 40) / (float) (1L << 24);
        if (a.salaireJoueur < 0.9f && a.ouvriers >= 1f && tirage < DEPART_MAL_PAYES) {
            a.ouvriers -= 1f;
            a.partis++;
        }
        if (a.intendance <= 0f && absent(a) && a.caisse > 0.0) {
            double vol = a.caisse * VOL_ABSENT;
            a.caisse -= vol;
            a.pertes += vol;
        }
        if (a.intendance > 0f && !a.recettes.isEmpty()) {
            // L'intendant est prudent : la caisse doit couvrir JOURS_PRUDENCE jours de salaires, et chaque ouvrier doit avoir de l'ouvrage.
            double paie = Math.max(1f, a.ouvriers) * Recettes.SALAIRE * a.salaireJoueur + INTENDANT_SALAIRE;
            float seuil = OUVRAGE_MAX - a.intendance / 1000f;
            if ((a.caisse < paie * JOURS_PRUDENCE * 0.5 || a.emploi < OUVRAGE_MIN) && a.ouvriers > 1f) a.ouvriers -= 1f;
            else if (a.emploi > seuil && a.caisse > paie * JOURS_PRUDENCE * 2 && a.ouvriers < a.maxOuvriers()) a.ouvriers += 1f;
        }
    }

    private void gerer(Agent a) {
        if (a.joueur) {
            gererJoueur(a);
            return;
        }
        float reserve = a.reserve();
        if (a.caisse < reserve * 0.25f && !a.recettes.isEmpty()) {
            // L'atelier est à sec : quelqu'un de la ville (le patron, un associé, un repreneur) y remet de l'argent.
            double apport = reserve * 0.5 - a.caisse;
            if (bourse > apport * 5.0) {
                bourse -= apport;
                a.caisse += apport;
                renflouements++;
            }
        }
        if (a.caisse > reserve) {
            double part = (a.caisse - reserve) * 0.2;
            a.caisse -= part;
            bourse += part;
        }
        if (a.recettes.isEmpty()) return;
        if (a.emploi > 0.85f && a.caisse > reserve * 0.5f) a.ouvriers = Math.min(a.maxOuvriers(), a.ouvriers * 1.03f + 0.05f);
        else if (a.emploi < 0.4f || a.caisse < -a.credit() * 0.5f) a.ouvriers = Math.max(a.source() ? 0.5f : Math.max(1, a.batiments), a.ouvriers * 0.97f);
    }

    /** Ce qu'un client paie (avant ses propres rabais) et qui est retiré du stock. */
    public void venteAuJoueur(Agent a, int b, int q, double total) {
        a.stock[b] -= q;
        a.caisse += total;
        a.demandeJour[b] += q;
        if (a.joueur) a.ventes += total;
    }

    public void achatAuJoueur(Agent a, int b, int q, double total) {
        a.caisse -= total;
        a.ajouter(b, q, (float) (total / q));
    }

    public void ecrire(DataOutputStream s) throws IOException {
        s.writeInt(ville);
        s.writeInt(jour);
        s.writeDouble(bourse);
        int n = Biens.TOUS.size();
        s.writeInt(n);
        s.writeInt(agents.size());
        for (Agent a : agents) {
            // L'atelier du joueur s'écrit avec un lieu négatif, suivi de ses réglages.
            s.writeInt(a.joueur ? -1 - a.lieu : a.lieu);
            if (a.joueur) {
                s.writeInt(a.batiments);
                s.writeFloat(a.margeJoueur);
                s.writeFloat(a.salaireJoueur);
                s.writeFloat(a.intendance);
                s.writeInt(a.presenceJusqua);
                s.writeInt(a.impayes);
                s.writeInt(a.partis);
                s.writeDouble(a.ventes);
                s.writeDouble(a.achats);
                s.writeDouble(a.salaires);
                s.writeDouble(a.pertes);
                s.writeInt(a.arret.length);
                for (boolean x : a.arret) s.writeBoolean(x);
            }
            s.writeFloat(a.ouvriers);
            s.writeFloat(a.emploi);
            s.writeDouble(a.caisse);
            for (int b = 0; b < n; b++) {
                s.writeFloat(a.stock[b]);
                s.writeFloat(a.prix[b]);
                s.writeFloat(a.cout[b]);
                s.writeFloat(a.demande[b]);
            }
        }
    }

    /** Relit l'état sauvegardé par-dessus une économie fraîchement construite (les agents qui n'existent plus sont ignorés). */
    void lire(DataInputStream s) throws IOException {
        jour = s.readInt();
        bourse = s.readDouble();
        int n = s.readInt(), nombre = s.readInt();
        int ici = Biens.TOUS.size();
        java.util.Set<Integer> lus = new java.util.HashSet<>();
        for (int k = 0; k < nombre; k++) {
            int code = s.readInt();
            Agent a;
            if (code < 0) {
                a = ajouterJoueur(-1 - code);
                a.batiments = s.readInt();
                a.margeJoueur = s.readFloat();
                a.salaireJoueur = s.readFloat();
                a.intendance = s.readFloat();
                a.presenceJusqua = s.readInt();
                a.impayes = s.readInt();
                a.partis = s.readInt();
                a.ventes = s.readDouble();
                a.achats = s.readDouble();
                a.salaires = s.readDouble();
                a.pertes = s.readDouble();
                int na = s.readInt();
                for (int x = 0; x < na; x++) {
                    boolean v = s.readBoolean();
                    if (x < a.arret.length) a.arret[x] = v;
                }
            } else {
                lus.add(code);
                a = agent(code);
                // Un atelier racheté en entier : l'agent de la ville a disparu dans la sauvegarde, celui-ci n'existe plus à part.
                if (a != null && a.joueur) a = null;
            }
            float ouvriers = s.readFloat(), emploi = s.readFloat();
            double caisse = s.readDouble();
            if (a != null) {
                a.ouvriers = ouvriers;
                a.emploi = emploi;
                a.caisse = caisse;
            }
            for (int b = 0; b < n; b++) {
                float st = s.readFloat(), px = s.readFloat(), co = s.readFloat(), de = s.readFloat();
                if (a == null || b >= ici) continue;
                a.stock[b] = st;
                if (px > 0f) a.prix[b] = px;
                if (co > 0f) a.cout[b] = co;
                a.demande[b] = de;
            }
        }
        // Un atelier racheté en entier n'a plus d'agent de la ville : on retire celui qu'a recréé la construction.
        java.util.List<Agent> retirer = new java.util.ArrayList<>();
        for (Agent a : agents) {
            if (a.joueur || a.recettes.isEmpty() || lus.contains(a.lieu) || agentJoueur(a.lieu) == null) continue;
            retirer.add(a);
        }
        agents.removeAll(retirer);
    }
}
