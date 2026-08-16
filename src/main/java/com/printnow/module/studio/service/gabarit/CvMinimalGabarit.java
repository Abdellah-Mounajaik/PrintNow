package com.printnow.module.studio.service.gabarit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.printnow.module.studio.enums.TypeSupport;
import com.printnow.module.studio.model.ContenuCv;
import com.printnow.module.studio.service.HtmlVersPdf;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** CV « minimaliste » : une colonne aérée, grande typo, chips. Rendu HTML/CSS. */
@Component
@RequiredArgsConstructor
public class CvMinimalGabarit implements Gabarit {

    public static final String CODE = "cv-minimal";

    private final ObjectMapper mapper;
    private final HtmlVersPdf html;

    @Override
    public TypeSupport type() {
        return TypeSupport.CV;
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public String promptSysteme() {
        return ContenuCv.PROMPT;
    }

    @Override
    public byte[] rendre(String json, Style style) throws Exception {
        ContenuCv cv = mapper.readValue(json, ContenuCv.class);
        if (cv == null || (cv.nom() == null && cv.titrePro() == null
                && (cv.experiences() == null || cv.experiences().isEmpty()))) {
            throw new IllegalArgumentException("JSON du CV vide ou inexploitable");
        }
        return html.versPdf(CvHtml.page(cv, style, "minimal"));
    }
}
