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
 * Lot D : la vie des affaires autour des ateliers du joueur.
 * - les ouvriers ont un nom, une fiabilité, une loyauté et une honnêteté : ils volent, se mettent en grève, se gagnent par une prime ;
 * - le seigneur lève chaque semaine la taille et la dîme sur chaque atelier (davantage en guerre) ;
 * - un atelier en faillite, ou dont le maître meurt sans héritier, est vendu aux enchères (annonce au tableau de la place) ;
 * - on peut bâtir un atelier neuf sur une ruine ;
 * - des caravanes relient ses propres ateliers d'une ville à l'autre, et les marchands du pays envoient leurs surplus là où ils manquent.
 */
public final class Affaires {
    // ---- Impôts du seigneur (par atelier, chaque semaine)
    public static final int TAILLE = 10, SEMAINE = 7;
    public static final float DIME = 0.08f, IMPOT_GUERRE = 1.5f;
    // ---- Ouvriers
    /** Loyauté d'un ouvrier qui arrive ; vers où elle tend selon le salaire (bas, comme ailleurs, généreux). */
    public static final float LOYAUTE_DEPART = 55f, LOYAUTE_BAS = 30f, LOYAUTE_NORMAL = 55f, LOYAUTE_GENEREUX = 75f;
    /** Sous GREVE de loyauté moyenne (avec au moins GREVE_MIN ouvriers), ils posent les outils ; au-dessus de FIN_GREVE, ils reprennent. */
    public static final float GREVE = 25f, FIN_GREVE = 45f;
    public static final int GREVE_MIN = 3;
    /** Chance par jour qu'un ouvrier malhonnête puise dans la caisse (doublée s'il est mécontent). */
    public static final float VOL = 0.03f;
    public static final int PRIME = 20, PRIME_LOYAUTE = 15;
    // ---- Enchères
    public static final float ENCHERE_CHANCE = 1f / 120f, MISE_A_PRIX = 0.45f, PLAFOND_MIN = 0.6f, PLAFOND_MAX = 0.95f, PAS = 0.08f;
    public static final int ENCHERE_JOURS = 6, ENCHERE_REPOS = 60;
    public static final int FAILLITE = 0, SANS_HERITIER = 1;
    // ---- Bâtir sur une ruine
    public static final float RUINE = 0.8f, RUINE_PROCHE = 8f;
    public static final int CHANTIER_JOURS = 12;
    // ---- Caravanes
    public static final float TRANSPORT_KM = 0.004f, PILLAGE = 0.04f, CARAVANE_KM = 250f, CARAVANE_MAX_KM = 600f;
    /** Rattrapage au plus de tant de jours d'un coup (partie reprise après longtemps). */
    public static final int RATTRAPAGE = 30;

    public static final class Employe {
        public String nom = "";
        public boolean femme;
        public long graine;
        /** 0,7 … 1,3 : multiplie l'ouvrage fait. */
        public float fiabilite = 1f;
        /** 0 … 100. */
        public float loyaute = LOYAUTE_DEPART;
        /** 0 … 1 : un ouvrier peu honnête puise dans la caisse. */
        public float honnetete = 0.7f;
    }

    public static final class Enchere {
        public int ville, lieu, fin, prix, plafond, raison;
        /** Le joueur a misé, et il mène. */
        public boolean mise, mene;
    }

    public static final class Chantier {
        public int ville, batiment, lieu, fin;
    }

    /** Lot G5 : un atelier bâti par le joueur sur un terrain libre : où il se tenait (a, b, en mètres locaux du village) et où il regardait. */
    public static final class Construction {
        public int ville, lieu, fin;
        public float a, b, angle;
        public boolean fini;
    }

    /** Lot G5 : le terrain coûte le prix du bâtiment × TERRAIN (sur une ruine : × RUINE), plus la licence. */
    public static final float TERRAIN = 1.0f;
    final List<Construction> constructions = new ArrayList<>();

    public static final class Caravane {
        public int deVille, deLieu, versVille, versLieu, bien, prochaine;
    }

    /** Un convoi des marchands du pays (pas enregistré : c'est une nouvelle). */
    public static final class Convoi {
        public int de, vers, bien, quantite, jour;
        public boolean annonce;
    }

    private final Joueur j;
    private final Population p;
    public final List<Enchere> encheres = new ArrayList<>();
    public final List<Chantier> chantiers = new ArrayList<>();
    public final List<Caravane> caravanes = new ArrayList<>();
    public final List<Convoi> convois = new ArrayList<>();
    private final Map<Integer, Integer> derniereEnchere = new HashMap<>();
    private int dernierJour = -1;
    /** Pour les essais : forcer une enchère dans la ville où l'on est, ou le pillage d'une caravane. */
    public boolean forcerEnchere, forcerPillage;

    Affaires(Joueur j, Population p) {
        this.j = j;
        this.p = p;
    }

    private String ville(int v) {
        return Noms.ville(p.villes.peuple[v], p.graine, v);
    }

    private Entreprise ent() {
        return j.entreprise();
    }

    boolean enGuerre(int v) {
        int region = p.villes.region[v];
        int etat = region >= 0 && region < p.etats.regionEtat.length ? p.etats.regionEtat[region] : -1;
        return etat >= 0 && p.etats.ennemi[etat] >= 0;
    }

    // ================================================================ Chaque jour

    /** Appelé par Joueur.avancerBande (l'écran, toutes les deux secondes) : les jours écoulés depuis la dernière fois. */
    void avancer() {
        int jour = j.jour();
        if (dernierJour < 0) dernierJour = jour - 1;
        if (jour <= dernierJour) {
            appliquer();
            return;
        }
        int debut = Math.max(dernierJour + 1, jour - RATTRAPAGE);
        for (int d = debut; d <= jour; d++) {
            for (Entreprise.Atelier x : new ArrayList<>(ent().ateliers)) {
                EconomieVille.Agent a = ent().agent(x);
                if (a == null) continue;
                journee(x, a, j.marche().ville(x.ville, jour), d);
            }
            if (d % SEMAINE == 0) {
                caravanesJoueur(d);
                caravanesPays(d);
            }
        }
        dernierJour = jour;
        encheres(jour);
        chantiers(jour);
        j.enregistrer();
    }

    /** L'état des ouvriers se reporte sur l'atelier (fiabilité, grève) ; après un chargement, avant le premier jour. */
    private void appliquer() {
        for (Entreprise.Atelier x : ent().ateliers) {
            EconomieVille.Agent a = ent().agent(x);
            if (a == null) continue;
            equipe(x, a);
        }
    }

