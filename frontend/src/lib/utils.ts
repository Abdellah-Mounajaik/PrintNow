import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

const API_HOST = "http://localhost:8080";

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