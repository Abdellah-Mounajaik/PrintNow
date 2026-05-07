package com.printnow.module.shop.dto;

import lombok.Data;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartnerRegistrationRequest {
    
    // ==========================================
    // 1. Infos du compte Gérant
    // ==========================================
    private String email;
    private String password;
    
    // (Optionnel) Tu peux mettre le SIRET ici si tu ne veux pas modifier 
    // ton entité Imprimerie existante, ou le mettre directement dans ImprimerieRequestDTO.
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