    private void journee(Entreprise.Atelier x, EconomieVille.Agent a, EconomieVille e, int d) {
        equipe(x, a);
        Random r = new Random(Travail.melanger(p.graine * 31L + x.ville * 7919L + x.lieu * 104729L + d * 15485863L));
        float cible = a.salaireJoueur < 0.9f ? LOYAUTE_BAS : a.salaireJoueur > 1.1f ? LOYAUTE_GENEREUX : LOYAUTE_NORMAL;
        if (a.impayes > 0) cible -= 25f;
        if (e.absent(a) && a.intendance <= 0f) cible -= 8f;
        for (Employe o : x.gens) {
            o.loyaute += (cible + (o.fiabilite - 1f) * 20f - o.loyaute) * 0.06f + (r.nextFloat() - 0.5f) * 2f;
            o.loyaute = Math.max(0f, Math.min(100f, o.loyaute));
            // Lot H1 : l'ouvrier apprend à l'ouvrage : sa fiche évolue et il travaille mieux.
            Fiche fx = x.greve ? null : j.gens().importants.get(o.graine);
            if (fx != null) {
                fx.apprendre(Emplois.competence(x.lieu), APPRENTISSAGE);
                o.fiabilite = fiabilite(fx, x.lieu);
            }
            float chance = VOL * (1f - o.honnetete) * (o.loyaute < 40f ? 2f : 1f) * (a.intendance > 0f ? 0.5f : 1f);
            if (!x.greve && a.caisse > 20.0 && r.nextFloat() < chance) {
                double somme = Math.min(a.caisse * 0.05, 8 + r.nextInt(30));
                a.caisse -= somme;
                a.pertes += somme;
                x.vole += somme;
                // On sait qui, une fois sur deux (plus souvent avec un intendant).
                if (r.nextFloat() < 0.5f + a.intendance / 200f) x.suspect = o.nom;
            }
        }
        float moyenne = loyaute(x);
        if (!x.greve && x.gens.size() >= GREVE_MIN && moyenne < GREVE) {
            x.greve = true;
            x.nouvelles.add("Tes ouvriers ont posé leurs outils : ils sont en grève.");
            j.annoncer("Grève dans " + Entreprise.possessif(Biens.nomBoutique(x.lieu)) + " " + Creation.de(ville(x.ville)) + " : tes ouvriers ont posé leurs outils.");
        } else if (x.greve && moyenne >= FIN_GREVE) {
            x.greve = false;
            x.nouvelles.add("La grève est finie : tes ouvriers ont repris le travail.");
        }
        if (x.greve && !x.gens.isEmpty() && r.nextFloat() < 0.08f) {
            // Un gréviste lassé s'en va pour de bon.
            Employe part = moinsLoyal(x);
            x.gens.remove(part);
            a.ouvriers = Math.max(0f, Math.round(a.ouvriers) - 1f);
            a.partis++;
        }
        a.greve = x.greve;
        // Les impôts du seigneur
        if (d % SEMAINE == 0) {
            double impot = TAILLE + DIME * e.ventesJour(a) * SEMAINE;
            if (enGuerre(x.ville)) impot *= IMPOT_GUERRE;
            a.caisse -= impot;
            e.bourse += impot;
            x.impots += impot;
        }
    }

    // ================================================================ Lot H5 : l'ouvrage fait sous les yeux du joueur

    /** Une heure d'ouvrage d'une journée d'atelier (HEURES_OUVRAGE heures). */
    public static final float HEURES_OUVRAGE = 8f;

    /**
     * Un ouvrier du joueur a travaillé « heures » heures sous ses yeux (l'écran le voit) : l'atelier fabrique cette part de journée
     * tout de suite. S'il est tout près, le joueur voit ce qui a été fait : « Miwyn : +0,4 fer ; −0,8 minerai de fer ». Rend le texte.
     */
    public String ouvrage(long id, float heures, boolean proche) {
        Entreprise.Atelier x = Emplois.employeur(j, id);
        if (x == null || x.greve) return "";
        EconomieVille.Agent a = ent().agent(x);
        if (a == null) return "";
        float[] avant = a.stock.clone();
        float u = j.marche().ville(x.ville, j.jour()).ouvrageVu(a, heures / HEURES_OUVRAGE);
        if (u <= 0f) {
            if (proche) j.annoncer(nomOuvrier(x, id) + " n'a rien à faire : il manque de la matière, ou la vitrine est pleine.");
            return "";
        }
        StringBuilder plus = new StringBuilder(), moins = new StringBuilder();
        for (int k = 0; k < avant.length; k++) {
            float d = a.stock[k] - avant[k];
            if (Math.abs(d) < 0.05f) continue;
            StringBuilder t = d > 0 ? plus : moins;
            t.append(t.length() == 0 ? "" : ", ").append(d > 0 ? "+" : "−").append(String.format(java.util.Locale.FRANCE, "%.1f ", Math.abs(d))).append(Biens.TOUS.get(k).nom);
        }
        String texte = nomOuvrier(x, id) + " : " + plus + (moins.length() > 0 ? " ; " + moins : "") + ".";
        if (proche) j.annoncer(texte);
        return texte;
    }

    private static String nomOuvrier(Entreprise.Atelier x, long id) {
        for (Employe o : x.gens) if (o.graine == id) return o.nom.split(" ")[0];
        return "Ton ouvrier";
    }

    /** Essai du lot H5 : l'atelier du joueur dans le bâtiment de ce métier du village vu, avec n ouvriers et de quoi travailler. */
    public String atelierIciPourEssai(int ville, int lieu, int n) {
        String t = equipePourEssai(ville, lieu, n);
        postePourEssai(ville, lieu, n);
        for (Entreprise.Atelier y : j.entreprise().ateliers) {
            if (y.ville != ville || y.lieu != lieu) continue;
            int[] bat = j.batimentsIci;
            y.batiment = bat != null && lieu < bat.length ? bat[lieu] : -1;
            EconomieVille.Agent a = ent().agent(y);
            if (a != null) for (Recettes.Recette r : a.recettes) for (Biens.Bien b : r.entrees) a.stock[b.id] += 30f;
            t += "batiment " + y.batiment + "\n";
        }
        return t;
    }

    /** Essai du lot H5 : le stock de l'atelier (ce qui n'est pas nul). */
    public String stockPourEssai(int ville, int lieu) {
        for (Entreprise.Atelier y : j.entreprise().ateliers) {
            if (y.ville != ville || y.lieu != lieu) continue;
            EconomieVille.Agent a = ent().agent(y);
            if (a == null) return "-";
            StringBuilder s = new StringBuilder();
            for (int k = 0; k < a.stock.length; k++) if (a.stock[k] > 0.05f) s.append(Biens.TOUS.get(k).nom).append(' ').append(String.format(java.util.Locale.FRANCE, "%.1f", a.stock[k])).append("; ");
            return s.append("fait sous les yeux aujourd'hui ").append(String.format(java.util.Locale.FRANCE, "%.2f", a.faitVu)).append(" journée").toString();
        }
        return "-";
    }

    /** Les ouvriers nommés suivent le nombre d'ouvriers de l'atelier : on en crée, ou le moins loyal s'en va. */
    void equipe(Entreprise.Atelier x, EconomieVille.Agent a) {
        int n = Math.max(0, Math.round(a.ouvriers));
        while (x.gens.size() < n) x.gens.add(nouveau(x));
        while (x.gens.size() > n) x.gens.remove(moinsLoyal(x));
        float f = 0f;
        for (Employe o : x.gens) f += o.fiabilite;
        a.fiabilite = x.gens.isEmpty() ? 1f : f / x.gens.size();
        a.greve = x.greve;
    }

    private Employe nouveau(Entreprise.Atelier x) {
        long g = Travail.melanger(p.graine * 131L + x.ville * 7919L + x.lieu * 104729L + (x.compteur++) * 15485863L);
        Employe o = new Employe();
        o.graine = g;
        o.femme = (g & 1L) == 1L;
        o.nom = Noms.personne(p.villes.peuple[x.ville], p.graine, g >>> 8, o.femme ? 1 : 0);
        // Lot G4 : l'ouvrier est un vrai habitant de la ville (la Foule), pas encore employé ailleurs chez le joueur.
        int q = p.villes.peuple[x.ville], r = p.villes.region[x.ville];
        Foule fl = q >= 0 && r >= 0 && q < p.foules.length && r < p.foules[q].length ? p.foules[q][r] : null;
        int meilleur = -1;
        for (int essai = 0; fl != null && essai < EMBAUCHE_ESSAIS; essai++) {
            int place = fl.habitantAuHasard(x.ville, p.annee, p.reglages.ageAdulte, Travail.melanger(g + essai * 977L));
            if (place < 0) break;
            long id = fl.idDe(place);
            if (id == j.identifiant() || embauche(id)) continue;
            // Lot H1 : on garde le plus habile pour ce métier.
            int c = j.gens().de(fl, place).competences[Emplois.competence(x.lieu)];
            if (c <= meilleur) continue;
            meilleur = c;
            o.graine = id;
            o.femme = fl.sexeDe(place) == 1;
            o.nom = Noms.personne(q, p.graine, id, fl.sexeDe(place));
        }
        o.fiabilite = 0.7f + ((g >>> 16) & 1023) / 1023f * 0.6f;
        o.honnetete = ((g >>> 26) & 1023) / 1023f;
        o.loyaute = LOYAUTE_DEPART + (((g >>> 36) & 1023) / 1023f - 0.5f) * 20f;
        // Lot H1 : un habitant réel apporte sa fiche, gardée désormais : sa compétence fait sa fiabilité, son honnêteté est la sienne.
        int ici = fl == null ? -1 : fl.placeDe(o.graine);
        if (ici >= 0) {
            Fiche fx = j.gens().retenir(fl, ici);
            o.fiabilite = fiabilite(fx, x.lieu);
            o.honnetete = (fx.axes[Fiche.HONNETETE] + 100) / 200f;
        }
        return o;
    }

