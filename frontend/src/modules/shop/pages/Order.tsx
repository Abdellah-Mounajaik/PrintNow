import { useState, useEffect } from "react";
import { useParams, Link, Navigate, useNavigate } from "react-router-dom";
import Header from "../../../components/layout/Header";
import { Button } from "../../../components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "../../../components/ui/card";
import { Label } from "../../../components/ui/label";
import { Input } from "../../../components/ui/input";
import { RadioGroup, RadioGroupItem } from "../../../components/ui/radio-group";
import { Checkbox } from "../../../components/ui/checkbox";
import { Badge } from "../../../components/ui/badge";
import { toast } from "../../../hooks/use-toast";
import { STRIPE_PUBLIC_KEY } from "../../../lib/api";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../../../components/ui/select";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "../../../components/ui/dialog";
import {
  ChevronLeft, Upload, FileText, Zap, GraduationCap, Truck,
  Store, CreditCard, AlertCircle, Tag, Check, MapPin,
  Lock, Loader2, Layers, Book, SpellCheck2, Sparkles
} from "lucide-react";
import { loadStripe } from "@stripe/stripe-js";
import { Elements, CardElement, useStripe, useElements } from "@stripe/react-stripe-js";
import * as pdfjsLib from "pdfjs-dist";
import pdfjsWorkerUrl from "pdfjs-dist/build/pdf.worker.min.mjs?url";

import { imprimerieService } from "../services/imprimerieService.service";
import { paiementService } from "../services/paiement.service";
import { commandeService } from "../services/commande.service";
import { fichierPdfService } from "../services/fichierPdf.service";
import { promoService } from "../services/promo.service";
import { correctionService } from "../services/correction.service";
import { userService } from "../../user/services/user.service";
import type { ImprimerieDetail } from "../models/Imprimerie.model";
import type { CommandeCreee, CommandeRequest, AdresseLivraisonRequest } from "../models/commande.model";
import type { EtatCorrection } from "../models/correction.model";
import { useAuth } from "../../auth/context/AuthContext";
import CorrectionOrthographe from "../components/CorrectionOrthographe";
import GenerateurBouton from "../../studio/components/GenerateurBouton";

pdfjsLib.GlobalWorkerOptions.workerSrc = pdfjsWorkerUrl;

const stripePromise = loadStripe(STRIPE_PUBLIC_KEY);

interface FileOptions {
  productId: number | "";
  copies: number;
  recto: "recto" | "rectoverso";
  binding: string;
  finish: string;
}

interface UploadedFile {
  file: File;
  pageCount: number;
  detectedFormatMm: { width: number; height: number } | null;
  options: FileOptions;
  /** Proposition du studio (design IA) utilisée : facturée au forfait, au bénéfice de PrintNow. */
  generationId?: number;
}

/** Forfait par design IA utilisé dans la commande — reversé à PrintNow, comme la correction. */
const PRIX_GENERATION = 4.9;

// Dimensions réelles (mm) des formats proposés — sert à détecter les
// incohérences entre le fichier déposé et le produit choisi (ex: un CV en
// format "Cartes de visite").
const FORMAT_DIMENSIONS_MM: Record<string, { width: number; height: number }> = {
  A6: { width: 105, height: 148 },
  A5: { width: 148, height: 210 },
  A4: { width: 210, height: 297 },
  A3: { width: 297, height: 420 },
  A2: { width: 420, height: 594 },
  A1: { width: 594, height: 841 },
  A0: { width: 841, height: 1189 },
  DL_10x21: { width: 100, height: 210 },
  CARTE_VISITE_85x55: { width: 85, height: 55 },
};

const FORMAT_TOLERANCE_MM = 5;

const formatMatchesDimensions = (
  formatKey: string,
  fileDims: { width: number; height: number }
): boolean => {
  const dims = FORMAT_DIMENSIONS_MM[formatKey];
  if (!dims) return true; // format inconnu : on ne bloque pas
  const matches = (w: number, h: number) =>
    Math.abs(fileDims.width - w) <= FORMAT_TOLERANCE_MM && Math.abs(fileDims.height - h) <= FORMAT_TOLERANCE_MM;
  // Les deux orientations (portrait/paysage) sont acceptées
  return matches(dims.width, dims.height) || matches(dims.height, dims.width);
};

/**
 * Format produit par le générateur IA pour chaque type de support. Sert à ne
 * proposer un type que si l'imprimerie a un produit actif dans ce format.
 */
const STUDIO_TYPES_FORMAT: Record<string, { width: number; height: number }> = {
  CV: FORMAT_DIMENSIONS_MM.A4,
  FLYER: FORMAT_DIMENSIONS_MM.A5,
  CARTE_VISITE: FORMAT_DIMENSIONS_MM.CARTE_VISITE_85x55,
};

/**
 * Le fichier correspond-il à l'un des formats que nous savons reconnaître ?
 *
 * Distinction indispensable : un A4 déposé chez une imprimerie qui ne fait que
 * des cartes de visite doit être refusé, alors qu'un format inhabituel — une
 * affiche carrée, par exemple — ne peut pas être jugé et ne doit donc pas
 * bloquer la commande.
 */
const formatReconnu = (fileDims: { width: number; height: number }): boolean =>
  Object.keys(FORMAT_DIMENSIONS_MM).some((cle) => formatMatchesDimensions(cle, fileDims));



interface OrderConfirmation {
  numeroCommande: string;
  numeroSuivi?: string;
  statutPaiement: "SUCCES";
  statutLivraison?: "EN_PREPARATION";
  modeRetrait: "RETRAIT_MAGASIN" | "LIVRAISON";
  /** Impression seule. */
  total: number;
  /** Vérification orthographique, facturée en plus de l'impression. */
  correction: number;
  /** Designs IA (studio), facturés en plus de l'impression. */
  generation: number;
  /**
   * Fichiers que le serveur n'a pas pu recevoir malgré le paiement. Tant qu'il
   * y en a, la commande ne peut pas être imprimée et il serait trompeur de
   * l'annoncer comme confirmée.
   */
  fichiersNonEnvoyes: string[];
}

// ──────────────────────────────────────────────────────────────────────────────
// Stripe checkout sub-component (must live inside <Elements>)
// ──────────────────────────────────────────────────────────────────────────────
interface CheckoutFormProps {
  total: number;
  canPay: boolean;
  addressValid: boolean;
  fulfillment: "pickup" | "delivery";
  token: string | null;
  /** Contrôle serveur des formats, effectué avant tout débit. */
  verifierFormats: () => Promise<void>;
  onSuccess: (paymentIntentId: string) => Promise<void>;
}

