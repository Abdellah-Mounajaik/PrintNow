/** Corps de POST /api/contact. */
export interface MessageContactRequest {
  nom: string;
  email: string;
  sujet: string;
  message: string;
}
