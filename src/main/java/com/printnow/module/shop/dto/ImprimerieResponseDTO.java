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
    private Double latitude;
    private Double longitude;
    private Boolean proposeExpress2h;
    private Double prixExpress2h;
    private Boolean livraisonActive;
    private Double prixLivraison;
    private Boolean actif;
    private List<HoraireOuvertureResponseDTO> horaires;
    private List<ProduitResponseDTO> produits;
    private Boolean proposeTarifEtudiant;
    private Integer pourcentageRemiseEtudiant;
    private Integer pourcentageRemiseRectoVerso;
    private String numeroTva;
    private String emailContact;
    private String telephoneContact;
    private Double noteMoyenne;
    private Integer nombreAvis;
}
