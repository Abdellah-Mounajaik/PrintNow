import { useState } from "react";
import { Sparkles, Loader2, Download, ArrowLeft } from "lucide-react";
import { Button } from "../../../components/ui/button";
import { Card, CardContent } from "../../../components/ui/card";
import { useAuth } from "../../auth/context/AuthContext";
import { studioService } from "../services/studio.service";

const EXEMPLE =
  "Je m'appelle …, développeur, à Bruxelles. Email …, tél …. Bachelier en informatique " +
  "(école, année). 1 an d'expérience chez … en Java/React. Compétences : … . Langues : …";

/**
 * Générateur de supports par IA — tranche verticale : le CV.
 *
 * Le client décrit son parcours ; l'IA le met en forme ; on affiche l'aperçu du
 * PDF produit, qu'il peut télécharger. Les gabarits Flyer/Carte, les palettes et
 * les « 3 propositions » viendront ensuite.
 */
const StudioPage = () => {
  const { token } = useAuth();
  const [brief, setBrief] = useState("");
  const [chargement, setChargement] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);
  const [apercu, setApercu] = useState<string | null>(null);
  const [propositionId, setPropositionId] = useState<number | null>(null);

  const generer = async () => {
    if (!token || brief.trim().length === 0) return;
    setChargement(true);
    setErreur(null);
    setApercu(null);
    try {
      const generation = await studioService.generer("CV", brief.trim(), token);
      const proposition = generation.propositions[0];
      if (!proposition) throw new Error("Aucun rendu n'a été produit.");
      setPropositionId(proposition.id);
      setApercu(await studioService.fichierUrl(proposition.id, "apercu", token));
    } catch (e) {
      setErreur(e instanceof Error ? e.message : "Une erreur est survenue.");
    } finally {
      setChargement(false);
    }
  };

  const telecharger = async () => {
    if (!token || propositionId == null) return;
    try {
      const url = await studioService.fichierUrl(propositionId, "pdf", token);
      const a = document.createElement("a");
      a.href = url;
      a.download = "mon-cv.pdf";
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      setErreur("Le téléchargement a échoué.");
    }
  };

  return (
    <div className="min-h-screen bg-background pt-20">
      <section className="bg-gradient-to-br from-primary to-primary/80 text-primary-foreground py-14">
        <div className="container mx-auto px-4">
          <div className="flex items-center gap-2 mb-3 text-primary-foreground/80">
            <Sparkles className="h-5 w-5" />
            <span className="text-sm font-medium">Générateur assisté par IA</span>
          </div>
          <h1 className="font-display text-3xl md:text-5xl font-bold mb-3 max-w-3xl">
            Créez votre CV en quelques mots
          </h1>
          <p className="text-primary-foreground/80 max-w-2xl">
            Décrivez votre parcours, l'IA le met en forme. Vous obtenez un PDF prêt à imprimer.
          </p>
        </div>
      </section>

      <section className="py-12">
        <div className="container mx-auto px-4 max-w-5xl grid grid-cols-1 lg:grid-cols-2 gap-8">
          {/* Saisie */}
          <div>
            <label className="block text-sm font-medium mb-2">Votre parcours</label>
            <textarea
              value={brief}
              onChange={(e) => setBrief(e.target.value)}
              placeholder={EXEMPLE}
              maxLength={1500}
              rows={12}
              className="w-full rounded-lg border border-border bg-background p-4 text-sm leading-relaxed
                         focus:outline-none focus:ring-2 focus:ring-primary/40 resize-none"
            />
            <div className="flex items-center justify-between mt-2">
              <span className="text-xs text-muted-foreground">{brief.length}/1500 — plus vous en dites, mieux c'est.</span>
            </div>
            <Button className="mt-4 w-full" onClick={generer} disabled={chargement || brief.trim().length === 0}>
              {chargement ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Génération en cours…
                </>
              ) : (
                <>
                  <Sparkles className="h-4 w-4 mr-2" />
                  Générer mon CV
                </>
              )}
            </Button>
            {erreur && <p className="mt-3 text-sm text-destructive">{erreur}</p>}
          </div>

          {/* Aperçu */}
          <div>
            <label className="block text-sm font-medium mb-2">Aperçu</label>
            <Card className="border-border/60 min-h-[400px]">
              <CardContent className="p-4 flex items-center justify-center">
                {chargement ? (
                  <div className="text-center text-muted-foreground py-20">
                    <Loader2 className="h-10 w-10 mx-auto mb-4 animate-spin opacity-60" />
                    <p className="text-sm">L'IA rédige et met en page votre CV…</p>
                  </div>
                ) : apercu ? (
                  <div className="w-full">
                    <img src={apercu} alt="Aperçu du CV" className="w-full rounded border border-border" />
                    <Button className="mt-4 w-full" variant="default" onClick={telecharger}>
                      <Download className="h-4 w-4 mr-2" />
                      Télécharger le PDF
                    </Button>
                  </div>
                ) : (
                  <div className="text-center text-muted-foreground py-20">
                    <ArrowLeft className="h-6 w-6 mx-auto mb-3 opacity-50" />
                    <p className="text-sm">Votre CV apparaîtra ici.</p>
                  </div>
                )}
              </CardContent>
            </Card>
          </div>
        </div>
      </section>
    </div>
  );
};

export default StudioPage;
