import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { toast } from "@/hooks/use-toast";
import { Mail, ArrowRight, ArrowLeft, CheckCircle2, Loader2 } from "lucide-react";

import { authService } from "../services/auth.service";

const MotDePasseOublie = () => {
  const { t } = useTranslation("auth");
  const [email, setEmail] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [sent, setSent] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();

    const adresse = email.trim();
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(adresse)) {
      toast({
        title: t("forgotPassword.errors.invalidEmailTitle"),
        description: t("forgotPassword.errors.invalidEmailDescription"),
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
        title: t("forgotPassword.errors.sendFailedTitle"),
        description: err instanceof Error ? err.message : t("forgotPassword.errors.retryLater"),
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
                  <CardTitle className="text-xl">{t("forgotPassword.title")}</CardTitle>
                  <CardDescription>
                    {t("forgotPassword.subtitle")}
                  </CardDescription>
                </CardHeader>

                <CardContent>
                  <form onSubmit={handleSubmit} className="space-y-4">
                    <div className="space-y-2">
                      <Label htmlFor="reset-email">{t("forgotPassword.emailLabel")}</Label>
                      <div className="relative">
                        <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                        <Input
                          id="reset-email"
                          type="email"
                          placeholder={t("forgotPassword.emailPlaceholder")}
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
                          {t("forgotPassword.submitButtonLoading")}
                        </>
                      ) : (
                        <>
                          {t("forgotPassword.submitButton")}
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
                      {t("forgotPassword.backToLogin")}
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
                    <CardTitle className="text-xl mb-2">{t("forgotPassword.sent.title")}</CardTitle>
                    <CardDescription className="leading-relaxed">
                      {t("forgotPassword.sent.descriptionBefore")}
                      <br />
                      <span className="font-medium text-foreground">{email.trim()}</span>
                      <br />
                      {t("forgotPassword.sent.descriptionAfter")}
                    </CardDescription>
                  </div>
                  <p className="text-xs text-muted-foreground">
                    {t("forgotPassword.sent.hint")}
                  </p>
                  <Button asChild className="w-full">
                    <Link to="/login">
                      {t("forgotPassword.sent.backToLogin")}
                      <ArrowRight className="h-4 w-4 ml-2" />
                    </Link>
                  </Button>
                </div>
              </CardContent>
            )}
          </Card>

          <div className="mt-8 text-center">
            <p className="text-muted-foreground text-sm">
              {t("forgotPassword.noAccountText")}{" "}
              <Link to="/login?tab=inscription" className="text-primary font-medium hover:underline">
                {t("forgotPassword.noAccountLink")}
              </Link>
            </p>
          </div>
        </div>
      </main>
    </div>
  );
};

export default MotDePasseOublie;
