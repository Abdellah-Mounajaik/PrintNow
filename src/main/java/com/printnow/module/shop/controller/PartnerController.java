package com.printnow.module.shop.controller;

import com.printnow.module.shop.dto.PartnerRegistrationRequest;
import com.printnow.module.shop.service.PartnerRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/partners")
@RequiredArgsConstructor
public class PartnerController {

    private final PartnerRegistrationService registrationService;

    @PostMapping("/register")
    public ResponseEntity<String> registerPartner(@RequestBody PartnerRegistrationRequest request) {
        try {
            registrationService.registerNewPartner(request);
            return ResponseEntity.ok("Imprimerie créée avec succès !");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la création : " + e.getMessage());
        }
    }
}