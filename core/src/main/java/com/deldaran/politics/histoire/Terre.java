package com.deldaran.politics.histoire;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Phase 11 (lot 11a) : la terre.
 * - le CALENDRIER : quatre saisons de JOURS_PAR_SAISON jours ; une année de jeu dure JOURS_PAR_AN jours ;
 * - les FERMES : dans chaque ville, on achète ou on loue des lopins hors des murs (M devant « les fermes et les bois ») ;
 * - les CULTURES : labourer, semer (au printemps et en été), entretenir, récolter ; le gel de l'hiver tue ce qui reste au champ ;
 * - la RÉSERVE de la ferme, petite sans grange ; la GRANGE se bâtit sur un lopin acheté ;
 * - les BÊTES au pré : poules (œufs), chèvres (fromage), moutons (laine au printemps), porcs ; il faut du grain l'hiver ;
 * - les JOURNALIERS : payés à la journée, ils font tout le travail d'un lopin quand tu n'es pas là.
 */
public final class Terre {
    public static final int JOURS_PAR_SAISON = 21, JOURS_PAR_AN = 4 * JOURS_PAR_SAISON;
    public static final int PRINTEMPS = 0, ETE = 1, AUTOMNE = 2, HIVER = 3;
    public static final String[] SAISONS = { "printemps", "été", "automne", "hiver" };

    /** Un lopin : prix d'achat, loyer par saison ; au plus LOPINS_MAX par ville. */
    public static final int PRIX_LOPIN = 400, LOYER_LOPIN = 50, LOPINS_MAX = 12;
    /** La grange : prix et planches ; capacité de la réserve sans et avec grange (en pièces). */
    public static final int PRIX_GRANGE = 250, PLANCHES_GRANGE = 20, RESERVE_SANS = 30, RESERVE_GRANGE = 300;
    /** Un journalier coûte SALAIRE cuivre par jour. */
    public static final int SALAIRE = 5;
    /** Heures de travail : labourer, semer, entretenir, récolter. */
    public static final float H_LABOUR = 4f, H_SEMIS = 1f, H_SOIN = 1f, H_RECOLTE = 3f;
    /** Bêtes : sans grange, au plus BETES_SANS ; l'hiver, un sac de grain par semaine pour BETES_PAR_SAC bêtes. */
    public static final int BETES_SANS = 4, BETES_PAR_SAC = 4;

    public static final int FRICHE = 0, LABOURE = 1, SEME = 2, MUR = 3;
    public static final String[] ETATS = { "en friche", "labouré", "semé", "mûr" };

    /** Les cultures : la semence, ce qu'elle rend, les jours pour mûrir. */
    public static final class Culture {
        public final String nom;
        public final Biens.Bien semence, recolte;
        public final int rendement, jours;

        Culture(String nom, Biens.Bien semence, Biens.Bien recolte, int rendement, int jours) {
            this.nom = nom;
            this.semence = semence;
            this.recolte = recolte;
            this.rendement = rendement;
            this.jours = jours;
        }
    }

    public static final Culture[] CULTURES = { new Culture("blé", Biens.GRAIN, Biens.GRAIN, 5, 14), new Culture("légumes", Biens.LEGUMES, Biens.LEGUMES, 5, 8) };

    public static final class Lopin {
        public boolean loue, journalier;
        public int etat, culture = -1, seme, soinJour = -1;
        public float soin;
    }

    public static final class Ferme {
        public int ville;
        public boolean grange;
        public final List<Lopin> lopins = new ArrayList<>();
        public int[] reserve = new int[Biens.TOUS.size()];
        public int[] betes = new int[Biens.TOUS.size()];

        public int occupe() {
            int n = 0;
            for (int b = 0; b < reserve.length; b++) n += reserve[b];
            return n;
        }

        public int nombreBetes() {
            int n = 0;
            for (int b = 0; b < betes.length; b++) n += betes[b];
            return n;
        }

        int capacite() {
            return grange ? RESERVE_GRANGE : RESERVE_SANS;
        }
    }

    private final Joueur j;
    private final Population p;
    public final List<Ferme> fermes = new ArrayList<>();
    public int recoltes;
    private int dernierJour = -1;

    /** Phase 11 : après une régence, on ne rattrape pas les années sautées. */
    void sauter() {
        dernierJour = j.jourMaintenant();
    }

