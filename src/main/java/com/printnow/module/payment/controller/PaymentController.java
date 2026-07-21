package com.printnow.module.payment.controller;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

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

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
            .setAmount(amount)
            .setCurrency("eur")
            .addPaymentMethodType("card")
            .build();

        PaymentIntent intent = PaymentIntent.create(params);

        Map<String, String> response = new HashMap<>();
        response.put("clientSecret", intent.getClientSecret());
        return response;
    }
}
