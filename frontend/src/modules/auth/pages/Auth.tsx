import { useState } from "react";
import { Link, useSearchParams, useNavigate } from "react-router-dom";
import { Button } from "../../../components/ui/button";
import { Input } from "../../../components/ui/input";
import { Label } from "../../../components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "../../../components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../../../components/ui/tabs";
import { Checkbox } from "../../../components/ui/checkbox";
import Header from "../../../components/layout/Header";
import { Printer, Mail, Lock, Eye, EyeOff, ArrowRight } from "lucide-react";

// 👇 NOUVEAUX IMPORTS POUR LE BACKEND
import { authService } from "../services/auth.service";
import { useAuth } from "../context/AuthContext";
import type { LoginRequest, SignupRequest } from "../models/auth.model";

const Auth = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { loginGlobal } = useAuth(); // Pour mettre à jour le contexte React

  const [activeTab, setActiveTab] = useState<string>(searchParams.get("tab") === "inscription" ? "inscription" : "connexion");
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  // 👇 ÉTATS POUR STOCKER CE QUE L'UTILISATEUR TAPE
  const [loginData, setLoginData] = useState<LoginRequest>({ email: "", motDePasse: "" });
  const [signupData, setSignupData] = useState<SignupRequest>({ prenom: "", nom: "", email: "", motDePasse: "", telephone: "" });

  // 👇 ÉTATS POUR AFFICHER LES MESSAGES D'ERREUR/SUCCÈS
  const [errorMsg, setErrorMsg] = useState("");
  const [successMsg, setSuccessMsg] = useState("");

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setErrorMsg("");

    try {
      // VRAI APPEL BACKEND
      const data = await authService.login(loginData);
      loginGlobal(data.token, { id: data.id, email: data.email, role: data.role });
      navigate("/dashboard"); // Redirection après succès
    } catch (err: any) {
      setErrorMsg(err.message || "Erreur de connexion");
    } finally {
      setIsLoading(false);
    }
  };

  const handleSignup = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setErrorMsg("");
    setSuccessMsg("");

    try {
      // VRAI APPEL BACKEND
      await authService.register(signupData);
      setSuccessMsg("Compte créé avec succès ! Connectez-vous.");
      setActiveTab("connexion"); // Bascule sur l'onglet connexion
    } catch (err: any) {
      setErrorMsg(err.message || "Erreur lors de l'inscription");
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
            <Link to="/" className="inline-flex items-center gap-2 group">
              <div className="p-3 rounded-xl bg-primary/10">
                <Printer className="h-8 w-8 text-primary" />
              </div>
              <span className="font-display font-bold text-2xl text-foreground">
                PrintHub
              </span>
            </Link>
          </div>

          <Card className="border-border shadow-lg">
            <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
              <CardHeader className="pb-4">
                <TabsList className="grid w-full grid-cols-2">
                  <TabsTrigger value="connexion">Connexion</TabsTrigger>
                  <TabsTrigger value="inscription">Inscription</TabsTrigger>
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
                      <CardTitle className="text-xl">Bon retour !</CardTitle>
                      <CardDescription>Connectez-vous pour accéder à votre espace</CardDescription>
                    </div>

                    <form onSubmit={handleLogin} className="space-y-4">
                      <div className="space-y-2">
                        <Label htmlFor="login-email">Email</Label>
                        <div className="relative">
                          <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                          <Input 
                            id="login-email" type="email" placeholder="votre@email.com" className="pl-10" required
                            value={loginData.email} 
                            onChange={(e) => setLoginData({...loginData, email: e.target.value})} 
                          />
                        </div>
                      </div>

                      <div className="space-y-2">
                        <div className="flex items-center justify-between">
                          <Label htmlFor="login-password">Mot de passe</Label>
                          <Link to="/mot-de-passe-oublie" className="text-sm text-primary hover:underline">Oublié ?</Link>
                        </div>
                        <div className="relative">
                          <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                          <Input 
                            id="login-password" type={showPassword ? "text" : "password"} placeholder="••••••••" className="pl-10 pr-10" required
                            value={loginData.motDePasse} 
                            onChange={(e) => setLoginData({...loginData, motDePasse: e.target.value})} 
                          />
                          <button type="button" onClick={() => setShowPassword(!showPassword)} className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground">
                            {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                          </button>
                        </div>
                      </div>

                      <Button type="submit" className="w-full" disabled={isLoading}>
                        {isLoading ? "Connexion..." : "Se connecter"}
                        <ArrowRight className="h-4 w-4 ml-2" />
                      </Button>
                    </form>
                  </div>
                </TabsContent>

                {/* Signup Tab */}
                <TabsContent value="inscription" className="mt-0">
                  <div className="space-y-4">
                    <div className="text-center mb-6">
                      <CardTitle className="text-xl">Créer un compte</CardTitle>
                      <CardDescription>Rejoignez PrintHub pour commander vos impressions</CardDescription>
                    </div>

                    <form onSubmit={handleSignup} className="space-y-4">
                      <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-2">
                          <Label htmlFor="signup-firstname">Prénom</Label>
                          <Input 
                            id="signup-firstname" type="text" placeholder="Jean" required
                            value={signupData.prenom} 
                            onChange={(e) => setSignupData({...signupData, prenom: e.target.value})} 
                          />
                        </div>
                        <div className="space-y-2">
                          <Label htmlFor="signup-lastname">Nom</Label>
                          <Input 
                            id="signup-lastname" type="text" placeholder="Dupont" required
                            value={signupData.nom} 
                            onChange={(e) => setSignupData({...signupData, nom: e.target.value})} 
                          />
                        </div>
                      </div>

                      <div className="space-y-2">
                        <Label htmlFor="signup-email">Email</Label>
                        <div className="relative">
                          <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                          <Input 
                            id="signup-email" type="email" placeholder="votre@email.com" className="pl-10" required
                            value={signupData.email} 
                            onChange={(e) => setSignupData({...signupData, email: e.target.value})} 
                          />
                        </div>
                      </div>

                      <div className="space-y-2">
                        <Label htmlFor="signup-password">Mot de passe</Label>
                        <div className="relative">
                          <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                          <Input 
                            id="signup-password" type={showPassword ? "text" : "password"} placeholder="••••••••" className="pl-10 pr-10" required
                            value={signupData.motDePasse} 
                            onChange={(e) => setSignupData({...signupData, motDePasse: e.target.value})} 
                          />
                          <button type="button" onClick={() => setShowPassword(!showPassword)} className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground">
                            {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                          </button>
                        </div>
                      </div>

                      <Button type="submit" className="w-full" disabled={isLoading}>
                        {isLoading ? "Création..." : "Créer mon compte"}
                        <ArrowRight className="h-4 w-4 ml-2" />
                      </Button>
                    </form>
                  </div>
                </TabsContent>
              </CardContent>
            </Tabs>
          </Card>
        </div>
      </main>
    </div>
  );
};

export default Auth;