import { useEffect } from "react";
import { useLocation } from "react-router-dom";

// React Router ne réinitialise jamais le scroll lors d'un changement de route :
// si on clique un lien depuis le bas d'une page (ex: les liens du footer), la
// nouvelle page s'ouvre déjà scrollée à la même position que l'ancienne.
// Exception : si l'URL contient une ancre (ex: /#imprimeries, utilisé par le
// mail de bienvenue), on scrolle vers cet élément plutôt que vers le haut.
const ScrollToTop = () => {
  const { pathname, hash } = useLocation();

  useEffect(() => {
    if (hash) {
      const element = document.getElementById(hash.replace("#", ""));
      if (element) {
        element.scrollIntoView({ behavior: "smooth" });
        return;
      }
    }
    window.scrollTo(0, 0);
  }, [pathname, hash]);

  return null;
};

export default ScrollToTop;
