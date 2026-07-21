import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Button } from "../../../components/ui/button";
import { Input } from "../../../components/ui/input";
import { Label } from "../../../components/ui/label";
import { Textarea } from "../../../components/ui/textarea";
import { Card } from "../../../components/ui/card";
import { Checkbox } from "../../../components/ui/checkbox";
import { Separator } from "../../../components/ui/separator";
import { Badge } from "../../../components/ui/badge";
import Header from "../../../components/layout/Header";
import { toast } from "../../../hooks/use-toast";

import { partnerService } from "../services/partner.service";
import { type PartnerRegistrationRequest } from "../models/partner.model";
import { DAYS, SERVICES, type Hours } from "../models/partner.constants";

import {
  Printer, Building2, Mail, Phone, MapPin, Upload,
  CheckCircle2, CreditCard, Clock, ArrowRight, ArrowLeft,
  Lock, Sparkles, Truck, GraduationCap, Zap,
  Eye, EyeOff, Book, Layers
} from "lucide-react";

// 👇 Imports Stripe
import { loadStripe } from "@stripe/stripe-js";
import { Elements, CardElement, useStripe, useElements } from "@stripe/react-stripe-js";

// 👇 Initialisation de Stripe 
const stripePromise = loadStripe("pk_test_51TTGFb2cg3RhdI8F1DZMf8XiWANwLYoDPQGDEfqARAwnDN4gvIBxQyioyQFUuxMvoVtJJHwdT6tJP2IdBIlOMdES00ZAsXr92a");

// =========================================================================
// SOUS-COMPOSANT : Formulaire Stripe sécurisé
// =========================================================================
const StripePaymentForm = ({ payload, onPaymentSuccess, onBack, amount }: any) => {
  const stripe = useStripe();
  const elements = useElements();
  const [isProcessing, setIsProcessing] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!stripe || !elements) return;

    setIsProcessing(true);
    setErrorMessage("");

    try {
      const savedPartnerResponse = await partnerService.register(payload) as any;
      const imprimerieId = savedPartnerResponse?.id || savedPartnerResponse;
      
      const intentResponse = await fetch("http://localhost:8080/api/payments/create-payment-intent", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ imprimerieId: imprimerieId })
      });
      
      if (!intentResponse.ok) throw new Error("Erreur lors de l'initialisation du paiement");
      const intentData = await intentResponse.json();

      const result = await stripe.confirmCardPayment(intentData.clientSecret, {
        payment_method: {
          card: elements.getElement(CardElement)!,
        }
      });

      if (result.error) {
        setErrorMessage(result.error.message || "Le paiement a échoué.");
      } else if (result.paymentIntent?.status === "succeeded") {
        onPaymentSuccess();
      }
    } catch (error: any) {
      setErrorMessage(error.message || "Une erreur de communication est survenue.");
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="p-4 border border-border rounded-lg bg-background">
        <CardElement options={{ hidePostalCode: true, style: { base: { fontSize: '16px', color: '#333' } } }} />
      </div>
      
      {errorMessage && <p className="text-sm text-destructive font-medium">{errorMessage}</p>}

      <div className="flex justify-between mt-8">
        <Button variant="outline" type="button" size="lg" onClick={onBack} disabled={isProcessing}>
          <ArrowLeft className="h-4 w-4 mr-2" /> Retour
        </Button>
        <Button variant="default" type="submit" size="lg" disabled={!stripe || isProcessing}>
          {isProcessing ? "Traitement sécurisé..." : `Payer ${amount.toFixed(2)}€ et activer`}
          {!isProcessing && <ArrowRight className="h-4 w-4 ml-2" />}
        </Button>
      </div>
    </form>
  );
};

