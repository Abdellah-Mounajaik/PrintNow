import { useState, useEffect } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";

import { Button } from "../ui/button"; 
import {
  Menu,
  X,
  User,
  LogIn,
  LogOut,
  Globe,
  LayoutDashboard
} from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "../ui/dropdown-menu";

// 👇 IMPORT DE NOTRE CONTEXTE D'AUTHENTIFICATION
import { useAuth } from "../../modules/auth/context/AuthContext";

const Header = () => {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [currentLang, setCurrentLang] = useState("FR");
  const [isScrolled, setIsScrolled] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();

  // 👇 RÉCUPÉRATION DE L'ÉTAT DE CONNEXION
  const { isAuthenticated, logoutGlobal, user } = useAuth();
  const dashboardPath = user?.role === "ROLE_IMPRIMERIE"
    ? "/dashboard-imprimeur"
    : user?.role === "ROLE_ADMIN"
    ? "/dashboard-admin"
    : "/dashboard";

  const languages = [
    { code: "FR", label: "Français" },
    { code: "EN", label: "English" },
    { code: "NL", label: "Nederlands" },
  ];

  const isHome = location.pathname === "/";

  useEffect(() => {
    // Sur l'accueil, la navbar ne devient opaque qu'une fois la bannière
    // dépassée (pas après 50px), pour ne pas casser l'effet "transparente sur
    // fond bleu" en plein milieu de la bannière.
    const handleScroll = () => {
      const heroHeight = isHome ? document.getElementById("hero")?.offsetHeight ?? 0 : 0;
      const threshold = isHome ? Math.max(heroHeight - 80, 50) : 50;
      setIsScrolled(window.scrollY > threshold);
    };
    handleScroll();
    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, [isHome]);

  const handleLogout = () => {
    logoutGlobal();
    setIsMenuOpen(false);
    navigate("/"); // Redirige vers l'accueil après déconnexion
  };

  const showSolidBg = !isHome || isScrolled;
  const useLight = isHome && !isScrolled;

  return (
    <header className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${
      showSolidBg ? "bg-card/95 backdrop-blur-md border-b border-border shadow-sm" : "bg-transparent"
    }`}>
      <div className="container mx-auto px-4">
        <div className="flex items-center justify-between h-16 md:h-20">
          {/* Logo */}
          <Link
            to="/"
            className="flex items-center group"
          >
            <img
              src={useLight ? "/logo-white.png" : "/logo.png"}
              alt="PrintNow"
              className="h-9 md:h-11 w-auto object-contain"
            />
          </Link>

          {/* Desktop Navigation */}
          <nav className="hidden md:flex items-center gap-6">
            <Link 
              to="/imprimeries" 
              className={`font-medium transition-colors hover:opacity-80 ${
                useLight ? "text-primary-foreground/80 hover:text-primary-foreground" : "text-muted-foreground hover:text-foreground"
              }`}
            >
              Imprimeries
            </Link>
            <Link
              to="/devenir-partenaire"
              className={`font-medium transition-colors hover:opacity-80 ${
                useLight ? "text-primary-foreground/80 hover:text-primary-foreground" : "text-muted-foreground hover:text-foreground"
              }`}
            >
              Devenir partenaire
            </Link>
            <Link
              to="/faq"
              className={`font-medium transition-colors hover:opacity-80 ${
                useLight ? "text-primary-foreground/80 hover:text-primary-foreground" : "text-muted-foreground hover:text-foreground"
              }`}
            >
              FAQ
            </Link>
          </nav>

          {/* Right Actions */}
          <div className="flex items-center gap-3">
            {/* Language Selector */}
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button 
                  variant="ghost" 
                  size="sm"
                  className={useLight ? "text-primary-foreground hover:bg-primary-foreground/10" : ""}
                >
                  <Globe className="h-4 w-4" />
                  <span className="hidden sm:inline ml-2">{currentLang}</span>
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                {languages.map((lang) => (
                  <DropdownMenuItem 
                    key={lang.code}
                    onClick={() => setCurrentLang(lang.code)}
                    className={currentLang === lang.code ? "bg-accent" : ""}
                  >
                    {lang.label}
                  </DropdownMenuItem>
                ))}
              </DropdownMenuContent>
            </DropdownMenu>

            {/* Auth Buttons - Desktop */}
            <div className="hidden md:flex items-center gap-2">
              {/* 👇 AFFICHAGE CONDITIONNEL SELON LA CONNEXION */}
              {isAuthenticated ? (
                <>
                  <Button variant="outline" size="sm" asChild>
                    <Link to={dashboardPath}>
                      <LayoutDashboard className="h-4 w-4 mr-2" />
                      Mon espace
                    </Link>
                  </Button>
                  <Button variant="destructive" size="sm" onClick={handleLogout}>
                    <LogOut className="h-4 w-4 mr-2" />
                    Déconnexion
                  </Button>
                </>
              ) : (
                <>
                  <Button variant="outline" size="sm" asChild>
                    {/* 👇 Redirige vers /login avec paramètre */}
                    <Link to="/login?tab=connexion">
                      <LogIn className="h-4 w-4 mr-2" />
                      Connexion
                    </Link>
                  </Button>
                  <Button variant={useLight ? "secondary" : "default"} size="sm" asChild>
                    {/* 👇 Redirige vers /login avec paramètre inscription */}
                    <Link to="/login?tab=inscription">
                      <User className="h-4 w-4 mr-2" />
                      Inscription
                    </Link>
                  </Button>
                </>
              )}
            </div>

            {/* Mobile Menu Button */}
            <Button
              variant="ghost"
              size="icon"
              className={`md:hidden ${useLight ? "text-primary-foreground hover:bg-primary-foreground/10" : ""}`}
              onClick={() => setIsMenuOpen(!isMenuOpen)}
            >
              {isMenuOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
            </Button>
          </div>
        </div>

        {/* Mobile Menu */}
        {isMenuOpen && (
          <div className="md:hidden absolute top-full left-0 right-0 bg-card border-b border-border shadow-lg animate-fade-in">
            <nav className="container mx-auto px-4 py-4 flex flex-col gap-3">
              <Link to="/imprimeries" className="py-2 text-foreground font-medium" onClick={() => setIsMenuOpen(false)}>
                Imprimeries
              </Link>
              <Link to="/devenir-partenaire" className="py-2 text-foreground font-medium" onClick={() => setIsMenuOpen(false)}>
                Devenir partenaire
              </Link>
              <Link to="/faq" className="py-2 text-foreground font-medium" onClick={() => setIsMenuOpen(false)}>
                FAQ
              </Link>
              <hr className="border-border" />
              
              <div className="flex gap-2">
                {/* 👇 AFFICHAGE CONDITIONNEL MOBILE */}
                {isAuthenticated ? (
                  <>
                    <Button variant="outline" className="flex-1" asChild>
                      <Link to={dashboardPath} onClick={() => setIsMenuOpen(false)}>Mon espace</Link>
                    </Button>
                    <Button variant="destructive" className="flex-1" onClick={handleLogout}>
                      Déconnexion
                    </Button>
                  </>
                ) : (
                  <>
                    <Button variant="outline" className="flex-1" asChild>
                      <Link to="/login?tab=connexion" onClick={() => setIsMenuOpen(false)}>Connexion</Link>
                    </Button>
                    <Button className="flex-1" asChild>
                      <Link to="/login?tab=inscription" onClick={() => setIsMenuOpen(false)}>Inscription</Link>
                    </Button>
                  </>
                )}
              </div>
            </nav>
          </div>
        )}
      </div>
    </header>
  );
};

export default Header;