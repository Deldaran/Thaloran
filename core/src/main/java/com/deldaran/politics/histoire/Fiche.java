package com.deldaran.politics.histoire;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class Fiche {
    public static final int VIGUEUR = 0, ADRESSE = 1, ENDURANCE = 2, HABILETE = 3, ESPRIT = 4, PRESENCE = 5, RUSE = 6, ATTRIBUTS = 7;
    public static final String[] NOMS_ATTRIBUTS = { "Vigueur", "Adresse", "Endurance", "Habileté", "Esprit", "Présence", "Ruse" };

    public static final int UNE_MAIN = 0, DEUX_MAINS = 1, HAST = 2, ARC = 3, ARBALETE = 4, JET = 5, ATHLETISME = 6, EQUITATION = 7, LABEUR = 8,
            FORGE = 9, BOIS = 10, METIERS_FINS = 11, LETTRES = 12, MEDECINE = 13, INTENDANCE = 14, ELOQUENCE = 15, COMMANDEMENT = 16, COMMERCE = 17,
            DISCRETION = 18, SURVIE = 19, TACTIQUE = 20, COMPETENCES = 21;
    public static final String[] NOMS_COMPETENCES = { "Une main", "Deux mains", "Hast", "Arc", "Arbalète", "Jet", "Athlétisme", "Équitation", "Labeur",
            "Forge", "Bois", "Métiers fins", "Lettres", "Médecine", "Intendance", "Éloquence", "Commandement", "Commerce", "Discrétion", "Survie", "Tactique" };

    public static final int LOI = 0, BIEN = 1, AMBITION = 2, COURAGE = 3, FOI = 4, GENEROSITE = 5, HONNETETE = 6, AXES = 7;
    public static final String[][] POLES = { { "Loyal", "Rebelle" }, { "Bon", "Cruel" }, { "Ambitieux", "Humble" }, { "Brave", "Prudent" }, { "Pieux", "Impie" },
            { "Généreux", "Avare" }, { "Honnête", "Fourbe" } };
    public static final int SEUIL_TRAIT = 25;

    public static final int ATTRIBUT_DEPART = 2, ATTRIBUT_MAX = 10, FOCUS_MAX = 5, COMPETENCE_MAX = 300, LECTURE = 70;

    public static final int PERE = 0, MERE = 1, FRERE = 2, SOEUR = 3, MAITRE = 4, AMI = 5, RIVAL = 6, AMOUR = 7, CONJOINT = 8, ENFANT = 9, CAMARADE = 10, ONCLE = 11, AIEUL = 12;
    public static final String[][] ROLES = { { "père", "père" }, { "mère", "mère" }, { "frère", "frère" }, { "sœur", "sœur" }, { "maître", "maîtresse" },
            { "ami", "amie" }, { "rival", "rivale" }, { "premier amour", "premier amour" }, { "époux", "épouse" }, { "fils", "fille" }, { "camarade", "camarade" },
            { "oncle", "tante" }, { "grand-père", "grand-mère" } };

    public static final class Proche {
        public long id;
        public int role;
        public int sexe;
        public int estime;
        public boolean mort;
        public String note = "";

        Proche copie() {
            Proche p = new Proche();
            p.id = id;
            p.role = role;
            p.sexe = sexe;
            p.estime = estime;
            p.mort = mort;
            p.note = note;
            return p;
        }
    }

    public String nom = "";
    public int peuple = -1, sexe, naissance, ville = -1, region = -1;
    public long id = -1L, apparence;
    /** Lot K3 : l'apparence choisie à la création (coiffure, habit, couleurs…). */
    public Aspect aspect = new Aspect();
    public int[] attributs = new int[ATTRIBUTS];
    public int[] competences = new int[COMPETENCES];
    public int[] focus = new int[COMPETENCES];
    public float[] experience = new float[COMPETENCES];
    public int[] axes = new int[AXES];
    public int argent, renom;
    public int origine = -1, voie = -1, aspiration = -1, etape = 0, ennemi = -1;
    public boolean aine;
    public List<String> objets = new ArrayList<>();
    public Inventaire sac = new Inventaire();
    public List<String> recit = new ArrayList<>();
    public List<Proche> proches = new ArrayList<>();

    public Fiche() {
        java.util.Arrays.fill(attributs, ATTRIBUT_DEPART);
    }

    public static int attributDe(int competence) {
        return competence / 3;
    }

    public boolean femme() {
        return sexe == 1;
    }

    public String e() {
        return femme() ? "e" : "";
    }

    public void gagner(int competence, int points) {
        competences[competence] = Math.max(0, Math.min(COMPETENCE_MAX, competences[competence] + points));
    }

    public void augmenter(int attribut, int points) {
        attributs[attribut] = Math.max(1, Math.min(ATTRIBUT_MAX, attributs[attribut] + points));
    }

    public void concentrer(int competence) {
        focus[competence] = Math.min(FOCUS_MAX, focus[competence] + 1);
    }

    public void pencher(int axe, int valeur) {
        axes[axe] = Math.max(-100, Math.min(100, axes[axe] + valeur));
    }

    public int plafond(int competence) {
        return Math.min(COMPETENCE_MAX, 20 + attributs[attributDe(competence)] * 15 + focus[competence] * 30);
    }

    public float vitesse(int competence) {
        float base = 0.4f + attributs[attributDe(competence)] * 0.12f + focus[competence] * 0.3f;
        float reste = 1f - competences[competence] / (float) Math.max(1, plafond(competence));
        return base * Math.max(0.05f, reste);
    }

    public int apprendre(int competence, float effort) {
        experience[competence] += effort * vitesse(competence);
        int gagnes = 0;
        while (competences[competence] < COMPETENCE_MAX) {
            float cout = 10f + competences[competence] * 0.6f;
            if (experience[competence] < cout) break;
            experience[competence] -= cout;
            competences[competence]++;
            gagnes++;
        }
        return gagnes;
    }

    /** Range un objet décrit en toutes lettres : dans le sac si c'est un bien du catalogue, sinon parmi les possessions (« un lopin de terre »). */
    public void recevoir(String objet) {
        int[] r = Biens.reconnaitre(objet);
        if (r == null) {
            if (!objets.contains(objet)) objets.add(objet);
        } else if (sac.nombre(r[0]) < r[1]) sac.ajouter(r[0], r[1] - sac.nombre(r[0]));
    }

    /** Kilos que le personnage porte sans être ralenti. */
    public float capacite() {
        return 20f + 3f * attributs[VIGUEUR] + 2f * attributs[ENDURANCE] + competences[ATHLETISME] / 20f + sac.charge();
    }

    /** 1 = allure normale ; descend jusqu'à 0,35 quand le sac est trop lourd. */
    public float allure() {
        float poids = sac.poids(), cap = capacite();
        if (poids <= cap) return 1f;
        return Math.max(0.35f, cap / poids);
    }

    public int instruction() {
        return Math.min(100, competences[LETTRES] / 2);
    }

    public boolean lit() {
        return competences[LETTRES] >= LECTURE;
    }

    public String trait(int axe) {
        if (axes[axe] >= SEUIL_TRAIT) return POLES[axe][0];
        if (axes[axe] <= -SEUIL_TRAIT) return POLES[axe][1];
        return null;
    }

    public List<String> traits() {
        List<String> t = new ArrayList<>();
        for (int a = 0; a < AXES; a++) {
            String s = trait(a);
            if (s != null) t.add(femme() ? feminin(s) : s);
        }
        return t;
    }

    static String feminin(String s) {
        switch (s) {
            case "Loyal": return "Loyale";
            case "Bon": return "Bonne";
            case "Cruel": return "Cruelle";
            case "Ambitieux": return "Ambitieuse";
            case "Pieux": return "Pieuse";
            case "Généreux": return "Généreuse";
            default: return s;
        }
    }

    public static String argentTexte(int cuivre) {
        int or = cuivre / 10000, argent = cuivre / 100 % 100, reste = cuivre % 100;
        StringBuilder b = new StringBuilder();
        if (or > 0) b.append(or).append(" or ");
        if (argent > 0) b.append(argent).append(" argent ");
        if (reste > 0 || b.length() == 0) b.append(reste).append(" cuivre");
        return b.toString().trim();
    }

    public Proche proche(int role) {
        for (Proche p : proches) if (p.role == role) return p;
        return null;
    }

    public Proche lier(long autre, int role, int sexeAutre, int estime, boolean mort) {
        Proche p = new Proche();
        p.id = autre;
        p.role = role;
        p.sexe = sexeAutre;
        p.estime = estime;
        p.mort = mort;
        proches.add(p);
        return p;
    }

    public String role(Proche p) {
        return ROLES[p.role][p.sexe == 1 ? 1 : 0];
    }

    public String resume() {
        StringBuilder b = new StringBuilder();
        b.append("Attributs : ");
        for (int a = 0; a < ATTRIBUTS; a++) {
            if (a > 0) b.append(", ");
            b.append(NOMS_ATTRIBUTS[a]).append(' ').append(attributs[a]);
        }
        b.append("\n\nCompétences :");
        for (int a = 0; a < ATTRIBUTS; a++) {
            b.append("\n  ").append(NOMS_ATTRIBUTS[a]).append(" — ");
            for (int k = 0; k < 3; k++) {
                int c = a * 3 + k;
                if (k > 0) b.append(", ");
                b.append(NOMS_COMPETENCES[c]).append(' ').append(competences[c]);
                for (int f = 0; f < focus[c]; f++) b.append(f == 0 ? " " : "").append('•');
            }
        }
        List<String> t = traits();
        b.append("\n\nCaractère : ").append(t.isEmpty() ? "rien de marqué" : String.join(", ", t));
        b.append("\nArgent : ").append(argentTexte(argent));
        b.append("\nRenom : ").append(renom);
        b.append(lit() ? "\nTu sais lire et écrire." : "\nTu ne sais pas lire.");
        b.append("\nSac : ").append(sac.texte()).append(" (").append(Biens.poids(sac.poids())).append(" sur ").append(Biens.poids(capacite())).append(')');
        if (!objets.isEmpty()) b.append("\nBiens : ").append(String.join(", ", objets));
        return b.toString();
    }

    public Fiche copie() {
        Fiche f = new Fiche();
        f.nom = nom;
        f.peuple = peuple;
        f.sexe = sexe;
        f.naissance = naissance;
        f.ville = ville;
        f.region = region;
        f.id = id;
        f.apparence = apparence;
        // Partagé et non copié : revenir en arrière dans la création ne défait pas les retouches d'apparence.
        f.aspect = aspect;
        f.attributs = attributs.clone();
        f.competences = competences.clone();
        f.focus = focus.clone();
        f.experience = experience.clone();
        f.axes = axes.clone();
        f.argent = argent;
        f.renom = renom;
        f.origine = origine;
        f.voie = voie;
        f.aspiration = aspiration;
        f.etape = etape;
        f.ennemi = ennemi;
        f.aine = aine;
        f.objets = new ArrayList<>(objets);
        f.sac = sac.copie();
        f.recit = new ArrayList<>(recit);
        f.proches = new ArrayList<>();
        for (Proche p : proches) f.proches.add(p.copie());
        return f;
    }

    public void ecrire(DataOutputStream s) throws IOException {
        s.writeUTF(nom);
        s.writeInt(peuple);
        s.writeInt(sexe);
        s.writeInt(naissance);
        s.writeInt(ville);
        s.writeInt(region);
        s.writeLong(id);
        s.writeLong(apparence);
        for (int v : attributs) s.writeByte(v);
        for (int v : competences) s.writeShort(v);
        for (int v : focus) s.writeByte(v);
        for (float v : experience) s.writeFloat(v);
        for (int v : axes) s.writeByte(v);
        s.writeInt(argent);
        s.writeInt(renom);
        s.writeInt(origine);
        s.writeInt(voie);
        s.writeInt(aspiration);
        s.writeInt(etape);
        s.writeInt(ennemi);
        s.writeBoolean(aine);
        s.writeInt(objets.size());
        for (String o : objets) s.writeUTF(o);
        s.writeInt(recit.size());
        for (String r : recit) s.writeUTF(r);
        s.writeInt(proches.size());
        for (Proche p : proches) {
            s.writeLong(p.id);
            s.writeByte(p.role);
            s.writeByte(p.sexe);
            s.writeShort(p.estime);
            s.writeBoolean(p.mort);
            s.writeUTF(p.note);
        }
        sac.ecrire(s);
    }

    public static Fiche lire(DataInputStream s, boolean avecSac) throws IOException {
        Fiche f = new Fiche();
        f.nom = s.readUTF();
        f.peuple = s.readInt();
        f.sexe = s.readInt();
        f.naissance = s.readInt();
        f.ville = s.readInt();
        f.region = s.readInt();
        f.id = s.readLong();
        f.apparence = s.readLong();
        for (int k = 0; k < ATTRIBUTS; k++) f.attributs[k] = s.readByte();
        for (int k = 0; k < COMPETENCES; k++) f.competences[k] = s.readShort();
        for (int k = 0; k < COMPETENCES; k++) f.focus[k] = s.readByte();
        for (int k = 0; k < COMPETENCES; k++) f.experience[k] = s.readFloat();
        for (int k = 0; k < AXES; k++) f.axes[k] = s.readByte();
        f.argent = s.readInt();
        f.renom = s.readInt();
        f.origine = s.readInt();
        f.voie = s.readInt();
        f.aspiration = s.readInt();
        f.etape = s.readInt();
        f.ennemi = s.readInt();
        f.aine = s.readBoolean();
        int n = s.readInt();
        for (int k = 0; k < n; k++) f.objets.add(s.readUTF());
        n = s.readInt();
        for (int k = 0; k < n; k++) f.recit.add(s.readUTF());
        n = s.readInt();
        for (int k = 0; k < n; k++) {
            Proche p = new Proche();
            p.id = s.readLong();
            p.role = s.readByte();
            p.sexe = s.readByte();
            p.estime = s.readShort();
            p.mort = s.readBoolean();
            p.note = s.readUTF();
            f.proches.add(p);
        }
        if (avecSac) f.sac = Inventaire.lire(s);
        else {
            List<String> anciens = new ArrayList<>(f.objets);
            f.objets.clear();
            for (String o : anciens) f.recevoir(o);
            f.sac.porterLeMieux();
        }
        return f;
    }
}
