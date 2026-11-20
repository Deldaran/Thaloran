package com.deldaran.politics.histoire;

import static com.deldaran.politics.histoire.Biens.*;

import com.deldaran.politics.Metiers;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Qui fabrique quoi, avec quoi, et combien les habitants consomment.
 * Le temps de travail d'une recette se déduit des prix de référence du catalogue :
 * jours = (valeur des sorties − valeur des entrées) ÷ VA_JOUR. Changer un prix dans Biens change donc aussi le travail qu'il faut.
 */
public final class Recettes {
    /** Salaire d'un ouvrier, en cuivre par jour. */
    public static final float SALAIRE = 8f;
    /** Valeur (au prix de référence) qu'un ouvrier ajoute en une journée. La différence avec le salaire paie l'atelier. */
    public static final float VA_JOUR = 12f;

    public static final class Recette {
        public final int lieu;
        public final List<Bien> sorties = new ArrayList<>(), entrees = new ArrayList<>();
        public final List<Float> qs = new ArrayList<>(), qe = new ArrayList<>();
        /** Journées de travail pour un lot. */
        public float jours;

        Recette(int lieu) {
            this.lieu = lieu;
        }

        Recette out(Bien b, float q) {
            sorties.add(b);
            qs.add(q);
            return this;
        }

        Recette in(Bien b, float q) {
            entrees.add(b);
            qe.add(q);
            return this;
        }

        public float valeurSorties() {
            float v = 0f;
            for (int k = 0; k < sorties.size(); k++) v += sorties.get(k).prix * qs.get(k);
            return v;
        }

        public float valeurEntrees() {
            float v = 0f;
            for (int k = 0; k < entrees.size(); k++) v += entrees.get(k).prix * qe.get(k);
            return v;
        }
    }

    private static final List<Recette> LISTE = new ArrayList<>();
    public static final List<Recette> TOUTES = Collections.unmodifiableList(LISTE);

    private static Recette r(int lieu) {
        Recette x = new Recette(lieu);
        LISTE.add(x);
        return x;
    }

    private static final int MOUL = Metiers.MOULIN, BOUL = Metiers.BOULANGERIE, BOUC = Metiers.BOUCHERIE, TANN = Metiers.TANNERIE, TISS = Metiers.TISSERAND,
            POT = Metiers.POTIER, MENU = Metiers.MENUISERIE, FORG = Metiers.FORGE, AUB = Metiers.AUBERGE, TAV = Metiers.TAVERNE, PECH = Metiers.PECHERIE, ECOL = Metiers.ECOLE;

    static {
        // Les fermes autour de la ville
        r(CHAMPS).out(GRAIN, 1);
        r(CHAMPS).out(LEGUMES, 1);
        r(CHAMPS).out(FROMAGE, 1);
        r(CHAMPS).out(MIEL, 1);
        r(CHAMPS).out(LAINE, 1);
        r(CHAMPS).out(PORC, 1);
        r(CHAMPS).out(POULE, 1);
        r(CHAMPS).out(CHEVRE, 1);
        r(CHAMPS).out(ANE, 1);
        r(CHAMPS).out(MULE, 1);
        // Les bois et la mine
        r(FORET).out(BILLE, 1);
        r(FORET).out(FAGOT, 1);
        r(MINE).out(MINERAI, 1);
        // Pêcherie
        r(PECH).out(POISSON, 6).in(SEL, 0.05f);
        r(PECH).out(FILET, 1).in(CORDE, 2);
        // Le grain
        r(MOUL).out(FARINE, 1).in(GRAIN, 1);
        r(BOUL).out(PAIN, 25).in(FARINE, 1);
        r(BOUL).out(GALETTE, 20).in(FARINE, 1);
        r(AUB).out(BIERE, 20).in(GRAIN, 1);
        r(TAV).out(BIERE, 20).in(GRAIN, 1);
        // La viande et le cuir
        r(BOUC).out(VIANDE, 25).out(JAMBON, 1).out(PEAU_BRUTE, 1).in(PORC, 1);
        r(TANN).out(PEAU, 2).in(PEAU_BRUTE, 3);
        r(TANN).out(BOTTES, 1).in(PEAU, 1);
        r(TANN).out(SAC_A_DOS, 1).in(PEAU, 1);
        r(TANN).out(BOURSE, 4).in(PEAU, 1);
        r(TANN).out(CALOTTE, 2).in(PEAU, 1);
        r(TANN).out(CUIR_BOUILLI, 1).in(PEAU, 4);
        r(TANN).out(BROIGNE, 1).in(PEAU, 3).in(FER, 3);
        // La laine
        r(TISS).out(DRAP, 1).in(LAINE, 6);
        r(TISS).out(CAPE, 1).in(DRAP, 0.8f);
        r(TISS).out(CAPE_DOUBLEE, 1).in(DRAP, 2);
        r(TISS).out(GAMBISON, 1).in(DRAP, 2.5f);
        // Le potier
        r(POT).out(POT_TERRE, 4);
        // Le bois
        r(MENU).out(PLANCHE, 5).out(FAGOT, 2).in(BILLE, 1);
        r(MENU).out(BATON, 3).in(BILLE, 1);
        r(MENU).out(LANCE, 1).in(PLANCHE, 1).in(FER, 0.3f);
        r(MENU).out(JAVELOTS, 1).in(PLANCHE, 2).in(FER, 0.5f);
        r(MENU).out(ARC, 1).in(PLANCHE, 1);
        r(MENU).out(ARC_LONG, 1).in(PLANCHE, 1.5f);
        r(MENU).out(FLECHES, 1).in(PLANCHE, 1).in(FER, 0.2f);
        r(MENU).out(BOUCLIER, 1).in(PLANCHE, 3).in(PEAU, 0.5f);
        r(MENU).out(TROUSSE_BOIS, 1).in(PLANCHE, 1).in(FER, 0.5f);
        // La forge
        r(FORG).out(FER, 1).in(MINERAI, 1).in(CHARBON, 0.5f);
        r(FORG).out(CHARBON, 3).in(BILLE, 1);
        r(FORG).out(COUTEAU, 1).in(FER, 0.2f).in(CHARBON, 0.2f);
        r(FORG).out(COUTEAU_BOUCHER, 1).in(FER, 0.25f).in(CHARBON, 0.2f);
        r(FORG).out(HACHE, 1).in(FER, 0.8f).in(CHARBON, 0.5f).in(PLANCHE, 0.2f);
        r(FORG).out(MARTEAU, 1).in(FER, 0.8f).in(CHARBON, 0.5f);
        r(FORG).out(PIC, 1).in(FER, 0.7f).in(CHARBON, 0.5f);
        r(FORG).out(FAUCILLE, 1).in(FER, 0.3f).in(CHARBON, 0.2f);
        r(FORG).out(LANTERNE, 1).in(FER, 0.4f).in(CHARBON, 0.2f);
        r(FORG).out(DAGUE, 1).in(FER, 0.4f).in(CHARBON, 0.3f);
        r(FORG).out(EPEE, 1).in(FER, 2).in(CHARBON, 2);
        r(FORG).out(EPEE_LONGUE, 1).in(FER, 3).in(CHARBON, 3);
        r(FORG).out(HACHE_GUERRE, 1).in(FER, 2).in(CHARBON, 1).in(PLANCHE, 0.3f);
        r(FORG).out(MASSE, 1).in(FER, 2.5f).in(CHARBON, 1);
        r(FORG).out(HALLEBARDE, 1).in(FER, 2).in(CHARBON, 1.5f).in(PLANCHE, 1);
        r(FORG).out(ARBALETE, 1).in(FER, 1.5f).in(PLANCHE, 1).in(CORDE, 1);
        r(FORG).out(ECU, 1).in(FER, 2).in(PLANCHE, 2);
        r(FORG).out(MAILLES, 1).in(FER, 10).in(CHARBON, 6);
        r(FORG).out(CUIRASSE, 1).in(FER, 12).in(CHARBON, 8);
        r(FORG).out(CASQUE, 1).in(FER, 2).in(CHARBON, 1.5f);
        // Les lettres
        r(ECOL).out(OUVRAGE, 1).in(PEAU, 2);
        r(ECOL).out(PRIERES, 1).in(PEAU, 1);
        r(ECOL).out(ECRITOIRE, 1).in(PLANCHE, 0.2f);

        for (Recette x : LISTE) x.jours = Math.max(x.valeurSorties() * 0.1f, x.valeurSorties() - x.valeurEntrees()) / VA_JOUR;
    }

