package com.printnow.module.user.dto;

import lombok.Data;

@Data
public class UserResponseDTO {
    private Long id;
    private String email;
    private String prenom;
    private String nom;
    private String telephone;
    private Boolean actif;
    private String roleNom; 
}