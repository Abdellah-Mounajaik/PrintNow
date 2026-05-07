import { useState } from "react";
import Header from "../../components/layout/Header";
import { Button } from "../../components/ui/button";
import { Card } from "../../components/ui/card";
import { Badge } from "../../components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../../components/ui/tabs";
import { Switch } from "../../components/ui/switch";
import { Input } from "../../components/ui/input";
import { Label } from "../../components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../../components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "../../components/ui/table";
import {
  Package,
  Euro,
  Clock,
  Printer,
  FileText,
  Star,
  Eye,
  Download,
  Store,
  Truck,
  CheckCircle2,
} from "lucide-react";
import { toast } from "../../hooks/use-toast";

type OrderStatus = "pending" | "printing" | "ready_pickup" | "shipped" | "delivered";
type Fulfillment = "pickup" | "delivery";

interface ShopOrder {
  id: string;
  client: string;
  items: string;
  total: number;
  status: OrderStatus;
  date: string;
  fulfillment: Fulfillment;
}

const initialOrders: ShopOrder[] = [
  { id: "CMD-2025-012", client: "Marie L.", items: "Rapport 30p.", total: 18.5, status: "pending", date: "Aujourd'hui 14:32", fulfillment: "pickup" },
  { id: "CMD-2025-011", client: "Jean P.", items: "Flyers A5 × 200", total: 45.0, status: "printing", date: "Aujourd'hui 11:15", fulfillment: "delivery" },
  { id: "CMD-2025-010", client: "Sophie M.", items: "CV × 20", total: 12.0, status: "printing", date: "Hier 16:40", fulfillment: "pickup" },
  { id: "CMD-2025-009", client: "Lucas D.", items: "Affiches A3 × 50", total: 89.9, status: "delivered", date: "12 avril", fulfillment: "delivery" },
  { id: "CMD-2025-008", client: "Emma V.", items: "Thèse 150p.", total: 62.5, status: "delivered", date: "10 avril", fulfillment: "pickup" },
];

const statusConfig: Record<OrderStatus, { label: string; color: string }> = {
  pending: { label: "Nouvelle", color: "bg-warning/10 text-warning" },
  printing: { label: "En préparation", color: "bg-info/10 text-info" },
  ready_pickup: { label: "Prête à retirer", color: "bg-success/10 text-success" },
  shipped: { label: "Expédiée", color: "bg-info/10 text-info" },
  delivered: { label: "Livrée", color: "bg-muted text-muted-foreground" },
};

const services = [
  { name: "Impression N&B A4", price: 0.1, unit: "page", active: true },
  { name: "Impression couleur A4", price: 0.3, unit: "page", active: true },
  { name: "Impression A3", price: 0.8, unit: "page", active: true },
  { name: "Reliure spirale", price: 3.5, unit: "unité", active: true },
  { name: "Plastification", price: 1.5, unit: "page", active: false },
];

const DAYS = [
  { key: "mon", label: "Lundi" },
  { key: "tue", label: "Mardi" },
  { key: "wed", label: "Mercredi" },
  { key: "thu", label: "Jeudi" },
  { key: "fri", label: "Vendredi" },
  { key: "sat", label: "Samedi" },
  { key: "sun", label: "Dimanche" },
];

interface DayHours {
  open: boolean;
  start: string;
  end: string;
}

const defaultHours: Record<string, DayHours> = {
  mon: { open: true, start: "08:00", end: "19:00" },
  tue: { open: true, start: "08:00", end: "19:00" },
  wed: { open: true, start: "08:00", end: "19:00" },
  thu: { open: true, start: "08:00", end: "19:00" },
  fri: { open: true, start: "08:00", end: "19:00" },
  sat: { open: true, start: "10:00", end: "17:00" },
  sun: { open: false, start: "10:00", end: "17:00" },
};

