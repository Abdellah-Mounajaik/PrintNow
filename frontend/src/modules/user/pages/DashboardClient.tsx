import { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
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
} from "lucide-react";
import { toast } from "../../../hooks/use-toast";
import { useRef } from "react";
import { useAuth } from "../../auth/context/AuthContext";
import { userService } from "../services/user.service";
import type { CommandeDTO, VerifDTO, SuiviDTO, UserProfileDTO } from "../models/user.model";

const STATUS_MAP: Record<string, { label: string; variant: "default"|"secondary"|"destructive"|"outline" }> = {
  EN_ATTENTE_PAIEMENT: { label: "En attente", variant: "outline" },
  PAYEE: { label: "Payée", variant: "secondary" },
  EN_COURS_IMPRESSION: { label: "En cours", variant: "default" },
  PRETE: { label: "Prêt à être retiré", variant: "default" },
  LIVREE: { label: "Livrée", variant: "default" },
  ANNULEE: { label: "Annulée", variant: "destructive" },
};

// Une facture n'existe que pour une commande dont le paiement a bien été
// confirmé (même condition que côté backend, FactureService.STATUTS_FACTURABLES).
const STATUTS_FACTURABLES = new Set(["PAYEE", "EN_COURS_IMPRESSION", "PRETE", "LIVREE"]);

