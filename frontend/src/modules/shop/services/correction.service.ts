import { API_URL } from "../../../lib/api";

export const correctionService = {
  /**
   * Récupère le PDF corrigé d'une vérification réglée.
   *
   * @return le fichier corrigé, ou null si le serveur ne l'a pas fourni —
   *         l'appelant imprime alors l'original plutôt que de bloquer la commande.
   */
  telechargerPdfCorrige: async (verificationId: number, nomFichier: string,
                                token: string): Promise<File | null> => {
    try {
      const response = await fetch(`${API_URL}/corrections/${verificationId}/pdf`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!response.ok) return null;

      return new File([await response.blob()], nomFichier, { type: "application/pdf" });
    } catch {
      return null;
    }
  },
};
