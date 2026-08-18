import { API_URL } from "../lib/api";
import type { ParametresPlateforme } from "../models/parametres.model";

export const parametresService = {
  /** Public : tarifs actuels de la plateforme. */
  getParametres: async (): Promise<ParametresPlateforme> => {
    const response = await fetch(`${API_URL}/parametres`);
    if (!response.ok) throw new Error("Erreur lors de la récupération des tarifs.");
    return response.json();
  },

  /** Réservé à l'administration. */
  mettreAJour: async (dto: ParametresPlateforme, token: string): Promise<ParametresPlateforme> => {
    const response = await fetch(`${API_URL}/parametres`, {
      method: "PUT",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
      body: JSON.stringify(dto),
    });
    if (!response.ok) {
      const err = await response.json().catch(() => ({}));
      const message = err.message || err.detail || err.error;
      throw new Error(message || "Erreur lors de la mise à jour des tarifs.");
    }
    return response.json();
  },
};