    /** Lot H1 : l'effort d'une journée à l'atelier, pour la compétence de l'ouvrier ; sa fiabilité va de 0,7 (débutant) à 1,3 (compétence 150). */
    static final float APPRENTISSAGE = 4f;

    static float fiabilite(Fiche fx, int lieu) {
        return 0.7f + Math.min(1f, fx.competences[Emplois.competence(lieu)] / 150f) * 0.6f;
    }

    /** Lot G4 : on tire au plus EMBAUCHE_ESSAIS habitants pour en trouver un qui ne travaille pas déjà pour le joueur. */
    static final int EMBAUCHE_ESSAIS = 12;

    /** Lot G4 : cet habitant (id de la Foule) travaille-t-il déjà dans un des ateliers du joueur ? */
    private boolean embauche(long id) {
        for (Entreprise.Atelier y : j.entreprise().ateliers) {
            for (Employe o : y.gens) if (o.graine == id) return true;
        }
        return false;
    }

    /** Essai du lot H1 : l'atelier d'essai a son poste dans l'économie de la ville (n ouvriers, une caisse pleine), pour que les journées passent. */
    public void postePourEssai(int ville, int lieu, int n) {
        EconomieVille e = j.marche().ville(ville, j.jour());
        EconomieVille.Agent a = e.agentJoueur(lieu);
        if (a == null) a = e.ajouterJoueur(lieu);
        a.ouvriers = n;
        a.caisse = 100000.0;
        a.presenceJusqua = j.jour() + 1000;
    }

    /** Essai du lot G4 : embauche n ouvriers dans un atelier du joueur (créé s'il n'existe pas) et dit qui ils sont. */
    public String equipePourEssai(int ville, int lieu, int n) {
        Entreprise.Atelier x = null;
        for (Entreprise.Atelier y : j.entreprise().ateliers) if (y.ville == ville && y.lieu == lieu) x = y;
        if (x == null) {
            x = new Entreprise.Atelier();
            x.ville = ville;
            x.lieu = lieu;
            j.entreprise().ateliers.add(x);
        }
        StringBuilder t = new StringBuilder();
        Foule fl = p.foules[p.villes.peuple[ville]][p.villes.region[ville]];
        for (int k = 0; k < n; k++) {
            Employe o = nouveau(x);
            x.gens.add(o);
            int place = fl.placeDe(o.graine);
            t.append(o.nom).append(place >= 0 ? " (habitant, ville " + fl.villeDe(place) + ")" : " (inventé)").append(" — ").append(Emplois.metier(j, p, ville, o.graine)).append('\n');
        }
        return t.toString();
    }

    private static Employe moinsLoyal(Entreprise.Atelier x) {
        Employe m = x.gens.get(x.gens.size() - 1);
        for (Employe o : x.gens) if (o.loyaute < m.loyaute) m = o;
        return m;
    }

    public static float loyaute(Entreprise.Atelier x) {
        if (x.gens.isEmpty()) return LOYAUTE_DEPART;
        float s = 0f;
        for (Employe o : x.gens) s += o.loyaute;
        return s / x.gens.size();
    }

    private static String humeur(float loyaute) {
        return loyaute >= 70f ? "dévoué" : loyaute >= 50f ? "content" : loyaute >= 30f ? "grogne" : "en colère";
    }

    private static String fiable(float f) {
        return f >= 1.15f ? "travailleur" : f >= 0.9f ? "correct" : "lent";
    }

    // ================================================================ Dans la fenêtre de l'atelier

    /** Ajoute au texte de « Gérer l'atelier » : impôts, ouvriers, grève, vols, nouvelles des caravanes. */
    void rapport(Entreprise.Atelier x, EconomieVille.Agent a, StringBuilder t) {
        equipe(x, a);
        if (!x.gens.isEmpty()) t.append("\nTes ouvriers : ").append(humeur(loyaute(x))).append(" dans l'ensemble (loyauté ").append(Math.round(loyaute(x))).append(").");
        if (x.greve) t.append(" Ils sont en grève : l'atelier ne produit plus rien.");
        if (x.impots >= 1.0) t.append("\nLe seigneur a levé ").append(Fiche.argentTexte((int) Math.round(x.impots))).append(" de taille et de dîme").append(enGuerre(x.ville) ? " (la guerre coûte cher)" : "").append('.');
        if (x.vole >= 1.0) {
            t.append("\n").append(Fiche.argentTexte((int) Math.round(x.vole))).append(" ont disparu de la caisse.");
            if (!x.suspect.isEmpty()) t.append(" Tu soupçonnes ").append(x.suspect).append('.');
        }
        for (String n : x.nouvelles) t.append("\n").append(n);
        x.impots = 0.0;
        x.vole = 0.0;
        x.nouvelles.clear();
    }

    /** Les choix ajoutés à « Gérer l'atelier ». */
    void choix(Entreprise.Atelier x, EconomieVille.Agent a, Joueur.Scene s) {
        if (!x.gens.isEmpty()) s.ajouter("Tes ouvriers (" + x.gens.size() + ")…", () -> ouvriers(x, null));
        if (x.greve) s.ajouter("Parler aux grévistes…", () -> greve(x, null));
        if (j.villeIci() == x.ville && j.atelierDevant(x.lieu) != null && !a.recettes.isEmpty())
            s.ajouter("Travailler toi-même à l'atelier (à la main)", () -> j.travail().commencerAMain(x.lieu, Travail.PATRON));
        if (destinations(x).size() > 0 || sortantes(x) > 0) s.ajouter("Caravanes vers tes autres ateliers…", () -> caravanes(x, null));
    }

    Joueur.Scene ouvriers(Entreprise.Atelier x, String message) {
        EconomieVille.Agent a = ent().agent(x);
        if (a == null) return ent().accueil(null);
        equipe(x, a);
        Joueur.Scene s = new Joueur.Scene("", "");
        for (Employe o : x.gens) s.ajouter(o.nom + " — " + fiable(o.fiabilite) + ", " + humeur(o.loyaute) + (o.nom.equals(x.suspect) ? " (soupçonné" + (o.femme ? "e" : "") + ")" : ""), () -> employe(x, o, null));
        int toutes = PRIME * x.gens.size();
        if (x.gens.size() > 1) s.ajouter("Une prime pour tous (" + Fiche.argentTexte(toutes) + ")", () -> {
            Fiche f = j.fiche();
            if (f.argent < toutes) return ouvriers(x, "Tu n'as pas de quoi.");
            f.argent -= toutes;
            for (Employe o : x.gens) o.loyaute = Math.min(100f, o.loyaute + PRIME_LOYAUTE);
            j.gesteJoueur = "donner";
            j.enregistrer();
            return ouvriers(x, "Tu distribues une prime : on te remercie, on se remet à l'ouvrage avec plus de cœur.");
        });
        s.ajouter("« Revenir", () -> ent().gerer(x, null));
        Joueur.Scene vraie = new Joueur.Scene("Tes ouvriers", Joueur.avec(message, "Ceux qui travaillent pour toi. Un ouvrier mécontent travaille mal, vole plus volontiers, et un atelier en colère se met en grève."));
        vraie.choix.addAll(s.choix);
        vraie.suites.addAll(s.suites);
        return vraie;
    }

