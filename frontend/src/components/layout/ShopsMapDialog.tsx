import { useState } from "react";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { MapContainer, TileLayer, CircleMarker, Popup, Tooltip, Polyline } from "react-leaflet";
import "leaflet/dist/leaflet.css";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "../ui/dialog";
import { Button } from "../ui/button";
import { Map as MapIcon, Footprints, Car, Loader2, Star } from "lucide-react";
import { formatDuration } from "../../lib/utils";
import { itineraireService } from "../../services/itineraire.service";
import type { ShopMapPoint, TravelTimeState } from "../../models/catalogue.model";
import type { PointGps } from "../../models/itineraire.model";


interface ShopsMapDialogProps {
  shops: ShopMapPoint[];
  userLocation?: PointGps | null;
}

const BELGIUM_CENTER: [number, number] = [50.5039, 4.4699];


const ShopsMapDialog = ({ shops, userLocation }: ShopsMapDialogProps) => {
  const { t } = useTranslation("imprimeries");
  const [open, setOpen] = useState(false);
  // Temps de trajet calculés à la demande (au clic sur un marqueur), pas pour
  // toute la liste — pour rester raisonnable envers les services gratuits utilisés.
  const [driveTimes, setDriveTimes] = useState<Record<string, TravelTimeState>>({});
  const [walkTimes, setWalkTimes] = useState<Record<string, TravelTimeState>>({});
  // Point actuellement sélectionné : trace le trait pointillé jusqu'à ce point
  const [selectedShop, setSelectedShop] = useState<{ lat: number; lng: number } | null>(null);

  const shopsWithCoords = shops.filter(
    (s): s is ShopMapPoint & { latitude: number; longitude: number } =>
      s.latitude != null && s.longitude != null
  );

  const center: [number, number] = userLocation
    ? [userLocation.lat, userLocation.lng]
    : BELGIUM_CENTER;
  const zoom = userLocation ? 12 : 8;

  const handleMarkerClick = (shop: ShopMapPoint & { latitude: number; longitude: number }) => {
    if (!userLocation) return;
    const destination = { lat: shop.latitude, lng: shop.longitude };
    setSelectedShop(destination);

    if (driveTimes[shop.id] === undefined) {
      setDriveTimes((prev) => ({ ...prev, [shop.id]: "loading" }));
      itineraireService.dureeEnVoitureMin(userLocation, destination).then((minutes) => {
        setDriveTimes((prev) => ({ ...prev, [shop.id]: minutes ?? "error" }));
      });
    }

    if (walkTimes[shop.id] === undefined) {
      setWalkTimes((prev) => ({ ...prev, [shop.id]: "loading" }));
      itineraireService.dureeAPiedMin(userLocation, destination).then((minutes) => {
        setWalkTimes((prev) => ({ ...prev, [shop.id]: minutes ?? "error" }));
      });
    }
  };

  const renderTravelTime = (state: TravelTimeState | undefined, label: string) => {
    if (!userLocation) return <span>{t("map.travel.positionRequired")}</span>;
    if (state === undefined || state === "loading") {
      return (
        <span className="flex items-center gap-1">
          <Loader2 className="h-3 w-3 animate-spin" /> {t("map.travel.calculating")}
        </span>
      );
    }
    if (state === "error") return <span>{t("map.travel.unavailable")}</span>;
    return <span>{formatDuration(state)} {label}</span>;
  };

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        setOpen(next);
        if (!next) setSelectedShop(null);
      }}
    >
      <DialogTrigger asChild>
        <Button variant="outline" size="icon" title={t("map.trigger")}>
          <MapIcon className="h-4 w-4" />
        </Button>
      </DialogTrigger>
      <DialogContent className="max-w-3xl">
        <DialogHeader>
          <DialogTitle className="font-display">{t("map.title")}</DialogTitle>
        </DialogHeader>

        {shopsWithCoords.length === 0 ? (
          <p className="text-sm text-muted-foreground text-center py-12">
            {t("map.empty")}
          </p>
        ) : (
          <>
            <div className="flex items-center gap-4 text-xs text-muted-foreground">
              <span className="flex items-center gap-1.5">
                <span className="h-2.5 w-2.5 rounded-full bg-emerald-500 inline-block" /> {t("status.open")}
              </span>
              <span className="flex items-center gap-1.5">
                <span className="h-2.5 w-2.5 rounded-full bg-red-500 inline-block" /> {t("status.closed")}
              </span>
              {userLocation && (
                <span className="flex items-center gap-1.5">
                  <span className="h-2.5 w-2.5 rounded-full bg-blue-500 inline-block" /> {t("map.legend.yourPosition")}
                </span>
              )}
            </div>

            <div className="h-[500px] w-full rounded-lg overflow-hidden">
              {open && (
                <MapContainer center={center} zoom={zoom} style={{ height: "100%", width: "100%" }}>
                  <TileLayer
                    attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                  />

                  {/* Trait pointillé entre votre position et l'imprimerie sélectionnée */}
                  {userLocation && selectedShop && (
                    <Polyline
                      positions={[
                        [userLocation.lat, userLocation.lng],
                        [selectedShop.lat, selectedShop.lng],
                      ]}
                      pathOptions={{ color: "#3b82f6", weight: 2.5, dashArray: "6 8", opacity: 0.8 }}
                    />
                  )}

                  {userLocation && (
                    <CircleMarker
                      center={[userLocation.lat, userLocation.lng]}
                      radius={9}
                      pathOptions={{ color: "#fff", weight: 2, fillColor: "#3b82f6", fillOpacity: 1 }}
                    >
                      <Tooltip>{t("map.legend.yourPosition")}</Tooltip>
                    </CircleMarker>
                  )}

                  {shopsWithCoords.map((shop) => (
                    <CircleMarker
                      key={shop.id}
                      center={[shop.latitude, shop.longitude]}
                      radius={8}
                      pathOptions={{
                        color: "#fff",
                        weight: 2,
                        fillColor: shop.isOpen ? "#10b981" : "#ef4444",
                        fillOpacity: 0.9,
                      }}
                      eventHandlers={{
                        click: () => handleMarkerClick(shop),
                      }}
                    >
                      <Popup>
                        <div className="space-y-1">
                          <p className="font-semibold">{shop.name}</p>
                          <p className="text-xs text-muted-foreground">{shop.address}</p>
                          <p className="text-xs">{shop.isOpen ? `🟢 ${t("status.open")}` : `🔴 ${t("status.closed")}`}</p>

                          {!!shop.reviewCount && (
                            <div className="flex items-center gap-1">
                              <Star className="h-3.5 w-3.5 text-secondary fill-secondary" />
                              <span className="text-xs font-medium">{shop.rating?.toFixed(1)}</span>
                              <span className="text-xs text-muted-foreground">({shop.reviewCount})</span>
                            </div>
                          )}

                          {shop.distanceKm != null && (
                            <div className="text-xs text-muted-foreground pt-1 space-y-0.5">
                              <div className="flex items-center gap-1.5">
                                <Footprints className="h-3 w-3 shrink-0" />
                                {renderTravelTime(walkTimes[shop.id], t("map.travel.walking"))}
                              </div>
                              <div className="flex items-center gap-1.5">
                                <Car className="h-3 w-3 shrink-0" />
                                {renderTravelTime(driveTimes[shop.id], t("map.travel.driving"))}
                              </div>
                            </div>
                          )}

                          <Link
                            to={`/imprimerie/${shop.slug ?? shop.id}`}
                            className="text-primary text-xs font-medium hover:underline"
                          >
                            {t("map.viewDetails")} →
                          </Link>
                        </div>
                      </Popup>
                    </CircleMarker>
                  ))}
                </MapContainer>
              )}
            </div>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
};

export default ShopsMapDialog;
