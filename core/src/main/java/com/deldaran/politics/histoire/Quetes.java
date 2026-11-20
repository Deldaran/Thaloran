package com.deldaran.politics.histoire;

import com.deldaran.politics.Metiers;
import com.deldaran.politics.Relations;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Lot E : les missions qui manquaient, jouées dans le monde.
 * - ESCORTE : un marchand te suit jusqu'à la ville voisine ; les brigands en veulent à sa marchandise ;
 * - LOUPS : les loups attaquent les troupeaux ; hors des murs, à la nuit tombée, ils sortent ;
 * - ENQUETE : un vol ; on interroge les habitants (X), deux témoins donnent le coupable, qu'on va accuser ;
 * - MEDIATION : deux voisins se querellent ; on écoute l'un et l'autre, puis on propose un arrangement ;
 * - DETTE : un artisan n'est pas payé ; on va réclamer au débiteur, puis on rapporte l'argent ;
 * - DEMANDE : un villageois qui t'aime bien te demande un service (lui rapporter quelque chose) ;
 * - CONTREBANDE et VOL : le tavernier, à voix basse ;
 * - CHANTIER : le bourgmestre veut relever une maison en ruine ; on fournit les planches, et la maison se relève.
 */
public final class Quetes {
    /** En escorte, une embuscade de plus à chaque tirage de la route. */
    public static final float ESCORTE_EMBUSCADE = 0.25f;
    /** Chances par jour et par ville qu'une telle mission paraisse au tableau. */
    public static final float CHANCE_ESCORTE = 0.35f, CHANCE_LOUPS = 0.2f, CHANCE_ENQUETE = 0.2f, CHANCE_MEDIATION = 0.15f, CHANCE_DETTE = 0.2f, CHANCE_CHANTIER = 0.3f;
    /** Il faut tant de témoins pour connaître le coupable ; après tant d'interrogatoires sans rien, le suivant parle. */
    public static final int TEMOINS = 2, INTERROGATOIRES_MAX = 6;
    /** Un villageois qui t'estime au moins ESTIME_DEMANDE peut te demander un service. */
    public static final float ESTIME_DEMANDE = 0.3f;
    /** Contrebande : chance d'être pris à la porte (moins la Discrétion) ; amende d'un vol raté. */
    public static final float GARDE = 0.35f;
    public static final int AMENDE = 100;
    /** Les loups sortent la nuit : de NUIT_DEBUT h à NUIT_FIN h. */
    public static final double NUIT_DEBUT = 20.0, NUIT_FIN = 5.0;
    /** Une meute de LOUPS_MIN à LOUPS_MAX bêtes ; un loup a une force de FORCE_LOUP à FORCE_LOUP + 9 (un brigand : 15 à 40) et fuit vite. */
    public static final int LOUPS_MIN = 2, LOUPS_MAX = 5;
    public static final float FORCE_LOUP = 10f, MORAL_LOUPS = 30f;

    private final Joueur j;
    private final Population p;
    private final Missions ms;
    /** Maisons relevées par les chantiers : ville, bâtiment, par deux. */
    final List<int[]> rebaties = new ArrayList<>();
    private int escortesVues = -1;

    Quetes(Joueur j, Population p, Missions ms) {
        this.j = j;
        this.p = p;
        this.ms = ms;
    }

    private String ville(int v) {
        return Noms.ville(p.villes.peuple[v], p.graine, v);
    }