    private Joueur.Scene employe(Entreprise.Atelier x, Employe o, String message) {
        String il = o.femme ? "Elle" : "Il";
        StringBuilder t = new StringBuilder(o.nom).append(". ").append(il).append(" est ").append(fiable(o.fiabilite)).append(o.femme ? "e" : "").append(" à l'ouvrage et ").append(humeur(o.loyaute)).append('.');
        Fiche fx = j.gens().importants.get(o.graine);
        if (fx != null) t.append("\n").append(j.gens().portrait(fx));
        if (o.nom.equals(x.suspect)) t.append("\nC'est ").append(o.femme ? "elle" : "lui").append(" que tu soupçonnes d'avoir puisé dans la caisse.");
        Joueur.Scene s = new Joueur.Scene(o.nom, Joueur.avec(message, t.toString()));
        s.ajouter("Lui donner une prime (" + Fiche.argentTexte(PRIME) + ")", () -> {
            Fiche f = j.fiche();
            if (f.argent < PRIME) return employe(x, o, "Tu n'as pas de quoi.");
            f.argent -= PRIME;
            o.loyaute = Math.min(100f, o.loyaute + PRIME_LOYAUTE);
            j.gesteJoueur = "donner";
            j.enregistrer();
            return employe(x, o, il + " te remercie.");
        });
        s.ajouter("L" + (o.femme ? "a" : "e") + " renvoyer", () -> {
            EconomieVille.Agent a = ent().agent(x);
            x.gens.remove(o);
            if (a != null) a.ouvriers = Math.max(0f, Math.round(a.ouvriers) - 1f);
            boolean voleur = o.nom.equals(x.suspect);
            if (voleur) x.suspect = "";
            // Renvoyer un voleur se comprend ; renvoyer quelqu'un sans raison inquiète les autres.
            if (!voleur) for (Employe e : x.gens) e.loyaute = Math.max(0f, e.loyaute - 3f);
            j.renommee().gagner(x.ville, 0f, voleur ? 0.5f : -0.5f, null);
            j.enregistrer();
            return ouvriers(x, voleur ? "Tu renvoies " + o.nom + " pour vol. Les autres baissent les yeux." : "Tu renvoies " + o.nom + ". Les autres se regardent en silence.");
        });
        s.ajouter("« Revenir", () -> ouvriers(x, null));
        return s;
    }

    private Joueur.Scene greve(Entreprise.Atelier x, String message) {
        EconomieVille.Agent a = ent().agent(x);
        if (a == null || !x.greve) return ent().gerer(x, null);
        Fiche f = j.fiche();
        int n = x.gens.size();
        String t = "Tes ouvriers se tiennent devant l'atelier, les bras croisés. Leur porte-parole, " + (n > 0 ? x.gens.get(0).nom : "un ancien") + ", réclame un meilleur salaire et du respect.";
        Joueur.Scene s = new Joueur.Scene("La grève", Joueur.avec(message, t));
        int prime = 10 * n;
        s.ajouter("Céder : salaire généreux et une prime de " + Fiche.argentTexte(10) + " chacun (" + Fiche.argentTexte(prime) + ")", () -> {
            if (f.argent < prime) return greve(x, "Tu n'as pas de quoi payer la prime.");
            f.argent -= prime;
            a.salaireJoueur = Entreprise.SALAIRES[2];
            for (Employe o : x.gens) o.loyaute = Math.min(100f, o.loyaute + 30f);
            x.greve = false;
            a.greve = false;
            j.renommee().gagner(x.ville, 1f, 1f, null);
            j.enregistrer();
            return ent().gerer(x, "Tu cèdes. On reprend le travail en chantant.");
        });
        s.ajouter("Leur parler (Éloquence)", () -> {
            int jet = f.competences[Fiche.ELOQUENCE] / 2 + 5 * f.attributs[Fiche.PRESENCE] + new Random(Travail.melanger(x.compteur * 31L + j.jour())).nextInt(40);
            f.apprendre(Fiche.ELOQUENCE, 3f);
            if (jet >= 55) {
                for (Employe o : x.gens) o.loyaute = Math.min(100f, o.loyaute + 22f);
                x.greve = false;
                a.greve = false;
                j.enregistrer();
                return ent().gerer(x, "Tu trouves les mots. Ils grognent encore, mais retournent à l'ouvrage.");
            }
            for (Employe o : x.gens) o.loyaute = Math.max(0f, o.loyaute - 5f);
            j.enregistrer();
            return greve(x, "Tes paroles tombent à plat. On te tourne le dos.");
        });
        s.ajouter("Les renvoyer tous", () -> {
            x.gens.clear();
            a.ouvriers = 0f;
            x.greve = false;
            a.greve = false;
            j.renommee().gagner(x.ville, 0f, -3f, "Briseur de grève");
            j.enregistrer();
            return ent().gerer(x, "Tu les renvoies tous. L'atelier est vide ; la ville en parlera.");
        });
        s.ajouter("Tenir bon (attendre)", () -> ent().gerer(x, "Tu les laisses à leur colère. Certains finiront par partir."));
        return s;
    }

    // ================================================================ Caravanes entre ses ateliers

    private static final class Route {
        Entreprise.Atelier vers;
        int bien;
        float km;
    }

    /** Ce que cet atelier fabrique et qu'un autre de mes ateliers, dans une autre ville, achète. */
    private List<Route> destinations(Entreprise.Atelier x) {
        List<Route> l = new ArrayList<>();
        EconomieVille.Agent a = ent().agent(x);
        if (a == null) return l;
        for (Entreprise.Atelier y : ent().ateliers) {
            if (y == x || y.ville == x.ville) continue;
            EconomieVille.Agent b = ent().agent(y);
            if (b == null) continue;
            float km = p.km(x.ville, y.ville);
            if (km > CARAVANE_MAX_KM) continue;
            for (int k = 0; k < a.stock.length; k++) {
                if (!a.fabrique[k] || !b.achete[k] || b.fabrique[k]) continue;
                Route r = new Route();
                r.vers = y;
                r.bien = k;
                r.km = km;
                l.add(r);
            }
        }
        return l;
    }

    private int sortantes(Entreprise.Atelier x) {
        int n = 0;
        for (Caravane c : caravanes) if (c.deVille == x.ville && c.deLieu == x.lieu) n++;
        return n;
    }

    private Caravane caravane(Entreprise.Atelier x, Route r) {
        for (Caravane c : caravanes)
            if (c.deVille == x.ville && c.deLieu == x.lieu && c.versVille == r.vers.ville && c.versLieu == r.vers.lieu && c.bien == r.bien) return c;
        return null;
    }

