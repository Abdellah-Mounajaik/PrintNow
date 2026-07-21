package com.printnow.module.shop.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImprimerieRequestDTO {
    private Long idGerant;

    @NotBlank(message = "Le nom de l'imprimerie est obligatoire.")
    @Size(max = 150, message = "Le nom ne peut pas dépasser 150 caractères.")
    private String nom;

    @NotBlank(message = "La description de l'imprimerie est obligatoire.")
    @Size(max = 2000, message = "La description ne peut pas dépasser 2000 caractères.")
    private String description;

    @NotBlank(message = "Le logo de l'imprimerie est obligatoire.")
    private String logoUrl;

    @Email(message = "L'email de contact n'est pas valide.")
    private String emailContact;

    @Pattern(regexp = "^$|^\\+?[0-9 ()./-]{8,20}$",
             message = "Le numéro de téléphone n'est pas valide.")
    private String telephoneContact;

    @NotBlank(message = "L'adresse est obligatoire.")
    private String adresse;
    private String ville;
    private String pays;
    private Double latitude;
    private Double longitude;
    private Boolean proposeExpress2h;
    private Integer pourcentageRemiseEtudiant;
    private Double prixExpress2h;
    private Boolean livraisonActive;
    private Double prixLivraison;
    private Boolean proposeTarifEtudiant;
    private String numeroTva;


}