    Terre(Joueur j, Population p) {
        this.j = j;
        this.p = p;
    }

    // ================================================================ Calendrier

    public static int saison(int jour) {
        return Math.floorMod(jour / JOURS_PAR_SAISON, 4);
    }

    public static int annee(Population p, int jour) {
        return p.annee + Math.floorDiv(jour, JOURS_PAR_AN);
    }

    /** « 12e jour du printemps, an 1324 ». */
    public static String date(Population p, int jour) {
        int d = Math.floorMod(jour, JOURS_PAR_SAISON) + 1;
        return (d == 1 ? "1er" : d + "e") + " jour " + (saison(jour) == PRINTEMPS ? "du " : "de l'") + SAISONS[saison(jour)] + ", an " + annee(p, jour);
    }

    private String ville(int v) {
        return Noms.ville(p.villes.peuple[v], p.graine, v);
    }

    public Ferme ferme(int v) {
        for (Ferme f : fermes) if (f.ville == v) return f;
        return null;
    }

    public int lopins() {
        int n = 0;
        for (Ferme f : fermes) n += f.lopins.size();
        return n;
    }

    public int journaliers() {
        int n = 0;
        for (Ferme f : fermes) for (Lopin l : f.lopins) if (l.journalier) n++;
        return n;
    }

    public int betes() {
        int n = 0;
        for (Ferme f : fermes) n += f.nombreBetes();
        return n;
    }

    private static boolean semis(int saison) {
        return saison == PRINTEMPS || saison == ETE;
    }

    /** Range dans la réserve ce qui tient ; renvoie ce qui a été perdu faute de place. */
    private static int ranger(Ferme f, int bien, int n) {
        int place = Math.max(0, f.capacite() - f.occupe());
        int q = Math.min(place, n);
        f.reserve[bien] += q;
        return n - q;
    }

    private int rendement(Lopin l) {
        Culture c = CULTURES[l.culture];
        Fiche f = j.fiche();
        float m = (0.6f + 0.4f * l.soin) * (1f + f.competences[Fiche.LABEUR] / 300f);
        return Math.max(1, Math.round(c.rendement * m));
    }

    // ================================================================ Chaque jour

    void avancer() {
        Fiche f = j.fiche();
        if (f == null) return;
        int jour = j.jourMaintenant();
        if (dernierJour < 0) dernierJour = jour;
        int debut = Math.max(dernierJour, jour - 60);
        for (int d = debut + 1; d <= jour; d++) journee(d);
        if (jour != dernierJour) j.enregistrer();
        dernierJour = Math.max(dernierJour, jour);
    }

    private void journee(int d) {
        Fiche f = j.fiche();
        int s = saison(d);
        boolean nouvelle = Math.floorMod(d, JOURS_PAR_SAISON) == 0;
        if (nouvelle) j.annoncer("Voici " + (s == PRINTEMPS ? "le " : "l'") + SAISONS[s] + " de l'an " + annee(p, d) + (s == PRINTEMPS ? " : le temps des semailles." : s == AUTOMNE ? " : il faut rentrer les récoltes avant le gel." : s == HIVER ? " : le gel prend les champs." : "."));
        for (Ferme fe : fermes) {
            String ici = ville(fe.ville);
            // Le loyer des lopins loués, à chaque saison.
            if (nouvelle) {
                for (int k = fe.lopins.size() - 1; k >= 0; k--) {
                    Lopin l = fe.lopins.get(k);
                    if (!l.loue) continue;
                    if (f.argent >= LOYER_LOPIN) {
                        f.argent -= LOYER_LOPIN;
                        j.marche().ville(fe.ville, j.jour()).bourse += LOYER_LOPIN;
                    } else {
                        fe.lopins.remove(k);
                        j.annoncer("Tu n'as pas payé le loyer : le propriétaire reprend ton lopin " + Creation.de(ici) + ".");
                    }
                }
            }
            for (Lopin l : fe.lopins) {
                // Le journalier.
                if (l.journalier) {
                    if (f.argent < SALAIRE) {
                        l.journalier = false;
                        j.annoncer("Ton journalier " + Creation.de(ici) + " s'en va : tu n'as plus de quoi le payer.");
                    } else {
                        f.argent -= SALAIRE;
                        travailJournalier(fe, l, s, d);
                    }
                }
                if (l.etat == SEME && d - l.seme >= CULTURES[l.culture].jours) {
                    l.etat = MUR;
                    j.annoncer("Ton " + CULTURES[l.culture].nom.replace("légumes", "potager") + " est mûr à " + ici + " : il faut récolter.");
                }
                if (nouvelle && s == HIVER && (l.etat == SEME || l.etat == MUR)) {
                    j.annoncer("Le gel a pris ce qui restait dans ton lopin " + Creation.de(ici) + ".");
                    l.etat = FRICHE;
                    l.culture = -1;
                }
                if (nouvelle && s == HIVER && l.etat == LABOURE) l.etat = FRICHE;
                l.soin = Math.max(0f, l.soin - 0.05f);
            }
            betes(fe, s, d, nouvelle, ici);
        }
    }

