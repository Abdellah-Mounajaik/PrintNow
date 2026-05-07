import React, { createContext, useState, useEffect, type ReactNode, useContext } from 'react';

export interface UserData {
    id: number;
    email: string;
    role: string;
}

interface AuthContextType {
    user: UserData | null;
    token: string | null;
    isAuthenticated: boolean;
    loginGlobal: (token: string, user: UserData) => void;
    logoutGlobal: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
    // 💡 STRATÉGIE : On lit le localStorage DIRECTEMENT dans l'état initial
    // Cela évite le flash "non connecté" au rafraîchissement de la page
    const [token, setToken] = useState<string | null>(() => {
        return localStorage.getItem("token");
    });

    const [user, setUser] = useState<UserData | null>(() => {
        const storedUser = localStorage.getItem("user");
        return storedUser ? JSON.parse(storedUser) : null;
    });

    // Fonction pour sauvegarder la session (appelée après le login)
    const loginGlobal = (newToken: string, loggedUser: UserData) => {
        // On sauve dans le localStorage pour la persistance
        localStorage.setItem("token", newToken);
        localStorage.setItem("user", JSON.stringify(loggedUser));
        
        // On met à jour l'état React pour l'UI
        setToken(newToken);
        setUser(loggedUser);
    };

    const logoutGlobal = () => {
        setToken(null);
        setUser(null);
        localStorage.removeItem("token");
        localStorage.removeItem("user");
    };

    // isAuthenticated est maintenant calculé instantanément au chargement
    const isAuthenticated = !!token;

    return (
        <AuthContext.Provider value={{ user, token, isAuthenticated, loginGlobal, logoutGlobal }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = (): AuthContextType => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error("useAuth doit être utilisé à l'intérieur d'un AuthProvider");
    }
    return context;
};