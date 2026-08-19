import { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import Header from "../../../components/layout/Header";
import { Button } from "../../../components/ui/button";
import { Card } from "../../../components/ui/card";
import { Badge } from "../../../components/ui/badge";
import { Input } from "../../../components/ui/input";
import { Label } from "../../../components/ui/label";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../../../components/ui/tabs";
import {
  Package,
  FileText,
  Clock,
  Euro,
  TrendingUp,
  User,
  Mail,
  Phone,
  Lock,
  GraduationCap,
  Upload,
  CheckCircle2,
  AlertCircle,
  Clock3,
  Loader2,
  Truck,
  ExternalLink,
  Download,
  Search,
  Trash2,
  AlertTriangle,
} from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "../../../components/ui/dialog";
import { toast } from "../../../hooks/use-toast";
import { getPaginationRange } from "../../../lib/utils";
import { useRef } from "react";
import { useAuth } from "../../auth/context/AuthContext";
import { userService } from "../services/user.service";
import type { CommandeDTO, VerifDTO, SuiviDTO, UserProfileDTO } from "../models/user.model";

// Une facture n'existe que pour une commande dont le paiement a bien été
// confirmé (même condition que côté backend, FactureService.STATUTS_FACTURABLES).
const STATUTS_FACTURABLES = new Set(["PAYEE", "EN_COURS_IMPRESSION", "PRETE", "LIVREE"]);

// ─── Pagination de la liste des commandes ────────────────────────────────────
const ORDERS_PER_PAGE = 6;

/**
 * Montant réellement débité : l'impression, plus la vérification orthographique
 * lorsqu'elle a été demandée. Cette dernière est vendue par PrintNow et ne
 * figure donc pas dans le total de la commande, qui ne concerne que l'imprimerie.
 */
const montantPaye = (commande: CommandeDTO) =>
  Number(commande.totalTTC ?? 0) + Number(commande.montantCorrections ?? 0) + Number(commande.montantGenerations ?? 0);

const DashboardClient = () => {
  const { t } = useTranslation("dashboardClient");
  const { user, token, logoutGlobal } = useAuth();
  const navigate = useNavigate();

  const STATUS_MAP_RETRAIT: Record<string, { label: string; variant: "default"|"secondary"|"destructive"|"outline" }> = {
    EN_ATTENTE_PAIEMENT: { label: t("orders.status.retrait.enAttentePaiement"), variant: "outline" },
    PAYEE: { label: t("orders.status.retrait.payee"), variant: "secondary" },
    EN_COURS_IMPRESSION: { label: t("orders.status.retrait.enCoursImpression"), variant: "default" },
    PRETE: { label: t("orders.status.retrait.prete"), variant: "default" },
    LIVREE: { label: t("orders.status.retrait.livree"), variant: "default" },
    ANNULEE: { label: t("orders.status.retrait.annulee"), variant: "destructive" },
  };

  const STATUS_MAP_LIVRAISON: Record<string, { label: string; variant: "default"|"secondary"|"destructive"|"outline" }> = {
    EN_ATTENTE_PAIEMENT: { label: t("orders.status.livraison.enAttentePaiement"), variant: "outline" },
    PAYEE: { label: t("orders.status.livraison.payee"), variant: "secondary" },
    EN_COURS_IMPRESSION: { label: t("orders.status.livraison.enCoursImpression"), variant: "default" },
    PRETE: { label: t("orders.status.livraison.prete"), variant: "default" },
    LIVREE: { label: t("orders.status.livraison.livree"), variant: "default" },
    ANNULEE: { label: t("orders.status.livraison.annulee"), variant: "destructive" },
  };

  // ─── Filtre par statut de la liste des commandes (même logique que la stat "En cours") ──
  const ORDER_STATUS_FILTERS: { key: string; label: string; statuts: string[] | null }[] = [
    { key: "ALL", label: t("orders.filters.all"), statuts: null },
    { key: "EN_COURS", label: t("orders.filters.inProgress"), statuts: ["PAYEE", "EN_COURS_IMPRESSION"] },
  ];
  const [orders, setOrders] = useState<CommandeDTO[]>([]);
  const [orderStatusFilter, setOrderStatusFilter] = useState<string>("ALL");
  const [ordersPage, setOrdersPage] = useState(1);
  const [invoicesPage, setInvoicesPage] = useState(1);
  const [invoiceSearchQuery, setInvoiceSearchQuery] = useState("");
  const [suiviData, setSuiviData] = useState<Record<number, SuiviDTO>>({});
  const [suiviLoading, setSuiviLoading] = useState<number | null>(null);
  const [verif, setVerif] = useState<VerifDTO | null>(null);
  const [verifLoaded, setVerifLoaded] = useState(false);
  const [uploading, setUploading] = useState(false);
  const carteEtudianteRef = useRef<HTMLInputElement>(null);
  const carteIdentiteRef = useRef<HTMLInputElement>(null);

  // --- Profil ---
  const [profil, setProfil] = useState<UserProfileDTO | null>(null);
  const [profileForm, setProfileForm] = useState({ prenom: "", nom: "", email: "", telephone: "" });
  const [savingProfile, setSavingProfile] = useState(false);
  const [passwordForm, setPasswordForm] = useState({ ancien: "", nouveau: "", confirmation: "" });
  const [savingPassword, setSavingPassword] = useState(false);
  const [confirmationSuppression, setConfirmationSuppression] = useState(false);
  const [suppressionEnCours, setSuppressionEnCours] = useState(false);
  const [compteSupprime, setCompteSupprime] = useState(false);

  useEffect(() => {
    if (!token) return;
    userService.getMonProfil(token)
      .then((data) => {
        setProfil(data);
        setProfileForm({
          prenom: data.prenom ?? "",
          nom: data.nom ?? "",
          email: data.email ?? "",
          telephone: data.telephone ?? "",
        });
      })
      .catch(() => setProfil(null));
  }, [token]);

  const handleSaveProfile = async () => {
    if (!token) return;
    setSavingProfile(true);
    try {
      const emailChanged = profil?.email !== profileForm.email;
      const updated = await userService.updateMonProfil(profileForm, token);
      setProfil(updated);
      if (emailChanged) {
        toast({ title: t("toasts.profileUpdated.title"), description: t("toasts.profileUpdated.emailChangedDescription") });
        logoutGlobal();
        navigate("/login?tab=connexion");
        return;
      }
      toast({ title: t("toasts.profileUpdated.title") });
    } catch (e) {
      toast({ title: t("toasts.error.title"), description: (e as Error).message, variant: "destructive" });
    } finally {
      setSavingProfile(false);
    }
  };

  const handleChangePassword = async () => {
    if (!token) return;
    if (passwordForm.nouveau !== passwordForm.confirmation) {
      toast({ title: t("toasts.error.title"), description: t("toasts.passwordMismatch"), variant: "destructive" });
      return;
    }
    setSavingPassword(true);
    try {
      await userService.changerMotDePasse(passwordForm.ancien, passwordForm.nouveau, token);
      setPasswordForm({ ancien: "", nouveau: "", confirmation: "" });
      toast({ title: t("toasts.passwordChanged.title") });
    } catch (e) {
      toast({ title: t("toasts.error.title"), description: (e as Error).message, variant: "destructive" });
    } finally {
      setSavingPassword(false);
    }
  };

  const handleSupprimerMonCompte = async () => {
    if (!token) return;
    setSuppressionEnCours(true);
    try {
      await userService.supprimerMonCompte(token);
      // La confirmation reste dans la fenêtre plutôt que dans une alerte : la
      // session doit être fermée juste après, et une alerte ne survivrait pas
      // au rechargement.
      setCompteSupprime(true);
    } catch (e) {
      toast({ title: t("toasts.deleteError.title"), description: (e as Error).message, variant: "destructive" });
    } finally {
      setSuppressionEnCours(false);
    }
  };

  /**
   * Ferme la session et repart de l'accueil.
   *
   * Un rechargement complet plutôt qu'une navigation interne : il ne reste ainsi
   * aucune donnée du compte supprimé en mémoire, et la route protégée n'a pas
   * le temps de renvoyer vers /login.
   */
  const quitterApresSuppression = () => {
    logoutGlobal();
    window.location.replace("/");
  };

  useEffect(() => {
    if (!token) return;

    userService.getMesCommandes(token)
      .then(setOrders)
      .catch(() => setOrders([]));

    userService.getMaVerification(token)
      .then((data) => setVerif(data))
      .catch(() => setVerif(null))
      .finally(() => setVerifLoaded(true));
  }, [token]);

  const [downloadingInvoiceId, setDownloadingInvoiceId] = useState<number | null>(null);
  const handleTelechargerFacture = async (commandeId: number, numeroCommande: string) => {
    if (!token) return;
    setDownloadingInvoiceId(commandeId);
    try {
      await userService.telechargerFacture(commandeId, numeroCommande, token);
    } catch (e) {
      toast({ title: t("toasts.error.title"), description: (e as Error).message, variant: "destructive" });
    } finally {
      setDownloadingInvoiceId(null);
    }
  };

  const handleFetchSuivi = async (orderId: number) => {
    if (!token) return;
    setSuiviLoading(orderId);
    try {
      const data = await userService.getSuiviLivraison(orderId, token);
      setSuiviData(prev => ({ ...prev, [orderId]: data }));
    } catch {
      toast({ title: t("toasts.error.title"), description: t("toasts.trackingError.description"), variant: "destructive" });
    } finally {
      setSuiviLoading(null);
    }
  };

  const handleSoumettre = async () => {
    const carteEt = carteEtudianteRef.current?.files?.[0];
    const carteId = carteIdentiteRef.current?.files?.[0];
    if (!carteEt || !carteId) {
      toast({ title: t("toasts.selectDocuments.title"), variant: "destructive" });
      return;
    }
    if (!token) return;
    setUploading(true);
    try {
      const data = await userService.soumettreVerification(carteEt, carteId, token);
      setVerif(data);
      toast({ title: t("toasts.verificationSent.title"), description: t("toasts.verificationSent.description") });
    } catch (e) {
      toast({ title: t("toasts.error.title"), description: (e as Error).message, variant: "destructive" });
    } finally {
      setUploading(false);
    }
  };

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
              {t("greeting", { name: displayName })}
            </h1>
            <p className="text-muted-foreground">
              {t("subtitle")}
            </p>
          </div>

          {/* Stats */}
          {(() => {
            const now = new Date();
            const enCours = orders.filter(o => o.statut === "PAYEE" || o.statut === "EN_COURS_IMPRESSION").length;
            const totalDepense = orders.reduce((s, o) => s + montantPaye(o), 0);
            const cesMois = orders.filter(o => {
              const d = new Date(o.dateCreation);
              return d.getMonth() === now.getMonth() && d.getFullYear() === now.getFullYear();
            }).length;
            return (
              <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
                <Card className="p-6">
                  <div className="flex items-center justify-between mb-2">
                    <Package className="h-8 w-8 text-primary" />
                    <Badge variant="outline">{t("stats.totalBadge")}</Badge>
                  </div>
                  <div className="font-display text-3xl font-bold">{orders.length}</div>
                  <div className="text-sm text-muted-foreground">{t("stats.ordersLabel")}</div>
                </Card>
                <Card className="p-6">
                  <div className="flex items-center justify-between mb-2">
                    <Clock className="h-8 w-8 text-warning" />
                    <Badge variant="outline">{t("stats.activeBadge")}</Badge>
                  </div>
                  <div className="font-display text-3xl font-bold">{enCours}</div>
                  <div className="text-sm text-muted-foreground">{t("stats.inProgressLabel")}</div>
                </Card>
                <Card className="p-6">
                  <div className="flex items-center justify-between mb-2">
                    <Euro className="h-8 w-8 text-success" />
                    <Badge variant="outline">{t("stats.totalBadge")}</Badge>
                  </div>
                  <div className="font-display text-3xl font-bold">{totalDepense.toFixed(2)}€</div>
                  <div className="text-sm text-muted-foreground">{t("stats.spentLabel")}</div>
                </Card>
                <Card className="p-6">
                  <div className="flex items-center justify-between mb-2">
                    <TrendingUp className="h-8 w-8 text-info" />
                    <Badge variant="outline">{t("stats.thisMonthBadge")}</Badge>
                  </div>
                  <div className="font-display text-3xl font-bold">{cesMois}</div>
                  <div className="text-sm text-muted-foreground">{t("stats.ordersLabel")}</div>
                </Card>
              </div>
            );
          })()}

          <Tabs defaultValue="orders" className="space-y-6">
            <TabsList>
              <TabsTrigger value="orders">{t("tabs.orders")}</TabsTrigger>
              <TabsTrigger value="invoices">{t("tabs.invoices")}</TabsTrigger>
              <TabsTrigger value="etudiant">{t("tabs.student")}</TabsTrigger>
              <TabsTrigger value="profile">{t("tabs.profile")}</TabsTrigger>
            </TabsList>

            {/* COMMANDES */}
            <TabsContent value="orders">
              <Card className="p-6">
                <h3 className="font-display font-semibold text-lg mb-4">{t("orders.title")}</h3>
                {orders.length > 0 && (
                  <div className="flex flex-wrap gap-2 mb-4">
                    {ORDER_STATUS_FILTERS.map((f) => (
                      <Button
                        key={f.key}
                        variant={orderStatusFilter === f.key ? "default" : "outline"}
                        size="sm"
                        onClick={() => { setOrderStatusFilter(f.key); setOrdersPage(1); }}
                      >
                        {f.label}
                      </Button>
                    ))}
                  </div>
                )}
                {(() => {
                  const activeStatusFilter = ORDER_STATUS_FILTERS.find((f) => f.key === orderStatusFilter) ?? ORDER_STATUS_FILTERS[0];
                  const filteredOrders = activeStatusFilter.statuts
                    ? orders.filter((o: any) => activeStatusFilter.statuts!.includes(o.statut))
                    : orders;

                  // La page demandée est ramenée dans les bornes valides si le
                  // filtre a réduit le nombre de résultats entre-temps.
                  const totalPages = Math.max(1, Math.ceil(filteredOrders.length / ORDERS_PER_PAGE));
                  const currentPage = Math.min(ordersPage, totalPages);
                  const paginatedOrders = filteredOrders.slice(
                    (currentPage - 1) * ORDERS_PER_PAGE,
                    currentPage * ORDERS_PER_PAGE
                  );

                  if (orders.length === 0) {
                    return (
                      <div className="text-center py-16 border-2 border-dashed rounded-lg bg-muted/20">
                        <Package className="h-12 w-12 mx-auto text-muted-foreground mb-4 opacity-50" />
                        <h4 className="text-lg font-semibold mb-1">{t("orders.empty.title")}</h4>
                        <p className="text-muted-foreground text-sm mb-4">
                          {t("orders.empty.description")}
                        </p>
                        <Button asChild>
                          <Link to="/imprimeries">{t("orders.empty.cta")}</Link>
                        </Button>
                      </div>
                    );
                  }

                  if (filteredOrders.length === 0) {
                    return (
                      <div className="text-center py-16 border-2 border-dashed rounded-lg bg-muted/20">
                        <Package className="h-12 w-12 mx-auto text-muted-foreground mb-4 opacity-50" />
                        <h4 className="text-lg font-semibold mb-1">{t("orders.emptyFiltered.title")}</h4>
                        <p className="text-muted-foreground text-sm">{t("orders.emptyFiltered.description", { filter: activeStatusFilter.label })}</p>
                      </div>
                    );
                  }

                  return (
                  <>
                  <div className="space-y-3">
                    {paginatedOrders.map((order: any) => {
                      const isLivraison = order.modeRetrait === "LIVRAISON";
                      const status = (isLivraison ? STATUS_MAP_LIVRAISON : STATUS_MAP_RETRAIT)[order.statut] ?? { label: order.statut, variant: "outline" };
                      const suivi = suiviData[order.id];
                      return (
                        <div key={order.id} className="border rounded-lg bg-muted/10 overflow-hidden">
                          <div className="flex items-center justify-between p-4 gap-4">
                            <div className="min-w-0">
                              <p className="font-mono font-medium text-sm">{order.numeroCommande}</p>
                              <p className="text-xs text-muted-foreground mt-0.5 flex items-center gap-1.5">
                                {order.nomImprimerie} · {new Date(order.dateCreation).toLocaleDateString("fr-BE")}
                                {isLivraison && <span className="inline-flex items-center gap-1"><Truck className="h-3 w-3" /> {t("orders.delivery")}</span>}
                              </p>
                            </div>
                            <div className="flex items-center gap-3 shrink-0">
                              <Badge variant={status.variant as any}>{status.label}</Badge>
                              <span className="font-semibold text-primary">{montantPaye(order).toFixed(2)}€</span>
                              {isLivraison && (
                                <Button
                                  variant="outline"
                                  size="sm"
                                  onClick={() => handleFetchSuivi(order.id)}
                                  disabled={suiviLoading === order.id}
                                >
                                  {suiviLoading === order.id
                                    ? <Loader2 className="h-3.5 w-3.5 animate-spin" />
                                    : <Truck className="h-3.5 w-3.5" />}
                                </Button>
                              )}
                            </div>
                          </div>

                          {suivi && (
                            <div className="px-4 pb-4 pt-0 border-t border-border bg-background">
                              {suivi.numeroSuivi ? (
                                <div className="flex items-center justify-between gap-3 pt-3">
                                  <div>
                                    <p className="text-xs text-muted-foreground">{t("orders.tracking.numberLabel")}</p>
                                    <p className="font-mono text-sm font-medium">{suivi.numeroSuivi}</p>
                                    {suivi.statutAfterShipping && (
                                      <p className="text-xs text-muted-foreground mt-0.5">{t("orders.tracking.statusLabel", { status: suivi.statutAfterShipping })}</p>
                                    )}
                                  </div>
                                  {suivi.lienSuiviBpost && (
                                    <Button size="sm" asChild>
                                      <a href={suivi.lienSuiviBpost} target="_blank" rel="noopener noreferrer">
                                        <ExternalLink className="h-3.5 w-3.5 mr-1" /> {t("orders.tracking.trackButton")}
                                      </a>
                                    </Button>
                                  )}
                                </div>
                              ) : (
                                <p className="pt-3 text-sm text-muted-foreground">{t("orders.tracking.notShippedYet")}</p>
                              )}
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>

                  {totalPages > 1 && (
                    <div className="flex items-center justify-center gap-2 mt-6">
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={currentPage === 1}
                        onClick={() => setOrdersPage(currentPage - 1)}
                      >
                        {t("pagination.previous")}
                      </Button>
                      {getPaginationRange(currentPage, totalPages).map((n, idx) =>
                        n === "..." ? (
                          <span key={`ellipsis-${idx}`} className="px-1 text-muted-foreground">…</span>
                        ) : (
                          <Button
                            key={n}
                            variant={n === currentPage ? "default" : "outline"}
                            size="sm"
                            className="w-9"
                            onClick={() => setOrdersPage(n)}
                          >
                            {n}
                          </Button>
                        )
                      )}
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={currentPage === totalPages}
                        onClick={() => setOrdersPage(currentPage + 1)}
                      >
                        {t("pagination.next")}
                      </Button>
                    </div>
                  )}
                  </>
                  );
                })()}
              </Card>
            </TabsContent>

            {/* FACTURES */}
            <TabsContent value="invoices">
              <Card className="p-6">
                {(() => {
                  const facturables = orders.filter((o) => STATUTS_FACTURABLES.has(o.statut));
                  const searchNormalized = invoiceSearchQuery.trim().toLowerCase();
                  const filteredFactures = searchNormalized
                    ? facturables.filter((o: any) =>
                        o.numeroCommande?.toLowerCase().includes(searchNormalized) ||
                        o.nomImprimerie?.toLowerCase().includes(searchNormalized)
                      )
                    : facturables;

                  // La page demandée est ramenée dans les bornes valides si la
                  // recherche a réduit le nombre de résultats entre-temps.
                  const totalPages = Math.max(1, Math.ceil(filteredFactures.length / ORDERS_PER_PAGE));
                  const currentPage = Math.min(invoicesPage, totalPages);
                  const paginatedFactures = filteredFactures.slice(
                    (currentPage - 1) * ORDERS_PER_PAGE,
                    currentPage * ORDERS_PER_PAGE
                  );

                  return (
                    <>
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4">
                      <h3 className="font-display font-semibold text-lg">{t("invoices.title")}</h3>
                      {facturables.length > 0 && (
                        <div className="relative sm:w-[22rem]">
                          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                          <Input
                            type="text"
                            placeholder={t("invoices.searchPlaceholder")}
                            value={invoiceSearchQuery}
                            onChange={(e) => { setInvoiceSearchQuery(e.target.value); setInvoicesPage(1); }}
                            className="pl-9"
                          />
                        </div>
                      )}
                    </div>

                    {facturables.length === 0 ? (
                      <div className="text-center py-16 border-2 border-dashed rounded-lg bg-muted/20">
                        <FileText className="h-12 w-12 mx-auto text-muted-foreground mb-4 opacity-50" />
                        <h4 className="text-lg font-semibold mb-1">{t("invoices.empty.title")}</h4>
                        <p className="text-muted-foreground text-sm">
                          {t("invoices.empty.description")}
                        </p>
                      </div>
                    ) : filteredFactures.length === 0 ? (
                      <div className="text-center py-16 border-2 border-dashed rounded-lg bg-muted/20">
                        <Search className="h-12 w-12 mx-auto text-muted-foreground mb-4 opacity-50" />
                        <h4 className="text-lg font-semibold mb-1">{t("invoices.emptyFiltered.title")}</h4>
                        <p className="text-muted-foreground text-sm">{t("invoices.emptyFiltered.description", { query: invoiceSearchQuery })}</p>
                      </div>
                    ) : (
                    <>
                    <div className="space-y-3">
                      {paginatedFactures.map((order) => (
                        <div
                          key={order.id}
                          className="flex items-center justify-between p-4 border border-border rounded-lg"
                        >
                          <div className="flex items-center gap-3 min-w-0">
                            <div className="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center shrink-0">
                              <FileText className="h-5 w-5 text-primary" />
                            </div>
                            <div className="min-w-0">
                              <p className="font-semibold truncate">{order.numeroCommande}</p>
                              <p className="text-sm text-muted-foreground truncate">
                                {order.nomImprimerie} · {new Date(order.dateCreation).toLocaleDateString("fr-BE")}
                              </p>
                            </div>
                          </div>
                          <div className="flex items-center gap-4 shrink-0">
                            <span className="font-semibold">{montantPaye(order).toFixed(2)}€</span>
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => handleTelechargerFacture(order.id, order.numeroCommande)}
                              disabled={downloadingInvoiceId === order.id}
                            >
                              {downloadingInvoiceId === order.id ? (
                                <Loader2 className="h-4 w-4 animate-spin mr-2" />
                              ) : (
                                <Download className="h-4 w-4 mr-2" />
                              )}
                              {t("invoices.download")}
                            </Button>
                          </div>
                        </div>
                      ))}
                    </div>

                    {totalPages > 1 && (
                      <div className="flex items-center justify-center gap-2 mt-6">
                        <Button
                          variant="outline"
                          size="sm"
                          disabled={currentPage === 1}
                          onClick={() => setInvoicesPage(currentPage - 1)}
                        >
                          {t("pagination.previous")}
                        </Button>
                        {getPaginationRange(currentPage, totalPages).map((n, idx) =>
                          n === "..." ? (
                            <span key={`ellipsis-${idx}`} className="px-1 text-muted-foreground">…</span>
                          ) : (
                            <Button
                              key={n}
                              variant={n === currentPage ? "default" : "outline"}
                              size="sm"
                              className="w-9"
                              onClick={() => setInvoicesPage(n)}
                            >
                              {n}
                            </Button>
                          )
                        )}
                        <Button
                          variant="outline"
                          size="sm"
                          disabled={currentPage === totalPages}
                          onClick={() => setInvoicesPage(currentPage + 1)}
                        >
                          {t("pagination.next")}
                        </Button>
                      </div>
                    )}
                    </>
                    )}
                    </>
                  );
                })()}
              </Card>
            </TabsContent>

            {/* TARIF ÉTUDIANT */}
            <TabsContent value="etudiant">
              <Card className="p-6">
                <div className="flex items-center gap-3 mb-6">
                  <GraduationCap className="h-6 w-6 text-primary" />
                  <div>
                    <h3 className="font-display font-semibold text-lg">{t("student.title")}</h3>
                    <p className="text-sm text-muted-foreground">
                      {t("student.description")}
                    </p>
                  </div>
                </div>

                {/* Statut actuel */}
                {verifLoaded && verif && (
                  <div className={`flex items-center gap-3 p-4 rounded-lg mb-6 border ${
                    verif.statut === "ACCEPTE" ? "bg-success/5 border-success/20" :
                    verif.statut === "EN_ATTENTE" ? "bg-warning/5 border-warning/20" :
                    "bg-destructive/5 border-destructive/20"
                  }`}>
                    {verif.statut === "ACCEPTE" && <CheckCircle2 className="h-5 w-5 text-success shrink-0" />}
                    {verif.statut === "EN_ATTENTE" && <Clock3 className="h-5 w-5 text-warning shrink-0" />}
                    {(verif.statut === "REFUSE" || verif.statut === "EXPIRE") && <AlertCircle className="h-5 w-5 text-destructive shrink-0" />}
                    <div>
                      {verif.statut === "ACCEPTE" && (
                        <>
                          <p className="font-semibold text-success">{t("student.status.accepted.label")}</p>
                          {verif.valableJusquA && (
                            <p className="text-sm text-muted-foreground">
                              {t("student.status.accepted.validUntil", { date: new Date(verif.valableJusquA).toLocaleDateString("fr-BE") })}
                            </p>
                          )}
                        </>
                      )}
                      {verif.statut === "EN_ATTENTE" && (
                        <>
                          <p className="font-semibold text-warning">{t("student.status.pending.label")}</p>
                          <p className="text-sm text-muted-foreground">
                            {t("student.status.pending.submittedOn", { date: new Date(verif.dateSoumission).toLocaleDateString("fr-BE") })}
                          </p>
                        </>
                      )}
                      {verif.statut === "REFUSE" && (
                        <>
                          <p className="font-semibold text-destructive">{t("student.status.refused.label")}</p>
                          {verif.motifRefus && (
                            <p className="text-sm text-muted-foreground mt-1">{t("student.status.refused.reason", { motif: verif.motifRefus })}</p>
                          )}
                        </>
                      )}
                      {verif.statut === "EXPIRE" && (
                        <p className="font-semibold text-destructive">{t("student.status.expired.label")}</p>
                      )}
                    </div>
                  </div>
                )}

                {/* Formulaire d'envoi */}
                {verifLoaded && (!verif || verif.statut === "REFUSE" || verif.statut === "EXPIRE") && (
                  <div className="space-y-4">
                    <div className="space-y-2">
                      <label className="text-sm font-medium">{t("student.form.studentCardLabel")}</label>
                      <input
                        ref={carteEtudianteRef}
                        type="file"
                        accept="image/*,.pdf"
                        className="block w-full text-sm text-muted-foreground file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-medium file:bg-primary file:text-primary-foreground hover:file:bg-primary/90 cursor-pointer"
                      />
                    </div>
                    <div className="space-y-2">
                      <label className="text-sm font-medium">{t("student.form.idCardLabel")}</label>
                      <input
                        ref={carteIdentiteRef}
                        type="file"
                        accept="image/*,.pdf"
                        className="block w-full text-sm text-muted-foreground file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-medium file:bg-primary file:text-primary-foreground hover:file:bg-primary/90 cursor-pointer"
                      />
                    </div>
                    <Button onClick={handleSoumettre} disabled={uploading}>
                      {uploading ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : <Upload className="h-4 w-4 mr-2" />}
                      {uploading ? t("student.form.submitting") : t("student.form.submit")}
                    </Button>
                  </div>
                )}
              </Card>
            </TabsContent>

            {/* PROFIL */}
            <TabsContent value="profile" className="space-y-6">
              <Card className="p-6">
                <div className="flex items-center gap-4 mb-6">
                  <div className="w-20 h-20 rounded-full bg-primary text-primary-foreground flex items-center justify-center">
                    <User className="h-10 w-10" />
                  </div>
                  <div>
                    <h3 className="font-display font-semibold text-xl">
                      {profil ? `${profil.prenom} ${profil.nom}` : displayName}
                    </h3>
                    <p className="text-muted-foreground text-sm">{user?.email}</p>
                  </div>
                </div>

                <div className="grid md:grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="prenom">{t("profile.firstNameLabel")}</Label>
                    <Input
                      id="prenom"
                      value={profileForm.prenom}
                      onChange={(e) => setProfileForm((f) => ({ ...f, prenom: e.target.value }))}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="nom">{t("profile.lastNameLabel")}</Label>
                    <Input
                      id="nom"
                      value={profileForm.nom}
                      onChange={(e) => setProfileForm((f) => ({ ...f, nom: e.target.value }))}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="email" className="flex items-center gap-1.5">
                      <Mail className="h-3.5 w-3.5" /> {t("profile.emailLabel")}
                    </Label>
                    <Input
                      id="email"
                      type="email"
                      value={profileForm.email}
                      onChange={(e) => setProfileForm((f) => ({ ...f, email: e.target.value }))}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="telephone" className="flex items-center gap-1.5">
                      <Phone className="h-3.5 w-3.5" /> {t("profile.phoneLabel")}
                    </Label>
                    <Input
                      id="telephone"
                      value={profileForm.telephone}
                      onChange={(e) => setProfileForm((f) => ({ ...f, telephone: e.target.value }))}
                      placeholder={t("profile.phonePlaceholder")}
                    />
                  </div>
                </div>

                <div className="mt-6 flex gap-3">
                  <Button onClick={handleSaveProfile} disabled={savingProfile}>
                    {savingProfile ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : null}
                    {savingProfile ? t("profile.saving") : t("profile.saveButton")}
                  </Button>
                </div>
              </Card>

              <Card className="p-6">
                <h3 className="font-display font-semibold text-lg mb-4 flex items-center gap-2">
                  <Lock className="h-5 w-5 text-muted-foreground" /> {t("profile.password.title")}
                </h3>
                <div className="grid md:grid-cols-3 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="ancien-mdp">{t("profile.password.currentLabel")}</Label>
                    <Input
                      id="ancien-mdp"
                      type="password"
                      autoComplete="new-password"
                      value={passwordForm.ancien}
                      onChange={(e) => setPasswordForm((f) => ({ ...f, ancien: e.target.value }))}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="nouveau-mdp">{t("profile.password.newLabel")}</Label>
                    <Input
                      id="nouveau-mdp"
                      type="password"
                      autoComplete="new-password"
                      value={passwordForm.nouveau}
                      onChange={(e) => setPasswordForm((f) => ({ ...f, nouveau: e.target.value }))}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="confirmation-mdp">{t("profile.password.confirmLabel")}</Label>
                    <Input
                      id="confirmation-mdp"
                      type="password"
                      autoComplete="new-password"
                      value={passwordForm.confirmation}
                      onChange={(e) => setPasswordForm((f) => ({ ...f, confirmation: e.target.value }))}
                    />
                  </div>
                </div>
                <div className="mt-6">
                  <Button variant="outline" onClick={handleChangePassword} disabled={savingPassword}>
                    {savingPassword ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : null}
                    {savingPassword ? t("profile.password.saving") : t("profile.password.changeButton")}
                  </Button>
                </div>
              </Card>

              <Card className="p-6 border-destructive/30">
                <h3 className="font-display font-semibold text-lg mb-2 flex items-center gap-2">
                  <AlertTriangle className="h-5 w-5 text-destructive" /> {t("profile.deleteAccount.title")}
                </h3>
                <p className="text-sm text-muted-foreground mb-4">
                  {t("profile.deleteAccount.description")}
                </p>
                <Button variant="destructive" onClick={() => setConfirmationSuppression(true)}>
                  <Trash2 className="h-4 w-4 mr-2" />
                  {t("profile.deleteAccount.button")}
                </Button>
              </Card>
            </TabsContent>
          </Tabs>

          <div className="mt-8 text-center">
            <Button variant="hero" size="lg" asChild>
              <Link to="/imprimeries">{t("newOrderCta")}</Link>
            </Button>
          </div>
        </div>
      </main>

      {/* La suppression est irréversible : elle mérite une confirmation explicite. */}
      <Dialog
        open={confirmationSuppression}
        onOpenChange={(open) => {
          if (open) return;
          // Une fois le compte supprimé, refermer par la croix mène au même
          // endroit que le bouton : l'espace client n'existe plus.
          if (compteSupprime) quitterApresSuppression();
          else setConfirmationSuppression(false);
        }}
      >
        <DialogContent className="sm:max-w-md">
          {compteSupprime ? (
            <>
              <DialogHeader>
                <div className="mx-auto w-14 h-14 rounded-full bg-primary/10 flex items-center justify-center mb-2">
                  <CheckCircle2 className="h-7 w-7 text-primary" />
                </div>
                <DialogTitle className="text-center">{t("deleteDialog.success.title")}</DialogTitle>
                <DialogDescription className="text-center">
                  {t("deleteDialog.success.description")}
                </DialogDescription>
              </DialogHeader>
              <DialogFooter>
                <Button className="w-full" onClick={quitterApresSuppression}>
                  {t("deleteDialog.success.backHome")}
                </Button>
              </DialogFooter>
            </>
          ) : (
            <>
              <DialogHeader>
                <div className="mx-auto w-14 h-14 rounded-full bg-destructive/10 flex items-center justify-center mb-2">
                  <AlertTriangle className="h-7 w-7 text-destructive" />
                </div>
                <DialogTitle className="text-center">{t("deleteDialog.confirm.title")}</DialogTitle>
                <DialogDescription className="text-center">
                  {t("deleteDialog.confirm.description")}
                </DialogDescription>
              </DialogHeader>

              <div className="text-sm text-muted-foreground space-y-2">
                <p>{t("deleteDialog.confirm.point1")}</p>
                <p>
                  {t("deleteDialog.confirm.point2")}
                </p>
                <p>{t("deleteDialog.confirm.point3")}</p>
              </div>

              <DialogFooter className="gap-2 sm:gap-0">
                <Button variant="outline" onClick={() => setConfirmationSuppression(false)} disabled={suppressionEnCours}>
                  {t("deleteDialog.confirm.cancel")}
                </Button>
                <Button variant="destructive" onClick={handleSupprimerMonCompte} disabled={suppressionEnCours}>
                  {suppressionEnCours ? (
                    <>
                      <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                      {t("deleteDialog.confirm.deleting")}
                    </>
                  ) : (
                    <>
                      <Trash2 className="h-4 w-4 mr-2" />
                      {t("deleteDialog.confirm.confirmButton")}
                    </>
                  )}
                </Button>
              </DialogFooter>
            </>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default DashboardClient;