    private void travailJournalier(Ferme fe, Lopin l, int s, int d) {
        if (l.etat == FRICHE && s != HIVER) l.etat = LABOURE;
        else if (l.etat == LABOURE && semis(s)) {
            for (int c = 0; c < CULTURES.length; c++) {
                Biens.Bien b = CULTURES[c].semence;
                if (fe.reserve[b.id] <= 0) continue;
                fe.reserve[b.id]--;
                l.etat = SEME;
                l.culture = c;
                l.seme = d;
                l.soin = 0.5f;
                break;
            }
        } else if (l.etat == SEME) l.soin = 1f;
        else if (l.etat == MUR) {
            Culture c = CULTURES[l.culture];
            int q = Math.max(1, Math.round(c.rendement * (0.6f + 0.4f * l.soin)));
            int perdu = ranger(fe, c.recolte.id, q);
            recoltes++;
            j.annoncer(q - perdu <= 0 ? "Ton journalier " + Creation.de(ville(fe.ville)) + " n'a rien pu rentrer : la réserve est pleine, la récolte pourrit."
                    : "Ton journalier " + Creation.de(ville(fe.ville)) + " a rentré " + c.recolte.quantite(q - perdu) + (perdu > 0 ? " (" + perdu + " perdus : la réserve est pleine)" : "") + ".");
            l.etat = FRICHE;
            l.culture = -1;
        }
    }

    private void betes(Ferme fe, int s, int d, boolean nouvelle, String ici) {
        if (fe.nombreBetes() == 0) return;
        int perdu = 0;
        // Une poule pond presque un œuf par jour (deux fois moins l'hiver) : une douzaine tous les quatre jours pour trois poules.
        int oeufs = (fe.betes[Biens.POULE.id] + 2) / 3;
        if (fe.betes[Biens.POULE.id] > 0 && Math.floorMod(d, s == HIVER ? 8 : 4) == 0) perdu += ranger(fe, Biens.OEUFS.id, oeufs);
        if (Math.floorMod(d, 4) == 0 && fe.betes[Biens.CHEVRE.id] > 0) perdu += ranger(fe, Biens.FROMAGE.id, fe.betes[Biens.CHEVRE.id]);
        if (nouvelle && s == PRINTEMPS && fe.betes[Biens.MOUTON.id] > 0) {
            perdu += ranger(fe, Biens.LAINE.id, 3 * fe.betes[Biens.MOUTON.id]);
            j.annoncer("La tonte des moutons " + Creation.de(ici) + " : " + Biens.LAINE.quantite(3 * fe.betes[Biens.MOUTON.id]) + ".");
        }
        // Au printemps, les bêtes qui ont une grange font des petits (une portée pour deux bêtes).
        if (nouvelle && s == PRINTEMPS && fe.grange) {
            for (Biens.Bien b : new Biens.Bien[] { Biens.POULE, Biens.CHEVRE, Biens.MOUTON, Biens.PORC }) {
                int nes = fe.betes[b.id] / 2;
                if (nes <= 0) continue;
                fe.betes[b.id] += nes;
                j.annoncer("Au printemps, " + b.quantite(nes) + " de plus dans ta ferme " + Creation.de(ici) + ".");
            }
        }
        // L'hiver, les bêtes mangent le grain de la réserve, une fois par semaine.
        if (s == HIVER && Math.floorMod(d, 7) == 0) {
            int faut = (fe.nombreBetes() + BETES_PAR_SAC - 1) / BETES_PAR_SAC;
            int mange = Math.min(faut, fe.reserve[Biens.GRAIN.id]);
            fe.reserve[Biens.GRAIN.id] -= mange;
            if (mange < faut) {
                for (Biens.Bien b : new Biens.Bien[] { Biens.PORC, Biens.MOUTON, Biens.CHEVRE, Biens.POULE }) {
                    if (fe.betes[b.id] <= 0) continue;
                    fe.betes[b.id]--;
                    j.annoncer("Faute de grain dans ta réserve " + Creation.de(ici) + ", " + b.un() + " est mort" + (b.feminin ? "e" : "") + " de faim.");
                    break;
                }
            }
        }
        if (perdu > 0 && Math.floorMod(d, 7) == 0) j.annoncer("Ta réserve " + Creation.de(ici) + " déborde : bâtis une grange, ou viens vider la réserve.");
    }

