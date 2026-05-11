export interface Horaire {
  jourSemaine: string;
  heureOuverture: string;
  heureFermeture: string;
  ferme: boolean;
}

export interface Produit {
  id: number;
  typeProduit: string;
  formatImpression: string;
  prixBase: number;
  prixParPage: number;
  proposePlastification?: boolean;
  prixParTypePlastification?: Record<string, number>;
  proposeReliure?: boolean;
  prixParTypeReliure?: Record<string, number>;
}

export interface ImprimerieDetail {
  id: number;
  nom: string;
  description: string;
  adresse: string;
  ville: string;
  logoUrl: string | null;
  telephoneContact: string;
  emailContact: string;
  proposeExpress2h: boolean;
  accepteEtudiants: boolean;
  livraisonActive: boolean;
  actif: boolean;
  horaires: Horaire[];
  produits: Produit[];
}