    Joueur.Scene caravanes(Entreprise.Atelier x, String message) {
        EconomieVille.Agent a = ent().agent(x);
        if (a == null) return ent().accueil(null);
        Joueur.Scene s = new Joueur.Scene("", "");
        for (Route r : destinations(x)) {
            Caravane c = caravane(x, r);
            int transport = Math.max(1, Math.round(Math.max(1f, a.prix[r.bien]) * TRANSPORT_KM * r.km));
            s.ajouter((c != null ? "[en route] " : "[arrêtée] ") + Biens.get(r.bien).nom + " vers " + Entreprise.possessif(Biens.nomBoutique(r.vers.lieu)) + " " + Creation.de(ville(r.vers.ville))
                    + " (" + Math.round(r.km) + " km, " + Fiche.argentTexte(transport) + " par pièce)", () -> {
                        Caravane cc = caravane(x, r);
                        if (cc != null) caravanes.remove(cc);
                        else {
                            cc = new Caravane();
                            cc.deVille = x.ville;
                            cc.deLieu = x.lieu;
                            cc.versVille = r.vers.ville;
                            cc.versLieu = r.vers.lieu;
                            cc.bien = r.bien;
                            cc.prochaine = j.jour();
                            caravanes.add(cc);
                        }
                        j.enregistrer();
                        return caravanes(x, null);
                    });
        }
        s.ajouter("« Revenir", () -> ent().gerer(x, null));
        String t = "Chaque semaine, une caravane emporte ce dont ton autre atelier a besoin. Le transport se paie sur sa caisse, et les routes ne sont pas sûres.";
        Joueur.Scene vraie = new Joueur.Scene("Caravanes", Joueur.avec(message, t));
        vraie.choix.addAll(s.choix);
        vraie.suites.addAll(s.suites);
        return vraie;
    }

    private void caravanesJoueur(int d) {
        for (Caravane c : new ArrayList<>(caravanes)) {
            if (d < c.prochaine) continue;
            Entreprise.Atelier x = ent().atelier(c.deVille, c.deLieu), y = ent().atelier(c.versVille, c.versLieu);
            EconomieVille.Agent a = x == null ? null : ent().agent(x), b = y == null ? null : ent().agent(y);
            if (a == null || b == null) {
                caravanes.remove(c);
                continue;
            }
            c.prochaine = d + SEMAINE;
            float besoin = Math.max(0f, b.cible[c.bien] * 1.5f - Math.max(0f, b.stock[c.bien]));
            int q = (int) Math.floor(Math.min(Math.max(0f, a.stock[c.bien]), besoin));
            String bien = Biens.get(c.bien).nom;
            if (q < 1) {
                x.nouvelles.add("La caravane pour " + ville(c.versVille) + " est partie à vide : " + (besoin < 1f ? "on n'y manque pas de " : "tu n'as pas de ") + bien + ".");
                continue;
            }
            float km = p.km(c.deVille, c.versVille);
            double transport = q * Math.max(1f, a.prix[c.bien]) * TRANSPORT_KM * km;
            EconomieVille ev = j.marche().ville(c.versVille, j.jour());
            b.caisse -= transport;
            b.achats += transport;
            ev.bourse += transport;
            a.stock[c.bien] -= q;
            Random r = new Random(Travail.melanger(c.deVille * 7919L + c.versVille * 104729L + d * 31L + c.bien));
            float risque = PILLAGE * (enGuerre(c.versVille) || enGuerre(c.deVille) ? 2f : 1f);
            if (forcerPillage || r.nextFloat() < risque) {
                forcerPillage = false;
                String n = "Ta caravane pour " + ville(c.versVille) + " a été pillée sur la route ; elle portait " + Biens.get(c.bien).quantite(q) + ".";
                x.nouvelles.add(n);
                y.nouvelles.add("La caravane qui venait " + Creation.de(ville(c.deVille)) + " n'est jamais arrivée : pillée sur la route.");
                j.annoncer(n);
                continue;
            }
            b.ajouter(c.bien, q, (float) (a.cout[c.bien] + transport / q));
            x.nouvelles.add("Caravane partie : " + Biens.get(c.bien).quantite(q) + " pour " + ville(c.versVille) + ".");
            y.nouvelles.add("Caravane arrivée " + Creation.de(ville(c.deVille)) + " : " + Biens.get(c.bien).quantite(q) + " (transport " + Fiche.argentTexte((int) Math.round(transport)) + ").");
        }
    }

    // ================================================================ Caravanes du pays

    /** Chaque semaine, les marchands envoient leurs surplus vers une ville proche qui les fait venir de loin, plus cher. */
    private void caravanesPays(int d) {
        List<EconomieVille> actives = new ArrayList<>();
        for (EconomieVille e : j.marche().economies()) if (e.jour >= d - SEMAINE) actives.add(e);
        for (EconomieVille e1 : actives) {
            for (EconomieVille.Agent a1 : e1.agents) {
                if (a1.joueur) continue;
                for (int b = 0; b < a1.stock.length; b++) {
                    if (!a1.fabrique[b]) continue;
                    // Ce qui dépasse la réserve voulue part en caravane ; la commande pousse le producteur à fabriquer davantage.
                    float surplus = a1.stock[b] - a1.cible[b];
                    if (surplus < 1f) continue;
                    EconomieVille meilleure = null;
                    EconomieVille.Agent preneur = null;
                    float km = 0f;
                    for (EconomieVille e2 : actives) {
                        if (e2 == e1) continue;
                        float k = p.km(e1.ville, e2.ville);
                        if (k > CARAVANE_KM) continue;
                        float rendu = a1.prix[b] * EconomieVille.GROS * (1f + TRANSPORT_KM * k);
                        if (rendu >= e2.prixImport(b) * 0.95f) continue;
                        for (EconomieVille.Agent a2 : e2.agents) {
                            if (a2.importe[b] < 0.3f || (preneur != null && a2.importe[b] <= preneur.importe[b])) continue;
                            preneur = a2;
                            meilleure = e2;
                            km = k;
                        }
                    }
                    if (preneur == null) continue;
                    int q = (int) Math.floor(Math.min(surplus, preneur.importe[b] * SEMAINE));
                    if (q < 1) continue;
                    float gros = a1.prix[b] * EconomieVille.GROS, rendu = gros * (1f + TRANSPORT_KM * km);
                    a1.stock[b] -= q;
                    a1.caisse += q * (double) gros;
                    a1.demandeJour[b] += q * 0.5f;
                    preneur.ajouter(b, q, rendu);
                    preneur.caisse -= q * (double) rendu;
                    Convoi c = new Convoi();
                    c.de = e1.ville;
                    c.vers = meilleure.ville;
                    c.bien = b;
                    c.quantite = q;
                    c.jour = d;
                    convois.add(c);
                    if (convois.size() > 40) convois.remove(0);
                }
            }
        }
    }

    /** Le joueur entre en ville : les caravanes arrivées cette semaine font parler. */
    void entrer(int v) {
        for (Convoi c : convois) {
            if (c.vers != v || c.annonce || j.jour() - c.jour > SEMAINE) continue;
            c.annonce = true;
            j.annoncer("Une caravane " + Creation.de(ville(c.de)) + " est arrivée avec " + Biens.get(c.bien).quantite(c.quantite) + ".");
            // Lot F : les caravaniers racontent ce qu'on dit de toi là d'où ils viennent.
            j.renommee().porter(c.de, c.vers, 0.1f);
            return;
        }
    }

    // ================================================================ Enchères

