package com.deldaran.politics.histoire;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Lot H3 : les notables (les rivaux) de chaque ville.
 *
 * Chaque ville que le joueur connaît (celle où il vit, celles où il passe) a un nombre fixe de notables, selon sa taille. Ce sont
 * des habitants de la Foule, choisis pour leur ambition, leur talent et leur rang ; ils deviennent importants (leur fiche est gardée
 * et évolue). Comme le joueur, chacun poursuit une aspiration (la même liste : fonder une entreprise, diriger sa ville, devenir
 * noble…) et franchit ses étapes jour après jour, selon sa compétence et son ambition ; il gagne aussi sa vie. S'il meurt ou quitte
 * la ville, le meilleur des autres prend sa place : le nombre reste le même, et c'est lui qui borne le calcul.
 *
 * Celui qui poursuit le même genre d'aspiration que le joueur, dans sa ville, est son RIVAL.
 */
public final class Notables {
    /** Le nombre de notables : NOMBRE_MIN dans un hameau, puis un de plus par PAR_RACINE de racine carrée d'habitants, NOMBRE_MAX au plus. */
    public static final int NOMBRE_MIN = 2, NOMBRE_MAX = 12;
    public static final float PAR_RACINE = 8f;
    /** L'âge (humain) où l'on peut devenir notable, et celui où l'on se retire. */
    public static final int AGE_MIN = 20, AGE_RETRAITE = 72;
    /** Une étape coûte ETAPE_BASE + ETAPE_PLUS × étape points d'avancée ; un jour en donne environ AVANCE_BASE + compétence / PAR_COMPETENCE + ambition / PAR_AMBITION (un but de quatre étapes prend des années). */
    public static final float ETAPE_BASE = 120f, ETAPE_PLUS = 80f, AVANCE_BASE = 0.4f, PAR_COMPETENCE = 150f, PAR_AMBITION = 150f;
    /** Le renom gagné à chaque étape franchie. */
    public static final int RENOM_ETAPE = 15;
    /** On suit au plus VILLES_MAX villes (les plus récemment vues restent). */
    public static final int VILLES_MAX = 16;

    private final Joueur j;
    private final Population p;
    /** Les notables de chaque ville suivie, par ordre de première visite. */
    final Map<Integer, List<Long>> villes = new LinkedHashMap<>();
    /** L'avancée de chacun vers sa prochaine étape. */
    final Map<Long, Float> avance = new HashMap<>();
    /** Le notable qui dirige la ville (il a accompli « Diriger sa ville »). */
    final Map<Integer, Long> chefs = new HashMap<>();
    int dernierJour = -1;

    Notables(Joueur j, Population p) {
        this.j = j;
        this.p = p;
    }

    /** Le nombre de notables d'une ville de cette taille. */
    public static int nombre(int habitants) {
        return Math.max(NOMBRE_MIN, Math.min(NOMBRE_MAX, Math.round(NOMBRE_MIN + (float) Math.sqrt(Math.max(0, habitants)) / PAR_RACINE)));
    }

    private Foule foule(int ville) {
        if (ville < 0 || !p.villes.estVivante(ville)) return null;
        int q = p.villes.peuple[ville], r = p.villes.region[ville];
        return q >= 0 && r >= 0 && q < p.foules.length && r < p.foules[q].length ? p.foules[q][r] : null;
    }

    private String nomVille(int v) {
        return Noms.ville(p.villes.peuple[v], p.graine, v);
    }

    /** Ce qui fait un notable : l'ambition, le talent, le courage, le rang (les nobles d'abord). */
    private static int valeur(Foule f, int q) {
        return f.ambitionDe(q) + f.talentDe(q) + f.courageDe(q) / 2 + (f.maisonDe(q) >= 0 ? 60 : 0) + f.rangDe(q) * 10;
    }

    /** Le notable est-il encore là (vivant, dans sa ville, en âge) ? */
    private boolean present(Foule f, int ville, long id) {
        int q = f.placeDe(id);
        if (q < 0 || !f.estEnVie(q, id) || f.villeDe(q) != ville) return false;
        return Gens.age(f, q, j.gens().annee()) < AGE_RETRAITE;
    }

    /** Commence à suivre une ville : ses notables sont choisis. */
    void suivre(int ville) {
        if (ville < 0 || villes.containsKey(ville) || foule(ville) == null) return;
        while (villes.size() >= VILLES_MAX) {
            Integer vieille = villes.keySet().iterator().next();
            if (j.fiche() != null && vieille == j.fiche().ville) {
                List<Long> l = villes.remove(vieille);
                villes.put(vieille, l);
                vieille = villes.keySet().iterator().next();
            }
            villes.remove(vieille);
        }
        villes.put(ville, new ArrayList<>());
        completer(ville, -1);
    }

