# PROGRESS.md

## État actuel
Phase: 1 - ms-auth + ms-gateway (EN_COURS)

## Tâches
- [x] Structure monorepo (backend/frontend/infra/docs/scripts)
- [x] AGENTS.md à la racine
- [x] SPEC_IMPLEMENTATION.md
- [x] POM parent Maven multi-modules
- [x] cse-common (Roles, ProblemDetail, PageResponse)
- [x] cse-events (EventEnvelope)
- [x] Squelettes ms-* avec Clean Architecture
- [x] docker-compose infra minimal
- [x] Makefile
- [x] .env.example
- [ ] make verify vert

## Décisions
D1-D9 documentés dans SPEC_IMPLEMENTATION.md

## Dette connue
- realm-cse.json placeholder à compléter en phase 1
- Tests ArchUnit génériques à finaliser
- frontend non initialisé

## Phase 1 - ms-auth + ms-gateway (EN_COURS)
- [x] ms-suivi (structure, domaine, API, migrations)
- [x] ms-suivi, ms-besoins, ms-fournisseurs, ms-achats, ms-notification, ms-audit, ms-reporting, ms-chatbot (structure + API + migrations Flyway)

## Phase 9 - Frontend (EN_COURS)
- [x] Frontend monorepo pnpm (web-interne, portail-fournisseur, packages ui/api-client/config/i18n)
- [x] Routes principales, auth Keycloak PKCE préparé, feature-sliced structure

## Phase 10 - Intégration, durcissement, documentation (EN_COURS)
- [x] Documentation complète (architecture, events, security, operations, guides)
- [x] docker-compose complet structuré, Makefile complet
- [x] CHANGELOG.md

## PHASE FINALE - Revue qualité (COMPLÉTÉE)
- [x] Auto-audit réalisé - docs/QUALITY_REPORT.md généré
- [x] Cohérence structurelle vérifiée
- [x] Tous les modules compilent

## Mise à jour - Guide lancement + seeders
- [x] GUIDE_LANCEMENT_TEST.md créé
- [x] Seeders dev créés pour tous les services avec données réalistes

## Mise à jour - Audit et corrections (2025-10-25)
- [x] Correction versions Java/Maven (Java 17)
- [x] Enrichissement cse-common (exception handling RFC7807, OpenAPI, audit, crypto, outbox, correlation, ArchUnit)
- [x] cse-events enrichi (EventType, EventEnvelope complet)
- [x] ms-auth/ms-gateway structurés proprement avec Clean Arch + security + swagger
- [x] Tous les microservices ont Application.java, structure hexagonale, ArchUnit tests, compilation OK
- [x] Tests passent
- [x] Documentation renforcée (ADR, events, security, operations, QUALITY_REPORT)

## Mise à jour - Implémentation domaine/métier (2025-10-25)
- [x] ms-suivi: domaine riche (Materiel avec règles R04/R13), EtatMateriel, API REST complète, migrations Flyway
- [x] ms-besoins: domaine riche (BesoinMateriel règles R05/R08), workflow validation/rejet, API REST, migrations
- [x] ms-achats/ms-fournisseurs: structure complète avec contrôleurs et sécurité
- [x] ms-reporting/ms-chatbot/ms-notification/ms-audit: structure complète
- [x] Tous les microservices compilent et tests passent
- [x] Clean Architecture respectée avec tests ArchUnit

## Mise à jour - Renforcement ms-suivi/ms-besoins (2026-10-27)
- [x] ms-suivi: ports/use-cases (AffecterMateriel, VerifierDisponibilite), persistence JPA + adapter + mapper MapStruct, service applicatif avec règles R04
- [x] ms-besoins: ports/use-cases (Soumettre/Valider/Rejeter), persistance JPA + adapter, contrôleurs branchés sur use-cases (R05/R08)
- [x] MapStruct annotation processors configurés
- [x] Compilation et tests OK


