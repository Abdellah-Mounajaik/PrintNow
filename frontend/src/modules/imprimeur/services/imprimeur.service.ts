import type { CommandeImprimeurDTO, PromoDTO, PromoRequest } from "../models/imprimeur.model";

const API_URL = "http://localhost:8080/api";

const authHeaders = (token: string) => ({ Authorization: `Bearer ${token}` });

export const imprimeurService = {
  // ── Commandes ──────────────────────────────────────────────────────────────
  getCommandes: async (imprimerieId: number, token: string): Promise<CommandeImprimeurDTO[]> => {
    const response = await fetch(`${API_URL}/commandes/imprimerie/${imprimerieId}`, {
      headers: authHeaders(token),
    });
    if (!response.ok) throw new Error("Erreur lors de la récupération des commandes");
    return response.json();
  },

  updateStatutCommande: async (
    commandeId: number,
    statut: string,
    token: string
  ): Promise<CommandeImprimeurDTO> => {
    const response = await fetch(`${API_URL}/commandes/${commandeId}/statut?statut=${statut}`, {
      method: "PATCH",
      headers: authHeaders(token),
    });
    if (!response.ok) throw new Error("Impossible de mettre à jour le statut");
    return response.json();
  },

  /** Enregistre le numéro de suivi bpost après dépôt du colis */
  ajouterNumeroSuivi: async (commandeId: number, numeroSuivi: string, token: string): Promise<void> => {
    const response = await fetch(`${API_URL}/livraisons/${commandeId}/suivi`, {
      method: "PATCH",
      headers: { ...authHeaders(token), "Content-Type": "application/json" },
      body: JSON.stringify({ numeroSuivi }),
    });
    if (!response.ok) throw new Error("Impossible d'enregistrer le numéro de suivi");
  },

  /** Télécharge le relevé de vente PDF d'une commande (montant vendu / commission déduite / net perçu) */
  telechargerReleveVente: async (commandeId: number, numeroCommande: string, token: string): Promise<void> => {
    const response = await fetch(`${API_URL}/commandes/${commandeId}/releve-vente`, {
      headers: authHeaders(token),
    });
    if (!response.ok) throw new Error("Impossible de télécharger le relevé de vente");
    const blob = await response.blob();
    const url = URL.createObjectURL(blob);
    const lien = document.createElement("a");
    lien.href = url;
    lien.download = `vente-${numeroCommande}.pdf`;
    document.body.appendChild(lien);
    lien.click();
    lien.remove();
    URL.revokeObjectURL(url);
  },

  // ── Fichiers PDF ───────────────────────────────────────────────────────────
  /** Télécharge un PDF protégé et renvoie une URL blob ouvrable */
  downloadFichierPdf: async (fichierId: number, token: string): Promise<string> => {
    const response = await fetch(`${API_URL}/fichiers-pdf/${fichierId}/download`, {
      headers: authHeaders(token),
    });
    if (!response.ok) throw new Error("Impossible de télécharger le fichier");
    const blob = await response.blob();
    return URL.createObjectURL(blob);
  },

  // ── Codes promo ────────────────────────────────────────────────────────────
  getPromos: async (imprimerieId: number, token: string): Promise<PromoDTO[]> => {
    const response = await fetch(`${API_URL}/promos/imprimerie/${imprimerieId}`, {
      headers: authHeaders(token),
    });
    if (!response.ok) throw new Error("Erreur lors de la récupération des codes promo");
    return response.json();
  },

  creerPromo: async (promo: PromoRequest, token: string): Promise<PromoDTO> => {
    const response = await fetch(`${API_URL}/promos`, {
      method: "POST",
      headers: { ...authHeaders(token), "Content-Type": "application/json" },
      body: JSON.stringify(promo),
    });
    if (!response.ok) {
      const err = await response.json().catch(() => ({}));
      throw new Error(err.message || err.detail || "Impossible de créer le code.");
    }
    return response.json();
  },

  togglePromo: async (id: number, token: string): Promise<PromoDTO> => {
    const response = await fetch(`${API_URL}/promos/${id}/toggle`, {
      method: "PATCH",
      headers: authHeaders(token),
    });
    if (!response.ok) throw new Error("Impossible de changer l'état du code");
    return response.json();
  },

  supprimerPromo: async (id: number, token: string): Promise<void> => {
    const response = await fetch(`${API_URL}/promos/${id}`, {
      method: "DELETE",
      headers: authHeaders(token),
    });
    if (!response.ok) throw new Error("Impossible de supprimer le code");
  },
};
