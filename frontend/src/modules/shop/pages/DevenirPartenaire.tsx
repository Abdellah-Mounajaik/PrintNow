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

// 👇 Imports API et Modèles
import { partnerService } from "../services/partner.service";
import { 
  type PartnerRegistrationRequest, 
  TypeProduit, 
  FormatImpression 
} from "../models/partner.model";

import {
  Printer, Building2, Mail, Phone, MapPin, Upload,
  CheckCircle2, CreditCard, Clock, ArrowRight, ArrowLeft,
  Lock, Sparkles, Users, TrendingUp, Truck, GraduationCap,
} from "lucide-react";

const DAYS = [
  { key: "mon", label: "Lundi" }, { key: "tue", label: "Mardi" },
  { key: "wed", label: "Mercredi" }, { key: "thu", label: "Jeudi" },
  { key: "fri", label: "Vendredi" }, { key: "sat", label: "Samedi" },
  { key: "sun", label: "Dimanche" },
];

// 👇 Adaptation aux Enums de ton backend Java
const SERVICES = [
  { id: "bw-a4", name: "Impression N&B A4", typeProduit: TypeProduit.DOCUMENT, formatImpression: FormatImpression.A4, defaultPrice: "0.10" },
  { id: "color-a4", name: "Impression couleur A4", typeProduit: TypeProduit.DOCUMENT, formatImpression: FormatImpression.A4, defaultPrice: "0.30" },
  { id: "bw-a3", name: "Impression N&B A3", typeProduit: TypeProduit.DOCUMENT, formatImpression: FormatImpression.A3, defaultPrice: "0.50" },
  { id: "color-a3", name: "Impression couleur A3", typeProduit: TypeProduit.DOCUMENT, formatImpression: FormatImpression.A3, defaultPrice: "0.80" },
  { id: "business-cards", name: "Cartes de visite", typeProduit: TypeProduit.CARTE_VISITE, formatImpression: FormatImpression.CARTE_VISITE_85x55, defaultPrice: "25.00" },
  { id: "flyers", name: "Flyers / Dépliants", typeProduit: TypeProduit.FLYER, formatImpression: FormatImpression.A5, defaultPrice: "0.15" },
  { id: "posters", name: "Affiches grand format", typeProduit: TypeProduit.POSTER, formatImpression: FormatImpression.A2, defaultPrice: "8.00" },
];

type Hours = { open: string; close: string; closed: boolean };
type ServiceState = { enabled: boolean; price: string };