const DashboardClient = () => {
  const { user, token, logoutGlobal } = useAuth();
  const navigate = useNavigate();
  const [orders, setOrders] = useState<CommandeDTO[]>([]);
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
        toast({ title: "Profil mis à jour", description: "Votre email a changé, veuillez vous reconnecter." });
        logoutGlobal();
        navigate("/login?tab=connexion");
        return;
      }
      toast({ title: "Profil mis à jour" });
    } catch (e) {
      toast({ title: "Erreur", description: (e as Error).message, variant: "destructive" });
    } finally {
      setSavingProfile(false);
    }
  };

  const handleChangePassword = async () => {
    if (!token) return;
    if (passwordForm.nouveau !== passwordForm.confirmation) {
      toast({ title: "Erreur", description: "Les mots de passe ne correspondent pas.", variant: "destructive" });
      return;
    }
    setSavingPassword(true);
    try {
      await userService.changerMotDePasse(passwordForm.ancien, passwordForm.nouveau, token);
      setPasswordForm({ ancien: "", nouveau: "", confirmation: "" });
      toast({ title: "Mot de passe changé" });
    } catch (e) {
      toast({ title: "Erreur", description: (e as Error).message, variant: "destructive" });
    } finally {
      setSavingPassword(false);
    }
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
      toast({ title: "Erreur", description: (e as Error).message, variant: "destructive" });
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
      toast({ title: "Erreur", description: "Impossible de récupérer le suivi.", variant: "destructive" });
    } finally {
      setSuiviLoading(null);
    }
  };

  const handleSoumettre = async () => {
    const carteEt = carteEtudianteRef.current?.files?.[0];
    const carteId = carteIdentiteRef.current?.files?.[0];
    if (!carteEt || !carteId) {
      toast({ title: "Sélectionnez les deux documents", variant: "destructive" });
      return;
    }
    if (!token) return;
    setUploading(true);
    try {
      const data = await userService.soumettreVerification(carteEt, carteId, token);
      setVerif(data);
      toast({ title: "Demande envoyée", description: "L'admin examinera vos documents." });
    } catch (e) {
      toast({ title: "Erreur", description: (e as Error).message, variant: "destructive" });
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
              <TabsTrigger value="etudiant">Vérification étudiant</TabsTrigger>
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
                      const isLivraison = order.modeRetrait === "LIVRAISON";
                      const suivi = suiviData[order.id];
                      return (
                        <div key={order.id} className="border rounded-lg bg-muted/10 overflow-hidden">
                          <div className="flex items-center justify-between p-4 gap-4">
                            <div className="min-w-0">
                              <p className="font-mono font-medium text-sm">{order.numeroCommande}</p>
                              <p className="text-xs text-muted-foreground mt-0.5 flex items-center gap-1.5">
                                {order.nomImprimerie} · {new Date(order.dateCreation).toLocaleDateString("fr-BE")}
                                {isLivraison && <span className="inline-flex items-center gap-1"><Truck className="h-3 w-3" /> Livraison</span>}
                              </p>
                            </div>
                            <div className="flex items-center gap-3 shrink-0">
                              <Badge variant={status.variant as any}>{status.label}</Badge>
                              <span className="font-semibold text-primary">{Number(order.totalTTC).toFixed(2)}€</span>
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
                                    <p className="text-xs text-muted-foreground">Numéro de suivi bpost</p>
                                    <p className="font-mono text-sm font-medium">{suivi.numeroSuivi}</p>
                                    {suivi.statutAfterShipping && (
                                      <p className="text-xs text-muted-foreground mt-0.5">Statut : {suivi.statutAfterShipping}</p>
                                    )}
                                  </div>
                                  {suivi.lienSuiviBpost && (
                                    <Button size="sm" asChild>
                                      <a href={suivi.lienSuiviBpost} target="_blank" rel="noopener noreferrer">
                                        <ExternalLink className="h-3.5 w-3.5 mr-1" /> Suivre sur bpost
                                      </a>
                                    </Button>
                                  )}
                                </div>
                              ) : (
                                <p className="pt-3 text-sm text-muted-foreground">Votre colis n'a pas encore été déposé chez bpost.</p>
                              )}
                            </div>
                          )}
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
                {(() => {
                  const facturables = orders.filter((o) => STATUTS_FACTURABLES.has(o.statut));
                  if (facturables.length === 0) {
                    return (
                      <div className="text-center py-16 border-2 border-dashed rounded-lg bg-muted/20">
                        <FileText className="h-12 w-12 mx-auto text-muted-foreground mb-4 opacity-50" />
                        <h4 className="text-lg font-semibold mb-1">Aucune facture</h4>
                        <p className="text-muted-foreground text-sm">
                          Une facture est disponible dès qu'une commande est payée.
                        </p>
                      </div>
                    );
                  }
                  return (
                    <div className="space-y-3">
                      {facturables.map((order) => (
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
                            <span className="font-semibold">{Number(order.totalTTC).toFixed(2)}€</span>
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
                              Télécharger
                            </Button>
                          </div>
                        </div>
                      ))}
                    </div>
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
                    <h3 className="font-display font-semibold text-lg">Vérification étudiant</h3>
                    <p className="text-sm text-muted-foreground">
                      Déposez votre carte étudiant + carte d'identité pour bénéficier du tarif réduit.
                      La vérification est valable jusqu'au 30 juin et doit être renouvelée chaque année.
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
                          <p className="font-semibold text-success">Vérification acceptée ✓</p>
                          {verif.valableJusquA && (
                            <p className="text-sm text-muted-foreground">
                              Valable jusqu'au {new Date(verif.valableJusquA).toLocaleDateString("fr-BE")}
                            </p>
                          )}
                        </>
                      )}
                      {verif.statut === "EN_ATTENTE" && (
                        <>
                          <p className="font-semibold text-warning">Demande en cours d'examen</p>
                          <p className="text-sm text-muted-foreground">
                            Soumise le {new Date(verif.dateSoumission).toLocaleDateString("fr-BE")} · l'admin examinera vos documents.
                          </p>
                        </>
                      )}
                      {verif.statut === "REFUSE" && (
                        <>
                          <p className="font-semibold text-destructive">Demande refusée — vous pouvez soumettre à nouveau.</p>
                          {verif.motifRefus && (
                            <p className="text-sm text-muted-foreground mt-1">Motif : {verif.motifRefus}</p>
                          )}
                        </>
                      )}
                      {verif.statut === "EXPIRE" && (
                        <p className="font-semibold text-destructive">Vérification expirée — renouvelez votre demande.</p>
                      )}
                    </div>
                  </div>
                )}

                {/* Formulaire d'envoi */}
                {verifLoaded && (!verif || verif.statut === "REFUSE" || verif.statut === "EXPIRE") && (
                  <div className="space-y-4">
                    <div className="space-y-2">
                      <label className="text-sm font-medium">Carte étudiant</label>
                      <input
                        ref={carteEtudianteRef}
                        type="file"
                        accept="image/*,.pdf"
                        className="block w-full text-sm text-muted-foreground file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-medium file:bg-primary file:text-primary-foreground hover:file:bg-primary/90 cursor-pointer"
                      />
                    </div>
                    <div className="space-y-2">
                      <label className="text-sm font-medium">Carte d'identité</label>
                      <input
                        ref={carteIdentiteRef}
                        type="file"
                        accept="image/*,.pdf"
                        className="block w-full text-sm text-muted-foreground file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-medium file:bg-primary file:text-primary-foreground hover:file:bg-primary/90 cursor-pointer"
                      />
                    </div>
                    <Button onClick={handleSoumettre} disabled={uploading}>
                      {uploading ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : <Upload className="h-4 w-4 mr-2" />}
                      {uploading ? "Envoi en cours…" : "Envoyer mes documents"}
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
                    <Label htmlFor="prenom">Prénom</Label>
                    <Input
                      id="prenom"
                      value={profileForm.prenom}
                      onChange={(e) => setProfileForm((f) => ({ ...f, prenom: e.target.value }))}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="nom">Nom</Label>
                    <Input
                      id="nom"
                      value={profileForm.nom}
                      onChange={(e) => setProfileForm((f) => ({ ...f, nom: e.target.value }))}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="email" className="flex items-center gap-1.5">
                      <Mail className="h-3.5 w-3.5" /> Email
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
                      <Phone className="h-3.5 w-3.5" /> Téléphone
                    </Label>
                    <Input
                      id="telephone"
                      value={profileForm.telephone}
                      onChange={(e) => setProfileForm((f) => ({ ...f, telephone: e.target.value }))}
                      placeholder="+32 ..."
                    />
                  </div>
                </div>

                <div className="mt-6 flex gap-3">
                  <Button onClick={handleSaveProfile} disabled={savingProfile}>
                    {savingProfile ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : null}
                    {savingProfile ? "Enregistrement..." : "Modifier le profil"}
                  </Button>
                </div>
              </Card>

              <Card className="p-6">
                <h3 className="font-display font-semibold text-lg mb-4 flex items-center gap-2">
                  <Lock className="h-5 w-5 text-muted-foreground" /> Changer le mot de passe
                </h3>
                <div className="grid md:grid-cols-3 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="ancien-mdp">Mot de passe actuel</Label>
                    <Input
                      id="ancien-mdp"
                      type="password"
                      autoComplete="new-password"
                      value={passwordForm.ancien}
                      onChange={(e) => setPasswordForm((f) => ({ ...f, ancien: e.target.value }))}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="nouveau-mdp">Nouveau mot de passe</Label>
                    <Input
                      id="nouveau-mdp"
                      type="password"
                      autoComplete="new-password"
                      value={passwordForm.nouveau}
                      onChange={(e) => setPasswordForm((f) => ({ ...f, nouveau: e.target.value }))}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="confirmation-mdp">Confirmer le nouveau mot de passe</Label>
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
                    {savingPassword ? "Enregistrement..." : "Changer le mot de passe"}
                  </Button>
                </div>
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
