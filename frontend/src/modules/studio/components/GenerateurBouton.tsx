import { useState, useRef, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { Sparkles, Loader2, Check, ZoomIn } from "lucide-react";
import { Button } from "../../../components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogTrigger,
} from "../../../components/ui/dialog";
import { useAuth } from "../../auth/context/AuthContext";
import { studioService } from "../services/studio.service";

const TYPES = [
  { value: "CV", labelKey: "designGenerator.types.cv.label", exempleKey: "designGenerator.types.cv.example" },
  { value: "FLYER", labelKey: "designGenerator.types.flyer.label", exempleKey: "designGenerator.types.flyer.example" },
  { value: "CARTE_VISITE", labelKey: "designGenerator.types.carteVisite.label", exempleKey: "designGenerator.types.carteVisite.example" },
] as const;

type TypeSupport = (typeof TYPES)[number]["value"];

interface Apercu {
  id: number;
  url: string;
}

interface Props {
  /** Appelé avec le PDF de la proposition choisie (+ son id, pour la facturation), à ajouter aux fichiers de la commande. */
  onFichierGenere: (fichier: File, generationId?: number) => void;
  /** Types que l'imprimerie sait imprimer (valeurs de TYPES) ; si absent, tous. */
  typesDisponibles?: string[];
}

/**
 * Le client choisit un type, décrit ce qu'il veut ; l'IA en fabrique 3 versions
 * (couleurs/police différentes). Il choisit sa préférée, et son PDF rejoint la
 * commande comme un fichier téléversé normal.
 */
