import { useState } from "react";
import { Link } from "react-router-dom";
import { MapContainer, TileLayer, Marker, Popup } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import markerIcon2x from "leaflet/dist/images/marker-icon-2x.png";
import markerIcon from "leaflet/dist/images/marker-icon.png";
import markerShadow from "leaflet/dist/images/marker-shadow.png";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "../ui/dialog";
import { Button } from "../ui/button";
import { Map as MapIcon } from "lucide-react";

// Correctif nécessaire avec Vite/Webpack : les icônes par défaut de Leaflet
// ne se chargent pas sans réassigner explicitement leurs URLs.
delete (L.Icon.Default.prototype as unknown as { _getIconUrl?: unknown })._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x,
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
});

interface ShopMapPoint {
  id: string;
  name: string;
  address: string;
  latitude: number | null;
  longitude: number | null;
}

interface ShopsMapDialogProps {
  shops: ShopMapPoint[];
}

const BELGIUM_CENTER: [number, number] = [50.5039, 4.4699];

const ShopsMapDialog = ({ shops }: ShopsMapDialogProps) => {
  const [open, setOpen] = useState(false);

  const shopsWithCoords = shops.filter(
    (s): s is ShopMapPoint & { latitude: number; longitude: number } =>
      s.latitude != null && s.longitude != null
  );

  return (
    <Dialog open={open} onOpenChange={setOpen}>
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
          <div className="h-[500px] w-full rounded-lg overflow-hidden">
            {open && (
              <MapContainer
                center={BELGIUM_CENTER}
                zoom={8}
                style={{ height: "100%", width: "100%" }}
              >
                <TileLayer
                  attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                  url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                />
                {shopsWithCoords.map((shop) => (
                  <Marker key={shop.id} position={[shop.latitude, shop.longitude]}>
                    <Popup>
                      <div className="space-y-1">
                        <p className="font-semibold">{shop.name}</p>
                        <p className="text-xs text-muted-foreground">{shop.address}</p>
                        <Link
                          to={`/imprimerie/${shop.id}`}
                          className="text-primary text-xs font-medium hover:underline"
                        >
                          Voir la fiche →
                        </Link>
                      </div>
                    </Popup>
                  </Marker>
                ))}
              </MapContainer>
            )}
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
};

export default ShopsMapDialog;
