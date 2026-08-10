package com.printnow.module.correction.service;

import com.printnow.module.correction.dto.DemandeCorrectionDTO;
import com.printnow.module.correction.model.VerificationOrthographe;
import com.printnow.module.correction.repository.VerificationOrthographeRepository;
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
 * Applique les corrections orthographiques réglées en même temps qu'une commande.
 *
 * Le montant de ces corrections revient intégralement à PrintNow : il s'ajoute au
 * paiement du client mais n'entre ni dans le total de la commande, ni dans la
 * part reversée à l'imprimerie, ni dans l'assiette de la commission.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CorrectionCommandeService {

    private final VerificationOrthographeRepository repository;
    private final CorrectionService correctionService;

    /**
     * @param demandes     corrections demandées par le client
     * @param client       propriétaire attendu des vérifications
     * @param totalCommande montant de l'impression, calculé côté serveur
     * @param montantRegle  montant réellement encaissé par Stripe, en centimes
     * @return le montant facturé au titre des corrections, à comptabiliser comme
     *         revenu de la plateforme
     */
    @Transactional
    public BigDecimal appliquerCorrections(List<DemandeCorrectionDTO> demandes,
                                           User client,
                                           BigDecimal totalCommande,
                                           Long montantRegle) {
        if (demandes == null || demandes.isEmpty()) return BigDecimal.ZERO;

        // On conserve la demande à côté de sa vérification : la liste est filtrée
        // (identifiants absents, corrections déjà appliquées), les index d'origine
        // ne peuvent donc pas servir de correspondance.
        record ACorriger(VerificationOrthographe verification,
                         List<Integer> fautesIgnorees,
                         java.util.Map<Integer, String> remplacementsChoisis) {}

        List<ACorriger> aTraiter = new ArrayList<>();
        BigDecimal totalCorrections = BigDecimal.ZERO;

        for (DemandeCorrectionDTO demande : demandes) {
            if (demande.getVerificationId() == null) continue;

            VerificationOrthographe verification = repository.findById(demande.getVerificationId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Vérification orthographique introuvable."));

            if (!verification.getClient().getId().equals(client.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vérification non autorisée.");
            }
            if (Boolean.TRUE.equals(verification.getPayee())) {
                continue; // déjà appliquée, on ne la facture pas deux fois
            }

            aTraiter.add(new ACorriger(verification, demande.getFautesIgnorees(), demande.getRemplacementsChoisis()));
            totalCorrections = totalCorrections.add(verification.getPrix());
        }

        if (aTraiter.isEmpty()) return BigDecimal.ZERO;

        // Contrôle cumulé : sans lui, un client pourrait régler une petite commande
        // et débloquer plusieurs corrections coûteuses, chacune passant isolément.
        BigDecimal attendu = totalCommande.add(totalCorrections);
        long attenduCentimes = attendu.multiply(BigDecimal.valueOf(100)).longValue();
        if (montantRegle == null || montantRegle < attenduCentimes) {
            log.warn("Paiement insuffisant : {} centimes réglés pour {} attendus (commande {} + corrections {})",
                    montantRegle, attenduCentimes, totalCommande, totalCorrections);
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                    "Le montant réglé ne couvre pas les corrections demandées.");
        }

        for (ACorriger element : aTraiter) {
            correctionService.appliquer(element.verification(), element.fautesIgnorees(), element.remplacementsChoisis());
        }
        return totalCorrections;
    }
}
