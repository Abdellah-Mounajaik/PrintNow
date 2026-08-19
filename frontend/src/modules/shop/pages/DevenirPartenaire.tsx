import { useState, useEffect, useRef } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
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
import { SERVER_URL, STRIPE_PUBLIC_KEY } from "../../../lib/api";
import { paiementService } from "../services/paiement.service";
import { adresseService, LONGUEUR_MINIMALE } from "../../../services/adresse.service";
import type { SuggestionAdresse } from "../../../models/adresse.model";

import { partnerService } from "../services/partner.service";
import { type PartnerRegistrationRequest } from "../models/partner.model";
import { DAYS, SERVICES, type Hours } from "../models/partner.constants";
import { parametresService } from "../../../services/parametres.service";
import type { ParametresPlateforme } from "../../../models/parametres.model";

import {
  Printer, Building2, Mail, Phone, MapPin, Upload,
  CheckCircle2, CreditCard, Clock, ArrowRight, ArrowLeft,
  Lock, Sparkles, Truck, GraduationCap, Zap,
  Eye, EyeOff, Book, Layers, Loader2
} from "lucide-react";

// 👇 Imports Stripe
import { loadStripe } from "@stripe/stripe-js";
import { Elements, CardElement, useStripe, useElements } from "@stripe/react-stripe-js";

// 👇 Initialisation de Stripe 
const stripePromise = loadStripe(STRIPE_PUBLIC_KEY);

// =========================================================================
// SOUS-COMPOSANT : Formulaire Stripe sécurisé
// =========================================================================
const StripePaymentForm = ({ payload, onPaymentSuccess, onBack, amount }: any) => {
  const { t } = useTranslation("devenirPartenaire");
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
      // 1. Créer le PaymentIntent — aucune donnée n'est encore écrite en base à ce stade
      const clientSecret = await paiementService.creerIntention(amount);

      // 2. Confirmer le paiement par carte
      const result = await stripe.confirmCardPayment(clientSecret, {
        payment_method: {
          card: elements.getElement(CardElement)!,
        }
      });

      if (result.error) {
        setErrorMessage(result.error.message || t("payment.errors.paymentFailed"));
        return;
      }
      if (result.paymentIntent?.status !== "succeeded") {
        setErrorMessage(t("payment.errors.paymentNotConfirmed"));
        return;
      }

      // 3. Paiement confirmé côté client : le compte n'est créé que maintenant.
      // Le backend revérifie indépendamment le statut auprès de Stripe avant
      // d'écrire quoi que ce soit — aucune ligne orpheline possible en cas d'échec.
      await partnerService.register({ ...payload, paymentIntentId: result.paymentIntent.id });
      onPaymentSuccess();
    } catch (error: any) {
      // error.message vient du backend (échec de l'appel API) : on ne le traduit pas.
      setErrorMessage(error.message || t("payment.errors.communicationError"));
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
          <ArrowLeft className="h-4 w-4 mr-2" /> {t("payment.back")}
        </Button>
        <Button variant="default" type="submit" size="lg" disabled={!stripe || isProcessing}>
          {isProcessing ? t("payment.processing") : t("payment.payButton", { amount: amount.toFixed(2) })}
          {!isProcessing && <ArrowRight className="h-4 w-4 ml-2" />}
        </Button>
      </div>
    </form>
  );
};

// =========================================================================
// COMPOSANT PRINCIPAL
// =========================================================================
// Valeurs de repli affichées le temps que les tarifs réels soient chargés
// depuis le serveur — évite un flash à "0€" au premier rendu.
const TARIFS_PAR_DEFAUT: ParametresPlateforme = {
  commissionPourcentage: 10,
  fraisInscription: 100,
  prixCorrectionForfait: 2.9,
  pagesInclusesCorrection: 10,
  prixCorrectionPageSupp: 0.2,
  prixGenerationDesign: 4.9,
};

