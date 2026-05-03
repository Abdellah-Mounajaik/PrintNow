import { Search, Upload, CreditCard, Package } from "lucide-react";

const steps = [
  {
    icon: Search,
    title: "Trouvez",
    description: "Recherchez une imprimerie près de vous et comparez les services et tarifs.",
  },
  {
    icon: Upload,
    title: "Téléversez",
    description: "Envoyez votre fichier PDF et configurez vos options d'impression.",
  },
  {
    icon: CreditCard,
    title: "Payez",
    description: "Réglez en ligne de manière sécurisée via Stripe.",
  },
  {
    icon: Package,
    title: "Récupérez",
    description: "Retirez votre commande ou faites-vous livrer à domicile.",
  },
];

const HowItWorksSection = () => {
  return (
    <section className="py-20 bg-muted/30">
      <div className="container mx-auto px-4">
        <div className="text-center mb-14">
          <h2 className="font-display text-3xl md:text-4xl font-bold text-foreground mb-4">
            Comment ça marche ?
          </h2>
          <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
            Commander vos impressions n'a jamais été aussi simple
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
          {steps.map((step, index) => (
            <div 
              key={step.title}
              className="relative text-center group"
            >
              {/* Connector Line */}
              {index < steps.length - 1 && (
                <div className="hidden lg:block absolute top-10 left-[60%] w-[80%] h-0.5 bg-border" />
              )}

              {/* Step Number */}
              <div className="absolute -top-3 left-1/2 -translate-x-1/2 w-6 h-6 rounded-full bg-secondary text-secondary-foreground text-sm font-bold flex items-center justify-center z-10">
                {index + 1}
              </div>

              {/* Icon */}
              <div className="relative mx-auto w-20 h-20 rounded-2xl bg-card border border-border shadow-card flex items-center justify-center mb-5 group-hover:border-secondary group-hover:shadow-glow transition-all duration-300">
                <step.icon className="h-9 w-9 text-primary group-hover:text-secondary transition-colors" />
              </div>

              {/* Content */}
              <h3 className="font-display font-semibold text-xl text-foreground mb-2">
                {step.title}
              </h3>
              <p className="text-muted-foreground text-sm leading-relaxed">
                {step.description}
              </p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};

export default HowItWorksSection;