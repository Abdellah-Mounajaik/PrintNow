package com.printnow.module.payment.controller;

import com.printnow.module.payment.service.PaiementAbandonneService;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaiementAbandonneService paiementAbandonneService;

    // On récupère ta clé Stripe depuis le fichier application.properties
    @Value("${stripe.api.key}")
    private String stripeApiKey;

    // Cette méthode initialise Stripe au démarrage du serveur
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    @PostMapping("/create-payment-intent")
    public Map<String, String> createPaymentIntent(@RequestBody Map<String, Object> requestData) throws Exception {
        // Montant en centimes envoyé par le frontend (ex: 2.28€ → 228)
        Long amount = ((Number) requestData.get("amount")).longValue();
        // "commande" ou "inscription" : le webhook Stripe s'en sert pour savoir
        // quel filet de sécurité appliquer (voir StripeWebhookController).
        String type = (String) requestData.getOrDefault("type", "");

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
            .setAmount(amount)
            .setCurrency("eur")
            .addPaymentMethodType("card")
            .putMetadata("type", type)
            .build();

        PaymentIntent intent = PaymentIntent.create(params);

        Map<String, String> response = new HashMap<>();
        response.put("clientSecret", intent.getClientSecret());
        return response;
    }

    /**
     * POST /api/payments/abandon
     * Rembourse un paiement encaissé dont la commande n'a jamais pu être créée.
     *
     * Appelé par le navigateur lorsqu'il constate l'échec, une fois ses tentatives
     * épuisées. Le remboursement est refusé si une commande correspond bien à ce
     * paiement.
     */
    @PostMapping("/abandon")
    public ResponseEntity<Void> abandonnerPaiement(@RequestBody Map<String, String> corps) {
        String demandeur = SecurityContextHolder.getContext().getAuthentication().getName();
        paiementAbandonneService.recuperer(corps.get("paymentIntentId"), demandeur);
        return ResponseEntity.noContent().build();
    }
}
