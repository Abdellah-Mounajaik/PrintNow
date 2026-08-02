import { Link } from "react-router-dom";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Scale, ArrowRight, Mail } from "lucide-react";

const legalSections = [
  {
    title: "Éditeur du site",
    content: [
      "PrintNow",
      "Siège social : Rue de l'Imprimerie 42, 1000 Bruxelles, Belgique",
      "Numéro d'entreprise : BE 0123.456.789",
      "TVA : BE0123456789",
      "Directeur de la publication : Abdellah",
      "Email : contact@printnow.be",
      "Téléphone : +32 2 123 45 67",
    ],
  },
  {
    title: "Hébergement",
    content: [
      "Hébergé sur des serveurs sécurisés.",
      "Pour toute question technique relative à la disponibilité de la plateforme, contactez notre équipe support.",
    ],
  },
  {
    title: "Objet de la plateforme",
    content: [
      "PrintNow est une marketplace en ligne qui met en relation des clients (particuliers et professionnels) avec des imprimeries partenaires. PrintNow n'est pas une imprimerie : elle facilite la mise en relation, la commande et le paiement, mais n'intervient pas dans la production physique des documents.",
    ],
  },
  {
    title: "Propriété intellectuelle",
    content: [
      "L'ensemble des éléments du site (logos, textes, graphismes, interfaces, code source) est protégé par le droit d'auteur et la propriété intellectuelle. Toute reproduction, distribution ou utilisation sans autorisation écrite est strictement interdite.",
    ],
  },
  {
    title: "Responsabilité",
    content: [
      "PrintNow s'efforce d'assurer l'exactitude des informations publiées. Cependant, la qualité des impressions, les délais de réalisation et la conformité des produits relèvent de la responsabilité de l'imprimerie partenaire sélectionnée par le client. PrintNow agit comme intermédiaire technique et de paiement.",
    ],
  },
  {
    title: "Données de contact",
    content: [
      "Pour toute question relative aux mentions légales, vous pouvez nous contacter à l'adresse contact@printnow.be ou par courrier à l'adresse du siège social indiquée ci-dessus.",
    ],
  },
];

const MentionsLegales = () => {
  return (
    <div className="min-h-screen flex flex-col bg-background">
      <main className="flex-1 pt-20">
        {/* Hero */}
        <section className="bg-gradient-to-br from-primary to-primary/80 text-primary-foreground py-16">
          <div className="container mx-auto px-4">
            <div className="flex items-center gap-2 mb-3 text-primary-foreground/80">
              <Scale className="h-5 w-5" />
              <span className="text-sm font-medium">Informations légales</span>
            </div>
            <h1 className="font-display text-3xl md:text-5xl font-bold mb-4 max-w-3xl">
              Mentions légales
            </h1>
          </div>
        </section>

        {/* Content */}
        <section className="py-16">
          <div className="container mx-auto px-4 max-w-4xl">
            <div className="space-y-6">
              {legalSections.map((section) => (
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
                Une question sur nos conditions ou notre politique de confidentialité ?
              </p>
              <div className="flex flex-wrap gap-3 justify-center">
                <Button variant="outline" size="sm" asChild>
                  <Link to="/conditions-generales">
                    Conditions générales
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

export default MentionsLegales;
