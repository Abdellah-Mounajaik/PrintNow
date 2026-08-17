import { Link } from "react-router-dom";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { FileText, ArrowRight, Mail } from "lucide-react";

const cgvSections = [
  {
    title: "Préambule",
    content: [
      "Les présentes Conditions Générales de Vente et d'Utilisation (CGV) régissent l'accès et l'utilisation de la plateforme PrintNow, ainsi que les relations entre PrintNow, les clients et les imprimeries partenaires. En utilisant le site, vous acceptez sans réserve les présentes conditions.",
    ],
  },
  {
    title: "Définitions",
    content: [
      "« Client » : toute personne physique ou morale passant commande via PrintNow.",
      "« Imprimeur partenaire » : professionnel de l'impression inscrit sur la plateforme et ayant accepté les conditions de partenariat.",
      "« Commande » : demande de production d'un ou plusieurs documents, transmise par un client à un imprimeur partenaire via PrintNow.",
      "« Service » : ensemble des fonctionnalités proposées par PrintNow pour faciliter la mise en relation, la commande et le paiement.",
    ],
  },
  {
    title: "Inscription et compte utilisateur",
    content: [
      "L'utilisation de certaines fonctionnalités nécessite la création d'un compte. L'utilisateur s'engage à fournir des informations exactes et à jour. Le compte est personnel et son mot de passe doit être conservé de manière confidentielle.",
    ],
  },
  {
    title: "Passage de commande",
    content: [
      "Le client choisit un imprimeur partenaire, téléverse ses fichiers PDF, sélectionne les options d'impression, le mode de retrait ou de livraison, puis procède au paiement. La commande est confirmée après validation du paiement.",
      "Le client est responsable de la qualité et de la conformité des fichiers transmis. PrintNow ne vérifie pas le contenu des documents avant impression.",
    ],
  },
  {
    title: "Prix et paiement",
    content: [
      "Les prix sont affichés par l'imprimeur partenaire et peuvent varier selon les options choisies. Le paiement est sécurisé et réalisé en ligne au moment de la commande.",
      "PrintNow applique une commission de 10% sur les commandes réalisées via la plateforme, prélevée sur le montant encaissé par l'imprimeur partenaire.",
      "Pour les imprimeurs souhaitant rejoindre le catalogue, un frais d'inscription de 100 € est requis. Ce montant est facturé lors de l'inscription et donne accès à l'activation automatique du compte partenaire.",
    ],
  },
  {
    title: "Options, réductions et codes promo",
    content: [
      "Selon l'imprimeur partenaire, des options payantes peuvent être proposées, comme l'impression express (délai raccourci) moyennant un supplément affiché avant paiement.",
      "Une réduction étudiante peut être appliquée après vérification de votre statut. Cette vérification suppose la transmission d'une carte étudiante et d'une pièce d'identité, traitées conformément à notre politique de confidentialité.",
      "Un imprimeur partenaire peut émettre des codes promotionnels. Un code n'est valable que chez l'imprimerie qui l'a émis et dans les conditions qu'elle a fixées (période, montant minimum, nombre d'utilisations).",
    ],
  },
  {
    title: "Correction orthographique",
    content: [
      "PrintNow propose un service optionnel et payant de correction orthographique de vos documents, réglé en même temps que la commande. L'analyse et l'aperçu des fautes sont gratuits ; seule l'application des corrections est facturée.",
      "Ce service repose sur une intelligence artificielle : le contenu du document est transmis à un prestataire spécialisé le temps du traitement. Le montant de la correction revient à PrintNow et n'entre pas dans la part reversée à l'imprimeur.",
    ],
  },
  {
    title: "Génération de designs par IA",
    content: [
      "PrintNow propose un service optionnel et payant de génération de supports (CV, flyers, cartes de visite) par intelligence artificielle : à partir de la description que vous fournissez, plusieurs propositions vous sont présentées et vous retenez celle de votre choix, qui rejoint alors votre commande. La génération et l'aperçu des propositions sont gratuits ; seul le design retenu et joint à une commande est facturé, au tarif indiqué avant paiement. Ce service n'est proposé que pour les supports que l'imprimerie sélectionnée est en mesure d'imprimer.",
      "Ce service repose sur une intelligence artificielle : la description que vous saisissez est transmise à un prestataire spécialisé le temps de produire le contenu. Les propositions sont fournies à titre d'aperçu et peuvent comporter des imprécisions ; il vous appartient d'en vérifier le contenu avant impression. Le montant de la génération revient à PrintNow et n'entre pas dans la part reversée à l'imprimeur.",
    ],
  },
  {
    title: "Livraison et retrait",
    content: [
      "Le client peut choisir entre le retrait en magasin et la livraison. Les délais et modalités de livraison sont indiqués par l'imprimeur partenaire. Les frais de livraison éventuels sont à la charge du client, sauf offre promotionnelle spécifique.",
    ],
  },
  {
    title: "Droit de rétractation",
    content: [
      "Conformément à la législation belge, les commandes de biens personnalisés ou imprimés sur mesure ne bénéficient pas du droit de rétractation une fois la production lancée. Toute annulation doit être demandée rapidement et reste soumise à l'acceptation de l'imprimeur partenaire.",
    ],
  },
  {
    title: "Responsabilité",
    content: [
      "PrintNow agit comme intermédiaire technique. La responsabilité relative à la qualité d'impression, aux délais et aux produits livrés incombe à l'imprimeur partenaire. PrintNow ne saurait être tenue responsable des retards ou défauts imputables à l'imprimeur ou à des cas de force majeure.",
    ],
  },
  {
    title: "Propriété intellectuelle",
    content: [
      "Le client garantit détenir tous les droits nécessaires sur les fichiers et contenus transmis. PrintNow et l'imprimeur partenaire ne pourront être tenus responsables d'une éventuelle violation de droits de propriété intellectuelle par le client.",
    ],
  },
  {
    title: "Avis clients",
    content: [
      "Un client ayant reçu une commande peut laisser un avis sur l'imprimerie concernée, à raison d'un avis par imprimerie. Les avis sont soumis à une modération automatique : tout propos injurieux ou inapproprié est refusé. PrintNow peut retirer un avis manifestement abusif.",
    ],
  },
  {
    title: "Modification des conditions",
    content: [
      "PrintNow se réserve le droit de modifier les présentes conditions à tout moment. Les conditions applicables sont celles en vigueur au moment de la commande ou de l'inscription.",
    ],
  },
  {
    title: "Loi applicable et juridiction",
    content: [
      "Les présentes conditions sont régies par le droit belge. En cas de litige, les parties s'engagent à rechercher une solution amiable avant de saisir les tribunaux compétents de Bruxelles.",
    ],
  },
];

