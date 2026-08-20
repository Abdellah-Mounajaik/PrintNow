import { API_URL } from "../../../lib/api";
import type { ImprimerieDetail } from "../models/Imprimerie.model";

const PARTNERS_API_URL = `${API_URL}/partners`;
const IMPRIMERIES_API_URL = `${API_URL}/imprimeries`; // Catalogue public
const UPLOADS_API_URL = `${API_URL}/uploads`;

export const partnerService = {

  // Upload du logo (avant l'inscription) : renvoie l'URL publique du fichier stocké
  uploadLogo: async (fichier: File): Promise<string> => {
    const formData = new FormData();
    formData.append("fichier", fichier);

    const response = await fetch(`${UPLOADS_API_URL}/logo`, {
      method: "POST",
      body: formData,
    });

    if (!response.ok) {
      const errorData = await response.text();
      let errorMsg = errorData;
      try {
        const parsed = JSON.parse(errorData);
        errorMsg = parsed.message || parsed.detail || errorData;
      } catch { /* texte brut */ }
      throw new Error(errorMsg || "Erreur lors de l'envoi du logo");
    }

    const data = await response.json();
    return data.url as string;
  },

  // Vérifie que l'email et le numéro de TVA sont libres, avant le paiement
  // (étape 1 du formulaire) : lève une erreur avec le message du backend si
  // l'un des deux est déjà pris.
  verifierDisponibilite: async (email: string, numeroTva: string): Promise<void> => {
    const response = await fetch(
      `${PARTNERS_API_URL}/verifier-disponibilite?email=${encodeURIComponent(email)}&numeroTva=${encodeURIComponent(numeroTva)}`
    );

    if (!response.ok) {
      const errorData = await response.text();
      let errorMsg = errorData;
      try {
        const parsed = JSON.parse(errorData);
        errorMsg = parsed.message || parsed.detail || errorData;
      } catch { /* texte brut */ }
      throw new Error(errorMsg || "Cet email ou ce numéro de TVA est déjà utilisé.");
    }
  },

  // 1. Méthode pour s'inscrire
  register: async (payload: any) => {
    const response = await fetch(`${PARTNERS_API_URL}/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });

    if (!response.ok) {
      // Le backend renvoie soit du JSON {message: "..."} (validation), soit du texte brut
      const errorData = await response.text();
      let errorMsg = errorData;
      try {
        const parsed = JSON.parse(errorData);
        errorMsg = parsed.message || parsed.detail || errorData;
      } catch { /* texte brut */ }
      // Un refus argumenté (4xx) ne vaut pas la peine d'être réessayé — contrairement
      // à une panne réseau/serveur (5xx), où la carte peut être débitée sans que la
      // demande n'ait jamais été traitée.
      const erreur = new Error(errorMsg || "Erreur lors de la création de l'imprimerie") as Error & { definitif?: boolean };
      erreur.definitif = response.status < 500;
      throw erreur;
    }

// 1. On lit la réponse brute en texte (ex: "15" ou '{"id": 15}')
    const rawData = await response.text(); 
    
    // 2. On essaie de la convertir proprement
    try {
      return JSON.parse(rawData); // Si c'est un objet JSON ou un chiffre, ça le convertit
    } catch (e) {
      return rawData; // Si ça échoue, on renvoie le texte brut tel quel
    }  },

  /**
   * Réclame le remboursement d'un paiement d'inscription qui n'a débouché sur
   * aucune imprimerie. Public : aucun compte n'existe encore à ce stade pour
   * authentifier la demande, contrairement au remboursement d'une commande.
   *
   * L'échec de cette réclamation n'est pas propagé : elle n'est qu'un secours,
   * et c'est l'échec de l'inscription qui doit être annoncé au client.
   *
   * @return true si le remboursement a été accepté
   */
  abandonnerInscription: async (paymentIntentId: string): Promise<boolean> => {
    try {
      const response = await fetch(`${PARTNERS_API_URL}/abandon-inscription`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ paymentIntentId }),
      });
      if (!response.ok) console.error("Remboursement refusé par le serveur :", response.status);
      return response.ok;
    } catch (e) {
      console.error("Remboursement injoignable :", e);
      return false;
    }
  },

  /** Catalogue public des imprimeries actives. */
  getAllActive: async (): Promise<ImprimerieDetail[]> => {
    const response = await fetch(IMPRIMERIES_API_URL);

    if (!response.ok) {
      throw new Error("Erreur lors de la récupération des imprimeries");
    }

    return response.json();
  }

};