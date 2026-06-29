import type { ImprimerieDetail } from "../models/Imprimerie.model";

const API_BASE_URL = "http://localhost:8080/api";

export interface ImprimerieUpdateDTO {
  idGerant?: number;
  nom?: string;
  description?: string;
  logoUrl?: string | null;
  emailContact?: string;
  telephoneContact?: string;
  adresse?: string;
  ville?: string;
  pays?: string;
  latitude?: number;
  longitude?: number;
  proposeExpress2h?: boolean;
  pourcentageRemiseEtudiant?: number;
  prixExpress2h?: number;
  livraisonActive?: boolean;
  proposeTarifEtudiant?: boolean;
  numeroTva?: string;
}

export const imprimerieService = {
  getImprimerieById: async (id: string): Promise<ImprimerieDetail> => {
    const response = await fetch(`${API_BASE_URL}/imprimeries/${id}`);
    if (!response.ok) throw new Error("Erreur lors de la récupération de l'imprimerie");
    return response.json();
  },

  updateImprimerie: async (id: string, dto: ImprimerieUpdateDTO): Promise<ImprimerieDetail> => {
    const response = await fetch(`${API_BASE_URL}/imprimeries/${id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(dto),
    });
    if (!response.ok) throw new Error("Erreur lors de la mise à jour de l'imprimerie");
    return response.json();
  },

  updateProduit: async (id: number, dto: Record<string, unknown>): Promise<unknown> => {
    const response = await fetch(`${API_BASE_URL}/produits/${id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(dto),
    });
    if (!response.ok) throw new Error("Erreur lors de la mise à jour du produit");
    return response.json();
  },

  updateHoraire: async (id: number, dto: Record<string, unknown>): Promise<unknown> => {
    const response = await fetch(`${API_BASE_URL}/horaires/${id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(dto),
    });
    if (!response.ok) throw new Error("Erreur lors de la mise à jour de l'horaire");
    return response.json();
  },
};