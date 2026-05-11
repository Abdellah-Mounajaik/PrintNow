import type { ImprimerieDetail } from "../models/Imprimerie.model";

const API_BASE_URL = "http://localhost:8080/api";

export const imprimerieService = {
  /**
   * Récupère les détails d'une imprimerie par son ID
   */
  getImprimerieById: async (id: string): Promise<ImprimerieDetail> => {
    const response = await fetch(`${API_BASE_URL}/imprimeries/${id}`);
    
    if (!response.ok) {
      throw new Error("Erreur lors de la récupération de l'imprimerie");
    }
    
    return response.json();
  },

  /**
   * Tu pourras ajouter d'autres méthodes ici plus tard, par exemple :
   * getAllImprimeries: async () => { ... }
   */
};