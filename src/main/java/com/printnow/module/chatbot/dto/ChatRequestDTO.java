package com.printnow.module.chatbot.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Historique de la conversation envoyé par le frontend. On le plafonne à 20
 * messages : au-delà, le coût en tokens grimpe sans réel gain de pertinence,
 * et cela limite ce qu'un visiteur malveillant peut injecter d'un seul coup.
 */
@Data
public class ChatRequestDTO {

    @NotEmpty
    @Size(max = 20)
    @Valid
    private List<ChatMessageDTO> messages;
}