const DevenirPartenaire = () => {
  const navigate = useNavigate();
  const [step, setStep] = useState(1);
  const [isProcessing, setIsProcessing] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);

  // Step 1
  const [shopName, setShopName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [address, setAddress] = useState("");
  const [siret, setSiret] = useState("");
  const [description, setDescription] = useState("");
  const [password, setPassword] = useState("");

  // Step 2 - services
  const [services, setServices] = useState<Record<string, ServiceState>>(
    Object.fromEntries(SERVICES.map((s) => [s.id, { enabled: false, price: s.defaultPrice }]))
  );

  // Step 2 - hours
  const [hours, setHours] = useState<Record<string, Hours>>(
    Object.fromEntries(DAYS.map((d) => [d.key, { open: "09:00", close: "18:00", closed: d.key === "sun" }]))
  );

  // Step 2 - extra options
  const [offersDelivery, setOffersDelivery] = useState(false);
  const [deliveryFee, setDeliveryFee] = useState("5.00");
  const [offersStudentDiscount, setOffersStudentDiscount] = useState(false);
  const [studentDiscountPct, setStudentDiscountPct] = useState("15");

  // Step 3 - payment mock
  const [cardNumber, setCardNumber] = useState("");
  const [cardExpiry, setCardExpiry] = useState("");
  const [cardCVC, setCardCVC] = useState("");
  const [cardName, setCardName] = useState("");

  const enabledServicesCount = Object.values(services).filter((s) => s.enabled).length;

  const getStep1Errors = () => {
    const missing: string[] = [];
    if (!shopName.trim()) missing.push("nom de l'imprimerie");
    if (!email.trim()) missing.push("email");
    if (!phone.trim()) missing.push("téléphone");
    if (!address.trim()) missing.push("adresse");
    if (password.length < 6) missing.push("mot de passe (min. 6 caractères)");
    return missing;
  };
  
  const canGoStep3 = enabledServicesCount >= 1;
  const canPay = cardNumber.length >= 16 && cardExpiry && cardCVC.length >= 3 && cardName;

  const toggleService = (id: string) => {
    setServices((prev) => ({ ...prev, [id]: { ...prev[id], enabled: !prev[id].enabled } }));
  };

  const updatePrice = (id: string, price: string) => {
    setServices((prev) => ({ ...prev, [id]: { ...prev[id], price } }));
  };

  const updateHours = (day: string, field: keyof Hours, value: string | boolean) => {
    setHours((prev) => ({ ...prev, [day]: { ...prev[day], [field]: value } }));
  };

  const handlePayment = async () => {
    setIsProcessing(true);
    
    try {
      // 1. Préparation des Produits
      const activeServices = Object.entries(services)
        .filter(([_, state]) => state.enabled)
        .map(([id, state]) => {
          const serviceInfo = SERVICES.find(s => s.id === id);
          return {
            typeProduit: serviceInfo!.typeProduit,
            formatImpression: serviceInfo!.formatImpression,
            prixBase: parseFloat(state.price),
            prixParPage: 0 // Ajustable si tu as un champ pour ça plus tard
          };
        });

      // 2. Préparation des Horaires
      const daysMap: Record<string, string> = {
        mon: "LUNDI", tue: "MARDI", wed: "MERCREDI", 
        thu: "JEUDI", fri: "VENDREDI", sat: "SAMEDI", sun: "DIMANCHE"
      };
      
      const activeHours = Object.entries(hours).map(([key, h]) => ({
        jourSemaine: daysMap[key],
        heureOuverture: h.open + ":00", // On ajoute les secondes pour Java LocalTime
        heureFermeture: h.close + ":00",
        ferme: h.closed
      }));

      // 3. Construction de l'objet DTO Final
      const payload: PartnerRegistrationRequest = {
        email: email,
        password: password,
        siret: siret,
        imprimerie: {
          nom: shopName,
          telephoneContact: phone,
          emailContact: email,
          adresse: address,
          description: description,
          livraisonActive: offersDelivery,
          ville: "Non spécifiée", // Tu pourras rajouter un champ Ville dans ton UI plus tard
          pays: "Belgique"
        },
        produits: activeServices,
        horaires: activeHours
      };

      // 4. Appel au backend
      await partnerService.register(payload);

      setIsSuccess(true);
      toast({
        title: "Paiement validé ✅",
        description: "Votre imprimerie est désormais active dans le catalogue !",
      });

    } catch (error: any) {
      toast({
        title: "Erreur",
        description: error.message || "Une erreur est survenue lors de l'inscription.",
        variant: "destructive"
      });
    } finally {
      setIsProcessing(false);
    }
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
                Bienvenue sur PrintHub ! 🎉
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
                <Button variant="default" size="lg" onClick={() => navigate("/dashboard")}>
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
            <Badge className="mb-4 bg-secondary/10 text-secondary border-secondary/20">
              <Sparkles className="h-3 w-3 mr-1 inline" />
              Inscription 100% automatique
            </Badge>
            <h1 className="font-display text-3xl md:text-5xl font-bold mb-4">
              Devenez imprimerie partenaire
            </h1>
            <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
              Inscription unique de <strong>100€</strong>, ensuite seulement <strong>10% de commission</strong> par commande.
              Votre boutique est activée immédiatement après paiement.
            </p>
          </div>

          {/* Benefits strip */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-10">
            {[
              { icon: Users, label: "Nouveaux clients" },
              { icon: TrendingUp, label: "+CA garanti" },
              { icon: Clock, label: "Activation instantanée" },
              { icon: Lock, label: "Paiement sécurisé" },
            ].map((b, i) => (
              <div key={i} className="flex items-center gap-3 p-4 rounded-xl bg-card border border-border">
                <div className="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center">
                  <b.icon className="h-5 w-5 text-primary" />
                </div>
                <span className="text-sm font-medium">{b.label}</span>
              </div>
            ))}
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
                  <Input
                    id="shopName"
                    placeholder="Ex: Imprimerie du Centre"
                    value={shopName}
                    onChange={(e) => setShopName(e.target.value)}
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="email">Email professionnel *</Label>
                  <div className="relative">
                    <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                    <Input
                      id="email"
                      type="email"
                      className="pl-10"
                      placeholder="contact@monimprimerie.be"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                    />
                  </div>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="phone">Téléphone *</Label>
                  <div className="relative">
                    <Phone className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                    <Input
                      id="phone"
                      className="pl-10"
                      placeholder="+32 2 123 45 67"
                      value={phone}
                      onChange={(e) => setPhone(e.target.value)}
                    />
                  </div>
                </div>

                <div className="space-y-2 md:col-span-2">
                  <Label htmlFor="address">Adresse complète *</Label>
                  <div className="relative">
                    <MapPin className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                    <Input
                      id="address"
                      className="pl-10"
                      placeholder="Rue, numéro, code postal, ville"
                      value={address}
                      onChange={(e) => setAddress(e.target.value)}
                    />
                  </div>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="siret">N° TVA / SIRET</Label>
                  <Input
                    id="siret"
                    placeholder="BE0123456789"
                    value={siret}
                    onChange={(e) => setSiret(e.target.value)}
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="password">Mot de passe (compte imprimeur) *</Label>
                  <Input
                    id="password"
                    type="password"
                    placeholder="Min. 6 caractères"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                  />
                </div>

                <div className="space-y-2 md:col-span-2">
                  <Label htmlFor="description">Description de votre imprimerie</Label>
                  <Textarea
                    id="description"
                    placeholder="Présentez votre boutique, spécialités, équipements..."
                    rows={3}
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                  />
                </div>

                <div className="space-y-2 md:col-span-2">
                  <Label>Logo de l'imprimerie</Label>
                  <div className="border-2 border-dashed border-border rounded-lg p-6 text-center hover:border-primary/50 transition-colors cursor-pointer">
                    <Upload className="h-6 w-6 mx-auto text-muted-foreground mb-2" />
                    <p className="text-sm text-muted-foreground">Cliquez pour téléverser (PNG, JPG)</p>
                  </div>
                </div>
              </div>

              {(() => {
                const missing = getStep1Errors();
                return (
                  <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mt-8">
                    {missing.length > 0 ? (
                      <p className="text-sm text-destructive">
                        Champs requis manquants : {missing.join(", ")}.
                      </p>
                    ) : (
                      <span />
                    )}
                    <Button
                      variant="default"
                      size="lg"
                      onClick={() => {
                        if (missing.length > 0) {
                          toast({
                            title: "Formulaire incomplet",
                            description: `Merci de compléter : ${missing.join(", ")}`,
                            variant: "destructive",
                          });
                          return;
                        }
                        setStep(2);
                      }}
                    >
                      Continuer
                      <ArrowRight className="h-4 w-4 ml-2" />
                    </Button>
                  </div>
                );
              })()}
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
                  Cochez les services que vous proposez et fixez vos tarifs.
                </p>

                <div className="space-y-3">
                  {SERVICES.map((s) => {
                    const state = services[s.id];
                    return (
                      <div
                        key={s.id}
                        className={`flex items-center gap-4 p-4 border rounded-lg transition-all ${
                          state.enabled ? "border-primary bg-primary/5" : "border-border"
                        }`}
                      >
                        <Checkbox
                          checked={state.enabled}
                          onCheckedChange={() => toggleService(s.id)}
                        />
                        <div className="flex-1">
                          <div className="font-medium">{s.name}</div>
                        </div>
                        <div className="flex items-center gap-2">
                          <Input
                            type="number"
                            step="0.01"
                            value={state.price}
                            onChange={(e) => updatePrice(s.id, e.target.value)}
                            className="w-24 h-9"
                            disabled={!state.enabled}
                          />
                          <span className="text-sm text-muted-foreground">€</span>
                        </div>
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
                <p className="text-sm text-muted-foreground mb-6">
                  Choisissez si vous souhaitez proposer ces services additionnels à vos clients.
                </p>

                <div className="space-y-4">
                  {/* Livraison */}
                  <div
                    className={`p-4 border rounded-lg transition-all ${
                      offersDelivery ? "border-primary bg-primary/5" : "border-border"
                    }`}
                  >
                    <div className="flex items-start gap-4">
                      <Checkbox
                        id="opt-delivery"
                        checked={offersDelivery}
                        onCheckedChange={(v) => setOffersDelivery(Boolean(v))}
                        className="mt-1"
                      />
                      <div className="flex-1">
                        <Label htmlFor="opt-delivery" className="flex items-center gap-2 cursor-pointer font-medium">
                          <Truck className="h-4 w-4 text-primary" />
                          Proposer la livraison
                        </Label>
                        <p className="text-sm text-muted-foreground mt-1">
                          Livrez les commandes directement chez vos clients.
                        </p>
                      </div>
                      {offersDelivery && (
                        <div className="flex items-center gap-2">
                          <Input
                            type="number"
                            step="0.01"
                            value={deliveryFee}
                            onChange={(e) => setDeliveryFee(e.target.value)}
                            className="w-24 h-9"
                            placeholder="Frais"
                          />
                          <span className="text-sm text-muted-foreground">€</span>
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Tarif étudiant */}
                  <div
                    className={`p-4 border rounded-lg transition-all ${
                      offersStudentDiscount ? "border-primary bg-primary/5" : "border-border"
                    }`}
                  >
                    <div className="flex items-start gap-4">
                      <Checkbox
                        id="opt-student"
                        checked={offersStudentDiscount}
                        onCheckedChange={(v) => setOffersStudentDiscount(Boolean(v))}
                        className="mt-1"
                      />
                      <div className="flex-1">
                        <Label htmlFor="opt-student" className="flex items-center gap-2 cursor-pointer font-medium">
                          <GraduationCap className="h-4 w-4 text-primary" />
                          Proposer le tarif étudiant
                        </Label>
                        <p className="text-sm text-muted-foreground mt-1">
                          Réduction automatique pour les étudiants (désactivée fin juin).
                        </p>
                      </div>
                      {offersStudentDiscount && (
                        <div className="flex items-center gap-2">
                          <Input
                            type="number"
                            step="1"
                            min="1"
                            max="50"
                            value={studentDiscountPct}
                            onChange={(e) => setStudentDiscountPct(e.target.value)}
                            className="w-20 h-9"
                          />
                          <span className="text-sm text-muted-foreground">%</span>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              </Card>

              <Card className="p-6 md:p-8">
                <div className="flex items-center gap-3 mb-6">
                  <Clock className="h-6 w-6 text-primary" />
                  <h2 className="font-display text-2xl font-semibold">Horaires d'ouverture</h2>
                </div>

                <div className="space-y-3">
                  {DAYS.map((d) => {
                    const h = hours[d.key];
                    return (
                      <div
                        key={d.key}
                        className="grid grid-cols-12 gap-3 items-center p-3 border border-border rounded-lg"
                      >
                        <div className="col-span-12 sm:col-span-3 font-medium">{d.label}</div>
                        <div className="col-span-12 sm:col-span-3 flex items-center gap-2">
                          <Checkbox
                            checked={h.closed}
                            onCheckedChange={(v) => updateHours(d.key, "closed", Boolean(v))}
                          />
                          <span className="text-sm text-muted-foreground">Fermé</span>
                        </div>
                        <div className="col-span-6 sm:col-span-3">
                          <Input
                            type="time"
                            value={h.open}
                            disabled={h.closed}
                            onChange={(e) => updateHours(d.key, "open", e.target.value)}
                          />
                        </div>
                        <div className="col-span-6 sm:col-span-3">
                          <Input
                            type="time"
                            value={h.close}
                            disabled={h.closed}
                            onChange={(e) => updateHours(d.key, "close", e.target.value)}
                          />
                        </div>
                      </div>
                    );
                  })}
                </div>
              </Card>

              <div className="flex justify-between">
                <Button variant="outline" size="lg" onClick={() => setStep(1)}>
                  <ArrowLeft className="h-4 w-4 mr-2" />
                  Retour
                </Button>
                <Button
                  variant="default"
                  size="lg"
                  disabled={!canGoStep3}
                  onClick={() => setStep(3)}
                >
                  Continuer vers le paiement
                  <ArrowRight className="h-4 w-4 ml-2" />
                </Button>
              </div>
            </div>
          )}

          {/* === STEP 3: Paiement mock === */}
          {step === 3 && (
            <div className="grid md:grid-cols-3 gap-6">
              {/* Payment form */}
              <Card className="p-6 md:p-8 md:col-span-2">
                <div className="flex items-center gap-3 mb-6">
                  <CreditCard className="h-6 w-6 text-primary" />
                  <h2 className="font-display text-2xl font-semibold">Paiement sécurisé</h2>
                  <Badge variant="outline" className="ml-auto gap-1">
                    <Lock className="h-3 w-3" />
                    SSL
                  </Badge>
                </div>

                <div className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="cardName">Nom sur la carte</Label>
                    <Input
                      id="cardName"
                      placeholder="Jean Dupont"
                      value={cardName}
                      onChange={(e) => setCardName(e.target.value)}
                    />
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="cardNumber">Numéro de carte</Label>
                    <div className="relative">
                      <CreditCard className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                      <Input
                        id="cardNumber"
                        className="pl-10 font-mono"
                        placeholder="4242 4242 4242 4242"
                        maxLength={19}
                        value={cardNumber}
                        onChange={(e) =>
                          setCardNumber(e.target.value.replace(/\D/g, "").slice(0, 16))
                        }
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="cardExpiry">Expiration (MM/AA)</Label>
                      <Input
                        id="cardExpiry"
                        placeholder="12/27"
                        maxLength={5}
                        value={cardExpiry}
                        onChange={(e) => setCardExpiry(e.target.value)}
                      />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="cardCVC">CVC</Label>
                      <Input
                        id="cardCVC"
                        placeholder="123"
                        maxLength={4}
                        value={cardCVC}
                        onChange={(e) => setCardCVC(e.target.value.replace(/\D/g, ""))}
                      />
                    </div>
                  </div>

                  <div className="flex items-start gap-2 p-3 rounded-lg bg-muted/50 text-xs text-muted-foreground">
                    <Lock className="h-4 w-4 mt-0.5 shrink-0" />
                    <span>
                      Ceci est un paiement de démonstration. Aucune transaction réelle n'est effectuée.
                      Utilisez 4242 4242 4242 4242 pour simuler un paiement réussi.
                    </span>
                  </div>
                </div>

                <div className="flex justify-between mt-8">
                  <Button variant="outline" size="lg" onClick={() => setStep(2)} disabled={isProcessing}>
                    <ArrowLeft className="h-4 w-4 mr-2" />
                    Retour
                  </Button>
                  <Button
                    variant="default"
                    size="lg"
                    disabled={!canPay || isProcessing}
                    onClick={handlePayment}
                  >
                    {isProcessing ? "Traitement..." : "Payer 100€ et activer"}
                    {!isProcessing && <ArrowRight className="h-4 w-4 ml-2" />}
                  </Button>
                </div>
              </Card>

              {/* Summary */}
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
                  <div>
                    <div className="text-muted-foreground">Jours d'ouverture</div>
                    <div className="font-medium">
                      {Object.values(hours).filter((h) => !h.closed).length}/7
                    </div>
                  </div>
                </div>

                <Separator className="my-4" />

                <div className="space-y-2 text-sm">
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Inscription plateforme</span>
                    <span>100.00€</span>
                  </div>
                  <div className="flex justify-between text-xs text-muted-foreground">
                    <span>Paiement unique</span>
                    <span>—</span>
                  </div>
                </div>

                <Separator className="my-4" />

                <div className="flex justify-between items-baseline mb-4">
                  <span className="font-display font-semibold">Total</span>
                  <span className="font-display text-2xl font-bold">100.00€</span>
                </div>

                <div className="p-3 rounded-lg bg-success/10 text-success text-xs">
                  <CheckCircle2 className="h-4 w-4 inline mr-1" />
                  Activation automatique après paiement
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