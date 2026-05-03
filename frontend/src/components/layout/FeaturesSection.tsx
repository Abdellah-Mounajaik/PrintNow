import { 
  Zap, 
  GraduationCap, 
  Truck, 
  Shield, 
  Clock, 
  CreditCard,
  FileCheck,
  Headphones
} from "lucide-react";

const features = [
  {
    icon: Zap,
    title: "Express 2h",
    description: "Besoin urgent ? Certaines imprimeries proposent l'impression express en 2 heures.",
    color: "text-secondary",
    bgColor: "bg-secondary/10",
  },
  {
    icon: GraduationCap,
    title: "Tarif étudiant",
    description: "Bénéficiez de réductions exclusives sur présentation de votre carte étudiante.",
    color: "text-info",
    bgColor: "bg-info/10",
  },
  {
    icon: Truck,
    title: "Livraison",
    description: "Faites-vous livrer directement à domicile ou au bureau.",
    color: "text-success",
    bgColor: "bg-success/10",
  },
  {
    icon: Shield,
    title: "Paiement sécurisé",
    description: "Vos transactions sont protégées par Stripe, leader du paiement en ligne.",
    color: "text-primary",
    bgColor: "bg-primary/10",
  },
  {
    icon: Clock,
    title: "Suivi en temps réel",
    description: "Suivez l'état de votre commande étape par étape.",
    color: "text-warning",
    bgColor: "bg-warning/10",
  },
  {
    icon: FileCheck,
    title: "Qualité garantie",
    description: "Nos imprimeries partenaires sont sélectionnées pour leur professionnalisme.",
    color: "text-success",
    bgColor: "bg-success/10",
  },
];

const FeaturesSection = () => {
  return (
    <section className="py-20 bg-background">
      <div className="container mx-auto px-4">
        <div className="text-center mb-14">
          <h2 className="font-display text-3xl md:text-4xl font-bold text-foreground mb-4">
            Pourquoi choisir PrintHub ?
          </h2>
          <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
            Une plateforme pensée pour simplifier vos impressions au quotidien
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {features.map((feature, index) => (
            <div 
              key={feature.title}
              className="p-6 rounded-2xl bg-card border border-border hover:border-secondary/50 hover:shadow-card transition-all duration-300 group"
              style={{ animationDelay: `${index * 0.1}s` }}
            >
              <div className={`w-12 h-12 rounded-xl ${feature.bgColor} flex items-center justify-center mb-4 group-hover:scale-110 transition-transform`}>
                <feature.icon className={`h-6 w-6 ${feature.color}`} />
              </div>
              <h3 className="font-display font-semibold text-lg text-foreground mb-2">
                {feature.title}
              </h3>
              <p className="text-muted-foreground text-sm leading-relaxed">
                {feature.description}
              </p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};

export default FeaturesSection;
