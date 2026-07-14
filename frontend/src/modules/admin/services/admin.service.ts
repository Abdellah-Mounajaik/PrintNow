import type {
  UserDTO,
  ImprimerieDTO,
  CommandeDTO,
  VerificationDTO,
  TypeCarte,
} from "../models/admin.model";

const API_URL = "http://localhost:8080/api";

const authHeaders = (token: string) => ({ Authorization: `Bearer ${token}` });

export const adminService = {
  getUsers: async (token: string): Promise<UserDTO[]> => {
    const response = await fetch(`${API_URL}/users`, { headers: authHeaders(token) });
    if (!response.ok) throw new Error("Erreur lors de la récupération des utilisateurs");
    return response.json();
  },

  getImprimeries: async (): Promise<ImprimerieDTO[]> => {
    const response = await fetch(`${API_URL}/imprimeries`);
    if (!response.ok) throw new Error("Erreur lors de la récupération des imprimeries");
    return response.json();
  },

  getCommandes: async (token: string): Promise<CommandeDTO[]> => {
    const response = await fetch(`${API_URL}/commandes`, { headers: authHeaders(token) });
    if (!response.ok) throw new Error("Erreur lors de la récupération des commandes");
    return response.json();
  },

  getVerifications: async (token: string): Promise<VerificationDTO[]> => {
    const response = await fetch(`${API_URL}/verifications-etudiants`, { headers: authHeaders(token) });
    if (!response.ok) throw new Error("Erreur lors de la récupération des vérifications");
    return response.json();
  },

  validerVerification: async (id: number, token: string): Promise<VerificationDTO> => {
    const response = await fetch(`${API_URL}/verifications-etudiants/${id}/valider`, {
      method: "PATCH",
      headers: authHeaders(token),
    });
    if (!response.ok) throw new Error("Impossible de valider la vérification");
    return response.json();
  },

  refuserVerification: async (id: number, motifRefus: string, token: string): Promise<VerificationDTO> => {
    const response = await fetch(`${API_URL}/verifications-etudiants/${id}/refuser`, {
      method: "PATCH",
      headers: { ...authHeaders(token), "Content-Type": "application/json" },
      body: JSON.stringify({ motifRefus }),
    });
    if (!response.ok) throw new Error("Impossible de refuser la vérification");
    return response.json();
  },

  /** URL d'une image de vérification (nécessite le token, à charger via fetch + blob) */
  getImageUrl: (verificationId: number, type: TypeCarte): string =>
    `${API_URL}/verifications-etudiants/${verificationId}/image/${type}`,

  /** Charge une image protégée et renvoie une URL blob affichable dans <img src> */
  fetchImageBlob: async (url: string, token: string): Promise<string> => {
    const response = await fetch(url, { headers: authHeaders(token) });
    if (!response.ok) throw new Error("Image introuvable");
    const blob = await response.blob();
    return URL.createObjectURL(blob);
  },
};