    private void encheres(int jour) {
        // Les enchères finies
        for (Enchere x : new ArrayList<>(encheres)) {
            if (jour < x.fin) continue;
            encheres.remove(x);
            adjuger(x);
        }
        // Une nouvelle, dans la ville où l'on est
        int v = j.villeIci();
        if (v < 0) return;
        for (Enchere x : encheres) if (x.ville == v) return;
        Integer derniere = derniereEnchere.get(v);
        if (!forcerEnchere && derniere != null && jour - derniere < ENCHERE_REPOS) return;
        EconomieVille e = j.marche().ville(v, jour);
        EconomieVille.Agent choisi = null;
        int raison = FAILLITE;
        List<EconomieVille.Agent> possibles = new ArrayList<>();
        for (EconomieVille.Agent a : e.agents) {
            if (a.joueur || a.recettes.isEmpty() || a.lieu <= Metiers.MAISON || a.lieu >= Metiers.NOMBRE || ent().atelier(v, a.lieu) != null) continue;
            possibles.add(a);
            if (a.caisse < -a.credit() && choisi == null) choisi = a;
        }
        if (choisi == null) {
            Random r = new Random(Travail.melanger(p.graine * 17L + v * 7919L + jour * 104729L));
            if (possibles.isEmpty() || (!forcerEnchere && r.nextFloat() >= ENCHERE_CHANCE)) return;
            choisi = possibles.get(r.nextInt(possibles.size()));
            raison = SANS_HERITIER;
        }
        forcerEnchere = false;
        Random r = new Random(Travail.melanger(p.graine * 23L + v * 31L + jour));
        double valeur = e.valeur(choisi, Entreprise.prixBatiment(choisi.lieu), Entreprise.JOURS_BENEFICE);
        Enchere x = new Enchere();
        x.ville = v;
        x.lieu = choisi.lieu;
        x.fin = jour + ENCHERE_JOURS;
        x.prix = (int) Math.ceil(valeur * MISE_A_PRIX);
        x.plafond = (int) Math.round(valeur * (PLAFOND_MIN + r.nextFloat() * (PLAFOND_MAX - PLAFOND_MIN)));
        x.raison = raison;
        encheres.add(x);
        derniereEnchere.put(v, jour);
        j.annoncer("Le crieur annonce une vente aux enchères : " + Biens.nomBoutique(x.lieu).toLowerCase() + (raison == FAILLITE ? ", en faillite." : ", dont le maître est mort sans héritier.") + " Voir le tableau de la place.");
    }

    private int licence(int lieu) {
        return (j.travail().maitrises & (1L << lieu)) != 0L ? 0 : j.renommee().licence(lieu, Entreprise.LICENCE);
    }

    /** Les lignes du tableau de la place. */
    void tableau(Joueur.Scene s, int v) {
        for (Enchere x : encheres) {
            if (x.ville != v) continue;
            int reste = Math.max(0, x.fin - j.jour());
            s.ajouter("Enchère : " + Biens.nomBoutique(x.lieu).toLowerCase() + " — " + Fiche.argentTexte(x.prix) + (x.mene ? " (tu mènes)" : "") + ", fin dans " + reste + " jour" + (reste > 1 ? "s" : ""),
                    () -> enchere(x, null));
        }
    }

    private String rival(Enchere x) {
        long g = Travail.melanger(p.graine * 41L + x.ville * 7919L + x.lieu * 104729L + x.fin);
        return Noms.personne(p.villes.peuple[x.ville], p.graine, g >>> 8, (int) (g & 1L));
    }

    Joueur.Scene enchere(Enchere x, String message) {
        Fiche f = j.fiche();
        int reste = Math.max(0, x.fin - j.jour());
        int licence = licence(x.lieu);
        StringBuilder t = new StringBuilder();
        t.append(Biens.nomBoutique(x.lieu)).append(' ').append(x.raison == FAILLITE ? "n'a plus payé ses dettes : ses créanciers la font vendre" : "a perdu son maître, mort sans héritier : la ville la vend")
                .append(", avec ses ouvriers et ce qui reste en réserve. Les dettes ne suivent pas.");
        t.append("\n").append(x.mise ? (x.mene ? "Tu mènes, à " : rival(x) + ", un marchand, mène à ") : "Mise à prix : ").append(Fiche.argentTexte(x.prix)).append('.');
        t.append(" Adjugé dans ").append(reste).append(" jour").append(reste > 1 ? "s" : "").append(" à qui mènera.");
        if (licence > 0) t.append(" La guilde demande en plus ").append(Fiche.argentTexte(licence)).append(" à qui n'est pas maître du métier.");
        t.append("\nTu as ").append(Fiche.argentTexte(f.argent)).append(". On ne paie qu'à la fin.");
        Joueur.Scene s = new Joueur.Scene("Vente aux enchères", Joueur.avec(message, t.toString()));
        if (!x.mene) {
            int petite = x.mise ? (int) Math.ceil(x.prix * (1f + PAS)) : x.prix;
            int grosse = (int) Math.ceil(x.prix * (1f + 3f * PAS));
            for (int m : new int[] { petite, grosse }) {
                if (m + licence > f.argent) continue;
                s.ajouter("Miser " + Fiche.argentTexte(m), () -> miser(x, m));
            }
        }
        s.ajouter("« Revenir", () -> j.missions().tableau(null));
        return s;
    }

    private Joueur.Scene miser(Enchere x, int mise) {
        x.mise = true;
        x.prix = mise;
        String r = rival(x);
        if (mise < x.plafond) {
            x.prix = Math.min(x.plafond, (int) Math.ceil(mise * (1f + PAS)));
            x.mene = false;
            j.enregistrer();
            return enchere(x, r + " lève la main : " + Fiche.argentTexte(x.prix) + ".");
        }
        x.mene = true;
        j.gesteJoueur = "saluer_main";
        j.enregistrer();
        return enchere(x, r + " secoue la tête et range sa bourse. Personne ne suit : tu mènes.");
    }

    private void adjuger(Enchere x) {
        String nom = Biens.nomBoutique(x.lieu).toLowerCase() + " " + Creation.de(ville(x.ville));
        EconomieVille e = j.marche().ville(x.ville, j.jour());
        EconomieVille.Agent a = e.agent(x.lieu);
        if (a == null || a.joueur) return;
        if (!x.mene) {
            if (x.mise) j.annoncer("Adjugé : " + nom + " va à " + rival(x) + " pour " + Fiche.argentTexte(x.prix) + ".");
            a.caisse = Math.max(a.caisse, a.reserve() * 0.5);
            return;
        }
        Fiche f = j.fiche();
        int total = x.prix + licence(x.lieu);
        if (f.argent < total) {
            j.annoncer("Adjugé à toi, mais tu ne peux pas payer : " + nom + " va au suivant. On ne l'oubliera pas.");
            j.renommee().gagner(x.ville, -2f, -2f, null);
            a.caisse = Math.max(a.caisse, a.reserve() * 0.5);
            return;
        }
        f.argent -= total;
        e.bourse += x.prix;
        EconomieVille.Agent mien = e.racheter(a);
        mien.caisse = Math.max(0.0, mien.caisse);
        mien.ouvriers = Math.max(1f, mien.ouvriers);
        ent().prendre(x.ville, x.lieu, -1, "En l'an " + p.annee + ", tu emportes aux enchères " + nom + ".");
        j.annoncer("Adjugé ! " + Character.toUpperCase(nom.charAt(0)) + nom.substring(1) + " est à toi pour " + Fiche.argentTexte(x.prix) + ".");
    }

    // ================================================================ Bâtir sur une ruine

