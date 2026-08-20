import { API_URL } from "../../../lib/api";

const authHeaders = (token: string) => ({ Authorization: `Bearer ${token}` });

export const paiementService = {
  /**
   * Prépare le paiement côté Stripe et renvoie le secret qui permettra de le
   * confirmer depuis le navigateur.
   *
   * @param montantEuros montant à débiter, en euros (converti en centimes ici)
   * @param token facultatif : l'inscription d'un partenaire est réglée avant
   *              même que son compte n'existe
   * @param type "commande" ou "inscription" : lu par le webhook Stripe pour
   *             savoir quel filet de sécurité appliquer si le paiement aboutit
   *             sans que le navigateur ne confirme la suite.
   */
  creerIntention: async (montantEuros: number, token?: string | null, type: "commande" | "inscription" = "commande"): Promise<string> => {
    const response = await fetch(`${API_URL}/payments/create-payment-intent`, {
      method: "POST",
      headers: {
        ...(token ? authHeaders(token) : {}),
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ amount: Math.round(montantEuros * 100), type }),
    });
    if (!response.ok) throw new Error("Erreur lors de la création du paiement.");

    const { clientSecret } = await response.json();
    return clientSecret;
  },

  /**
   * Réclame le remboursement d'un paiement dont la commande n'a pas pu être
   * enregistrée. Le serveur vérifie lui-même qu'aucune commande n'y correspond.
   *
   * L'échec n'est pas propagé : cette réclamation n'est qu'un secours, et c'est
   * l'échec de la commande qui doit être annoncé au client.
   *
   * @return true si le remboursement a été accepté
   */
  abandonner: async (paymentIntentId: string, token: string): Promise<boolean> => {
    try {
      const response = await fetch(`${API_URL}/payments/abandon`, {
        method: "POST",
        headers: { ...authHeaders(token), "Content-Type": "application/json" },
        body: JSON.stringify({ paymentIntentId }),
      });
      if (!response.ok) console.error("Remboursement refusé par le serveur :", response.status);
      return response.ok;
    } catch (e) {
      console.error("Remboursement injoignable :", e);
      return false;
    }
  },
};
