/** Fichier PDF joint à une ligne de commande */
export interface FichierPDF {
  id: number;
  nomFichier: string;
  nbPagesDetectees: number;
}

/** Ligne d'une commande (un produit imprimé) */
export interface LigneCommande {
  id: number;
  nomProduit: string;
  nbPages: number;
  quantite: number;
  rectoVerso: boolean;
  reliure: string;
  finition: string;
  prixTotal: number;
  fichiers: FichierPDF[];
}

/** Adresse de livraison (mode LIVRAISON uniquement) */
export interface AdresseLivraison {
  nomDestinataire: string;
  rue: string;
  numero: string;
  codePostal: string;
  ville: string;
  pays: string;
  telephone: string;
}

/** Commande telle qu'affichée dans le dashboard imprimeur */
export interface CommandeImprimeurDTO {
  id: number;
  numeroCommande: string;
  statut: string;
  modeRetrait: string;
  express2h: boolean;
  totalTTC: number;
  dateCreation: string;
  nomClient: string;
  lignes: LigneCommande[];
  adresseLivraison: AdresseLivraison | null;
}

/** Code promo créé par l'imprimeur */
export interface PromoDTO {
  id: number;
  code: string;
  typeReduction: string;
  valeurReduction: number;
  dateFin: string | null;
  utilisationMax: number | null;
  utilisationCourante: number;
  montantMinimumCommande: number | null;
  actif: boolean;
}

/** Payload de création d'un code promo */
export interface PromoRequest {
  code: string;
  typeReduction: string;
  valeurReduction: number;
  dateFin: string | null;
  utilisationMax: number | null;
  montantMinimumCommande: number | null;
  imprimerieId: number;
}
