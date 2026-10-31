# PROGRESS.md

> État réel du projet, tenu à jour à chaque étape. Historique détaillé : `git log`.

## Refonte v2 (octobre 2026)

L'implémentation précédente a été auditée puis reconstruite (voir ADR-0003). Constats ayant motivé la refonte :
- un filtre de développement accordait **tous les rôles** à toute requête sans jeton (activé par défaut) ;
- aucun convertisseur des rôles Keycloak : les `@PreAuthorize` ne pouvaient pas fonctionner avec de vrais jetons ;
- la plupart des endpoints renvoyaient des données codées en dur ; règles métier dans les contrôleurs ;
- aucun événement Kafka réellement émis (reporting, notifications et audit vides) ;
- application interne sans aucune page fonctionnelle.

## Fait

| Lot | État | Vérification |
|---|---|---|
| Contrat d'API et d'événements unique (`docs/API_CONTRACT.md`) | ✅ | référence front + back |
| `cse-common` / `cse-events` (sécurité Keycloak, RFC 7807, outbox + audit, idempotence + DLT, AES-GCM) | ✅ | 20 tests dont PostgreSQL réel |
| Infra : utilisateur + schéma PostgreSQL par service, realm Keycloak à identifiants fixes | ✅ | isolation vérifiée (`permission denied`) |
| ms-suivi | ✅ | 125 tests, 99,5 % (domaine+application), test de concurrence |
| ms-besoins | ✅ | 94 tests, 99,5 % |
| ms-fournisseurs | ✅ | 138 tests, 99,5 % |
| ms-achats | ✅ | 146 tests, 99,6 %, cycle d'achat complet |
| ms-auth + ms-gateway | ✅ | 81 + 18 tests |
| ms-reporting + ms-audit | ✅ | 65 + 28 tests |
| ms-notification + ms-chatbot | ✅ | 65 + 101 tests |
| Frontend `web-interne` (tous les modules) | ✅ | 174 tests, typecheck/lint stricts |
| Intégration réelle (10 services + Keycloak + Kafka) | ✅ | `scripts/smoke.sh` : 6 rôles, RBAC conforme ; QA navigateur sans erreur API |

## En cours

- Corrections issues de la QA en conditions réelles : disponibilité opérationnelle, dates de paiement dans `FacturePayload`,
  lien AO → contrat, tuiles cartographiques, densité des tableaux, passage mobile.
- Exploitation : image frontend + Nginx HTTPS, sauvegardes 90 jours + restauration testée, CI, test de charge.

## Reste à faire

- Tests Playwright des 3 scénarios critiques (soumission/validation d'un besoin, cycle d'achat, réception + clôture de maintenance).
- Documentation complète (architecture C4, événements, sécurité, exploitation, guides développeur et utilisateur, rapport qualité).

## Hors périmètre (décision utilisateur)

- Portail fournisseur (`frontend/apps/portail-fournisseur`) : non traité ; ses sources sont vides et il est exclu de `make verify`.
  Les API `/api/portail/**` existent côté backend.

## Dette connue

- Jobs planifiés sans verrou distribué (une seule instance par service supposée ; ShedLock à ajouter pour du multi-instance).
- Registre SSE et limiteur de débit du chatbot en mémoire (mono-instance).
- 2FA : rôles concernés marqués `CONFIGURE_TOTP` à la création par ms-auth ; le realm de démonstration n'impose pas l'OTP aux comptes seedés.
- Client Keycloak `cse-tests-e2e` (mot de passe direct) à retirer du realm de production.
