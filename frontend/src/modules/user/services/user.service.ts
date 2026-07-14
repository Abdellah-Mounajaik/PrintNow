import type { CommandeDTO, VerifDTO, SuiviDTO } from "../models/user.model";

const API_URL = "http://localhost:8080/api";

const authHeaders = (token: string) => ({ Authorization: `Bearer ${token}` });

export const userService = {
  /** Commandes du client connecté */
  getMesCommandes: async (token: string): Promise<CommandeDTO[]> => {
    const response = await fetch(`${API_URL}/commandes/me`, { headers: authHeaders(token) });
    if (!response.ok) throw new Error("Erreur lors de la récupération des commandes");
    return response.json();
  },

  /** Vérification étudiante du client — null s'il n'en a pas encore soumis (204) */
  getMaVerification: async (token: string): Promise<VerifDTO | null> => {
    const response = await fetch(`${API_URL}/verifications-etudiants/me`, { headers: authHeaders(token) });
    if (response.status === 204) return null;
    if (!response.ok) throw new Error("Erreur lors de la récupération de la vérification");
    return response.json();
  },

  /** Dépôt des 2 justificatifs étudiants */
  soumettreVerification: async (
    carteEtudiante: File,
    carteIdentite: File,
    token: string
  ): Promise<VerifDTO> => {
    const formData = new FormData();
    formData.append("carteEtudiante", carteEtudiante);
    formData.append("carteIdentite", carteIdentite);

    const response = await fetch(`${API_URL}/verifications-etudiants/me`, {
      method: "POST",
      headers: authHeaders(token),
      body: formData,
    });
    if (!response.ok) {
      const err = await response.json().catch(() => ({}));
      throw new Error(err.message || err.detail || "Impossible d'envoyer les documents");
    }
    return response.json();
  },

  /** Suivi bpost d'une commande livrée à domicile */
  getSuiviLivraison: async (commandeId: number, token: string): Promise<SuiviDTO> => {
    const response = await fetch(`${API_URL}/livraisons/${commandeId}/suivi`, {
      headers: authHeaders(token),
    });
    if (!response.ok) throw new Error("Impossible de récupérer le suivi");
    return response.json();
  },
};
