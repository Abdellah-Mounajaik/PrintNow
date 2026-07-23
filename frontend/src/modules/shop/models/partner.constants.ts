import { TypeProduit, FormatImpression } from "./partner.model";

export const DAYS = [
  { key: "mon", label: "Lundi" }, 
  { key: "tue", label: "Mardi" },
  { key: "wed", label: "Mercredi" }, 
  { key: "thu", label: "Jeudi" },
  { key: "fri", label: "Vendredi" }, 
  { key: "sat", label: "Samedi" },
  { key: "sun", label: "Dimanche" },
];

export const SERVICES = [
  { id: "bw-a4", name: "Impression N&B A4", typeProduit: TypeProduit.DOCUMENT, formatImpression: FormatImpression.A4, defaultPrice: "0.10", couleur: false },
  { id: "color-a4", name: "Impression couleur A4", typeProduit: TypeProduit.DOCUMENT, formatImpression: FormatImpression.A4, defaultPrice: "0.30", couleur: true },
  { id: "bw-a3", name: "Impression N&B A3", typeProduit: TypeProduit.DOCUMENT, formatImpression: FormatImpression.A3, defaultPrice: "0.50", couleur: false },
  { id: "color-a3", name: "Impression couleur A3", typeProduit: TypeProduit.DOCUMENT, formatImpression: FormatImpression.A3, defaultPrice: "0.80", couleur: true },
  { id: "business-cards", name: "Cartes de visite", typeProduit: TypeProduit.CARTE_VISITE, formatImpression: FormatImpression.CARTE_VISITE_85x55, defaultPrice: "25.00", couleur: true },
  { id: "flyers", name: "Flyers / Dépliants", typeProduit: TypeProduit.FLYER, formatImpression: FormatImpression.A5, defaultPrice: "0.15", couleur: true },
  { id: "posters", name: "Affiches grand format", typeProduit: TypeProduit.POSTER, formatImpression: FormatImpression.A2, defaultPrice: "8.00", couleur: true },
];

export type Hours = { open: string; close: string; closed: boolean };
export type ServiceState = { enabled: boolean; price: string };