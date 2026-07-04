package com.printnow.module.order.dto;

import java.util.List;

import lombok.Data;

@Data
public class CommandeRequestDTO {
    private String modeRetrait;
    private Boolean express2h;
    private String codePromo;
    private List<LigneCommandeRequestDTO> lignes;
    private AdresseLivraisonRequestDTO adresseLivraison;
}
