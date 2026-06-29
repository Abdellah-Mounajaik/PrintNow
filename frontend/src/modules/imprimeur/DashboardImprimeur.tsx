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
import {
  Table,
  TableBody,
  TableHead,
  TableHeader,
  TableRow,
} from "../../components/ui/table";
import {
  Package, Euro, Clock, FileText, Star,
  Store, Truck, Layers, Book,
  Image as ImageIcon, Zap, GraduationCap
} from "lucide-react";
import { toast } from "../../hooks/use-toast";

import { imprimerieService } from "../shop/services/imprimerieService.service";
import type { ImprimerieDetail } from "../shop/models/Imprimerie.model";
import { useAuth } from "../auth/context/AuthContext";

const DashboardImprimeur = () => {
  const { user } = useAuth();

  const [shop, setShop] = useState<ImprimerieDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [orders, setOrders] = useState<any[]>([]);

  // Édition "Ma boutique"
  const [isEditingShop, setIsEditingShop] = useState(false);
  const [isSavingShop, setIsSavingShop] = useState(false);
  const [shopForm, setShopForm] = useState({
    nom: "", emailContact: "", telephoneContact: "",
    adresse: "", ville: "", numeroTva: "", description: "",
  });

  // Édition Services
  const [editingServiceId, setEditingServiceId] = useState<number | null>(null);
  const [serviceForm, setServiceForm] = useState({ prixBase: 0, prixParPage: 0 });
  const [isSavingService, setIsSavingService] = useState(false);

  // Édition Horaires
  const [editingHoraireId, setEditingHoraireId] = useState<number | null>(null);
  const [horaireForm, setHoraireForm] = useState({ heureOuverture: "", heureFermeture: "", ferme: false });
  const [isSavingHoraire, setIsSavingHoraire] = useState(false);

  useEffect(() => {
    if (!user || !user.id) return;
    imprimerieService.getImprimerieById(user.id.toString())
      .then(data => {
        setShop(data);
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

  // ── Ma boutique ──────────────────────────────
  const handleSaveShop = async () => {
    if (!user || !shop) return;
    setIsSavingShop(true);
    try {
      const updated = await imprimerieService.updateImprimerie(user.id.toString(), buildFullDto(shopForm));
      setShop(updated);
      setIsEditingShop(false);
      toast({ title: "Succès", description: "Informations mises à jour avec succès." });
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

  // ── Options ──────────────────────────────────
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

  // ── Services ─────────────────────────────────
  const startEditService = (service: any) => {
    setEditingServiceId(service.id);
    setServiceForm({ prixBase: service.prixBase, prixParPage: service.prixParPage });
  };

  const handleSaveService = async (service: any) => {
    setIsSavingService(true);
    try {
      await imprimerieService.updateProduit(service.id, {
        imprimerieId: shop?.id,
        typeProduit: service.typeProduit,
        formatImpression: service.formatImpression,
        prixBase: serviceForm.prixBase,
        prixParPage: serviceForm.prixParPage,
        proposePlastification: service.proposePlastification,
        prixParTypePlastification: service.prixParTypePlastification,
        proposeReliure: service.proposeReliure,
        prixParTypeReliure: service.prixParTypeReliure,
      });
      // Recharger l'imprimerie pour avoir les données fraîches
      if (user) {
        const updated = await imprimerieService.getImprimerieById(user.id.toString());
        setShop(updated);
      }
      setEditingServiceId(null);
      toast({ title: "Succès", description: "Tarif mis à jour." });
    } catch {
      toast({ title: "Erreur", description: "Impossible de sauvegarder le tarif.", variant: "destructive" });
    } finally {
      setIsSavingService(false);
    }
  };

  // ── Horaires ─────────────────────────────────
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
      if (user) {
        const updated = await imprimerieService.getImprimerieById(user.id.toString());
        setShop(updated);
      }
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
          <p className="text-muted-foreground text-center max-w-lg">
            Aucune imprimerie n'est associée à votre compte.
          </p>
        </main>
      </div>
    );
  }

  const revenue = orders.reduce((s, o) => s + o.total, 0);
  const pending = orders.filter((o) => o.status === "pending").length;

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
              <Card className="p-6">
                <h3 className="font-display font-semibold text-lg mb-6">Mon catalogue de services</h3>
                <div className="space-y-4">
                  {shop.produits && shop.produits.length > 0 ? (
                    shop.produits.map((service: any) => {
                      const isEditing = editingServiceId === service.id;
                      return (
                        <div key={service.id} className="border border-border rounded-lg overflow-hidden bg-background">
                          <div className="flex items-center justify-between p-4">
                            <div className="flex items-center gap-4">
                              <div className="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center">
                                {service.typeProduit === "DOCUMENT"
                                  ? <FileText className="h-5 w-5 text-primary" />
                                  : <ImageIcon className="h-5 w-5 text-primary" />}
                              </div>
                              <div>
                                <div className="flex items-center gap-2">
                                  <span className="font-medium">{formatEnumName(service.typeProduit)} {service.formatImpression}</span>
                                  <Badge variant="outline" className={service.actif ? "text-success" : "text-muted-foreground"}>
                                    {service.actif ? "Actif" : "Inactif"}
                                  </Badge>
                                </div>
                                {!isEditing ? (
                                  <div className="text-sm text-muted-foreground">
                                    {service.prixBase.toFixed(2)}€ de base
                                    {service.prixParPage > 0 && ` + ${service.prixParPage.toFixed(2)}€/page`}
                                  </div>
                                ) : (
                                  <div className="flex items-center gap-3 mt-2">
                                    <div className="flex items-center gap-1">
                                      <Label className="text-xs text-muted-foreground whitespace-nowrap">Base (€)</Label>
                                      <Input
                                        type="number"
                                        step="0.01"
                                        min="0"
                                        value={serviceForm.prixBase}
                                        onChange={(e) => setServiceForm(f => ({ ...f, prixBase: parseFloat(e.target.value) || 0 }))}
                                        className="w-24 h-7 text-sm"
                                      />
                                    </div>
                                    <div className="flex items-center gap-1">
                                      <Label className="text-xs text-muted-foreground whitespace-nowrap">Par page (€)</Label>
                                      <Input
                                        type="number"
                                        step="0.01"
                                        min="0"
                                        value={serviceForm.prixParPage}
                                        onChange={(e) => setServiceForm(f => ({ ...f, prixParPage: parseFloat(e.target.value) || 0 }))}
                                        className="w-24 h-7 text-sm"
                                      />
                                    </div>
                                  </div>
                                )}
                              </div>
                            </div>
                            <div className="flex gap-2 shrink-0">
                              {!isEditing ? (
                                <Button variant="outline" size="sm" onClick={() => startEditService(service)}>
                                  Modifier
                                </Button>
                              ) : (
                                <>
                                  <Button variant="outline" size="sm" onClick={() => setEditingServiceId(null)} disabled={isSavingService}>
                                    Annuler
                                  </Button>
                                  <Button size="sm" onClick={() => handleSaveService(service)} disabled={isSavingService}>
                                    {isSavingService ? "..." : "Enregistrer"}
                                  </Button>
                                </>
                              )}
                            </div>
                          </div>

                          {(service.proposeReliure || service.proposePlastification) && (
                            <div className="bg-muted/30 border-t border-border px-4 py-4 space-y-4">
                              {service.proposePlastification && service.prixParTypePlastification && (
                                <div className="rounded-md bg-background border p-3">
                                  <div className="flex items-center gap-2 mb-2">
                                    <Layers className="h-4 w-4 text-primary" />
                                    <Label className="font-medium text-sm">Plastification proposée :</Label>
                                  </div>
                                  <div className="flex flex-wrap gap-2">
                                    {Object.entries(service.prixParTypePlastification).map(([type, prix]) => (
                                      <Badge key={type} variant="secondary" className="font-normal text-xs">
                                        {formatEnumName(type)} (+{Number(prix).toFixed(2)}€)
                                      </Badge>
                                    ))}
                                  </div>
                                </div>
                              )}
                              {service.proposeReliure && service.prixParTypeReliure && (
                                <div className="rounded-md bg-background border p-3">
                                  <div className="flex items-center gap-2 mb-2">
                                    <Book className="h-4 w-4 text-primary" />
                                    <Label className="font-medium text-sm">Reliure proposée :</Label>
                                  </div>
                                  <div className="flex flex-wrap gap-2">
                                    {Object.entries(service.prixParTypeReliure).map(([type, prix]) => (
                                      <Badge key={type} variant="secondary" className="font-normal text-xs">
                                        {formatEnumName(type)} (+{Number(prix).toFixed(2)}€)
                                      </Badge>
                                    ))}
                                  </div>
                                </div>
                              )}
                            </div>
                          )}
                        </div>
                      );
                    })
                  ) : (
                    <p className="text-muted-foreground text-center py-8">Aucun produit configuré.</p>
                  )}
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
                    <Switch
                      checked={!!shop.proposeTarifEtudiant}
                      onCheckedChange={(v) => handleToggleOption("proposeTarifEtudiant", v)}
                    />
                  </div>
                </div>

                <div className="flex items-center justify-between p-4 border border-border rounded-lg bg-background">
                  <div>
                    <Label className="font-semibold flex items-center gap-2"><Zap className="h-4 w-4 text-primary" /> Impression express 2h</Label>
                    <p className="text-sm text-muted-foreground mt-1">Proposez l'impression prioritaire très rapide.</p>
                  </div>
                  <Switch
                    checked={!!shop.proposeExpress2h}
                    onCheckedChange={(v) => handleToggleOption("proposeExpress2h", v)}
                  />
                </div>

                <div className="flex items-center justify-between p-4 border border-border rounded-lg bg-background">
                  <div>
                    <Label className="font-semibold flex items-center gap-2"><Truck className="h-4 w-4 text-primary" /> Livraison à domicile</Label>
                    <p className="text-sm text-muted-foreground mt-1">Activez la livraison autour de votre boutique.</p>
                  </div>
                  <Switch
                    checked={!!shop.livraisonActive}
                    onCheckedChange={(v) => handleToggleOption("livraisonActive", v)}
                  />
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
                              {h.ferme ? (
                                <span className="text-destructive font-medium">Fermé</span>
                              ) : (
                                `${h.heureOuverture?.slice(0, 5)} - ${h.heureFermeture?.slice(0, 5)}`
                              )}
                            </div>
                          ) : (
                            <div className="flex-1 flex flex-wrap items-center gap-3">
                              <div className="flex items-center gap-2">
                                <Label className="text-xs text-muted-foreground">Fermé</Label>
                                <Switch
                                  checked={horaireForm.ferme}
                                  onCheckedChange={(v) => setHoraireForm(f => ({ ...f, ferme: v }))}
                                />
                              </div>
                              {!horaireForm.ferme && (
                                <>
                                  <div className="flex items-center gap-1">
                                    <Label className="text-xs text-muted-foreground">Ouverture</Label>
                                    <Input
                                      type="time"
                                      value={horaireForm.heureOuverture}
                                      onChange={(e) => setHoraireForm(f => ({ ...f, heureOuverture: e.target.value }))}
                                      className="w-28 h-7 text-sm"
                                    />
                                  </div>
                                  <div className="flex items-center gap-1">
                                    <Label className="text-xs text-muted-foreground">Fermeture</Label>
                                    <Input
                                      type="time"
                                      value={horaireForm.heureFermeture}
                                      onChange={(e) => setHoraireForm(f => ({ ...f, heureFermeture: e.target.value }))}
                                      className="w-28 h-7 text-sm"
                                    />
                                  </div>
                                </>
                              )}
                            </div>
                          )}

                          <div className="flex gap-2 shrink-0">
                            {!isEditing ? (
                              <Button variant="outline" size="sm" onClick={() => startEditHoraire(h)}>
                                Modifier
                              </Button>
                            ) : (
                              <>
                                <Button variant="outline" size="sm" onClick={() => setEditingHoraireId(null)} disabled={isSavingHoraire}>
                                  Annuler
                                </Button>
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
                    <Input id="shop-nom" value={shopForm.nom}
                      onChange={(e) => setShopForm(f => ({ ...f, nom: e.target.value }))}
                      readOnly={!isEditingShop}
                      onClick={() => !isEditingShop && setIsEditingShop(true)}
                      className={!isEditingShop ? "bg-muted/40" : ""} />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="shop-tva">N° de TVA</Label>
                    <Input id="shop-tva" value={shopForm.numeroTva}
                      onChange={(e) => setShopForm(f => ({ ...f, numeroTva: e.target.value }))}
                      readOnly={!isEditingShop}
                      onClick={() => !isEditingShop && setIsEditingShop(true)}
                      className={!isEditingShop ? "bg-muted/40" : ""} />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="shop-tel">Téléphone</Label>
                    <Input id="shop-tel" value={shopForm.telephoneContact}
                      onChange={(e) => setShopForm(f => ({ ...f, telephoneContact: e.target.value }))}
                      readOnly={!isEditingShop}
                      onClick={() => !isEditingShop && setIsEditingShop(true)}
                      className={!isEditingShop ? "bg-muted/40" : ""} />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="shop-email">Email</Label>
                    <Input id="shop-email" value={shopForm.emailContact}
                      onChange={(e) => setShopForm(f => ({ ...f, emailContact: e.target.value }))}
                      readOnly={!isEditingShop}
                      onClick={() => !isEditingShop && setIsEditingShop(true)}
                      className={!isEditingShop ? "bg-muted/40" : ""} />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="shop-adresse">Adresse</Label>
                    <Input id="shop-adresse" value={shopForm.adresse}
                      onChange={(e) => setShopForm(f => ({ ...f, adresse: e.target.value }))}
                      readOnly={!isEditingShop}
                      onClick={() => !isEditingShop && setIsEditingShop(true)}
                      className={!isEditingShop ? "bg-muted/40" : ""} />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="shop-ville">Ville</Label>
                    <Input id="shop-ville" value={shopForm.ville}
                      onChange={(e) => setShopForm(f => ({ ...f, ville: e.target.value }))}
                      readOnly={!isEditingShop}
                      onClick={() => !isEditingShop && setIsEditingShop(true)}
                      className={!isEditingShop ? "bg-muted/40" : ""} />
                  </div>
                  <div className="space-y-2 md:col-span-2">
                    <Label htmlFor="shop-desc">Description</Label>
                    <Textarea id="shop-desc" value={shopForm.description}
                      onChange={(e) => setShopForm(f => ({ ...f, description: e.target.value }))}
                      readOnly={!isEditingShop}
                      onClick={() => !isEditingShop && setIsEditingShop(true)}
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
