package com.printnow.module.shop.dto;

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
}