    /** Remplit la liste de la ville jusqu'à son nombre, avec les meilleurs habitants qui ne sont pas déjà notables. Rend le dernier pris. */
    private long completer(int ville, int jour) {
        return completer(ville, jour, -1L);
    }

    private long completer(int ville, int jour, long exclu) {
        Foule f = foule(ville);
        List<Long> l = villes.get(ville);
        if (f == null || l == null) return -1L;
        int voulu = nombre(p.villes.habitants[ville]);
        long dernier = -1L;
        long moi = j.identifiant();
        while (l.size() < voulu) {
            int meilleur = -1, score = Integer.MIN_VALUE;
            for (int q = 0; q < f.taille(); q++) {
                long id = f.idDe(q);
                if (id == moi || id == exclu || !f.estEnVie(q, id) || f.villeDe(q) != ville || l.contains(id)) continue;
                float age = Gens.age(f, q, j.gens().annee());
                if (age < AGE_MIN || age >= AGE_RETRAITE - 5) continue;
                int s = valeur(f, q);
                if (s > score) {
                    score = s;
                    meilleur = q;
                }
            }
            if (meilleur < 0) break;
            long id = f.idDe(meilleur);
            l.add(id);
            Fiche x = j.gens().retenir(f, meilleur);
            if (x.aspiration < 0) {
                x.aspiration = aspiration(x, f.maisonDe(meilleur) >= 0, -1);
                x.etape = 0;
            }
            avance.putIfAbsent(id, 0f);
            dernier = id;
        }
        return dernier;
    }

    /** L'aspiration d'un notable : celle qui va à son caractère et à ce qu'il sait faire (couronne et conquêtes pour les nobles seulement). */
    static int aspiration(Fiche x, boolean noble, int ancienne) {
        Random r = new Random(Travail.melanger(x.id * 31L + 17L + ancienne * 977L));
        int meilleure = -1;
        float score = -Float.MAX_VALUE;
        for (int k = 0; k < Aspirations.TOUTES.size(); k++) {
            Aspirations.Aspiration a = Aspirations.TOUTES.get(k);
            if (k == Aspirations.VENGEANCE || k == ancienne) continue;
            boolean royale = a.nom.startsWith("Devenir roi") || a.nom.startsWith("Unifier") || a.nom.startsWith("Fonder une ville");
            if (royale && !noble) continue;
            float s = -Math.abs(x.axes[a.axe] - a.penchant * 2f) / 2f + x.competences[a.competence] / 3f + r.nextFloat() * 40f;
            if (a.groupe == Aspirations.POUVOIR && noble) s += 30f;
            if (s > score) {
                score = s;
                meilleure = k;
            }
        }
        return meilleure;
    }

    /** Le rival : poursuit le même genre d'aspiration que le joueur, dans la ville où il vit. */
    public boolean rival(Fiche x) {
        Fiche moi = j.fiche();
        if (moi == null || moi.aspiration < 0 || x.aspiration < 0) return false;
        List<Long> l = villes.get(moi.ville);
        return l != null && l.contains(x.id) && Aspirations.TOUTES.get(x.aspiration).groupe == Aspirations.TOUTES.get(moi.aspiration).groupe;
    }

    // ================================================================ Chaque jour

    void avancer() {
        Fiche moi = j.fiche();
        if (moi == null) return;
        suivre(moi.ville);
        suivre(j.villeIci());
        int jour = j.jourMaintenant();
        if (dernierJour < 0) dernierJour = jour;
        for (int d = Math.max(dernierJour, jour - 60) + 1; d <= jour; d++) journee(d);
        dernierJour = Math.max(dernierJour, jour);
    }

    void journee(int d) {
        for (Map.Entry<Integer, List<Long>> e : villes.entrySet()) {
            int ville = e.getKey();
            Foule f = foule(ville);
            if (f == null) continue;
            List<Long> l = e.getValue();
            // Les morts, les partis et les vieux laissent leur place.
            for (int k = l.size() - 1; k >= 0; k--) {
                long id = l.get(k);
                if (present(f, ville, id)) continue;
                l.remove(k);
                avance.remove(id);
                String ancien = Noms.personne(f.indicePeuple, p.graine, id, f.placeDe(id) >= 0 ? f.sexeDe(f.placeDe(id)) : 0);
                if (chefs.get(ville) != null && chefs.get(ville) == id) chefs.remove(ville);
                long nouveau = completer(ville, d);
                if (nouveau >= 0L) annoncerIci(ville, "À " + nomVille(ville) + ", " + prenom(Noms.personne(f.indicePeuple, p.graine, nouveau, f.sexeDe(f.placeDe(nouveau))))
                        + " prend la place de " + prenom(ancien) + " parmi les notables.");
            }
            completer(ville, d);
            for (long id : l) vivre(f, ville, id, d);
        }
    }

