# ms-audit : Journal d'audit immuable

Port **8089** · schéma PostgreSQL `audit` · utilisateur `ms_audit` · contrat : `docs/API_CONTRACT.md` §8.

Le service enregistre, en ajout seul, toutes les actions métier de la plateforme. Il ne publie aucun
événement et n'appelle aucun autre service.

## Événements consommés

| Topic | Type | Payload | Effet |
|---|---|---|---|
| `cse.audit.events` | `AUDIT` | `AuditPayload {service, action, entite, entiteId, resume?}` | ajout d'une ligne `audit_log` |

Les autres types éventuellement présents sur le topic sont ignorés. Chaque événement de domaine écrit par
l'outbox d'un service produit automatiquement son entrée `AUDIT` (ADR-0007).

Correspondance des champs : `eventId`, `occurredAt`, `correlationId` et l'auteur (`actor.id`, `actor.nom`,
`actor.roles`) viennent de l'enveloppe ; `service`, `entite`, `entiteId` du payload (à défaut :
`source`, `aggregateType`, `aggregateId` de l'enveloppe).

Idempotence : `processed_event` (consommateur `ms-audit.journal`, configurable) **et** contrainte
d'unicité `uk_audit_log_event` + vérification applicative. Un message illisible ou incomplet
(`EntreeAuditInvalideException`, sous-classe d'`IllegalArgumentException`) part en `cse.audit.events.DLT`
sans rejeu ; une erreur technique est rejouée puis envoyée en DLT (gestionnaire de `cse-common`).

## API (rôle `ADMIN_SYSTEME` uniquement, R12)

| Méthode | Route | Détail |
|---|---|---|
| GET | `/api/audit` | `?utilisateurId&entite&action&service&du&au&q&page&size&sort` → `PageResponse<AuditLog>` |
| GET | `/api/audit/statistiques` | → `{ total, aujourdhui, septDerniersJours, parService: {service,total}[] }` |

- `du` / `au` : jours ISO (`2026-10-30`), inclus, en UTC. `du > au` → 400 `PERIODE_INVALIDE`.
- `q` : recherche insensible à la casse dans résumé, identifiant d'entité, nom de l'auteur, action et
  entité (caractères `%`/`_` recherchés littéralement) ; longueur max `cse.audit.longueur-max-recherche`.
- Pagination : défaut 20, max 50 ; tri autorisé `occurredAt` (défaut, décroissant), `service`, `action`,
  `entite`, `utilisateurNom` ; autre champ → 400 `TRI_NON_AUTORISE`.
- Statistiques : « aujourd'hui » et « sept derniers jours » sont calculés sur `occurredAt` en jours UTC,
  jour courant inclus (fenêtre `cse.audit.fenetre-statistiques-jours`, 7 par défaut).
- Toutes les erreurs sont au format RFC 7807 (401 `NON_AUTHENTIFIE`, 403 `ACCES_REFUSE`, 400…).

## Immuabilité

1. **Code** : le port `JournalAuditRepository` n'offre que l'ajout et la lecture ; le dépôt Spring Data
   étend `Repository` (aucune méthode `delete`/`save` héritée) ; l'entité est `@Immutable` et toutes ses
   colonnes sont `updatable = false`.
2. **Base** : la migration `V1__journal_audit.sql` installe les déclencheurs `trg_audit_log_immuable`
   (`BEFORE UPDATE OR DELETE`, par ligne) et `trg_audit_log_sans_truncate` (`BEFORE TRUNCATE`) qui lèvent
   `Journal d'audit immuable : opération … interdite` (SQLSTATE `42501`). Ils protègent aussi contre un
   accès SQL direct avec l'utilisateur `ms_audit`. Le test d'intégration le vérifie.

Seul un super-utilisateur PostgreSQL peut désactiver ces déclencheurs (`ALTER TABLE … DISABLE TRIGGER`),
action elle-même tracée dans les journaux PostgreSQL.

## Rétention et archivage (pas de purge)

Aucun job de purge n'existe : supprimer des lignes violerait l'immuabilité. La conservation longue est
assurée par archivage hors ligne, sous la responsabilité de l'exploitation :

- **Sauvegarde** : le schéma `audit` est inclus dans la sauvegarde quotidienne conservée 90 jours
  (`docs/OPERATIONS.md`).
- **Archivage annuel** recommandé : export en lecture seule des années closes, par exemple
  `COPY (SELECT * FROM audit.audit_log WHERE occurred_at < '2026-01-01') TO STDOUT WITH CSV HEADER`,
  compressé, horodaté, avec empreinte SHA-256 et stockage WORM (objet verrouillé) hors site.
- **Volume** : si la table devient très volumineuse, la convertir en table partitionnée par mois
  (`PARTITION BY RANGE (occurred_at)`) via une migration dédiée ; les partitions anciennes peuvent alors
  être détachées (`DETACH PARTITION`) et archivées sans `DELETE`, les déclencheurs étant recréés sur
  chaque partition.

## Configuration (`cse.audit.*`)

| Clé | Défaut | Rôle |
|---|---|---|
| `fenetre-statistiques-jours` | 7 (`AUDIT_FENETRE_STATISTIQUES_JOURS`) | fenêtre de `septDerniersJours` |
| `longueur-max-recherche` | 100 (`AUDIT_LONGUEUR_MAX_RECHERCHE`) | longueur max de `q` |
| `consommateur` | `ms-audit.journal` | nom du consommateur dans `processed_event` |

`cse.outbox.relay-enabled=false` : le service ne publie rien.

## Index

`occurred_at DESC`, `(service, occurred_at)`, `(action, occurred_at)`, `(entite, entite_id)`,
`(utilisateur_id, occurred_at)`, `correlation_id` (partiel). La recherche `q` (`ILIKE %…%`) parcourt la
table filtrée : pour de gros volumes, ajouter un index trigramme `pg_trgm` (extension à installer par un
super-utilisateur).

## Architecture

```
sn.cse.audit
 ├─ domain        AuditLog (immuable), TraceAction, CritereRechercheAudit, StatistiquesAudit, ParametresAudit
 ├─ application   EnregistrerTraceService, ConsulterAuditService ; port JournalAuditRepository
 ├─ infrastructure persistence (JPA + Criteria), messaging (AuditEventListener), config (AuditProperties)
 └─ api           AuditController, DTO AuditLog / StatistiquesAudit
```

## Tests

`cd backend && mvn -o -q -pl ms-audit spotless:apply verify`

- domaine et application (fakes en mémoire, idempotence « même événement deux fois → une entrée ») ;
- `@WebMvcTest` : 401, 403 pour un non-administrateur, 400 tri / période / date, bornage à 50 ;
- intégration Testcontainers : listener appelé deux fois, déclencheurs UPDATE/DELETE/TRUNCATE, API, et
  instantané OpenAPI (`src/test/resources/openapi.json`, régénération volontaire avec
  `-Dopenapi.regenerer=true`) ;
- architecture hexagonale (`HexagonalArchitectureRules`).
