/**
 * Un message de la conversation, tel que l'API l'attend.
 *
 * À ne pas confondre avec le message affiché à l'écran, qui porte en plus un
 * identifiant et un rôle propres à l'interface.
 */
export interface MessageChat {
  role: "user" | "assistant";
  content: string;
}
