import { API_URL } from "../lib/api";
import type { MessageContactRequest } from "../models/contact.model";

export const contactService = {
  /**
   * Transmet le formulaire de contact public.
   *
   * @throws Error portant l'explication du serveur, ou un message de repli
   */
  envoyerMessage: async (message: MessageContactRequest): Promise<void> => {
    const response = await fetch(`${API_URL}/contact`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(message),
    });

    if (!response.ok) {
      const corps = await response.json().catch(() => null);
      throw new Error(corps?.message || "Impossible d'envoyer le message.");
    }
  },
};