    // ================================================================ Scènes (M hors des murs, devant les fermes)

    /** Dans la boutique des fermes : tes champs. */
    void choixBoutique(Joueur.Scene s, int lieu, EconomieVille.Agent a) {
        if (lieu != Biens.CHAMPS || j.villeIci() < 0) return;
        Ferme fe = ferme(j.villeIci());
        s.ajouter(fe == null || fe.lopins.isEmpty() ? "Acheter ou louer un lopin de terre…" : "Ta ferme (" + fe.lopins.size() + " lopin" + (fe.lopins.size() > 1 ? "s" : "") + ")…", () -> ferme(null));
    }

    /** Depuis le menu du personnage : toutes ses terres. */
    Joueur.Scene accueil() {
        StringBuilder t = new StringBuilder(date(p, j.jourMaintenant())).append(".");
        if (fermes.isEmpty()) t.append("\nTu n'as pas de terre. Les lopins s'achètent ou se louent hors des murs, devant les fermes (M).");
        for (Ferme fe : fermes) {
            t.append("\n").append(ville(fe.ville)).append(" : ").append(fe.lopins.size()).append(" lopin(s)").append(fe.grange ? ", une grange" : "").append(", ").append(fe.nombreBetes()).append(" bête(s), réserve ").append(fe.occupe()).append("/").append(fe.capacite());
            for (Lopin l : fe.lopins) t.append("\n   • ").append(etat(l));
        }
        Joueur.Scene s = new Joueur.Scene("Mes terres", t.toString());
        if (j.villeIci() >= 0 && j.horsLesMurs()) s.ajouter("Ma ferme d'ici", () -> ferme(null));
        s.ajouter("« Revenir", j::accueilScene);
        return s;
    }

    private String etat(Lopin l) {
        String t = (l.loue ? "loué" : "à toi") + ", " + ETATS[l.etat];
        if (l.culture >= 0) t += " (" + CULTURES[l.culture].nom + (l.etat == SEME ? ", mûr dans " + Math.max(0, CULTURES[l.culture].jours - (j.jourMaintenant() - l.seme)) + " j" : "") + ")";
        if (l.etat == SEME) t += l.soin >= 0.7f ? ", bien tenu" : l.soin >= 0.3f ? ", à désherber" : ", envahi d'herbes";
        if (l.journalier) t += ", un journalier y travaille";
        return t;
    }

