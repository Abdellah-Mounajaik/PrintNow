import PrintShopsSection from "../components/layout/PrintShopsSection";
import Seo from "../components/Seo";

const Imprimeries = () => {
  return (
    <div className="min-h-screen flex flex-col">
      <Seo
        title="Trouvez une imprimerie partenaire"
        description="Parcourez les imprimeries partenaires de PrintNow près de chez vous : horaires, avis clients, options express et livraison."
        path="/imprimeries"
      />
      <main className="flex-1 pt-20">
        <PrintShopsSection />
      </main>
    </div>
  );
};

export default Imprimeries;
