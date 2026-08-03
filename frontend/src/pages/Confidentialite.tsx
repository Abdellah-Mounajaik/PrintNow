import { Link } from "react-router-dom";
import { Card, CardContent } from "../components/ui/card";
import { Button } from "../components/ui/button";
import { Shield, ArrowRight, Mail, CheckCircle2 } from "lucide-react";

const privacySections = [
  {
    title: "Qui est le responsable du traitement ?",
    content: [
      "Le responsable du traitement des données personnelles est PrintNow, dont le siège social est situé Rue de l'Imprimerie 42, 1000 Bruxelles, Belgique. Pour toute question relative à la protection de vos données, vous pouvez contacter notre équipe privacy à contact@printnow.be.",
    ],
  },
  {
    title: "Quelles données collectons-nous ?",
    content: [
      "Nous collectons uniquement les données nécessaires au fonctionnement de la plateforme : identité, adresse email, numéro de téléphone, adresse postale, données de paiement (via un prestataire sécurisé), historique des commandes, fichiers PDF transmis pour impression, et données de navigation.",
    ],
  },
  {
    title: "Pourquoi utilisons-nous vos données ?",
    content: [
      "Vos données sont utilisées pour : créer et gérer votre compte, traiter vos commandes, assurer la livraison ou le retrait, communiquer avec vous sur votre commande, améliorer nos services, prévenir la fraude et respecter nos obligations légales.",
    ],
  },
  {
    title: "Qui a accès à vos données ?",
    content: [
      "Vos données sont accessibles à PrintNow et, lorsque cela est nécessaire à la réalisation de votre commande, à l'imprimerie partenaire que vous avez choisie. Nous ne vendons jamais vos données à des tiers. Les fichiers que vous téléversez sont destinés à l'impression et ne sont conservés que le temps nécessaire.",
    ],
  },
  {
    title: "Sécurité des données",
    content: [
      "Nous mettons en œuvre des mesures techniques et organisationnelles pour protéger vos données contre la perte, l'accès non autorisé ou la divulgation. Les paiements sont traités par des prestataires certifiés et les communications sont sécurisées.",
    ],
  },
  {
    title: "Cookies et technologies similaires",
    content: [
      "PrintNow utilise des cookies essentiels au fonctionnement du site et, avec votre consentement, des cookies d'analyse pour comprendre l'utilisation de la plateforme. Vous pouvez gérer vos préférences depuis les paramètres de votre navigateur.",
    ],
  },
  {
    title: "Vos droits en tant qu'utilisateur",
    content: [
      "Conformément au RGPD, vous disposez des droits suivants : droit d'accès, de rectification, d'effacement, de limitation du traitement, d'opposition, de portabilité et de retrait de votre consentement. Pour exercer ces droits, envoyez-nous une demande à contact@printnow.be avec une preuve d'identité.",
    ],
  },
  {
    title: "Durée de conservation",
    content: [
      "Vos données de compte sont conservées tant que votre compte est actif. Les données relatives aux commandes sont conservées pendant la durée légale de conservation des documents comptables. Les fichiers PDF sont supprimés après réalisation de la commande, sauf obligation légale contraire.",
    ],
  },
  {
    title: "Modifications de la politique",
    content: [
      "Cette politique de confidentialité peut être mise à jour. Nous vous invitons à la consulter régulièrement.",
    ],
  },
];

const rights = [
  "Droit d'accès à vos données",
  "Droit de rectification",
  "Droit à l'effacement",
  "Droit à la limitation du traitement",
  "Droit d'opposition",
  "Droit à la portabilité",
  "Droit de retirer votre consentement",
];

const Confidentialite = () => {
  return (
    <div className="min-h-screen flex flex-col bg-background">
      <main className="flex-1 pt-20">
        {/* Hero */}
        <section className="bg-gradient-to-br from-primary to-primary/80 text-primary-foreground py-16">
          <div className="container mx-auto px-4">
            <div className="flex items-center gap-2 mb-3 text-primary-foreground/80">
              <Shield className="h-5 w-5" />
              <span className="text-sm font-medium">Protection des données</span>
            </div>
            <h1 className="font-display text-3xl md:text-5xl font-bold mb-4 max-w-3xl">
              Confidentialité & RGPD
            </h1>
          </div>
        </section>

        {/* Content */}
        <section className="py-16">
          <div className="container mx-auto px-4 max-w-4xl">
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              <div className="lg:col-span-2 space-y-6">
                {privacySections.map((section) => (
                  <Card key={section.title} className="border-border/50">
                    <CardContent className="p-6 md:p-8">
                      <h2 className="font-display text-xl font-semibold text-foreground mb-4">
                        {section.title}
                      </h2>
                      <div className="space-y-2">
                        {section.content.map((paragraph, index) => (
                          <p key={index} className="text-muted-foreground leading-relaxed">
                            {paragraph}
                          </p>
                        ))}
                      </div>
                    </CardContent>
                  </Card>
                ))}
              </div>

              <div className="lg:col-span-1">
                <Card className="border-border/50 sticky top-24">
                  <CardContent className="p-6">
                    <h3 className="font-display text-lg font-semibold text-foreground mb-4">
                      Vos droits RGPD
                    </h3>
                    <ul className="space-y-3 mb-6">
                      {rights.map((right) => (
                        <li key={right} className="flex items-start gap-2 text-sm">
                          <CheckCircle2 className="h-4 w-4 text-success shrink-0 mt-0.5" />
                          <span className="text-muted-foreground">{right}</span>
                        </li>
                      ))}
                    </ul>
                    <Button className="w-full" size="sm" asChild>
                      <Link to="mailto:contact@printnow.be">
                        <Mail className="h-4 w-4" />
                        Exercer un droit
                      </Link>
                    </Button>
                  </CardContent>
                </Card>
              </div>
            </div>

            <div className="mt-12 text-center">
              <p className="text-muted-foreground mb-6">
                Pour toute question, notre équipe est disponible pour vous accompagner.
              </p>
              <div className="flex flex-wrap gap-3 justify-center">
                <Button variant="outline" size="sm" asChild>
                  <Link to="/mentions-legales">
                    Mentions légales
                    <ArrowRight className="h-4 w-4" />
                  </Link>
                </Button>
                <Button variant="outline" size="sm" asChild>
                  <Link to="/conditions-generales">
                    Conditions générales
                    <ArrowRight className="h-4 w-4" />
                  </Link>
                </Button>
                <Button variant="default" size="sm" asChild>
                  <Link to="mailto:contact@printnow.be">
                    <Mail className="h-4 w-4" />
                    Nous contacter
                  </Link>
                </Button>
              </div>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
};

export default Confidentialite;
