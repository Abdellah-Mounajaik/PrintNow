package com.printnow.module.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserResponseDTO {
    private Long id;
    private String email;
    private String prenom;
    private String nom;
    private String telephone;
    private Boolean actif;
    private String roleNom;

    /**
     * Renseignée si le compte a été supprimé. L'administration en a besoin :
     * les commandes d'un compte supprimé s'affichent au nom de « Compte
     * supprimé », et il faut pouvoir remonter à la ligne correspondante.
     */
    private LocalDateTime dateSuppression;
}