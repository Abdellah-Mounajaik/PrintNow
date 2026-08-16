package com.printnow.module.studio.service.gabarit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.printnow.module.studio.enums.TypeSupport;
import com.printnow.module.studio.model.ContenuCv;
import com.printnow.module.studio.service.HtmlVersPdf;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** Le CV et ses structures. Rendu HTML/CSS via {@link CvHtml}. */
@Component
@RequiredArgsConstructor
public class CvGabarit implements Gabarit {

    private final ObjectMapper mapper;
    private final HtmlVersPdf html;

    @Override
    public TypeSupport type() {
        return TypeSupport.CV;
    }

    @Override
    public String promptSysteme() {
        return ContenuCv.PROMPT;
    }

    @Override
    public List<String> structures() {
        return List.of("moderne", "entete", "minimal", "droite", "bandeau", "timeline", "cartes");
    }

    @Override
    public byte[] rendre(String json, Style style, String structure) throws Exception {
        ContenuCv cv = mapper.readValue(json, ContenuCv.class);
        if (cv == null || (cv.nom() == null && cv.titrePro() == null
                && (cv.experiences() == null || cv.experiences().isEmpty()))) {
            throw new IllegalArgumentException("JSON du CV vide ou inexploitable");
        }
        return html.versPdf(CvHtml.page(cv, style, structure));
    }
}
