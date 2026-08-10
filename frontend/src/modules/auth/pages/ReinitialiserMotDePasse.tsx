import { useEffect, useState, type FormEvent } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { toast } from "@/hooks/use-toast";
import { Lock, Eye, EyeOff, ArrowRight, ArrowLeft, AlertCircle, Loader2 } from "lucide-react";

import { authService } from "../services/auth.service";

// Mêmes exigences qu'à l'inscription et que côté serveur.
const MOT_DE_PASSE_VALIDE = /^(?=.*[A-Za-z])(?=.*\d).{8,}$/;

const ReinitialiserMotDePasse = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const jeton = searchParams.get("jeton") ?? "";

  const [lienValide, setLienValide] = useState<boolean | null>(null);
  const [motDePasse, setMotDePasse] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmation, setShowConfirmation] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  // Le lien est éprouvé avant d'afficher le formulaire : inutile de faire
  // choisir un mot de passe pour annoncer ensuite que le lien a expiré.
  useEffect(() => {
    if (!jeton) {
      setLienValide(false);
      return;
    }
    authService.verifierLienReinitialisation(jeton)
      .then(setLienValide)
      .catch(() => setLienValide(false));
  }, [jeton]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();

    if (!MOT_DE_PASSE_VALIDE.test(motDePasse)) {
      toast({
        title: "Mot de passe trop faible",
        description: "Le mot de passe doit contenir au moins 8 caractères, avec des lettres et des chiffres.",
        variant: "destructive",
      });
      return;
    }

    if (motDePasse !== confirmation) {
      toast({
        title: "Les mots de passe ne correspondent pas",
        description: "Saisissez deux fois le même mot de passe.",
        variant: "destructive",
      });
      return;
    }

    setIsLoading(true);
    try {
      await authService.reinitialiserMotDePasse(jeton, motDePasse);
      toast({
        title: "Mot de passe modifié",
        description: "Vous pouvez maintenant vous connecter avec votre nouveau mot de passe.",
      });
      navigate("/login");
    } catch (err) {
      toast({
        title: "Réinitialisation impossible",
        description: err instanceof Error ? err.message : "Réessayez dans un instant.",
        variant: "destructive",
      });
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col bg-background">
      <main className="flex-1 pt-24 pb-16">
        <div className="container mx-auto px-4 max-w-md">
          <Card className="border-border shadow-lg">
            {lienValide === null ? (
              <CardContent className="pt-6">
                <div className="text-center py-8 text-muted-foreground">
                  <Loader2 className="h-6 w-6 mx-auto mb-3 animate-spin" />
                  Vérification du lien...
                </div>
              </CardContent>
            ) : lienValide ? (
              <>
                <CardHeader className="text-center pb-4">
                  <CardTitle className="text-xl">Nouveau mot de passe</CardTitle>
                  <CardDescription>
                    Choisissez le mot de passe qui protégera votre compte PrintNow
                  </CardDescription>
                </CardHeader>

                <CardContent>
                  <form onSubmit={handleSubmit} className="space-y-4">
                    <div className="space-y-2">
                      <Label htmlFor="nouveau-mdp">Nouveau mot de passe</Label>
                      <div className="relative">
                        <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                        <Input
                          id="nouveau-mdp"
                          type={showPassword ? "text" : "password"}
                          placeholder="••••••••"
                          className="pl-10 pr-10"
                          value={motDePasse}
                          onChange={(e) => setMotDePasse(e.target.value)}
                          required
                        />
                        <button
                          type="button"
                          onClick={() => setShowPassword((v) => !v)}
                          aria-label={showPassword ? "Masquer le mot de passe" : "Afficher le mot de passe"}
                          className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                        >
                          {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                        </button>
                      </div>
                      <p className="text-xs text-muted-foreground">
                        Minimum 8 caractères avec lettres et chiffres
                      </p>
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="confirmation-mdp">Confirmer le mot de passe</Label>
                      <div className="relative">
                        <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                        <Input
                          id="confirmation-mdp"
                          type={showConfirmation ? "text" : "password"}
                          placeholder="••••••••"
                          className="pl-10 pr-10"
                          value={confirmation}
                          onChange={(e) => setConfirmation(e.target.value)}
                          required
                        />
                        <button
                          type="button"
                          onClick={() => setShowConfirmation((v) => !v)}
                          aria-label={showConfirmation ? "Masquer le mot de passe" : "Afficher le mot de passe"}
                          className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                        >
                          {showConfirmation ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                        </button>
                      </div>
                    </div>

                    <Button type="submit" className="w-full" disabled={isLoading}>
                      {isLoading ? (
                        <>
                          <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                          Modification...
                        </>
                      ) : (
                        <>
                          Changer mon mot de passe
                          <ArrowRight className="h-4 w-4 ml-2" />
                        </>
                      )}
                    </Button>
                  </form>

                  <div className="mt-6 text-center">
                    <Link
                      to="/login"
                      className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground"
                    >
                      <ArrowLeft className="h-3.5 w-3.5" />
                      Retour à la connexion
                    </Link>
                  </div>
                </CardContent>
              </>
            ) : (
              <CardContent className="pt-6">
                <div className="text-center space-y-4">
                  <div className="mx-auto w-14 h-14 rounded-full bg-destructive/10 flex items-center justify-center">
                    <AlertCircle className="h-7 w-7 text-destructive" />
                  </div>
                  <div>
                    <CardTitle className="text-xl mb-2">Lien expiré</CardTitle>
                    <CardDescription className="leading-relaxed">
                      Ce lien de réinitialisation n'est plus valable. Les liens expirent
                      au bout de 30 minutes et ne peuvent servir qu'une seule fois.
                    </CardDescription>
                  </div>
                  <Button asChild className="w-full">
                    <Link to="/mot-de-passe-oublie">
                      Demander un nouveau lien
                      <ArrowRight className="h-4 w-4 ml-2" />
                    </Link>
                  </Button>
                  <Link
                    to="/login"
                    className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground"
                  >
                    <ArrowLeft className="h-3.5 w-3.5" />
                    Retour à la connexion
                  </Link>
                </div>
              </CardContent>
            )}
          </Card>
        </div>
      </main>
    </div>
  );
};

export default ReinitialiserMotDePasse;
