package com.printnow.module.studio.dto;

import lombok.Getter;
import lombok.Setter;

/** Ce que le client envoie pour lancer une génération. */
@Getter
@Setter
public class GenererRequestDTO {
    /** Type de support : « CV », « FLYER », « CARTE_VISITE ». */
    private String type;
    /** Description libre de ce que le client veut. */
    private String brief;
}
