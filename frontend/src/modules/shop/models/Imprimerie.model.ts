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
  /** Uniquement pertinent pour les DOCUMENT (photocopies) : N&B ou Couleur */
  couleur?: boolean;
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
  /** Adresse lisible de la fiche : « imprimerie-du-centre ». */
  slug?: string;
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
  pourcentageRemiseRectoVerso?: number;
  livraisonActive: boolean;
  prixLivraison?: number;
  actif: boolean;
  noteMoyenne?: number | null;
  nombreAvis?: number;
  horaires?: Horaire[];
  produits: Produit[];
  /** Présent seulement si les frais d'inscription ont bien été facturés. */
  numeroFactureInscription?: string | null;
}
/**
 * Modification d'une imprimerie : tous les champs sont facultatifs, seuls ceux
 * transmis sont enregistres.
 */
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
