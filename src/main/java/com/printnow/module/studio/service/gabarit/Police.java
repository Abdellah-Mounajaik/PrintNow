package com.printnow.module.studio.service.gabarit;

/**
 * Une famille de police, mappée à une famille CSS par les maquettes HTML :
 * {@code moderne} → sans-serif, {@code classique} → serif.
 */
public enum Police {
    MODERNE("moderne"),
    CLASSIQUE("classique");

    private final String code;

    Police(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
