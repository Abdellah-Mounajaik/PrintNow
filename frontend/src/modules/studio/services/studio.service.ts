import { API_URL } from "../../../lib/api";
import type { GenerationStudio } from "../models/studio.model";

const STUDIO_URL = `${API_URL}/studio`;
const authHeaders = (token: string) => ({ Authorization: `Bearer ${token}` });

/** Seul endroit qui parle à /api/studio. */
export const studioService = {
  /** Lance une génération et renvoie la génération + ses propositions (synchrone). */
  generer: async (type: string, brief: string, token: string): Promise<GenerationStudio> => {
    const response = await fetch(`${STUDIO_URL}/generer`, {
      method: "POST",
      headers: { ...authHeaders(token), "Content-Type": "application/json" },
      body: JSON.stringify({ type, brief }),
    });
    if (!response.ok) {
      const brut = await response.text();
      let message = "La génération n'a pas abouti.";
      try {
        message = JSON.parse(brut).message || message;
      } catch {
        /* réponse non JSON */
      }
      throw new Error(message);
    }
    return response.json();
  },

  /**
   * Récupère un fichier protégé (aperçu PNG ou PDF) avec le jeton, et renvoie une
   * URL blob affichable dans une <img> ou téléchargeable. Ces endpoints exigent
   * l'authentification : impossible de les mettre directement dans un src.
   */
  fichierUrl: async (propositionId: number, type: "apercu" | "pdf", token: string): Promise<string> => {
    const response = await fetch(`${STUDIO_URL}/propositions/${propositionId}/${type}`, {
      headers: authHeaders(token),
    });
    if (!response.ok) throw new Error("Fichier indisponible.");
    return URL.createObjectURL(await response.blob());
  },

  /**
   * Récupère le PDF généré comme un vrai fichier, prêt à entrer dans le tunnel de
   * commande exactement comme un PDF téléversé par le client.
   */
  pdfFichier: async (propositionId: number, token: string): Promise<File> => {
    const response = await fetch(`${STUDIO_URL}/propositions/${propositionId}/pdf`, {
      headers: authHeaders(token),
    });
    if (!response.ok) throw new Error("PDF indisponible.");
    return new File([await response.blob()], "support-genere.pdf", { type: "application/pdf" });
  },
};
