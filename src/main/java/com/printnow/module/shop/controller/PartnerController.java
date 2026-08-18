package com.printnow.module.shop.controller;

import com.printnow.module.parametres.service.ParametresService;
import com.printnow.module.shop.dto.PartnerRegistrationRequest;
import com.printnow.module.shop.service.PartnerRegistrationService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/partners")
@RequiredArgsConstructor
public class PartnerController {

    private final PartnerRegistrationService registrationService;
    private final ParametresService parametresService;

    @PostMapping("/register")
    public ResponseEntity<Object> registerPartner(@Valid @RequestBody PartnerRegistrationRequest request) {
        // On revérifie directement auprès de Stripe que le paiement a bien été confirmé,
        // pour le montant exact attendu, avant de créer quoi que ce soit en base (jamais
        // confiance au seul client, ni sur le statut seul : un paiement d'1€ confirmé ne
        // doit pas suffire à activer une boutique dont l'inscription en coûte 100).
        try {
            PaymentIntent intent = PaymentIntent.retrieve(request.getPaymentIntentId());
            if (!"succeeded".equals(intent.getStatus())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Le paiement n'est pas confirmé."));
            }
            long fraisAttendusCentimes = parametresService.getParametres().getFraisInscription()
                    .multiply(new BigDecimal(100)).longValue();
            if (intent.getAmount() < fraisAttendusCentimes) {
                return ResponseEntity.badRequest().body(Map.of("message", "Le montant payé ne correspond pas aux frais d'inscription."));
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
