package com.printnow.module.order.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdresseLivraisonRequestDTO {
    private String nomDestinataire;
    private String rue;
    private String numero;
    private String codePostal;
    private String ville;    private String pays = "Belgique"; 
    private String telephone;
}