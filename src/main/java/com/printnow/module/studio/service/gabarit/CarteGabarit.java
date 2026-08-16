package com.printnow.module.studio.service.gabarit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.printnow.module.studio.enums.TypeSupport;
import com.printnow.module.studio.model.ContenuCarteVisite;
import com.printnow.module.studio.service.HtmlVersPdf;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** La carte de visite et ses structures. Rendu HTML/CSS via {@link CarteHtml}. */
@Component
@RequiredArgsConstructor
public class CarteGabarit implements Gabarit {

    private final ObjectMapper mapper;
    private final HtmlVersPdf html;

    @Override
    public TypeSupport type() {
        return TypeSupport.CARTE_VISITE;
    }

    @Override
    public String promptSysteme() {
        return ContenuCarteVisite.PROMPT;
    }

    @Override
    public List<String> structures() {
        return List.of("monogramme", "diagonale", "pleine", "sobre");
    }

    @Override
    public byte[] rendre(String json, Style style, String structure) throws Exception {
        ContenuCarteVisite carte = mapper.readValue(json, ContenuCarteVisite.class);
        if (carte == null || (!notBlank(carte.nom()) && !notBlank(carte.entreprise()))) {
            throw new IllegalArgumentException("JSON de la carte vide ou inexploitable");
        }
        return html.versPdf(CarteHtml.page(carte, style, structure));
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
