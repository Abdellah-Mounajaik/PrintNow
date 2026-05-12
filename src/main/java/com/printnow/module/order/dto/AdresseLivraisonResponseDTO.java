package com.printnow.module.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdresseLivraisonResponseDTO {
    private Long id;
    private String nomDestinataire;
    private String rue;
    private String numero;
    private String codePostal;
    private String ville;
    private String pays;
    private String telephone;
}