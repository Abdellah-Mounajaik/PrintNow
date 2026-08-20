package com.printnow.module.payment.controller;

import com.printnow.module.payment.service.PaiementAbandonneService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reçoit directement de Stripe la confirmation qu'un paiement a réussi —
 * contrairement à /api/payments/abandon, ne dépend jamais du navigateur du
 * client : Stripe appelle ce point d'entrée serveur à serveur, donc même si
 * l'appareil du client meurt juste après le paiement (onglet fermé, réseau
 * coupé), ce filet de sécurité fonctionne quand même.
 *
 * Couvre les deux paiements de la plateforme, distingués par la metadata
 * "type" posée à la création du PaymentIntent : "commande" (achat client) et
 * "inscription" (frais d'inscription d'une imprimerie partenaire).
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    private final PaiementAbandonneService paiementAbandonneService;

    @PostMapping("/webhook")
    public ResponseEntity<String> recevoirWebhookStripe(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {

        Event event;
        try {
            event = Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Signature de webhook Stripe invalide — requête rejetée", e);
            return ResponseEntity.badRequest().build();
        }

        if ("payment_intent.succeeded".equals(event.getType())) {
            // deserializeUnsafe() plutôt que getObject() : ce dernier échoue
            // silencieusement dès que la version d'API du compte Stripe diffère
            // de celle figée dans le SDK Java (ex. compte plus récent que la
            // librairie) — cas pourtant courant et sans rapport avec un vrai souci.
            Object obj;
            try {
                obj = event.getDataObjectDeserializer().deserializeUnsafe();
            } catch (Exception e) {
                log.warn("Webhook Stripe {} reçu mais contenu illisible", event.getId(), e);
                return ResponseEntity.ok("");
            }
            if (obj instanceof PaymentIntent intent) {
                String type = intent.getMetadata().get("type");
                if ("commande".equals(type)) {
                    paiementAbandonneService.recupererApresDelai(intent.getId());
                } else if ("inscription".equals(type)) {
                    paiementAbandonneService.recupererInscriptionApresDelai(intent.getId());
                }
            }
        }

        return ResponseEntity.ok("");
    }
}