const ConditionsGenerales = () => {
  return (
    <div className="min-h-screen flex flex-col bg-background">
      <main className="flex-1 pt-20">
        {/* Hero */}
        <section className="bg-gradient-to-br from-primary to-primary/80 text-primary-foreground py-16">
          <div className="container mx-auto px-4">
            <div className="flex items-center gap-2 mb-3 text-primary-foreground/80">
              <FileText className="h-5 w-5" />
              <span className="text-sm font-medium">Conditions d'utilisation</span>
            </div>
            <h1 className="font-display text-3xl md:text-5xl font-bold mb-4 max-w-3xl">
              Conditions générales de vente et d'utilisation
            </h1>
          </div>
        </section>

        {/* Content */}
        <section className="py-16">
          <div className="container mx-auto px-4 max-w-4xl">
            <div className="space-y-6">
              {cgvSections.map((section) => (
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

            <div className="mt-12 text-center">
              <p className="text-muted-foreground mb-6">
                Des questions sur nos conditions ? Notre équipe reste à votre disposition.
              </p>
              <div className="flex flex-wrap gap-3 justify-center">
                <Button variant="outline" size="sm" asChild>
                  <Link to="/mentions-legales">
                    Mentions légales
                    <ArrowRight className="h-4 w-4" />
                  </Link>
                </Button>
                <Button variant="outline" size="sm" asChild>
                  <Link to="/confidentialite">
                    Confidentialité & RGPD
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

export default ConditionsGenerales;
