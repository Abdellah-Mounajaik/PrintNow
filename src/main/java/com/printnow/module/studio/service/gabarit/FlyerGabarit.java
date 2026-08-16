package com.printnow.module.studio.service.gabarit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.printnow.module.studio.enums.TypeSupport;
import com.printnow.module.studio.model.ContenuFlyer;
import com.printnow.module.studio.service.HtmlVersPdf;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** Le flyer et ses structures. Rendu HTML/CSS via {@link FlyerHtml}. */
@Component
@RequiredArgsConstructor
public class FlyerGabarit implements Gabarit {

    private final ObjectMapper mapper;
    private final HtmlVersPdf html;

    @Override
    public TypeSupport type() {
        return TypeSupport.FLYER;
    }

    @Override
    public String promptSysteme() {
        return ContenuFlyer.PROMPT;
    }

    @Override
    public List<String> structures() {
        return List.of("pleine", "editorial", "split", "centre", "lateral", "affiche");
    }

    @Override
    public byte[] rendre(String json, Style style, String structure) throws Exception {
        ContenuFlyer flyer = mapper.readValue(json, ContenuFlyer.class);
        if (flyer == null || (!notBlank(flyer.titre()) && !notBlank(flyer.accroche()))) {
            throw new IllegalArgumentException("JSON du flyer vide ou inexploitable");
        }
        return html.versPdf(FlyerHtml.page(flyer, style, structure));
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
