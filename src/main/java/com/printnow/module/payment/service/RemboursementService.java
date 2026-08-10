package com.printnow.module.payment.service;

import com.stripe.model.Refund;
import com.stripe.param.RefundCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Rembourse un paiement lorsque la commande n'a finalement pas pu être créée.
 *
 * Stripe encaisse avant que le serveur ne recalcule le total : si celui-ci
 * refuse ensuite la commande — montant insuffisant, option devenue
 * indisponible —, le client se retrouverait débité sans rien recevoir. Le
 * remboursement rétablit la situation et évite qu'une somme encaissée ne
 * corresponde à aucune commande.
 */
@Service
@Slf4j
public class RemboursementService {

    /**
     * Rembourse intégralement un paiement.
     *
     * L'échec du remboursement n'est jamais propagé : il ne doit pas masquer la
     * raison pour laquelle la commande a été refusée, qui seule intéresse le
     * client. Il est en revanche journalisé en erreur, car il demande alors une
     * intervention manuelle.
     */
    public void rembourser(String paymentIntentId, String motif) {
        if (paymentIntentId == null || paymentIntentId.isBlank()) return;

        try {
            Refund remboursement = Refund.create(RefundCreateParams.builder()
                    .setPaymentIntent(paymentIntentId)
                    .build());
            log.info("Paiement {} remboursé ({} centimes) : {}",
                    paymentIntentId, remboursement.getAmount(), motif);

        } catch (Exception e) {
            log.error("Remboursement impossible du paiement {} — à traiter manuellement. Motif du refus : {}",
                    paymentIntentId, motif, e);
        }
    }
}
