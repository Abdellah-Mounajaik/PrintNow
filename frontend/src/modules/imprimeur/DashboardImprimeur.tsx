import { useState, useEffect } from "react";
import Header from "../../components/layout/Header";
import { Button } from "../../components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Badge } from "../../components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../../components/ui/tabs";
import { Switch } from "../../components/ui/switch";
import { Input } from "../../components/ui/input";
import { Label } from "../../components/ui/label";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "../../components/ui/table";
import {
  Package, Euro, Clock, Printer, FileText, Star,
  Eye, Download, Store, Truck, CheckCircle2, Layers, Book,
  Image as ImageIcon, CreditCard, Zap, GraduationCap
} from "lucide-react";
import { toast } from "../../hooks/use-toast";

// Imports pour la connexion au Backend
import { imprimerieService } from "../shop/services/imprimerieService.service";
import type { ImprimerieDetail } from "../shop/models/Imprimerie.model";
import { useAuth } from "../auth/context/AuthContext"; // 👈 IMPORT DU CONTEXTE D'AUTHENTIFICATION

const DashboardImprimeur = () => {
  // 👈 RÉCUPÉRATION DE L'UTILISATEUR CONNECTÉ
  const { user } = useAuth(); 

  const [shop, setShop] = useState<ImprimerieDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  
  // Les commandes sont un tableau vide pour l'instant
  const [orders, setOrders] = useState<any[]>([]); 

  useEffect(() => {
    // Si l'utilisateur n'est pas encore chargé, on attend
    if (!user || !user.id) return;

    const currentShopId = user.id.toString(); 

    imprimerieService.getImprimerieById(currentShopId)
      .then(data => {
        setShop(data);
        setIsLoading(false);
      })
      .catch(err => {
        toast({ title: "Erreur", description: "Impossible de charger les données", variant: "destructive" });
        setIsLoading(false);
      });
  }, [user]); // 👈 On recharge si l'utilisateur change

  const formatEnumName = (text: string) => {
    if (!text) return "";
    const formatted = text.replace(/_/g, ' ').toLowerCase();
    return formatted.charAt(0).toUpperCase() + formatted.slice(1);
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
            Si vous venez de vous inscrire, assurez-vous que la création s'est bien déroulée.
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
          
          {/* HEADER DASHBOARD */}
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-8">
            <div>
              <h1 className="font-display text-3xl md:text-4xl font-bold text-foreground mb-2">
                {shop.nom}
              </h1>
              <p className="text-muted-foreground flex items-center gap-2">
                <Store className="h-4 w-4" />
                Espace professionnel imprimeur
              </p>
            </div>
            <Badge className={(shop as any).actif ? "bg-success/10 text-success border-success/20" : "bg-destructive/10 text-destructive border-destructive/20"}>
              ● {(shop as any).actif ? "Boutique visible en ligne" : "Boutique hors ligne"}
            </Badge>
          </div>

          {/* KPI STATS */}
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

            {/* TAB : COMMANDES */}
            <TabsContent value="orders">
              <Card className="p-6">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="font-display font-semibold text-lg">Commandes reçues</h3>
                </div>
                {orders.length > 0 ? (
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>N° Commande</TableHead>
                        <TableHead>Client</TableHead>
                        <TableHead>Total</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                    </TableBody>
                  </Table>
                ) : (
                  <div className="text-center py-16 border-2 border-dashed rounded-lg bg-muted/20">
                    <Package className="h-12 w-12 mx-auto text-muted-foreground mb-4 opacity-50" />
                    <h4 className="text-lg font-semibold mb-1">Aucune commande</h4>
                    <p className="text-muted-foreground text-sm">
                      La réception des commandes est en cours d'intégration dans le backend.
                    </p>
                  </div>
                )}
              </Card>
            </TabsContent>

            {/* TAB : SERVICES */}
            <TabsContent value="services">
              <Card className="p-6">
                <div className="flex items-center justify-between mb-6">
                  <h3 className="font-display font-semibold text-lg">Mon catalogue de services</h3>
                </div>
                <div className="space-y-4">
                  {shop.produits && shop.produits.length > 0 ? (
                    shop.produits.map((service: any) => (
                      <div key={service.id} className="border border-border rounded-lg overflow-hidden bg-background">
                        <div className="flex items-center justify-between p-4">
                          <div className="flex items-center gap-4">
                            <div className="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center">
                              {service.typeProduit === "DOCUMENT" ? <FileText className="h-5 w-5 text-primary" /> : <ImageIcon className="h-5 w-5 text-primary" />}
                            </div>
                            <div>
                              <div className="flex items-center gap-2">
                                <span className="font-medium">{formatEnumName(service.typeProduit)} {service.formatImpression}</span>
                                <Badge variant="outline" className={service.actif ? "text-success" : "text-muted-foreground"}>
                                  {service.actif ? "Actif" : "Inactif"}
                                </Badge>
                              </div>
                              <div className="text-sm text-muted-foreground">
                                {service.prixBase.toFixed(2)}€ / de base
                                {service.prixParPage > 0 && ` + ${service.prixParPage.toFixed(2)}€ / page`}
                              </div>
                            </div>
                          </div>
                          <Button variant="outline" size="sm">Modifier</Button>
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
                                      {formatEnumName(type)} (+{Number(prix as number).toFixed(2)}€)
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
                                      {formatEnumName(type)} (+{Number(prix as number).toFixed(2)}€)
                                    </Badge>
                                  ))}
                                </div>
                              </div>
                            )}

                          </div>
                        )}
                      </div>
                    ))
                  ) : (
                    <p className="text-muted-foreground text-center py-8">Aucun produit configuré.</p>
                  )}
                </div>
              </Card>
            </TabsContent>

            {/* TAB : OPTIONS */}
            <TabsContent value="options">
              <Card className="p-6 space-y-6">
                <h3 className="font-display font-semibold text-lg">Options proposées aux clients</h3>

                <div className="flex items-center justify-between p-4 border border-border rounded-lg bg-background">
                  <div>
                    <Label className="font-semibold flex items-center gap-2"><GraduationCap className="h-4 w-4 text-primary" /> Tarif étudiant</Label>
                    <p className="text-sm text-muted-foreground mt-1">
                      Remise accordée aux étudiants sur présentation de carte.
                    </p>
                  </div>
                  <div className="flex items-center gap-3">
                    {shop.proposeTarifEtudiant && shop.pourcentageRemiseEtudiant ? (
                      <span className="text-sm font-bold text-primary">-{shop.pourcentageRemiseEtudiant}%</span>
                    ) : null}
                    <Switch checked={!!shop.proposeTarifEtudiant} disabled />
                  </div>
                </div>

                <div className="flex items-center justify-between p-4 border border-border rounded-lg bg-background">
                  <div>
                    <Label className="font-semibold flex items-center gap-2"><Zap className="h-4 w-4 text-primary" /> Impression express 2h</Label>
                    <p className="text-sm text-muted-foreground mt-1">
                      Proposez l'impression prioritaire très rapide.
                    </p>
                  </div>
                  <Switch checked={!!shop.proposeExpress2h} disabled />
                </div>

                <div className="flex items-center justify-between p-4 border border-border rounded-lg bg-background">
                  <div>
                    <Label className="font-semibold flex items-center gap-2"><Truck className="h-4 w-4 text-primary" /> Livraison à domicile</Label>
                    <p className="text-sm text-muted-foreground mt-1">
                      Activez la livraison autour de votre boutique.
                    </p>
                  </div>
                  <Switch checked={!!shop.livraisonActive} disabled />
                </div>
              </Card>
            </TabsContent>

            {/* TAB : HORAIRES */}
            <TabsContent value="hours">
              <Card className="p-6 space-y-4">
                <h3 className="font-display font-semibold text-lg">Horaires d'ouverture</h3>
                <div className="space-y-3">
                  {shop.horaires && shop.horaires.length > 0 ? (
                    shop.horaires.map((h: any, i: number) => (
                      <div key={i} className="flex items-center justify-between p-4 border border-border rounded-lg bg-background">
                        <div className="font-medium capitalize w-32">{h.jourSemaine.toLowerCase()}</div>
                        <div className="flex-1 text-sm text-muted-foreground">
                          {h.ferme ? (
                            <span className="text-destructive">Fermé</span>
                          ) : (
                            `${h.heureOuverture.slice(0, 5)} - ${h.heureFermeture.slice(0, 5)}`
                          )}
                        </div>
                        <Button variant="outline" size="sm">Modifier</Button>
                      </div>
                    ))
                  ) : (
                    <p className="text-muted-foreground text-center py-4">Horaires non configurés.</p>
                  )}
                </div>
              </Card>
            </TabsContent>

            {/* TAB : BOUTIQUE */}
            <TabsContent value="shop">
              <Card className="p-6 space-y-6">
                <h3 className="font-display font-semibold text-lg">Informations de la boutique</h3>
                <div className="grid md:grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label>Nom de l'imprimerie</Label>
                    <Input defaultValue={shop.nom || ""} readOnly />
                  </div>
                  <div className="space-y-2">
                    <Label>N° de TVA</Label>
                    <Input defaultValue={(shop as any).numeroTva || ""} readOnly />
                  </div>
                  <div className="space-y-2">
                    <Label>Téléphone</Label>
                    <Input defaultValue={(shop as any).telephoneContact || ""} readOnly />
                  </div>
                  <div className="space-y-2">
                    <Label>Email</Label>
                    <Input defaultValue={shop.emailContact || ""} readOnly />
                  </div>
                  <div className="space-y-2 md:col-span-2">
                    <Label>Adresse</Label>
                    <Input defaultValue={`${shop.adresse || ""}, ${shop.ville || ""}`} readOnly />
                  </div>
                </div>
                <p className="text-xs text-muted-foreground italic mt-4">
                  * Fonctionnalité de mise à jour des informations en cours d'intégration.
                </p>
              </Card>
            </TabsContent>

          </Tabs>
        </div>
      </main>
    </div>
  );
};

export default DashboardImprimeur;