const CheckoutForm: React.FC<CheckoutFormProps> = ({
  total, canPay, addressValid, fulfillment, token, verifierFormats, onSuccess,
}) => {
  const stripe = useStripe();
  const elements = useElements();
  const [processing, setProcessing] = useState(false);
  const [cardComplete, setCardComplete] = useState(false);
  const [cardError, setCardError] = useState<string | null>(null);

  const handlePay = async () => {
    if (!stripe || !elements) {
      toast({ title: "Erreur", description: "Stripe n'est pas chargé. Vérifiez votre clé publique.", variant: "destructive" });
      return;
    }
    // La page entière est déjà réservée aux clients connectés ; ce contrôle
    // n'existe que pour que le jeton soit certainement présent ici.
    if (!token) {
      toast({ title: "Session expirée", description: "Reconnectez-vous pour finaliser votre commande.", variant: "destructive" });
      return;
    }
    if (!canPay) {
      toast({
        title: "Commande incomplète",
        description: "Ajoutez au moins un PDF, et vérifiez que son format correspond bien "
          + "à un produit proposé par cette imprimerie.",
        variant: "destructive",
      });
      return;
    }
    if (fulfillment === "delivery" && !addressValid) {
      toast({ title: "Adresse incomplète", description: "Veuillez renseigner votre adresse de livraison.", variant: "destructive" });
      return;
    }
    if (!cardComplete) {
      toast({ title: "Carte invalide", description: cardError ?? "Veuillez entrer un numéro de carte valide.", variant: "destructive" });
      return;
    }

    setProcessing(true);
    try {
      // 0. Le serveur mesure lui-même chaque PDF : l'imprimeur ne doit jamais
      // recevoir une commande payée qu'il ne pourra pas honorer, et le contrôle
      // du navigateur ne suffit pas à le garantir.
      await verifierFormats();

      // 1. Create PaymentIntent on the backend
      const clientSecret = await paiementService.creerIntention(total, token);

      // 2. Confirm with real card via Stripe.js
      const cardElement = elements.getElement(CardElement);
      if (!cardElement) throw new Error("Élément de carte introuvable.");

      const { error, paymentIntent } = await stripe.confirmCardPayment(clientSecret, {
        payment_method: { card: cardElement },
      });
      if (error) throw new Error(error.message ?? "Erreur de paiement.");
      if (!paymentIntent) throw new Error("Paiement non confirmé.");

      // 3. Create the order only after successful payment
      await onSuccess(paymentIntent.id);
    } catch (err) {
      const message = err instanceof Error ? err.message : "Une erreur est survenue.";
      // Le titre suit ce qui a échoué : parler d'« erreur de paiement » alors
      // que la carte a bien été débitée inquiéterait pour rien.
      const paiementPasse = /rembours|enregistr/i.test(message);
      toast({
        title: paiementPasse ? "Commande non enregistrée" : "Erreur de paiement",
        description: message,
        variant: "destructive",
      });
    } finally {
      setProcessing(false);
    }
  };

  return (
    <div className="space-y-4">
      <div className="p-3 border rounded-lg bg-background">
        <CardElement
          options={{
            hidePostalCode: true,
            style: {
              base: {
                fontSize: "16px",
                color: "#1a1a1a",
                "::placeholder": { color: "#a0a0a0" },
              },
              invalid: {
                color: "#1a1a1a",
                iconColor: "#a0a0a0",
              },
            },
          }}
          onChange={(e) => {
            setCardComplete(e.complete);
            setCardError(e.error?.message ?? null);
          }}
        />
      </div>
      <div className="flex items-center gap-2 text-xs text-muted-foreground">
        <Lock className="h-3.5 w-3.5" /> Paiement sécurisé Stripe (Chiffrement 256 bits).
      </div>
      <Button
        variant="hero"
        size="lg"
        className="w-full"
        disabled={!canPay || processing || !stripe}
        onClick={handlePay}
      >
        {processing ? (
          <><Loader2 className="h-5 w-5 mr-2 animate-spin" /> Traitement…</>
        ) : (
          <><CreditCard className="h-5 w-5 mr-2" /> Payer {total.toFixed(2)}€</>
        )}
      </Button>
    </div>
  );
};

