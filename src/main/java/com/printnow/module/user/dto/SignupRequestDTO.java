package com.printnow.module.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignupRequestDTO {

    @NotBlank(message = "Le prénom est obligatoire.")
    @Size(max = 50, message = "Le prénom ne peut pas dépasser 50 caractères.")
    private String prenom;

    @NotBlank(message = "Le nom est obligatoire.")
    @Size(max = 50, message = "Le nom ne peut pas dépasser 50 caractères.")
    private String nom;

    @NotBlank(message = "L'email est obligatoire.")
    @Email(message = "L'email n'est pas valide.")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
             message = "Le mot de passe doit contenir au moins 8 caractères, avec des lettres et des chiffres.")
    private String motDePasse;

    // Optionnel : vide accepté, sinon format téléphone
    @Pattern(regexp = "^$|^\\+?[0-9 ()./-]{8,20}$",
             message = "Le numéro de téléphone n'est pas valide.")
    private String telephone;
}