    /** Devant une ruine (M) : quel atelier y bâtir. */
    Joueur.Scene ruine(String message) {
        Fiche f = j.fiche();
        int v = j.villeIci(), q = j.ruineProche;
        if (v < 0 || q < 0 || j.ruineDistance > RUINE_PROCHE) return j.accueilScene();
        for (Chantier c : chantiers) if (c.ville == v && c.batiment == q) {
            Joueur.Scene s = new Joueur.Scene("Ton chantier", "Les maçons sont à l'ouvrage. " + Biens.nomBoutique(c.lieu) + " sera prêt" + (Biens.nomBoutique(c.lieu).startsWith("La") ? "e" : "") + " dans "
                    + Math.max(1, c.fin - j.jour()) + " jours.");
            j.quotidien().choixChantier(s, c);
            s.ajouter("« Revenir", j::accueilScene);
            return s;
        }
        StringBuilder t = new StringBuilder("Des murs noircis, un toit effondré. Le terrain n'appartient plus à personne : le seigneur le cède à qui y bâtit.");
        t.append("\nTu as ").append(Fiche.argentTexte(f.argent)).append(". Le chantier dure ").append(CHANTIER_JOURS).append(" jours.");
        Joueur.Scene s = new Joueur.Scene("", "");
        for (int m = Metiers.MAISON + 1; m < Metiers.NOMBRE; m++) {
            if (Recettes.de(m).isEmpty() || ent().atelier(v, m) != null) continue;
            int prix = Math.round(Entreprise.prixBatiment(m) * RUINE) + licence(m);
            if (prix > f.argent) continue;
            int lieu = m;
            s.ajouter("Bâtir " + Biens.nomBoutique(m).toLowerCase() + " (" + Fiche.argentTexte(prix) + ")", () -> batir(v, q, lieu, prix));
        }
        if (s.choix.isEmpty()) t.append("\nTu n'as pas de quoi bâtir quoi que ce soit.");
        j.quotidien().choixRuine(s, v);
        s.ajouter("« Revenir", j::accueilScene);
        Joueur.Scene vraie = new Joueur.Scene("Une ruine", Joueur.avec(message, t.toString()));
        vraie.choix.addAll(s.choix);
        vraie.suites.addAll(s.suites);
        return vraie;
    }

    private Joueur.Scene batir(int v, int q, int lieu, int prix) {
        Fiche f = j.fiche();
        if (f.argent < prix) return ruine("Tu n'as plus de quoi.");
        f.argent -= prix;
        j.marche().ville(v, j.jour()).bourse += prix;
        Chantier c = new Chantier();
        c.ville = v;
        c.batiment = q;
        c.lieu = lieu;
        c.fin = j.jour() + CHANTIER_JOURS;
        chantiers.add(c);
        j.gesteJoueur = "donner";
        j.noter("Tu fais bâtir " + Biens.nomBoutique(lieu).toLowerCase() + " sur une ruine " + Creation.de(ville(v)) + ".");
        j.enregistrer();
        return ruine("Marché conclu. Les maçons arrivent demain ; reviens dans " + CHANTIER_JOURS + " jours.");
    }

    // ================================================================ Lot G5 : bâtir sur un terrain libre

    /** Devant un terrain libre (M) : quel atelier y bâtir. */
    Joueur.Scene terrain(String message) {
        Fiche f = j.fiche();
        double[] t = j.terrainLibre;
        int v = j.villeIci();
        if (t == null || v < 0 || (int) t[0] != v) return j.accueilScene();
        long masque = (long) t[4];
        StringBuilder tx = new StringBuilder("Un terrain nu, assez grand pour un atelier. Le seigneur le cède à qui y bâtit, contre le prix du terrain et la licence.");
        tx.append("\nTu as ").append(Fiche.argentTexte(f.argent)).append(". Le chantier dure ").append(CHANTIER_JOURS).append(" jours.");
        Joueur.Scene s = new Joueur.Scene("", "");
        int tropCher = 0;
        for (int m = Metiers.MAISON + 1; m < Metiers.NOMBRE; m++) {
            if ((masque & (1L << m)) == 0L || Recettes.de(m).isEmpty() || ent().atelier(v, m) != null || enConstruction(v, m)) continue;
            int prix = prixTerrain(m);
            if (prix > f.argent) {
                tropCher++;
                continue;
            }
            int lieu = m;
            float a = (float) t[1], b = (float) t[2], angle = (float) t[3];
            s.ajouter("Bâtir " + Biens.nomBoutique(m).toLowerCase() + " ici (" + Fiche.argentTexte(prix) + ")", () -> batirTerrain(v, a, b, angle, lieu, prix));
        }
        if (s.choix.isEmpty()) tx.append(tropCher > 0 ? "\nTu n'as pas de quoi bâtir ici." : "\nRien de ce que tu pourrais bâtir ne tient ici : essaie plus loin, ou tourne-toi.");
        s.ajouter("« Revenir", j::accueilScene);
        Joueur.Scene vraie = new Joueur.Scene("Un terrain libre", Joueur.avec(message, tx.toString()));
        vraie.choix.addAll(s.choix);
        vraie.suites.addAll(s.suites);
        return vraie;
    }

    int prixTerrain(int m) {
        return Math.round(Entreprise.prixBatiment(m) * TERRAIN) + licence(m);
    }

    private boolean enConstruction(int v, int m) {
        for (Construction c : constructions) if (c.ville == v && c.lieu == m && !c.fini) return true;
        return false;
    }

    private Joueur.Scene batirTerrain(int v, float a, float b, float angle, int lieu, int prix) {
        Fiche f = j.fiche();
        if (f.argent < prix) return terrain("Tu n'as plus de quoi.");
        f.argent -= prix;
        j.marche().ville(v, j.jour()).bourse += prix;
        Construction c = new Construction();
        c.ville = v;
        c.lieu = lieu;
        c.a = a;
        c.b = b;
        c.angle = angle;
        c.fin = j.jour() + CHANTIER_JOURS;
        constructions.add(c);
        j.gesteJoueur = "donner";
        j.noter("Tu fais bâtir " + Biens.nomBoutique(lieu).toLowerCase() + " sur un terrain libre " + Creation.de(ville(v)) + ".");
        publier();
        j.enregistrer();
        Joueur.Scene s = new Joueur.Scene("Ton chantier", "Marché conclu. Les maçons tracent les murs devant toi ; " + Biens.nomBoutique(lieu).toLowerCase() + " sera prêt"
                + (Biens.nomBoutique(lieu).startsWith("La") ? "e" : "") + " dans " + CHANTIER_JOURS + " jours.");
        s.ajouter("« Revenir", j::accueilScene);
        return s;
    }

    void ecrireConstructions(java.io.DataOutputStream s) throws IOException {
        s.writeInt(constructions.size());
        for (Construction c : constructions) {
            s.writeInt(c.ville);
            s.writeInt(c.lieu);
            s.writeInt(c.fin);
            s.writeFloat(c.a);
            s.writeFloat(c.b);
            s.writeFloat(c.angle);
            s.writeBoolean(c.fini);
        }
    }

    void lireConstructions(java.io.DataInputStream s) throws IOException {
        constructions.clear();
        int n = s.readInt();
        for (int k = 0; k < n; k++) {
            Construction c = new Construction();
            c.ville = s.readInt();
            c.lieu = s.readInt();
            c.fin = s.readInt();
            c.a = s.readFloat();
            c.b = s.readFloat();
            c.angle = s.readFloat();
            c.fini = s.readBoolean();
            constructions.add(c);
        }
    }

    /** Essais du lot G5. */
    public Joueur.Scene terrainPourEssai() {
        return terrain(null);
    }

    public void finirConstructionsPourEssai() {
        for (Construction c : constructions) c.fin = Math.min(c.fin, j.jour());
        chantiers(j.jour());
    }