    private static String maj(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    static boolean speciale(int type) {
        return type >= Missions.ESCORTE && type != Missions.CHANTIER;
    }

    // ================================================================ Au tableau

    /** Les nouvelles annonces du jour pour la ville v (appelé par Missions.actualiser). */
    int offrir(int v, int jour, Random h, int offertes, EconomieVille e) {
        // Escorte : un marchand part pour une ville voisine, par une route ouverte.
        if (offertes < Missions.MAX_OFFRES && !ms.existe(v, Missions.ESCORTE, -1, -1) && tire(h, CHANCE_ESCORTE)) {
            int[] voisine = voisine(v, h);
            if (voisine != null) {
                Missions.Mission m = nouvelle(Missions.ESCORTE, v, Biens.MARCHE, jour);
                m.dest = voisine[0];
                float km = voisine[1];
                m.prime = Math.round(40f + 2.2f * km);
                m.renom = 1;
                m.limite = jour + 3 + (int) Math.ceil(km / Missions.KM_PAR_JOUR) * 2;
                m.cible = Travail.melanger(p.graine * 53L + v * 7919L + jour * 31L) | 1L << 62;
                m.cibleNom = Noms.personne(p.villes.peuple[v], p.graine, m.cible >>> 8, (int) (m.cible & 1L));
                m.cibleFemme = (m.cible & 1L) == 1L;
                m.donneur = m.cibleNom + (m.cibleFemme ? ", une marchande" : ", un marchand");
                offertes++;
            }
        }
        // Loups : il faut des fermes et des troupeaux.
        EconomieVille.Agent champs = e.agent(Biens.CHAMPS);
        if (offertes < Missions.MAX_OFFRES && champs != null && champs.ouvriers > 0f && !ms.existe(v, Missions.LOUPS, -1, -1) && tire(h, CHANCE_LOUPS)) {
            Missions.Mission m = nouvelle(Missions.LOUPS, v, Biens.MARCHE, jour);
            m.quantite = LOUPS_MIN + h.nextInt(LOUPS_MAX - LOUPS_MIN + 1);
            m.prime = 50 + 12 * m.quantite;
            m.renom = 1;
            m.limite = jour + 8;
            m.donneur = "les fermiers";
            offertes++;
        }
        long[] gens = j.gensVille == v ? j.gensIci : null;
        String[] noms = j.gensNoms;
        boolean[] femmes = j.gensFemmes;
        if (gens != null && noms != null && femmes != null && gens.length >= 6 && noms.length == gens.length && femmes.length == gens.length) {
            // Enquête : un vol dans un atelier ; le coupable est un habitant.
            if (offertes < Missions.MAX_OFFRES && !ms.existe(v, Missions.ENQUETE, -1, -1) && tire(h, CHANCE_ENQUETE)) {
                EconomieVille.Agent a = atelier(e, h);
                if (a != null) {
                    Missions.Mission m = nouvelle(Missions.ENQUETE, v, a.lieu, jour);
                    int k = h.nextInt(gens.length);
                    m.cible = gens[k];
                    m.cibleNom = noms[k];
                    m.cibleFemme = femmes[k];
                    m.quantite = 40 + h.nextInt(160);
                    m.prime = 40 + m.quantite / 2;
                    m.renom = 1;
                    m.limite = jour + 6;
                    m.donneur = ms.donneur(a.lieu);
                    offertes++;
                }
            }
            // Médiation : deux voisins.
            if (offertes < Missions.MAX_OFFRES && !ms.existe(v, Missions.MEDIATION, -1, -1) && tire(h, CHANCE_MEDIATION)) {
                int a = h.nextInt(gens.length), b = (a + 1 + h.nextInt(gens.length - 1)) % gens.length;
                Missions.Mission m = nouvelle(Missions.MEDIATION, v, Biens.MARCHE, jour);
                m.cible = gens[a];
                m.cibleNom = noms[a];
                m.cibleFemme = femmes[a];
                m.cible2 = gens[b];
                m.cible2Nom = noms[b];
                m.prime = 45;
                m.renom = 1;
                m.limite = jour + 5;
                m.donneur = "le bourgmestre";
                offertes++;
            }
            // Dette : un artisan n'est pas payé.
            if (offertes < Missions.MAX_OFFRES && !ms.existe(v, Missions.DETTE, -1, -1) && tire(h, CHANCE_DETTE)) {
                EconomieVille.Agent a = atelier(e, h);
                if (a != null) {
                    Missions.Mission m = nouvelle(Missions.DETTE, v, a.lieu, jour);
                    int k = h.nextInt(gens.length);
                    m.cible = gens[k];
                    m.cibleNom = noms[k];
                    m.cibleFemme = femmes[k];
                    m.quantite = 30 + h.nextInt(120);
                    m.prime = Math.max(10, m.quantite / 4);
                    m.limite = jour + 7;
                    m.donneur = ms.donneur(a.lieu);
                    offertes++;
                }
            }
        }
        // Chantier : relever une maison en ruine.
        int[] ruines = j.ruinesVille == v ? j.ruinesIci : null;
        if (offertes < Missions.MAX_OFFRES && ruines != null && ruines.length > 0 && !ms.existe(v, Missions.CHANTIER, -1, -1) && tire(h, CHANCE_CHANTIER)) {
            int q = -1;
            for (int r : ruines) if (!rebatie(v, r)) { q = r; break; }
            if (q >= 0) {
                Missions.Mission m = nouvelle(Missions.CHANTIER, v, Biens.MARCHE, jour);
                m.dest = q;
                m.bien = Biens.PLANCHE.id;
                m.quantite = 15 + h.nextInt(16);
                m.prixUnite = Math.max(1, Math.round(e.prixImport(m.bien) * 1.1f));
                m.prime = 30;
                m.renom = 1;
                m.limite = jour + 10;
                m.donneur = "le bourgmestre";
                offertes++;
            }
        }
        return offertes;
    }

    private Missions.Mission nouvelle(int type, int v, int lieu, int jour) {
        Missions.Mission m = new Missions.Mission();
        m.type = type;
        m.ville = v;
        m.lieu = lieu;
        m.creee = jour;
        ms.liste.add(m);
        return m;
    }

    /** Une ville voisine par une route ouverte : { ville, km }. */
    private int[] voisine(int v, Random h) {
        Routes r = p.routes;
        List<int[]> l = new ArrayList<>();
        for (int k = 0; k < r.nombre; k++) {
            if (r.fin[k] != Routes.OUVERTE) continue;
            int autre = r.villeA[k] == v ? r.villeB[k] : r.villeB[k] == v ? r.villeA[k] : -1;
            if (autre < 0 || !p.villes.estVivante(autre)) continue;
            l.add(new int[] { autre, Math.round(r.longueurKm[k]) });
        }
        return l.isEmpty() ? null : l.get(h.nextInt(l.size()));
    }

    private static EconomieVille.Agent atelier(EconomieVille e, Random h) {
        List<EconomieVille.Agent> l = new ArrayList<>();
        for (EconomieVille.Agent a : e.agents) if (!a.joueur && !a.recettes.isEmpty() && a.lieu > Metiers.MAISON && a.lieu < Metiers.NOMBRE) l.add(a);
        return l.isEmpty() ? null : l.get(h.nextInt(l.size()));
    }

    boolean rebatie(int v, int q) {
        for (int[] x : rebaties) if (x[0] == v && x[1] == q) return true;
        return false;
    }

    // ================================================================ Textes

    String resume(Missions.Mission m) {
        switch (m.type) {
            case Missions.ESCORTE:
                return "Escorter " + m.donneur + ", et sa mule jusqu'au marché " + Creation.de(ville(m.dest)) + ". Les brigands guettent la marchandise.";
            case Missions.LOUPS:
                return "Les loups attaquent les troupeaux " + Creation.de(ville(m.ville)) + " (" + m.quantite + " bêtes). Sors des murs à la nuit tombée, chasse-les, puis reviens le dire sur la place.";
            case Missions.ENQUETE:
                return "On a volé " + Fiche.argentTexte(m.quantite) + " chez " + Missions.role(m.lieu) + ". Interroge les habitants (X) : il faut " + TEMOINS + " témoins pour savoir qui. Puis va accuser le coupable.";
            case Missions.MEDIATION:
                return m.cibleNom + " et " + m.cible2Nom + " se querellent. Écoute l'un et l'autre (X), puis propose un arrangement.";
            case Missions.DETTE:
                return m.cibleNom + " doit " + Fiche.argentTexte(m.quantite) + " " + Missions.a(m.donneur) + ". Va réclamer la somme (X), puis rapporte-la à l'atelier.";
            case Missions.DEMANDE:
                return m.cibleNom + " aimerait que tu lui rapportes " + Biens.get(m.bien).quantite(m.quantite) + ". Va le lui donner (X).";
            case Missions.CONTREBANDE:
                return "Passer " + Biens.get(m.bien).quantite(m.quantite) + " sans que la garde les voie, jusqu'à la taverne " + Creation.de(ville(m.dest)) + ".";
            case Missions.VOL:
                return "Forcer la nuit la caisse " + Creation.de(Biens.nomBoutique(m.lieu).toLowerCase()) + " " + Creation.de(ville(m.ville)) + ", puis rapporter la moitié du butin au tavernier.";
            case Missions.CHANTIER:
                return "Fournir " + Biens.get(m.bien).quantite(m.quantite) + " au bourgmestre " + Creation.de(ville(m.ville)) + ", sur la place, pour relever une maison en ruine.";
            default:
                return null;
        }
    }

    String court(Missions.Mission m) {
        switch (m.type) {
            case Missions.ESCORTE: return "Escorter " + m.cibleNom + " jusqu'à " + ville(m.dest);
            case Missions.LOUPS: return "Loups " + Creation.de(ville(m.ville)) + (m.indices == 2 ? " (chassés)" : m.livre > 0 ? " (" + m.livre + " tués)" : "");
            case Missions.ENQUETE: return "Vol chez " + Missions.role(m.lieu) + (m.indices >= TEMOINS ? " : c'est " + m.cibleNom : " (" + m.indices + "/" + TEMOINS + " témoins)");
            case Missions.MEDIATION: return "Querelle entre " + m.cibleNom + " et " + m.cible2Nom;
            case Missions.DETTE: return "Dette de " + m.cibleNom + (m.livre > 0 ? " (réclamée)" : "");
            case Missions.DEMANDE: return maj(Biens.get(m.bien).quantite(m.quantite)) + " pour " + m.cibleNom;
            case Missions.CONTREBANDE: return "Contrebande vers " + ville(m.dest);
            case Missions.VOL: return "Caisse " + Creation.de(Biens.nomBoutique(m.lieu).toLowerCase()) + (m.indices == 1 ? " (forcée)" : "");
            case Missions.CHANTIER: return maj(Biens.get(m.bien).quantite(m.reste())) + " pour relever une maison";
            default: return null;
        }
    }

    /** Le verbe du choix qui rend la mission là où l'on est. */
    String verbe(Missions.Mission m) {
        switch (m.type) {
            case Missions.ESCORTE: return "Conduire " + m.cibleNom + " au marché";
            case Missions.LOUPS: return "Annoncer aux fermiers que les loups sont chassés";
            case Missions.DETTE: return "Rendre les " + Fiche.argentTexte(m.quantite) + " " + Missions.a(m.donneur);
            case Missions.CONTREBANDE: return "Remettre la marchandise au tavernier";
            case Missions.VOL: return "Donner sa part au tavernier";
            default: return "Rendre la mission";
        }
    }

    // ================================================================ Où, quand

    /** Le repère de l'écran (appelé par Missions.majCible). */
    boolean cible(Missions.Mission m) {
        if (!speciale(m.type)) return false;
        j.cibleCamp = -1;
        j.cibleGraine = 0L;
        j.cibleTexte = court(m);
        switch (m.type) {
            case Missions.ESCORTE:
                j.cibleVille = m.dest;
                j.cibleLieu = Joueur.LIEU_MARCHE;
                return true;
            case Missions.CONTREBANDE:
                j.cibleVille = m.dest;
                j.cibleLieu = Metiers.TAVERNE;
                return true;
            case Missions.LOUPS:
                j.cibleVille = m.ville;
                j.cibleLieu = m.indices == 2 ? Joueur.LIEU_MARCHE : -4;
                return true;
            case Missions.VOL:
                j.cibleVille = m.ville;
                j.cibleLieu = m.indices == 1 ? Metiers.TAVERNE : m.lieu;
                return true;
            case Missions.DETTE:
                j.cibleVille = m.ville;
                if (m.livre > 0) j.cibleLieu = m.lieu;
                else {
                    j.cibleLieu = -3;
                    j.cibleGraine = m.cible;
                }
                return true;
            case Missions.ENQUETE:
                j.cibleVille = m.ville;
                j.cibleLieu = -3;
                j.cibleGraine = m.indices >= TEMOINS ? m.cible : 0L;
                return true;
            case Missions.MEDIATION:
                j.cibleVille = m.ville;
                j.cibleLieu = -3;
                j.cibleGraine = (m.indices & 1) == 0 ? m.cible : (m.indices & 2) == 0 ? m.cible2 : m.cible;
                return true;
            default:
                j.cibleVille = m.ville;
                j.cibleLieu = -3;
                j.cibleGraine = m.cible;
                return true;
        }
    }

    boolean livrable(Missions.Mission m, int v) {
        boolean place = j.agentIci(Biens.MARCHE) != null || j.devantTableau;
        switch (m.type) {
            case Missions.ESCORTE: return v == m.dest && place;
            case Missions.LOUPS: return v == m.ville && m.indices == 2 && place;
            case Missions.DETTE: return v == m.ville && m.livre > 0 && j.agentIci(m.lieu) != null;
            case Missions.CONTREBANDE: return v == m.dest && m.indices == 1 && j.agentIci(Metiers.TAVERNE) != null;
            case Missions.VOL: return v == m.ville && m.indices == 1 && j.agentIci(Metiers.TAVERNE) != null;
            default: return false;
        }
    }

    private static boolean nuit(double h) {
        return h >= NUIT_DEBUT || h < NUIT_FIN;
    }

    /** Toutes les deux secondes (Joueur.avancerBande) : les loups sortent ou rentrent, l'escorte se montre. */
    void avancer() {
        int v = j.villeIci();
        double h = j.heureIci();
        int dehors = 0;
        for (Missions.Mission m : ms.liste) {
            if (m.type == Missions.LOUPS && m.etat == Missions.ACCEPTEE && m.indices == 1 && m.ville == v && j.horsLesMurs()) dehors += Math.max(0, m.quantite - m.livre);
        }
        j.loupsDehors = dehors;
        for (Missions.Mission m : ms.liste) {
            if (m.type != Missions.LOUPS || m.etat != Missions.ACCEPTEE || m.indices == 2) continue;
            if (m.indices == 0 && v == m.ville && j.horsLesMurs() && nuit(h)) {
                m.indices = 1;
                j.annoncer("Des hurlements montent des champs : les loups sont là ! (Q pour les affronter)");
                ms.majCible();
            } else if (m.indices == 1 && !nuit(h)) {
                m.indices = 0;
                j.annoncer("Le jour se lève : les loups sont retournés au bois.");
            }
        }
        int n = escortes().size();
        if (n != escortesVues) {
            escortesVues = n;
            j.publierBande();
        }
    }

    /** Les marchands escortés, pour que l'écran les fasse suivre comme la bande. */
    List<Bande.Compagnon> escortes() {
        List<Bande.Compagnon> l = new ArrayList<>();
        for (Missions.Mission m : ms.liste) {
            if (m.type != Missions.ESCORTE || m.etat != Missions.ACCEPTEE) continue;
            Bande.Compagnon c = new Bande.Compagnon();
            c.graine = m.cible;
            c.nom = m.cibleNom;
            c.femme = (m.cible & 1L) == 1L;
            c.peuple = p.villes.peuple[m.ville];
            c.ville = m.ville;
            l.add(c);
        }
        return l;
    }

    /** On parle au marchand qu'on escorte (X près de lui). */
    Joueur.Scene escorteParle(Bande.Rencontre r) {
        for (Missions.Mission m : ms.liste) {
            if (m.type != Missions.ESCORTE || m.etat != Missions.ACCEPTEE || m.cible != r.graine) continue;
            Joueur.Scene s = new Joueur.Scene(m.cibleNom, "« Encore un bout de chemin jusqu'à " + ville(m.dest) + ". Garde l'œil ouvert : les brigands aiment les mules chargées. »");
            s.ajouter("« On y sera bientôt. »", j::fermerScene);
            return s;
        }
        return null;
    }

    public boolean escorte() {
        for (Missions.Mission m : ms.liste) if (m.type == Missions.ESCORTE && m.etat == Missions.ACCEPTEE) return true;
        return false;
    }

    /** En haut de la fenêtre des missions : ce qui presse (les loups sont là). */
    void urgences(Joueur.Scene s) {
        for (Missions.Mission m : ms.liste) {
            if (m.type == Missions.LOUPS && m.etat == Missions.ACCEPTEE && m.indices == 1) s.ajouter("Affronter les loups !", () -> loups(m));
        }
    }

    /** Entrer dans une ville : la garde à la porte (contrebande). */
    void entrer(int v) {
        Fiche f = j.fiche();
        for (Missions.Mission m : ms.liste) {
            if (m.type != Missions.CONTREBANDE || m.etat != Missions.ACCEPTEE || m.dest != v || m.indices != 0) continue;
            if (f.sac.nombre(m.bien) < m.quantite) continue;
            Random h = new Random(Travail.melanger(m.creee * 31L + v * 7919L + j.jourMaintenant()));
            // Lot F : un royaume qui te tient à l'œil fouille plus volontiers.
            float royaume = j.renommee().groupe(Renommee.ROYAUME, j.renommee().royaumeDe(v));
            float risque = Math.max(0.05f, GARDE - f.competences[Fiche.DISCRETION] / 300f - f.attributs[Fiche.RUSE] * 0.02f + Math.max(0f, -royaume) / 200f);
            f.apprendre(Fiche.DISCRETION, 3f);
            if (h.nextFloat() < risque) {
                f.sac.retirer(m.bien, m.quantite);
                int amende = Math.min(f.argent, AMENDE);
                f.argent -= amende;
                j.marche().ville(v, j.jour()).bourse += amende;
                m.etat = Missions.ECHOUEE;
                j.renommee().gagner(v, 0f, -6f, null);
                j.renommee().marquer(Renommee.VOLEUR);
                j.annoncer("À la porte, la garde fouille ton sac : la contrebande est saisie, et tu paies " + Fiche.argentTexte(amende) + " d'amende.");
            } else {
                m.indices = 1;
                j.annoncer("La garde te laisse passer sans fouiller ton sac. Va à la taverne.");
            }
            ms.majCible();
            j.rafraichirSac();
            j.enregistrer();
        }
    }

    // ================================================================ Les loups

    private Joueur.Scene loups(Missions.Mission m) {
        Fiche f = j.fiche();
        if (m.indices != 1) return ms.accueil("Les loups ne sont pas là.");
        List<Combat.Combattant> tous = new ArrayList<>();
        int[] eq = Combat.equipement(f.sac.portes());
        Combat.Combattant moi = new Combat.Combattant();
        moi.nom = f.nom;
        moi.joueur = true;
        moi.camp = Combat.NOUS;
        moi.graine = -1L;
        moi.arme = eq[0];
        moi.armure = eq[1];
        moi.force = Combat.forceJoueur(f, eq[0]);
        tous.add(moi);
        tous.addAll(j.bande().combattants());
        Random h = new Random(Travail.melanger(m.creee * 131L + j.jourMaintenant() * 31L + m.livre));
        int restants = m.quantite - m.livre;
        for (int k = 0; k < restants; k++) {
            Combat.Combattant c = new Combat.Combattant();
            c.nom = "un loup";
            c.camp = Combat.EUX;
            c.graine = h.nextLong();
            c.force = FORCE_LOUP + h.nextInt(10);
            tous.add(c);
        }
        Combat.Resultat res = Combat.resoudre(tous, f.competences[Fiche.SURVIE], 25f, 60f, MORAL_LOUPS, h.nextLong());
        int tues = 0, fuis = 0;
        for (Combat.Combattant c : res.tous) {
            if (c.camp != Combat.EUX) continue;
            if (c.etat == Combat.MORT || c.etat == Combat.A_TERRE) tues++;
            else if (c.etat == Combat.FUI) fuis++;
        }
        String pertes = j.bande().apres(res);
        j.santeJoueur = Math.max(0.05f, res.tous.get(0).sante);
        if (res.tous.get(0).etat == Combat.A_TERRE && res.tous.get(0).sante <= 0.2f) j.lignee().blessureMortelle("dévoré" + f.e() + " par les loups");
        Biens.Bien arme = eq[0] >= 0 ? Biens.get(eq[0]) : null;
        f.apprendre(arme != null && arme.competence >= 0 ? arme.competence : Fiche.ATHLETISME, 3f + tues);
        f.apprendre(Fiche.SURVIE, 2f);
        m.livre += tues;
        j.loupsDehors = 0;
        StringBuilder t = new StringBuilder("Les loups surgissent de l'herbe, les yeux luisants. ");
        boolean fini = res.vainqueur == Combat.NOUS || m.livre >= m.quantite;
        if (fini) {
            m.indices = 2;
            if (tues > 0) f.sac.ajouter(Biens.PEAU_BRUTE.id, tues);
            t.append("Tu en abats ").append(tues).append(fuis > 0 ? ", les autres s'enfuient la queue basse pour ne plus revenir." : ".")
                    .append(tues > 0 ? " Tu prends leurs peaux." : "").append(" Va le dire aux fermiers, sur la place.");
            j.noter("En l'an " + p.annee + ", tu chasses les loups qui dévoraient les troupeaux " + Creation.de(ville(m.ville)) + ".");
        } else {
            m.indices = 0;
            t.append(tues > 0 ? "Tu en abats " + tues + ", mais la" : "La").append(" meute a le dessus : tu te replies, mordu. Ils reviendront la nuit prochaine.");
        }
        if (pertes != null && !pertes.isEmpty()) t.append("\n").append(pertes);
        ms.majCible();
        j.rafraichirSac();
        j.enregistrer();
        Joueur.Scene s = new Joueur.Scene("Les loups", t.toString());
        s.ajouter("Continuer", j::fermerScene);
        return s;
    }

    // ================================================================ Rendre

    Joueur.Scene livrer(Missions.Mission m) {
        Fiche f = j.fiche();
        EconomieVille e = j.marche().ville(m.ville, j.jour());
        switch (m.type) {
            case Missions.ESCORTE:
                // Lot F : le marchand raconte tes exploits là où il arrive.
                j.renommee().porter(m.ville, m.dest, 0.2f);
                return reussir(m, m.prime, m.cibleNom + " décharge sa mule devant les étals et te compte ta paie. « Sans toi, je n'y serais pas arrivé" + (m.cibleFemme ? "e" : "") + ". »", m.dest);
            case Missions.LOUPS:
                return reussir(m, m.prime, "Les fermiers t'offrent à boire et réunissent la prime. « Les bêtes dormiront tranquilles. »", m.ville);
            case Missions.DETTE: {
                if (f.argent < m.quantite) return ms.detail(m, "Tu n'as plus les " + Fiche.argentTexte(m.quantite) + " que tu as réclamés.");
                f.argent -= m.quantite;
                EconomieVille.Agent a = e.agent(m.lieu);
                if (a != null) a.caisse += m.quantite;
                e.bourse += m.prime;
                return reussir(m, m.prime, "Tu rends la somme. " + maj(m.donneur) + (m.donneur.contains(", ") ? "," : "") + " te laisse ta part : " + Fiche.argentTexte(m.prime) + ".", m.ville);
            }
            case Missions.CONTREBANDE: {
                if (f.sac.nombre(m.bien) < m.quantite) return ms.detail(m, "Tu n'as plus la marchandise.");
                f.sac.retirer(m.bien, m.quantite);
                f.apprendre(Fiche.DISCRETION, 4f);
                j.renommee().marquer(Renommee.VOLEUR);
                return reussir(m, m.prime, "Le tavernier fait disparaître les sacs dans sa cave et glisse une bourse sur le comptoir.", m.dest);
            }
            case Missions.VOL: {
                int part = m.quantite / 2;
                if (f.argent < part) return ms.detail(m, "Tu n'as plus de quoi lui donner sa part.");
                f.argent -= part;
                return reussir(m, 0, "Tu glisses " + Fiche.argentTexte(part) + " au tavernier. « Bon travail. J'en aurai d'autres. »", m.ville);
            }
            default:
                return ms.accueil(null);
        }
    }

    /** Une mission réussie : paie, renom, Lien, carnet. */
    private Joueur.Scene reussir(Missions.Mission m, int paye, String texte, int ou) {
        Fiche f = j.fiche();
        m.etat = Missions.REUSSIE;
        if (paye > 0) {
            f.argent += paye;
            j.marche().ville(ou, j.jour()).bourse -= paye;
        }
        if (m.type != Missions.VOL && m.type != Missions.CONTREBANDE) {
            j.renommee().gagner(ou, Math.max(Missions.RENOM_MINIMUM, Missions.RENOM_PAR_POINT * m.renom), 3f, "Mission réussie");
            j.renommee().marquer(Renommee.FIABLE);
        }
        j.destin().signaler(Destin.MISSION, -1, 0f);
        j.noter("Mission réussie : " + resume(m));
        j.gesteJoueur = "recevoir";
        ms.majCible();
        j.publierBande();
        j.rafraichirSac();
        j.enregistrer();
        return ms.accueil(texte + (paye > 0 ? " Tu reçois " + Fiche.argentTexte(paye) + "." : ""));
    }

    private Joueur.Scene echouer(Missions.Mission m, String texte, float reputation) {
        m.etat = Missions.ECHOUEE;
        j.renommee().gagner(m.ville, 0f, -reputation, null);
        j.renommee().marquer(Renommee.MANQUEES);
        ms.majCible();
        j.enregistrer();
        Joueur.Scene s = new Joueur.Scene("Mission manquée", texte);
        s.ajouter("Continuer", j::fermerScene);
        return s;
    }

    private Relations.Lien lien(long graine) {
        Relations r = j.relations;
        return r == null ? null : r.existant(graine);
    }

    private static void estime(Relations.Lien l, float d) {
        if (l != null) l.estime = Math.max(-1f, Math.min(1f, l.estime + d));
    }

    private void modifie() {
        if (j.relations != null) j.relations.modifie = true;
    }

    // ================================================================ Parler aux gens (X)

    /** Les choix ajoutés quand on parle à un villageois (Bande.parler). */
    void choix(Joueur.Scene s, Bande.Rencontre r) {
        if (j.fiche() == null || r.enfant) return;
        for (Missions.Mission m : ms.liste) {
            if (m.etat != Missions.ACCEPTEE || m.ville != r.ville) continue;
            switch (m.type) {
                case Missions.ENQUETE:
                    if (r.graine == m.cible && m.indices >= TEMOINS) s.ajouter("L'accuser du vol chez " + Missions.role(m.lieu), () -> accuser(m, r));
                    else if (r.graine != m.cible && !m.vus.contains(r.graine)) s.ajouter("L'interroger sur le vol chez " + Missions.role(m.lieu), () -> interroger(m, r));
                    break;
                case Missions.MEDIATION:
                    if (r.graine == m.cible || r.graine == m.cible2) {
                        int bit = r.graine == m.cible ? 1 : 2;
                        if ((m.indices & bit) == 0) s.ajouter("Écouter sa version de la querelle", () -> ecouter(m, r, bit));
                        else if (m.indices == 3) s.ajouter("Proposer un arrangement", () -> arrangement(m, r));
                    }
                    break;
                case Missions.DETTE:
                    if (r.graine == m.cible && m.livre == 0) s.ajouter("Réclamer les " + Fiche.argentTexte(m.quantite) + " qu'" + (r.femme ? "elle" : "il") + " doit", () -> reclamer(m, r, null));
                    break;
                case Missions.DEMANDE:
                    if (r.graine == m.cible && j.fiche().sac.nombre(m.bien) >= m.quantite) s.ajouter("Lui donner " + Biens.get(m.bien).quantite(m.quantite), () -> donner(m, r));
                    break;
                default:
            }
        }
        if (demandeVoulue(r)) s.ajouter("« Tu as besoin de quelque chose ? »", () -> demande(r));
    }

    private Joueur.Scene interroger(Missions.Mission m, Bande.Rencontre r) {
        Fiche f = j.fiche();
        m.vus.add(r.graine);
        f.apprendre(Fiche.ELOQUENCE, 1f);
        boolean temoin = Math.floorMod(Travail.melanger(m.cible * 31L + r.graine + m.creee), 3L) == 0L || m.vus.size() >= INTERROGATOIRES_MAX + m.indices * 2;
        String rep;
        if (!temoin) {
            String[] rien = { "« Je n'ai rien vu, je dormais. »", "« Un vol ? Première nouvelle. »", "« Demande plutôt aux voisins. »", "« Je ne me mêle pas de ça. »" };
            rep = rien[(int) Math.floorMod(r.graine, (long) rien.length)];
        } else {
            m.indices++;
            String qui = m.cibleFemme ? "une femme" : "un homme";
            if (m.indices < TEMOINS) rep = "« Cette nuit-là, j'ai vu " + qui + " rôder près de chez " + Missions.role(m.lieu).replaceFirst("^le ", "").replaceFirst("^l'", "") + ". Je n'ai pas vu son visage. »";
            else {
                rep = "« Je l'ai vu" + (m.cibleFemme ? "e" : "") + ", de mes yeux : c'était " + m.cibleNom + ". »";
                j.noter("Un témoin accuse " + m.cibleNom + " du vol chez " + Missions.role(m.lieu) + ".");
            }
            ms.majCible();
        }
        j.enregistrer();
        Joueur.Scene s = new Joueur.Scene(r.nom, rep);
        s.ajouter("« Merci. »", j::fermerScene);
        return s;
    }

    private Joueur.Scene accuser(Missions.Mission m, Bande.Rencontre r) {
        Fiche f = j.fiche();
        Random h = new Random(Travail.melanger(r.graine * 17L + j.jourMaintenant() * 31L + m.indices));
        int jet = f.competences[Fiche.ELOQUENCE] / 2 + 5 * f.attributs[Fiche.PRESENCE] + 12 * m.indices + h.nextInt(35);
        f.apprendre(Fiche.ELOQUENCE, 3f);
        Relations.Lien l = r.lien;
        if (jet >= 50) {
            if (l != null) {
                l.rancune = Math.min(1f, l.rancune + 0.3f);
                estime(l, -0.2f);
                modifie();
            }
            EconomieVille e = j.marche().ville(m.ville, j.jour());
            EconomieVille.Agent a = e.agent(m.lieu);
            if (a != null) a.caisse += m.quantite;
            j.renommee().gagner(m.ville, 0f, 2f, null);
            return reussir(m, m.prime, (r.femme ? "Elle" : "Il") + " pâlit, puis avoue : l'argent est caché sous son lit. La garde l'emmène, et " + m.donneur + (m.donneur.contains(", ") ? "," : "") + " retrouve son bien.", m.ville);
        }
        m.indices = Math.max(0, m.indices - 1);
        ms.majCible();
        j.enregistrer();
        Joueur.Scene s = new Joueur.Scene(r.nom, "« Moi ? Tu n'as aucune preuve ! » " + (r.femme ? "Elle" : "Il") + " nie tout. Il te faut un témoin de plus.");
        s.ajouter("Continuer", j::fermerScene);
        return s;
    }

    private static final String[] QUERELLES = {
        "« Il a déplacé la borne de mon champ, la nuit, d'un bon pas. »",
        "« Elle me doit trois poules depuis la Saint-Martin. »",
        "« Son chien a mordu mon fils, et il en rit. »",
        "« Il a médit de moi à la taverne, devant tout le monde. »",
        "« Elle lave son linge en amont du mien et salit mon eau. »",
        "« Il a vendu ma vache comme si c'était la sienne. »",
    };

    private Joueur.Scene ecouter(Missions.Mission m, Bande.Rencontre r, int bit) {
        m.indices |= bit;
        String autre = bit == 1 ? m.cible2Nom : m.cibleNom;
        String grief = QUERELLES[(int) Math.floorMod(r.graine * 7L + m.creee, (long) QUERELLES.length)].replace("Il ", autre + " ").replace("Elle ", autre + " ").replace("« Son ", "« Le ").replace("« Il ", "« " + autre + " ").replace("« Elle ", "« " + autre + " ");
        ms.majCible();
        j.enregistrer();
        Joueur.Scene s = new Joueur.Scene(r.nom, grief + (m.indices == 3 ? "\nTu as entendu les deux. À toi de proposer un arrangement." : "\nVa écouter " + autre + "."));
        s.ajouter("« Je vais voir ce que je peux faire. »", j::fermerScene);
        return s;
    }

    private Joueur.Scene arrangement(Missions.Mission m, Bande.Rencontre r) {
        Fiche f = j.fiche();
        Joueur.Scene s = new Joueur.Scene("Un arrangement", "Que proposes-tu à " + m.cibleNom + " et " + m.cible2Nom + " ?");
        s.ajouter("Que chacun reconnaisse ses torts (Éloquence)", () -> {
            Random h = new Random(Travail.melanger(m.cible * 13L + m.cible2 + j.jourMaintenant()));
            int jet = f.competences[Fiche.ELOQUENCE] + 4 * f.attributs[Fiche.PRESENCE] + h.nextInt(40);
            f.apprendre(Fiche.ELOQUENCE, 4f);
            if (jet >= 50) return apaiser(m, 0.08f, 0.08f, "Ils finissent par se serrer la main, de mauvaise grâce d'abord, puis en riant.");
            estime(lien(m.cible), -0.05f);
            estime(lien(m.cible2), -0.05f);
            modifie();
            return echouer(m, "Le ton monte ; chacun repart en jurant que tu as pris le parti de l'autre.", 1f);
        });
        s.ajouter("Donner raison à " + m.cibleNom, () -> apaiser(m, 0.12f, -0.15f, m.cibleNom + " triomphe ; " + m.cible2Nom + " s'incline, mais ne te le pardonnera pas de sitôt."));
        s.ajouter("Donner raison à " + m.cible2Nom, () -> apaiser(m, -0.15f, 0.12f, m.cible2Nom + " triomphe ; " + m.cibleNom + " s'incline, mais ne te le pardonnera pas de sitôt."));
        if (f.argent >= 30) s.ajouter("Payer toi-même le dédommagement (" + Fiche.argentTexte(30) + ")", () -> {
            f.argent -= 30;
            return apaiser(m, 0.1f, 0.1f, "Tu poses les pièces sur la table. La querelle s'éteint d'un coup.");
        });
        s.ajouter("« Laisse-moi réfléchir. »", j::fermerScene);
        return s;
    }

    private Joueur.Scene apaiser(Missions.Mission m, float a, float b, String texte) {
        estime(lien(m.cible), a);
        estime(lien(m.cible2), b);
        modifie();
        return reussir(m, m.prime, texte + " Le bourgmestre te fait porter sa récompense.", m.ville);
    }

    private Joueur.Scene reclamer(Missions.Mission m, Bande.Rencontre r, String message) {
        Fiche f = j.fiche();
        String il = r.femme ? "elle" : "il", Il = r.femme ? "Elle" : "Il";
        Joueur.Scene s = new Joueur.Scene(r.nom, Joueur.avec(message, r.nom + " doit " + Fiche.argentTexte(m.quantite) + " " + Missions.a(m.donneur) + ". " + Il + " te regarde venir d'un mauvais œil."));
        s.ajouter("Exiger qu'" + il + " paie (Présence)", () -> {
            if (m.indices == j.jourMaintenant() + 1) return reclamer(m, r, "« Je t'ai déjà dit que je n'avais rien aujourd'hui ! »");
            Random h = new Random(Travail.melanger(r.graine * 11L + j.jourMaintenant()));
            int jet = 5 * f.attributs[Fiche.PRESENCE] + f.competences[Fiche.COMMANDEMENT] / 3 + 8 * Renommee.palier(j.renommee().renom(m.ville)) + h.nextInt(45);
            f.apprendre(Fiche.COMMANDEMENT, 2f);
            if (jet >= 45) return encaisser(m, r, Il + " soupire et compte les pièces une à une.");
            m.indices = j.jourMaintenant() + 1;
            j.enregistrer();
            return reclamer(m, r, "« Je n'ai rien ! Reviens demain. »");
        });
        s.ajouter("Le menacer", () -> {
            Relations.Lien l = r.lien;
            if (l != null) {
                l.peur = Math.min(1f, l.peur + 0.3f);
                l.rancune = Math.min(1f, l.rancune + 0.2f);
                modifie();
            }
            j.renommee().gagner(m.ville, 0f, -2f, null);
            return encaisser(m, r, Il + " blêmit et paie tout, sans un mot.");
        });
        s.ajouter("« Je reviendrai. »", j::fermerScene);
        return s;
    }

    private Joueur.Scene encaisser(Missions.Mission m, Bande.Rencontre r, String texte) {
        m.livre = 1;
        j.fiche().argent += m.quantite;
        j.gesteJoueur = "recevoir";
        ms.majCible();
        j.enregistrer();
        Joueur.Scene s = new Joueur.Scene(r.nom, texte + " Rapporte les " + Fiche.argentTexte(m.quantite) + " " + Missions.a(m.donneur) + ".");
        s.ajouter("Continuer", j::fermerScene);
        return s;
    }

    // ---------------------------------------------------------------- Demandes des villageois

    private static final Biens.Bien[] SERVICES = { Biens.BIERE, Biens.VIN, Biens.FROMAGE, Biens.MIEL, Biens.CHANDELLE, Biens.COUTEAU, Biens.CAPE, Biens.BOTTES, Biens.SOINS, Biens.EPICES, Biens.PAIN };

    private boolean demandeVoulue(Bande.Rencontre r) {
        Fiche.Proche q = j.social().procheDe(r.graine);
        boolean famille = q != null && q.estime >= ESTIME_FAMILLE;
        if (!famille && (r.lien == null || r.lien.estime < ESTIME_DEMANDE) || r.rival || r.ville < 0) return false;
        int n = 0;
        for (Missions.Mission m : ms.liste) {
            if (m.type != Missions.DEMANDE || (m.etat != Missions.ACCEPTEE && m.etat != Missions.OFFERTE)) continue;
            if (m.cible == r.graine) return false;
            n++;
        }
        if (n >= 2) return false;
        return Math.floorMod(Travail.melanger(r.graine * 3L + j.jourMaintenant()), 3L) == 0L;
    }

    /** Lot G2 : les proches de la famille demandent aussi (dès que leur estime atteint ESTIME_FAMILLE). */
    static final int ESTIME_FAMILLE = 20;
    private static final Biens.Bien[] PARENTS = { Biens.SOINS, Biens.PLANCHE, Biens.CHANDELLE, Biens.LAINE, Biens.FARINE };
    private static final String[] PARENTS_TEXTE = { "Mes vieux os me font souffrir. Tu pourrais me rapporter %s ?", "Le toit fuit encore, et je n'ai plus l'âge de grimper. Rapporte-moi %s ?",
        "Les soirées sont longues et je n'y vois plus. Tu me rapporterais %s ?", "Je voudrais tricoter pour l'hiver. Rapporte-moi %s ?", "La huche est vide. Tu me rapporterais %s ?" };
    private static final Biens.Bien[] FRATRIE = { Biens.BOTTES, Biens.COUTEAU, Biens.VIN, Biens.CAPE, Biens.MIEL };
    private static final String[] FRATRIE_TEXTE = { "Mon petit use ses souliers plus vite qu'il ne grandit. Tu lui trouverais %s ?", "J'ai cassé mon couteau. Tu me rapporterais %s ?",
        "On fête mes noces d'étain, et il manque à boire. Rapporte-moi %s ?", "L'hiver vient et ma cape est en loques. Tu me trouverais %s ?", "C'est l'anniversaire de ma fille. Rapporte-moi %s, elle adore ça." };

    private Joueur.Scene demande(Bande.Rencontre r) {
        Fiche.Proche proche = j.social().procheDe(r.graine);
        if (proche != null) return demandeFamille(r, proche);
        Biens.Bien b = SERVICES[(int) Math.floorMod(Travail.melanger(r.graine + j.jourMaintenant() * 7L), (long) SERVICES.length)];
        int q = b.prix < 5 ? 3 : 1;
        String il = r.femme ? "Elle" : "Il";
        Joueur.Scene s = new Joueur.Scene(r.nom, il + " hésite, puis : « Justement… Tu pourrais me rapporter " + b.quantite(q) + " ? Je te le revaudrai. »");
        s.ajouter("« Compte sur moi. »", () -> {
            Missions.Mission m = nouvelle(Missions.DEMANDE, r.ville, -1, j.jourMaintenant());
            m.etat = Missions.ACCEPTEE;
            m.bien = b.id;
            m.quantite = q;
            m.prime = Math.round(b.prix * q * 1.5f) + 10;
            m.limite = j.jourMaintenant() + 5;
            m.cible = r.graine;
            m.cibleNom = r.nom;
            m.cibleFemme = r.femme;
            j.social().promettre(r.graine, r.nom, "lui rapporter " + b.quantite(q), m.limite, r.ville);
            m.donneur = r.nom;
            ms.majCible();
            j.enregistrer();
            Joueur.Scene ok = new Joueur.Scene(r.nom, "« Merci, vraiment. »");
            ok.ajouter("Continuer", j::fermerScene);
            return ok;
        });
        s.ajouter("« Pas maintenant. »", j::fermerScene);
        return s;
    }

    private Joueur.Scene demandeFamille(Bande.Rencontre r, Fiche.Proche q) {
        boolean parent = q.role == Fiche.PERE || q.role == Fiche.MERE || q.role == Fiche.AIEUL || q.role == Fiche.ONCLE;
        Biens.Bien[] biens = parent ? PARENTS : FRATRIE;
        String[] textes = parent ? PARENTS_TEXTE : FRATRIE_TEXTE;
        int k = (int) Math.floorMod(Travail.melanger(r.graine + j.jourMaintenant() * 7L), (long) biens.length);
        Biens.Bien b = biens[k];
        int q2 = b.prix < 8 ? 4 : 1;
        String qui = j.social().roleDe(q);
        Joueur.Scene s = new Joueur.Scene(r.nom + ", " + qui, "« " + String.format(textes[k], b.quantite(q2)) + " »");
        s.ajouter("« Bien sûr. »", () -> {
            Missions.Mission m = nouvelle(Missions.DEMANDE, r.ville, -1, j.jourMaintenant());
            m.etat = Missions.ACCEPTEE;
            m.bien = b.id;
            m.quantite = q2;
            // La famille ne paie que la moitié de ce que ça coûte : c'est surtout l'affection qui compte.
            m.prime = Math.round(b.prix * q2 * 0.5f);
            m.limite = j.jourMaintenant() + 7;
            m.cible = r.graine;
            m.cibleNom = r.nom;
            m.cibleFemme = r.femme;
            j.social().promettre(r.graine, r.nom, "lui rapporter " + b.quantite(q2), m.limite, r.ville);
            m.donneur = r.nom + " (" + qui + ")";
            ms.majCible();
            j.enregistrer();
            Joueur.Scene ok = new Joueur.Scene(r.nom, "« Tu es bon" + (j.fiche().femme() ? "ne" : "") + " avec nous. »");
            ok.ajouter("Continuer", j::fermerScene);
            return ok;
        });
        s.ajouter("« Pas maintenant. »", () -> {
            q.estime = Math.max(0, q.estime - 2);
            return j.fermerScene();
        });
        return s;
    }

    private Joueur.Scene donner(Missions.Mission m, Bande.Rencontre r) {
        Fiche f = j.fiche();
        f.sac.retirer(m.bien, m.quantite);
        estime(r.lien, 0.15f);
        Fiche.Proche proche = j.social().procheDe(r.graine);
        if (proche != null) {
            proche.estime = Math.min(100, proche.estime + 10);
            j.destin().moral = Math.min(100f, j.destin().moral + 5f);
        }
        modifie();
        j.social().tenir(r.graine, "lui rapporter");
        j.gesteJoueur = "donner_objet";
        return reussir(m, m.prime, (r.femme ? "Elle" : "Il") + " te serre les mains : « Je n'oublierai pas. »", m.ville);
    }

    // ================================================================ Le tavernier louche

    /** Dans la boutique : à la taverne, parler à voix basse ; devant l'atelier à voler, la nuit, forcer la caisse. */
    void choixBoutique(Joueur.Scene s, int lieu, EconomieVille.Agent a) {
        if (a.joueur) return;
        int v = j.villeIci();
        if (lieu == Metiers.TAVERNE) s.ajouter("Parler à voix basse au tavernier…", () -> tavernier(lieu));
        for (Missions.Mission m : ms.liste) {
            if (m.type == Missions.VOL && m.etat == Missions.ACCEPTEE && m.ville == v && m.lieu == lieu && m.indices == 0) {
                if (nuit(j.heureIci())) s.ajouter("Forcer la caisse (la nuit, personne ne regarde)", () -> forcer(m, a));
                else s.ajouter("Forcer la caisse… (attends la nuit : il y a du monde)", () -> j.boutiqueScene(lieu, "Pas en plein jour."));
            }
        }
    }

    private Joueur.Scene tavernier(int lieu) {
        Fiche f = j.fiche();
        int v = j.villeIci();
        float rep = j.renommee().reputation(v);
        if (rep > 20f && f.competences[Fiche.DISCRETION] < 25) return j.boutiqueScene(lieu, "Le tavernier essuie un verre : « Je ne vois pas de quoi tu parles. » Tu as trop bonne réputation pour lui.");
        for (Missions.Mission m : ms.liste)
            if ((m.type == Missions.CONTREBANDE || m.type == Missions.VOL) && m.etat == Missions.ACCEPTEE) return j.boutiqueScene(lieu, "« Finis déjà ce que je t'ai demandé. »");
        Random h = new Random(Travail.melanger(p.graine * 71L + v * 7919L + j.jourMaintenant()));
        Joueur.Scene s = new Joueur.Scene("Le tavernier", "Il se penche par-dessus le comptoir : « J'ai peut-être du travail pour quelqu'un qui sait se taire. »");
        int[] voisine = voisine(v, h);
        if (voisine != null) {
            Biens.Bien b = h.nextBoolean() ? Biens.SEL : Biens.EPICES;
            int q = b == Biens.SEL ? 4 : 2;
            int prime = Math.round(60f + 1.5f * voisine[1] + b.prix * q * 0.5f);
            s.ajouter("Passer " + b.quantite(q) + " jusqu'à la taverne " + Creation.de(ville(voisine[0])) + " sans payer l'octroi (" + Fiche.argentTexte(prime) + ")", () -> {
                Missions.Mission m = nouvelle(Missions.CONTREBANDE, v, Metiers.TAVERNE, j.jourMaintenant());
                m.etat = Missions.ACCEPTEE;
                m.dest = voisine[0];
                m.bien = b.id;
                m.quantite = q;
                m.prime = prime;
                m.limite = j.jourMaintenant() + 3 + (int) Math.ceil(voisine[1] / Missions.KM_PAR_JOUR) * 2;
                m.donneur = "le tavernier";
                f.sac.ajouter(b.id, q);
                ms.majCible();
                j.rafraichirSac();
                j.enregistrer();
                return j.boutiqueScene(lieu, "Il glisse les sacs sous ta cape. « La garde fouille à la porte. Ne te fais pas prendre. »");
            });
        }
        EconomieVille e = j.marche().ville(v, j.jour());
        EconomieVille.Agent cible = null;
        for (EconomieVille.Agent a : e.agents)
            if (!a.joueur && !a.recettes.isEmpty() && a.lieu > Metiers.MAISON && a.lieu < Metiers.NOMBRE && a.lieu != Metiers.TAVERNE && a.caisse > 150.0 && (cible == null || a.caisse > cible.caisse)) cible = a;
        if (cible != null) {
            int l = cible.lieu;
            s.ajouter("Forcer la caisse " + Creation.de(Biens.nomBoutique(l).toLowerCase()) + ", la nuit (moitié du butin pour lui)", () -> {
                Missions.Mission m = nouvelle(Missions.VOL, v, l, j.jourMaintenant());
                m.etat = Missions.ACCEPTEE;
                m.limite = j.jourMaintenant() + 4;
                m.donneur = "le tavernier";
                ms.majCible();
                j.enregistrer();
                return j.boutiqueScene(lieu, "« Le maître dort à l'étage. Passe par-derrière, après la tombée de la nuit. »");
            });
        }
        s.ajouter("« Je n'ai rien entendu. »", () -> j.boutiqueScene(lieu));
        return s;
    }

    private Joueur.Scene forcer(Missions.Mission m, EconomieVille.Agent a) {
        Fiche f = j.fiche();
        Random h = new Random(Travail.melanger(m.creee * 97L + j.jourMaintenant() * 31L));
        int jet = f.competences[Fiche.DISCRETION] + 4 * f.attributs[Fiche.RUSE] + 3 * f.attributs[Fiche.ADRESSE] + h.nextInt(45);
        f.apprendre(Fiche.DISCRETION, 4f);
        if (jet >= 55) {
            int butin = (int) Math.min(300.0, Math.max(10.0, a.caisse * 0.25));
            a.caisse -= butin;
            f.argent += butin;
            m.quantite = butin;
            m.indices = 1;
            j.renommee().marquer(Renommee.VOLEUR);
            ms.majCible();
            j.enregistrer();
            Joueur.Scene s = new Joueur.Scene("La caisse", "Le loquet cède sans bruit. Tu prends " + Fiche.argentTexte(butin) + " et ressors comme une ombre. Va donner sa part au tavernier.");
            s.ajouter("Continuer", j::fermerScene);
            return s;
        }
        int amende = Math.min(f.argent, AMENDE);
        f.argent -= amende;
        j.marche().ville(m.ville, j.jour()).bourse += amende;
        j.renommee().marquer(Renommee.VOLEUR);
        return echouer(m, "Une planche craque. Le maître descend, une lanterne à la main, et crie au voleur. La garde te fait payer " + Fiche.argentTexte(amende) + " d'amende ; toute la ville le saura.", 8f);
    }

    // ================================================================ Chantiers

    /** Le chantier fourni : la maison sera relevée (l'écran la dessine à la place de la ruine). */
    void rebatir(Missions.Mission m) {
        if (m.dest >= 0 && !rebatie(m.ville, m.dest)) rebaties.add(new int[] { m.ville, m.dest });
        j.affaires().publier();
    }

    // ================================================================ Essais

    /** Pour les essais : toutes les missions paraissent. */
    public static boolean forcer;

    private static boolean tire(Random h, float chance) {
        return h.nextFloat() < chance || forcer;
    }

    public void offrirPourEssai(int v) {
        forcer = true;
        offrir(v, j.jourMaintenant(), new Random(v * 31L + j.jourMaintenant()), 0, j.marche().ville(v, j.jour()));
        forcer = false;
    }

    public Joueur.Scene detailPourEssai(Missions.Mission m) {
        return ms.detail(m, null);
    }

    public Joueur.Scene loupsPourEssai(Missions.Mission m) {
        m.indices = 1;
        return loups(m);
    }

    public Joueur.Scene livrerPourEssai(Missions.Mission m) {
        return livrer(m);
    }

    // ================================================================ Sauvegarde

    void ecrire(DataOutputStream s) throws IOException {
        s.writeInt(rebaties.size());
        for (int[] x : rebaties) {
            s.writeInt(x[0]);
            s.writeInt(x[1]);
        }
    }

    void lire(DataInputStream s) throws IOException {
        int n = s.readInt();
        for (int k = 0; k < n; k++) rebaties.add(new int[] { s.readInt(), s.readInt() });
    }
}
