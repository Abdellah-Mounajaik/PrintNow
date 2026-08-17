import { useState, useRef, useEffect } from "react";
import { Sparkles, Loader2, Check } from "lucide-react";
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
  {
    value: "CV",
    label: "CV",
    exemple:
      "Je m'appelle …, développeur, à Bruxelles. Email …, tél …. Bachelier en informatique " +
      "(école, année). 1 an d'expérience chez … en Java/React. Compétences : … . Langues : …",
  },
  {
    value: "FLYER",
    label: "Flyer",
    exemple:
      "Un flyer pour la réouverture de mon restaurant Chez Marco samedi. Menu du midi à 15 €. " +
      "Ambiance chaleureuse. Adresse : rue de la Loi 12, Bruxelles. Tél 02 345 67 89.",
  },
  {
    value: "CARTE_VISITE",
    label: "Carte de visite",
    exemple:
      "Carte pour Sarah Lemaire, designer UX/UI chez Studio Web. Tél 0475 98 76 54, " +
      "email sarah@studioweb.be, site sarahlemaire.be, Bruxelles.",
  },
] as const;

type TypeSupport = (typeof TYPES)[number]["value"];

interface Apercu {
  id: number;
  url: string;
}

interface Props {
  /** Appelé avec le PDF de la proposition choisie (+ son id, pour la facturation), à ajouter aux fichiers de la commande. */
  onFichierGenere: (fichier: File, generationId?: number) => void;
}

/**
 * Le client choisit un type, décrit ce qu'il veut ; l'IA en fabrique 3 versions
 * (couleurs/police différentes). Il choisit sa préférée, et son PDF rejoint la
 * commande comme un fichier téléversé normal.
 */
const GenerateurBouton = ({ onFichierGenere }: Props) => {
  const { token } = useAuth();
  const [ouvert, setOuvert] = useState(false);
  const [type, setType] = useState<TypeSupport>("CV");
  const [brief, setBrief] = useState("");
  const [chargement, setChargement] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);
  const [apercus, setApercus] = useState<Apercu[]>([]);
  const [choisiId, setChoisiId] = useState<number | null>(null);
  const [progres, setProgres] = useState(0);
  const minuteur = useRef<number | null>(null);

  const exemple = TYPES.find((t) => t.value === type)!.exemple;

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
    ? "Rédaction du contenu…"
    : progres < 58
      ? "Choix des couleurs…"
      : "Mise en page des 3 versions…";

  const reinitialiser = () => {
    setBrief("");
    setErreur(null);
    setApercus([]);
    setChoisiId(null);
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
    demarrerProgres();
    try {
      const generation = await studioService.generer(type, brief.trim(), token);
      if (generation.propositions.length === 0) throw new Error("Aucun rendu n'a été produit.");
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
      setErreur(e instanceof Error ? e.message : "Une erreur est survenue.");
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
      setErreur("Impossible de récupérer le PDF.");
    }
  };

  return (
    <Dialog open={ouvert} onOpenChange={(o) => { setOuvert(o); if (!o) reinitialiser(); }}>
      <DialogTrigger asChild>
        <Button variant="outline" className="w-full">
          <Sparkles className="h-4 w-4 mr-2" />
          Je n'ai pas de fichier — générer avec l'IA
        </Button>
      </DialogTrigger>
      <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Sparkles className="h-5 w-5 text-primary" />
            Générer un support
          </DialogTitle>
          <DialogDescription>
            Choisissez un type, décrivez ce que vous voulez : l'IA en propose 3 versions, vous choisissez la vôtre.
          </DialogDescription>
        </DialogHeader>

        {/* Type */}
        <div className="flex flex-wrap gap-2">
          {TYPES.map((t) => (
            <Button
              key={t.value}
              variant={type === t.value ? "default" : "outline"}
              size="sm"
              onClick={() => { setType(t.value); setApercus([]); setChoisiId(null); }}
            >
              {t.label}
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
            <span className="text-xs text-muted-foreground">{brief.length}/4000 — plus vous en dites, mieux c'est.</span>
          </div>
          <Button className="mt-2 w-full" onClick={generer} disabled={chargement || brief.trim().length === 0}>
            {chargement ? (
              <><Loader2 className="h-4 w-4 mr-2 animate-spin" />Génération… {Math.round(progres)}%</>
            ) : (
              <><Sparkles className="h-4 w-4 mr-2" />Générer 3 propositions</>
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
            <p className="text-xs text-muted-foreground mt-3 text-center">L'IA fabrique vos 3 versions…</p>
          </div>
        ) : apercus.length > 0 ? (
          <div>
            <p className="text-sm font-medium mb-2">Choisissez votre version préférée :</p>
            <div className="grid grid-cols-3 gap-3">
              {apercus.map((a, i) => (
                <button
                  key={a.id}
                  type="button"
                  onClick={() => setChoisiId(a.id)}
                  className={`rounded-lg border-2 p-1 transition-colors text-left ${
                    choisiId === a.id ? "border-primary ring-2 ring-primary/30" : "border-border hover:border-primary/40"
                  }`}
                >
                  <img src={a.url} alt={`Version ${i + 1}`} className="w-full rounded" />
                  <span className="block text-center text-xs text-muted-foreground mt-1">Version {i + 1}</span>
                </button>
              ))}
            </div>
            <Button className="mt-4 w-full" onClick={utiliser} disabled={choisiId == null}>
              <Check className="h-4 w-4 mr-2" />
              Utiliser cette version
            </Button>
          </div>
        ) : null}
      </DialogContent>
    </Dialog>
  );
};

export default GenerateurBouton;
