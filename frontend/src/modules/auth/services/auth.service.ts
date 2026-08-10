import type { SignupRequest, LoginRequest, AuthResponse } from '../models/auth.model';

const API_URL = "http://localhost:8080/api/auth";

/** Le backend renvoie soit du JSON {message: "..."} (validation), soit du texte brut. */
const messageDErreur = async (response: Response, defaut: string): Promise<string> => {
    const raw = await response.text();
    try {
        const parsed = JSON.parse(raw);
        return parsed.message || parsed.detail || raw || defaut;
    } catch {
        return raw || defaut;
    }
};

export const authService = {

    register: async (userData: SignupRequest): Promise<void> => {
        const response = await fetch(`${API_URL}/register`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(userData)
        });

        if (!response.ok) {
            throw new Error(await messageDErreur(response, "Erreur lors de l'inscription"));
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
    },

    /**
     * Demande l'envoi d'un lien de réinitialisation.
     *
     * Le serveur répond de la même façon que l'adresse existe ou non : impossible
     * — et c'est voulu — d'en déduire qui est inscrit sur PrintNow.
     */
    demanderReinitialisation: async (email: string): Promise<void> => {
        const response = await fetch(`${API_URL}/mot-de-passe-oublie`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ email })
        });

        if (!response.ok) {
            throw new Error(await messageDErreur(response, "L'envoi du lien a échoué. Réessayez dans un instant."));
        }
    },

    /** Vérifie qu'un lien reçu par email est toujours exploitable. */
    verifierLienReinitialisation: async (jeton: string): Promise<boolean> => {
        const response = await fetch(`${API_URL}/reinitialiser?jeton=${encodeURIComponent(jeton)}`);
        if (!response.ok) return false;

        const data: { valide: boolean } = await response.json();
        return data.valide;
    },

    /** Remplace le mot de passe du compte désigné par le jeton du lien. */
    reinitialiserMotDePasse: async (jeton: string, nouveauMotDePasse: string): Promise<void> => {
        const response = await fetch(`${API_URL}/reinitialiser`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ jeton, nouveauMotDePasse })
        });

        if (!response.ok) {
            throw new Error(await messageDErreur(response, "La réinitialisation a échoué."));
        }
    }
};