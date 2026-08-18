import { useEffect, useState } from "react";
import { Link, useSearchParams, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Button } from "../../../components/ui/button";
import { Input } from "../../../components/ui/input";
import { Label } from "../../../components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "../../../components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../../../components/ui/tabs";
import { Checkbox } from "../../../components/ui/checkbox";
import Header from "../../../components/layout/Header";
import { Mail, Lock, Eye, EyeOff, ArrowRight } from "lucide-react";

// 👇 NOUVEAUX IMPORTS POUR LE BACKEND
import { authService } from "../services/auth.service";
import { useAuth } from "../context/AuthContext";
import type { LoginRequest, SignupRequest } from "../models/auth.model";

const Auth = () => {
  const { t } = useTranslation("auth");
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const { loginGlobal } = useAuth();

  const [activeTab, setActiveTab] = useState<string>(searchParams.get("tab") === "inscription" ? "inscription" : "connexion");
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const [loginData, setLoginData] = useState<LoginRequest>({ email: "", password: "" });
  const [signupData, setSignupData] = useState<SignupRequest>({ prenom: "", nom: "", email: "", motDePasse: "", telephone: "" });
  
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  // 👇 ÉTATS POUR AFFICHER LES MESSAGES D'ERREUR/SUCCÈS
  const [errorMsg, setErrorMsg] = useState("");
  const [successMsg, setSuccessMsg] = useState("");
  
  useEffect(() => {
    const tab = searchParams.get("tab");
    if (tab === "inscription" || tab === "connexion") {
      setActiveTab(tab);
    }
  }, [searchParams]);

  /**
   * Change d'onglet en gardant l'adresse à jour : sans cela, l'URL continuait
   * d'annoncer l'onglet d'arrivée, et un rechargement ou un lien partagé
   * ramenait sur le mauvais formulaire.
   *
   * Le remplacement de l'entrée d'historique évite que le bouton « Précédent »
   * ait à défaire chaque aller-retour entre les deux onglets.
   */
  const changerOnglet = (onglet: string) => {
    setActiveTab(onglet);
    setSearchParams({ tab: onglet }, { replace: true });
  };

 const handleLogin = async (e: React.FormEvent) => {
  e.preventDefault();
  setIsLoading(true);
  setErrorMsg("");

  try {
    const data = await authService.login(loginData);
    
    // 1. On met à jour l'état global
    loginGlobal(data.token, { 
      id: data.id, 
      email: data.email, 
      role: data.role 
    });

    // 2. Redirection selon le rôle
    if (data.role === "ROLE_IMPRIMERIE") {
      navigate("/dashboard-imprimeur");
    } else if (data.role === "ROLE_ADMIN") {
      navigate("/dashboard-admin");
    } else {
      navigate("/dashboard");
    }

  } catch (err: any) {
    setErrorMsg(err.message || t("login.errors.generic"));
  } finally {
    setIsLoading(false);
  }
};

  const handleSignup = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setErrorMsg("");
    setSuccessMsg("");

    // 👇 VALIDATIONS avant d'appeler le backend
    if (!/^(?=.*[A-Za-z])(?=.*\d).{8,}$/.test(signupData.motDePasse)) {
      setErrorMsg(t("register.errors.weakPassword"));
      setIsLoading(false);
      return;
    }
    if (signupData.motDePasse !== confirmPassword) {
      setErrorMsg(t("register.errors.passwordMismatch"));
      setIsLoading(false);
      return; // On arrête la fonction ici, on n'appelle pas le backend
    }
    if (signupData.telephone && !/^\+?[0-9 ()./-]{8,20}$/.test(signupData.telephone)) {
      setErrorMsg(t("register.errors.invalidPhone"));
      setIsLoading(false);
      return;
    }

    try {
      await authService.register(signupData);
      setSuccessMsg(t("register.successMessage"));
      changerOnglet("connexion");

      // On vide les champs de mot de passe par sécurité
      setSignupData({...signupData, motDePasse: ""});
      setConfirmPassword("");
    } catch (err: any) {
      setErrorMsg(err.message || t("register.errors.generic"));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col bg-background">
      <Header />
      
      <main className="flex-1 pt-24 pb-16">
        <div className="container mx-auto px-4 max-w-md">
          {/* Logo */}
          <div className="text-center mb-8">
            <Link to="/" className="inline-flex items-center group">
              <img src="/logo.png" alt="PrintNow" className="h-12 w-auto object-contain" />
            </Link>
          </div>

          <Card className="border-border shadow-lg">
            <Tabs value={activeTab} onValueChange={changerOnglet} className="w-full">
              <CardHeader className="pb-4">
                <TabsList className="grid w-full grid-cols-2">
                  <TabsTrigger value="connexion">{t("tabs.login")}</TabsTrigger>
                  <TabsTrigger value="inscription">{t("tabs.register")}</TabsTrigger>
                </TabsList>
              </CardHeader>

              <CardContent>
                {/* AFFICHAGE DES MESSAGES D'ERREUR/SUCCÈS */}
                {errorMsg && <div className="mb-4 p-3 text-sm text-red-500 bg-red-100 rounded-md">{errorMsg}</div>}
                {successMsg && <div className="mb-4 p-3 text-sm text-green-600 bg-green-100 rounded-md">{successMsg}</div>}

                {/* Login Tab */}
                <TabsContent value="connexion" className="mt-0">
                  <div className="space-y-4">
                    <div className="text-center mb-6">
                      <CardTitle className="text-xl">{t("login.title")}</CardTitle>
                      <CardDescription>{t("login.subtitle")}</CardDescription>
                    </div>

                    <form onSubmit={handleLogin} className="space-y-4">
                      <div className="space-y-2">
                        <Label htmlFor="login-email">{t("login.emailLabel")}</Label>
                        <div className="relative">
                          <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                          <Input
                            id="login-email" type="email" placeholder={t("login.emailPlaceholder")} className="pl-10" required
                            value={loginData.email}
                            onChange={(e) => setLoginData({...loginData, email: e.target.value})}
                          />
                        </div>
                      </div>

                      <div className="space-y-2">
                        <div className="flex items-center justify-between">
                          <Label htmlFor="login-password">{t("login.passwordLabel")}</Label>
                          <Link to="/mot-de-passe-oublie" className="text-sm text-primary hover:underline">{t("login.forgotPasswordLink")}</Link>
                        </div>
                        <div className="relative">
                          <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                          <Input
                            id="login-password" type={showPassword ? "text" : "password"} placeholder={t("login.passwordPlaceholder")} className="pl-10 pr-10" required
                            value={loginData.password}
                            onChange={(e) => setLoginData({...loginData, password: e.target.value})}
                          />
                          <button type="button" onClick={() => setShowPassword(!showPassword)} className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground">
                            {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                          </button>
                        </div>
                      </div>

                      <Button type="submit" className="w-full" disabled={isLoading}>
                        {isLoading ? t("login.submitButtonLoading") : t("login.submitButton")}
                        <ArrowRight className="h-4 w-4 ml-2" />
                      </Button>
                    </form>
                  </div>
                </TabsContent>

                {/* Signup Tab */}
                <TabsContent value="inscription" className="mt-0">
                  <div className="space-y-4">
                    <div className="text-center mb-6">
                      <CardTitle className="text-xl">{t("register.title")}</CardTitle>
                      <CardDescription>{t("register.subtitle")}</CardDescription>
                    </div>

                    <form onSubmit={handleSignup} className="space-y-4">
                      <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-2">
                          <Label htmlFor="signup-firstname">{t("register.firstNameLabel")}</Label>
                          <Input
                            id="signup-firstname" type="text" placeholder={t("register.firstNamePlaceholder")} required
                            value={signupData.prenom}
                            onChange={(e) => setSignupData({...signupData, prenom: e.target.value})}
                          />
                        </div>
                        <div className="space-y-2">
                          <Label htmlFor="signup-lastname">{t("register.lastNameLabel")}</Label>
                          <Input
                            id="signup-lastname" type="text" placeholder={t("register.lastNamePlaceholder")} required
                            value={signupData.nom}
                            onChange={(e) => setSignupData({...signupData, nom: e.target.value})}
                          />
                        </div>
                      </div>

                      <div className="space-y-2">
                        <Label htmlFor="signup-email">{t("register.emailLabel")}</Label>
                        <div className="relative">
                          <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                          <Input
                            id="signup-email" type="email" placeholder={t("register.emailPlaceholder")} className="pl-10" required
                            value={signupData.email}
                            onChange={(e) => setSignupData({...signupData, email: e.target.value})}
                          />
                        </div>
                      </div>

                      <div className="space-y-2">
                        <Label htmlFor="signup-password">{t("register.passwordLabel")}</Label>
                        <div className="relative">
                          <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                          <Input
                            id="signup-password" type={showPassword ? "text" : "password"} placeholder={t("register.passwordPlaceholder")} className="pl-10 pr-10" required
                            value={signupData.motDePasse}
                            onChange={(e) => setSignupData({...signupData, motDePasse: e.target.value})}
                          />
                          <button type="button" onClick={() => setShowPassword(!showPassword)} className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground">
                            {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                          </button>
                        </div>
                        <p className="text-xs text-muted-foreground">
                          {t("register.passwordHint")}
                        </p>
                      </div>

                      <div className="space-y-2">
                        <Label htmlFor="signup-confirm-password">{t("register.confirmPasswordLabel")}</Label>
                        <div className="relative">
                          <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                          <Input
                            id="signup-confirm-password" type={showConfirmPassword ? "text" : "password"} placeholder={t("register.confirmPasswordPlaceholder")} className="pl-10 pr-10" required
                            value={confirmPassword}
                            onChange={(e) => setConfirmPassword(e.target.value)}
                          />
                          <button type="button" onClick={() => setShowConfirmPassword(!showConfirmPassword)} className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground">
                            {showConfirmPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                          </button>
                        </div>
                      </div>

                      <div className="flex items-start gap-2 pt-2">
                        <Checkbox id="terms" required className="mt-1" />
                        <Label htmlFor="terms" className="text-sm font-normal leading-relaxed">
                          {t("register.termsPrefix")}{" "}
                          <Link to="/conditions" className="text-primary hover:underline">
                            {t("register.termsLink")}
                          </Link>{" "}
                          {t("register.termsAnd")}{" "}
                          <Link to="/confidentialite" className="text-primary hover:underline">
                            {t("register.privacyLink")}
                          </Link>
                        </Label>
                      </div>

                      <Button type="submit" className="w-full" disabled={isLoading}>
                        {isLoading ? t("register.submitButtonLoading") : t("register.submitButton")}
                        <ArrowRight className="h-4 w-4 ml-2" />
                      </Button>
                    </form>
                  </div>
                </TabsContent>
              </CardContent>
            </Tabs>
          </Card>
          
          <div className="mt-8 text-center">
            <p className="text-muted-foreground text-sm">
              {t("partnerCta.text")}{" "}
              <Link to="/devenir-partenaire" className="text-primary font-medium hover:underline">
                {t("partnerCta.link")}
              </Link>
            </p>
          </div>
        </div>
      </main>
    </div>
  );
};

export default Auth;