const DevenirPartenaire = () => {
  const { t } = useTranslation("devenirPartenaire");
  const navigate = useNavigate();
  const [step, setStep] = useState(1);
  const [isSuccess, setIsSuccess] = useState(false);

  // Frais d'inscription et commission : configurables par un administrateur,
  // affichés ici tels quels (jamais en dur) pour ne jamais mentir au partenaire
  // ni envoyer à Stripe un montant différent de celui réellement dû.
  const [tarifs, setTarifs] = useState<ParametresPlateforme>(TARIFS_PAR_DEFAUT);
  useEffect(() => {
    parametresService.getParametres().then(setTarifs).catch(() => {});
  }, []);
  const REGISTRATION_FEE = tarifs.fraisInscription;
  const COMMISSION_PERCENT = tarifs.commissionPourcentage;

  // Step 1
  const [shopName, setShopName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [address, setAddress] = useState("");
  const [ville, setVille] = useState("");
  const [pays, setPays] = useState("Belgique");
  const [siret, setSiret] = useState("");
  const [description, setDescription] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [logoUrl, setLogoUrl] = useState("");
  const [logoUploading, setLogoUploading] = useState(false);
  const [logoError, setLogoError] = useState("");
  const [checkingAvailability, setCheckingAvailability] = useState(false);

  const handleLogoChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const fichier = e.target.files?.[0];
    if (!fichier) return;
    setLogoUploading(true);
    setLogoError("");
    try {
      const url = await partnerService.uploadLogo(fichier);
      setLogoUrl(url);
    } catch (err) {
      // (err as Error).message vient du backend (échec de l'appel API) : on ne le traduit pas.
      setLogoError((err as Error).message || t("step1.logo.defaultError"));
      setLogoUrl("");
    } finally {
      setLogoUploading(false);
    }
  };

  // ── Autocomplétion d'adresse (Photon / OpenStreetMap, gratuit) ──────────────
  const [addressSuggestions, setAddressSuggestions] = useState<SuggestionAdresse[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [isSearchingAddress, setIsSearchingAddress] = useState(false);
  const suggestionsRef = useRef<HTMLDivElement>(null);
  const addressSelectedRef = useRef(false);

  useEffect(() => {
    // Ne relance pas de recherche juste après avoir cliqué sur une suggestion
    if (addressSelectedRef.current) {
      addressSelectedRef.current = false;
      return;
    }
    if (address.trim().length < LONGUEUR_MINIMALE) {
      setAddressSuggestions([]);
      return;
    }

    const timeoutId = setTimeout(async () => {
      setIsSearchingAddress(true);
      setAddressSuggestions(await adresseService.rechercher(address));
      setShowSuggestions(true);
      setIsSearchingAddress(false);
    }, 500); // debounce : on attend que le client arrête de taper

    return () => clearTimeout(timeoutId);
  }, [address]);

  // Ferme la liste de suggestions si on clique en dehors
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (suggestionsRef.current && !suggestionsRef.current.contains(e.target as Node)) {
        setShowSuggestions(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleSelectSuggestion = (s: SuggestionAdresse) => {
    addressSelectedRef.current = true;
    setAddress(s.adresse);
    setVille(s.ville);
    if (s.pays) setPays(s.pays);
    setShowSuggestions(false);
    setAddressSuggestions([]);
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
  const [rectoVersoDiscountPct, setRectoVersoDiscountPct] = useState("15");

  const enabledServicesCount = Object.values(services).filter((s) => s.enabled).length;

  const getStep1Errors = () => {
    const missing: string[] = [];
    if (!shopName.trim()) missing.push(t("step1.errors.shopName"));
    if (!email.trim()) missing.push(t("step1.errors.email"));
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) missing.push(t("step1.errors.emailInvalid"));
    if (!phone.trim()) missing.push(t("step1.errors.phone"));
    else if (!/^\+?[0-9 ()./-]{8,20}$/.test(phone)) missing.push(t("step1.errors.phoneInvalid"));
    if (!address.trim()) missing.push(t("step1.errors.address"));
    if (!ville.trim()) missing.push(t("step1.errors.city"));
    if (!siret.trim()) missing.push(t("step1.errors.vat"));
    if (!/^(?=.*[A-Za-z])(?=.*\d).{8,}$/.test(password)) missing.push(t("step1.errors.password"));
    if (password !== confirmPassword) missing.push(t("step1.errors.passwordMismatch"));
    if (!description.trim()) missing.push(t("step1.errors.description"));
    if (!logoUrl) missing.push(t("step1.errors.logo"));
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
          couleur: serviceInfo!.couleur,

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
        pourcentageRemiseRectoVerso: parseInt(rectoVersoDiscountPct) || 15,
        ville: ville,
        pays: pays,
      },
      produits: activeServices as any,
      horaires: activeHours
    };
  };

  const handlePaymentSuccess = () => {
    setIsSuccess(true);
    toast({
      title: t("success.toastTitle"),
      description: t("success.toastDescription"),
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
                {t("success.title")}
              </h1>
              <p className="text-muted-foreground mb-2">
                {t("success.paymentConfirmedBefore")}<strong>{t("feeAmount", { fee: REGISTRATION_FEE })}</strong>{t("success.paymentConfirmedAfter")}
              </p>
              <p className="text-muted-foreground mb-8">
                {t("success.descriptionBefore")}<strong>{shopName}</strong>{t("success.descriptionAfter")}
              </p>
              <div className="grid grid-cols-3 gap-3 mb-8 text-left">
                <div className="p-4 rounded-lg bg-muted/50">
                  <div className="text-xs text-muted-foreground">{t("success.stats.services")}</div>
                  <div className="font-display text-2xl font-bold">{enabledServicesCount}</div>
                </div>
                <div className="p-4 rounded-lg bg-muted/50">
                  <div className="text-xs text-muted-foreground">{t("success.stats.openDays")}</div>
                  <div className="font-display text-2xl font-bold">
                    {Object.values(hours).filter((h) => !h.closed).length}/7
                  </div>
                </div>
                <div className="p-4 rounded-lg bg-muted/50">
                  <div className="text-xs text-muted-foreground">{t("success.stats.status")}</div>
                  <div className="font-display text-sm font-bold text-success mt-1">{t("success.stats.statusActive")}</div>
                </div>
              </div>
              <div className="flex flex-col sm:flex-row gap-3 justify-center">
                <Button variant="default" size="lg" onClick={() => navigate("/dashboard-imprimeur")}>
                  {t("success.goToSpace")}
                  <ArrowRight className="h-4 w-4 ml-2" />
                </Button>
                <Button variant="outline" size="lg" asChild>
                  <Link to="/">{t("success.backHome")}</Link>
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
              {t("hero.title")}
            </h1>
            <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
              {t("hero.subtitleBeforeFee")}<strong>{t("feeAmount", { fee: REGISTRATION_FEE })}</strong>{t("hero.subtitleBetween")}<strong>{t("hero.commissionAmount", { commission: COMMISSION_PERCENT })}</strong>{t("hero.subtitleAfter")}
            </p>
          </div>

          {/* Stepper */}
          <div className="flex items-center justify-center gap-2 md:gap-4 mb-8">
            {(t("stepper.steps", { returnObjects: true, fee: REGISTRATION_FEE }) as unknown as string[]).map((label, i) => {
              const n = i + 1;
              return (
                <div key={n} className="flex items-center gap-2 md:gap-4">
                  <div className="flex flex-col items-center">
                    <div
                      className={`w-10 h-10 rounded-full flex items-center justify-center font-semibold transition-colors ${
                        step >= n
                          ? "bg-primary text-primary-foreground"
                          : "bg-muted text-muted-foreground"
                      }`}
                    >
                      {step > n ? <CheckCircle2 className="h-5 w-5" /> : n}
                    </div>
                    <span className={`text-xs mt-2 hidden sm:block ${step >= n ? "text-foreground font-medium" : "text-muted-foreground"}`}>
                      {label}
                    </span>
                  </div>
                  {i < 2 && (
                    <div className={`h-0.5 w-8 md:w-16 ${step > n ? "bg-primary" : "bg-border"}`} />
                  )}
                </div>
              );
            })}
          </div>

          {/* === STEP 1: Informations === */}
          {step === 1 && (
            <Card className="p-6 md:p-8">
              <div className="flex items-center gap-3 mb-6">
                <Building2 className="h-6 w-6 text-primary" />
                <h2 className="font-display text-2xl font-semibold">{t("step1.title")}</h2>
              </div>

              <div className="grid md:grid-cols-2 gap-4">
                <div className="space-y-2 md:col-span-2">
                  <Label htmlFor="shopName">{t("step1.shopName.label")}</Label>
                  <Input id="shopName" placeholder={t("step1.shopName.placeholder")} value={shopName} onChange={(e) => setShopName(e.target.value)} />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="email">{t("step1.email.label")}</Label>
                  <div className="relative">
                    <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                    <Input id="email" type="email" className="pl-10" placeholder={t("step1.email.placeholder")} value={email} onChange={(e) => setEmail(e.target.value)} />
                  </div>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="phone">{t("step1.phone.label")}</Label>
                  <div className="relative">
                    <Phone className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                    <Input id="phone" className="pl-10" placeholder={t("step1.phone.placeholder")} value={phone} onChange={(e) => setPhone(e.target.value)} />
                  </div>
                </div>
                <div className="space-y-2 md:col-span-2 relative" ref={suggestionsRef}>
                  <Label htmlFor="address">{t("step1.address.label")}</Label>
                  <div className="relative">
                    <MapPin className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                    <Input
                      id="address"
                      className="pl-10 pr-10"
                      placeholder={t("step1.address.placeholder")}
                      value={address}
                      onChange={(e) => setAddress(e.target.value)}
                      onFocus={() => addressSuggestions.length > 0 && setShowSuggestions(true)}
                      autoComplete="off"
                    />
                    {isSearchingAddress && (
                      <Loader2 className="absolute right-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground animate-spin" />
                    )}
                  </div>
                  {showSuggestions && addressSuggestions.length > 0 && (
                    <div className="absolute z-20 mt-1 w-full bg-card border border-border rounded-lg shadow-lg overflow-hidden">
                      {addressSuggestions.map((s, i) => (
                        <button
                          key={i}
                          type="button"
                          onClick={() => handleSelectSuggestion(s)}
                          className="w-full text-left px-3 py-2 text-sm hover:bg-muted transition-colors flex items-center gap-2"
                        >
                          <MapPin className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
                          {s.label}
                        </button>
                      ))}
                    </div>
                  )}
                </div>
                <div className="space-y-2">
                  <Label htmlFor="ville">{t("step1.city.label")}</Label>
                  <Input id="ville" placeholder={t("step1.city.placeholder")} value={ville} onChange={(e) => setVille(e.target.value)} />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="pays">{t("step1.country.label")}</Label>
                  <Input id="pays" placeholder={t("step1.country.placeholder")} value={pays} onChange={(e) => setPays(e.target.value)} />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="siret">{t("step1.vat.label")}</Label>
                  <Input id="siret" placeholder={t("step1.vat.placeholder")} value={siret} onChange={(e) => setSiret(e.target.value)} />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="password">{t("step1.password.label")}</Label>
                  <div className="relative">
                    <Input id="password" type={showPassword ? "text" : "password"} placeholder={t("step1.password.placeholder")} value={password} onChange={(e) => setPassword(e.target.value)} className="pr-10" />
                    <button type="button" onClick={() => setShowPassword(!showPassword)} className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground">
                      {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                  </div>
                </div>

                <div className="space-y-2">
                    <Label htmlFor="confirmPassword">{t("step1.confirmPassword.label")}</Label>
                    <div className="relative">
                        <Input id="confirmPassword" type={showPassword ? "text" : "password"} placeholder={t("step1.confirmPassword.placeholder")} value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} className={confirmPassword && password !== confirmPassword ? "border-destructive pr-10" : "pr-10"} />
                        <button type="button" onClick={() => setShowPassword(!showPassword)} className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground">
                        {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                        </button>
                    </div>
                </div>

                <div className="space-y-2 md:col-span-2">
                  <Label htmlFor="description">{t("step1.description.label")}</Label>
                  <Textarea id="description" placeholder={t("step1.description.placeholder")} rows={3} value={description} onChange={(e) => setDescription(e.target.value)} />
                </div>

                <div className="space-y-2 md:col-span-2">
                  <Label>{t("step1.logo.label")}</Label>
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
                      <p className="text-sm text-muted-foreground">{t("step1.logo.uploading")}</p>
                    ) : logoUrl ? (
                      <div className="flex flex-col items-center gap-2">
                        <img
                          src={`${SERVER_URL}${logoUrl}`}
                          alt={t("step1.logo.alt")}
                          className="h-16 w-16 object-contain rounded-md border border-border bg-background"
                        />
                        <p className="text-sm text-success">{t("step1.logo.uploaded")}</p>
                      </div>
                    ) : (
                      <>
                        <Upload className="h-6 w-6 mx-auto text-muted-foreground mb-2" />
                        <p className="text-sm text-muted-foreground">{t("step1.logo.cta")}</p>
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
                <Button variant="default" size="lg" disabled={checkingAvailability} onClick={async () => {
                  const missing = getStep1Errors();
                  if (missing.length > 0) {
                    toast({ title: t("step1.invalidFormTitle"), description: t("step1.invalidFormDescription", { error: missing[0] }), variant: "destructive" });
                    return;
                  }
                  setCheckingAvailability(true);
                  try {
                    await partnerService.verifierDisponibilite(email, siret);
                    setStep(2);
                  } catch (e: any) {
                    toast({ title: t("step1.unavailableTitle"), description: e.message, variant: "destructive" });
                  } finally {
                    setCheckingAvailability(false);
                  }
                }}>
                  {checkingAvailability ? t("step1.checking") : t("step1.continue")}
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
                  <h2 className="font-display text-2xl font-semibold">{t("step2.services.title")}</h2>
                  <Badge variant="outline" className="ml-auto">
                    {t("step2.services.selectedCount", { count: enabledServicesCount })}
                  </Badge>
                </div>
                <p className="text-sm text-muted-foreground mb-6">
                  {t("step2.services.hint")}
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
                            <div className="font-medium">{t(`servicesCatalog.${s.id}`)}</div>
                          </div>
                          <div className="flex items-center gap-2">
                            <Label className="text-xs text-muted-foreground hidden sm:block">{t("step2.services.basePrice")}</Label>
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
                                    <Label htmlFor={`plast-${s.id}`} className="cursor-pointer font-medium">{t("step2.services.offerLamination")}</Label>
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
                                            <span className="text-xs font-medium">{t(`step2.services.laminationTypes.${type}`)}</span>
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
                                    <Label htmlFor={`rel-${s.id}`} className="cursor-pointer font-medium">{t("step2.services.offerBinding")}</Label>
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
                                            <span className="text-xs font-medium truncate">{t(`step2.services.bindingTypes.${type}`)}</span>
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
                  <h2 className="font-display text-2xl font-semibold">{t("step2.extraOptions.title")}</h2>
                </div>
                <div className="space-y-4">
                  <div className={`p-4 border rounded-lg transition-all ${offersExpress ? "border-primary bg-primary/5" : "border-border"}`}>
                    <div className="flex items-start gap-4">
                      <Checkbox id="opt-express" checked={offersExpress} onCheckedChange={(v) => setOffersExpress(Boolean(v))} className="mt-1" />
                      <div className="flex-1">
                        <Label htmlFor="opt-express" className="flex items-center gap-2 cursor-pointer font-medium">
                          <Zap className="h-4 w-4 text-primary" /> {t("step2.extraOptions.express.label")}
                        </Label>
                        <p className="text-sm text-muted-foreground mt-1">{t("step2.extraOptions.express.description")}</p>
                      </div>
                      {offersExpress && (
                        <div className="flex items-center gap-2">
                          <Input type="number" step="0.50" min="0" value={expressPrice} onChange={(e) => setExpressPrice(e.target.value)} className="w-24 h-9" placeholder={t("step2.extraOptions.express.feePlaceholder")} />
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
                          <Truck className="h-4 w-4 text-primary" /> {t("step2.extraOptions.delivery.label")}
                        </Label>
                      </div>
                      {offersDelivery && (
                        <div className="flex items-center gap-2">
                          <Input type="number" step="0.01" value={deliveryFee} onChange={(e) => setDeliveryFee(e.target.value)} className="w-24 h-9" placeholder={t("step2.extraOptions.delivery.feePlaceholder")} />
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
                          <GraduationCap className="h-4 w-4 text-primary" /> {t("step2.extraOptions.studentDiscount.label")}
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

                  <div className="p-4 border border-border rounded-lg">
                    <div className="flex items-start gap-4">
                      <Layers className="h-4 w-4 text-primary mt-1" />
                      <div className="flex-1">
                        <Label className="font-medium">{t("step2.extraOptions.duplexDiscount.label")}</Label>
                        <p className="text-sm text-muted-foreground mt-1">{t("step2.extraOptions.duplexDiscount.description")}</p>
                      </div>
                      <div className="flex items-center gap-2">
                        <Input type="number" step="1" min="0" max="100" value={rectoVersoDiscountPct} onChange={(e) => setRectoVersoDiscountPct(e.target.value)} className="w-20 h-9" />
                        <span className="text-sm text-muted-foreground">%</span>
                      </div>
                    </div>
                  </div>
                </div>
              </Card>

              {/* HORAIRES */}
              <Card className="p-6 md:p-8">
                <div className="flex items-center gap-3 mb-6">
                  <Clock className="h-6 w-6 text-primary" />
                  <h2 className="font-display text-2xl font-semibold">{t("step2.hours.title")}</h2>
                </div>
                <div className="space-y-3">
                  {DAYS.map((d) => {
                    const h = hours[d.key];
                    return (
                      <div key={d.key} className="grid grid-cols-12 gap-3 items-center p-3 border border-border rounded-lg">
                        <div className="col-span-12 sm:col-span-3 font-medium">{t(`daysOfWeek.${d.key}`)}</div>
                        <div className="col-span-12 sm:col-span-3 flex items-center gap-2">
                          <Checkbox checked={h.closed} onCheckedChange={(v) => updateHours(d.key, "closed", Boolean(v))} />
                          <span className="text-sm text-muted-foreground">{t("step2.hours.closed")}</span>
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
                  <ArrowLeft className="h-4 w-4 mr-2" /> {t("step2.back")}
                </Button>
                <Button variant="default" size="lg" disabled={!canGoStep3} onClick={() => setStep(3)}>
                  {t("step2.continueToPayment")}
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
                  <h2 className="font-display text-2xl font-semibold">{t("step3.title")}</h2>
                  <Badge variant="outline" className="ml-auto gap-1">
                    <Lock className="h-3 w-3" /> Stripe
                  </Badge>
                </div>

                <Elements stripe={stripePromise}>
                  <StripePaymentForm
                    payload={buildPayload()}
                    amount={REGISTRATION_FEE}
                    onBack={() => setStep(2)}
                    onPaymentSuccess={handlePaymentSuccess}
                  />
                </Elements>
              </Card>

              <Card className="p-6 h-fit sticky top-24">
                <h3 className="font-display font-semibold text-lg mb-4">{t("step3.summary.title")}</h3>
                <div className="space-y-3 text-sm mb-4">
                  <div>
                    <div className="text-muted-foreground">{t("step3.summary.shop")}</div>
                    <div className="font-medium">{shopName || "—"}</div>
                  </div>
                  <div>
                    <div className="text-muted-foreground">{t("step3.summary.servicesActive")}</div>
                    <div className="font-medium">{enabledServicesCount}</div>
                  </div>
                </div>
                <Separator className="my-4" />
                <div className="flex justify-between items-baseline mb-4">
                  <span className="font-display font-semibold">{t("step3.summary.total")}</span>
                  <span className="font-display text-2xl font-bold">{t("feeAmount", { fee: REGISTRATION_FEE.toFixed(2) })}</span>
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