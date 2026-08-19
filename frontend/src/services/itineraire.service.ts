import type { PointGps } from "../models/itineraire.model";

/** Routage gratuit basé sur OpenStreetMap, sans clé. Ne passe pas par le backend. */
const OSRM_URL = "https://router.project-osrm.org/route/v1/driving";
const VALHALLA_URL = "https://valhalla1.openstreetmap.de/route";

const attendre = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * Réessaie une fois après un court délai en cas d'échec : ces services publics
 * gratuits (OSRM/Valhalla), partagés avec tout le monde, échouent parfois de
 * façon purement transitoire (surcharge momentanée), sans lien avec la requête
 * elle-même — un simple nouvel essai suffit la plupart du temps.
 */
async function avecNouvelEssai<T>(tache: () => Promise<T>): Promise<T> {
  try {
    return await tache();
  } catch (e) {
    await attendre(600);
    return tache();
  }
}

export const itineraireService = {
  /**
   * Temps de trajet en voiture, en minutes.
   *
   * Suit les vraies routes plutôt qu'une estimation à vol d'oiseau.
   *
   * @return null si le service est indisponible ou ne trouve pas d'itinéraire :
   *         l'appelant retombe alors sur la distance à vol d'oiseau.
   */
  dureeEnVoitureMin: async (depart: PointGps, arrivee: PointGps): Promise<number | null> => {
    try {
      return await avecNouvelEssai(async () => {
        const url = `${OSRM_URL}/${depart.lng},${depart.lat};${arrivee.lng},${arrivee.lat}?overview=false`;
        const data = await (await fetch(url)).json();

        const secondes = data?.routes?.[0]?.duration;
        return typeof secondes === "number" ? Math.round(secondes / 60) : null;
      });
    } catch {
      return null;
    }
  },

  /**
   * Temps de trajet à pied, en minutes.
   *
   * Valhalla plutôt qu'OSRM : le profil « à pied » du serveur public d'OSRM
   * renvoie en réalité des vitesses de voiture.
   *
   * @return null si le service est indisponible
   */
  dureeAPiedMin: async (depart: PointGps, arrivee: PointGps): Promise<number | null> => {
    try {
      return await avecNouvelEssai(async () => {
        const response = await fetch(VALHALLA_URL, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            locations: [
              { lat: depart.lat, lon: depart.lng },
              { lat: arrivee.lat, lon: arrivee.lng },
            ],
            costing: "pedestrian",
          }),
        });
        const data = await response.json();

        const secondes = data?.trip?.summary?.time;
        return typeof secondes === "number" ? Math.round(secondes / 60) : null;
      });
    } catch {
      return null;
    }
  },
};
