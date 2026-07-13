import { useEffect, useState } from "react";
import { Star, Loader2 } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "../../../components/ui/card";
import { Button } from "../../../components/ui/button";
import { Textarea } from "../../../components/ui/textarea";
import { toast } from "../../../hooks/use-toast";
import { avisService, type AvisImprimerie } from "../services/avisService.service";

// Affichage d'une note en étoiles (lecture seule)
const StarDisplay = ({ note, size = 16 }: { note: number; size?: number }) => (
  <div className="flex items-center gap-0.5">
    {[1, 2, 3, 4, 5].map((n) => (
      <Star
        key={n}
        style={{ width: size, height: size }}
        className={n <= Math.round(note) ? "fill-amber-400 text-amber-400" : "text-muted-foreground/30"}
      />
    ))}
  </div>
);

// Sélecteur d'étoiles interactif (formulaire)
const StarInput = ({ value, onChange }: { value: number; onChange: (n: number) => void }) => {
  const [hover, setHover] = useState(0);
  return (
    <div className="flex items-center gap-1">
      {[1, 2, 3, 4, 5].map((n) => (
        <button
          key={n}
          type="button"
          onClick={() => onChange(n)}
          onMouseEnter={() => setHover(n)}
          onMouseLeave={() => setHover(0)}
          className="transition-transform hover:scale-110"
        >
          <Star
            className={`h-7 w-7 ${n <= (hover || value) ? "fill-amber-400 text-amber-400" : "text-muted-foreground/30"}`}
          />
        </button>
      ))}
    </div>
  );
};

interface Props {
  imprimerieId: number;
  token: string | null;
}

const AvisSection = ({ imprimerieId, token }: Props) => {
  const [data, setData] = useState<AvisImprimerie | null>(null);
  const [loading, setLoading] = useState(true);
  const [note, setNote] = useState(0);
  const [commentaire, setCommentaire] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const charger = () => {
    avisService.getAvisImprimerie(imprimerieId, token)
      .then(setData)
      .catch(() => setData(null))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    charger();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [imprimerieId, token]);

  const handleSubmit = async () => {
    if (note < 1) {
      toast({ title: "Note requise", description: "Sélectionnez une note de 1 à 5 étoiles.", variant: "destructive" });
      return;
    }
    if (!token) return;
    setSubmitting(true);
    try {
      await avisService.creerAvis({ imprimerieId, note, commentaire }, token);
      toast({ title: "Merci pour votre avis !" });
      setNote(0);
      setCommentaire("");
      charger();
    } catch (e) {
      toast({ title: "Erreur", description: (e as Error).message, variant: "destructive" });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Card className="shadow-card">
      <CardHeader>
        <CardTitle className="font-display text-xl flex items-center justify-between">
          <span>Avis clients</span>
          {data && data.nombreAvis > 0 && data.noteMoyenne != null && (
            <span className="flex items-center gap-2 text-base font-normal">
              <StarDisplay note={data.noteMoyenne} size={18} />
              <span className="font-semibold">{data.noteMoyenne.toFixed(1)}</span>
              <span className="text-muted-foreground text-sm">({data.nombreAvis})</span>
            </span>
          )}
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-6">
        {loading ? (
          <div className="flex justify-center py-8"><Loader2 className="h-6 w-6 animate-spin text-primary" /></div>
        ) : (
          <>
            {/* Formulaire — visible seulement si le client peut noter */}
            {data?.peutNoter && (
              <div className="p-4 rounded-lg border bg-muted/30 space-y-3">
                <p className="font-medium text-sm">Laissez votre avis</p>
                <StarInput value={note} onChange={setNote} />
                <Textarea
                  placeholder="Partagez votre expérience (optionnel)…"
                  value={commentaire}
                  onChange={(e) => setCommentaire(e.target.value)}
                  rows={3}
                />
                <Button onClick={handleSubmit} disabled={submitting}>
                  {submitting ? <Loader2 className="h-4 w-4 mr-2 animate-spin" /> : null}
                  Publier mon avis
                </Button>
              </div>
            )}

            {data?.dejaNote && (
              <p className="text-sm text-muted-foreground italic">Vous avez déjà laissé un avis pour cette imprimerie.</p>
            )}

            {/* Liste des avis */}
            {!data || data.avis.length === 0 ? (
              <p className="text-sm text-muted-foreground text-center py-6">
                Aucun avis pour le moment. Soyez le premier à donner votre avis après une commande !
              </p>
            ) : (
              <div className="space-y-4">
                {data.avis.map((a) => (
                  <div key={a.id} className="border-b last:border-b-0 pb-4 last:pb-0">
                    <div className="flex items-center justify-between mb-1">
                      <span className="font-medium text-sm">{a.nomClient}</span>
                      <span className="text-xs text-muted-foreground">
                        {new Date(a.dateCreation).toLocaleDateString("fr-BE")}
                      </span>
                    </div>
                    <StarDisplay note={a.note} />
                    {a.commentaire && <p className="text-sm text-muted-foreground mt-1.5">{a.commentaire}</p>}
                  </div>
                ))}
              </div>
            )}
          </>
        )}
      </CardContent>
    </Card>
  );
};

export default AvisSection;
