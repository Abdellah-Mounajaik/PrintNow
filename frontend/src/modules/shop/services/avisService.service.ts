import { API_URL as API_BASE_URL } from "../../../lib/api";

export interface AvisResponse {
  id: number;
  userId: number;
  nomClient: string;
  note: number;
  commentaire: string | null;
  dateCreation: string;
}

export interface AvisImprimerie {
  noteMoyenne: number | null;
  nombreAvis: number;
  peutNoter: boolean;
  dejaNote: boolean;
  avis: AvisResponse[];
}

export const avisService = {
  getAvisImprimerie: async (imprimerieId: number, token?: string | null): Promise<AvisImprimerie> => {
    const response = await fetch(`${API_BASE_URL}/avis/imprimerie/${imprimerieId}`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    });
    if (!response.ok) throw new Error("Erreur lors de la récupération des avis");
    return response.json();
  },

  creerAvis: async (
    dto: { imprimerieId: number; note: number; commentaire: string },
    token: string
  ): Promise<AvisResponse> => {
    const response = await fetch(`${API_BASE_URL}/avis`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
      body: JSON.stringify(dto),
    });
    if (!response.ok) {
      const err = await response.json().catch(() => ({}));
      // Spring peut renvoyer le texte dans "message" (format classique) ou "detail" (ProblemDetail)
      const message = err.message || err.detail || err.error;
      throw new Error(message || "Erreur lors de l'envoi de l'avis");
    }
    return response.json();
  },
};