    private void chantiers(int jour) {
        for (Chantier c : new ArrayList<>(chantiers)) {
            if (jour < c.fin) continue;
            chantiers.remove(c);
            EconomieVille e = j.marche().ville(c.ville, jour);
            EconomieVille.Agent a = e.agentJoueur(c.lieu);
            if (a == null) a = e.ajouterJoueur(c.lieu);
            a.presenceJusqua = jour + Entreprise.PRESENCE_JOURS;
            Entreprise.Atelier x = ent().prendre(c.ville, c.lieu, c.batiment, "En l'an " + p.annee + ", tu bâtis " + Biens.nomBoutique(c.lieu).toLowerCase() + " " + Creation.de(ville(c.ville)) + " sur une ruine.");
            x.bati = true;
            j.annoncer("Le chantier est fini : " + Entreprise.possessif(Biens.nomBoutique(c.lieu)) + " " + Creation.de(ville(c.ville)) + " t'attend. Embauche, et mets de l'argent dans la caisse.");
        }
        // Lot G5 : les ateliers bâtis sur un terrain libre.
        for (Construction c : constructions) {
            if (c.fini || jour < c.fin) continue;
            c.fini = true;
            EconomieVille e = j.marche().ville(c.ville, jour);
            EconomieVille.Agent a = e.agentJoueur(c.lieu);
            if (a == null) a = e.ajouterJoueur(c.lieu);
            a.presenceJusqua = jour + Entreprise.PRESENCE_JOURS;
            Entreprise.Atelier x = ent().prendre(c.ville, c.lieu, -1, "En l'an " + p.annee + ", tu bâtis " + Biens.nomBoutique(c.lieu).toLowerCase() + " " + Creation.de(ville(c.ville)) + " sur un terrain libre.");
            x.bati = true;
            j.annoncer("Le chantier est fini : " + Entreprise.possessif(Biens.nomBoutique(c.lieu)) + " " + Creation.de(ville(c.ville)) + " est debout. Embauche, et mets de l'argent dans la caisse.");
        }
        publier();
    }

    /** Pour l'écran : les ateliers bâtis sur des ruines (ville, bâtiment, métier) par trois. */
    void publier() {
        List<Entreprise.Atelier> l = new ArrayList<>();
        for (Entreprise.Atelier x : ent().ateliers) if (x.bati && x.batiment >= 0) l.add(x);
        List<int[]> maisons = j.missions().quetes.rebaties;
        int[] t = new int[(l.size() + maisons.size()) * 3];
        for (int k = 0; k < l.size(); k++) {
            t[k * 3] = l.get(k).ville;
            t[k * 3 + 1] = l.get(k).batiment;
            t[k * 3 + 2] = l.get(k).lieu;
        }
        for (int k = 0; k < maisons.size(); k++) {
            int i = (l.size() + k) * 3;
            t[i] = maisons.get(k)[0];
            t[i + 1] = maisons.get(k)[1];
            t[i + 2] = com.deldaran.politics.Metiers.MAISON;
        }
        j.batisVue = t;
        // Lot G5 : les bâtiments sur terrain libre ; l'écran refait le village quand la liste change.
        float[] c = new float[constructions.size() * 6];
        for (int k = 0; k < constructions.size(); k++) {
            Construction x = constructions.get(k);
            c[k * 6] = x.ville;
            c[k * 6 + 1] = x.a;
            c[k * 6 + 2] = x.b;
            c[k * 6 + 3] = x.angle;
            c[k * 6 + 4] = x.lieu;
            c[k * 6 + 5] = x.fini ? 0f : 1f;
        }
        if (!java.util.Arrays.equals(c, j.constructionsVue)) {
            j.constructionsVue = c;
            j.constructionsVersion++;
        }
    }

    /** Pour les essais. */
    public Joueur.Scene ruinePourEssai() {
        return ruine(null);
    }

    public Joueur.Scene encherePourEssai(Enchere x) {
        return enchere(x, null);
    }

    public void avancerPourEssai() {
        avancer();
    }

    /** Pour les essais : un atelier du joueur dans la ville vivante la plus proche, qui achète ce que fabrique x, et la caravane entre les deux. */
    public String caravanePourEssai(Entreprise.Atelier x) {
        EconomieVille.Agent a = ent().agent(x);
        int meilleure = -1;
        float dm = Float.MAX_VALUE;
        for (int v = 0; v < p.villes.nombre; v++) {
            if (v == x.ville || !p.villes.estVivante(v)) continue;
            float k = p.km(x.ville, v);
            if (k < dm) {
                dm = k;
                meilleure = v;
            }
        }
        if (a == null || meilleure < 0) return "pas d'autre ville";
        EconomieVille e2 = j.marche().ville(meilleure, j.jour());
        for (int m = Metiers.MAISON + 1; m < Metiers.NOMBRE; m++) {
            if (m == x.lieu || Recettes.de(m).isEmpty() || ent().atelier(meilleure, m) != null || e2.agentJoueur(m) != null) continue;
            EconomieVille.Agent b = e2.ajouterJoueur(m);
            int bien = -1;
            for (int k = 0; k < b.stock.length && bien < 0; k++) if (a.fabrique[k] && b.achete[k] && !b.fabrique[k]) bien = k;
            if (bien < 0) {
                e2.agents.remove(b);
                continue;
            }
            b.ouvriers = 2f;
            b.caisse = 500.0;
            a.stock[bien] = Math.max(a.stock[bien], 30f);
            ent().prendre(meilleure, m, -1, "Tu ouvres " + Biens.nomBoutique(m).toLowerCase() + " " + Creation.de(ville(meilleure)) + ".");
            Caravane c = new Caravane();
            c.deVille = x.ville;
            c.deLieu = x.lieu;
            c.versVille = meilleure;
            c.versLieu = m;
            c.bien = bien;
            c.prochaine = j.jour();
            caravanes.add(c);
            return Biens.nomBoutique(m) + " " + Creation.de(ville(meilleure)) + " à " + Math.round(dm) + " km, bien " + Biens.get(bien).nom;
        }
        return "aucun métier ne convient";
    }

    // ================================================================ Sauvegarde

    void ecrire(DataOutputStream s) throws IOException {
        s.writeInt(dernierJour);
        s.writeInt(encheres.size());
        for (Enchere x : encheres) {
            s.writeInt(x.ville);
            s.writeInt(x.lieu);
            s.writeInt(x.fin);
            s.writeInt(x.prix);
            s.writeInt(x.plafond);
            s.writeInt(x.raison);
            s.writeBoolean(x.mise);
            s.writeBoolean(x.mene);
        }
        s.writeInt(chantiers.size());
        for (Chantier c : chantiers) {
            s.writeInt(c.ville);
            s.writeInt(c.batiment);
            s.writeInt(c.lieu);
            s.writeInt(c.fin);
        }
        s.writeInt(caravanes.size());
        for (Caravane c : caravanes) {
            s.writeInt(c.deVille);
            s.writeInt(c.deLieu);
            s.writeInt(c.versVille);
            s.writeInt(c.versLieu);
            s.writeInt(c.bien);
            s.writeInt(c.prochaine);
        }
        s.writeInt(derniereEnchere.size());
        for (Map.Entry<Integer, Integer> e : derniereEnchere.entrySet()) {
            s.writeInt(e.getKey());
            s.writeInt(e.getValue());
        }
    }

    void lire(DataInputStream s) throws IOException {
        dernierJour = s.readInt();
        int n = s.readInt();
        for (int k = 0; k < n; k++) {
            Enchere x = new Enchere();
            x.ville = s.readInt();
            x.lieu = s.readInt();
            x.fin = s.readInt();
            x.prix = s.readInt();
            x.plafond = s.readInt();
            x.raison = s.readInt();
            x.mise = s.readBoolean();
            x.mene = s.readBoolean();
            encheres.add(x);
        }
        n = s.readInt();
        for (int k = 0; k < n; k++) {
            Chantier c = new Chantier();
            c.ville = s.readInt();
            c.batiment = s.readInt();
            c.lieu = s.readInt();
            c.fin = s.readInt();
            chantiers.add(c);
        }
        n = s.readInt();
        for (int k = 0; k < n; k++) {
            Caravane c = new Caravane();
            c.deVille = s.readInt();
            c.deLieu = s.readInt();
            c.versVille = s.readInt();
            c.versLieu = s.readInt();
            c.bien = s.readInt();
            c.prochaine = s.readInt();
            caravanes.add(c);
        }
        n = s.readInt();
        for (int k = 0; k < n; k++) derniereEnchere.put(s.readInt(), s.readInt());
    }
}
