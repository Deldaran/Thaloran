package com.deldaran.politics.histoire;

import com.deldaran.politics.Metiers;
import com.deldaran.politics.Relations;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Phase 4 : le joueur se fait embaucher et travaille.
 *
 * Deux façons de travailler :
 *   - à la journée (journalier) : aux champs, au bois, à la mine, au marché ou au moulin, sans contrat, payé le soir ;
 *   - sous contrat chez un artisan : apprenti → compagnon → maître (chef-d'œuvre).
 * Une séance de travail fait passer le temps (on « finit la journée ») : le résultat se calcule d'après la compétence du métier,
 * la fatigue et un peu de hasard. Les journées faites produisent vraiment dans l'atelier (EconomieVille.travailJoueur)
 * et le salaire sort de sa caisse.
 */
public final class Travail {
    public static final int SANS = -1, JOURNALIER = 0, APPRENTI = 1, COMPAGNON = 2, MAITRE = 3;
    /** Le joueur travaille dans son propre atelier : pas de salaire, pas de maître, mais l'ouvrage avance et le patron est là. */
    public static final int PATRON = 4;
    public static final String[][] RANGS = { { "journalier", "journalière" }, { "apprenti", "apprentie" }, { "compagnon", "compagnonne" }, { "maître", "maîtresse" } };

    /** Heures qui font une journée complète de travail (une journée d'ouvrier dans l'économie). */
    public static final float HEURES_JOUR = 8f;
    /** Heures locales d'ouverture et de fermeture des ateliers. */
    public static final int OUVERTURE = 6, FERMETURE = 19;
    /** Salaires par jour, en cuivre : journalier 6 + compétence ÷ 25 ; apprenti 3 (nourri) ; compagnon 8 + compétence ÷ 15 ; maître 14 + compétence ÷ 10. */
    public static final float SALAIRE_JOURNALIER = 6f, SALAIRE_APPRENTI = 3f, SALAIRE_MAITRE = 14f;
    /** Rendement quand on fait passer le temps plutôt que de travailler à la main (le travail à la main viendra plus tard). */
    public static final float PASSE = 0.9f;
    /** Fatigue gagnée par heure de travail (moins avec l'Endurance), perdue par heure de repos ; au-delà de FATIGUE_MAX on ne peut plus travailler. */
    public static final float FATIGUE_HEURE = 8f, REPOS_HEURE = 5f, FATIGUE_MAX = 90f;
    /** Expérience gagnée par heure dans la compétence du métier (× 1,5 pour un apprenti, que le maître instruit). */
    public static final float APPRENTISSAGE = 6f;
    /** Passer compagnon : compétence et jours d'apprentissage ; passer maître : compétence, jours de compagnon, droit payé à la guilde. */
    public static final int COMPETENCE_COMPAGNON = 40, JOURS_APPRENTI = 20, COMPETENCE_MAITRE = 100, JOURS_COMPAGNON = 30, DROIT_MAITRISE = 150;
    /** Ce que le maître attend d'une heure de travail selon le rang (en rendement) : au-dessus, il est content ; en dessous, il l'est moins. */
    public static final float[] ATTENDU = { 0f, 0.3f, 0.8f, 1.3f };
    /** Chances par journée complète : accident (travail de force), client difficile, caisse ouverte, éloge du maître. */
    public static final double CHANCE_ACCIDENT = 0.05, CHANCE_CLIENT = 0.08, CHANCE_VOL = 0.04, CHANCE_ELOGE = 0.08;
    /** Estime du maître (0 à 100) : on est renvoyé sous RENVOI ; chaque jour manqué au-delà du jour de repos coûte ABSENCE. */
    public static final float RENVOI = 15f, ABSENCE = 8f;

    private static final int RIEN = 0, ACCIDENT = 1, CLIENT = 2, VOL = 3, ELOGE = 4;

    private final Joueur j;
    private final Population p;

    // ---- État sauvegardé
    public int ville = -1, lieu = -1, rang = SANS;
    public int salaire, debut, dernierJour, manquesComptes, arrieres, essaiMaitrise = -1000;
    /** Jours où l'on est venu travailler (en tout chez ce maître, et depuis le dernier rang). */
    public int joursTravailles, joursRang;
    public float heuresTravaillees, estime = 50f, fatigue;
    public double reposDepuis = Double.NaN;
    /** Bit (1 << lieu) pour chaque métier où le joueur a été reçu maître. */
    public long maitrises;
    /** Ce qui s'est passé pendant l'absence du joueur (renvoi…), montré à la prochaine ouverture. */
    public String nouvelle = "";

    /** Refus d'embauche du jour, par lieu : on ne redemande pas le même jour. */
    private final Map<Integer, Integer> refus = new HashMap<>();

    Travail(Joueur j, Population p) {
        this.j = j;
        this.p = p;
    }

    // ---------------------------------------------------------------- Les métiers

    /** La compétence qui sert dans ce lieu. */
    public static int competence(int lieu) {
        switch (lieu) {
            case Metiers.FORGE: return Fiche.FORGE;
            case Metiers.MENUISERIE: case Biens.FORET: return Fiche.BOIS;
            case Metiers.BOULANGERIE: case Metiers.BOUCHERIE: case Metiers.TANNERIE: case Metiers.TISSERAND: case Metiers.POTIER: return Fiche.METIERS_FINS;
            case Metiers.AUBERGE: case Metiers.TAVERNE: return Fiche.ELOQUENCE;
            case Metiers.GRENIER: case Metiers.ENTREPOT: return Fiche.INTENDANCE;
            case Metiers.PECHERIE: return Fiche.SURVIE;
            case Metiers.ECOLE: return Fiche.LETTRES;
            case Biens.MARCHE: return Fiche.COMMERCE;
            default: return Fiche.LABEUR;
        }
    }

    /** Un travail de force : on y gagne aussi de l'Athlétisme, et on peut s'y blesser. */
    static boolean physique(int lieu) {
        switch (lieu) {
            case Biens.CHAMPS: case Biens.FORET: case Biens.MINE: case Biens.MARCHE: case Metiers.MOULIN: case Metiers.FORGE: case Metiers.MENUISERIE:
            case Metiers.PECHERIE: case Metiers.GRENIER: case Metiers.ENTREPOT: case Metiers.BOUCHERIE: return true;
            default: return false;
        }
    }

    /** On y sert des clients (scènes de client difficile). */
    static boolean clients(int lieu) {
        return lieu == Biens.MARCHE || lieu == Metiers.AUBERGE || lieu == Metiers.TAVERNE || lieu == Metiers.BOULANGERIE || lieu == Metiers.BOUCHERIE;
    }

    /** On peut s'y louer à la journée. */
    static boolean journalier(EconomieVille.Agent a) {
        switch (a.lieu) {
            case Biens.CHAMPS: case Biens.FORET: case Biens.MINE: case Biens.MARCHE: case Metiers.MOULIN: case Metiers.GRENIER: case Metiers.ENTREPOT: case Metiers.PECHERIE: return true;
            default: return false;
        }
    }

    /** On peut y signer un contrat (il faut un atelier qui produit ; les étals du marché ne prennent que des journaliers). */
    static boolean contrat(EconomieVille.Agent a) {
        return !a.recettes.isEmpty();
    }

    /** On peut y devenir maître : seulement les métiers de guilde, pas les fermes, les bois ni la mine. */
    static boolean guilde(int lieu) {
        return lieu >= 0 && lieu < Metiers.NOMBRE;
    }

    static String gestes(int lieu) {
        switch (lieu) {
            case Biens.CHAMPS: return "Tu fauches, tu lies les gerbes, tu sors les bêtes et tu nettoies l'étable.";
            case Biens.FORET: return "Tu abats des arbres, tu les ébranches et tu débites les billes.";
            case Biens.MINE: return "Tu piques le minerai au fond de la galerie et tu remontes les paniers.";
            case Biens.MARCHE: return "Tu portes des sacs entre les étals et tu tiens une table quand le marchand s'absente.";
            case Metiers.MOULIN: return "Tu portes les sacs de grain, tu verses dans la trémie et tu surveilles les meules.";
            case Metiers.BOULANGERIE: return "Tu pétris la pâte, tu chauffes le four et tu enfournes les miches.";
            case Metiers.BOUCHERIE: return "Tu découpes les carcasses, tu sales les jambons et tu sers au comptoir.";
            case Metiers.GRENIER: case Metiers.ENTREPOT: return "Tu comptes les sacs, tu tiens le registre et tu ranges les arrivages.";
            case Metiers.FORGE: return "Tu tiens le soufflet, tu frappes le fer sur l'enclume et tu trempes les pièces.";
            case Metiers.MENUISERIE: return "Tu scies les planches, tu rabotes et tu assembles.";
            case Metiers.TANNERIE: return "Tu grattes les peaux et tu les plonges dans les cuves qui puent.";
            case Metiers.TISSERAND: return "Tu cardes la laine, tu files et tu passes la navette sur le métier.";
            case Metiers.POTIER: return "Tu tournes l'argile et tu surveilles le four.";
            case Metiers.AUBERGE: case Metiers.TAVERNE: return "Tu brasses la bière, tu sers les tables et tu écoutes les clients.";
            case Metiers.PECHERIE: return "Tu remontes les filets et tu sales le poisson.";
            case Metiers.ECOLE: return "Tu copies des textes et tu fais réciter les enfants.";
            default: return "Tu fais ce qu'on te dit.";
        }
    }

    public static int salaire(int rang, int competence) {
        switch (rang) {
            case JOURNALIER: return Math.round(SALAIRE_JOURNALIER + competence / 25f);
            case APPRENTI: return Math.round(SALAIRE_APPRENTI);
            case COMPAGNON: return Math.round(Recettes.SALAIRE + competence / 15f);
            default: return Math.round(SALAIRE_MAITRE + competence / 10f);
        }
    }

    /** Ce qu'une heure de travail vaut en heures d'un ouvrier ordinaire : 0,35 sans savoir-faire, 0,85 à 60, 1,35 à 120, 2 à 200. */
    public static float rendement(int competence) {
        return Math.max(0.35f, Math.min(2.2f, 0.35f + competence / 120f));
    }

    private Fiche f() {
        return j.fiche();
    }

    private String rangNom(int r) {
        return RANGS[r][f().femme() ? 1 : 0];
    }

    /** Mélange une graine (SplitMix64) : deux graines voisines donnent des tirages sans rapport. */
    static long melanger(long x) {
        x += 0x9E3779B97F4A7C15L;
        x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
        return x ^ (x >>> 31);
    }

    private static String minuscule(String s) {
        return s.isEmpty() ? s : Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    /** « au moulin », « à la forge », « aux fermes ». */
    private static String au(int lieu) {
        String n = minuscule(Biens.nomBoutique(lieu));
        if (n.startsWith("le ")) return "au " + n.substring(3);
        if (n.startsWith("les ")) return "aux " + n.substring(4);
        return "à " + n;
    }

    /** Le maître du lieu : le villageois qui y travaille (s'il est à l'écran), sinon un nom générique. */
    /** Le nom du maître de ce lieu, ou « le patron ». */
    String maitreNom(int lieu) {
        return maitre(lieu);
    }

    private String maitre(int lieu) {
        Relations.Lien l = lien(lieu);
        if (l != null) return l.nomComplet();
        switch (lieu) {
            case Biens.CHAMPS: return "le fermier";
            case Biens.FORET: return "le maître bûcheron";
            case Biens.MINE: return "le contremaître de la mine";
            case Biens.MARCHE: return "les marchands";
            default: return "le patron";
        }
    }

    private Relations.Lien lien(int lieu) {
        Relations.Lien[] m = j.maitres;
        return m != null && lieu >= 0 && lieu < m.length ? m[lieu] : null;
    }

    private void toucherLien(int lieu, float estimeLien) {
        Relations.Lien l = lien(lieu);
        if (l == null || estimeLien == 0f) return;
        l.estime = Math.max(-1f, Math.min(1f, l.estime + estimeLien));
        Relations r = j.relations;
        if (r != null) r.modifie = true;
    }

    private static String humeur(float estime) {
        if (estime >= 80f) return "très content";
        if (estime >= 60f) return "content";
        if (estime >= 40f) return "assez content";
        if (estime >= 25f) return "mécontent";
        return "à bout";
    }

    private static String heureTexte(double h) {
        int hh = (int) Math.floor(h) % 24, mm = (int) ((h - Math.floor(h)) * 60.0);
        return hh + " h" + (mm >= 10 ? "" + mm : mm > 0 ? "0" + mm : "");
    }

    public boolean employe() {
        return rang >= APPRENTI;
    }

    String ville(int v) {
        return Noms.ville(p.villes.peuple[v], p.graine, v);
    }

    // ---------------------------------------------------------------- Temps, fatigue, absences

    /** La fatigue baisse avec le temps passé hors du travail. */
    void reposer() {
        double maintenant = j.maintenant();
        if (Double.isNaN(reposDepuis) || maintenant < reposDepuis) reposDepuis = maintenant;
        fatigue = Math.max(0f, fatigue - (float) (maintenant - reposDepuis) * REPOS_HEURE);
        reposDepuis = maintenant;
    }

    /** Compte les jours manqués : un jour de repos est permis entre deux jours de travail, chaque jour en plus fâche le maître. */
    void verifier() {
        if (!employe()) return;
        int manques = Math.max(0, j.jourMaintenant() - dernierJour - 2);
        if (manques > manquesComptes) {
            estime -= (manques - manquesComptes) * ABSENCE;
            manquesComptes = manques;
            if (estime < RENVOI) {
                String m = maitre(lieu);
                nouvelle = Character.toUpperCase(m.charAt(0)) + m.substring(1) + " t'a renvoyé" + f().e() + " : tu n'es pas venu" + f().e() + " travailler depuis " + (manques + 1) + " jours.";
                toucherLien(lieu, -0.1f);
                j.renommee().gagner(ville, 0f, -5f, null);
                quitter();
            }
        }
    }

    void quitter() {
        rang = SANS;
        lieu = -1;
        ville = -1;
        arrieres = 0;
        estime = 50f;
    }

    // ---------------------------------------------------------------- Scènes

    /** La scène « Travail » (touche L). */
    public Joueur.Scene accueil() {
        return accueil(null);
    }

    Joueur.Scene accueil(String message) {
        Fiche f = f();
        if (f == null) return j.accueilScene();
        reposer();
        verifier();
        if (!nouvelle.isEmpty()) {
            message = message == null ? nouvelle : nouvelle + "\n\n" + message;
            nouvelle = "";
            j.enregistrer();
        }
        double h = j.heureIci();
        int horaire = employe() ? lieu : -1;
        boolean ouvert = Quotidien.ouvert(horaire, h);
        int v = j.villeIci();
        StringBuilder t = new StringBuilder();
        Joueur.Scene s = new Joueur.Scene("Travail", "");
        if (employe()) {
            int c = f.competences[competence(lieu)];
            t.append("Tu es ").append(rangNom(rang)).append(' ').append(au(lieu)).append(' ').append(Creation.de(ville(ville))).append(", chez ").append(maitre(lieu))
                    .append(". Salaire : ").append(Fiche.argentTexte(salaire)).append(" par jour");
            if (rang == APPRENTI) t.append(", nourri").append(f.e());
            t.append(".");
            if (arrieres > 0) t.append(" On te doit encore ").append(Fiche.argentTexte(arrieres)).append('.');
            int comp = competence(lieu), plafond = f.plafond(comp);
            t.append("\nTu es venu").append(f.e()).append(" travailler ").append(joursTravailles).append(joursTravailles > 1 ? " jours" : " jour").append(". Ton maître est ").append(humeur(estime)).append(" de toi.");
            t.append("\n").append(Fiche.NOMS_COMPETENCES[comp]).append(" : ").append(c).append(" (ta main plafonne à ").append(plafond).append(").");
            int vise = rang == APPRENTI ? COMPETENCE_COMPAGNON : COMPETENCE_MAITRE;
            if (rang == APPRENTI) t.append(" Pour passer ").append(rangNom(COMPAGNON)).append(" : ").append(COMPETENCE_COMPAGNON).append(", ").append(JOURS_APPRENTI)
                    .append(" jours d'apprentissage (tu en as ").append(joursRang).append(") et un maître content.");
            else if (rang == COMPAGNON && guilde(lieu)) t.append(" Pour passer ").append(rangNom(MAITRE)).append(" : ").append(COMPETENCE_MAITRE).append(", ").append(JOURS_COMPAGNON)
                    .append(" jours comme ").append(rangNom(COMPAGNON)).append(" (tu en as ").append(joursRang).append("), un chef-d'œuvre et ")
                    .append(Fiche.argentTexte(DROIT_MAITRISE)).append(" de droit.");
            if (rang <= COMPAGNON && plafond < vise && (rang == APPRENTI || guilde(lieu)))
                t.append(" Sans plus de don (").append(Fiche.NOMS_ATTRIBUTS[Fiche.attributDe(comp)]).append(" ou focus), tu n'y arriveras pas.");
            if (v != ville) t.append("\nTon travail est ").append(Creation.de(ville(ville))).append(" : tu n'y es pas.");
            else {
                EconomieVille.Agent a = j.agentIci(lieu);
                if (a == null) {
                    EconomieVille.Agent loin = j.marche().ville(v, j.jour()).agent(lieu);
                    t.append("\n").append(Biens.nomBoutique(lieu)).append(" : ").append(loin == null ? "fermé" : j.ouEst(loin)).append(". Va jusqu'à ton poste pour travailler.");
                } else if (ouvert) {
                    float reste = (float) (Quotidien.fermeture(lieu) - h), possible = Math.min(reste, heuresPossibles());
                    if (possible < 0.5f) t.append("\nTu es trop fatigué").append(f.e()).append(" pour travailler.");
                    else {
                        s.ajouter("Travailler à la main (tu fais les gestes, le temps défile)", () -> commencerAMain(lieu, rang));
                        if (possible >= 3f) s.ajouter((possible < reste - 0.5f ? "Faire passer le temps jusqu'à n'en plus pouvoir (" : "Faire passer le temps jusqu'au soir (") + Math.round(possible) + " h)",
                                () -> travailler(lieu, rang, reste));
                        s.ajouter("Faire passer deux heures", () -> travailler(lieu, rang, Math.min(2f, reste)));
                    }
                }
                if (rang == COMPAGNON && guilde(lieu) && c >= COMPETENCE_MAITRE && joursRang >= JOURS_COMPAGNON && estime >= 60f
                        && j.jourMaintenant() - essaiMaitrise >= 10 && a != null)
                    s.ajouter("Présenter un chef-d'œuvre (" + Fiche.argentTexte(DROIT_MAITRISE) + ")", this::chefDoeuvre);
                if (salaire(rang, c) > salaire + 1 && a != null) s.ajouter("Demander une augmentation", this::augmentation);
            }
        } else t.append("Tu n'as pas d'emploi. Tu peux te louer à la journée aux champs, au bois, à la mine, au moulin ou au marché, ou entrer chez un artisan.");
        t.append("\n\nIl est ").append(heureTexte(h)).append(ouvert ? "" : " : c'est fermé (on y travaille de " + Quotidien.ouverture(horaire) + " h à " + Quotidien.fermeture(horaire) + " h)")
                .append(". Fatigue : ").append(Math.round(fatigue)).append(" %. Tu as ").append(Fiche.argentTexte(f.argent)).append('.');
        // Lot D : devant son propre atelier, le patron peut se mettre à l'ouvrage.
        if (v >= 0 && ouvert) for (Entreprise.Atelier x : j.entreprise().ateliers) {
            if (x.ville != v || Recettes.de(x.lieu).isEmpty() || j.atelierDevant(x.lieu) == null || j.agentIci(x.lieu) == null) continue;
            int l = x.lieu;
            s.ajouter("Travailler dans " + Entreprise.possessif(Biens.nomBoutique(l)) + " (à la main)", () -> commencerAMain(l, PATRON));
        }
        if (v >= 0) s.ajouter(employe() ? "Chercher un autre travail" : "Chercher du travail", () -> chercher(null));
        if (!ouvert || fatigue > 30f) {
            float jusqua = (float) (((Quotidien.ouverture(horaire) - h) % 24.0 + 24.0) % 24.0);
            if (jusqua < 1f) jusqua += 24f;
            float attente = jusqua;
            s.ajouter("Dormir… (" + Math.round(attente) + " h jusqu'au matin)", j::dormir);
        }
        j.quotidien().choixTravail(s);
        if (employe()) s.ajouter("Quitter cet emploi", this::demission);
        s.ajouter("« Revenir", j::accueilScene);
        Joueur.Scene vraie = new Joueur.Scene("Travail", Joueur.avec(message, t.toString()));
        vraie.choix.addAll(s.choix);
        vraie.suites.addAll(s.suites);
        return vraie;
    }

    /** Les lieux de la ville où l'on peut travailler : ceux devant lesquels on est d'abord, puis les autres avec leur distance. */
    Joueur.Scene chercher(String message) {
        int v = j.villeIci();
        if (v < 0) return accueil("Tu n'es dans aucune ville.");
        EconomieVille e = j.marche().ville(v, j.jour());
        Joueur.Scene s = new Joueur.Scene("Chercher du travail", Joueur.avec(message,
                "On demande du travail en personne : va jusqu'à l'atelier, la ferme ou l'étal, et parle au patron.\nAux champs, au bois, à la mine, au moulin et au marché, on embauche aussi à la journée."));
        for (int passe = 0; passe < 2; passe++) {
            for (EconomieVille.Agent a : e.agents) {
                if (a.lieu == Biens.DEHORS || a.joueur || !(journalier(a) || contrat(a)) || j.proche(a) != (passe == 0)) continue;
                String quoi = journalier(a) && contrat(a) ? "journées ou contrat" : journalier(a) ? "à la journée" : "contrat";
                if (passe == 0) s.ajouter(a.nom + " — ici (" + quoi + ")", () -> offre(a.lieu, null));
                else s.ajouter(a.nom + " — " + j.ouEst(a) + " (" + quoi + ")", () -> chercher(a.nom + " : " + j.ouEst(a) + ". Va sur place pour demander."));
            }
        }
        s.ajouter("« Revenir", () -> accueil(null));
        return s;
    }

    /** Devant un atelier : est-ce qu'on y embauche, à quel prix. */
    Joueur.Scene offre(int lieu, String message) {
        EconomieVille.Agent a = j.agentIci(lieu);
        if (a == null) return chercher("Tu n'es plus assez près.");
        Fiche f = f();
        int comp = competence(lieu), c = f.competences[comp];
        StringBuilder t = new StringBuilder(a.nom).append(", chez ").append(maitre(lieu)).append(". ").append(gestes(lieu));
        if (!a.recettes.isEmpty()) {
            t.append("\n").append(Math.round(a.ouvriers)).append(a.ouvriers >= 1.5f ? " personnes y travaillent" : " personne y travaille");
            t.append(a.emploi > 0.85f ? " et elles croulent sous l'ouvrage." : a.emploi > 0.6f ? " et le travail ne manque pas." : " et c'est plutôt calme.");
        }
        t.append("\nIci, il faut du ").append(Fiche.NOMS_COMPETENCES[comp]).append(" : tu as ").append(c).append('.');
        Joueur.Scene s = new Joueur.Scene(a.nom, "");
        double h = j.heureIci();
        boolean ouvert = Quotidien.ouvert(lieu, h);
        if (journalier(a)) {
            int sj = salaire(JOURNALIER, c);
            t.append("\nÀ la journée, on te paierait ").append(Fiche.argentTexte(sj)).append(" pour une journée complète.");
            if (ouvert) {
                float reste = (float) (Quotidien.fermeture(lieu) - h);
                s.ajouter("Se louer et travailler à la main", () -> commencerAMain(lieu, JOURNALIER));
                if (reste >= 3f) s.ajouter("Se louer et faire passer le temps jusqu'au soir (" + Math.round(reste) + " h, environ " + Fiche.argentTexte(Math.round(sj * reste / HEURES_JOUR)) + ")", () -> louer(lieu, reste));
                s.ajouter("Se louer deux heures (passer le temps)", () -> louer(lieu, Math.min(2f, reste)));
            } else t.append(" Reviens entre ").append(Quotidien.ouverture(lieu)).append(" h et ").append(Quotidien.fermeture(lieu)).append(" h.");
        }
        if (contrat(a)) {
            int r = rangPossible(lieu, c);
            if (employe() && lieu == this.lieu && j.villeIci() == ville) t.append("\nC'est ici que tu travailles.");
            else s.ajouter("Demander à entrer comme " + rangNom(r) + " (" + Fiche.argentTexte(salaire(r, c)) + " par jour)", () -> embauche(lieu, r));
        }
        s.ajouter("« Revenir", () -> chercher(null));
        Joueur.Scene vraie = new Joueur.Scene(a.nom, Joueur.avec(message, t.toString()));
        vraie.choix.addAll(s.choix);
        vraie.suites.addAll(s.suites);
        return vraie;
    }

    private int rangPossible(int lieu, int c) {
        if (guilde(lieu) && (maitrises & (1L << lieu)) != 0L) return MAITRE;
        return c >= COMPETENCE_COMPAGNON ? COMPAGNON : APPRENTI;
    }

    /**
     * Le patron décide. Il regarde s'il a besoin de bras (l'occupation de l'atelier), ton savoir-faire, ta prestance,
     * ce qu'il pense de toi (son Lien avec toi), ton renom, et si tu es d'ici.
     */
    float score(EconomieVille.Agent a, int r) {
        Fiche f = f();
        int c = f.competences[competence(a.lieu)];
        float besoin = Math.min(1f, a.emploi);
        if (a.ouvriers >= a.maxOuvriers() - 0.5f) besoin -= 0.3f;
        Relations.Lien l = lien(a.lieu);
        float s = 50f * besoin + c / 4f + 3f * f.attributs[Fiche.PRESENCE] + f.competences[Fiche.ELOQUENCE] / 15f + f.renom / 10f;
        if (l != null) s += 30f * l.estime - 30f * l.rancune;
        if (p.villes.peuple[j.villeIci()] != f.peuple) s -= 10f;
        if (r == APPRENTI) s += 5f;
        return s;
    }

    /** Score à atteindre : 35 pour un apprenti, 45 pour un compagnon, 50 pour un maître. */
    static float seuil(int r) {
        return r == APPRENTI ? 35f : r == COMPAGNON ? 45f : 50f;
    }

    private Joueur.Scene embauche(int lieu, int r) {
        EconomieVille.Agent a = j.agentIci(lieu);
        if (a == null) return chercher("Tu n'es plus assez près.");
        int aujourdhui = j.jourMaintenant();
        Integer dejaRefuse = refus.get(lieu);
        String m = maitre(lieu);
        String M = Character.toUpperCase(m.charAt(0)) + m.substring(1);
        if (dejaRefuse != null && dejaRefuse == aujourdhui) return offre(lieu, M + " t'a déjà répondu aujourd'hui. Reviens un autre jour.");
        float sc = score(a, r);
        if (sc < seuil(r)) {
            refus.put(lieu, aujourdhui);
            String pourquoi = a.emploi < 0.5f ? "Il n'y a pas assez d'ouvrage pour une personne de plus." : r == APPRENTI ? "Je ne te connais pas, et je n'ai pas le temps d'instruire n'importe qui."
                    : "Tu n'as pas encore la main qu'il faut ici.";
            return offre(lieu, M + " secoue la tête. « " + pourquoi + " »");
        }
        boolean ailleurs = employe();
        if (ailleurs) toucherLien(this.lieu, -0.05f);
        Fiche f = f();
        ville = j.villeIci();
        this.lieu = lieu;
        rang = r;
        salaire = salaire(r, f.competences[competence(lieu)]);
        debut = aujourdhui;
        dernierJour = aujourdhui;
        manquesComptes = 0;
        heuresTravaillees = 0f;
        joursTravailles = 0;
        joursRang = 0;
        arrieres = 0;
        estime = 50f;
        toucherLien(lieu, 0.05f);
        j.noter("En l'an " + p.annee + ", tu entres comme " + rangNom(r) + " " + au(lieu) + " " + Creation.de(ville(ville)) + ", chez " + m + ".");
        j.enregistrer();
        return accueil(M + " accepte. « Tu commences demain à l'aube, " + Fiche.argentTexte(salaire) + " par jour" + (r == APPRENTI ? ", nourri" + f.e() : "") + ". Ne sois pas en retard. »"
                + (ailleurs ? "\nTu quittes ton ancien emploi." : ""));
    }

    private Joueur.Scene demission() {
        Joueur.Scene s = new Joueur.Scene("Quitter cet emploi", "Tu es sûr" + f().e() + " ? " + Character.toUpperCase(maitre(lieu).charAt(0)) + maitre(lieu).substring(1)
                + " ne t'en voudra pas trop si tu as bien travaillé." + (arrieres > 0 ? " On te doit encore " + Fiche.argentTexte(arrieres) + " : tu les perdras." : ""));
        s.ajouter("Oui, je pars", () -> {
            toucherLien(lieu, estime >= 50f ? 0f : -0.05f);
            j.noter("Tu quittes ton travail " + au(lieu) + " " + Creation.de(ville(ville)) + ".");
            quitter();
            j.enregistrer();
            return accueil("Tu rends ton tablier.");
        });
        s.ajouter("Non, je reste", () -> accueil(null));
        return s;
    }

    private Joueur.Scene augmentation() {
        Fiche f = f();
        int voulu = salaire(rang, f.competences[competence(lieu)]);
        String M = Character.toUpperCase(maitre(lieu).charAt(0)) + maitre(lieu).substring(1);
        if (estime >= 55f) {
            salaire = voulu;
            estime -= 2f;
            j.enregistrer();
            return accueil(M + " grogne, mais reconnaît que tu as fait des progrès. Tu gagnes désormais " + Fiche.argentTexte(salaire) + " par jour.");
        }
        estime -= 4f;
        j.enregistrer();
        return accueil(M + " te regarde de haut. « Travaille mieux d'abord, on en reparlera. »");
    }

    private Joueur.Scene chefDoeuvre() {
        Fiche f = f();
        EconomieVille.Agent a = j.agentIci(lieu);
        if (a == null) return accueil("Tu n'es plus à l'atelier.");
        if (f.argent < DROIT_MAITRISE) return accueil("Il te faut " + Fiche.argentTexte(DROIT_MAITRISE) + " pour payer le droit à la guilde.");
        f.argent -= DROIT_MAITRISE;
        int c = f.competences[competence(lieu)];
        Random hasard = new Random(melanger(f.id * 977L + j.jourMaintenant() * 131L + lieu));
        int jet = c + 10 * f.attributs[Fiche.HABILETE] + hasard.nextInt(61);
        essaiMaitrise = j.jourMaintenant();
        j.passer(2 * HEURES_JOUR);
        fatigue = Math.min(100f, fatigue + 40f);
        reposDepuis = j.maintenant();
        if (jet >= 200) {
            maitrises |= 1L << lieu;
            rang = MAITRE;
            joursRang = 0;
            salaire = salaire(MAITRE, c);
            estime = Math.min(100f, estime + 10f);
            j.renommee().gagner(ville, 15f, 8f, "Reçu" + f.e() + " maître");
            j.renommee().marquer(Renommee.MAITRE);
            j.renommee().metierMaitre = lieu;
            j.renommee().guilde(lieu, 15f);
            j.social().grandMoment("reçu" + f.e() + " maître");
            toucherLien(lieu, 0.15f);
            String phrase = "En l'an " + p.annee + ", tu es reçu" + f.e() + " " + rangNom(MAITRE) + " " + minuscule(Metiers.NOMS[lieu]) + " " + Creation.de(ville(ville)) + ".";
            f.recit.add(phrase);
            j.noter(phrase);
            j.enregistrer();
            return accueil("Tu passes deux jours sur ton chef-d'œuvre. Les maîtres de la guilde l'examinent longtemps, puis hochent la tête : tu es reçu" + f.e()
                    + " " + rangNom(MAITRE) + " ! Tu gagnes " + Fiche.argentTexte(salaire) + " par jour, et un jour tu pourras ouvrir ton propre atelier.");
        }
        j.enregistrer();
        return accueil("Tu passes deux jours sur ton chef-d'œuvre, mais les maîtres de la guilde y trouvent des défauts. Le droit est perdu ; tu pourras réessayer dans dix jours.");
    }

    /** Fatigue gagnée par heure de travail : moins avec de l'Endurance. */
    private float parHeure() {
        return FATIGUE_HEURE * Math.max(0.5f, 1.2f - 0.04f * f().attributs[Fiche.ENDURANCE]);
    }

    /** Heures qu'on peut encore travailler avant d'être épuisé. */
    private float heuresPossibles() {
        return Math.max(0f, (FATIGUE_MAX - fatigue) / parHeure());
    }

    private Joueur.Scene louer(int lieu, float heures) {
        EconomieVille.Agent a = j.agentIci(lieu);
        if (a == null) return chercher("Tu n'es plus assez près.");
        if (!a.source() && a.lieu != Biens.MARCHE && a.emploi < 0.3f) {
            String M = Character.toUpperCase(maitre(lieu).charAt(0)) + maitre(lieu).substring(1);
            return offre(lieu, M + " n'a besoin de personne aujourd'hui.");
        }
        return travailler(lieu, JOURNALIER, heures);
    }

    private Joueur.Scene attendre(float heures, String texte) {
        j.passer(heures);
        reposer();
        fatigue = Math.max(0f, fatigue - heures * REPOS_HEURE);
        reposDepuis = j.maintenant();
        j.enregistrer();
        return accueil(texte);
    }

    /** Ce qu'a donné un moment de travail. */
    private static final class Seance {
        int evenement = RIEN, paye, du, gagnes;
        float jours, fait;
        /** Ce qui s'est passé (accident, client…), une phrase par événement. */
        final StringBuilder recit = new StringBuilder();
    }

    /**
     * Le cœur du travail, commun au temps qu'on fait passer et au travail à la main : l'atelier produit, le salaire sort de sa caisse,
     * on apprend, on se fatigue, le maître juge, et parfois il arrive quelque chose. « facteur » : PASSE ou A_LA_MAIN.
     */
    private Seance seance(EconomieVille.Agent a, int lieu, int r, float heures, float facteur, long graine) {
        Fiche f = f();
        j.quotidien().seance(r, heures);
        Seance s = new Seance();
        int comp = competence(lieu), c = f.competences[comp];
        float rendement = rendement(c) * (1f - fatigue / 250f) * facteur * j.destin().facteurMoral();
        int aujourdhui = j.jourMaintenant();
        Random hasard = new Random(melanger(graine));
        float part = heures / HEURES_JOUR;
        double tirage = hasard.nextDouble();
        boolean contrat = r != JOURNALIER && r != PATRON;
        double seuil = 0.0;
        if (physique(lieu) && tirage < (seuil += CHANCE_ACCIDENT * part)) s.evenement = ACCIDENT;
        else if (clients(lieu) && tirage < (seuil += CHANCE_CLIENT * part)) s.evenement = CLIENT;
        else if (contrat && a.caisse > 60.0 && tirage < (seuil += CHANCE_VOL * part)) s.evenement = VOL;
        else if (contrat && rendement > ATTENDU[r] && tirage < (seuil += CHANCE_ELOGE * part)) s.evenement = ELOGE;
        String M = Character.toUpperCase(maitre(lieu).charAt(0)) + maitre(lieu).substring(1);
        float gainEstime = contrat ? ((rendement - ATTENDU[r]) * 3f + 0.4f) * part : 0f;
        if (s.evenement == ACCIDENT) {
            boolean evite = hasard.nextInt(10) + f.attributs[Fiche.ADRESSE] >= 9;
            if (evite) s.recit.append("Un faux mouvement : tu évites de justesse de te blesser.");
            else {
                rendement *= 0.6f;
                fatigue += 20f;
                s.recit.append("Tu te blesses à la main. Tu continues au ralenti, la paume bandée.");
            }
        } else if (s.evenement == CLIENT) {
            int jet = f.competences[Fiche.ELOQUENCE] / 3 + 4 * f.attributs[Fiche.PRESENCE] + hasard.nextInt(40);
            f.apprendre(Fiche.ELOQUENCE, 3f);
            if (jet >= 35) {
                gainEstime += 3f;
                s.recit.append("Un client furieux crie que la marchandise est avariée. Tu le calmes et il repart content.");
            } else {
                gainEstime -= 3f;
                s.recit.append("Un client furieux crie que la marchandise est avariée. Le ton monte, il part en jurant de ne plus revenir.");
            }
        } else if (s.evenement == ELOGE) {
            gainEstime += 3f;
            s.recit.append(M).append(" regarde ton travail et hoche la tête.");
        }
        EconomieVille e = j.marche().ville(j.villeIci(), j.jour());
        s.jours = part * rendement;
        s.fait = e.travailJoueur(a, s.jours);
        if (r == PATRON) a.presenceJusqua = Math.max(a.presenceJusqua, j.jour() + Entreprise.PRESENCE_JOURS);
        int sal = r == PATRON ? 0 : contrat ? salaire : salaire(JOURNALIER, c);
        // Le salaire s'accumule en fractions de cuivre ; on paie les pièces entières, le reste attend la suite.
        reliquat += sal * part;
        s.du = (int) Math.floor(reliquat) + (contrat ? arrieres : 0);
        reliquat -= (float) Math.floor(reliquat);
        s.paye = (int) Math.min(s.du, Math.max(0.0, Math.floor(a.caisse)));
        a.caisse -= s.paye;
        f.argent += s.paye;
        if (contrat) arrieres = s.du - s.paye;
        s.gagnes = f.apprendre(comp, heures * APPRENTISSAGE * (r == APPRENTI ? 1.5f : 1f));
        if (physique(lieu)) f.apprendre(Fiche.ATHLETISME, heures * 0.3f);
        fatigue = Math.min(100f, fatigue + heures * parHeure());
        j.destin().signaler(Destin.TRAVAIL, -1, heures);
        if (contrat) {
            heuresTravaillees += heures;
            if (aujourdhui != dernierJour || joursTravailles == 0) {
                joursTravailles++;
                joursRang++;
                j.renommee().marquer(Renommee.JOURS);
                j.renommee().guilde(lieu, 0.5f);
                j.renommee().gagner(ville, 0f, 0.3f, null);
            }
            dernierJour = aujourdhui;
            manquesComptes = 0;
            estime = Math.max(0f, Math.min(100f, estime + gainEstime));
            if (gainEstime > 0.5f) toucherLien(lieu, 0.01f);
        }
        return s;
    }

    /** Salaire gagné mais pas encore payé (moins d'un cuivre). */
    private float reliquat;

    /**
     * Faire passer le temps de travail d'un coup (« finir la journée ») : le résultat se calcule comme pour un PNJ,
     * avec un rendement un peu moindre (PASSE) qu'à la main.
     */
    Joueur.Scene travailler(int lieu, int r, float voulues) {
        EconomieVille.Agent a = j.agentIci(lieu);
        if (a == null) return accueil("Tu n'es plus à ton poste.");
        Fiche f = f();
        reposer();
        double h = j.heureIci();
        if (!Quotidien.ouvert(lieu, h)) return accueil("C'est fermé à cette heure-ci (on y travaille de " + Quotidien.ouverture(lieu) + " h à " + Quotidien.fermeture(lieu) + " h).");
        float heures = (float) Math.min(voulues, Quotidien.fermeture(lieu) - h);
        heures = Math.min(heures, heuresPossibles());
        if (heures < 0.5f) return accueil("Tu es trop fatigué" + f.e() + " pour travailler. Va dormir.");
        int comp = competence(lieu);
        Seance s = seance(a, lieu, r, heures, PASSE, f.id * 7919L + j.jourMaintenant() * 104729L + (long) (h * 10.0) + lieu);
        j.passer(heures);
        reposDepuis = j.maintenant();
        StringBuilder t = new StringBuilder(gestes(lieu));
        if (s.recit.length() > 0) t.append('\n').append(s.recit);
        if (!a.recettes.isEmpty() && s.fait < s.jours * 0.3f) t.append("\nIl n'y avait pas grand-chose à faire : tu as surtout rangé et nettoyé.");
        t.append("\n\nTu travailles ").append(heures >= 1.5f ? Math.round(heures) + " heures" : "une heure").append(". ");
        resume(t, s.paye, s.du, comp);
        if (r != JOURNALIER && r != PATRON) {
            String promo = promotion();
            if (promo != null) t.append("\n\n").append(promo);
        }
        if (s.evenement == VOL) return vol(lieu, t.toString(), a);
        j.enregistrer();
        return fin(t.toString());
    }

    private void resume(StringBuilder t, int paye, int du, int comp) {
        Fiche f = f();
        if (paye > 0) t.append("Tu reçois ").append(Fiche.argentTexte(paye)).append('.');
        if (paye < du) t.append(" La caisse est vide : on te doit ").append(Fiche.argentTexte(du - paye)).append('.');
        t.append(" ").append(Fiche.NOMS_COMPETENCES[comp]).append(" : ").append(f.competences[comp]).append('.');
        t.append(" Fatigue : ").append(Math.round(fatigue)).append(" %.");
    }

    // ---------------------------------------------------------------- Travail à la main (en action)

    /** Rendement quand on travaille soi-même : meilleur que quand on fait passer le temps. */
    public static final float A_LA_MAIN = 1f;
    /** Le temps de jeu est découpé en tranches de cette durée (heures) pendant le travail à la main. */
    public static final float TRANCHE = 0.25f;

    /** Une séance à la main est en cours : l'écran fait jouer les gestes et défiler le temps. */
    public volatile boolean mainActif;
    /** Pourquoi la séance doit s'arrêter (fatigue, fermeture, poste quitté) ; null tant qu'elle continue. */
    public volatile String mainArret;
    /** Le lieu où l'on travaille (pour les gestes) et la ligne d'état montrée dans le bandeau. */
    public volatile int mainLieu = -1;
    public volatile String mainTexte = "";
    private int mainRang, mainPaye, mainGagnesAvant, mainVol = -1;
    private float mainHeures;
    private final StringBuilder mainRecit = new StringBuilder();

    /** Choix « Travailler à la main » : vérifie qu'on peut, puis ferme la fenêtre ; l'écran prend la suite. */
    Joueur.Scene commencerAMain(int lieu, int r) {
        EconomieVille.Agent a = j.agentIci(lieu);
        if (a == null) return accueil("Tu n'es plus à ton poste.");
        Fiche f = f();
        reposer();
        double h = j.heureIci();
        if (!Quotidien.ouvert(lieu, h)) return accueil("C'est fermé à cette heure-ci (on y travaille de " + Quotidien.ouverture(lieu) + " h à " + Quotidien.fermeture(lieu) + " h).");
        if (heuresPossibles() < 0.5f) return accueil("Tu es trop fatigué" + f.e() + " pour travailler. Va dormir.");
        if (r == JOURNALIER && !a.source() && a.lieu != Biens.MARCHE && a.emploi < 0.3f) {
            String M = Character.toUpperCase(maitre(lieu).charAt(0)) + maitre(lieu).substring(1);
            return offre(lieu, M + " n'a besoin de personne aujourd'hui.");
        }
        mainLieu = lieu;
        mainRang = r;
        mainHeures = 0f;
        mainPaye = 0;
        mainVol = -1;
        mainGagnesAvant = f.competences[competence(lieu)];
        mainRecit.setLength(0);
        mainArret = null;
        mainActif = true;
        majTexte();
        j.annoncer("Au travail ! Bouge, ou appuie sur L, pour t'arrêter.");
        Joueur.Scene s = new Joueur.Scene("", "");
        s.fermer = true;
        return s;
    }

    private void majTexte() {
        Fiche f = f();
        int comp = competence(mainLieu);
        int hh = (int) mainHeures, mm = Math.round((mainHeures - hh) * 60f);
        mainTexte = "Au travail " + au(mainLieu) + " : " + hh + " h " + (mm < 10 ? "0" : "") + mm + " · " + Fiche.NOMS_COMPETENCES[comp] + " " + f.competences[comp]
                + " · fatigue " + Math.round(fatigue) + " % · gagné " + Fiche.argentTexte(mainPaye) + "  (bouge ou L pour t'arrêter)";
    }

    /** Une tranche de travail à la main, appelée par l'écran à mesure que le temps de jeu passe. */
    public void heureAMain(float heures) {
        if (!mainActif || mainArret != null || f() == null) return;
        EconomieVille.Agent a = j.agentIci(mainLieu);
        if (a == null) {
            mainArret = "Tu as quitté ton poste.";
            return;
        }
        double h = j.heureIci();
        if (!Quotidien.ouvert(mainLieu, h)) {
            mainArret = "C'est l'heure de fermer.";
            return;
        }
        Fiche f = f();
        Seance s = seance(a, mainLieu, mainRang, heures, A_LA_MAIN, f.id * 7919L + j.jourMaintenant() * 104729L + (long) (mainHeures * 100f) + mainLieu);
        reposDepuis = j.maintenant();
        mainHeures += heures;
        mainPaye += s.paye;
        if (s.recit.length() > 0) {
            mainRecit.append(mainRecit.length() > 0 ? "\n" : "").append(s.recit);
            j.annoncer(s.recit.toString());
        }
        if (s.evenement == VOL) {
            mainVol = mainLieu;
            mainArret = "En fin de journée, il se passe quelque chose…";
        } else if (fatigue >= FATIGUE_MAX) mainArret = "Tu n'en peux plus.";
        majTexte();
    }

    /** Fin de la séance à la main (l'écran l'appelle quand le joueur bouge, appuie sur L, ou quand mainArret est posé). */
    public Joueur.Scene finirAMain() {
        if (!mainActif) return accueil(null);
        mainActif = false;
        Fiche f = f();
        int comp = competence(mainLieu);
        StringBuilder t = new StringBuilder(mainArret != null ? mainArret + "\n\n" : "");
        int hh = (int) mainHeures, mm = Math.round((mainHeures - hh) * 60f);
        t.append("Tu as travaillé ").append(hh).append(" h ").append(mm < 10 ? "0" : "").append(mm).append(' ').append(au(mainLieu)).append(", de tes mains. ");
        if (mainRecit.length() > 0) t.append('\n').append(mainRecit).append('\n');
        if (mainPaye > 0) t.append("Tu as reçu ").append(Fiche.argentTexte(mainPaye)).append(". ");
        if (arrieres > 0 && mainRang != JOURNALIER && mainRang != PATRON) t.append("La caisse est vide : on te doit ").append(Fiche.argentTexte(arrieres)).append(". ");
        int gagnes = f.competences[comp] - mainGagnesAvant;
        if (gagnes > 0) t.append(Fiche.NOMS_COMPETENCES[comp]).append(" +").append(gagnes).append(" (").append(f.competences[comp]).append("). ");
        t.append("Fatigue : ").append(Math.round(fatigue)).append(" %.");
        if (mainRang != JOURNALIER && mainRang != PATRON && employe()) {
            String promo = promotion();
            if (promo != null) t.append("\n\n").append(promo);
        }
        int lieu = mainLieu;
        mainLieu = -1;
        mainTexte = "";
        if (mainVol >= 0) {
            EconomieVille.Agent a = j.agentIci(lieu);
            if (a != null) return vol(lieu, t.toString(), a);
        }
        j.enregistrer();
        return fin(t.toString());
    }

    private Joueur.Scene fin(String texte) {
        Joueur.Scene s = new Joueur.Scene("Fin du travail", texte);
        s.ajouter("Continuer", () -> accueil(null));
        return s;
    }

    /** Apprenti → compagnon : le maître le décide de lui-même quand tu as la main, l'ancienneté et son estime. */
    private String promotion() {
        Fiche f = f();
        int c = f.competences[competence(lieu)];
        if (rang != APPRENTI || c < COMPETENCE_COMPAGNON || joursRang < JOURS_APPRENTI || estime < 50f) return null;
        rang = COMPAGNON;
        joursRang = 0;
        salaire = salaire(COMPAGNON, c);
        toucherLien(lieu, 0.05f);
        j.renommee().gagner(ville, 3f, 3f, "Passé" + f.e() + " compagnon");
        String phrase = "En l'an " + p.annee + ", tu deviens " + rangNom(COMPAGNON) + " " + au(lieu) + " " + Creation.de(ville(ville)) + ".";
        f.recit.add(phrase);
        j.noter(phrase);
        String M = Character.toUpperCase(maitre(lieu).charAt(0)) + maitre(lieu).substring(1);
        return M + " pose la main sur ton épaule : « Tu n'es plus un" + (f.femme() ? "e" : "") + " " + rangNom(APPRENTI) + ". » Te voilà " + rangNom(COMPAGNON) + ", à "
                + Fiche.argentTexte(salaire) + " par jour.";
    }

    /** La caisse est restée ouverte. Prendre ou non ? Être pris, c'est le renvoi. */
    private Joueur.Scene vol(int lieu, String avant, EconomieVille.Agent a) {
        Fiche f = f();
        int somme = (int) Math.max(3, Math.min(40, Math.floor(a.caisse * 0.03)));
        Joueur.Scene s = new Joueur.Scene("La caisse", avant + "\n\nEn fin de journée, " + maitre(lieu) + " sort un moment. La caisse est restée ouverte : il y a là au moins "
                + Fiche.argentTexte(somme) + " que personne ne compterait tout de suite.");
        s.ajouter("Prendre les pièces", () -> {
            f.pencher(Fiche.HONNETETE, -5);
            Random hasard = new Random(melanger(f.id * 31L + j.jourMaintenant() * 17L + 5L));
            float pris = 0.35f - f.competences[Fiche.DISCRETION] / 400f - 0.02f * f.attributs[Fiche.RUSE];
            if (hasard.nextFloat() < Math.max(0.05f, pris)) {
                f.pencher(Fiche.LOI, -3);
                Relations.Lien l = lien(lieu);
                if (l != null) l.rancune = Math.min(1f, l.rancune + 0.2f);
                toucherLien(lieu, -0.3f);
                j.renommee().gagner(ville, 2f, -20f, null);
                j.renommee().marquer(Renommee.VOLEUR);
                String M = Character.toUpperCase(maitre(lieu).charAt(0)) + maitre(lieu).substring(1);
                j.noter("Tu as été pris" + f.e() + " la main dans la caisse " + au(lieu) + " " + Creation.de(ville(ville)) + ".");
                quitter();
                j.enregistrer();
                return fin(M + " revient plus tôt que prévu et te prend la main dans la caisse. Tu es chassé" + f.e() + " sur-le-champ, sans tes gages.");
            }
            a.caisse -= somme;
            f.argent += somme;
            j.enregistrer();
            return fin("Tu glisses " + Fiche.argentTexte(somme) + " dans ta bourse. Personne n'a rien vu… cette fois.");
        });
        s.ajouter("Ne pas y toucher", () -> {
            f.pencher(Fiche.HONNETETE, 3);
            estime = Math.min(100f, estime + 1f);
            j.enregistrer();
            return fin("Tu refermes la caisse et tu attends le retour de " + maitre(lieu) + ".");
        });
        return s;
    }

    // ---------------------------------------------------------------- Sauvegarde

    void ecrire(DataOutputStream s) throws IOException {
        s.writeInt(ville);
        s.writeInt(lieu);
        s.writeInt(rang);
        s.writeInt(salaire);
        s.writeInt(debut);
        s.writeInt(dernierJour);
        s.writeInt(manquesComptes);
        s.writeInt(arrieres);
        s.writeInt(essaiMaitrise);
        s.writeFloat(heuresTravaillees);
        s.writeInt(joursTravailles);
        s.writeInt(joursRang);
        s.writeFloat(estime);
        s.writeFloat(fatigue);
        s.writeDouble(reposDepuis);
        s.writeLong(maitrises);
        s.writeUTF(nouvelle);
    }

    void lire(DataInputStream s) throws IOException {
        ville = s.readInt();
        lieu = s.readInt();
        rang = s.readInt();
        salaire = s.readInt();
        debut = s.readInt();
        dernierJour = s.readInt();
        manquesComptes = s.readInt();
        arrieres = s.readInt();
        essaiMaitrise = s.readInt();
        heuresTravaillees = s.readFloat();
        joursTravailles = s.readInt();
        joursRang = s.readInt();
        estime = s.readFloat();
        fatigue = s.readFloat();
        reposDepuis = s.readDouble();
        maitrises = s.readLong();
        nouvelle = s.readUTF();
    }
}
