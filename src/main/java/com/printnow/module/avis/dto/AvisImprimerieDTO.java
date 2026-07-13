package com.printnow.module.avis.dto;

import lombok.Data;

import java.util.List;

/**
 * Vue complète des avis d'une imprimerie pour la page détail.
 * peutNoter / dejaNote dépendent de l'utilisateur connecté (false si anonyme).
 */
@Data
public class AvisImprimerieDTO {
    private Double noteMoyenne;
    private long nombreAvis;
    private boolean peutNoter;   // le client a une commande livrée et n'a pas encore noté
    private boolean dejaNote;    // le client a déjà laissé un avis
    private List<AvisResponseDTO> avis;
}
