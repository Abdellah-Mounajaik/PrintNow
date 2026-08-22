

import CTASection from "../components/layout/CTASection";
import FeaturesSection from "../components/layout/FeaturesSection";
import HeroSection from "../components/layout/HeroSection";
import HowItWorksSection from "../components/layout/HowItWorksSection";
import Seo from "../components/Seo";

const Home = () => {
  return (
    <div className="min-h-screen flex flex-col">
      <Seo
        title="Impression en ligne près de chez vous"
        description="Commandez vos impressions (documents, flyers, cartes de visite, posters) auprès d'une imprimerie locale partenaire, avec retrait en magasin ou livraison, et récupérez-les rapidement."
        path="/"
      />
      <main className="flex-1">
        <HeroSection />
        <HowItWorksSection />
        <FeaturesSection />
        <CTASection />
      </main>
    </div>
  );
};

export default Home;
