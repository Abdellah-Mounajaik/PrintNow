

import CTASection from "../components/layout/CTASection";
import FeaturesSection from "../components/layout/FeaturesSection";
import Header from "../components/layout/Header";
import HeroSection from "../components/layout/HeroSection";
import HowItWorksSection from "../components/layout/HowItWorksSection";

const Home = () => {
  return (
    <div className="min-h-screen flex flex-col">
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
