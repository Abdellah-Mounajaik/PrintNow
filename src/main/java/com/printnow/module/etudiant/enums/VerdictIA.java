package com.printnow.module.etudiant.enums;

/**
 * Verdict de l'analyse automatique des deux documents déposés lors d'une
 * demande de vérification étudiante.
 */
public enum VerdictIA {
    /** Documents valides, noms concordants : accepté automatiquement. */
    CORRESPONDANCE,
    /** Mauvais type de document, ou noms manifestement différents : refusé automatiquement. */
    DIVERGENCE_MANIFESTE,
    /** Doute (image floue, lecture partielle, etc.) : reste à la charge de l'admin. */
    INCERTAIN
}
