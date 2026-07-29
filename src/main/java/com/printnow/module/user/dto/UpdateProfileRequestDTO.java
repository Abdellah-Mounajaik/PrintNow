package com.printnow.module.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Modification du profil par l'utilisateur lui-même : volontairement séparé
 * du mot de passe (voir ChangePasswordRequestDTO) pour ne jamais risquer de
 * l'écraser accidentellement.
 */
@Data
public class UpdateProfileRequestDTO {

    @NotBlank(message = "Le prénom est obligatoire.")
    @Size(max = 50, message = "Le prénom ne peut pas dépasser 50 caractères.")
    private String prenom;

    @NotBlank(message = "Le nom est obligatoire.")
    @Size(max = 50, message = "Le nom ne peut pas dépasser 50 caractères.")
    private String nom;

    @NotBlank(message = "L'email est obligatoire.")
    @Email(message = "L'email n'est pas valide.")
    private String email;

    @Pattern(regexp = "^$|^\\+?[0-9 ()./-]{8,20}$",
             message = "Le numéro de téléphone n'est pas valide.")
    private String telephone;
}