    Joueur.Scene ferme(String message) {
        Fiche f = j.fiche();
        int v = j.villeIci();
        if (v < 0) return accueil();
        Ferme fe = ferme(v);
        int s = saison(j.jourMaintenant());
        StringBuilder t = new StringBuilder(date(p, j.jourMaintenant())).append(".");
        if (fe == null || fe.lopins.isEmpty()) t.append("\nLes fermiers " + Creation.de(ville(v)) + " ont de la terre à vendre ou à louer.");
        else {
            t.append("\nTa ferme ").append(Creation.de(ville(v))).append(fe.grange ? ", avec sa grange" : ", sans grange").append(". Réserve : ").append(fe.occupe()).append("/").append(fe.capacite()).append(".");
            if (fe.nombreBetes() > 0) {
                t.append("\nAu pré :");
                for (int b = 0; b < fe.betes.length; b++) if (fe.betes[b] > 0) t.append(" ").append(Biens.get(b).quantite(fe.betes[b])).append(";");
            }
        }
        Joueur.Scene sc = new Joueur.Scene("Ta ferme", Joueur.avec(message, t.toString()));
        if (fe != null) for (int k = 0; k < fe.lopins.size(); k++) {
            Lopin l = fe.lopins.get(k);
            int n = k;
            sc.ajouter("Lopin " + (k + 1) + " : " + etat(l), () -> lopin(fe, n, null));
        }
        int nb = fe == null ? 0 : fe.lopins.size();
        if (nb < LOPINS_MAX) {
            if (f.argent >= PRIX_LOPIN) sc.ajouter("Acheter un lopin (" + Fiche.argentTexte(PRIX_LOPIN) + ")", () -> prendre(v, false));
            if (f.argent >= LOYER_LOPIN) sc.ajouter("Louer un lopin (" + Fiche.argentTexte(LOYER_LOPIN) + " par saison)", () -> prendre(v, true));
        }
        if (fe != null) {
            boolean achete = false;
            for (Lopin l : fe.lopins) achete |= !l.loue;
            if (!fe.grange && achete) sc.ajouter("Bâtir une grange (" + Fiche.argentTexte(PRIX_GRANGE) + " et " + Biens.PLANCHE.quantite(PLANCHES_GRANGE) + ")", () -> grange(fe));
            sc.ajouter("La réserve (" + fe.occupe() + ")…", () -> reserve(fe, null));
            boolean betesSac = false;
            for (Biens.Bien b : Biens.TOUS) betesSac |= b.categorie == Biens.BETAIL && f.sac.nombre(b.id) > 0;
            if (betesSac) sc.ajouter("Mettre tes bêtes au pré", () -> pre(fe));
        }
        sc.ajouter("« Revenir", () -> j.boutiqueScene(Biens.CHAMPS));
        return sc;
    }

    private Joueur.Scene prendre(int v, boolean loue) {
        Fiche f = j.fiche();
        int prix = loue ? LOYER_LOPIN : PRIX_LOPIN;
        if (f.argent < prix) return ferme("Tu n'as pas assez d'argent.");
        f.argent -= prix;
        j.marche().ville(v, j.jour()).bourse += prix;
        Ferme fe = ferme(v);
        if (fe == null) {
            fe = new Ferme();
            fe.ville = v;
            fermes.add(fe);
        }
        Lopin l = new Lopin();
        l.loue = loue;
        fe.lopins.add(l);
        if (!loue && !f.objets.contains("un lopin de terre")) f.objets.add("un lopin de terre");
        j.noter("En l'an " + annee(p, j.jourMaintenant()) + ", tu " + (loue ? "loues" : "achètes") + " un lopin de terre " + Creation.de(ville(v)) + ".");
        j.destin().verifierEtapes();
        j.enregistrer();
        return ferme(loue ? "Le fermier te montre ton lopin. Le loyer se paie à chaque saison." : "Le lopin est à toi : une bande de terre grasse, derrière les dernières maisons.");
    }

    private Joueur.Scene grange(Ferme fe) {
        Fiche f = j.fiche();
        if (f.argent < PRIX_GRANGE || f.sac.nombre(Biens.PLANCHE.id) < PLANCHES_GRANGE) return ferme("Il te faut " + Fiche.argentTexte(PRIX_GRANGE) + " et " + Biens.PLANCHE.quantite(PLANCHES_GRANGE) + " dans ton sac.");
        f.argent -= PRIX_GRANGE;
        f.sac.retirer(Biens.PLANCHE.id, PLANCHES_GRANGE);
        fe.grange = true;
        j.passer(8.0);
        j.rafraichirSac();
        j.noter("En l'an " + annee(p, j.jourMaintenant()) + ", tu bâtis une grange sur ta terre " + Creation.de(ville(fe.ville)) + ".");
        j.enregistrer();
        return ferme("Avec deux voisins, tu dresses la charpente en une journée. Ta grange peut tout garder, et tes bêtes y passeront l'hiver.");
    }

