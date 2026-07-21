// ==========================================
// SUBSTITUT AUX ENUMS (Compatible avec Vite / erasableSyntaxOnly)
// ==========================================
export const TypeProduit = {
  DOCUMENT: "DOCUMENT",
  FLYER: "FLYER",
  CARTE_VISITE: "CARTE_VISITE",
  POSTER: "POSTER"
} as const;
export type TypeProduit = typeof TypeProduit[keyof typeof TypeProduit];

export const FormatImpression = {
  A6: "A6", A5: "A5", A4: "A4", A3: "A3", A2: "A2", A1: "A1", A0: "A0",
  DL_10x21: "DL_10x21",
  CARTE_VISITE_85x55: "CARTE_VISITE_85x55"
} as const;
export type FormatImpression = typeof FormatImpression[keyof typeof FormatImpression];

// NOUVEAU : Types pour la plastification
export const TypePlastification = {
  AUCUNE: "AUCUNE",
  MAT: "MAT",
  BRILLANT: "BRILLANT",
  SOFT_TOUCH: "SOFT_TOUCH"
} as const;
export type TypePlastification = typeof TypePlastification[keyof typeof TypePlastification];

// NOUVEAU : Types pour la reliure
export const TypeReliure = {
  AUCUNE: "AUCUNE",
  SPIRALE_PLASTIQUE: "SPIRALE_PLASTIQUE",
  SPIRALE_METALLIQUE: "SPIRALE_METALLIQUE",
  DOS_CARRE_COLLE: "DOS_CARRE_COLLE",
  AGRAFE_DEUX_POINTS: "AGRAFE_DEUX_POINTS",
  THERMIQUE: "THERMIQUE"
} as const;
export type TypeReliure = typeof TypeReliure[keyof typeof TypeReliure];


// ==========================================
// DTOs
// ==========================================
export interface ProduitRequestDTO {
  typeProduit: TypeProduit;
  formatImpression: FormatImpression;
  prixBase: number;
  prixParPage: number;
  
  // NOUVEAUX CHAMPS POUR LES OPTIONS DE FINITION
  proposePlastification?: boolean;
  prixPlastification?: number | null;
  typesPlastification?: TypePlastification[];
  
  proposeReliure?: boolean;
  prixReliure?: number | null;
  typesReliure?: TypeReliure[];
}

export interface HoraireOuvertureRequestDTO {
  jourSemaine: string;
  heureOuverture: string; // Format "HH:mm:ss"
  heureFermeture: string; // Format "HH:mm:ss"
  ferme: boolean;
}

export interface ImprimerieRequestDTO {
  nom: string;
  description: string;
  logoUrl: string;
  emailContact: string;
  telephoneContact: string;
  adresse: string;
  numeroTva: string;
  ville?: string;
  pays?: string;
  livraisonActive: boolean;
  proposeTarifEtudiant: boolean;
  pourcentageRemiseEtudiant?: number;
  proposeExpress2h: boolean;
  prixExpress2h?: number;
}

export interface PartnerRegistrationRequest {
  email: string;
  password?: string;
  siret?: string;
  paymentIntentId?: string;
  imprimerie: ImprimerieRequestDTO;
  produits: ProduitRequestDTO[];
  horaires: HoraireOuvertureRequestDTO[];
}