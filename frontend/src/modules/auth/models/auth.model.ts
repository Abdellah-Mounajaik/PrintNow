
export interface SignupRequest {
    prenom: string;
    nom: string;
    email: string;
    motDePasse: string;
    telephone?: string;
}

export interface LoginRequest {
    email: string;
    password: string;
}

export interface AuthResponse {
    token: string;
    type: string;
    id: number;
    email: string;
    role: string;
}