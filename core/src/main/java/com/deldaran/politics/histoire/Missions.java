package com.deldaran.politics.histoire;

import com.deldaran.politics.Metiers;
import com.deldaran.politics.Relations;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Phase 5 : les missions données par la ville. Elles naissent des besoins réels, jamais d'une liste au hasard :
 *   - LIVRAISON : un atelier fait venir une matière des colporteurs (au prix d'import) ou en manque → « apporte-m'en » ;
 *   - LETTRE : le bourgmestre a du courrier pour une ville voisine, reliée par une route ;
 *   - ARMES : le royaume est en guerre, le capitaine de la garde équipe la milice.
 * Les missions s'affichent au tableau de la place. Une mission que personne ne prend est faite par un autre (un colporteur,
 * un bûcheron…) quand son délai passe ; une mission acceptée et non tenue fâche celui qui l'a donnée.
 */
public final class Missions {
    public static final int LIVRAISON = 0, LETTRE = 1, ARMES = 2, CHASSE = 3, MERCENAIRE = 4;
    /** Lot E (histoire/Quetes.java). */
    public static final int ESCORTE = 5, LOUPS = 6, ENQUETE = 7, MEDIATION = 8, DETTE = 9, DEMANDE = 10, CONTREBANDE = 11, VOL = 12, CHANTIER = 13;
    public static final int OFFERTE = 0, ACCEPTEE = 1, REUSSIE = 2, ECHOUEE = 3, PRISE = 4;

    /** Missions au tableau d'une ville, au plus ; missions acceptées en même temps, au plus. */
    public static final int MAX_OFFRES = 6, MAX_ACCEPTEES = 5;
    /** Une livraison paie le prix d'import × PRIME_LIVRAISON : un peu plus que ce que coûterait le colporteur. */
    public static final float PRIME_LIVRAISON = 1.05f, PRIME_ARMES = 1.3f;
    /** Une lettre : BASE_LETTRE + PAR_KM cuivre par km ; on compte KM_PAR_JOUR km de marche par jour pour le délai. */
    public static final float BASE_LETTRE = 15f, PAR_KM = 1.2f, KM_PAR_JOUR = 25f;
    /** Une livraison vaut au plus VALEUR_MAX cuivre et pèse au plus POIDS_MAX kg. */
    public static final float VALEUR_MAX = 2500f, POIDS_MAX = 60f;
    /** Un atelier est en manque quand son stock tombe sous MANQUE × son stock visé. */
    public static final float MANQUE = 0.4f;
    /** Une livraison doit rapporter au moins VALEUR_MIN cuivre (personne ne court la campagne pour trois miches). */
    public static final float VALEUR_MIN = 60f;
    /** Renom gagné par point de renom de la mission (une mission vaut 1 ou 2 points). Même une petite mission rapporte un peu. */
    public static final float RENOM_PAR_POINT = 4f, RENOM_MINIMUM = 1.5f;

    public static final class Mission {
        public int type, ville, lieu, bien = -1, quantite, livre, prixUnite, prime, renom, creee, limite, dest = -1, etat;
        public String donneur = "";
        /** Lot E : les personnes en jeu (graine du villageois, nom), l'avancement (témoins, versions entendues, loups sortis…), qui on a déjà interrogé. */
        public long cible, cible2;
        public String cibleNom = "", cible2Nom = "";
        public boolean cibleFemme;
        public int indices;
        public final List<Long> vus = new ArrayList<>();

        public int reste() {
            return quantite - livre;
        }

        public int total() {
            return type == LETTRE || type == CHASSE || Quetes.speciale(type) ? prime : prixUnite * quantite + prime;
        }

        void ecrire(DataOutputStream s) throws IOException {
            s.writeInt(type);
            s.writeInt(ville);
            s.writeInt(lieu);
            s.writeInt(bien);
            s.writeInt(quantite);
            s.writeInt(livre);
            s.writeInt(prixUnite);
            s.writeInt(prime);
            s.writeInt(renom);
            s.writeInt(creee);
            s.writeInt(limite);
            s.writeInt(dest);
            s.writeInt(etat);
            s.writeUTF(donneur);
        }

        void ecrireSuite(DataOutputStream s) throws IOException {
            s.writeLong(cible);
            s.writeLong(cible2);
            s.writeUTF(cibleNom);
            s.writeUTF(cible2Nom);
            s.writeBoolean(cibleFemme);
            s.writeInt(indices);
            s.writeInt(vus.size());
            for (long g : vus) s.writeLong(g);
        }

