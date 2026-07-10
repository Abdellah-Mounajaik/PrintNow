export interface Horaire {
  id: number;
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
  actif?: boolean;
  proposePlastification?: boolean;
  prixParTypePlastification?: Record<string, number>;
  proposeReliure?: boolean;
  prixParTypeReliure?: Record<string, number>;
  
}

export interface ImprimerieDetail {
  proposeTarifEtudiant: boolean;
  id: number;
  nom: string;
  description: string;
  adresse: string;
  ville: string;
  numeroTva?: string;
  logoUrl: string | null;
  telephoneContact? : string;
  emailContact: string;
  proposeExpress2h: boolean;
  prixExpress2h?: number;
  accepteEtudiants: boolean;
  pourcentageRemiseEtudiant?: number;  
  livraisonActive: boolean;
  prixLivraison?: number;
  actif: boolean;
  horaires?: Horaire[];
  produits: Produit[];
}