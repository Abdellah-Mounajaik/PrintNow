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
  Zap, GraduationCap
} from "lucide-react";
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
  const { user } = useAuth();

  const [shop, setShop] = useState<ImprimerieDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const orders: any[] = [];

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
        setIsLoading(false);
      })
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

  const revenue = orders.reduce((s, o) => s + o.total, 0);
  const pending = orders.filter((o) => o.status === "pending").length;
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
          <Tabs defaultValue="services" className="space-y-6">
            <TabsList className="flex flex-wrap h-auto gap-2 bg-transparent p-0">
              <TabsTrigger value="orders" className="data-[state=active]:bg-primary data-[state=active]:text-primary-foreground border bg-background">Commandes</TabsTrigger>
              <TabsTrigger value="services" className="data-[state=active]:bg-primary data-[state=active]:text-primary-foreground border bg-background">Services & Tarifs</TabsTrigger>
              <TabsTrigger value="options" className="data-[state=active]:bg-primary data-[state=active]:text-primary-foreground border bg-background">Options</TabsTrigger>
              <TabsTrigger value="hours" className="data-[state=active]:bg-primary data-[state=active]:text-primary-foreground border bg-background">Horaires</TabsTrigger>
              <TabsTrigger value="shop" className="data-[state=active]:bg-primary data-[state=active]:text-primary-foreground border bg-background">Ma boutique</TabsTrigger>
            </TabsList>

            {/* COMMANDES */}
            <TabsContent value="orders">
              <Card className="p-6">
                <h3 className="font-display font-semibold text-lg mb-4">Commandes reçues</h3>
                <div className="text-center py-16 border-2 border-dashed rounded-lg bg-muted/20">
                  <Package className="h-12 w-12 mx-auto text-muted-foreground mb-4 opacity-50" />
                  <h4 className="text-lg font-semibold mb-1">Aucune commande</h4>
                  <p className="text-muted-foreground text-sm">La réception des commandes est en cours d'intégration.</p>
                </div>
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

                <div className="flex items-center justify-between p-4 border border-border rounded-lg bg-background">
                  <div>
                    <Label className="font-semibold flex items-center gap-2"><Zap className="h-4 w-4 text-primary" /> Impression express 2h</Label>
                    <p className="text-sm text-muted-foreground mt-1">Proposez l'impression prioritaire très rapide.</p>
                  </div>
                  <Switch checked={!!shop.proposeExpress2h} onCheckedChange={(v) => handleToggleOption("proposeExpress2h", v)} />
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

          </Tabs>
        </div>
      </main>
    </div>
  );
};

export default DashboardImprimeur;
