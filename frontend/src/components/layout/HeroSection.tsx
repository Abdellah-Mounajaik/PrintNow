import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Search, MapPin, ArrowRight, } from "lucide-react";

const HeroSection = () => {
  return (
    <section className="relative min-h-[90vh] flex items-center hero-gradient overflow-hidden">
      {/* Background Pattern */}
      <div className="absolute inset-0 opacity-10">
        <div className="absolute top-20 left-10 w-72 h-72 bg-secondary rounded-full blur-3xl" />
        <div className="absolute bottom-20 right-10 w-96 h-96 bg-info rounded-full blur-3xl" />
      </div>

      {/* Grid Pattern Overlay */}
      <div 
        className="absolute inset-0 opacity-5"
        style={{
          backgroundImage: `url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='1'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E")`,
        }}
      />

      <div className="container mx-auto px-4 pt-20 relative z-10">
        <div className="max-w-4xl mx-auto text-center">
          {/* Badge */}
          <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-primary-foreground/10 border border-primary-foreground/20 text-primary-foreground/90 text-sm mb-8 animate-fade-in">
            <span className="w-2 h-2 rounded-full bg-success animate-pulse" />
            Plus de 150 imprimeries partenaires
          </div>

          {/* Main Headline */}
          <h1 className="font-display text-4xl md:text-5xl lg:text-6xl font-bold text-primary-foreground mb-6 leading-tight animate-slide-up">
            Vos impressions, <br />
            <span className="text-secondary">livrées ou retirées</span> <br />
            en un clic
          </h1>

          {/* Subtitle */}
          <p className="text-lg md:text-xl text-primary-foreground/80 mb-10 max-w-2xl mx-auto animate-slide-up" style={{ animationDelay: "0.1s" }}>
            Trouvez l'imprimerie idéale près de chez vous. Documents, flyers, affiches... 
            Comparez les prix et passez commande en quelques minutes.
          </p>

          {/* Search Box */}
          <div 
            className="bg-card rounded-2xl p-2 shadow-xl max-w-2xl mx-auto animate-slide-up"
            style={{ animationDelay: "0.2s" }}
          >
            <div className="flex flex-col sm:flex-row gap-2">
              <div className="relative flex-1">
                <MapPin className="absolute left-4 top-1/2 -translate-y-1/2 h-5 w-5 text-muted-foreground" />
                <Input
                  type="text"
                  placeholder="Entrez votre adresse ou ville..."
                  className="pl-12 h-12 border-0 bg-muted/50 focus-visible:ring-1"
                />
              </div>
              <Button variant="hero" size="lg" className="h-12 px-8">
                <Search className="h-5 w-5" />
                Rechercher
              </Button>
            </div>
          </div>

          {/* Quick Actions */}
          <div 
            className="flex flex-wrap justify-center gap-4 mt-8 animate-slide-up"
            style={{ animationDelay: "0.3s" }}
          >
            <Button 
              variant="outline-light" 
              className="group"
            >
              Documents & CV
              <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
            </Button>
            <Button 
              variant="outline-light"
              className="group"
            >
              Flyers & Affiches
              <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
            </Button>
            <Button 
              variant="outline-light"
              className="group"
            >
              Cartes de visite
              <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
            </Button>
          </div>

          {/* Stats */}
          <div 
            className="grid grid-cols-3 gap-8 mt-16 pt-8 border-t border-white/20 animate-slide-up"
            style={{ animationDelay: "0.4s" }}
          >
            <div className="text-center bg-primary/40 backdrop-blur-md rounded-xl py-5 px-2 border border-white/30 shadow-lg">
              <div className="font-display text-3xl md:text-4xl font-bold text-white drop-shadow-md">
                150+
              </div>
              <div className="text-sm text-white font-medium mt-1">
                Imprimeries
              </div>
            </div>
            <div className="text-center bg-primary/40 backdrop-blur-md rounded-xl py-5 px-2 border border-white/30 shadow-lg">
              <div className="font-display text-3xl md:text-4xl font-bold text-white drop-shadow-md">
                50k+
              </div>
              <div className="text-sm text-white font-medium mt-1">
                Commandes
              </div>
            </div>
            <div className="text-center bg-primary/40 backdrop-blur-md rounded-xl py-5 px-2 border border-white/30 shadow-lg">
              <div className="font-display text-3xl md:text-4xl font-bold text-white drop-shadow-md">
                4.8 ★
              </div>
              <div className="text-sm text-white font-medium mt-1">
                Note moyenne
              </div>
            </div>
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