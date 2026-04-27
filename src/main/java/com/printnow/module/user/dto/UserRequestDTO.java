package com.printnow.module.user.dto;

import lombok.Data;

@Data
public class UserRequestDTO {
    private String email;
    private String motDePasse;
    private String prenom;
    private String nom;
    private String telephone;
    private Long roleId; // On passe l'ID du rôle pour simplifier la création
}