        void lireSuite(DataInputStream s) throws IOException {
            cible = s.readLong();
            cible2 = s.readLong();
            cibleNom = s.readUTF();
            cible2Nom = s.readUTF();
            cibleFemme = s.readBoolean();
            indices = s.readInt();
            int n = s.readInt();
            for (int k = 0; k < n; k++) vus.add(s.readLong());
        }

        static Mission lire(DataInputStream s) throws IOException {
            Mission m = new Mission();
            m.type = s.readInt();
            m.ville = s.readInt();
            m.lieu = s.readInt();
            m.bien = s.readInt();
            m.quantite = s.readInt();
            m.livre = s.readInt();
            m.prixUnite = s.readInt();
            m.prime = s.readInt();
            m.renom = s.readInt();
            m.creee = s.readInt();
            m.limite = s.readInt();
            m.dest = s.readInt();
            m.etat = s.readInt();
            m.donneur = s.readUTF();
            return m;
        }
    }

    private final Joueur j;
    private final Population p;
    /** Toutes les missions connues (au tableau, acceptées, et terminées récemment). */
    final List<Mission> liste = new ArrayList<>();
    /** Dernier jour où le tableau de chaque ville a été mis à jour. */
    private final Map<Integer, Integer> misesAJour = new HashMap<>();
    /** Ce qui s'est passé depuis la dernière fois (missions perdues…), montré à la prochaine ouverture. */
    private final List<String> nouvelles = new ArrayList<>();

    /** Lot E : les missions jouées avec les gens (escorte, loups, enquête…). */
    final Quetes quetes;

    public Quetes quetes() {
        return quetes;
    }

    Missions(Joueur j, Population p) {
        this.j = j;
        this.p = p;
        this.quetes = new Quetes(j, p, this);
    }

    /** Met à jour le repère de l'écran : la première mission en cours, où la rendre. */
    void majCible() {
        for (Mission m : liste) {
            if (m.etat != ACCEPTEE || m.type == MERCENAIRE) continue;
            if (quetes.cible(m)) return;
            j.cibleGraine = 0L;
            if (m.type == CHASSE) {
                boolean fait = j.brigands().detruit(m.dest);
                j.cibleVille = m.ville;
                j.cibleLieu = fait ? Joueur.LIEU_MARCHE : -2;
                j.cibleCamp = fait ? -1 : m.dest;
                j.cibleTexte = court(m);
                return;
            }
            j.cibleCamp = -1;
            j.cibleVille = m.type == LETTRE ? m.dest : m.ville;
            j.cibleLieu = m.lieu == Biens.MARCHE ? Joueur.LIEU_MARCHE : m.lieu;
            j.cibleTexte = court(m);
            return;
        }
        j.cibleVille = -1;
        j.cibleLieu = -1;
        j.cibleCamp = -1;
        j.cibleGraine = 0L;
        j.cibleTexte = "";
    }

    public List<Mission> liste() {
        return liste;
    }

    public int acceptees() {
        int n = 0;
        for (Mission m : liste) if (m.etat == ACCEPTEE) n++;
        return n;
    }

    // ---------------------------------------------------------------- Noms

    private String ville(int v) {
        return Noms.ville(p.villes.peuple[v], p.graine, v);
    }

