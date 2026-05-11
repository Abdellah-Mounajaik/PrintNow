package com.printnow.module.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImprimerieRequestDTO {
    private Long idGerant;
    private String nom;
    private String description;
    private String logoUrl;
    private String emailContact;
    private String telephoneContact;
    private String adresse;
    private String ville;
    private String pays;
    private Double latitude;
    private Double longitude;
    private Boolean proposeExpress2h;
    private Integer pourcentageRemiseEtudiant;
    private Double prixExpress2h;
    private Boolean livraisonActive;
    private Boolean proposeTarifEtudiant;
    private String numeroTva;


}