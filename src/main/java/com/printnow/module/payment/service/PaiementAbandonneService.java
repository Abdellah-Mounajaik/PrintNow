package com.printnow.module.payment.service;

import com.printnow.module.order.repository.CommandeRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Rend son argent au client lorsqu'un paiement n'a débouché sur aucune commande.
 *
 * Stripe encaisse avant que la commande ne soit créée. Quand le serveur refuse
 * la commande, le contrôleur rembourse aussitôt ; mais si l'appel n'arrive
 * jamais — réseau coupé, serveur redémarré, navigateur fermé —, personne n'est
 * là pour le faire et la somme reste encaissée sans contrepartie. C'est ce
 * trou-là que ce service comble, à la demande du navigateur qui vient de
 * constater l'échec.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaiementAbandonneService {

    private final CommandeRepository commandeRepository;
    private final RemboursementService remboursementService;

    /**
     * Rembourse le paiement s'il n'est rattaché à aucune commande.
     *
     * Refuse dans le cas contraire : la demande vient du navigateur, et rien
     * n'empêcherait sinon de réclamer le remboursement d'une commande bel et
     * bien passée — la sienne comme celle d'un autre.
     *
     * @throws ResponseStatusException 409 si une commande correspond à ce paiement
     */
    public void recuperer(String paymentIntentId, String emailDemandeur) {
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paiement non précisé.");
        }

        if (commandeRepository.existsByPaymentIntentId(paymentIntentId)) {
            log.warn("Remboursement refusé pour {} : une commande existe déjà pour ce paiement", paymentIntentId);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce paiement correspond à une commande enregistrée.");
        }

        // On interroge Stripe plutôt que de croire le navigateur : rembourser un
        // paiement qui n'a jamais abouti n'aurait aucun sens.
        PaymentIntent paiement;
        try {
            paiement = PaymentIntent.retrieve(paymentIntentId);
        } catch (StripeException e) {
            log.error("Paiement {} introuvable chez Stripe — remboursement impossible", paymentIntentId, e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paiement introuvable.");
        }

        if (!"succeeded".equals(paiement.getStatus())) {
            log.info("Paiement {} au statut {} : rien à rembourser", paymentIntentId, paiement.getStatus());
            return;
        }

        log.error("Paiement {} encaissé sans commande (client {}) — remboursement automatique",
                paymentIntentId, emailDemandeur);
        remboursementService.rembourser(paymentIntentId, "Commande non enregistrée après le paiement");
    }
}
