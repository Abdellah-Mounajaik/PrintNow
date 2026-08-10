import { Link } from "react-router-dom";
import { useAuth } from "../modules/auth/context/AuthContext";

/** L'espace correspondant au rôle, pour renvoyer l'utilisateur là où il a le droit d'être. */
const espaceDuRole = (role?: string) => {
  if (role === "ROLE_ADMIN") return "/dashboard-admin";
  if (role === "ROLE_IMPRIMERIE") return "/dashboard-imprimeur";
  return "/dashboard";
};

const Forbidden = () => {
  const { isAuthenticated, user } = useAuth();

  return (
    <div className="flex min-h-screen items-center justify-center bg-muted px-4 pt-24 pb-16">
      <div className="text-center">
        <h1 className="mb-4 text-7xl font-bold text-primary">403</h1>
        <p className="mb-2 text-2xl font-semibold">Accès interdit</p>
        <p className="mb-6 text-muted-foreground">
          Vous n'avez pas les permissions nécessaires pour accéder à cette page.
        </p>
        <div className="flex flex-col sm:flex-row items-center justify-center gap-3">
          <Link
            to="/"
            className="rounded-lg bg-primary px-5 py-2.5 text-primary-foreground font-medium hover:bg-primary/90 transition-colors"
          >
            Retour à l'accueil
          </Link>
          {/* Proposer « Se connecter » à quelqu'un déjà connecté n'aurait pas de
              sens : c'est son espace qu'il cherche, pas la page de connexion. */}
          {isAuthenticated ? (
            <Link
              to={espaceDuRole(user?.role)}
              className="rounded-lg border border-border px-5 py-2.5 font-medium hover:bg-accent transition-colors"
            >
              Retour à mon espace
            </Link>
          ) : (
            <Link
              to="/login?tab=connexion"
              className="rounded-lg border border-border px-5 py-2.5 font-medium hover:bg-accent transition-colors"
            >
              Se connecter
            </Link>
          )}
        </div>
      </div>
    </div>
  );
};

export default Forbidden;
