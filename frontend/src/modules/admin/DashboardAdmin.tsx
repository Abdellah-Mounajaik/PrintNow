import { useEffect, useState } from "react";
import Header from "../../components/layout/Header";
import { Button } from "../../components/ui/button";
import { Card } from "../../components/ui/card";
import { Badge } from "../../components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../../components/ui/tabs";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "../../components/ui/table";
import {
  Users,
  Store,
  Euro,
  TrendingUp,
  CheckCircle2,
  Eye,
  Shield,
  Settings,
  Loader2,
  ShoppingCart,
} from "lucide-react";
import { useAuth } from "../auth/context/AuthContext";

type UserDTO = {
  id: number;
  email: string;
  prenom: string;
  nom: string;
  telephone: string;
  actif: boolean;
  roleNom: string;
};

type ImprimerieDTO = {
  id: number;
  nom: string;
  ville: string;
  emailContact: string;
  actif: boolean;
};

type CommandeDTO = {
  id: number;
  numeroCommande: string;
  statut: string;
  totalTTC: number;
  dateCreation: string;
  nomClient: string;
  nomImprimerie: string;
};

const STATUT_LABELS: Record<string, string> = {
  EN_ATTENTE_PAIEMENT: "En attente",
  PAYEE: "Payée",
  EN_COURS_IMPRESSION: "En cours",
  PRETE: "Prête",
  LIVREE: "Livrée",
  ANNULEE: "Annulée",
};

