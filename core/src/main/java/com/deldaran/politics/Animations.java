package com.deldaran.politics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Animations {
    public static final int BOUCLE = 0, UNE_FOIS = 1, TENIR = 2;
    public static final int FACE = 0, COTE = 1, DESSUS = 2;
    private static final String[] VISAGES = { "neutre", "colere", "triste", "joie", "peur", "douleur", "dort", "cri" };
    private static Animations toutes;

    private static class Reglage {
        final List<int[]> indices = new ArrayList<>();
        final List<double[]> valeurs = new ArrayList<>();
        int objetD = Pose.GARDER, objetG = Pose.GARDER, visage = -1;

        void appliquer(Pose p) {
            for (int k = 0; k < indices.size(); k++) {
                int i = indices.get(k)[0];
                double v = valeurs.get(k)[0];
                p.v[i] = indices.get(k)[1] == 1 ? p.v[i] + v : v;
            }
            if (objetD != Pose.GARDER) p.objetD = objetD;
            if (objetG != Pose.GARDER) p.objetG = objetG;
            if (visage >= 0) p.expression = visage;
        }
    }

    public static class Animation {
        public int id;
        public String nom, categorie, bulle;
        public int images, mode;
        public float duree;
        public boolean mobile;
        public String nomPartenaire;
        public Animation partenaire;
        public double distance = 0.6;
        public int placement = FACE;
        final List<Double> temps = new ArrayList<>();
        final List<Pose> cles = new ArrayList<>();

        public void pose(int image, Pose sortie) {
            double t = mode == BOUCLE ? (double) image / images : images <= 1 ? 0.0 : (double) image / (images - 1);
            int n = cles.size();
            if (n == 1) {
                sortie.copier(cles.get(0));
                return;
            }
            int k = n - 1;
            for (int i = 0; i < n; i++) {
                if (temps.get(i) > t) {
                    k = i - 1;
                    break;
                }
            }
            Pose a, b;
            double t0, t1;
            if (k < 0) {
                if (mode != BOUCLE) {
                    sortie.copier(cles.get(0));
                    return;
                }
                a = cles.get(n - 1);
                b = cles.get(0);
                t0 = temps.get(n - 1) - 1.0;
                t1 = temps.get(0);
            } else if (k == n - 1) {
                if (mode != BOUCLE) {
                    sortie.copier(cles.get(n - 1));
                    return;
                }
                a = cles.get(n - 1);
                b = cles.get(0);
                t0 = temps.get(n - 1);
                t1 = temps.get(0) + 1.0;
            } else {
                a = cles.get(k);
                b = cles.get(k + 1);
                t0 = temps.get(k);
                t1 = temps.get(k + 1);
            }
            double u = t1 - t0 < 1e-9 ? 0.0 : (t - t0) / (t1 - t0);
            u = 0.5 - 0.5 * Math.cos(Math.max(0.0, Math.min(1.0, u)) * Math.PI);
            Pose.melanger(a, b, u, sortie);
        }

        public int image(float temps) {
            int f = (int) Math.floor(temps / duree * (mode == BOUCLE ? images : Math.max(1, images - 1)));
            if (mode == BOUCLE) return Math.floorMod(f, images);
            return Math.max(0, Math.min(images - 1, f));
        }

        public boolean fini(float temps) {
            return mode == UNE_FOIS && temps >= duree;
        }
    }

    public final List<Animation> liste = new ArrayList<>();
    private final Map<String, Animation> parNom = new LinkedHashMap<>();
    private final Map<String, Reglage> poses = new LinkedHashMap<>();

    public static Animations toutes() {
        if (toutes == null) toutes = charger(com.badlogic.gdx.Gdx.files.internal("animations/animations.txt").readString("UTF-8"));
        return toutes;
    }

    public Animation get(String nom) {
        return parNom.get(nom);
    }

    public static Animations charger(String texte) {
        Animations a = new Animations();
        Animation courante = null;
        int numero = 0;
        for (String brute : texte.split("\n")) {
            numero++;
            String ligne = brute.trim();
            int diese = ligne.indexOf('#');
            if (diese >= 0) ligne = ligne.substring(0, diese).trim();
            if (ligne.isEmpty()) continue;
            String[] t = ligne.split("\\s+");
            try {
                if (t[0].equals("pose")) {
                    Reglage r = new Reglage();
                    a.lire(r, t, 2);
                    a.poses.put(t[1], r);
                    courante = null;
                } else if (t[0].equals("anim")) {
                    courante = new Animation();
                    courante.id = a.liste.size();
                    courante.nom = t[1];
                    courante.categorie = t[2];
                    courante.images = Integer.parseInt(t[3]);
                    courante.duree = Float.parseFloat(t[4]);
                    courante.mode = t[5].equals("boucle") ? BOUCLE : t[5].equals("tenir") ? TENIR : UNE_FOIS;
                    Reglage commun = new Reglage();
                    for (int i = 6; i < t.length; i++) {
                        String mot = t[i];
                        if (mot.equals("mobile")) courante.mobile = true;
                        else if (mot.startsWith("partenaire=")) courante.nomPartenaire = mot.substring(11);
                        else if (mot.startsWith("distance=")) courante.distance = Double.parseDouble(mot.substring(9));
                        else if (mot.startsWith("bulle=")) courante.bulle = mot.substring(6);
                        else if (mot.startsWith("placement=")) courante.placement = mot.endsWith("cote") ? COTE : mot.endsWith("dessus") ? DESSUS : FACE;
                        else a.lire(commun, new String[] { mot }, 0);
                    }
                    a.poses.put("@" + courante.nom, commun);
                    a.liste.add(courante);
                    a.parNom.put(courante.nom, courante);
                } else if (courante != null) {
                    double temps = Double.parseDouble(t[0]);
                    Pose p = new Pose();
                    a.poses.get("@" + courante.nom).appliquer(p);
                    int debut = 1;
                    if (t.length > 1 && !t[1].contains("=")) {
                        for (String base : t[1].split("\\+")) {
                            Reglage r = a.poses.get(base);
                            if (r == null) throw new IllegalArgumentException("pose inconnue : " + base);
                            r.appliquer(p);
                        }
                        debut = 2;
                    }
                    Reglage r = new Reglage();
                    a.lire(r, t, debut);
                    r.appliquer(p);
                    courante.temps.add(temps);
                    courante.cles.add(p);
                }
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("animations.txt ligne " + numero + " : " + brute.trim() + " (" + e.getMessage() + ")", e);
            }
        }
        for (Animation x : a.liste) {
            if (x.cles.isEmpty()) {
                x.temps.add(0.0);
                Pose p = new Pose();
                a.poses.get("@" + x.nom).appliquer(p);
                x.cles.add(p);
            }
            if (x.nomPartenaire != null) {
                x.partenaire = a.parNom.get(x.nomPartenaire);
                if (x.partenaire == null) throw new IllegalArgumentException("partenaire inconnu : " + x.nomPartenaire);
            }
        }
        return a;
    }

    private void lire(Reglage r, String[] t, int debut) {
        for (int i = debut; i < t.length; i++) {
            String mot = t[i];
            if (!mot.contains("=")) {
                Reglage base = poses.get(mot);
                if (base == null) throw new IllegalArgumentException("pose inconnue : " + mot);
                r.indices.addAll(base.indices);
                r.valeurs.addAll(base.valeurs);
                if (base.objetD != Pose.GARDER) r.objetD = base.objetD;
                if (base.objetG != Pose.GARDER) r.objetG = base.objetG;
                if (base.visage >= 0) r.visage = base.visage;
                continue;
            }
            boolean ajout = mot.contains("+=");
            String[] kv = mot.split("\\+?=");
            String cle = kv[0], valeur = kv[1];
            switch (cle) {
                case "md": r.objetD = Peintre.objet(valeur); break;
                case "mg": r.objetG = Peintre.objet(valeur); break;
                case "visage": r.visage = visage(valeur); break;
                default: {
                    int indice = Pose.indice(cle);
                    if (indice < 0) throw new IllegalArgumentException("parametre inconnu : " + cle);
                    double v = Double.parseDouble(valeur);
                    if (Pose.angle(indice)) v = Math.toRadians(v);
                    r.indices.add(new int[] { indice, ajout ? 1 : 0 });
                    r.valeurs.add(new double[] { v });
                }
            }
        }
    }

    private static int visage(String nom) {
        for (int i = 0; i < VISAGES.length; i++) if (VISAGES[i].equals(nom)) return i;
        throw new IllegalArgumentException("visage inconnu : " + nom);
    }
}
