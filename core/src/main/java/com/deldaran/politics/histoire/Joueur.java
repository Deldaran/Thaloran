package com.deldaran.politics.histoire;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public class Joueur {
    private static final int FORMAT = 1;
    private static final int VOISINS = 6;
    private static final int ETAGERE = 7;
    private static final int PAROLES = 2;
    private static final int SOUVENIRS = 60;

    public static final class Scene {
        public final String titre;
        public final String texte;
        public final List<String> choix = new ArrayList<>();
        public final List<Supplier<Scene>> suites = new ArrayList<>();

        Scene(String titre, String texte) {
            this.titre = titre;
            this.texte = texte;
        }

        void ajouter(String nom, Supplier<Scene> suite) {
            choix.add(nom);
            suites.add(suite);
        }
    }

    private final Population p;
    private final FileHandle fichier;
    private final LinkedHashSet<String> carnet = new LinkedHashSet<>();
    private int peuple = -1;
    private int region = -1;
    private int place = -1;
    private long id = -1L;
    private int quartier = 0;

    public Joueur(Population p, FileHandle fichier) {
        this.p = p;
        this.fichier = fichier;
    }

    private Foule foule() {
        return p.foules[peuple][region];
    }

    private boolean enVie() {
        return id >= 0L && foule().estEnVie(place, id);
    }

    private Savoir.Temoin moi() {
        return Savoir.temoin(p, foule(), place);
    }

    public Scene ouvrir() {
        return enVie() ? accueil() : choisir();
    }

    private Scene choisir() {
        Scene s = new Scene("Entrer dans le monde", "Qui veux-tu être ?");
        for (int q = 0; q < Peuple.NOMBRE; q++) {
            int indice = q;
            s.ajouter(p.reglages.peuples[q].nom, () -> incarner(indice));
        }
        s.ajouter("Au hasard", () -> incarner(-1));
        return s;
    }

    private Scene incarner(int voulu) {
        Random hasard = new Random();
        Villes v = p.villes;
        for (int essai = 0; essai < 20000; essai++) {
            int ville = hasard.nextInt(v.nombre);
            if (!v.estVivante(ville) || (voulu >= 0 && v.peuple[ville] != voulu)) continue;
            Foule f = p.foules[v.peuple[ville]][v.region[ville]];
            if (f == null || f.vivants == 0) continue;
            int trouve = f.habitantAuHasard(ville, p.annee, p.reglages.ageAdulte, hasard.nextLong() >>> 1);
            if (trouve < 0) continue;
            peuple = f.indicePeuple;
            region = f.region;
            place = trouve;
            id = f.idDe(trouve);
            quartier = 0;
            carnet.clear();
            enregistrer();
            return accueil();
        }
        return new Scene("Entrer dans le monde", "Ce peuple n'a plus de ville.");
    }

    private Scene accueil() {
        Savoir.Temoin t = moi();
        Scene s = new Scene(Recit.nom(p, t), Recit.sePresenter(p, t) + "\n\n" + Recit.gouvernement(p, t));
        s.ajouter("Ce que je sais", this::memoire);
        s.ajouter("Regarder autour de moi", this::autour);
        s.ajouter("Chercher des livres et des chansons", () -> etagere(0));
        s.ajouter("Relire mon carnet (" + carnet.size() + ")", this::relire);
        s.ajouter("Devenir quelqu'un d'autre", this::choisir);
        return s;
    }

    private Scene memoire() {
        Savoir.Temoin t = moi();
        Scene s = new Scene("Ce que je sais", Recit.parler(p, t, Savoir.souvenirs(p, t, SOUVENIRS), 8));
        s.ajouter("Revenir", this::accueil);
        return s;
    }

    private List<Integer> entourage(Savoir.Temoin t) {
        Foule f = foule();
        List<Integer> places = new ArrayList<>();
        Etats e = p.etats;
        if (t.etat >= 0 && e.capitale[t.etat] == t.ville && e.peuple[t.etat] == peuple && e.placeChef(t.etat) != place) {
            places.add(e.placeChef(t.etat));
        }
        for (int k = 0; k < VOISINS * 4 && places.size() < VOISINS; k++) {
            long tirage = (long) (Savoir.tirage(id + quartier, k) * Long.MAX_VALUE);
            int voisin = f.habitantAuHasard(t.ville, p.annee, p.reglages.ageAdulte, tirage);
            if (voisin >= 0 && voisin != place && !places.contains(voisin)) places.add(voisin);
        }
        return places;
    }

    private Scene autour() {
        Savoir.Temoin t = moi();
        Foule f = foule();
        List<Integer> places = entourage(t);
        Scene s = new Scene("Autour de moi", places.isEmpty() ? "Il n'y a personne ici." : "À qui veux-tu parler ?");
        for (int voisin : places) {
            Savoir.Temoin autre = Savoir.temoin(p, f, voisin);
            s.ajouter(Recit.decrire(p, autre), () -> rencontrer(autre));
        }
        s.ajouter("Aller dans un autre quartier", () -> {
            quartier++;
            return autour();
        });
        s.ajouter("Revenir", this::accueil);
        return s;
    }

    private Scene rencontrer(Savoir.Temoin t) {
        return conversation(t, Recit.saluer(p, t), 0);
    }

    private Scene conversation(Savoir.Temoin t, String texte, int rang) {
        Scene s = new Scene(Recit.nom(p, t), texte);
        s.ajouter("Qui es-tu ?", () -> conversation(t, Recit.sePresenter(p, t), rang));
        s.ajouter("Qui gouverne ici ?", () -> conversation(t, Recit.gouvernement(p, t), rang));
        s.ajouter("Raconte-moi le passé", () -> raconter(t, rang));
        s.ajouter("Au revoir", this::autour);
        return s;
    }

    private Scene raconter(Savoir.Temoin t, int rang) {
        List<String> phrases = Recit.paroles(p, t, Savoir.souvenirs(p, t, SOUVENIRS));
        if (phrases.isEmpty()) return conversation(t, "Je ne sais pas grand-chose de tout ça. Ici, on s'occupe de ses bêtes et de sa récolte.", rang);
        if (rang >= phrases.size()) return conversation(t, "Je t'ai dit tout ce que je sais.", rang);
        int fin = Math.min(phrases.size(), rang + PAROLES);
        String nom = Recit.nom(p, t);
        StringBuilder texte = new StringBuilder();
        for (int k = rang; k < fin; k++) {
            if (k > rang) texte.append("\n\n");
            texte.append(phrases.get(k));
            noter(nom + " : " + phrases.get(k));
        }
        enregistrer();
        return conversation(t, texte.toString(), fin);
    }

    private Scene etagere(int debut) {
        Savoir.Temoin t = moi();
        Livres l = p.livres;
        List<Integer> oeuvres = new ArrayList<>();
        for (int o = 0; o < l.nombre; o++) {
            if (l.accessible(p, o, t)) oeuvres.add(o);
        }
        boolean lecteur = t.instruction >= l.reglages.lecture;
        String texte = oeuvres.isEmpty() ? "Il n'y a ici rien que tu puisses lire ou chanter." : lecteur ? "Voici ce que tu trouves ici." : "Tu ne sais pas lire. Il te reste les chansons d'ici.";
        Scene s = new Scene("Livres et chansons", texte);
        int fin = Math.min(oeuvres.size(), debut + ETAGERE);
        for (int k = debut; k < fin; k++) {
            int o = oeuvres.get(k);
            s.ajouter(Recit.titre(p, o), () -> lire(o, debut));
        }
        if (oeuvres.size() > ETAGERE) s.ajouter("Autres titres", () -> etagere(fin >= oeuvres.size() ? 0 : fin));
        s.ajouter("Revenir", this::accueil);
        return s;
    }

    private Scene lire(int o, int debut) {
        String titre = Recit.titre(p, o);
        noter((p.livres.ecrit[o] ? "Lu : " : "Entendu : ") + titre);
        enregistrer();
        Scene s = new Scene(titre, Recit.lire(p, o));
        s.ajouter("Reposer", () -> etagere(debut));
        return s;
    }

    private Scene relire() {
        Scene s = new Scene("Mon carnet", carnet.isEmpty() ? "Ton carnet est vide." : String.join("\n\n", carnet));
        s.ajouter("Revenir", this::accueil);
        return s;
    }

    private void noter(String phrase) {
        carnet.add(phrase);
    }

    private void enregistrer() {
        try (DataOutputStream sortie = new DataOutputStream(new BufferedOutputStream(fichier.write(false)))) {
            sortie.writeInt(FORMAT);
            sortie.writeInt(peuple);
            sortie.writeInt(region);
            sortie.writeInt(place);
            sortie.writeLong(id);
            sortie.writeInt(quartier);
            sortie.writeInt(carnet.size());
            for (String phrase : carnet) {
                sortie.writeUTF(phrase);
            }
        } catch (IOException | GdxRuntimeException e) {
            System.err.println("Impossible d'enregistrer le joueur : " + e.getMessage());
        }
    }

    public void charger() {
        if (!fichier.exists()) return;
        try (DataInputStream entree = new DataInputStream(new BufferedInputStream(fichier.read()))) {
            if (entree.readInt() != FORMAT) return;
            int q = entree.readInt(), r = entree.readInt(), lieu = entree.readInt();
            long numero = entree.readLong();
            int coin = entree.readInt(), notes = entree.readInt();
            Foule f = p.foules[q][r];
            if (f == null || !f.estEnVie(lieu, numero)) return;
            List<String> lues = new ArrayList<>();
            for (int k = 0; k < notes; k++) {
                lues.add(entree.readUTF());
            }
            peuple = q;
            region = r;
            place = lieu;
            id = numero;
            quartier = coin;
            carnet.addAll(lues);
        } catch (IOException | RuntimeException e) {
            id = -1L;
            carnet.clear();
        }
    }
}
