import { useState, useEffect } from "react";
import { useParams, Link, Navigate } from "react-router-dom";
import Header from "../../../components/layout/Header";
import { Button } from "../../../components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "../../../components/ui/card";
import { Label } from "../../../components/ui/label";
import { Input } from "../../../components/ui/input";
import { RadioGroup, RadioGroupItem } from "../../../components/ui/radio-group";
import { Checkbox } from "../../../components/ui/checkbox";
import { Badge } from "../../../components/ui/badge";
import { toast } from "../../../hooks/use-toast";
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
  Lock, Loader2, Layers, Book
} from "lucide-react";
import { loadStripe } from "@stripe/stripe-js";
import { Elements, CardElement, useStripe, useElements } from "@stripe/react-stripe-js";

import { imprimerieService } from "../services/imprimerieService.service";
import type { ImprimerieDetail } from "../models/Imprimerie.model";
import { useAuth } from "../../auth/context/AuthContext";

const stripePromise = loadStripe(import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY ?? "");

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
  options: FileOptions;
}


interface DeliveryAddress {
  nomDestinataire: string;
  rue: string;
  numero: string;
  codePostal: string;
  ville: string;
  pays: string;
  telephone: string;
}

interface OrderConfirmation {
  numeroCommande: string;
  numeroSuivi?: string;
  statutPaiement: "SUCCES";
  statutLivraison?: "EN_PREPARATION";
  modeRetrait: "RETRAIT_MAGASIN" | "LIVRAISON_DHL";
  total: number;
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
  onSuccess: (paymentIntentId: string) => Promise<void>;
}

