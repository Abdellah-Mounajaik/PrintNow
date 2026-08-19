import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"
import { SERVER_URL } from "./api"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

const API_HOST = SERVER_URL;

/**
 * Le backend renvoie des chemins relatifs pour les fichiers uploadés (ex: logos
 * d'imprimerie : "/uploads/logos/xxx.png"). On les préfixe par l'hôte du backend
 * pour que le navigateur les charge depuis le bon serveur (et non depuis le frontend).
 * Les URLs déjà absolues (http://, https://) sont laissées telles quelles.
 */
export function resolveFileUrl(path: string | null | undefined): string | undefined {
  if (!path) return undefined;
  if (path.startsWith("http://") || path.startsWith("https://")) return path;
  return `${API_HOST}${path}`;
}

/** Distance en kilomètres entre deux points GPS (formule de Haversine). */
export function haversineDistanceKm(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371; // rayon moyen de la Terre en km
  const toRad = (deg: number) => (deg * Math.PI) / 180;
  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) ** 2;
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

/**
 * Numéros de page à afficher dans une pagination, tronqués avec des "..." au
 * lieu de tout lister : au-delà d'une poignée de pages, un bouton par page
 * déborde et devient inutilisable (ex. 39 pages de commandes).
 *
 * Toujours la première, la dernière, et un voisinage autour de la page
 * courante — ex. pour (20, 39) : [1, "...", 19, 20, 21, "...", 39].
 */
export function getPaginationRange(page: number, totalPages: number, delta = 1): (number | "...")[] {
  if (totalPages <= 7) return Array.from({ length: totalPages }, (_, i) => i + 1);

  const range: (number | "...")[] = [1];
  const gauche = Math.max(2, page - delta);
  const droite = Math.min(totalPages - 1, page + delta);

  if (gauche > 2) range.push("...");
  for (let i = gauche; i <= droite; i++) range.push(i);
  if (droite < totalPages - 1) range.push("...");
  range.push(totalPages);

  return range;
}

/** Formate une durée en minutes en texte lisible ("8 min", "1h20"). */
export function formatDuration(minutes: number): string {
  if (minutes < 1) return "< 1 min";
  if (minutes < 60) return `${minutes} min`;
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return m === 0 ? `${h}h` : `${h}h${m}`;
}


