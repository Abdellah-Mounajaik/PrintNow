import { useState, useEffect } from "react";
import { useParams, Link } from "react-router-dom";
import { imprimerieService } from "../services/imprimerieService.service";
import type { ImprimerieDetail } from "../models/Imprimerie.model";

// UI Components & Icons
import Header from "../../../components/layout/Header";
import { Button } from "../../../components/ui/button";
import { Badge } from "../../../components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "../../../components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../../../components/ui/tabs";
import { 
  MapPin, Clock, Phone, Mail, Zap, GraduationCap, Truck, 
  ChevronLeft, FileText, Image as ImageIcon, CreditCard, Printer 
} from "lucide-react";

interface TabItem {
  label: string;
  format: string;
  prixBase: number;
  prixParPage: number;
}

const PrintShopDetail = () => {
    
  const { id } = useParams<{ id: string }>();
  const [shop, setShop] = useState<ImprimerieDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (id) {
      imprimerieService.getImprimerieById(id)
        .then(data => {
          setShop(data);
          setIsLoading(false);
        })
        .catch(err => {
          setError(err.message);
          setIsLoading(false);
        });
    }
  }, [id]);

  const todayStr = new Date().toLocaleDateString("fr-FR", { weekday: "long" }).toUpperCase();

  const formatEnumName = (text: string) => {
    if (!text) return "";
    const formatted = text.replace(/_/g, ' ').toLowerCase();
    return formatted.charAt(0).toUpperCase() + formatted.slice(1);
  };

  if (isLoading) return <div className="p-20 text-center">Chargement...</div>;
  if (error || !shop) return <div className="p-20 text-center text-red-500">Erreur : {error}</div>;

  // 1. DOCUMENTS
  const documentsItems: TabItem[] = [];
  const nbA4 = shop.produits?.find(p => p.typeProduit === "DOCUMENT" && p.formatImpression === "A4" && p.prixBase <= 0.20);
  const coulA4 = shop.produits?.find(p => p.typeProduit === "DOCUMENT" && p.formatImpression === "A4" && p.prixBase > 0.20);
  const nbA3 = shop.produits?.find(p => p.typeProduit === "DOCUMENT" && p.formatImpression === "A3" && p.prixBase <= 0.65);
  const coulA3 = shop.produits?.find(p => p.typeProduit === "DOCUMENT" && p.formatImpression === "A3" && p.prixBase > 0.65);

  if (nbA4) documentsItems.push({ label: "N&B A4", format: "Papier 80g", prixBase: nbA4.prixBase, prixParPage: nbA4.prixParPage });
  if (coulA4) documentsItems.push({ label: "Couleur A4", format: "Papier 80g", prixBase: coulA4.prixBase, prixParPage: coulA4.prixParPage });
  if (nbA3) documentsItems.push({ label: "N&B A3", format: "Papier 80g", prixBase: nbA3.prixBase, prixParPage: nbA3.prixParPage });
  if (coulA3) documentsItems.push({ label: "Couleur A3", format: "Papier 80g", prixBase: coulA3.prixBase, prixParPage: coulA3.prixParPage });

  // 2. FLYERS & AFFICHES
  const flyersItems: TabItem[] = [];
  const flyer = shop.produits?.find(p => p.typeProduit === "FLYER");
  const poster = shop.produits?.find(p => p.typeProduit === "POSTER");
  if (flyer) flyersItems.push({ label: "Flyers / Dépliants", format: flyer.formatImpression, prixBase: flyer.prixBase, prixParPage: flyer.prixParPage });
  if (poster) flyersItems.push({ label: "Affiches grand format", format: poster.formatImpression, prixBase: poster.prixBase, prixParPage: poster.prixParPage });

  // 3. CARTES DE VISITE
  const cartesItems: TabItem[] = [];
  const carte = shop.produits?.find(p => p.typeProduit === "CARTE_VISITE");
  if (carte) cartesItems.push({ label: "Cartes de visite", format: "Standard", prixBase: carte.prixBase, prixParPage: carte.prixParPage });

  // 4. RELIURE ET PLASTIFICATION (ANTI-DOUBLONS)
  const reliureItems: TabItem[] = [];
  shop.produits?.forEach(p => {
    if (p.proposeReliure && p.prixParTypeReliure) {
      Object.entries(p.prixParTypeReliure).forEach(([type, prix]) => {
        if (type !== "AUCUNE" && prix != null) {
          const labelStr = "Reliure " + formatEnumName(type);
          if (!reliureItems.some(item => item.label === labelStr)) {
            reliureItems.push({ label: labelStr, format: "Par document", prixBase: Number(prix), prixParPage: 0 });
          }
        }
      });
    }
    if (p.proposePlastification && p.prixParTypePlastification) {
      Object.entries(p.prixParTypePlastification).forEach(([type, prix]) => {
        if (type !== "AUCUNE" && prix != null) {
          const labelStr = "Plastification " + formatEnumName(type);
          if (!reliureItems.some(item => item.label === labelStr)) {
            reliureItems.push({ label: labelStr, format: "Par page", prixBase: Number(prix), prixParPage: 0 });
          }
        }
      });
    }
  });

  const servicesStructure = [
    { id: "documents", name: "Documents", icon: FileText, description: "Impression de documents A4/A3", items: documentsItems },
    { id: "flyers", name: "Flyers & Affiches", icon: ImageIcon, description: "Flyers, affiches et posters", items: flyersItems },
    { id: "cartes", name: "Cartes de visite", icon: CreditCard, description: "Cartes de visite professionnelles", items: cartesItems },
    { id: "reliure", name: "Reliure & Finitions", icon: Printer, description: "Options de reliure et plastification", items: reliureItems }
  ];

  const activeServices = servicesStructure.filter(service => service.items.length > 0);

  return (
    <div className="min-h-screen flex flex-col bg-background">
      <Header />
      <main className="flex-1 pt-20">
        <div className="relative h-64 md:h-80 overflow-hidden bg-slate-200">
          <img
            src={shop.logoUrl || "https://images.unsplash.com/photo-1562240020-ce31ccb0fa7d?w=800&fit=crop"}
            alt={shop.nom}
            className="w-full h-full object-cover"
          />
          <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent" />
          <div className="absolute top-4 left-4">
            <Button variant="secondary" size="sm" asChild>
              <Link to="/"><ChevronLeft className="h-4 w-4 mr-1" /> Retour</Link>
            </Button>
          </div>
        </div>

        <div className="container mx-auto px-4 -mt-20 relative z-10 pb-16">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
            <div className="lg:col-span-2 space-y-6">
              <Card>
                <CardContent className="p-6">
                  <h1 className="text-3xl font-bold mb-2">{shop.nom}</h1>
                  <div className="flex items-center gap-2 text-muted-foreground mb-4">
                    <MapPin className="h-4 w-4" />
                    <span>{shop.adresse}, {shop.ville}</span>
                  </div>
                  <p className="mb-6">{shop.description}</p>
                  
                  <div className="flex flex-wrap gap-3">
                    {shop.proposeExpress2h && (
                      <Badge variant="outline" className="rounded-full px-3 py-1.5 font-normal text-slate-700 border-slate-200 shadow-sm">
                        <Zap className="w-4 h-4 mr-2 text-amber-500 fill-amber-500/20" /> 
                        Express 2h disponible
                      </Badge>
                    )}
                    {shop.proposeTarifEtudiant && shop.pourcentageRemiseEtudiant && shop.pourcentageRemiseEtudiant > 0 && (
                      <Badge variant="outline" className="rounded-full px-3 py-1.5 font-normal text-slate-700 border-slate-200 shadow-sm">
                        <GraduationCap className="w-4 h-4 mr-2 text-blue-500" /> 
                        -{shop.pourcentageRemiseEtudiant}% étudiants
                      </Badge>
                    )}
                    {shop.livraisonActive && (
                      <Badge variant="outline" className="rounded-full px-3 py-1.5 font-normal text-slate-700 border-slate-200 shadow-sm">
                        <Truck className="w-4 h-4 mr-2 text-green-500" /> 
                        Livraison disponible
                      </Badge>
                    )}
                  </div>
                </CardContent>
              </Card>

              <Card className="shadow-card">
                <CardHeader>
                  <CardTitle className="font-display text-xl">Services & Tarifs</CardTitle>
                </CardHeader>
                <CardContent>
                  {activeServices.length > 0 ? (
                    <Tabs defaultValue={activeServices[0].id} className="w-full">
                      <TabsList className="w-full justify-start flex-wrap h-auto gap-2 bg-transparent p-0 mb-6">
                        {activeServices.map((service) => (
                          <TabsTrigger key={service.id} value={service.id} className="data-[state=active]:bg-primary data-[state=active]:text-primary-foreground rounded-lg px-4 py-2">
                            <service.icon className="h-4 w-4 mr-2" />
                            {service.name}
                          </TabsTrigger>
                        ))}
                      </TabsList>
                      {activeServices.map((service) => (
                        <TabsContent key={service.id} value={service.id}>
                          <p className="text-muted-foreground mb-4">{service.description}</p>
                          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                            {service.items.map((item, index) => (
                              <div key={index} className="flex justify-between items-center p-4 rounded-lg bg-muted/50 hover:bg-muted transition-colors border">
                                <div>
                                  <div className="font-semibold text-foreground">{item.label}</div>
                                  <div className="text-xs text-muted-foreground max-w-[200px]">{item.format}</div>
                                </div>
                                <div className="text-right font-bold text-primary whitespace-nowrap ml-2">
                                  {item.prixBase.toFixed(2)}€ 
                                  <span className="text-[10px] font-normal text-muted-foreground block">/ unité</span>
                                  {item.prixParPage > 0 && (
                                    <span className="text-[10px] font-normal text-muted-foreground block text-xs">
                                      + {item.prixParPage.toFixed(2)}€ / page
                                    </span>
                                  )}
                                </div>
                              </div>
                            ))}
                          </div>
                        </TabsContent>
                      ))}
                    </Tabs>
                  ) : (
                    <div className="flex flex-col items-center justify-center py-12 text-center text-muted-foreground">
                      <Printer className="h-10 w-10 mb-3 text-muted" />
                      Aucun tarif disponible.
                    </div>
                  )}
                </CardContent>
              </Card>
            </div>

            <div className="space-y-6">
              <Button size="lg" className="w-full text-lg h-14" asChild>
                <Link to={`/commander/${shop.id}`}>Commander maintenant</Link>
              </Button>
              <Card>
                <CardHeader><CardTitle className="text-lg">Contact</CardTitle></CardHeader>
                <CardContent className="space-y-3">
                  {shop.telephoneContact && (
                    <div className="flex items-center gap-2 text-sm text-muted-foreground">
                      <Phone className="w-4 h-4" /> {shop.telephoneContact}
                    </div>
                  )}
                  {shop.emailContact && (
                    <div className="flex items-center gap-2 text-sm text-muted-foreground">
                      <Mail className="w-4 h-4" /> {shop.emailContact}
                    </div>
                  )}
                </CardContent>
              </Card>
              <Card>
                <CardHeader><CardTitle className="text-lg flex items-center gap-2"><Clock className="w-4 h-4" /> Horaires</CardTitle></CardHeader>
                <CardContent>
                  {shop.horaires && shop.horaires.length > 0 ? (
                    <ul className="text-sm space-y-2">
                      {shop.horaires.map((h, i) => (
                        <li key={i} className={`flex justify-between ${h.jourSemaine === todayStr ? "font-bold text-primary" : "text-muted-foreground"}`}>
                          <span className="capitalize">{h.jourSemaine.toLowerCase()}</span>
                          <span>{h.ferme ? "Fermé" : `${h.heureOuverture.slice(0,5)} - ${h.heureFermeture.slice(0,5)}`}</span>
                        </li>
                      ))}
                    </ul>
                  ) : (
                    <p className="text-sm text-muted-foreground italic">Horaires non renseignés.</p>
                  )}
                </CardContent>
              </Card>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};

export default PrintShopDetail;