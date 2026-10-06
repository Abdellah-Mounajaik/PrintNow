import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Cookie } from "lucide-react";
import { Button } from "../ui/button";

const CLE_STOCKAGE = "cookies-info-vue";
const EVENEMENT_REAFFICHAGE = "printnow:afficher-info-cookies";

/**
 * Réaffiche le bandeau, depuis n'importe où (lien du pied de page).
 *
 * Passe par un événement du navigateur plutôt que par un contexte React : un
 * fournisseur de plus pour un seul booléen alourdirait l'arbre sans rien
 * apporter, le bandeau restant seul à connaître son état.
 */
export const afficherInfoCookies = () => {
  try {
    localStorage.removeItem(CLE_STOCKAGE);
  } catch {
    // Le bandeau se rouvre quand même : seul l'oubli du choix est perdu.
  }
  window.dispatchEvent(new Event(EVENEMENT_REAFFICHAGE));
};

/**
 * Bandeau d'information sur le stockage navigateur.
 *
 * Volontairement sans bouton « refuser » : le site n'emploie que du stockage
 * strictement nécessaire (session, droits, langue) et les cookies de sécurité
 * de Stripe sur les pages de paiement, tous dispensés de consentement. Proposer
 * un choix laisserait croire qu'il y a quelque chose à refuser, et le refus
 * reviendrait à empêcher la connexion ou le paiement.
 */
const BandeauCookies = () => {
  const { t } = useTranslation("common");

  // Le navigateur peut refuser le stockage (navigation privée, réglages
  // stricts) : le bandeau s'affiche alors à chaque visite, ce qui reste
  // préférable à une page blanche.
  const [masque, setMasque] = useState(() => {
    try {
      return localStorage.getItem(CLE_STOCKAGE) === "1";
    } catch {
      return false;
    }
  });

  useEffect(() => {
    const rouvrir = () => setMasque(false);
    window.addEventListener(EVENEMENT_REAFFICHAGE, rouvrir);
    return () => window.removeEventListener(EVENEMENT_REAFFICHAGE, rouvrir);
  }, []);

  if (masque) return null;

  const accepter = () => {
    try {
      localStorage.setItem(CLE_STOCKAGE, "1");
    } catch {
      // Tant pis : le bandeau reparaîtra, sans rien casser.
    }
    setMasque(true);
  };

  return (
    <div
      role="region"
      aria-label={t("cookies.ariaLabel")}
      className="fixed bottom-4 left-4 right-4 z-50 sm:right-auto sm:max-w-sm"
    >
      <div className="flex items-start gap-3 rounded-xl border border-border bg-background/95 p-3.5
                      shadow-lg backdrop-blur supports-[backdrop-filter]:bg-background/85">
        <Cookie className="mt-0.5 h-4 w-4 shrink-0 text-muted-foreground" />
        <div className="min-w-0 space-y-2">
          <p className="text-xs leading-relaxed text-muted-foreground">
            {t("cookies.message")}{" "}
            <Link to="/confidentialite#cookies" className="underline underline-offset-2 hover:text-foreground">
              {t("cookies.learnMore")}
            </Link>
          </p>
          <Button size="sm" variant="secondary" onClick={accepter} className="h-7 px-3 text-xs">
            {t("cookies.acknowledge")}
          </Button>
        </div>
      </div>
    </div>
  );
};

export default BandeauCookies;
