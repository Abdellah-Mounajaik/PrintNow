package com.printnow.module.order.dto;

import java.util.List;

import com.printnow.module.correction.dto.DemandeCorrectionDTO;
import lombok.Data;

@Data
public class CommandeRequestDTO {
    private String modeRetrait;
    private Boolean express2h;
    private Boolean tarifEtudiant;
    private String codePromo;
    private List<LigneCommandeRequestDTO> lignes;
    private AdresseLivraisonRequestDTO adresseLivraison;
    /** Identifiant du PaymentIntent Stripe déjà confirmé côté navigateur. */
    private String paymentIntentId;
    /**
     * Corrections orthographiques à appliquer, réglées avec la commande.
     * Ce service est facturé par PrintNow : son montant n'entre ni dans le
     * total de la commande, ni dans la part reversée à l'imprimerie.
     */
    private List<DemandeCorrectionDTO> corrections;
}
