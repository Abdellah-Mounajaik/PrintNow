/**
 * Le catalogue tel qu'il s'affiche.
 *
 * Ce ne sont pas des DTO : le serveur ne renvoie rien de cette forme. Ces types
 * décrivent ce que les cartes et la vue carte ont besoin de montrer, une fois la
 * réponse de l'API transformée — horaires convertis en « ouvert », logo replié
 * sur une image par défaut, produits résumés en libellés de services.
 *
 * Ils vivent ici plutôt que dans un composant parce que trois d'entre eux se les
 * partagent, et que deux avaient fini par déclarer le même type chacun de leur côté.
 */

/** Ce qu'une imprimerie propose, réduit à ce qui sert au filtrage et à l'estimation. */
export interface PrintShopProduit {
  typeProduit: string;
  actif: boolean;
  prixBase: number | null;
}

/** Une imprimerie, prête à être affichée dans une carte du catalogue. */
export interface PrintShop {
  id: string;
  name: string;
  /** Adresse lisible de la fiche ; l'identifiant sert de repli. */
  slug?: string;
  address: string;
  distance: string;
  latitude: number | null;
  longitude: number | null;
  distanceKm?: number;
  rating: number;
  reviewCount: number;
  isOpen: boolean;
  openingHours: string;
  image: string;
  services: string[];
  produits: PrintShopProduit[];
  hasExpressOption: boolean;
  hasStudentDiscount: boolean;
  hasDelivery: boolean;
}

/** Temps de trajet réel : un nombre de minutes, en cours de calcul, ou indisponible. */
export type TravelTimeState = number | "loading" | "error";

/** Ce qu'un marqueur de la vue carte a besoin de connaître. */
export interface ShopMapPoint {
  id: string;
  name: string;
  /** Adresse lisible de la fiche ; l'identifiant sert de repli. */
  slug?: string;
  address: string;
  latitude: number | null;
  longitude: number | null;
  isOpen: boolean;
  distanceKm?: number;
}
