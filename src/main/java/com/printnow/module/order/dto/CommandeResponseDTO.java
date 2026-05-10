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
    private BigDecimal totalHT;
    private BigDecimal totalTVA;
    private LocalDateTime dateCreation;
    private String nomClient;
    private String nomImprimerie;
    private List<LigneCommandeResponseDTO> lignes;
}
