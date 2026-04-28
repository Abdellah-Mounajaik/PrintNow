import React, { type JSX } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';

// Import de notre contexte d'authentification
import { AuthProvider, useAuth } from './modules/auth/context/AuthContext';

// Import de notre page d'authentification
import Auth from './modules/auth/pages/Auth';
import Header from './components/layout/Header';

// --- COMPOSANT DE SÉCURITÉ ---
// Ce petit composant vérifie si l'utilisateur est connecté.
// Si oui, il affiche la page demandée. Sinon, il le renvoie vers /login.
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
    <div style={{ padding: '50px', textAlign: 'center', fontFamily: 'sans-serif' }}>
      <h1>Bienvenue sur ton espace, {user?.email} !</h1>
      <p>Ton rôle : {user?.role}</p>
      <button 
        onClick={logoutGlobal}
        style={{ padding: '10px 20px', marginTop: '20px', background: 'red', color: 'white', border: 'none', borderRadius: '5px', cursor: 'pointer' }}
      >
        Se déconnecter
      </button>
    </div>
  );
};

// --- APPLICATION PRINCIPALE ---
function App() {
  return (
    // 1. On englobe TOUTE l'application avec notre AuthProvider
    <AuthProvider>
      <Header />
      {/* 2. On configure le routeur pour la navigation */}
      
        <Routes>
          {/* Redirection par défaut vers la page de connexion */}
          <Route path="/" element={<Navigate to="/login" replace />} />
          
          {/* Notre fameuse page d'authentification (gère ?tab=connexion ou ?tab=inscription) */}
          <Route path="/login" element={<Auth />} />

          {/* Une route protégée : on ne peut y accéder que si on est connecté */}
          <Route 
            path="/dashboard" 
            element={
              <ProtectedRoute>
                <DashboardPlaceholder />
              </ProtectedRoute>
            } 
          />

          {/* Route 404 : Si l'utilisateur tape une URL qui n'existe pas */}
          <Route path="*" element={<h1 style={{textAlign: 'center', marginTop: '50px'}}>404 - Page introuvable</h1>} />
        </Routes>
    </AuthProvider>
  );
}

export default App;