package com.printnow.module.user.dto;

import lombok.Data;

@Data
public class SignupRequestDTO {
    private String prenom;
    private String nom;
    private String email;
    private String motDePasse;
    private String telephone;
}