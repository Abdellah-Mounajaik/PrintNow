import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Card, CardContent } from "../components/ui/card";
import { Button } from "../components/ui/button";
import { Shield, ArrowRight, Mail, CheckCircle2 } from "lucide-react";

type LegalSection = {
  title: string;
  content: string[];
};

const Confidentialite = () => {
  const { t } = useTranslation("legal");
  const privacySections = t("confidentiality.sections", { returnObjects: true }) as LegalSection[];
  const rights = t("confidentiality.rights.items", { returnObjects: true }) as string[];

  return (
    <div className="min-h-screen flex flex-col bg-background">
      <main className="flex-1 pt-20">
        {/* Hero */}
        <section className="bg-gradient-to-br from-primary to-primary/80 text-primary-foreground py-16">
          <div className="container mx-auto px-4">
            <div className="flex items-center gap-2 mb-3 text-primary-foreground/80">
              <Shield className="h-5 w-5" />
              <span className="text-sm font-medium">{t("confidentiality.hero.badge")}</span>
            </div>
            <h1 className="font-display text-3xl md:text-5xl font-bold mb-4 max-w-3xl">
              {t("confidentiality.hero.title")}
            </h1>
            <p className="text-sm text-primary-foreground/80">{t("confidentiality.hero.lastUpdate")}</p>
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
                      {t("confidentiality.rights.title")}
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
                        {t("confidentiality.rights.cta")}
                      </Link>
                    </Button>
                  </CardContent>
                </Card>
              </div>
            </div>

            <div className="mt-12 text-center">
              <p className="text-muted-foreground mb-6">
                {t("confidentiality.cta.question")}
              </p>
              <div className="flex flex-wrap gap-3 justify-center">
                <Button variant="outline" size="sm" asChild>
                  <Link to="/mentions-legales">
                    {t("confidentiality.cta.mentions")}
                    <ArrowRight className="h-4 w-4" />
                  </Link>
                </Button>
                <Button variant="outline" size="sm" asChild>
                  <Link to="/conditions-generales">
                    {t("confidentiality.cta.cgv")}
                    <ArrowRight className="h-4 w-4" />
                  </Link>
                </Button>
                <Button variant="default" size="sm" asChild>
                  <Link to="mailto:contact@printnow.be">
                    <Mail className="h-4 w-4" />
                    {t("confidentiality.cta.contact")}
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
