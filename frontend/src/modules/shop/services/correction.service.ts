import { API_URL } from "../../../lib/api";
import type { ChoixCorrection, EtapeAnalyse, Verification } from "../models/correction.model";

const API = `${API_URL}/corrections`;

const authHeaders = (token: string) => ({ Authorization: `Bearer ${token}` });

export const correctionService = {
  /**
   * Analyse un PDF : compte les fautes et calcule le prix, sans rien facturer.
   *
   * @param suivi identifiant sous lequel le serveur publiera son avancement
   * @throws Error portant l'explication du serveur
   */
  analyser: async (fichier: File, suivi: string, token: string): Promise<Verification> => {
    const formData = new FormData();
    formData.append("file", fichier);
    formData.append("suivi", suivi);

    const response = await fetch(`${API}/analyser`, {
      method: "POST",
      headers: authHeaders(token),
      body: formData,
    });

    const corps = await response.json().catch(() => null);
    if (!response.ok) throw new Error(corps?.message ?? "Analyse impossible.");
    return corps;
  },

  /**
   * Où en est l'analyse en cours.
   *
   * @return null si elle est terminée, pas encore commencée, ou si la
   *         consultation a échoué — une étape manquée est sans conséquence,
   *         la suivante suivra.
   */
  progression: async (suivi: string, token: string): Promise<EtapeAnalyse | null> => {
    try {
      const response = await fetch(`${API}/progression/${suivi}`, { headers: authHeaders(token) });
      return response.status === 200 ? await response.json() : null;
    } catch {
      return null;
    }
  },

  /**
   * Image filigranée d'une page corrigée.
   *
   * C'est une image et non le PDF : le document corrigé n'est jamais transmis
   * avant paiement.
   *
   * @throws Error si le serveur ne peut pas produire l'aperçu
   */
  apercuPage: async (verificationId: number, page: number, choix: ChoixCorrection,
                     token: string): Promise<Blob> => {
    const response = await fetch(`${API}/${verificationId}/apercu?page=${page}`, {
      method: "POST",
      headers: { ...authHeaders(token), "Content-Type": "application/json" },
      body: JSON.stringify(choix),
    });

    if (!response.ok) throw new Error("Aperçu indisponible.");
    return response.blob();
  },

  /**
   * Récupère le PDF corrigé d'une vérification réglée.
   *
   * @return le fichier corrigé, ou null si le serveur ne l'a pas fourni —
   *         l'appelant imprime alors l'original plutôt que de bloquer la commande.
   */
  telechargerPdfCorrige: async (verificationId: number, nomFichier: string,
                                token: string): Promise<File | null> => {
    try {
      const response = await fetch(`${API}/${verificationId}/pdf`, { headers: authHeaders(token) });
      if (!response.ok) return null;

      return new File([await response.blob()], nomFichier, { type: "application/pdf" });
    } catch {
      return null;
    }
  },
};