    /** Une journée d'un notable : il gagne sa vie et avance vers son but. */
    private void vivre(Foule f, int ville, long id, int d) {
        int q = f.placeDe(id);
        if (q < 0) return;
        Fiche x = j.gens().retenir(f, q);
        if (x.aspiration < 0) return;
        Aspirations.Aspiration a = Aspirations.TOUTES.get(x.aspiration);
        Random r = new Random(Travail.melanger(id * 7919L + d * 104729L));
        // Le gagne-pain : un peu plus que ce qu'il dépense, selon ce qu'il sait faire.
        int meilleur = 0;
        for (int c : x.competences) meilleur = Math.max(meilleur, c);
        x.argent = Math.max(0, x.argent + meilleur / 12 + r.nextInt(4) - 3);
        if (x.etape >= a.etapes.length) {
            // Le but est atteint : un autre, qui va plus loin.
            x.aspiration = aspiration(x, f.maisonDe(q) >= 0, x.aspiration);
            x.etape = 0;
            avance.put(id, 0f);
            return;
        }
        float pas = (AVANCE_BASE + x.competences[a.competence] / PAR_COMPETENCE + Math.max(0, x.axes[Fiche.AMBITION]) / PAR_AMBITION) * (0.5f + r.nextFloat());
        float v = avance.getOrDefault(id, 0f) + pas;
        float besoin = ETAPE_BASE + ETAPE_PLUS * x.etape;
        if (v < besoin) {
            avance.put(id, v);
            return;
        }
        avance.put(id, 0f);
        String etape = a.etapes[x.etape];
        x.etape++;
        x.renom += RENOM_ETAPE;
        if (a.competence >= 0) x.competences[a.competence] = Math.min(x.plafond(a.competence), x.competences[a.competence] + 5);
        if (a.groupe == Aspirations.RICHESSE) x.argent += 200 * x.etape;
        String qui = prenom(x.nom);
        boolean rival = rival(x);
        if (x.etape >= a.etapes.length && a.nom.startsWith("Diriger sa ville")) {
            chefs.put(ville, id);
            annoncerIci(ville, qui + " est désormais à la tête " + Creation.de(nomVille(ville)) + "." + (rival ? (x.femme() ? " Ta rivale l'a emporté." : " Ton rival l'a emporté.") : ""));
            return;
        }
        String fait = troisieme(etape);
        String texte = (rival ? (x.femme() ? "Ta rivale " : "Ton rival ") + qui : "On dit à " + nomVille(ville) + " que " + qui) + (rival ? " avance : " : " a réussi : ") + "« " + fait + " »"
                + (x.etape >= a.etapes.length ? " — son but est atteint." : ".");
        annoncerIci(ville, texte);
    }

    /** Les nouvelles d'une ville arrivent au joueur s'il y est ou s'il y vit ; elles vont aussi dans son carnet. */
    private void annoncerIci(int ville, String texte) {
        Fiche moi = j.fiche();
        if (moi == null || (ville != moi.ville && ville != j.villeIci())) return;
        j.annoncer(texte);
        j.noter(Terre.date(p, j.jour()) + " : " + texte);
    }

    /** « veut diriger sa ville », « rêve d'être grand marchand ». */
    static String but(Aspirations.Aspiration a) {
        String n = a.nom.substring(0, 1).toLowerCase() + a.nom.substring(1);
        for (String v : new String[] { "devenir", "diriger", "fonder", "unifier", "travailler", "protéger" }) if (n.startsWith(v + " ")) return "veut " + n;
        if (n.equals("grand amour")) return "cherche le grand amour";
        return "rêve d'être " + n;
    }

    /** Une étape d'aspiration, écrite pour le joueur (« tes parents »), dite d'un autre (« ses parents »), sans la parenthèse. */
    static String troisieme(String etape) {
        String t = " " + etape.replaceAll(" \\(.*\\)", "") + " ";
        t = t.replace(" tes ", " ses ").replace(" ta ", " sa ").replace(" ton ", " son ").replace(" toi ", " lui ");
        return t.trim();
    }

    private static String prenom(String nom) {
        return nom.split(" ")[0];
    }

    // ================================================================ Ce qu'on en voit