const DashboardImprimeur = () => {
  const [studentDiscount, setStudentDiscount] = useState(true);
  const [expressPrint, setExpressPrint] = useState(true);
  const [delivery, setDelivery] = useState(false);
  const [orders, setOrders] = useState<ShopOrder[]>(initialOrders);
  const [hours, setHours] = useState<Record<string, DayHours>>(defaultHours);

  const revenue = orders.reduce((s, o) => s + o.total, 0);
  const pending = orders.filter((o) => o.status === "pending").length;

  const updateOrderStatus = (id: string, status: OrderStatus) => {
    setOrders((prev) => prev.map((o) => (o.id === id ? { ...o, status } : o)));
    toast({ title: "Statut mis à jour", description: `Commande ${id} : ${statusConfig[status].label}` });
  };

  const updateHours = (day: string, patch: Partial<DayHours>) => {
    setHours((prev) => ({ ...prev, [day]: { ...prev[day], ...patch } }));
  };

  // Available status options based on fulfillment
  const getNextStatuses = (order: ShopOrder): OrderStatus[] => {
    if (order.fulfillment === "pickup") {
      return ["pending", "printing", "ready_pickup", "delivered"];
    }
    return ["pending", "printing", "shipped", "delivered"];
  };

  return (
    <div className="min-h-screen flex flex-col bg-muted/30">
      <Header />
      <main className="flex-1 pt-24 pb-16">
        <div className="container mx-auto px-4">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-8">
            <div>
              <h1 className="font-display text-3xl md:text-4xl font-bold text-foreground mb-2">
                Imprimerie Centrale
              </h1>
              <p className="text-muted-foreground flex items-center gap-2">
                <Store className="h-4 w-4" />
                Espace professionnel imprimeur
              </p>
            </div>
            <Badge className="status-open w-fit">● Boutique ouverte</Badge>
          </div>

          {/* Stats */}
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
              <div className="font-display text-3xl font-bold">4.9</div>
              <div className="text-sm text-muted-foreground">Note (128 avis)</div>
            </Card>
          </div>

          <Tabs defaultValue="orders" className="space-y-6">
            <TabsList>
              <TabsTrigger value="orders">Commandes</TabsTrigger>
              <TabsTrigger value="services">Services & tarifs</TabsTrigger>
              <TabsTrigger value="options">Options</TabsTrigger>
              <TabsTrigger value="hours">Horaires</TabsTrigger>
              <TabsTrigger value="invoices">Factures</TabsTrigger>
              <TabsTrigger value="shop">Ma boutique</TabsTrigger>
            </TabsList>

            <TabsContent value="orders">
              <Card className="p-6">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="font-display font-semibold text-lg">Commandes reçues</h3>
                  <Button variant="outline" size="sm">
                    <Download className="h-4 w-4" />
                    Exporter
                  </Button>
                </div>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>N° Commande</TableHead>
                      <TableHead>Client</TableHead>
                      <TableHead>Articles</TableHead>
                      <TableHead>Mode</TableHead>
                      <TableHead>Date</TableHead>
                      <TableHead>Total</TableHead>
                      <TableHead>Statut</TableHead>
                      <TableHead>Action</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {orders.map((order) => (
                      <TableRow key={order.id}>
                        <TableCell className="font-mono text-xs">{order.id}</TableCell>
                        <TableCell className="font-medium">{order.client}</TableCell>
                        <TableCell>{order.items}</TableCell>
                        <TableCell>
                          <Badge variant="outline" className="gap-1">
                            {order.fulfillment === "pickup" ? (
                              <><Store className="h-3 w-3" /> Retrait</>
                            ) : (
                              <><Truck className="h-3 w-3" /> Livraison</>
                            )}
                          </Badge>
                        </TableCell>
                        <TableCell className="text-muted-foreground">{order.date}</TableCell>
                        <TableCell className="font-semibold">{order.total.toFixed(2)}€</TableCell>
                        <TableCell>
                          <Select
                            value={order.status}
                            onValueChange={(v) => updateOrderStatus(order.id, v as OrderStatus)}
                          >
                            <SelectTrigger className="w-[170px] h-8">
                              <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                              {getNextStatuses(order).map((s) => (
                                <SelectItem key={s} value={s}>
                                  {statusConfig[s].label}
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        </TableCell>
                        <TableCell>
                          <Button variant="ghost" size="sm">
                            <Eye className="h-4 w-4" />
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </Card>
            </TabsContent>

            <TabsContent value="services">
              <Card className="p-6">
                <div className="flex items-center justify-between mb-6">
                  <h3 className="font-display font-semibold text-lg">Mes services et tarifs</h3>
                  <Button variant="hero" size="sm">
                    <Printer className="h-4 w-4" />
                    Ajouter un service
                  </Button>
                </div>
                <div className="space-y-3">
                  {services.map((service, idx) => (
                    <div key={idx} className="flex items-center justify-between p-4 border border-border rounded-lg">
                      <div className="flex items-center gap-4">
                        <div className="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center">
                          <FileText className="h-5 w-5 text-primary" />
                        </div>
                        <div>
                          <div className="font-medium">{service.name}</div>
                          <div className="text-sm text-muted-foreground">
                            {service.price.toFixed(2)}€ / {service.unit}
                          </div>
                        </div>
                      </div>
                      <div className="flex items-center gap-3">
                        <Switch defaultChecked={service.active} />
                        <Button variant="ghost" size="sm">Modifier</Button>
                      </div>
                    </div>
                  ))}
                </div>
              </Card>
            </TabsContent>

            <TabsContent value="options">
              <Card className="p-6 space-y-6">
                <h3 className="font-display font-semibold text-lg">Options proposées aux clients</h3>

                <div className="flex items-center justify-between p-4 border border-border rounded-lg">
                  <div>
                    <Label className="font-semibold">Tarif étudiant</Label>
                    <p className="text-sm text-muted-foreground mt-1">
                      -15% sur présentation d'une carte étudiante valide. Désactivé automatiquement fin juin.
                    </p>
                  </div>
                  <Switch checked={studentDiscount} onCheckedChange={setStudentDiscount} />
                </div>

                <div className="flex items-center justify-between p-4 border border-border rounded-lg">
                  <div>
                    <Label className="font-semibold">Impression express 2h</Label>
                    <p className="text-sm text-muted-foreground mt-1">
                      Proposez l'impression prioritaire en 2 heures (supplément +50%).
                    </p>
                  </div>
                  <Switch checked={expressPrint} onCheckedChange={setExpressPrint} />
                </div>

                <div className="flex items-center justify-between p-4 border border-border rounded-lg">
                  <div>
                    <Label className="font-semibold">Livraison à domicile</Label>
                    <p className="text-sm text-muted-foreground mt-1">
                      Activez la livraison dans un rayon de 10 km autour de votre boutique.
                    </p>
                  </div>
                  <Switch checked={delivery} onCheckedChange={setDelivery} />
                </div>

                <div className="flex items-center justify-between p-4 border border-border rounded-lg">
                  <div>
                    <Label className="font-semibold">Codes promo</Label>
                    <p className="text-sm text-muted-foreground mt-1">
                      Créez des codes de réduction pour fidéliser vos clients.
                    </p>
                  </div>
                  <Button variant="outline" size="sm">Gérer les codes</Button>
                </div>
              </Card>
            </TabsContent>

            <TabsContent value="hours">
              <Card className="p-6 space-y-4">
                <div>
                  <h3 className="font-display font-semibold text-lg">Horaires d'ouverture</h3>
                  <p className="text-sm text-muted-foreground mt-1">
                    Définissez vos heures pour chaque jour de la semaine.
                  </p>
                </div>
                <div className="space-y-3">
                  {DAYS.map((day) => {
                    const h = hours[day.key];
                    return (
                      <div
                        key={day.key}
                        className="grid grid-cols-1 md:grid-cols-[160px_auto_1fr_1fr] items-center gap-3 p-4 border border-border rounded-lg"
                      >
                        <div className="font-medium">{day.label}</div>
                        <div className="flex items-center gap-2">
                          <Switch
                            checked={h.open}
                            onCheckedChange={(v) => updateHours(day.key, { open: v })}
                          />
                          <span className="text-sm text-muted-foreground">
                            {h.open ? "Ouvert" : "Fermé"}
                          </span>
                        </div>
                        <div className="flex items-center gap-2">
                          <Label className="text-xs text-muted-foreground w-16">Ouverture</Label>
                          <Input
                            type="time"
                            value={h.start}
                            disabled={!h.open}
                            onChange={(e) => updateHours(day.key, { start: e.target.value })}
                          />
                        </div>
                        <div className="flex items-center gap-2">
                          <Label className="text-xs text-muted-foreground w-16">Fermeture</Label>
                          <Input
                            type="time"
                            value={h.end}
                            disabled={!h.open}
                            onChange={(e) => updateHours(day.key, { end: e.target.value })}
                          />
                        </div>
                      </div>
                    );
                  })}
                </div>
                <Button
                  variant="hero"
                  onClick={() => toast({ title: "Horaires enregistrés" })}
                >
                  Enregistrer les horaires
                </Button>
              </Card>
            </TabsContent>

            <TabsContent value="invoices" className="space-y-6">
              <Card className="p-6">
                <div className="flex items-center justify-between mb-4">
                  <div>
                    <h3 className="font-display font-semibold text-lg">Factures émises aux clients</h3>
                    <p className="text-sm text-muted-foreground mt-1">
                      Une facture est générée automatiquement pour chaque commande livrée.
                    </p>
                  </div>
                  <Button variant="outline" size="sm">
                    <Download className="h-4 w-4" />
                    Tout exporter
                  </Button>
                </div>
                <div className="space-y-3">
                  {orders
                    .filter((o) => o.status === "delivered")
                    .map((order) => (
                      <div
                        key={order.id}
                        className="flex items-center justify-between p-4 border border-border rounded-lg hover:bg-muted/50 transition-colors"
                      >
                        <div className="flex items-center gap-3">
                          <FileText className="h-8 w-8 text-primary" />
                          <div>
                            <div className="font-medium">Facture {order.id}</div>
                            <div className="text-sm text-muted-foreground">
                              {order.client} · {order.date} · {order.total.toFixed(2)}€
                            </div>
                          </div>
                        </div>
                        <div className="flex items-center gap-2">
                          <Badge variant="outline" className="text-success border-success">
                            <CheckCircle2 className="h-3 w-3 mr-1" />
                            Payée
                          </Badge>
                          <Button variant="outline" size="sm">
                            <Download className="h-4 w-4" />
                            PDF
                          </Button>
                        </div>
                      </div>
                    ))}
                </div>
              </Card>

              <Card className="p-6">
                <div className="flex items-center justify-between mb-4">
                  <div>
                    <h3 className="font-display font-semibold text-lg">Factures de commission PrintHub</h3>
                    <p className="text-sm text-muted-foreground mt-1">
                      Factures émises par la plateforme — commission de 10% sur votre CA mensuel.
                    </p>
                  </div>
                  <Badge variant="outline" className="gap-1">
                    <Euro className="h-3 w-3" /> 10%
                  </Badge>
                </div>
                <div className="space-y-3">
                  {[
                    { id: "FACT-PH-2025-04", period: "Avril 2025", revenue: revenue, commission: revenue * 0.1, status: "pending" as const },
                    { id: "FACT-PH-2025-03", period: "Mars 2025", revenue: 4120, commission: 412, status: "paid" as const },
                    { id: "FACT-PH-2025-02", period: "Février 2025", revenue: 3580, commission: 358, status: "paid" as const },
                  ].map((inv) => (
                    <div
                      key={inv.id}
                      className="flex items-center justify-between p-4 border border-border rounded-lg hover:bg-muted/50 transition-colors"
                    >
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center">
                          <FileText className="h-5 w-5 text-primary" />
                        </div>
                        <div>
                          <div className="font-medium">{inv.id}</div>
                          <div className="text-sm text-muted-foreground">
                            {inv.period} · CA {inv.revenue.toFixed(0)}€ · Commission {inv.commission.toFixed(2)}€
                          </div>
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        {inv.status === "paid" ? (
                          <Badge variant="outline" className="text-success border-success">
                            <CheckCircle2 className="h-3 w-3 mr-1" />
                            Payée
                          </Badge>
                        ) : (
                          <Badge className="bg-warning/10 text-warning">À régler</Badge>
                        )}
                        <Button variant="outline" size="sm">
                          <Download className="h-4 w-4" />
                          PDF
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              </Card>
            </TabsContent>

            <TabsContent value="shop">
              <Card className="p-6 space-y-6">
                <h3 className="font-display font-semibold text-lg">Informations de la boutique</h3>
                <div className="grid md:grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label>Nom de l'imprimerie</Label>
                    <Input defaultValue="Imprimerie Centrale" />
                  </div>
                  <div className="space-y-2">
                    <Label>Téléphone</Label>
                    <Input defaultValue="+32 2 123 45 67" />
                  </div>
                  <div className="space-y-2 md:col-span-2">
                    <Label>Adresse</Label>
                    <Input defaultValue="Avenue Louise 120, 1050 Bruxelles" />
                  </div>
                </div>
                <Button variant="hero">Enregistrer les modifications</Button>
              </Card>
            </TabsContent>
          </Tabs>
        </div>
      </main>
    </div>
  );
};

export default DashboardImprimeur;
