package com.deldaran.politics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.deldaran.politics.planet.GenerateurMonde;
import com.deldaran.politics.planet.Monde;
import com.deldaran.politics.planet.Plaque;
import com.deldaran.politics.planet.Simulation;
import com.deldaran.politics.planet.Terrain;
import com.badlogic.gdx.files.FileHandle;
import com.deldaran.politics.planet.Sauvegarde;
import com.deldaran.politics.histoire.Regions;
import com.deldaran.politics.planet.Biome;
import java.util.function.Consumer;
import java.util.function.Supplier;
import com.deldaran.politics.histoire.Histoire;
import com.deldaran.politics.histoire.Biens;
import com.deldaran.politics.histoire.Joueur;
import com.deldaran.politics.histoire.Bande;
import com.deldaran.politics.histoire.Brigands;
import com.deldaran.politics.histoire.Combat;
import com.deldaran.politics.histoire.Population;
import com.deldaran.politics.histoire.Peuple;
import com.deldaran.politics.histoire.Noms;
import com.deldaran.politics.histoire.Villes;
import com.deldaran.politics.histoire.Economie;
import com.deldaran.politics.histoire.Etats;
import com.deldaran.politics.histoire.Livres;
import com.deldaran.politics.histoire.Recit;
import java.util.function.Function;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FirstScreen implements Screen {

    GameCamera camera;
    CameraController cameraController;
    CarteCube carte;
    CarteCube fond;
    boolean fondPret;
    RenduGlobe rendu;
    int[] couleurs;
    float tempsCarte = 0f;
    int modeCarte = -1;

    Monde monde;
    Simulation simulation;
    FileHandle fichierMonde;
    boolean mondeEnregistre;
    FileHandle fichierHistoire;
    FileHandle fichierRelations;
    IaLocale ia;
    float attenteSauvegarde;
    boolean reprendreHistoire = true;
    Regions regions;
    final Regions.Reglages reglagesRegions = new Regions.Reglages();
    final Population.Reglages reglagesPopulation = new Population.Reglages();

    float pasParSeconde = 10f;

    FenetreDebug fenetreDebug;
    int modeAffichage = 4;
    float dernierClimat = -1000f;

    RenduTuiles tuiles;
    Vegetation vegetation;
    Terrain terrain;
    Assembleur assembleur;
    Batiments batiments;
    final Villages villages = new Villages();
    final Habitants habitants = new Habitants();
    Meteo meteo;
    Ciel ciel;
    Traces traces;
    Rues rues;
    Chargement chargement;
    final Obstacles obstacles = new Obstacles() {
        public boolean bloque(double x, double y, double z, double r) {
            return (villages != null && villages.bloque(x, y, z, r)) || (vegetation != null && vegetation.bloque(x, y, z, r));
        }

        public boolean surPont(double x, double y, double z) {
            return villages != null && villages.surPont(x, y, z);
        }
    };
    boolean arrive;
    final List<Personnage> personnages = new ArrayList<>();
    private final Terrain.Point point = new Terrain.Point();
    private final Random hasard = new Random();
    private float[] errance = new float[0];
    Personnage joueur;
    int animationTest = -1;
    private static final double RAYON_JOUEUR = 2.0e7;
    private static final double ZOOM_JEU = 6.0e7;
    private static final double MARCHE = 1.4;
    private static final double COURSE = 4.0;

    /** Lot K1 : le monde joué (sa fiche : nom, graine, durée de l'histoire, dossier des sauvegardes). */
    final Mondes.Fiche fiche;
    private final long GRAINE;
    /** Lot K1 : le menu Échap ; la fenêtre de debug demandée par la touche ². */
    MenuPause pause;
    /** Lot K2 : la barre du bas pendant la création du monde ; le dernier fait de la chronique qu'elle montre. */
    BarreCreation barre;
    /** Lot K2 : la plaque en haut à droite qui dit quelle carte on regarde. */
    EtiquetteCarte etiquetteCarte;
    /** Lot K3 : l'écran de création du personnage (le personnage en grand, sa fiche, les choix). */
    EcranCreation ecranCreation;
    private String evenementCreation = "";
    private float tempsEvenement;
    private boolean debugDemande;

    /** Le monde 42 d'avant les mondes multiples (les essais l'utilisent). */
    public FirstScreen() {
        this(Mondes.ancien());
    }

    public FirstScreen(Mondes.Fiche fiche) {
        this.fiche = fiche;
        GRAINE = fiche.graine;
    }


    /** Lot J1 : pour les essais, un moment de l'année forcé (0 à 1) ; négatif, c'est celui du calendrier. */
    float anneeChamps = -1f;
    Histoire histoire;
    boolean enPause = false;
    float tempsAccumule = 0f;
    private static final float SEUIL_RIVIERE = 10f;
    private static final String[] NOMS_BIOMES = { "Toundra", "Taiga", "Prairie", "Foret", "Desert", "Savane", "Jungle", "Montagne" };
    private static final byte[] BIOMES_FERTILES = { Biome.TOUNDRA, Biome.TAIGA, Biome.PRAIRIE, Biome.FORET, Biome.DESERT, Biome.SAVANE, Biome.JUNGLE, Biome.MONTAGNE };

    private static final float PERIODE_CARTE = 0.1f;
    private static final double RAYON_TUILES = 300.0;
    private static final double ZOOM_CARTE = 5000.0, ZOOM_LIBRE = 166400000.0;
    private static final int MODE_BIOMES = 4;
    private static final float OPACITE_CALQUE = 0.55f;
    private static final Color[] COULEURS_PEUPLES = {
        new Color(0.39f, 0.83f, 0.44f, 1f), new Color(0.85f, 0.40f, 0.23f, 1f), new Color(0.95f, 0.81f, 0.24f, 1f), new Color(0.64f, 0.23f, 0.82f, 1f)
    };


    private static final String[] NOMS_MODES = { "plaques", "relief", "temperature", "pluie", "biomes", "regions", "nourriture", "population", "villes", "commerce", "etats", "routes" };

    private final Color couleurTemp = new Color();

    @Override
    public void show() {
        camera = new GameCamera();
        cameraController = new CameraController(camera);


        monde = new GenerateurMonde().genererMonde(512, 256, GRAINE);

        simulation = new Simulation(monde);
        fichierMonde = fiche.fichier("monde-" + GRAINE + ".bin");
        mondeEnregistre = Sauvegarde.charger(monde, simulation, fichierMonde);
        fichierHistoire = fiche.fichier("histoire-" + GRAINE + ".bin");
        fichierRelations = fiche.fichier("relations-" + GRAINE + ".bin");
        habitants.relations.charger(fichierRelations);
        ia = new IaLocale(Gdx.files.external(Thaloran.DOSSIER + "ia").file());
        habitants.ia = ia;
        if (System.getenv("SANS_IA") == null) ia.demarrer();

        carte = new CarteCube(monde.sphere);
        fond = new CarteCube(monde.sphere);
        rendu = new RenduGlobe();
        couleurs = new int[monde.sphere.nombre];

        fenetreDebug = new FenetreDebug();
        ui = new Interface();
        remplirDebug();
        chargement = new Chargement();

        pause = new MenuPause(fiche.nom, this::quitterMonde);
        fenetreDebug.afficher(System.getenv("SORTIE") != null);
        com.badlogic.gdx.InputAdapter touches = new com.badlogic.gdx.InputAdapter() {
            @Override
            public boolean keyDown(int code) {
                if (code == Input.Keys.GRAVE) debugDemande = true;
                return false;
            }

            @Override
            public boolean keyTyped(char c) {
                if (c == '²') {
                    debugDemande = true;
                    return true;
                }
                return false;
            }
        };
        barre = new BarreCreation(0, i -> pasParSeconde = BarreCreation.PAS_PAR_SECONDE[i]);
        etiquetteCarte = new EtiquetteCarte();
        ecranCreation = new EcranCreation();
        ecranCreation.zone = this::carteDetail;
        Gdx.input.setInputProcessor(new InputMultiplexer(pause.getStage(), ecranCreation.getStage(), touches, barre.getStage(), ui.getStage(), fenetreDebug.getStage(), cameraController));
    }

    @Override
    public void render(float delta) {
        // Lot K1 : ² montre ou cache le debug ; Échap ferme d'abord une fenêtre ouverte, sinon ouvre le menu du jeu.
        if (debugDemande) {
            debugDemande = false;
            if (!ui.saisieActive()) fenetreDebug.afficher(!fenetreDebug.affichee());
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) && !pause.ouvert() && ecranCreation.prendreEchap()) {
            // L'écran de création se ferme : on revient au monde.
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) && !ui.prendreEchap() && !ui.saisieActive()) pause.basculer();
        if (pause.ouvert()) delta = 0f;
        musique();
        cameraController.bloque = ui != null && ui.saisieActive() || pause.ouvert() || ecranCreation.ouvert();
        cameraController.update(delta);
        rejoindreVille();
        attenteSauvegarde += delta;
        if (attenteSauvegarde > 15f) {
            attenteSauvegarde = 0f;
            if (habitants.relations.modifie && fichierRelations != null) habitants.relations.enregistrer(fichierRelations);
        }
        if (touche(Input.Keys.TAB)) {
            modeAffichage = (modeAffichage + 1) % NOMS_MODES.length;
        }
        if (touche(Input.Keys.P) && terrain != null) {
            peupler();
        }
        if (touche(Input.Keys.O) && meteo != null) {
            meteo.forcage = (meteo.forcage + 1) % Meteo.FORCAGES.length;
        }
        if (touche(Input.Keys.T) && terrain != null) {
            teleporterSouris();
        }
        if (touche(Input.Keys.H) && meteo != null) {
            meteo.decalageHeures += 3.0;
        }
        if (meteo != null) meteo.avancer(delta, accelerationMeteo());
        if (meteo != null && histoire != null) {
            Joueur hj = histoire.joueur;
            synchronized (hj) {
                // Le travail et le sommeil font passer le temps : on l'ajoute à la météo, puis on donne au joueur l'heure qu'il est.
                meteo.decalageHeures += hj.prendreHeures();
                hj.heureLocale = meteo.heureLocale(camera.longitude);
                hj.horloge(meteo.heures());
            }
        }
        if (touche(Input.Keys.SPACE)) {
            enPause = !enPause;
        }

        if (!enPause) {
            tempsAccumule += delta;
            float periode = 1f / pasParSeconde;
            long limite = System.nanoTime() + 25000000L;
            while (tempsAccumule >= periode && System.nanoTime() < limite) {
                simulation.pas();
                tempsAccumule -= periode;
            }
            tempsAccumule = Math.min(tempsAccumule, 0.1f);
        }

        if (!mondeEnregistre && simulation.phase == Simulation.Phase.TERMINEE) {
            Sauvegarde.enregistrer(monde, simulation, fichierMonde);
            mondeEnregistre = true;
        }
        if (regions == null && simulation.phase == Simulation.Phase.TERMINEE) {
            regions = new Regions(monde, simulation.climat.hauteurPlaines, reglagesRegions);
        }
        if (histoire == null && regions != null) {
            histoire = new Histoire(monde, regions, reglagesPopulation, fichierHistoire, reprendreHistoire);
            histoire.dureeMax = fiche.duree;
        }
        if (histoire != null) {
            histoire.enPause = enPause;
        }

        if (simulation.phase == Simulation.Phase.TECTONIQUE && modeAffichage >= 2 && monde.temps - dernierClimat >= 100f) {
            simulation.climat.calculer();
            dernierClimat = monde.temps;
        }


        tempsCarte += delta;
        if (modeCarte != modeAffichage || (!enPause && tempsCarte >= PERIODE_CARTE)) {
            rafraichirCarte();
        }



        ScreenUtils.clear(Color.BLACK);
        if (simulation.phase != Simulation.Phase.TERMINEE) {
            rendu.dessiner(carte, camera, 1f);
        } else {
            if (!fondPret) preparerFond();
            if (tuiles == null) initialiserMonde();
            if (!arrive && camera.rayonPixels >= RAYON_JOUEUR) {
                arrive = true;
                chargement.commencer();
            }
            if (camera.rayonPixels >= RAYON_TUILES) {
                // Pendant la création, les tuiles se chargent peu à peu : le globe coloré (mêmes tons) bouche les trous.
                if (barre.visible() && tuiles.enAttente() > 0) rendu.dessiner(fond, camera, 1f);
                dessinerTuiles();
            } else {
                rendu.dessiner(fond, camera, 1f);
            }
            ciel.ecrasement = camera.rayonPixels >= RAYON_TUILES ? tuiles.ecrasement() : 1f;
            ciel.rotation = camera.rayonPixels >= RAYON_TUILES ? tuiles.rotation() : 0.0;
            Personnage.rotationVue = ciel.rotation;
            camera.rotationVue = ciel.rotation;
            camera.ecrasementVue = ciel.ecrasement;
            ciel.dessiner(camera, meteo, delta);
            if (incendies != null && camera.rayonPixels >= RAYON_TUILES) incendies.dessinerLumieres(ciel.nuit);
            if (modeAffichage != MODE_BIOMES) {
                rendu.dessiner(carte, camera, OPACITE_CALQUE);
            }
        }

        fenetreDebug.bandeau(bandeau());
        fenetreDebug.dessiner(delta);
        majInterface();
        ui.dessiner(delta);
        // Pendant la création, la barre du bas dit déjà où l'on en est : l'écran de chargement attend l'entrée dans le monde.
        if (!barre.visible()) dessinerChargement(delta);
        barre.dessiner(Gdx.graphics.getDeltaTime());
        // On la montre pendant la création, sur le globe vu de loin, ou dès qu'une carte autre que les biomes est posée sur le monde.
        etiquetteCarte.montrer(modeAffichage, barre.visible() || camera.rayonPixels < RAYON_TUILES || modeAffichage != MODE_BIOMES);
        etiquetteCarte.dessiner(Gdx.graphics.getDeltaTime());
        if (ecranCreation.ouvert() && histoire != null) {
            com.deldaran.politics.histoire.Creation c = histoire.joueur.creationEnCours();
            if (c != null) ecranCreation.apercu(c.apercu(), c.peuple(), c.femme(), c.graineApercu(), c.aspect());
        }
        ecranCreation.dessiner(Gdx.graphics.getDeltaTime());
        pause.dessiner(Gdx.graphics.getDeltaTime());
    }

    /** Lot K2 : la barre dit ce qui se passe et quand : la terre (en millions d'années), les peuples, puis l'histoire (en années). */
    private void majBarre() {
        Simulation.Phase ph = simulation.phase;
        if (ph == Simulation.Phase.TECTONIQUE) {
            float p = monde.temps / simulation.dureeTectonique;
            String titre = p < 0.15f ? "La croûte se fend en plaques" : p < 0.55f ? "Les continents dérivent et se heurtent" : p < 0.85f ? "Les montagnes se lèvent" : "Les océans s'ouvrent";
            int ma = Math.max(0, Math.round(simulation.dureeTectonique - monde.temps)) + 15;
            barre.montrer(0, titre, "Il y a " + milliers(ma) + " millions d'années", p * 0.94f, "", true);
        } else if (ph == Simulation.Phase.RIVIERES) {
            int fait = simulation.pasRivieresFaits();
            float p = fait / (float) simulation.pasRivieres;
            String titre = fait >= simulation.pasRivieres - 20 ? "Les glaciers creusent les vallées" : p < 0.4f ? "Les pluies tombent, les fleuves naissent" : "Les fleuves creusent leur lit";
            String ma = String.format(java.util.Locale.FRANCE, "%.1f", (simulation.pasRivieres - fait) * 0.01);
            barre.montrer(0, titre, "Il y a " + ma + " millions d'années", 0.94f + p * 0.06f, "", true);
        } else if (histoire == null) {
            barre.montrer(1, "Les peuples s'éveillent", "Les premiers berceaux, les premières tribus", -1f, "", false);
        } else if (histoire.population.annee < histoire.dureeMax) {
            int an = histoire.population.annee, fin = histoire.dureeMax;
            float p = an / (float) fin;
            String titre = p < 0.08f ? "Les premières villes sont fondées" : p < 0.35f ? "Les royaumes naissent et se font la guerre" : p < 0.75f ? "Les dynasties se succèdent" : "Le monde se prépare à ta naissance";
            tempsEvenement += Gdx.graphics.getDeltaTime();
            if (tempsEvenement > 2.5f) {
                tempsEvenement = 0f;
                try {
                    com.deldaran.politics.histoire.Chronique ch = histoire.population.etats.chronique;
                    int i = ch.recent(0);
                    if (i >= 0) evenementCreation = ch.texte(histoire.population, i);
                } catch (RuntimeException e) {
                    // La chronique s'écrit dans un autre fil : on réessaiera.
                }
            }
            barre.montrer(2, titre, "An " + milliers(an) + " sur " + milliers(fin), p, evenementCreation, false);
        } else barre.cacher();
    }

    private static String milliers(int n) {
        return String.format(java.util.Locale.FRANCE, "%,d", n).replace('\u202f', ' ').replace('\u00a0', ' ');
    }

    /** Lot K1 : la musique suit le moment : le monde qui se forme et son histoire, puis la création du personnage, puis le silence du jeu. */
    private void musique() {
        if (histoire == null || histoire.population.annee < histoire.dureeMax) Sons.musique("creation_monde");
        else if (!incarne) Sons.musique("select_personnage_history");
        else Sons.musique(null);
    }

    /** Lot K1 : avant de quitter le monde (menu principal ou fin du jeu) : on arrête l'histoire et l'on enregistre le joueur. */
    private void quitterMonde() {
        if (habitants.relations.modifie && fichierRelations != null) habitants.relations.enregistrer(fichierRelations);
        Histoire h = histoire;
        if (h == null) return;
        h.arreter();
        if (h.joueur.fiche() != null) h.joueur.enregistrer();
    }

    private void dessinerChargement(float delta) {
        int sol = 0, forets = 0, gens = 0, ciel = 0;
        if (tuiles != null && camera.rayonPixels >= RAYON_TUILES) {
            sol = tuiles.enAttente();
            forets = vegetation.parcellesManquantes;
            gens = vegetation.personnagesManquants + vegetation.maisonsManquantes;
        }
        if (meteo != null && !meteo.pret()) ciel = 1;
        String etape = sol > 0 ? "La terre se forme" : forets > 0 ? "Les forets poussent" : gens > 0 ? "Les vivants s'eveillent" : ciel > 0 ? "Le ciel se couvre" : "";
        chargement.dessiner(sol + forets + gens + ciel, etape, delta);
    }

    private void couleurPlaque(int i) {
        Plaque plaque = monde.plaques.get(monde.plaque[i]);
        couleurTemp.set(plaque.couleur);
        if (!monde.isContinentale[i]) {
            couleurTemp.mul(0.7f, 0.7f, 0.7f, 1f);
        }
    }

    private void initialiserMonde() {
        terrain = new Terrain(monde, simulation.climat.hauteurPlaines);
        tuiles = new RenduTuiles(terrain);
        vegetation = new Vegetation(terrain);
        assembleur = new Assembleur();
        batiments = new Batiments();
        meteo = new Meteo(terrain);
        ciel = new Ciel(terrain);
        incendies = new Incendies(terrain);
        incendies.front.vegetation = vegetation;
        vegetation.incendies = incendies;
        traces = new Traces();
        rues = new Rues();
        chargement.commencer();
    }

    private boolean accessible(double x, double y, double z) {
        terrain.echantillonner(x, y, z, 11, point);
        return Personnage.profondeur(point) == 0f;
    }

    void teleporter(double x, double y, double z) {
        double[] d = { x, y, z };
        Random r = new Random(Double.doubleToLongBits(x * 31.0 + y));
        double lon0 = Math.atan2(y, x), lat0 = Math.asin(Math.max(-1.0, Math.min(1.0, z)));
        boolean trouve = false;
        for (int k = 0; k < 6000 && !trouve; k++) {
            double ecart = k == 0 ? 0.0 : 0.00002 * (1 + k / 50);
            double lat = lat0 + (r.nextDouble() - 0.5) * ecart, lon = lon0 + (r.nextDouble() - 0.5) * ecart;
            d[0] = Math.cos(lat) * Math.cos(lon);
            d[1] = Math.cos(lat) * Math.sin(lon);
            d[2] = Math.sin(lat);
            trouve = accessible(d[0], d[1], d[2]);
        }
        if (!trouve) return;
        camera.longitude = Math.atan2(d[1], d[0]);
        camera.latitude = Math.asin(d[2]);
        camera.rayonPixels = ZOOM_JEU;
        traces.effacer();
        personnages.remove(joueur);
        joueur = null;
        placerJoueur();
        chargement.commencer();
    }

    void teleporterSouris() {
        double qx = (Gdx.input.getX() - Gdx.graphics.getWidth() / 2.0) / camera.rayonPixels;
        double qy = (Gdx.graphics.getHeight() / 2.0 - Gdx.input.getY()) / camera.rayonPixels / (tuiles != null && camera.rayonPixels >= RAYON_TUILES ? tuiles.ecrasement() : 1.0);
        double rv = tuiles != null && camera.rayonPixels >= RAYON_TUILES ? tuiles.rotation() : 0.0;
        double rx = Math.cos(rv) * qx + Math.sin(rv) * qy, ry = -Math.sin(rv) * qx + Math.cos(rv) * qy;
        qx = rx;
        qy = ry;
        double q2 = qx * qx + qy * qy;
        if (q2 >= 1.0) return;
        double cosLon = Math.cos(camera.longitude), sinLon = Math.sin(camera.longitude);
        double cosLat = Math.cos(camera.latitude), sinLat = Math.sin(camera.latitude);
        double qz = Math.sqrt(1.0 - q2);
        double x = -sinLon * qx - sinLat * cosLon * qy + cosLat * cosLon * qz;
        double y = cosLon * qx - sinLat * sinLon * qy + cosLat * sinLon * qz;
        double z = cosLat * qy + sinLat * qz;
        if (joueur != null && camera.rayonPixels >= RAYON_JOUEUR) {
            if (!accessible(x, y, z)) return;
            joueur.position[0] = x;
            joueur.position[1] = y;
            joueur.position[2] = z;
            joueur.altitude = point.altitude;
            joueur.profondeur = 0f;
            joueur.dansHerbe = Personnage.herbe(terrain, x, y, z, point);
            return;
        }
        teleporter(x, y, z);
    }

    private int villeAtteindre = -1;
    Interface ui;
    Incendies incendies;
    boolean incarne, zoomLibre;

    private void majInterface() {
        camera.rayonMax = incarne || zoomLibre ? ZOOM_LIBRE : ZOOM_CARTE;
        majBarre();
        if (histoire == null || histoire.population.annee < histoire.dureeMax) {
            ui.cacherBouton();
            return;
        }
        ui.bouton(incarne ? "Mon personnage" : "Entrer dans le monde", true, () -> jouer(histoire.joueur::ouvrir));
    }

    private void rejoindreVille() {
        if (histoire == null || terrain == null) return;
        if (histoire.joueur.nouvelleApparence && joueur != null) {
            histoire.joueur.nouvelleApparence = false;
            Joueur incarne = histoire.joueur;
            Personnage ancien = joueur;
            joueur = new Personnage(assembleur.tirer(incarne.apparence(), incarne.peuple() >= 0 ? incarne.peuple() : Peuple.HUMAIN, incarne.sexe()), ancien.position[0], ancien.position[1], ancien.position[2]);
            joueur.altitude = ancien.altitude;
            joueur.dansHerbe = ancien.dansHerbe;
            joueur.sante = ancien.sante;
            personnages.set(personnages.indexOf(ancien), joueur);
            habiller();
        }
        int demandee = histoire.joueur.villeDemandee;
        if (demandee >= 0) {
            histoire.joueur.villeDemandee = -1;
            incarne = true;
            int c = histoire.population.villes.caseDe[demandee];
            villeAtteindre = demandee;
            teleporter(monde.sphere.x[c], monde.sphere.y[c], monde.sphere.z[c]);
            return;
        }
        if (villeAtteindre < 0) return;
        for (int i = 0; i < villages.nombreVillages(); i++) {
            Villages.Village w = villages.village(i);
            if (w.ville != villeAtteindre || w.positions == null) continue;
            villeAtteindre = -1;
            if (w.portes != null && w.portes.length >= 4) {
                double[] porte = new double[3];
                w.monde(w.portes[2], w.portes[3], porte);
                teleporter(porte[0], porte[1], porte[2]);
            } else teleporter(w.x, w.y, w.z);
            return;
        }
    }

    void teleporterVille(boolean auHasard) {
        if (histoire == null || terrain == null) return;
        com.deldaran.politics.histoire.Villes v = histoire.population.villes;
        int[] grandes = v.plusGrandes(40);
        int choix = -1;
        for (int k = 0; k < grandes.length; k++) {
            int c = auHasard ? grandes[hasard.nextInt(grandes.length)] : grandes[k];
            if (c >= 0) {
                choix = c;
                break;
            }
        }
        if (choix < 0) return;
        int c = v.caseDe[choix];
        teleporter(monde.sphere.x[c], monde.sphere.y[c], monde.sphere.z[c]);
    }

    void teleporterBiome(int biome) {
        if (terrain == null) return;
        for (int k = 0; k < 40000; k++) {
            double z = hasard.nextDouble() * 2.0 - 1.0, a = hasard.nextDouble() * 2.0 * Math.PI, r = Math.sqrt(1.0 - z * z);
            double x = r * Math.cos(a), y = r * Math.sin(a);
            terrain.echantillonner(x, y, z, 9, point);
            if (point.altitude < 0.01f || point.riviere || point.lac) continue;
            if (biome >= 0 && terrain.biome(x, y, z, point) != biome) continue;
            teleporter(x, y, z);
            return;
        }
    }

    void teleporterRelief() {
        if (terrain == null) return;
        double meilleur = -1.0, bx = 0.0, by = 0.0, bz = 1.0, e = 80.0 / (Terrain.RAYON_KM * 1000.0);
        for (int k = 0; k < 3000; k++) {
            double z = hasard.nextDouble() * 2.0 - 1.0, a = hasard.nextDouble() * 2.0 * Math.PI, r = Math.sqrt(1.0 - z * z);
            double x = r * Math.cos(a), y = r * Math.sin(a);
            terrain.echantillonner(x, y, z, 11, point);
            if (point.altitude < 0.3f || point.riviere || point.lac) continue;
            float h = point.altitude;
            double n = Math.sqrt(x * x + y * y);
            terrain.echantillonner(x - y / n * e, y + x / n * e, z, 11, point);
            float he = point.altitude;
            terrain.echantillonner(x - z * x / n * e, y - z * y / n * e, z + n * e, 11, point);
            float hn = point.altitude;
            double pente = Math.hypot(he - h, hn - h);
            if (pente > meilleur) {
                meilleur = pente;
                bx = x;
                by = y;
                bz = z;
            }
        }
        if (meilleur > 0.0) teleporter(bx, by, bz);
    }

    /** Pendant le travail à la main, une heure de jeu passe en 4 secondes. */
    static final double ACCELERATION_TRAVAIL = 900.0;
    /** Pendant le sommeil, une heure de jeu passe en une seconde. */
    static final double ACCELERATION_SOMMEIL = 3600.0;

    private double accelerationMeteo() {
        if (histoire != null && histoire.joueur.sommeil != Joueur.EVEILLE) return ACCELERATION_SOMMEIL;
        if (histoire != null && histoire.joueur.travail().mainActif) return ACCELERATION_TRAVAIL;
        double proche = Math.log(camera.rayonPixels / 1.0e5) / Math.log(RAYON_JOUEUR / 1.0e5);
        proche = Math.max(0.0, Math.min(1.0, proche));
        return Meteo.ACCELERATION + (Meteo.ACCELERATION_LOCALE - Meteo.ACCELERATION) * proche;
    }

    private void dessinerTuiles() {
        piloter(Gdx.graphics.getDeltaTime());
        animerObjets(Gdx.graphics.getDeltaTime());
        errer(Gdx.graphics.getDeltaTime());
        traces.mollesse = 0.6f + ciel.locale.neigeSol * 3f + Math.max(0f, ciel.locale.mouille - 0.3f) * 2.5f;
        traces.comblement = ciel.locale.neige;
        traces.villages = villages;
        traces.mettreAJour(camera, personnages, Gdx.graphics.getDeltaTime());
        tuiles.traces = traces.texture();
        tuiles.traceX = traces.cameraX;
        tuiles.traceY = traces.cameraY;
        tuiles.mouille = ciel.locale.mouille;
        tuiles.neigeSol = ciel.locale.neigeSol;
        tuiles.annee = anneeChamps >= 0f || histoire == null ? Math.max(0f, anneeChamps) : (float) (((histoire.joueur.maintenant() / 24.0) % com.deldaran.politics.histoire.Terre.JOURS_PAR_AN + com.deldaran.politics.histoire.Terre.JOURS_PAR_AN) % com.deldaran.politics.histoire.Terre.JOURS_PAR_AN / com.deldaran.politics.histoire.Terre.JOURS_PAR_AN);
        vegetation.vent = (float) Math.hypot(ciel.locale.ventEst, ciel.locale.ventNord) + ciel.locale.orage * 8f;
        vegetation.neige = ciel.locale.neigeSol;
        vegetation.ventEst = ciel.locale.ventEst;
        vegetation.ventNord = ciel.locale.ventNord;
        if (histoire != null) villages.mettreAJour(histoire.population, monde.sphere, terrain, camera);
        if (histoire != null && habitants.foyers == null) habitants.foyers = this::foyersDe;
        if (histoire != null && meteo != null && camera.rayonPixels >= RAYON_JOUEUR * 0.3) {
            double cosLat = Math.cos(camera.latitude);
            habitants.heures = meteo.heures();
            habitants.mettreAJour(Gdx.graphics.getDeltaTime(), meteo.heureLocale(camera.longitude), accelerationMeteo(), villages, personnages, assembleur, terrain, joueur,
                    cosLat * Math.cos(camera.longitude), cosLat * Math.sin(camera.longitude), Math.sin(camera.latitude));
            // Lot H4 : ce que les gens importants ont gagné ou dépensé sous nos yeux va sur leur fiche (sur le fil de l'histoire).
            if (!habitants.bourses.isEmpty()) {
                List<long[]> b = new ArrayList<>();
                for (long[] x; (x = habitants.bourses.poll()) != null;) b.add(x);
                Joueur hj = histoire.joueur;
                histoire.demander(() -> {
                    for (long[] x : b) hj.gens().depenser(x[0], (int) x[1]);
                });
            }
            // Lot H5 : l'ouvrage des ouvriers du joueur fait tourner son atelier.
            if (!habitants.ouvrages.isEmpty()) {
                List<long[]> o = new ArrayList<>();
                for (long[] x; (x = habitants.ouvrages.poll()) != null;) o.add(x);
                Joueur hj = histoire.joueur;
                histoire.demander(() -> {
                    for (long[] x : o) hj.affaires().ouvrage(x[0], 1f, x[1] == 1L);
                });
            }
        }
        rues.mettreAJour(camera, villages, tuiles.metresPixel(), tuiles.demiVueMetres());
        tuiles.rues = rues;
        tuiles.dessiner(camera, true);
        vegetation.joueur = joueur;
        double cosCam = Math.cos(camera.latitude);
        incendies.avancer(Gdx.graphics.getDeltaTime(), villages, ciel.locale, cosCam * Math.cos(camera.longitude), cosCam * Math.sin(camera.longitude), Math.sin(camera.latitude));
        vegetation.dessiner(camera, tuiles, personnages, assembleur, villages, batiments);
        incendies.dessinerFumee(ciel.nuit);

    }

    void peupler() {
        double cosLat = Math.cos(camera.latitude);
        double x0 = cosLat * Math.cos(camera.longitude), y0 = cosLat * Math.sin(camera.longitude), z0 = Math.sin(camera.latitude);
        for (int k = 0; k < 30; k++) {
            Personnage p = new Personnage(assembleur.tirer(hasard.nextLong(), hasard.nextInt(Peuple.NOMBRE)), x0, y0, z0);
            terrain.echantillonner(x0, y0, z0, 11, point);
            p.altitude = point.altitude;
            p.deplacer((hasard.nextDouble() - 0.5) * 30.0, (hasard.nextDouble() - 0.5) * 20.0, 0f, terrain, point);
            p.enMarche = false;
            personnages.add(p);
        }
        errance = new float[personnages.size() * 3];
        chargement.commencer();
    }

    private void errer(float delta) {
        for (Personnage p : personnages) p.avancer(delta);
        for (int k = 0; k < errance.length / 3; k++) {
            if (personnages.get(k) == joueur || personnages.get(k).animation != null || personnages.get(k).vie != null) continue;
            errance[k * 3] -= delta;
            if (errance[k * 3] <= 0f) {
                errance[k * 3] = 1f + hasard.nextFloat() * 3f;
                int choix = hasard.nextInt(6);
                errance[k * 3 + 1] = choix == 0 ? 1f : choix == 1 ? -1f : 0f;
                errance[k * 3 + 2] = choix == 2 ? 1f : choix == 3 ? -1f : 0f;
            }
            personnages.get(k).deplacer(errance[k * 3 + 1] * 1.4 * delta, errance[k * 3 + 2] * 1.4 * delta, delta, terrain, point, obstacles);
        }
    }
    private void piloter(float delta) {
        boolean proche = camera.rayonPixels >= RAYON_JOUEUR;
        if (proche && (joueur == null || touche(Input.Keys.J))) placerJoueur();
        cameraController.libre = !proche || joueur == null;
        if (cameraController.libre) return;
        if (habitants.joueurATerre > 0f) {
            suivre();
            return;
        }
        if (incarne && histoire != null && (lutte != null || histoire.joueur.bataille != null)) {
            lutte(delta);
            suivre();
            return;
        }
        if (touche(Input.Keys.G)) habitants.agir(Habitants.SALUER, joueur);
        if (touche(Input.Keys.F)) habitants.agir(Habitants.FRAPPER, joueur);
        if (touche(Input.Keys.I) && joueur != null && incendies != null && incendies.allumerPres(villages, joueur.position[0], joueur.position[1], joueur.position[2], 3.0) == null) incendies.allumerSol(villages, joueur.position[0], joueur.position[1], joueur.position[2]);
        if (touche(Input.Keys.R)) habitants.agir(Habitants.OFFRIR, joueur);
        if (touche(Input.Keys.E)) habitants.agir(Habitants.EPARGNER, joueur);
        if (touche(Input.Keys.K)) habitants.agir(Habitants.ACHEVER, joueur);
        if (touche(Input.Keys.N)) essayerAnimation(1);
        if (touche(Input.Keys.B)) essayerAnimation(-1);
        if (touche(Input.Keys.V)) essayerAnimation(0);
        if (touche(Input.Keys.C)) joueur.arreter();
        reperer(delta);
        boolean auTravail = incarne && histoire != null && histoire.joueur.fiche() != null && histoire.joueur.travail().mainActif;
        if (auTravail) travaillerAMain(delta);
        if (incarne && histoire != null) dormirAction(delta);
        if (incarne && histoire != null && histoire.joueur.fiche() != null && histoire.joueur.sommeil == Joueur.EVEILLE) {
            gestesJoueur();
            if (!auTravail && touche(Input.Keys.Z)) jouer(histoire.joueur::dormir);
            if (touche(Input.Keys.M)) jouer(histoire.joueur::allerAuMarche);
            if (!auTravail && touche(Input.Keys.L)) jouer(histoire.joueur::allerTravailler);
            if (touche(Input.Keys.Q)) jouer(histoire.joueur::allerAuxMissions);
            if (touche(Input.Keys.U)) jouer(histoire.joueur::sac);
            if (touche(Input.Keys.X)) parler();
            if (versionTenue != histoire.joueur.versionTenue) habiller();
        }
        double est = 0.0, nord = 0.0;
        if (tenue(Input.Keys.W)) nord += 1.0;
        if (tenue(Input.Keys.S)) nord -= 1.0;
        if (tenue(Input.Keys.D)) est += 1.0;
        if (tenue(Input.Keys.A)) est -= 1.0;
        double longueur = Math.sqrt(est * est + nord * nord);
        if (longueur > 0.0) {
            float allure = incarne && histoire != null ? histoire.joueur.allure : 1f;
            boolean course = tenue(Input.Keys.SHIFT_LEFT) && allure > 0.6f;
            double vitesse = (course ? COURSE : MARCHE) * allure * delta / longueur;
            est *= vitesse;
            nord *= vitesse;
        }
        double cr = Math.cos(Personnage.rotationVue), sr = Math.sin(Personnage.rotationVue);
        double versEst = cr * est + sr * nord, versNord = -sr * est + cr * nord;
        joueur.deplacer(versEst, versNord, delta, terrain, point, obstacles);
        suivre();
        if (incarne && histoire != null && histoire.joueur.fiche() != null) {
            suivreBande(delta);
            camps(delta);
        }
    }

    /** Distance (m) à laquelle on peut parler à quelqu'un (touche X). */
    static final double PARLER_PORTEE = 2.5;
    /** La bande : au-delà de BANDE_TELEPORT mètres (ou coincé), un membre rejoint d'un coup ; rangs de trois, espacés de BANDE_RANG et BANDE_ECART. */
    static final double BANDE_TELEPORT = 40.0, BANDE_RANG = 1.1, BANDE_ECART = 0.9, BANDE_DEPART = 0.9, BANDE_ARRET = 0.3;

    /** Un membre de la bande qui marche derrière le joueur. */
    static final class Suiveur {
        long graine;
        Personnage corps;
        float coince;
        int arme = -2, armure = -2, ville = -1;
        /** Lot G3 : le chemin (coordonnées du village) calculé autour des maisons, l'étape suivie, et l'attente avant de le recalculer. */
        float[] chemin;
        int etape;
        float recalcul;
    }

    /** Lot G3 : le A* des villageois, pour la bande ; le chemin est recalculé toutes les BANDE_RECALCUL secondes. */
    private final Chemins cheminsBande = new Chemins();
    static final float BANDE_RECALCUL = 1.2f;
    private final double[] bandeA = new double[3], bandeB = new double[3], bandeEtape = new double[3];

    /** Un ancien membre de la bande qui rentre à pied chez lui avant de disparaître. */
    static final class Partant {
        Personnage corps;
        int ville;
        float reste;
    }

    final List<Partant> partants = new ArrayList<>();
    /** Secondes (réelles) entre deux paroles de la bande, au plus et au moins. */
    static final float PAROLE_MIN = 35f, PAROLE_MAX = 80f;
    private float paroleBande = 8f;

    final List<Suiveur> suiveurs = new ArrayList<>();
    private float bandeTic;
    private final double[] bandeCible = new double[3], bandeEcart = new double[2];

    /** Touche X : parler au villageois le plus proche (ou à un membre de la bande). */
    void parler() {
        Joueur j = histoire.joueur;
        Bande.Rencontre r = new Bande.Rencontre();
        Suiveur proche = null;
        double dp = PARLER_PORTEE;
        for (Suiveur s : suiveurs) {
            ecart(joueur.position, s.corps.position, bandeEcart);
            double d = Math.hypot(bandeEcart[0], bandeEcart[1]);
            if (d < dp) {
                dp = d;
                proche = s;
            }
        }
        if (proche != null) {
            r.graine = proche.graine;
            jouer(() -> j.bande().parler(r));
            return;
        }
        Villageois w = habitants.aQuiParler(joueur, PARLER_PORTEE);
        if (w == null) {
            habitants.annoncer("Personne a qui parler ici.");
            return;
        }
        habitants.retenir(w, joueur);
        Bande.Rencontre vue = rencontre(w);
        jouer(() -> j.bande().parler(vue));
    }

    /** Ce que la bande doit savoir du villageois à qui l'on parle : nom, situation, Lien avec le joueur. */
    Bande.Rencontre rencontre(Villageois w) {
        Bande.Rencontre r = new Bande.Rencontre();
        Relations.Lien l = habitants.relations.lien(w);
        r.graine = w.graine;
        r.nom = habitants.nom(w);
        r.femme = w.corps.apparence.femme;
        r.enfant = w.enfant;
        r.marie = w.conjoint != null;
        r.rival = l.rival;
        r.peuple = w.village.peuple;
        r.ville = w.village.ville;
        r.travail = w.travail;
        r.ruines = w.village.ruines > 0;
        r.lien = l;
        if (w.travail >= 0 && w.village.metiers != null && w.travail < w.village.metiers.length) {
            String m = Metiers.NOMS[w.village.metiers[w.travail]];
            r.metierTexte = "aeiouyh".indexOf(m.charAt(0)) >= 0 ? "de l'" + m : m.endsWith("e") ? "de la " + m : "du " + m;
        }
        return r;
    }

    /** L'écart (est, nord) en mètres pour aller de « de » à « vers », dans le repère local de « de ». */
    static void ecart(double[] de, double[] vers, double[] sortie) {
        double x = de[0], y = de[1], z = de[2];
        double ex = -y, ey = x, n = Math.sqrt(ex * ex + ey * ey);
        ex /= n;
        ey /= n;
        double nx = -z * ey, ny = z * ex, nz = x * ey - y * ex;
        double dx = vers[0] - x, dy = vers[1] - y, dz = vers[2] - z, r = Terrain.RAYON_KM * 1000.0;
        sortie[0] = (dx * ex + dy * ey) * r;
        sortie[1] = (dx * nx + dy * ny + dz * nz) * r;
    }

    /**
     * La bande suit le joueur à pied, en rangs de trois derrière lui. Les nouveaux venus quittent leur village (Habitants.retirer) ;
     * ceux qui partent disparaissent. Un membre trop loin ou coincé rejoint d'un coup. Toutes les deux secondes, les jours passent pour la bande.
     */
    void suivreBande(float delta) {
        Joueur j = histoire.joueur;
        Bande.Compagnon[] vue = j.bandeVue;
        for (Bande.Compagnon c : vue) {
            boolean la = false;
            for (Suiveur s : suiveurs) la |= s.graine == c.graine;
            if (la) continue;
            Suiveur s = new Suiveur();
            s.graine = c.graine;
            s.corps = habitants.retirer(c.graine);
            if (s.corps == null) s.corps = corpsLibres.remove(c.graine);
            s.ville = c.ville;
            if (s.corps == null) {
                Apparence a = null;
                for (int k = 0; k < 64; k++) {
                    a = assembleur.tirer(c.graine + k * 7919L, c.peuple);
                    if (a.femme == c.femme && !a.enfant) break;
                }
                Personnage.decaler(joueur.position, -joueur.dirEst * 2.0, -joueur.dirNord * 2.0, bandeCible);
                s.corps = new Personnage(a, bandeCible[0], bandeCible[1], bandeCible[2]);
                terrain.echantillonner(bandeCible[0], bandeCible[1], bandeCible[2], 11, point);
                s.corps.altitude = point.altitude;
            }
            suiveurs.add(s);
        }
        for (Suiveur s : suiveurs) for (Bande.Compagnon c : vue) {
            if (c.graine != s.graine || (s.arme == c.arme && s.armure == c.armure)) continue;
            s.arme = c.arme;
            s.armure = c.armure;
            armer(s.corps, c.arme, c.armure);
        }
        for (java.util.Iterator<Suiveur> it = suiveurs.iterator(); it.hasNext(); ) {
            Suiveur s = it.next();
            boolean garde = false;
            for (Bande.Compagnon c : vue) garde |= c.graine == s.graine;
            if (garde) continue;
            if (!cadavres.contains(s.corps)) {
                // Il rentre chez lui, à pied.
                s.corps.arreter();
                Partant p = new Partant();
                p.corps = s.corps;
                p.ville = s.ville;
                p.reste = 15f;
                partants.add(p);
            }
            habitants.partis.remove(s.graine);
            it.remove();
        }
        for (java.util.Iterator<Partant> it = partants.iterator(); it.hasNext(); ) {
            Partant p = it.next();
            p.reste -= delta;
            Villages.Village v = p.ville >= 0 ? villageDe(p.ville) : null;
            double[] but = v != null ? new double[] { v.x, v.y, v.z } : null;
            if (but != null) ecart(p.corps.position, but, bandeEcart);
            else ecart(joueur.position, p.corps.position, bandeEcart);
            double dl = Math.hypot(bandeEcart[0], bandeEcart[1]);
            if (p.reste <= 0f || (but != null && dl < 5.0)) {
                p.corps.arreter();
                personnages.remove(p.corps);
                it.remove();
                continue;
            }
            p.corps.deplacer(bandeEcart[0] / dl * MARCHE * delta, bandeEcart[1] / dl * MARCHE * delta, delta, terrain, point, obstacles);
        }
        // De temps en temps, quelqu'un de la bande dit quelque chose.
        paroleBande -= delta;
        if (paroleBande <= 0f && !suiveurs.isEmpty() && j.sommeil == Joueur.EVEILLE) {
            paroleBande = PAROLE_MIN + hasard.nextFloat() * (PAROLE_MAX - PAROLE_MIN);
            Suiveur qui = suiveurs.get(hasard.nextInt(suiveurs.size()));
            String[] p = j.bande().parole(qui.graine, hasard.nextLong());
            if (p != null) {
                habitants.direBande(p[0], p[1], p[2]);
                if (qui.corps.animation == null) qui.corps.jouer(Animations.toutes().get("parler"));
            }
        }
        bandeTic += delta;
        if (bandeTic > 2f) {
            bandeTic = 0f;
            histoire.demander(j::avancerBande);
        }
        double de = joueur.dirEst, dn = joueur.dirNord;
        for (int k = 0; k < suiveurs.size(); k++) {
            Suiveur s = suiveurs.get(k);
            Personnage c = s.corps;
            if (!personnages.contains(c)) personnages.add(c);
            double recul = BANDE_RANG * (k / 3 + 1) + 0.4, cote = (k % 3 - 1) * BANDE_ECART;
            Personnage.decaler(joueur.position, -de * recul + dn * cote, -dn * recul - de * cote, bandeCible);
            ecart(c.position, bandeCible, bandeEcart);
            double d = Math.hypot(bandeEcart[0], bandeEcart[1]);
            if (d > BANDE_TELEPORT || (s.coince > 2f && d > 3.0)) {
                terrain.echantillonner(bandeCible[0], bandeCible[1], bandeCible[2], 11, point);
                if (Personnage.profondeur(point) < Personnage.PROFONDEUR_MAX) {
                    System.arraycopy(bandeCible, 0, c.position, 0, 3);
                    c.altitude = point.altitude;
                }
                s.coince = 0f;
                continue;
            }
            if (d < BANDE_ARRET || (d < BANDE_DEPART && !c.enMarche)) {
                if (c.enMarche) c.deplacer(0.0, 0.0, delta, terrain, point, obstacles);
                s.coince = 0f;
                continue;
            }
            double vitesse = d > 5.0 ? COURSE : d > 2.0 ? MARCHE * 1.4 : MARCHE;
            // Lot G3 : dans un village, si une maison est entre lui et sa place, il suit un chemin qui la contourne.
            Villages.Village vg = villageVu;
            if (vg != null && vg.grille != null) {
                vg.local(c.position[0], c.position[1], c.position[2], bandeA);
                vg.local(bandeCible[0], bandeCible[1], bandeCible[2], bandeB);
                boolean dedans = vg.grille.indice(bandeA[0], bandeA[1]) >= 0 && vg.grille.indice(bandeB[0], bandeB[1]) >= 0;
                if (!dedans || Chemins.direct(vg.grille, bandeA[0], bandeA[1], bandeB[0], bandeB[1])) s.chemin = null;
                else {
                    s.recalcul -= delta;
                    if (s.chemin == null || s.recalcul <= 0f) {
                        s.chemin = cheminsBande.trouver(vg.grille, bandeA[0], bandeA[1], bandeB[0], bandeB[1]);
                        s.etape = 1;
                        s.recalcul = BANDE_RECALCUL;
                    }
                    while (s.chemin != null && s.etape * 2 + 1 < s.chemin.length && Math.hypot(s.chemin[s.etape * 2] - bandeA[0], s.chemin[s.etape * 2 + 1] - bandeA[1]) < 0.4) s.etape++;
                    if (s.chemin != null && s.etape * 2 + 1 < s.chemin.length) {
                        vg.monde(s.chemin[s.etape * 2], s.chemin[s.etape * 2 + 1], bandeEtape);
                        ecart(c.position, bandeEtape, bandeEcart);
                        d = Math.max(1e-3, Math.hypot(bandeEcart[0], bandeEcart[1]));
                    }
                }
            }
            double pas = Math.min(d, vitesse * delta);
            double x = c.position[0], y = c.position[1], z = c.position[2];
            c.deplacer(bandeEcart[0] / d * pas, bandeEcart[1] / d * pas, delta, terrain, point, obstacles);
            double bouge = Math.sqrt((c.position[0] - x) * (c.position[0] - x) + (c.position[1] - y) * (c.position[1] - y) + (c.position[2] - z) * (c.position[2] - z)) * Terrain.RAYON_KM * 1000.0;
            if (bouge < pas * 0.3) {
                // Bloqué : on tente de contourner par un côté, puis par l'autre.
                double gx = -bandeEcart[1] / d * pas, gy = bandeEcart[0] / d * pas;
                double ax = c.position[0], ay = c.position[1];
                int sens = (s.graine & 1L) == 0L ? 1 : -1;
                c.deplacer(gx * sens, gy * sens, delta, terrain, point, obstacles);
                if (c.position[0] == ax && c.position[1] == ay) c.deplacer(-gx * sens, -gy * sens, delta, terrain, point, obstacles);
                s.coince += delta;
            } else s.coince = 0f;
        }
    }

    // ---------------------------------------------------------------- Phase 9 : brigands et batailles

    /** Un brigand montré au campement : son repaire, son rang, son corps, et ce qu'il devient (CAMP, BATAILLE, A_TERRE, MORT, FUITE, LIGOTE). */
    static final class Brigand {
        int repaire, k;
        long graine;
        Personnage corps;
        int etat;
        float reste;
    }

    static final int B_CAMP = 0, B_A_TERRE = 2, B_MORT = 3, B_FUITE = 4, B_LIGOTE = 5;
    /** Le campement se peuple à moins de CAMP_VISIBLE m, s'efface au-delà de CAMP_OUBLI ; la rencontre s'ouvre à CAMP_RENCONTRE m, et ne revient
     *  qu'après s'être éloigné à plus de CAMP_QUITTE m. */
    static final double CLAIRIERE = 14.0;
    static final double CAMP_VISIBLE = 160.0, CAMP_OUBLI = 260.0, CAMP_RENCONTRE = 16.0, CAMP_QUITTE = 40.0;
    /** La bataille rejouée : durée de la charge, temps entre deux coups (au plus), durée totale visée des coups, attente finale. */
    static final float CHARGE = 1.2f, PAS_COUP = 0.3f, DUREE_COUPS = 10f, ATTENTE_FIN = 1.6f;
    private static final String[] POSES_CAMP = { "se_rechauffer_feu", "assis_sol", "aiguiser_lame", "croiser_bras", "se_rechauffer_feu", "regarder_autour" };
    private static final String[] ATTAQUES = { "attaque_taille", "attaque_revers", "attaque_estoc" };

    final List<Brigand> brigandsVus = new ArrayList<>();
    private final java.util.Map<Integer, double[]> campPositions = new java.util.HashMap<>();
    /** Les embuscades n'ont pas de clairière ; le mobilier de chaque campement (positions, plans), calculé une fois. */
    private final java.util.Set<Integer> campsRoute = new java.util.HashSet<>();
    private final java.util.Map<Integer, Object[]> mobilier = new java.util.HashMap<>();
    private final List<Batiments.Plan> mobilierPlans = new ArrayList<>();
    private float[] mobilierPos = new float[64];
    private float feuTemps;
    /** Le feu de camp : trois formes de flammes qui alternent ; les tentes à TENTE_DISTANCE m du feu. */
    private static final Batiments.Plan[] FEUX = { Batiments.objet(Batiments.FEU, 1.3, 1.3, 0.6, 0.0, 0), Batiments.objet(Batiments.FEU, 1.3, 1.3, 0.6, 0.0, 1), Batiments.objet(Batiments.FEU, 1.3, 1.3, 0.6, 0.0, 2) };
    static final double TENTE_DISTANCE = 6.8;
    /** Embuscades : un tirage tous les EMBUSCADE_PAS mètres parcourus loin des villes ; les brigands surgissent à EMBUSCADE_DISTANCE m devant. */
    static final double EMBUSCADE_PAS = 500.0, EMBUSCADE_DISTANCE = 13.0;
    private double routeMetres, routeE = 1.0, routeN;
    private final double[] routeAvant = new double[3];
    private boolean routeAvantOk;
    /** Les deuils déjà rendus aux familles (graines des brigands morts). */
    private final java.util.Set<Long> deuilsFaits = new java.util.HashSet<>();
    private float deuilTic;
    /** Corps laissés par une bataille, que la bande peut reprendre (brigands enrôlés), et cadavres de compagnons. */
    final java.util.Map<Long, Personnage> corpsLibres = new java.util.HashMap<>();
    final List<Personnage> cadavres = new ArrayList<>();
    private int campRencontre = -1;
    private final double[] campEcart = new double[2], campMonde = new double[3];

    /** La bataille en cours de lecture. */
    static final class Lutte {
        Combat.Resultat r;
        Personnage[] corps;
        int[] etat, cible;
        float[] pose, depart;
        float temps, pas, derniere;
        int prochain;
        boolean charge, finie;
    }

    Lutte lutte;

    /** Le village (le plus proche du joueur) de cette ville, ou null. */
    Villages.Village villageDe(int ville) {
        Villages.Village mieux = null;
        double d = Double.MAX_VALUE;
        for (int i = 0; i < villages.nombreVillages(); i++) {
            Villages.Village v = villages.village(i);
            if (v == null || v.ville != ville || v.positions == null) continue;
            double e = (v.x - joueur.position[0]) * (v.x - joueur.position[0]) + (v.y - joueur.position[1]) * (v.y - joueur.position[1]) + (v.z - joueur.position[2]) * (v.z - joueur.position[2]);
            if (e < d) {
                d = e;
                mieux = v;
            }
        }
        return mieux;
    }

    /** La position du campement : à r.bord mètres du bord de la ville, dans la direction r.angle ; si c'est l'eau, on tourne jusqu'à la terre ferme. */
    double[] campPosition(Brigands.Repaire r) {
        double[] p = campPositions.get(r.id);
        if (p != null) return p;
        Villages.Village v = villageDe(r.ville);
        if (v == null) return null;
        double rayon = v.rayon * Terrain.RAYON_KM * 1000.0;
        for (int essai = 0; essai < 24; essai++) {
            double a = r.angle + essai * 0.45, d = rayon + r.bord + (essai / 8) * 40.0;
            v.monde(Math.cos(a) * d, Math.sin(a) * d, campMonde);
            terrain.echantillonner(campMonde[0], campMonde[1], campMonde[2], 11, point);
            if (Personnage.profondeur(point) > 0f) continue;
            p = campMonde.clone();
            break;
        }
        if (p == null) p = campMonde.clone();
        campPositions.put(r.id, p);
        // Une clairière autour du campement : les arbres s'écartent.
        double[] c = new double[campPositions.size() * 4];
        int k = 0;
        for (java.util.Map.Entry<Integer, double[]> en : campPositions.entrySet()) {
            if (campsRoute.contains(en.getKey())) continue;
            double[] q = en.getValue();
            c[k++] = q[0];
            c[k++] = q[1];
            c[k++] = q[2];
            c[k++] = CLAIRIERE;
        }
        villages.clairieres = java.util.Arrays.copyOf(c, k);
        return p;
    }

    private double distanceA(double[] p) {
        ecart(joueur.position, p, campEcart);
        return Math.hypot(campEcart[0], campEcart[1]);
    }

    private static Animations.Animation anim(String nom) {
        return Animations.toutes().get(nom);
    }

    /** Un corps tiré de la graine comme les villageois (même apparence d'une partie à l'autre), habillé et armé. */
    Personnage corpsArme(long graine, int peuple, boolean femme, int arme, int armure, double[] ou) {
        Apparence a = null;
        for (int k = 0; k < 64; k++) {
            a = assembleur.tirer(graine + k * 7919L, peuple);
            if (a.femme == femme && !a.enfant) break;
        }
        Personnage c = new Personnage(a, ou[0], ou[1], ou[2]);
        terrain.echantillonner(ou[0], ou[1], ou[2], 11, point);
        c.altitude = point.altitude;
        armer(c, arme, armure);
        return c;
    }

    static void armer(Personnage c, int arme, int armure) {
        java.util.List<Integer> ids = new ArrayList<>();
        if (arme >= 0) ids.add(arme);
        if (armure >= 0) ids.add(armure);
        int[] t = new int[ids.size()];
        for (int k = 0; k < t.length; k++) t[k] = ids.get(k);
        c.habits = Biens.habiller(Tenue.tirer(c.apparence.graine, c.apparence.femme, c.apparence.enfant), t);
        if (arme >= 0 && Biens.get(arme).piece >= 0) c.habits.tenir(Biens.get(arme).piece);
    }

    /** Les campements de brigands autour du joueur : on les peuple, on les efface, et la rencontre s'ouvre quand on approche. */
    void camps(float delta) {
        Joueur j = histoire.joueur;
        Brigands.Repaire[] vue = j.repairesVue;
        int aujourdhui = (int) Math.floor(j.maintenant() / 24.0);
        route(j);
        for (Brigands.Repaire r : vue) {
            if (r.route && !campPositions.containsKey(r.id)) placerEmbuscade(r);
            double[] pos = campPosition(r);
            if (pos == null) continue;
            double d = distanceA(pos);
            if (d > CAMP_VISIBLE) continue;
            for (int k = 0; k < r.etats.length; k++) {
                if (r.etats[k] != Brigands.EN_VIE) continue;
                boolean la = false;
                for (Brigand b : brigandsVus) la |= b.repaire == r.id && b.k == k;
                if (la) continue;
                double ang = k * 2.39996, rayon = 2.2 + 0.9 * (k % 3);
                double[] ou = new double[3];
                Personnage.decaler(pos, Math.cos(ang) * rayon, Math.sin(ang) * rayon, ou);
                Brigand b = new Brigand();
                b.repaire = r.id;
                b.k = k;
                b.graine = r.graineDe(k);
                b.corps = corpsArme(b.graine, r.peuple, r.femmes[k], r.armes[k], r.armures[k], ou);
                b.corps.orienter(-Math.cos(ang), -Math.sin(ang));
                if (r.route) {
                    ecart(b.corps.position, joueur.position, campEcart);
                    b.corps.orienter(campEcart[0], campEcart[1]);
                }
                personnages.add(b.corps);
                brigandsVus.add(b);
            }
            // La rencontre : ils se lèvent et barrent la route.
            if (d < CAMP_RENCONTRE && campRencontre != r.id && r.passage != aujourdhui && lutte == null && j.bataille == null) {
                campRencontre = r.id;
                for (Brigand b : brigandsVus) {
                    if (b.repaire != r.id || b.etat != B_CAMP) continue;
                    ecart(b.corps.position, joueur.position, campEcart);
                    b.corps.arreter();
                    b.corps.orienter(campEcart[0], campEcart[1]);
                    Animations.Animation a = anim(b.k == 0 ? "defier" : hasard.nextBoolean() ? "garde_combat" : "menacer_doigt");
                    if (a != null) b.corps.jouer(a);
                    b.reste = 6f;
                }
                habitants.annoncer(r.route ? "Embuscade ! " + r.chef + " et ses brigands surgissent devant toi." : "\"Halte !\" " + r.chef + " et ses brigands te barrent la route.");
                int id = r.id;
                jouer(() -> j.brigands().rencontre(id));
            } else if (campRencontre == r.id && d > CAMP_QUITTE) campRencontre = -1;
        }
        mobilier(vue, delta);
        // Les brigands au campement vivent leur vie ; on efface ceux des campements lointains ou disparus.
        for (java.util.Iterator<Brigand> it = brigandsVus.iterator(); it.hasNext(); ) {
            Brigand b = it.next();
            double[] pos = campPositions.get(b.repaire);
            boolean actif = false;
            for (Brigands.Repaire r : vue) actif |= r.id == b.repaire && r.etats[b.k] == Brigands.EN_VIE;
            boolean loin = pos == null || distanceA(pos) > CAMP_OUBLI;
            boolean parti = b.etat == B_FUITE && b.reste <= 0f;
            if (!actif && !loin && b.etat == B_CAMP && lutte == null) {
                // Leur bande s'est dispersée (embuscade finie) : ils s'en vont en courant.
                b.etat = B_FUITE;
                b.reste = 6f;
                b.corps.arreter();
            }
            if (loin || parti) {
                b.corps.arreter();
                personnages.remove(b.corps);
                it.remove();
                continue;
            }
            b.reste -= delta;
            if (b.etat == B_FUITE) {
                ecart(joueur.position, b.corps.position, campEcart);
                double l = Math.max(0.1, Math.hypot(campEcart[0], campEcart[1]));
                b.corps.deplacer(campEcart[0] / l * COURSE * delta, campEcart[1] / l * COURSE * delta, delta, terrain, point, obstacles);
                if (b.corps.animation == null) b.corps.jouer(anim("fuir_debandade"));
            } else if (b.etat == B_CAMP && b.corps.animation == null && b.reste <= 0f && lutte == null) {
                Animations.Animation a = anim(POSES_CAMP[(int) Math.floorMod(b.graine, POSES_CAMP.length)]);
                if (a != null) b.corps.jouer(a);
            } else if (b.etat == B_A_TERRE && b.corps.animation == null) b.corps.jouer(anim("a_terre_assomme"));
            else if (b.etat == B_MORT && b.corps.animation == null) b.corps.jouer(anim("mort_au_sol"));
            else if (b.etat == B_LIGOTE && b.corps.animation == null) b.corps.jouer(anim("etre_ligote"));
        }
        for (java.util.Iterator<Personnage> it = cadavres.iterator(); it.hasNext(); ) {
            Personnage c = it.next();
            if (distanceA(c.position) > CAMP_OUBLI) {
                personnages.remove(c);
                it.remove();
            } else if (c.animation == null) c.jouer(anim("mort_au_sol"));
        }
        deuils(j, delta);
        // Le sort des prisonniers, choisi dans la fenêtre.
        int sort = j.sortPrisonniers;
        if (sort >= 0) {
            j.sortPrisonniers = -1;
            prisonniers(sort);
        }
        float sante = j.santeJoueur;
        if (sante >= 0f) {
            j.santeJoueur = -1f;
            joueur.sante = Math.min(joueur.sante, sante);
        }
    }

    /** Les embuscades : loin des villes (aucun village à moins de 400 m), tous les EMBUSCADE_PAS mètres parcourus, l'histoire tire au sort. */
    private void route(Joueur j) {
        if (routeAvantOk) {
            ecart(routeAvant, joueur.position, campEcart);
            double pas = Math.hypot(campEcart[0], campEcart[1]);
            if (pas > 0.01 && pas < 20.0) {
                routeE = campEcart[0] / pas;
                routeN = campEcart[1] / pas;
                if (villageVu == null && lutte == null && j.bataille == null && j.sommeil == Joueur.EVEILLE) {
                    routeMetres += pas;
                    if (routeMetres >= EMBUSCADE_PAS) {
                        routeMetres = 0.0;
                        int v = villeProche();
                        histoire.demander(() -> j.brigands().embuscade(v));
                    }
                }
            }
        }
        System.arraycopy(joueur.position, 0, routeAvant, 0, 3);
        routeAvantOk = true;
    }

    /** L'embuscade se tient devant le joueur, sur son chemin (sur la terre ferme). */
    private void placerEmbuscade(Brigands.Repaire r) {
        double[] p = new double[3];
        for (int essai = 0; essai < 8; essai++) {
            double a = Math.atan2(routeN, routeE) + (essai % 2 == 0 ? 1 : -1) * (essai / 2) * 0.6;
            Personnage.decaler(joueur.position, Math.cos(a) * EMBUSCADE_DISTANCE, Math.sin(a) * EMBUSCADE_DISTANCE, p);
            terrain.echantillonner(p[0], p[1], p[2], 11, point);
            if (Personnage.profondeur(point) <= 0f) break;
        }
        campPositions.put(r.id, p);
        campsRoute.add(r.id);
    }

    /** Le mobilier des campements proches : le feu de camp (ses flammes alternent trois formes) et des tentes tournées vers lui, posés parmi les bâtiments. */
    private void mobilier(Brigands.Repaire[] vue, float delta) {
        feuTemps = (feuTemps + delta) % 30f;
        int phase = (int) (feuTemps / 0.17f) % 3;
        mobilierPlans.clear();
        int n = 0;
        for (Brigands.Repaire r : vue) {
            if (r.route) continue;
            double[] pos = campPositions.get(r.id);
            if (pos == null || distanceA(pos) > CAMP_OUBLI) continue;
            Object[] m = mobilier.get(r.id);
            if (m == null) {
                int tentes = Math.min(4, 1 + r.effectif() / 3);
                float[] ou = new float[(tentes + 1) * 4];
                Batiments.Plan[] plans = new Batiments.Plan[tentes + 1];
                double depart = ((r.graine >>> 8) & 0xFFFF) / 65536.0 * Math.PI * 2.0;
                double[] q = new double[3];
                for (int t = 0; t <= tentes; t++) {
                    if (t == 0) System.arraycopy(pos, 0, q, 0, 3);
                    else {
                        double a = depart + (t - 1) * Math.PI * 2.0 / tentes;
                        Personnage.decaler(pos, Math.cos(a) * TENTE_DISTANCE, Math.sin(a) * TENTE_DISTANCE, q);
                        Batiments.Plan pl = Batiments.objet(Batiments.TENTE, 2.6, 2.2, 1.5, 0.0, (int) ((r.graine >>> (t * 3)) & 7));
                        pl.orienter(a + Math.PI);
                        plans[t] = pl;
                    }
                    terrain.echantillonner(q[0], q[1], q[2], 11, point);
                    ou[t * 4] = (float) q[0];
                    ou[t * 4 + 1] = (float) q[1];
                    ou[t * 4 + 2] = (float) q[2];
                    ou[t * 4 + 3] = (float) point.altitude;
                }
                m = new Object[] { ou, plans };
                mobilier.put(r.id, m);
            }
            float[] ou = (float[]) m[0];
            Batiments.Plan[] plans = (Batiments.Plan[]) m[1];
            for (int t = 0; t < plans.length; t++) {
                if (n * 4 + 4 > mobilierPos.length) mobilierPos = java.util.Arrays.copyOf(mobilierPos, mobilierPos.length * 2);
                System.arraycopy(ou, t * 4, mobilierPos, n * 4, 4);
                mobilierPlans.add(t == 0 ? FEUX[(phase + (int) (r.graine & 3L)) % 3] : plans[t]);
                n++;
            }
        }
        mobilierN = n;
    }

    /** Lot G3 : le nombre d'objets du campement posés cette image (animerObjets les pose avec ceux des moulins). */
    private int mobilierN;
    /** Une phase de la roue dure TOUR_ROUE s, une phase des ailes TOUR_AILES s. */
    static final float TOUR_ROUE = 0.2f, TOUR_AILES = 0.28f;
    private float moulinTemps;
    final java.util.Map<Long, Object[]> moulins = new java.util.HashMap<>();
    /** Lot G3 : les loups rôdent à LOUP_RAYON m (± 2) du joueur ; un pas dure LOUP_PAS s. Leurs images : angle × pas × pelage. */
    static final double LOUP_RAYON = 10.0;
    static final float LOUP_PAS = 0.14f;
    private final java.util.Map<Integer, Batiments.Plan> loupsPlans = new java.util.HashMap<>();
    private final double[] loupPos = new double[3];

    private Batiments.Plan loup(double cap, int pas, int pelage) {
        int angle = (int) Math.floorMod(Math.round(cap / (2.0 * Math.PI / Batiments.ANGLES)), (long) Batiments.ANGLES);
        int cle = (angle * Batiments.PAS_LOUP + pas) * 2 + pelage;
        Batiments.Plan pl = loupsPlans.get(cle);
        if (pl == null) {
            pl = Batiments.objet(Batiments.LOUP, 1.4, 0.45, 1.0, 0.0, pas + Batiments.PAS_LOUP * pelage);
            pl.orienter(cap);
            loupsPlans.put(cle, pl);
        }
        return pl;
    }

    /** Lot G3 : les objets qui bougent, posés chaque image parmi les bâtiments : le mobilier des campements, les roues et ailes des moulins. */
    void animerObjets(float delta) {
        if (villages == null) return;
        int n = mobilierN;
        mobilierN = 0;
        while (mobilierPlans.size() > n) mobilierPlans.remove(mobilierPlans.size() - 1);
        moulinTemps = (moulinTemps + delta) % 600f;
        int phaseRoue = (int) (moulinTemps / TOUR_ROUE) % Batiments.PHASES_MOULIN, phaseAiles = (int) (moulinTemps / TOUR_AILES) % Batiments.PHASES_MOULIN;
        for (int i = 0; i < villages.nombreVillages(); i++) {
            Villages.Village v = villages.village(i);
            if (v == null || v.positions == null || v.plans == null) continue;
            int fin = Math.min(v.plans.length, v.positions.length / 4);
            for (int q = 0; q < fin; q++) {
                Batiments.Plan pl = v.plans[q];
                if (pl == null || pl.ruine || pl.metier != Metiers.MOULIN) continue;
                long cle = ((long) v.ville << 20) | q;
                Object[] m = moulins.get(cle);
                if (m == null) {
                    Batiments.Plan[] phases = new Batiments.Plan[Batiments.PHASES_MOULIN];
                    for (int k = 0; k < phases.length; k++)
                        phases[k] = pl.roue ? Batiments.objet(Batiments.ROUE_EAU, 0.5, 3.6, 3.6, pl.angle, k) : Batiments.objet(Batiments.AILES, 7.6, 0.6, pl.murs, pl.angle, k);
                    // Où l'objet se pose : sur le flanc (roue) ou devant la façade (ailes), selon l'angle du moulin.
                    double lx = pl.roue ? pl.largeur / 2.0 + 0.35 : 0.0, lz = pl.roue ? 0.0 : pl.profondeur / 2.0 + 0.25;
                    double c = Math.cos(pl.angle), s = Math.sin(pl.angle);
                    double est = lx * c + lz * s, nord = lx * s - lz * c;
                    double[] base = { v.positions[q * 4], v.positions[q * 4 + 1], v.positions[q * 4 + 2] }, ou = new double[3];
                    Personnage.decaler(base, est, nord, ou);
                    m = new Object[] { phases, new float[] { (float) ou[0], (float) ou[1], (float) ou[2], (float) v.positions[q * 4 + 3] } };
                    moulins.put(cle, m);
                }
                Batiments.Plan[] phases = (Batiments.Plan[]) m[0];
                if (n * 4 + 4 > mobilierPos.length) mobilierPos = java.util.Arrays.copyOf(mobilierPos, mobilierPos.length * 2);
                System.arraycopy((float[]) m[1], 0, mobilierPos, n * 4, 4);
                mobilierPlans.add(phases[pl.roue ? phaseRoue : phaseAiles]);
                n++;
            }
        }
        // Lot G3 : les loups sortis rôdent en cercle autour du joueur, chacun à son allure.
        int loups = incarne && histoire != null && joueur != null ? histoire.joueur.loupsDehors : 0;
        for (int k = 0; k < loups; k++) {
            double vitesse = 0.3 + 0.06 * k, a = moulinTemps * vitesse + k * 2.0 * Math.PI / loups;
            double r = LOUP_RAYON + 2.0 * Math.sin(moulinTemps * 0.7 + k * 1.3);
            Personnage.decaler(joueur.position, Math.cos(a) * r, Math.sin(a) * r, loupPos);
            terrain.echantillonner(loupPos[0], loupPos[1], loupPos[2], 11, point);
            int pas = ((int) (moulinTemps / LOUP_PAS) + k) % Batiments.PAS_LOUP;
            if (n * 4 + 4 > mobilierPos.length) mobilierPos = java.util.Arrays.copyOf(mobilierPos, mobilierPos.length * 2);
            mobilierPos[n * 4] = (float) loupPos[0];
            mobilierPos[n * 4 + 1] = (float) loupPos[1];
            mobilierPos[n * 4 + 2] = (float) loupPos[2];
            mobilierPos[n * 4 + 3] = (float) point.altitude;
            mobilierPlans.add(loup(a + Math.PI / 2.0, pas, k % 2));
            n++;
        }
        villages.poserObjets(mobilierPos, mobilierPlans, n);
    }

    /** Les familles des brigands tués : dans leur ville, un habitant (toujours le même) s'en souvient ; parfois il jure vengeance et devient un rival. */
    private void deuils(Joueur j, float delta) {
        deuilTic += delta;
        if (deuilTic < 2f || villageVu == null) return;
        deuilTic = 0f;
        List<Villageois> gens = null;
        for (Brigands.Deuil d : j.deuilsVue) {
            if (d.ville != villageVu.ville || deuilsFaits.contains(d.graine)) continue;
            if (gens == null) {
                gens = new ArrayList<>();
                for (Villageois w : habitants.tous) if (w.village == villageVu && !w.enfant && !w.garde && w.activite != Villageois.MORT) gens.add(w);
                gens.sort(java.util.Comparator.comparingLong(w -> w.graine));
            }
            if (gens.isEmpty()) return;
            Villageois w = gens.get((int) Math.floorMod(d.graine, (long) gens.size()));
            deuilsFaits.add(d.graine);
            Relations.Lien l = habitants.relations.lien(w);
            boolean deja = l.mort;
            for (Relations.Souvenir s : l.souvenirs) deja |= s.type == Relations.PROCHE_TUE && d.nom.equals(s.sujet);
            if (deja) continue;
            double jour = habitants.relations.jourCourant;
            habitants.relations.retenir(w, Relations.PROCHE_TUE, jour, d.nom);
            boolean venger = d.execute || ((d.graine >>> 3) & 1L) == 0L;
            if (venger && habitants.relations.promouvoir(w, Relations.TITRE_VENGEANCE, jour)) {
                l.prochaineTraque = jour + 0.2;
                habitants.annoncer(l.nom + " a appris que tu as tué " + d.nom + ", de sa famille. On l'appelle désormais " + l.nomComplet() + ".");
            }
        }
    }

    /** Ce que deviennent, à l'écran, les brigands à terre après la bataille. */
    void prisonniers(int sort) {
        for (Brigand b : brigandsVus) {
            if (b.etat != B_A_TERRE) continue;
            b.corps.arreter();
            switch (sort) {
                case Brigands.LIVRER:
                    b.etat = B_LIGOTE;
                    b.corps.jouer(anim("etre_ligote"));
                    break;
                case Brigands.ENROLER:
                    // La bande les reprend (suivreBande) : le même corps se relève et suit.
                    corpsLibres.put(b.graine, b.corps);
                    b.corps.jouer(anim("se_relever"));
                    b.etat = -1;
                    break;
                case Brigands.RELACHER:
                    b.etat = B_FUITE;
                    b.reste = 5f;
                    b.corps.jouer(anim("fuir_debandade"));
                    break;
                default:
                    b.etat = B_MORT;
                    b.corps.jouer(anim("mourir"));
            }
        }
        if (sort == Brigands.EXECUTER) joueur.jouer(anim("attaque_estoc"));
        if (sort == Brigands.LIVRER) joueur.jouer(anim("ligoter"));
        // Les enrôlés ne sont plus des brigands du campement.
        brigandsVus.removeIf(b -> {
            if (b.etat != -1) return false;
            if (!corpsLibres.containsKey(b.graine)) personnages.remove(b.corps);
            return true;
        });
    }

    /** Le corps d'un combattant : le joueur, un membre de la bande, ou un brigand du campement. */
    private Personnage corpsDe(Combat.Combattant c, Combat.Resultat r) {
        if (c.joueur) return joueur;
        if (c.camp == Combat.NOUS) {
            for (Suiveur s : suiveurs) if (s.graine == c.graine) return s.corps;
            return null;
        }
        for (Brigand b : brigandsVus) if (b.repaire == r.repaire && b.k == c.brigand) return b.corps;
        return null;
    }

    private void commencerLutte(Combat.Resultat r) {
        Lutte l = new Lutte();
        l.r = r;
        int n = r.tous.size();
        l.corps = new Personnage[n];
        l.etat = new int[n];
        l.cible = new int[n];
        l.pose = new float[n];
        java.util.Arrays.fill(l.cible, -1);
        java.util.Arrays.fill(l.pose, -1f);
        for (int i = 0; i < n; i++) {
            l.corps[i] = corpsDe(r.tous.get(i), r);
            if (l.corps[i] != null) l.corps[i].arreter();
        }
        for (Brigand b : brigandsVus) if (b.repaire == r.repaire && b.etat == B_CAMP) b.reste = 999f;
        int coups = r.coups.size();
        l.pas = coups == 0 ? 0f : Math.min(PAS_COUP, DUREE_COUPS / coups);
        l.derniere = (r.reddition ? 0.5f : CHARGE) + coups * l.pas;
        lutte = l;
        joueur.arreter();
    }

    /** Rejoue la bataille : la charge, les coups un à un (attaques, parades, chutes, morts, fuites), puis le dénouement. */
    void lutte(float delta) {
        Joueur j = histoire.joueur;
        if (lutte == null) {
            Combat.Resultat r = j.bataille;
            if (r == null) return;
            commencerLutte(r);
        }
        Lutte l = lutte;
        l.temps += delta;
        int n = l.corps.length;
        if (!l.charge) {
            l.charge = true;
            for (int i = 0; i < n; i++) {
                Personnage c = l.corps[i];
                if (c == null) continue;
                Combat.Combattant x = l.r.tous.get(i);
                if (l.r.reddition && x.camp == Combat.EUX) {
                    c.jouer(anim("se_rendre"));
                    continue;
                }
                c.jouer(anim(x.camp == Combat.NOUS && x.joueur ? "cri_de_guerre" : hasard.nextBoolean() ? "cri_de_guerre" : "garde_combat"));
            }
        }
        // Chacun court vers son adversaire, sauf les archers.
        if (!l.r.reddition) for (int i = 0; i < n; i++) {
            Personnage c = l.corps[i];
            if (c == null || l.etat[i] != Combat.DEBOUT) continue;
            int cible = l.cible[i];
            if (cible < 0 || l.etat[cible] != Combat.DEBOUT || l.corps[cible] == null) {
                cible = -1;
                double dm = Double.MAX_VALUE;
                for (int q = 0; q < n; q++) {
                    if (l.corps[q] == null || l.etat[q] != Combat.DEBOUT || l.r.tous.get(q).camp == l.r.tous.get(i).camp) continue;
                    ecart(c.position, l.corps[q].position, campEcart);
                    double d = Math.hypot(campEcart[0], campEcart[1]);
                    if (d < dm) {
                        dm = d;
                        cible = q;
                    }
                }
                l.cible[i] = cible;
            }
            if (cible < 0) continue;
            ecart(c.position, l.corps[cible].position, campEcart);
            double d = Math.hypot(campEcart[0], campEcart[1]);
            int arme = l.r.tous.get(i).arme;
            boolean archer = arme >= 0 && (Biens.get(arme).competence == com.deldaran.politics.histoire.Fiche.ARC || Biens.get(arme).competence == com.deldaran.politics.histoire.Fiche.ARBALETE);
            if (d > (archer ? 12.0 : 1.1) && (c.animation == null || c.animation.mobile || c.animation.mode == Animations.BOUCLE)) {
                double pas = Math.min(d - 1.0, COURSE * 0.9 * delta);
                c.deplacer(campEcart[0] / d * pas, campEcart[1] / d * pas, delta, terrain, point, obstacles);
            } else c.orienter(campEcart[0], campEcart[1]);
        }
        // Les coups.
        float debut = l.r.reddition ? 0.5f : CHARGE;
        while (l.prochain < l.r.coups.size() && debut + l.prochain * l.pas <= l.temps) {
            Combat.Coup coup = l.r.coups.get(l.prochain++);
            int a = coup.attaquant, d = coup.defenseur;
            Personnage pa = l.corps[a], pd = l.corps[d];
            if (coup.etat == Combat.FUI && a == d) {
                l.etat[d] = Combat.FUI;
                if (pd != null) pd.jouer(anim("fuir_debandade"));
                continue;
            }
            if (pa != null && pd != null) {
                ecart(pa.position, pd.position, campEcart);
                pa.orienter(campEcart[0], campEcart[1]);
                int arme = l.r.tous.get(a).arme;
                Biens.Bien b = arme >= 0 ? Biens.get(arme) : null;
                String nom = b == null ? "coup_de_poing" : b.competence == com.deldaran.politics.histoire.Fiche.ARC || b.competence == com.deldaran.politics.histoire.Fiche.ARBALETE ? "tirer_arc" : ATTAQUES[hasard.nextInt(ATTAQUES.length)];
                pa.jouer(anim(nom));
            }
            l.cible[a] = d;
            if (pd == null) {
                l.etat[d] = coup.etat;
                continue;
            }
            if (coup.etat == Combat.A_TERRE || coup.etat == Combat.MORT) {
                pd.jouer(anim(coup.etat == Combat.MORT ? "mourir" : "tomber_au_sol"));
                l.pose[d] = l.temps + 1.0f;
            } else if (coup.touche) pd.jouer(anim(hasard.nextFloat() < 0.3f ? "chanceler" : "etre_touche"));
            else pd.jouer(anim(hasard.nextBoolean() ? "parer" : "esquiver"));
            l.etat[d] = coup.etat;
        }
        // Les poses finales (à terre, mort), et les fuyards qui s'éloignent.
        for (int i = 0; i < n; i++) {
            Personnage c = l.corps[i];
            if (c == null) continue;
            if (l.pose[i] >= 0f && l.temps >= l.pose[i]) {
                l.pose[i] = -1f;
                c.jouer(anim(l.etat[i] == Combat.MORT ? "mort_au_sol" : "a_terre_assomme"));
            }
            if (l.etat[i] == Combat.FUI && c != joueur) {
                ecart(joueur.position, c.position, campEcart);
                double dl = Math.max(0.1, Math.hypot(campEcart[0], campEcart[1]));
                c.deplacer(campEcart[0] / dl * COURSE * delta, campEcart[1] / dl * COURSE * delta, delta, terrain, point, obstacles);
            }
        }
        if (!l.finie && l.prochain >= l.r.coups.size() && l.temps > l.derniere + ATTENTE_FIN) {
            l.finie = true;
            // L'écran garde la trace : brigands à terre, morts, en fuite ; compagnons morts laissés au sol.
            for (int i = 0; i < n; i++) {
                Combat.Combattant x = l.r.tous.get(i);
                Personnage c = l.corps[i];
                if (c == null) continue;
                if (x.camp == Combat.EUX) {
                    for (Brigand b : brigandsVus) {
                        if (b.corps != c) continue;
                        b.etat = l.etat[i] == Combat.MORT ? B_MORT : l.etat[i] == Combat.A_TERRE ? B_A_TERRE : l.etat[i] == Combat.FUI ? B_FUITE : B_CAMP;
                        b.reste = b.etat == B_FUITE ? 5f : 2f;
                    }
                } else if (l.etat[i] == Combat.MORT) cadavres.add(c);
                else if (l.etat[i] == Combat.A_TERRE || (x.joueur && l.r.vainqueur != Combat.NOUS && x.etat == Combat.A_TERRE)) c.jouer(anim("se_relever"));
                else if (c != joueur) c.arreter();
            }
            if (l.r.vainqueur == Combat.NOUS) joueur.jouer(anim(l.r.reddition ? "mains_sur_hanches" : "cri_de_guerre"));
            lutte = null;
            jouer(() -> j.brigands().apresBataille());
        }
    }

    /** Le repère d'une mission de chasse : direction et distance du campement. */
    String repereCamp() {
        Joueur j = histoire.joueur;
        if (j.cibleLieu != -2 || j.cibleCamp < 0 || j.cibleTexte.isEmpty()) return null;
        for (Brigands.Repaire r : j.repairesVue) {
            if (r.id != j.cibleCamp) continue;
            double[] pos = campPosition(r);
            if (pos == null) return "Mission : " + j.cibleTexte + ", près d'une autre ville";
            ecart(joueur.position, pos, campEcart);
            double d = Math.hypot(campEcart[0], campEcart[1]);
            if (d > 5000.0) return "Mission : " + j.cibleTexte + ", près d'une autre ville";
            int k = (int) Math.floorMod(Math.round(Math.atan2(campEcart[1], campEcart[0]) / (Math.PI / 4.0)), 8);
            return "Mission : " + j.cibleTexte + ", leur campement à " + Math.round(d) + " m " + DIRECTIONS[k];
        }
        return null;
    }

    private double travailDerniere = Double.NaN, travailCumul;
    private volatile boolean travailAttente, travailFin;
    private float gesteTravailReste;

    /**
     * Travail à la main : le personnage enchaîne les gestes du métier pendant que le temps défile (ACCELERATION_TRAVAIL) ;
     * chaque quart d'heure de jeu est compté par Travail.heureAMain. Bouger, appuyer sur L, l'épuisement ou la fermeture arrêtent.
     */
    void travaillerAMain(float delta) {
        com.deldaran.politics.histoire.Travail tr = histoire.joueur.travail();
        boolean bouge = tenue(Input.Keys.W) || tenue(Input.Keys.A) || tenue(Input.Keys.S) || tenue(Input.Keys.D);
        if (!travailFin && (bouge || touche(Input.Keys.L) || tr.mainArret != null)) {
            arreterTravail();
            return;
        }
        if (travailFin) return;
        double h = meteo.heures();
        if (Double.isNaN(travailDerniere)) travailDerniere = h;
        travailCumul += Math.max(0.0, h - travailDerniere);
        travailDerniere = h;
        if (travailCumul >= com.deldaran.politics.histoire.Travail.TRANCHE && !travailAttente) {
            travailCumul -= com.deldaran.politics.histoire.Travail.TRANCHE;
            travailAttente = true;
            histoire.demander(() -> {
                tr.heureAMain(com.deldaran.politics.histoire.Travail.TRANCHE);
                travailAttente = false;
            });
        }
        gesteTravailReste -= delta;
        if (gesteTravailReste <= 0f || joueur.animation == null) {
            Animations.Animation[] g = Habitants.gestesJoueur(tr.mainLieu);
            Animations.Animation a = null;
            if (tr.fatigue > 70f && hasard.nextFloat() < 0.25f) a = Animations.toutes().get("s_eponger_front");
            if (a == null && g.length > 0) a = g[hasard.nextInt(g.length)];
            if (a != null) {
                joueur.jouer(a);
                gesteTravailReste = a.mode == Animations.UNE_FOIS ? a.duree + 0.2f : 3.5f;
            }
        }
    }

    private boolean sommeilVu, reveilDemande;
    private double coucheVue = Double.NaN;
    private float sommeilDuree;

    /**
     * Sommeil : à l'auberge le personnage disparaît dans sa chambre, dehors il s'allonge ; le temps file (ACCELERATION_SOMMEIL).
     * Bouger, ou l'heure du réveil, le réveille.
     */
    void dormirAction(float delta) {
        Joueur j = histoire.joueur;
        if (j.sommeil == Joueur.EVEILLE) {
            if (sommeilVu) {
                sommeilVu = false;
                reveilDemande = false;
                if (!personnages.contains(joueur)) personnages.add(joueur);
                joueur.arreter();
            }
            return;
        }
        if (!sommeilVu || j.couche != coucheVue) {
            sommeilVu = true;
            coucheVue = j.couche;
            if (!personnages.contains(joueur)) personnages.add(joueur);
            reveilDemande = false;
            sommeilDuree = 0f;
            if (j.sommeil == Joueur.CHAMBRE) personnages.remove(joueur);
            else {
                Animations.Animation a = Animations.toutes().get("dormir");
                if (a != null) joueur.jouer(a);
            }
        }
        sommeilDuree += delta;
        boolean bouge = tenue(Input.Keys.W) || tenue(Input.Keys.A) || tenue(Input.Keys.S) || tenue(Input.Keys.D);
        if (!reveilDemande && ((bouge && sommeilDuree > 0.5f) || j.reveilPasse())) {
            reveilDemande = true;
            histoire.demander(j::reveiller);
        }
    }

    /** Arrête le travail à la main et montre le bilan. */
    void arreterTravail() {
        travailFin = true;
        travailDerniere = Double.NaN;
        travailCumul = 0.0;
        joueur.arreter();
        jouer(() -> {
            com.deldaran.politics.histoire.Joueur.Scene s = histoire.joueur.finirTravailAMain();
            travailFin = false;
            return s;
        });
    }

    /** Les gestes demandés par les scènes (payer, recevoir, lire, livrer) et la réaction du marchand. */
    void gestesJoueur() {
        Joueur j = histoire.joueur;
        String g = j.gesteJoueur;
        if (g != null) {
            j.gesteJoueur = null;
            Animations.Animation a = Animations.toutes().get(g);
            if (a != null && joueur != null) joueur.jouer(a);
        }
        int r = j.reactionLieu;
        if (r >= 0) {
            j.reactionLieu = -1;
            String geste = j.reactionGeste;
            j.reactionGeste = null;
            if (geste == null) geste = hasard.nextBoolean() ? "montrer_marchandise" : "saluer_main";
            // Le marchand sort de son atelier et vient servir.
            if (villageVu != null && r < Metiers.NOMBRE && batimentsVus != null && batimentsVus[r] >= 0) habitants.servir(villageVu, batimentsVus[r], joueur, geste);
            // Lot G3 : sur la place, le marchand de l'étal le plus proche vient servir.
            else if (villageVu != null && r == Joueur.LIEU_MARCHE) habitants.servirEtal(villageVu, joueur, geste);
        }
    }

    String bandeau() {
        if (joueur == null || cameraController.libre) return null;
        String sante = joueur.sante < 0.999f ? String.format("Sante %.0f%%", joueur.sante * 100f) : null;
        if (incarne && histoire != null && histoire.joueur.fiche() != null && histoire.joueur.corps().faim >= com.deldaran.politics.histoire.Corps.FAIM_ALERTE) {
            String f = histoire.joueur.corps().faible() ? "Affame (U pour manger)" : "Faim";
            sante = sante == null ? f : sante + " · " + f;
        }
        String texte = habitants.annonceReste > 0f ? habitants.annonce : null;
        if (incarne && histoire != null && histoire.joueur.travail().mainActif) {
            String hud = histoire.joueur.travail().mainTexte;
            texte = texte == null ? hud : texte + "\n" + hud;
        }
        if (incarne && histoire != null && histoire.joueur.sommeil != Joueur.EVEILLE) {
            String z = (histoire.joueur.sommeil == Joueur.CHAMBRE ? (histoire.joueur.dortChezSoi ? "Tu dors chez toi" : "Tu dors à l'auberge") : "Tu dors à la belle étoile") + " · " + (int) meteo.heureLocale(camera.longitude) + " h  (bouge pour te lever)";
            texte = texte == null ? z : texte + "\n" + z;
        }
        if (texte == null && incarne && histoire != null && histoire.joueur.fiche() != null && histoire.joueur.devantMaison && histoire.joueur.sommeil == Joueur.EVEILLE)
            texte = "Devant toi : ta maison (Z pour dormir, M pour ton coffre)";
        if (texte == null && devantRuine && devant == null && incarne && histoire != null && histoire.joueur.fiche() != null)
            texte = "Devant toi : une ruine (M pour y bâtir un atelier)";
        if (texte == null && tableauProche && incarne && histoire != null && histoire.joueur.fiche() != null) texte = "Devant toi : le tableau des annonces (Q pour le lire)";
        if (texte == null && devantMien && incarne && histoire != null && histoire.joueur.fiche() != null) texte = "Devant toi : " + devant + " (M pour le gérer)";
        if (texte == null && devant != null && incarne && histoire != null && histoire.joueur.fiche() != null) texte = "Devant toi : " + devant + " (M pour commercer, L pour travailler" + (devant.equals("le marché") ? ", Q pour le tableau des missions)" : ")");
        if (texte == null && terrainDevant && incarne && histoire != null && histoire.joueur.fiche() != null) texte = "Devant toi : un terrain libre (M pour y bâtir)";
        if (repere != null) texte = texte == null ? repere : texte + "\n" + repere;
        if (texte == null) return sante;
        return sante == null ? texte : texte + "\n" + sante;
    }

    void essayerAnimation(int pas) {
        if (joueur == null) return;
        java.util.List<Animations.Animation> liste = Animations.toutes().liste;
        if (pas != 0 || animationTest < 0) animationTest = Math.floorMod(animationTest + (pas == 0 ? 1 : pas), liste.size());
        Animations.Animation a = liste.get(animationTest);
        joueur.arreter();
        Personnage proche = null;
        double meilleur = 30.0 / (Terrain.RAYON_KM * 1000.0);
        for (Personnage p : personnages) {
            if (p == joueur) continue;
            double dx = p.position[0] - joueur.position[0], dy = p.position[1] - joueur.position[1], dz = p.position[2] - joueur.position[2];
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (d < meilleur) {
                meilleur = d;
                proche = p;
            }
        }
        if (a.partenaire != null && proche != null) {
            proche.arreter();
            joueur.jouer(a, proche);
        } else joueur.jouer(a);
    }

    String nomAnimation(Personnage p) {
        if (p == null || p.animation == null) return "-";
        Animations.Animation a = p.animation;
        return a.nom + " (" + a.categorie + ", " + (a.id + 1) + "/" + Animations.toutes().liste.size() + (p.partenaire != null ? ", avec partenaire" : "") + ")";
    }

    private int versionTenue = -1;
    private float reperage;
    /** Nom du lieu de commerce devant lequel se trouve le joueur, affiché dans le bandeau. */
    private String devant;
    /** Le bâtiment devant lequel on se tient est un de mes ateliers. */
    private boolean devantMien;
    private final double[] local = new double[2], localBatiment = new double[2];

    /** Trois fois par seconde : dans quel village est le joueur, et à quelle distance de chaque atelier, de la place et de la sortie (phase 3). */
    private int villeEntree = -1, lieuDevant = -1;
    /** Le village où l'on est et, par métier, le bâtiment le plus proche (pour faire réagir le marchand). */
    Villages.Village villageVu;
    /** Lot D : le joueur est devant une ruine où il pourrait bâtir. */
    boolean devantRuine;
    /** Lot G5 : un terrain libre devant le joueur arrêté depuis TERRAIN_ARRET s ; les constructions déjà prises en compte. */
    boolean terrainDevant;
    static final float TERRAIN_ARRET = 1.5f;
    private float immobile;
    private int constructionsVersion = -1;
    private final Villages.Ajout terrainEssai = new Villages.Ajout();
    private final double[] terrainCentre = new double[2];

    /** Lot G5 : les bâtiments du joueur d'une ville, tels que l'histoire les publie (ville, a, b, angle, métier, chantier). */
    private java.util.List<Villages.Ajout> ajoutsDe(int ville) {
        float[] t = histoire == null ? null : histoire.joueur.constructionsVue;
        java.util.List<Villages.Ajout> l = new java.util.ArrayList<>();
        for (int k = 0; t != null && k + 5 < t.length; k += 6) {
            if ((int) t[k] != ville) continue;
            Villages.Ajout x = new Villages.Ajout();
            x.a = t[k + 1];
            x.b = t[k + 2];
            x.angle = t[k + 3];
            x.metier = (int) t[k + 4];
            x.chantier = t[k + 5] > 0f;
            l.add(x);
        }
        return l;
    }

    // ---- Lot H2 : les habitants de chaque village, répartis par l'histoire (sur son fil), refaits toutes les FOYERS_DUREE s.
    private final java.util.Map<Integer, com.deldaran.politics.histoire.Gens.Foyers> foyersPrets = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<Integer, Long> foyersDemandes = new java.util.concurrent.ConcurrentHashMap<>();
    static final long FOYERS_DUREE = 30_000_000_000L;

    /** Les habitants du village (ceux de la dernière répartition) ; en demande une nouvelle à l'histoire quand elle a vieilli. */
    com.deldaran.politics.histoire.Gens.Foyers foyersDe(Villages.Village v) {
        com.deldaran.politics.histoire.Gens.Foyers fo = foyersPrets.get(v.ville);
        Long quand = foyersDemandes.get(v.ville);
        long maintenant = System.nanoTime();
        if (histoire != null && v.plans != null && v.positions != null && (quand == null || maintenant - quand > FOYERS_DUREE)) {
            foyersDemandes.put(v.ville, maintenant);
            int n = Math.min(v.plans.length, Math.min(v.metiers.length, v.positions.length / 4));
            int[] lieux = new int[n];
            boolean[] logeable = new boolean[n];
            for (int q = 0; q < n; q++) {
                lieux[q] = -1;
                Batiments.Plan pl = v.plans[q];
                if (pl == null || pl.ruine) continue;
                int m = v.metiers[q];
                if (q >= v.habitables && pl.style == Batiments.ETAL) lieux[q] = com.deldaran.politics.histoire.Gens.LIEU_ETAL;
                else if (q < v.habitables) {
                    if (m > Metiers.MAISON && m < Metiers.NOMBRE) lieux[q] = m;
                    logeable[q] = Habitants.logement(m) && !habitants.maisonsJoueur.contains(v.ville + ":" + q);
                } else if (pl.joueur && m > Metiers.MAISON && m < Metiers.NOMBRE) lieux[q] = com.deldaran.politics.histoire.Gens.JOUEUR + m;
            }
            int ville = v.ville;
            Joueur j = histoire.joueur;
            histoire.demander(() -> foyersPrets.put(ville, j.gens().foyers(ville, lieux, logeable)));
        }
        return fo;
    }

    /** Lot G5 : la direction où regarde le joueur, en radians (0 vers l'est, π/2 vers le nord). */
    private static double regard(int direction) {
        switch (direction) {
            case Personnage.DROITE: return 0.0;
            case Personnage.HAUT_DROITE: return Math.PI / 4.0;
            case Personnage.HAUT: return Math.PI / 2.0;
            case Personnage.HAUT_GAUCHE: return Math.PI * 0.75;
            case Personnage.GAUCHE: return Math.PI;
            case Personnage.BAS_GAUCHE: return -Math.PI * 0.75;
            case Personnage.BAS_DROITE: return -Math.PI / 4.0;
            default: return -Math.PI / 2.0;
        }
    }

    /** Lot G5 : les métiers (un bit chacun) dont l'atelier tient sur le terrain devant le joueur (les plantes du village seront arrachées). */
    private long terrainsLibres(Villages.Village w, double pa, double pb, double angle) {
        long libres = 0L;
        terrainEssai.a = pa;
        terrainEssai.b = pb;
        terrainEssai.angle = angle;
        for (int m = Metiers.MAISON + 1; m < Metiers.NOMBRE; m++) {
            terrainEssai.metier = m;
            Batiments.Plan pl = Villages.planAjout(w.ville, terrainEssai, terrainCentre);
            if (Villages.terrainLibre(w.grille, pl, terrainCentre[0], terrainCentre[1])) libres |= 1L << m;
        }
        return libres;
    }
    float gensTic = 2f;
    final double[] ecartCible = new double[2];
    int[] batimentsVus;
    /** Ligne du bandeau qui guide vers la mission en cours. */
    private String repere;
    /** La mission dont le destinataire est déjà venu à la rencontre du joueur (pour ne le faire venir qu'une fois). */
    private String missionAccueillie = "";
    /** À cette distance (m) du lieu de la mission, le destinataire sort et vient t'attendre. */
    static final double MISSION_ACCUEIL = 12.0;
    /** Le joueur est à moins de TABLEAU_PROCHE mètres du tableau des annonces. */
    private boolean tableauProche;
    static final double TABLEAU_PROCHE = 2.5;
    /** Distance (m, depuis le bord approché de la maison) à laquelle on est devant sa porte. */
    static final float MAISON_PROCHE = 6f;
    private static final String[] DIRECTIONS = { "à l'est", "au nord-est", "au nord", "au nord-ouest", "à l'ouest", "au sud-ouest", "au sud", "au sud-est" };

    void reperer(float delta) {
        reperage -= delta;
        if (reperage > 0f || histoire == null || joueur == null || villages == null) return;
        reperage = 0.3f;
        Joueur j = histoire.joueur;
        // Lot G5 : quand les constructions du joueur changent, les villages concernés se refont (avec leurs habitants).
        if (villages.ajouts == null) villages.ajouts = this::ajoutsDe;
        int cv = j.constructionsVersion;
        if (cv != constructionsVersion) {
            constructionsVersion = cv;
            float[] t = j.constructionsVue;
            java.util.Set<Integer> refaire = new java.util.HashSet<>();
            for (int k = 0; t != null && k + 5 < t.length; k += 6) refaire.add((int) t[k]);
            for (int v : refaire) {
                Villages.Village vieux = villages.oublier(v);
                if (vieux != null) habitants.oublier(vieux, personnages);
                foyersPrets.remove(v);
                foyersDemandes.remove(v);
            }
        }
        // Les messages du joueur (envie accomplie, renom…) passent dans le bandeau.
        for (String a = j.prendreAnnonce(); a != null; a = j.prendreAnnonce()) habitants.annoncer(a);
        Villages.Village w = null;
        double ecart = Double.MAX_VALUE;
        double[] p = joueur.position;
        for (int i = 0; i < villages.nombreVillages(); i++) {
            Villages.Village v = villages.village(i);
            if (v == null || v.positions == null || v.plans == null) continue;
            v.local(p[0], p[1], p[2], local);
            double d = Math.hypot(local[0], local[1]) - v.rayon * Terrain.RAYON_KM * 1000.0;
            if (d < ecart) {
                ecart = d;
                w = v;
            }
        }
        repere = j.cibleVille >= 0 && !j.cibleTexte.isEmpty() ? "Mission : " + j.cibleTexte + ", dans une autre ville" : null;
        String camp = repereCamp();
        if (camp != null) repere = camp;
        if (w == null || ecart > 400.0) {
            j.maisonProche = -1;
            j.devantMaison = false;
            j.ruineProche = -1;
            devantRuine = false;
            terrainDevant = false;
            j.terrainLibre = null;
            villageVu = null;
            tableauProche = false;
            j.devantTableau = false;
            j.villeVue = -1;
            j.distances = null;
            devant = null;
            villeEntree = -1;
            lieuDevant = -1;
            habitants.joueurPalier = 0;
            return;
        }
        if (w.ville != villeEntree && incarne && j.fiche() != null) {
            villeEntree = w.ville;
            int v = w.ville;
            histoire.demander(() -> j.entrer(v));
        }
        if (j.fiche() != null) {
            habitants.joueurPalier = j.renommee().palierEn(w.ville);
            habitants.joueurReputation = j.renommee().reputationAupres(w.ville);
            String n = j.fiche().nom;
            habitants.joueurNom = n.contains(" ") ? n.substring(0, n.indexOf(' ')) : n;
            habitants.joueurSurnom = j.renommee().surnomEn(w.ville);
        }
        float[] d = new float[Joueur.LIEUX];
        java.util.Arrays.fill(d, Float.NaN);
        int[] batiment = new int[Metiers.NOMBRE];
        java.util.Arrays.fill(batiment, -1);
        w.local(p[0], p[1], p[2], local);
        double pa = local[0], pb = local[1];
        int n = Math.min(w.plans.length, w.positions.length / 4);
        for (int q = 0; q < n; q++) {
            Batiments.Plan plan = w.plans[q];
            if (plan == null || plan.ruine) continue;
            boolean etal = q >= w.habitables && plan.style == Batiments.ETAL;
            int m = etal ? -1 : plan.metier;
            if (!etal && (m <= Metiers.MAISON || m >= Metiers.NOMBRE)) continue;
            w.local(w.positions[q * 4], w.positions[q * 4 + 1], w.positions[q * 4 + 2], localBatiment);
            float dist = (float) Math.max(0.0, Math.hypot(pa - localBatiment[0], pb - localBatiment[1]) - plan.rayon());
            int i = etal ? Joueur.LIEU_MARCHE : m;
            if (Float.isNaN(d[i]) || dist < d[i]) {
                d[i] = dist;
                if (!etal) batiment[m] = q;
            }
        }
        // Le maître de chaque atelier : le premier adulte du village qui travaille dans le bâtiment le plus proche de ce métier (phase 4).
        Relations.Lien[] maitres = new Relations.Lien[Joueur.LIEUX];
        for (Villageois o : habitants.tous) {
            if (o.village != w || o.enfant || o.travail < 0 || o.travail >= w.metiers.length) continue;
            int m = w.metiers[o.travail];
            if (m <= Metiers.MAISON || m >= Metiers.NOMBRE || batiment[m] != o.travail || maitres[m] != null) continue;
            maitres[m] = habitants.relations.lien(o);
        }
        j.maitres = maitres;
        j.relations = habitants.relations;
        villageVu = w;
        batimentsVus = batiment;
        tableauProche = false;
        if (w.tableau >= 0 && w.tableau < n) {
            w.local(w.positions[w.tableau * 4], w.positions[w.tableau * 4 + 1], w.positions[w.tableau * 4 + 2], localBatiment);
            tableauProche = Math.hypot(pa - localBatiment[0], pb - localBatiment[1]) < TABLEAU_PROCHE + w.plans[w.tableau].rayon() * 0.5;
        }
        j.devantTableau = tableauProche;
        // Le repère de la mission en cours : direction et distance du lieu où la rendre.
        if (j.cibleVille == w.ville && !j.cibleTexte.isEmpty() && j.cibleLieu >= 0) {
            int cl = j.cibleLieu;
            double ta = 0.0, tb = 0.0;
            if (cl >= 0 && cl < Metiers.NOMBRE && batiment[cl] >= 0) {
                int q = batiment[cl];
                w.local(w.positions[q * 4], w.positions[q * 4 + 1], w.positions[q * 4 + 2], localBatiment);
                ta = localBatiment[0];
                tb = localBatiment[1];
            }
            double dx = ta - pa, dy = tb - pb, dist = Math.hypot(dx, dy);
            String ou = cl == Joueur.LIEU_MARCHE || cl >= Metiers.NOMBRE || batiment[cl] < 0 ? "la place" : Biens.nomBoutique(cl).toLowerCase();
            if (dist < MISSION_ACCUEIL && cl >= 0 && cl < Metiers.NOMBRE && batiment[cl] >= 0 && !missionAccueillie.equals(j.cibleTexte)) {
                // Le destinataire sort de chez lui et vient à ta rencontre.
                missionAccueillie = j.cibleTexte;
                Villageois qui = habitants.servir(w, batiment[cl], joueur, "saluer_main");
                if (qui != null) habitants.annoncer(habitants.nom(qui) + " t'attendait : Q pour lui remettre " + (j.cibleTexte.startsWith("Lettre") ? "la lettre." : "ta livraison."));
            }
            // Lot G3 : une mission sur la place (une lettre) : le bailli, ou un habitant, vient t'y attendre.
            boolean place = cl == Joueur.LIEU_MARCHE || cl >= Metiers.NOMBRE || batiment[cl] < 0;
            if (place && dist - 10.0 < MISSION_ACCUEIL && !missionAccueillie.equals(j.cibleTexte)) {
                missionAccueillie = j.cibleTexte;
                Villageois qui = habitants.accueillir(w, batiment[Metiers.MAIRIE], joueur, "saluer_main");
                if (qui != null) habitants.annoncer(habitants.nom(qui) + " t'attendait sur la place : Q pour lui remettre " + (j.cibleTexte.startsWith("Lettre") ? "la lettre." : "ce que tu apportes."));
            }
            // Distance jusqu'au mur du bâtiment (ou au bord de la place) : c'est elle qui compte pour pouvoir livrer.
            boolean atelier = cl >= 0 && cl < Metiers.NOMBRE && batiment[cl] >= 0 && !Float.isNaN(d[cl]);
            double bord = atelier ? d[cl] : Math.max(0.0, dist - 10.0);
            if (bord < Joueur.PROCHE) repere = "Mission : " + j.cibleTexte + ", tu y es (Q pour la rendre)";
            else {
                int k = (int) Math.floorMod(Math.round(Math.atan2(dy, dx) / (Math.PI / 4.0)), 8);
                repere = "Mission : " + j.cibleTexte + ", vers " + ou + " à " + Math.round(bord) + " m " + DIRECTIONS[k];
            }
        }
        // Lot E : la mission demande d'aller parler à quelqu'un (-3), ou de sortir des murs la nuit (-4).
        if (j.cibleVille == w.ville && !j.cibleTexte.isEmpty() && j.cibleLieu == -4)
            repere = "Mission : " + j.cibleTexte + ", hors des murs à la nuit tombée (Q quand ils sont là)";
        if (j.cibleVille == w.ville && !j.cibleTexte.isEmpty() && j.cibleLieu == -3) {
            long g = j.cibleGraine;
            // Lot H2 : celui qu'on cherche prend corps en priorité.
            if (g != 0L && !habitants.prioritaires.contains(g)) habitants.prioritaires = java.util.Set.of(g);
            Villageois qui = null;
            if (g != 0L) for (Villageois o : habitants.tous) if (o.village == w && o.graine == g && o.activite != Villageois.MORT) qui = o;
            if (g == 0L) repere = "Mission : " + j.cibleTexte + " : interroge les habitants (X)";
            else if (qui == null) repere = "Mission : " + j.cibleTexte + " : introuvable pour l'instant, sans doute chez lui";
            else {
                ecart(joueur.position, qui.corps.position, ecartCible);
                double dc = Math.hypot(ecartCible[0], ecartCible[1]);
                if (dc < PARLER_PORTEE * 1.5) repere = "Mission : " + j.cibleTexte + " : " + habitants.nom(qui) + " est là (X pour lui parler)";
                else {
                    int k = (int) Math.floorMod(Math.round(Math.atan2(ecartCible[1], ecartCible[0]) / (Math.PI / 4.0)), 8);
                    repere = "Mission : " + j.cibleTexte + ", " + habitants.nom(qui) + " à " + Math.round(dc) + " m " + DIRECTIONS[k];
                }
            }
        }
        // La maison la plus proche (pour la louer ou l'acheter), et la sienne.
        int maisonQ = -1;
        float maisonD = Float.MAX_VALUE, chezMoi = Float.MAX_VALUE;
        int[] mv = j.maisonVue;
        for (int q = 0; q < Math.min(n, w.habitables); q++) {
            Batiments.Plan plan = w.plans[q];
            if (plan == null || plan.ruine || plan.metier != Metiers.MAISON) continue;
            w.local(w.positions[q * 4], w.positions[q * 4 + 1], w.positions[q * 4 + 2], localBatiment);
            float dist = (float) Math.max(0.0, Math.hypot(pa - localBatiment[0], pb - localBatiment[1]) - plan.rayon());
            if (mv != null && mv[0] == w.ville && mv[1] == q) chezMoi = dist;
            if (dist < maisonD) {
                maisonD = dist;
                maisonQ = q;
            }
        }
        j.maisonProche = maisonQ;
        j.maisonDistance = maisonD;
        j.devantMaison = chezMoi < MAISON_PROCHE;
        // Lot D : la ruine la plus proche (pour y bâtir), et les ateliers déjà bâtis sur des ruines, dessinés à leur place.
        int ruineQ = -1;
        float ruineD = Float.MAX_VALUE;
        for (int q = 0; q < n; q++) {
            Batiments.Plan plan = w.plans[q];
            if (plan == null || !plan.ruine || plan.chantier) continue;
            w.local(w.positions[q * 4], w.positions[q * 4 + 1], w.positions[q * 4 + 2], localBatiment);
            float dist = (float) Math.max(0.0, Math.hypot(pa - localBatiment[0], pb - localBatiment[1]) - plan.rayon());
            if (dist < ruineD) {
                ruineD = dist;
                ruineQ = q;
            }
        }
        j.ruineProche = ruineQ;
        j.ruineDistance = ruineD;
        devantRuine = ruineQ >= 0 && ruineD <= com.deldaran.politics.histoire.Affaires.RUINE_PROCHE;
        // Lot G5 : un terrain libre devant le joueur, quand il s'est arrêté (rien d'autre devant lui).
        immobile = joueur.enMarche ? 0f : immobile + 0.3f;
        double[] terrainVu = null;
        if (incarne && j.fiche() != null && immobile >= TERRAIN_ARRET && !devantRuine && w.grille != null) {
            double angle = regard(joueur.direction);
            long libres = terrainsLibres(w, pa, pb, angle);
            if (libres != 0L) terrainVu = new double[] { w.ville, pa, pb, angle, libres };
        }
        j.terrainLibre = terrainVu;
        terrainDevant = terrainVu != null;
        int[] batis = j.batisVue;
        boolean refaire = false;
        for (int k = 0; batis != null && k + 2 < batis.length; k += 3) {
            int q = batis[k + 1];
            if (batis[k] != w.ville || q < 0 || q >= n) continue;
            Batiments.Plan vieux = w.plans[q];
            if (vieux == null || !vieux.ruine) continue;
            Batiments.Plan neuf = batis[k + 2] == Metiers.MAISON ? Batiments.plan(w.graines[q]) : Batiments.planMetier(w.graines[q], batis[k + 2], false);
            neuf.largeur = vieux.largeur;
            neuf.profondeur = vieux.profondeur;
            neuf.angle = vieux.angle;
            neuf.joueur = batis[k + 2] != Metiers.MAISON;
            w.plans[q] = neuf;
            w.metiers[q] = batis[k + 2];
            refaire = true;
        }
        if (refaire) villages.rafraichir();
        // Lot E : les adultes du village et les ruines, pour les missions (toutes les deux secondes environ).
        gensTic += 0.3f;
        if (gensTic >= 2f || j.gensVille != w.ville) {
            gensTic = 0f;
            List<Villageois> gens = new ArrayList<>();
            for (Villageois o : habitants.tous) if (o.village == w && !o.enfant && !o.garde && o.activite != Villageois.MORT) gens.add(o);
            gens.sort(java.util.Comparator.comparingLong(o -> o.graine));
            long[] gg = new long[gens.size()];
            String[] gn = new String[gens.size()];
            boolean[] gf = new boolean[gens.size()];
            for (int k = 0; k < gens.size(); k++) {
                gg[k] = gens.get(k).graine;
                gn[k] = habitants.nom(gens.get(k));
                gf[k] = gens.get(k).corps.apparence.femme;
            }
            List<Integer> rs = new ArrayList<>();
            for (int q = 0; q < n; q++) if (w.plans[q] != null && w.plans[q].ruine && !w.plans[q].chantier) rs.add(q);
            int[] ri = new int[rs.size()];
            for (int k = 0; k < ri.length; k++) ri[k] = rs.get(k);
            j.gensIci = gg;
            j.gensNoms = gn;
            j.gensFemmes = gf;
            j.gensVille = w.ville;
            j.ruinesIci = ri;
            j.ruinesVille = w.ville;
            // Lot F : la famille du joueur, incarnée par des habitants (choisis par Social) : leur nom, et l'estime qu'ils lui portent.
            habitants.nomsForces = j.social().nomsVue;
            for (java.util.Map.Entry<Long, Float> en : j.social().estimesVue.entrySet()) {
                for (Villageois o : gens) {
                    if (o.graine != en.getKey()) continue;
                    Relations.Lien l = habitants.relations.lien(o);
                    if (l.estime < en.getValue()) l.estime = en.getValue();
                }
            }
        }
        if (mv != null && mv[0] == w.ville && !habitants.maisonsJoueur.contains(mv[0] + ":" + mv[1])) habitants.vider(w, mv[1], personnages);
        if (mv == null && !habitants.maisonsJoueur.isEmpty()) habitants.maisonsJoueur.clear();
        double centre = Math.hypot(pa, pb);
        float place = (float) Math.max(0.0, centre - 10.0);
        if (Float.isNaN(d[Joueur.LIEU_MARCHE]) || place < d[Joueur.LIEU_MARCHE]) d[Joueur.LIEU_MARCHE] = place;
        double rayon = w.rayon * Terrain.RAYON_KM * 1000.0 - 8.0;
        d[Joueur.LIEU_DEHORS] = (float) Math.max(0.0, rayon * 0.85 - centre);
        j.villeVue = w.ville;
        j.distances = d;
        j.batimentsIci = batiment;
        devant = null;
        devantMien = false;
        float mieux = Joueur.PROCHE;
        int lieu = -1;
        for (int i = 0; i < Joueur.LIEUX; i++) {
            if (Float.isNaN(d[i]) || d[i] > mieux) continue;
            mieux = d[i];
            lieu = i;
            devantMien = i < Metiers.NOMBRE && j.entreprise().possede(w.ville, batiment[i]);
            devant = i == Joueur.LIEU_MARCHE ? "le marché" : i == Joueur.LIEU_DEHORS ? "les fermes et les bois"
                    : devantMien ? com.deldaran.politics.histoire.Entreprise.possessif(Biens.nomBoutique(i)) : Biens.nomBoutique(i).toLowerCase();
        }
        if (lieu != lieuDevant) {
            lieuDevant = lieu;
            if (lieu >= 0 && lieu < Metiers.NOMBRE && incarne && j.fiche() != null) {
                int l = lieu;
                histoire.demander(() -> j.devant(l));
            }
        }
    }

    /** Remet sur le sprite du joueur les armes et armures qu'il porte vraiment (phase 2). */
    void habiller() {
        if (joueur == null || histoire == null) return;
        Joueur incarne = histoire.joueur;
        versionTenue = incarne.versionTenue;
        if (incarne.fiche() == null) return;
        // Lot K3 : la tenue de tous les jours et la coiffure sont celles choisies à la création.
        com.deldaran.politics.histoire.Aspect aspect = incarne.fiche().aspect;
        Tenue base = aspect.tenue(joueur.apparence.graine, joueur.apparence.femme);
        if (joueur.traits == null) joueur.traits = aspect.traits(joueur.apparence.graine, joueur.apparence.espece, joueur.apparence.femme);
        joueur.habits = Biens.habiller(base, incarne.portes);
    }

    void basculerMain(int[] main) {
        if (joueur == null) return;
        Tenue t = joueur.tenue();
        int tenue = t.tenue(main);
        int debut = 0;
        if (tenue >= 0) {
            t.ranger(tenue);
            for (int k = 0; k < main.length; k++) if (main[k] == tenue) debut = k + 1;
        }
        for (int k = debut; k < main.length; k++) {
            if (t.porte(main[k])) {
                t.tenir(main[k]);
                return;
            }
        }
    }

    private boolean touche(int k) {
        return (ui == null || !ui.saisieActive()) && (pause == null || !pause.ouvert()) && (ecranCreation == null || !ecranCreation.ouvert()) && Gdx.input.isKeyJustPressed(k);
    }

    private boolean tenue(int k) {
        return (ui == null || !ui.saisieActive()) && (pause == null || !pause.ouvert()) && (ecranCreation == null || !ecranCreation.ouvert()) && Gdx.input.isKeyPressed(k);
    }

    void placerJoueur() {
        double cosLat = Math.cos(camera.latitude);
        double x = cosLat * Math.cos(camera.longitude), y = cosLat * Math.sin(camera.longitude), z = Math.sin(camera.latitude);
        terrain.echantillonner(x, y, z, 11, point);
        if (point.altitude < 0f || point.riviere || point.lac) return;
        Joueur incarne = histoire != null ? histoire.joueur : null;
        int espece = incarne != null && incarne.peuple() >= 0 ? incarne.peuple() : Peuple.HUMAIN;
        long graine = incarne != null && incarne.identifiant() >= 0L ? incarne.apparence() : 1L;
        int sexe = incarne != null && incarne.identifiant() >= 0L ? incarne.sexe() : -1;
        personnages.remove(joueur);
        joueur = new Personnage(assembleur.tirer(graine, espece, sexe), x, y, z);
        joueur.altitude = point.altitude;
        joueur.dansHerbe = Personnage.herbe(terrain, x, y, z, point);
        personnages.add(joueur);
        habiller();
    }

    void suivre() {
        double cosLon = Math.cos(camera.longitude), sinLon = Math.sin(camera.longitude);
        double cosLat = Math.cos(camera.latitude), sinLat = Math.sin(camera.latitude);
        double[] p = joueur.position;
        double est = -sinLon * p[0] + cosLon * p[1];
        double nord = -sinLat * cosLon * p[0] - sinLat * sinLon * p[1] + cosLat * p[2];
        camera.deplacerPixels(est * camera.rayonPixels, nord * camera.rayonPixels);
    }

    /**
     * Lot K3 : la carte du monde à plat (un pixel par case, 512 x 256), avec les tons du jeu et un léger relief ombré
     * (une pente qui monte vers le nord-ouest s'éclaire, l'autre s'assombrit).
     */
    Pixmap carteAPlat() {
        int l = monde.largeur, h = monde.hauteur;
        Pixmap image = new Pixmap(l, h, Pixmap.Format.RGBA8888);
        // Les grands fonds sont presque noirs dans la palette : sur une petite carte, on les rapproche du bleu des eaux claires.
        Color hautsFonds = new Color();
        Palette.tonEau(hautsFonds);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < l; x++) {
                int c = monde.index(x, y);
                tonCellule(c);
                if (!monde.estSousLEau(c)) {
                    int nw = monde.index(Math.floorMod(x - 1, l), Math.min(h - 1, y + 1));
                    float pente = monde.altitude[c] - monde.altitude[nw];
                    float f = 1.2f * Math.max(0.72f, Math.min(1.3f, 1f + pente * 0.9f));
                    couleurTemp.r = Math.min(1f, couleurTemp.r * f);
                    couleurTemp.g = Math.min(1f, couleurTemp.g * f);
                    couleurTemp.b = Math.min(1f, couleurTemp.b * f);
                } else couleurTemp.lerp(hautsFonds, 0.6f);
                image.drawPixel(x, h - 1 - y, Color.rgba8888(couleurTemp));
            }
        }
        return image;
    }

    /**
     * Lot K3 : une zone du monde peinte en détail depuis le terrain (relief fin, biomes, fleuves, lacs), pour choisir sa ville.
     * Hors du royaume, la terre s'assombrit ; sa frontière est un filet d'or. Appelée dans un fil à part.
     */
    /** La force du relief ombré des cartes détaillées : une pente de 1 (autant de montée que d'avancée) éclaire de 2,4. */
    private static final float OMBRAGE_DETAIL = 2.4f;

    int[] carteDetail(double lon0, double lon1, double lat0, double lat1, int l, int h, int etatCadre, int niveau) {
        com.deldaran.politics.histoire.Population p = histoire.population;
        Terrain.Point pt = new Terrain.Point();
        Color c = new Color();
        float[] alt = new float[l * h];
        double pasGrain = (lon1 - lon0) / l;
        int[] etat = new int[l * h], sortie = new int[l * h];
        for (int y = 0; y < h; y++) {
            double lat = lat1 - (y + 0.5) / h * (lat1 - lat0);
            for (int x = 0; x < l; x++) {
                double lon = lon0 + (x + 0.5) / l * (lon1 - lon0);
                double dx = Math.cos(lat) * Math.cos(lon), dy = Math.cos(lat) * Math.sin(lon), dz = Math.sin(lat);
                terrain.echantillonner(dx, dy, dz, niveau, pt);
                int k = y * l + x;
                alt[k] = pt.altitude;
                int cellule = monde.sphere.plusProche((float) dx, (float) dy, (float) dz);
                int r = p.regions.regionDe[cellule];
                int e = r < 0 ? -3 : p.etats.regionEtat[r];
                etat[k] = r < 0 ? -3 : e >= 0 && p.etats.estVivant(e) ? e : -1;
                // Le grain de couleur suit la grille des points (et pas une grille fixe) : pas de gros carreaux de près.
                int g = (int) Math.floor(lon / pasGrain) * 73856093 ^ (int) Math.floor(lat / pasGrain) * 19349663;
                float grain = ((g ^ (g >>> 13)) & 1023) / 1023f;
                if (pt.altitude < 0f) Palette.ton(com.deldaran.politics.planet.Biome.OCEAN, pt.altitude, grain, c);
                else if (pt.lac || pt.riviere) Palette.tonEau(c);
                else Palette.ton(terrain.biome(dx, dy, dz, pt), pt.altitude, grain, c);
                sortie[k] = Color.rgba8888(c);
            }
        }
        // Le relief ombré suit la vraie pente : la taille d'un point (en km) dépend du cadre.
        double kmPoint = (lon1 - lon0) * Terrain.RAYON_KM * Math.cos((lat0 + lat1) / 2.0) / l;
        float ombrage = (float) (OMBRAGE_DETAIL / Math.max(0.005, kmPoint));
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < l; x++) {
                int k = y * l + x;
                c.set(sortie[k]);
                if (alt[k] >= 0f && x > 0 && y > 0) {
                    float f = Math.max(0.7f, Math.min(1.35f, 1.12f + (alt[k] - alt[k - l - 1]) * ombrage));
                    c.r = Math.min(1f, c.r * f);
                    c.g = Math.min(1f, c.g * f);
                    c.b = Math.min(1f, c.b * f);
                }
                if (etat[k] > -3) {
                    boolean bord = (x + 1 < l && etat[k + 1] != etat[k]) || (y + 1 < h && etat[k + l] != etat[k]);
                    if (etat[k] != etatCadre) c.mul(0.55f, 0.55f, 0.58f, 1f);
                    if (bord && (etat[k] == etatCadre || (x + 1 < l && etat[k + 1] == etatCadre) || (y + 1 < h && etat[k + l] == etatCadre))) c.set(0.86f, 0.68f, 0.32f, 1f);
                }
                c.a = 1f;
                sortie[k] = Color.rgba8888(c);
            }
        }
        return sortie;
    }

    private void preparerFond() {
        int[] teintes = new int[couleurs.length];
        for (int i = 0; i < teintes.length; i++) {
            tonCellule(i);
            teintes[i] = Color.rgba8888(couleurTemp);
        }
        fond.mettreAJour(teintes);
        fondPret = true;
    }


    private void rafraichirCarte() {
        for (int i = 0; i < couleurs.length; i++) {
            choisirCouleur(i);
            couleurs[i] = Color.rgba8888(couleurTemp);
        }
        carte.mettreAJour(couleurs);
        tempsCarte = 0f;
        modeCarte = modeAffichage;
    }

    private void ajouterEau(int i) {
        if (monde.estSousLEau(i) || monde.estGele(i)) return;
        if (monde.lac[i] || monde.debit[i] > SEUIL_RIVIERE) Palette.tonEau(couleurTemp);
    }

    /** Lot K2 : le ton d'une cellule pris dans palettes/sol.png, comme le rendu des tuiles ; un grain fixe par cellule. */
    private void tonCellule(int i) {
        int h = i * 0x9E3779B1;
        float grain = ((h ^ (h >>> 15)) & 1023) / 1023f;
        // monde.biome n'est recalculé qu'avec le climat (tous les 100 Ma) : entre deux, une terre qui vient d'émerger
        // serait encore « océan ». On choisit donc le biome ici, d'après l'altitude du moment.
        byte biome = monde.biome[i];
        if (simulation.phase == Simulation.Phase.TECTONIQUE) {
            float relief = Math.max(0f, monde.altitude[i] - monde.niveauMer - simulation.climat.hauteurPlaines);
            biome = Biome.choisir(monde.temperature[i], monde.pluie[i], monde.estSousLEau(i), relief);
        }
        Palette.ton(biome, monde.altitude[i] - monde.niveauMer, grain, couleurTemp);
        ajouterEau(i);
    }

    private void choisirCouleur(int i) {
        switch (modeAffichage) {
            case 0: couleurPlaque(i); break;
            case 1: Palette.relief(monde.altitude[i] - monde.niveauMer, monde.temperature[i], couleurTemp); ajouterEau(i); break;
            case 2: couleurTemperature(i); break;
            case 3: couleurPluie(i); break;
            case 5: couleurRegion(i); break;
            case 6: couleurNourriture(i); break;
            case 7: couleurPopulation(i); break;
            case 8: couleurVilles(i); break;
            case 9: couleurCommerce(i); break;
            case 10: couleurEtat(i); break;
            case 11: couleurRoutes(i); break;

            default: tonCellule(i); break;

        }
    }

    private void couleurRoutes(int i) {
        couleurVilles(i);
        if (histoire == null) return;
        Population p = histoire.population;
        if (p.villeDe[i] >= 0) return;
        com.deldaran.politics.histoire.Routes routes = p.routes;
        int n = routes.niveauCase[i];
        if (n > 0) {
            float t = (n - 1) / 3f;
            couleurTemp.set(0.55f + 0.4f * t, 0.38f + 0.5f * t, 0.2f + 0.6f * t, 1f);
        } else if (routes.ruineCase[i] > 0) {
            couleurTemp.set(0.4f, 0.22f, 0.2f, 1f);
        }
    }

    private void couleurEtat(int i) {
        if (histoire == null) {
            couleurTemp.set(Color.DARK_GRAY);
            return;
        }
        Population p = histoire.population;
        int r = p.regions.regionDe[i];
        if (!p.regions.estTerre[r]) {
            couleurTemp.set(0.06f, 0.12f, 0.24f, 0f);
            return;
        }
        int e = p.etats.regionEtat[r];
        if (e < 0) {
            couleurTemp.set(0.29f, 0.29f, 0.26f, 0f);
            return;
        }
        couleurTemp.fromHsv((e * 47) % 360, 0.65f, 0.9f);
        couleurTemp.a = 1f;
    }


    private void couleurCommerce(int i) {
        if (histoire == null) {
            couleurTemp.set(Color.DARK_GRAY);
            return;
        }
        Population p = histoire.population;
        int r = p.regions.regionDe[i];
        float t = Math.min(1f, (float) Math.log10(1f + p.economie.trafic[r]) / 6f);
        if (p.regions.estTerre[r]) {
            couleurTemp.set(0.16f + 0.84f * t, 0.14f + 0.66f * t, 0.1f, 1f);
        } else {
            couleurTemp.set(0.04f, 0.08f + 0.5f * t, 0.18f + 0.8f * t, 1f);
        }
    }

    private void couleurVilles(int i) {
        couleurPopulation(i);
        couleurTemp.mul(0.4f, 0.4f, 0.4f, 1f);
        if (histoire == null) return;
        Population p = histoire.population;
        Villes villes = p.villes;
        int v = p.villeDe[i];
        if (v < 0 || v >= villes.nombre) return;
        if (!villes.estVivante(v)) {
            couleurTemp.set(0.55f, 0.5f, 0.45f, 1f);
            return;
        }
        float force = 0.3f + 0.7f * Math.min(1f, villes.habitants[v] / 3000f);
        couleurTemp.set(COULEURS_PEUPLES[villes.peuple[v]]).lerp(Color.WHITE, force);
    }

    private void couleurPopulation(int i) {
        if (histoire == null) {
            couleurTemp.set(Color.DARK_GRAY);
            return;
        }
        Population p = histoire.population;
        int r = p.regions.regionDe[i];
        if (!p.regions.estTerre[r]) {
            couleurTemp.set(0.06f, 0.12f, 0.24f, 0f);
            return;
        }
        int dominant = -1, nombre = 0;
        for (int q = 0; q < Peuple.NOMBRE; q++) {
            int vivants = p.foules[q][r].vivants;
            if (vivants > nombre) {
                nombre = vivants;
                dominant = q;
            }
        }
        if (dominant < 0) {
            couleurTemp.set(0.29f, 0.29f, 0.26f, 0f);
            return;
        }
        float d = 0.45f + 0.55f * Math.min(1f, p.habitants[r] / Math.max(1f, p.capacite[dominant][r]));
        couleurTemp.set(COULEURS_PEUPLES[dominant]).mul(d, d, d, 1f);
    }


    private void couleurTemperature(int i) {
        float t = MathUtils.clamp((monde.temperature[i] + 30f) / 60f, 0f, 1f);
        couleurTemp.set(t, 0.3f + 0.3f * (1f - Math.abs(t - 0.5f) * 2f), 1f - t, 1f);
    }

    private void couleurPluie(int i) {
        float t = MathUtils.clamp(monde.pluie[i] / 2000f, 0f, 1f);
        float base = monde.estSousLEau(i) ? 0.25f : 1f;
        couleurTemp.set(base * (1f - t), base * (1f - t * 0.6f), base * (1f - t * 0.1f), 1f);
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) return;
        fenetreDebug.redimensionner(width, height);
        if (ui != null) ui.redimensionner(width, height);
        if (pause != null) pause.redimensionner(width, height);
        if (barre != null) barre.redimensionner(width, height);
        if (etiquetteCarte != null) etiquetteCarte.redimensionner(width, height);
        if (ecranCreation != null) ecranCreation.redimensionner(width, height);
    }
    private void couleurRegion(int i) {
        if (regions == null) {
            couleurTemp.set(Color.DARK_GRAY);
            return;
        }
        int r = regions.regionDe[i];
        int h = r * 0x9E3779B1;
        couleurTemp.set(0.35f + ((h >>> 8) & 127) / 254f, 0.35f + ((h >>> 16) & 127) / 254f, 0.35f + ((h >>> 24) & 127) / 254f, 1f);
        if (!regions.estTerre[r]) {
            couleurTemp.mul(0.35f, 0.4f, 0.7f, 1f);
        }
    }

    private void couleurNourriture(int i) {
        if (regions == null) {
            couleurTemp.set(Color.DARK_GRAY);
            return;
        }
        int r = regions.regionDe[i];
        if (!regions.estTerre[r]) {
            couleurTemp.set(0.06f, 0.12f, 0.24f, 0f);
            return;
        }
        float f = regions.nourriture[r] / regions.cases[r];
        couleurTemp.set(0.1f + 0.6f * (1f - f), 0.25f + 0.7f * f, 0.2f, 1f);
    }

    private void remplirDebug() {
        FenetreDebug.Section affichage = fenetreDebug.ajouterSection("Affichage").ouvrir();
        affichage.ajouterInfo("FPS", () -> String.valueOf(Gdx.graphics.getFramesPerSecond()));
        affichage.ajouterInfo("Mode (TAB)", () -> NOMS_MODES[modeAffichage]);

        FenetreDebug.Section simulationDebug = fenetreDebug.ajouterSection("Simulation").ouvrir();
        simulationDebug.ajouterInfo("Etat (ESPACE)", () -> enPause ? "en pause" : "en cours");
        simulationDebug.ajouterInfo("Phase", () -> simulation.phase.name());
        simulationDebug.ajouterInfo("Temps", () -> simulation.tempsTexte());
        simulationDebug.ajouterSlider("Pas par seconde", 10f, 500f, 10f, 10f, valeur -> pasParSeconde = valeur);

        FenetreDebug.Section planete = fenetreDebug.ajouterSection("Planete");
        planete.ajouterInfo("Rayon", () -> String.format("%.0f km", Terrain.RAYON_KM));
        planete.ajouterInfo("Taille d'une case", () -> String.format("%.0f km2", surfaceCase()));
        planete.ajouterInfo("Niveau de la mer", () -> String.format("%.2f km", monde.niveauMer));

        FenetreDebug.Section plaques = planete.ajouterSection("Tectonique");
        plaques.ajouterInfo("Nombre de plaques", () -> String.valueOf(monde.plaques.size));
        plaques.ajouterInfo("Epaisseur maximale", () -> String.format("%.2f km", monde.epaisseurMax()));
        plaques.ajouterInfo("Fractures", () -> String.valueOf(simulation.tectonique.nombreFractures));
        plaques.ajouterInfo("Ruptures", () -> String.valueOf(simulation.tectonique.nombreRuptures));
        plaques.ajouterInfo("Soudures", () -> String.valueOf(simulation.tectonique.nombreSoudures));

        FenetreDebug.Section sectionRegions = fenetreDebug.ajouterSection("Regions");
        infoRegion(sectionRegions, "Regions de terre", () -> String.valueOf(regions.regionsTerre));
        infoRegion(sectionRegions, "Regions de mer", () -> String.valueOf(regions.regionsMer));
        infoRegion(sectionRegions, "Ports", () -> String.valueOf(regions.ports));
        infoRegion(sectionRegions, "Terres", () -> String.format("%.1f M km2", regions.casesTerre * surfaceCase() / 1e6));
        infoRegion(sectionRegions, "Terres fertiles", () -> String.format("%.1f M km2", regions.casesFertiles * surfaceCase() / 1e6));

        Regions.Reglages r = reglagesRegions;
        FenetreDebug.Section decoupe = sectionRegions.ajouterSection("Decoupage");
        reglerRegion(decoupe, "Ecart terre", 1f, 8f, 1f, r.ecartTerre, v -> r.ecartTerre = v.intValue());
        reglerRegion(decoupe, "Ecart mer", 2f, 16f, 1f, r.ecartMer, v -> r.ecartMer = v.intValue());

        FenetreDebug.Section ressources = sectionRegions.ajouterSection("Ressources");
        reglerRegion(ressources, "Debit d'un fleuve", 5f, 100f, 5f, r.debitFleuve, v -> r.debitFleuve = v);
        reglerRegion(ressources, "Bonus fleuve", 0f, 1f, 0.05f, r.bonusFleuve, v -> r.bonusFleuve = v);
        reglerRegion(ressources, "Peche cotiere", 0f, 1f, 0.05f, r.pecheCotiere, v -> r.pecheCotiere = v);
        reglerRegion(ressources, "Debut minerai", 0f, 2f, 0.1f, r.debutMinerai, v -> r.debutMinerai = v);
        reglerRegion(ressources, "Etendue minerai", 0.1f, 2f, 0.1f, r.etendueMinerai, v -> r.etendueMinerai = v);

        FenetreDebug.Section fertilite = sectionRegions.ajouterSection("Fertilite des biomes");
        for (int i = 0; i < BIOMES_FERTILES.length; i++) {
            byte biome = BIOMES_FERTILES[i];
            reglerRegion(fertilite, NOMS_BIOMES[i], 0f, 1f, 0.05f, r.fertilite[biome], v -> r.fertilite[biome] = v);
        }
        FenetreDebug.Section sectionMeteo = fenetreDebug.ajouterSection("Meteo");
        sectionMeteo.ajouterInfo("Mode (O)", () -> meteo == null ? "-" : Meteo.FORCAGES[meteo.forcage]);
        sectionMeteo.ajouterInfo("Temps", () -> meteo == null || !meteo.pret() ? "calcul..." : String.format("jour %d, %02dh%02d ici (H : +3h)", (int) (meteo.heures() / 24), (int) meteo.heureLocale(camera.longitude), (int) (meteo.heureLocale(camera.longitude) % 1.0 * 60.0)));
        sectionMeteo.ajouterInfo("Ici", () -> ciel == null ? "-" : String.format("%.0f C, nuages %.0f%%, pluie %.0f%%, neige %.0f%%, brouillard %.0f%%",
            ciel.locale.temperature, ciel.locale.nuage * 100f, ciel.locale.pluie * 100f, ciel.locale.neige * 100f, ciel.locale.brouillard * 100f));
        sectionMeteo.ajouterInfo("Sol", () -> ciel == null ? "-" : String.format("mouille %.0f%%, neige %.0f%%", ciel.locale.mouille * 100f, ciel.locale.neigeSol * 100f));
        sectionMeteo.ajouterInfo("Vent", () -> ciel == null ? "-" : String.format("%.1f m/s", Math.hypot(ciel.locale.ventEst, ciel.locale.ventNord)));

        FenetreDebug.Section sectionVoyage = fenetreDebug.ajouterSection("Teleportation");
        sectionVoyage.ajouterInfo("T", () -> "joueur sous la souris");
        sectionVoyage.ajouterBouton("Plus grande ville", () -> teleporterVille(false));
        sectionVoyage.ajouterBouton("Ville au hasard", () -> teleporterVille(true));
        sectionVoyage.ajouterBouton("Lieu au hasard", () -> teleporterBiome(-1));
        sectionVoyage.ajouterBouton("Foret", () -> teleporterBiome(Biome.FORET));
        sectionVoyage.ajouterBouton("Jungle", () -> teleporterBiome(Biome.JUNGLE));
        sectionVoyage.ajouterBouton("Taiga", () -> teleporterBiome(Biome.TAIGA));
        sectionVoyage.ajouterBouton("Prairie", () -> teleporterBiome(Biome.PRAIRIE));
        sectionVoyage.ajouterBouton("Savane", () -> teleporterBiome(Biome.SAVANE));
        sectionVoyage.ajouterBouton("Desert", () -> teleporterBiome(Biome.DESERT));
        sectionVoyage.ajouterBouton("Toundra", () -> teleporterBiome(Biome.TOUNDRA));
        sectionVoyage.ajouterBouton("Montagne", () -> teleporterBiome(Biome.MONTAGNE));
        sectionVoyage.ajouterBouton("Relief escarpe", () -> teleporterRelief());

        FenetreDebug.Section sectionGens = fenetreDebug.ajouterSection("Personnages");
        sectionGens.ajouterInfo("Nombre", () -> String.valueOf(personnages.size()));
        sectionGens.ajouterInfo("Joueur", () -> joueur == null ? "-" : joueur.profondeur > 0f ? String.format("dans l'eau (%.1f m)", joueur.profondeur) : joueur.dansHerbe ? "dans l'herbe" : "au sol");
        sectionGens.ajouterBouton("Ajouter 30 habitants", () -> { if (terrain != null) peupler(); });
        sectionGens.ajouterBouton("Effacer les habitants", () -> {
            habitants.oublier(personnages);
            personnages.clear();
            if (joueur != null) personnages.add(joueur);
            errance = new float[personnages.size() * 3];
        });
        sectionGens.ajouterBouton("Replacer le joueur (J)", () -> { if (terrain != null) placerJoueur(); });
        sectionGens.ajouterBouton("Habiller / deshabiller le joueur", () -> { if (joueur != null) joueur.nu = !joueur.nu; });
        sectionGens.ajouterInfo("Coiffure", () -> joueur == null ? "-" : Traits.NOMS_COUPES[joueur.traits().coupe] + String.format(" %.0f cm", joueur.traits().cheveux));
        sectionGens.ajouterInfo("Barbe", () -> joueur == null ? "-" : Traits.NOMS_BARBES[joueur.traits().barbe] + String.format(" %.1f cm", joueur.traits().longueurBarbe));
        sectionGens.ajouterInfo("Expression", () -> joueur == null ? "-" : Traits.NOMS_EXPRESSIONS[joueur.traits().expression]);
        sectionGens.ajouterBouton("Changer de coiffure", () -> { if (joueur != null) joueur.traits().coupe = (joueur.traits().coupe + 1) % Traits.COUPES; });
        sectionGens.ajouterBouton("Changer de barbe", () -> { if (joueur != null) joueur.traits().barbe = (joueur.traits().barbe + 1) % Traits.BARBES; });
        sectionGens.ajouterBouton("Laisser pousser 3 mois", () -> { if (joueur != null) joueur.traits().pousser(90.0, false); });
        sectionGens.ajouterBouton("Couper les cheveux / raser", () -> { if (joueur != null) { joueur.traits().couper(4.0); joueur.traits().raser(); } });
        sectionGens.ajouterBouton("Changer d'expression", () -> { if (joueur != null) joueur.traits().expression = (joueur.traits().expression + 1) % Traits.EXPRESSIONS; });
        sectionGens.ajouterBouton("Changer d'habits", () -> { if (joueur != null) joueur.habits = Tenue.tirer(hasard.nextLong(), joueur.apparence.femme, joueur.apparence.enfant); });
        sectionGens.ajouterBouton("Sortir / ranger arme droite", () -> basculerMain(Tenue.MAIN_DROITE));
        sectionGens.ajouterBouton("Sortir / ranger objet gauche", () -> basculerMain(Tenue.MAIN_GAUCHE));
        sectionGens.ajouterInfo("Villageois", () -> habitants.dehors() + " dehors / " + habitants.tous.size());
        sectionGens.ajouterInfo("Villageois proche", () -> {
            if (joueur == null) return "-";
            Villageois w = habitants.proche(joueur.position);
            return w == null ? "-" : w.description();
        });
        sectionGens.ajouterInfo("Animation (N / B / V / C)", () -> nomAnimation(joueur));
        sectionGens.ajouterBouton("Animation suivante (N)", () -> essayerAnimation(1));
        sectionGens.ajouterBouton("Animation precedente (B)", () -> essayerAnimation(-1));
        sectionGens.ajouterBouton("Rejouer l'animation (V)", () -> essayerAnimation(0));
        sectionGens.ajouterBouton("Arreter l'animation (C)", () -> { if (joueur != null) joueur.arreter(); });
        sectionGens.ajouterBouton("Effacer les traces", () -> { if (traces != null) traces.effacer(); });

        FenetreDebug.Section sectionRelations = fenetreDebug.ajouterSection("Relations");
        sectionRelations.ajouterInfo("Touches", () -> "G saluer, F frapper, R offrir, E epargner, K achever");
        sectionRelations.ajouterInfo("Sante du joueur", () -> joueur == null ? "-" : String.format("%.0f%%", joueur.sante * 100f) + (habitants.joueurATerre > 0f ? " (a terre)" : ""));
        sectionRelations.ajouterInfo("Dernier evenement", () -> habitants.annonce == null ? "-" : habitants.annonce);
        sectionRelations.ajouterInfo("Avis du plus proche", () -> {
            if (joueur == null) return "-";
            Villageois w = habitants.proche(joueur.position);
            return w == null ? "-" : w.lien == null ? "ne te connait pas" : w.lien.resume();
        });
        sectionRelations.ajouterInfo("Reputation du village", () -> {
            if (joueur == null) return "-";
            Villageois w = habitants.proche(joueur.position);
            return w == null ? "-" : String.format("%+.0f%%", habitants.reputation(w.village) * 100f);
        });
        sectionRelations.ajouterInfo("Rivaux", () -> {
            if (habitants.relations.rivaux.isEmpty()) return "aucun";
            StringBuilder t = new StringBuilder();
            for (Relations.Lien l : habitants.relations.rivaux) t.append(t.length() > 0 ? " ; " : "").append(l.nomComplet()).append(" niv. ").append(l.niveau).append(l.soumis ? " (soumis)" : "");
            return t.toString();
        });
        sectionRelations.ajouterBouton("Faire du plus proche un rival", () -> {
            if (joueur == null) return;
            Villageois w = habitants.proche(joueur.position);
            if (w == null || w.enfant) return;
            Relations.Lien l = habitants.relations.lien(w);
            l.rancune = 1f;
            l.estime = -0.8f;
            habitants.relations.promouvoir(w, Relations.TITRE_VENGEANCE, habitants.heures / 24.0);
        });
        sectionRelations.ajouterInfo("IA locale", () -> ia == null ? "-" : ia.etat() + " (" + habitants.parolesIa + " repliques IA, " + habitants.parolesSecours + " de secours, " + ia.echecs + " echecs" + (ia.reponses > 0 ? String.format(", %.2f s", ia.derniereDuree) : "") + ")");
        sectionRelations.ajouterBouton("Demarrer / arreter l'IA locale", () -> {
            if (ia == null) return;
            if (ia.pret()) ia.arreter();
            else ia.demarrer();
        });
        sectionRelations.ajouterBouton("Enregistrer les relations", () -> habitants.relations.enregistrer(fichierRelations));
        sectionRelations.ajouterBouton("Oublier toutes les relations", () -> {
            habitants.relations.oublier();
            for (Villageois w : habitants.tous) w.lien = null;
        });

        FenetreDebug.Section sectionJoueur = fenetreDebug.ajouterSection("Joueur");
        sectionJoueur.ajouterBouton("Entrer dans le monde", () -> {
            if (histoire != null) jouer(histoire.joueur::ouvrir);
        });
        sectionJoueur.ajouterInfo("Zoom", () -> incarne ? "libre (incarne)" : zoomLibre ? "libre (debug)" : "bloque sur la carte");
        sectionJoueur.ajouterBouton("Debloquer / bloquer le zoom", () -> zoomLibre = !zoomLibre);
        sectionJoueur.ajouterBouton("Commercer de partout (debug)", () -> { if (histoire != null) histoire.joueur.partout = !histoire.joueur.partout; });
        sectionJoueur.ajouterInfo("Commerce", () -> histoire == null ? "-" : "ville " + histoire.joueur.villeVue + ", jour " + histoire.joueur.jour() + (histoire.joueur.partout ? ", partout" : "") + (devant != null ? ", devant " + devant : ""));

        FenetreDebug.Section sectionFeu = fenetreDebug.ajouterSection("Incendies");
        sectionFeu.ajouterInfo("Foyers", () -> incendies == null ? "-" : incendies.resume());
        sectionFeu.ajouterInfo("Causes", () -> incendies == null ? "-" : String.format("%d allumages : %d par la chaleur, %d par brandons, %d par la foudre", incendies.allumages, incendies.parChaleur, incendies.parBrandon, incendies.parFoudre));
        sectionFeu.ajouterInfo("Vent du feu", () -> incendies == null ? "-" : String.format("%.1f m/s, pluie %.0f%%, sol mouille %.0f%%", Math.hypot(incendies.ventEst, incendies.ventNord), incendies.pluie * 100f, incendies.mouille * 100f));
        sectionFeu.ajouterBouton("Mettre le feu au batiment le plus proche (I en jeu)", () -> {
            if (incendies == null) return;
            double cl = Math.cos(camera.latitude);
            if (joueur != null) incendies.allumerPres(villages, joueur.position[0], joueur.position[1], joueur.position[2], 60.0);
            else incendies.allumerPres(villages, cl * Math.cos(camera.longitude), cl * Math.sin(camera.longitude), Math.sin(camera.latitude), 60.0);
        });
        sectionFeu.ajouterBouton("Allumer l'herbe ici", () -> {
            if (incendies == null) return;
            double cl = Math.cos(camera.latitude);
            if (joueur != null) incendies.allumerSol(villages, joueur.position[0], joueur.position[1], joueur.position[2]);
            else incendies.allumerSol(villages, cl * Math.cos(camera.longitude), cl * Math.sin(camera.longitude), Math.sin(camera.latitude));
        });
        sectionFeu.ajouterBouton("Tout eteindre", () -> {
            if (incendies != null) incendies.eteindre();
        });
        sectionFeu.ajouterSlider("Vitesse du feu", 0.25f, 8f, 0.25f, 1f, v -> {
            if (incendies != null) incendies.vitesse = v;
        });
        sectionFeu.ajouterSlider("Vent force (m/s, -1 = meteo)", -1f, 25f, 1f, -1f, v -> {
            if (incendies != null) incendies.ventForce = v;
        });

        FenetreDebug.Section sectionHistoire = fenetreDebug.ajouterSection("Histoire");
        infoHistoire(sectionHistoire, "Annee", () -> String.valueOf(histoire.population.annee));
        infoHistoire(sectionHistoire, "Vivants", () -> String.valueOf(histoire.population.vivants));
        infoHistoire(sectionHistoire, "Regions habitees", () -> String.valueOf(histoire.population.regionsHabitees));
        infoHistoire(sectionHistoire, "Ayant vecu", () -> String.valueOf(histoire.population.naissancesTotales));
        infoHistoire(sectionHistoire, "Age moyen au deces", () -> String.format("%.1f ans", histoire.population.ageMoyenAuDeces));
        infoHistoire(sectionHistoire, "Duree d'une annee", () -> String.format("%.1f ms", histoire.msParAn));
        sectionHistoire.ajouterBouton("Recommencer", this::arreterHistoire);
        infoHistoire(sectionHistoire, "Sauvegarde", () -> histoire.sauvegarde);
        sectionHistoire.ajouterBouton("Sauvegarder maintenant", () -> {
            if (histoire != null) histoire.demander(histoire::enregistrer);
        });


        Population.Reglages p = reglagesPopulation;
        FenetreDebug.Section depart = sectionHistoire.ajouterSection("Depart");
        reglerDepart(depart, "Vivants maximum", 1000000f, 20000000f, 1000000f, p.populationMax, v -> p.populationMax = v.intValue());
        reglerDepart(depart, "Habitants par km2 fertile", 0.5f, 10f, 0.5f, p.habitantsParKm2, v -> p.habitantsParKm2 = v);
        FenetreDebug.Section demographie = sectionHistoire.ajouterSection("Demographie");
        demographie.ajouterSlider("Natalite", 0.05f, 0.6f, 0.01f, p.natalite, v -> p.natalite = v);
        demographie.ajouterSlider("Frein natalite", 0.1f, 1f, 0.01f, p.freinNatalite, v -> p.freinNatalite = v);
        demographie.ajouterSlider("Famine", 0f, 5f, 0.1f, p.famine, v -> p.famine = v);

        FenetreDebug.Section migrations = sectionHistoire.ajouterSection("Migrations");
        migrations.ajouterSlider("Seuil", 0.1f, 1.5f, 0.05f, p.seuilMigration, v -> p.seuilMigration = v);
        migrations.ajouterSlider("Force", 0f, 1f, 0.05f, p.migration, v -> p.migration = v);
        migrations.ajouterSlider("Part par la mer", 0f, 0.3f, 0.01f, p.partMer, v -> p.partMer = v);
        FenetreDebug.Section sectionVilles = sectionHistoire.ajouterSection("Villes");
        infoHistoire(sectionVilles, "Villes vivantes", () -> String.valueOf(histoire.population.villes.nombre - histoire.population.villes.ruines));
        infoHistoire(sectionVilles, "Ruines", () -> String.valueOf(histoire.population.villes.ruines));
        sectionVilles.ajouterSlider("Seuil de fondation", 500f, 6000f, 100f, p.seuilVille, v -> p.seuilVille = v.intValue());
        sectionVilles.ajouterSlider("Colons", 20f, 500f, 10f, p.colons, v -> p.colons = v.intValue());
        sectionVilles.ajouterSlider("Exode rural", 0f, 0.3f, 0.01f, p.exode, v -> p.exode = v);
        infoHistoire(sectionVilles, "Abandons apres guerre", () -> String.valueOf(histoire.population.villes.abandonsGuerre));
        infoHistoire(sectionVilles, "Refondations", () -> String.valueOf(histoire.population.villes.refondations));
        sectionVilles.ajouterSlider("Abandon apres un sac", 0f, 1f, 0.05f, p.etats.abandon, v -> p.etats.abandon = v);
        infoHistoire(sectionVilles, "Ville proche", () -> {
            int v = villeProche();
            if (v < 0) return "aucune";
            Villes vs = histoire.population.villes;
            return "n" + v + ", " + vs.habitants[v] + " hab., pic " + vs.pic[v] + " an " + vs.anneePic[v];
        });
        infoHistoire(sectionVilles, "Son histoire", () -> {
            int v = villeProche();
            return v < 0 ? "" : histoire.population.villes.raconter(v);
        });
        FenetreDebug.Section sectionRoutes = sectionHistoire.ajouterSection("Routes");
        infoHistoire(sectionRoutes, "Routes ouvertes", () -> histoire.population.routes.ouvertes + " / " + histoire.population.routes.nombre);
        infoHistoire(sectionRoutes, "Longueur ouverte", () -> String.format("%.0f km", histoire.population.routes.kmOuverts));
        infoHistoire(sectionRoutes, "Sentiers / chemins / routes / voies", () -> {
            com.deldaran.politics.histoire.Routes rt = histoire.population.routes;
            return rt.compter(com.deldaran.politics.histoire.Routes.SENTIER) + " / " + rt.compter(com.deldaran.politics.histoire.Routes.CHEMIN) + " / " + rt.compter(com.deldaran.politics.histoire.Routes.ROUTE) + " / " + rt.compter(com.deldaran.politics.histoire.Routes.VOIE);
        });
        infoHistoire(sectionRoutes, "Routes de la ville proche", () -> {
            int v = villeProche();
            return v < 0 ? "" : histoire.population.routes.decrire(histoire.population, v);
        });
        infoHistoire(sectionVilles, "Habitants par siecle", () -> {
            int v = villeProche();
            if (v < 0) return "";
            Villes vs = histoire.population.villes;
            StringBuilder b = new StringBuilder();
            int fin = vs.estVivante(v) ? histoire.population.annee : vs.fin[v];
            for (int an = vs.fondation[v]; an <= fin; an += 100) b.append(b.length() > 0 ? " " : "").append(vs.habitantsEn(v, an));
            return b.toString();
        });
        FenetreDebug.Section grandes = sectionVilles.ajouterSection("Plus grandes");

        Etats.Reglages reglagesEtats = p.etats;
        FenetreDebug.Section sectionEtats = sectionHistoire.ajouterSection("Etats");
        infoHistoire(sectionEtats, "Etats vivants", () -> String.valueOf(histoire.population.etats.vivants()));
        infoHistoire(sectionEtats, "Chutes", () -> String.valueOf(histoire.population.etats.chutes));
        infoHistoire(sectionEtats, "Guerres", () -> String.valueOf(histoire.population.etats.guerres));
        infoHistoire(sectionEtats, "Revolutions", () -> String.valueOf(histoire.population.etats.revolutions));
        infoHistoire(sectionEtats, "Morts au combat", () -> String.valueOf(histoire.population.etats.mortsAuCombat));
        infoHistoire(sectionEtats, "Morts des sacs", () -> String.valueOf(histoire.population.etats.mortsDesSacs));
        sectionEtats.ajouterSlider("Envie de guerre", 0f, 0.1f, 0.005f, reglagesEtats.envieGuerre, v -> reglagesEtats.envieGuerre = v);
        sectionEtats.ajouterSlider("Siege", 1f, 4f, 0.1f, reglagesEtats.siege, v -> reglagesEtats.siege = v);
        sectionEtats.ajouterSlider("Part de l'armee", 0.005f, 0.1f, 0.005f, reglagesEtats.partArmee, v -> reglagesEtats.partArmee = v);
        sectionEtats.ajouterSlider("Seuil de revolution", 0f, 50f, 1f, reglagesEtats.seuilRevolution, v -> reglagesEtats.seuilRevolution = v);
        FenetreDebug.Section sectionChronique = sectionEtats.ajouterSection("Chronique");
        for (int rang = 0; rang < 6; rang++) {
            ajouterFait(sectionChronique, rang);
        }

        FenetreDebug.Section sectionFamilles = sectionHistoire.ajouterSection("Familles");
        infoHistoire(sectionFamilles, "Maisons", () -> String.valueOf(histoire.population.maisons.nombre));
        infoHistoire(sectionFamilles, "Regnes", () -> String.valueOf(histoire.population.maisons.nombreRegnes));
        infoHistoire(sectionFamilles, "Heritages", () -> String.valueOf(histoire.population.etats.heritages));
        infoHistoire(sectionFamilles, "Crises", () -> String.valueOf(histoire.population.etats.crises));
        infoHistoire(sectionFamilles, "Regences", () -> String.valueOf(histoire.population.etats.regences));
        infoHistoire(sectionFamilles, "Usurpations", () -> String.valueOf(histoire.population.etats.usurpations));
        sectionFamilles.ajouterBouton("Voir les grandes maisons", () -> montrerTexte("Maisons", q -> Recit.grandesMaisons(q, 25)));
        sectionFamilles.ajouterSlider("Chance d'usurpation", 0f, 0.5f, 0.01f, reglagesEtats.usurpation, v -> reglagesEtats.usurpation = v);


        Livres.Reglages reglagesLivres = p.livres;
        FenetreDebug.Section sectionSavoir = sectionHistoire.ajouterSection("Savoir");
        infoHistoire(sectionSavoir, "Oeuvres", () -> String.valueOf(histoire.population.livres.nombre));
        infoHistoire(sectionSavoir, "Oeuvres survivantes", () -> String.valueOf(histoire.population.livres.vivantes()));
        infoHistoire(sectionSavoir, "Chansons", () -> String.valueOf(histoire.population.livres.parType[Livres.CHANSON]));
        sectionSavoir.ajouterBouton("Interroger un paysan", () -> montrerTexte("Temoignage", q -> Recit.interroger(q, 0, MathUtils.random.nextLong())));
        sectionSavoir.ajouterBouton("Interroger un citadin", () -> montrerTexte("Temoignage", q -> Recit.interroger(q, 1, MathUtils.random.nextLong())));
        sectionSavoir.ajouterBouton("Interroger un lettre", () -> montrerTexte("Temoignage", q -> Recit.interroger(q, 2, MathUtils.random.nextLong())));
        sectionSavoir.ajouterBouton("Lire un livre", () -> montrerTexte("Livre", q -> Recit.lireAuHasard(q, true, MathUtils.random.nextLong())));
        sectionSavoir.ajouterBouton("Ecouter une chanson", () -> montrerTexte("Chanson", q -> Recit.lireAuHasard(q, false, MathUtils.random.nextLong())));
        sectionSavoir.ajouterBouton("Lire le plus ancien livre", () -> montrerTexte("Livre", Recit::lirePlusAncien));
        sectionSavoir.ajouterBouton("Voir la vraie chronique", () -> montrerTexte("Chronique", q -> Recit.verite(q, 60)));
        sectionSavoir.ajouterSlider("Portee des nouvelles", 0.2f, 5f, 0.1f, reglagesLivres.portee, v -> reglagesLivres.portee = v);
        sectionSavoir.ajouterSlider("Memoire orale", 10f, 200f, 5f, reglagesLivres.demiVie, v -> reglagesLivres.demiVie = v);
        sectionSavoir.ajouterSlider("Ecriture", 0f, 0.02f, 0.001f, reglagesLivres.ecriture, v -> reglagesLivres.ecriture = v);
        sectionSavoir.ajouterSlider("Perte des copies", 0f, 0.02f, 0.001f, reglagesLivres.perteCopie, v -> reglagesLivres.perteCopie = v);


        Economie.Reglages e = p.economie;
        FenetreDebug.Section sectionEconomie = sectionHistoire.ajouterSection("Economie");
        sectionEconomie.ajouterSlider("Rendement paysan", 1f, 3f, 0.05f, e.rendement, v -> e.rendement = v);
        sectionEconomie.ajouterSlider("Cout terre", 0.01f, 0.3f, 0.01f, e.coutTerre, v -> e.coutTerre = v);
        sectionEconomie.ajouterSlider("Cout mer", 0.01f, 0.3f, 0.01f, e.coutMer, v -> e.coutMer = v);
        sectionEconomie.ajouterSlider("Emplois du commerce", 0f, 3f, 0.1f, e.emplois, v -> e.emplois = v);
        sectionEconomie.ajouterSlider("Part urbaine du commerce", 0f, 0.9f, 0.05f, e.partCommerce, v -> e.partCommerce = v);

        for (int g = 0; g < Economie.NOMBRE; g++) {
            ajouterMarchandise(sectionEconomie, g);
        }


        for (int rang = 0; rang < 5; rang++) {
            ajouterGrandeVille(grandes, rang);
        }

        for (int q = 0; q < Peuple.NOMBRE; q++) {
            ajouterPeuple(sectionHistoire, q);
        }

    }

    void jouer(Supplier<Joueur.Scene> suite) {
        if (histoire == null) return;
        Histoire courante = histoire;
        if (courante.population.annee < courante.dureeMax) {
            ui.afficher("Patience", "L'histoire n'est pas encore finie.", java.util.List.of(), i -> { });
            return;
        }
        courante.demander(() -> {
            Joueur.Scene scene = suite.get();
            // Lot K3 : tant que le personnage se crée, ses étapes s'affichent dans l'écran de création, avec la fiche à jour.
            com.deldaran.politics.histoire.Creation creation = courante.joueur.creationEnCours();
            java.util.List<String[]> fiche = creation != null && !scene.fermer ? creation.resume() : null;
            // Lot K4 : la vignette (qui est là), et où est la ville natale pour son décor.
            com.deldaran.politics.histoire.Creation.Tableau tableau = fiche != null ? creation.vignette() : null;
            double[] lieu = tableau != null && tableau.ville >= 0 ? lieuVille(courante.population, tableau.ville) : null;
            Gdx.app.postRunnable(() -> {
                if (scene.fermer) {
                    ui.fermer();
                    ecranCreation.fermer();
                    return;
                }
                if (fiche != null) {
                    ui.fermer();
                    if (tableau != null) ecranCreation.montrerVignette(tableau, lieu != null ? lieu[0] : 0.0, lieu != null ? lieu[1] : 0.0, lieu != null ? (int) lieu[2] : -1);
                    if (scene.carte != Joueur.Scene.SANS_CARTE && ecranCreation.carteManque()) ecranCreation.donnerCarte(monde, courante.population, carteAPlat());
                    ecranCreation.afficher(scene, fiche, i -> jouer(scene.suites.get(i)), texte -> jouer(() -> scene.valider.apply(texte)), () -> jouer(scene.annuler));
                    return;
                }
                ecranCreation.fermer();
                if (scene.saisie != null) ui.saisir(scene.titre, scene.texte, scene.saisie, texte -> jouer(() -> scene.valider.apply(texte)), () -> jouer(scene.annuler));
                else ui.afficher(scene.titre, scene.texte, scene.choix, i -> jouer(scene.suites.get(i)));
            });
        });
    }

    /** La longitude, la latitude (radians) et l'État vivant (-1 sinon) d'une ville. */
    private double[] lieuVille(Population p, int ville) {
        int c = p.villes.caseDe[ville];
        com.deldaran.politics.planet.Sphere s = monde.sphere;
        double lon = Math.atan2(s.y[c], s.x[c]), lat = Math.asin(Math.max(-1.0, Math.min(1.0, s.z[c])));
        int r = p.villes.region[ville];
        int e = r < 0 ? -1 : p.etats.regionEtat[r];
        return new double[] { lon, lat, e >= 0 && p.etats.estVivant(e) ? e : -1 };
    }

    private void montrerTexte(String titre, Function<Population, String> texte) {
        if (histoire == null) return;
        Histoire courante = histoire;
        courante.demander(() -> {
            String resultat = texte.apply(courante.population);
            Gdx.app.postRunnable(() -> fenetreDebug.afficherTexte(titre, resultat));
        });
    }


    private void ajouterFait(FenetreDebug.Section section, int rang) {
        infoHistoire(section, String.valueOf(rang + 1), () -> {
            Population p = histoire.population;
            int i = p.etats.chronique.recent(rang);
            return i < 0 ? "-" : p.etats.chronique.texte(p, i);
        });
    }


    private void ajouterMarchandise(FenetreDebug.Section parent, int g) {
        FenetreDebug.Section section = parent.ajouterSection(Economie.NOMS[g]);
        infoHistoire(section, "Production", () -> String.format("%.0f", histoire.population.economie.total(histoire.population.economie.production, g)));
        infoHistoire(section, "Besoin", () -> String.format("%.0f", histoire.population.economie.total(histoire.population.economie.besoin, g)));
        infoHistoire(section, "Stock", () -> String.format("%.0f", histoire.population.economie.stockTotal(g)));
        infoHistoire(section, "Prix moyen", () -> String.format("%.2f", histoire.population.economie.prixMoyen(g)));
    }


    private void ajouterGrandeVille(FenetreDebug.Section section, int rang) {
        infoHistoire(section, String.valueOf(rang + 1), () -> {
            Population p = histoire.population;
            int v = p.villes.plusGrandes(rang + 1)[rang];
            if (v < 0) return "-";
            return Noms.ville(p.villes.peuple[v], p.graine, v) + " " + p.villes.habitants[v];
        });
    }

    private void ajouterPeuple(FenetreDebug.Section parent, int q) {
        Peuple peuple = reglagesPopulation.peuples[q];
        FenetreDebug.Section section = parent.ajouterSection(peuple.nom);
        infoHistoire(section, "Vivants", () -> String.valueOf(histoire.population.vivantsPeuple[q]));
        infoHistoire(section, "Regions", () -> String.valueOf(histoire.population.regionsPeuple[q]));
        infoHistoire(section, "Urbains", () -> String.valueOf(histoire.population.urbainsPeuple[q]));
        infoHistoire(section, "Age au deces", () -> String.format("%.0f ans", histoire.population.ageAuDeces[q]));
        section.ajouterSlider("Mobilite", 0f, 2f, 0.1f, peuple.mobilite, v -> peuple.mobilite = v);
        section.ajouterSlider("Part urbaine", 0f, 1f, 0.05f, peuple.urbain, v -> peuple.urbain = v);
        reglerDepart(section, "Rythme de vie", 0.05f, 2f, 0.05f, peuple.rythme, v -> peuple.rythme = v);
        reglerDepart(section, "Annee d'eveil", 0f, 4000f, 100f, peuple.eveil, v -> peuple.eveil = v.intValue());
        reglerDepart(section, "Berceaux", 1f, 6f, 1f, peuple.berceaux, v -> peuple.berceaux = v.intValue());
        reglerDepart(section, "Fondateurs", 200f, 5000f, 100f, peuple.fondateurs, v -> peuple.fondateurs = v.intValue());
    }

    private int villeProche() {
        double cosLat = Math.cos(camera.latitude);
        double cx = cosLat * Math.cos(camera.longitude), cy = cosLat * Math.sin(camera.longitude), cz = Math.sin(camera.latitude);
        int meilleure = -1;
        double proche = -2.0;
        for (int i = 0; i < villages.nombreVillages(); i++) {
            Villages.Village w = villages.village(i);
            double d = w.x * cx + w.y * cy + w.z * cz;
            if (d > proche) {
                proche = d;
                meilleure = w.ville;
            }
        }
        return meilleure;
    }

    private void infoHistoire(FenetreDebug.Section section, String nom, Supplier<String> valeur) {
        section.ajouterInfo(nom, () -> histoire == null ? "-" : valeur.get());
    }

    private void reglerDepart(FenetreDebug.Section section, String nom, float min, float max, float pas, float depart, Consumer<Float> action) {
        section.ajouterSlider(nom, min, max, pas, depart, valeur -> {
            action.accept(valeur);
            arreterHistoire();
        });
    }

    private void arreterHistoire() {
        reprendreHistoire = false;
        if (histoire != null) {
            histoire.arreter();
            histoire = null;
        }
    }

    private void infoRegion(FenetreDebug.Section section, String nom, Supplier<String> valeur) {
        section.ajouterInfo(nom, () -> regions == null ? "-" : valeur.get());
    }

    private void reglerRegion(FenetreDebug.Section section, String nom, float min, float max, float pas, float depart, Consumer<Float> action) {
        section.ajouterSlider(nom, min, max, pas, depart, valeur -> {
            action.accept(valeur);
            arreterHistoire();
            regions = null;
            modeCarte = -1;
        });
    }

    private double surfaceCase() {
        return 4.0 * Math.PI * Terrain.RAYON_KM * Terrain.RAYON_KM / monde.plaque.length;
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        if (habitants.relations.modifie && fichierRelations != null) habitants.relations.enregistrer(fichierRelations);
        if (ia != null) ia.dispose();
    }

    @Override
    public void dispose() {
        carte.dispose();
        fond.dispose();
        rendu.dispose();
        if (tuiles != null) tuiles.dispose();
        if (vegetation != null) vegetation.dispose();
        if (assembleur != null) assembleur.dispose();
        if (batiments != null) batiments.dispose();
        if (meteo != null) meteo.dispose();
        if (ciel != null) ciel.dispose();
        if (incendies != null) incendies.dispose();
        if (traces != null) traces.dispose();
        if (rues != null) rues.dispose();
        fenetreDebug.dispose();
        if (pause != null) pause.dispose();
        if (barre != null) barre.dispose();
        if (etiquetteCarte != null) etiquetteCarte.dispose();
        if (ecranCreation != null) ecranCreation.dispose();
        if (ui != null) ui.dispose();
        chargement.dispose();
        if (habitants.relations.modifie && fichierRelations != null) habitants.relations.enregistrer(fichierRelations);
        if (ia != null) ia.dispose();
        arreterHistoire();
    }
}
