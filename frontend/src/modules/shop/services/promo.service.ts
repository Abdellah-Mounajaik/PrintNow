import { API_URL } from "../../../lib/api";
import type { PromoValide } from "../models/commande.model";

export const promoService = {
  /**
   * Vérifie un code promo avant le paiement.
   *
   * L'imprimerie est transmise : un code ne vaut que chez celle qui l'a créé.
   * Le serveur explique pourquoi il refuse (code inconnu, expiré, montant
   * minimum non atteint…) ; ce motif est remonté tel quel pour être affiché.
   *
   * @throws Error portant le motif du refus
   */
  valider: async (code: string, montantTTC: number, imprimerieId: number | string | undefined,
                  token: string): Promise<PromoValide> => {
    const parametres = new URLSearchParams({
      code,
      montant: montantTTC.toFixed(2),
      imprimerieId: String(imprimerieId ?? ""),
    });

    const response = await fetch(`${API_URL}/promos/valider?${parametres}`, {
      headers: { Authorization: `Bearer ${token}` },
    });

    if (!response.ok) {
      let motif = "Code invalide ou inexistant.";
      try {
        const erreur = await response.json();
        motif = erreur.message || erreur.detail || motif;
      } catch { /* réponse illisible : on garde le message par défaut */ }
      throw new Error(motif);
    }

    return response.json();
  },
};
