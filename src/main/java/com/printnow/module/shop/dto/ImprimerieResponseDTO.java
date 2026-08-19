package com.printnow.module.shop.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class ImprimerieResponseDTO {
    private Long id;
    private String nom;
    /** Adresse lisible de la fiche, dérivée du nom : « imprimerie-du-centre ». */
    private String slug;
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
    /** Présent seulement si les frais d'inscription ont bien été facturés (voir Imprimerie). */
    private String numeroFactureInscription;
    /** Montant réellement payé à l'inscription, figé (voir Imprimerie.montantFraisInscription). */
    private BigDecimal montantFraisInscription;
}
