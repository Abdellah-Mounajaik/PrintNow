import { type JSX } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom'; // On enlève BrowserRouter d'ici

import { AuthProvider, useAuth } from './modules/auth/context/AuthContext';
import Auth from './modules/auth/pages/Auth';
import Header from './components/layout/Header';
import Footer from './components/layout/Footer';
import ScrollToTop from './components/layout/ScrollToTop';
import Home from './pages/Home';
import Imprimeries from './pages/Imprimeries';
import DevenirPartenaire from './modules/shop/pages/DevenirPartenaire';
import DashboardImprimeur from './modules/imprimeur/pages/DashboardImprimeur';
import DashboardClient from './modules/user/pages/DashboardClient';
import DashboardAdmin from './modules/admin/pages/DashboardAdmin';
import PrintShopDetail from './modules/shop/pages/PrintShopDetail';
import Order from './modules/shop/pages/Order';
import MentionsLegales from './pages/MentionsLegales';
import ConditionsGenerales from './pages/ConditionsGenerales';
import Confidentialite from './pages/Confidentialite';
import Contact from './pages/Contact';
import FAQ from './pages/FAQ';
import { Toaster } from './components/ui/toaster';

const ProtectedRoute = ({ children }: { children: JSX.Element }) => {
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return children;
};

function App() {
  return (
    <AuthProvider>
      <ScrollToTop />
      <Header />
      <div className="min-h-screen bg-background text-foreground">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/imprimeries" element={<Imprimeries />} />
          <Route path="/login" element={<Auth />} />
          <Route path="/devenir-partenaire" element={<DevenirPartenaire />} />
          <Route path='/imprimerie/:slug' element={<PrintShopDetail />} />
          <Route path='/commander/:slug' element={<Order />} />
          <Route path="/mentions-legales" element={<MentionsLegales />} />
          <Route path="/conditions-generales" element={<ConditionsGenerales />} />
          <Route path="/confidentialite" element={<Confidentialite />} />
          <Route path="/contact" element={<Contact />} />
          <Route path="/faq" element={<FAQ />} />
          <Route 
            path="/dashboard-imprimeur" 
            element={
              <ProtectedRoute>
                <DashboardImprimeur />
              </ProtectedRoute>
            } 
          />
          
          {/* Redirection au cas où */}
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <DashboardClient />
              </ProtectedRoute>
            }
          />
          <Route
            path="/dashboard-admin"
            element={
              <ProtectedRoute>
                <DashboardAdmin />
              </ProtectedRoute>
            }
          />
          <Route path="/imprimeur" element={<Navigate to="/dashboard-imprimeur" replace />} />
          <Route path="*" element={<h1 className="text-center mt-20 text-2xl">404 - Page introuvable</h1>} />
        </Routes>
      </div>
      <Footer />
      {/* Zone d'affichage des messages : sans elle, tous les appels à toast()
          restaient sans effet et les erreurs passaient inaperçues. */}
      <Toaster />
    </AuthProvider>
  );
}

export default App;