const GenerateurBouton = ({ onFichierGenere, typesDisponibles }: Props) => {
  const { t } = useTranslation("order");
  const { token } = useAuth();
  // On ne montre que les types imprimables par cette imprimerie, avec leurs libellés traduits.
  const typesAffiches = TYPES
    .filter((meta) => !typesDisponibles || typesDisponibles.includes(meta.value))
    .map((meta) => ({ value: meta.value, label: t(meta.labelKey), exemple: t(meta.exempleKey) }));
  const [ouvert, setOuvert] = useState(false);
  const [type, setType] = useState<TypeSupport>(typesAffiches[0]?.value ?? "CV");
  const [brief, setBrief] = useState("");
  const [chargement, setChargement] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);
  const [apercus, setApercus] = useState<Apercu[]>([]);
  const [choisiId, setChoisiId] = useState<number | null>(null);
  const [agrandi, setAgrandi] = useState<Apercu | null>(null);
  const [progres, setProgres] = useState(0);
  const minuteur = useRef<number | null>(null);

  const exemple = typesAffiches.find((meta) => meta.value === type)?.exemple ?? "";

  // Progression simulée : le backend est synchrone (aucune vraie progression),
  // mais la barre monte par paliers vers ~92 % puis termine à la réponse. Les
  // libellés suivent les vraies étapes (contenu → couleurs → mise en page).
  const arreterProgres = () => {
    if (minuteur.current !== null) {
      clearInterval(minuteur.current);
      minuteur.current = null;
    }
  };

  const demarrerProgres = () => {
    arreterProgres();
    setProgres(5);
    minuteur.current = window.setInterval(() => {
      setProgres((p) => (p >= 92 ? p : Math.min(92, p + Math.max(0.6, (92 - p) * 0.07))));
    }, 220);
  };

  useEffect(() => arreterProgres, []);

  const etape = progres < 30
    ? t("designGenerator.progress.writingContent")
    : progres < 58
      ? t("designGenerator.progress.choosingColors")
      : t("designGenerator.progress.layout");

  const reinitialiser = () => {
    setBrief("");
    setErreur(null);
    setApercus([]);
    setChoisiId(null);
    setAgrandi(null);
    setChargement(false);
    arreterProgres();
    setProgres(0);
  };

  const generer = async () => {
    if (!token || brief.trim().length === 0) return;
    setChargement(true);
    setErreur(null);
    setApercus([]);
    setChoisiId(null);
    setAgrandi(null);
    demarrerProgres();
    try {
      const generation = await studioService.generer(type, brief.trim(), token);
      if (generation.propositions.length === 0) throw new Error(t("designGenerator.errors.noResult"));
      const charges = await Promise.all(
        generation.propositions.map(async (p) => ({
          id: p.id,
          url: await studioService.fichierUrl(p.id, "apercu", token),
        }))
      );
      setProgres(100);
      setApercus(charges);
      setChoisiId(charges[0].id);
    } catch (e) {
      setErreur(e instanceof Error ? e.message : t("designGenerator.errors.generic"));
    } finally {
      arreterProgres();
      setChargement(false);
    }
  };

  const utiliser = async () => {
    if (!token || choisiId == null) return;
    try {
      const fichier = await studioService.pdfFichier(choisiId, token);
      onFichierGenere(fichier, choisiId);
      setOuvert(false);
      reinitialiser();
    } catch {
      setErreur(t("designGenerator.errors.pdfRetrievalFailed"));
    }
  };

  return (
    <>
    <Dialog open={ouvert} onOpenChange={(o) => { setOuvert(o); if (!o) reinitialiser(); }}>
      <DialogTrigger asChild>
        <Button variant="outline" className="w-full">
          <Sparkles className="h-4 w-4 mr-2" />
          {t("designGenerator.triggerButton")}
        </Button>
      </DialogTrigger>
      <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Sparkles className="h-5 w-5 text-primary" />
            {t("designGenerator.dialog.title")}
          </DialogTitle>
          <DialogDescription>
            {t("designGenerator.dialog.description")}
          </DialogDescription>
        </DialogHeader>

        {/* Type */}
        <div className="flex flex-wrap gap-2">
          {typesAffiches.map((meta) => (
            <Button
              key={meta.value}
              variant={type === meta.value ? "default" : "outline"}
              size="sm"
              onClick={() => { setType(meta.value); setApercus([]); setChoisiId(null); }}
            >
              {meta.label}
            </Button>
          ))}
        </div>

        {/* Brief */}
        <div>
          <textarea
            value={brief}
            onChange={(e) => setBrief(e.target.value)}
            placeholder={exemple}
            maxLength={4000}
            rows={5}
            className="w-full rounded-lg border border-border bg-background p-3 text-sm leading-relaxed
                       focus:outline-none focus:ring-2 focus:ring-primary/40 resize-none"
          />
          <div className="flex items-center justify-between mt-1">
            <span className="text-xs text-muted-foreground">{t("designGenerator.charCountHint", { length: brief.length })}</span>
          </div>
          <Button className="mt-2 w-full" onClick={generer} disabled={chargement || brief.trim().length === 0}>
            {chargement ? (
              <><Loader2 className="h-4 w-4 mr-2 animate-spin" />{t("designGenerator.generatingButton", { percent: Math.round(progres) })}</>
            ) : (
              <><Sparkles className="h-4 w-4 mr-2" />{t("designGenerator.generateButton")}</>
            )}
          </Button>
          {erreur && <p className="mt-2 text-sm text-destructive">{erreur}</p>}
        </div>

        {/* Propositions */}
        {chargement ? (
          <div className="py-8">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm text-muted-foreground">{etape}</span>
              <span className="text-sm font-semibold tabular-nums">{Math.round(progres)}%</span>
            </div>
            <div className="h-2 w-full rounded-full bg-muted overflow-hidden">
              <div
                className="h-full rounded-full bg-primary transition-all duration-200 ease-out"
                style={{ width: `${progres}%` }}
              />
            </div>
            <p className="text-xs text-muted-foreground mt-3 text-center">{t("designGenerator.generatingHint")}</p>
          </div>
        ) : apercus.length > 0 ? (
          <div>
            <p className="text-sm font-medium mb-2">{t("designGenerator.chooseVersionPrompt")}</p>
            <div className="grid grid-cols-3 gap-3">
              {apercus.map((a, i) => (
                <div
                  key={a.id}
                  role="button"
                  tabIndex={0}
                  onClick={() => setChoisiId(a.id)}
                  onKeyDown={(e) => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); setChoisiId(a.id); } }}
                  className={`relative rounded-lg border-2 p-1 transition-colors text-left cursor-pointer ${
                    choisiId === a.id ? "border-primary ring-2 ring-primary/30" : "border-border hover:border-primary/40"
                  }`}
                >
                  <img src={a.url} alt={t("designGenerator.versionLabel", { index: i + 1 })} className="w-full rounded" />
                  {/* Agrandir pour lire le détail, sans quitter la sélection. */}
                  <button
                    type="button"
                    onClick={(e) => { e.stopPropagation(); setAgrandi(a); }}
                    title={t("designGenerator.zoomButton")}
                    aria-label={t("designGenerator.zoomButton")}
                    className="absolute top-2 right-2 rounded-md border border-border bg-background/85 p-1
                               text-muted-foreground shadow-sm hover:text-foreground"
                  >
                    <ZoomIn className="h-4 w-4" />
                  </button>
                  <span className="block text-center text-xs text-muted-foreground mt-1">{t("designGenerator.versionLabel", { index: i + 1 })}</span>
                </div>
              ))}
            </div>
            <Button className="mt-4 w-full" onClick={utiliser} disabled={choisiId == null}>
              <Check className="h-4 w-4 mr-2" />
              {t("designGenerator.useThisVersion")}
            </Button>
          </div>
        ) : null}
      </DialogContent>
    </Dialog>

    {/* Aperçu agrandi (filigrané) : lire le détail avant de choisir. */}
    <Dialog open={agrandi != null} onOpenChange={(o) => { if (!o) setAgrandi(null); }}>
      <DialogContent className="max-w-2xl max-h-[92vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <ZoomIn className="h-5 w-5 text-primary" />
            {t("designGenerator.zoomDialog.title")}
          </DialogTitle>
          <DialogDescription>
            {t("designGenerator.zoomDialog.description")}
          </DialogDescription>
        </DialogHeader>
        {agrandi && (
          <div className="flex flex-col items-center gap-4">
            <img
              src={agrandi.url}
              alt={t("designGenerator.zoomDialog.imageAlt")}
              className="max-h-[74vh] max-w-full w-auto rounded border border-border"
            />
            <Button
              className="w-full"
              onClick={() => { setChoisiId(agrandi.id); setAgrandi(null); }}
            >
              <Check className="h-4 w-4 mr-2" />
              {t("designGenerator.chooseThisVersion")}
            </Button>
          </div>
        )}
      </DialogContent>
    </Dialog>
    </>
  );
};

export default GenerateurBouton;
