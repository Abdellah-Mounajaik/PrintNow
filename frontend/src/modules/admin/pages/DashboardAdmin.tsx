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
  DialogDescription,
  DialogFooter,
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
  Loader2,
  ShoppingCart,
  GraduationCap,
  XCircle,
  Download,
  Search,
  Trash2,
  AlertTriangle,
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

// ─── Pagination de la liste des imprimeries ──────────────────────────────────
const SHOPS_PER_PAGE = 9;

// ─── Pagination de la liste des utilisateurs ─────────────────────────────────
const USERS_PER_PAGE = 9;

// ─── Pagination de la liste des commandes ────────────────────────────────────
const COMMANDES_PER_PAGE = 9;

const DashboardAdmin = () => {
  const { token } = useAuth();
  const [users, setUsers] = useState<UserDTO[]>([]);
  const [imprimeries, setImprimeries] = useState<ImprimerieDTO[]>([]);
  const [shopSearchQuery, setShopSearchQuery] = useState("");
  const [shopsPage, setShopsPage] = useState(1);
  const [userSearchQuery, setUserSearchQuery] = useState("");
  // Filtre par rôle : « Utilisateurs » gère TOUS les comptes (clients, imprimeurs,
  // admins), car c'est le seul endroit où l'on peut les supprimer. Ce filtre
  // permet d'isoler un rôle à la demande — par ex. ne voir que les clients —
  // sans perdre l'accès aux autres.
  const [userRoleFilter, setUserRoleFilter] = useState<"TOUS" | "CLIENT" | "IMPRIMERIE">("TOUS");
  const [usersPage, setUsersPage] = useState(1);
  const [userASupprimer, setUserASupprimer] = useState<UserDTO | null>(null);
  const [suppressionEnCours, setSuppressionEnCours] = useState(false);
  const [imprimerieAFermer, setImprimerieAFermer] = useState<ImprimerieDTO | null>(null);
  const [fermetureEnCours, setFermetureEnCours] = useState(false);

  const handleFermerImprimerie = async () => {
    if (!token || !imprimerieAFermer) return;
    setFermetureEnCours(true);
    try {
      await adminService.fermerImprimerie(imprimerieAFermer.id, token);
      setImprimeries((prev) => prev.map((i) => (i.id === imprimerieAFermer.id ? { ...i, actif: false } : i)));
      toast({
        title: "Imprimerie fermée",
        description: "Elle ne figure plus au catalogue et ne recevra plus de commandes.",
      });
      setImprimerieAFermer(null);
    } catch (err) {
      toast({
        title: "Fermeture impossible",
        description: err instanceof Error ? err.message : "Réessayez dans un instant.",
        variant: "destructive",
      });
    } finally {
      setFermetureEnCours(false);
    }
  };
  const [commandeSearchQuery, setCommandeSearchQuery] = useState("");
  const [commandesPage, setCommandesPage] = useState(1);
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

  const handleSupprimerUtilisateur = async () => {
    if (!token || !userASupprimer) return;
    setSuppressionEnCours(true);
    try {
      await adminService.supprimerUtilisateur(userASupprimer.id, token);
      // La ligne reste affichée, marquée comme supprimée : c'est ce qui permet
      // de comprendre à qui renvoient les commandes « Compte supprimé ».
      setUsers((prev) => prev.map((u) => (u.id === userASupprimer.id
        ? { ...u, prenom: "Compte", nom: "supprimé", email: `supprime-${u.id}@printnow.invalid`,
            telephone: "", actif: false, dateSuppression: new Date().toISOString() }
        : u)));
      toast({
        title: "Compte supprimé",
        description: "Ses données personnelles ont été effacées ; ses commandes restent enregistrées.",
      });
      setUserASupprimer(null);
    } catch (err) {
      // Le serveur explique pourquoi il refuse (commandes en cours, dernier
      // administrateur) : c'est cette phrase-là qui aide, pas un message générique.
      toast({
        title: "Suppression impossible",
        description: err instanceof Error ? err.message : "Réessayez dans un instant.",
        variant: "destructive",
      });
    } finally {
      setSuppressionEnCours(false);
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
  // La vérification orthographique et les designs IA (studio) sont des services de
  // la plateforme : leur montant ne passe pas par l'imprimerie et s'ajoute donc
  // entièrement à ses revenus.
  const correctionsTotal = commandes.reduce((s, c) => s + Number(c.montantCorrections ?? 0), 0);
  const generationsTotal = commandes.reduce((s, c) => s + Number(c.montantGenerations ?? 0), 0);
  const revenuPlateforme = commissionTotale + correctionsTotal + generationsTotal;
  const recentesImprimeries = [...imprimeries].sort((a, b) => b.id - a.id).slice(0, 5);

  const shopSearchNormalized = shopSearchQuery.trim().toLowerCase();
  const filteredImprimeries = shopSearchNormalized
    ? imprimeries.filter((shop) =>
        shop.nom?.toLowerCase().includes(shopSearchNormalized) ||
        shop.ville?.toLowerCase().includes(shopSearchNormalized) ||
        shop.emailContact?.toLowerCase().includes(shopSearchNormalized)
      )
    : imprimeries;

  // La page demandée est ramenée dans les bornes valides si la recherche a
  // réduit le nombre de résultats entre-temps.
  const shopsTotalPages = Math.max(1, Math.ceil(filteredImprimeries.length / SHOPS_PER_PAGE));
  const shopsCurrentPage = Math.min(shopsPage, shopsTotalPages);
  const paginatedImprimeries = filteredImprimeries.slice(
    (shopsCurrentPage - 1) * SHOPS_PER_PAGE,
    shopsCurrentPage * SHOPS_PER_PAGE
  );

  const userSearchNormalized = userSearchQuery.trim().toLowerCase();
  const filteredUsers = users.filter((u) => {
    const correspondRole = userRoleFilter === "TOUS" || u.roleNom === userRoleFilter;
    const correspondRecherche =
      !userSearchNormalized ||
      `${u.prenom ?? ""} ${u.nom ?? ""}`.toLowerCase().includes(userSearchNormalized) ||
      (u.email?.toLowerCase().includes(userSearchNormalized) ?? false);
    return correspondRole && correspondRecherche;
  });

  // La page demandée est ramenée dans les bornes valides si la recherche a
  // réduit le nombre de résultats entre-temps.
  const usersTotalPages = Math.max(1, Math.ceil(filteredUsers.length / USERS_PER_PAGE));
  const usersCurrentPage = Math.min(usersPage, usersTotalPages);
  const paginatedUsers = filteredUsers.slice(
    (usersCurrentPage - 1) * USERS_PER_PAGE,
    usersCurrentPage * USERS_PER_PAGE
  );

  const commandeSearchNormalized = commandeSearchQuery.trim().toLowerCase();
  const filteredCommandes = commandeSearchNormalized
    ? commandes.filter((c) =>
        c.numeroCommande?.toLowerCase().includes(commandeSearchNormalized) ||
        c.nomClient?.toLowerCase().includes(commandeSearchNormalized) ||
        c.nomImprimerie?.toLowerCase().includes(commandeSearchNormalized)
      )
    : commandes;

  // La page demandée est ramenée dans les bornes valides si la recherche a
  // réduit le nombre de résultats entre-temps.
  const commandesTotalPages = Math.max(1, Math.ceil(filteredCommandes.length / COMMANDES_PER_PAGE));
  const commandesCurrentPage = Math.min(commandesPage, commandesTotalPages);
  const paginatedCommandes = filteredCommandes.slice(
    (commandesCurrentPage - 1) * COMMANDES_PER_PAGE,
    commandesCurrentPage * COMMANDES_PER_PAGE
  );

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
              {/* Les comptes supprimés restent listés plus bas, mais ne comptent
                  pas ici : ce chiffre doit dire combien de personnes utilisent
                  PrintNow, pas combien de lignes existent en base. */}
              <div className="font-display text-3xl font-bold">
                {users.filter((u) => !u.dateSuppression).length}
              </div>
              <div className="text-sm text-muted-foreground">Utilisateurs</div>
              {users.some((u) => u.dateSuppression) && (
                <div className="text-xs text-muted-foreground mt-1">
                  {users.filter((u) => u.dateSuppression).length} compte(s) supprimé(s)
                </div>
              )}
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
              {correctionsTotal > 0 && (
                <div className="text-xs text-success">
                  Corrections : {correctionsTotal.toFixed(2)}€
                </div>
              )}
              {generationsTotal > 0 && (
                <div className="text-xs text-success">
                  Designs IA : {generationsTotal.toFixed(2)}€
                </div>
              )}
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
                        onChange={(e) => { setShopSearchQuery(e.target.value); setShopsPage(1); }}
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
                      {paginatedImprimeries.map((shop) => (
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
                            <div className="flex items-center gap-1">
                              {/* Une boutique déjà fermée n'a plus rien à fermer. */}
                              {shop.actif && (
                                <Button
                                  variant="ghost"
                                  size="sm"
                                  aria-label={`Fermer l'imprimerie ${shop.nom}`}
                                  className="text-destructive hover:text-destructive hover:bg-destructive/10"
                                  onClick={() => setImprimerieAFermer(shop)}
                                >
                                  <Store className="h-4 w-4" />
                                </Button>
                              )}
                            </div>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                )}

                {shopsTotalPages > 1 && (
                  <div className="flex items-center justify-center gap-2 mt-6">
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={shopsCurrentPage === 1}
                      onClick={() => setShopsPage(shopsCurrentPage - 1)}
                    >
                      Précédent
                    </Button>
                    {Array.from({ length: shopsTotalPages }, (_, i) => i + 1).map((n) => (
                      <Button
                        key={n}
                        variant={n === shopsCurrentPage ? "default" : "outline"}
                        size="sm"
                        className="w-9"
                        onClick={() => setShopsPage(n)}
                      >
                        {n}
                      </Button>
                    ))}
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={shopsCurrentPage === shopsTotalPages}
                      onClick={() => setShopsPage(shopsCurrentPage + 1)}
                    >
                      Suivant
                    </Button>
                  </div>
                )}
              </Card>
            </TabsContent>

            {/* Utilisateurs */}
            <TabsContent value="users">
              <Card className="p-6">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4">
                  <h3 className="font-display font-semibold text-lg">
                    Gestion des utilisateurs
                  </h3>
                  {users.length > 0 && (
                    <div className="relative sm:w-80">
                      <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                      <Input
                        type="text"
                        placeholder="Rechercher par nom ou email..."
                        value={userSearchQuery}
                        onChange={(e) => { setUserSearchQuery(e.target.value); setUsersPage(1); }}
                        className="pl-9"
                      />
                    </div>
                  )}
                </div>
                {users.length > 0 && (
                  <div className="flex flex-wrap gap-2 mb-4">
                    {([
                      { value: "TOUS", label: "Tous" },
                      { value: "CLIENT", label: "Clients" },
                      { value: "IMPRIMERIE", label: "Imprimeurs" },
                    ] as const).map((filtre) => (
                      <Button
                        key={filtre.value}
                        variant={userRoleFilter === filtre.value ? "default" : "outline"}
                        size="sm"
                        onClick={() => { setUserRoleFilter(filtre.value); setUsersPage(1); }}
                      >
                        {filtre.label}
                      </Button>
                    ))}
                  </div>
                )}
                {users.length === 0 ? (
                  <p className="text-sm text-muted-foreground">Aucun utilisateur.</p>
                ) : filteredUsers.length === 0 ? (
                  <div className="text-center py-16 border-2 border-dashed rounded-lg bg-muted/20">
                    <Search className="h-12 w-12 mx-auto text-muted-foreground mb-4 opacity-50" />
                    <h4 className="text-lg font-semibold mb-1">Aucun résultat</h4>
                    <p className="text-muted-foreground text-sm">
                      {userSearchNormalized
                        ? `Aucun utilisateur ne correspond à "${userSearchQuery}".`
                        : "Aucun utilisateur pour ce rôle."}
                    </p>
                  </div>
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
                      {paginatedUsers.map((user) => (
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
                            {/* Supprimé passe avant actif/inactif : c'est l'état qui
                                explique pourquoi le compte ne sert plus à rien. */}
                            {user.dateSuppression ? (
                              <Badge variant="outline" className="text-destructive border-destructive/30">
                                Supprimé le {new Date(user.dateSuppression).toLocaleDateString("fr-BE")}
                              </Badge>
                            ) : (
                              <Badge className={user.actif ? "status-open" : "status-closed"}>
                                {user.actif ? "Actif" : "Inactif"}
                              </Badge>
                            )}
                          </TableCell>
                          <TableCell>
                            <div className="flex items-center gap-1">
                              {!user.dateSuppression && (
                                <Button
                                  variant="ghost"
                                  size="sm"
                                  aria-label={`Supprimer le compte de ${user.prenom} ${user.nom}`}
                                  className="text-destructive hover:text-destructive hover:bg-destructive/10"
                                  onClick={() => setUserASupprimer(user)}
                                >
                                  <Trash2 className="h-4 w-4" />
                                </Button>
                              )}
                            </div>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                )}

                {usersTotalPages > 1 && (
                  <div className="flex items-center justify-center gap-2 mt-6">
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={usersCurrentPage === 1}
                      onClick={() => setUsersPage(usersCurrentPage - 1)}
                    >
                      Précédent
                    </Button>
                    {Array.from({ length: usersTotalPages }, (_, i) => i + 1).map((n) => (
                      <Button
                        key={n}
                        variant={n === usersCurrentPage ? "default" : "outline"}
                        size="sm"
                        className="w-9"
                        onClick={() => setUsersPage(n)}
                      >
                        {n}
                      </Button>
                    ))}
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={usersCurrentPage === usersTotalPages}
                      onClick={() => setUsersPage(usersCurrentPage + 1)}
                    >
                      Suivant
                    </Button>
                  </div>
                )}
              </Card>
            </TabsContent>

            {/* Commandes */}
            <TabsContent value="commandes">
              <Card className="p-6">
                <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-3 mb-4">
                  <h3 className="font-display font-semibold text-lg">
                    Toutes les commandes
                  </h3>
                  <div className="flex flex-col sm:flex-row sm:items-center gap-3">
                    {commandes.length > 0 && (
                      <div className="relative sm:w-80">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                        <Input
                          type="text"
                          placeholder="Rechercher par n°, client ou imprimerie..."
                          value={commandeSearchQuery}
                          onChange={(e) => { setCommandeSearchQuery(e.target.value); setCommandesPage(1); }}
                          className="pl-9"
                        />
                      </div>
                    )}
                    <div className="text-sm text-muted-foreground whitespace-nowrap">
                      Commission totale :{" "}
                      <span className="font-semibold text-success">
                        {commissionTotale.toFixed(2)}€
                      </span>
                    </div>
                  </div>
                </div>
                {commandes.length === 0 ? (
                  <p className="text-sm text-muted-foreground">Aucune commande.</p>
                ) : filteredCommandes.length === 0 ? (
                  <div className="text-center py-16 border-2 border-dashed rounded-lg bg-muted/20">
                    <Search className="h-12 w-12 mx-auto text-muted-foreground mb-4 opacity-50" />
                    <h4 className="text-lg font-semibold mb-1">Aucun résultat</h4>
                    <p className="text-muted-foreground text-sm">Aucune commande ne correspond à "{commandeSearchQuery}".</p>
                  </div>
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
                      {paginatedCommandes.map((c) => (
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
                                aria-label={`Relevé des revenus de la commande ${c.numeroCommande}`}
                                title="Relevé des revenus PrintNow"
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

                {commandesTotalPages > 1 && (
                  <div className="flex items-center justify-center gap-2 mt-6">
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={commandesCurrentPage === 1}
                      onClick={() => setCommandesPage(commandesCurrentPage - 1)}
                    >
                      Précédent
                    </Button>
                    {Array.from({ length: commandesTotalPages }, (_, i) => i + 1).map((n) => (
                      <Button
                        key={n}
                        variant={n === commandesCurrentPage ? "default" : "outline"}
                        size="sm"
                        className="w-9"
                        onClick={() => setCommandesPage(n)}
                      >
                        {n}
                      </Button>
                    ))}
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={commandesCurrentPage === commandesTotalPages}
                      onClick={() => setCommandesPage(commandesCurrentPage + 1)}
                    >
                      Suivant
                    </Button>
                  </div>
                )}
                {commandes.length > 0 && (
                  <div className="mt-6 p-4 rounded-lg bg-success/5 border border-success/20 flex items-center justify-between">
                    <div>
                      <div className="text-sm text-muted-foreground">CA total plateforme</div>
                      <div className="font-display text-2xl font-bold">{caTotal.toFixed(2)}€</div>
                    </div>
                    <div className="text-right">
                      <div className="text-sm text-muted-foreground">
                        {(correctionsTotal > 0 || generationsTotal > 0) ? "Revenus PrintNow" : "Commissions totales"}
                      </div>
                      <div className="font-display text-2xl font-bold text-success">
                        {revenuPlateforme.toFixed(2)}€
                      </div>
                      {correctionsTotal > 0 && (
                        <div className="text-xs text-muted-foreground">
                          dont {correctionsTotal.toFixed(2)}€ de corrections
                        </div>
                      )}
                      {generationsTotal > 0 && (
                        <div className="text-xs text-muted-foreground">
                          dont {generationsTotal.toFixed(2)}€ de designs IA
                        </div>
                      )}
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

      {/* Confirmation de suppression d'un compte : l'action est irréversible,
          elle ne doit pas tenir à un clic malheureux sur une corbeille. */}
      <Dialog open={!!userASupprimer} onOpenChange={(open) => { if (!open) setUserASupprimer(null); }}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <div className="mx-auto w-14 h-14 rounded-full bg-destructive/10 flex items-center justify-center mb-2">
              <AlertTriangle className="h-7 w-7 text-destructive" />
            </div>
            <DialogTitle className="text-center">Supprimer ce compte ?</DialogTitle>
            <DialogDescription className="text-center">
              {userASupprimer?.prenom} {userASupprimer?.nom} — {userASupprimer?.email}
            </DialogDescription>
          </DialogHeader>

          <div className="text-sm text-muted-foreground space-y-2">
            <p>Son nom, son email et son téléphone seront définitivement effacés.</p>
            <p>
              Ses commandes et factures sont conservées, comme la loi l'impose, mais ne
              porteront plus son nom. S'il gère une imprimerie, celle-ci sera fermée.
            </p>
            <p className="font-medium text-foreground">Cette action est irréversible.</p>
          </div>

          <DialogFooter className="gap-2 sm:gap-0">
            <Button variant="outline" onClick={() => setUserASupprimer(null)} disabled={suppressionEnCours}>
              Annuler
            </Button>
            <Button variant="destructive" onClick={handleSupprimerUtilisateur} disabled={suppressionEnCours}>
              {suppressionEnCours ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Suppression...
                </>
              ) : (
                <>
                  <Trash2 className="h-4 w-4 mr-2" />
                  Supprimer définitivement
                </>
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Fermeture d'une imprimerie : le commerce disparaît du catalogue, ça ne
          doit pas tenir à un clic malheureux. */}
      <Dialog open={!!imprimerieAFermer} onOpenChange={(open) => { if (!open) setImprimerieAFermer(null); }}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <div className="mx-auto w-14 h-14 rounded-full bg-destructive/10 flex items-center justify-center mb-2">
              <AlertTriangle className="h-7 w-7 text-destructive" />
            </div>
            <DialogTitle className="text-center">Fermer cette imprimerie ?</DialogTitle>
            <DialogDescription className="text-center">
              {imprimerieAFermer?.nom}{imprimerieAFermer?.ville ? ` — ${imprimerieAFermer.ville}` : ""}
            </DialogDescription>
          </DialogHeader>

          <div className="text-sm text-muted-foreground space-y-2">
            <p>Elle disparaîtra du catalogue et ne recevra plus aucune commande.</p>
            <p>
              Ses commandes en cours et ses factures restent intactes, et son gérant
              conserve l'accès à son espace pour les honorer.
            </p>
            <p>Vous pourrez la rouvrir en la réactivant.</p>
          </div>

          <DialogFooter className="gap-2 sm:gap-0">
            <Button variant="outline" onClick={() => setImprimerieAFermer(null)} disabled={fermetureEnCours}>
              Annuler
            </Button>
            <Button variant="destructive" onClick={handleFermerImprimerie} disabled={fermetureEnCours}>
              {fermetureEnCours ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Fermeture...
                </>
              ) : (
                <>
                  <Store className="h-4 w-4 mr-2" />
                  Fermer l'imprimerie
                </>
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default DashboardAdmin;
