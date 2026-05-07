package com.printnow.module.payment.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.printnow.module.shop.service.PartnerRegistrationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class WebhookController {

    @Value("${stripe.webhook.secret}") // On verra comment l'obtenir juste après
    private String endpointSecret;

    private final PartnerRegistrationService partnerService;

    public WebhookController(PartnerRegistrationService partnerService) {
        this.partnerService = partnerService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload, 
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            // Stripe vérifie que ce message vient bien de chez eux et pas d'un hacker
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Signature invalide");
        }

        // Si le paiement est un succès total
        if ("payment_intent.succeeded".equals(event.getType())) {
            PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer().getObject().get();
            
            // On récupère l'ID qu'on avait caché dans les metadata
            String imprimerieIdStr = intent.getMetadata().get("imprimerie_id");
            
            if (imprimerieIdStr != null) {
                // MAGIE : On active le compte dans la BDD !
                partnerService.activatePartnerAccount(Long.parseLong(imprimerieIdStr));
                System.out.println("✅ Imprimerie " + imprimerieIdStr + " activée avec succès !");
            }
        }

        return ResponseEntity.ok("Webhook reçu"); // Toujours dire "Merci" à Stripe (Status 200)
    }
}
