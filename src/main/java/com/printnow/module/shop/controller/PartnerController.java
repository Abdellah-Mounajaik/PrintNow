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
public ResponseEntity<Object> registerPartner(@RequestBody PartnerRegistrationRequest request) {
    
    System.out.println("DEBUG: Tentative d'inscription avec l'email: [" + request.getEmail() + "]");
    
    try {
        Long imprimerieId = registrationService.registerNewPartner(request);
        
        return ResponseEntity.ok(imprimerieId);
        
    } catch (Exception e) {
        e.printStackTrace();
        System.out.println("ERREUR CRITIQUE : " + e.getMessage());
        return ResponseEntity.badRequest().body("Erreur lors de la création : " + e.getMessage());
    }
}
}