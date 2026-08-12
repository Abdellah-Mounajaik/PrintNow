import type { SuggestionAdresse } from "../models/adresse.model";

/**
 * Photon (Komoot) : service public d'auto-complétion d'adresses, adossé à
 * OpenStreetMap. Il ne passe pas par le backend PrintNow.
 */
const PHOTON_URL = "https://photon.komoot.io/api/";

/** En deçà, la recherche renvoie surtout du bruit. */
export const LONGUEUR_MINIMALE = 4;

export const adresseService = {
  /**
   * Propose des adresses à partir de ce que l'utilisateur a commencé à taper.
   *
   * Les résultats sans rue ni ville sont écartés : un pays ou une région seuls
   * ne remplissent pas un formulaire de livraison.
   *
   * Ne lève jamais : une suggestion manquée ne doit pas interrompre la saisie.
   */
  rechercher: async (requete: string): Promise<SuggestionAdresse[]> => {
    if (requete.trim().length < LONGUEUR_MINIMALE) return [];

    try {
      const response = await fetch(
        `${PHOTON_URL}?q=${encodeURIComponent(requete)}&limit=5&lang=fr`
      );
      const data = await response.json();

      return (data.features || [])
        .map((element: any): SuggestionAdresse | null => {
          const p = element.properties;
          const rue = [p.housenumber, p.street || p.name].filter(Boolean).join(" ");
          const ville = p.city || p.town || p.village || "";
          if (!rue || !ville) return null;

          return {
            label: [rue, p.postcode, ville].filter(Boolean).join(", "),
            adresse: [rue, p.postcode].filter(Boolean).join(" "),
            ville,
            pays: p.country || "",
          };
        })
        .filter((s: SuggestionAdresse | null): s is SuggestionAdresse => s !== null);
    } catch {
      return [];
    }
  },
};
