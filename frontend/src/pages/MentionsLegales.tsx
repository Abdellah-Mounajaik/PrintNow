import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Card, CardContent } from "../components/ui/card";
import { Button } from "../components/ui/button";
import { Scale, ArrowRight, Mail } from "lucide-react";

type LegalSection = {
  title: string;
  content: string[];
};

const MentionsLegales = () => {
  const { t } = useTranslation("legal");
  const legalSections = t("mentions.sections", { returnObjects: true }) as LegalSection[];

  return (
    <div className="min-h-screen flex flex-col bg-background">
      <main className="flex-1 pt-20">
        {/* Hero */}
        <section className="bg-gradient-to-br from-primary to-primary/80 text-primary-foreground py-16">
          <div className="container mx-auto px-4">
            <div className="flex items-center gap-2 mb-3 text-primary-foreground/80">
              <Scale className="h-5 w-5" />
              <span className="text-sm font-medium">{t("mentions.hero.badge")}</span>
            </div>
            <h1 className="font-display text-3xl md:text-5xl font-bold mb-4 max-w-3xl">
              {t("mentions.hero.title")}
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
                {t("mentions.cta.question")}
              </p>
              <div className="flex flex-wrap gap-3 justify-center">
                <Button variant="outline" size="sm" asChild>
                  <Link to="/conditions-generales">
                    {t("mentions.cta.cgv")}
                    <ArrowRight className="h-4 w-4" />
                  </Link>
                </Button>
                <Button variant="outline" size="sm" asChild>
                  <Link to="/confidentialite">
                    {t("mentions.cta.confidentiality")}
                    <ArrowRight className="h-4 w-4" />
                  </Link>
                </Button>
                <Button variant="default" size="sm" asChild>
                  <Link to="mailto:contact@printnow.be">
                    <Mail className="h-4 w-4" />
                    {t("mentions.cta.contact")}
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