    private Joueur.Scene pre(Ferme fe) {
        Fiche f = j.fiche();
        int mis = 0;
        for (Biens.Bien b : Biens.TOUS) {
            if (b.categorie != Biens.BETAIL) continue;
            int n = f.sac.nombre(b.id);
            if (!fe.grange) n = Math.min(n, BETES_SANS - fe.nombreBetes());
            if (n <= 0) continue;
            f.sac.retirer(b.id, n);
            fe.betes[b.id] += n;
            mis += n;
        }
        j.rafraichirSac();
        j.destin().verifierEtapes();
        j.enregistrer();
        return ferme(mis == 0 ? "Sans grange, ton pré ne peut pas nourrir plus de " + BETES_SANS + " bêtes." : "Tes bêtes broutent au pré (" + mis + ").");
    }

    private Joueur.Scene reserve(Ferme fe, String message) {
        Fiche f = j.fiche();
        Joueur.Scene s = new Joueur.Scene("La réserve", Joueur.avec(message, "Ce que tu ranges ici reste à la ferme. " + fe.occupe() + "/" + fe.capacite() + "."));
        for (int b = 0; b < fe.reserve.length; b++) {
            if (fe.reserve[b] <= 0) continue;
            int bien = b;
            s.ajouter("Prendre " + Biens.get(b).quantite(fe.reserve[b]), () -> {
                f.sac.ajouter(bien, fe.reserve[bien]);
                fe.reserve[bien] = 0;
                j.rafraichirSac();
                j.enregistrer();
                return reserve(fe, null);
            });
        }
        for (Culture c : CULTURES) {
            int n = f.sac.nombre(c.semence.id);
            if (n <= 0) continue;
            s.ajouter("Déposer " + c.semence.quantite(n) + " (semences, fourrage)", () -> {
                int perdu = ranger(fe, c.semence.id, n);
                f.sac.retirer(c.semence.id, n - perdu);
                j.rafraichirSac();
                j.enregistrer();
                return reserve(fe, perdu > 0 ? "La réserve est pleine." : null);
            });
        }
        s.ajouter("« Revenir", () -> ferme(null));
        return s;
    }

    private Joueur.Scene lopin(Ferme fe, int k, String message) {
        if (k >= fe.lopins.size()) return ferme(null);
        Fiche f = j.fiche();
        Lopin l = fe.lopins.get(k);
        int jour = j.jourMaintenant();
        int s = saison(jour);
        Joueur.Scene sc = new Joueur.Scene("Lopin " + (k + 1), Joueur.avec(message, date(p, jour) + ". Ton lopin est " + etat(l) + "."));
        if (l.etat == FRICHE && s != HIVER) sc.ajouter("Labourer (" + Math.round(H_LABOUR) + " h)", () -> {
            travailler(H_LABOUR, "labourer");
            l.etat = LABOURE;
            return lopin(fe, k, "Tu retournes la terre, sillon après sillon.");
        });
        if (l.etat == LABOURE && semis(s)) for (int c = 0; c < CULTURES.length; c++) {
            Culture cu = CULTURES[c];
            int n = c;
            boolean en = f.sac.nombre(cu.semence.id) > 0 || fe.reserve[cu.semence.id] > 0;
            if (en) sc.ajouter("Semer du " + cu.nom.replace("du légumes", "des légumes") + " (1 " + cu.semence.nom + ")", () -> {
                if (f.sac.nombre(cu.semence.id) > 0) f.sac.retirer(cu.semence.id, 1);
                else fe.reserve[cu.semence.id]--;
                travailler(H_SEMIS, "semer");
                l.etat = SEME;
                l.culture = n;
                l.seme = j.jourMaintenant();
                l.soin = 0.5f;
                j.rafraichirSac();
                return lopin(fe, k, "C'est semé. Il faudra " + cu.jours + " jours, et un peu d'entretien.");
            });
        }
        if (l.etat == LABOURE && !semis(s)) sc.ajouter("(On sème au printemps et en été.)", () -> lopin(fe, k, null));
        if (l.etat == SEME && l.soinJour != jour) sc.ajouter("Désherber et arroser (" + Math.round(H_SOIN) + " h)", () -> {
            travailler(H_SOIN, "desherber");
            l.soin = Math.min(1f, l.soin + 0.4f);
            l.soinJour = j.jourMaintenant();
            return lopin(fe, k, "Le champ est propre.");
        });
        if (l.etat == MUR) {
            boolean faucille = f.sac.nombre(Biens.FAUCILLE.id) > 0;
            float h = faucille ? H_RECOLTE / 2f : H_RECOLTE;
            sc.ajouter("Récolter (" + Math.round(h) + " h" + (faucille ? ", avec ta faucille" : "") + ")", () -> {
                Culture c = CULTURES[l.culture];
                int q = rendement(l);
                travailler(h, "moissonner");
                int perdu = ranger(fe, c.recolte.id, q);
                l.etat = FRICHE;
                l.culture = -1;
                recoltes++;
                if (recoltes == 1) j.noter("En l'an " + annee(p, j.jourMaintenant()) + ", tu rentres ta première récolte " + Creation.de(ville(fe.ville)) + ".");
                j.destin().verifierEtapes();
                return lopin(fe, k, "Tu rentres " + c.recolte.quantite(q - perdu) + " dans la réserve" + (perdu > 0 ? " ; " + perdu + " restent à pourrir, faute de place" : "") + ".");
            });
        }
        if (!l.journalier && f.argent >= SALAIRE) sc.ajouter("Engager un journalier (" + Fiche.argentTexte(SALAIRE) + " par jour)", () -> {
            l.journalier = true;
            j.destin().verifierEtapes();
            j.enregistrer();
            return lopin(fe, k, "Un garçon de ferme s'occupera du lopin : il prend les semences dans la réserve et y range la récolte.");
        });
        if (l.journalier) sc.ajouter("Renvoyer le journalier", () -> {
            l.journalier = false;
            j.enregistrer();
            return lopin(fe, k, "Il prend sa paie et s'en va.");
        });
        if (l.loue) sc.ajouter("Rendre ce lopin au propriétaire", () -> {
            fe.lopins.remove(l);
            j.enregistrer();
            return ferme("Tu rends le lopin.");
        });
        sc.ajouter("« Revenir", () -> ferme(null));
        return sc;
    }

