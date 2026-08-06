package com.printnow.module.chatbot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Un message de la conversation. Le rôle suit la convention de l'API Mistral :
 * "user" pour le visiteur, "assistant" pour les réponses précédentes du bot.
 */
@Data
public class ChatMessageDTO {

    @NotBlank
    private String role;

    @NotBlank
    @Size(max = 500)
    private String content;
}
