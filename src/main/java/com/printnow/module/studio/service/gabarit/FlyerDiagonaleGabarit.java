package com.printnow.module.studio.service.gabarit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.printnow.module.studio.enums.TypeSupport;
import com.printnow.module.studio.model.ContenuFlyer;
import com.printnow.module.studio.service.HtmlVersPdf;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Flyer « split » : grand bloc coloré en haut, bas blanc à puces. Rendu HTML/CSS. */
@Component
@RequiredArgsConstructor
public class FlyerDiagonaleGabarit implements Gabarit {

    public static final String CODE = "flyer-split";

    private final ObjectMapper mapper;
    private final HtmlVersPdf html;

    @Override
    public TypeSupport type() {
        return TypeSupport.FLYER;
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public String promptSysteme() {
        return ContenuFlyer.PROMPT;
    }

    @Override
    public byte[] rendre(String json, Style style) throws Exception {
        ContenuFlyer flyer = mapper.readValue(json, ContenuFlyer.class);
        if (flyer == null || (!notBlank(flyer.titre()) && !notBlank(flyer.accroche()))) {
            throw new IllegalArgumentException("JSON du flyer vide ou inexploitable");
        }
        return html.versPdf(FlyerHtml.page(flyer, style, "split"));
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
