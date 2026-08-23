# 🖨️ PrintNow

**PrintNow** est une plateforme de mise en relation entre des clients et des imprimeries locales (Belgique), pensée comme une place de marché : chaque imprimerie gère son propre catalogue et ses propres tarifs, le client compare, commande et se fait livrer ou retire en magasin — le tout enrichi d'outils IA (génération de supports, correction orthographique, chatbot).

Projet full-stack personnel : backend Spring Boot / Java, frontend React / TypeScript.

---

## Sommaire

- [Fonctionnalités](#fonctionnalités)
- [Stack technique](#stack-technique)
- [Démarrage rapide](#démarrage-rapide)
- [Variables d'environnement](#variables-denvironnement)
- [Documentation de l'API](#documentation-de-lapi)
- [Tests](#tests)
- [Tâches planifiées (RGPD)](#tâches-planifiées-rgpd)

---

## Fonctionnalités

Trois rôles cohabitent sur la plateforme : **client**, **imprimerie** (partenaire) et **administrateur**.

**Client** — recherche d'imprimeries (carte interactive, horaires, catalogue, avis) · commande de documents, reliure, plastification, flyers, cartes de visite, posters · retrait en magasin ou livraison suivie (bpost/AfterShip) · paiement par carte (Stripe) · **studio IA** pour générer CV/flyers/cartes de visite à partir d'un brief texte · **correction orthographique** de PDF par IA · réduction étudiante avec vérification automatique de la carte · codes promo, avis, chatbot d'assistance (fr/en/nl) · suppression de compte conforme RGPD.

**Imprimerie partenaire** — inscription en ligne avec activation automatique · gestion du catalogue, des tarifs, des horaires et du logo · suivi des commandes (payée → impression → prête → livrée) · codes promo propres · factures de commission et relevés de ventes.

**Administration** — vue d'ensemble des commandes/imprimeries/utilisateurs · paramétrage global (commission, frais partenaire, tarifs correction et studio IA) · modération et vérification étudiante manuelle des cas ambigus.

**Transverse** — authentification JWT (Spring Security) · multilingue fr/en/nl · SEO (meta par page, sitemap, robots.txt) · filet de sécurité webhook Stripe.

---

## Stack technique

**Backend**
- Java 21, Spring Boot 3.4 (Web, Security, Data JPA, Validation, Mail)
- MySQL 8
- JWT (jjwt)
- MapStruct (mapping DTO ↔ entités)
- Stripe Java (paiement)
- PDFBox + openhtmltopdf (génération de PDF)
- springdoc-openapi (Swagger UI)
- API [Mistral AI](https://mistral.ai) (chatbot, correction, vision, génération de contenu)
- [LanguageTool](https://languagetool.org) auto-hébergé (détection orthographique)
- API AfterShip (suivi de livraison bpost)
- Mailtrap (emails transactionnels, sandbox)

**Frontend**
- React 19, TypeScript, Vite
- TailwindCSS + Radix UI
- react-router-dom v7
- react-i18next (fr / en / nl)
- Stripe.js
- Leaflet / react-leaflet (carte des imprimeries)
- pdfjs-dist (aperçu PDF)

**Infrastructure locale**
- Docker Compose : MySQL, deux instances LanguageTool, phpMyAdmin

---

## Démarrage rapide

### Prérequis
- Java 21+
- Node.js 20+
- Docker (pour MySQL et LanguageTool)
- Un compte Stripe (clés de test), une clé API Mistral, un compte AfterShip et un compte Mailtrap (sandbox) pour les fonctionnalités qui en dépendent

### 1. Lancer les services externes

```bash
docker-compose up -d
```

Démarre MySQL (port 3306), deux instances LanguageTool (ports 8010/8011) et phpMyAdmin (port 8081).

### 2. Configurer les variables d'environnement du backend

Voir la [section dédiée](#variables-denvironnement) ci-dessous — ces variables doivent être définies dans l'environnement avant de lancer l'application (aucun fichier `.env` n'est lu côté backend).

### 3. Lancer le backend

```bash
./mvnw spring-boot:run
```

L'API démarre sur `http://localhost:8080`.

### 4. Lancer le frontend

```bash
cd frontend
npm install
npm run dev
```

L'application démarre sur `http://localhost:5173`. Le fichier `frontend/.env` définit déjà `VITE_API_URL` (pointant vers le backend local) et une clé Stripe de test publique.

---

## Variables d'environnement

À définir dans l'environnement du backend avant démarrage :

<details>
<summary>Liste des variables requises</summary>

| Variable | Usage |
|---|---|
| `JWT_SECRET` | Signature des jetons de connexion |
| `STRIPE_SECRET_KEY` | Appels à l'API Stripe |
| `STRIPE_WEBHOOK_SECRET` | Vérification des webhooks Stripe |
| `MISTRAL_API_KEY` | Chatbot, correction orthographique, vision (étudiant), studio IA |
| `AFTERSHIPPING_API_KEY` | Suivi des livraisons bpost |
| `MAILTRAP_USERNAME` / `MAILTRAP_PASSWORD` | Envoi d'emails transactionnels (sandbox) |

</details>

---

## Documentation de l'API

Une fois le backend lancé, la documentation interactive (Swagger UI) est disponible sur :

```
http://localhost:8080/swagger-ui.html
```

---

## Tests

```bash
./mvnw test        # backend
```

Le frontend n'a pas encore de suite de tests automatisés configurée.

---

## Tâches planifiées (RGPD)

Quatre purges quotidiennes tournent en tâche de fond pour respecter les durées de conservation légales (best-effort, une exécution manquée n'est pas critique).

<details>
<summary>Détail des tâches</summary>

| Tâche | Heure | Rôle |
|---|---|---|
| `PurgeFichiersClientsService` | 03h45 | Supprime les PDF clients 7 jours après la clôture de la commande |
| `PurgeCorrectionsService` | 03h15 | Supprime les documents de correction orthographique après 7 jours |
| `PurgeGenerationsService` | 03h30 | Supprime les fichiers générés par le studio IA après 7 jours |
| `PurgeFacturesArchiveesService` | 03h30 | Supprime les factures archivées au terme de leur délai légal de conservation |

</details>
