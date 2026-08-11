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

  /**
   * Supprime un compte. Le serveur conserve la ligne — commandes et factures y
   * renvoient — mais en efface les données personnelles.
   *
   * Il refuse dans deux cas (commandes en cours, dernier administrateur) : son
   * explication est remontée telle quelle pour être affichée.
   */
  supprimerUtilisateur: async (id: number, token: string): Promise<void> => {
    const response = await fetch(`${API_URL}/users/${id}`, {
      method: "DELETE",
      headers: authHeaders(token),
    });
    if (!response.ok) {
      const brut = await response.text();
      let motif = "Erreur lors de la suppression du compte";
      try {
        motif = JSON.parse(brut).message || motif;
      } catch { if (brut) motif = brut; }
      throw new Error(motif);
    }
  },

  /**
   * Ferme une imprimerie : elle quitte le catalogue et ne reçoit plus de
   * commandes. Ses données restent en base — ses commandes passées y renvoient.
   */
  fermerImprimerie: async (id: number, token: string): Promise<void> => {
    const response = await fetch(`${API_URL}/imprimeries/${id}`, {
      method: "DELETE",
      headers: authHeaders(token),
    });
    if (!response.ok) {
      const brut = await response.text();
      let motif = "Erreur lors de la fermeture de l'imprimerie";
      try {
        motif = JSON.parse(brut).message || motif;
      } catch { if (brut) motif = brut; }
      throw new Error(motif);
    }
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

  /** Télécharge le relevé de commission PDF d'une commande (montant vendu / commission / net imprimerie) */
  telechargerFactureCommission: async (commandeId: number, numeroCommande: string, token: string): Promise<void> => {
    const response = await fetch(`${API_URL}/commandes/${commandeId}/facture-commission`, {
      headers: authHeaders(token),
    });
    if (!response.ok) throw new Error("Impossible de télécharger le relevé de commission");
    const blob = await response.blob();
    const url = URL.createObjectURL(blob);
    const lien = document.createElement("a");
    lien.href = url;
    lien.download = `commission-${numeroCommande}.pdf`;
    document.body.appendChild(lien);
    lien.click();
    lien.remove();
    URL.revokeObjectURL(url);
  },
};
