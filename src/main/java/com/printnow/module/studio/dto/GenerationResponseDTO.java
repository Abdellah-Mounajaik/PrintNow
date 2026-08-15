package com.printnow.module.studio.dto;

import java.util.List;

/**
 * Ce que l'API renvoie après une génération : la génération et ses propositions.
 *
 * On n'expose PAS les chemins de fichiers (les PDF/PNG se récupèrent par des
 * endpoints dédiés, avec contrôle de propriété), seulement de quoi les afficher.
 */
public record GenerationResponseDTO(
        Long id,
        String type,
        String statut,
        String suivi,
        List<PropositionResponseDTO> propositions
) {
    public record PropositionResponseDTO(
            Long id,
            String gabaritCode,
            String paletteCode,
            boolean apercuDisponible,
            boolean pdfDisponible
    ) {}
}
