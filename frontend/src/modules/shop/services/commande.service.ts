import { API_URL } from "../../../lib/api";
import type { CommandeCreee, CommandeRequest } from "../models/commande.model";

/** Le serveur répond en JSON ({"message": "..."}) ; sans cette lecture, l'objet entier s'afficherait. */
const messageDErreur = async (response: Response, defaut: string): Promise<string> => {
  const brut = await response.text();
  try {
    return JSON.parse(brut).message || brut || defaut;
  } catch {
    return brut || defaut;
  }
};

export const commandeService = {
  /**
   * Enregistre la commande, une fois le paiement encaissé.
   *
   * L'erreur levée porte un indicateur `definitif` : un refus argumenté du
   * serveur (4xx) ne s'arrangera pas en réessayant — et il a déjà remboursé —,
   * alors qu'une panne (5xx) mérite une nouvelle tentative.
   */
  passerCommande: async (payload: CommandeRequest, token: string): Promise<CommandeCreee> => {
    const response = await fetch(`${API_URL}/commandes`, {
      method: "POST",
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      const erreur = new Error(
        await messageDErreur(response, "Erreur lors de la création de la commande")
      ) as Error & { definitif?: boolean };
      erreur.definitif = response.status < 500;
      throw erreur;
    }

    return response.json();
  },
};
