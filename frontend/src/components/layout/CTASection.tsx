import { Button } from "../ui/button";
import { ArrowRight, Building2 } from "lucide-react";
import { Link } from "react-router-dom";

const CTASection = () => {
  return (
    <section className="py-20 bg-primary relative overflow-hidden">
      {/* Background Pattern */}
      <div className="absolute inset-0 opacity-10">
        <div className="absolute top-0 right-0 w-96 h-96 bg-secondary rounded-full blur-3xl -translate-y-1/2 translate-x-1/2" />
        <div className="absolute bottom-0 left-0 w-72 h-72 bg-info rounded-full blur-3xl translate-y-1/2 -translate-x-1/2" />
      </div>

      <div className="container mx-auto px-4 relative z-10">
        <div className="max-w-4xl mx-auto text-center">
          <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-primary-foreground/10 border border-primary-foreground/20 text-primary-foreground/90 text-sm mb-6">
            <Building2 className="h-4 w-4" />
            Pour les professionnels
          </div>

          <h2 className="font-display text-3xl md:text-4xl lg:text-5xl font-bold text-primary-foreground mb-6">
            Vous êtes imprimeur ? <br />
            Rejoignez notre réseau
          </h2>
          
          <p className="text-lg text-primary-foreground/80 mb-8 max-w-2xl mx-auto">
            Développez votre activité en rejoignant PrintNow. 
            Accédez à de nouveaux clients et gérez vos commandes simplement.
          </p>

          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Button 
              variant="hero" 
              size="xl"
              className="group"
              asChild
            >
              <Link to="/devenir-partenaire">
                Devenir partenaire
                <ArrowRight className="h-5 w-5 transition-transform group-hover:translate-x-1" />
              </Link>
            </Button>
            <Button 
              variant="outline-light" 
              size="xl"
              asChild
            >
              <Link to="/contact">
                Nous contacter
              </Link>
            </Button>
          </div>

          {/* Benefits */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-6 mt-14 pt-10 border-t border-primary-foreground/10">
            <div className="text-center">
              <div className="font-display text-2xl font-bold text-primary-foreground mb-1">
                100€
              </div>
              <div className="text-sm text-primary-foreground/60">
                Frais d'inscription unique
              </div>
            </div>
            <div className="text-center">
              <div className="font-display text-2xl font-bold text-primary-foreground mb-1">
                10%
              </div>
              <div className="text-sm text-primary-foreground/60">
                Commission unique
              </div>
            </div>
            <div className="text-center">
              <div className="font-display text-2xl font-bold text-primary-foreground mb-1">
                24/7
              </div>
              <div className="text-sm text-primary-foreground/60">
                Support dédié
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};

export default CTASection;