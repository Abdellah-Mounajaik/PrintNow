package com.printnow.module.shop.controller;

import com.printnow.module.shop.dto.PartnerRegistrationRequest;
import com.printnow.module.shop.service.PartnerRegistrationService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/partners")
@RequiredArgsConstructor
public class PartnerController {

    private final PartnerRegistrationService registrationService;

    @PostMapping("/register")
    public ResponseEntity<Object> registerPartner(@Valid @RequestBody PartnerRegistrationRequest request) {
        // On revérifie directement auprès de Stripe que le paiement a bien été confirmé
        // avant de créer quoi que ce soit en base (jamais confiance au seul client).
        try {
            PaymentIntent intent = PaymentIntent.retrieve(request.getPaymentIntentId());
            if (!"succeeded".equals(intent.getStatus())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Le paiement n'est pas confirmé."));
            }
        } catch (StripeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Impossible de vérifier le paiement auprès de Stripe."));
        }

        try {
            Long imprimerieId = registrationService.registerNewPartner(request);
            return ResponseEntity.ok(imprimerieId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Erreur lors de la création : " + e.getMessage()));
        }
    }
}