// =========================================================================
// COMPOSANT PRINCIPAL
// =========================================================================
const DevenirPartenaire = () => {
  const navigate = useNavigate();
  const [step, setStep] = useState(1);
  const [isSuccess, setIsSuccess] = useState(false);

  // Step 1
  const [shopName, setShopName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [address, setAddress] = useState("");
  const [siret, setSiret] = useState("");
  const [description, setDescription] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [logoUrl, setLogoUrl] = useState("");
  const [logoUploading, setLogoUploading] = useState(false);
  const [logoError, setLogoError] = useState("");

  const handleLogoChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const fichier = e.target.files?.[0];
    if (!fichier) return;
    setLogoUploading(true);
    setLogoError("");
    try {
      const url = await partnerService.uploadLogo(fichier);
      setLogoUrl(url);
    } catch (err) {
      setLogoError((err as Error).message || "Erreur lors de l'envoi du logo");
      setLogoUrl("");
    } finally {
      setLogoUploading(false);
    }
  };

  // Valeurs et états par défaut pour les options de finition
  const defaultPlastificationPrices = { MAT: "0.50", BRILLANT: "0.60", SOFT_TOUCH: "0.80" };
  const defaultPlastificationActive = { MAT: true, BRILLANT: true, SOFT_TOUCH: false };

  const defaultReliurePrices = { 
    SPIRALE_PLASTIQUE: "1.50", 
    SPIRALE_METALLIQUE: "2.50", 
    DOS_CARRE_COLLE: "3.50", 
    AGRAFE_DEUX_POINTS: "0.50", 
    THERMIQUE: "3.00" 
  };
  const defaultReliureActive = { 
    SPIRALE_PLASTIQUE: true, 
    SPIRALE_METALLIQUE: true, 
    DOS_CARRE_COLLE: false, 
    AGRAFE_DEUX_POINTS: false, 
    THERMIQUE: false 
  };

  // Step 2 - services
  const [services, setServices] = useState<Record<string, any>>(
    Object.fromEntries(SERVICES.map((s) => [s.id, { 
      enabled: false, 
      price: s.defaultPrice,
      
      proposePlastification: false,
      activePlastification: { ...defaultPlastificationActive },
      prixParTypePlastification: { ...defaultPlastificationPrices },
      
      proposeReliure: false,
      activeReliure: { ...defaultReliureActive },
      prixParTypeReliure: { ...defaultReliurePrices }
    }]))
  );

  // Step 2 - hours
  const [hours, setHours] = useState<Record<string, Hours>>(
    Object.fromEntries(DAYS.map((d) => [d.key, { open: "09:00", close: "18:00", closed: d.key === "sun" }]))
  );

  // Step 2 - extra options
  const [offersExpress, setOffersExpress] = useState(false);
  const [expressPrice, setExpressPrice] = useState("5.00");
  const [offersDelivery, setOffersDelivery] = useState(false);
  const [deliveryFee, setDeliveryFee] = useState("5.00");
  const [offersStudentDiscount, setOffersStudentDiscount] = useState(false);
  const [studentDiscountPct, setStudentDiscountPct] = useState("10");

  const enabledServicesCount = Object.values(services).filter((s) => s.enabled).length;

  const getStep1Errors = () => {
    const missing: string[] = [];
    if (!shopName.trim()) missing.push("nom de l'imprimerie");
    if (!email.trim()) missing.push("email");
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) missing.push("email invalide");
    if (!phone.trim()) missing.push("téléphone");
    else if (!/^\+?[0-9 ()./-]{8,20}$/.test(phone)) missing.push("téléphone invalide");
    if (!address.trim()) missing.push("adresse");
    if (!siret.trim()) missing.push("N° TVA");
    if (!/^(?=.*[A-Za-z])(?=.*\d).{8,}$/.test(password)) missing.push("mot de passe (min. 8 caractères, lettres et chiffres)");
    if (password !== confirmPassword) missing.push("les mots de passe ne correspondent pas");
    if (!description.trim()) missing.push("description de l'imprimerie");
    if (!logoUrl) missing.push("logo de l'imprimerie");
    return missing;
  };
  
  const canGoStep3 = enabledServicesCount >= 1;

  const toggleService = (id: string) => {
    setServices((prev) => ({ ...prev, [id]: { ...prev[id], enabled: !prev[id].enabled } }));
  };

  const updateServiceField = (id: string, field: string, value: any) => {
    setServices((prev) => ({ ...prev, [id]: { ...prev[id], [field]: value } }));
  };

  const updateOptionPrice = (serviceId: string, category: "prixParTypePlastification" | "prixParTypeReliure", type: string, value: string) => {
    setServices((prev) => ({
      ...prev,
      [serviceId]: { ...prev[serviceId], [category]: { ...prev[serviceId][category], [type]: value } }
    }));
  };

  // 👇 NOUVELLE FONCTION pour cocher/décocher un type précis
  const toggleOptionActive = (serviceId: string, category: "activePlastification" | "activeReliure", type: string, value: boolean) => {
    setServices((prev) => ({
      ...prev,
      [serviceId]: { ...prev[serviceId], [category]: { ...prev[serviceId][category], [type]: value } }
    }));
  };

  const updateHours = (day: string, field: keyof Hours, value: string | boolean) => {
    setHours((prev) => ({ ...prev, [day]: { ...prev[day], [field]: value } }));
  };

  const formatEnumName = (text: string) => {
    if (!text) return "";
    const formatted = text.replace(/_/g, ' ').toLowerCase();
    return formatted.charAt(0).toUpperCase() + formatted.slice(1);
  };

  // 👇 Filtrage strict avant envoi au backend 👇
  const buildPayload = (): PartnerRegistrationRequest => {
    const activeServices = Object.entries(services)
      .filter(([_, state]) => state.enabled)
      .map(([id, state]) => {
        const serviceInfo = SERVICES.find(s => s.id === id);
        
        const isDocument = serviceInfo!.typeProduit === "DOCUMENT";
        const canBePlastified = serviceInfo!.typeProduit !== "POSTER";

        // On ne garde QUE les types qui ont été cochés par l'imprimeur
        const parsedPlastif = Object.fromEntries(
          Object.entries(state.prixParTypePlastification)
            .filter(([k]) => state.activePlastification[k])
            .map(([k, v]) => [k, parseFloat(v as string) || 0])
        );

        const parsedReliure = Object.fromEntries(
          Object.entries(state.prixParTypeReliure)
            .filter(([k]) => state.activeReliure[k])
            .map(([k, v]) => [k, parseFloat(v as string) || 0])
        );

        const hasAnyPlastif = Object.keys(parsedPlastif).length > 0;
        const hasAnyReliure = Object.keys(parsedReliure).length > 0;

        return {
          typeProduit: serviceInfo!.typeProduit,
          formatImpression: serviceInfo!.formatImpression,
          prixBase: parseFloat(state.price),
          prixParPage: 0,
          
          proposePlastification: canBePlastified && state.proposePlastification && hasAnyPlastif,
          prixParTypePlastification: (canBePlastified && state.proposePlastification && hasAnyPlastif) ? parsedPlastif : null,
          
          proposeReliure: isDocument && state.proposeReliure && hasAnyReliure,
          prixParTypeReliure: (isDocument && state.proposeReliure && hasAnyReliure) ? parsedReliure : null
        };
      });

    const daysMap: Record<string, string> = {
      mon: "LUNDI", tue: "MARDI", wed: "MERCREDI", 
      thu: "JEUDI", fri: "VENDREDI", sat: "SAMEDI", sun: "DIMANCHE"
    };
    
    const activeHours = Object.entries(hours).map(([key, h]) => ({
      jourSemaine: daysMap[key],
      heureOuverture: h.open + ":00",
      heureFermeture: h.close + ":00",
      ferme: h.closed
    }));

    return {
      email: email,
      password: password,
      siret: siret,
      imprimerie: {
        nom: shopName,
        telephoneContact: phone,
        emailContact: email,
        adresse: address,
        numeroTva: siret,
        description: description,
        logoUrl: logoUrl,
        proposeExpress2h: offersExpress,
        prixExpress2h: offersExpress ? parseFloat(expressPrice) : undefined,
        livraisonActive: offersDelivery,
        proposeTarifEtudiant: offersStudentDiscount,
        pourcentageRemiseEtudiant: offersStudentDiscount ? parseInt(studentDiscountPct) : undefined,
        ville: "Non spécifiée", 
        pays: "Belgique"
      },
      produits: activeServices as any,
      horaires: activeHours
    };
  };

  const handlePaymentSuccess = () => {
    setIsSuccess(true);
    toast({
      title: "Paiement validé ✅",
      description: "Votre imprimerie est désormais activée avec succès !",
    });
  };

  // --- Success screen ---
  if (isSuccess) {
    return (
      <div className="min-h-screen flex flex-col bg-muted/30">
        <Header />
        <main className="flex-1 pt-24 pb-16 flex items-center">
          <div className="container mx-auto px-4 max-w-2xl">
            <Card className="p-10 text-center">
              <div className="w-20 h-20 rounded-full bg-success/10 flex items-center justify-center mx-auto mb-6">
                <CheckCircle2 className="h-10 w-10 text-success" />
              </div>
              <h1 className="font-display text-3xl md:text-4xl font-bold mb-4">
                Bienvenue sur PrintNow ! 🎉
              </h1>
              <p className="text-muted-foreground mb-2">
                Paiement de <strong>100€</strong> confirmé.
              </p>
              <p className="text-muted-foreground mb-8">
                Votre imprimerie <strong>{shopName}</strong> est maintenant visible dans le catalogue et prête à recevoir des commandes.
              </p>
              <div className="grid grid-cols-3 gap-3 mb-8 text-left">
                <div className="p-4 rounded-lg bg-muted/50">
                  <div className="text-xs text-muted-foreground">Services</div>
                  <div className="font-display text-2xl font-bold">{enabledServicesCount}</div>
                </div>
                <div className="p-4 rounded-lg bg-muted/50">
                  <div className="text-xs text-muted-foreground">Jours ouverts</div>
                  <div className="font-display text-2xl font-bold">
                    {Object.values(hours).filter((h) => !h.closed).length}/7
                  </div>
                </div>
                <div className="p-4 rounded-lg bg-muted/50">
                  <div className="text-xs text-muted-foreground">Statut</div>
                  <div className="font-display text-sm font-bold text-success mt-1">● Actif</div>
                </div>
              </div>
              <div className="flex flex-col sm:flex-row gap-3 justify-center">
                <Button variant="default" size="lg" onClick={() => navigate("/dashboard-imprimeur")}>
                  Accéder à mon espace
                  <ArrowRight className="h-4 w-4 ml-2" />
                </Button>
                <Button variant="outline" size="lg" asChild>
                  <Link to="/">Retour à l'accueil</Link>
                </Button>
              </div>
            </Card>
          </div>
        </main>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex flex-col bg-muted/30">
      <Header />
      <main className="flex-1 pt-24 pb-16">
        <div className="container mx-auto px-4 max-w-4xl">
          {/* Hero */}
          <div className="text-center mb-10">
            <h1 className="font-display text-3xl md:text-5xl font-bold mb-4">
              Devenez imprimerie partenaire
            </h1>
            <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
              Inscription unique de <strong>100€</strong>, ensuite seulement <strong>10% de commission</strong> par commande.
              Votre boutique est activée immédiatement après paiement.
            </p>
          </div>

          {/* Stepper */}
          <div className="flex items-center justify-center gap-2 md:gap-4 mb-8">
            {[
              { n: 1, label: "Informations" },
              { n: 2, label: "Services & horaires" },
              { n: 3, label: "Paiement 100€" },
            ].map((s, i) => (
              <div key={s.n} className="flex items-center gap-2 md:gap-4">
                <div className="flex flex-col items-center">
                  <div
                    className={`w-10 h-10 rounded-full flex items-center justify-center font-semibold transition-colors ${
                      step >= s.n
                        ? "bg-primary text-primary-foreground"
                        : "bg-muted text-muted-foreground"
                    }`}
                  >
                    {step > s.n ? <CheckCircle2 className="h-5 w-5" /> : s.n}
                  </div>
                  <span className={`text-xs mt-2 hidden sm:block ${step >= s.n ? "text-foreground font-medium" : "text-muted-foreground"}`}>
                    {s.label}
                  </span>
                </div>
                {i < 2 && (
                  <div className={`h-0.5 w-8 md:w-16 ${step > s.n ? "bg-primary" : "bg-border"}`} />
                )}
              </div>
            ))}
          </div>

          {/* === STEP 1: Informations === */}
          {step === 1 && (
            <Card className="p-6 md:p-8">
              <div className="flex items-center gap-3 mb-6">
                <Building2 className="h-6 w-6 text-primary" />
                <h2 className="font-display text-2xl font-semibold">Informations de l'imprimerie</h2>
              </div>

              <div className="grid md:grid-cols-2 gap-4">
                <div className="space-y-2 md:col-span-2">
                  <Label htmlFor="shopName">Nom de l'imprimerie *</Label>
                  <Input id="shopName" placeholder="Ex: Imprimerie du Centre" value={shopName} onChange={(e) => setShopName(e.target.value)} />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="email">Email professionnel *</Label>
                  <div className="relative">
                    <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                    <Input id="email" type="email" className="pl-10" placeholder="contact@monimprimerie.be" value={email} onChange={(e) => setEmail(e.target.value)} />
                  </div>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="phone">Téléphone *</Label>
                  <div className="relative">
                    <Phone className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                    <Input id="phone" className="pl-10" placeholder="+32 2 123 45 67" value={phone} onChange={(e) => setPhone(e.target.value)} />
                  </div>
                </div>
                <div className="space-y-2 md:col-span-2">
                  <Label htmlFor="address">Adresse complète *</Label>
                  <div className="relative">
                    <MapPin className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                    <Input id="address" className="pl-10" placeholder="Rue, numéro, code postal, ville" value={address} onChange={(e) => setAddress(e.target.value)} />
                  </div>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="siret">N° TVA *</Label>
                  <Input id="siret" placeholder="BE0123456789" value={siret} onChange={(e) => setSiret(e.target.value)} />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="password">Mot de passe *</Label>
                  <div className="relative">
                    <Input id="password" type={showPassword ? "text" : "password"} placeholder="Min. 8 caractères, lettres et chiffres" value={password} onChange={(e) => setPassword(e.target.value)} className="pr-10" />
                    <button type="button" onClick={() => setShowPassword(!showPassword)} className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground">
                      {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                  </div>
                </div>

                <div className="space-y-2">
                    <Label htmlFor="confirmPassword">Confirmer le mot de passe *</Label>
                    <div className="relative">
                        <Input id="confirmPassword" type={showPassword ? "text" : "password"} placeholder="Répétez le mot de passe" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} className={confirmPassword && password !== confirmPassword ? "border-destructive pr-10" : "pr-10"} />
                        <button type="button" onClick={() => setShowPassword(!showPassword)} className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground">
                        {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                        </button>
                    </div>
                </div>

                <div className="space-y-2 md:col-span-2">
                  <Label htmlFor="description">Description de votre imprimerie *</Label>
                  <Textarea id="description" placeholder="Présentez votre boutique, spécialités, équipements..." rows={3} value={description} onChange={(e) => setDescription(e.target.value)} />
                </div>

                <div className="space-y-2 md:col-span-2">
                  <Label>Logo de l'imprimerie *</Label>
                  <label
                    htmlFor="logo-upload"
                    className="block border-2 border-dashed border-border rounded-lg p-6 text-center hover:border-primary/50 transition-colors cursor-pointer"
                  >
                    <input
                      id="logo-upload"
                      type="file"
                      accept="image/*"
                      className="hidden"
                      onChange={handleLogoChange}
                    />
                    {logoUploading ? (
                      <p className="text-sm text-muted-foreground">Envoi en cours…</p>
                    ) : logoUrl ? (
                      <div className="flex flex-col items-center gap-2">
                        <img
                          src={`http://localhost:8080${logoUrl}`}
                          alt="Logo de l'imprimerie"
                          className="h-16 w-16 object-contain rounded-md border border-border bg-background"
                        />
                        <p className="text-sm text-success">Logo envoyé — cliquez pour changer</p>
                      </div>
                    ) : (
                      <>
                        <Upload className="h-6 w-6 mx-auto text-muted-foreground mb-2" />
                        <p className="text-sm text-muted-foreground">Cliquez pour téléverser (PNG, JPG)</p>
                      </>
                    )}
                  </label>
                  {logoError && <p className="text-sm text-destructive">{logoError}</p>}
                </div>
              </div>

              <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mt-8">
                {getStep1Errors().length > 0 ? (
                  <p className="text-sm text-destructive">{getStep1Errors()[0]}</p>
                ) : <span />}
                <Button variant="default" size="lg" onClick={() => {
                  const missing = getStep1Errors();
                  if (missing.length > 0) {
                    toast({ title: "Formulaire invalide", description: `Merci de corriger : ${missing[0]}`, variant: "destructive" });
                    return;
                  }
                  setStep(2);
                }}>
                  Continuer
                  <ArrowRight className="h-4 w-4 ml-2" />
                </Button>
              </div>
            </Card>
          )}

          {/* === STEP 2: Services & horaires === */}
          {step === 2 && (
            <div className="space-y-6">
              <Card className="p-6 md:p-8">
                <div className="flex items-center gap-3 mb-6">
                  <Printer className="h-6 w-6 text-primary" />
                  <h2 className="font-display text-2xl font-semibold">Services proposés</h2>
                  <Badge variant="outline" className="ml-auto">
                    {enabledServicesCount} sélectionné{enabledServicesCount > 1 ? "s" : ""}
                  </Badge>
                </div>
                <p className="text-sm text-muted-foreground mb-6">
                  Cochez les services que vous proposez et configurez vos options de finition.
                </p>

                <div className="space-y-4">
                  {SERVICES.map((s) => {
                    const state = services[s.id];
                    
                    const canPlastify = s.typeProduit !== "POSTER";
                    const canBind = s.typeProduit === "DOCUMENT";
                    const showOptionsPanel = state.enabled && (canPlastify || canBind);

                    return (
                      <div key={s.id} className={`border rounded-lg transition-all ${state.enabled ? "border-primary shadow-sm" : "border-border"}`}>
                        <div className={`flex items-center gap-4 p-4 ${state.enabled && showOptionsPanel ? "bg-primary/5 border-b border-primary/20" : state.enabled ? "bg-primary/5" : ""}`}>
                          <Checkbox checked={state.enabled} onCheckedChange={() => toggleService(s.id)} />
                          <div className="flex-1">
                            <div className="font-medium">{s.name}</div>
                          </div>
                          <div className="flex items-center gap-2">
                            <Label className="text-xs text-muted-foreground hidden sm:block">Prix base</Label>
                            <Input type="number" step="0.01" value={state.price} onChange={(e) => updateServiceField(s.id, "price", e.target.value)} className="w-24 h-9" disabled={!state.enabled} />
                            <span className="text-sm text-muted-foreground">€</span>
                          </div>
                        </div>

                        {showOptionsPanel && (
                          <div className="p-4 bg-muted/20 space-y-6">
                            
                            {/* === Option Plastification === */}
                            {canPlastify && (
                              <div>
                                <div className="flex items-center gap-4 mb-3">
                                  <Checkbox id={`plast-${s.id}`} checked={state.proposePlastification} onCheckedChange={(v) => updateServiceField(s.id, "proposePlastification", Boolean(v))} />
                                  <div className="flex-1 flex items-center gap-2">
                                    <Layers className="h-4 w-4 text-muted-foreground" />
                                    <Label htmlFor={`plast-${s.id}`} className="cursor-pointer font-medium">Proposer Plastification</Label>
                                  </div>
                                </div>
                                
                                {state.proposePlastification && (
                                  <div className="ml-8 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                                    {Object.entries(state.prixParTypePlastification).map(([type, price]) => {
                                      const isActive = state.activePlastification[type];
                                      return (
                                        <div key={type} className={`flex items-center justify-between p-2 border rounded-md transition-colors ${isActive ? 'bg-background border-primary/40' : 'bg-muted/50 border-border opacity-70'}`}>
                                          <div className="flex items-center gap-2">
                                            <Checkbox checked={isActive} onCheckedChange={(v) => toggleOptionActive(s.id, "activePlastification", type, Boolean(v))} />
                                            <span className="text-xs font-medium">{formatEnumName(type)}</span>
                                          </div>
                                          <div className="flex items-center gap-1">
                                            <Input type="number" step="0.01" value={price as string} disabled={!isActive} onChange={(e) => updateOptionPrice(s.id, "prixParTypePlastification", type, e.target.value)} className="w-16 h-7 text-xs px-2" />
                                            <span className="text-xs text-muted-foreground">€</span>
                                          </div>
                                        </div>
                                      );
                                    })}
                                  </div>
                                )}
                              </div>
                            )}

                            {/* === Option Reliure === */}
                            {canBind && (
                              <div>
                                <div className="flex items-center gap-4 mb-3">
                                  <Checkbox id={`rel-${s.id}`} checked={state.proposeReliure} onCheckedChange={(v) => updateServiceField(s.id, "proposeReliure", Boolean(v))} />
                                  <div className="flex-1 flex items-center gap-2">
                                    <Book className="h-4 w-4 text-muted-foreground" />
                                    <Label htmlFor={`rel-${s.id}`} className="cursor-pointer font-medium">Proposer Reliure</Label>
                                  </div>
                                </div>

                                {state.proposeReliure && (
                                  <div className="ml-8 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                                    {Object.entries(state.prixParTypeReliure).map(([type, price]) => {
                                      const isActive = state.activeReliure[type];
                                      return (
                                        <div key={type} className={`flex items-center justify-between p-2 border rounded-md transition-colors ${isActive ? 'bg-background border-primary/40' : 'bg-muted/50 border-border opacity-70'}`}>
                                          <div className="flex items-center gap-2 overflow-hidden">
                                            <Checkbox checked={isActive} onCheckedChange={(v) => toggleOptionActive(s.id, "activeReliure", type, Boolean(v))} />
                                            <span className="text-xs font-medium truncate">{formatEnumName(type)}</span>
                                          </div>
                                          <div className="flex items-center gap-1 shrink-0">
                                            <Input type="number" step="0.01" value={price as string} disabled={!isActive} onChange={(e) => updateOptionPrice(s.id, "prixParTypeReliure", type, e.target.value)} className="w-16 h-7 text-xs px-2" />
                                            <span className="text-xs text-muted-foreground">€</span>
                                          </div>
                                        </div>
                                      );
                                    })}
                                  </div>
                                )}
                              </div>
                            )}

                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </Card>

              {/* Options supplémentaires */}
              <Card className="p-6 md:p-8">
                <div className="flex items-center gap-3 mb-6">
                  <Sparkles className="h-6 w-6 text-primary" />
                  <h2 className="font-display text-2xl font-semibold">Options supplémentaires</h2>
                </div>
                <div className="space-y-4">
                  <div className={`p-4 border rounded-lg transition-all ${offersExpress ? "border-primary bg-primary/5" : "border-border"}`}>
                    <div className="flex items-start gap-4">
                      <Checkbox id="opt-express" checked={offersExpress} onCheckedChange={(v) => setOffersExpress(Boolean(v))} className="mt-1" />
                      <div className="flex-1">
                        <Label htmlFor="opt-express" className="flex items-center gap-2 cursor-pointer font-medium">
                          <Zap className="h-4 w-4 text-primary" /> Express 2h
                        </Label>
                        <p className="text-sm text-muted-foreground mt-1">Besoin urgent ? Proposez l'impression en 2 heures.</p>
                      </div>
                      {offersExpress && (
                        <div className="flex items-center gap-2">
                          <Input type="number" step="0.50" min="0" value={expressPrice} onChange={(e) => setExpressPrice(e.target.value)} className="w-24 h-9" placeholder="Frais" />
                          <span className="text-sm text-muted-foreground">€</span>
                        </div>
                      )}
                    </div>
                  </div>

                  <div className={`p-4 border rounded-lg transition-all ${offersDelivery ? "border-primary bg-primary/5" : "border-border"}`}>
                    <div className="flex items-start gap-4">
                      <Checkbox id="opt-delivery" checked={offersDelivery} onCheckedChange={(v) => setOffersDelivery(Boolean(v))} className="mt-1" />
                      <div className="flex-1">
                        <Label htmlFor="opt-delivery" className="flex items-center gap-2 cursor-pointer font-medium">
                          <Truck className="h-4 w-4 text-primary" /> Livraison
                        </Label>
                      </div>
                      {offersDelivery && (
                        <div className="flex items-center gap-2">
                          <Input type="number" step="0.01" value={deliveryFee} onChange={(e) => setDeliveryFee(e.target.value)} className="w-24 h-9" placeholder="Frais" />
                          <span className="text-sm text-muted-foreground">€</span>
                        </div>
                      )}
                    </div>
                  </div>

                  <div className={`p-4 border rounded-lg transition-all ${offersStudentDiscount ? "border-primary bg-primary/5" : "border-border"}`}>
                    <div className="flex items-start gap-4">
                      <Checkbox id="opt-student" checked={offersStudentDiscount} onCheckedChange={(v) => setOffersStudentDiscount(Boolean(v))} className="mt-1" />
                      <div className="flex-1">
                        <Label htmlFor="opt-student" className="flex items-center gap-2 cursor-pointer font-medium">
                          <GraduationCap className="h-4 w-4 text-primary" /> Tarif étudiant
                        </Label>
                      </div>
                      {offersStudentDiscount && (
                        <div className="flex items-center gap-2">
                          <Input type="number" step="1" value={studentDiscountPct} onChange={(e) => setStudentDiscountPct(e.target.value)} className="w-20 h-9" />
                          <span className="text-sm text-muted-foreground">%</span>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              </Card>

              {/* HORAIRES */}
              <Card className="p-6 md:p-8">
                <div className="flex items-center gap-3 mb-6">
                  <Clock className="h-6 w-6 text-primary" />
                  <h2 className="font-display text-2xl font-semibold">Horaires d'ouverture</h2>
                </div>
                <div className="space-y-3">
                  {DAYS.map((d) => {
                    const h = hours[d.key];
                    return (
                      <div key={d.key} className="grid grid-cols-12 gap-3 items-center p-3 border border-border rounded-lg">
                        <div className="col-span-12 sm:col-span-3 font-medium">{d.label}</div>
                        <div className="col-span-12 sm:col-span-3 flex items-center gap-2">
                          <Checkbox checked={h.closed} onCheckedChange={(v) => updateHours(d.key, "closed", Boolean(v))} />
                          <span className="text-sm text-muted-foreground">Fermé</span>
                        </div>
                        <div className="col-span-6 sm:col-span-3">
                          <Input type="time" value={h.open} disabled={h.closed} onChange={(e) => updateHours(d.key, "open", e.target.value)} />
                        </div>
                        <div className="col-span-6 sm:col-span-3">
                          <Input type="time" value={h.close} disabled={h.closed} onChange={(e) => updateHours(d.key, "close", e.target.value)} />
                        </div>
                      </div>
                    );
                  })}
                </div>
              </Card>

              <div className="flex justify-between">
                <Button variant="outline" size="lg" onClick={() => setStep(1)}>
                  <ArrowLeft className="h-4 w-4 mr-2" /> Retour
                </Button>
                <Button variant="default" size="lg" disabled={!canGoStep3} onClick={() => setStep(3)}>
                  Continuer vers le paiement
                  <ArrowRight className="h-4 w-4 ml-2" />
                </Button>
              </div>
            </div>
          )}

          {/* === STEP 3: Paiement === */}
          {step === 3 && (
            <div className="grid md:grid-cols-3 gap-6">
              <Card className="p-6 md:p-8 md:col-span-2">
                <div className="flex items-center gap-3 mb-6">
                  <CreditCard className="h-6 w-6 text-primary" />
                  <h2 className="font-display text-2xl font-semibold">Paiement sécurisé</h2>
                  <Badge variant="outline" className="ml-auto gap-1">
                    <Lock className="h-3 w-3" /> Stripe
                  </Badge>
                </div>

                <Elements stripe={stripePromise}>
                  <StripePaymentForm 
                    payload={buildPayload()}
                    amount={100} 
                    onBack={() => setStep(2)} 
                    onPaymentSuccess={handlePaymentSuccess} 
                  />
                </Elements>
              </Card>

              <Card className="p-6 h-fit sticky top-24">
                <h3 className="font-display font-semibold text-lg mb-4">Récapitulatif</h3>
                <div className="space-y-3 text-sm mb-4">
                  <div>
                    <div className="text-muted-foreground">Imprimerie</div>
                    <div className="font-medium">{shopName || "—"}</div>
                  </div>
                  <div>
                    <div className="text-muted-foreground">Services activés</div>
                    <div className="font-medium">{enabledServicesCount}</div>
                  </div>
                </div>
                <Separator className="my-4" />
                <div className="flex justify-between items-baseline mb-4">
                  <span className="font-display font-semibold">Total</span>
                  <span className="font-display text-2xl font-bold">100.00€</span>
                </div>
              </Card>
            </div>
          )}
        </div>
      </main>
    </div>
  );
};

export default DevenirPartenaire;