/** Adresse de livraison, telle qu'envoyée avec la commande. */
export interface AdresseLivraisonRequest {
  nomDestinataire: string;
  rue: string;
  numero: string;
  codePostal: string;
  ville: string;
  pays: string;
  telephone: string;
}

/** Un fichier à imprimer, avec les options choisies pour lui. */
export interface LigneCommandeRequest {
  produitId: number | "";
  quantite: number;
  nbPages: number;
  couleur: boolean;
  rectoVerso: boolean;
  reliure: string;
  finition: string;
}

/** Vérification orthographique réglée avec la commande. */
export interface CorrectionRequest {
  verificationId: number;
  fautesIgnorees: number[];
  remplacementsChoisis: Record<number, string>;
}

/** Corps de POST /api/commandes. */
export interface CommandeRequest {
  paymentIntentId: string;
  modeRetrait: "RETRAIT_MAGASIN" | "LIVRAISON";
  express2h: boolean;
  tarifEtudiant: boolean;
  codePromo: string | null;
  adresseLivraison?: AdresseLivraisonRequest;
  lignes: LigneCommandeRequest[];
  corrections: CorrectionRequest[];
}

/** Ligne enregistrée, renvoyée par le serveur : son id sert à y rattacher le PDF. */
export interface LigneCommandeCreee {
  id: number;
}

/** Ce que renvoie le serveur une fois la commande enregistrée. */
export interface CommandeCreee {
  numeroCommande: string;
  numeroSuivi?: string;
  /** Impression seule ; la correction est facturée à part. */
  totalTTC: number | string;
  montantCorrections?: number | string | null;
  lignes?: LigneCommandeCreee[];
}

/** Code promo accepté par le serveur (GET /api/promos/valider). */
export interface PromoValide {
  code: string;
  typeReduction: string;
  valeurReduction: number | string;
  montantMinimumCommande?: number | string | null;
}
