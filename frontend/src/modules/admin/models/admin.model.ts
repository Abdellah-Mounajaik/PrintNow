export interface UserDTO {
  id: number;
  email: string;
  prenom: string;
  nom: string;
  telephone: string;
  actif: boolean;
  roleNom: string;
  /** Renseignée si le compte a été supprimé — ses données ont alors été effacées. */
  dateSuppression: string | null;
}

export interface ImprimerieDTO {
  id: number;
  nom: string;
  ville: string;
  emailContact: string;
  actif: boolean;
  /** Absent pour les imprimeries inscrites avant la mise en place de cette facture. */
  numeroFactureInscription?: string | null;
  /** Montant réellement payé à l'inscription, figé (voir numeroFactureInscription). */
  montantFraisInscription?: number | null;
}

export interface CommandeDTO {
  id: number;
  numeroCommande: string;
  statut: string;
  totalTTC: number;
  /** Vérification orthographique réglée avec la commande : revenu PrintNow. */
  montantCorrections?: number | null;
  /** Designs IA (studio) réglés avec la commande : revenu PrintNow. */
  montantGenerations?: number | null;
  /** Commission PrintNow sur cette commande, déjà calculée au tarif en vigueur au moment de la commande. */
  commissionPlateforme?: number | null;
  dateCreation: string;
  nomClient: string;
  nomImprimerie: string;
}

export interface VerificationDTO {
  id: number;
  userId: number;
  nomUtilisateur: string;
  emailUtilisateur: string;
  statut: string;
  dateSoumission: string;
  dateValidation: string | null;
  valableJusquA: string | null;
  carteEtudiantePresente: boolean;
  carteIdentitePresente: boolean;
  motifRefus: string | null;
  /** Verdict de l'analyse automatique : "CORRESPONDANCE" | "DIVERGENCE_MANIFESTE" | "INCERTAIN" | null (pas encore analysée / IA indisponible). */
  verdictIa: string | null;
  nomExtraitCarteEtudiante: string | null;
  nomExtraitCarteIdentite: string | null;
  /** True si le statut final a été décidé par l'IA, sans intervention d'un admin. */
  decisionAutomatique: boolean;
}

/** Type d'image d'une vérification étudiante */
export type TypeCarte = "etudiante" | "identite";
