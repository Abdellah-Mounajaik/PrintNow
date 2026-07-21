package com.printnow.module.shop.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartnerRegistrationRequest {

    // ==========================================
    // 1. Infos du compte Gérant
    // ==========================================
    @NotBlank(message = "L'email est obligatoire.")
    @Email(message = "L'email n'est pas valide.")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
             message = "Le mot de passe doit contenir au moins 8 caractères, avec des lettres et des chiffres.")
    private String password;

    @NotBlank(message = "Le numéro de TVA est obligatoire.")
    private String siret;

    // ==========================================
    // 2. Infos de la boutique
    // ==========================================
    @NotNull(message = "Les informations de l'imprimerie sont obligatoires.")
    @Valid
    private ImprimerieRequestDTO imprimerie;

    // ==========================================
    // 3. Listes des produits et horaires
    // ==========================================
    @NotEmpty(message = "Sélectionnez au moins un service proposé.")
    private List<ProduitRequestDTO> produits;

    @NotEmpty(message = "Les horaires d'ouverture sont obligatoires.")
    private List<HoraireOuvertureRequestDTO> horaires;
}
