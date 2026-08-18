import { useState } from "react";
import { useTranslation } from "react-i18next";
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

import { correctionService } from "../services/correction.service";
import type { ChoixCorrection, EtatCorrection } from "../models/correction.model";




interface Props {
  file: File;
  etat: EtatCorrection | null;
  onChange: (etat: EtatCorrection | null) => void;
}

const CorrectionOrthographe = ({ file, etat, onChange }: Props) => {
  const { t } = useTranslation("order");
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
      const choix: ChoixCorrection = {
        fautesIgnorees: etat.fautesIgnorees,
        remplacementsChoisis: etat.remplacementsChoisis,
      };

      const pages: string[] = [];
      for (let page = 1; page <= etat.verification.nbPages; page++) {
        const image = await correctionService.apercuPage(etat.verification.id, page, choix, token);
        pages.push(URL.createObjectURL(image));
        setApercuPages([...pages]);
      }
    } catch (e) {
      toast({ title: t("spellCheck.toast.errorTitle"), description: (e as Error).message, variant: "destructive" });
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
    setAvancement({ cible: 0, affiche: 0, libelle: t("spellCheck.progress.sendingDocument") });

    const sondage = window.setInterval(async () => {
      const etape = await correctionService.progression(suivi, token);
      if (!etape) return; // analyse terminée, pas encore commencée, ou consultation manquée
      setAvancement((a) => ({
        cible: etape.pourcentage,
        // La barre ne revient jamais en arrière.
        affiche: Math.max(a?.affiche ?? 0, etape.pourcentage),
        libelle: etape.libelle,
      }));
    }, 500);

    // Entre deux étapes, la barre avance d'elle-même, sans jamais dépasser de
    // trop l'avancement réellement annoncé.
    const glissement = window.setInterval(() => {
      setAvancement((a) =>
        a ? { ...a, affiche: Math.min(a.affiche + 1, a.cible + 12, 99) } : a
      );
    }, 400);

    try {
      const data = await correctionService.analyser(file, suivi, token);

      // La correction est proposée activée si des fautes ont été trouvées.
      onChange({ verification: data, active: data.nbFautes > 0, fautesIgnorees: [], remplacementsChoisis: {} });
      if (data.nbFautes === 0) {
        toast({ title: t("spellCheck.toast.noErrorsTitle"), description: t("spellCheck.toast.noErrorsDescription") });
      }
    } catch (e) {
      toast({ title: t("spellCheck.toast.errorTitle"), description: (e as Error).message, variant: "destructive" });
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
              <p className="text-sm font-medium">{t("spellCheck.title")}</p>
              <p className="text-xs text-muted-foreground">
                {t("spellCheck.freeHint")}
              </p>
            </div>
          </div>
          <Button type="button" variant="outline" size="sm" onClick={analyser} disabled={analyse}>
            {analyse ? <Loader2 className="h-4 w-4 mr-2 animate-spin" /> : <Sparkles className="h-4 w-4 mr-2" />}
            {analyse ? t("spellCheck.analyzing") : t("spellCheck.analyzeButton")}
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
        <p className="text-sm">{t("spellCheck.noErrorsInDocument")}</p>
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
            {t("spellCheck.errorsFound", { count: verification.nbFautes })}
          </p>
          <p className="text-xs text-muted-foreground">
            {t("spellCheck.pagesAnalyzed", { count: verification.nbPages })}
            {verification.langue ? t("spellCheck.languageSuffix", { lang: verification.langue }) : ""}
            {" "}{t("spellCheck.chooseSuggestionHint")}
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
                <span className="text-muted-foreground shrink-0 w-8">{t("spellCheck.pageAbbrev", { page: faute.page })}</span>
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
                      {t("spellCheck.fixYourself")}
                    </span>
                  )}
                </span>
                <button
                  type="button"
                  onClick={() => basculerFaute(i)}
                  title={ignoree ? t("spellCheck.restoreCorrection") : t("spellCheck.keepOriginalWord")}
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
            {t("spellCheck.printCorrectedVersion")}
            <span className="font-semibold text-primary">{t("spellCheck.priceSuffix", { price: verification.prix.toFixed(2) })}</span>
            <span className="block text-xs text-muted-foreground">
              {t("spellCheck.correctionsApplied", { count: aCorriger })}
            </span>
          </span>
        </label>

        <Button type="button" variant="outline" size="sm" onClick={ouvrirApercu}>
          <Eye className="h-4 w-4 mr-2" />
          {t("spellCheck.previewButton")}
        </Button>
      </div>

      <Dialog open={apercuOuvert} onOpenChange={setApercuOuvert}>
        <DialogContent className="max-w-3xl">
          <DialogHeader>
            <DialogTitle>{t("spellCheck.previewDialog.title")}</DialogTitle>
            <DialogDescription>
              {t("spellCheck.previewDialog.description")}
            </DialogDescription>
          </DialogHeader>

          <div className="rounded-lg border bg-muted/20 overflow-auto max-h-[65vh] p-3 space-y-4">
            {apercuPages.map((url, i) => (
              <div key={i} className="space-y-1">
                <p className="text-xs text-muted-foreground text-center">
                  {t("spellCheck.previewDialog.pageXOfY", { page: i + 1, total: verification.nbPages })}
                </p>
                <img
                  src={url}
                  alt={t("spellCheck.previewDialog.pageAlt", { page: i + 1 })}
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
                  ? t("spellCheck.previewDialog.generating")
                  : t("spellCheck.previewDialog.loadingPage", { page: apercuPages.length + 1, total: verification.nbPages })}
              </div>
            )}
          </div>

          <p className="text-xs text-muted-foreground flex items-center gap-1.5">
            <Lock className="h-3.5 w-3.5 shrink-0" />
            {t("spellCheck.previewDialog.imageOnlyHint")}
          </p>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default CorrectionOrthographe;
