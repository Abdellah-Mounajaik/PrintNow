package com.printnow.module.studio.service.gabarit;

import java.util.Random;

/**
 * Réglages de forme tirés au hasard, qui déclinent une même maquette en de
 * nombreuses variantes : forme du monogramme, forme des puces, style des traits
 * d'accent. Combinés aux couleurs, au fond et à la police, ils font qu'une
 * génération ne ressemble pas à la précédente.
 */
public record Variante(Badge badge, Puce puce, Trait trait) {

    public enum Badge { ANNEAU, DISQUE, CARRE }

    public enum Puce { POINT, CARRE, TIRET }

    public enum Trait { COURT, LONG, POINTS }

    public static Variante aleatoire(Random r) {
        return new Variante(
                Badge.values()[r.nextInt(Badge.values().length)],
                Puce.values()[r.nextInt(Puce.values().length)],
                Trait.values()[r.nextInt(Trait.values().length)]);
    }

    public static Variante defaut() {
        return new Variante(Badge.ANNEAU, Puce.POINT, Trait.COURT);
    }
}
