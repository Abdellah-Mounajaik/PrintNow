import { useState } from "react";
import { Button } from "../ui/button";
import { Badge } from "../ui/badge";
import PrintShopCard, { type PrintShop } from "./PrintShopCard";
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
  X
} from "lucide-react";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../ui/select";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "../ui/sheet";
import { Checkbox } from "../ui/checkbox";
import { Label } from "../ui/label";

// Mock data
const mockShops: PrintShop[] = [
  {
    id: "1",
    name: "Print Express Bruxelles",
    address: "Rue de la Loi 42, 1000 Bruxelles",
    distance: "0.8 km",
    rating: 4.9,
    reviewCount: 234,
    isOpen: true,
    openingHours: "8h - 19h",
    image: "https://images.unsplash.com/photo-1562240020-ce31ccb0fa7d?w=400&h=300&fit=crop",
    services: ["Documents", "Flyers", "Affiches", "Cartes de visite", "Reliure"],
    hasExpressOption: true,
    hasStudentDiscount: true,
    hasDelivery: true,
    priceRange: "€€",
  },
  {
    id: "2",
    name: "CopyWorld Center",
    address: "Avenue Louise 231, 1050 Ixelles",
    distance: "1.2 km",
    rating: 4.7,
    reviewCount: 189,
    isOpen: true,
    openingHours: "9h - 18h",
    image: "https://images.unsplash.com/photo-1586075010923-2dd4570fb338?w=400&h=300&fit=crop",
    services: ["Documents", "Photos", "Affiches grand format"],
    hasExpressOption: true,
    hasStudentDiscount: false,
    hasDelivery: true,
    priceRange: "€€€",
  },
  {
    id: "3",
    name: "Imprimerie du Quartier",
    address: "Chaussée de Wavre 89, 1040 Etterbeek",
    distance: "1.5 km",
    rating: 4.5,
    reviewCount: 156,
    isOpen: false,
    openingHours: "Fermé - Ouvre à 9h",
    image: "https://images.unsplash.com/photo-1573164574472-797cdf4a583a?w=400&h=300&fit=crop",
    services: ["Documents", "Flyers", "Cartes de visite"],
    hasExpressOption: false,
    hasStudentDiscount: true,
    hasDelivery: false,
    priceRange: "€",
  },
  {
    id: "4",
    name: "Digital Print Pro",
    address: "Rue Haute 112, 1000 Bruxelles",
    distance: "2.1 km",
    rating: 4.8,
    reviewCount: 312,
    isOpen: true,
    openingHours: "8h30 - 20h",
    image: "https://images.unsplash.com/photo-1504868584819-f8e8b4b6d7e3?w=400&h=300&fit=crop",
    services: ["Documents", "Photos", "Toiles", "Stickers", "Affiches"],
    hasExpressOption: true,
    hasStudentDiscount: true,
    hasDelivery: true,
    priceRange: "€€",
  },
  {
    id: "5",
    name: "Éco-Print",
    address: "Place Flagey 18, 1050 Ixelles",
    distance: "2.4 km",
    rating: 4.6,
    reviewCount: 98,
    isOpen: true,
    openingHours: "9h - 17h30",
    image: "https://images.unsplash.com/photo-1617727553252-65863c156eb0?w=400&h=300&fit=crop",
    services: ["Documents", "Papier recyclé", "Flyers"],
    hasExpressOption: false,
    hasStudentDiscount: true,
    hasDelivery: false,
    priceRange: "€",
  },
  {
    id: "6",
    name: "PrintMaster Studio",
    address: "Avenue de Tervueren 45, 1040 Etterbeek",
    distance: "2.8 km",
    rating: 4.9,
    reviewCount: 267,
    isOpen: true,
    openingHours: "7h - 21h",
    image: "https://images.unsplash.com/photo-1557804506-669a67965ba0?w=400&h=300&fit=crop",
    services: ["Documents", "Flyers", "Affiches", "Bâches", "Roll-up"],
    hasExpressOption: true,
    hasStudentDiscount: false,
    hasDelivery: true,
    priceRange: "€€€",
  },
];

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
  const [selectedFilters, setSelectedFilters] = useState<string[]>([]);
  const [sortBy, setSortBy] = useState("distance");
  const [showOpenOnly, setShowOpenOnly] = useState(false);

  const toggleFilter = (filterId: string) => {
    setSelectedFilters(prev => 
      prev.includes(filterId) 
        ? prev.filter(f => f !== filterId)
        : [...prev, filterId]
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
              {mockShops.length} imprimeries trouvées dans votre zone
            </p>
          </div>

          <div className="flex items-center gap-3">
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
                      Ouvert maintenant
                    </Label>
                  </div>

                  {/* Services */}
                  <div>
                    <h4 className="font-medium mb-3">Services</h4>
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
                    <h4 className="font-medium mb-3">Options</h4>
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
                      Effacer les filtres
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
                Ouvert
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
              const filter = [...serviceFilters, ...optionFilters].find(f => f.id === filterId);
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

        {/* Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {mockShops.map((shop, index) => (
            <PrintShopCard key={shop.id} shop={shop} index={index} />
          ))}
        </div>

        {/* Load More */}
        <div className="flex justify-center mt-10">
          <Button variant="outline" size="lg">
            Voir plus d'imprimeries
          </Button>
        </div>
      </div>
    </section>
  );
};

export default PrintShopsSection;