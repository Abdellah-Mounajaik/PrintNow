package com.printnow.module.shop.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartnerRegistrationRequest {
    
    // ==========================================
    // 1. Infos du compte Gérant
    // ==========================================
    private String email;
    private String password;
    
    // (Optionnel) SIRET directement rattaché au compte ou à l'inscription initiale
    private String siret; 

    // ==========================================
    // 2. Infos de la boutique
    // ==========================================
    private ImprimerieRequestDTO imprimerie;

    // ==========================================
    // 3. Listes des produits et horaires
    // ==========================================
    private List<ProduitRequestDTO> produits;
    private List<HoraireOuvertureRequestDTO> horaires;
}