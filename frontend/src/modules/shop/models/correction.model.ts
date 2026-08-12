/** Où en est l'analyse, telle que le serveur la publie pendant qu'il travaille. */
export interface EtapeAnalyse {
  pourcentage: number;
  libelle: string;
}

/** Les choix du client, transmis pour que l'aperçu montre ce qu'il recevra. */
export interface ChoixCorrection {
  fautesIgnorees: number[];
  remplacementsChoisis: Record<number, string>;
}

/** Une faute repérée dans le document, avec la correction proposée. */
export interface Faute {
  page: number;
  motFautif: string;
  correction: string | null;
  suggestions: string[] | null;
  message: string;
  contexte: string;
  corrigeeDansPdf: boolean;
}

/** Résultat de l'analyse d'un PDF : ce qui a été trouvé, et à quel prix. */
export interface Verification {
  id: number;
  nomFichier: string;
  nbPages: number;
  nbFautes: number;
  /** Langue reconnue dans le document (« français », « néerlandais », « anglais »). */
  langue?: string;
  prix: number;
  payee: boolean;
  nbCorrigees: number | null;
  fautes: Faute[] | null;
}

/**
 * Ce que le tunnel de commande doit retenir pour facturer et appliquer la
 * correction.
 *
 * Ce n'est pas une réponse du serveur mais un état partagé entre le composant
 * de correction et la page de commande : les fautes que le client a écartées et
 * les suggestions qu'il a préférées.
 */
export interface EtatCorrection {
  verification: Verification;
  active: boolean;
  fautesIgnorees: number[];
  /** Position de la faute → suggestion retenue, quand elle diffère de celle par défaut. */
  remplacementsChoisis: Record<number, string>;
}
