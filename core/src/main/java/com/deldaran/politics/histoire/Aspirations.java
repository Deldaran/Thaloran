package com.deldaran.politics.histoire;

import java.util.ArrayList;
import java.util.List;

public final class Aspirations {
    public static final int POUVOIR = 0, GUERRE = 1, RICHESSE = 2, TERRE = 3, SAVOIR = 4, FAMILLE = 5, LIBERTE = 6, GROUPES = 7;
    public static final String[] GROUPES_NOMS = { "Pouvoir", "Guerre", "Richesse", "Terre", "Savoir et foi", "Famille et cœur", "Liberté et renom" };
    public static final String[] GROUPES_TEXTES = {
        "Commander aux autres : une ville, un fief, un royaume.",
        "Vivre par l'épée : batailles, compagnies, vengeance.",
        "Faire fortune : ateliers, caravanes, banque.",
        "Vivre de la terre : champs, bêtes, domaines.",
        "Comprendre le monde, le soigner ou le guider.",
        "Ceux qu'on aime avant tout le reste.",
        "Ne dépendre de personne et que l'on chante ton nom."
    };

    public static final class Aspiration {
        public final int groupe;
        public final String nom, texte;
        public final String[] etapes;
        public final int competence;
        public final int axe, penchant;

        Aspiration(int groupe, String nom, String texte, int competence, int axe, int penchant, String... etapes) {
            this.groupe = groupe;
            this.nom = nom;
            this.texte = texte;
            this.competence = competence;
            this.axe = axe;
            this.penchant = penchant;
            this.etapes = etapes;
        }
    }

    public static final List<Aspiration> TOUTES = new ArrayList<>();
    public static final int VENGEANCE;

    private static int ajouter(int groupe, String nom, String texte, int competence, int axe, int penchant, String... etapes) {
        TOUTES.add(new Aspiration(groupe, nom, texte, competence, axe, penchant, etapes));
        return TOUTES.size() - 1;
    }