    private void travailler(float heures, String geste) {
        Fiche f = j.fiche();
        f.apprendre(Fiche.LABEUR, heures * 0.5f);
        f.apprendre(Fiche.ATHLETISME, heures * 0.2f);
        j.destin().signaler(Destin.TRAVAIL, -1, heures);
        j.gesteJoueur = geste;
        j.passer(heures);
        j.enregistrer();
    }

    // ================================================================ Essais

    public Joueur.Scene fermePourEssai() {
        return ferme(null);
    }

    public Joueur.Scene lopinPourEssai(Ferme fe, int k) {
        return lopin(fe, k, null);
    }

    // ================================================================ Sauvegarde

    void ecrire(DataOutputStream s) throws IOException {
        s.writeInt(dernierJour);
        s.writeInt(recoltes);
        s.writeInt(fermes.size());
        int n = Biens.TOUS.size();
        s.writeInt(n);
        for (Ferme fe : fermes) {
            s.writeInt(fe.ville);
            s.writeBoolean(fe.grange);
            for (int b = 0; b < n; b++) {
                s.writeInt(fe.reserve[b]);
                s.writeInt(fe.betes[b]);
            }
            s.writeInt(fe.lopins.size());
            for (Lopin l : fe.lopins) {
                s.writeBoolean(l.loue);
                s.writeBoolean(l.journalier);
                s.writeInt(l.etat);
                s.writeInt(l.culture);
                s.writeInt(l.seme);
                s.writeInt(l.soinJour);
                s.writeFloat(l.soin);
            }
        }
    }

    void lire(DataInputStream s) throws IOException {
        dernierJour = s.readInt();
        recoltes = s.readInt();
        int nf = s.readInt(), n = s.readInt();
        int ici = Biens.TOUS.size();
        for (int k = 0; k < nf; k++) {
            Ferme fe = new Ferme();
            fe.ville = s.readInt();
            fe.grange = s.readBoolean();
            for (int b = 0; b < n; b++) {
                int r = s.readInt(), x = s.readInt();
                if (b < ici) {
                    fe.reserve[b] = r;
                    fe.betes[b] = x;
                }
            }
            int nl = s.readInt();
            for (int q = 0; q < nl; q++) {
                Lopin l = new Lopin();
                l.loue = s.readBoolean();
                l.journalier = s.readBoolean();
                l.etat = s.readInt();
                l.culture = s.readInt();
                l.seme = s.readInt();
                l.soinJour = s.readInt();
                l.soin = s.readFloat();
                fe.lopins.add(l);
            }
            fermes.add(fe);
        }
    }
}
