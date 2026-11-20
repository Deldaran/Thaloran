package com.deldaran.politics;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

/** Lot K1 : les panneaux communs au menu d'accueil et au menu du jeu. */
final class Panneaux {
    private Panneaux() {
    }

    /** Les paramètres : affichage et volumes. Tout s'applique et s'enregistre tout de suite. retour : null s'il n'y a pas de bouton Retour. */
    static Table parametres(Runnable retour) {
        Theme t = Theme.get();
        Table p = t.panneau();
        p.defaults().left().padBottom(14f);
        p.add(t.label("Paramètres", t.grand, Theme.OR_VIF)).padBottom(22f).row();
        p.add(t.label("Affichage", t.bouton, Theme.OR)).padBottom(8f).row();
        p.add(t.options(new String[] { "Plein écran", "Fenêtre" }, Parametres.pleinEcran ? 0 : 1, i -> {
            Parametres.pleinEcran = i == 0;
            Parametres.enregistrer();
            Parametres.appliquerEcran();
        })).padBottom(22f).row();
        p.add(t.label("Son", t.bouton, Theme.OR)).padBottom(8f).row();
        p.add(t.curseur("Musique", Parametres.musique, v -> {
            Parametres.musique = v;
            Parametres.enregistrer();
        })).row();
        p.add(t.curseur("Bruitages", Parametres.sons, v -> {
            Parametres.sons = v;
            Parametres.enregistrer();
        })).padBottom(22f).row();
        p.add(t.label("En jeu", t.bouton, Theme.OR)).padBottom(8f).row();
        Label aide = t.paragraphe("Échap : le menu du jeu.\n² : afficher ou cacher la fenêtre de debug.");
        aide.setColor(Theme.ETEINT);
        p.add(aide).width(510f).row();
        if (retour != null) p.add(t.bouton("Retour", t.bouton, retour)).padTop(10f).padBottom(0f).row();
        return p;
    }
}
