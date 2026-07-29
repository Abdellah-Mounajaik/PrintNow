/** Profil complet de l'utilisateur connecté */
export interface UserProfileDTO {
  id: number;
  email: string;
  prenom: string;
  nom: string;
  telephone: string | null;
  actif: boolean;
  roleNom: string;
}

/** Vérification étudiante du client connecté */
export interface VerifDTO {
  id: number;
  statut: string;
  dateSoumission: string;
  valableJusquA: string | null;
  motifRefus: string | null;
}

/** Commande telle qu'affichée dans l'espace client */
export interface CommandeDTO {
  id: number;
  numeroCommande: string;
  statut: string;
  modeRetrait: string;
  totalTTC: number;
  dateCreation: string;
  nomImprimerie: string;
}

/** Suivi de livraison bpost */
export interface SuiviDTO {
  commandeId: number;
  numeroSuivi: string | null;
  statut: string;
  statutAfterShipping: string | null;
  lienSuiviBpost: string | null;
}
