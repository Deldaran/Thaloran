package com.deldaran.politics.histoire;

import com.deldaran.politics.Metiers;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Phase 6 : le panneau « Mon destin ».
 *
 * - Les ÉTAPES de l'aspiration sont vérifiées d'après ce qui se passe vraiment dans le jeu (renom, réputation, argent, métier,
 *   lecture, voyages…). Une étape atteinte donne un point de focus dans la compétence de l'aspiration et du renom.
 *   Celles qui dépendent de systèmes à venir (bande, fiefs, mariage…) sont marquées comme telles.
 * - Les ENVIES du jour, courtes et tirées du contexte (boire une bière à la taverne, s'offrir une hache…) :
 *   les accomplir donne du MORAL ; les laisser passer en ôte un peu. Le moral joue sur le rendement au travail.
 */
public final class Destin {
    public static final int TRAVAIL = 0, BIERE = 1, OBJET = 2, MISSION = 3, LIRE = 4, VOIR = 5, PROCHE = 6;
    /** Envies par jour ; moral gagné par envie accomplie, perdu par envie manquée. */
    public static final int ENVIES_PAR_JOUR = 2;
    public static final float MORAL_ENVIE = 8f, MORAL_MANQUEE = 3f;
    /** Heures de travail dans la journée pour l'envie « une bonne journée de travail ». */
    public static final float HEURES_BONNE_JOURNEE = 6f;
    /** Renom donné par une étape franchie. */
    public static final float RENOM_ETAPE = 5f;
    /** « Réunir 1 000 pièces » : en cuivre (1 or). */
    public static final int FORTUNE = 10000;

    public static final class Envie {
        public int type, param = -1, jour;
        public boolean faite;
        public String texte = "";
    }

    private final Joueur j;
    private final Population p;
    public float moral = 50f;
    public final List<Envie> envies = new ArrayList<>();
    private int jourEnvies = -1;
    private float heuresDuJour;
    private int jourHeures = -1;
    /** Livres lus (index de Livres) et régions traversées. */
    final Set<Integer> livres = new HashSet<>(), regions = new HashSet<>();

    Destin(Joueur j, Population p) {
        this.j = j;
        this.p = p;
    }

    /** Le moral joue sur le rendement au travail : × 0,85 au plus bas, × 1,15 au plus haut. */
    public float facteurMoral() {
        return 0.85f + 0.3f * moral / 100f;
    }

    static String humeur(float moral) {
        if (moral >= 80f) return "radieux";
        if (moral >= 60f) return "de bonne humeur";
        if (moral >= 40f) return "d'humeur égale";
        if (moral >= 20f) return "morose";
        return "abattu";
    }

    // ---------------------------------------------------------------- Étapes

    /** L'étape est-elle mesurable aujourd'hui ? (Sinon elle attend un système d'une phase suivante.) */
    static boolean mesurable(String etape) {
        return condition(etape) >= 0;
    }

    private static int condition(String e) {
        if (e.contains("renom 50")) return 0;
        if (e.startsWith("Gagner l'estime de la ville")) return 1;
        if (e.startsWith("Réunir 1 000 pièces")) return 2;
        if (e.startsWith("Devenir compagnon")) return 3;
        if (e.startsWith("Réaliser un chef-d'œuvre")) return 4;
        if (e.startsWith("Avoir un lopin")) return 5;
        if (e.startsWith("Avoir des bêtes")) return 6;
        if (e.startsWith("Avoir des chevaux")) return 7;
        if (e.startsWith("Savoir lire")) return 8;
        if (e.startsWith("Lire vingt livres")) return 9;
        if (e.startsWith("Visiter dix régions")) return 10;
        if (e.startsWith("Parcourir cinq régions")) return 11;
        if (e.startsWith("Être connu dans trois royaumes")) return 12;
        if (e.startsWith("Ouvrir un premier atelier")) return 13;
        if (e.startsWith("Employer cinq personnes")) return 14;
        if (e.startsWith("Tenir trois ateliers")) return 15;
        if (e.startsWith("Tenir une chaîne complète")) return 16;
        if (e.startsWith("Avoir des ateliers dans trois villes")) return 17;
        if (e.startsWith("Se marier") || e.startsWith("L'épouser")) return 18;
        if (e.startsWith("Rencontrer quelqu'un")) return 19;
        if (e.startsWith("Lui faire la cour")) return 20;
        if (e.startsWith("Faire une première récolte")) return 21;
        if (e.startsWith("Tenir une ferme avec des ouvriers")) return 22;
        if (e.startsWith("Tenir un domaine de dix champs")) return 23;
        if (e.startsWith("Avoir trois enfants")) return 24;
        if (e.startsWith("Voir naître la troisième génération")) return 25;
        if (e.startsWith("Vieillir ensemble")) return 26;
        if (e.startsWith("Entrer au service d'un seigneur") || e.startsWith("Servir un seigneur") || e.startsWith("Devenir écuyer")) return 27;
        if (e.startsWith("Être adoubé, acheter un titre")) return 28;
        if (e.equals("Être adoubé")) return 29;
        if (e.startsWith("Recevoir un premier fief") || e.startsWith("Obtenir un fief")) return 30;
        if (e.startsWith("Gagner un tournoi")) return 31;
        if (e.startsWith("Défendre un fief")) return 32;
        if (e.startsWith("Remplir trois contrats pour des seigneurs")) return 33;
        if (e.startsWith("Qu'un enfant devienne noble ou maître")) return 34;
        if (e.startsWith("Réunir une bande de 20")) return 35;
        if (e.startsWith("Réunir une compagnie de 30")) return 36;
        if (e.startsWith("Compagnie de 100")) return 37;
        if (e.equals("Gagner une bataille")) return 38;
        if (e.startsWith("Gagner cinq batailles")) return 39;
        if (e.startsWith("Gagner vingt batailles")) return 40;
        if (e.startsWith("Vaincre une armée deux fois plus grande")) return 41;
        if (e.startsWith("Rallier trois vassaux")) return 42;
        if (e.startsWith("Réclamer le trône")) return 43;
        if (e.startsWith("Être couronné") || e.equals("Devenir roi ou reine")) return 44;
        if (e.startsWith("Prendre une capitale de ton peuple")) return 45;
        if (e.startsWith("Tenir toutes les capitales de ton peuple")) return 46;
        if (e.startsWith("Apprendre dix chansons")) return 47;
        if (e.startsWith("Jouer dans cinq tavernes")) return 48;
        if (e.startsWith("Composer une chanson qui te survivra")) return 49;
        if (e.startsWith("Qu'une chanson soit écrite sur toi")) return 50;
        return -1;
    }

    /** Où l'on en est pour une étape mesurable, en texte (« 23 sur 50 »). */
    private String progres(String e) {
        Fiche f = j.fiche();
        Renommee r = j.renommee();
        switch (condition(e)) {
            case 0: return Math.round(r.meilleurRenom()) + " sur 50";
            case 1: return Math.round(r.meilleureReputation()) + " sur 50";
            case 2: return Fiche.argentTexte(f.argent) + " sur " + Fiche.argentTexte(FORTUNE);
            case 9: return livres.size() + " sur 20";
            case 10: return regions.size() + " sur 10";
            case 11: return regions.size() + " sur 5";
            case 12: return r.royaumesOuConnu() + " sur 3";
            case 14: return j.entreprise().employes() + " sur 5";
            case 15: return j.entreprise().nombre() + " sur 3";
            case 17: return j.entreprise().villes() + " sur 3";
            case 22: return j.terre().journaliers() + " sur 2";
            case 23: return j.terre().lopins() + " sur 10";
            case 24: return j.lignee().enfants.size() + " sur 3";
            case 33: return (j.noblesse().reussies + j.armee().contrats) + " sur 3";
            case 35: return j.armee().effectif() + " sur 20";
            case 36: return j.armee().effectif() + " sur 30";
            case 37: return j.armee().effectif() + " sur 100";
            case 39: return j.armee().victoires + " sur 5";
            case 40: return j.armee().victoires + " sur 20";
            case 42: return j.armee().vassaux + " sur 3";
            case 46: return j.armee().capitalesDuPeuple()[0] + " sur " + j.armee().capitalesDuPeuple()[1];
            case 47: return j.chansons().connues() + " sur 10";
            case 48: return j.chansons().tavernes.size() + " sur 5";
            case 49: return j.chansons().meilleureDeToi() + " villes sur " + Chansons.VILLES_SURVIE;
            default: return "";
        }
    }

    private boolean atteinte(String e) {
        Fiche f = j.fiche();
        Renommee r = j.renommee();
        Travail t = j.travail();
        switch (condition(e)) {
            case 0: return r.meilleurRenom() >= 50f;
            case 1: return r.meilleureReputation() >= 50f;
            case 2: return f.argent >= FORTUNE;
            case 3: return t.rang >= Travail.COMPAGNON || t.maitrises != 0L;
            case 4: return t.maitrises != 0L;
            case 5: for (String o : f.objets) if (o.contains("lopin") || o.contains("champ")) return true; return false;
            case 6: if (j.terre().betes() > 0) return true; for (Biens.Bien b : Biens.TOUS) if (b.place == Biens.BETE && b != Biens.CHEVAL && f.sac.nombre(b.id) > 0) return true; return false;
            case 7: return f.sac.nombre(Biens.CHEVAL.id) > 0;
            case 8: return f.competences[Fiche.LETTRES] >= Fiche.LECTURE;
            case 9: return livres.size() >= 20;
            case 10: return regions.size() >= 10;
            case 11: return regions.size() >= 5;
            case 12: return r.royaumesOuConnu() >= 3;
            case 13: return j.entreprise().nombre() >= 1;
            case 14: return j.entreprise().employes() >= 5;
            case 15: return j.entreprise().nombre() >= 3;
            case 16: return j.entreprise().chaine();
            case 17: return j.entreprise().villes() >= 3;
            case 18: return j.social().marie();
            case 19: return j.social().meilleurAmour() >= 0.2f;
            case 20: return j.social().meilleurStatut() >= Social.AMOUREUX;
            case 21: return j.terre().recoltes > 0;
            case 22: return j.terre().journaliers() >= 2;
            case 23: return j.terre().lopins() >= 10;
            case 24: return j.lignee().enfants.size() >= 3;
            case 27: return j.noblesse().servi || j.armee().forts > 0;
            case 28: return j.noblesse().noble;
            case 29: return j.noblesse().noble && j.noblesse().voie == 0;
            case 30: return !j.noblesse().fiefs.isEmpty();
            case 31: return j.noblesse().tournois > 0;
            case 32: return j.noblesse().defenses > 0;
            case 33: return j.noblesse().reussies + j.armee().contrats >= 3;
            case 35: return j.armee().effectif() >= 20;
            case 36: return j.armee().effectif() >= 30;
            case 37: return j.armee().effectif() >= 100;
            case 38: return j.armee().victoires >= 1;
            case 39: return j.armee().victoires >= 5;
            case 40: return j.armee().victoires >= 20;
            case 41: return j.armee().grandeVictoire;
            case 42: return j.armee().vassaux >= 3;
            case 43: return j.armee().reclame;
            case 44: return !j.armee().couronnes.isEmpty();
            case 45: return j.armee().capitalesPrises > 0;
            case 46: { int[] c = j.armee().capitalesDuPeuple(); return c[1] > 0 && c[0] >= c[1]; }
            case 47: return j.chansons().connues() >= 10;
            case 48: return j.chansons().tavernes.size() >= 5;
            case 49: return j.chansons().meilleureDeToi() >= Chansons.VILLES_SURVIE;
            case 50: return j.chansons().surToi();
            case 34: return !j.lignee().ancetres.isEmpty() && (j.noblesse().noble || j.travail().maitrises != 0L);
            case 25: return !j.lignee().ancetres.isEmpty() && !j.lignee().enfants.isEmpty();
            case 26: return j.social().conjoint() != null && j.lignee().ageHumain(j.fiche().naissance, j.fiche().peuple) >= 60;
            default: return false;
        }
    }

    /** Franchit les étapes atteintes, dans l'ordre ; s'arrête à la première qui ne l'est pas. */
    void verifierEtapes() {
        Fiche f = j.fiche();
        if (f == null || f.aspiration < 0) return;
        Aspirations.Aspiration a = Aspirations.TOUTES.get(f.aspiration);
        while (f.etape < a.etapes.length && atteinte(a.etapes[f.etape])) {
            String e = a.etapes[f.etape];
            f.etape++;
            f.concentrer(a.competence);
            int v = j.villeIci() >= 0 ? j.villeIci() : f.ville;
            j.renommee().gagner(v, RENOM_ETAPE, 2f, null);
            String phrase = "En l'an " + p.annee + ", tu franchis une étape de ton destin : " + Character.toLowerCase(e.charAt(0)) + e.substring(1) + ".";
            f.recit.add(phrase);
            j.noter(phrase);
            j.annoncer("Destin : « " + e + " » — fait ! (focus en " + Fiche.NOMS_COMPETENCES[a.competence] + ")");
            if (f.etape == a.etapes.length) j.social().grandMoment("ton aspiration accomplie");
        }
    }

    // ---------------------------------------------------------------- Envies

    /** Change de jour : les envies d'hier non faites ôtent du moral, et de nouvelles naissent du contexte. */
    void actualiser() {
        Fiche f = j.fiche();
        if (f == null) return;
        j.renommee().avancer(j.jourMaintenant());
        verifierEtapes();
        int jour = j.jourMaintenant();
        if (jour == jourEnvies) return;
        for (Envie e : envies) if (!e.faite) moral = Math.max(0f, moral - MORAL_MANQUEE);
        envies.clear();
        jourEnvies = jour;
        Random hasard = new Random(Travail.melanger(f.id * 13L + jour * 7919L));
        List<Envie> possibles = new ArrayList<>();
        int v = j.villeIci() >= 0 ? j.villeIci() : f.ville;
        EconomieVille e = v >= 0 ? j.marche().ville(v, j.jour()) : null;
        possibles.add(envie(TRAVAIL, -1, "Faire une bonne journée de travail (" + Math.round(HEURES_BONNE_JOURNEE) + " h)"));
        if (e != null && (e.agent(Metiers.TAVERNE) != null || e.agent(Metiers.AUBERGE) != null)) possibles.add(envie(BIERE, -1, "Boire une bière à la taverne ou à l'auberge"));
        possibles.add(envie(MISSION, -1, "Rendre service : réussir une mission"));
        if (f.competences[Fiche.LETTRES] >= Fiche.LECTURE) possibles.add(envie(LIRE, -1, "Lire un livre"));
        if (e != null) {
            // S'offrir un objet qu'on n'a pas, vendu en ville, et qu'on peut presque se payer.
            List<Integer> objets = new ArrayList<>();
            for (EconomieVille.Agent a : e.agents) {
                for (int b = 0; b < a.vend.length; b++) {
                    Biens.Bien x = Biens.get(b);
                    if (!a.vend[b] || x.place < 0 || x.place == Biens.BETE || f.sac.nombre(b) > 0 || objets.contains(b)) continue;
                    if (a.prix[b] < 5f || a.prix[b] > Math.max(40f, f.argent * 0.6f)) continue;
                    objets.add(b);
                }
            }
            if (!objets.isEmpty()) {
                int b = objets.get(hasard.nextInt(objets.size()));
                possibles.add(envie(OBJET, b, "T'offrir " + Biens.get(b).un()));
            }
            List<Integer> lieux = new ArrayList<>();
            for (EconomieVille.Agent a : e.agents) if (a.lieu < Metiers.NOMBRE && a.lieu != Biens.MARCHE) lieux.add(a.lieu);
            if (!lieux.isEmpty()) {
                int l = lieux.get(hasard.nextInt(lieux.size()));
                possibles.add(envie(VOIR, l, "Aller voir ce qui se fait " + Missions.a(Missions.role(l)).replace("au ", "chez le ").replace("à l'", "chez l'")));
            }
        }
        // Lot F : une envie tournée vers quelqu'un qu'on aime bien (un parent, un ami, un amour).
        Envie proche = j.social().envieProche(hasard);
        if (proche != null) possibles.add(proche);
        for (int k = 0; k < ENVIES_PAR_JOUR && !possibles.isEmpty(); k++) {
            Envie x = possibles.remove(hasard.nextInt(possibles.size()));
            x.jour = jour;
            envies.add(x);
        }
    }

    private static Envie envie(int type, int param, String texte) {
        Envie e = new Envie();
        e.type = type;
        e.param = param;
        e.texte = texte;
        return e;
    }

    /** Un acte du joueur : accomplit-il une envie du jour ? */
    void signaler(int type, int param, float quantite) {
        if (j.fiche() == null) return;
        if (type == TRAVAIL) {
            int jour = j.jourMaintenant();
            if (jour != jourHeures) {
                jourHeures = jour;
                heuresDuJour = 0f;
            }
            heuresDuJour += quantite;
            if (heuresDuJour < HEURES_BONNE_JOURNEE) return;
        }
        if (type == LIRE && param >= 0) livres.add(param);
        for (Envie e : envies) {
            if (e.faite || e.type != type) continue;
            if (e.type == OBJET && e.param != param) continue;
            if (e.type == VOIR && e.param != param) continue;
            e.faite = true;
            moral = Math.min(100f, moral + MORAL_ENVIE);
            j.annoncer("Envie accomplie : " + Character.toLowerCase(e.texte.charAt(0)) + e.texte.substring(1) + " (moral " + Math.round(moral) + ")");
        }
        verifierEtapes();
    }

    // ---------------------------------------------------------------- Scène

    Joueur.Scene scene() {
        Fiche f = j.fiche();
        actualiser();
        Renommee r = j.renommee();
        StringBuilder t = new StringBuilder();
        if (f.aspiration >= 0) {
            Aspirations.Aspiration a = Aspirations.TOUTES.get(f.aspiration);
            t.append(a.nom).append(" — ").append(a.texte).append('\n');
            for (int k = 0; k < a.etapes.length; k++) {
                String e = a.etapes[k];
                t.append('\n').append(k == f.etape ? "  » " : "     ").append(k + 1).append(". ").append(e).append(k < f.etape ? " (fait)" : "");
                if (k == f.etape) {
                    if (!mesurable(e)) t.append(" (pas encore possible dans le jeu)");
                    else {
                        String pr = progres(e);
                        if (!pr.isEmpty()) t.append(" (").append(pr).append(')');
                    }
                }
            }
            t.append("\nChaque étape franchie : un point de focus en ").append(Fiche.NOMS_COMPETENCES[a.competence]).append(" et du renom.");
        } else t.append("Tu n'as pas encore d'aspiration.");
        t.append("\n\nEnvies du jour :");
        for (Envie e : envies) t.append("\n  • ").append(e.texte).append(e.faite ? " (fait)" : "");
        t.append("\nTu es ").append(humeur(moral)).append(" (moral ").append(Math.round(moral)).append(").");
        Joueur.Scene s = new Joueur.Scene("Mon destin", t.toString());
        j.social().choixDestin(s);
        j.lignee().choixDestin(s);
        s.ajouter("Ce qu'on dit de moi", this::renom);
        s.ajouter("« Revenir", j::accueilScene);
        return s;
    }

    /** Le renom ville par ville, la réputation, le surnom. */
    Joueur.Scene renom() {
        Fiche f = j.fiche();
        Renommee r = j.renommee();
        int v = j.villeIci();
        String surnom = r.surnom();
        StringBuilder t = new StringBuilder(surnom.isEmpty() ? "On ne te donne pas encore de surnom." : "On t'appelle « " + surnom + " ».");
        if (v >= 0) {
            String ici = r.surnomEn(v);
            t.append(ici.isEmpty() ? "\nIci, on ne te donne pas de surnom." : "\nIci, on t'appelle « " + ici + " ».");
        }
        t.append("\nLà où l'on te connaît :").append(r.villesConnues(6));
        t.append("\nCe que pensent de toi :").append(r.groupesTexte(v));
        if (v >= 0) t.append("\nIci : ").append(Renommee.NOMS_PALIERS[r.palierEn(v)]).append(f.e()).append(", réputation ").append(Renommee.reputationTexte(r.reputation(v))).append('.');
        t.append("\nTon renom voyage par les routes, porté par les marchands et les voyageurs, et s'efface s'il n'est pas entretenu.");
        t.append("\nRenom total : ").append(f.renom).append(". Régions traversées : ").append(regions.size()).append(". Livres lus : ").append(livres.size()).append('.');
        Joueur.Scene s = new Joueur.Scene("Ce qu'on dit de moi", t.toString());
        s.ajouter("« Revenir", this::scene);
        return s;
    }

    // ---------------------------------------------------------------- Sauvegarde

    void ecrire(DataOutputStream s) throws IOException {
        s.writeFloat(moral);
        s.writeInt(jourEnvies);
        s.writeFloat(heuresDuJour);
        s.writeInt(jourHeures);
        s.writeInt(envies.size());
        for (Envie e : envies) {
            s.writeInt(e.type);
            s.writeInt(e.param);
            s.writeInt(e.jour);
            s.writeBoolean(e.faite);
            s.writeUTF(e.texte);
        }
        s.writeInt(livres.size());
        for (int x : livres) s.writeInt(x);
        s.writeInt(regions.size());
        for (int x : regions) s.writeInt(x);
    }

    void lire(DataInputStream s) throws IOException {
        moral = s.readFloat();
        jourEnvies = s.readInt();
        heuresDuJour = s.readFloat();
        jourHeures = s.readInt();
        int n = s.readInt();
        for (int k = 0; k < n; k++) {
            Envie e = new Envie();
            e.type = s.readInt();
            e.param = s.readInt();
            e.jour = s.readInt();
            e.faite = s.readBoolean();
            e.texte = s.readUTF();
            envies.add(e);
        }
        n = s.readInt();
        for (int k = 0; k < n; k++) livres.add(s.readInt());
        n = s.readInt();
        for (int k = 0; k < n; k++) regions.add(s.readInt());
    }
}
