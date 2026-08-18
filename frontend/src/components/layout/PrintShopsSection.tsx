import { useState, useEffect, useRef } from "react";
import { useSearchParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Button } from "../ui/button";
import { Badge } from "../ui/badge";
import PrintShopCard from "./PrintShopCard";
import type { PrintShop, TravelTimeState } from "../../models/catalogue.model";
import { partnerService } from "../../modules/shop/services/partner.service";
import { toast } from "../../hooks/use-toast";
import { resolveFileUrl, haversineDistanceKm } from "../../lib/utils";
import { itineraireService } from "../../services/itineraire.service";
import ShopsMapDialog from "./ShopsMapDialog";

import {
  SlidersHorizontal,
  MapPin,
  Clock,
  Zap,
  GraduationCap,
  Truck,
  FileText,
  Image as ImageIcon,
  CreditCard,
  X,
  Loader2,
  Printer
} from "lucide-react";
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from "../ui/select";
import {
  Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger,
} from "../ui/sheet";
import { Checkbox } from "../ui/checkbox";
import { Label } from "../ui/label";
import { Input } from "../ui/input";
import { Search } from "lucide-react";

/** Une option de tri par prix (correspond à l'enum TypeProduit du backend), une fois traduite. */
type PriceProductTypeOption = { id: string; label: string };

// Mise en cache de la liste des imprimeries au niveau du module (survit au
// démontage/remontage du composant lors d'une navigation) pour éviter de
// rappeler le backend et de réafficher le spinner à chaque retour sur la page.
let cachedShops: PrintShop[] | null = null;

const SHOPS_PER_PAGE = 9;

