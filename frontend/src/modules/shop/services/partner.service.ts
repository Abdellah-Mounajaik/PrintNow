const PARTNERS_API_URL = "http://localhost:8080/api/partners";
const IMPRIMERIES_API_URL = "http://localhost:8080/api/imprimeries"; // 👈 Nouvelle URL pour le catalogue

export const partnerService = {
  
  // 1. Méthode pour s'inscrire
  register: async (payload: any) => {
    const response = await fetch(`${PARTNERS_API_URL}/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });

    if (!response.ok) {
      const errorData = await response.text(); 
      throw new Error(errorData || "Erreur lors de la création de l'imprimerie");
    }

// 1. On lit la réponse brute en texte (ex: "15" ou '{"id": 15}')
    const rawData = await response.text(); 
    
    // 2. On essaie de la convertir proprement
    try {
      return JSON.parse(rawData); // Si c'est un objet JSON ou un chiffre, ça le convertit
    } catch (e) {
      return rawData; // Si ça échoue, on renvoie le texte brut tel quel
    }  },

  getAllActive: async () => {
    const response = await fetch(IMPRIMERIES_API_URL);
    
    if (!response.ok) {
      throw new Error("Erreur lors de la récupération des imprimeries");
    }
    
    return response.json(); // On renvoie la liste au format JSON
  }

};