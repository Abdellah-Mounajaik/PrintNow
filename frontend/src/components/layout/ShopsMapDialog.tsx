import { useState } from "react";
import { Link } from "react-router-dom";
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
import { Map as MapIcon, Footprints, Car, Loader2 } from "lucide-react";
import { formatDuration, fetchDrivingDurationMin, fetchWalkingDurationMin } from "../../lib/utils";

interface ShopMapPoint {
  id: string;
  name: string;
  address: string;
  latitude: number | null;
  longitude: number | null;
  isOpen: boolean;
  distanceKm?: number;
}

interface ShopsMapDialogProps {
  shops: ShopMapPoint[];
  userLocation?: { lat: number; lng: number } | null;
}

const BELGIUM_CENTER: [number, number] = [50.5039, 4.4699];

type TravelTimeState = number | "loading" | "error";

const ShopsMapDialog = ({ shops, userLocation }: ShopsMapDialogProps) => {
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
      fetchDrivingDurationMin(userLocation, destination).then((minutes) => {
        setDriveTimes((prev) => ({ ...prev, [shop.id]: minutes ?? "error" }));
      });
    }

    if (walkTimes[shop.id] === undefined) {
      setWalkTimes((prev) => ({ ...prev, [shop.id]: "loading" }));
      fetchWalkingDurationMin(userLocation, destination).then((minutes) => {
        setWalkTimes((prev) => ({ ...prev, [shop.id]: minutes ?? "error" }));
      });
    }
  };

  const renderTravelTime = (state: TravelTimeState | undefined, label: string) => {
    if (!userLocation) return <span>Position requise pour l'itinéraire</span>;
    if (state === undefined || state === "loading") {
      return (
        <span className="flex items-center gap-1">
          <Loader2 className="h-3 w-3 animate-spin" /> Calcul...
        </span>
      );
    }
    if (state === "error") return <span>Itinéraire indisponible</span>;
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
        <Button variant="outline" size="icon" title="Voir sur la carte">
          <MapIcon className="h-4 w-4" />
        </Button>
      </DialogTrigger>
      <DialogContent className="max-w-3xl">
        <DialogHeader>
          <DialogTitle className="font-display">Imprimeries sur la carte</DialogTitle>
        </DialogHeader>

        {shopsWithCoords.length === 0 ? (
          <p className="text-sm text-muted-foreground text-center py-12">
            Aucune imprimerie localisée pour le moment.
          </p>
        ) : (
          <>
            <div className="flex items-center gap-4 text-xs text-muted-foreground">
              <span className="flex items-center gap-1.5">
                <span className="h-2.5 w-2.5 rounded-full bg-emerald-500 inline-block" /> Ouvert
              </span>
              <span className="flex items-center gap-1.5">
                <span className="h-2.5 w-2.5 rounded-full bg-red-500 inline-block" /> Fermé
              </span>
              {userLocation && (
                <span className="flex items-center gap-1.5">
                  <span className="h-2.5 w-2.5 rounded-full bg-blue-500 inline-block" /> Votre position
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
                      <Tooltip>Votre position</Tooltip>
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
                          <p className="text-xs">{shop.isOpen ? "🟢 Ouvert" : "🔴 Fermé"}</p>

                          {shop.distanceKm != null && (
                            <div className="text-xs text-muted-foreground pt-1 space-y-0.5">
                              <div className="flex items-center gap-1.5">
                                <Footprints className="h-3 w-3 shrink-0" />
                                {renderTravelTime(walkTimes[shop.id], "à pied")}
                              </div>
                              <div className="flex items-center gap-1.5">
                                <Car className="h-3 w-3 shrink-0" />
                                {renderTravelTime(driveTimes[shop.id], "en voiture")}
                              </div>
                            </div>
                          )}

                          <Link
                            to={`/imprimerie/${shop.id}`}
                            className="text-primary text-xs font-medium hover:underline"
                          >
                            Voir la fiche →
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
