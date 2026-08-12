import { Badge } from "../ui/badge";
import { Card, CardContent } from "../ui/card";
import {
  MapPin,
  Clock,
  Star,
  Zap,
  GraduationCap,
  Truck,
  Footprints,
  Car,
  Loader2
} from "lucide-react";
import { Link } from "react-router-dom";
import { formatDuration } from "../../lib/utils";
import type { PrintShop, TravelTimeState } from "../../models/catalogue.model";




interface PrintShopCardProps {
  shop: PrintShop;
  index?: number;
  walkTime?: TravelTimeState;
  driveTime?: TravelTimeState;
  /** Prix de départ estimé, calculé à partir des produits actifs de l'imprimerie. */
  estimatedPrice: number | null;
}

const PrintShopCard = ({ shop, index = 0, walkTime, driveTime, estimatedPrice }: PrintShopCardProps) => {
  return (
    <Card
      className="group overflow-hidden hover-lift cursor-pointer border-border/50"
      style={{ animationDelay: `${index * 0.1}s` }}
    >
      <Link to={`/imprimerie/${shop.slug ?? shop.id}`}>
        {/* Image Header */}
        <div className="relative h-40 overflow-hidden">
          <img
            src={shop.image}
            alt={shop.name}
            className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110"
          />
          <div className="absolute inset-0 bg-gradient-to-t from-foreground/60 to-transparent" />
          
          {/* Status Badge */}
          <div className="absolute top-3 left-3">
            <Badge 
              variant="secondary"
              className={shop.isOpen ? "status-open" : "status-closed"}
            >
              <span className={`w-1.5 h-1.5 rounded-full mr-1.5 ${shop.isOpen ? "bg-success" : "bg-destructive"}`} />
              {shop.isOpen ? "Ouvert" : "Fermé"}
            </Badge>
          </div>

          {/* Options Badges */}
          <div className="absolute top-3 right-3 flex gap-1.5">
            {shop.hasExpressOption && (
              <Badge variant="secondary" className="bg-secondary text-secondary-foreground">
                <Zap className="h-3 w-3 mr-1" />
                Express
              </Badge>
            )}
          </div>

          {/* Distance + temps de trajet réel (OSRM/Valhalla) */}
          <div className="absolute bottom-3 left-3 flex items-center gap-2 text-primary-foreground text-sm bg-foreground/40 backdrop-blur-sm rounded-full px-2.5 py-1">
            <span className="flex items-center gap-1">
              <MapPin className="h-4 w-4" />
              {shop.distanceKm != null ? `${shop.distanceKm.toFixed(1)} km` : shop.distance}
            </span>
            {(walkTime === "loading" || driveTime === "loading") && (
              <Loader2 className="h-3.5 w-3.5 animate-spin" />
            )}
            {typeof walkTime === "number" && (
              <span className="flex items-center gap-1">
                <Footprints className="h-3.5 w-3.5" />
                {formatDuration(walkTime)}
              </span>
            )}
            {typeof driveTime === "number" && (
              <span className="flex items-center gap-1">
                <Car className="h-3.5 w-3.5" />
                {formatDuration(driveTime)}
              </span>
            )}
          </div>

          {/* Rating — affiché seulement s'il y a au moins un avis */}
          {shop.reviewCount > 0 && (
            <div className="absolute bottom-3 right-3 flex items-center gap-1 bg-card/90 backdrop-blur-sm rounded-full px-2 py-1">
              <Star className="h-4 w-4 text-secondary fill-secondary" />
              <span className="text-sm font-medium">{shop.rating.toFixed(1)}</span>
              <span className="text-xs text-muted-foreground">({shop.reviewCount})</span>
            </div>
          )}
        </div>

        <CardContent className="p-4">
          {/* Title & Address */}
          <h3 className="font-display font-semibold text-lg text-foreground mb-1 group-hover:text-primary transition-colors">
            {shop.name}
          </h3>
          <p className="text-sm text-muted-foreground mb-3 truncate">
            {shop.address}
          </p>

          {/* Opening Hours */}
          <div className="flex items-center gap-2 text-sm text-muted-foreground mb-3">
            <Clock className="h-4 w-4" />
            {shop.openingHours}
          </div>

          {/* Services */}
          <div className="flex flex-wrap gap-1.5 mb-4">
            {shop.services.slice(0, 3).map((service) => (
              <Badge key={service} variant="outline" className="text-xs">
                {service}
              </Badge>
            ))}
            {shop.services.length > 3 && (
              <Badge variant="outline" className="text-xs">
                +{shop.services.length - 3}
              </Badge>
            )}
          </div>

          {/* Footer */}
          <div className="flex items-center justify-between pt-3 border-t border-border">
            <div className="flex items-center gap-2">
              {shop.hasStudentDiscount && (
                <div className="flex items-center gap-1 text-xs text-info">
                  <GraduationCap className="h-3.5 w-3.5" />
                  -10%
                </div>
              )}
              {shop.hasDelivery && (
                <div className="flex items-center gap-1 text-xs text-muted-foreground">
                  <Truck className="h-3.5 w-3.5" />
                  Livraison
                </div>
              )}
            </div>
            <span className="text-sm font-medium text-foreground">
              {estimatedPrice != null ? `à partir de ${estimatedPrice.toFixed(2)} €` : "Prix indisponible"}
            </span>
          </div>
        </CardContent>
      </Link>
    </Card>
  );
};

export default PrintShopCard;