const DashboardAdmin = () => {
  const { token } = useAuth();
  const [users, setUsers] = useState<UserDTO[]>([]);
  const [imprimeries, setImprimeries] = useState<ImprimerieDTO[]>([]);
  const [commandes, setCommandes] = useState<CommandeDTO[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const headers = { Authorization: `Bearer ${token}` };
    Promise.all([
      fetch("http://localhost:8080/api/users", { headers }).then((r) => r.json()),
      fetch("http://localhost:8080/api/imprimeries").then((r) => r.json()),
      fetch("http://localhost:8080/api/commandes", { headers }).then((r) => r.json()),
    ])
      .then(([u, i, c]) => {
        setUsers(Array.isArray(u) ? u : []);
        setImprimeries(Array.isArray(i) ? i : []);
        setCommandes(Array.isArray(c) ? c : []);
      })
      .finally(() => setLoading(false));
  }, [token]);

  const imprimeriesActives = imprimeries.filter((i) => i.actif);
  const caTotal = commandes.reduce((s, c) => s + Number(c.totalTTC ?? 0), 0);
  const commissionTotale = caTotal * 0.1;
  const recentesImprimeries = [...imprimeries].sort((a, b) => b.id - a.id).slice(0, 5);

  if (loading) {
    return (
      <div className="min-h-screen flex flex-col bg-muted/30">
        <Header />
        <main className="flex-1 flex items-center justify-center">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
        </main>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex flex-col bg-muted/30">
      <Header />
      <main className="flex-1 pt-24 pb-16">
        <div className="container mx-auto px-4">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-8">
            <div>
              <h1 className="font-display text-3xl md:text-4xl font-bold text-foreground mb-2 flex items-center gap-3">
                <Shield className="h-8 w-8 text-primary" />
                Administration
              </h1>
              <p className="text-muted-foreground">
                Back-office PrintHub · Gestion globale de la plateforme
              </p>
            </div>
            <Badge variant="outline" className="w-fit">
              Admin
            </Badge>
          </div>

          {/* KPIs */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
            <Card className="p-6">
              <div className="flex items-center justify-between mb-3">
                <Users className="h-8 w-8 text-primary" />
                <TrendingUp className="h-4 w-4 text-success" />
              </div>
              <div className="font-display text-3xl font-bold">{users.length}</div>
              <div className="text-sm text-muted-foreground">Utilisateurs</div>
            </Card>
            <Card className="p-6">
              <div className="flex items-center justify-between mb-3">
                <Store className="h-8 w-8 text-secondary" />
                <TrendingUp className="h-4 w-4 text-success" />
              </div>
              <div className="font-display text-3xl font-bold">{imprimeriesActives.length}</div>
              <div className="text-sm text-muted-foreground">Imprimeries actives</div>
              <div className="text-xs text-muted-foreground mt-1">
                {imprimeries.length} au total
              </div>
            </Card>
            <Card className="p-6">
              <div className="flex items-center justify-between mb-3">
                <Euro className="h-8 w-8 text-success" />
                <TrendingUp className="h-4 w-4 text-success" />
              </div>
              <div className="font-display text-3xl font-bold">
                {caTotal.toFixed(2)}€
              </div>
              <div className="text-sm text-muted-foreground">CA total</div>
              <div className="text-xs text-success mt-1">
                Commission : {commissionTotale.toFixed(2)}€
              </div>
            </Card>
            <Card className="p-6">
              <div className="flex items-center justify-between mb-3">
                <ShoppingCart className="h-8 w-8 text-success" />
                <TrendingUp className="h-4 w-4 text-success" />
              </div>
              <div className="font-display text-3xl font-bold">{commandes.length}</div>
              <div className="text-sm text-muted-foreground">Commandes totales</div>
            </Card>
          </div>

          <Tabs defaultValue="signups" className="space-y-6">
            <TabsList>
              <TabsTrigger value="signups">Inscriptions récentes</TabsTrigger>
              <TabsTrigger value="shops">Imprimeries</TabsTrigger>
              <TabsTrigger value="users">Utilisateurs</TabsTrigger>
              <TabsTrigger value="commandes">Commandes</TabsTrigger>
            </TabsList>

            {/* Inscriptions récentes */}
            <TabsContent value="signups">
              <Card className="p-6">
                <div className="mb-4">
                  <h3 className="font-display font-semibold text-lg">
                    Inscriptions récentes
                  </h3>
                  <p className="text-sm text-muted-foreground mt-1">
                    Dernières imprimeries inscrites sur la plateforme.
                  </p>
                </div>
                {recentesImprimeries.length === 0 ? (
                  <p className="text-sm text-muted-foreground">Aucune imprimerie pour l'instant.</p>
                ) : (
                  <div className="space-y-3">
                    {recentesImprimeries.map((shop) => (
                      <div
                        key={shop.id}
                        className="flex flex-col md:flex-row md:items-center justify-between gap-4 p-4 border border-border rounded-lg"
                      >
                        <div className="flex items-center gap-4">
                          <div className="w-12 h-12 rounded-lg bg-success/10 flex items-center justify-center">
                            <CheckCircle2 className="h-6 w-6 text-success" />
                          </div>
                          <div>
                            <div className="font-semibold">{shop.nom}</div>
                            <div className="text-sm text-muted-foreground">
                              {shop.ville}
                              {shop.emailContact ? ` · ${shop.emailContact}` : ""}
                            </div>
                          </div>
                        </div>
                        <div className="flex items-center gap-3">
                          <Badge className={shop.actif ? "status-open" : "status-closed"}>
                            {shop.actif ? "Active" : "Inactive"}
                          </Badge>
                          <Button variant="outline" size="sm">
                            <Eye className="h-4 w-4 mr-1" />
                            Voir
                          </Button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </Card>
            </TabsContent>

            {/* Imprimeries */}
            <TabsContent value="shops">
              <Card className="p-6">
                <h3 className="font-display font-semibold text-lg mb-4">
                  Imprimeries partenaires
                </h3>
                {imprimeries.length === 0 ? (
                  <p className="text-sm text-muted-foreground">Aucune imprimerie.</p>
                ) : (
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Imprimerie</TableHead>
                        <TableHead>Ville</TableHead>
                        <TableHead>Email</TableHead>
                        <TableHead>Statut</TableHead>
                        <TableHead>Action</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {imprimeries.map((shop) => (
                        <TableRow key={shop.id}>
                          <TableCell className="font-medium">{shop.nom}</TableCell>
                          <TableCell>{shop.ville ?? "—"}</TableCell>
                          <TableCell className="text-muted-foreground">
                            {shop.emailContact ?? "—"}
                          </TableCell>
                          <TableCell>
                            {shop.actif ? (
                              <Badge className="status-open">Active</Badge>
                            ) : (
                              <Badge className="status-closed">Inactive</Badge>
                            )}
                          </TableCell>
                          <TableCell>
                            <Button variant="ghost" size="sm">
                              <Settings className="h-4 w-4" />
                            </Button>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                )}
              </Card>
            </TabsContent>

            {/* Utilisateurs */}
            <TabsContent value="users">
              <Card className="p-6">
                <h3 className="font-display font-semibold text-lg mb-4">
                  Gestion des utilisateurs
                </h3>
                {users.length === 0 ? (
                  <p className="text-sm text-muted-foreground">Aucun utilisateur.</p>
                ) : (
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Nom</TableHead>
                        <TableHead>Email</TableHead>
                        <TableHead>Rôle</TableHead>
                        <TableHead>Statut</TableHead>
                        <TableHead>Action</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {users.map((user) => (
                        <TableRow key={user.id}>
                          <TableCell className="font-medium">
                            {user.prenom} {user.nom}
                          </TableCell>
                          <TableCell className="text-muted-foreground">
                            {user.email}
                          </TableCell>
                          <TableCell>
                            <Badge variant="outline">{user.roleNom}</Badge>
                          </TableCell>
                          <TableCell>
                            <Badge className={user.actif ? "status-open" : "status-closed"}>
                              {user.actif ? "Actif" : "Inactif"}
                            </Badge>
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
                )}
              </Card>
            </TabsContent>

            {/* Commandes */}
            <TabsContent value="commandes">
              <Card className="p-6">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="font-display font-semibold text-lg">
                    Toutes les commandes
                  </h3>
                  <div className="text-sm text-muted-foreground">
                    Commission totale :{" "}
                    <span className="font-semibold text-success">
                      {commissionTotale.toFixed(2)}€
                    </span>
                  </div>
                </div>
                {commandes.length === 0 ? (
                  <p className="text-sm text-muted-foreground">Aucune commande.</p>
                ) : (
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>N° Commande</TableHead>
                        <TableHead>Client</TableHead>
                        <TableHead>Imprimerie</TableHead>
                        <TableHead>Total TTC</TableHead>
                        <TableHead>Commission (10%)</TableHead>
                        <TableHead>Statut</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {commandes.map((c) => (
                        <TableRow key={c.id}>
                          <TableCell className="font-mono text-xs">
                            {c.numeroCommande}
                          </TableCell>
                          <TableCell>{c.nomClient ?? "—"}</TableCell>
                          <TableCell>{c.nomImprimerie ?? "—"}</TableCell>
                          <TableCell className="font-semibold">
                            {Number(c.totalTTC).toFixed(2)}€
                          </TableCell>
                          <TableCell className="text-success">
                            {(Number(c.totalTTC) * 0.1).toFixed(2)}€
                          </TableCell>
                          <TableCell>
                            <Badge variant="outline">
                              {STATUT_LABELS[c.statut] ?? c.statut}
                            </Badge>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                )}
                {commandes.length > 0 && (
                  <div className="mt-6 p-4 rounded-lg bg-success/5 border border-success/20 flex items-center justify-between">
                    <div>
                      <div className="text-sm text-muted-foreground">CA total plateforme</div>
                      <div className="font-display text-2xl font-bold">{caTotal.toFixed(2)}€</div>
                    </div>
                    <div className="text-right">
                      <div className="text-sm text-muted-foreground">Commissions totales</div>
                      <div className="font-display text-2xl font-bold text-success">
                        {commissionTotale.toFixed(2)}€
                      </div>
                    </div>
                  </div>
                )}
              </Card>
            </TabsContent>
          </Tabs>
        </div>
      </main>
    </div>
  );
};

export default DashboardAdmin;
