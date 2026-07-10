package com.printnow.module.order.service;

import com.printnow.module.order.dto.LivraisonSuiviDTO;
import com.printnow.module.order.enums.StatutLivraison;
import com.printnow.module.order.model.Livraison;
import com.printnow.module.order.repository.LivraisonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class LivraisonService {

    private final LivraisonRepository livraisonRepository;
    private final AfterShippingService afterShippingService;

    // Correspondance entre les tags AfterShipping et notre enum interne
    private static final Map<String, StatutLivraison> TAG_TO_STATUT = Map.of(
            "Pending",          StatutLivraison.EXPEDIE,
            "InfoReceived",     StatutLivraison.EXPEDIE,
            "InTransit",        StatutLivraison.EN_COURS,
            "OutForDelivery",   StatutLivraison.EN_COURS,
            "AttemptFail",      StatutLivraison.ECHEC,
            "Delivered",        StatutLivraison.LIVRE,
            "Exception",        StatutLivraison.ECHEC
    );

    /**
     * Appelé par l'imprimeur quand il dépose le colis chez bpost et reçoit un numéro de suivi.
     * 1. Sauvegarde le numéro en base
     * 2. Enregistre le suivi auprès d'AfterShipping
     */
    @Transactional
    public LivraisonSuiviDTO ajouterNumeroSuivi(Long commandeId, String numeroSuivi) {
        Livraison livraison = livraisonRepository.findByCommandeId(commandeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Livraison introuvable pour la commande " + commandeId));

        if (livraisonRepository.existsByNumeroSuivi(numeroSuivi)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce numéro de suivi est déjà utilisé.");
        }

        livraison.setNumeroSuivi(numeroSuivi);
        livraison.setStatutLivraison(StatutLivraison.EXPEDIE);
        livraisonRepository.save(livraison);

        // On enregistre le suivi chez AfterShipping (erreur ignorée si clé non configurée)
        try {
            afterShippingService.creerSuivi(numeroSuivi);
        } catch (Exception ignored) {}

        return toDto(livraison, null);
    }

    /**
     * Appelé par le client pour voir où en est son colis.
     * Interroge AfterShipping en temps réel et met à jour la base si le statut a changé.
     */
    @Transactional
    public LivraisonSuiviDTO getStatutLivraison(Long commandeId) {
        Livraison livraison = livraisonRepository.findByCommandeId(commandeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Livraison introuvable pour la commande " + commandeId));

        String tagAfterShipping = null;

        if (livraison.getNumeroSuivi() != null) {
            try {
                tagAfterShipping = afterShippingService.getTagStatut(livraison.getNumeroSuivi());
                if (tagAfterShipping != null) {
                    StatutLivraison nouveauStatut = TAG_TO_STATUT.getOrDefault(
                            tagAfterShipping, livraison.getStatutLivraison());
                    if (nouveauStatut != livraison.getStatutLivraison()) {
                        livraison.setStatutLivraison(nouveauStatut);
                        livraisonRepository.save(livraison);
                    }
                }
            } catch (Exception ignored) {}
        }

        return toDto(livraison, tagAfterShipping);
    }

    private LivraisonSuiviDTO toDto(Livraison livraison, String tagAfterShipping) {
        String lienBpost = livraison.getNumeroSuivi() != null
                ? "https://track.bpost.cloud/btr/web/#/search?itemCode=" + livraison.getNumeroSuivi()
                : null;

        return LivraisonSuiviDTO.builder()
                .commandeId(livraison.getCommande() != null ? livraison.getCommande().getId() : null)
                .numeroSuivi(livraison.getNumeroSuivi())
                .statut(livraison.getStatutLivraison().name())
                .statutAfterShipping(tagAfterShipping)
                .lienSuiviBpost(lienBpost)
                .build();
    }
}
