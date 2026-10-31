# Registre des décisions d'architecture

Les décisions détaillées (contexte, décision, conséquences) sont dans [`docs/adr/`](docs/adr/). Règle de
préséance des sources : **spécifications techniques > annexe UML > cahier des charges** ;
toute contradiction tranchée fait l'objet d'un ADR listé ici.

| N° | Titre | Date | Statut | Résumé |
|---|---|---|---|---|
| [0001](docs/adr/0001-architecture-microservices.md) | Architecture microservices Clean/Hexagonale | 2025-10-25 | Accepté | Un service par contexte métier, couches domain/application/infrastructure/api, REST + Kafka, outbox, vérifié par ArchUnit. |
| [0002](docs/adr/0002-keycloak-auth.md) | Authentification Keycloak OAuth2/OIDC | 2025-10-25 | Accepté | Realm `cse`, clients publics PKCE, services en Resource Server JWT, ms-auth encapsule l'API d'administration. |
| [0003](docs/adr/0003-contrat-api-v2.md) | Contrat d'API v2 unique et refonte du socle | 2026-10-30 | Accepté | `docs/API_CONTRACT.md` devient la référence front/back ; suppression du contournement d'authentification de la v1 ; rôles lus dans `realm_access.roles`. |
| [0004](docs/adr/0004-database-per-service.md) | Isolation PostgreSQL par service | 2026-10-30 | Accepté | Une instance, mais un schéma + un utilisateur `ms_<service>` par service ; aucun accès croisé possible. |
| [0005](docs/adr/0005-versions-socle.md) | Spring Boot 3.5 / Spring Cloud 2025.0 / Spring AI 1.0 | 2026-10-30 | Accepté | Versions GA compatibles Spring AI et Docker 29 (Testcontainers 1.21.4) ; Java 17 conservé. |
| [0006](docs/adr/0006-csrf-api-stateless.md) | CSRF désactivé sur les API | 2026-10-30 | Accepté | API sans état authentifiée par en-tête Bearer, aucun cookie d'authentification : CSRF sans objet. |
| [0007](docs/adr/0007-outbox-et-audit.md) | Outbox transactionnelle et audit dérivé des événements | 2026-10-30 | Accepté | `OutboxWriter` dans la transaction, relais `SKIP LOCKED`, entrée `AUDIT` automatique par événement, consommateurs idempotents + DLT. |
| [0008](docs/adr/0008-donnees-de-demonstration.md) | Données de démonstration produites par le code | 2026-10-30 | Accepté | `DevDataSeeder` (profil `dev`) via domaine + outbox, identifiants partagés fixes, dates sur 9 mois. |
| [0009](docs/adr/0009-orientation-des-besoins.md) | Orientation d'un besoin validé | 2026-10-30 | Accepté | Orientation synchrone avec le JWT du RM ; si ms-suivi est indisponible, le besoin reste `VALIDE` et le RM relance ; jamais d'achat par défaut. |
| [0010](docs/adr/0010-chaine-validation-besoins.md) | Chaîne de validation configurable des besoins | 2026-10-30 | Accepté | Chaîne de responsabilité dans le domaine ; une étape RM active (R01), étape « Directeur de projet » déclarée inactive. |
| [0011](docs/adr/0011-seuil-delegation.md) | Seuil de délégation des achats (R11) | 2026-10-30 | Accepté | Seuil global `cse.achats.seuil-delegation` (150 M FCFA) au lieu de l'attribut UML par acheteur ; DG au-delà, co-validation RM toujours. |
| [0012](docs/adr/0012-pas-de-mot-de-passe-local.md) | Aucun mot de passe stocké hors Keycloak | 2026-10-30 | Accepté | `motDePasseHash` de l'UML omis ; Keycloak seul détenteur des secrets et des seconds facteurs. |
| [0013](docs/adr/0013-ms-auth-synchronisation-keycloak.md) | ms-auth : Keycloak source de vérité | 2026-10-30 | Accepté | Synchronisation idempotente Keycloak → profils, compensation à la création, changement de rôle SINGLE_TABLE par réécriture du discriminateur. |

## Décisions de cadrage issues de la spécification

| N° | Décision |
|---|---|
| D1 | Microservices ms-besoins, ms-achats, ms-fournisseurs, ms-suivi, ms-reporting, ms-chatbot, plus ms-gateway, ms-auth, ms-notification et ms-audit. Nom retenu : ms-achats (et non ms-acquisitions). |
| D2 | ms-auth encapsule l'API d'administration Keycloak ; hiérarchie `Utilisateur` en JPA `SINGLE_TABLE` ; `motDePasseHash` omis (délégation à Keycloak). |
| D3 | Chaque service est propriétaire de ses données ; ms-reporting construit ses vues à partir des événements (CQRS). |
| D4 | REST synchrone et Kafka asynchrone, sans Eureka (URL par configuration ou DNS Docker). |
| D5 | Kafka KRaft, topics `cse.*.events` et leurs `.DLT`, contrats versionnés dans `cse-events`. |
| D6 | Realm `cse`, clients publics PKCE, rôles `CHEF_CHANTIER`, `RESPONSABLE_MATERIEL`, `RESPONSABLE_ACHATS`, `DAF`, `DIRECTION_GENERALE`, `ADMIN_SYSTEME`, `FOURNISSEUR`, claims `chantier_ids` et `fournisseur_id` ; OTP pour l'administration, la DG, la DAF et le RA. |
| D7 | Portail fournisseur sous `/api/portail/**` ; un compte reste `EN_ATTENTE_VALIDATION` jusqu'à sa validation par le RA. |
| D8 | Projections locales dans ms-achats (besoins en achat) ; ms-reporting agrège les événements. |
| D9 | Règle R05 précisée, règles R08 à R14 ajoutées. |
