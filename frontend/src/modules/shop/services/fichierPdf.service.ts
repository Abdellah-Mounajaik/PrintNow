import { API_URL } from "../../../lib/api";

const authHeaders = (token: string) => ({ Authorization: `Bearer ${token}` });

const messageDErreur = async (response: Response): Promise<string | null> => {
  try {
    return JSON.parse(await response.text()).message ?? null;
  } catch {
    return null;
  }
};

export const fichierPdfService = {
  /**
   * Contrôle qu'un PDF correspond au format du produit choisi, sans rien
   * enregistrer.
   *
   * Appelé AVANT le paiement : le même contrôle a lieu à l'envoi du fichier,
   * mais la commande serait alors déjà réglée.
   *
   * @throws Error si le format ne convient pas
   */
  verifierFormat: async (fichier: File, produitId: number | "", token: string): Promise<void> => {
    const formData = new FormData();
    formData.append("file", fichier);
    formData.append("produitId", String(produitId));

    const response = await fetch(`${API_URL}/fichiers-pdf/verifier-format`, {
      method: "POST",
      headers: authHeaders(token),
      body: formData,
    });

    if (!response.ok) {
      throw new Error(await messageDErreur(response)
        ?? `Le fichier « ${fichier.name} » ne convient pas au produit choisi.`);
    }
  },

  /**
   * Transmet le PDF à imprimer, rattaché à sa ligne de commande.
   *
   * L'erreur levée porte un indicateur `definitif` : un refus du serveur (4xx)
   * ne s'arrangera pas en réessayant, une panne passagère si.
   */
  envoyer: async (fichier: File, ligneCommandeId: number, nbPages: number, token: string): Promise<void> => {
    const formData = new FormData();
    formData.append("file", fichier);
    formData.append("ligneCommandeId", ligneCommandeId.toString());
    formData.append("nbPages", nbPages.toString());

    const response = await fetch(`${API_URL}/fichiers-pdf`, {
      method: "POST",
      headers: authHeaders(token),
      body: formData,
    });

    if (!response.ok) {
      const erreur = new Error(
        await messageDErreur(response) ?? `Upload échoué (${response.status})`
      ) as Error & { definitif?: boolean };
      erreur.definitif = response.status < 500;
      throw erreur;
    }
  },
};
