import { useState, useEffect, useRef } from "react";
import { Button } from "../ui/button";
import { Badge } from "../ui/badge";
import PrintShopCard, { type PrintShop, type TravelTimeState } from "./PrintShopCard";
import { partnerService } from "../../modules/shop/services/partner.service";
import { toast } from "../../hooks/use-toast";
import {
  resolveFileUrl,
  haversineDistanceKm,
  fetchDrivingDurationMin,
  fetchWalkingDurationMin,
} from "../../lib/utils";
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

const serviceFilters = [
  { id: "documents", label: "Documents", icon: FileText },
  { id: "flyers", label: "Flyers & Affiches", icon: ImageIcon },
  { id: "photos", label: "Photos", icon: ImageIcon },
  { id: "cartes", label: "Cartes de visite", icon: CreditCard },
];

const optionFilters = [
  { id: "express", label: "Express 2h", icon: Zap },
  { id: "student", label: "Tarif étudiant", icon: GraduationCap },
  { id: "delivery", label: "Livraison", icon: Truck },
];

const PrintShopsSection = () => {
  const [shops, setShops] = useState<PrintShop[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedFilters, setSelectedFilters] = useState<string[]>([]);
  const [sortBy, setSortBy] = useState("distance");
  const [showOpenOnly, setShowOpenOnly] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [userLocation, setUserLocation] = useState<{ lat: number; lng: number } | null>(null);
  const [geoError, setGeoError] = useState<string | null>(null);
  const geoRequested = useRef(false);

  // Temps de trajet réels (OSRM/Valhalla), chargés directement pour toutes les
  // imprimeries dès que la position du client est connue.
  const [walkTimes, setWalkTimes] = useState<Record<string, TravelTimeState>>({});
  const [driveTimes, setDriveTimes] = useState<Record<string, TravelTimeState>>({});
  const travelFetchedIds = useRef<Set<string>>(new Set());

  // ========================================================
  // 1. FONCTION : Statut Ouvert/Fermé en temps réel
  // ========================================================
  const getShopStatus = (horaires: any[]) => {
    if (!horaires || horaires.length === 0) return { isOpen: false, text: "Voir horaires" };

    const daysMap = ["DIMANCHE", "LUNDI", "MARDI", "MERCREDI", "JEUDI", "VENDREDI", "SAMEDI"];
    const now = new Date();
    const todayString = daysMap[now.getDay()];

    const todaySchedule = horaires.find((h: any) => h.jourSemaine === todayString);

    if (!todaySchedule || todaySchedule.ferme) {
      return { isOpen: false, text: "Fermé aujourd'hui" };
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
      text: isOpenNow ? text : `Fermé (Horaires : ${text})`
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
            name: shopApi.nom,
            address: `${shopApi.adresse}, ${shopApi.ville || "Belgique"}`,
            distance: "À proximité",
            latitude: shopApi.latitude ?? null,
            longitude: shopApi.longitude ?? null,
            rating: shopApi.noteMoyenne ?? 0,
            reviewCount: shopApi.nombreAvis ?? 0,

            isOpen: status.isOpen, // 👈 Le badge sera Rouge ou Vert selon l'heure exacte
            openingHours: status.text, // 👈 Affichera "Fermé aujourd'hui" ou "8h - 19h"

            image: resolveFileUrl(shopApi.logoUrl) || "https://images.unsplash.com/photo-1562240020-ce31ccb0fa7d?w=400&h=300&fit=crop",

            services: servicesList, // 👈 Affichera "Documents", "Flyers", etc.

            hasExpressOption: !!shopApi.proposeExpress2h,
            hasDelivery: !!shopApi.livraisonActive, // 👈 Activera l'icône camion si coché !
            hasStudentDiscount: !!shopApi.proposeTarifEtudiant,
            priceRange: "€€"
          };
        });

        setShops(formattedShops);
      } catch (error) {
        console.error("Erreur backend:", error);
        toast({
          title: "Erreur",
          description: "Impossible de charger les imprimeries.",
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
        setGeoError("Activez la localisation pour trier par distance.");
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
      fetchWalkingDurationMin(userLocation, destination).then((minutes) => {
        setWalkTimes((prev) => ({ ...prev, [shop.id]: minutes ?? "error" }));
      });
      fetchDrivingDurationMin(userLocation, destination).then((minutes) => {
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

  const sortedShops = [...shopsWithDistance].sort((a, b) => {
    if (sortBy === "rating") {
      return b.rating - a.rating;
    }
    if (sortBy !== "distance") return 0;
    // Les imprimeries sans coordonnées connues passent en dernier plutôt que de casser le tri
    if (a.distanceKm == null && b.distanceKm == null) return 0;
    if (a.distanceKm == null) return 1;
    if (b.distanceKm == null) return -1;
    return a.distanceKm - b.distanceKm;
  });

  // Filtre par nom ou adresse selon le texte tapé dans la barre de recherche
  const query = searchQuery.trim().toLowerCase();
  const filteredShops = query
    ? sortedShops.filter(
        (shop) =>
          shop.name.toLowerCase().includes(query) ||
          shop.address.toLowerCase().includes(query)
      )
    : sortedShops;

  const toggleFilter = (filterId: string) => {
    setSelectedFilters(prev =>
      prev.includes(filterId) ? prev.filter(f => f !== filterId) : [...prev, filterId]
    );
  };

  const clearFilters = () => {
    setSelectedFilters([]);
    setShowOpenOnly(false);
  };

  return (
    <section className="py-16 bg-background">
      <div className="container mx-auto px-4">
        {/* Section Header */}
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 mb-8">
          <div>
            <h2 className="font-display text-3xl md:text-4xl font-bold text-foreground mb-2">
              Imprimeries près de vous
            </h2>
            <p className="text-muted-foreground">
              {isLoading ? "Recherche en cours..." : `${filteredShops.length} imprimeries trouvées dans le catalogue`}
            </p>
          </div>

          <div className="flex items-center gap-3">
            {/* Barre de recherche */}
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                type="text"
                placeholder="Rechercher une imprimerie..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-9 w-[220px]"
              />
            </div>

            {/* Sort Select */}
            <Select value={sortBy} onValueChange={setSortBy}>
              <SelectTrigger className="w-[180px]">
                <SelectValue placeholder="Trier par" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="distance">
                  <div className="flex items-center gap-2">
                    <MapPin className="h-4 w-4" />
                    Distance
                  </div>
                </SelectItem>
                <SelectItem value="rating">
                  <div className="flex items-center gap-2">
                    ⭐ Note
                  </div>
                </SelectItem>
                <SelectItem value="price-asc">Prix croissant</SelectItem>
                <SelectItem value="price-desc">Prix décroissant</SelectItem>
              </SelectContent>
            </Select>
            <ShopsMapDialog shops={filteredShops} userLocation={userLocation} />
            {sortBy === "distance" && geoError && (
              <span className="text-xs text-muted-foreground hidden md:inline">{geoError}</span>
            )}

            {/* Filter Sheet */}
            <Sheet>
              <SheetTrigger asChild>
                <Button variant="outline" className="gap-2">
                  <SlidersHorizontal className="h-4 w-4" />
                  Filtres
                  {selectedFilters.length > 0 && (
                    <Badge variant="secondary" className="ml-1 h-5 w-5 p-0 justify-center">
                      {selectedFilters.length}
                    </Badge>
                  )}
                </Button>
              </SheetTrigger>
              <SheetContent>
                <SheetHeader>
                  <SheetTitle className="font-display">Filtrer les imprimeries</SheetTitle>
                </SheetHeader>
                {/* ... Contenu du Sheet ... */}
              </SheetContent>
            </Sheet>
          </div>
        </div>

        {/* --- CONTENU PRINCIPAL --- */}
        {isLoading ? (
          <div className="flex flex-col items-center justify-center py-20 text-muted-foreground">
            <Loader2 className="h-10 w-10 animate-spin mb-4 text-primary" />
            <p>Chargement des imprimeries partenaires...</p>
          </div>
        ) : shops.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20 text-center">
            <div className="w-20 h-20 bg-muted rounded-full flex items-center justify-center mb-4">
              <Printer className="h-10 w-10 text-muted-foreground" />
            </div>
            <h3 className="text-xl font-bold mb-2">Aucun partenaire pour le moment</h3>
            <p className="text-muted-foreground">Soyez le premier à inscrire votre imprimerie !</p>
          </div>
        ) : filteredShops.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20 text-center">
            <div className="w-20 h-20 bg-muted rounded-full flex items-center justify-center mb-4">
              <Search className="h-10 w-10 text-muted-foreground" />
            </div>
            <h3 className="text-xl font-bold mb-2">Aucun résultat</h3>
            <p className="text-muted-foreground">Aucune imprimerie ne correspond à "{searchQuery}".</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {filteredShops.map((shop, index) => (
              <PrintShopCard
                key={shop.id}
                shop={shop}
                index={index}
                walkTime={walkTimes[shop.id]}
                driveTime={driveTimes[shop.id]}
              />
            ))}
          </div>
        )}

      </div>
    </section>
  );
};

export default PrintShopsSection;
