package com.deldaran.politics.histoire;

import com.deldaran.politics.Metiers;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Phase 10 (lot 10a) : la noblesse.
 * - LA COUR : devant le château d'une capitale (touche M), on se présente au roi ou à la reine du royaume ;
 * - LE SERVICE : connu en ville (renom 50), on offre ses services ; le souverain confie des tâches (porter une lettre, lever l'impôt
 *   d'un village, nettoyer les routes des brigands) ; chaque tâche réussie monte la FAVEUR, chaque échec la fait baisser ;
 * - DEVENIR NOBLE : l'adoubement (faveur 60, renom 50 et un exploit), l'achat d'un titre (moins cher quand le trésor est vide)
 *   ou le mariage dans une Maison noble de la cour ;
 * - LE TOURNOI : une fois par saison, trois joutes contre des chevaliers ;
 * - LE FIEF : le souverain donne un village à son vassal noble. Chaque saison : l'impôt (bas, juste ou lourd), l'aide due au suzerain,
 *   une affaire de justice à trancher ; on y paie des gardes et l'on y fait bâtir (palissade, grenier, chapelle).
 */
public final class Noblesse {
    /** Faveur nécessaire pour l'adoubement, un mariage noble, un fief ; renom pour entrer au service. */
    public static final float FAVEUR_ADOUBEMENT = 60f, FAVEUR_MARIAGE = 40f, FAVEUR_FIEF = 50f, RENOM_SERVICE = 50f;
    /** Prix d'un titre (cuivre), divisé par deux quand le trésor du royaume est presque vide ; la dot d'un mariage noble. */
    public static final int PRIX_TITRE = 30000, DOT = 5000;
    /** Une tâche dure DELAI jours ; récompenses en faveur. */
    public static final int DELAI = 20;
    public static final float FAVEUR_MESSAGE = 8f, FAVEUR_IMPOT = 12f, FAVEUR_BRIGANDS = 15f, FAVEUR_ECHEC = 10f;
    /** Le tournoi : droit d'entrée, prix, force des trois chevaliers. */
    public static final int ENTREE_TOURNOI = 50, PRIX_TOURNOI = 600;
    public static final float[] FORCE_JOUTE = { 28f, 36f, 46f };
    /** Le fief : cuivre par habitant et par saison selon le taux (bas, juste, lourd) ; part due au suzerain ; gardes. */
    public static final float[] TAUX = { 2f, 4f, 7f };
    public static final String[] NOMS_TAUX = { "bas", "juste", "lourd" };
    public static final float[] REPUTATION_TAUX = { 3f, 0f, -4f };
    public static final float AIDE = 0.2f;
    public static final int GARDES_MAX = 12, SOLDE_GARDE = 3;
    /** Les travaux du fief : palissade (moins de brigands), grenier (+20 % d'impôt), chapelle (réputation chaque saison). */
    public static final int PALISSADE = 0, GRENIER = 1, CHAPELLE = 2, TRAVAUX = 3;
    public static final String[] NOMS_TRAVAUX = { "une palissade", "un grenier seigneurial", "une chapelle" };
    public static final int[] PRIX_TRAVAUX = { 1500, 1000, 1200 };

    public static final int MESSAGE = 0, IMPOT = 1, BRIGANDS = 2;
    public static final int EN_COURS = 0, FAITE = 1;

    public static final class Tache {
        public int type, ville = -1, etat, echeance, montant;
    }

    public static final class Fief {
        public int ville, taux = 1, gardes, affaire = -1, recue;
        public boolean[] travaux = new boolean[TRAVAUX];
        public boolean remise;
    }

    private final Joueur j;
    private final Population p;
    /** Le royaume servi (-1 : personne) ; la faveur par royaume. */
    public int suzerain = -1;
    public final Map<Integer, Float> faveurs = new HashMap<>();
    /** Noble ; comment (0 adoubé, 1 titre acheté, 2 par mariage) ; la Maison épousée (index Maisons) ou -1. */
    public boolean noble, servi;
    public int voie = -1, maisonEpousee = -1;
    public Tache tache;
    public int reussies, exploits, tournois, defenses;
    private int dernierTournoi = -1, dernierJour = -1;
    /** Le parti de secours proposé à la cour (un cadet de la Maison régnante). */
    private int partiEtat = -1, partiSexe;
    private long partiId = -1L;
    public final List<Fief> fiefs = new ArrayList<>();
    /** Pour les essais : la prochaine joute est gagnée. */
    public boolean forcer;

    Noblesse(Joueur j, Population p) {
        this.j = j;
        this.p = p;
    }

    // ================================================================ Le monde

    /** Le royaume dont dépend une ville, ou -1. */
    public int etatDe(int v) {
        if (v < 0) return -1;
        int r = p.villes.region[v];
        return r < 0 ? -1 : p.etats.regionEtat[r];
    }

    private boolean capitale(int v) {
        int e = etatDe(v);
        return e >= 0 && p.etats.capitale[e] == v;
    }

    private String ville(int v) {
        return Noms.ville(p.villes.peuple[v], p.graine, v);
    }

    private String royaume(int e) {
        return Noms.ville(p.etats.peuple[e], p.graine, p.etats.capitale[e]);
    }

    private boolean souveraine(int e) {
        return p.maisons.sexeDe(p.etats.chef[e]) == 1;
    }

    /** « le roi Thador, de la Maison d'Isidor ». */
    public String souverain(int e) {
        boolean femme = souveraine(e);
        Fiche moi = j.fiche();
        if (moi != null && p.etats.chef[e] == moi.id) return (moi.femme() ? "la reine " : "le roi ") + moi.nom + " (toi)";
        String nom = Noms.personne(p.etats.peuple[e], p.graine, p.etats.chef[e], femme ? 1 : 0);
        return Recit.rang(p, e, femme) + " " + nom + ", " + Recit.du(Recit.nomMaison(p, p.etats.maison[e]));
    }

    private String sire(int e) {
        return souveraine(e) ? "Majesté" : "Sire";
    }

    int maisonDe(int e) {
        return e >= 0 && e < p.etats.maison.length ? p.etats.maison[e] : -1;
    }

    /** La Maison régnante te tient pour ennemi : la cour ne te confie plus rien. */
    boolean hostile(int e) {
        int m = maisonDe(e);
        return m >= 0 && j.renommee().groupe(Renommee.MAISON, m) <= -40f;
    }

    public float faveur(int e) {
        return faveurs.getOrDefault(e, 0f);
    }

    void changerFaveur(int e, float d) {
        if (e < 0) return;
        // Lot G2 : la Maison qui règne retient ce que l'on fait pour (ou contre) sa couronne ; une Maison qui t'aime fait monter la faveur plus vite.
        int m = maisonDe(e);
        if (m >= 0) {
            float g = j.renommee().groupe(Renommee.MAISON, m);
            if (d > 0f) d *= 1f + g / 200f;
            j.renommee().changer(Renommee.MAISON, m, d * 0.5f);
        }
        faveurs.put(e, Math.max(-100f, Math.min(100f, faveur(e) + d)));
    }

    /** Les villes vivantes d'un royaume, sauf la capitale. */
    private List<Integer> villesDe(int e) {
        List<Integer> l = new ArrayList<>();
        for (int v = 0; v < p.villes.nombre; v++) if (p.villes.estVivante(v) && etatDe(v) == e && p.etats.capitale[e] != v) l.add(v);
        return l;
    }

    public Fief fief(int v) {
        for (Fief f : fiefs) if (f.ville == v) return f;
        return null;
    }

    /** « chevalier », « dame », « seigneur de X »… pour la présentation. */
    public String titre() {
        Fiche f = j.fiche();
        if (!j.armee().couronnes.isEmpty()) return (f.femme() ? "reine " : "roi ") + Creation.de(royaume(j.armee().couronnes.get(0)));
        if (!noble) return "";
        String t = f.femme() ? "dame" : (voie == 0 ? "chevalier" : "seigneur");
        if (!fiefs.isEmpty()) t = (f.femme() ? "dame" : "seigneur") + " " + Creation.de(ville(fiefs.get(0).ville));
        return t;
    }

    /** Embuscades près d'un fief : moins de brigands avec des gardes et une palissade. */
    public float facteurBrigands(int v) {
        Fief f = fief(v);
        if (f == null) return 1f;
        return (f.travaux[PALISSADE] ? 0.5f : 1f) * Math.max(0.3f, 1f - 0.06f * f.gardes);
    }

    // ================================================================ Le temps

    void avancer() {
        if (j.fiche() == null) return;
        int jour = j.jourMaintenant();
        if (dernierJour < 0) dernierJour = jour;
        for (int d = Math.max(dernierJour, jour - 60) + 1; d <= jour; d++) journee(d);
        if (jour != dernierJour) j.enregistrer();
        dernierJour = Math.max(dernierJour, jour);
    }

    void sauter() {
        dernierJour = j.jourMaintenant();
    }

    private void journee(int d) {
        Fiche f = j.fiche();
        for (Fief x : fiefs) {
            if (x.gardes == 0) continue;
            int solde = x.gardes * SOLDE_GARDE;
            if (f.argent >= solde) f.argent -= solde;
            else {
                j.annoncer("Faute de solde, les gardes de " + ville(x.ville) + " rentrent chez eux.");
                x.gardes = 0;
            }
        }
        if (tache != null && tache.etat == EN_COURS && d > tache.echeance) {
            changerFaveur(suzerain, -FAVEUR_ECHEC);
            j.annoncer("Le temps est passé : la tâche confiée par " + souverain(suzerain) + " est un échec (faveur −" + Math.round(FAVEUR_ECHEC) + ").");
            tache = null;
        }
        if (Math.floorMod(d, Terre.JOURS_PAR_SAISON) == 0) saison(d);
    }

    private void saison(int d) {
        Fiche f = j.fiche();
        Random h = new Random(Travail.melanger(f.id * 53L + d));
        for (Fief x : fiefs) {
            int hab = p.villes.habitants[x.ville];
            int impot = Math.round(hab * TAUX[x.taux] * (x.travaux[GRENIER] ? 1.2f : 1f) * (x.remise ? 0.5f : 1f));
            x.remise = false;
            int aide = suzerain >= 0 ? Math.round(impot * AIDE) : 0;
            f.argent += impot - aide;
            x.recue = impot - aide;
            float rep = REPUTATION_TAUX[x.taux] + (x.travaux[CHAPELLE] ? 2f : 0f);
            if (x.affaire >= 0) {
                rep -= 2f;
                j.annoncer("À " + ville(x.ville) + ", on se plaint que le seigneur ne rende pas la justice.");
            }
            if (rep != 0f) j.renommee().gagner(x.ville, 0f, rep, "seigneurie");
            x.affaire = h.nextInt(AFFAIRES.length);
            j.annoncer("Ton fief " + Creation.de(ville(x.ville)) + " rapporte " + Fiche.argentTexte(impot) + (aide > 0 ? ", dont " + Fiche.argentTexte(aide) + " d'aide à ton suzerain" : "")
                    + ". Une affaire attend ta justice.");
        }
        if (suzerain >= 0 && !fiefs.isEmpty()) changerFaveur(suzerain, 2f);
    }

    /** L'écran : on entre dans une ville (lettre remise, impôt levé). */
    void entrer(int v) {
        if (tache == null || tache.etat != EN_COURS || tache.ville != v) return;
        if (tache.type == MESSAGE) {
            tache.etat = FAITE;
            j.annoncer("Tu remets la lettre " + Creation.de(souverain(suzerain)) + " au bailli " + Creation.de(ville(v)) + ". Retourne au château rendre compte.");
        } else if (tache.type == IMPOT) {
            tache.etat = FAITE;
            tache.montant = p.villes.habitants[v] * 2;
            j.annoncer("Le bailli " + Creation.de(ville(v)) + " te remet l'impôt : " + Fiche.argentTexte(tache.montant) + ". Porte-le au château.");
        }
        j.enregistrer();
    }

    /** Brigands : une victoire près d'une ville (tâche du souverain, défense de son fief). */
    void victoire(int v) {
        if (fief(v) != null) {
            defenses++;
            j.annoncer("Tu as défendu ton fief " + Creation.de(ville(v)) + " contre les brigands.");
            j.renommee().gagner(v, 5f, 5f, "défense du fief");
        }
        if (tache != null && tache.etat == EN_COURS && tache.type == BRIGANDS && etatDe(v) == suzerain) {
            tache.etat = FAITE;
            j.annoncer("Les routes du royaume sont plus sûres. Retourne au château rendre compte.");
        }
        j.destin().verifierEtapes();
    }

    // ================================================================ La cour

    /** Devant le château (M) : la cour ; devant la mairie de son fief : le fief. Sinon null. */
    Joueur.Scene devant(float[] d) {
        int v = j.villeIci();
        if (v < 0 || d == null) return null;
        if (!Float.isNaN(d[Metiers.CHATEAU]) && d[Metiers.CHATEAU] <= Joueur.PROCHE && capitale(v)) return cour(null);
        if (!Float.isNaN(d[Metiers.MAIRIE]) && d[Metiers.MAIRIE] <= Joueur.PROCHE && fief(v) != null) return fiefScene(fief(v), null);
        return null;
    }

    Joueur.Scene cour(String message) {
        Fiche f = j.fiche();
        int v = j.villeIci();
        int e = etatDe(v);
        if (e < 0 || !p.etats.estVivant(e)) return j.fermerScene();
        StringBuilder t = new StringBuilder("Le château " + Creation.de(ville(p.etats.capitale[e])) + ". Ici siège " + souverain(e) + ".");
        boolean roi = j.armee().roi(e);
        if (!roi) t.append("\nTa faveur à la cour : ").append(faveurTexte(faveur(e))).append(" (").append(Math.round(faveur(e))).append(").");
        if (roi) t.append(" C'est ta cour : tu règnes ici.");
        else if (suzerain == e) t.append(" Tu es au service de la couronne.");
        else if (suzerain >= 0) t.append(" Tu sers " + souverain(suzerain) + ".");
        if (noble) t.append(" On t'appelle ").append(titre()).append('.');
        if (tache != null && suzerain == e) t.append("\nTa tâche : ").append(decrire(tache)).append('.');
        boolean ennemi = !roi && hostile(e);
        if (ennemi) t.append("\n").append(Character.toUpperCase(Recit.nomMaison(p, maisonDe(e)).charAt(0))).append(Recit.nomMaison(p, maisonDe(e)).substring(1)).append(" te tient pour ennemi : on ne te confie plus rien ici, et l'on ne veut pas de ton service.");
        Joueur.Scene s = new Joueur.Scene("La cour", Joueur.avec(message, t.toString()));
        float renom = j.renommee().renom(v);
        if (roi) {
        } else if (ennemi) {
            // La Maison régnante te hait : ni service ni tâche.
            // Roi ou reine : ni service ni titre à demander.
        } else if (suzerain != e) {
            if (suzerain >= 0) s.ajouter("Quitter le service " + Creation.de(royaume(suzerain)) + " pour celui-ci", () -> servir(e, renom));
            else s.ajouter("Offrir tes services à la couronne", () -> servir(e, renom));
        } else {
            if (tache == null) s.ajouter("Demander une tâche", () -> nouvelleTache(e));
            else if (tache.etat == FAITE && tache.type == IMPOT) {
                s.ajouter("Remettre l'impôt (" + Fiche.argentTexte(tache.montant) + ")", () -> rendre(e, true));
                s.ajouter("Garder l'impôt pour toi", () -> rendre(e, false));
            } else if (tache.etat == FAITE) s.ajouter("Rendre compte de ta tâche", () -> rendre(e, true));
            if (!noble) s.ajouter("Demander l'adoubement", () -> adoubement(e));
            if (noble) s.ajouter("Demander un fief", () -> demanderFief(e));
            s.ajouter("Quitter le service de la couronne", () -> {
                suzerain = -1;
                tache = null;
                changerFaveur(e, -15f);
                j.enregistrer();
                return cour("Tu rends ton serment. On te regarde partir sans un mot.");
            });
        }
        if (!noble && !roi) s.ajouter("Acheter un titre (" + Fiche.argentTexte(prixTitre(e)) + ")", () -> acheterTitre(e));
        j.armee().choixCour(s, e);
        if (!j.social().marie() && !roi) {
            int[] c = promis(e);
            if (c != null) {
                Foule fl = p.foules[p.etats.peuple[e]][c[2]];
                String nom = Noms.personne(p.etats.peuple[e], p.graine, fl.idDe(c[0]), c[1]);
                s.ajouter("Demander la main de " + nom + ", " + Recit.du(Recit.nomMaison(p, fl.maisonDe(c[0]))) + " (dot : " + Fiche.argentTexte(DOT) + ")", () -> mariage(e, c[0], c[2]));
            } else if (faveur(e) >= FAVEUR_MARIAGE && j.renommee().meilleurRenom() >= RENOM_SERVICE) {
                // Personne de libre à la cour : un cadet de la Maison régnante, qu'on fait venir de ses terres.
                if (partiEtat != e) {
                    partiEtat = e;
                    partiId = j.nouvelId();
                    partiSexe = 1 - f.sexe;
                }
                String nom = Noms.personne(f.peuple, p.graine, partiId, partiSexe);
                s.ajouter("Demander la main de " + nom + ", " + (partiSexe == 1 ? "cousine" : "cousin") + " " + Creation.de(souverain(e)) + " (dot : " + Fiche.argentTexte(DOT) + ")", () -> marierCadet(e));
            }
        }
        int saison = Math.floorDiv(j.jourMaintenant(), Terre.JOURS_PAR_SAISON);
        if (dernierTournoi != saison) s.ajouter("S'inscrire au tournoi (" + Fiche.argentTexte(ENTREE_TOURNOI) + ")", () -> tournoi(e));
        if (!fiefs.isEmpty()) s.ajouter("Mes fiefs", this::accueil);
        s.ajouter("« Revenir", j::fermerScene);
        return s;
    }

    private static String faveurTexte(float x) {
        if (x >= 60f) return "dans les bonnes grâces";
        if (x >= 25f) return "bien vu" ;
        if (x > -25f) return "inconnu ou presque";
        return "en disgrâce";
    }

    private Joueur.Scene servir(int e, float renom) {
        if (faveur(e) <= -25f) return cour("Le chambellan ne te laisse pas approcher : tu es en disgrâce.");
        if (renom < RENOM_SERVICE) return cour("« Et qui es-tu ? » Le chambellan ne connaît pas ton nom. Fais-toi un nom dans cette ville (renom " + Math.round(renom) + " sur " + Math.round(RENOM_SERVICE) + ").");
        if (suzerain >= 0) changerFaveur(suzerain, -20f);
        suzerain = e;
        servi = true;
        tache = null;
        changerFaveur(e, 10f);
        Fiche f = j.fiche();
        String phrase = "En l'an " + Terre.annee(p, j.jourMaintenant()) + ", " + f.nom + " entre au service " + Creation.de(souverain(e)) + ".";
        f.recit.add(phrase);
        j.noter(phrase);
        j.renommee().gagner(j.villeIci(), 5f, 2f, "service de la couronne");
        j.destin().verifierEtapes();
        j.enregistrer();
        return cour("Tu poses le genou à terre et prêtes serment. « Sers-nous bien, et tu seras récompensé" + f.e() + ". »");
    }

    private String decrire(Tache t) {
        String reste = t.etat == FAITE ? " — c'est fait, reviens au château" : " (encore " + Math.max(0, t.echeance - j.jourMaintenant()) + " jours)";
        switch (t.type) {
            case MESSAGE: return "porter une lettre au bailli " + Creation.de(ville(t.ville)) + reste;
            case IMPOT: return (t.etat == FAITE ? "rapporter l'impôt " + Creation.de(ville(t.ville)) + " (" + Fiche.argentTexte(t.montant) + ")" : "lever l'impôt " + Creation.de(ville(t.ville)) + reste);
            default: return "chasser les brigands des routes du royaume" + reste;
        }
    }

    private Joueur.Scene nouvelleTache(int e) {
        Random h = new Random(Travail.melanger(j.fiche().id * 7L + j.jourMaintenant() * 131L + reussies));
        List<Integer> villes = villesDe(e);
        Tache t = new Tache();
        t.type = villes.isEmpty() ? BRIGANDS : h.nextInt(3);
        if (t.type != BRIGANDS) t.ville = villes.get(h.nextInt(villes.size()));
        t.echeance = j.jourMaintenant() + DELAI;
        tache = t;
        j.enregistrer();
        String dit = t.type == MESSAGE ? "« Porte ce pli scellé au bailli " + Creation.de(ville(t.ville)) + ". Qu'il ne passe par aucune autre main. »"
                : t.type == IMPOT ? "« " + ville(t.ville) + " doit son impôt. Va le chercher, et rapporte-le jusqu'au dernier sou. »"
                : "« Des brigands rançonnent nos routes. Trouve-les, et fais-en un exemple. »";
        return cour(dit + " Tu as " + DELAI + " jours.");
    }

    private Joueur.Scene rendre(int e, boolean honnete) {
        Fiche f = j.fiche();
        Tache t = tache;
        tache = null;
        if (t == null) return cour(null);
        if (t.type == IMPOT && !honnete) {
            f.argent += t.montant;
            changerFaveur(e, -40f);
            j.renommee().changer(Renommee.ROYAUME, e, -10f);
            f.pencher(Fiche.HONNETETE, -10);
            j.enregistrer();
            return cour("Tu gardes " + Fiche.argentTexte(t.montant) + ". Le trésorier fronce les sourcils : la cour saura vite que l'impôt n'est jamais arrivé.");
        }
        float gain = t.type == MESSAGE ? FAVEUR_MESSAGE : t.type == IMPOT ? FAVEUR_IMPOT : FAVEUR_BRIGANDS;
        changerFaveur(e, gain);
        reussies++;
        if (t.type == BRIGANDS) exploits++;
        int prime = t.type == MESSAGE ? 30 : t.type == IMPOT ? t.montant / 10 : 150;
        f.argent += prime;
        j.renommee().gagner(j.villeIci(), t.type == BRIGANDS ? 10f : 3f, 3f, "service de la couronne");
        j.renommee().changer(Renommee.ROYAUME, e, 3f);
        j.destin().verifierEtapes();
        j.enregistrer();
        return cour("« Bien. » " + majuscule(souverain(e)) + " te fait remettre " + Fiche.argentTexte(prime) + " (faveur +" + Math.round(gain) + ").");
    }

    private static String majuscule(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ================================================================ Devenir noble

    private Joueur.Scene adoubement(int e) {
        Fiche f = j.fiche();
        List<String> manque = new ArrayList<>();
        if (faveur(e) < FAVEUR_ADOUBEMENT) manque.add("la faveur de la couronne (" + Math.round(faveur(e)) + " sur " + Math.round(FAVEUR_ADOUBEMENT) + ")");
        if (j.renommee().meilleurRenom() < RENOM_SERVICE) manque.add("un nom connu (renom 50)");
        if (exploits < 1) manque.add("un exploit au service de la couronne (chasser des brigands)");
        if (!manque.isEmpty()) return cour("« Pas encore. » Il te manque : " + String.join(", ", manque) + ".");
        anoblir(0, -1, "adoubé" + f.e() + " par " + souverain(e));
        f.sac.ajouter(Biens.EPEE.id, 1);
        return cour("Dans la grande salle, " + souverain(e) + " pose l'épée sur ton épaule. « Relève-toi, " + titre() + " " + f.nom + ". » La cour applaudit ; on t'offre une épée.");
    }

    private int prixTitre(int e) {
        boolean vide = p.etats.tresor[e] < p.etats.reglages.tresorMax * 0.2f;
        return vide ? PRIX_TITRE / 2 : PRIX_TITRE;
    }

    private Joueur.Scene acheterTitre(int e) {
        Fiche f = j.fiche();
        int prix = prixTitre(e);
        if (faveur(e) < 0f) return cour("« La couronne ne vend rien à qui l'a desservie. »");
        if (f.argent < prix) return cour("Le trésorier compte tes pièces et secoue la tête : il faut " + Fiche.argentTexte(prix) + ".");
        f.argent -= prix;
        p.etats.tresor[e] += prix / 1000f;
        changerFaveur(e, 10f);
        anoblir(1, -1, "achète ses lettres de noblesse " + Creation.de(souverain(e)));
        return cour("Le trésorier scelle tes lettres de noblesse. " + (prix < PRIX_TITRE ? "Les coffres étaient vides : tu as eu le titre à moitié prix. " : "") + "Les vieilles Maisons murmurent ; toi, tu souris.");
    }

    /** Un parti noble à la cour : une personne d'une Maison, en âge de se marier, libre, de l'autre sexe. { place, sexe } ou null. */
    private int[] promis(int e) {
        Fiche f = j.fiche();
        if (faveur(e) < FAVEUR_MARIAGE || j.renommee().meilleurRenom() < RENOM_SERVICE) return null;
        int q = p.etats.peuple[e];
        float r = p.reglages.peuples[q].rythme;
        int an = Terre.annee(p, j.jourMaintenant());
        int mieux = -1, region = -1;
        Foule choisie = null;
        for (int g = 0; g < p.etats.regionEtat.length; g++) {
            if (p.etats.regionEtat[g] != e) continue;
            Foule fl = p.foules[q][g];
            if (fl == null) continue;
            for (int k = 0; k < fl.taille(); k++) {
                if (!fl.estEnVie(k, fl.idDe(k)) || fl.maisonDe(k) < 0 || fl.conjointDe(k) >= 0 || fl.sexeDe(k) == f.sexe || fl.idDe(k) == f.id) continue;
                int age = (int) ((an - fl.naissanceDe(k)) * r);
                if (age < 16 || age > 50) continue;
                if (mieux < 0 || fl.rangDe(k) < choisie.rangDe(mieux)) {
                    mieux = k;
                    region = g;
                    choisie = fl;
                }
            }
        }
        return mieux < 0 ? null : new int[] { mieux, choisie.sexeDe(mieux), region };
    }

    private Joueur.Scene mariage(int e, int place, int region) {
        Fiche f = j.fiche();
        if (f.argent < DOT) return cour("Il faut une dot de " + Fiche.argentTexte(DOT) + " pour prétendre à une noble Maison.");
        int q = p.etats.peuple[e];
        Foule fl = p.foules[q][region];
        long id = fl.idDe(place);
        int sexe = fl.sexeDe(place);
        String nom = Noms.personne(q, p.graine, id, sexe);
        f.argent -= DOT;
        Foule mienne = p.foules[f.peuple][f.region];
        if (mienne == fl) {
            int moi = fl.placeDe(f.id);
            if (moi >= 0) fl.unir(moi, place);
        }
        return epouser(e, id, sexe, nom, fl.maisonDe(place));
    }

    /** Le parti de secours : un cadet de la Maison régnante, qui entre dans le monde (comme un enfant) et y reste. */
    private Joueur.Scene marierCadet(int e) {
        Fiche f = j.fiche();
        if (f.argent < DOT) return cour("Il faut une dot de " + Fiche.argentTexte(DOT) + " pour prétendre à une noble Maison.");
        f.argent -= DOT;
        Creation.Ajout a = new Creation.Ajout();
        a.id = partiId;
        a.sexe = partiSexe;
        a.naissance = Terre.annee(p, j.jourMaintenant()) - (int) Math.ceil(25 / p.reglages.peuples[f.peuple].rythme);
        a.maison = p.etats.maison[e];
        a.rang = 2;
        a.ville = p.etats.capitale[e];
        a.conjoint = f.id;
        j.inserer(a);
        Foule fl = p.foules[f.peuple][f.region];
        int moi = fl.placeDe(f.id), lui = fl.placeDe(partiId);
        if (moi >= 0 && lui >= 0) fl.unir(moi, lui);
        partiEtat = -1;
        return epouser(e, partiId, partiSexe, Noms.personne(f.peuple, p.graine, partiId, partiSexe), p.etats.maison[e]);
    }

    private Joueur.Scene epouser(int e, long id, int sexe, String nom, int maison) {
        Fiche f = j.fiche();
        Social.Amour a = new Social.Amour();
        a.graine = id;
        a.nom = nom;
        a.femme = sexe == 1;
        a.ville = p.etats.capitale[e];
        a.statut = Social.MARIE;
        a.amour = 0.6f;
        a.depuis = j.jourMaintenant();
        a.vu = j.jourMaintenant();
        j.social().amours.put(id, a);
        f.lier(id, Fiche.CONJOINT, sexe, 40, false);
        maisonEpousee = maison;
        changerFaveur(e, 5f);
        j.renommee().changer(Renommee.MAISON, maison, 25f);
        if (!noble) anoblir(2, maisonEpousee, "épouse " + nom + ", " + Recit.nomMaison(p, maisonEpousee));
        else {
            String phrase = "En l'an " + Terre.annee(p, j.jourMaintenant()) + ", " + f.nom + " épouse " + nom + ", " + Recit.nomMaison(p, maisonEpousee) + ".";
            f.recit.add(phrase);
            j.noter(phrase);
            j.renommee().gagner(j.villeIci(), 15f, 5f, "noces nobles");
            j.enregistrer();
        }
        j.social().grandMoment("noces nobles");
        return cour("Les noces se font à la chapelle du château. " + nom + " te tend la main : te voici " + Recit.du(Recit.nomMaison(p, maisonEpousee)) + ".");
    }

    private void anoblir(int comment, int maison, String quoi) {
        Fiche f = j.fiche();
        noble = true;
        voie = comment;
        String phrase = "En l'an " + Terre.annee(p, j.jourMaintenant()) + ", " + f.nom + " est " + quoi + ".";
        f.recit.add(phrase);
        j.noter(phrase);
        if (!f.objets.contains("une chevalière")) f.objets.add("une chevalière");
        j.renommee().gagner(j.villeIci(), 30f, 10f, "anobli");
        j.annoncer("Te voici noble !");
        j.social().grandMoment("anobli" + f.e());
        j.destin().verifierEtapes();
        j.enregistrer();
    }

    private Joueur.Scene demanderFief(int e) {
        float requis = FAVEUR_FIEF + 20f * fiefs.size();
        if (faveur(e) < requis) return cour("« Un fief se mérite. » (faveur " + Math.round(faveur(e)) + " sur " + Math.round(requis) + ")");
        int choix = -1;
        for (int v : villesDe(e)) {
            boolean pris = fief(v) != null;
            if (!pris && (choix < 0 || p.villes.habitants[v] < p.villes.habitants[choix])) choix = v;
        }
        if (choix < 0) return cour("« Il n'y a plus de terre à donner dans ce royaume. »");
        Fief x = new Fief();
        x.ville = choix;
        fiefs.add(x);
        changerFaveur(e, -20f);
        Fiche f = j.fiche();
        String phrase = "En l'an " + Terre.annee(p, j.jourMaintenant()) + ", " + f.nom + " reçoit en fief " + ville(choix) + ".";
        f.recit.add(phrase);
        j.noter(phrase);
        j.renommee().gagner(choix, 40f, 5f, "nouveau seigneur");
        j.social().grandMoment("premier fief");
        j.destin().verifierEtapes();
        j.enregistrer();
        return cour("« Nous te donnons " + ville(choix) + " (" + p.villes.habitants[choix] + " âmes), ses terres et ses gens. Tu nous en devras l'aide, chaque saison. » Va voir ton fief : la mairie t'y attend.");
    }

    // ================================================================ Le tournoi

    private Joueur.Scene tournoi(int e) {
        Fiche f = j.fiche();
        if (f.argent < ENTREE_TOURNOI) return cour("Il faut " + Fiche.argentTexte(ENTREE_TOURNOI) + " pour s'inscrire.");
        f.argent -= ENTREE_TOURNOI;
        dernierTournoi = Math.floorDiv(j.jourMaintenant(), Terre.JOURS_PAR_SAISON);
        Random h = new Random(Travail.melanger(f.id * 401L + j.jourMaintenant()));
        int[] eq = Combat.equipement(f.sac.portes());
        StringBuilder t = new StringBuilder("Les bannières claquent au vent ; la foule se presse autour de la lice.");
        int gagnees = 0;
        float sante = 1f;
        for (int k = 0; k < FORCE_JOUTE.length; k++) {
            List<Combat.Combattant> tous = new ArrayList<>();
            Combat.Combattant moi = new Combat.Combattant();
            moi.nom = f.nom;
            moi.joueur = true;
            moi.camp = Combat.NOUS;
            moi.graine = -1L;
            moi.arme = eq[0];
            moi.armure = eq[1];
            moi.force = Combat.forceJoueur(f, eq[0]) + f.competences[Fiche.EQUITATION] / 10f;
            moi.sante = sante;
            tous.add(moi);
            Combat.Combattant c = new Combat.Combattant();
            boolean dame = h.nextInt(4) == 0;
            c.nom = Noms.personne(p.etats.peuple[e], p.graine, h.nextLong(), dame ? 1 : 0);
            c.femme = dame;
            c.camp = Combat.EUX;
            c.graine = h.nextLong();
            c.force = FORCE_JOUTE[k] + h.nextInt(8);
            c.arme = Biens.EPEE.id;
            tous.add(c);
            Combat.Resultat res = Combat.resoudre(tous, f.competences[Fiche.TACTIQUE], 30f, 80f, 80f, h.nextLong());
            boolean gagne = forcer || res.vainqueur == Combat.NOUS;
            sante = Math.max(0.3f, res.tous.get(0).sante);
            t.append("\n").append(k == 0 ? "Premier" : k == 1 ? "Deuxième" : "Dernier").append(" adversaire, ").append(c.nom).append(" : ").append(gagne ? "tu l'emportes." : "tu vides les étriers.");
            if (!gagne) break;
            gagnees++;
        }
        forcer = false;
        j.santeJoueur = sante;
        f.apprendre(Fiche.EQUITATION, 3f + gagnees);
        f.apprendre(Fiche.UNE_MAIN, 2f + gagnees);
        if (gagnees == FORCE_JOUTE.length) {
            tournois++;
            f.argent += PRIX_TOURNOI;
            changerFaveur(e, 10f);
            j.renommee().gagner(j.villeIci(), 20f, 5f, "tournoi gagné");
            String phrase = "En l'an " + Terre.annee(p, j.jourMaintenant()) + ", " + f.nom + " gagne le tournoi " + Creation.de(ville(j.villeIci())) + ".";
            f.recit.add(phrase);
            j.noter(phrase);
            t.append("\nTu gagnes le tournoi ! ").append(majuscule(souverain(e))).append(" te remet le prix : ").append(Fiche.argentTexte(PRIX_TOURNOI)).append('.');
        } else if (gagnees > 0) {
            j.renommee().gagner(j.villeIci(), 3f * gagnees, 0f, "tournoi");
            t.append("\nTu t'es bien battu").append(f.e()).append(" : la foule retient ton nom.");
        }
        j.passer(4.0);
        j.destin().verifierEtapes();
        j.enregistrer();
        Joueur.Scene s = new Joueur.Scene("Le tournoi", t.toString());
        s.ajouter("Continuer", () -> cour(null));
        return s;
    }

    // ================================================================ Le fief

    /** « Mes fiefs » (Mon personnage) : le service et les fiefs. */
    Joueur.Scene accueil() {
        Fiche f = j.fiche();
        StringBuilder t = new StringBuilder();
        if (noble) t.append("Tu es noble : on t'appelle ").append(titre()).append(maisonEpousee >= 0 ? ", de la " + Recit.nomMaison(p, maisonEpousee) : "").append(".\n");
        if (suzerain >= 0) t.append("Tu sers ").append(souverain(suzerain)).append(" (faveur ").append(Math.round(faveur(suzerain))).append(").\n");
        if (tache != null) t.append("Ta tâche : ").append(decrire(tache)).append(".\n");
        if (fiefs.isEmpty()) t.append("Tu n'as pas de fief.");
        for (Fief x : fiefs) t.append("\n").append(ville(x.ville)).append(" : impôt ").append(NOMS_TAUX[x.taux]).append(", ").append(x.gardes).append(" gardes, dernière saison ").append(Fiche.argentTexte(x.recue)).append(x.affaire >= 0 ? ", une affaire attend" : "");
        Joueur.Scene s = new Joueur.Scene("Mes fiefs", t.toString().trim());
        for (Fief x : fiefs) s.ajouter("Gouverner " + ville(x.ville), () -> fiefScene(x, null));
        s.ajouter("« Revenir", j::accueilScene);
        return s;
    }

    Joueur.Scene fiefScene(Fief x, String message) {
        int hab = p.villes.habitants[x.ville];
        int impot = Math.round(hab * TAUX[x.taux] * (x.travaux[GRENIER] ? 1.2f : 1f));
        StringBuilder t = new StringBuilder(ville(x.ville) + ", ton fief : " + hab + " âmes. Réputation : " + Renommee.reputationTexte(j.renommee().reputation(x.ville)) + ".");
        t.append("\nImpôt ").append(NOMS_TAUX[x.taux]).append(" : environ ").append(Fiche.argentTexte(impot)).append(" par saison").append(suzerain >= 0 ? ", dont " + Math.round(AIDE * 100) + " % d'aide à ton suzerain" : "").append('.');
        t.append("\nGardes : ").append(x.gardes).append(" (").append(Fiche.argentTexte(x.gardes * SOLDE_GARDE)).append(" par jour).");
        List<String> faits = new ArrayList<>();
        for (int k = 0; k < TRAVAUX; k++) if (x.travaux[k]) faits.add(NOMS_TRAVAUX[k]);
        if (!faits.isEmpty()) t.append(" On y a bâti ").append(String.join(", ", faits)).append('.');
        Joueur.Scene s = new Joueur.Scene("Seigneurie " + Creation.de(ville(x.ville)), Joueur.avec(message, t.toString()));
        if (x.affaire >= 0) s.ajouter("Rendre la justice : " + AFFAIRES[x.affaire][0], () -> justice(x));
        for (int k = 0; k < TAUX.length; k++) {
            int n = k;
            if (k != x.taux) s.ajouter("Fixer un impôt " + NOMS_TAUX[k], () -> {
                x.taux = n;
                j.enregistrer();
                return fiefScene(x, "L'impôt sera désormais " + NOMS_TAUX[n] + ".");
            });
        }
        if (x.gardes < GARDES_MAX) s.ajouter("Engager deux gardes", () -> {
            x.gardes = Math.min(GARDES_MAX, x.gardes + 2);
            j.enregistrer();
            return fiefScene(x, "Deux gardes de plus patrouillent à " + ville(x.ville) + ".");
        });
        if (x.gardes > 0) s.ajouter("Renvoyer deux gardes", () -> {
            x.gardes = Math.max(0, x.gardes - 2);
            j.enregistrer();
            return fiefScene(x, "Deux gardes rentrent chez eux.");
        });
        for (int k = 0; k < TRAVAUX; k++) {
            int n = k;
            if (!x.travaux[k]) s.ajouter("Faire bâtir " + NOMS_TRAVAUX[k] + " (" + Fiche.argentTexte(PRIX_TRAVAUX[k]) + ")", () -> batir(x, n));
        }
        j.armee().choixFief(s, x);
        s.ajouter("« Revenir", j::fermerScene);
        return s;
    }

    private Joueur.Scene batir(Fief x, int k) {
        Fiche f = j.fiche();
        if (f.argent < PRIX_TRAVAUX[k]) return fiefScene(x, "Il faut " + Fiche.argentTexte(PRIX_TRAVAUX[k]) + ".");
        f.argent -= PRIX_TRAVAUX[k];
        x.travaux[k] = true;
        j.renommee().gagner(x.ville, 5f, 6f, "travaux du seigneur");
        j.noter("En l'an " + Terre.annee(p, j.jourMaintenant()) + ", tu fais bâtir " + NOMS_TRAVAUX[k] + " à " + ville(x.ville) + ".");
        j.enregistrer();
        return fiefScene(x, "Les ouvriers se mettent à l'ouvrage : " + NOMS_TRAVAUX[k] + " se dresse bientôt.");
    }

    /** Les affaires de justice : { titre, choix 1, choix 2, choix 3 } ; leurs effets dans juger(). */
    static final String[][] AFFAIRES = {
        { "un voleur pris au marché", "Le faire pendre", "Le condamner à une amende", "Le gracier" },
        { "deux familles se disputent un champ", "Trancher pour la plus riche (elle te remercie)", "Trancher selon la coutume", "Partager le champ" },
        { "le meunier triche sur les poids", "Le fouet, sur la place", "Une lourde amende", "Fermer les yeux contre un présent" },
        { "les paysans demandent une remise d'impôt", "Accorder la moitié de l'impôt cette saison", "Refuser", "Leur faire grâce de la dîme du seigneur seulement" },
    };

    private Joueur.Scene justice(Fief x) {
        String[] a = AFFAIRES[x.affaire];
        Joueur.Scene s = new Joueur.Scene("La justice du seigneur", "À " + ville(x.ville) + ", " + a[0] + ". On attend ta sentence.");
        for (int k = 1; k < a.length; k++) {
            int n = k;
            s.ajouter(a[k], () -> juger(x, n));
        }
        return s;
    }

    private Joueur.Scene juger(Fief x, int choix) {
        Fiche f = j.fiche();
        int affaire = x.affaire;
        x.affaire = -1;
        // { réputation, argent, axe, valeur de l'axe } pour chaque choix.
        int[][][] effets = {
            { { -2, 0, Fiche.BIEN, -6 }, { 0, 20, Fiche.LOI, 3 }, { 3, 0, Fiche.BIEN, 5 } },
            { { -3, 40, Fiche.HONNETETE, -5 }, { 2, 0, Fiche.LOI, 4 }, { 1, 0, Fiche.BIEN, 3 } },
            { { 2, 0, Fiche.BIEN, -3 }, { 1, 60, Fiche.LOI, 3 }, { -4, 40, Fiche.HONNETETE, -6 } },
            { { 5, 0, Fiche.GENEROSITE, 6 }, { -4, 0, Fiche.GENEROSITE, -5 }, { 2, 0, Fiche.GENEROSITE, 2 } },
        };
        int[] e = effets[affaire][choix - 1];
        if (e[1] > 0) f.argent += e[1];
        if (affaire == 3 && choix == 1) x.remise = true;
        f.pencher(e[2], e[3]);
        if (e[0] != 0) j.renommee().gagner(x.ville, 1f, e[0], "justice");
        j.passer(1.0);
        j.enregistrer();
        String echo = e[0] > 0 ? "On loue ta sagesse." : e[0] < 0 ? "On murmure dans ton dos." : "La sentence est rendue.";
        return fiefScene(x, AFFAIRES[affaire][choix] + ". " + echo + (e[1] > 0 ? " (+" + Fiche.argentTexte(e[1]) + ")" : ""));
    }

    // ================================================================ Pour les essais

    public Joueur.Scene courPourEssai() {
        return cour(null);
    }

    public Joueur.Scene fiefPourEssai(int k) {
        return fiefScene(fiefs.get(k), null);
    }

    public void entrerPourEssai(int v) {
        entrer(v);
    }

    public void victoirePourEssai(int v) {
        victoire(v);
    }

    public void renomPourEssai(float r) {
        j.renommee().gagner(j.villeIci(), r, 0f, "essai");
    }

    // ================================================================ Sauvegarde

    void ecrire(DataOutputStream s) throws IOException {
        s.writeInt(suzerain);
        s.writeInt(faveurs.size());
        for (Map.Entry<Integer, Float> e : faveurs.entrySet()) {
            s.writeInt(e.getKey());
            s.writeFloat(e.getValue());
        }
        s.writeBoolean(noble);
        s.writeBoolean(servi);
        s.writeInt(voie);
        s.writeInt(maisonEpousee);
        s.writeBoolean(tache != null);
        if (tache != null) {
            s.writeInt(tache.type);
            s.writeInt(tache.ville);
            s.writeInt(tache.etat);
            s.writeInt(tache.echeance);
            s.writeInt(tache.montant);
        }
        s.writeInt(reussies);
        s.writeInt(exploits);
        s.writeInt(tournois);
        s.writeInt(defenses);
        s.writeInt(dernierTournoi);
        s.writeInt(dernierJour);
        s.writeInt(partiEtat);
        s.writeInt(partiSexe);
        s.writeLong(partiId);
        s.writeInt(fiefs.size());
        for (Fief x : fiefs) {
            s.writeInt(x.ville);
            s.writeInt(x.taux);
            s.writeInt(x.gardes);
            s.writeInt(x.affaire);
            s.writeInt(x.recue);
            s.writeBoolean(x.remise);
            for (boolean b : x.travaux) s.writeBoolean(b);
        }
    }

    void lire(DataInputStream s) throws IOException {
        suzerain = s.readInt();
        int n = s.readInt();
        for (int k = 0; k < n; k++) faveurs.put(s.readInt(), s.readFloat());
        noble = s.readBoolean();
        servi = s.readBoolean();
        voie = s.readInt();
        maisonEpousee = s.readInt();
        if (s.readBoolean()) {
            tache = new Tache();
            tache.type = s.readInt();
            tache.ville = s.readInt();
            tache.etat = s.readInt();
            tache.echeance = s.readInt();
            tache.montant = s.readInt();
        }
        reussies = s.readInt();
        exploits = s.readInt();
        tournois = s.readInt();
        defenses = s.readInt();
        dernierTournoi = s.readInt();
        dernierJour = s.readInt();
        partiEtat = s.readInt();
        partiSexe = s.readInt();
        partiId = s.readLong();
        n = s.readInt();
        for (int k = 0; k < n; k++) {
            Fief x = new Fief();
            x.ville = s.readInt();
            x.taux = s.readInt();
            x.gardes = s.readInt();
            x.affaire = s.readInt();
            x.recue = s.readInt();
            x.remise = s.readBoolean();
            for (int q = 0; q < TRAVAUX; q++) x.travaux[q] = s.readBoolean();
            fiefs.add(x);
        }
    }
}
