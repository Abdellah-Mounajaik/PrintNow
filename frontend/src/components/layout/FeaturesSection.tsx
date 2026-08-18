import {
  Zap,
  GraduationCap,
  Truck,
  Sparkles,
  Clock,
  SpellCheck2
} from "lucide-react";
import { useTranslation } from "react-i18next";

const featureStyles = [
  { icon: Zap, color: "text-secondary", bgColor: "bg-secondary/10" },
  { icon: SpellCheck2, color: "text-secondary", bgColor: "bg-secondary/10" },
  { icon: Sparkles, color: "text-primary", bgColor: "bg-primary/10" },
  { icon: GraduationCap, color: "text-info", bgColor: "bg-info/10" },
  { icon: Truck, color: "text-success", bgColor: "bg-success/10" },
  { icon: Clock, color: "text-warning", bgColor: "bg-warning/10" },
];

type FeatureItem = {
  title: string;
  description: string;
};

const FeaturesSection = () => {
  const { t } = useTranslation("home");
  const features = t("features.items", { returnObjects: true }) as FeatureItem[];

  return (
    <section className="py-20 bg-background">
      <div className="container mx-auto px-4">
        <div className="text-center mb-14">
          <h2 className="font-display text-3xl md:text-4xl font-bold text-foreground mb-4">
            {t("features.title")}
          </h2>
          <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
            {t("features.subtitle")}
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {features.map((feature, index) => {
            const style = featureStyles[index];
            const Icon = style.icon;
            return (
              <div
                key={feature.title}
                className="p-6 rounded-2xl bg-card border border-border hover:border-secondary/50 hover:shadow-card transition-all duration-300 group"
                style={{ animationDelay: `${index * 0.1}s` }}
              >
                <div className={`w-12 h-12 rounded-xl ${style.bgColor} flex items-center justify-center mb-4 group-hover:scale-110 transition-transform`}>
                  <Icon className={`h-6 w-6 ${style.color}`} />
                </div>
                <h3 className="font-display font-semibold text-lg text-foreground mb-2">
                  {feature.title}
                </h3>
                <p className="text-muted-foreground text-sm leading-relaxed">
                  {feature.description}
                </p>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
};

export default FeaturesSection;
