import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription,
} from "@/components/ui/dialog";
import { toast } from "@/hooks/use-toast";
import { useAuth } from "@/modules/auth/context/AuthContext";
import {
  SpellCheck2, Loader2, CheckCircle2, AlertTriangle, Sparkles, Undo2, Eye, Lock,
} from "lucide-react";

import { API_URL } from "../../../lib/api";
import type { EtatCorrection } from "../models/correction.model";

const API = `${API_URL}/corrections`;




interface Props {
  file: File;
  etat: EtatCorrection | null;
  onChange: (etat: EtatCorrection | null) => void;
}

const CorrectionOrthographe = ({ file, etat, onChange }: Props) => {
  const { token } = useAuth();
  const [analyse, setAnalyse] = useState(false);
  /**
   * `cible` est l'étape annoncée par le serveur, `affiche` la valeur montrée.
   * La seconde rattrape la première puis continue d'avancer doucement : le
   * serveur ne publie qu'aux changements d'étape, et une barre immobile
   * plusieurs secondes donne l'impression que rien ne se passe.
   */
  const [avancement, setAvancement] = useState<
    { cible: number; affiche: number; libelle: string } | null
  >(null);
  const [apercuOuvert, setApercuOuvert] = useState(false);
  const [apercuPages, setApercuPages] = useState<string[]>([]);
  const [apercuChargement, setApercuChargement] = useState(false);

  /**
   * L'aperçu est constitué d'images filigranées produites par le serveur : le
   * PDF corrigé n'est jamais transmis avant paiement. Les pages sont chargées
   * l'une après l'autre pour que la première s'affiche sans attendre les autres.
   */
  const ouvrirApercu = async () => {
    if (!etat || !token) return;
    setApercuOuvert(true);
    if (apercuPages.length > 0) return;

    setApercuChargement(true);
    try {
      // On transmet les choix en cours pour que l'aperçu montre exactement ce
      // que le client recevra, y compris les suggestions qu'il a modifiées.
      const choix = JSON.stringify({
        fautesIgnorees: etat.fautesIgnorees,
        remplacementsChoisis: etat.remplacementsChoisis,
      });

      const pages: string[] = [];
      for (let page = 1; page <= etat.verification.nbPages; page++) {
        const res = await fetch(`${API}/${etat.verification.id}/apercu?page=${page}`, {
          method: "POST",
          headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
          body: choix,
        });
        if (!res.ok) throw new Error("Aperçu indisponible.");
        pages.push(URL.createObjectURL(await res.blob()));
        setApercuPages([...pages]);
      }
    } catch (e) {
      toast({ title: "Erreur", description: (e as Error).message, variant: "destructive" });
    } finally {
      setApercuChargement(false);
    }
  };

  const analyser = async () => {
    if (!token) return;
    setAnalyse(true);

    // Le serveur publie son avancement sous cet identifiant, que nous
    // interrogeons pendant qu'il travaille : l'analyse demande plusieurs
    // secondes, mieux vaut montrer où elle en est.
    const suivi = crypto.randomUUID();
    setAvancement({ cible: 0, affiche: 0, libelle: "Envoi du document" });

    const sondage = window.setInterval(async () => {
      try {
        const res = await fetch(`${API}/progression/${suivi}`, {
          headers: { Authorization: `Bearer ${token}` },
        });
        if (res.status !== 200) return; // analyse terminée ou pas encore commencée
        const etape = await res.json();
        setAvancement((a) => ({
          cible: etape.pourcentage,
          // La barre ne revient jamais en arrière.
          affiche: Math.max(a?.affiche ?? 0, etape.pourcentage),
          libelle: etape.libelle,
        }));
      } catch {
        // Une consultation manquée n'a pas d'importance : la suivante suivra.
      }
    }, 500);

    // Entre deux étapes, la barre avance d'elle-même, sans jamais dépasser de
    // trop l'avancement réellement annoncé.
    const glissement = window.setInterval(() => {
      setAvancement((a) =>
        a ? { ...a, affiche: Math.min(a.affiche + 1, a.cible + 12, 99) } : a
      );
    }, 400);

    try {
      const formData = new FormData();
      formData.append("file", file);
      formData.append("suivi", suivi);

      const res = await fetch(`${API}/analyser`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
        body: formData,
      });
      const data = await res.json().catch(() => null);
      if (!res.ok) throw new Error(data?.message ?? "Analyse impossible.");

      // La correction est proposée activée si des fautes ont été trouvées.
      onChange({ verification: data, active: data.nbFautes > 0, fautesIgnorees: [], remplacementsChoisis: {} });
      if (data.nbFautes === 0) {
        toast({ title: "Aucune faute détectée", description: "Votre document semble correct." });
      }
    } catch (e) {
      toast({ title: "Erreur", description: (e as Error).message, variant: "destructive" });
    } finally {
      window.clearInterval(sondage);
      window.clearInterval(glissement);
      setAvancement(null);
      setAnalyse(false);
    }
  };

  /** L'aperçu déjà chargé ne reflète plus les choix : on le fera régénérer. */
  const invaliderApercu = () => {
    apercuPages.forEach((url) => URL.revokeObjectURL(url));
    setApercuPages([]);
  };

  const basculerFaute = (index: number) => {
    if (!etat) return;
    const ignorees = etat.fautesIgnorees.includes(index)
      ? etat.fautesIgnorees.filter((i) => i !== index)
      : [...etat.fautesIgnorees, index];
    invaliderApercu();
    onChange({ ...etat, fautesIgnorees: ignorees });
  };

  const choisirSuggestion = (index: number, suggestion: string) => {
    if (!etat) return;
    invaliderApercu();
    onChange({
      ...etat,
      remplacementsChoisis: { ...etat.remplacementsChoisis, [index]: suggestion },
      // Choisir une suggestion réactive la correction si elle avait été écartée.
      fautesIgnorees: etat.fautesIgnorees.filter((i) => i !== index),
    });
  };

  // ── Avant analyse ────────────────────────────────────────────────────────
  if (!etat) {
    return (
      <div className="mt-3 p-3 rounded-lg border border-dashed bg-muted/20">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-start gap-2 min-w-0">
            <SpellCheck2 className="h-4 w-4 text-primary mt-0.5 shrink-0" />
            <div className="min-w-0">
              <p className="text-sm font-medium">Vérifier l'orthographe avant impression</p>
              <p className="text-xs text-muted-foreground">
                Analyse et aperçu des corrections gratuits.
              </p>
            </div>
          </div>
          <Button type="button" variant="outline" size="sm" onClick={analyser} disabled={analyse}>
            {analyse ? <Loader2 className="h-4 w-4 mr-2 animate-spin" /> : <Sparkles className="h-4 w-4 mr-2" />}
            {analyse ? "Analyse…" : "Analyser"}
          </Button>
        </div>

        {avancement && (
          <div className="mt-3">
            <div className="flex items-center justify-between mb-1.5">
              <span className="text-xs text-muted-foreground">{avancement.libelle}…</span>
              <span className="text-xs font-medium tabular-nums">{avancement.affiche} %</span>
            </div>
            <div className="h-1.5 w-full rounded-full bg-muted overflow-hidden">
              <div
                className="h-full rounded-full bg-primary transition-all duration-500 ease-out"
                style={{ width: `${avancement.affiche}%` }}
              />
            </div>
          </div>
        )}
      </div>
    );
  }

  const { verification, active, fautesIgnorees } = etat;

  // ── Aucune faute ─────────────────────────────────────────────────────────
  if (verification.nbFautes === 0) {
    return (
      <div className="mt-3 p-3 rounded-lg border border-success/30 bg-success/5 flex items-center gap-2">
        <CheckCircle2 className="h-4 w-4 text-success shrink-0" />
        <p className="text-sm">Aucune faute détectée dans ce document.</p>
      </div>
    );
  }

  const fautes = verification.fautes ?? [];
  const aCorriger = fautes.length - fautesIgnorees.length;

  // ── Fautes trouvées : aperçu gratuit et choix ────────────────────────────
  return (
    <div className="mt-3 p-3 rounded-lg border border-warning/40 bg-warning/5 space-y-3">
      <div className="flex items-start gap-2">
        <AlertTriangle className="h-4 w-4 text-warning mt-0.5 shrink-0" />
        <div className="min-w-0">
          <p className="text-sm font-medium">
            {verification.nbFautes} faute{verification.nbFautes > 1 ? "s" : ""} détectée
            {verification.nbFautes > 1 ? "s" : ""}
          </p>
          <p className="text-xs text-muted-foreground">
            {verification.nbPages} page{verification.nbPages > 1 ? "s" : ""} analysée
            {verification.nbPages > 1 ? "s" : ""}
            {verification.langue ? ` en ${verification.langue}` : ""} · choisissez une autre suggestion,
            ou conservez le mot d'origine
          </p>
        </div>
      </div>

      <div className="max-h-56 overflow-y-auto rounded-md border bg-background divide-y">
        {fautes.map((faute, i) => {
          const ignoree = fautesIgnorees.includes(i);
          const retenue = etat.remplacementsChoisis[i] ?? faute.correction;
          const alternatives = (faute.suggestions ?? []).filter((s) => s !== retenue).slice(0, 4);

          return (
            <div key={i} className="px-3 py-2 text-xs">
              <div className="flex items-center gap-2">
                <span className="text-muted-foreground shrink-0 w-8">p.{faute.page}</span>
                <span className={`min-w-0 flex-1 ${ignoree ? "opacity-40" : ""}`}>
                  <span className={retenue ? "line-through text-destructive" : "text-destructive"}>
                    {faute.motFautif}
                  </span>
                  {retenue ? (
                    <>
                      <span className="mx-1 text-muted-foreground">→</span>
                      <span className="font-medium text-success">{retenue}</span>
                    </>
                  ) : (
                    // Faute repérée mais qu'aucune suggestion ne permet de réparer :
                    // mieux vaut le signaler que de taire le problème.
                    <span className="ml-2 text-muted-foreground italic">
                      à corriger vous-même
                    </span>
                  )}
                </span>
                <button
                  type="button"
                  onClick={() => basculerFaute(i)}
                  title={ignoree ? "Rétablir cette correction" : "Conserver le mot d'origine"}
                  className="shrink-0 text-muted-foreground hover:text-foreground"
                >
                  {ignoree ? <Undo2 className="h-3.5 w-3.5" /> : <CheckCircle2 className="h-3.5 w-3.5 text-success" />}
                </button>
              </div>

              {/* Le contexte permet de juger les corrections grammaticales, où le
                  correcteur se trompe parfois (accord calé sur un mot mal écrit). */}
              {faute.contexte && (
                <p className="pl-10 mt-0.5 text-[11px] text-muted-foreground italic truncate">
                  {faute.contexte}
                </p>
              )}

              {!ignoree && alternatives.length > 0 && (
                <div className="flex flex-wrap gap-1 mt-1 pl-10">
                  {alternatives.map((suggestion) => (
                    <button
                      key={suggestion}
                      type="button"
                      onClick={() => choisirSuggestion(i, suggestion)}
                      className="px-1.5 py-0.5 rounded border text-[11px] text-muted-foreground hover:border-primary hover:text-primary"
                    >
                      {suggestion}
                    </button>
                  ))}
                </div>
              )}
            </div>
          );
        })}
      </div>

      <div className="flex flex-wrap items-center justify-between gap-2">
        <label className="flex items-start gap-2.5 cursor-pointer">
          <Checkbox
            checked={active}
            onCheckedChange={(v) => onChange({ ...etat, active: v === true })}
            className="mt-0.5"
          />
          <span className="text-sm">
            Imprimer la version corrigée
            <span className="font-semibold text-primary"> +{verification.prix.toFixed(2)}€</span>
            <span className="block text-xs text-muted-foreground">
              {aCorriger} correction{aCorriger > 1 ? "s" : ""} appliquée{aCorriger > 1 ? "s" : ""} ·
              réglé avec votre commande
            </span>
          </span>
        </label>

        <Button type="button" variant="outline" size="sm" onClick={ouvrirApercu}>
          <Eye className="h-4 w-4 mr-2" />
          Aperçu
        </Button>
      </div>

      <Dialog open={apercuOuvert} onOpenChange={setApercuOuvert}>
        <DialogContent className="max-w-3xl">
          <DialogHeader>
            <DialogTitle>Aperçu du document corrigé</DialogTitle>
            <DialogDescription>
              Les mots corrigés sont surlignés en vert. Le fichier PDF, sans
              surlignage ni filigrane, est remis après le règlement de votre commande.
            </DialogDescription>
          </DialogHeader>

          <div className="rounded-lg border bg-muted/20 overflow-auto max-h-[65vh] p-3 space-y-4">
            {apercuPages.map((url, i) => (
              <div key={i} className="space-y-1">
                <p className="text-xs text-muted-foreground text-center">
                  Page {i + 1} sur {verification.nbPages}
                </p>
                <img
                  src={url}
                  alt={`Page ${i + 1} du document corrigé`}
                  className="w-full select-none pointer-events-none rounded border bg-white"
                  draggable={false}
                  onContextMenu={(e) => e.preventDefault()}
                />
              </div>
            ))}

            {apercuChargement && (
              <div className="flex items-center justify-center gap-2 py-8 text-sm text-muted-foreground">
                <Loader2 className="h-4 w-4 animate-spin" />
                {apercuPages.length === 0
                  ? "Génération de l'aperçu…"
                  : `Page ${apercuPages.length + 1} sur ${verification.nbPages}…`}
              </div>
            )}
          </div>

          <p className="text-xs text-muted-foreground flex items-center gap-1.5">
            <Lock className="h-3.5 w-3.5 shrink-0" />
            Aperçu en image, non téléchargeable.
          </p>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default CorrectionOrthographe;
