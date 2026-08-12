/** Une adresse proposée pendant la saisie, déjà découpée en champs du formulaire. */
export interface SuggestionAdresse {
  /** Ce qui est montré dans la liste : « Rue de la Loi 12, 1000, Bruxelles ». */
  label: string;
  adresse: string;
  ville: string;
  pays: string;
}
