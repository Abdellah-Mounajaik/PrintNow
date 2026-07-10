import { useState, useEffect } from "react";
import Header from "../../components/layout/Header";
import { Button } from "../../components/ui/button";
import { Card } from "../../components/ui/card";
import { Badge } from "../../components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../../components/ui/tabs";
import { Switch } from "../../components/ui/switch";
import { Input } from "../../components/ui/input";
import { Label } from "../../components/ui/label";
import { Textarea } from "../../components/ui/textarea";
import { Checkbox } from "../../components/ui/checkbox";
import {
  Package, Euro, Clock, Star,
  Store, Truck, Layers, Book, Printer,
  Zap, GraduationCap, ChevronDown, ChevronUp,
  FileText, CheckCircle, RotateCcw, Tag, Trash2
} from "lucide-react";
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from "../../components/ui/select";
import { toast } from "../../hooks/use-toast";

import { imprimerieService } from "../shop/services/imprimerieService.service";
import type { ImprimerieDetail } from "../shop/models/Imprimerie.model";
import { useAuth } from "../auth/context/AuthContext";
import { SERVICES } from "../shop/models/partner.constants";

// ─── Constantes options de finition ──────────────────────────────────────────
const DEFAULT_PLAST_PRICES = { MAT: "0.50", BRILLANT: "0.60", SOFT_TOUCH: "0.80" };
const DEFAULT_REL_PRICES = {
  SPIRALE_PLASTIQUE: "1.50", SPIRALE_METALLIQUE: "2.50",
  DOS_CARRE_COLLE: "3.50", AGRAFE_DEUX_POINTS: "0.50", THERMIQUE: "3.00",
};

// ─── Initialise l'état des services depuis les produits existants ─────────────
const initServicesFromProduits = (produits: any[]) => {
  const grouped: Record<string, any[]> = {};
  for (const p of produits) {
    const key = `${p.typeProduit}_${p.formatImpression}`;
    if (!grouped[key]) grouped[key] = [];
    grouped[key].push(p);
  }
  for (const k of Object.keys(grouped)) {
    grouped[k].sort((a: any, b: any) => a.prixBase - b.prixBase);
  }
  const claimedIds = new Set<number>();

  return Object.fromEntries(SERVICES.map((s) => {
    const key = `${s.typeProduit}_${s.formatImpression}`;
    const match = (grouped[key] || []).find((p: any) => !claimedIds.has(p.id));
    if (match) claimedIds.add(match.id);

    const prixParTypePlastification = { ...DEFAULT_PLAST_PRICES };
    const activePlastification: Record<string, boolean> = { MAT: false, BRILLANT: false, SOFT_TOUCH: false };
    if (match?.prixParTypePlastification) {
      for (const [type, prix] of Object.entries(match.prixParTypePlastification)) {
        if (type in activePlastification) {
          activePlastification[type] = true;
          prixParTypePlastification[type as keyof typeof DEFAULT_PLAST_PRICES] = String(prix);
        }
      }
    }

    const prixParTypeReliure = { ...DEFAULT_REL_PRICES };
    const activeReliure: Record<string, boolean> = {
      SPIRALE_PLASTIQUE: false, SPIRALE_METALLIQUE: false,
      DOS_CARRE_COLLE: false, AGRAFE_DEUX_POINTS: false, THERMIQUE: false,
    };
    if (match?.prixParTypeReliure) {
      for (const [type, prix] of Object.entries(match.prixParTypeReliure)) {
        if (type in activeReliure) {
          activeReliure[type] = true;
          prixParTypeReliure[type as keyof typeof DEFAULT_REL_PRICES] = String(prix);
        }
      }
    }

    return [s.id, {
      existingProduitId: match?.id ?? null,
      existingActif: match?.actif ?? null,
      enabled: match ? !!match.actif : false,
      price: match ? String(match.prixBase) : s.defaultPrice,
      proposePlastification: !!match?.proposePlastification,
      activePlastification,
      prixParTypePlastification,
      proposeReliure: !!match?.proposeReliure,
      activeReliure,
      prixParTypeReliure,
    }];
  }));
};

