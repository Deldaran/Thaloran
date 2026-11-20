package com.deldaran.politics.histoire;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Phase 11 (lot 11b) : la famille.
 * - VIVRE ENSEMBLE : marié, on invite son conjoint chez soi (il faut une maison) ; les soirées ensemble remontent le moral ;
 * - les ENFANTS : ils naissent du couple (ou s'adoptent), entrent dans la population du monde, grandissent avec le calendrier ;
 *   le temps passé avec eux les instruit, et l'héritier en gardera quelque chose ;
 * - la VIEILLESSE : chaque nouvel an, le risque de mourir suit la table de mortalité du monde ; les ans pèsent sur la Vigueur ;
 * - la MORT (vieillesse, ou blessure mortelle au combat) et la SUCCESSION : on choisit un héritier parmi ses enfants
 *   (un mineur attend sa majorité : les années passent), ses frères et sœurs ; il hérite de l'argent, des terres, des ateliers,
 *   de la maison, de la moitié du renom ; on peut aussi PASSER LA MAIN de son vivant.
 */
public final class Famille {
    /** Une grossesse dure GROSSESSE jours (trois saisons) ; chance par jour, si l'on vit ensemble ou non ; au plus ENFANTS_MAX enfants. */
    public static final int GROSSESSE = 3 * Terre.JOURS_PAR_SAISON, ENFANTS_MAX = 6;
    public static final float CHANCE_ENSEMBLE = 0.03f, CHANCE_SEPARES = 0.008f, AMOUR_ENFANT = 0.6f;
    /** Une adoption prend ADOPTION jours ; l'enfant adopté a AGE_ADOPTE ans (âge humain). */
    public static final int ADOPTION = 7, AGE_ADOPTE = 5;
    /** Au combat, une blessure mortelle tue une fois sur MORT_AU_COMBAT ; les droits de succession prennent DROITS de l'argent. */
    public static final float MORT_AU_COMBAT = 0.5f, DROITS = 0.1f;
    /** Âge (humain) à partir duquel les ans pèsent sur la Vigueur. */
    public static final int VIEUX = 55;
    /** Lot I2 : l'apprentissage commence à AGE_APPRENTI ans (humains) ; le maître demande FRAIS_APPRENTI cuivre ; chaque jour est un effort APPRENTISSAGE. */
    public static final int AGE_APPRENTI = 12, FRAIS_APPRENTI = 30;
    public static final float APPRENTISSAGE = 4f;
    /** Lot I2 : chaque nouvel an, un enfant majeur et seul se marie avec cette chance ; l'écart d'âge (humain) accepté. */
    public static final float MARIAGE_AN = 0.35f;
    public static final int ECART_MARIAGE = 8;

    public static final class Enfant {
        public long id;
        public String nom = "";
        public int sexe, naissance;
        public float education;
        public boolean adopte;
    }

    private final Joueur j;
    private final Population p;
    public final List<Enfant> enfants = new ArrayList<>();
    /** Le conjoint vit chez toi. */
    public boolean ensemble;
    /** Jour où naîtra l'enfant attendu (-1 : personne n'attend d'enfant) ; vrai si c'est une adoption. */
    public int terme = -1;
    public boolean adoption;
    /** Mort en attente de succession (la cause), sinon null. */
    public String mort;
    private int dernierJour = -1, soiree = -1, enfantsJour = -1;
    /** Pour les essais : la prochaine chance réussit toujours. */
    public boolean forcer;

    Famille(Joueur j, Population p) {
        this.j = j;
        this.p = p;
    }

    /** « 1 an », « 12 ans », « nouveau-né ». */
    public static String ans(int n) {
        return n <= 0 ? "moins d'un an" : n == 1 ? "1 an" : n + " ans";
    }

    /** « de Thador », « d'Isidor ». */
    static String de(String nom) {
        return !nom.isEmpty() && "AEIOUYÉÈÊÂÎÔaeiouyéèêâîô".indexOf(nom.charAt(0)) >= 0 ? "d'" + nom : "de " + nom;
    }

    private String ville(int v) {
        return Noms.ville(p.villes.peuple[v], p.graine, v);
    }

    private float rythme(int peuple) {
        return p.reglages.peuples[peuple].rythme;
    }

    /** L'âge réel (en années de jeu) de quelqu'un né en l'an « naissance ». */
    public int age(int naissance) {
        return Terre.annee(p, j.jourMaintenant()) - naissance;
    }

    /** L'âge « humain » : un elfe de 240 ans en a 20. */
    public int ageHumain(int naissance, int peuple) {
        return (int) (age(naissance) * rythme(peuple));
    }

    /** L'âge réel de la majorité pour ce peuple. */
    public int majorite(int peuple) {
        return (int) Math.ceil(p.reglages.ageAdulte / rythme(peuple));
    }

    public boolean majeur(Enfant e) {
        return age(e.naissance) >= majorite(j.fiche().peuple);
    }

    // ================================================================ Chaque jour

    void avancer() {
        Fiche f = j.fiche();
        if (f == null || mort != null) return;
        int jour = j.jourMaintenant();
        if (dernierJour < 0) dernierJour = jour;
        for (int d = Math.max(dernierJour, jour - 60) + 1; d <= jour; d++) journee(d);
        if (jour != dernierJour) j.enregistrer();
        dernierJour = Math.max(dernierJour, jour);
    }

    private void journee(int d) {
        Fiche f = j.fiche();
        Social.Amour c = j.social().conjoint();
        // La vie commune.
        if (c != null && ensemble) j.destin().moral = Math.min(100f, j.destin().moral + 0.5f);
        // Un enfant s'annonce.
        if (terme < 0 && c != null && enfants.size() < ENFANTS_MAX && c.amour >= AMOUR_ENFANT && c.femme != f.femme() && fertile()) {
            Random h = new Random(Travail.melanger(f.id * 17L + d * 7919L));
            if (forcer || h.nextFloat() < (ensemble ? CHANCE_ENSEMBLE : CHANCE_SEPARES)) {
                forcer = false;
                terme = d + GROSSESSE;
                adoption = false;
                j.annoncer((f.femme() ? "Tu attends un enfant" : c.nom + " attend un enfant") + " : il naîtra vers le " + Terre.date(p, terme) + ".");
            }
        }
        if (terme >= 0 && d >= terme) naissance(d);
        apprendre();
        // Le nouvel an : la vieillesse.
        if (Math.floorMod(d, Terre.JOURS_PAR_AN) == 0) nouvelAn(d);
    }

    private boolean fertile() {
        Fiche f = j.fiche();
        int a = ageHumain(f.naissance, f.peuple);
        return a >= 16 && (!f.femme() || a <= 45) && a <= 60;
    }

    private void naissance(int d) {
        Fiche f = j.fiche();
        Social.Amour c = j.social().conjoint();
        Random h = new Random(Travail.melanger(f.id * 31L + d));
        Enfant e = new Enfant();
        e.sexe = h.nextInt(2);
        e.id = j.nouvelId();
        e.nom = Noms.personne(f.peuple, p.graine, e.id, e.sexe);
        e.adopte = adoption;
        e.naissance = Terre.annee(p, d) - (adoption ? (int) Math.ceil(AGE_ADOPTE / rythme(f.peuple)) : 0);
        Creation.Ajout a = new Creation.Ajout();
        a.id = e.id;
        if (f.femme()) a.mere = f.id;
        else a.pere = f.id;
        a.naissance = e.naissance;
        a.sexe = e.sexe;
        a.ville = j.corps().aUneMaison() ? j.corps().maisonVille : f.ville;
        j.inserer(a);
        f.lier(e.id, Fiche.ENFANT, e.sexe, 80, false);
        enfants.add(e);
        terme = -1;
        String qui = e.sexe == 1 ? "une fille" : "un fils";
        String phrase = adoption ? "En l'an " + Terre.annee(p, d) + ", tu adoptes " + qui + ", " + e.nom + ", " + (e.sexe == 1 ? "une orpheline" : "un orphelin") + " de " + AGE_ADOPTE + " ans."
                : "En l'an " + Terre.annee(p, d) + ", " + (c != null && !f.femme() ? c.nom + " te donne " : "tu donnes naissance à ") + qui + " : " + e.nom + ".";
        f.recit.add(phrase);
        j.noter(phrase);
        j.annoncer(adoption ? "L'enfant que tu adoptes est arrivé" + (e.sexe == 1 ? "e" : "") + " : " + e.nom + "." : "Naissance : " + qui + ", " + e.nom + " !");
        j.destin().moral = Math.min(100f, j.destin().moral + 20f);
        adoption = false;
        j.destin().verifierEtapes();
    }

    private void nouvelAn(int d) {
        Fiche f = j.fiche();
        for (Enfant e : enfants) marier(e, d, false);
        int a = ageHumain(f.naissance, f.peuple);
        if (a >= VIEUX && f.attributs[Fiche.VIGUEUR] > 1) {
            f.attributs[Fiche.VIGUEUR]--;
            j.annoncer("Le nouvel an : tu sens le poids des ans (Vigueur " + f.attributs[Fiche.VIGUEUR] + ").");
        }
        float[] table = p.reglages.mortalite;
        float risque = a < 40 ? 0f : table[Math.min(a, table.length - 1)];
        Random h = new Random(Travail.melanger(f.id * 101L + d));
        if (h.nextFloat() < risque) mourir("mort" + f.e() + " de vieillesse, dans son lit");
    }

    // ================================================================ Lot I2 : grandir

    /** L'âge humain d'un enfant. */
    int ageHumain(Enfant e) {
        return ageHumain(e.naissance, j.fiche().peuple);
    }

    /** Le lieu où l'enfant est en apprentissage, ou -1. */
    public int apprentissage(Enfant e) {
        Integer l = j.gens().lieuFixe(e.id);
        return l == null ? -1 : l;
    }

    private Foule fouleFamille() {
        Fiche f = j.fiche();
        return f == null ? null : p.foules[f.peuple][f.region];
    }

    /** Est-il marié ? (dans la Foule) */
    public boolean marie(Enfant e) {
        Foule fl = fouleFamille();
        int q = fl == null ? -1 : fl.placeDe(e.id);
        return q >= 0 && fl.conjointDe(q) >= 0;
    }

    /** Chaque jour : chaque apprenti apprend le métier de son maître ; sa fiche (gardée) en garde la trace. */
    private void apprendre() {
        Foule fl = fouleFamille();
        if (fl == null) return;
        for (Enfant e : enfants) {
            int q = fl.placeDe(e.id);
            if (q < 0 || !fl.estEnVie(q, e.id)) continue;
            Fiche x = j.gens().retenir(fl, q);
            int lieu = apprentissage(e);
            if (lieu >= 0 && ageHumain(e) >= AGE_APPRENTI) x.apprendre(Emplois.competence(lieu), APPRENTISSAGE);
            // L'éducation reçue à la maison devient la lecture.
            int lettres = Math.round(e.education * 120f * Math.min(1f, ageHumain(e) / 14f));
            if (x.competences[Fiche.LETTRES] < lettres) x.competences[Fiche.LETTRES] = Math.min(x.plafond(Fiche.LETTRES), lettres);
        }
    }

    /** Les métiers qu'on peut apprendre dans la ville : les ateliers de son économie, plus les champs. */
    private List<Integer> metiersIci(int ville) {
        List<Integer> l = new ArrayList<>();
        if (ville >= 0) for (EconomieVille.Agent a : j.marche().ville(ville, j.jour()).agents) if (!l.contains(a.lieu) && a.lieu >= 0) l.add(a.lieu);
        if (!l.contains(Biens.CHAMPS)) l.add(Biens.CHAMPS);
        return l;
    }

    private Joueur.Scene choisirMaitre(Enfant e) {
        Fiche f = j.fiche();
        int v = j.villeIci() >= 0 ? j.villeIci() : f.ville;
        Joueur.Scene s = new Joueur.Scene("L'apprentissage " + de(e.nom), "Chez qui placer " + e.nom + " ? Le maître demande " + Fiche.argentTexte(FRAIS_APPRENTI)
                + ". " + (e.sexe == 1 ? "Elle" : "Il") + " y apprendra le métier chaque jour, et y travaillera.");
        for (int lieu : metiersIci(v)) {
            if (lieu < 0) continue;
            String quoi = Emplois.phrase(lieu, 0L).replace("travaille ", "");
            s.ajouter("Placer " + e.nom + " : " + quoi, () -> placer(e, lieu));
        }
        s.ajouter("« Revenir", this::foyer);
        return s;
    }

    /** Place l'enfant chez un maître (le lieu fixé de sa fiche) : il y travaillera et y apprendra. */
    Joueur.Scene placer(Enfant e, int lieu) {
        Fiche f = j.fiche();
        if (f.argent < FRAIS_APPRENTI) {
            Joueur.Scene s = new Joueur.Scene("L'apprentissage " + de(e.nom), "Il te faut " + Fiche.argentTexte(FRAIS_APPRENTI) + " pour le maître.");
            s.ajouter("« Revenir", this::foyer);
            return s;
        }
        f.argent -= FRAIS_APPRENTI;
        j.gens().placer(e.id, lieu);
        String phrase = "En l'an " + Terre.annee(p, j.jourMaintenant()) + ", " + e.nom + " entre en apprentissage : " + Emplois.phrase(lieu, 0L).replace("travaille ", "") + ".";
        j.noter(phrase);
        f.recit.add(phrase);
        j.enregistrer();
        return foyer();
    }

    /**
     * Un enfant majeur et seul se marie (une chance par nouvel an, ou à coup sûr pour les essais) avec un habitant seul de sa ville,
     * d'un âge proche. Le couple s'installe dans sa propre maison ; le mariage est gardé par l'ajout de l'enfant.
     */
    String marier(Enfant e, int d, boolean sur) {
        Foule fl = fouleFamille();
        if (fl == null || !majeur(e) || marie(e)) return null;
        int q = fl.placeDe(e.id);
        if (q < 0 || !fl.estEnVie(q, e.id)) return null;
        Random h = new Random(Travail.melanger(e.id * 977L + d));
        if (!sur && h.nextFloat() >= MARIAGE_AN) return null;
        int ecart = (int) Math.ceil(ECART_MARIAGE / rythme(j.fiche().peuple));
        int ne = fl.naissanceDe(q);
        List<Integer> l = fl.gens(fl.villeDe(q), ne - ecart, ne + ecart, 1 - e.sexe, true);
        l.removeIf(c -> fl.idDe(c) == j.identifiant() || fl.idDe(c) == e.id);
        for (Enfant o : enfants) l.removeIf(c -> fl.idDe(c) == o.id);
        if (l.isEmpty()) return null;
        int c = l.get(h.nextInt(l.size()));
        fl.unir(q, c);
        Creation.Ajout a = j.ajout(e.id);
        if (a != null) a.conjoint = fl.idDe(c);
        String conj = Noms.personne(fl.indicePeuple, p.graine, fl.idDe(c), fl.sexeDe(c));
        String phrase = "En l'an " + Terre.annee(p, d) + ", " + (e.sexe == 1 ? "ta fille " : "ton fils ") + e.nom + " épouse " + conj + ". Ils s'installent dans leur maison.";
        j.noter(phrase);
        j.fiche().recit.add(phrase);
        j.annoncer("Noces : " + (e.sexe == 1 ? "ta fille " : "ton fils ") + e.nom + " épouse " + conj + " et quitte la maison.");
        return phrase;
    }

    // ================================================================ La mort

    /** Une blessure mortelle (au combat) : une fois sur deux, on n'en revient pas. */
    void blessureMortelle(String cause) {
        Fiche f = j.fiche();
        Random h = new Random(Travail.melanger(f.id * 13L + j.jourMaintenant()));
        if (forcer || h.nextFloat() < MORT_AU_COMBAT) {
            forcer = false;
            mourir(cause);
        } else j.annoncer("Tu as été laissé" + f.e() + " pour mort" + f.e() + ", mais tu respires encore.");
    }

    void mourir(String cause) {
        Fiche f = j.fiche();
        if (mort != null) return;
        mort = cause;
        String phrase = "En l'an " + Terre.annee(p, j.jourMaintenant()) + ", " + f.nom + " meurt, " + cause + ", à " + ans(age(f.naissance)) + ".";
        f.recit.add(phrase);
        j.noter(phrase);
        j.annoncer(f.nom + " est mort" + f.e() + " (" + cause + "). Ouvre « Mon personnage » pour choisir qui reprend le flambeau.");
        j.enregistrer();
    }

    /** Les héritiers possibles : les enfants vivants, puis les frères et sœurs vivants. { id, sexe, naissance, rôle (0 enfant, 1 frère ou sœur) }. */
    private List<long[]> heritiers() {
        Fiche f = j.fiche();
        List<long[]> l = new ArrayList<>();
        Foule fl = p.foules[f.peuple][f.region];
        for (Enfant e : enfants) if (fl != null && fl.placeDe(e.id) >= 0) l.add(new long[] { e.id, e.sexe, e.naissance, 0 });
        for (Fiche.Proche q : f.proches) {
            if (q.mort || (q.role != Fiche.FRERE && q.role != Fiche.SOEUR) || fl == null) continue;
            int place = fl.placeDe(q.id);
            if (place < 0) continue;
            Savoir.Temoin t = Savoir.temoin(p, fl, place);
            l.add(new long[] { q.id, q.sexe, t.naissance, 1 });
        }
        return l;
    }

    private String nomDe(long[] h) {
        for (Enfant e : enfants) if (e.id == h[0]) return e.nom;
        if (ancetres.containsKey(h[0])) return ancetres.get(h[0]);
        return Noms.personne(j.fiche().peuple, p.graine, h[0], (int) h[1]);
    }

    /** La scène de la succession (ouverte par « Mon personnage » quand on est mort, ou pour passer la main). */
    Joueur.Scene succession(boolean vivant) {
        Fiche f = j.fiche();
        List<long[]> l = heritiers();
        StringBuilder t = new StringBuilder(vivant ? "Tu te retires. Qui reprend tes affaires, tes terres et ton nom ?" : f.nom + " est mort" + f.e() + ", " + mort + ". Qui reprend le flambeau ?");
        t.append("\nL'héritier garde l'argent (moins ").append(Math.round(DROITS * 100f)).append(" % de droits au seigneur), la maison, les terres, les ateliers, et la moitié du renom.");
        Joueur.Scene s = new Joueur.Scene(vivant ? "Passer la main" : "La succession", t.toString());
        int maj = majorite(f.peuple);
        for (long[] h : l) {
            if (vivant && age((int) h[2]) < maj) continue;
            int a = age((int) h[2]);
            String role = h[3] == 0 ? (h[1] == 1 ? "ta fille" : "ton fils") : (h[1] == 1 ? "ta sœur" : "ton frère");
            String reg = a < maj ? " — régence : " + (maj - a) + " ans passeront jusqu'à sa majorité" : "";
            s.ajouter(nomDe(h) + ", " + role + " (" + ans(a) + ")" + reg, () -> heriter(h, a < maj ? maj - a : 0, vivant));
        }
        if (l.isEmpty() && !vivant) s.ajouter("Personne : ta lignée s'éteint. Créer un nouveau personnage", j::nouvelleCreation);
        if (vivant) s.ajouter("« Pas encore", () -> j.destin().scene());
        return s;
    }

    private Joueur.Scene heriter(long[] h, int annees, boolean vivant) {
        Fiche f = j.fiche();
        Enfant enfant = null;
        for (Enfant e : enfants) if (e.id == h[0]) enfant = e;
        Fiche n = new Fiche();
        n.id = h[0];
        n.sexe = (int) h[1];
        n.naissance = (int) h[2];
        n.nom = nomDe(h);
        n.peuple = f.peuple;
        n.region = f.region;
        n.ville = j.villeIci() >= 0 ? j.villeIci() : f.ville;
        n.apparence = Travail.melanger(n.id ^ 0x5EEDL);
        n.origine = f.origine;
        n.voie = f.voie;
        for (int k = 0; k < n.focus.length; k++) n.focus[k] = f.focus[k] / 2;
        // Ce que l'on tient de son parent : la moitié de ses dons au-dessus de la moyenne, un quart de son savoir, plus l'éducation reçue.
        float edu = enfant == null ? 0f : enfant.education;
        for (int k = 0; k < n.attributs.length; k++) n.attributs[k] = Math.max(Fiche.ATTRIBUT_DEPART, Math.round(Fiche.ATTRIBUT_DEPART + (f.attributs[k] - Fiche.ATTRIBUT_DEPART) * 0.5f));
        for (int k = 0; k < n.competences.length; k++) n.competences[k] = Math.round(f.competences[k] * (0.25f + 0.25f * Math.min(1f, edu)));
        for (int k = 0; k < n.axes.length; k++) n.axes[k] = f.axes[k] / 2;
        int droits = Math.round(f.argent * DROITS);
        n.argent = f.argent - droits;
        EconomieVille ev = j.villeIci() >= 0 ? j.marcheVille() : null;
        if (ev != null) ev.bourse += droits;
        n.sac = f.sac;
        n.objets = new ArrayList<>(f.objets);
        n.aspiration = f.aspiration;
        n.etape = 0;
        n.ennemi = f.ennemi;
        n.renom = f.renom / 2;
        // Lot I3 : l'héritier a vécu : ce qu'il a appris (apprentissage, lecture) et son argent à lui comptent.
        Fiche vecu = j.gens().importants.get(n.id);
        String appris = "";
        if (vecu != null) {
            for (int k = 0; k < n.competences.length; k++) n.competences[k] = Math.max(n.competences[k], vecu.competences[k]);
            for (int k = 0; k < n.attributs.length; k++) n.attributs[k] = Math.max(n.attributs[k], vecu.attributs[k]);
            for (int k = 0; k < n.focus.length; k++) n.focus[k] = Math.max(n.focus[k], vecu.focus[k]);
            for (int k = 0; k < n.axes.length; k++) n.axes[k] = (n.axes[k] + vecu.axes[k]) / 2;
            n.argent += vecu.argent;
            String m = Gens.meilleure(n);
            if (!m.isEmpty()) appris = " Ce que tu sais de ta vie à toi : " + m + ".";
        }
        n.recit = new ArrayList<>();
        // La famille, vue par l'héritier.
        boolean enfantDuMort = h[3] == 0;
        n.lier(f.id, enfantDuMort ? (f.femme() ? Fiche.MERE : Fiche.PERE) : (f.femme() ? Fiche.SOEUR : Fiche.FRERE), f.sexe, 90, !vivant);
        for (Enfant e : enfants) if (e.id != n.id) n.lier(e.id, enfantDuMort ? (e.sexe == 1 ? Fiche.SOEUR : Fiche.FRERE) : Fiche.AMI, e.sexe, 60, false);
        if (!enfantDuMort) for (Fiche.Proche q : f.proches) if (q.id != n.id && q.role != Fiche.ENFANT) n.proches.add(q.copie());
        // Lot G2 : les frères et sœurs du mort deviennent oncles et tantes, ses parents grands-parents.
        if (enfantDuMort) for (Fiche.Proche q : f.proches) {
            int role = q.role == Fiche.FRERE || q.role == Fiche.SOEUR ? Fiche.ONCLE : q.role == Fiche.PERE || q.role == Fiche.MERE ? Fiche.AIEUL : -1;
            if (role < 0 || q.id == n.id) continue;
            Fiche.Proche x = n.lier(q.id, role, q.sexe, Math.min(100, q.estime / 2 + 30), q.mort);
            x.note = q.note;
        }
        String lien = enfantDuMort ? (n.sexe == 1 ? "fille" : "fils") : (n.sexe == 1 ? "sœur" : "frère");
        String phrase = "En l'an " + Terre.annee(p, j.jourMaintenant() + annees * Terre.JOURS_PAR_AN) + ", " + n.nom + ", " + lien + " " + de(f.nom) + ", reprend " + (vivant ? "ses affaires." : "le flambeau.");
        n.recit.add(phrase);
        List<Enfant> freres = new ArrayList<>(enfants);
        j.succeder(n, annees * Terre.JOURS_PAR_AN, phrase);
        // Les frères et sœurs de l'héritier ne sont plus « tes enfants ».
        Famille nf = j.lignee();
        if (enfantDuMort) for (Enfant e : freres) if (e.id != n.id) nf.freres.add(e);
        nf.ancetres.putAll(ancetres);
        nf.ancetres.put(f.id, f.nom);
        Joueur.Scene s = new Joueur.Scene(n.nom, (annees > 0 ? annees + " ans ont passé. " : "") + "Tu es désormais " + n.nom + ", " + ans(age(n.naissance)) + ", " + lien + " " + de(f.nom) + ". "
                + "Tu hérites de " + Fiche.argentTexte(n.argent) + (droits > 0 ? " (le seigneur a pris " + Fiche.argentTexte(droits) + ")" : "") + ", de la maison, des terres et des ateliers. On connaît ton nom de famille." + appris);
        s.ajouter("Continuer", j::fermerScene);
        return s;
    }

    /** Les frères et sœurs de l'héritier (enfants du défunt), gardés pour mémoire. */
    public final List<Enfant> freres = new ArrayList<>();
    /** Les noms choisis des ancêtres joués (l'identifiant ne donne pas le nom choisi à la création). */
    public final java.util.Map<Long, String> ancetres = new java.util.HashMap<>();

    /** Le nom d'un proche : celui d'un ancêtre joué, sinon le nom du monde. */
    public String nom(long id, String sinon) {
        return ancetres.getOrDefault(id, sinon);
    }

    // ================================================================ Scènes

    /** Avec son conjoint (X) : vivre ensemble, passer la soirée, adopter. */
    void choix(Joueur.Scene s, Bande.Rencontre r) {
        Social.Amour c = j.social().conjoint();
        Fiche f = j.fiche();
        if (c == null || c.graine != r.graine || f == null) return;
        if (!ensemble && j.corps().aUneMaison()) s.ajouter("« Viens vivre chez moi. »", () -> {
            ensemble = true;
            j.noter("En l'an " + Terre.annee(p, j.jourMaintenant()) + ", " + c.nom + " vient vivre avec toi.");
            j.enregistrer();
            return reponse(r, "« Enfin sous le même toit. » " + c.nom + " fait porter ses affaires chez toi.");
        });
        if (soiree != j.jourMaintenant()) s.ajouter("Passer la soirée ensemble", () -> {
            soiree = j.jourMaintenant();
            c.amour = Math.min(1f, c.amour + 0.05f);
            c.vu = j.jourMaintenant();
            j.destin().moral = Math.min(100f, j.destin().moral + 6f);
            j.passer(3.0);
            j.enregistrer();
            return reponse(r, "Vous dînez, vous parlez de tout et de rien. La soirée passe trop vite.");
        });
        if (c.femme == f.femme() && terme < 0 && enfants.size() < ENFANTS_MAX) s.ajouter("« Et si nous adoptions un enfant ? »", () -> {
            terme = j.jourMaintenant() + ADOPTION;
            adoption = true;
            j.enregistrer();
            return reponse(r, "« Il y a des orphelins au temple… Oui. Allons-y. » L'enfant arrivera d'ici " + ADOPTION + " jours.");
        });
    }

    private Joueur.Scene reponse(Bande.Rencontre r, String texte) {
        Joueur.Scene s = new Joueur.Scene(r.nom, texte);
        s.ajouter("Continuer", j::fermerScene);
        return s;
    }

    /** Chez soi (M dans sa maison) : la famille. */
    void choixMaison(Joueur.Scene s) {
        if (enfants.isEmpty() && j.social().conjoint() == null) return;
        s.ajouter("Ta famille…", this::foyer);
    }

    private Joueur.Scene foyer() {
        Social.Amour c = j.social().conjoint();
        StringBuilder t = new StringBuilder();
        if (c != null) t.append(c.nom).append(ensemble ? " vit avec toi." : " vit toujours de son côté, à " + ville(c.ville) + ".");
        for (Enfant e : enfants) {
            t.append("\n").append(e.nom).append(", ").append(e.sexe == 1 ? "ta fille" : "ton fils").append(e.adopte ? " (adopté" + (e.sexe == 1 ? "e" : "") + ")" : "").append(", ").append(ans(age(e.naissance))).append(majeur(e) ? ", majeur" + (e.sexe == 1 ? "e" : "") : "").append(e.education >= 0.5f ? ", bien instruit" + (e.sexe == 1 ? "e" : "") : "");
            // Lot I2 : son métier, ce qu'il en sait, s'il est marié.
            int lieu = apprentissage(e);
            Fiche x = j.gens().importants.get(e.id);
            if (lieu >= 0) t.append(", en apprentissage : ").append(Emplois.phrase(lieu, 0L).replace("travaille ", "")).append(x == null ? "" : " (" + Fiche.NOMS_COMPETENCES[Emplois.competence(lieu)] + " " + x.competences[Emplois.competence(lieu)] + ")");
            if (marie(e)) t.append(", marié").append(e.sexe == 1 ? "e" : "").append(", vit dans sa maison");
        }
        if (terme >= 0) t.append("\n").append(adoption ? "L'enfant adopté arrivera vers le " : "Un enfant est attendu vers le ").append(Terre.date(p, terme)).append('.');
        Joueur.Scene s = new Joueur.Scene("Ta famille", t.toString().trim());
        if (!enfants.isEmpty() && enfantsJour != j.jourMaintenant()) s.ajouter("Passer du temps avec tes enfants (2 h)", () -> {
            enfantsJour = j.jourMaintenant();
            for (Enfant e : enfants) e.education = Math.min(1f, e.education + 0.05f + j.fiche().competences[Fiche.LETTRES] / 3000f);
            j.destin().moral = Math.min(100f, j.destin().moral + 4f);
            j.passer(2.0);
            j.enregistrer();
            return foyer();
        });
        for (Enfant e : enfants) if (apprentissage(e) < 0 && !marie(e) && ageHumain(e) >= AGE_APPRENTI) s.ajouter("Placer " + e.nom + " en apprentissage…", () -> choisirMaitre(e));
        s.ajouter("« Revenir", () -> j.corps().coffre(null));
        return s;
    }

    /** Dans « Mon destin » : passer la main de son vivant, s'il y a un héritier majeur. */
    void choixDestin(Joueur.Scene s) {
        Fiche f = j.fiche();
        int maj = majorite(f.peuple);
        boolean majeur = false;
        for (long[] h : heritiers()) majeur |= age((int) h[2]) >= maj;
        if (majeur) s.ajouter("Passer la main à un héritier…", () -> succession(true));
    }

    /** Pour les essais. */
    public Joueur.Scene choixPourEssai(Bande.Rencontre r) {
        Joueur.Scene s = new Joueur.Scene(r.nom, "");
        choix(s, r);
        return s;
    }

    public Joueur.Scene successionPourEssai(boolean vivant) {
        return succession(vivant);
    }

    public void blessurePourEssai(String cause) {
        blessureMortelle(cause);
    }

    public Joueur.Scene foyerPourEssai() {
        return foyer();
    }

    /** Essai du lot I : un enfant de cet âge (humain), né dans la ville de la maison. */
    public Enfant enfantPourEssai(int ageHumain, int sexe) {
        Fiche f = j.fiche();
        Enfant e = new Enfant();
        e.sexe = sexe;
        e.id = j.nouvelId();
        e.nom = Noms.personne(f.peuple, p.graine, e.id, e.sexe);
        e.naissance = Terre.annee(p, j.jourMaintenant()) - (int) Math.ceil(ageHumain / rythme(f.peuple));
        e.education = 0.6f;
        Creation.Ajout a = new Creation.Ajout();
        a.id = e.id;
        if (f.femme()) a.mere = f.id;
        else a.pere = f.id;
        a.naissance = e.naissance;
        a.sexe = e.sexe;
        a.ville = j.corps().aUneMaison() ? j.corps().maisonVille : f.ville;
        j.inserer(a);
        f.lier(e.id, Fiche.ENFANT, e.sexe, 80, false);
        enfants.add(e);
        return e;
    }

    public Joueur.Scene placerPourEssai(Enfant e, int lieu) {
        return placer(e, lieu);
    }

    public String marierPourEssai(Enfant e) {
        return marier(e, j.jourMaintenant(), true);
    }

    public void joursPourEssai(int n) {
        for (int k = 0; k < n; k++) apprendre();
    }

    public Joueur.Scene choisirMaitrePourEssai(Enfant e) {
        return choisirMaitre(e);
    }

    // ================================================================ Sauvegarde

    void ecrire(DataOutputStream s) throws IOException {
        s.writeBoolean(ensemble);
        s.writeInt(terme);
        s.writeBoolean(adoption);
        s.writeUTF(mort == null ? "" : mort);
        s.writeInt(dernierJour);
        s.writeInt(soiree);
        s.writeInt(enfantsJour);
        s.writeInt(ancetres.size());
        for (java.util.Map.Entry<Long, String> e : ancetres.entrySet()) {
            s.writeLong(e.getKey());
            s.writeUTF(e.getValue());
        }
        for (List<Enfant> l : List.of(enfants, freres)) {
            s.writeInt(l.size());
            for (Enfant e : l) {
                s.writeLong(e.id);
                s.writeUTF(e.nom);
                s.writeInt(e.sexe);
                s.writeInt(e.naissance);
                s.writeFloat(e.education);
                s.writeBoolean(e.adopte);
            }
        }
    }

    void lire(DataInputStream s) throws IOException {
        ensemble = s.readBoolean();
        terme = s.readInt();
        adoption = s.readBoolean();
        String m = s.readUTF();
        mort = m.isEmpty() ? null : m;
        dernierJour = s.readInt();
        soiree = s.readInt();
        enfantsJour = s.readInt();
        int na = s.readInt();
        for (int k = 0; k < na; k++) ancetres.put(s.readLong(), s.readUTF());
        for (List<Enfant> l : List.of(enfants, freres)) {
            int n = s.readInt();
            for (int k = 0; k < n; k++) {
                Enfant e = new Enfant();
                e.id = s.readLong();
                e.nom = s.readUTF();
                e.sexe = s.readInt();
                e.naissance = s.readInt();
                e.education = s.readFloat();
                e.adopte = s.readBoolean();
                l.add(e);
            }
        }
    }
}
