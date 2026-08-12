/**
 * Adresse du serveur, en un seul endroit.
 *
 * Elle était auparavant écrite en dur dans quatorze fichiers : un déploiement
 * demandait de tous les retrouver, et il suffisait d'en oublier un pour qu'une
 * partie de l'application continue d'interroger la machine du développeur.
 *
 * La valeur vient de VITE_API_URL. Le repli sur localhost garde le projet
 * utilisable sans configuration, comme c'était le cas jusqu'ici.
 */
export const SERVER_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

/** Base de l'API. Les fichiers y ajoutent leur propre chemin. */
export const API_URL = `${SERVER_URL}/api`;

/**
 * Clé publique Stripe, celle que le navigateur a le droit de connaître.
 *
 * Déclarée ici pour la même raison que l'adresse du serveur : elle était écrite
 * en dur dans la page d'inscription des partenaires, si bien que passer en
 * production aurait laissé cette page sur la clé de test — des paiements
 * fictifs, sans que rien ne le signale.
 */
export const STRIPE_PUBLIC_KEY = import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY ?? "";