// ──────────────────────────────────────────────────────────────────────────────
// Order page
// ──────────────────────────────────────────────────────────────────────────────
const Order = () => {
  const { slug } = useParams<{ slug: string }>();
  const { token, user } = useAuth();
  const navigate = useNavigate();

  if (!token) return <Navigate to="/login" replace />;
  if (user?.role === "ROLE_IMPRIMERIE") return <Navigate to="/dashboard-imprimeur" replace />;

  const [shop, setShop] = useState<ImprimerieDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const [files, setFiles] = useState<UploadedFile[]>([]);
  const [expressOption, setExpressOption] = useState(false);
  const [studentDiscount, setStudentDiscount] = useState(false);
  const [studentVerified, setStudentVerified] = useState(false);
  const [fulfillment, setFulfillment] = useState<"pickup" | "delivery">("pickup");
  const [promoInput, setPromoInput] = useState("");
  const [promoError, setPromoError] = useState<string | null>(null);
  const [appliedPromo, setAppliedPromo] = useState<{ code: string; typeReduction: string; valeurReduction: number; montantMinimum?: number } | null>(null);

  const [address, setAddress] = useState<AdresseLivraisonRequest>({
    nomDestinataire: "", rue: "", numero: "", codePostal: "", ville: "", pays: "Belgique", telephone: "",
  });

  const [confirmation, setConfirmation] = useState<OrderConfirmation | null>(null);

  useEffect(() => {
    if (slug) {
      // Le serveur accepte aussi un numéro : les liens /commander/31 partagés
      // avant la mise en place des adresses lisibles continuent de fonctionner.
      imprimerieService.getImprimerieBySlug(slug)
        .then(data => {
          setShop(data);
          if (!data.livraisonActive) setFulfillment("pickup");
          setIsLoading(false);
        })
        .catch(() => {
          toast({ title: "Erreur", description: "Imprimerie introuvable.", variant: "destructive" });
          setIsLoading(false);
        });
    } else {
      setIsLoading(false);
    }
  }, [slug]);

  useEffect(() => {
    if (!token) return;
    // Le service du module « user » couvrait déjà cet appel : la page le
    // refaisait à la main.
    userService.getMaVerification(token).then(verification => {
      if (verification && verification.statut === "ACCEPTE" && verification.valableJusquA
          && new Date(verification.valableJusquA) > new Date()) {
        setStudentVerified(true);
      }
    }).catch(() => {});
  }, [token]);

  const formatEnumName = (text: string) => {
    if (!text) return "";
    const formatted = text.replace(/_/g, " ").toLowerCase();
    return formatted.charAt(0).toUpperCase() + formatted.slice(1);
  };

  const getProductLabel = (p: { typeProduit: string; formatImpression: string; couleur?: boolean }): string => {
    if (p.typeProduit === "DOCUMENT") {
      return `${p.couleur ? "Couleur" : "N&B"} ${p.formatImpression}`;
    }
    if (p.typeProduit === "CARTE_VISITE") return "Cartes de visite";
    if (p.typeProduit === "FLYER") return "Flyers / Dépliants";
    if (p.typeProduit === "POSTER") return "Affiches grand format";
    return `${formatEnumName(p.typeProduit)} ${p.formatImpression}`;
  };

  // Repli si PDF.js n'arrive pas du tout à ouvrir le fichier (corrompu,
  // chiffré...) : compte grossièrement les pages par recherche de texte.
  const countPdfPagesFallback = async (file: File): Promise<number> => {
    return new Promise((resolve) => {
      const reader = new FileReader();
      reader.onload = (e) => {
        const text = e.target?.result as string;
        const matches = text.match(/\/Type\s*\/Page[^s]/g);
        const pageCount = matches ? matches.length : 1;
        resolve(Math.max(1, pageCount));
      };
      reader.onerror = () => resolve(1);
      reader.readAsText(file);
    });
  };

  const PT_TO_MM = 25.4 / 72;

  // Lit le vrai nombre de pages et les dimensions réelles (mm) de la première
  // page via PDF.js, qui gère correctement les flux compressés (contrairement
  // à une simple recherche de texte dans le fichier) — renvoie formatMm à
  // null si la lecture échoue, pour ne rien bloquer dans ce cas.
  const readPdfInfo = async (
    file: File
  ): Promise<{ pageCount: number; formatMm: { width: number; height: number } | null }> => {
    try {
      const arrayBuffer = await file.arrayBuffer();
      const pdf = await pdfjsLib.getDocument({ data: arrayBuffer }).promise;
      const page = await pdf.getPage(1);
      const viewport = page.getViewport({ scale: 1 });
      return {
        pageCount: pdf.numPages,
        formatMm: { width: viewport.width * PT_TO_MM, height: viewport.height * PT_TO_MM },
      };
    } catch {
      return { pageCount: await countPdfPagesFallback(file), formatMm: null };
    }
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFiles = e.target.files;
    if (selectedFiles) {
      const newFiles: UploadedFile[] = [];
      for (let i = 0; i < selectedFiles.length; i++) {
        const file = selectedFiles[i];
        if (file.type === "application/pdf") {
          const { pageCount, formatMm: detectedFormatMm } = await readPdfInfo(file);
          const compatibleProduct = detectedFormatMm
            ? activeProducts.find((p) => formatMatchesDimensions(p.formatImpression, detectedFormatMm))
            : null;
          const defaultProduct = compatibleProduct || shop?.produits?.find(p => p.actif) || null;
          newFiles.push({
            file,
            pageCount,
            detectedFormatMm,
            options: {
              productId: defaultProduct ? defaultProduct.id : "",
              copies: 1,
              recto: "recto",
              binding: "AUCUNE",
              finish: "AUCUNE",
            },
          });
        }
      }
      setFiles((prev) => [...prev, ...newFiles]);
    }
    e.target.value = "";
  };

  // Un support généré par l'IA rejoint la commande comme un fichier téléversé
  // normal : même détection de pages/format, mêmes options, même tunnel.
  const ajouterFichierGenere = async (file: File, generationId?: number) => {
    const { pageCount, formatMm: detectedFormatMm } = await readPdfInfo(file);
    const compatibleProduct = detectedFormatMm
      ? activeProducts.find((p) => formatMatchesDimensions(p.formatImpression, detectedFormatMm))
      : null;
    const defaultProduct = compatibleProduct || shop?.produits?.find((p) => p.actif) || null;
    setFiles((prev) => [...prev, {
      file,
      pageCount,
      detectedFormatMm,
      generationId,
      options: {
        productId: defaultProduct ? defaultProduct.id : "",
        copies: 1,
        recto: "recto",
        binding: "AUCUNE",
        finish: "AUCUNE",
      },
    }]);
  };

  const removeFile = (index: number) => setFiles((prev) => prev.filter((_, i) => i !== index));

  /**
   * Vérification orthographique de chaque fichier, indexée comme `files`.
   * L'analyse est gratuite ; seule la production du PDF corrigé est facturée,
   * en même temps que la commande.
   */
  const [corrections, setCorrections] = useState<Record<number, EtatCorrection | null>>({});

  const majCorrection = (index: number, etat: EtatCorrection | null) => {
    setCorrections((prev) => ({ ...prev, [index]: etat }));
  };

  /** Corrections effectivement retenues par le client, avec leur position de fichier. */
  const correctionsActives = Object.entries(corrections)
    .filter(([, etat]) => etat?.active)
    .map(([index, etat]) => ({ index: Number(index), etat: etat as EtatCorrection }));

  const totalCorrections = correctionsActives.reduce((s, c) => s + c.etat.verification.prix, 0);

  /** Designs IA encore présents dans la commande (dédupliqués) : chacun facturé au forfait. */
  const generationsActives = [...new Set(
    files.filter((f) => f.generationId != null).map((f) => f.generationId as number)
  )];
  const totalGenerations = generationsActives.length * PRIX_GENERATION;

  const updateFileOption = <K extends keyof FileOptions>(index: number, key: K, value: FileOptions[K]) => {
    setFiles((prev) =>
      prev.map((f, i) => {
        if (i !== index) return f;
        const updatedOptions = { ...f.options, [key]: value };
        if (key === "productId") {
          updatedOptions.binding = "AUCUNE";
          updatedOptions.finish = "AUCUNE";
          // Le recto-verso n'a aucun sens pour une affiche (personne ne voit le verso)
          const newProduct = activeProducts.find((p) => p.id === value);
          if (newProduct?.typeProduit === "POSTER") {
            updatedOptions.recto = "recto";
          }
        }
        return { ...f, options: updatedOptions };
      })
    );
  };

  const computeFilePrice = (f: UploadedFile) => {
    if (!f.options.productId || !shop || !shop.produits) return 0;
    const product = shop.produits.find(p => p.id === f.options.productId);
    if (!product) return 0;

    let unitPrice = product.prixBase * f.pageCount;
    if (f.options.recto === "rectoverso") {
      const tauxRV = shop.pourcentageRemiseRectoVerso ?? 15;
      unitPrice = Math.max(0, unitPrice - unitPrice * (tauxRV / 100));
    }
    if (f.options.binding !== "AUCUNE" && product.prixParTypeReliure) {
      unitPrice += Number(product.prixParTypeReliure[f.options.binding] || 0);
    }
    if (f.options.finish !== "AUCUNE" && product.prixParTypePlastification) {
      unitPrice += Number(product.prixParTypePlastification[f.options.finish] || 0);
    }
    return unitPrice * f.options.copies;
  };

  const subtotal = files.reduce((sum, f) => sum + computeFilePrice(f), 0);
  const expressAmount = expressOption ? (shop?.prixExpress2h ?? 5) : 0;
  const deliveryPrice = fulfillment === "delivery" ? (shop?.prixLivraison ?? 4.99) : 0;
  const studentDiscountAmount = (studentDiscount && shop?.pourcentageRemiseEtudiant)
    ? subtotal * (shop.pourcentageRemiseEtudiant / 100) : 0;
  const totalAvantPromo = subtotal + expressAmount + deliveryPrice - studentDiscountAmount;
  const promoDiscountAmount = appliedPromo
    ? (appliedPromo.typeReduction === "POURCENTAGE"
        ? subtotal * (appliedPromo.valeurReduction / 100)
        : appliedPromo.valeurReduction)
    : 0;
  const totalHT = Math.max(0, totalAvantPromo - promoDiscountAmount);
  const tva = totalHT * 0.21;
  const total = totalHT + tva;
  // Les corrections et les designs IA sont des services PrintNow : ils s'ajoutent
  // au règlement mais restent hors du total de la commande (et donc hors de la
  // part imprimeur et de l'assiette de commission).
  const totalAPayer = total + totalCorrections + totalGenerations;

  useEffect(() => {
    if (appliedPromo?.montantMinimum && totalAvantPromo * 1.21 < appliedPromo.montantMinimum) {
      setAppliedPromo(null);
      toast({ title: "Code promo retiré", description: `Montant minimum de ${appliedPromo.montantMinimum.toFixed(2)}€ requis.`, variant: "destructive" });
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [totalAvantPromo]);

  const applyPromo = async () => {
    const code = promoInput.trim().toUpperCase();
    if (!code) return;
    setPromoError(null);
    try {
      const totalTTCAvantPromo = totalAvantPromo * 1.21;
      const promo = await promoService.valider(code, totalTTCAvantPromo, shop?.id, token);
      setAppliedPromo({
        code: promo.code,
        typeReduction: promo.typeReduction,
        valeurReduction: Number(promo.valeurReduction),
        montantMinimum: promo.montantMinimumCommande ? Number(promo.montantMinimumCommande) : undefined,
      });
      setPromoError(null);
      toast({ title: "Code appliqué !", description: promo.typeReduction === "POURCENTAGE" ? `Réduction de ${promo.valeurReduction}%` : `Réduction de ${Number(promo.valeurReduction).toFixed(2)}€` });
    } catch (e) {
      // Le serveur dit pourquoi il refuse (code expiré, mauvaise imprimerie,
      // montant minimum…) : c'est cette phrase qui aide, pas un message générique.
      setPromoError(e instanceof Error ? e.message : "Impossible de vérifier le code.");
    }
  };

  const removePromo = () => {
    setAppliedPromo(null);
    setPromoInput("");
  };

  const totalPages = files.reduce((s, f) => s + f.pageCount * f.options.copies, 0);

  const isAddressValid = () =>
    !!(address.nomDestinataire.trim() && address.rue.trim() && address.numero.trim() &&
      address.codePostal.trim() && address.ville.trim() && address.telephone.trim());

  const isExpressAvailable = (): boolean => {
    if (!shop?.horaires) return false;
    const JOURS = ["DIMANCHE", "LUNDI", "MARDI", "MERCREDI", "JEUDI", "VENDREDI", "SAMEDI"];
    const now = new Date();
    const jourAujourdhui = JOURS[now.getDay()];
    const horaire = shop.horaires.find(h => h.jourSemaine === jourAujourdhui);
    if (!horaire || horaire.ferme) return false;
    const toMinutes = (t: string) => { const [h, m] = t.split(":").map(Number); return h * 60 + m; };
    const maintenant = now.getHours() * 60 + now.getMinutes();
    const ouverture = toMinutes(horaire.heureOuverture);
    const fermeture = toMinutes(horaire.heureFermeture);
    return maintenant >= ouverture && fermeture - maintenant >= 120;
  };

  // Called after Stripe payment is confirmed — creates the commande and uploads PDFs
  /**
   * Réclame le remboursement d'un paiement dont la commande n'a pas pu être
   * enregistrée. Le serveur vérifie lui-même qu'aucune commande n'y correspond.
   *
   * L'échec de cette réclamation n'est pas propagé : elle n'est qu'un secours,
   * et c'est l'échec de la commande qui doit être annoncé au client.
   */
  const reclamerLeRemboursement = (paymentIntentId: string) =>
    paiementService.abandonner(paymentIntentId, token);

  const handleCreateOrder = async (paymentIntentId: string) => {
    const payload: CommandeRequest = {
      paymentIntentId,
      modeRetrait: fulfillment === "pickup" ? "RETRAIT_MAGASIN" : "LIVRAISON",
      express2h: expressOption,
      tarifEtudiant: studentDiscount && studentVerified,
      codePromo: appliedPromo?.code ?? null,
      adresseLivraison: fulfillment === "delivery" ? {
        nomDestinataire: address.nomDestinataire,
        rue: address.rue,
        numero: address.numero,
        codePostal: address.codePostal,
        ville: address.ville,
        pays: address.pays,
        telephone: address.telephone,
      } : undefined,
      lignes: files.map(f => ({
        produitId: f.options.productId,
        quantite: f.options.copies,
        nbPages: f.pageCount,
        couleur: !!shop?.produits?.find(p => p.id === f.options.productId)?.couleur,
        rectoVerso: f.options.recto === "rectoverso",
        reliure: f.options.binding || "AUCUNE",
        finition: f.options.finish || "AUCUNE",
      })),
      corrections: correctionsActives.map(({ etat }) => ({
        verificationId: etat.verification.id,
        fautesIgnorees: etat.fautesIgnorees,
        remplacementsChoisis: etat.remplacementsChoisis,
      })),
      generations: generationsActives,
    };

    // La carte est déjà débitée : plutôt que d'abandonner à la première coupure,
    // on réessaie, et on ne renonce qu'après avoir récupéré l'argent du client.
    // Le service distingue un refus définitif (déjà remboursé par le serveur)
    // d'une panne passagère.
    let data: CommandeCreee;
    for (let tentative = 1; ; tentative++) {
      try {
        data = await commandeService.passerCommande(payload, token);
        break;
      } catch (e) {
        const definitif = (e as Error & { definitif?: boolean }).definitif;
        if (!definitif && tentative < 3) {
          await new Promise((r) => setTimeout(r, tentative * 1000));
          continue;
        }
        if (!definitif) {
          // Panne côté serveur : personne là-bas n'a pu rembourser, on le
          // réclame nous-mêmes avant d'annoncer l'échec au client.
          const rembourse = await reclamerLeRemboursement(paymentIntentId);
          (e as Error).message = rembourse
            ? "Votre commande n'a pas pu être enregistrée. Votre paiement a été remboursé, rien ne vous sera prélevé."
            : "Votre commande n'a pas pu être enregistrée. Contactez-nous : votre paiement vous sera remboursé.";
        }
        throw e;
      }
    }

    const lignes = data.lignes ?? [];
    const uploadResults = await Promise.allSettled(
      files.map(async (uploadedFile, i) => {
        const ligneId = lignes[i]?.id;
        if (!ligneId) throw new Error(`Ligne ${i} introuvable`);

        // Si une correction a été réglée pour ce fichier, c'est la version
        // corrigée — générée par le serveur au moment du paiement — qui est
        // transmise à l'imprimerie. En cas d'échec, on imprime l'original
        // plutôt que de bloquer la commande.
        let aEnvoyer = uploadedFile.file;
        const correction = corrections[i];
        if (correction?.active) {
          const corrige = await correctionService.telechargerPdfCorrige(
            correction.verification.id, uploadedFile.file.name, token);
          if (corrige) aEnvoyer = corrige;
        }

        const envoyer = () =>
          fichierPdfService.envoyer(aEnvoyer, ligneId, uploadedFile.pageCount, token);

        // Le paiement est déjà encaissé : plutôt que d'abandonner à la première
        // coupure, on réessaie brièvement avant de renoncer.
        for (let tentative = 1; ; tentative++) {
          try {
            return await envoyer();
          } catch (e) {
            const definitif = (e as Error & { definitif?: boolean }).definitif;
            if (definitif || tentative >= 3) throw e;
            await new Promise((r) => setTimeout(r, tentative * 1000));
          }
        }
      })
    );

    // Les fichiers que l'imprimeur ne recevra pas : la commande est payée, mais
    // elle ne peut pas être honorée en l'état. Le client doit l'apprendre ici,
    // pas en constatant plus tard que rien n'a été imprimé.
    const fichiersNonEnvoyes = uploadResults
      .map((resultat, i) => (resultat.status === "rejected" ? files[i].file.name : null))
      .filter((nom): nom is string => nom !== null);

    if (fichiersNonEnvoyes.length > 0) {
      console.error("Erreurs upload PDF:", uploadResults.filter((r) => r.status === "rejected"));
    }

    setConfirmation({
      numeroCommande: data.numeroCommande,
      numeroSuivi: data.numeroSuivi ?? undefined,
      statutPaiement: "SUCCES",
      statutLivraison: fulfillment === "delivery" ? "EN_PREPARATION" : undefined,
      modeRetrait: fulfillment === "delivery" ? "LIVRAISON" : "RETRAIT_MAGASIN",
      total: Number(data.totalTTC),
      // La vérification orthographique est facturée à part de l'impression :
      // sans elle, le total affiché ne correspondrait pas au montant débité.
      correction: Number(data.montantCorrections ?? 0),
      generation: Number(data.montantGenerations ?? 0),
      fichiersNonEnvoyes,
    });
  };

  // La commande est déjà passée à ce stade : fermer la modale doit renvoyer
  // le client vers son espace plutôt que de le laisser sur le formulaire de commande.
  const handleFermerConfirmation = () => {
    setConfirmation(null);
    navigate("/dashboard");
  };

  if (isLoading) return <div className="min-h-screen flex items-center justify-center bg-muted/30"><Loader2 className="animate-spin h-8 w-8 text-primary" /></div>;
  if (!shop) return <div className="min-h-screen flex flex-col items-center justify-center bg-muted/30 text-destructive"><p className="text-xl font-bold">Imprimerie introuvable.</p><Button className="mt-4" asChild><Link to="/">Retour à l'accueil</Link></Button></div>;

  const activeProducts = shop.produits?.filter(p => p.actif) || [];

  // Types du générateur IA que cette imprimerie peut imprimer : un type n'est
  // proposé que si un produit actif existe dans le format qu'il produit.
  const typesGenerablesDisponibles = Object.entries(STUDIO_TYPES_FORMAT)
    .filter(([, dims]) => activeProducts.some((p) => formatMatchesDimensions(p.formatImpression, dims)))
    .map(([type]) => type);

  /**
   * Le fichier peut-il être imprimé tel quel par cette imprimerie ?
   *
   * Un format que nous savons reconnaître doit trouver un produit à sa mesure ;
   * un format inhabituel échappe au contrôle, faute de pouvoir en juger.
   */
  const fichierImprimable = (fichier: UploadedFile) => {
    const dims = fichier.detectedFormatMm;
    if (!dims || !formatReconnu(dims)) return true;

    const produit = activeProducts.find((p) => p.id === fichier.options.productId);
    return !!produit && formatMatchesDimensions(produit.formatImpression, dims);
  };

  // On ne laisse pas payer une commande qu'aucune imprimerie ne pourra honorer.
  const commandeImprimable = files.length > 0 && files.every(fichierImprimable);

  /**
   * Fait confirmer par le serveur que chaque fichier correspond bien au produit
   * choisi, avant tout débit. Le navigateur a déjà fait ce contrôle, mais lui
   * seul ne prouve rien : c'est la mesure du serveur qui fait foi.
   */
  const verifierFormats = async () => {
    for (const fichier of files) {
      await fichierPdfService.verifierFormat(fichier.file, fichier.options.productId, token);
    }
  };

  return (
    <div className="min-h-screen flex flex-col bg-muted/30">
      <Header />

      <main className="flex-1 pt-24 pb-16">
        <div className="container mx-auto px-4 max-w-6xl">
          <Button variant="ghost" size="sm" className="mb-6" asChild>
            <Link to={`/imprimerie/${slug}`}>
              <ChevronLeft className="h-4 w-4 mr-1" />
              Retour à l'imprimerie
            </Link>
          </Button>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
            <div className="lg:col-span-2 space-y-6">
              <div>
                <h1 className="font-display text-2xl md:text-3xl font-bold text-foreground mb-2">Nouvelle commande</h1>
                <p className="text-muted-foreground">{shop.nom} • {shop.ville}</p>
              </div>

              {/* Step 1: Upload */}
              <Card className="shadow-card">
                <CardHeader>
                  <CardTitle className="font-display text-lg flex items-center gap-2">
                    <span className="w-7 h-7 rounded-full bg-primary text-primary-foreground text-sm flex items-center justify-center">1</span>
                    Téléverser vos fichiers
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="border-2 border-dashed rounded-xl p-8 text-center transition-colors border-border hover:border-primary/50">
                    <input type="file" accept=".pdf" multiple onChange={handleFileChange} className="hidden" id="file-upload" />
                    <label htmlFor="file-upload" className="cursor-pointer">
                      <div className="flex flex-col items-center gap-3">
                        <div className="w-14 h-14 rounded-xl bg-muted flex items-center justify-center">
                          <Upload className="h-7 w-7 text-muted-foreground" />
                        </div>
                        <div>
                          <p className="font-medium text-foreground">Cliquez pour téléverser</p>
                          <p className="text-sm text-muted-foreground">Format PDF uniquement (plusieurs possibles)</p>
                        </div>
                      </div>
                    </label>
                  </div>

                  {/* Pas de fichier ? Le générer avec l'IA — seulement pour les types que
                      cette imprimerie sait imprimer. */}
                  {typesGenerablesDisponibles.length > 0 && (
                    <>
                      <div className="flex items-center gap-3 my-4">
                        <div className="h-px flex-1 bg-border" />
                        <span className="text-xs text-muted-foreground">ou</span>
                        <div className="h-px flex-1 bg-border" />
                      </div>
                      <GenerateurBouton
                        onFichierGenere={ajouterFichierGenere}
                        typesDisponibles={typesGenerablesDisponibles}
                      />
                    </>
                  )}

                  {files.length === 0 && (
                    <div className="flex items-center gap-2 mt-4 text-sm text-muted-foreground">
                      <AlertCircle className="h-4 w-4" /> Vos fichiers doivent être au format PDF
                    </div>
                  )}
                </CardContent>
              </Card>

              {/* Per-file options */}
              {files.map((uploadedFile, index) => {
                const selectedProduct = activeProducts.find(p => p.id === uploadedFile.options.productId);
                const fileDims = uploadedFile.detectedFormatMm;
                const anyCompatible = !fileDims || activeProducts.some((p) => formatMatchesDimensions(p.formatImpression, fileDims));
                // Aucun produit ne convient, mais le format du fichier est bien
                // identifié : l'imprimerie ne sait tout simplement pas l'imprimer.
                const formatNonProposé = !!fileDims && !anyCompatible && formatReconnu(fileDims);
                // Format inhabituel, que nous ne savons pas juger : on laisse
                // choisir plutôt que d'empêcher une commande légitime.
                const formatIndeterminé = !!fileDims && !anyCompatible && !formatReconnu(fileDims);
                return (
                  <Card key={index} className="shadow-card overflow-visible">
                    <CardHeader>
                      <div className="flex items-start justify-between gap-3">
                        <div className="flex items-center gap-3 min-w-0">
                          <div className="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center shrink-0">
                            <FileText className="h-5 w-5 text-primary" />
                          </div>
                          <div className="min-w-0">
                            <CardTitle className="font-display text-base truncate" title={uploadedFile.file.name}>
                              {uploadedFile.file.name}
                            </CardTitle>
                            <p className="text-xs text-muted-foreground mt-1">
                              {uploadedFile.pageCount} page{uploadedFile.pageCount > 1 ? "s" : ""} • {(uploadedFile.file.size / 1024 / 1024).toFixed(2)} MB
                            </p>
                          </div>
                        </div>
                        <Button variant="ghost" size="sm" onClick={() => removeFile(index)} className="text-destructive hover:bg-destructive/10">✕</Button>
                      </div>
                    </CardHeader>
                    <CardContent className="space-y-5">
                      <div className="space-y-2">
                        <Label className="text-sm font-semibold text-primary">Que souhaitez-vous imprimer ?</Label>
                        <Select value={uploadedFile.options.productId.toString()} onValueChange={(v) => updateFileOption(index, "productId", Number(v))}>
                          <SelectTrigger className="border-primary/30 bg-primary/5">
                            <SelectValue placeholder="Sélectionnez un produit..." />
                          </SelectTrigger>
                          <SelectContent>
                            {activeProducts.map((p) => {
                              const dims = FORMAT_DIMENSIONS_MM[p.formatImpression];
                              const compatible = !fileDims || formatIndeterminé
                                || formatMatchesDimensions(p.formatImpression, fileDims);
                              return (
                                <SelectItem key={p.id} value={p.id.toString()} disabled={!compatible}>
                                  {getProductLabel(p)}{" "}
                                  {dims && (
                                    <span className="text-muted-foreground ml-1">({dims.width}×{dims.height} mm)</span>
                                  )}{" "}
                                  <span className="text-muted-foreground ml-2">({p.prixBase.toFixed(2)}€/page)</span>
                                  {!compatible && (
                                    <span className="text-destructive ml-2 text-xs">Format incompatible avec votre fichier</span>
                                  )}
                                </SelectItem>
                              );
                            })}
                          </SelectContent>
                        </Select>
                        {formatNonProposé && fileDims && (
                          <p className="flex items-start gap-1.5 text-xs text-destructive">
                            <AlertCircle className="h-3.5 w-3.5 shrink-0 mt-0.5" />
                            <span>
                              Cette imprimerie ne propose aucun format correspondant à votre fichier
                              ({Math.round(fileDims.width)}×{Math.round(fileDims.height)} mm).
                              Retirez-le, ou choisissez une autre imprimerie.
                            </span>
                          </p>
                        )}
                        {fileDims && selectedProduct && anyCompatible && !formatMatchesDimensions(selectedProduct.formatImpression, fileDims) && (
                          <p className="flex items-center gap-1.5 text-xs text-destructive">
                            <AlertCircle className="h-3.5 w-3.5" />
                            Le format de ce produit ne correspond pas aux dimensions de votre fichier ({Math.round(fileDims.width)}×{Math.round(fileDims.height)} mm).
                          </p>
                        )}
                      </div>

                      <div className={`grid gap-4 ${selectedProduct?.typeProduit === "POSTER" ? "grid-cols-1" : "grid-cols-2"}`}>
                        {selectedProduct?.typeProduit !== "POSTER" && (
                          <div className="space-y-3">
                            <Label className="text-sm">Mise en page</Label>
                            <RadioGroup value={uploadedFile.options.recto} onValueChange={(v) => updateFileOption(index, "recto", v as "recto" | "rectoverso")} className="flex gap-4">
                              <div className="flex items-center space-x-2"><RadioGroupItem value="recto" id={`recto-${index}`} /><Label htmlFor={`recto-${index}`} className="cursor-pointer">Recto</Label></div>
                              <div className="flex items-center space-x-2"><RadioGroupItem value="rectoverso" id={`rv-${index}`} /><Label htmlFor={`rv-${index}`} className="cursor-pointer">Recto-Verso (-{shop?.pourcentageRemiseRectoVerso ?? 15}%)</Label></div>
                            </RadioGroup>
                          </div>
                        )}
                        <div className="space-y-2">
                          <Label className="text-sm">Copies du fichier</Label>
                          <Input type="number" min={1} value={uploadedFile.options.copies} onChange={(e) => updateFileOption(index, "copies", Math.max(1, parseInt(e.target.value) || 1))} />
                        </div>
                      </div>

                      {selectedProduct && (selectedProduct.proposeReliure || selectedProduct.proposePlastification) && (
                        <div className="pt-4 border-t grid grid-cols-1 sm:grid-cols-2 gap-4">
                          {selectedProduct.proposeReliure && selectedProduct.prixParTypeReliure && (
                            <div className="space-y-2">
                              <Label className="text-sm flex items-center gap-2"><Book className="w-4 h-4 text-muted-foreground" /> Reliure</Label>
                              <Select value={uploadedFile.options.binding} onValueChange={(v) => updateFileOption(index, "binding", v)}>
                                <SelectTrigger><SelectValue /></SelectTrigger>
                                <SelectContent>
                                  <SelectItem value="AUCUNE">Aucune reliure</SelectItem>
                                  {Object.entries(selectedProduct.prixParTypeReliure).map(([type, prix]) => {
                                    if (type === "AUCUNE" || prix == null) return null;
                                    return <SelectItem key={type} value={type}>{formatEnumName(type)} (+{Number(prix).toFixed(2)}€)</SelectItem>;
                                  })}
                                </SelectContent>
                              </Select>
                            </div>
                          )}
                          {selectedProduct.proposePlastification && selectedProduct.prixParTypePlastification && (
                            <div className="space-y-2">
                              <Label className="text-sm flex items-center gap-2"><Layers className="w-4 h-4 text-muted-foreground" /> Plastification</Label>
                              <Select value={uploadedFile.options.finish} onValueChange={(v) => updateFileOption(index, "finish", v)}>
                                <SelectTrigger><SelectValue /></SelectTrigger>
                                <SelectContent>
                                  <SelectItem value="AUCUNE">Aucune plastification</SelectItem>
                                  {Object.entries(selectedProduct.prixParTypePlastification).map(([type, prix]) => {
                                    if (type === "AUCUNE" || prix == null) return null;
                                    return <SelectItem key={type} value={type}>{formatEnumName(type)} (+{Number(prix).toFixed(2)}€)</SelectItem>;
                                  })}
                                </SelectContent>
                              </Select>
                            </div>
                          )}
                        </div>
                      )}

                      {/* Vérification orthographique : réservée aux documents, les fautes
                          pouvant être volontaires sur une affiche ou une carte de visite. */}
                      {selectedProduct?.typeProduit === "DOCUMENT" && (
                        <CorrectionOrthographe
                          file={uploadedFile.file}
                          etat={corrections[index] ?? null}
                          onChange={(etat) => majCorrection(index, etat)}
                        />
                      )}

                      <div className="flex items-center justify-between pt-4 border-t mt-4">
                        <span className="text-sm text-muted-foreground">Sous-total fichier</span>
                        <span className="font-semibold text-primary">{computeFilePrice(uploadedFile).toFixed(2)}€</span>
                      </div>
                    </CardContent>
                  </Card>
                );
              })}

              {/* Step 2: Fulfillment */}
              <Card className="shadow-card">
                <CardHeader>
                  <CardTitle className="font-display text-lg flex items-center gap-2">
                    <span className="w-7 h-7 rounded-full bg-primary text-primary-foreground text-sm flex items-center justify-center">2</span>
                    Mode de réception
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <RadioGroup value={fulfillment} onValueChange={(v) => setFulfillment(v as "pickup" | "delivery")} className="grid grid-cols-1 md:grid-cols-2 gap-3">
                    <Label htmlFor="pickup" className={`cursor-pointer p-4 border-2 rounded-lg transition-colors ${fulfillment === "pickup" ? "border-primary bg-primary/5" : "border-border"}`}>
                      <div className="flex items-start gap-3">
                        <RadioGroupItem value="pickup" id="pickup" className="mt-1" />
                        <div className="flex-1">
                          <div className="flex items-center gap-2 font-medium"><Store className="h-4 w-4" /> Retrait en magasin</div>
                          <p className="text-sm text-muted-foreground mt-1">Gratuit. Directement à l'imprimerie.</p>
                        </div>
                      </div>
                    </Label>
                    {shop.livraisonActive && (
                      <Label htmlFor="delivery" className={`cursor-pointer p-4 border-2 rounded-lg transition-colors ${fulfillment === "delivery" ? "border-primary bg-primary/5" : "border-border"}`}>
                        <div className="flex items-start gap-3">
                          <RadioGroupItem value="delivery" id="delivery" className="mt-1" />
                          <div className="flex-1">
                            <div className="flex items-center gap-2 font-medium">
                              <Truck className="h-4 w-4" /> Livraison <Badge variant="secondary" className="ml-auto">+{(shop.prixLivraison ?? 4.99).toFixed(2)}€</Badge>
                            </div>
                            <p className="text-sm text-muted-foreground mt-1">Livraison à domicile sous 24-48h.</p>
                          </div>
                        </div>
                      </Label>
                    )}
                  </RadioGroup>
                </CardContent>
              </Card>

              {/* Step 2bis: Delivery address */}
              {fulfillment === "delivery" && (
                <Card className="shadow-card border-primary/20">
                  <CardHeader>
                    <CardTitle className="font-display text-lg flex items-center gap-2">
                      <MapPin className="h-5 w-5 text-primary" /> Adresse de livraison
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div className="space-y-2">
                      <Label htmlFor="addr-name" className="text-sm">Nom du destinataire</Label>
                      <Input id="addr-name" placeholder="Jean Dupont" value={address.nomDestinataire} onChange={(e) => setAddress({ ...address, nomDestinataire: e.target.value })} />
                    </div>
                    <div className="grid grid-cols-3 gap-3">
                      <div className="col-span-2 space-y-2">
                        <Label htmlFor="addr-rue" className="text-sm">Rue</Label>
                        <Input id="addr-rue" placeholder="Rue de la Loi" value={address.rue} onChange={(e) => setAddress({ ...address, rue: e.target.value })} />
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="addr-num" className="text-sm">Numéro</Label>
                        <Input id="addr-num" placeholder="12B" value={address.numero} onChange={(e) => setAddress({ ...address, numero: e.target.value })} />
                      </div>
                    </div>
                    <div className="grid grid-cols-3 gap-3">
                      <div className="space-y-2">
                        <Label htmlFor="addr-cp" className="text-sm">Code postal</Label>
                        <Input id="addr-cp" placeholder="1000" value={address.codePostal} onChange={(e) => setAddress({ ...address, codePostal: e.target.value })} />
                      </div>
                      <div className="col-span-2 space-y-2">
                        <Label htmlFor="addr-ville" className="text-sm">Ville</Label>
                        <Input id="addr-ville" placeholder="Bruxelles" value={address.ville} onChange={(e) => setAddress({ ...address, ville: e.target.value })} />
                      </div>
                    </div>
                    <div className="grid grid-cols-2 gap-3">
                      <div className="space-y-2">
                        <Label htmlFor="addr-pays" className="text-sm">Pays</Label>
                        <Select value={address.pays} onValueChange={(v) => setAddress({ ...address, pays: v })}>
                          <SelectTrigger id="addr-pays"><SelectValue /></SelectTrigger>
                          <SelectContent>
                            <SelectItem value="Belgique">Belgique</SelectItem>
                            <SelectItem value="France">France</SelectItem>
                          </SelectContent>
                        </Select>
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="addr-tel" className="text-sm">Téléphone</Label>
                        <Input id="addr-tel" placeholder="+32 4 ..." value={address.telephone} onChange={(e) => setAddress({ ...address, telephone: e.target.value })} />
                      </div>
                    </div>
                  </CardContent>
                </Card>
              )}

              {/* Step 3: Options (Express/Etudiant) */}
              {(shop.proposeExpress2h || shop.proposeTarifEtudiant) && (
                <Card className="shadow-card">
                  <CardHeader>
                    <CardTitle className="font-display text-lg flex items-center gap-2">
                      <span className="w-7 h-7 rounded-full bg-primary text-primary-foreground text-sm flex items-center justify-center">3</span>
                      Options supplémentaires
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    {shop.proposeExpress2h && (() => {
                      const expressOk = isExpressAvailable();
                      return (
                        <div className={`flex items-start space-x-3 p-4 rounded-lg transition-colors ${expressOk ? "bg-muted/50 hover:bg-muted" : "bg-muted/20 opacity-60"}`}>
                          <Checkbox id="express" checked={expressOption} disabled={!expressOk} onCheckedChange={(checked) => setExpressOption(checked === true)} />
                          <div className="flex-1">
                            <Label htmlFor="express" className={`flex items-center gap-2 ${expressOk ? "cursor-pointer" : "cursor-not-allowed"}`}>
                              <Zap className="h-4 w-4 text-secondary" /> <span className="font-medium">Express 2h</span> <Badge variant="secondary" className="ml-auto">+{(shop?.prixExpress2h ?? 5).toFixed(2)}€</Badge>
                            </Label>
                            <p className="text-sm text-muted-foreground mt-1">
                              {expressOk ? "Prêt dans les 2 heures." : "Indisponible — l'imprimerie ferme dans moins de 2h ou est fermée aujourd'hui."}
                            </p>
                          </div>
                        </div>
                      );
                    })()}
                    {shop.proposeTarifEtudiant && shop.pourcentageRemiseEtudiant && shop.pourcentageRemiseEtudiant > 0 && (
                      <div className={`flex items-start space-x-3 p-4 rounded-lg transition-colors ${studentVerified ? "bg-muted/50 hover:bg-muted" : "bg-muted/20 opacity-60"}`}>
                        <Checkbox
                          id="student"
                          checked={studentDiscount}
                          disabled={!studentVerified}
                          onCheckedChange={(checked) => studentVerified && setStudentDiscount(checked === true)}
                        />
                        <div className="flex-1">
                          <Label htmlFor="student" className={`flex items-center gap-2 ${studentVerified ? "cursor-pointer" : "cursor-not-allowed"}`}>
                            <GraduationCap className="h-4 w-4 text-info" />
                            <span className="font-medium">Tarif étudiant</span>
                            <Badge variant="outline" className="ml-auto text-success border-success">-{shop.pourcentageRemiseEtudiant}%</Badge>
                          </Label>
                          {!studentVerified && (
                            <p className="text-sm text-destructive mt-1">Vérification étudiant requise — déposez vos documents dans votre espace client.</p>
                          )}
                        </div>
                      </div>
                    )}
                  </CardContent>
                </Card>
              )}

              {/* Step 4: Promo code */}
              <Card className="shadow-card">
                <CardHeader>
                  <CardTitle className="font-display text-lg flex items-center gap-2">
                    <span className="w-7 h-7 rounded-full bg-primary text-primary-foreground text-sm flex items-center justify-center">
                      {(shop.proposeExpress2h || shop.proposeTarifEtudiant) ? "4" : "3"}
                    </span>
                    Code promo PrintNow
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  {appliedPromo ? (
                    <div className="flex items-center justify-between p-4 rounded-lg bg-success/5 border border-success/20">
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-lg bg-success/10 flex items-center justify-center"><Check className="h-5 w-5 text-success" /></div>
                        <div>
                          <div className="font-medium">{appliedPromo.code}</div>
                          <div className="text-sm text-muted-foreground">
                            {appliedPromo.typeReduction === "POURCENTAGE"
                              ? `Réduction de ${appliedPromo.valeurReduction}% appliquée`
                              : `Réduction de ${appliedPromo.valeurReduction.toFixed(2)}€ appliquée`}
                          </div>
                        </div>
                      </div>
                      <Button variant="ghost" size="sm" onClick={removePromo}>Retirer</Button>
                    </div>
                  ) : (
                    <div className="space-y-2">
                      <div className="flex gap-2">
                        <div className="relative flex-1">
                          <Tag className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                          <Input
                            placeholder="Entrez votre code"
                            value={promoInput}
                            onChange={(e) => { setPromoInput(e.target.value); setPromoError(null); }}
                            className={`pl-9 uppercase ${promoError ? "border-destructive" : ""}`}
                          />
                        </div>
                        <Button variant="outline" onClick={applyPromo}>Appliquer</Button>
                      </div>
                      {promoError && (
                        <p className="text-sm text-destructive flex items-center gap-1">
                          <AlertCircle className="h-3.5 w-3.5 shrink-0" /> {promoError}
                        </p>
                      )}
                    </div>
                  )}
                </CardContent>
              </Card>

              {/* Step 5: Payment — real Stripe CardElement */}
              <Card className="shadow-card">
                <CardHeader>
                  <CardTitle className="font-display text-lg flex items-center gap-2">
                    <span className="w-7 h-7 rounded-full bg-primary text-primary-foreground text-sm flex items-center justify-center">
                      {(shop.proposeExpress2h || shop.proposeTarifEtudiant) ? "5" : "4"}
                    </span>
                    Paiement
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <Elements stripe={stripePromise}>
                    <CheckoutForm
                      total={totalAPayer}
                      canPay={commandeImprimable}
                      verifierFormats={verifierFormats}
                      addressValid={isAddressValid()}
                      fulfillment={fulfillment}
                      token={token}
                      onSuccess={handleCreateOrder}
                    />
                  </Elements>
                </CardContent>
              </Card>

            </div>

            {/* Sidebar: Order Summary */}
            <div className="lg:sticky lg:top-24 h-fit">
              <Card className="shadow-card border-primary/20">
                <CardHeader className="bg-primary/5 rounded-t-lg">
                  <CardTitle className="font-display text-lg">Récapitulatif</CardTitle>
                </CardHeader>
                <CardContent className="p-6 space-y-4">
                  {files.length > 0 ? (
                    <div className="space-y-2">
                      {files.map((uploadedFile, index) => {
                        const prod = activeProducts.find(p => p.id === uploadedFile.options.productId);
                        return (
                          <div key={index} className="flex items-center gap-3 p-3 rounded-lg bg-muted/50">
                            <FileText className="h-5 w-5 text-primary shrink-0" />
                            <div className="flex-1 min-w-0">
                              <p className="text-sm font-medium truncate">{uploadedFile.file.name}</p>
                              <p className="text-xs text-muted-foreground">
                                {prod ? getProductLabel(prod) : "Non sélectionné"} • {uploadedFile.options.copies}x
                              </p>
                            </div>
                            <span className="text-sm font-medium">{computeFilePrice(uploadedFile).toFixed(2)}€</span>
                          </div>
                        );
                      })}
                      <p className="text-xs text-muted-foreground text-center mt-3">Total: {totalPages} pages dans la commande</p>
                    </div>
                  ) : (
                    <p className="text-sm text-muted-foreground text-center py-4">Aucun fichier ajouté.</p>
                  )}

                  <div className="space-y-2 text-sm border-t pt-4">
                    <div className="flex justify-between"><span className="text-muted-foreground">Sous-total</span><span>{subtotal.toFixed(2)}€</span></div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground flex items-center gap-1">
                        {fulfillment === "pickup" ? <Store className="h-3 w-3" /> : <Truck className="h-3 w-3" />}
                        {fulfillment === "pickup" ? "Retrait magasin" : "Livraison"}
                      </span>
                      <span>{fulfillment === "pickup" ? "Gratuit" : `+${deliveryPrice.toFixed(2)}€`}</span>
                    </div>
                    {expressOption && <div className="flex justify-between"><span className="text-muted-foreground">Express 2h</span><span>+{expressAmount.toFixed(2)}€</span></div>}
                    {studentDiscount && <div className="flex justify-between text-success"><span>Réduction étudiant</span><span>-{studentDiscountAmount.toFixed(2)}€</span></div>}
                    {appliedPromo && <div className="flex justify-between text-success"><span>Code promo</span><span>-{promoDiscountAmount.toFixed(2)}€</span></div>}
                  </div>

                  <div className="border-t pt-4 space-y-1 text-sm">
                    <div className="flex justify-between text-muted-foreground">
                      <span>Total HT</span><span>{totalHT.toFixed(2)}€</span>
                    </div>
                    <div className="flex justify-between text-muted-foreground">
                      <span>TVA (21%)</span><span>+{tva.toFixed(2)}€</span>
                    </div>
                    {totalCorrections > 0 && (
                      <div className="flex justify-between text-muted-foreground">
                        <span className="flex items-center gap-1.5">
                          <SpellCheck2 className="h-3.5 w-3.5" />
                          Correction orthographique
                        </span>
                        <span>+{totalCorrections.toFixed(2)}€</span>
                      </div>
                    )}
                    {totalGenerations > 0 && (
                      <div className="flex justify-between text-muted-foreground">
                        <span className="flex items-center gap-1.5">
                          <Sparkles className="h-3.5 w-3.5" />
                          Design IA ({generationsActives.length} × {PRIX_GENERATION.toFixed(2)}€)
                        </span>
                        <span>+{totalGenerations.toFixed(2)}€</span>
                      </div>
                    )}
                    <div className="flex justify-between items-center pt-2 border-t">
                      <span className="font-semibold text-lg">Total à payer</span>
                      <span className="font-display font-bold text-2xl text-primary">{totalAPayer.toFixed(2)}€</span>
                    </div>
                  </div>

                  <p className="text-xs text-muted-foreground text-center pt-2 border-t">
                    Remplissez vos informations de carte dans le formulaire de paiement ci-contre.
                  </p>
                </CardContent>
              </Card>
            </div>
          </div>
        </div>
      </main>

      {/* Confirmation modal */}
      <Dialog open={!!confirmation} onOpenChange={(o) => !o && handleFermerConfirmation()}>
        <DialogContent>
          <DialogHeader>
            {confirmation?.fichiersNonEnvoyes.length ? (
              <>
                <div className="mx-auto w-14 h-14 rounded-full bg-destructive/10 flex items-center justify-center mb-2">
                  <AlertCircle className="h-7 w-7 text-destructive" />
                </div>
                <DialogTitle className="text-center font-display text-2xl">
                  Paiement reçu, fichier non transmis
                </DialogTitle>
                <DialogDescription className="text-center">
                  Votre paiement a bien été enregistré, mais {confirmation.fichiersNonEnvoyes.length > 1
                    ? "certains fichiers n'ont pas pu être envoyés"
                    : "votre fichier n'a pas pu être envoyé"} à l'imprimerie.
                  Contactez-nous en indiquant le numéro de commande ci-dessous : nous reprendrons la main.
                </DialogDescription>
              </>
            ) : (
              <>
                <div className="mx-auto w-14 h-14 rounded-full bg-success/10 flex items-center justify-center mb-2">
                  <Check className="h-7 w-7 text-success" />
                </div>
                <DialogTitle className="text-center font-display text-2xl">Commande confirmée !</DialogTitle>
                <DialogDescription className="text-center">Merci pour votre commande. Un email vous a été envoyé.</DialogDescription>
              </>
            )}
          </DialogHeader>

          {confirmation && (
            <div className="space-y-3 text-sm">
              <div className="flex justify-between p-3 rounded-lg bg-muted/50">
                <span className="text-muted-foreground">N° de commande</span>
                <span className="font-mono font-medium">{confirmation.numeroCommande}</span>
              </div>
              <div className="flex justify-between p-3 rounded-lg bg-muted/50">
                <span className="text-muted-foreground flex items-center gap-2"><CreditCard className="h-4 w-4" /> Paiement</span>
                <Badge className="bg-success/15 text-success border-success/20" variant="outline">Payé</Badge>
              </div>
              {confirmation.fichiersNonEnvoyes.length > 0 && (
                <div className="p-3 rounded-lg border border-destructive/30 bg-destructive/5">
                  <p className="text-destructive font-medium mb-1">
                    Fichier{confirmation.fichiersNonEnvoyes.length > 1 ? "s" : ""} non transmis
                  </p>
                  <ul className="list-disc list-inside text-muted-foreground">
                    {confirmation.fichiersNonEnvoyes.map((nom) => (
                      <li key={nom} className="truncate">{nom}</li>
                    ))}
                  </ul>
                </div>
              )}
              <div className="flex justify-between p-3 rounded-lg bg-muted/50">
                <span className="text-muted-foreground flex items-center gap-2">
                  {confirmation.modeRetrait === "RETRAIT_MAGASIN" ? <Store className="h-4 w-4" /> : <Truck className="h-4 w-4" />} Mode de retrait
                </span>
                <span className="font-medium">{confirmation.modeRetrait === "RETRAIT_MAGASIN" ? "Retrait magasin" : "Livraison bpost"}</span>
              </div>
              {confirmation.numeroSuivi && (
                <div className="flex justify-between p-3 rounded-lg bg-muted/50">
                  <span className="text-muted-foreground">N° de suivi</span>
                  <span className="font-mono">{confirmation.numeroSuivi}</span>
                </div>
              )}
              {(confirmation.correction > 0 || confirmation.generation > 0) && (
                <div className="space-y-1 pt-2 border-t text-muted-foreground">
                  <div className="flex justify-between">
                    <span>Impression</span>
                    <span>{confirmation.total.toFixed(2)}€</span>
                  </div>
                  {confirmation.correction > 0 && (
                    <div className="flex justify-between">
                      <span className="flex items-center gap-1.5">
                        <SpellCheck2 className="h-3.5 w-3.5" />
                        Correction orthographique
                      </span>
                      <span>{confirmation.correction.toFixed(2)}€</span>
                    </div>
                  )}
                  {confirmation.generation > 0 && (
                    <div className="flex justify-between">
                      <span className="flex items-center gap-1.5">
                        <Sparkles className="h-3.5 w-3.5" />
                        Design IA
                      </span>
                      <span>{confirmation.generation.toFixed(2)}€</span>
                    </div>
                  )}
                </div>
              )}
              <div className="flex justify-between pt-2 border-t">
                <span className="font-semibold">Total payé</span>
                <span className="font-display font-bold text-primary">
                  {(confirmation.total + confirmation.correction + confirmation.generation).toFixed(2)}€
                </span>
              </div>
            </div>
          )}

          <DialogFooter className="sm:justify-center mt-4">
            <Button variant="hero" className="w-full" onClick={handleFermerConfirmation}>Fermer</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

    </div>
  );
};

export default Order;
