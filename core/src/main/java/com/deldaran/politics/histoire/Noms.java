package com.deldaran.politics.histoire;

public final class Noms {
    private static final String[][] DEBUTS = {
        { "Ae", "Ili", "Sy", "Vae", "Lu", "Tha", "Ny", "Ela", "Ori", "Cae", "Mi", "Fae", "Ara", "Eo", "Isi", "Lae", "Nae", "Sae", "Ty", "Ula" },
        { "Du", "Kha", "Bo", "Thra", "Gra", "Mo", "Ha", "Dun", "Bra", "Ke", "Or", "Sto", "Bal", "Dra", "Fun", "Gro", "Kra", "Nor", "Tho", "Vor" },
        { "Al", "Ber", "Cal", "Dor", "Es", "Gar", "Hel", "Mar", "Nor", "Ros", "Sen", "Val", "Ar", "Bel", "Cor", "Fal", "Lor", "Mon", "Tor", "Ver" },
        { "Gru", "Zo", "Ukh", "Mo", "Sha", "Kra", "Ur", "Dro", "Gha", "Ra", "Vo", "Na", "Bu", "Gor", "Ka", "Lu", "Ma", "Sna", "Tu", "Ya" }
    };
    private static final String[][] MILIEUX = {
        { "la", "ri", "ne", "si", "va", "the", "lia", "ve", "mi", "ro" },
        { "ga", "ri", "do", "ka", "ru", "ba", "ma", "zi", "", "" },
        { "ma", "le", "ri", "ne", "ta", "ve", "lan", "so", "", "" },
        { "ga", "ru", "za", "ka", "ta", "mu", "", "", "", "" }
    };
    private static final String[][] FINS = {
        { "thil", "lien", "wen", "dar", "riel", "nor", "sae", "lune", "nith", "mar", "ion", "las", "rath", "vel" },
        { "dum", "grim", "heim", "dor", "zar", "bar", "kor", "rak", "dal", "gund", "mir", "nar", "thor", "vik" },
        { "bourg", "mont", "ville", "gard", "port", "val", "ford", "mer", "court", "haven", "lac", "pont", "roc", "stein" },
        { "gash", "mok", "dush", "rog", "gul", "nakh", "zug", "kar", "bur", "lug", "murk", "rath", "shak", "tuk" }
    };

    private static final String[][] FINS_PERSONNES = {
        { "ndil", "riel", "wyn", "las", "thien", "dor", "mir", "ael" },
        { "in", "ur", "ik", "or", "grim", "dain", "bur", "rin" },
        { "ric", "bert", "mond", "aud", "ard", "ine", "elle", "ian" },
        { "ug", "ash", "ok", "uk", "ar", "um", "ag", "oth" }
    };


    private Noms() {
    }

    public static String ville(int peuple, long graine, int ville) {
        long h = graine * 0x9E3779B97F4A7C15L + ville * 0xBF58476D1CE4E5B9L + peuple;
        h = (h ^ (h >>> 30)) * 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return choisir(DEBUTS[peuple], h) + choisir(MILIEUX[peuple], h >>> 12) + choisir(FINS[peuple], h >>> 24);
    }
    
    public static String personne(int peuple, long graine, long numero) {
        long h = graine * 0xD1342543DE82EF95L + numero * 0x9E3779B97F4A7C15L + peuple;
        h = (h ^ (h >>> 29)) * 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return choisir(DEBUTS[peuple], h) + choisir(FINS_PERSONNES[peuple], h >>> 16);
    }


    private static String choisir(String[] liste, long h) {
        return liste[(int) ((h & 0xFFFL) % liste.length)];
    }
}
