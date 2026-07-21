import type { SignupRequest, LoginRequest, AuthResponse } from '../models/auth.model';

const API_URL = "http://localhost:8080/api/auth";

export const authService = {
    
    register: async (userData: SignupRequest): Promise<void> => {
        const response = await fetch(`${API_URL}/register`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(userData)
        });

        if (!response.ok) {
            // Le backend renvoie soit du JSON {message: "..."} (validation), soit du texte brut
            const raw = await response.text();
            let errorMsg = raw;
            try {
                const parsed = JSON.parse(raw);
                errorMsg = parsed.message || parsed.detail || raw;
            } catch { /* texte brut, on le garde tel quel */ }
            throw new Error(errorMsg || "Erreur lors de l'inscription");
        }
    },

    login: async (credentials: LoginRequest): Promise<AuthResponse> => {
        const response = await fetch(`${API_URL}/login`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(credentials)
        });

        if (!response.ok) {
            throw new Error("Email ou mot de passe incorrect");
        }
        
        const data: AuthResponse = await response.json();
        
        if (data.token) {
            localStorage.setItem("token", data.token);
            localStorage.setItem("user", JSON.stringify({ 
                id: data.id, 
                email: data.email, 
                role: data.role 
            }));
        }
        
        return data;
    },

    logout: (): void => {
        localStorage.removeItem("token");
        localStorage.removeItem("user");
    }
};