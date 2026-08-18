import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import LanguageDetector from "i18next-browser-languagedetector";

import commonFr from "./locales/fr/common.json";
import headerFr from "./locales/fr/header.json";
import footerFr from "./locales/fr/footer.json";
import homeFr from "./locales/fr/home.json";
import imprimeriesFr from "./locales/fr/imprimeries.json";
import printShopDetailFr from "./locales/fr/printShopDetail.json";
import devenirPartenaireFr from "./locales/fr/devenirPartenaire.json";
import orderFr from "./locales/fr/order.json";
import faqFr from "./locales/fr/faq.json";
import contactFr from "./locales/fr/contact.json";
import legalFr from "./locales/fr/legal.json";
import authFr from "./locales/fr/auth.json";
import dashboardClientFr from "./locales/fr/dashboardClient.json";
import dashboardAdminFr from "./locales/fr/dashboardAdmin.json";
import dashboardImprimeurFr from "./locales/fr/dashboardImprimeur.json";

import commonEn from "./locales/en/common.json";
import headerEn from "./locales/en/header.json";
import footerEn from "./locales/en/footer.json";
import homeEn from "./locales/en/home.json";
import imprimeriesEn from "./locales/en/imprimeries.json";
import printShopDetailEn from "./locales/en/printShopDetail.json";
import devenirPartenaireEn from "./locales/en/devenirPartenaire.json";
import orderEn from "./locales/en/order.json";
import faqEn from "./locales/en/faq.json";
import contactEn from "./locales/en/contact.json";
import legalEn from "./locales/en/legal.json";
import authEn from "./locales/en/auth.json";
import dashboardClientEn from "./locales/en/dashboardClient.json";
import dashboardAdminEn from "./locales/en/dashboardAdmin.json";
import dashboardImprimeurEn from "./locales/en/dashboardImprimeur.json";

// Le sélecteur de langue propose aussi "NL", mais aucune ressource n'existe
// encore pour ce namespace : i18next retombe alors silencieusement sur
// fallbackLng (fr) tant que les traductions néerlandaises ne sont pas ajoutées.
i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      fr: {
        common: commonFr,
        header: headerFr,
        footer: footerFr,
        home: homeFr,
        imprimeries: imprimeriesFr,
        printShopDetail: printShopDetailFr,
        devenirPartenaire: devenirPartenaireFr,
        order: orderFr,
        faq: faqFr,
        contact: contactFr,
        legal: legalFr,
        auth: authFr,
        dashboardClient: dashboardClientFr,
        dashboardAdmin: dashboardAdminFr,
        dashboardImprimeur: dashboardImprimeurFr,
      },
      en: {
        common: commonEn,
        header: headerEn,
        footer: footerEn,
        home: homeEn,
        imprimeries: imprimeriesEn,
        printShopDetail: printShopDetailEn,
        devenirPartenaire: devenirPartenaireEn,
        order: orderEn,
        faq: faqEn,
        contact: contactEn,
        legal: legalEn,
        auth: authEn,
        dashboardClient: dashboardClientEn,
        dashboardAdmin: dashboardAdminEn,
        dashboardImprimeur: dashboardImprimeurEn,
      },
    },
    fallbackLng: "fr",
    supportedLngs: ["fr", "en"],
    ns: [
      "common",
      "header",
      "footer",
      "home",
      "imprimeries",
      "printShopDetail",
      "devenirPartenaire",
      "order",
      "faq",
      "contact",
      "legal",
      "auth",
      "dashboardClient",
      "dashboardAdmin",
      "dashboardImprimeur",
    ],
    defaultNS: "common",
    interpolation: {
      escapeValue: false,
    },
    detection: {
      order: ["localStorage", "navigator"],
      caches: ["localStorage"],
      lookupLocalStorage: "printnow_language",
    },
  });

export default i18n;
