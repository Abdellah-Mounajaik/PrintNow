import { useState, useEffect } from "react";
import { useParams, Link, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useAuth } from "../../auth/context/AuthContext";
import { imprimerieService } from "../services/imprimerieService.service";
import type { ImprimerieDetail } from "../models/Imprimerie.model";
import { resolveFileUrl } from "../../../lib/utils";

// UI Components & Icons
import Header from "../../../components/layout/Header";
import { Button } from "../../../components/ui/button";
import { Badge } from "../../../components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "../../../components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../../../components/ui/tabs";
import {
  MapPin, Clock, Phone, Mail, Zap, GraduationCap, Truck,
  ChevronLeft, FileText, Image as ImageIcon, CreditCard, Printer, Star
} from "lucide-react";
import AvisSection from "../components/AvisSection";
import Seo from "../../../components/Seo";

interface TabItem {
  label: string;
  format: string;
  prixBase: number;
}

const PrintShopDetail = () => {
  const { t } = useTranslation("printShopDetail");
  const { slug } = useParams<{ slug: string }>();
  const { user, token } = useAuth();
  const navigate = useNavigate();
  const [shop, setShop] = useState<ImprimerieDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (slug) {
      imprimerieService.getImprimerieBySlug(slug)
        .then(data => {
          setShop(data);
          setIsLoading(false);
        })
        .catch(err => {
          setError(err.message);
          setIsLoading(false);
        });
    }
  }, [slug]);

  const todayStr = new Date().toLocaleDateString("fr-FR", { weekday: "long" }).toUpperCase();

  const formatEnumName = (text: string) => {
    if (!text) return "";
    const formatted = text.replace(/_/g, ' ').toLowerCase();
    return formatted.charAt(0).toUpperCase() + formatted.slice(1);
  };

  if (isLoading) return <div className="p-20 text-center">{t("loading")}</div>;
  if (error || !shop) return <div className="p-20 text-center text-red-500">{t("error.generic", { message: error })}</div>;

  const seoDescription = shop.description
    ? (shop.description.length > 155 ? `${shop.description.slice(0, 155)}…` : shop.description)
    : `Impression en ligne chez ${shop.nom}${shop.ville ? `, à ${shop.ville}` : ""} : documents, flyers, cartes de visite. Retrait en magasin ou livraison.`;

  const produitsActifs = shop.produits?.filter(p => p.actif) ?? [];

  // 1. DOCUMENTS
  const documentsItems: TabItem[] = [];
  const nbA4 = produitsActifs.find(p => p.typeProduit === "DOCUMENT" && p.formatImpression === "A4" && !p.couleur);
  const coulA4 = produitsActifs.find(p => p.typeProduit === "DOCUMENT" && p.formatImpression === "A4" && p.couleur);
  const nbA3 = produitsActifs.find(p => p.typeProduit === "DOCUMENT" && p.formatImpression === "A3" && !p.couleur);
  const coulA3 = produitsActifs.find(p => p.typeProduit === "DOCUMENT" && p.formatImpression === "A3" && p.couleur);

  if (nbA4) documentsItems.push({ label: t("items.documentBwA4"), format: "", prixBase: nbA4.prixBase });
  if (coulA4) documentsItems.push({ label: t("items.documentColorA4"), format: "", prixBase: coulA4.prixBase });
  if (nbA3) documentsItems.push({ label: t("items.documentBwA3"), format: "", prixBase: nbA3.prixBase });
  if (coulA3) documentsItems.push({ label: t("items.documentColorA3"), format: "", prixBase: coulA3.prixBase });

  // 2. FLYERS & AFFICHES
  const flyersItems: TabItem[] = [];
  const flyer = produitsActifs.find(p => p.typeProduit === "FLYER");
  const poster = produitsActifs.find(p => p.typeProduit === "POSTER");
  if (flyer) flyersItems.push({ label: t("items.flyers"), format: flyer.formatImpression, prixBase: flyer.prixBase });
  if (poster) flyersItems.push({ label: t("items.posters"), format: poster.formatImpression, prixBase: poster.prixBase });

  // 3. CARTES DE VISITE
  const cartesItems: TabItem[] = [];
  const carte = produitsActifs.find(p => p.typeProduit === "CARTE_VISITE");
  if (carte) cartesItems.push({ label: t("items.businessCards"), format: t("items.standardFormat"), prixBase: carte.prixBase });

  // 4. RELIURE ET PLASTIFICATION (ANTI-DOUBLONS)
  const reliureItems: TabItem[] = [];
  produitsActifs.forEach(p => {
    if (p.proposeReliure && p.prixParTypeReliure) {
      Object.entries(p.prixParTypeReliure).forEach(([type, prix]) => {
        if (type !== "AUCUNE" && prix != null) {
          const labelStr = t("items.binding", { type: formatEnumName(type) });
          if (!reliureItems.some(item => item.label === labelStr)) {
            reliureItems.push({ label: labelStr, format: t("items.perDocument"), prixBase: Number(prix) });
          }
        }
      });
    }
    if (p.proposePlastification && p.prixParTypePlastification) {
      Object.entries(p.prixParTypePlastification).forEach(([type, prix]) => {
        if (type !== "AUCUNE" && prix != null) {
          const labelStr = t("items.lamination", { type: formatEnumName(type) });
          if (!reliureItems.some(item => item.label === labelStr)) {
            reliureItems.push({ label: labelStr, format: t("items.perPage"), prixBase: Number(prix) });
          }
        }
      });
    }
  });

  const servicesStructure = [
    { id: "documents", name: t("services.documents.name"), icon: FileText, description: t("services.documents.description"), items: documentsItems },
    { id: "flyers", name: t("services.flyers.name"), icon: ImageIcon, description: t("services.flyers.description"), items: flyersItems },
    { id: "cartes", name: t("services.cartes.name"), icon: CreditCard, description: t("services.cartes.description"), items: cartesItems },
    { id: "reliure", name: t("services.reliure.name"), icon: Printer, description: t("services.reliure.description"), items: reliureItems }
  ];

  const activeServices = servicesStructure.filter(service => service.items.length > 0);

  return (
    <div className="min-h-screen flex flex-col bg-background">
      <Seo title={shop.nom} description={seoDescription} path={`/imprimerie/${slug}`} />
      <Header />
      <main className="flex-1 pt-20">
        <div className="relative h-64 md:h-80 overflow-hidden bg-slate-200">
          <img
            src={resolveFileUrl(shop.logoUrl) || "https://images.unsplash.com/photo-1562240020-ce31ccb0fa7d?w=800&fit=crop"}
            alt={shop.nom}
            className="w-full h-full object-cover"
          />
          <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent" />
          <div className="absolute top-4 left-4">
            <Button variant="secondary" size="sm" onClick={() => navigate(-1)}>
              <ChevronLeft className="h-4 w-4 mr-1" /> {t("actions.back")}
            </Button>
          </div>
        </div>

        <div className="container mx-auto px-4 -mt-20 relative z-10 pb-16">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
            <div className="lg:col-span-2 space-y-6">
              <Card>
                <CardContent className="p-6">
                  <h1 className="text-3xl font-bold mb-2">{shop.nom}</h1>
                  {shop.nombreAvis != null && shop.nombreAvis > 0 && shop.noteMoyenne != null && (
                    <div className="flex items-center gap-1.5 mb-2">
                      <Star className="h-4 w-4 fill-amber-400 text-amber-400" />
                      <span className="font-semibold">{shop.noteMoyenne.toFixed(1)}</span>
                      <span className="text-sm text-muted-foreground">
                        {t("reviews.count", { count: shop.nombreAvis })}
                      </span>
                    </div>
                  )}
                  <div className="flex items-center gap-2 text-muted-foreground mb-4">
                    <MapPin className="h-4 w-4" />
                    <span>{shop.adresse}, {shop.ville}</span>
                  </div>
                  <p className="mb-6">{shop.description}</p>
                  
                  <div className="flex flex-wrap gap-3">
                    {shop.proposeExpress2h && (
                      <Badge variant="outline" className="rounded-full px-3 py-1.5 font-normal text-slate-700 border-slate-200 shadow-sm">
                        <Zap className="w-4 h-4 mr-2 text-amber-500 fill-amber-500/20" />
                        {t("badges.express")}
                      </Badge>
                    )}
                    {shop.proposeTarifEtudiant && shop.pourcentageRemiseEtudiant && shop.pourcentageRemiseEtudiant > 0 && (
                      <Badge variant="outline" className="rounded-full px-3 py-1.5 font-normal text-slate-700 border-slate-200 shadow-sm">
                        <GraduationCap className="w-4 h-4 mr-2 text-blue-500" />
                        {t("badges.studentDiscount", { percent: shop.pourcentageRemiseEtudiant })}
                      </Badge>
                    )}
                    {shop.livraisonActive && (
                      <Badge variant="outline" className="rounded-full px-3 py-1.5 font-normal text-slate-700 border-slate-200 shadow-sm">
                        <Truck className="w-4 h-4 mr-2 text-green-500" />
                        {t("badges.delivery")}
                      </Badge>
                    )}
                  </div>
                </CardContent>
              </Card>

              <Card className="shadow-card">
                <CardHeader>
                  <CardTitle className="font-display text-xl">{t("services.title")}</CardTitle>
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
                                  {item.format && (
                                    <div className="text-xs text-muted-foreground max-w-[200px]">{item.format}</div>
                                  )}
                                </div>
                                <div className="text-right font-bold text-primary whitespace-nowrap ml-2">
                                  {item.prixBase.toFixed(2)}€
                                  <span className="text-[10px] font-normal text-muted-foreground block">{t("pricing.perUnit")}</span>
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
                      {t("services.empty")}
                    </div>
                  )}
                </CardContent>
              </Card>

              <AvisSection imprimerieId={shop.id} token={token} />
            </div>

            <div className="space-y-6">
              {user?.role === "ROLE_IMPRIMERIE" || user?.role === "ROLE_ADMIN" ? (
                <Button size="lg" className="w-full text-lg h-14" disabled>
                  {t("actions.reservedForClients")}
                </Button>
              ) : !token ? (
                <Button size="lg" className="w-full text-lg h-14" onClick={() => navigate("/login")}>
                  {t("actions.loginToOrder")}
                </Button>
              ) : (
                <Button size="lg" className="w-full text-lg h-14" asChild>
                  <Link to={`/commander/${shop.slug ?? shop.id}`}>{t("actions.orderNow")}</Link>
                </Button>
              )}
              <Card>
                <CardHeader><CardTitle className="text-lg">{t("contact.title")}</CardTitle></CardHeader>
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
                <CardHeader><CardTitle className="text-lg flex items-center gap-2"><Clock className="w-4 h-4" /> {t("hours.title")}</CardTitle></CardHeader>
                <CardContent>
                  {shop.horaires && shop.horaires.length > 0 ? (
                    <ul className="text-sm space-y-2">
                      {shop.horaires.map((h, i) => (
                        <li key={i} className={`flex justify-between ${h.jourSemaine === todayStr ? "font-bold text-primary" : "text-muted-foreground"}`}>
                          <span className="capitalize">{h.jourSemaine.toLowerCase()}</span>
                          <span>{h.ferme ? t("hours.closed") : `${h.heureOuverture.slice(0,5)} - ${h.heureFermeture.slice(0,5)}`}</span>
                        </li>
                      ))}
                    </ul>
                  ) : (
                    <p className="text-sm text-muted-foreground italic">{t("hours.empty")}</p>
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