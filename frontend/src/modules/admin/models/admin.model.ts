export interface UserDTO {
  id: number;
  email: string;
  prenom: string;
  nom: string;
  telephone: string;
  actif: boolean;
  roleNom: string;
}

export interface ImprimerieDTO {
  id: number;
  nom: string;
  ville: string;
  emailContact: string;
  actif: boolean;
}

export interface CommandeDTO {
  id: number;
  numeroCommande: string;
  statut: string;
  totalTTC: number;
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
}

/** Type d'image d'une vérification étudiante */
export type TypeCarte = "etudiante" | "identite";