    static {
        ajouter(POUVOIR, "Devenir roi ou reine", "Porter une couronne, la sienne ou celle d'un autre.", Fiche.COMMANDEMENT, Fiche.AMBITION, 30,
            "Se faire un nom dans sa ville (renom 50)", "Réunir une bande de 20 hommes", "Servir un seigneur ou prendre un fort", "Obtenir un fief",
            "Rallier trois vassaux", "Réclamer le trône par le sang, la conquête ou l'élection", "Être couronné");
        ajouter(POUVOIR, "Devenir noble", "Entrer dans une Maison, ou en fonder une.", Fiche.ELOQUENCE, Fiche.AMBITION, 20,
            "Se faire un nom dans sa ville (renom 50)", "Entrer au service d'un seigneur", "Être adoubé, acheter un titre ou épouser une noble Maison", "Recevoir un premier fief");
        ajouter(POUVOIR, "Diriger sa ville", "Être celui ou celle que la ville écoute : maire, bourgmestre, ancien.", Fiche.ELOQUENCE, Fiche.LOI, 10,
            "Gagner l'estime de la ville", "Entrer dans une guilde", "Financer un bâtiment public", "Être élu ou nommé à la tête de la ville");
        ajouter(POUVOIR, "Fonder une ville", "Planter un camp là où il n'y a rien, et le voir devenir une ville.", Fiche.INTENDANCE, Fiche.AMBITION, 20,
            "Rassembler dix colons", "Trouver un site", "Dresser le camp", "Bâtir vingt maisons", "Faire reconnaître la ville");
        ajouter(POUVOIR, "Unifier son peuple", "Que tout ton peuple n'obéisse qu'à une seule couronne : la tienne.", Fiche.TACTIQUE, Fiche.AMBITION, 40,
            "Devenir roi ou reine", "Prendre une capitale de ton peuple", "Tenir toutes les capitales de ton peuple");

        ajouter(GUERRE, "Chef de guerre légendaire", "Que ton nom fasse trembler les armées.", Fiche.TACTIQUE, Fiche.COURAGE, 30,
            "Gagner une bataille", "Gagner cinq batailles", "Gagner vingt batailles", "Vaincre une armée deux fois plus grande", "Qu'une chanson soit écrite sur toi");
        ajouter(GUERRE, "Capitaine mercenaire", "Vendre son épée, et bien la vendre.", Fiche.COMMANDEMENT, Fiche.AMBITION, 10,
            "Réunir une compagnie de 30", "Remplir trois contrats pour des seigneurs", "Compagnie de 100", "Être connu dans trois royaumes");
        ajouter(GUERRE, "Chevalier", "Servir, protéger, et être reconnu pour cela.", Fiche.EQUITATION, Fiche.LOI, 25,
            "Devenir écuyer d'un noble", "Être adoubé", "Gagner un tournoi", "Défendre un fief");
        VENGEANCE = ajouter(GUERRE, "Vengeance", "Quelqu'un t'a tout pris. Tu ne l'as pas oublié.", Fiche.UNE_MAIN, Fiche.BIEN, -15,
            "Savoir qui est coupable", "Le retrouver", "L'affaiblir : lui prendre ses biens, ses alliés, ses hommes", "Le vaincre ou le tuer");
        ajouter(GUERRE, "Roi des voleurs", "Régner sur ceux qui vivent dans l'ombre.", Fiche.DISCRETION, Fiche.HONNETETE, -30,
            "Entrer dans une guilde de voleurs", "Diriger la guilde de ta ville", "Tenir un réseau dans trois villes", "Corrompre un seigneur");

        ajouter(RICHESSE, "Fonder une entreprise", "Des ateliers, des employés, une chaîne qui va de la forêt à l'étal.", Fiche.INTENDANCE, Fiche.AMBITION, 15,
            "Ouvrir un premier atelier", "Employer cinq personnes", "Tenir trois ateliers", "Tenir une chaîne complète, de la matière au produit", "Avoir des ateliers dans trois villes");
        ajouter(RICHESSE, "Grand marchand", "Acheter là où c'est bon marché, vendre là où c'est rare.", Fiche.COMMERCE, Fiche.AMBITION, 10,
            "Réunir 1 000 pièces", "Monter une caravane", "Ouvrir des comptoirs dans trois villes", "Tenir une route commerciale entre deux royaumes");
        ajouter(RICHESSE, "Maître artisan", "Faire de ses mains l'objet que l'on se transmettra.", Fiche.FORGE, Fiche.HONNETETE, 15,
            "Devenir compagnon", "Réaliser un chef-d'œuvre", "Devenir maître de guilde", "Créer un objet légendaire qui portera un nom");
        ajouter(RICHESSE, "Banquier", "L'or des autres travaille pour toi.", Fiche.COMMERCE, Fiche.GENEROSITE, -20,
            "Prêter à dix personnes", "Prêter à un seigneur", "Financer une guerre");

        ajouter(TERRE, "Travailler la terre", "Une ferme prospère qui nourrit les siens, et parfois toute une ville.", Fiche.LABEUR, Fiche.HONNETETE, 10,
            "Avoir un lopin", "Faire une première récolte", "Avoir des bêtes", "Tenir une ferme avec des ouvriers", "Nourrir une ville pendant une famine");
        ajouter(TERRE, "Seigneur foncier", "Des champs à perte de vue, et ceux qui les travaillent.", Fiche.INTENDANCE, Fiche.AMBITION, 15,
            "Tenir un domaine de dix champs", "Bâtir un moulin", "Avoir un village de fermiers");
        ajouter(TERRE, "Éleveur de chevaux", "Les meilleurs chevaux du royaume sortent de chez toi.", Fiche.EQUITATION, Fiche.GENEROSITE, 0,
            "Avoir des chevaux", "Bâtir un haras", "Fournir la cavalerie d'un seigneur");

        ajouter(SAVOIR, "Érudit", "Lire tout ce qui a été écrit, et écrire ce qui manque.", Fiche.LETTRES, Fiche.HONNETETE, 10,
            "Savoir lire", "Lire vingt livres", "Découvrir une vérité cachée de l'histoire", "Écrire ta propre chronique, que d'autres liront");
        ajouter(SAVOIR, "Guérisseur", "Arracher les gens à la mort.", Fiche.MEDECINE, Fiche.BIEN, 30,
            "Soigner dix blessés", "Ouvrir un dispensaire", "Arrêter une épidémie");
        ajouter(SAVOIR, "Prêtre ou prophète", "Que les autres croient avec toi.", Fiche.ELOQUENCE, Fiche.FOI, 40,
            "Servir au temple", "Convertir des fidèles", "Bâtir un temple", "Fonder un culte");
        ajouter(SAVOIR, "Explorateur", "Voir ce que personne n'a vu depuis mille ans.", Fiche.SURVIE, Fiche.COURAGE, 15,
            "Visiter dix régions", "Retrouver une ville en ruine ancienne", "Cartographier un royaume");

        ajouter(FAMILLE, "Fonder une dynastie", "Que ton sang tienne encore dans trois générations.", Fiche.INTENDANCE, Fiche.AMBITION, 10,
            "Se marier", "Avoir trois enfants", "Qu'un enfant devienne noble ou maître", "Voir naître la troisième génération");
        ajouter(FAMILLE, "Grand amour", "Trouver la bonne personne et ne plus la quitter.", Fiche.ELOQUENCE, Fiche.BIEN, 15,
            "Rencontrer quelqu'un", "Lui faire la cour", "L'épouser", "Vieillir ensemble");
        ajouter(FAMILLE, "Protéger les siens", "Que ta famille ne manque jamais de rien, et ne craigne personne.", Fiche.LABEUR, Fiche.BIEN, 20,
            "Loger et nourrir tes parents", "Bâtir une maison de pierre", "Mettre ta famille à l'abri de toute guerre");

        ajouter(LIBERTE, "Aventurier", "Les chemins, les ruines, les bêtes que l'on croit des légendes.", Fiche.ATHLETISME, Fiche.COURAGE, 25,
            "Parcourir cinq régions", "Explorer une ruine ancienne", "Tuer une bête légendaire");
        ajouter(LIBERTE, "Hors-la-loi légendaire", "Les seigneurs te craignent, le peuple te chante.", Fiche.DISCRETION, Fiche.LOI, -40,
            "Avoir ta tête mise à prix", "Réunir une bande de brigands", "Voler un seigneur", "Qu'une chanson soit écrite sur toi");
        ajouter(LIBERTE, "Barde", "Les chansons durent plus longtemps que les rois.", Fiche.ELOQUENCE, Fiche.GENEROSITE, 10,
            "Apprendre dix chansons", "Jouer dans cinq tavernes", "Composer une chanson qui te survivra");
        ajouter(LIBERTE, "Champion", "Être le meilleur, et le prouver devant tous.", Fiche.DEUX_MAINS, Fiche.COURAGE, 30,
            "Gagner un tournoi", "Gagner des tournois dans trois royaumes");
    }

    private Aspirations() {
    }

    public static List<Integer> du(int groupe) {
        List<Integer> l = new ArrayList<>();
        for (int k = 0; k < TOUTES.size(); k++) if (TOUTES.get(k).groupe == groupe) l.add(k);
        return l;
    }

    public static String decrire(int k, int etape) {
        Aspiration a = TOUTES.get(k);
        StringBuilder b = new StringBuilder(a.texte).append("\n");
        for (int e = 0; e < a.etapes.length; e++) {
            b.append("\n").append(e == etape ? "  » " : "     ").append(e + 1).append(". ").append(a.etapes[e]).append(e < etape ? " (fait)" : "");
        }
        return b.toString();
    }
}
