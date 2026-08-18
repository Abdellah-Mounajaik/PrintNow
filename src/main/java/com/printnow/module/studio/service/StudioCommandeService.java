package com.printnow.module.studio.service;

import com.printnow.module.parametres.service.ParametresService;
import com.printnow.module.studio.model.PropositionSupport;
import com.printnow.module.studio.repository.PropositionSupportRepository;
import com.printnow.module.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Facture les designs générés par l'IA (studio) utilisés dans une commande.
 *
 * Générer et prévisualiser les 3 propositions reste gratuit ; seule l'utilisation
 * d'un design dans une commande payée est facturée, au forfait. Comme les
 * corrections orthographiques, ce montant revient intégralement à PrintNow : il
 * s'ajoute au paiement du client mais n'entre ni dans le total de la commande,
 * ni dans la part reversée à l'imprimerie, ni dans l'assiette de la commission.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudioCommandeService {

    private final PropositionSupportRepository propositionRepository;
    private final ParametresService parametresService;

    /**
     * @param propositionIds propositions du studio utilisées dans la commande
     * @param client         propriétaire attendu des générations
     * @param totalPrecedent total déjà dû (impression + éventuelles corrections)
     * @param montantRegle   montant réellement encaissé par Stripe, en centimes
     * @return le montant facturé au titre des générations, à comptabiliser comme
     *         revenu de la plateforme
     */
    @Transactional
    public BigDecimal appliquerGenerations(List<Long> propositionIds, User client,
                                           BigDecimal totalPrecedent, Long montantRegle) {
        if (propositionIds == null || propositionIds.isEmpty()) return BigDecimal.ZERO;

        BigDecimal prixGeneration = parametresService.getParametres().getPrixGenerationDesign();
        List<PropositionSupport> aFacturer = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Long id : propositionIds) {
            if (id == null) continue;
            PropositionSupport proposition = propositionRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Génération introuvable."));

            if (proposition.getGeneration().getClient() == null
                    || !proposition.getGeneration().getClient().getId().equals(client.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Génération non autorisée.");
            }
            if (proposition.isPayee()) continue; // déjà facturée, on ne la compte pas deux fois

            aFacturer.add(proposition);
            total = total.add(prixGeneration);
        }

        if (aFacturer.isEmpty()) return BigDecimal.ZERO;

        // Contrôle cumulé : le paiement doit couvrir l'impression, les corrections
        // ET les générations. Sans lui, une petite commande pourrait débloquer
        // plusieurs designs payants.
        BigDecimal attendu = totalPrecedent.add(total);
        long attenduCentimes = attendu.multiply(BigDecimal.valueOf(100)).longValue();
        if (montantRegle == null || montantRegle < attenduCentimes) {
            log.warn("Paiement insuffisant : {} centimes réglés pour {} attendus (précédent {} + générations {})",
                    montantRegle, attenduCentimes, totalPrecedent, total);
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                    "Le montant réglé ne couvre pas les designs générés.");
        }

        for (PropositionSupport proposition : aFacturer) {
            proposition.setPayee(true);
            proposition.setChoisie(true);
            propositionRepository.save(proposition);
        }
        return total;
    }
}
