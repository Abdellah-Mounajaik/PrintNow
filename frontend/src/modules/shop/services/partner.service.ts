const API_URL = "http://localhost:8080/api/partners";

export const partnerService = {
  
  register: async (payload: any) => {
    const response = await fetch(`${API_URL}/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });

    if (!response.ok) {
      // On peut récupérer le message d'erreur du backend s'il y en a un
      const errorData = await response.text(); 
      throw new Error(errorData || "Erreur lors de la création de l'imprimerie");
    }

    return response.text(); // Ou response.json() si ton backend renvoie un JSON
  }

};