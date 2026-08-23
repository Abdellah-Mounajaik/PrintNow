import path from "path"
import react from "@vitejs/plugin-react"
import { defineConfig } from "vite"

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  build: {
    // Cible large et conservatrice : sans elle, Vite peut produire du JS
    // utilisant des fonctionnalites recentes (ex. le global Iterator) absentes
    // des versions de Safari/iOS encore courantes chez les visiteurs.
    target: "es2020",
  },
})