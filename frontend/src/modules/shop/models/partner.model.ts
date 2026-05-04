// ==========================================
// SUBSTITUT AUX ENUMS (Compatible avec Vite / erasableSyntaxOnly)
// ==========================================
export const TypeProduit = {
  DOCUMENT: "DOCUMENT",
  FLYER: "FLYER",
  CARTE_VISITE: "CARTE_VISITE",
  POSTER: "POSTER"
} as const;

// On crée le type TypeScript à partir de l'objet ci-dessus
export type TypeProduit = typeof TypeProduit[keyof typeof TypeProduit];

export const FormatImpression = {
  A6: "A6", A5: "A5", A4: "A4", A3: "A3", A2: "A2", A1: "A1", A0: "A0",
  DL_10x21: "DL_10x21",
  CARTE_VISITE_85x55: "CARTE_VISITE_85x55"
} as const;

// On crée le type TypeScript à partir de l'objet ci-dessus
export type FormatImpression = typeof FormatImpression[keyof typeof FormatImpression];

// ==========================================
// DTOs
// ==========================================
export interface ProduitRequestDTO {
  typeProduit: TypeProduit;
  formatImpression: FormatImpression;
  prixBase: number;
  prixParPage: number;
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
  emailContact: string;
  telephoneContact: string;
  adresse: string;
  ville?: string;
  pays?: string;
  livraisonActive: boolean;
}

export interface PartnerRegistrationRequest {
  email: string;
  password?: string;
  siret?: string;
  imprimerie: ImprimerieRequestDTO;
  produits: ProduitRequestDTO[];
  horaires: HoraireOuvertureRequestDTO[];
}