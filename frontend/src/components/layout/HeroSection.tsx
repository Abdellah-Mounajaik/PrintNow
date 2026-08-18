import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Button } from "../ui/button";
import { FileText, Megaphone, CreditCard, Star, ArrowRight } from "lucide-react";

const categoryIcons = [FileText, Megaphone, CreditCard];

type HeroCategory = {
  title: string;
  description: string;
};

type HeroStat = {
  value: string;
  label: string;
};

const HeroSection = () => {
  const { t } = useTranslation("home");
  const categories = t("hero.categories", { returnObjects: true }) as HeroCategory[];
  const stats = t("hero.stats", { returnObjects: true }) as HeroStat[];

  return (
    <section id="hero" className="relative min-h-[90vh] flex items-center hero-gradient overflow-hidden">
      {/* Background Ambient Glows */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute top-[-10%] right-[-10%] w-[500px] h-[500px] md:w-[700px] md:h-[700px] bg-secondary/10 rounded-full blur-[120px]" />
        <div className="absolute bottom-[-10%] left-[-10%] w-[500px] h-[500px] md:w-[700px] md:h-[700px] bg-info/10 rounded-full blur-[120px]" />
      </div>

      {/* Subtle Grid Pattern */}
      <div
        className="absolute inset-0 opacity-[0.03]"
        style={{
          backgroundImage: `radial-gradient(circle at 2px 2px, hsl(var(--primary-foreground)) 1px, transparent 0)`,
          backgroundSize: "40px 40px",
        }}
      />

      <div className="container mx-auto px-4 pt-24 pb-16 relative z-10">
        <div className="max-w-5xl mx-auto text-center">
          {/* Main Headline */}
          <h1 className="font-display text-4xl md:text-6xl lg:text-7xl font-bold text-primary-foreground mb-6 leading-[1.1] tracking-tight animate-slide-up">
            {t("hero.title.line1")} <br />
            <span className="text-secondary">{t("hero.title.highlight")}</span> <br />
            {t("hero.title.line2")}
          </h1>

          {/* Subtitle */}
          <p
            className="text-lg md:text-xl text-primary-foreground/70 mb-12 max-w-2xl mx-auto animate-slide-up leading-relaxed"
            style={{ animationDelay: "0.1s" }}
          >
            {t("hero.subtitle")}
          </p>

          {/* CTA */}
          <div
            className="flex items-center justify-center mb-20 animate-slide-up"
            style={{ animationDelay: "0.2s" }}
          >
            <Button variant="secondary" size="lg" asChild className="h-14 px-10 text-lg shadow-glow">
              <Link to="/imprimeries">
                {t("hero.cta")}
                <ArrowRight className="h-5 w-5 ml-2" />
              </Link>
            </Button>
          </div>

          {/* Quick Categories */}
          <div
            className="grid grid-cols-1 md:grid-cols-3 gap-4 max-w-3xl mx-auto mb-20 animate-slide-up"
            style={{ animationDelay: "0.3s" }}
          >
            {categories.map((category, index) => {
              const Icon = categoryIcons[index];
              return (
                <Link
                  key={category.title}
                  to="/imprimeries"
                  className="group p-6 rounded-2xl bg-primary-foreground/[0.03] border border-primary-foreground/10 hover:border-secondary/50 transition-all duration-300 hover:-translate-y-1 text-left"
                >
                  <div className="w-12 h-12 rounded-xl bg-secondary/10 flex items-center justify-center mb-4 group-hover:bg-secondary group-hover:text-primary transition-colors duration-300">
                    <Icon className="h-6 w-6 text-secondary group-hover:text-primary" />
                  </div>
                  <h3 className="font-display font-bold text-primary-foreground text-lg mb-1">
                    {category.title}
                  </h3>
                  <p className="text-sm text-primary-foreground/50">
                    {category.description}
                  </p>
                </Link>
              );
            })}
          </div>

          {/* Stats */}
          <div
            className="w-full max-w-4xl mx-auto grid grid-cols-3 divide-x divide-primary-foreground/10 border-t border-primary-foreground/10 pt-8 animate-slide-up"
            style={{ animationDelay: "0.4s" }}
          >
            {stats.map((stat, index) => (
              <div key={stat.label} className="flex flex-col items-center px-2">
                <div className="flex items-center gap-1">
                  <span className="font-display text-3xl md:text-4xl font-bold text-primary-foreground">
                    {stat.value}
                  </span>
                  {index === stats.length - 1 && (
                    <Star className="h-5 w-5 md:h-6 md:w-6 text-secondary fill-secondary" />
                  )}
                </div>
                <span className="text-xs md:text-sm text-primary-foreground/50 uppercase tracking-widest mt-1">
                  {stat.label}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Bottom Wave */}
      <div className="absolute bottom-0 left-0 right-0">
        <svg
          viewBox="0 0 1440 120"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
          className="w-full h-auto"
        >
          <path
            d="M0 120L60 110C120 100 240 80 360 70C480 60 600 60 720 65C840 70 960 80 1080 85C1200 90 1320 90 1380 90L1440 90V120H1380C1320 120 1200 120 1080 120C960 120 840 120 720 120C600 120 480 120 360 120C240 120 120 120 60 120H0V120Z"
            fill="hsl(var(--background))"
          />
        </svg>
      </div>
    </section>
  );
};

export default HeroSection;