// ─── Composant principal ──────────────────────────────────────────────────────
const DashboardImprimeur = () => {
  const { user, token } = useAuth();

  const [shop, setShop] = useState<ImprimerieDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [orders, setOrders] = useState<any[]>([]);
  const [expandedOrderId, setExpandedOrderId] = useState<number | null>(null);
  const [promos, setPromos] = useState<any[]>([]);
  const [promoForm, setPromoForm] = useState({ code: "", typeReduction: "POURCENTAGE", valeurReduction: "", dateFin: "", utilisationMax: "", montantMinimumCommande: "" });
  const [updatingStatutId, setUpdatingStatutId] = useState<number | null>(null);
  const [suiviInputs, setSuiviInputs] = useState<Record<number, string>>({});
  const [suiviSaving, setSuiviSaving] = useState<number | null>(null);

  // Services
  const [servicesState, setServicesState] = useState<Record<string, any>>({});
  const [isSavingServices, setIsSavingServices] = useState(false);

  // Ma boutique
  const [isEditingShop, setIsEditingShop] = useState(false);
  const [isSavingShop, setIsSavingShop] = useState(false);
  const [shopForm, setShopForm] = useState({
    nom: "", emailContact: "", telephoneContact: "",
    adresse: "", ville: "", numeroTva: "", description: "",
  });

  // Horaires
  const [editingHoraireId, setEditingHoraireId] = useState<number | null>(null);
  const [horaireForm, setHoraireForm] = useState({ heureOuverture: "", heureFermeture: "", ferme: false });
  const [isSavingHoraire, setIsSavingHoraire] = useState(false);

  useEffect(() => {
    if (!user || !user.id) return;
    imprimerieService.getImprimerieById(user.id.toString())
      .then(data => {
        setShop(data);
        setServicesState(initServicesFromProduits(data.produits || []));
        setShopForm({
          nom: data.nom || "",
          emailContact: data.emailContact || "",
          telephoneContact: (data as any).telephoneContact || "",
          adresse: data.adresse || "",
          ville: data.ville || "",
          numeroTva: (data as any).numeroTva || "",
          description: data.description || "",
        });
        return fetch(`http://localhost:8080/api/commandes/imprimerie/${data.id}`, {
          headers: { "Authorization": `Bearer ${token}` },
        });
      })
      .then(res => res && res.ok ? res.json() : [])
      .then(data => { setOrders(data); setIsLoading(false); })
      .catch(() => {
        toast({ title: "Erreur", description: "Impossible de charger les données", variant: "destructive" });
        setIsLoading(false);
      });
  }, [user]);

  const reloadShop = async () => {
    if (!user) return;
    const updated = await imprimerieService.getImprimerieById(user.id.toString());
    setShop(updated);
    return updated;
  };

  // ── Helpers services ─────────────────────────────────────────────────────
  const updateSvc = (id: string, field: string, value: any) =>
    setServicesState(prev => ({ ...prev, [id]: { ...prev[id], [field]: value } }));

  const updateSvcOptionPrice = (svcId: string, cat: "prixParTypePlastification" | "prixParTypeReliure", type: string, value: string) =>
    setServicesState(prev => ({
      ...prev,
      [svcId]: { ...prev[svcId], [cat]: { ...prev[svcId][cat], [type]: value } },
    }));

  const toggleSvcOptionActive = (svcId: string, cat: "activePlastification" | "activeReliure", type: string, value: boolean) =>
    setServicesState(prev => ({
      ...prev,
      [svcId]: { ...prev[svcId], [cat]: { ...prev[svcId][cat], [type]: value } },
    }));

  // ── Sauvegarder les services ─────────────────────────────────────────────
  const handleSaveServices = async () => {
    if (!shop || !user) return;
    setIsSavingServices(true);
    try {
      for (const s of SERVICES) {
        const state = servicesState[s.id];
        if (!state) continue;

        const canPlastify = s.typeProduit !== "POSTER";
        const isDocument = s.typeProduit === "DOCUMENT";

        const parsedPlastif = canPlastify && state.proposePlastification
          ? Object.fromEntries(
              Object.entries(state.prixParTypePlastification)
                .filter(([k]) => state.activePlastification[k])
                .map(([k, v]) => [k, parseFloat(v as string) || 0])
            )
          : {};

        const parsedReliure = isDocument && state.proposeReliure
          ? Object.fromEntries(
              Object.entries(state.prixParTypeReliure)
                .filter(([k]) => state.activeReliure[k])
                .map(([k, v]) => [k, parseFloat(v as string) || 0])
            )
          : {};

        const dto = {
          imprimerieId: shop.id,
          typeProduit: s.typeProduit,
          formatImpression: s.formatImpression,
          prixBase: parseFloat(state.price) || 0,
          prixParPage: 0,
          proposePlastification: canPlastify && state.proposePlastification && Object.keys(parsedPlastif).length > 0,
          prixParTypePlastification: Object.keys(parsedPlastif).length > 0 ? parsedPlastif : null,
          proposeReliure: isDocument && state.proposeReliure && Object.keys(parsedReliure).length > 0,
          prixParTypeReliure: Object.keys(parsedReliure).length > 0 ? parsedReliure : null,
        };

        if (state.enabled) {
          if (!state.existingProduitId) {
            await imprimerieService.createProduit(dto);
          } else if (!state.existingActif) {
            await imprimerieService.updateProduit(state.existingProduitId, { ...dto, actif: true });
          } else {
            await imprimerieService.updateProduit(state.existingProduitId, dto);
          }
        } else if (state.existingProduitId && state.existingActif) {
          await imprimerieService.deleteProduit(state.existingProduitId);
        }
      }

      const updated = await reloadShop();
      if (updated) setServicesState(initServicesFromProduits(updated.produits || []));
      toast({ title: "Succès", description: "Services mis à jour." });
    } catch {
      toast({ title: "Erreur", description: "Impossible de sauvegarder les services.", variant: "destructive" });
    } finally {
      setIsSavingServices(false);
    }
  };

  // ── Ma boutique ──────────────────────────────────────────────────────────
  const buildFullDto = (overrides: Record<string, unknown>) => ({
    idGerant: user?.id,
    nom: shop?.nom,
    description: shop?.description,
    logoUrl: shop?.logoUrl,
    emailContact: shop?.emailContact,
    telephoneContact: (shop as any)?.telephoneContact,
    adresse: shop?.adresse,
    ville: shop?.ville,
    numeroTva: (shop as any)?.numeroTva,
    proposeExpress2h: shop?.proposeExpress2h,
    prixExpress2h: shop?.prixExpress2h,
    livraisonActive: shop?.livraisonActive,
    proposeTarifEtudiant: shop?.proposeTarifEtudiant,
    pourcentageRemiseEtudiant: shop?.pourcentageRemiseEtudiant,
    ...overrides,
  });

  const handleSaveShop = async () => {
    if (!user || !shop) return;
    setIsSavingShop(true);
    try {
      const updated = await imprimerieService.updateImprimerie(user.id.toString(), buildFullDto(shopForm));
      setShop(updated);
      setIsEditingShop(false);
      toast({ title: "Succès", description: "Informations mises à jour." });
    } catch {
      toast({ title: "Erreur", description: "Impossible de sauvegarder.", variant: "destructive" });
    } finally {
      setIsSavingShop(false);
    }
  };

  const handleCancelShop = () => {
    if (!shop) return;
    setShopForm({
      nom: shop.nom || "",
      emailContact: shop.emailContact || "",
      telephoneContact: (shop as any).telephoneContact || "",
      adresse: shop.adresse || "",
      ville: shop.ville || "",
      numeroTva: (shop as any).numeroTva || "",
      description: shop.description || "",
    });
    setIsEditingShop(false);
  };

  // ── Options ──────────────────────────────────────────────────────────────
  const handleUpdatePrixExpress = async (prix: number) => {
    if (!user || !shop || isNaN(prix)) return;
    try {
      const updated = await imprimerieService.updateImprimerie(user.id.toString(), buildFullDto({ prixExpress2h: prix }));
      setShop(updated);
      toast({ title: "Prix express mis à jour" });
    } catch {
      toast({ title: "Erreur", description: "Impossible de sauvegarder.", variant: "destructive" });
    }
  };

  // ── Codes Promo ──────────────────────────────────────────────────────────────
  useEffect(() => {
    if (!shop?.id || !token) return;
    fetch(`http://localhost:8080/api/promos/imprimerie/${shop.id}`, { headers: { Authorization: `Bearer ${token}` } })
      .then(r => r.ok ? r.json() : []).then(setPromos).catch(() => {});
  }, [shop?.id, token]);

  const handleCreerPromo = async () => {
    if (!shop || !promoForm.code || !promoForm.valeurReduction) return;
    try {
      const res = await fetch("http://localhost:8080/api/promos", {
        method: "POST",
        headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
        body: JSON.stringify({
          code: promoForm.code,
          typeReduction: promoForm.typeReduction,
          valeurReduction: parseFloat(promoForm.valeurReduction),
          dateFin: promoForm.dateFin ? `${promoForm.dateFin}T23:59:59` : null,
          utilisationMax: promoForm.utilisationMax ? parseInt(promoForm.utilisationMax) : null,
          montantMinimumCommande: promoForm.montantMinimumCommande ? parseFloat(promoForm.montantMinimumCommande) : null,
          imprimerieId: shop.id,
        }),
      });
      if (!res.ok) {
        let msg = "Impossible de créer le code.";
        try { const err = await res.json(); msg = err.message || err.detail || msg; } catch { /* ignore */ }
        toast({ title: "Erreur", description: msg, variant: "destructive" });
        return;
      }
      const created = await res.json();
      setPromos(prev => [created, ...prev]);
      setPromoForm({ code: "", typeReduction: "POURCENTAGE", valeurReduction: "", dateFin: "", utilisationMax: "", montantMinimumCommande: "" });
      toast({ title: "Code promo créé !" });
    } catch {
      toast({ title: "Erreur réseau", description: "Vérifiez votre connexion.", variant: "destructive" });
    }
  };

  const handleTogglePromo = async (id: number) => {
    try {
      const res = await fetch(`http://localhost:8080/api/promos/${id}/toggle`, { method: "PATCH", headers: { Authorization: `Bearer ${token}` } });
      if (!res.ok) throw new Error();
      const updated = await res.json();
      setPromos(prev => prev.map((p: any) => p.id === id ? updated : p));
    } catch {
      toast({ title: "Erreur", variant: "destructive" });
    }
  };

  const handleSupprimerPromo = async (id: number) => {
    try {
      await fetch(`http://localhost:8080/api/promos/${id}`, { method: "DELETE", headers: { Authorization: `Bearer ${token}` } });
      setPromos(prev => prev.filter((p: any) => p.id !== id));
      toast({ title: "Code supprimé" });
    } catch {
      toast({ title: "Erreur", variant: "destructive" });
    }
  };

  const handleToggleOption = async (field: "proposeExpress2h" | "livraisonActive" | "proposeTarifEtudiant", value: boolean) => {
    if (!user || !shop) return;
    try {
      const updated = await imprimerieService.updateImprimerie(user.id.toString(), buildFullDto({ [field]: value }));
      setShop(updated);
      toast({ title: "Option mise à jour" });
    } catch {
      toast({ title: "Erreur", description: "Impossible de modifier l'option.", variant: "destructive" });
    }
  };

  // ── Horaires ─────────────────────────────────────────────────────────────
  const startEditHoraire = (h: any) => {
    setEditingHoraireId(h.id);
    setHoraireForm({
      heureOuverture: h.heureOuverture ? h.heureOuverture.slice(0, 5) : "",
      heureFermeture: h.heureFermeture ? h.heureFermeture.slice(0, 5) : "",
      ferme: !!h.ferme,
    });
  };

  const handleSaveHoraire = async (h: any) => {
    setIsSavingHoraire(true);
    try {
      await imprimerieService.updateHoraire(h.id, {
        jourSemaine: h.jourSemaine,
        heureOuverture: horaireForm.ferme ? null : horaireForm.heureOuverture,
        heureFermeture: horaireForm.ferme ? null : horaireForm.heureFermeture,
        ferme: horaireForm.ferme,
      });
      await reloadShop();
      setEditingHoraireId(null);
      toast({ title: "Succès", description: "Horaire mis à jour." });
    } catch {
      toast({ title: "Erreur", description: "Impossible de sauvegarder l'horaire.", variant: "destructive" });
    } finally {
      setIsSavingHoraire(false);
    }
  };

  const formatEnumName = (text: string) => {
    if (!text) return "";
    return text.replace(/_/g, ' ').toLowerCase().replace(/^./, c => c.toUpperCase());
  };

  const openPDF = async (fichierId: number, nomFichier: string) => {
    try {
      const res = await fetch(`http://localhost:8080/api/fichiers-pdf/${fichierId}/download`, {
        headers: { "Authorization": `Bearer ${token}` },
      });
      if (!res.ok) throw new Error();
      const blob = await res.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.target = "_blank";
      a.rel = "noopener noreferrer";
      a.click();
      setTimeout(() => URL.revokeObjectURL(url), 10000);
    } catch {
      toast({ title: "Erreur", description: `Impossible d'ouvrir ${nomFichier}.`, variant: "destructive" });
    }
  };

  const handleUpdateStatut = async (orderId: number, newStatut: string) => {
    setUpdatingStatutId(orderId);
    try {
      const res = await fetch(`http://localhost:8080/api/commandes/${orderId}/statut?statut=${newStatut}`, {
        method: "PATCH",
        headers: { "Authorization": `Bearer ${token}` },
      });
      if (!res.ok) throw new Error();
      const updated = await res.json();
      setOrders(prev => prev.map(o => o.id === orderId ? updated : o));
      toast({ title: "Statut mis à jour", description: `Commande passée à "${STATUT_LABELS[newStatut]?.label ?? newStatut}".` });
    } catch {
      toast({ title: "Erreur", description: "Impossible de mettre à jour le statut.", variant: "destructive" });
    } finally {
      setUpdatingStatutId(null);
    }
  };

  const handleAjouterSuivi = async (orderId: number) => {
    const numeroSuivi = suiviInputs[orderId]?.trim();
    if (!numeroSuivi) return;
    setSuiviSaving(orderId);
    try {
      const res = await fetch(`http://localhost:8080/api/livraisons/${orderId}/suivi`, {
        method: "PATCH",
        headers: { "Authorization": `Bearer ${token}`, "Content-Type": "application/json" },
        body: JSON.stringify({ numeroSuivi }),
      });
      if (!res.ok) throw new Error();
      toast({ title: "Numéro de suivi enregistré", description: `Le client peut maintenant suivre son colis bpost.` });
      setSuiviInputs(prev => ({ ...prev, [orderId]: "" }));
    } catch {
      toast({ title: "Erreur", description: "Impossible d'enregistrer le numéro de suivi.", variant: "destructive" });
    } finally {
      setSuiviSaving(null);
    }
  };

  const STATUT_LABELS: Record<string, { label: string; color: string }> = {
    EN_ATTENTE_PAIEMENT: { label: "En attente", color: "bg-yellow-100 text-yellow-800 border-yellow-200" },
    PAYEE:               { label: "En attente", color: "bg-yellow-100 text-yellow-800 border-yellow-200" },
    PRETE:               { label: "Prêt à être retiré", color: "bg-green-100 text-green-800 border-green-200" },
    LIVREE:              { label: "Récupérée", color: "bg-gray-100 text-gray-600 border-gray-200" },
    ANNULEE:             { label: "Annulée", color: "bg-red-100 text-red-800 border-red-200" },
  };

  const NEXT_STATUT: Record<string, { statut: string; label: string; icon: React.ReactNode }> = {
    EN_ATTENTE_PAIEMENT: { statut: "PRETE", label: "Marquer comme prêt à retirer", icon: <CheckCircle className="h-4 w-4 mr-2" /> },
    PAYEE:               { statut: "PRETE", label: "Marquer comme prêt à retirer", icon: <CheckCircle className="h-4 w-4 mr-2" /> },
    PRETE:               { statut: "LIVREE", label: "Marquer comme récupéré", icon: <CheckCircle className="h-4 w-4 mr-2" /> },
  };

  // ── Rendu ─────────────────────────────────────────────────────────────────
  if (isLoading) {
    return (
      <div className="min-h-screen flex flex-col bg-muted/30">
        <Header />
        <main className="flex-1 flex items-center justify-center">
          <p className="text-muted-foreground animate-pulse">Chargement de votre espace pro...</p>
        </main>
      </div>
    );
  }

  if (!shop) {
    return (
      <div className="min-h-screen flex flex-col bg-muted/30">
        <Header />
        <main className="flex-1 pt-32 pb-16 flex flex-col items-center">
          <h2 className="text-2xl font-bold text-destructive mb-4">Imprimerie introuvable</h2>
          <p className="text-muted-foreground text-center max-w-lg">Aucune imprimerie n'est associée à votre compte.</p>
        </main>
      </div>
    );
  }

  const revenue = orders.reduce((s, o) => s + Number(o.totalTTC ?? 0), 0);
  const pending = orders.filter((o) => o.statut === "PAYEE" || o.statut === "EN_COURS_IMPRESSION").length;
  const activeServicesCount = Object.values(servicesState).filter((s: any) => s.enabled).length;

  return (
    <div className="min-h-screen flex flex-col bg-muted/30">
      <Header />
      <main className="flex-1 pt-24 pb-16">
        <div className="container mx-auto px-4 max-w-6xl">

          {/* HEADER */}
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-8">
            <div>
              <h1 className="font-display text-3xl md:text-4xl font-bold text-foreground mb-2">{shop.nom}</h1>
              <p className="text-muted-foreground flex items-center gap-2">
                <Store className="h-4 w-4" /> Espace professionnel imprimeur
              </p>
            </div>
            <Badge className={shop.actif ? "bg-success/10 text-success border-success/20" : "bg-destructive/10 text-destructive border-destructive/20"}>
              ● {shop.actif ? "Boutique visible en ligne" : "Boutique hors ligne"}
            </Badge>
          </div>

          {/* KPI */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
            <Card className="p-6">
              <Package className="h-8 w-8 text-primary mb-3" />
              <div className="font-display text-3xl font-bold">{orders.length}</div>
              <div className="text-sm text-muted-foreground">Commandes du mois</div>
            </Card>
            <Card className="p-6">
              <Clock className="h-8 w-8 text-warning mb-3" />
              <div className="font-display text-3xl font-bold">{pending}</div>
              <div className="text-sm text-muted-foreground">À traiter</div>
            </Card>
            <Card className="p-6">
              <Euro className="h-8 w-8 text-success mb-3" />
              <div className="font-display text-3xl font-bold">{revenue.toFixed(0)}€</div>
              <div className="text-sm text-muted-foreground">Revenus (CA)</div>
            </Card>
            <Card className="p-6">
              <Star className="h-8 w-8 text-secondary mb-3" />
              <div className="font-display text-3xl font-bold">N/A</div>
              <div className="text-sm text-muted-foreground">Note moyenne</div>
            </Card>
          </div>

          {/* TABS */}
          <Tabs defaultValue="orders" className="space-y-6">
            <TabsList className="flex flex-wrap h-auto gap-2 bg-transparent p-0">
              <TabsTrigger value="orders" className="data-[state=active]:bg-primary data-[state=active]:text-primary-foreground border bg-background">Commandes</TabsTrigger>
              <TabsTrigger value="services" className="data-[state=active]:bg-primary data-[state=active]:text-primary-foreground border bg-background">Services & Tarifs</TabsTrigger>
              <TabsTrigger value="options" className="data-[state=active]:bg-primary data-[state=active]:text-primary-foreground border bg-background">Options</TabsTrigger>
              <TabsTrigger value="hours" className="data-[state=active]:bg-primary data-[state=active]:text-primary-foreground border bg-background">Horaires</TabsTrigger>
              <TabsTrigger value="shop" className="data-[state=active]:bg-primary data-[state=active]:text-primary-foreground border bg-background">Ma boutique</TabsTrigger>
              <TabsTrigger value="promos" className="data-[state=active]:bg-primary data-[state=active]:text-primary-foreground border bg-background">Codes Promo</TabsTrigger>
            </TabsList>

            {/* COMMANDES */}
            <TabsContent value="orders">
              <Card className="p-6">
                <h3 className="font-display font-semibold text-lg mb-4">Commandes reçues</h3>
                {orders.length === 0 ? (
                  <div className="text-center py-16 border-2 border-dashed rounded-lg bg-muted/20">
                    <Package className="h-12 w-12 mx-auto text-muted-foreground mb-4 opacity-50" />
                    <h4 className="text-lg font-semibold mb-1">Aucune commande</h4>
                    <p className="text-muted-foreground text-sm">Vos commandes apparaîtront ici dès qu'un client passera commande.</p>
                  </div>
                ) : (
                  <div className="space-y-3">
                    {orders.map((order: any) => {
                      const isExpanded = expandedOrderId === order.id;
                      const statutInfo = STATUT_LABELS[order.statut];
                      const nextAction = NEXT_STATUT[order.statut];
                      const isUpdating = updatingStatutId === order.id;

                      return (
                        <div key={order.id} className="border rounded-xl overflow-hidden">
                          {/* En-tête cliquable */}
                          <button
                            className="w-full flex items-center justify-between p-4 bg-muted/10 hover:bg-muted/20 transition-colors text-left"
                            onClick={() => setExpandedOrderId(isExpanded ? null : order.id)}
                          >
                            <div className="min-w-0">
                              <p className="font-mono font-semibold text-sm">{order.numeroCommande}</p>
                              <p className="text-xs text-muted-foreground mt-0.5">
                                {order.nomClient} · {new Date(order.dateCreation).toLocaleDateString("fr-BE")}
                                {order.modeRetrait === "LIVRAISON" && <span className="ml-2 inline-flex items-center gap-1"><Truck className="h-3 w-3" /> Livraison</span>}
                                {order.express2h && <span className="ml-2 inline-flex items-center gap-1 text-secondary"><Zap className="h-3 w-3" /> Express</span>}
                              </p>
                            </div>
                            <div className="flex items-center gap-3 shrink-0 ml-4">
                              <span className={`text-xs font-medium px-2 py-1 rounded-full border ${statutInfo?.color ?? "bg-gray-100 text-gray-600"}`}>
                                {statutInfo?.label ?? order.statut}
                              </span>
                              <span className="font-bold text-primary">{Number(order.totalTTC).toFixed(2)}€</span>
                              {isExpanded ? <ChevronUp className="h-4 w-4 text-muted-foreground" /> : <ChevronDown className="h-4 w-4 text-muted-foreground" />}
                            </div>
                          </button>

                          {/* Détails */}
                          {isExpanded && (
                            <div className="p-4 border-t bg-background space-y-4">
                              {/* Lignes de commande */}
                              <div>
                                <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-2">Fichiers à imprimer</p>
                                <div className="space-y-2">
                                  {(order.lignes ?? []).map((ligne: any, idx: number) => (
                                    <div key={ligne.id ?? idx} className="p-3 rounded-lg bg-muted/30 space-y-2">
                                      <div className="flex items-start gap-3">
                                        <FileText className="h-5 w-5 text-primary mt-0.5 shrink-0" />
                                        <div className="flex-1 min-w-0">
                                          <p className="font-medium text-sm">{ligne.nomProduit}</p>
                                          <div className="flex flex-wrap gap-x-3 gap-y-0.5 mt-1 text-xs text-muted-foreground">
                                            <span>{ligne.nbPages} page{ligne.nbPages > 1 ? "s" : ""}</span>
                                            <span>{ligne.quantite} copie{ligne.quantite > 1 ? "s" : ""}</span>
                                            {ligne.rectoVerso && <span>Recto-Verso</span>}
                                            {ligne.reliure && ligne.reliure !== "AUCUNE" && <span>Reliure : {formatEnumName(ligne.reliure)}</span>}
                                            {ligne.finition && ligne.finition !== "AUCUNE" && <span>Plastification : {formatEnumName(ligne.finition)}</span>}
                                          </div>
                                        </div>
                                        <span className="font-semibold text-sm shrink-0">{Number(ligne.prixTotal).toFixed(2)}€</span>
                                      </div>
                                      {/* Fichiers PDF */}
                                      {(ligne.fichiers ?? []).length > 0 && (
                                        <div className="flex flex-wrap gap-2 pl-8">
                                          {(ligne.fichiers as any[]).map((fichier: any) => (
                                            <button
                                              key={fichier.id}
                                              onClick={() => openPDF(fichier.id, fichier.nomFichier)}
                                              className="inline-flex items-center gap-1.5 text-xs px-3 py-1.5 rounded-md bg-primary/10 text-primary hover:bg-primary/20 transition-colors font-medium"
                                            >
                                              <FileText className="h-3.5 w-3.5" />
                                              {fichier.nomFichier}
                                              <span className="text-muted-foreground">({fichier.nbPagesDetectees}p)</span>
                                            </button>
                                          ))}
                                        </div>
                                      )}
                                    </div>
                                  ))}
                                </div>
                              </div>

                              {/* Récapitulatif financier */}
                              <div className="flex justify-between text-sm pt-2 border-t">
                                <span className="text-muted-foreground">Total TTC</span>
                                <span className="font-bold text-primary">{Number(order.totalTTC).toFixed(2)}€</span>
                              </div>

                              {/* Suivi bpost pour les livraisons */}
                              {order.modeRetrait === "LIVRAISON" && (
                                <div className="pt-2 border-t space-y-2">
                                  <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide flex items-center gap-1.5">
                                    <Truck className="h-3.5 w-3.5" /> Numéro de suivi bpost
                                  </p>
                                  <div className="flex gap-2">
                                    <Input
                                      placeholder="ex: 010123456789"
                                      value={suiviInputs[order.id] ?? ""}
                                      onChange={(e) => setSuiviInputs(prev => ({ ...prev, [order.id]: e.target.value }))}
                                    />
                                    <Button
                                      variant="outline"
                                      size="sm"
                                      disabled={!suiviInputs[order.id]?.trim() || suiviSaving === order.id}
                                      onClick={() => handleAjouterSuivi(order.id)}
                                    >
                                      {suiviSaving === order.id ? <RotateCcw className="h-4 w-4 animate-spin" /> : "Enregistrer"}
                                    </Button>
                                  </div>
                                </div>
                              )}

                              {/* Bouton d'action statut */}
                              {nextAction && (
                                <Button
                                  className="w-full mt-2"
                                  onClick={() => handleUpdateStatut(order.id, nextAction.statut)}
                                  disabled={isUpdating}
                                >
                                  {isUpdating ? (
                                    <RotateCcw className="h-4 w-4 mr-2 animate-spin" />
                                  ) : nextAction.icon}
                                  {nextAction.label}
                                </Button>
                              )}

                              {(order.statut === "LIVREE" || order.statut === "ANNULEE") && (
                                <p className="text-center text-xs text-muted-foreground pt-1">
                                  {order.statut === "LIVREE" ? "Commande terminée." : "Commande annulée."}
                                </p>
                              )}
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                )}
              </Card>
            </TabsContent>

            {/* SERVICES */}
            <TabsContent value="services">
              <Card className="p-6 md:p-8">
                <div className="flex items-center gap-3 mb-2">
                  <Printer className="h-6 w-6 text-primary" />
                  <h2 className="font-display text-xl font-semibold">Services proposés</h2>
                  <Badge variant="outline" className="ml-auto">
                    {activeServicesCount} actif{activeServicesCount > 1 ? "s" : ""}
                  </Badge>
                </div>
                <p className="text-sm text-muted-foreground mb-6">
                  Cochez les services que vous proposez et configurez vos options de finition.
                </p>

                <div className="space-y-4">
                  {SERVICES.map((s) => {
                    const state = servicesState[s.id];
                    if (!state) return null;

                    const canPlastify = s.typeProduit !== "POSTER";
                    const canBind = s.typeProduit === "DOCUMENT";
                    const showOptions = state.enabled && (canPlastify || canBind);

                    return (
                      <div key={s.id} className={`border rounded-lg transition-all ${state.enabled ? "border-primary shadow-sm" : "border-border"}`}>
                        {/* Ligne principale */}
                        <div className={`flex items-center gap-4 p-4 ${state.enabled && showOptions ? "bg-primary/5 border-b border-primary/20" : state.enabled ? "bg-primary/5" : ""}`}>
                          <Checkbox
                            checked={state.enabled}
                            onCheckedChange={(v) => updateSvc(s.id, "enabled", Boolean(v))}
                          />
                          <div className="flex-1 font-medium">{s.name}</div>
                          <div className="flex items-center gap-2">
                            <Label className="text-xs text-muted-foreground hidden sm:block">Prix base</Label>
                            <Input
                              type="number" step="0.01" min="0"
                              value={state.price}
                              onChange={(e) => updateSvc(s.id, "price", e.target.value)}
                              className="w-24 h-9"
                              disabled={!state.enabled}
                            />
                            <span className="text-sm text-muted-foreground">€</span>
                          </div>
                        </div>

                        {/* Options finition */}
                        {showOptions && (
                          <div className="p-4 bg-muted/20 space-y-6">

                            {/* Plastification */}
                            {canPlastify && (
                              <div>
                                <div className="flex items-center gap-4 mb-3">
                                  <Checkbox
                                    id={`plast-${s.id}`}
                                    checked={state.proposePlastification}
                                    onCheckedChange={(v) => updateSvc(s.id, "proposePlastification", Boolean(v))}
                                  />
                                  <div className="flex items-center gap-2">
                                    <Layers className="h-4 w-4 text-muted-foreground" />
                                    <Label htmlFor={`plast-${s.id}`} className="cursor-pointer font-medium">Proposer Plastification</Label>
                                  </div>
                                </div>
                                {state.proposePlastification && (
                                  <div className="ml-8 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                                    {Object.entries(state.prixParTypePlastification).map(([type, price]) => {
                                      const isActive = state.activePlastification[type];
                                      return (
                                        <div key={type} className={`flex items-center justify-between p-2 border rounded-md transition-colors ${isActive ? "bg-background border-primary/40" : "bg-muted/50 border-border opacity-70"}`}>
                                          <div className="flex items-center gap-2">
                                            <Checkbox checked={isActive} onCheckedChange={(v) => toggleSvcOptionActive(s.id, "activePlastification", type, Boolean(v))} />
                                            <span className="text-xs font-medium">{formatEnumName(type)}</span>
                                          </div>
                                          <div className="flex items-center gap-1">
                                            <Input type="number" step="0.01" value={price as string} disabled={!isActive}
                                              onChange={(e) => updateSvcOptionPrice(s.id, "prixParTypePlastification", type, e.target.value)}
                                              className="w-16 h-7 text-xs px-2" />
                                            <span className="text-xs text-muted-foreground">€</span>
                                          </div>
                                        </div>
                                      );
                                    })}
                                  </div>
                                )}
                              </div>
                            )}

                            {/* Reliure */}
                            {canBind && (
                              <div>
                                <div className="flex items-center gap-4 mb-3">
                                  <Checkbox
                                    id={`rel-${s.id}`}
                                    checked={state.proposeReliure}
                                    onCheckedChange={(v) => updateSvc(s.id, "proposeReliure", Boolean(v))}
                                  />
                                  <div className="flex items-center gap-2">
                                    <Book className="h-4 w-4 text-muted-foreground" />
                                    <Label htmlFor={`rel-${s.id}`} className="cursor-pointer font-medium">Proposer Reliure</Label>
                                  </div>
                                </div>
                                {state.proposeReliure && (
                                  <div className="ml-8 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                                    {Object.entries(state.prixParTypeReliure).map(([type, price]) => {
                                      const isActive = state.activeReliure[type];
                                      return (
                                        <div key={type} className={`flex items-center justify-between p-2 border rounded-md transition-colors ${isActive ? "bg-background border-primary/40" : "bg-muted/50 border-border opacity-70"}`}>
                                          <div className="flex items-center gap-2 overflow-hidden">
                                            <Checkbox checked={isActive} onCheckedChange={(v) => toggleSvcOptionActive(s.id, "activeReliure", type, Boolean(v))} />
                                            <span className="text-xs font-medium truncate">{formatEnumName(type)}</span>
                                          </div>
                                          <div className="flex items-center gap-1 shrink-0">
                                            <Input type="number" step="0.01" value={price as string} disabled={!isActive}
                                              onChange={(e) => updateSvcOptionPrice(s.id, "prixParTypeReliure", type, e.target.value)}
                                              className="w-16 h-7 text-xs px-2" />
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

                <div className="flex justify-end mt-6">
                  <Button onClick={handleSaveServices} disabled={isSavingServices}>
                    {isSavingServices ? "Enregistrement..." : "Enregistrer les modifications"}
                  </Button>
                </div>
              </Card>
            </TabsContent>

            {/* OPTIONS */}
            <TabsContent value="options">
              <Card className="p-6 space-y-6">
                <h3 className="font-display font-semibold text-lg">Options proposées aux clients</h3>

                <div className="flex items-center justify-between p-4 border border-border rounded-lg bg-background">
                  <div>
                    <Label className="font-semibold flex items-center gap-2"><GraduationCap className="h-4 w-4 text-primary" /> Tarif étudiant</Label>
                    <p className="text-sm text-muted-foreground mt-1">Remise accordée aux étudiants sur présentation de carte.</p>
                  </div>
                  <div className="flex items-center gap-3">
                    {shop.proposeTarifEtudiant && shop.pourcentageRemiseEtudiant ? (
                      <span className="text-sm font-bold text-primary">-{shop.pourcentageRemiseEtudiant}%</span>
                    ) : null}
                    <Switch checked={!!shop.proposeTarifEtudiant} onCheckedChange={(v) => handleToggleOption("proposeTarifEtudiant", v)} />
                  </div>
                </div>

                <div className="p-4 border border-border rounded-lg bg-background space-y-3">
                  <div className="flex items-center justify-between">
                    <div>
                      <Label className="font-semibold flex items-center gap-2"><Zap className="h-4 w-4 text-primary" /> Impression express 2h</Label>
                      <p className="text-sm text-muted-foreground mt-1">Proposez l'impression prioritaire très rapide.</p>
                    </div>
                    <Switch checked={!!shop.proposeExpress2h} onCheckedChange={(v) => handleToggleOption("proposeExpress2h", v)} />
                  </div>
                  {shop.proposeExpress2h && (
                    <div className="flex items-center gap-2 pt-2 border-t border-border">
                      <Label className="text-sm text-muted-foreground">Frais express :</Label>
                      <Input
                        type="number"
                        defaultValue={shop.prixExpress2h ?? 5}
                        min={0}
                        step={0.5}
                        className="w-24 h-8"
                        onBlur={(e) => handleUpdatePrixExpress(parseFloat(e.target.value))}
                      />
                      <span className="text-sm text-muted-foreground">€</span>
                    </div>
                  )}
                </div>

                <div className="flex items-center justify-between p-4 border border-border rounded-lg bg-background">
                  <div>
                    <Label className="font-semibold flex items-center gap-2"><Truck className="h-4 w-4 text-primary" /> Livraison à domicile</Label>
                    <p className="text-sm text-muted-foreground mt-1">Activez la livraison autour de votre boutique.</p>
                  </div>
                  <Switch checked={!!shop.livraisonActive} onCheckedChange={(v) => handleToggleOption("livraisonActive", v)} />
                </div>
              </Card>
            </TabsContent>

            {/* HORAIRES */}
            <TabsContent value="hours">
              <Card className="p-6 space-y-4">
                <h3 className="font-display font-semibold text-lg">Horaires d'ouverture</h3>
                <div className="space-y-3">
                  {shop.horaires && shop.horaires.length > 0 ? (
                    shop.horaires.map((h: any) => {
                      const isEditing = editingHoraireId === h.id;
                      return (
                        <div key={h.id} className="flex items-center gap-3 p-4 border border-border rounded-lg bg-background">
                          <div className="font-medium capitalize w-28 shrink-0">{h.jourSemaine.toLowerCase()}</div>

                          {!isEditing ? (
                            <div className="flex-1 text-sm text-muted-foreground">
                              {h.ferme ? <span className="text-destructive font-medium">Fermé</span>
                                : `${h.heureOuverture?.slice(0, 5)} - ${h.heureFermeture?.slice(0, 5)}`}
                            </div>
                          ) : (
                            <div className="flex-1 flex flex-wrap items-center gap-3">
                              <div className="flex items-center gap-2">
                                <Label className="text-xs text-muted-foreground">Fermé</Label>
                                <Switch checked={horaireForm.ferme} onCheckedChange={(v) => setHoraireForm(f => ({ ...f, ferme: v }))} />
                              </div>
                              {!horaireForm.ferme && (
                                <>
                                  <div className="flex items-center gap-1">
                                    <Label className="text-xs text-muted-foreground">Ouverture</Label>
                                    <Input type="time" value={horaireForm.heureOuverture}
                                      onChange={(e) => setHoraireForm(f => ({ ...f, heureOuverture: e.target.value }))}
                                      className="w-28 h-7 text-sm" />
                                  </div>
                                  <div className="flex items-center gap-1">
                                    <Label className="text-xs text-muted-foreground">Fermeture</Label>
                                    <Input type="time" value={horaireForm.heureFermeture}
                                      onChange={(e) => setHoraireForm(f => ({ ...f, heureFermeture: e.target.value }))}
                                      className="w-28 h-7 text-sm" />
                                  </div>
                                </>
                              )}
                            </div>
                          )}

                          <div className="flex gap-2 shrink-0">
                            {!isEditing ? (
                              <Button variant="outline" size="sm" onClick={() => startEditHoraire(h)}>Modifier</Button>
                            ) : (
                              <>
                                <Button variant="outline" size="sm" onClick={() => setEditingHoraireId(null)} disabled={isSavingHoraire}>Annuler</Button>
                                <Button size="sm" onClick={() => handleSaveHoraire(h)} disabled={isSavingHoraire}>
                                  {isSavingHoraire ? "..." : "Enregistrer"}
                                </Button>
                              </>
                            )}
                          </div>
                        </div>
                      );
                    })
                  ) : (
                    <p className="text-muted-foreground text-center py-4">Horaires non configurés.</p>
                  )}
                </div>
              </Card>
            </TabsContent>

            {/* MA BOUTIQUE */}
            <TabsContent value="shop">
              <Card className="p-6 space-y-6">
                <div className="flex items-center justify-between">
                  <h3 className="font-display font-semibold text-lg">Informations de la boutique</h3>
                  {!isEditingShop ? (
                    <Button variant="outline" size="sm" onClick={() => setIsEditingShop(true)}>Modifier</Button>
                  ) : (
                    <div className="flex gap-2">
                      <Button variant="outline" size="sm" onClick={handleCancelShop} disabled={isSavingShop}>Annuler</Button>
                      <Button size="sm" onClick={handleSaveShop} disabled={isSavingShop}>
                        {isSavingShop ? "Enregistrement..." : "Enregistrer"}
                      </Button>
                    </div>
                  )}
                </div>

                <div className="grid md:grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="shop-nom">Nom de l'imprimerie</Label>
                    <Input id="shop-nom" value={shopForm.nom} onChange={(e) => setShopForm(f => ({ ...f, nom: e.target.value }))}
                      readOnly={!isEditingShop} onClick={() => !isEditingShop && setIsEditingShop(true)}
                      className={!isEditingShop ? "bg-muted/40" : ""} />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="shop-tva">N° de TVA</Label>
                    <Input id="shop-tva" value={shopForm.numeroTva} onChange={(e) => setShopForm(f => ({ ...f, numeroTva: e.target.value }))}
                      readOnly={!isEditingShop} onClick={() => !isEditingShop && setIsEditingShop(true)}
                      className={!isEditingShop ? "bg-muted/40" : ""} />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="shop-tel">Téléphone</Label>
                    <Input id="shop-tel" value={shopForm.telephoneContact} onChange={(e) => setShopForm(f => ({ ...f, telephoneContact: e.target.value }))}
                      readOnly={!isEditingShop} onClick={() => !isEditingShop && setIsEditingShop(true)}
                      className={!isEditingShop ? "bg-muted/40" : ""} />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="shop-email">Email</Label>
                    <Input id="shop-email" value={shopForm.emailContact} onChange={(e) => setShopForm(f => ({ ...f, emailContact: e.target.value }))}
                      readOnly={!isEditingShop} onClick={() => !isEditingShop && setIsEditingShop(true)}
                      className={!isEditingShop ? "bg-muted/40" : ""} />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="shop-adresse">Adresse</Label>
                    <Input id="shop-adresse" value={shopForm.adresse} onChange={(e) => setShopForm(f => ({ ...f, adresse: e.target.value }))}
                      readOnly={!isEditingShop} onClick={() => !isEditingShop && setIsEditingShop(true)}
                      className={!isEditingShop ? "bg-muted/40" : ""} />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="shop-ville">Ville</Label>
                    <Input id="shop-ville" value={shopForm.ville} onChange={(e) => setShopForm(f => ({ ...f, ville: e.target.value }))}
                      readOnly={!isEditingShop} onClick={() => !isEditingShop && setIsEditingShop(true)}
                      className={!isEditingShop ? "bg-muted/40" : ""} />
                  </div>
                  <div className="space-y-2 md:col-span-2">
                    <Label htmlFor="shop-desc">Description</Label>
                    <Textarea id="shop-desc" value={shopForm.description} onChange={(e) => setShopForm(f => ({ ...f, description: e.target.value }))}
                      readOnly={!isEditingShop} onClick={() => !isEditingShop && setIsEditingShop(true)}
                      className={!isEditingShop ? "bg-muted/40 resize-none" : "resize-none"} rows={3} />
                  </div>
                </div>
              </Card>
            </TabsContent>

            {/* CODES PROMO */}
            <TabsContent value="promos">
              <Card className="p-6 space-y-6">
                <div>
                  <h3 className="font-display font-semibold text-lg mb-1">Codes promo</h3>
                  <p className="text-sm text-muted-foreground">Créez des codes de réduction pour vos clients.</p>
                </div>

                {/* Formulaire création */}
                <div className="border rounded-lg p-4 space-y-4 bg-muted/30">
                  <h4 className="font-medium text-sm flex items-center gap-2"><Tag className="h-4 w-4" /> Nouveau code</h4>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div className="space-y-1">
                      <Label className="text-xs text-muted-foreground">Code</Label>
                      <Input placeholder="EX: BIENVENUE10" value={promoForm.code} onChange={(e) => setPromoForm(f => ({ ...f, code: e.target.value.toUpperCase() }))} />
                    </div>
                    <div className="space-y-1">
                      <Label className="text-xs text-muted-foreground">Type de réduction</Label>
                      <Select value={promoForm.typeReduction} onValueChange={(v) => setPromoForm(f => ({ ...f, typeReduction: v }))}>
                        <SelectTrigger><SelectValue /></SelectTrigger>
                        <SelectContent>
                          <SelectItem value="POURCENTAGE">Pourcentage (%)</SelectItem>
                          <SelectItem value="MONTANT_FIXE">Montant fixe (€)</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                    <div className="space-y-1">
                      <Label className="text-xs text-muted-foreground">Valeur {promoForm.typeReduction === "POURCENTAGE" ? "(%)" : "(€)"}</Label>
                      <Input type="number" min="0" value={promoForm.valeurReduction} onChange={(e) => setPromoForm(f => ({ ...f, valeurReduction: e.target.value }))} />
                    </div>
                    <div className="space-y-1">
                      <Label className="text-xs text-muted-foreground">Date d'expiration (optionnel)</Label>
                      <Input type="date" value={promoForm.dateFin} onChange={(e) => setPromoForm(f => ({ ...f, dateFin: e.target.value }))} />
                    </div>
                    <div className="space-y-1">
                      <Label className="text-xs text-muted-foreground">Utilisations max par personne (optionnel)</Label>
                      <Input type="number" min="1" placeholder="Illimité" value={promoForm.utilisationMax} onChange={(e) => setPromoForm(f => ({ ...f, utilisationMax: e.target.value }))} />
                    </div>
                    <div className="space-y-1">
                      <Label className="text-xs text-muted-foreground">Montant minimum commande (optionnel)</Label>
                      <Input type="number" min="0" placeholder="Aucun" value={promoForm.montantMinimumCommande} onChange={(e) => setPromoForm(f => ({ ...f, montantMinimumCommande: e.target.value }))} />
                    </div>
                  </div>
                  <Button onClick={handleCreerPromo} disabled={!promoForm.code || !promoForm.valeurReduction}>
                    <Tag className="h-4 w-4 mr-2" /> Créer le code
                  </Button>
                </div>

                {/* Liste des codes */}
                <div className="space-y-3">
                  {promos.length === 0 && (
                    <p className="text-sm text-muted-foreground text-center py-8">Aucun code promo pour l'instant.</p>
                  )}
                  {promos.map((p: any) => (
                    <div key={p.id} className={`flex items-center justify-between p-4 rounded-lg border ${p.actif ? "bg-background" : "bg-muted/30 opacity-60"}`}>
                      <div className="flex flex-wrap items-center gap-3">
                        <span className="font-mono font-bold text-sm">{p.code}</span>
                        <Badge variant="outline">
                          {p.typeReduction === "POURCENTAGE" ? `-${p.valeurReduction}%` : `-${Number(p.valeurReduction).toFixed(2)}€`}
                        </Badge>
                        {p.dateFin && (
                          <span className="text-xs text-muted-foreground">Expire le {new Date(p.dateFin).toLocaleDateString("fr-BE")}</span>
                        )}
                        {p.utilisationMax && (
                          <span className="text-xs text-muted-foreground">Max {p.utilisationMax}x/personne · {p.utilisationCourante} utilisation{p.utilisationCourante !== 1 ? "s" : ""} au total</span>
                        )}
                        {p.montantMinimumCommande && (
                          <span className="text-xs text-muted-foreground">Min. {Number(p.montantMinimumCommande).toFixed(2)}€</span>
                        )}
                      </div>
                      <div className="flex items-center gap-2 shrink-0 ml-2">
                        <Switch checked={!!p.actif} onCheckedChange={() => handleTogglePromo(p.id)} />
                        <Button variant="ghost" size="icon" onClick={() => handleSupprimerPromo(p.id)}>
                          <Trash2 className="h-4 w-4 text-destructive" />
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              </Card>
            </TabsContent>

          </Tabs>
        </div>
      </main>
    </div>
  );
};

export default DashboardImprimeur;
