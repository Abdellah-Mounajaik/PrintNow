import { useEffect, useState } from "react";
import Header from "../../../components/layout/Header";
import { Button } from "../../../components/ui/button";
import { Card } from "../../../components/ui/card";
import { Badge } from "../../../components/ui/badge";
import { Input } from "../../../components/ui/input";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../../../components/ui/tabs";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "../../../components/ui/table";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "../../../components/ui/dialog";
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
  GraduationCap,
  XCircle,
  Download,
  Search,
} from "lucide-react";
import { toast } from "../../../hooks/use-toast";
import { useAuth } from "../../auth/context/AuthContext";
import { adminService } from "../services/admin.service";
import type {
  UserDTO,
  ImprimerieDTO,
  CommandeDTO,
  VerificationDTO,
} from "../models/admin.model";

const STATUT_VERIF_CONFIG: Record<string, { label: string; className: string }> = {
  EN_ATTENTE: { label: "En attente", className: "bg-warning/10 text-warning border-warning/30" },
  ACCEPTE:    { label: "Acceptée",   className: "status-open" },
  REFUSE:     { label: "Refusée",    className: "status-closed" },
  EXPIRE:     { label: "Expirée",    className: "bg-muted text-muted-foreground" },
};

const AuthImage = ({ url, alt, token }: { url: string; alt: string; token: string }) => {
  const [src, setSrc] = useState<string | null>(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    adminService.fetchImageBlob(url, token)
      .then(setSrc)
      .catch(() => setError(true));
  }, [url, token]);

  if (error) return <p className="text-xs text-destructive">{alt} introuvable</p>;
  if (!src) return (
    <div className="flex items-center gap-2 text-xs text-muted-foreground">
      <Loader2 className="h-4 w-4 animate-spin" /> Chargement {alt}…
    </div>
  );
  return (
    <div className="space-y-1">
      <p className="text-xs font-medium text-muted-foreground">{alt}</p>
      <img src={src} alt={alt} className="max-h-52 rounded-lg object-contain border border-border" />
    </div>
  );
};

const STATUT_LABELS: Record<string, string> = {
  EN_ATTENTE_PAIEMENT: "En attente",
  PAYEE: "Payée",
  EN_COURS_IMPRESSION: "En cours",
  PRETE: "Prête",
  LIVREE: "Livrée",
  ANNULEE: "Annulée",
};

// Un relevé de commission n'existe que pour une commande dont le paiement a
// bien été confirmé (même condition que côté backend, FactureCommissionService.STATUTS_FACTURABLES).
const STATUTS_FACTURABLES = new Set(["PAYEE", "EN_COURS_IMPRESSION", "PRETE", "LIVREE"]);

