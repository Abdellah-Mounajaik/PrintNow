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