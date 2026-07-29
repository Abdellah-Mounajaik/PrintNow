package com.printnow.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ChangePasswordRequestDTO {

    @NotBlank(message = "L'ancien mot de passe est obligatoire.")
    private String ancienMotDePasse;

    @NotBlank(message = "Le nouveau mot de passe est obligatoire.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
             message = "Le nouveau mot de passe doit contenir au moins 8 caractères, avec des lettres et des chiffres.")
    private String nouveauMotDePasse;
}
