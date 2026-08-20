package com.printnow.module.payment.service;

import com.printnow.module.order.repository.CommandeRepository;
import com.printnow.module.shop.repository.ImprimerieRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Rend son argent au client lorsqu'un paiement n'a débouché sur rien —
 * ni commande, ni inscription partenaire selon le cas.
 *
 * Stripe encaisse avant que la commande/l'imprimerie ne soit créée. Quand le
 * serveur refuse la création, le contrôleur rembourse aussitôt ; mais si
 * l'appel n'arrive jamais — réseau coupé, serveur redémarré, navigateur
 * fermé —, personne n'est là pour le faire et la somme reste encaissée sans
 * contrepartie. C'est ce trou-là que ce service comble : à la demande du
 * navigateur qui vient de constater l'échec ({@link #recuperer}), ou de façon
 * fiable via le webhook Stripe ({@link #recupererApresDelai}).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaiementAbandonneService {

    private final CommandeRepository commandeRepository;
    private final ImprimerieRepository imprimerieRepository;
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
        verifierEtRembourser(
                paymentIntentId,
                paymentIntentId != null && !paymentIntentId.isBlank() && commandeRepository.existsByPaymentIntentId(paymentIntentId),
                "Ce paiement correspond à une commande enregistrée.",
                "Commande non enregistrée après le paiement (demandeur : " + emailDemandeur + ")");
    }

    /**
     * Même principe que {@link #recuperer}, mais pour les frais d'inscription
     * d'une imprimerie partenaire plutôt qu'une commande.
     *
     * @throws ResponseStatusException 409 si une imprimerie correspond à ce paiement
     */
    public void recupererInscription(String paymentIntentId) {
        verifierEtRembourser(
                paymentIntentId,
                paymentIntentId != null && !paymentIntentId.isBlank() && imprimerieRepository.existsByPaymentIntentId(paymentIntentId),
                "Ce paiement correspond à une inscription enregistrée.",
                "Inscription partenaire non enregistrée après le paiement");
    }

    private void verifierEtRembourser(String paymentIntentId, boolean dejaRattache,
                                       String messageSiDejaRattache, String motifRemboursement) {
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paiement non précisé.");
        }

        if (dejaRattache) {
            log.warn("Remboursement refusé pour {} : {}", paymentIntentId, messageSiDejaRattache);
            throw new ResponseStatusException(HttpStatus.CONFLICT, messageSiDejaRattache);
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

        log.error("Paiement {} encaissé sans contrepartie — remboursement automatique ({})",
                paymentIntentId, motifRemboursement);
        remboursementService.rembourser(paymentIntentId, motifRemboursement);
    }

    /**
     * Même vérification que {@link #recuperer}, mais déclenchée par le webhook
     * Stripe plutôt que par le navigateur — donc jamais dépendante de lui.
     *
     * Un délai précède la vérification : le webhook peut arriver avant même que
     * la confirmation du navigateur n'ait atteint notre serveur, et vérifier
     * trop tôt rembourserait à tort des commandes parfaitement valides, tout
     * juste en train de se créer.
     */
    @Async
    public void recupererApresDelai(String paymentIntentId) {
        if (!attendreLeDelaiDeGrace()) return;
        try {
            recuperer(paymentIntentId, "webhook-stripe");
        } catch (ResponseStatusException e) {
            // 409 : une commande existe bien — c'est le cas normal, rien à faire.
            // 400 : déjà journalisé par recuperer() elle-même.
        }
    }

    /** Pendant utile de {@link #recupererApresDelai} pour les inscriptions partenaires. */
    @Async
    public void recupererInscriptionApresDelai(String paymentIntentId) {
        if (!attendreLeDelaiDeGrace()) return;
        try {
            recupererInscription(paymentIntentId);
        } catch (ResponseStatusException e) {
            // 409 : une imprimerie existe bien — c'est le cas normal, rien à faire.
            // 400 : déjà journalisé par recupererInscription() elle-même.
        }
    }

    /** @return false si l'attente a été interrompue (arrêt du serveur, par ex.) */
    private boolean attendreLeDelaiDeGrace() {
        try {
            Thread.sleep(90_000);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
