package com.deldaran.politics.histoire;

import com.deldaran.politics.Metiers;
import com.deldaran.politics.Relations;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Phase 7 : les ateliers du joueur.
 *
 * Un atelier racheté devient un agent de l'économie de la ville (EconomieVille.Agent avec joueur = true) :
 * il achète ses matières, fabrique, vend aux foyers et aux autres ateliers, en concurrence avec ceux du pays.
 * Le patron règle les ouvriers (embaucher, renvoyer), le salaire, les prix, ce qu'on fabrique, la caisse,
 * et peut engager un intendant qui gère quand il n'est pas là. Sans patron ni intendant, l'ouvrage ralentit et la caisse fond.
 */
public final class Entreprise {
    /** Prix des murs d'un atelier (cuivre), multiplié selon le métier ; s'y ajoutent JOURS_BENEFICE jours de bénéfice, le stock et la caisse. */
    public static final int BATIMENT = 1500;
    public static final int JOURS_BENEFICE = 90;
    /** Droit de la guilde pour tenir un atelier sans y être maître. */
    public static final int LICENCE = 600;
    /** Part de sa valeur que la ville rend quand on revend un atelier. */
    public static final float REVENTE = 0.7f;
    /** Après une visite, le patron compte comme présent pendant tant de jours. */
    public static final int PRESENCE_JOURS = 7;
    /** Le maître refuse de vendre si son estime pour le joueur est sous ce seuil ; personne ne veut travailler pour quelqu'un de cette réputation. */
    public static final float ESTIME_MIN = -0.2f, REPUTATION_MIN = -25f;
    public static final float[] SALAIRES = { 0.8f, 1f, 1.25f };
    public static final String[] NOMS_SALAIRES = { "bas", "comme ailleurs", "généreux" };
    public static final float[] MARGES = { 0.6f, 1f, 1.5f };
    public static final String[] NOMS_PRIX = { "bas", "comme ailleurs", "hauts" };

    public static final class Atelier {
        public int ville, lieu;
        /** Index du bâtiment dans le village (pour le reconnaître à l'écran), -1 s'il n'est pas connu. */
        public int batiment = -1;
        public String intendant = "";
        // ---- Lot D
        /** Les ouvriers, un par un (Affaires.equipe les tient au nombre de l'atelier). */
        public final List<Affaires.Employe> gens = new ArrayList<>();
        public boolean greve, bati;
        /** Le dernier ouvrier soupçonné de vol ; compteur pour nommer les nouveaux. */
        public String suspect = "";
        public int compteur;
        /** Depuis la dernière visite : impôts levés, argent volé, nouvelles (caravanes, grève). */
        public double impots, vole;
        public final List<String> nouvelles = new ArrayList<>();
    }

    private final Joueur j;
    private final Population p;
    public final List<Atelier> ateliers = new ArrayList<>();

    Entreprise(Joueur j, Population p) {
        this.j = j;
        this.p = p;
    }

    private Fiche f() {
        return j.fiche();
    }

    private String ville(int v) {
        return Noms.ville(p.villes.peuple[v], p.graine, v);
    }

    static int prixBatiment(int lieu) {
        float facteur;
        switch (lieu) {
            case Metiers.FORGE: case Metiers.MOULIN: case Metiers.AUBERGE: facteur = 1.5f; break;
            case Metiers.TAVERNE: case Metiers.BOULANGERIE: case Metiers.BOUCHERIE: case Metiers.MENUISERIE: facteur = 1.2f; break;
            default: facteur = 1f;
        }
        return Math.round(BATIMENT * facteur);
    }

    /** « La forge » → « ta forge », « Le moulin » → « ton moulin », « L'auberge » → « ton auberge ». */
    public static String possessif(String nomBoutique) {
        String n = nomBoutique;
        if (n.startsWith("La ")) return "ta " + n.substring(3);
        if (n.startsWith("Le ")) return "ton " + n.substring(3);
        if (n.startsWith("L'")) return "ton " + n.substring(2);
        if (n.startsWith("Les ")) return "tes " + n.substring(4);
        return n.toLowerCase();
    }

    EconomieVille.Agent agent(Atelier x) {
        return j.marche().ville(x.ville, j.jour()).agentJoueur(x.lieu);
    }

