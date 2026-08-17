package com.printnow.module.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class CommandeResponseDTO {
private Long id;
    private String numeroCommande;
    private String statut;
    private String modeRetrait;
    private Boolean express2h;
    private BigDecimal totalTTC;

    /**
     * Vérification orthographique réglée avec la commande. Revenu de la
     * plateforme : compté à part du total, qui ne concerne que l'impression.
     */
    private BigDecimal montantCorrections;
    /** Designs IA (studio) réglés avec la commande. Revenu de la plateforme, compté à part du total. */
    private BigDecimal montantGenerations;
    private BigDecimal totalHT;
    private BigDecimal totalTVA;
    private LocalDateTime dateCreation;
    private String nomClient;
    private String nomImprimerie;
    private String numeroSuivi;
    private AdresseLivraisonResponseDTO adresseLivraison;
    private List<LigneCommandeResponseDTO> lignes;
}
