# CSE Platform - Système de Management et de Suivi des Matériels et Équipements

Compagnie Sahélienne d'Entreprises (CSE) - Plateforme microservices pour la gestion des besoins, achats, fournisseurs, parc matériel, reporting et chatbot IA.

## Démarrage rapide
```bash
cp .env.example .env
make up
make build
```

## Architecture
Microservices Spring Boot 3.x, Clean/Hexagonale, Kafka, Keycloak, PostgreSQL, React 18+ TypeScript.




refait moi la partie front au complet ... la partie back est prete :
en integrant les apis et faites les teste depuis le fornt aussi .. avec des identifiant de test pour chaque profile .
# MISSION : Refonte et Implémentation Complète du Frontend CSE (`web-interne` & `portail-fournisseur`)

Tu es un **Architecte Frontend Senior React 18 / TypeScript / Tailwind CSS**.
Le backend (10 microservices Spring Boot 3 exposés via `ms-gateway` sur `http://localhost:8080`) est **déjà terminé, fonctionnel et peuplé de données**.
En revanche, le frontend actuel dans `frontend/` est incomplet ou mal aligné avec les contrats JSON réels du backend.

Ta mission est de **réécrire proprement et intégralement** les deux applications du monorepo `frontend/` :
1. `frontend/apps/web-interne` (port `5173`) — Application métier interne CSE (avec sidebar, header, sélecteur de rôle RBAC, cloche de notifications, tiroir chatbot IA, et 8 pages dédiées avec React Router).
2. `frontend/apps/portail-fournisseur` (port `5174`) — Portail externe pour les fournisseurs de la CSE.

---

## 1. EXIGENCES TECHNIQUES & DESIGN SYSTEM (OBLIGATOIRES)

