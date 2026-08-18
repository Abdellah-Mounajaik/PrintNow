/** Tarifs de la plateforme, modifiables par un administrateur. */
export interface ParametresPlateforme {
  commissionPourcentage: number;
  fraisInscription: number;
  prixCorrectionForfait: number;
  pagesInclusesCorrection: number;
  prixCorrectionPageSupp: number;
  prixGenerationDesign: number;
}
