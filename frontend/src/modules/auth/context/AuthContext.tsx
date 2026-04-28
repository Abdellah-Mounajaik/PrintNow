// src/core/context/AuthContext.tsx
import React, { createContext, useState, useEffect, type ReactNode, useContext } from 'react';

// Les données que l'on veut garder en mémoire globale
export interface UserData {
    id: number;
    email: string;
    role: string;
}

// L'interface de ce que notre contexte va fournir
interface AuthContextType {
    user: UserData | null;
    token: string | null;
    isAuthenticated: boolean;
    loginGlobal: (token: string, user: UserData) => void;
    logoutGlobal: () => void;
}

// Création du Context
const AuthContext = createContext<AuthContextType | undefined>(undefined);

// Le Provider (qui va englober toute notre application)
export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
    const [user, setUser] = useState<UserData | null>(null);
    const [token, setToken] = useState<string | null>(null);

    // Au premier chargement de l'application, on vérifie le localStorage
    useEffect(() => {
        const storedToken = localStorage.getItem("token");
        const storedUser = localStorage.getItem("user");
        
        if (storedToken && storedUser) {
            setToken(storedToken);
            setUser(JSON.parse(storedUser));
        }
    }, []);

    // Fonction appelée par la LoginPage une fois la requête API réussie
    const loginGlobal = (newToken: string, loggedUser: UserData) => {
        setToken(newToken);
        setUser(loggedUser);
    };

    // Fonction appelée quand on clique sur "Se déconnecter" n'importe où dans l'app
    const logoutGlobal = () => {
        setToken(null);
        setUser(null);
        localStorage.removeItem("token");
        localStorage.removeItem("user");
    };

    // Un simple booléen pour savoir si l'utilisateur est connecté
    const isAuthenticated = !!token;

    return (
        <AuthContext.Provider value={{ user, token, isAuthenticated, loginGlobal, logoutGlobal }}>
            {children}
        </AuthContext.Provider>
    );
};

// Hook personnalisé très pratique pour ne pas avoir à importer useContext partout
export const useAuth = (): AuthContextType => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error("useAuth doit être utilisé à l'intérieur d'un AuthProvider");
    }
    return context;
};