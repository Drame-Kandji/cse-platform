# ms-notification (port 8088, schéma `notification`, utilisateur `ms_notification`)

Notifications in-app (liste, compteur, lecture, flux SSE) et e-mails, produites à partir des
événements Kafka selon des **règles de routage déclarées en YAML** (`cse.notification.regles`).

## API (contrat §7) — tous les rôles internes
| Méthode | Route | Détail |
|---|---|---|
| GET | `/api/notifications?lue&page&size` | `Page<Notification>` (destinataire = l'utilisateur ou l'un de ses rôles), plus récentes d'abord |
| GET | `/api/notifications/non-lues/count` | `{ count }` |
| PATCH | `/api/notifications/{id}/lue` | idempotent ; 404 si la notification n'est pas destinée à l'appelant |
| POST | `/api/notifications/tout-lire` | 204 |
| GET | `/api/notifications/stream` | SSE : `ping` immédiat puis toutes les 25 s, `notification` (données = `Notification`, `id` = id) |

Flux SSE : `fetch` en streaming avec l'en-tête `Authorization` (jamais de jeton dans l'URL) ; en-têtes
`Cache-Control: no-cache` et `X-Accel-Buffering: no`. Connexions par utilisateur bornées
(`cse.notification.sse.max-connexions-par-utilisateur`, la plus ancienne est fermée), durée de vie
`cse.notification.sse.timeout`, délai de reconnexion conseillé `reconnexion`.

## Modèle
- Notification adressée à un **rôle** : stockée une seule fois (`destinataire_role`) ; adressée à un
  **utilisateur** : `destinataire_id`. L'état « lue » est par utilisateur (`notification_lectures`).
- Annuaire local (`annuaire_utilisateurs`) alimenté par `cse.auth.events` (`UTILISATEUR_*`) : adresses
  e-mail et chefs de chantier (« chefs du chantier du payload »). Une version plus ancienne est ignorée.

## Événements consommés (idempotents, rejeu + DLT de cse-common)
`cse.besoins.events`, `cse.suivi.events`, `cse.fournisseurs.events`, `cse.achats.events` → routage ;
`cse.auth.events` → annuaire. Le service ne publie aucun événement (relais outbox désactivé).

## Règles de routage
Chaque règle : `evenement`, destinataires (`roles`, `champs-destinataires` = champ du payload contenant
un id utilisateur, `chefs-du-chantier` + `champ-chantier`), `niveau`, `titre`/`message`/`lien` à
marqueurs `{champ}` (+ `{acteurNom}`), `entite`/`entite-id`, `email`, `condition` optionnelle
(`EGAL`, `DIFFERENT`, `INFERIEUR`, `SUPERIEUR`, `VRAI`, `FAUX`, `PRESENT`, `COMMENCE_PAR`). Valeurs mises
en forme en français (dates jj/mm/aaaa, montants groupés, codes traduits par `cse.notification.libelles`).
Un lien dont un marqueur manque est omis. La configuration est validée au démarrage.

## E-mail
`MailPort` → Spring Mail (MailHog en dev : `MAIL_HOST=localhost`, `MAIL_PORT=1025`) + gabarit Thymeleaf
`templates/mail/notification.html` (bandeau CSE, couleur du niveau, bouton vers
`FRONTEND_BASE_URL + lien`). Envoi **après validation** de la transaction ; un échec est journalisé,
jamais propagé (pas de rejeu Kafka). `MAIL_ENABLED=false` pour désactiver.

## Variables
`DB_URL`, `DB_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`, `JWK_SET_URI`, `JWT_ISSUER_URI`, `MAIL_HOST`, `MAIL_PORT`,
`MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `MAIL_ENABLED`, `FRONTEND_BASE_URL`, `SSE_PING_INTERVAL`,
`SSE_TIMEOUT`, `SSE_MAX_CONNEXIONS`.

## Build
`cd backend && mvn -o -q -pl ms-notification spotless:apply verify` (snapshot OpenAPI :
`-Dopenapi.regenerer=true` pour régénérer `src/test/resources/openapi.json`).
