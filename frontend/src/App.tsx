import React, { type JSX } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';

// Import de notre contexte d'authentification
import { AuthProvider, useAuth } from './modules/auth/context/AuthContext';

// Import de nos composants
import Auth from './modules/auth/pages/Auth';
import Header from './components/layout/Header';

// 🚨 LE SECRET EST ICI : On IMPORTE ton vrai fichier Home qui contient le HeroSection
// (Vérifie juste que le chemin correspond bien au dossier où tu as mis ton fichier Home.tsx)
import Home from './pages/Home'; 
import DevenirPartenaire from './modules/shop/pages/DevenirPartenaire';

// --- COMPOSANT DE SÉCURITÉ ---
const ProtectedRoute = ({ children }: { children: JSX.Element }) => {
  const { isAuthenticated } = useAuth();
  
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  
  return children;
};

// --- COMPOSANT PLACEHOLDER POUR LE DASHBOARD ---
const DashboardPlaceholder = () => {
  const { user, logoutGlobal } = useAuth();
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-background text-foreground">
      <h1 className="text-3xl font-bold">Bienvenue sur ton espace, {user?.email} !</h1>
      <p className="mt-2 text-muted-foreground">Ton rôle : {user?.role}</p>
      <button 
        onClick={logoutGlobal}
        className="px-5 py-2 mt-5 bg-destructive text-destructive-foreground rounded cursor-pointer hover:opacity-90 transition-opacity"
      >
        Se déconnecter
      </button>
    </div>
  );
};

// --- APPLICATION PRINCIPALE ---
function App() {
  return (
    <div className=" min-h-screen bg-background text-foreground">
      <AuthProvider>
        {/* Ton Header global */}
        <Header />
        
        <Routes>
          <Route path="/" element={<Home />} />
          
          <Route path="/login" element={<Auth />} />
<Route path="/devenir-partenaire" element={<DevenirPartenaire />} />
          <Route 
            path="/dashboard" 
            element={
              <ProtectedRoute>
                <DashboardPlaceholder />
              </ProtectedRoute>
            } 
          />

          <Route path="*" element={<h1 className="text-center mt-20 text-2xl">404 - Page introuvable</h1>} />
        </Routes>
      </AuthProvider>
    </div>
  );
}

export default App;