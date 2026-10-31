# ADR 0003 : Contrat d'API v2 unique et refonte du socle

Date : 2026-10-30
Statut : Accepté

## Contexte
La première implémentation renvoyait des données codées en dur (Map.of), plaçait les règles dans les contrôleurs et un filtre de développement (DevSwaggerAuthFilter) accordait **tous les rôles** à toute requête sans jeton, activé par défaut. Aucun convertisseur ne lisait les rôles Keycloak : les `@PreAuthorize` ne pouvaient pas fonctionner avec de vrais jetons. Le frontend et le backend divergeaient sur les formats.

## Décision
- `docs/API_CONTRACT.md` devient la référence unique (routes, DTO, énumérations, rôles, événements, données de démonstration) pour backend et frontend.
- Suppression du contournement d'authentification ; rôles lus dans `realm_access.roles` (`KeycloakJwtConverter`).
- Refonte de `cse-common` / `cse-events` ; chaque service est réécrit selon `docs/BACKEND_CONVENTIONS.md`.

## Conséquences
Les tests via Swagger exigent désormais un vrai jeton (flux OAuth2 PKCE intégré à Swagger UI, client `cse-swagger`). Toute évolution d'API commence par une mise à jour du contrat.