    public static List<Recette> de(int lieu) {
        List<Recette> l = new ArrayList<>();
        for (Recette x : LISTE) if (x.lieu == lieu) l.add(x);
        return l;
    }

    /** Ce qu'un habitant achète en moyenne par jour. Les soldats de la caserne ajoutent CONSO_GARNISON. */
    public static final float[] CONSO = new float[TOUS.size()], CONSO_GARNISON = new float[TOUS.size()];

    private static void c(Bien b, float q) {
        CONSO[b.id] = q;
    }

    private static void g(Bien b, float q) {
        CONSO_GARNISON[b.id] = q;
    }

    static {
        c(PAIN, 0.7f);
        c(FARINE, 0.01f);
        c(VIANDE, 0.05f);
        c(JAMBON, 0.002f);
        c(POISSON, 0.04f);
        c(FROMAGE, 0.04f);
        c(LEGUMES, 0.12f);
        c(MIEL, 0.004f);
        c(BIERE, 0.25f);
        c(VIN, 0.01f);
        c(SEL, 0.002f);
        c(FAGOT, 0.06f);
        c(CHANDELLE, 0.03f);
        c(POT_TERRE, 0.002f);
        c(LAINE, 0.002f);
        c(DRAP, 0.0005f);
        c(PLANCHE, 0.003f);
        c(CORDE, 0.001f);
        c(COUTEAU, 0.0003f);
        c(FAUCILLE, 0.0002f);
        c(HACHE, 0.0002f);
        c(BOTTES, 0.0006f);
        c(CAPE, 0.0003f);
        c(BOURSE, 0.0002f);
        c(BATON, 0.0002f);
        c(POULE, 0.001f);
        c(CHEVRE, 0.0002f);
        c(SOINS, 0.0003f);
        c(MULE, 0.00004f);
        c(ANE, 0.00005f);
        c(CHEVAL, 0.00002f);
        c(OUVRAGE, 0.00002f);
        c(PRIERES, 0.00005f);
        c(LANCE, 0.0001f);
        c(ARC, 0.0001f);
        c(FLECHES, 0.0002f);
        c(GAMBISON, 0.00005f);
        c(EPICES, 0.00003f);
        g(LANCE, 0.0004f);
        g(EPEE, 0.00008f);
        g(CASQUE, 0.0001f);
        g(GAMBISON, 0.0002f);
        g(MAILLES, 0.00003f);
        g(BOUCLIER, 0.0002f);
        g(ARBALETE, 0.00004f);
        g(FLECHES, 0.0004f);
    }

    private Recettes() {
    }
}
