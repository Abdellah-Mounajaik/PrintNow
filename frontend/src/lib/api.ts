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
