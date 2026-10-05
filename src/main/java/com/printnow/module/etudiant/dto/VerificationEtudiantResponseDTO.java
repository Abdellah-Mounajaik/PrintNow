package com.printnow.module.etudiant.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VerificationEtudiantResponseDTO {
    private Long id;
    private Long userId;
    private String nomUtilisateur;
    private String emailUtilisateur;
    private String statut;
    private LocalDateTime dateSoumission;
    private LocalDateTime dateValidation;
    private LocalDateTime valableJusquA;
    private boolean carteEtudiantePresente;
    private boolean carteIdentitePresente;
    private String motifRefus;
    private String verdictIa;
    private String nomExtraitCarteEtudiante;
    private String nomExtraitCarteIdentite;
    private boolean decisionAutomatique;

    /**
     * Faux après trois refus : seul le support peut encore trancher.
     *
     * Calculé par le serveur plutôt que déduit d'un compteur côté navigateur,
     * pour que la règle ne soit écrite qu'à un seul endroit.
     */
    private boolean peutResoumettre;
}
