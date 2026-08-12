/** Un avis laissé par un client sur une imprimerie. */
export interface AvisResponse {
  id: number;
  userId: number;
  nomClient: string;
  note: number;
  commentaire: string | null;
  dateCreation: string;
}

/**
 * Les avis d'une imprimerie, avec ce que le visiteur a le droit d'en faire.
 *
 * `peutNoter` n'est vrai qu'après une commande récupérée chez elle, et
 * `dejaNote` empêche un second avis du même client.
 */
export interface AvisImprimerie {
  noteMoyenne: number | null;
  nombreAvis: number;
  peutNoter: boolean;
  dejaNote: boolean;
  avis: AvisResponse[];
}

/** Corps de POST /api/avis. */
export interface AvisRequest {
  imprimerieId: number;
  note: number;
  commentaire: string;
}