const PrintShopsSection = () => {
  const { t } = useTranslation("imprimeries");
  const [shops, setShops] = useState<PrintShop[]>(cachedShops ?? []);
  const [isLoading, setIsLoading] = useState(!cachedShops);

  // Options de filtre "Services" / "Options" du panneau, avec leurs libellés traduits.
  const serviceFilters = [
    { id: "documents", label: t("filters.services.documents"), icon: FileText },
    { id: "flyers", label: t("filters.services.flyers"), icon: ImageIcon },
    { id: "cartes", label: t("filters.services.cartes"), icon: CreditCard },
  ];

  const optionFilters = [
    { id: "express", label: t("filters.options.express"), icon: Zap },
    { id: "student", label: t("filters.options.student"), icon: GraduationCap },
    { id: "delivery", label: t("filters.options.delivery"), icon: Truck },
  ];

  // Types de produits utilisables pour le tri par prix (correspond à l'enum
  // TypeProduit du backend)
  const priceProductTypes = t("priceType.types", { returnObjects: true }) as PriceProductTypeOption[];

  // Filtres/tri/recherche stockés dans l'URL plutôt qu'en state local : ils
  // survivent ainsi à la navigation vers une fiche imprimerie puis au retour.
  const [searchParams, setSearchParams] = useSearchParams();
  const searchQuery = searchParams.get("q") ?? "";
  const sortBy = searchParams.get("sort") ?? "distance";
  const showOpenOnly = searchParams.get("open") === "1";
  const selectedFilters = searchParams.get("filters")?.split(",").filter(Boolean) ?? [];

  // Applique plusieurs changements de paramètres en un seul appel à
  // setSearchParams. Important : deux appels séparés (ex: updateParam("q", ..)
  // puis updateParam("page", null)) se marchent dessus l'un l'autre — le
  // second écrase le premier avant que le re-render n'ait eu lieu, ce qui
  // annulait ce qu'on venait de taper dans la recherche.
  const updateParams = (updates: Record<string, string | null>) => {
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev);
        for (const [key, value] of Object.entries(updates)) {
          if (value === null || value === "") {
            next.delete(key);
          } else {
            next.set(key, value);
          }
        }
        return next;
      },
      { replace: true }
    );
  };

  const updateParam = (key: string, value: string | null) => updateParams({ [key]: value });

  const page = Math.max(1, Number(searchParams.get("page") ?? "1") || 1);
  const setPage = (value: number) => updateParam("page", value > 1 ? String(value) : null);

  const setSearchQuery = (value: string) => updateParams({ q: value, page: null });
  const setShowOpenOnly = (value: boolean) => updateParams({ open: value ? "1" : null, page: null });

  // Change le tri, et nettoie le paramètre "type" (spécifique au tri par prix)
  // si on quitte ce mode, pour ne pas le laisser traîner inutilement dans l'URL.
  const setSortBy = (value: string) => {
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev);
        if (value === "distance") {
          next.delete("sort");
        } else {
          next.set("sort", value);
        }
        if (value !== "price-asc" && value !== "price-desc") {
          next.delete("type");
        }
        next.delete("page");
        return next;
      },
      { replace: true }
    );
  };

  // Type de produit utilisé pour estimer un prix par imprimerie (tri par prix)
  const priceProductType = searchParams.get("type") ?? "";
  const setPriceProductType = (value: string) => updateParams({ type: value, page: null });

  const [userLocation, setUserLocation] = useState<{ lat: number; lng: number } | null>(null);
  const [geoError, setGeoError] = useState<string | null>(null);
  const geoRequested = useRef(false);

  // Remonte en haut du catalogue à chaque changement de page (pas au premier
  // rendu) : la hauteur de la grille varie selon le contenu des cartes, donc
  // rester à la même position de défilement ferait atterrir ailleurs qu'attendu.
  // On compare à la valeur précédente (pas un simple booléen "premier rendu")
  // car React StrictMode exécute cet effet deux fois au montage en dev, ce qui
  // ferait déclencher le scroll dès le chargement initial avec un booléen.
  const catalogTopRef = useRef<HTMLDivElement>(null);
  const previousPageRef = useRef(page);
  useEffect(() => {
    if (previousPageRef.current === page) return;
    previousPageRef.current = page;
    catalogTopRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
  }, [page]);

  // Temps de trajet réels (OSRM/Valhalla), chargés directement pour toutes les
  // imprimeries dès que la position du client est connue.
  const [walkTimes, setWalkTimes] = useState<Record<string, TravelTimeState>>({});
  const [driveTimes, setDriveTimes] = useState<Record<string, TravelTimeState>>({});
  const travelFetchedIds = useRef<Set<string>>(new Set());

  // ========================================================
  // 1. FONCTION : Statut Ouvert/Fermé en temps réel
  // ========================================================
  const getShopStatus = (horaires: any[]) => {
    if (!horaires || horaires.length === 0) return { isOpen: false, text: t("status.seeHours") };

    const daysMap = ["DIMANCHE", "LUNDI", "MARDI", "MERCREDI", "JEUDI", "VENDREDI", "SAMEDI"];
    const now = new Date();
    const todayString = daysMap[now.getDay()];

    const todaySchedule = horaires.find((h: any) => h.jourSemaine === todayString);

    if (!todaySchedule || todaySchedule.ferme) {
      return { isOpen: false, text: t("status.closedToday") };
    }

    const formatTime = (timeStr: string) => {
      if (!timeStr) return "";
      const [hours, minutes] = timeStr.split(':');
      return minutes === "00" ? `${parseInt(hours)}h` : `${parseInt(hours)}h${minutes}`;
    };

    const text = `${formatTime(todaySchedule.heureOuverture)} - ${formatTime(todaySchedule.heureFermeture)}`;

    // Vérification de l'heure actuelle
    const currentTimeStr = now.toLocaleTimeString('fr-FR', { hour12: false });
    const isOpenNow = currentTimeStr >= todaySchedule.heureOuverture && currentTimeStr <= todaySchedule.heureFermeture;

    return {
      isOpen: isOpenNow,
      text: isOpenNow ? text : t("status.closedWithHours", { hours: text })
    };
  };

  // ========================================================
  // 2. FONCTION : Formater la liste des services
  // ========================================================
  const formatServices = (produits: any[]) => {
    if (!produits || produits.length === 0) return ["Standard"];

    // Extraire les types de produits sans doublons
    const types = Array.from(new Set(produits.filter((p: any) => p.actif).map((p: any) => p.typeProduit)));

    // Traduire le nom du backend pour l'affichage
    const traductions: Record<string, string> = {
      DOCUMENT: "Documents",
      FLYER: "Flyers",
      CARTE_VISITE: "Cartes de visite",
      POSTER: "Affiches"
    };

    return types.map((t: any) => traductions[t] || t);
  };

  // === CHARGEMENT DES DONNÉES ===
  useEffect(() => {
    if (cachedShops) return; // déjà chargées lors d'un précédent passage sur la page

    const fetchImprimeries = async () => {
      try {
        setIsLoading(true);
        const dataBackend = await partnerService.getAllActive();

        const formattedShops: PrintShop[] = dataBackend.map((shopApi: any) => {

          // 👈 Appel de nos fonctions magiques ici
          const status = getShopStatus(shopApi.horaires);
          const servicesList = formatServices(shopApi.produits);

          return {
            id: shopApi.id.toString(),
            slug: shopApi.slug,
            name: shopApi.nom,
            address: `${shopApi.adresse}, ${shopApi.ville || "Belgique"}`,
            distance: t("card.distanceUnknown"),
            latitude: shopApi.latitude ?? null,
            longitude: shopApi.longitude ?? null,
            rating: shopApi.noteMoyenne ?? 0,
            reviewCount: shopApi.nombreAvis ?? 0,

            isOpen: status.isOpen, // 👈 Le badge sera Rouge ou Vert selon l'heure exacte
            openingHours: status.text, // 👈 Affichera "Fermé aujourd'hui" ou "8h - 19h"

            image: resolveFileUrl(shopApi.logoUrl) || "https://images.unsplash.com/photo-1562240020-ce31ccb0fa7d?w=400&h=300&fit=crop",

            services: servicesList, // 👈 Affichera "Documents", "Flyers", etc.

            produits: (shopApi.produits || []).map((p: any) => ({
              typeProduit: p.typeProduit,
              actif: !!p.actif,
              prixBase: p.prixBase ?? null,
            })),

            hasExpressOption: !!shopApi.proposeExpress2h,
            hasDelivery: !!shopApi.livraisonActive, // 👈 Activera l'icône camion si coché !
            hasStudentDiscount: !!shopApi.proposeTarifEtudiant,
          };
        });

        cachedShops = formattedShops;
        setShops(formattedShops);
      } catch (error) {
        console.error("Erreur backend:", error);
        toast({
          title: t("toast.errorTitle"),
          description: t("toast.loadError"),
          variant: "destructive"
        });
      } finally {
        setIsLoading(false);
      }
    };

    fetchImprimeries();
  }, []);

  // Demande la position du client au navigateur pour le tri par distance
  useEffect(() => {
    if (sortBy !== "distance" || geoRequested.current || !("geolocation" in navigator)) return;
    geoRequested.current = true;

    navigator.geolocation.getCurrentPosition(
      (position) => {
        setUserLocation({ lat: position.coords.latitude, lng: position.coords.longitude });
        setGeoError(null);
      },
      () => {
        setGeoError(t("geo.locationRequired"));
      },
      { timeout: 10000 }
    );
  }, [sortBy]);

  // Charge le temps de trajet réel (marche + voiture) pour toutes les imprimeries
  // dès que la position du client est connue.
  useEffect(() => {
    if (!userLocation) return;
    const toFetch = shops.filter(
      (s) => s.latitude != null && s.longitude != null && !travelFetchedIds.current.has(s.id)
    );
    if (toFetch.length === 0) return;
    toFetch.forEach((s) => travelFetchedIds.current.add(s.id));

    setWalkTimes((prev) => {
      const next = { ...prev };
      toFetch.forEach((s) => { next[s.id] = "loading"; });
      return next;
    });
    setDriveTimes((prev) => {
      const next = { ...prev };
      toFetch.forEach((s) => { next[s.id] = "loading"; });
      return next;
    });

    toFetch.forEach((shop) => {
      const destination = { lat: shop.latitude as number, lng: shop.longitude as number };
      itineraireService.dureeAPiedMin(userLocation, destination).then((minutes) => {
        setWalkTimes((prev) => ({ ...prev, [shop.id]: minutes ?? "error" }));
      });
      itineraireService.dureeEnVoitureMin(userLocation, destination).then((minutes) => {
        setDriveTimes((prev) => ({ ...prev, [shop.id]: minutes ?? "error" }));
      });
    });
  }, [shops, userLocation]);

  // Calcule la distance réelle de chaque imprimerie par rapport au client
  const shopsWithDistance = shops.map((shop) => {
    if (userLocation && shop.latitude != null && shop.longitude != null) {
      const distanceKm = haversineDistanceKm(userLocation.lat, userLocation.lng, shop.latitude, shop.longitude);
      return { ...shop, distanceKm };
    }
    return shop;
  });

  // Estime le prix d'une imprimerie : le prix de départ le plus bas parmi les
  // produits actifs correspondant au type choisi (tous formats confondus), ou
  // parmi tous ses produits actifs si aucun type n'est choisi.
  const getEstimatedPrice = (shop: PrintShop): number | null => {
    const actifs = shop.produits.filter(
      (p) => p.actif && p.prixBase != null && (!priceProductType || p.typeProduit === priceProductType)
    );
    if (actifs.length === 0) return null;
    return Math.min(...actifs.map((p) => p.prixBase as number));
  };

  const sortedShops = [...shopsWithDistance].sort((a, b) => {
    if (sortBy === "rating") {
      return b.rating - a.rating;
    }
    if (sortBy === "price-asc" || sortBy === "price-desc") {
      const priceA = getEstimatedPrice(a);
      const priceB = getEstimatedPrice(b);
      // Les imprimeries sans prix estimable passent en dernier plutôt que de casser le tri
      if (priceA == null && priceB == null) return 0;
      if (priceA == null) return 1;
      if (priceB == null) return -1;
      return sortBy === "price-asc" ? priceA - priceB : priceB - priceA;
    }
    if (sortBy !== "distance") return 0;
    // Les imprimeries sans coordonnées connues passent en dernier plutôt que de casser le tri
    if (a.distanceKm == null && b.distanceKm == null) return 0;
    if (a.distanceKm == null) return 1;
    if (b.distanceKm == null) return -1;
    return a.distanceKm - b.distanceKm;
  });

  // Filtre "Services" / "Options" du panneau de filtres
  const matchesSelectedFilters = (shop: PrintShop) => {
    if (showOpenOnly && !shop.isOpen) return false;
    return selectedFilters.every((filterId) => {
      switch (filterId) {
        case "documents":
          return shop.services.some((s) => s.toLowerCase().includes("document"));
        case "flyers":
          return shop.services.some(
            (s) => s.toLowerCase().includes("flyer") || s.toLowerCase().includes("affiche")
          );
        case "cartes":
          return shop.services.some((s) => s.toLowerCase().includes("carte"));
        case "express":
          return shop.hasExpressOption;
        case "student":
          return shop.hasStudentDiscount;
        case "delivery":
          return shop.hasDelivery;
        default:
          return true;
      }
    });
  };

  // Filtre par nom ou adresse selon le texte tapé dans la barre de recherche,
  // combiné avec les filtres "Ouvert maintenant" / Services / Options
  const query = searchQuery.trim().toLowerCase();
  const filteredShops = sortedShops.filter((shop) => {
    const matchesQuery =
      !query ||
      shop.name.toLowerCase().includes(query) ||
      shop.address.toLowerCase().includes(query);
    return matchesQuery && matchesSelectedFilters(shop);
  });

  // Pagination : la page demandée est ramenée dans les bornes valides si les
  // filtres/recherche ont réduit le nombre de résultats entre-temps.
  const totalPages = Math.max(1, Math.ceil(filteredShops.length / SHOPS_PER_PAGE));
  const currentPage = Math.min(page, totalPages);
  const paginatedShops = filteredShops.slice(
    (currentPage - 1) * SHOPS_PER_PAGE,
    currentPage * SHOPS_PER_PAGE
  );

  const toggleFilter = (filterId: string) => {
    const next = selectedFilters.includes(filterId)
      ? selectedFilters.filter((f) => f !== filterId)
      : [...selectedFilters, filterId];
    updateParams({ filters: next.join(","), page: null });
  };

  const clearFilters = () => {
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev);
        next.delete("filters");
        next.delete("open");
        next.delete("page");
        return next;
      },
      { replace: true }
    );
  };

  return (
    <section id="imprimeries" className="py-16 bg-background">
      <div className="container mx-auto px-4">
        {/* Section Header */}
        <div ref={catalogTopRef} className="flex flex-col md:flex-row md:items-end justify-between gap-4 mb-8">
          <div>
            <h2 className="font-display text-3xl md:text-4xl font-bold text-foreground mb-2">
              {t("section.title")}
            </h2>
            <p className="text-muted-foreground">
              {isLoading ? t("section.searching") : t("section.resultsCount", { count: filteredShops.length })}
            </p>
          </div>

          <div className="flex items-center gap-3">
            {/* Barre de recherche */}
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                type="text"
                placeholder={t("search.placeholder")}
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-9 w-[220px]"
              />
            </div>

            {/* Sort Select */}
            <Select value={sortBy} onValueChange={setSortBy}>
              <SelectTrigger className="w-[180px]">
                <SelectValue placeholder={t("sort.placeholder")} />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="distance">
                  <div className="flex items-center gap-2">
                    <MapPin className="h-4 w-4" />
                    {t("sort.distance")}
                  </div>
                </SelectItem>
                <SelectItem value="rating">
                  <div className="flex items-center gap-2">
                    ⭐ {t("sort.rating")}
                  </div>
                </SelectItem>
                <SelectItem value="price-asc">{t("sort.priceAsc")}</SelectItem>
                <SelectItem value="price-desc">{t("sort.priceDesc")}</SelectItem>
              </SelectContent>
            </Select>

            {/* Type de produit, pour estimer un prix plus précis lors du tri par prix */}
            {(sortBy === "price-asc" || sortBy === "price-desc") && (
              <Select value={priceProductType || "any"} onValueChange={(v) => setPriceProductType(v === "any" ? "" : v)}>
                <SelectTrigger className="w-[170px]">
                  <SelectValue placeholder={t("priceType.placeholder")} />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="any">{t("priceType.any")}</SelectItem>
                  {priceProductTypes.map((pt) => (
                    <SelectItem key={pt.id} value={pt.id}>{pt.label}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}

            <ShopsMapDialog shops={filteredShops} userLocation={userLocation} />
            {sortBy === "distance" && geoError && (
              <span className="text-xs text-muted-foreground hidden md:inline">{geoError}</span>
            )}

            {/* Filter Sheet */}
            <Sheet>
              <SheetTrigger asChild>
                <Button variant="outline" className="gap-2">
                  <SlidersHorizontal className="h-4 w-4" />
                  {t("filters.button")}
                  {selectedFilters.length > 0 && (
                    <Badge variant="secondary" className="ml-1 h-5 w-5 p-0 justify-center">
                      {selectedFilters.length}
                    </Badge>
                  )}
                </Button>
              </SheetTrigger>
              <SheetContent>
                <SheetHeader>
                  <SheetTitle className="font-display">{t("filters.title")}</SheetTitle>
                </SheetHeader>

                <div className="mt-6 space-y-6">
                  {/* Open Only */}
                  <div className="flex items-center space-x-3 p-3 rounded-lg bg-muted/50">
                    <Checkbox
                      id="open-only"
                      checked={showOpenOnly}
                      onCheckedChange={(checked) => setShowOpenOnly(checked === true)}
                    />
                    <Label htmlFor="open-only" className="flex items-center gap-2 cursor-pointer">
                      <Clock className="h-4 w-4 text-success" />
                      {t("filters.openNow")}
                    </Label>
                  </div>

                  {/* Services */}
                  <div>
                    <h4 className="font-medium mb-3">{t("filters.servicesTitle")}</h4>
                    <div className="space-y-2">
                      {serviceFilters.map((filter) => (
                        <div key={filter.id} className="flex items-center space-x-3">
                          <Checkbox
                            id={filter.id}
                            checked={selectedFilters.includes(filter.id)}
                            onCheckedChange={() => toggleFilter(filter.id)}
                          />
                          <Label htmlFor={filter.id} className="flex items-center gap-2 cursor-pointer">
                            <filter.icon className="h-4 w-4 text-muted-foreground" />
                            {filter.label}
                          </Label>
                        </div>
                      ))}
                    </div>
                  </div>

                  {/* Options */}
                  <div>
                    <h4 className="font-medium mb-3">{t("filters.optionsTitle")}</h4>
                    <div className="space-y-2">
                      {optionFilters.map((filter) => (
                        <div key={filter.id} className="flex items-center space-x-3">
                          <Checkbox
                            id={filter.id}
                            checked={selectedFilters.includes(filter.id)}
                            onCheckedChange={() => toggleFilter(filter.id)}
                          />
                          <Label htmlFor={filter.id} className="flex items-center gap-2 cursor-pointer">
                            <filter.icon className="h-4 w-4 text-muted-foreground" />
                            {filter.label}
                          </Label>
                        </div>
                      ))}
                    </div>
                  </div>

                  {/* Clear Filters */}
                  {(selectedFilters.length > 0 || showOpenOnly) && (
                    <Button
                      variant="ghost"
                      className="w-full"
                      onClick={clearFilters}
                    >
                      <X className="h-4 w-4 mr-2" />
                      {t("filters.clear")}
                    </Button>
                  )}
                </div>
              </SheetContent>
            </Sheet>
          </div>
        </div>

        {/* Active Filters */}
        {(selectedFilters.length > 0 || showOpenOnly) && (
          <div className="flex flex-wrap gap-2 mb-6">
            {showOpenOnly && (
              <Badge variant="secondary" className="gap-1 pr-1">
                <Clock className="h-3 w-3" />
                {t("status.open")}
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-4 w-4 p-0 hover:bg-transparent"
                  onClick={() => setShowOpenOnly(false)}
                >
                  <X className="h-3 w-3" />
                </Button>
              </Badge>
            )}
            {selectedFilters.map((filterId) => {
              const filter = [...serviceFilters, ...optionFilters].find((f) => f.id === filterId);
              if (!filter) return null;
              return (
                <Badge key={filterId} variant="secondary" className="gap-1 pr-1">
                  <filter.icon className="h-3 w-3" />
                  {filter.label}
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-4 w-4 p-0 hover:bg-transparent"
                    onClick={() => toggleFilter(filterId)}
                  >
                    <X className="h-3 w-3" />
                  </Button>
                </Badge>
              );
            })}
          </div>
        )}

        {/* --- CONTENU PRINCIPAL --- */}
        {isLoading ? (
          <div className="flex flex-col items-center justify-center py-20 text-muted-foreground">
            <Loader2 className="h-10 w-10 animate-spin mb-4 text-primary" />
            <p>{t("loading")}</p>
          </div>
        ) : shops.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20 text-center">
            <div className="w-20 h-20 bg-muted rounded-full flex items-center justify-center mb-4">
              <Printer className="h-10 w-10 text-muted-foreground" />
            </div>
            <h3 className="text-xl font-bold mb-2">{t("emptyStates.noPartners.title")}</h3>
            <p className="text-muted-foreground">{t("emptyStates.noPartners.description")}</p>
          </div>
        ) : filteredShops.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20 text-center">
            <div className="w-20 h-20 bg-muted rounded-full flex items-center justify-center mb-4">
              <Search className="h-10 w-10 text-muted-foreground" />
            </div>
            <h3 className="text-xl font-bold mb-2">{t("emptyStates.noResults.title")}</h3>
            <p className="text-muted-foreground">
              {query
                ? t("emptyStates.noResults.withQuery", { query: searchQuery })
                : t("emptyStates.noResults.withoutQuery")}
            </p>
          </div>
        ) : (
          <>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {paginatedShops.map((shop, index) => (
                <PrintShopCard
                  key={shop.id}
                  shop={shop}
                  index={index}
                  walkTime={walkTimes[shop.id]}
                  driveTime={driveTimes[shop.id]}
                  estimatedPrice={getEstimatedPrice(shop)}
                />
              ))}
            </div>

            {totalPages > 1 && (
              <div className="flex items-center justify-center gap-2 mt-10">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={currentPage === 1}
                  onClick={() => setPage(currentPage - 1)}
                >
                  {t("pagination.previous")}
                </Button>
                {Array.from({ length: totalPages }, (_, i) => i + 1).map((n) => (
                  <Button
                    key={n}
                    variant={n === currentPage ? "default" : "outline"}
                    size="sm"
                    className="w-9"
                    onClick={() => setPage(n)}
                  >
                    {n}
                  </Button>
                ))}
                <Button
                  variant="outline"
                  size="sm"
                  disabled={currentPage === totalPages}
                  onClick={() => setPage(currentPage + 1)}
                >
                  {t("pagination.next")}
                </Button>
              </div>
            )}
          </>
        )}

      </div>
    </section>
  );
};

export default PrintShopsSection;
