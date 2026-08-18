package com.printnow.module.parametres.service;

import com.printnow.module.parametres.dto.ParametresPlateformeDTO;
import com.printnow.module.parametres.model.ParametresPlateforme;
import com.printnow.module.parametres.repository.ParametresPlateformeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

/**
 * Tarifs de la plateforme (commission, frais d'inscription, correction
 * orthographique, génération de design), modifiables par un administrateur.
 *
 * Gardés en mémoire après la première lecture : ces valeurs sont consultées à
 * chaque commande et ne changent qu'occasionnellement, un aller-retour en base
 * à chaque appel serait superflu. Le cache est rafraîchi à chaque modification.
 */
@Service
@RequiredArgsConstructor
public class ParametresService {

    private final ParametresPlateformeRepository repository;

    private volatile ParametresPlateforme cache;

    /** Renvoie les tarifs actuels, en créant la ligne par défaut si c'est la toute première consultation. */
    @Transactional
    public ParametresPlateforme getParametres() {
        if (cache == null) {
            synchronized (this) {
                if (cache == null) {
                    cache = repository.findById(1L)
                            .orElseGet(() -> repository.save(ParametresPlateforme.valeursParDefaut()));
                }
            }
        }
        return cache;
    }

    /**
     * Met à jour les tarifs. Toutes les valeurs doivent être strictement positives
     * (une commission ou un prix nul ou négatif n'a pas de sens métier), et la
     * commission ne peut pas dépasser 100%.
     */
    @Transactional
    public synchronized ParametresPlateforme mettreAJour(ParametresPlateformeDTO dto) {
        validerPositif(dto.getCommissionPourcentage(), "La commission");
        if (dto.getCommissionPourcentage().compareTo(new BigDecimal("100")) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La commission ne peut pas dépasser 100%.");
        }
        validerPositif(dto.getFraisInscription(), "Les frais d'inscription");
        validerPositif(dto.getPrixCorrectionForfait(), "Le prix de la correction");
        validerPositif(dto.getPrixCorrectionPageSupp(), "Le prix par page supplémentaire");
        validerPositif(dto.getPrixGenerationDesign(), "Le prix de la génération de design");
        if (dto.getPagesInclusesCorrection() == null || dto.getPagesInclusesCorrection() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nombre de pages incluses doit être positif.");
        }

        ParametresPlateforme p = getParametres();
        p.setCommissionPourcentage(dto.getCommissionPourcentage());
        p.setFraisInscription(dto.getFraisInscription());
        p.setPrixCorrectionForfait(dto.getPrixCorrectionForfait());
        p.setPagesInclusesCorrection(dto.getPagesInclusesCorrection());
        p.setPrixCorrectionPageSupp(dto.getPrixCorrectionPageSupp());
        p.setPrixGenerationDesign(dto.getPrixGenerationDesign());

        cache = repository.save(p);
        return cache;
    }

    private void validerPositif(BigDecimal valeur, String champ) {
        if (valeur == null || valeur.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, champ + " doit être positif.");
        }
    }
}
