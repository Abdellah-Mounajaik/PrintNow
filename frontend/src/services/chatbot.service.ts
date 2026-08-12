import { API_URL } from "../lib/api";
import type { MessageChat } from "../models/chatbot.model";

/** Le serveur n'accepte que 20 messages ; on en envoie moins pour garder de la marge. */
const MAX_HISTORIQUE = 10;

/** Longueur maximale acceptée par message. */
const MAX_CARACTERES = 500;

export const chatbotService = {
  /**
   * Pose une question à l'assistant de la FAQ.
   *
   * Les limites de l'API — longueur des messages et taille de l'historique —
   * sont appliquées ici : ce sont des contraintes du contrat, pas de l'affichage.
   *
   * @return la réponse de l'assistant
   * @throws Error dont le message est directement affichable au visiteur
   */
  demander: async (conversation: MessageChat[]): Promise<string> => {
    let response: Response;
    try {
      response = await fetch(`${API_URL}/chat`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          messages: conversation.slice(-MAX_HISTORIQUE).map((m) => ({
            role: m.role,
            content: m.content.slice(0, MAX_CARACTERES),
          })),
        }),
      });
    } catch {
      throw new Error("Je n'arrive pas à joindre le serveur pour le moment. "
        + "Réessayez dans un instant ou écrivez-nous via la page Contact.");
    }

    const corps = await response.json().catch(() => null);
    if (!response.ok) {
      throw new Error(corps?.message ?? "L'assistant est momentanément indisponible.");
    }
    return corps?.reply ?? "L'assistant est momentanément indisponible.";
  },
};
