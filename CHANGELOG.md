# Journal des modifications

Format inspiré de [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/) ; versions sémantiques.

## [2.0.0] - 2026-10-31 : Refonte complète

Reconstruction de la plateforme après audit de la v1 (voir [ADR-0003](docs/adr/0003-contrat-api-v2.md)). Rupture de compatibilité : nouveau contrat d'API, nouveaux schémas de base, nouveau realm.

### Ajouté
- **Contrat unique** `docs/API_CONTRACT.md` (routes, DTO, énumérations, rôles, événements, jeu de démonstration).
- **Socle** `cse-common` / `cse-events` : sécurité Keycloak (`KeycloakJwtConverter`, `CurrentUser`), erreurs
  RFC 7807, pagination bornée, outbox transactionnelle + audit automatique, consommateurs idempotents avec
  rejeu et DLT, chiffrement AES-256-GCM, client REST inter-services, règles ArchUnit.
- **ms-besoins** : chaîne de validation configurable (R01, R05, R08), orientation résiliente vers ms-suivi,
  consolidation et détection de doublons.
- **ms-fournisseurs** : référentiel, score par stratégie (R07), blacklistage en deux étapes (R09), litiges,
  catalogue et historique des prix, coordonnées bancaires chiffrées.
- **ms-suivi** : chantiers, parc, affectations anti-conflit (R04), réforme (R13), maintenance, pannes, carte
  GeoJSON, planning, alertes planifiées, création des matériels à la livraison.
- **ms-achats** : appels d'offres anonymisés (R02, R10), évaluation multicritère, attribution co-validée (R03, R11),
  contrats chiffrés, engagement DAF (R06), livraisons et pénalités, factures, PV PDF, portail fournisseur (API).
- **ms-auth** et **ms-gateway** : profils et administration des comptes via Keycloak, synchronisation ;
  passerelle sécurisée (JWT, limitation de débit Redis, CORS, coupe-circuits, Swagger agrégé, état de la plateforme).
- **ms-reporting** et **ms-audit** : projections CQRS, KPI par profil, séries, exports PDF/Excel ; journal
  d'audit immuable (déclencheurs PostgreSQL).
- **ms-notification** et **ms-chatbot** : notifications routées par configuration (in-app, SSE, e-mail) ;
  assistant avec intentions, filtre par rôle, LLM optionnel et mode dégradé.
- **web-interne** : nouveau socle (design system, coquille, Keycloak PKCE, couche HTTP) et tous les modules
  branchés sur l'API réelle (tableaux de bord, besoins et validation, parc, carte, planning, chantiers,
  maintenance, appels d'offres, contrats, fournisseurs, finance, rapports, administration, notifications,
  assistant), mode sombre, affichage mobile.
- **Exploitation** : image web-interne + Nginx HTTPS (CSP, HSTS), sauvegardes quotidiennes 90 jours et
  restauration testée, CI GitHub Actions, test de charge k6 (p95 23 ms à 50 utilisateurs), pilotage local
  `scripts/cse.sh`, scripts de jeton et de fumée par rôle.
- **Documentation** : architecture C4, catalogue d'événements, sécurité, exploitation, guides développeur et
  utilisateur, rapport qualité, ADR 0003 à 0013, collections `.http`.

### Modifié
- Isolation PostgreSQL par service (schéma + utilisateur dédiés) ; Spring Boot 3.5, Spring Cloud 2025.0,
  Spring AI 1.0, Testcontainers 1.21.4.
- Données de démonstration produites par le code applicatif (plus de seed SQL).

### Supprimé
- Filtre de développement qui accordait tous les rôles aux requêtes sans jeton.
- Données codées en dur dans les contrôleurs, guides de lancement obsolètes.

### Corrigé (QA en conditions réelles)
- Disponibilité opérationnelle, dates métier des factures, lien AO → contrat, `chantierId` sur la carte,
  tuiles OpenStreetMap en mode sombre, densité des tableaux, axes des graphiques, affichage mobile.

## [1.0.0-SNAPSHOT] - 2025-10-25
- Initialisation du monorepo, squelettes des microservices et du frontend, infrastructure Docker Compose.
