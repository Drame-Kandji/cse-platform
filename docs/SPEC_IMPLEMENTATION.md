# SPEC_IMPLEMENTATION.md

## B.1 Décisions d'architecture
D1: Microservices ms-besoins, ms-achats, ms-fournisseurs, ms-suivi, ms-reporting, ms-chatbot + ms-gateway, ms-auth + ms-notification, ms-audit. Nom retenu ms-achats (pas ms-acquisitions).
D2: ms-auth encapsule API Admin Keycloak, hiérarchie Utilisateur JPA SINGLE_TABLE. motDePasseHash omis (délégation Keycloak).
D3: Propriété données par service, ms-reporting en read-models via événements (CQRS).
D4: REST synchrone + Kafka asynchrone, pas d'Eureka (URLs par config/DNS Docker).
D5: Kafka KRaft, topics cse.*.events + .DLT, contrats dans cse-events versionnés.
D6: Realm cse, clients web-interne/portail-fournisseur/gateway/services, rôles CHEF_CHANTIER, RESPONSABLE_MATERIEL, RESPONSABLE_ACHATS, DAF, DIRECTION_GENERALE, ADMIN_SYSTEME, FOURNISSEUR, claims roles/chantier_ids/fournisseur_id, OTP obligatoire pour ADMIN_SYSTEME/DIRECTION_GENERALE/DAF/RESPONSABLE_ACHATS.
D7: Portail fournisseur /api/portail/**, auto-inscription Keycloak, compte EN_ATTENTE_VALIDATION jusqu'à validation RA.
D8: Projections locales dans ms-achats (BesoinEnAchat), ms-reporting agrège événements.
D9: R05 défini, R08-R14 ajoutées.

## B.2 Règles de gestion
R01-R14 comme spécifiées. Chaque doit avoir test nommé *_R0X_*.

## B.3 Machines à états
StatutBesoin: EN_ATTENTE → VALIDE → (AFFECTE_DIRECT|EN_ACHAT); EN_ATTENTE → REJETE. StatutAO: PUBLIE → CLOTURE → ATTRIBUE. EtatMateriel: DISPONIBLE⇄AFFECTE, DISPONIBLE/AFFECTE→EN_MAINTENANCE→DISPONIBLE, *→REFORME (terminal). StatutFournisseur: EN_ATTENTE_VALIDATION → ACTIF ⇄ SUSPENDU → BLACKLISTE. Maintenance: PLANIFIEE → EN_COURS → CLOTUREE. Contrat: BROUILLON → SIGNE → ENGAGEMENT_VALIDE → EN_LIVRAISON → LIVRE → CLOTURE.

## B.4-10 Services
Détails complets selon B.4 (ms-besoins), B.5 (ms-achats), B.6 (ms-fournisseurs), B.7 (ms-suivi), B.8 (ms-reporting), B.9 (ms-chatbot), B.10 (ms-notification/ms-audit).

## B.11 Gateway
Routage complet, JWT, rate limiting Redis, CORS, corrélation, circuit breakers, Swagger agrégé.

## B.12 Événements
Enveloppe {eventId,type,version,occurredAt,aggregateId,correlationId,actor,payload}, versionnés, compatibilité schéma.

## B.13 Sécurité
Matrice d'accès + TLS obligatoire en prod, AES-256-GCM, audit horodaté.

## B.14 Infrastructure
Postgres (schémas/utilisateurs/ENUM par service), Keycloak realm import, Kafka KRaft, Redis, MailHog, services, Nginx HTTPS, backup 90j, Makefile (build/test/verify/up/down/logs/seed/e2e/docs/clean), CI.

## B.15 Frontend écrans
web-interne et portail-fournisseur selon spécification.

## B.16 Documentation
README, architecture.md (C4 Mermaid), adr/*.md, events.md, SECURITY.md, OPERATIONS.md, DEVELOPER_GUIDE.md, USER_GUIDE.md, README par service, CHANGELOG.md, collections .http.
