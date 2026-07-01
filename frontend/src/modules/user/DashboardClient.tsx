import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import Header from "../../components/layout/Header";
import { Button } from "../../components/ui/button";
import { Card } from "../../components/ui/card";
import { Badge } from "../../components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../../components/ui/tabs";
import {
  Package,
  FileText,
  Clock,
  Euro,
  TrendingUp,
  User,
  Mail,
} from "lucide-react";
import { useAuth } from "../auth/context/AuthContext";

const STATUS_MAP: Record<string, { label: string; variant: "default"|"secondary"|"destructive"|"outline" }> = {
  EN_ATTENTE_PAIEMENT: { label: "En attente", variant: "outline" },
  PAYEE: { label: "Payée", variant: "secondary" },
  EN_COURS_IMPRESSION: { label: "En cours", variant: "default" },
  PRETE: { label: "Prêt à être retiré", variant: "default" },
  LIVREE: { label: "Livrée", variant: "default" },
  ANNULEE: { label: "Annulée", variant: "destructive" },
};

const DashboardClient = () => {
  const { user, token } = useAuth();
  const [orders, setOrders] = useState<any[]>([]);

  useEffect(() => {
    if (!token) return;
    fetch("http://localhost:8080/api/commandes/me", {
      headers: { "Authorization": `Bearer ${token}` },
    })
      .then(res => res.ok ? res.json() : [])
      .then(data => setOrders(data))
      .catch(() => setOrders([]));
  }, [token]);

  const emailPrefix = user?.email?.split("@")[0] ?? "";
  const displayName = emailPrefix.charAt(0).toUpperCase() + emailPrefix.slice(1);

  return (
    <div className="min-h-screen flex flex-col bg-muted/30">
      <Header />
      <main className="flex-1 pt-24 pb-16">
        <div className="container mx-auto px-4">

          {/* Header */}
          <div className="mb-8">
            <h1 className="font-display text-3xl md:text-4xl font-bold text-foreground mb-2">
              Bonjour, {displayName} 👋
            </h1>
            <p className="text-muted-foreground">
              Gérez vos commandes et votre profil depuis votre espace client
            </p>
          </div>

          {/* Stats */}
          {(() => {
            const now = new Date();
            const enCours = orders.filter(o => o.statut === "PAYEE" || o.statut === "EN_COURS_IMPRESSION").length;
            const totalDepense = orders.reduce((s, o) => s + Number(o.totalTTC ?? 0), 0);
            const cesMois = orders.filter(o => {
              const d = new Date(o.dateCreation);
              return d.getMonth() === now.getMonth() && d.getFullYear() === now.getFullYear();
            }).length;
            return (
              <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
                <Card className="p-6">
                  <div className="flex items-center justify-between mb-2">
                    <Package className="h-8 w-8 text-primary" />
                    <Badge variant="outline">Total</Badge>
                  </div>
                  <div className="font-display text-3xl font-bold">{orders.length}</div>
                  <div className="text-sm text-muted-foreground">Commandes</div>
                </Card>
                <Card className="p-6">
                  <div className="flex items-center justify-between mb-2">
                    <Clock className="h-8 w-8 text-warning" />
                    <Badge variant="outline">Actives</Badge>
                  </div>
                  <div className="font-display text-3xl font-bold">{enCours}</div>
                  <div className="text-sm text-muted-foreground">En cours</div>
                </Card>
                <Card className="p-6">
                  <div className="flex items-center justify-between mb-2">
                    <Euro className="h-8 w-8 text-success" />
                    <Badge variant="outline">Total</Badge>
                  </div>
                  <div className="font-display text-3xl font-bold">{totalDepense.toFixed(2)}€</div>
                  <div className="text-sm text-muted-foreground">Dépensé</div>
                </Card>
                <Card className="p-6">
                  <div className="flex items-center justify-between mb-2">
                    <TrendingUp className="h-8 w-8 text-info" />
                    <Badge variant="outline">Ce mois</Badge>
                  </div>
                  <div className="font-display text-3xl font-bold">{cesMois}</div>
                  <div className="text-sm text-muted-foreground">Commandes</div>
                </Card>
              </div>
            );
          })()}

          <Tabs defaultValue="orders" className="space-y-6">
            <TabsList>
              <TabsTrigger value="orders">Mes commandes</TabsTrigger>
              <TabsTrigger value="invoices">Factures</TabsTrigger>
              <TabsTrigger value="profile">Profil</TabsTrigger>
            </TabsList>

            {/* COMMANDES */}
            <TabsContent value="orders">
              <Card className="p-6">
                <h3 className="font-display font-semibold text-lg mb-4">Mes commandes</h3>
                {orders.length === 0 ? (
                  <div className="text-center py-16 border-2 border-dashed rounded-lg bg-muted/20">
                    <Package className="h-12 w-12 mx-auto text-muted-foreground mb-4 opacity-50" />
                    <h4 className="text-lg font-semibold mb-1">Aucune commande</h4>
                    <p className="text-muted-foreground text-sm mb-4">
                      Vous n'avez pas encore passé de commande.
                    </p>
                    <Button asChild>
                      <Link to="/">Trouver une imprimerie</Link>
                    </Button>
                  </div>
                ) : (
                  <div className="space-y-3">
                    {orders.map((order: any) => {
                      const status = STATUS_MAP[order.statut] ?? { label: order.statut, variant: "outline" };
                      return (
                        <div key={order.id} className="flex items-center justify-between p-4 border rounded-lg bg-muted/10 gap-4">
                          <div className="min-w-0">
                            <p className="font-mono font-medium text-sm">{order.numeroCommande}</p>
                            <p className="text-xs text-muted-foreground mt-0.5">
                              {order.nomImprimerie} · {new Date(order.dateCreation).toLocaleDateString("fr-BE")}
                            </p>
                          </div>
                          <div className="flex items-center gap-3 shrink-0">
                            <Badge variant={status.variant as any}>{status.label}</Badge>
                            <span className="font-semibold text-primary">{Number(order.totalTTC).toFixed(2)}€</span>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}
              </Card>
            </TabsContent>

            {/* FACTURES */}
            <TabsContent value="invoices">
              <Card className="p-6">
                <h3 className="font-display font-semibold text-lg mb-4">Mes factures</h3>
                <div className="text-center py-16 border-2 border-dashed rounded-lg bg-muted/20">
                  <FileText className="h-12 w-12 mx-auto text-muted-foreground mb-4 opacity-50" />
                  <h4 className="text-lg font-semibold mb-1">Aucune facture</h4>
                  <p className="text-muted-foreground text-sm">
                    Vos factures apparaîtront ici après votre première commande.
                  </p>
                </div>
              </Card>
            </TabsContent>

            {/* PROFIL */}
            <TabsContent value="profile">
              <Card className="p-6">
                <div className="flex items-center gap-4 mb-6">
                  <div className="w-20 h-20 rounded-full bg-primary text-primary-foreground flex items-center justify-center">
                    <User className="h-10 w-10" />
                  </div>
                  <div>
                    <h3 className="font-display font-semibold text-xl">{displayName}</h3>
                    <p className="text-muted-foreground text-sm">{user?.email}</p>
                  </div>
                </div>

                <div className="grid md:grid-cols-2 gap-4">
                  <div className="flex items-center gap-3 p-4 border border-border rounded-lg">
                    <Mail className="h-5 w-5 text-muted-foreground" />
                    <div>
                      <div className="text-xs text-muted-foreground">Email</div>
                      <div className="font-medium">{user?.email ?? "—"}</div>
                    </div>
                  </div>
                </div>

                <div className="mt-6 flex gap-3">
                  <Button variant="default" disabled>Modifier le profil</Button>
                  <Button variant="outline" disabled>Changer le mot de passe</Button>
                </div>
                <p className="text-xs text-muted-foreground mt-3">
                  La modification du profil sera disponible prochainement.
                </p>
              </Card>
            </TabsContent>
          </Tabs>

          <div className="mt-8 text-center">
            <Button variant="hero" size="lg" asChild>
              <Link to="/">Passer une nouvelle commande</Link>
            </Button>
          </div>
        </div>
      </main>
    </div>
  );
};

export default DashboardClient;
