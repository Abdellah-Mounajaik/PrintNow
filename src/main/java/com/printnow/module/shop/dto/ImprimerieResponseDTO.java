package com.printnow.module.shop.dto;

import java.util.List;

import lombok.Data;

@Data
public class ImprimerieResponseDTO {
    private Long id;
    private String nom;
    private String description;
    private String logoUrl;
    private String adresse;
    private String ville;
    private Boolean proposeExpress2h;
    private Boolean livraisonActive;
    private Boolean actif;
    private List<HoraireOuvertureResponseDTO> horaires;
    private List<ProduitResponseDTO> produits;
    private Boolean proposeTarifEtudiant;
    private String numeroTva;
    private String emailContact;
    private String telephoneContact;
}
