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
      "Selon les fonctionnalités que vous utilisez : votre identité (nom, prénom), votre adresse email, votre numéro de téléphone, votre adresse postale, vos données de paiement (traitées par notre prestataire Stripe — PrintNow n'y a jamais accès), l'historique de vos commandes, les fichiers PDF que vous téléversez pour impression, les documents que vous soumettez à la correction orthographique, les descriptions que vous saisissez et les designs produits lorsque vous utilisez la génération par IA, et, si vous demandez le tarif étudiant, votre carte étudiante et votre carte d'identité (uniquement le temps de la vérification, voir plus bas).",
      "Avec votre autorisation, nous utilisons également votre position pour vous montrer les imprimeries proches et estimer les temps de trajet. Cette position n'est pas conservée.",
    ],
  },
  {
    title: "Pourquoi utilisons-nous vos données ?",
    content: [
      "Vos données servent à : créer et gérer votre compte, traiter vos commandes, assurer la livraison ou le retrait, corriger vos documents et générer des designs lorsque vous le demandez, vérifier votre statut étudiant, communiquer avec vous, prévenir la fraude, améliorer nos services et respecter nos obligations légales.",
    ],
  },
  {
    title: "Qui a accès à vos données ? Nos sous-traitants",
    content: [
      "Vos données sont accessibles à PrintNow et, pour la réalisation de votre commande, à l'imprimerie partenaire que vous avez choisie.",
      "Nous faisons appel à des sous-traitants spécialisés, strictement pour ce qui est nécessaire au service : Stripe (paiements), Mistral AI (correction orthographique, génération de designs, assistant de la FAQ et modération des avis), bpost et AfterShip (suivi des livraisons), notre prestataire d'envoi d'e-mails, et un service de calcul d'itinéraires (temps de trajet). Nous ne vendons jamais vos données.",
    ],
  },
  {
    title: "Recours à l'intelligence artificielle",
    content: [
      "Quatre fonctionnalités reposent sur une intelligence artificielle fournie par Mistral : la correction orthographique, la génération de designs, l'assistant de la FAQ et la modération des avis. Pour la correction, le contenu du document que vous soumettez est transmis à ce prestataire le temps de l'analyse ; pour la génération, c'est la description que vous saisissez qui lui est transmise le temps de produire le support. Ces traitements servent uniquement à rendre le service que vous demandez.",
    ],
  },
  {
    title: "Vérification du statut étudiant",
    content: [
      "Pour vous accorder le tarif étudiant, nous vous demandons votre carte étudiante et votre carte d'identité, afin de vérifier que la carte étudiante est bien la vôtre. Ces images ne servent qu'à cette vérification : elles sont effacées dès que notre équipe a rendu sa décision. Nous ne conservons alors que le résultat — statut accepté ou refusé et sa durée de validité.",
    ],
  },
  {
    title: "Sécurité des données",
    content: [
      "Nous mettons en œuvre des mesures techniques et organisationnelles pour protéger vos données contre la perte, l'accès non autorisé ou la divulgation. L'accès à vos documents est réservé à vous-même, à l'imprimerie concernée et, le cas échéant, à notre administration. Les paiements sont traités par un prestataire certifié et les échanges sont sécurisés.",
    ],
  },
  {
    title: "Cookies et technologies similaires",
    content: [
      "PrintNow n'utilise que les cookies et le stockage local strictement nécessaires au fonctionnement du site — par exemple pour vous garder connecté. Nous n'utilisons pas de cookies publicitaires ni de traceurs tiers à des fins de profilage.",
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
      "Nous ne conservons vos données que le temps nécessaire à chaque finalité :",
      "• Les données de votre compte : tant que votre compte est actif.",
      "• Les fichiers que vous téléversez pour impression : effacés 7 jours après que la commande est terminée.",
      "• Les documents soumis à la correction orthographique : effacés 7 jours après.",
      "• Les descriptions et designs de la génération par IA : effacés 7 jours après votre dernière activité les concernant.",
      "• Les cartes d'identité et étudiantes : effacées dès la décision de vérification.",
      "• Les factures : conservées 7 ans, comme l'impose la loi comptable belge ; passé ce délai, elles sont détruites.",
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
            <p className="text-sm text-primary-foreground/80">Dernière mise à jour : 17 août 2026</p>
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
