/**
 * Ce que l'API du studio renvoie, prêt à afficher.
 *
 * Ce ne sont pas les entités du serveur : pas de chemins de fichiers ici, juste
 * de quoi savoir si un aperçu / PDF est disponible et par quel endpoint le tirer.
 */
export interface PropositionStudio {
  id: number;
  gabaritCode: string;
  paletteCode: string;
  apercuDisponible: boolean;
  pdfDisponible: boolean;
}

export interface GenerationStudio {
  id: number;
  type: string;
  statut: string; // EN_COURS | PRETE | ECHOUEE
  suivi: string;
  propositions: PropositionStudio[];
}