    private static String majuscule(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** Le rôle de celui qui donne la mission, d'après le lieu. */
    static String role(int lieu) {
        switch (lieu) {
            case Biens.MARCHE: return "un marchand de la place";
            case Metiers.MOULIN: return "le meunier";
            case Metiers.BOULANGERIE: return "le boulanger";
            case Metiers.BOUCHERIE: return "le boucher";
            case Metiers.GRENIER: return "le grenetier";
            case Metiers.FORGE: return "le forgeron";
            case Metiers.MENUISERIE: return "le menuisier";
            case Metiers.TANNERIE: return "le tanneur";
            case Metiers.TISSERAND: return "le tisserand";
            case Metiers.POTIER: return "le potier";
            case Metiers.AUBERGE: return "l'aubergiste";
            case Metiers.TAVERNE: return "le tavernier";
            case Metiers.ENTREPOT: return "le maître de l'entrepôt";
            case Metiers.PECHERIE: return "le maître pêcheur";
            case Metiers.ECOLE: return "le maître d'école";
            default: return "un artisan";
        }
    }

    /** « au meunier », « à Faemir, le forgeron ». */
    static String a(String qui) {
        if (qui.startsWith("le ")) return "au " + qui.substring(3);
        if (qui.startsWith("les ")) return "aux " + qui.substring(4);
        return "à " + qui;
    }

    /** « Faemir, le forgeron » si l'on connaît le villageois qui travaille là, sinon « le forgeron ». */
    String donneur(int lieu) {
        Relations.Lien[] m = j.maitres;
        Relations.Lien l = m != null && lieu >= 0 && lieu < m.length && j.villeVue >= 0 ? m[lieu] : null;
        return l != null ? l.nomComplet() + ", " + role(lieu) : role(lieu);
    }

    private Relations.Lien lien(Mission m) {
        Relations.Lien[] t = j.maitres;
        if (t == null || m.lieu < 0 || m.lieu >= t.length || j.villeVue != m.ville) return null;
        Relations.Lien l = t[m.lieu];
        return l != null && m.donneur.startsWith(l.nomComplet()) ? l : null;
    }

    private void toucher(Mission m, float estime) {
        Relations.Lien l = lien(m);
        if (l == null) return;
        l.estime = Math.max(-1f, Math.min(1f, l.estime + estime));
        Relations r = j.relations;
        if (r != null) r.modifie = true;
    }

    /** Une phrase qui dit ce qu'il faut faire. */
    public String resume(Mission m) {
        String q = quetes.resume(m);
        if (q != null) return q;
        switch (m.type) {
            case LETTRE:
                return "Porter une lettre du bourgmestre " + Creation.de(ville(m.ville)) + " jusqu'à la place " + Creation.de(ville(m.dest)) + ".";
            case ARMES:
                return "Fournir " + Biens.get(m.bien).quantite(m.quantite) + " au capitaine de la garde " + Creation.de(ville(m.ville)) + ", sur la place.";
            case CHASSE: {
                Brigands.Repaire r = j.brigands().repaire(m.dest);
                return "Chasser les brigands de " + (r == null ? "la région" : r.chef + ", qui campent " + Brigands.direction(r.angle) + " " + Creation.de(ville(m.ville))) + ", puis revenir le dire au capitaine, sur la place.";
            }
            case MERCENAIRE:
                return "Mettre ta bande au service de la garde " + Creation.de(ville(m.ville)) + " pendant " + m.quantite + " jours : chasser tous les brigands qui viendront rôder autour de la ville.";
            default:
                return "Apporter " + Biens.get(m.bien).quantite(m.quantite) + " " + a(m.donneur) + " " + Creation.de(ville(m.ville)) + ".";
        }
    }

    private String recompense(Mission m) {
        return Fiche.argentTexte(m.total()) + (m.renom > 0 ? " et un peu de renom" : "");
    }

    // ---------------------------------------------------------------- Naissance des missions

    /** Met à jour le tableau d'une ville : retire ce qui est périmé, ajoute les nouveaux besoins. Une fois par jour et par ville. */
    void actualiser(int v) {
        int jour = j.jourMaintenant();
        echeances(jour);
        Integer deja = misesAJour.get(v);
        if (deja != null && deja == jour) return;
        misesAJour.put(v, jour);
        j.brigands().actualiser(v);
        j.brigands().offrirContrat(v, liste, jour);
        EconomieVille e = j.marche().ville(v, j.jour());
        Random hasard = new Random(Travail.melanger(p.graine * 31L + v * 1000003L + jour));
        int offertes = 0;
        for (Mission m : liste) if (m.ville == v && m.etat == OFFERTE) offertes++;
        // Livraisons : les manques et les importations des ateliers, les plus gros d'abord.
        List<float[]> besoins = new ArrayList<>();
        for (EconomieVille.Agent a : e.agents) {
            if (a.source() || a.lieu == Biens.DEHORS || a.joueur) continue;
            for (int b = 0; b < a.stock.length; b++) {
                if (!a.achete[b] || a.fabrique[b] || a.cible[b] < 1f) continue;
                // Un étal qui revend ce que la ville fabrique attend simplement l'artisan : seules comptent les marchandises venues de loin.
                if (a.lieu == Biens.MARCHE || a.lieu == Metiers.ENTREPOT || a.lieu == Metiers.GRENIER) {
                    boolean local = false;
                    for (EconomieVille.Agent x : e.agents) local |= x != a && x.fabrique[b];
                    if (local) continue;
                }
                boolean manque = a.stock[b] < MANQUE * a.cible[b];
                if (!manque && a.importe[b] < 0.05f) continue;
                float poids = (a.importe[b] + Math.max(0f, a.cible[b] - a.stock[b])) * e.reference(b);
                besoins.add(new float[] { a.lieu, b, poids });
            }
        }
        besoins.sort((x, y) -> Float.compare(y[2], x[2]));
        for (float[] x : besoins) {
            if (offertes >= MAX_OFFRES - 1) break;
            int lieu = (int) x[0], b = (int) x[1];
            if (existe(v, LIVRAISON, lieu, b)) continue;
            EconomieVille.Agent a = e.agent(lieu);
            Biens.Bien bien = Biens.get(b);
            float voulu = Math.max(a.cible[b] - a.stock[b], a.importe[b] * 7f);
            int q = (int) Math.ceil(voulu);
            q = Math.min(q, (int) Math.floor(VALEUR_MAX / Math.max(1f, e.prixImport(b))));
            if (bien.poids > 0f) q = Math.min(q, (int) Math.floor(POIDS_MAX / bien.poids));
            if (q < 1 || q * e.prixImport(b) * PRIME_LIVRAISON < VALEUR_MIN) continue;
            Mission m = new Mission();
            m.type = LIVRAISON;
            m.ville = v;
            m.lieu = lieu;
            m.bien = b;
            m.quantite = q;
            m.prixUnite = Math.max(1, Math.round(e.prixImport(b) * PRIME_LIVRAISON));
            m.prime = 0;
            m.renom = m.prixUnite * q >= 300 ? 1 : 0;
            m.creee = jour;
            m.limite = jour + 6 + hasard.nextInt(5);
            m.donneur = donneur(lieu);
            liste.add(m);
            offertes++;
        }
        // Lettres : une ville voisine par une route ouverte.
        if (offertes < MAX_OFFRES && !existe(v, LETTRE, -1, -1) && hasard.nextFloat() < 0.5f) {
            Routes r = p.routes;
            List<Integer> voisines = new ArrayList<>();
            List<Float> km = new ArrayList<>();
            for (int k = 0; k < r.nombre; k++) {
                if (r.fin[k] != Routes.OUVERTE) continue;
                int autre = r.villeA[k] == v ? r.villeB[k] : r.villeB[k] == v ? r.villeA[k] : -1;
                if (autre < 0 || !p.villes.estVivante(autre)) continue;
                voisines.add(autre);
                km.add(r.longueurKm[k]);
            }
            if (!voisines.isEmpty()) {
                int k = hasard.nextInt(voisines.size());
                Mission m = new Mission();
                m.type = LETTRE;
                m.ville = v;
                m.lieu = Biens.MARCHE;
                m.dest = voisines.get(k);
                float d = km.get(k);
                m.prime = Math.round(BASE_LETTRE + PAR_KM * d);
                m.renom = d > 40f ? 1 : 0;
                m.creee = jour;
                m.limite = jour + 3 + (int) Math.ceil(d / KM_PAR_JOUR) * 2;
                m.donneur = "le bourgmestre";
                liste.add(m);
                offertes++;
            }
        }
        // Lot E : escortes, loups, enquêtes, querelles, dettes, chantiers.
        offertes = quetes.offrir(v, jour, hasard, offertes, e);
        // Armes : le royaume est en guerre.
        int region = p.villes.region[v];
        int etat = region >= 0 && region < p.etats.regionEtat.length ? p.etats.regionEtat[region] : -1;
        if (offertes < MAX_OFFRES && etat >= 0 && p.etats.ennemi[etat] >= 0 && !existe(v, ARMES, -1, -1)) {
            Biens.Bien[] armes = { Biens.LANCE, Biens.JAVELOTS, Biens.FLECHES, Biens.BOUCLIER, Biens.CASQUE, Biens.GAMBISON };
            Biens.Bien bien = armes[hasard.nextInt(armes.length)];
            int q = Math.max(3, Math.min(25, (int) (e.habitants / 150f)));
            q = Math.min(q, (int) Math.floor(VALEUR_MAX / Math.max(1f, e.reference(bien.id) * PRIME_ARMES)));
            if (q >= 1) {
                Mission m = new Mission();
                m.type = ARMES;
                m.ville = v;
                m.lieu = Biens.MARCHE;
                m.bien = bien.id;
                m.quantite = q;
                m.prixUnite = Math.max(1, Math.round(e.reference(bien.id) * PRIME_ARMES));
                m.renom = 2;
                m.creee = jour;
                m.limite = jour + 10 + hasard.nextInt(6);
                int ennemi = p.etats.ennemi[etat];
                m.donneur = "le capitaine de la garde (guerre contre " + Noms.ville(p.etats.peuple[ennemi], p.graine, p.etats.capitale[ennemi]) + ")";
                liste.add(m);
            }
        }
    }

    boolean existe(int v, int type, int lieu, int bien) {
        for (Mission m : liste) {
            if (m.ville != v || m.type != type || (m.etat != OFFERTE && m.etat != ACCEPTEE)) continue;
            if (lieu >= 0 && m.lieu != lieu) continue;
            if (bien >= 0 && m.bien != bien) continue;
            return true;
        }
        return false;
    }

    /** Les délais passés : les offres sont prises par d'autres ; les missions acceptées échouent. On oublie les vieilles missions finies. */
    void echeances(int jour) {
        Iterator<Mission> it = liste.iterator();
        while (it.hasNext()) {
            Mission m = it.next();
            if (m.type == MERCENAIRE && m.etat == ACCEPTEE && jour > m.limite) {
                // Fin du contrat : réussi si plus aucun brigand ne rôde autour de la ville.
                if (j.brigands().finirContrat(m)) {
                    m.etat = REUSSIE;
                    j.fiche().argent += m.prime;
                    j.marche().ville(m.ville, j.jour()).bourse -= m.prime;
                    j.renommee().gagner(m.ville, RENOM_PAR_POINT * m.renom, 4f, "Contrat tenu");
                    j.renommee().marquer(Renommee.FIABLE);
                    nouvelles.add("Contrat tenu : " + resume(m) + " Le capitaine te verse la prime de " + Fiche.argentTexte(m.prime) + ".");
                    j.annoncer("Contrat tenu : la garde te verse " + Fiche.argentTexte(m.prime) + ".");
                } else {
                    m.etat = ECHOUEE;
                    j.renommee().gagner(m.ville, 0f, -6f, null);
                    j.renommee().marquer(Renommee.MANQUEES);
                    nouvelles.add("Contrat manqué : des brigands rôdent encore autour " + Creation.de(ville(m.ville)) + ". Le capitaine ne te paiera pas la prime.");
                    j.annoncer("Contrat manqué : des brigands rôdent encore.");
                }
                continue;
            }
            if (m.etat == OFFERTE && jour > m.limite) {
                m.etat = PRISE;
                j.quotidien().prise(m);
            }
            else if (m.etat == ACCEPTEE && jour > m.limite) {
                m.etat = ECHOUEE;
                toucher(m, -0.08f);
                j.renommee().gagner(m.ville, 0f, -4f, null);
                j.renommee().marquer(Renommee.MANQUEES);
                String autre = m.type == LETTRE ? "Un messager" : m.type == ARMES ? "Un marchand d'armes" : m.type == LIVRAISON ? "Un colporteur" : "Quelqu'un d'autre";
                nouvelles.add("Mission manquée : " + resume(m) + " Le délai est passé. " + autre + " s'en est chargé à ta place, et " + m.donneur + " ne l'oubliera pas.");
            }
            if ((m.etat == PRISE || m.etat == REUSSIE || m.etat == ECHOUEE) && jour > m.limite + 30) it.remove();
        }
    }

    // ---------------------------------------------------------------- Scènes

    /** La scène « Missions » (touche Q). */
    public Joueur.Scene accueil() {
        return accueil(null);
    }

    Joueur.Scene accueil(String message) {
        Fiche f = j.fiche();
        if (f == null) return j.accueilScene();
        int v = j.villeIci();
        if (v >= 0) actualiser(v);
        else echeances(j.jourMaintenant());
        j.destin().actualiser();
        if (!nouvelles.isEmpty()) {
            String n = String.join("\n", nouvelles);
            nouvelles.clear();
            message = message == null ? n : n + "\n\n" + message;
            j.enregistrer();
        }
        majCible();
        StringBuilder t = new StringBuilder();
        Joueur.Scene s = new Joueur.Scene("Missions", "");
        int jour = j.jourMaintenant();
        int n = 0;
        quetes.urgences(s);
        for (Mission m : liste) {
            if (m.etat != ACCEPTEE) continue;
            n++;
            boolean ici = livrableIci(m);
            s.ajouter((ici ? "» " : "") + court(m) + " — " + delai(m, jour), () -> detail(m, null));
        }
        t.append(n == 0 ? "Tu n'as aucune mission en cours." : "Tes missions en cours (» : tu peux la rendre ici).");
        if (v >= 0) {
            boolean place = j.agentIci(Biens.MARCHE) != null || j.devantTableau;
            int offres = 0;
            for (Mission m : liste) if (m.ville == v && m.etat == OFFERTE) offres++;
            if (place) s.ajouter("Lire le tableau de la place (" + offres + ")", () -> tableau(null));
            else t.append("\nLe tableau des annonces " + Creation.de(ville(v)) + " est sur la place : ").append(offres).append(offres > 1 ? " annonces." : " annonce.");
            t.append(j.quotidien().texte(v));
        } else t.append("\nTu n'es dans aucune ville : les annonces sont sur la place de chaque ville.");
        t.append("\nRenom : ").append(f.renom).append('.');
        s.ajouter("« Revenir", j::accueilScene);
        Joueur.Scene vraie = new Joueur.Scene("Missions", Joueur.avec(message, t.toString()));
        vraie.choix.addAll(s.choix);
        vraie.suites.addAll(s.suites);
        return vraie;
    }

    String court(Mission m) {
        String q = quetes.court(m);
        if (q != null) return q;
        switch (m.type) {
            case LETTRE: return "Lettre pour " + ville(m.dest);
            case ARMES: return majuscule(Biens.get(m.bien).quantite(m.reste())) + " pour la garde";
            case CHASSE: {
                Brigands.Repaire r = j.brigands().repaire(m.dest);
                return "Brigands de " + (r == null ? "la région" : r.chef) + (j.brigands().detruit(m.dest) ? " (vaincus)" : "");
            }
            case MERCENAIRE: return "Contrat avec la garde " + Creation.de(ville(m.ville));
            default: return majuscule(Biens.get(m.bien).quantite(m.reste())) + " pour " + role(m.lieu);
        }
    }

    private static String delai(Mission m, int jour) {
        int reste = m.limite - jour;
        return reste <= 0 ? "dernier jour" : reste == 1 ? "1 jour" : reste + " jours";
    }

    /** Peut-on rendre la mission là où l'on est ? */
    boolean livrableIci(Mission m) {
        int v = j.villeIci();
        if (Quetes.speciale(m.type)) return quetes.livrable(m, v);
        if (m.type == LETTRE) return v == m.dest && j.agentIci(Biens.MARCHE) != null;
        if (m.type == MERCENAIRE) return false;
        if (m.type == CHASSE) return v == m.ville && (j.agentIci(Biens.MARCHE) != null || j.devantTableau) && j.brigands().detruit(m.dest);
        return v == m.ville && j.agentIci(m.lieu) != null;
    }

    Joueur.Scene tableau(String message) {
        int v = j.villeIci();
        if (v < 0 || (j.agentIci(Biens.MARCHE) == null && !j.devantTableau)) return accueil("Le tableau est sur la place.");
        actualiser(v);
        j.gesteJoueur = "lire";
        Joueur.Scene s = new Joueur.Scene("Le tableau de la place", "");
        int jour = j.jourMaintenant();
        for (Mission m : liste) {
            if (m.ville != v || m.etat != OFFERTE) continue;
            s.ajouter(court(m) + " — " + Fiche.argentTexte(m.total()) + ", " + delai(m, jour), () -> detail(m, null));
        }
        j.affaires().tableau(s, v);
        String texte = s.choix.isEmpty() ? "Il n'y a aucune annonce aujourd'hui. La ville ne manque de rien." : "Les annonces du jour. Le crieur les répète sur la place.";
        s.ajouter("« Revenir", () -> accueil(null));
        Joueur.Scene vraie = new Joueur.Scene("Le tableau de la place", Joueur.avec(message, texte));
        vraie.choix.addAll(s.choix);
        vraie.suites.addAll(s.suites);
        return vraie;
    }

    Joueur.Scene detail(Mission m, String message) {
        Fiche f = j.fiche();
        int jour = j.jourMaintenant();
        StringBuilder t = new StringBuilder(resume(m));
        t.append("\nDonnée par ").append(m.donneur).append(". Récompense : ").append(recompense(m)).append(". Délai : ").append(delai(m, jour)).append('.');
        if (m.type == LIVRAISON) t.append("\n").append(majuscule(role(m.lieu))).append(" en manque, ou doit en faire venir de loin par les colporteurs : on t'en paie ")
                .append(Fiche.argentTexte(m.prixUnite)).append(" pièce, le prix du colporteur et un peu plus.");
        if (m.type == ARMES) t.append("\nLe royaume est en guerre : la milice a besoin d'être équipée. On paie ").append(Fiche.argentTexte(m.prixUnite)).append(" pièce.");
        if (m.type == CHASSE) {
            Brigands.Repaire r = j.brigands().repaire(m.dest);
            if (r != null && r.etat == Brigands.ACTIF) t.append("\nOn les dit ").append(r.effectif()).append(". Leur campement est à quelques centaines de pas de la ville. Morts ou vifs : les prisonniers ligotés sont payés en plus.");
            else t.append("\nLa bande n'existe plus : va le dire au capitaine.");
        }
        if (m.type == MERCENAIRE) t.append("\nLa garde paie ").append(Fiche.argentTexte(m.prixUnite)).append(" par jour (").append(Fiche.argentTexte(7 * m.prixUnite))
                .append(" chaque semaine), et ").append(Fiche.argentTexte(m.prime)).append(" à la fin si plus aucun brigand ne rôde. Pendant le contrat, les brigands viendront plus souvent.");
        if (m.bien >= 0) t.append(" Tu en as ").append(f.sac.nombre(m.bien)).append(m.livre > 0 ? ", tu en as déjà livré " + m.livre : "").append('.');
        Joueur.Scene s = new Joueur.Scene(court(m), Joueur.avec(message, t.toString()));
        if (m.etat == OFFERTE) {
            if (acceptees() >= MAX_ACCEPTEES) t.append("\nTu as déjà trop de missions en cours.");
            else s.ajouter("Accepter", () -> accepter(m));
            s.ajouter("« Revenir", () -> tableau(null));
        } else if (m.etat == ACCEPTEE) {
            if (livrableIci(m)) {
                if (Quetes.speciale(m.type)) s.ajouter(quetes.verbe(m), () -> livrer(m));
                else if (m.type == LETTRE) s.ajouter("Remettre la lettre", () -> livrer(m));
                else if (m.type == CHASSE) s.ajouter("Raconter ta victoire au capitaine", () -> livrer(m));
                else if (f.sac.nombre(m.bien) > 0) s.ajouter("Livrer " + Biens.get(m.bien).quantite(Math.min(m.reste(), f.sac.nombre(m.bien))), () -> livrer(m));
            }
            s.ajouter("Abandonner la mission", () -> abandonner(m));
            s.ajouter("« Revenir", () -> accueil(null));
        } else s.ajouter("« Revenir", () -> accueil(null));
        Joueur.Scene vraie = new Joueur.Scene(s.titre, Joueur.avec(message, t.toString()));
        vraie.choix.addAll(s.choix);
        vraie.suites.addAll(s.suites);
        return vraie;
    }

    private Joueur.Scene accepter(Mission m) {
        if (m.etat != OFFERTE) return tableau("Quelqu'un l'a déjà prise.");
        m.etat = ACCEPTEE;
        if (m.type == MERCENAIRE) j.brigands().commencerContrat(m);
        if (m.type == ESCORTE) j.publierBande();
        toucher(m, 0.02f);
        majCible();
        j.noter("Tu acceptes une mission : " + resume(m));
        j.enregistrer();
        return detail(m, m.type == LETTRE ? "Le bourgmestre te remet une lettre scellée."
                : m.type == ESCORTE ? m.cibleNom + " charge sa mule et se met en route derrière toi." : majuscule(m.donneur) + " compte sur toi.");
    }

    private Joueur.Scene abandonner(Mission m) {
        m.etat = ECHOUEE;
        toucher(m, -0.05f);
        majCible();
        j.renommee().gagner(m.ville, 0f, -2f, null);
        j.enregistrer();
        return accueil("Tu renonces : " + resume(m));
    }

    private Joueur.Scene livrer(Mission m) {
        if (!livrableIci(m)) return detail(m, "Tu n'es pas au bon endroit.");
        if (Quetes.speciale(m.type)) return quetes.livrer(m);
        Fiche f = j.fiche();
        int v = j.villeIci();
        EconomieVille e = j.marche().ville(m.ville, j.jour());
        int paye;
        String texte;
        if (m.type == LETTRE) {
            paye = m.prime;
            // La lettre est payée par la ville qui l'envoie (le port est réglé d'avance).
            e.bourse -= paye;
            m.livre = m.quantite;
            texte = "Tu remets la lettre au bourgmestre " + Creation.de(ville(v)) + ".";
        } else if (m.type == CHASSE) {
            paye = m.prime;
            e.bourse -= paye;
            m.livre = m.quantite;
            texte = "Le capitaine écoute ton récit et fait compter la prime.";
        } else {
            int q = Math.min(m.reste(), f.sac.nombre(m.bien));
            if (q <= 0) return detail(m, "Tu n'en as pas sur toi.");
            paye = q * m.prixUnite;
            f.sac.retirer(m.bien, q);
            m.livre += q;
            EconomieVille.Agent a = e.agent(m.lieu);
            if (m.type == LIVRAISON && a != null) {
                // L'atelier paie de sa caisse ; s'il n'a pas assez, la ville avance le reste.
                double avance = Math.max(0.0, paye - Math.max(0.0, a.caisse));
                e.achatAuJoueur(a, m.bien, q, paye);
                a.caisse += avance;
                e.bourse -= avance;
            } else e.bourse -= paye;
            texte = "Tu livres " + Biens.get(m.bien).quantite(q) + ".";
        }
        f.argent += paye;
        f.apprendre(Fiche.COMMERCE, Math.min(3f, paye / 200f));
        j.gesteJoueur = "donner_objet";
        j.reactionGeste = "recevoir_objet";
        j.reactionLieu = m.lieu;
        if (m.reste() > 0) {
            j.rafraichirSac();
            j.enregistrer();
            return detail(m, texte + " Tu reçois " + Fiche.argentTexte(paye) + ". Il en manque encore " + m.reste() + ".");
        }
        m.etat = REUSSIE;
        toucher(m, 0.08f);
        majCible();
        // Le renom naît là où l'on a rendu service : la ville qui a demandé, et pour une lettre aussi celle qui l'a reçue.
        j.renommee().gagner(m.ville, Math.max(RENOM_MINIMUM, RENOM_PAR_POINT * m.renom), 3f, "Mission réussie");
        if (m.type == LETTRE) j.renommee().gagner(m.dest, RENOM_PAR_POINT * m.renom * 0.5f, 1f, null);
        j.renommee().marquer(Renommee.FIABLE);
        if (m.type == LETTRE) j.renommee().marquer(Renommee.LETTRES);
        j.destin().signaler(Destin.MISSION, -1, 0f);
        if (m.type == CHANTIER) quetes.rebatir(m);
        String merci = m.type == CHANTIER ? "Le bourgmestre fait venir les charpentiers : la maison en ruine sera relevée, et une famille y vivra." : m.type == CHASSE ? "« Les routes seront plus sûres. »" : m.type == LETTRE ? "« Ça arrive vite, merci ! »" : m.type == ARMES ? "Le capitaine fait distribuer les armes à la milice." : majuscule(m.donneur) + " te remercie" + (m.lieu == Biens.MARCHE ? " : son étal est de nouveau garni." : " : son atelier peut tourner.");
        j.noter("Mission réussie : " + resume(m));
        j.rafraichirSac();
        j.enregistrer();
        return accueil(texte + " Tu reçois " + Fiche.argentTexte(paye) + (m.renom > 0 ? ", et on parle un peu de toi" : "") + ". " + merci);
    }

    // ---------------------------------------------------------------- Sauvegarde

    void ecrire(DataOutputStream s) throws IOException {
        s.writeInt(liste.size());
        for (Mission m : liste) m.ecrire(s);
    }

    void lire(DataInputStream s) throws IOException {
        int n = s.readInt();
        for (int k = 0; k < n; k++) liste.add(Mission.lire(s));
    }

    /** Lot E (format 14) : ce que les missions ont de plus, et les maisons relevées. Écrit après le reste de la partie. */
    void ecrireSuite(DataOutputStream s) throws IOException {
        for (Mission m : liste) m.ecrireSuite(s);
        quetes.ecrire(s);
    }

    void lireSuite(DataInputStream s) throws IOException {
        for (Mission m : liste) m.lireSuite(s);
        quetes.lire(s);
    }
}