const CheckoutForm: React.FC<CheckoutFormProps> = ({
  total, canPay, addressValid, fulfillment, token, onSuccess,
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
    if (!canPay) {
      toast({ title: "Aucun fichier", description: "Ajoutez au moins un PDF.", variant: "destructive" });
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
      // 1. Create PaymentIntent on the backend
      const piRes = await fetch("http://localhost:8080/api/payments/create-payment-intent", {
        method: "POST",
        headers: { "Content-Type": "application/json", "Authorization": `Bearer ${token}` },
        body: JSON.stringify({ amount: Math.round(total * 100) }),
      });
      if (!piRes.ok) throw new Error("Erreur lors de la création du paiement.");
      const { clientSecret } = await piRes.json();

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
      toast({ title: "Erreur de paiement", description: message, variant: "destructive" });
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
  const { id } = useParams<{ id: string }>();
  const { token, user } = useAuth();

  if (!token) return <Navigate to="/login" replace />;
  if (user?.role === "ROLE_IMPRIMERIE") return <Navigate to="/dashboard-imprimeur" replace />;

  const [shop, setShop] = useState<ImprimerieDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const [files, setFiles] = useState<UploadedFile[]>([]);
  const [expressOption, setExpressOption] = useState(false);
  const [studentDiscount, setStudentDiscount] = useState(false);
  const [fulfillment, setFulfillment] = useState<"pickup" | "delivery">("pickup");
  const [promoInput, setPromoInput] = useState("");
  const [promoError, setPromoError] = useState<string | null>(null);
  const [appliedPromo, setAppliedPromo] = useState<{ code: string; typeReduction: string; valeurReduction: number; montantMinimum?: number } | null>(null);

  const [address, setAddress] = useState<DeliveryAddress>({
    nomDestinataire: "", rue: "", numero: "", codePostal: "", ville: "", pays: "Belgique", telephone: "",
  });

  const [confirmation, setConfirmation] = useState<OrderConfirmation | null>(null);

  useEffect(() => {
    if (id) {
      imprimerieService.getImprimerieById(id)
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
  }, [id]);

  const formatEnumName = (text: string) => {
    if (!text) return "";
    const formatted = text.replace(/_/g, " ").toLowerCase();
    return formatted.charAt(0).toUpperCase() + formatted.slice(1);
  };

  const getProductLabel = (p: { typeProduit: string; formatImpression: string; prixBase: number }): string => {
    if (p.typeProduit === "DOCUMENT") {
      const nbThreshold: Record<string, number> = { A4: 0.20, A3: 0.65 };
      const t = nbThreshold[p.formatImpression];
      if (t !== undefined) return `${p.prixBase <= t ? "N&B" : "Couleur"} ${p.formatImpression}`;
    }
    if (p.typeProduit === "CARTE_VISITE") return "Cartes de visite";
    if (p.typeProduit === "FLYER") return "Flyers / Dépliants";
    if (p.typeProduit === "POSTER") return "Affiches grand format";
    return `${formatEnumName(p.typeProduit)} ${p.formatImpression}`;
  };

  const countPdfPages = async (file: File): Promise<number> => {
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

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFiles = e.target.files;
    if (selectedFiles) {
      const newFiles: UploadedFile[] = [];
      for (let i = 0; i < selectedFiles.length; i++) {
        const file = selectedFiles[i];
        if (file.type === "application/pdf") {
          const pageCount = await countPdfPages(file);
          const defaultProduct = shop?.produits?.find(p => p.actif) || null;
          newFiles.push({
            file,
            pageCount,
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

  const removeFile = (index: number) => setFiles((prev) => prev.filter((_, i) => i !== index));

  const updateFileOption = <K extends keyof FileOptions>(index: number, key: K, value: FileOptions[K]) => {
    setFiles((prev) =>
      prev.map((f, i) => {
        if (i !== index) return f;
        const updatedOptions = { ...f.options, [key]: value };
        if (key === "productId") {
          updatedOptions.binding = "AUCUNE";
          updatedOptions.finish = "AUCUNE";
        }
        return { ...f, options: updatedOptions };
      })
    );
  };

  const computeFilePrice = (f: UploadedFile) => {
    if (!f.options.productId || !shop || !shop.produits) return 0;
    const product = shop.produits.find(p => p.id === f.options.productId);
    if (!product) return 0;

    let unitPrice = (product.prixBase + product.prixParPage) * f.pageCount;
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
  const deliveryPrice = fulfillment === "delivery" ? 4.99 : 0;
  const studentDiscountAmount = (studentDiscount && shop?.pourcentageRemiseEtudiant)
    ? subtotal * (shop.pourcentageRemiseEtudiant / 100) : 0;
  const totalAvantPromo = subtotal + expressAmount + deliveryPrice - studentDiscountAmount;
  const promoDiscountAmount = appliedPromo
    ? (appliedPromo.typeReduction === "POURCENTAGE"
        ? subtotal * (appliedPromo.valeurReduction / 100)
        : appliedPromo.valeurReduction)
    : 0;
  const totalHT = Math.max(0, totalAvantPromo - promoDiscountAmount);
  const tva = totalHT * 0.20;
  const total = totalHT + tva;

  useEffect(() => {
    if (appliedPromo?.montantMinimum && totalAvantPromo * 1.20 < appliedPromo.montantMinimum) {
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
      const totalTTCAvantPromo = totalAvantPromo * 1.20;
      const res = await fetch(`http://localhost:8080/api/promos/valider?code=${encodeURIComponent(code)}&montant=${totalTTCAvantPromo.toFixed(2)}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) {
        let msg = "Code invalide ou inexistant.";
        try { const err = await res.json(); msg = err.message || err.detail || msg; } catch { /* ignore */ }
        setPromoError(msg);
        return;
      }
      const promo = await res.json();
      setAppliedPromo({
        code: promo.code,
        typeReduction: promo.typeReduction,
        valeurReduction: Number(promo.valeurReduction),
        montantMinimum: promo.montantMinimumCommande ? Number(promo.montantMinimumCommande) : undefined,
      });
      setPromoError(null);
      toast({ title: "Code appliqué !", description: promo.typeReduction === "POURCENTAGE" ? `Réduction de ${promo.valeurReduction}%` : `Réduction de ${Number(promo.valeurReduction).toFixed(2)}€` });
    } catch {
      setPromoError("Impossible de vérifier le code. Vérifiez votre connexion.");
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
  const handleCreateOrder = async (_paymentIntentId: string) => {
    const payload = {
      modeRetrait: fulfillment === "pickup" ? "RETRAIT_MAGASIN" : "LIVRAISON",
      express2h: expressOption,
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
        couleur: false,
        rectoVerso: f.options.recto === "rectoverso",
        reliure: f.options.binding || "AUCUNE",
        finition: f.options.finish || "AUCUNE",
      })),
    };

    const response = await fetch("http://localhost:8080/api/commandes", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${token}`,
      },
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(errorText || "Erreur lors de la création de la commande");
    }

    const data = await response.json();

    const lignes: any[] = data.lignes ?? [];
    const uploadResults = await Promise.allSettled(
      files.map(async (uploadedFile, i) => {
        const ligneId = lignes[i]?.id;
        if (!ligneId) throw new Error(`Ligne ${i} introuvable`);
        const formData = new FormData();
        formData.append("file", uploadedFile.file);
        formData.append("ligneCommandeId", ligneId.toString());
        formData.append("nbPages", uploadedFile.pageCount.toString());
        const res = await fetch("http://localhost:8080/api/fichiers-pdf", {
          method: "POST",
          headers: { "Authorization": `Bearer ${token}` },
          body: formData,
        });
        if (!res.ok) {
          const txt = await res.text();
          throw new Error(`Upload échoué (${res.status}): ${txt}`);
        }
        return res.json();
      })
    );

    const failed = uploadResults.filter(r => r.status === "rejected");
    if (failed.length > 0) {
      console.error("Erreurs upload PDF:", failed);
      toast({ title: "Attention", description: "Certains fichiers PDF n'ont pas pu être envoyés.", variant: "destructive" });
    }

    setConfirmation({
      numeroCommande: data.numeroCommande,
      numeroSuivi: data.numeroSuivi ?? undefined,
      statutPaiement: "SUCCES",
      statutLivraison: fulfillment === "delivery" ? "EN_PREPARATION" : undefined,
      modeRetrait: fulfillment === "delivery" ? "LIVRAISON_DHL" : "RETRAIT_MAGASIN",
      total: Number(data.totalTTC),
    });
  };

  if (isLoading) return <div className="min-h-screen flex items-center justify-center bg-muted/30"><Loader2 className="animate-spin h-8 w-8 text-primary" /></div>;
  if (!shop) return <div className="min-h-screen flex flex-col items-center justify-center bg-muted/30 text-destructive"><p className="text-xl font-bold">Imprimerie introuvable.</p><Button className="mt-4" asChild><Link to="/">Retour à l'accueil</Link></Button></div>;

  const activeProducts = shop.produits?.filter(p => p.actif) || [];

  return (
    <div className="min-h-screen flex flex-col bg-muted/30">
      <Header />

      <main className="flex-1 pt-24 pb-16">
        <div className="container mx-auto px-4 max-w-6xl">
          <Button variant="ghost" size="sm" className="mb-6" asChild>
            <Link to={`/imprimerie/${id}`}>
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
                            {activeProducts.map((p) => (
                              <SelectItem key={p.id} value={p.id.toString()}>
                                {getProductLabel(p)}{" "}
                                <span className="text-muted-foreground ml-2">({(p.prixBase + p.prixParPage).toFixed(2)}€/page)</span>
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </div>

                      <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-3">
                          <Label className="text-sm">Mise en page</Label>
                          <RadioGroup value={uploadedFile.options.recto} onValueChange={(v) => updateFileOption(index, "recto", v as "recto" | "rectoverso")} className="flex gap-4">
                            <div className="flex items-center space-x-2"><RadioGroupItem value="recto" id={`recto-${index}`} /><Label htmlFor={`recto-${index}`} className="cursor-pointer">Recto</Label></div>
                            <div className="flex items-center space-x-2"><RadioGroupItem value="rectoverso" id={`rv-${index}`} /><Label htmlFor={`rv-${index}`} className="cursor-pointer">Recto-Verso (-15%)</Label></div>
                          </RadioGroup>
                        </div>
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
                              <Truck className="h-4 w-4" /> Livraison <Badge variant="secondary" className="ml-auto">+4.99€</Badge>
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
                      <div className="flex items-start space-x-3 p-4 rounded-lg bg-muted/50 hover:bg-muted transition-colors">
                        <Checkbox id="student" checked={studentDiscount} onCheckedChange={(checked) => setStudentDiscount(checked === true)} />
                        <div className="flex-1">
                          <Label htmlFor="student" className="flex items-center gap-2 cursor-pointer">
                            <GraduationCap className="h-4 w-4 text-info" /> <span className="font-medium">Tarif étudiant</span> <Badge variant="outline" className="ml-auto text-success border-success">-{shop.pourcentageRemiseEtudiant}%</Badge>
                          </Label>
                          <p className="text-sm text-muted-foreground mt-1">Justificatif requis lors du retrait.</p>
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
                    Code promo PrintHub
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
                      total={total}
                      canPay={files.length > 0}
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
                      <span>TVA (20%)</span><span>+{tva.toFixed(2)}€</span>
                    </div>
                    <div className="flex justify-between items-center pt-2 border-t">
                      <span className="font-semibold text-lg">Total TTC</span>
                      <span className="font-display font-bold text-2xl text-primary">{total.toFixed(2)}€</span>
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
      <Dialog open={!!confirmation} onOpenChange={(o) => !o && setConfirmation(null)}>
        <DialogContent>
          <DialogHeader>
            <div className="mx-auto w-14 h-14 rounded-full bg-success/10 flex items-center justify-center mb-2">
              <Check className="h-7 w-7 text-success" />
            </div>
            <DialogTitle className="text-center font-display text-2xl">Commande confirmée !</DialogTitle>
            <DialogDescription className="text-center">Merci pour votre commande. Un email vous a été envoyé.</DialogDescription>
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
              <div className="flex justify-between p-3 rounded-lg bg-muted/50">
                <span className="text-muted-foreground flex items-center gap-2">
                  {confirmation.modeRetrait === "RETRAIT_MAGASIN" ? <Store className="h-4 w-4" /> : <Truck className="h-4 w-4" />} Mode de retrait
                </span>
                <span className="font-medium">{confirmation.modeRetrait === "RETRAIT_MAGASIN" ? "Retrait magasin" : "Livraison DHL"}</span>
              </div>
              {confirmation.numeroSuivi && (
                <div className="flex justify-between p-3 rounded-lg bg-muted/50">
                  <span className="text-muted-foreground">N° de suivi</span>
                  <span className="font-mono">{confirmation.numeroSuivi}</span>
                </div>
              )}
              <div className="flex justify-between pt-2 border-t">
                <span className="font-semibold">Total payé</span>
                <span className="font-display font-bold text-primary">{confirmation.total.toFixed(2)}€</span>
              </div>
            </div>
          )}

          <DialogFooter className="sm:justify-center mt-4">
            <Button variant="hero" className="w-full" onClick={() => setConfirmation(null)}>Fermer</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

    </div>
  );
};

export default Order;