    Atelier atelier(int ville, int lieu) {
        for (Atelier x : ateliers) if (x.ville == ville && x.lieu == lieu) return x;
        return null;
    }

    /** Ce bâtiment du village est-il à moi ? (lu par l'écran pour le bandeau) */
    public boolean possede(int ville, int batiment) {
        for (Atelier x : ateliers) if (x.ville == ville && x.batiment == batiment && batiment >= 0) return true;
        return false;
    }

    /** Le patron passe : ses ateliers de cette ville le comptent présent pour PRESENCE_JOURS jours. */
    void visiter(int ville) {
        for (Atelier x : ateliers) {
            if (x.ville != ville) continue;
            EconomieVille.Agent a = agent(x);
            if (a != null) a.presenceJusqua = j.jour() + PRESENCE_JOURS;
        }
    }

    public int nombre() {
        return ateliers.size();
    }

    public int employes() {
        int n = 0;
        for (Atelier x : ateliers) {
            EconomieVille.Agent a = agent(x);
            if (a != null) n += Math.round(a.ouvriers);
        }
        return n;
    }

    public int villes() {
        Set<Integer> v = new HashSet<>();
        for (Atelier x : ateliers) v.add(x.ville);
        return v.size();
    }

    /** Une chaîne : un de mes ateliers fabrique ce qu'un autre de mes ateliers achète comme matière. */
    public boolean chaine() {
        for (Atelier x : ateliers) {
            EconomieVille.Agent a = agent(x);
            if (a == null) continue;
            for (Atelier y : ateliers) {
                if (y == x) continue;
                EconomieVille.Agent b = agent(y);
                if (b == null) continue;
                for (Recettes.Recette r : b.recettes) for (Biens.Bien e : r.entrees) if (a.fabrique[e.id]) return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- Scènes

    /** « Mes ateliers » : la liste, ou comment en avoir un. */
    public Joueur.Scene accueil(String message) {
        StringBuilder t = new StringBuilder();
        Joueur.Scene s = new Joueur.Scene("Mes ateliers", "");
        if (ateliers.isEmpty()) t.append("Tu n'as pas d'atelier. Pour en racheter un, va devant, appuie sur M et propose au maître de te le vendre. "
                + "On en trouve aussi aux enchères (tableau de la place), et l'on peut bâtir sur une ruine.");
        else t.append("Tes ateliers :");
        for (Atelier x : ateliers) {
            EconomieVille.Agent a = agent(x);
            if (a == null) continue;
            s.ajouter(Biens.nomBoutique(x.lieu) + " " + Creation.de(ville(x.ville)) + " — " + Math.round(a.ouvriers) + " ouvriers, caisse " + Fiche.argentTexte((int) Math.floor(a.caisse)),
                    () -> gerer(x, null));
        }
        if (j.ruineProche >= 0 && j.ruineDistance <= Affaires.RUINE_PROCHE && j.villeIci() >= 0) s.ajouter("Bâtir un atelier sur la ruine devant toi…", () -> j.affaires().ruine(null));
        s.ajouter("« Revenir", j::accueilScene);
        Joueur.Scene vraie = new Joueur.Scene("Mes ateliers", Joueur.avec(message, t.toString()));
        vraie.choix.addAll(s.choix);
        vraie.suites.addAll(s.suites);
        return vraie;
    }

    /** Depuis la boutique : proposer au maître de racheter son atelier. */
    Joueur.Scene proposer(int lieu) {
        Fiche f = f();
        int v = j.villeIci();
        EconomieVille.Agent a = j.agentIci(lieu);
        if (a == null || v < 0) return j.accueilScene();
        EconomieVille e = j.marche().ville(v, j.jour());
        String M = j.travail().maitreNom(lieu);
        String Mm = Character.toUpperCase(M.charAt(0)) + M.substring(1);
        Relations.Lien l = j.maitres != null && lieu < j.maitres.length ? j.maitres[lieu] : null;
        if (l != null && l.estime < ESTIME_MIN) return refus(lieu, Mm + " te toise : « Vendre à toi ? Jamais. »");
        if (j.renommee().reputation(v) < REPUTATION_MIN) return refus(lieu, Mm + " a entendu parler de toi et ne veut pas traiter avec toi.");
        int prix = (int) Math.ceil(e.valeur(a, prixBatiment(lieu), JOURS_BENEFICE));
        float part = 1f / Math.max(1, a.batiments);
        boolean maitre = (j.travail().maitrises & (1L << lieu)) != 0L;
        int licence = maitre ? 0 : j.renommee().licence(lieu, LICENCE);
        StringBuilder t = new StringBuilder();
        t.append(Mm).append(" réfléchit. ").append(a.batiments > 1 ? "Il tient " + a.batiments + " ateliers en ville ; il veut bien t'en céder un, avec ses ouvriers et sa part du stock."
                : "C'est son seul atelier : tu reprendrais tout, les ouvriers, le stock et la caisse.");
        t.append("\nSes clients lui achètent pour environ ").append(Fiche.argentTexte((int) Math.round(e.ventesJour(a) * part))).append(" par jour ; il en tire environ ")
                .append(Fiche.argentTexte((int) Math.round(Math.max(0.0, e.beneficeJour(a) * part)))).append(" de bénéfice.");
        t.append("\nSon prix : ").append(Fiche.argentTexte(prix)).append('.');
        if (licence > 0) t.append(" La guilde demande en plus un droit de ").append(Fiche.argentTexte(licence)).append(" à qui n'est pas maître du métier.");
        t.append("\nTu as ").append(Fiche.argentTexte(f.argent)).append('.');
        Joueur.Scene s = new Joueur.Scene("Racheter " + Biens.nomBoutique(lieu).toLowerCase(), t.toString());
        if (f.argent >= prix + licence) s.ajouter("Payer " + Fiche.argentTexte(prix + licence) + " et prendre l'atelier", () -> racheter(lieu, prix + licence));
        s.ajouter("« Non, pas maintenant", () -> j.boutiqueScene(lieu));
        return s;
    }

    private Joueur.Scene refus(int lieu, String texte) {
        Joueur.Scene s = new Joueur.Scene("Racheter " + Biens.nomBoutique(lieu).toLowerCase(), texte);
        s.ajouter("« Revenir", () -> j.boutiqueScene(lieu));
        return s;
    }

    private Joueur.Scene racheter(int lieu, int prix) {
        Fiche f = f();
        int v = j.villeIci();
        EconomieVille.Agent a = j.agentIci(lieu);
        if (a == null || v < 0 || f.argent < prix) return j.accueilScene();
        EconomieVille e = j.marche().ville(v, j.jour());
        f.argent -= prix;
        e.bourse += prix;
        EconomieVille.Agent mien = e.racheter(a);
        mien.presenceJusqua = j.jour() + PRESENCE_JOURS;
        Atelier x = atelier(v, lieu);
        if (x == null) {
            x = new Atelier();
            x.ville = v;
            x.lieu = lieu;
            ateliers.add(x);
        }
        int[] bat = j.batimentsIci;
        x.batiment = bat != null && lieu < bat.length ? bat[lieu] : -1;
        // On ne peut pas être à la fois l'ouvrier et le patron de ce même atelier.
        Travail t = j.travail();
        if (t.employe() && t.ville == v && t.lieu == lieu) t.quitter();
        String phrase = "En l'an " + p.annee + ", tu rachètes " + Biens.nomBoutique(lieu).toLowerCase() + " " + Creation.de(ville(v)) + ".";
        f.recit.add(phrase);
        j.noter(phrase);
        j.renommee().gagner(v, 4f, 2f, "Tu as ton atelier");
        j.destin().verifierEtapes();
        j.enregistrer();
        return gerer(x, "L'affaire est conclue. " + Character.toUpperCase(possessif(Biens.nomBoutique(lieu)).charAt(0)) + possessif(Biens.nomBoutique(lieu)).substring(1)
                + " est à toi. N'oublie pas de laisser de quoi payer les ouvriers dans la caisse.");
    }

    /** Un atelier devient le mien (enchère gagnée, chantier fini) : l'agent du joueur existe déjà dans l'économie de la ville. */
    Atelier prendre(int v, int lieu, int batiment, String phrase) {
        Atelier x = atelier(v, lieu);
        if (x == null) {
            x = new Atelier();
            x.ville = v;
            x.lieu = lieu;
            ateliers.add(x);
        }
        x.batiment = batiment;
        Travail t = j.travail();
        if (t.employe() && t.ville == v && t.lieu == lieu) t.quitter();
        Fiche f = f();
        f.recit.add(phrase);
        j.noter(phrase);
        j.renommee().gagner(v, 4f, 2f, "Tu as ton atelier");
        j.destin().verifierEtapes();
        j.enregistrer();
        return x;
    }

    /** La fenêtre de gestion d'un atelier. */
    public Joueur.Scene gerer(Atelier x, String message) {
        EconomieVille.Agent a = agent(x);
        if (a == null) {
            ateliers.remove(x);
            return accueil("Cet atelier n'existe plus.");
        }
        EconomieVille e = j.marche().ville(x.ville, j.jour());
        if (j.villeIci() == x.ville) a.presenceJusqua = j.jour() + PRESENCE_JOURS;
        Fiche f = f();
        String nom = possessif(Biens.nomBoutique(x.lieu));
        StringBuilder t = new StringBuilder();
        t.append(Character.toUpperCase(nom.charAt(0))).append(nom.substring(1)).append(' ').append(Creation.de(ville(x.ville))).append(" : ")
                .append(Math.round(a.ouvriers)).append(" ouvrier").append(a.ouvriers >= 1.5f ? "s" : "").append(" (au plus ").append(Math.round(a.maxOuvriers())).append("), ouvrage ")
                .append(Math.round(Math.min(1f, a.emploi) * 100f)).append(" %.");
        t.append("\nCaisse : ").append(a.caisse < 0 ? "dette de " + Fiche.argentTexte((int) Math.ceil(-a.caisse)) : Fiche.argentTexte((int) Math.floor(a.caisse)))
                .append(". Chaque jour : ").append(Fiche.argentTexte(Math.round(a.ouvriers * Recettes.SALAIRE * a.salaireJoueur + (a.intendance > 0f ? EconomieVille.INTENDANT_SALAIRE : 0))))
                .append(" de salaires.");
        t.append(" Les clients t'achètent pour environ ").append(Fiche.argentTexte((int) Math.round(e.ventesJour(a)))).append(" par jour.");
        t.append("\nSalaire ").append(NOMS_SALAIRES[indice(SALAIRES, a.salaireJoueur)]).append(", prix ").append(NOMS_PRIX[indice(MARGES, a.margeJoueur)]).append('.');
        t.append(x.intendant.isEmpty() ? " Pas d'intendant." : " Intendant : " + x.intendant + " (Intendance " + Math.round(a.intendance) + ").");
        // Les comptes depuis la dernière fois
        double benefice = a.ventes - a.achats - a.salaires - a.pertes;
        if (a.ventes + a.achats + a.salaires + a.pertes > 0.5) {
            t.append("\nDepuis ta dernière visite : ventes ").append(Fiche.argentTexte((int) Math.round(a.ventes))).append(", matières ").append(Fiche.argentTexte((int) Math.round(a.achats)))
                    .append(", salaires ").append(Fiche.argentTexte((int) Math.round(a.salaires)));
            if (a.pertes >= 1.0) t.append(", disparu de la caisse ").append(Fiche.argentTexte((int) Math.round(a.pertes)));
            t.append(benefice >= 0 ? ". Bénéfice : " : ". Perte : ").append(Fiche.argentTexte((int) Math.round(Math.abs(benefice)))).append('.');
        }
        if (a.partis > 0) t.append("\n").append(a.partis).append(a.partis > 1 ? " ouvriers sont partis" : " ouvrier est parti").append(a.impayes > 0 ? " : la caisse ne pouvait plus les payer." : ".");
        if (a.pertes >= 1.0 && x.intendant.isEmpty()) t.append(" Sans personne pour surveiller, l'argent file.");
        if (a.ouvriers >= 2f && a.emploi < EconomieVille.OUVRAGE_MIN && !a.recettes.isEmpty())
            t.append("\nTes ouvriers n'ont de l'ouvrage que ").append(Math.round(a.emploi * 100f)).append(" % du temps : tu en paies trop pour ce que la ville achète.");
        else if (a.emploi > EconomieVille.OUVRAGE_MAX && a.ouvriers < a.maxOuvriers() && !a.recettes.isEmpty())
            t.append("\nTes ouvriers ne suffisent pas à l'ouvrage : tu pourrais en embaucher.");
        j.affaires().rapport(x, a, t);
        a.ventes = a.achats = a.salaires = a.pertes = 0.0;
        a.partis = 0;
        // Ce qu'il y a en stock, ce qui manque
        StringBuilder st = new StringBuilder(), manque = new StringBuilder();
        for (int b = 0; b < a.stock.length; b++) {
            Biens.Bien bien = Biens.get(b);
            if (a.fabrique[b] && a.stock[b] >= 1f) st.append(st.length() > 0 ? ", " : "").append(bien.quantite((int) a.stock[b])).append(" à ").append(Fiche.argentTexte(Math.max(1, Math.round(a.prix[b]))));
            if (a.achete[b] && !a.fabrique[b] && a.cible[b] >= 1f && a.stock[b] < a.cible[b] * 0.3f) manque.append(manque.length() > 0 ? ", " : "").append(bien.nom);
        }
        t.append("\nEn réserve : ").append(st.length() == 0 ? "rien" : st).append('.');
        if (manque.length() > 0) t.append(" Il manque : ").append(manque).append('.');
        if (e.absent(a) && a.intendance <= 0f) t.append("\nTu n'es pas passé").append(f.e()).append(" depuis longtemps : sans toi, l'ouvrage traîne.");
        Joueur.Scene s = new Joueur.Scene("", "");
        s.ajouter("Mettre de l'argent dans la caisse", () -> deposer(x));
        if (a.caisse >= 1.0) s.ajouter("Prendre de l'argent dans la caisse", () -> retirer(x));
        if (a.ouvriers < a.maxOuvriers()) s.ajouter("Embaucher un ouvrier", () -> embaucher(x));
        if (a.ouvriers >= 1f) s.ajouter("Renvoyer un ouvrier", () -> renvoyer(x));
        s.ajouter("Salaire : " + NOMS_SALAIRES[indice(SALAIRES, a.salaireJoueur)] + " (changer)", () -> {
            a.salaireJoueur = SALAIRES[(indice(SALAIRES, a.salaireJoueur) + 1) % SALAIRES.length];
            j.enregistrer();
            return gerer(x, "Tu paies désormais tes ouvriers " + (a.salaireJoueur < 0.9f ? "moins qu'ailleurs : certains iront voir ailleurs." : a.salaireJoueur > 1.1f
                    ? "mieux qu'ailleurs : ils travaillent de bon cœur." : "comme ailleurs."));
        });
        s.ajouter("Prix : " + NOMS_PRIX[indice(MARGES, a.margeJoueur)] + " (changer)", () -> {
            a.margeJoueur = MARGES[(indice(MARGES, a.margeJoueur) + 1) % MARGES.length];
            j.enregistrer();
            return gerer(x, a.margeJoueur < 0.9f ? "Tu vends moins cher que les autres : plus de clients, moins de marge." : a.margeJoueur > 1.1f
                    ? "Tu vends cher : moins de clients, plus de marge." : "Tu vends au prix de la ville.");
        });
        if (!a.recettes.isEmpty()) s.ajouter("Ce qu'on fabrique", () -> production(x));
        s.ajouter(x.intendant.isEmpty() ? "Engager un intendant (" + Fiche.argentTexte(EconomieVille.INTENDANT_SALAIRE) + " par jour)" : "Renvoyer l'intendant", () -> intendant(x));
        j.affaires().choix(x, a, s);
        s.ajouter("Revendre l'atelier", () -> revendre(x));
        s.ajouter("« Revenir", () -> accueil(null));
        Joueur.Scene vraie = new Joueur.Scene(Character.toUpperCase(nom.charAt(0)) + nom.substring(1), Joueur.avec(message, t.toString()));
        vraie.choix.addAll(s.choix);
        vraie.suites.addAll(s.suites);
        return vraie;
    }

    private static int indice(float[] valeurs, float v) {
        int k = 0;
        for (int i = 1; i < valeurs.length; i++) if (Math.abs(valeurs[i] - v) < Math.abs(valeurs[k] - v)) k = i;
        return k;
    }

    private Joueur.Scene deposer(Atelier x) {
        Fiche f = f();
        Joueur.Scene s = new Joueur.Scene("La caisse", "Combien mets-tu dans la caisse ? Tu as " + Fiche.argentTexte(f.argent) + ".");
        for (int somme : new int[] { 100, 500, 1000, 5000 }) {
            if (somme > f.argent) continue;
            s.ajouter(Fiche.argentTexte(somme), () -> mouvement(x, somme));
        }
        if (f.argent > 0) s.ajouter("Tout (" + Fiche.argentTexte(f.argent) + ")", () -> mouvement(x, f.argent));
        s.ajouter("« Revenir", () -> gerer(x, null));
        return s;
    }

    private Joueur.Scene retirer(Atelier x) {
        EconomieVille.Agent a = agent(x);
        int dispo = (int) Math.floor(Math.max(0.0, a.caisse));
        Joueur.Scene s = new Joueur.Scene("La caisse", "Combien prends-tu ? Il y a " + Fiche.argentTexte(dispo) + " dans la caisse. Laisse de quoi payer les salaires.");
        for (int somme : new int[] { 100, 500, 1000, 5000 }) {
            if (somme > dispo) continue;
            s.ajouter(Fiche.argentTexte(somme), () -> mouvement(x, -somme));
        }
        if (dispo > 0) s.ajouter("Tout (" + Fiche.argentTexte(dispo) + ")", () -> mouvement(x, -dispo));
        s.ajouter("« Revenir", () -> gerer(x, null));
        return s;
    }

    /** Argent de la poche vers la caisse (somme positive) ou de la caisse vers la poche (négative). */
    private Joueur.Scene mouvement(Atelier x, int somme) {
        EconomieVille.Agent a = agent(x);
        Fiche f = f();
        if (somme > 0) somme = Math.min(somme, f.argent);
        else somme = -Math.min(-somme, (int) Math.floor(Math.max(0.0, a.caisse)));
        f.argent -= somme;
        a.caisse += somme;
        j.enregistrer();
        return gerer(x, somme >= 0 ? "Tu mets " + Fiche.argentTexte(somme) + " dans la caisse." : "Tu prends " + Fiche.argentTexte(-somme) + " dans la caisse.");
    }

    private Joueur.Scene embaucher(Atelier x) {
        EconomieVille.Agent a = agent(x);
        if (j.renommee().reputation(x.ville) < REPUTATION_MIN) return gerer(x, "Personne ne veut travailler pour toi : ta réputation te précède.");
        if (a.ouvriers >= a.maxOuvriers()) return gerer(x, "Il n'y a plus de place dans l'atelier.");
        a.ouvriers = Math.round(a.ouvriers) + 1f;
        j.destin().verifierEtapes();
        j.enregistrer();
        return gerer(x, "Un ouvrier de plus. Il te coûtera " + Fiche.argentTexte(Math.round(Recettes.SALAIRE * a.salaireJoueur)) + " par jour.");
    }

    private Joueur.Scene renvoyer(Atelier x) {
        EconomieVille.Agent a = agent(x);
        a.ouvriers = Math.max(0f, Math.round(a.ouvriers) - 1f);
        j.renommee().gagner(x.ville, 0f, -0.5f, null);
        j.enregistrer();
        return gerer(x, "Tu renvoies un ouvrier. Il s'en va sans un mot.");
    }

    private Joueur.Scene production(Atelier x) {
        EconomieVille.Agent a = agent(x);
        Joueur.Scene s = new Joueur.Scene("Ce qu'on fabrique", "Ce que l'atelier sait faire. Arrêter une recette, c'est ne plus en acheter les matières.");
        for (int k = 0; k < a.recettes.size(); k++) {
            Recettes.Recette r = a.recettes.get(k);
            StringBuilder l = new StringBuilder(a.arret[k] ? "[arrêté] " : "[en marche] ");
            for (int i = 0; i < r.sorties.size(); i++) l.append(i > 0 ? ", " : "").append(r.sorties.get(i).nom);
            if (!r.entrees.isEmpty()) {
                l.append(" (avec ");
                for (int i = 0; i < r.entrees.size(); i++) l.append(i > 0 ? ", " : "").append(r.entrees.get(i).nom);
                l.append(')');
            }
            int kk = k;
            s.ajouter(l.toString(), () -> {
                a.arret[kk] = !a.arret[kk];
                j.enregistrer();
                return production(x);
            });
        }
        s.ajouter("« Revenir", () -> gerer(x, null));
        return s;
    }

    private Joueur.Scene intendant(Atelier x) {
        EconomieVille.Agent a = agent(x);
        if (!x.intendant.isEmpty()) {
            String nom = x.intendant;
            x.intendant = "";
            a.intendance = 0f;
            j.enregistrer();
            return gerer(x, nom + " rend les clés.");
        }
        long g = Travail.melanger(p.graine * 31L + x.ville * 7919L + x.lieu * 104729L + j.jour());
        int sexe = (int) (g & 1L);
        String nom = Noms.personne(p.villes.peuple[x.ville], p.graine, g >>> 8, sexe);
        float competence = 20f + (g >>> 20) % 61;
        x.intendant = nom;
        a.intendance = competence;
        j.enregistrer();
        return gerer(x, nom + " sera ton intendant" + (sexe == 1 ? "e" : "") + " (Intendance " + Math.round(competence) + ") : "
                + (sexe == 1 ? "elle" : "il") + " surveillera la caisse, embauchera et renverra selon l'ouvrage.");
    }

    private Joueur.Scene revendre(Atelier x) {
        EconomieVille.Agent a = agent(x);
        EconomieVille e = j.marche().ville(x.ville, j.jour());
        int prix = (int) Math.floor(e.valeur(a, prixBatiment(x.lieu), JOURS_BENEFICE) * REVENTE);
        Joueur.Scene s = new Joueur.Scene("Revendre", "Un patron de la ville te rachèterait l'atelier, avec son stock et sa caisse, pour " + Fiche.argentTexte(prix) + ".");
        s.ajouter("Vendre pour " + Fiche.argentTexte(prix), () -> {
            Fiche f = f();
            f.argent += prix;
            e.bourse -= prix;
            e.rendre(a);
            ateliers.remove(x);
            j.noter("Tu revends " + Biens.nomBoutique(x.lieu).toLowerCase() + " " + Creation.de(ville(x.ville)) + ".");
            j.enregistrer();
            return accueil("Tu as vendu l'atelier pour " + Fiche.argentTexte(prix) + ".");
        });
        s.ajouter("« Non", () -> gerer(x, null));
        return s;
    }

    void ecrire(DataOutputStream s) throws IOException {
        s.writeInt(ateliers.size());
        for (Atelier x : ateliers) {
            s.writeInt(x.ville);
            s.writeInt(x.lieu);
            s.writeInt(x.batiment);
            s.writeUTF(x.intendant);
            s.writeBoolean(x.bati);
            s.writeBoolean(x.greve);
            s.writeUTF(x.suspect);
            s.writeInt(x.compteur);
            s.writeInt(x.gens.size());
            for (Affaires.Employe o : x.gens) {
                s.writeUTF(o.nom);
                s.writeBoolean(o.femme);
                s.writeLong(o.graine);
                s.writeFloat(o.fiabilite);
                s.writeFloat(o.loyaute);
                s.writeFloat(o.honnetete);
            }
        }
    }

    void lire(DataInputStream s, int format) throws IOException {
        int n = s.readInt();
        for (int k = 0; k < n; k++) {
            Atelier x = new Atelier();
            x.ville = s.readInt();
            x.lieu = s.readInt();
            x.batiment = s.readInt();
            x.intendant = s.readUTF();
            if (format >= 13) {
                x.bati = s.readBoolean();
                x.greve = s.readBoolean();
                x.suspect = s.readUTF();
                x.compteur = s.readInt();
                int g = s.readInt();
                for (int i = 0; i < g; i++) {
                    Affaires.Employe o = new Affaires.Employe();
                    o.nom = s.readUTF();
                    o.femme = s.readBoolean();
                    o.graine = s.readLong();
                    o.fiabilite = s.readFloat();
                    o.loyaute = s.readFloat();
                    o.honnetete = s.readFloat();
                    x.gens.add(o);
                }
            }
            ateliers.add(x);
        }
    }
}
