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

import commonNl from "./locales/nl/common.json";
import headerNl from "./locales/nl/header.json";
import footerNl from "./locales/nl/footer.json";
import homeNl from "./locales/nl/home.json";
import imprimeriesNl from "./locales/nl/imprimeries.json";
import printShopDetailNl from "./locales/nl/printShopDetail.json";
import devenirPartenaireNl from "./locales/nl/devenirPartenaire.json";
import orderNl from "./locales/nl/order.json";
import faqNl from "./locales/nl/faq.json";
import contactNl from "./locales/nl/contact.json";
import legalNl from "./locales/nl/legal.json";
import authNl from "./locales/nl/auth.json";
import dashboardClientNl from "./locales/nl/dashboardClient.json";
import dashboardAdminNl from "./locales/nl/dashboardAdmin.json";
import dashboardImprimeurNl from "./locales/nl/dashboardImprimeur.json";

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
      nl: {
        common: commonNl,
        header: headerNl,
        footer: footerNl,
        home: homeNl,
        imprimeries: imprimeriesNl,
        printShopDetail: printShopDetailNl,
        devenirPartenaire: devenirPartenaireNl,
        order: orderNl,
        faq: faqNl,
        contact: contactNl,
        legal: legalNl,
        auth: authNl,
        dashboardClient: dashboardClientNl,
        dashboardAdmin: dashboardAdminNl,
        dashboardImprimeur: dashboardImprimeurNl,
      },
    },
    fallbackLng: "fr",
    supportedLngs: ["fr", "en", "nl"],
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
