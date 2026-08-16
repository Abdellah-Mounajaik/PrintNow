package com.printnow.module.studio.service.gabarit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.printnow.module.studio.enums.TypeSupport;
import com.printnow.module.studio.model.ContenuCarteVisite;
import com.printnow.module.studio.service.HtmlVersPdf;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Carte « monogramme » : pastille d'initiales, coordonnées à puces. Rendu HTML/CSS. */
@Component
@RequiredArgsConstructor
public class CarteMonogrammeGabarit implements Gabarit {

    public static final String CODE = "carte-monogramme";

    private final ObjectMapper mapper;
    private final HtmlVersPdf html;

    @Override
    public TypeSupport type() {
        return TypeSupport.CARTE_VISITE;
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public String promptSysteme() {
        return ContenuCarteVisite.PROMPT;
    }

    @Override
    public byte[] rendre(String json, Style style) throws Exception {
        ContenuCarteVisite carte = mapper.readValue(json, ContenuCarteVisite.class);
        if (carte == null || (!notBlank(carte.nom()) && !notBlank(carte.entreprise()))) {
            throw new IllegalArgumentException("JSON de la carte vide ou inexploitable");
        }
        return html.versPdf(CarteHtml.page(carte, style, "monogramme"));
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
