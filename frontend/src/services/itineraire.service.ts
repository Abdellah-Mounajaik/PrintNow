import type { PointGps } from "../models/itineraire.model";

/** Routage gratuit basé sur OpenStreetMap, sans clé. Ne passe pas par le backend. */
const OSRM_URL = "https://router.project-osrm.org/route/v1/driving";
const VALHALLA_URL = "https://valhalla1.openstreetmap.de/route";

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
      const url = `${OSRM_URL}/${depart.lng},${depart.lat};${arrivee.lng},${arrivee.lat}?overview=false`;
      const data = await (await fetch(url)).json();

      const secondes = data?.routes?.[0]?.duration;
      return typeof secondes === "number" ? Math.round(secondes / 60) : null;
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
    } catch {
      return null;
    }
  },
};
