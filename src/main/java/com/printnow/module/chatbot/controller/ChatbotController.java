package com.printnow.module.chatbot.controller;

import com.printnow.module.chatbot.dto.ChatRequestDTO;
import com.printnow.module.chatbot.dto.ChatResponseDTO;
import com.printnow.module.chatbot.service.ChatRateLimiter;
import com.printnow.module.chatbot.service.ChatbotService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final ChatRateLimiter rateLimiter;

    @PostMapping
    public ResponseEntity<Object> discuter(@Valid @RequestBody ChatRequestDTO request,
                                           HttpServletRequest httpRequest) {
        // En production derrière un reverse proxy, il faudra lire l'en-tête
        // X-Forwarded-For : getRemoteAddr() renverrait sinon l'IP du proxy.
        if (!rateLimiter.autoriser(httpRequest.getRemoteAddr())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", "Trop de messages envoyés. Merci de patienter un instant."));
        }

        String reponse = chatbotService.repondre(request.getMessages(), request.getLangue());
        if (reponse == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", "L'assistant est momentanément indisponible. Réessayez ou écrivez-nous via la page Contact."));
        }

        return ResponseEntity.ok(new ChatResponseDTO(reponse));
    }
}
