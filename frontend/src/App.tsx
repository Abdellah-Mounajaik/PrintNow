import React, { type JSX } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom'; // On enlève BrowserRouter d'ici

import { AuthProvider, useAuth } from './modules/auth/context/AuthContext';
import Auth from './modules/auth/pages/Auth';
import Header from './components/layout/Header';
import Home from './pages/Home'; 
import DevenirPartenaire from './modules/shop/pages/DevenirPartenaire';
import DashboardImprimeur from './modules/imprimeur/DashboardImprimeur';

const ProtectedRoute = ({ children }: { children: JSX.Element }) => {
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return children;
};

function App() {
  return (
    <AuthProvider>
      <div className="min-h-screen bg-background text-foreground">
        <Header />
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Auth />} />
          <Route path="/devenir-partenaire" element={<DevenirPartenaire />} />
          
          <Route 
            path="/dashboard-imprimeur" 
            element={
              <ProtectedRoute>
                <DashboardImprimeur />
              </ProtectedRoute>
            } 
          />
          
          {/* Redirection au cas où */}
          <Route path="/imprimeur" element={<Navigate to="/dashboard-imprimeur" replace />} />
          <Route path="*" element={<h1 className="text-center mt-20 text-2xl">404 - Page introuvable</h1>} />
        </Routes>
      </div>
    </AuthProvider>
  );
}

export default App;