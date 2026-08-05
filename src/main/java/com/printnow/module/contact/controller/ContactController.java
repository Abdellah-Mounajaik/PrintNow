package com.printnow.module.contact.controller;

import com.printnow.infrastructure.email.EmailService;
import com.printnow.module.contact.dto.ContactRequestDTO;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
@Slf4j
public class ContactController {

    private final EmailService emailService;

    @PostMapping
    public ResponseEntity<Object> envoyerMessage(@Valid @RequestBody ContactRequestDTO request) {
        try {
            emailService.envoyerMessageContact(request.getNom(), request.getEmail(), request.getSujet(), request.getMessage());
            return ResponseEntity.ok().build();
        } catch (MessagingException e) {
            log.warn("Échec de l'envoi du message de contact de {}", request.getEmail(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Impossible d'envoyer le message pour le moment. Merci de réessayer."));
        }
    }
}
