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

/** Formate une durée en minutes en texte lisible ("8 min", "1h20"). */
export function formatDuration(minutes: number): string {
  if (minutes < 1) return "< 1 min";
  if (minutes < 60) return `${minutes} min`;
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return m === 0 ? `${h}h` : `${h}h${m}`;
}

/**
 * Calcule un vrai temps de trajet en voiture via OSRM (routage gratuit basé sur
 * OpenStreetMap, sans clé) : suit les vraies routes plutôt qu'une estimation à
 * vol d'oiseau.
 */
export async function fetchDrivingDurationMin(
  from: { lat: number; lng: number },
  to: { lat: number; lng: number }
): Promise<number | null> {
  try {
    const url = `https://router.project-osrm.org/route/v1/driving/${from.lng},${from.lat};${to.lng},${to.lat}?overview=false`;
    const res = await fetch(url);
    const data = await res.json();
    const duration = data?.routes?.[0]?.duration;
    return typeof duration === "number" ? Math.round(duration / 60) : null;
  } catch {
    return null;
  }
}

/**
 * Calcule un vrai temps de trajet à pied via Valhalla (instance publique gratuite
 * de la communauté OpenStreetMap allemande, sans clé). Contrairement au profil
 * "à pied" d'OSRM (qui renvoie des vitesses de voiture sur le serveur public
 * officiel), Valhalla gère correctement le profil piéton.
 */
export async function fetchWalkingDurationMin(
  from: { lat: number; lng: number },
  to: { lat: number; lng: number }
): Promise<number | null> {
  try {
    const res = await fetch("https://valhalla1.openstreetmap.de/route", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        locations: [
          { lat: from.lat, lon: from.lng },
          { lat: to.lat, lon: to.lng },
        ],
        costing: "pedestrian",
      }),
    });
    const data = await res.json();
    const seconds = data?.trip?.summary?.time;
    return typeof seconds === "number" ? Math.round(seconds / 60) : null;
  } catch {
    return null;
  }
}