const DashboardAdmin = () => {
  const { token } = useAuth();
  const [users, setUsers] = useState<UserDTO[]>([]);
  const [imprimeries, setImprimeries] = useState<ImprimerieDTO[]>([]);
  const [shopSearchQuery, setShopSearchQuery] = useState("");
  const [commandes, setCommandes] = useState<CommandeDTO[]>([]);
  const [verifications, setVerifications] = useState<VerificationDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedVerif, setSelectedVerif] = useState<VerificationDTO | null>(null);
  const [motifRefus, setMotifRefus] = useState("");
  const [downloadingCommissionId, setDownloadingCommissionId] = useState<number | null>(null);

  useEffect(() => {
    if (!token) return;
    Promise.all([
      adminService.getUsers(token),
      adminService.getImprimeries(),
      adminService.getCommandes(token),
      adminService.getVerifications(token),
    ])
      .then(([u, i, c, v]) => {
        setUsers(Array.isArray(u) ? u : []);
        setImprimeries(Array.isArray(i) ? i : []);
        setCommandes(Array.isArray(c) ? c : []);
        setVerifications(Array.isArray(v) ? v : []);
      })
      .finally(() => setLoading(false));
  }, [token]);

  const handleValider = async (id: number) => {
    if (!token) return;
    try {
      const updated = await adminService.validerVerification(id, token);
      setVerifications((prev) => prev.map((v) => (v.id === id ? updated : v)));
      toast({ title: "Vérification acceptée" });
    } catch {
      toast({ title: "Erreur", description: "Impossible de valider.", variant: "destructive" });
    }
  };

  const handleRefuser = async (id: number, motif: string) => {
    if (!token) return;
    try {
      const updated = await adminService.refuserVerification(id, motif, token);
      setVerifications((prev) => prev.map((v) => (v.id === id ? updated : v)));
      toast({ title: "Vérification refusée", variant: "destructive" });
    } catch {
      toast({ title: "Erreur", description: "Impossible de refuser.", variant: "destructive" });
    }
  };

  const handleTelechargerCommission = async (commandeId: number, numeroCommande: string) => {
    if (!token) return;
    setDownloadingCommissionId(commandeId);
    try {
      await adminService.telechargerFactureCommission(commandeId, numeroCommande, token);
    } catch (e) {
      toast({ title: "Erreur", description: (e as Error).message, variant: "destructive" });
    } finally {
      setDownloadingCommissionId(null);
    }
  };

  const imprimeriesActives = imprimeries.filter((i) => i.actif);
  const caTotal = commandes.reduce((s, c) => s + Number(c.totalTTC ?? 0), 0);
  const commissionTotale = caTotal * 0.1;
  const recentesImprimeries = [...imprimeries].sort((a, b) => b.id - a.id).slice(0, 5);

  const shopSearchNormalized = shopSearchQuery.trim().toLowerCase();
  const filteredImprimeries = shopSearchNormalized
    ? imprimeries.filter((shop) =>
        shop.nom?.toLowerCase().includes(shopSearchNormalized) ||
        shop.ville?.toLowerCase().includes(shopSearchNormalized) ||
        shop.emailContact?.toLowerCase().includes(shopSearchNormalized)
      )
    : imprimeries;

  if (loading) {
    return (
      <div className="min-h-screen flex flex-col bg-muted/30">
        <Header />
        <main className="flex-1 flex items-center justify-center">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
        </main>
ue       </div>
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
                Back-office PrintNow · Gestion globale de la plateforme
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
              <TabsTrigger value="verifications">
                Vérifications étudiantes
                {verifications.filter((v) => v.statut === "EN_ATTENTE").length > 0 && (
                  <span className="ml-2 inline-flex items-center justify-center w-5 h-5 text-xs font-bold rounded-full bg-warning text-warning-foreground">
                    {verifications.filter((v) => v.statut === "EN_ATTENTE").length}
                  </span>
                )}
              </TabsTrigger>
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
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4">
                  <h3 className="font-display font-semibold text-lg">
                    Imprimeries partenaires
                  </h3>
                  {imprimeries.length > 0 && (
                    <div className="relative sm:w-80">
                      <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                      <Input
                        type="text"
                        placeholder="Rechercher par nom, ville ou email..."
                        value={shopSearchQuery}
                        onChange={(e) => setShopSearchQuery(e.target.value)}
                        className="pl-9"
                      />
                    </div>
                  )}
                </div>
                {imprimeries.length === 0 ? (
                  <p className="text-sm text-muted-foreground">Aucune imprimerie.</p>
                ) : filteredImprimeries.length === 0 ? (
                  <div className="text-center py-16 border-2 border-dashed rounded-lg bg-muted/20">
                    <Search className="h-12 w-12 mx-auto text-muted-foreground mb-4 opacity-50" />
                    <h4 className="text-lg font-semibold mb-1">Aucun résultat</h4>
                    <p className="text-muted-foreground text-sm">Aucune imprimerie ne correspond à "{shopSearchQuery}".</p>
                  </div>
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
                      {filteredImprimeries.map((shop) => (
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
                        <TableHead>Relevé</TableHead>
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
                          <TableCell>
                            {STATUTS_FACTURABLES.has(c.statut) ? (
                              <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => handleTelechargerCommission(c.id, c.numeroCommande)}
                                disabled={downloadingCommissionId === c.id}
                              >
                                {downloadingCommissionId === c.id ? (
                                  <Loader2 className="h-4 w-4 animate-spin" />
                                ) : (
                                  <Download className="h-4 w-4" />
                                )}
                              </Button>
                            ) : (
                              <span className="text-xs text-muted-foreground">—</span>
                            )}
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
            {/* Vérifications étudiantes */}
            <TabsContent value="verifications">
              <Card className="p-6">
                <div className="flex items-center justify-between mb-4">
                  <div>
                    <h3 className="font-display font-semibold text-lg flex items-center gap-2">
                      <GraduationCap className="h-5 w-5 text-primary" />
                      Vérifications étudiantes
                    </h3>
                    <p className="text-sm text-muted-foreground mt-1">
                      Validité jusqu'au 30 juin · renouvellement annuel obligatoire
                    </p>
                  </div>
                  <Badge variant="outline">
                    {verifications.filter((v) => v.statut === "EN_ATTENTE").length} en attente
                  </Badge>
                </div>

                {verifications.length === 0 ? (
                  <p className="text-sm text-muted-foreground">Aucune demande reçue.</p>
                ) : (
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Étudiant</TableHead>
                        <TableHead>Email</TableHead>
                        <TableHead>Soumis le</TableHead>
                        <TableHead>Valable jusqu'au</TableHead>
                        <TableHead>Statut</TableHead>
                        <TableHead>Dossier</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {[...verifications]
                        .sort((a, b) => (a.statut === "EN_ATTENTE" ? -1 : b.statut === "EN_ATTENTE" ? 1 : 0))
                        .map((v) => {
                          const cfg = STATUT_VERIF_CONFIG[v.statut] ?? { label: v.statut, className: "" };
                          return (
                            <TableRow key={v.id}>
                              <TableCell className="font-medium">{v.nomUtilisateur}</TableCell>
                              <TableCell className="text-muted-foreground">{v.emailUtilisateur}</TableCell>
                              <TableCell>{new Date(v.dateSoumission).toLocaleDateString("fr-BE")}</TableCell>
                              <TableCell>
                                {v.valableJusquA
                                  ? new Date(v.valableJusquA).toLocaleDateString("fr-BE")
                                  : "—"}
                              </TableCell>
                              <TableCell>
                                <Badge className={cfg.className}>{cfg.label}</Badge>
                              </TableCell>
                              <TableCell>
                                <Button variant="outline" size="sm" onClick={() => setSelectedVerif(v)}>
                                  <Eye className="h-4 w-4 mr-1" /> Voir
                                </Button>
                              </TableCell>
                            </TableRow>
                          );
                        })}
                    </TableBody>
                  </Table>
                )}
              </Card>

              {/* Modale de détail */}
              <Dialog open={!!selectedVerif} onOpenChange={(open) => { if (!open) { setSelectedVerif(null); setMotifRefus(""); } }}>
                <DialogContent className="max-w-2xl">
                  <DialogHeader>
                    <DialogTitle className="flex items-center gap-2">
                      <GraduationCap className="h-5 w-5 text-primary" />
                      {selectedVerif?.nomUtilisateur}
                    </DialogTitle>
                  </DialogHeader>

                  {selectedVerif && (
                    <div className="space-y-4">
                      <div className="flex items-center justify-between text-sm text-muted-foreground">
                        <span>{selectedVerif.emailUtilisateur}</span>
                        <Badge className={STATUT_VERIF_CONFIG[selectedVerif.statut]?.className ?? ""}>
                          {STATUT_VERIF_CONFIG[selectedVerif.statut]?.label ?? selectedVerif.statut}
                        </Badge>
                      </div>

                      <div className="grid grid-cols-2 gap-4">
                        <AuthImage
                          url={adminService.getImageUrl(selectedVerif.id, "etudiante")}
                          alt="Carte étudiant"
                          token={token!}
                        />
                        <AuthImage
                          url={adminService.getImageUrl(selectedVerif.id, "identite")}
                          alt="Carte d'identité"
                          token={token!}
                        />
                      </div>

                      {selectedVerif.motifRefus && selectedVerif.statut !== "EN_ATTENTE" && (
                        <div className="p-3 rounded-lg bg-destructive/10 border border-destructive/20 text-sm">
                          <p className="font-medium text-destructive mb-1">Motif du refus précédent</p>
                          <p className="text-muted-foreground">{selectedVerif.motifRefus}</p>
                        </div>
                      )}

                      {selectedVerif.statut === "EN_ATTENTE" && (
                        <div className="space-y-3 pt-2 border-t border-border">
                          <textarea
                            className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm resize-none focus:outline-none focus:ring-2 focus:ring-ring"
                            rows={3}
                            placeholder="Motif du refus (optionnel) — visible par l'étudiant"
                            value={motifRefus}
                            onChange={(e) => setMotifRefus(e.target.value)}
                          />
                          <div className="flex gap-2">
                            <Button
                              className="flex-1"
                              onClick={async () => {
                                await handleValider(selectedVerif.id);
                                setSelectedVerif((prev) => prev ? { ...prev, statut: "ACCEPTE" } : null);
                                setMotifRefus("");
                              }}
                            >
                              <CheckCircle2 className="h-4 w-4 mr-1" /> Accepter
                            </Button>
                            <Button
                              className="flex-1"
                              variant="destructive"
                              disabled={!motifRefus.trim()}
                              onClick={async () => {
                                await handleRefuser(selectedVerif.id, motifRefus);
                                setSelectedVerif((prev) => prev ? { ...prev, statut: "REFUSE", motifRefus } : null);
                                setMotifRefus("");
                              }}
                            >
                              <XCircle className="h-4 w-4 mr-1" /> Refuser
                            </Button>
                          </div>
                        </div>
                      )}
                    </div>
                  )}
                </DialogContent>
              </Dialog>
            </TabsContent>
          </Tabs>
        </div>
      </main>
    </div>
  );
};

export default DashboardAdmin;
