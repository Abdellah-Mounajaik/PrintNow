package com.printnow.module.avis.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AvisResponseDTO {
    private Long id;
    private Long userId;
    private String nomClient;
    private Integer note;
    private String commentaire;
    private LocalDateTime dateCreation;
}
