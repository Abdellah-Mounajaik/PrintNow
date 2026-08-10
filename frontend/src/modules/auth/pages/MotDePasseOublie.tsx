import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { toast } from "@/hooks/use-toast";
import { Mail, ArrowRight, ArrowLeft, CheckCircle2, Loader2 } from "lucide-react";

import { authService } from "../services/auth.service";

const MotDePasseOublie = () => {
  const [email, setEmail] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [sent, setSent] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();

    const adresse = email.trim();
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(adresse)) {
      toast({
        title: "Email invalide",
        description: "Merci de saisir une adresse email valide.",
        variant: "destructive",
      });
      return;
    }

    setIsLoading(true);
    try {
      await authService.demanderReinitialisation(adresse);
      // Le serveur ne dit pas si l'adresse est connue : on affiche donc la même
      // confirmation dans tous les cas, sans quoi cette page révélerait qui est
      // inscrit sur PrintNow.
      setSent(true);
    } catch (err) {
      toast({
        title: "Envoi impossible",
        description: err instanceof Error ? err.message : "Réessayez dans un instant.",
        variant: "destructive",
      });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col bg-background">
      <main className="flex-1 pt-24 pb-16">
        <div className="container mx-auto px-4 max-w-md">
          <Card className="border-border shadow-lg">
            {!sent ? (
              <>
                <CardHeader className="text-center pb-4">
                  <CardTitle className="text-xl">Mot de passe oublié</CardTitle>
                  <CardDescription>
                    Saisissez votre email pour recevoir un lien de réinitialisation
                  </CardDescription>
                </CardHeader>

                <CardContent>
                  <form onSubmit={handleSubmit} className="space-y-4">
                    <div className="space-y-2">
                      <Label htmlFor="reset-email">Email</Label>
                      <div className="relative">
                        <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                        <Input
                          id="reset-email"
                          type="email"
                          placeholder="votre@email.com"
                          className="pl-10"
                          value={email}
                          onChange={(e) => setEmail(e.target.value)}
                          required
                        />
                      </div>
                    </div>

                    <Button type="submit" className="w-full" disabled={isLoading}>
                      {isLoading ? (
                        <>
                          <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                          Envoi...
                        </>
                      ) : (
                        <>
                          Envoyer le lien
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
                  <div className="mx-auto w-14 h-14 rounded-full bg-primary/10 flex items-center justify-center">
                    <CheckCircle2 className="h-7 w-7 text-primary" />
                  </div>
                  <div>
                    <CardTitle className="text-xl mb-2">Vérifiez votre boîte mail</CardTitle>
                    <CardDescription className="leading-relaxed">
                      Si un compte PrintNow est associé à
                      <br />
                      <span className="font-medium text-foreground">{email.trim()}</span>
                      <br />
                      vous y recevrez un lien de réinitialisation.
                    </CardDescription>
                  </div>
                  <p className="text-xs text-muted-foreground">
                    Pensez à vérifier vos courriers indésirables. Le lien expire dans 30 minutes.
                  </p>
                  <Button asChild className="w-full">
                    <Link to="/login">
                      Retour à la connexion
                      <ArrowRight className="h-4 w-4 ml-2" />
                    </Link>
                  </Button>
                </div>
              </CardContent>
            )}
          </Card>

          <div className="mt-8 text-center">
            <p className="text-muted-foreground text-sm">
              Pas encore de compte ?{" "}
              <Link to="/login?tab=inscription" className="text-primary font-medium hover:underline">
                Créer un compte
              </Link>
            </p>
          </div>
        </div>
      </main>
    </div>
  );
};

export default MotDePasseOublie;
