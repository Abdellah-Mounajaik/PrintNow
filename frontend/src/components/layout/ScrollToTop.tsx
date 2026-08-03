import { useEffect } from "react";
import { useLocation } from "react-router-dom";

// React Router ne réinitialise jamais le scroll lors d'un changement de route :
// si on clique un lien depuis le bas d'une page (ex: les liens du footer), la
// nouvelle page s'ouvre déjà scrollée à la même position que l'ancienne.
const ScrollToTop = () => {
  const { pathname } = useLocation();

  useEffect(() => {
    window.scrollTo(0, 0);
  }, [pathname]);

  return null;
};

export default ScrollToTop;