    /** « C'est un notable d'Ulanewen : il veut fonder une entreprise (étape 2 sur 5). » ; vide s'il n'en est pas un. */
    public String role(long id) {
        for (Map.Entry<Integer, List<Long>> e : villes.entrySet()) {
            if (!e.getValue().contains(id)) continue;
            Fiche x = j.gens().importants.get(id);
            if (x == null || x.aspiration < 0) return "";
            Aspirations.Aspiration a = Aspirations.TOUTES.get(x.aspiration);
            boolean femme = x.femme();
            String chef = chefs.get(e.getKey()) != null && chefs.get(e.getKey()) == id ? (femme ? " Elle dirige la ville." : " Il dirige la ville.") : "";
            return " " + (femme ? "C'est une notable " : "C'est un notable ") + Creation.de(nomVille(e.getKey())) + " : " + (femme ? "elle" : "il") + " " + but(a) + " (étape " + Math.min(x.etape + 1, a.etapes.length) + " sur " + a.etapes.length + ")."
                    + chef + (rival(x) ? (femme ? " C'est ta rivale." : " C'est ton rival.") : "");
        }
        return "";
    }

    /** La page « Les notables d'ici » : chacun, son métier, son but, où il en est, sa fortune. */
    public List<String> lignes(int ville) {
        List<String> r = new ArrayList<>();
        List<Long> l = villes.get(ville);
        Foule f = foule(ville);
        if (l == null || f == null) return r;
        for (long id : l) {
            Fiche x = j.gens().importants.get(id);
            int q = f.placeDe(id);
            if (x == null || q < 0 || x.aspiration < 0) continue;
            Aspirations.Aspiration a = Aspirations.TOUTES.get(x.aspiration);
            String ou = x.etape >= a.etapes.length ? "but atteint" : "en est à « " + troisieme(a.etapes[x.etape]) + " »";
            boolean chef = chefs.get(ville) != null && chefs.get(ville) == id;
            r.add(x.nom + (chef ? " (à la tête de la ville)" : "") + " — " + Emplois.metier(j, p, ville, id) + " ; " + but(a) + ", " + ou + " ; " + Fiche.argentTexte(x.argent) + ", renom " + x.renom + (rival(x) ? (x.femme() ? " — TA RIVALE" : " — TON RIVAL") : ""));
        }
        return r;
    }

    /** Les notables de la ville (pour l'écran et les essais). */
    public List<Long> de(int ville) {
        List<Long> l = villes.get(ville);
        return l == null ? new ArrayList<>() : new ArrayList<>(l);
    }

    // ================================================================ Sauvegarde

    void ecrire(DataOutputStream s) throws IOException {
        s.writeInt(dernierJour);
        s.writeInt(villes.size());
        for (Map.Entry<Integer, List<Long>> e : villes.entrySet()) {
            s.writeInt(e.getKey());
            s.writeInt(e.getValue().size());
            for (long id : e.getValue()) s.writeLong(id);
        }
        s.writeInt(avance.size());
        for (Map.Entry<Long, Float> e : avance.entrySet()) {
            s.writeLong(e.getKey());
            s.writeFloat(e.getValue());
        }
        s.writeInt(chefs.size());
        for (Map.Entry<Integer, Long> e : chefs.entrySet()) {
            s.writeInt(e.getKey());
            s.writeLong(e.getValue());
        }
    }

    void lire(DataInputStream s) throws IOException {
        villes.clear();
        avance.clear();
        chefs.clear();
        dernierJour = s.readInt();
        int n = s.readInt();
        for (int k = 0; k < n; k++) {
            int v = s.readInt();
            int m = s.readInt();
            List<Long> l = new ArrayList<>();
            for (int i = 0; i < m; i++) l.add(s.readLong());
            villes.put(v, l);
        }
        n = s.readInt();
        for (int k = 0; k < n; k++) avance.put(s.readLong(), s.readFloat());
        n = s.readInt();
        for (int k = 0; k < n; k++) chefs.put(s.readInt(), s.readLong());
    }

    // ================================================================ Essais

    /** Essai du lot H3 : fait passer des jours (sans attendre l'horloge). */
    public void joursPourEssai(int debut, int n) {
        for (int d = debut; d < debut + n; d++) journee(d);
    }

    /** Essai du lot H3 : un notable s'en va (comme s'il était mort) ; rend son nom. */
    public String retirerPourEssai(int ville) {
        List<Long> l = villes.get(ville);
        if (l == null || l.isEmpty()) return "-";
        long id = l.remove(0);
        Fiche x = j.gens().importants.get(id);
        int d = j.jour();
        long nouveau = completer(ville, d, id);
        Fiche y = j.gens().importants.get(nouveau);
        return (x == null ? id : x.nom) + " remplacé par " + (y == null ? "-" : y.nom);
    }
}
