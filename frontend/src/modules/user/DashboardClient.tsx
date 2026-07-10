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
  GraduationCap,
  Upload,
  CheckCircle2,
  AlertCircle,
  Clock3,
  Loader2,
  Truck,
  ExternalLink,
} from "lucide-react";
import { toast } from "../../hooks/use-toast";
import { useRef } from "react";
import { useAuth } from "../auth/context/AuthContext";

const STATUS_MAP: Record<string, { label: string; variant: "default"|"secondary"|"destructive"|"outline" }> = {
  EN_ATTENTE_PAIEMENT: { label: "En attente", variant: "outline" },
  PAYEE: { label: "Payée", variant: "secondary" },
  EN_COURS_IMPRESSION: { label: "En cours", variant: "default" },
  PRETE: { label: "Prêt à être retiré", variant: "default" },
  LIVREE: { label: "Livrée", variant: "default" },
  ANNULEE: { label: "Annulée", variant: "destructive" },
};

type VerifDTO = {
  id: number;
  statut: string;
  dateSoumission: string;
  valableJusquA: string | null;
  motifRefus: string | null;
};

const DashboardClient = () => {
  const { user, token } = useAuth();
  const [orders, setOrders] = useState<any[]>([]);
  const [suiviData, setSuiviData] = useState<Record<number, any>>({});
  const [suiviLoading, setSuiviLoading] = useState<number | null>(null);
  const [verif, setVerif] = useState<VerifDTO | null>(null);
  const [verifLoaded, setVerifLoaded] = useState(false);
  const [uploading, setUploading] = useState(false);
  const carteEtudianteRef = useRef<HTMLInputElement>(null);
  const carteIdentiteRef = useRef<HTMLInputElement>(null);

  const headers = { Authorization: `Bearer ${token}` };

  useEffect(() => {
    if (!token) return;
    fetch("http://localhost:8080/api/commandes/me", { headers })
      .then(res => res.ok ? res.json() : [])
      .then(data => setOrders(data))
      .catch(() => setOrders([]));

    fetch("http://localhost:8080/api/verifications-etudiants/me", { headers })
      .then(res => res.status === 204 ? null : res.json())
      .then(data => { setVerif(data ?? null); setVerifLoaded(true); })
      .catch(() => { setVerif(null); setVerifLoaded(true); });
  }, [token]);

  const handleFetchSuivi = async (orderId: number) => {
    setSuiviLoading(orderId);
    try {
      const res = await fetch(`http://localhost:8080/api/livraisons/${orderId}/suivi`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) throw new Error();
      const data = await res.json();
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
    setUploading(true);
    const formData = new FormData();
    formData.append("carteEtudiante", carteEt);
    formData.append("carteIdentite", carteId);
    try {
      const res = await fetch("http://localhost:8080/api/verifications-etudiants/me", {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
        body: formData,
      });
      if (res.ok) {
        const data = await res.json();
        setVerif(data);
        toast({ title: "Demande envoyée", description: "L'admin examinera vos documents." });
      } else {
        const err = await res.json().catch(() => ({}));
        toast({ title: "Erreur", description: err.message ?? "Impossible d'envoyer.", variant: "destructive" });
      }
    } catch {
      toast({ title: "Erreur réseau", variant: "destructive" });
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
                <div className="text-center py-16 border-2 border-dashed rounded-lg bg-muted/20">
                  <FileText className="h-12 w-12 mx-auto text-muted-foreground mb-4 opacity-50" />
                  <h4 className="text-lg font-semibold mb-1">Aucune facture</h4>
                  <p className="text-muted-foreground text-sm">
                    Vos factures apparaîtront ici après votre première commande.
                  </p>
                </div>
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
