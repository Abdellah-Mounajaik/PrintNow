package com.printnow.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ReinitialisationRequestDTO {

    @NotBlank(message = "Le lien de réinitialisation est incomplet.")
    private String jeton;

    // Mêmes exigences qu'à l'inscription : un lien de réinitialisation ne doit
    // pas servir à contourner la règle.
    @NotBlank(message = "Le mot de passe est obligatoire.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
             message = "Le mot de passe doit contenir au moins 8 caractères, avec des lettres et des chiffres.")
    private String nouveauMotDePasse;
}
