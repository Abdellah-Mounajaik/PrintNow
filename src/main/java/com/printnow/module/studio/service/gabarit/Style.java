package com.printnow.module.studio.service.gabarit;

import static com.printnow.module.studio.service.gabarit.OutilsGabarit.contraster;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.foncer;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.melerBlanc;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.melerNoir;
import static com.printnow.module.studio.service.gabarit.OutilsGabarit.sombre;

/**
 * Le style résolu d'un rendu : une police, trois couleurs (principale, accent,
 * texte) et un {@code fond}. Quand {@code impose} est vrai, le fond vient d'une
 * demande explicite du client (« fond noir ») et s'applique à toute la maquette ;
 * sinon la page est blanche et les aplats colorés dérivent de la principale.
 *
 * Les maquettes ne lisent pas les couleurs brutes mais des « jetons » calculés
 * ici, toujours contrastés par rapport à la surface qui les porte — d'où une
 * lisibilité garantie que le fond soit clair ou sombre.
 */
public record Style(String code, int[] fond, boolean impose, int[] primaire, int[] accent, int[] texte,
                    Police police) {

    private static final int[] BLANC = {255, 255, 255};
    private static final int[] ENCRE_SOMBRE = {38, 40, 46};
    private static final int[] ENCRE_CLAIR = {236, 239, 245};
    private static final int[] DOUX_SOMBRE = {104, 108, 118};
    private static final int[] DOUX_CLAIR = {190, 195, 206};

    // --- surfaces --------------------------------------------------------------

    /** Fond des maquettes claires (blanc, ou le fond imposé). */
    public int[] page() {
        return impose ? fond : BLANC;
    }

    /** Aplat plein coloré (maquettes « pleine couleur »). */
    public int[] bloc() {
        return impose ? fond : foncer(primaire, 76);
    }

    /** Aplat posé sur la page (colonne latérale, bandeau, bande) : distinct du fond. */
    public int[] panneau() {
        if (!impose) return foncer(primaire, 74);
        return sombre(fond) ? melerBlanc(fond, 0.11) : melerNoir(fond, 0.10);
    }

    /** Pastille de monogramme. */
    public int[] pastille() {
        return sombre(page()) ? melerBlanc(page(), 0.16) : foncer(primaire, 102);
    }

    /** Fond d'une puce/chip posée sur la page. */
    public int[] chip() {
        return sombre(page()) ? melerBlanc(page(), 0.14) : melerBlanc(primaire, 0.86);
    }

    /** Filet séparateur discret. */
    public int[] filet() {
        return sombre(page()) ? new int[]{74, 78, 88} : new int[]{222, 225, 231};
    }

    // --- texte / accents, contrastés selon la surface --------------------------

    public int[] encre() { return texteSur(page()); }
    public int[] encreDoux() { return douxSur(page()); }
    public int[] texteDoux() { return contraster(texte, page(), 80); }
    public int[] titre() { return contraster(primaire, page(), 105); }
    public int[] accentPage() { return contraster(accent, page(), 95); }

    public int[] surBloc() { return texteSur(bloc()); }
    public int[] surBlocDoux() { return douxSur(bloc()); }
    public int[] accentBloc() { return contraster(accent, bloc(), 95); }
    public int[] chipSurBloc() { return melerBlanc(bloc(), 0.15); }

    public int[] surPanneau() { return texteSur(panneau()); }
    public int[] surPanneauDoux() { return douxSur(panneau()); }
    public int[] accentPanneau() { return contraster(accent, panneau(), 95); }

    public int[] surPastille() { return texteSur(pastille()); }

    public int[] chipTexte() {
        return sombre(chip()) ? ENCRE_CLAIR : contraster(primaire, chip(), 85);
    }

    private int[] texteSur(int[] surface) {
        return sombre(surface) ? ENCRE_CLAIR : ENCRE_SOMBRE;
    }

    private int[] douxSur(int[] surface) {
        return sombre(surface) ? DOUX_CLAIR : DOUX_SOMBRE;
    }
}