1. **Architecture multi-pages propre (`react-router-dom`)** :
   - Interdiction de tout mettre dans un seul fichier monolithique illisible.
   - Crée un vrai **Layout industriel BTP moderne** (Sidebar latérale fixe à gauche avec icônes et compteurs, Topbar avec fil d'Ariane, Sélecteur de profil RBAC CSE, Cloche de notifications avec badge non-lues, et Widget Chatbot IA flottant).
   - Palette de couleurs BTP CSE Sénégal : Bleu marine profond (`slate-900` / `blue-950`), Jaune/Ambre engin de chantier (`amber-500`), Émeraude validation (`emerald-600`), Rouge alerte (`red-600`), fond clair (`slate-50`).
   - Utilise **Recharts** pour les graphiques dans le tableau de bord et **Leaflet (`react-leaflet`)** (avec tuiles OpenStreetMap + marqueurs GPS réels du Sénégal : Dakar `14.6928, -17.4467`, Thiès `14.7886, -16.9260`, Saint-Louis `16.0179, -16.4896`, Kaolack `14.1520, -16.0726`, Ziguinchor `12.5681, -16.2719`) pour la carte du parc et des chantiers.
2. **Proxy Vite & Client Axios** :
   - Dans `vite.config.ts` des deux apps (`5173` et `5174`), configure le proxy `/api` vers `http://localhost:8080`.
   - Attention : plusieurs endpoints GET du backend retournent un objet paginé `PageResponse<T>` de la forme :
     `{ content: T[], page: number, size: number, totalElements: number, totalPages: number, last: boolean }`
     tandis que d'autres retournent directement `T[]`. Ton helper de déballage (`unwrapList`) DOIT gérer les deux : `Array.isArray(data) ? data : (data?.content ?? [])`.
   - Intercepte les erreurs RFC 7807 (`ProblemDetail` : `data.detail`, `data.title`, `data.code`) et affiche des toasts/bannières d'erreur clairs en français.

---

## 2. CONTRATS API EXACTS DU BACKEND (À RESPECTER AU CHAMP PRÈS)

Ne devine aucun nom de champ : utilise **strictement** les schémas ci-dessous (vérifiés dans les contrôleurs Java du dossier `backend/`) :

### A. `ms-besoins` (`/api/besoins`) — [BesoinController.java]
- **Modèle `BesoinMateriel` reçu** :
  ```json
  {
    "id": "c1111111-1111-1111-1111-111111111111",
    "reference": "BES-2026-001",
    "designation": "Pelle hydraulique Caterpillar 320D",
    "quantite": 2,
    "specifications": "Godet 1.5m3, chenilles renforcées",
    "priorite": "NORMAL" | "URGENT" | "CRITIQUE",
    "statut": "EN_ATTENTE" | "VALIDE" | "REJETE" | "EN_ACHAT" | "SATISFAIT",
    "chantierId": "11111111-1111-1111-1111-111111111111",
    "demandeurId": "b1111111-1111-1111-1111-111111111111",
    "dureeUtilisationPrevue": 30,
    "justification": "Démarrage urgent terrassement Dakar Plateau",
    "motifRejet": null,
    "createdAt": "2026-10-27T..."
  }
  ```
  *(Attention : l'enum priorité est `NORMAL`, `URGENT`, `CRITIQUE` au masculin, et le statut initial est `EN_ATTENTE`).*
- `GET /api/besoins?chantierId=&statut=&page=0&size=20` $\rightarrow$ retourne `PageResponse<BesoinMateriel>`.
- `POST /api/besoins` $\rightarrow$ Body :
  `{ "designation": string, "quantite": number, "specifications": string, "priorite": "NORMAL"|"URGENT"|"CRITIQUE", "chantierId": UUID, "demandeurId": UUID, "dureeUtilisationPrevue": number, "justification": string }`
  *(Règle R08 : si `priorite` est `URGENT` ou `CRITIQUE`, `justification` est obligatoire).*
- `PATCH /api/besoins/{id}/valider` $\rightarrow$ Body : `{ "validePar": "b2222222-2222-2222-2222-222222222222" }` (Règle R01 : uniquement si `statut === "EN_ATTENTE"`).
- `PATCH /api/besoins/{id}/rejeter` $\rightarrow$ Body : `{ "motif": string, "rejetePar": "b2222222-2222-2222-2222-222222222222" }` (Règle R05/R08 : `motif` non vide obligatoire).
- `GET /api/besoins/consolidation` $\rightarrow$ retourne `BesoinMateriel[]`.
- `GET /api/besoins/{id}/historique` $\rightarrow$ retourne `{ besoinId, statutActuel, dateCreation }[]`.

### B. `ms-suivi` (`/api/materiels`, `/api/chantiers`, `/api/maintenances`, `/api/pannes`)
- **Modèle `Materiel` reçu** :
  ```json
  {
    "id": "aaaa1111-1111-1111-1111-111111111111",
    "immatriculation": "CSE-PEL-001",
    "designation": "Pelle hydraulique Caterpillar 320D",
    "categorie": "PELLE",
    "marque": "Caterpillar",
    "modele": "320D",
    "etat": "DISPONIBLE" | "AFFECTE" | "EN_MAINTENANCE" | "EN_PANNE" | "REFORME",
    "chantierActuelId": "11111111-1111-1111-1111-111111111111",
    "latitude": 14.6928,
    "longitude": -17.4467,
    "heuresUtilisation": 1250
  }
  ```
- `GET /api/materiels?etat=&categorie=&page=0&size=20` $\rightarrow$ `PageResponse<Materiel>`.
- `GET /api/materiels/disponibilite?categorie=PELLE&designation=Caterpillar&quantite=1` $\rightarrow$ `{ disponible: boolean, quantiteDisponible: number, materielsDisponibles: Materiel[] }`.
- `GET /api/materiels/carte` $\rightarrow$ `{ type: "FeatureCollection", features: Materiel[] }`.
- `POST /api/materiels/{id}/affecter` (Règle R04) $\rightarrow$ Body :
  `{ "chantierId": UUID, "besoinId": UUID | null, "demandeurId": UUID, "dateDebut": "YYYY-MM-DD", "dateFinPrevue": "YYYY-MM-DD", "motif": string, "affectePar": "resp.materiel@cse.sn" }`.
- `POST /api/materiels/{id}/confirmer-reception` $\rightarrow$ `{ id, status: "RECEPTIONNE" }`.
- `POST /api/materiels/{id}/reforme/demande` $\rightarrow$ demande la réforme (R13).
- `PATCH /api/materiels/{id}/reforme/valider` $\rightarrow$ valide la réforme par la DG (R13) et passe l'engin à `REFORME`.
- `GET /api/chantiers` $\rightarrow$ retourne `ChantierDto[]` : `{ id, nom, adresse, ville, latitude, longitude, statut, budget }`.
- `POST /api/chantiers` $\rightarrow$ Body : `{ "nom": string, "adresse": string, "ville": string, "latitude": number, "longitude": number, "budget": number }`.
- `GET /api/maintenances` $\rightarrow$ retourne `MaintenanceDto[]` : `{ id, numeroOt, materielId, typeMaintenance, statut, datePlanifiee, technicien, motif, cout }`.
- `POST /api/maintenances` $\rightarrow$ Body : `{ "materielId": UUID, "typeMaintenance": "PREVENTIVE"|"CORRECTIVE"|"LOURDE", "datePlanifiee": "YYYY-MM-DD", "technicien": string, "motif": string, "coutEstime": number }`.
- `PATCH /api/maintenances/{id}/cloturer` $\rightarrow$ clôture l'OT et remet le matériel `DISPONIBLE`.
- `POST /api/pannes` $\rightarrow$ Body : `{ "materielId": UUID, "chantierId": UUID, "description": string, "urgence": "NORMALE"|"URGENTE"|"CRITIQUE" }`.
- `GET /api/affectations/planning` $\rightarrow$ retourne `{ materielImmatriculation, designation, chantier, dateDebut, dateFinPrevue }[]`.

### C. `ms-achats` (`/api/appels-offres`, `/api/contrats`, `/api/engagements`, `/api/commandes`, `/api/factures`)
- **Modèle `AppelOffreDto`** : `{ id, reference, objet, typeAo, besoinId, dateLimite, statut: "PUBLIE"|"CLOTURE"|"ATTRIBUE", offreRetenueId }`.
- `GET /api/appels-offres?statut=&page=0&size=20` $\rightarrow$ `PageResponse<AppelOffreDto>`.
- `POST /api/appels-offres` $\rightarrow$ Body : `{ "objet": string, "typeAo": "APPEL_OFFRES"|"CONSULTATION_RESTREINTE", "besoinId": UUID, "dateLimite": "YYYY-MM-DD" }`.
- `GET /api/appels-offres/{id}/offres` $\rightarrow$ retourne `OffreDto[]` : `{ id, appelOffreId, fournisseurId, codeAnonyme, montantPropose, delaiLivraisonJours, noteTechnique }` (Règle R10 : `fournisseurId` est `null` tant que l'AO est `PUBLIE`).
- `POST /api/portail/appels-offres/{id}/offres` (Règle R02) $\rightarrow$ Body :
  `{ "fournisseurId": UUID, "montantPropose": number, "delaiLivraisonJours": number, "noteTechnique": number }`.
- `POST /api/appels-offres/{id}/cloturer` $\rightarrow$ passe l'AO à `CLOTURE` et lève l'anonymat R10.
- `GET /api/appels-offres/{id}/comparatif` $\rightarrow$ retourne `{ offreId, codeAnonyme, montantPropose, delaiLivraisonJours, noteTechnique, scoreGlobalPondere }[]`.
- `POST /api/appels-offres/{id}/attribuer` (R03) $\rightarrow$ Body : `{ "offreId": UUID }`.
- `GET /api/contrats` $\rightarrow$ retourne `ContratDto[]` : `{ id, numero, appelOffreId, offreId, fournisseurId, montantTotal, statut, engagementValideDaf }`.
- `POST /api/contrats` (R03/R11 : exige un AO `ATTRIBUE`, ex: `d2222222-2222-2222-2222-222222222222`) $\rightarrow$ Body : `{ "appelOffreId": UUID, "offreId": UUID, "fournisseurId": UUID, "montantTotal": number }`.
- `POST /api/contrats/{id}/signer` $\rightarrow$ signe le contrat.
- `POST /api/engagements/{contratId}/valider` (R06 : validation DAF) $\rightarrow$ passe `engagementValideDaf` à `true` et `statut` à `"ENGAGEMENT_VALIDE"`.
- `GET /api/commandes` $\rightarrow$ `{ id, numeroCommande, contratNumero, statut, delaiContractuelJours }[]`.
- `GET /api/factures` $\rightarrow$ `{ id, numeroFacture, contratId, montantTtc, statut }[]`.
- `POST /api/factures/{id}/mandater` $\rightarrow$ `{ factureId, statut: "PAIEMENT_MANDATE" }`.

### D. `ms-fournisseurs` (`/api/fournisseurs`)
- **Modèle `Fournisseur`** :
  ```json
  {
    "id": "f1111111-1111-1111-1111-111111111111",
    "raisonSociale": "Sénégal Équipements BTP SA",
    "ninea": "SN-DKR-2020-00145",
    "rc": "RC-DKR-1450",
    "email": "contact@senegal-equipements.sn",
    "telephone": "+221 33 821 00 00",
    "specialite": "MATERIEL_ROULANT",
    "statut": "ACTIF" | "EN_ATTENTE_VALIDATION" | "SUSPENDU" | "BLACKLISTE",
    "scorePerformance": 91.5,
    "demandeBlacklistage": false,
    "motifBlacklistage": null,
    "createdAt": "..."
  }
  ```
- `GET /api/fournisseurs?specialite=&statut=&page=0&size=20` $\rightarrow$ `PageResponse<Fournisseur>`.
- `POST /api/fournisseurs` $\rightarrow$ Body : `{ "raisonSociale": string, "ninea": string, "rc": string, "email": string, "telephone": string, "specialite": string }`.
- `PATCH /api/fournisseurs/{id}/valider-inscription` $\rightarrow$ valide l'homologation (`statut` devient `"ACTIF"`).
- `POST /api/fournisseurs/{id}/evaluer` (R07) $\rightarrow$ Body : `{ "noteQualite": number, "noteDelai": number, "noteConformite": number, "noteSav": number, "commentaire": string }`.
- `PATCH /api/fournisseurs/{id}/suspendre` (R14) $\rightarrow$ suspend le fournisseur.
- `POST /api/fournisseurs/{id}/blacklistage/demande` (R09 Étape 1 — Resp. Achats) $\rightarrow$ Body : `{ "motif": string }`.
- `PATCH /api/fournisseurs/{id}/blacklister` (R09 Étape 2 — Direction Générale) $\rightarrow$ confirme le blacklistage (`statut` devient `"BLACKLISTE"`).
- `GET /api/fournisseurs/classement?categorie=` $\rightarrow$ `Fournisseur[]` trié par `scorePerformance` décroissant.
- `GET /api/fournisseurs/{id}/catalogue` $\rightarrow$ `{ reference, designation, categorie, prixUnitaireFcfa, delaiJours }[]`.
- `GET /api/fournisseurs/comparaison-prix?categorie=MATERIEL_ROULANT` $\rightarrow$ `{ fournisseur, categorie, designation, prixUnitaireFcfa, scoreFournisseur }[]`.
- `POST /api/fournisseurs/{id}/litiges` $\rightarrow$ Body : `{ "motif": string }`.

### E. `ms-reporting`, `ms-auth`, `ms-audit`, `ms-notification`, `ms-chatbot`
- **Reporting** :
  - `GET /api/reporting/kpis?profil=DIRECTION_GENERALE&periode=2026-Q4` $\rightarrow$ `{ profil, periode, tauxDisponibiliteParcPct, tauxUtilisationMoyennePct, delaiMoyenApprovisionnementJours, budgetEngageFcfa, budgetRealiseFcfa, chantiersActifs, enginsEnMaintenance, fournisseursActifs }`.
  - `GET /api/reporting/series?kpi=TAUX_DISPONIBILITE&granularite=MENSUEL` $\rightarrow$ `{ periode, kpi, valeur }[]` (afficher avec un vrai graphique `ResponsiveContainer` + `AreaChart` / `BarChart` de `recharts`).
  - `GET /api/reporting/export?format=pdf&periode=2026-Q4` $\rightarrow$ `{ status, format, periode, fichier }`.
- **Administration Utilisateurs (`ms-auth`)** :
  - `GET /api/admin/utilisateurs?page=0&size=20` $\rightarrow$ `PageResponse<UtilisateurDto>` (`{ id, nom, prenom, email, role, actif, chantierAttribueId, perimetreParc }`).
  - `POST /api/admin/utilisateurs` $\rightarrow$ Body : `{ "nom": string, "prenom": string, "email": string, "role": string, "chantierAttribueId": UUID | null, "perimetreParc": string | null }`.
  - `PATCH /api/admin/utilisateurs/{id}/statut` $\rightarrow$ Body : `{ "actif": boolean }`.
- **Audit (`ms-audit`)** :
  - `GET /api/audit?utilisateur=&entite=&action=&page=0&size=20` $\rightarrow$ `PageResponse<AuditLogDto>` (`{ id, utilisateur, action, entite, entiteId, details, timestamp }`).
- **Notifications (`ms-notification`)** :
  - `GET /api/notifications?destinataire=` $\rightarrow$ `{ id, destinataire, type, sujet, message, lue, createdAt }[]`.
  - `POST /api/notifications` $\rightarrow$ `{ "destinataire": string, "type": string, "sujet": string, "message": string }`.
  - `PATCH /api/notifications/{id}/lue` $\rightarrow$ marque la notification comme lue.
- **Chatbot IA (`ms-chatbot`)** :
  - `POST /api/chatbot/query` $\rightarrow$ Body : `{ "question": string }` $\rightarrow$ retourne `{ id, question, intentionDetectee, reponse, horodatage }`.
  - `GET /api/chatbot/historique` $\rightarrow$ retourne la liste des `ConversationEntry`.
  - `DELETE /api/chatbot/historique/{id}` $\rightarrow$ supprime une entrée de l'historique.

---

## 3. ÉCRANS À LIVRER & CRITÈRES D'ACCEPTATION

1. **`apps/web-interne` (port `5173`)** :
   - **Page 1 (`/`) — Dashboard & Reporting** : Cartes KPI dynamiques selon le rôle sélectionné, graphiques **Recharts** (`AreaChart` évolution disponibilité, `BarChart` budget engagé vs réalisé), boutons d'export PDF/XLSX fonctionnels.
   - **Page 2 (`/besoins`) — Expression & Validation des Besoins** : Onglet liste filtrable, badges de statut/priorité, modale de création avec validation **R08** (`URGENT`/`CRITIQUE`), boutons d'action Valider (**R01**) et Rejeter avec modale motif obligatoire (**R05**), onglet Consolidation et modale Historique.
   - **Page 3 (`/parc`) — Parc Matériel, Chantiers & Maintenances** : Liste des engins avec filtres par état/catégorie, **vraie carte interactive Leaflet (`react-leaflet`)** centrée sur le Sénégal (`[14.4974, -14.4524]`, zoom 7) affichant les chantiers et engins avec popups, modale d'affectation (**R04**), confirmation de réception, déclaration de panne, onglet Ordres de Travail de maintenance (création + clôture), et workflow de réforme (**R13** : demande RM + validation DG).
   - **Page 4 (`/achats`) — Appels d'Offres, Comparatif & Contrats** : Création d'AO, liste des offres anonymisées (`codeAnonyme` - **R10**), bouton de clôture AO, tableau comparatif multicritères (`scoreGlobalPondere`), bouton d'attribution (**R03**), onglet Contrats (création à partir d'un AO attribué **R11** + signature) et Bons de commande.
   - **Page 5 (`/fournisseurs`) — Annuaire, Évaluation R07, Catalogue & Blacklistage R09** : Liste paginée avec barre de progression `scorePerformance` (rouge si `< 50` - **R07**), modale d'évaluation à 4 curseurs (`noteQualite`, `noteDelai`, `noteConformite`, `noteSav`), validation d'inscription, suspension (**R14**), workflow de blacklistage en 2 étapes (**R09** : bouton "Demander Blacklistage" pour RM/RA $\rightarrow$ bouton "Confirmer Blacklistage DG" quand `demandeBlacklistage === true`), onglet Classement, onglet Comparateur de prix & Catalogue, modale d'ouverture de litige.
   - **Page 6 (`/finance`) — Espace DAF** : Liste des contrats avec bouton de validation d'engagement budgétaire DAF (`POST /api/engagements/{contratId}/valider` - **R06**), liste des factures et bouton de mandatement de paiement (`POST /api/factures/{id}/mandater`).
   - **Page 7 (`/admin`) — Administration IAM & Journal d'Audit** : CRUD Utilisateurs (`ms-auth`) avec activation/désactivation et table du journal d'audit immuable (`ms-audit`) avec filtres par utilisateur, entité et action.
   - **Page 8 (`/chatbot`) + Widget flottant** : Interface conversationnelle complète avec badges d'`intentionDetectee`, suggestions rapides et suppression d'entrées d'historique.

2. **`apps/portail-fournisseur` (port `5174`)** :
   - Sélecteur du fournisseur connecté en haut à droite (chargé depuis `GET /api/fournisseurs`) affichant son statut (`ACTIF`, `EN_ATTENTE_VALIDATION`, `SUSPENDU`, `BLACKLISTE`) et son `scorePerformance`.
   - **Vue Appels d'Offres Ouverts** : Liste des AO `PUBLIE` et modale de dépôt d'offre (`POST /api/portail/appels-offres/{id}/offres` avec `fournisseurId`, `montantPropose`, `delaiLivraisonJours`, `noteTechnique`) + affichage du `codeAnonyme` reçu (**R10**).
   - **Vue Mon Catalogue** : Affichage de `GET /api/fournisseurs/{id}/catalogue` et comparatif marché.
   - **Vue Commandes & Factures** : Suivi des commandes (`GET /api/commandes`) et statut des factures (`GET /api/factures`).
   - **Vue Devenir Fournisseur CSE** : Formulaire d'inscription (`POST /api/fournisseurs`).

3. **Vérification Finale Obligatoire** :
   - Installe les dépendances manquantes éventuelles (`lucide-react` si souhaité, `@types/leaflet` pour TypeScript) et importe le CSS de Leaflet (`leaflet/dist/leaflet.css`) dans `index.html` ou `main.tsx`.
   - Exécute `pnpm -r build` dans `frontend/` et corrige **toutes** les erreurs TypeScript (`tsc`) et Vite jusqu'à obtenir un build 100 % vert.
surprime le front complet et refait le complet ...en basant sur les apis ... 