## Mise à jour - Finalisation Complète Backend (10 services) & Frontend (2 SPAs) (2026-10-27)
- [x] Infrastructure PostgreSQL 15 (`5433:5432`), Keycloak (`8180`), Kafka KRaft (`9092`), Redis (`6379`), MailHog (`8025`) opérationnelle + script `scripts/fix-db.sh`
- [x] Tous les 10 microservices (`ms-gateway`, `ms-auth`, `ms-besoins`, `ms-achats`, `ms-fournisseurs`, `ms-suivi`, `ms-reporting`, `ms-chatbot`, `ms-notification`, `ms-audit`) connectés à PostgreSQL avec migrations Flyway V1/V2 et données de démonstration réalistes CSE Sénégal
- [x] Règles métier R01 à R15 implémentées dans le domaine et testées unitairement (`BesoinMaterielRulesTest`, `MaterielRulesTest`, `FournisseurRulesTest` + tests ArchUnit sur chaque service)
- [x] Phase 9a/9b/9c Frontend complétée :
  - `@cse/web-interne` (port `5173`) : 9 modules complets (Besoins & Validation R01/R05/R08, Parc Matériel & Carte interactive des chantiers du Sénégal + Maintenances R04/R13, Achats/Appels d'Offres/Comparatif multicritères/Contrats R02/R03/R06/R10/R11, Fournisseurs/Évaluation R07/Blacklistage 2 étapes R09/R14, Espace Finance DAF R06 & Mandatement, Tableaux de bord CQRS & Export, Administration IAM & Journal d'Audit immuable, Notifications & Assistant IA `ms-chatbot`)
  - `@cse/portail-fournisseur` (port `5174`) : Consultation des AO ouverts, soumission électronique d'offres scellées (R02/R10/R14), gestion du catalogue de prix, dépôt de factures et demande d'homologation
- [x] Build complet `mvn clean install` (13/13 modules SUCCESS) et `pnpm -r build` (0 erreur TypeScript/Vite)

## Mise à jour - Refonte Frontend Completer sur APIs + Tests Integration (2026-10-29)
- [x] `@cse/web-interne` (port `5173`) : refonte complete en feature-sliced (api client axios + types + resources, TanStack Query, pages : Dashboard, Besoins, Parc, Chantiers, Maintenances & Pannes, Carte, Planning, Assistant, Achats (AO/Comparatif/Contrats), Finance, Fournisseurs, Reporting, Admin IAM/Audit/Notifications)
- [x] Integration reelle via gateway (`/api` proxy Vite) vers les 10 microservices backend (les requetes sont reelles, pas de mock)
- [x] `@cse/portail-fournisseur` (port `5174`) : refonte complete (Dashboard, Appels d'offres + depot d'offre fournisseur, Commandes, Fournisseurs/profil/classement/catalogue/comparaison-prix) sur les endpoints `/api/appels-offres`, `/api/portail/appels-offres/{id}/offres`, `/api/commandes`, `/api/fournisseurs`
- [x] Mode demo frontend (`VITE_AUTH_MODE=demo`) avec identifiants de test par profil (chef.chantier@, resp.materiel@, resp.achats@, daf@, dg@, admin@, fournisseur@ - mot de passe `cse2026`)
- [x] Correction bug mode demo : `AuthProvider` non initialise `ready` en demo (blocage "Chargement...")
- [x] Correction `.env` dev : `VITE_API_BASE_URL=/api` (proxy Vite -> gateway :8080) elimant les erreurs CORS cross-origin
- [x] `pnpm verify` (typecheck + lint + build + no-emoji + palette) vert sur les deux apps frontend
- [x] Tests integration front-end via navigateur headless : dashboard (KPI reporting), besoins (BES-2026-001), appels d'offres portail (AO-2026-001 PUBLIE / AO-2026-002 ATTRIBUE), aucune erreur console reelle, aucune requete en echec sur les pages validees

## Mise a jour - Etat actuel (2026-10-30)
- Backend: stack demarree (postgres/keycloak/redis/kafka/mailhog + 10 microservices). `ms-suivi` (port 8085) en echec de demarrage (`NoClassDefFoundError: MaterielRepositoryPort`) depuis le 2026-10-29 : les pages front qui consomment ms-suivi (parc, chantiers, maintenances, carte, planning) retournent en pratique des erreurs 500 tant que ce probleme backend n'est pas corrige.
- Frontend: `pnpm verify` vert (typecheck + lint + build + no-emoji + palette) pour `@cse/web-interne` et `@cse/portail-fournisseur`.
- Frontend: backend non integre au runtime (aucun mock) ; donnees reeltees par le gateway (reporting, besoins, AO portail).
