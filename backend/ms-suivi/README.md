# ms-suivi — Suivi du parc matériel

Microservice de la plateforme CSE (port **8085**, schéma PostgreSQL **`suivi`**, utilisateur `ms_suivi`).
Il gère les **chantiers**, le **parc de matériels** (cycle de vie `EtatMateriel`), les **affectations**
aux chantiers (règle R04, détection des chevauchements), les **réceptions**, les **ordres de travail**
de maintenance, les **pannes**, la **réforme** en deux temps (R13), la **carte GeoJSON** et le **planning**.

Contrat de référence : [`docs/API_CONTRACT.md`](../../docs/API_CONTRACT.md) §2 (routes, DTO, rôles), §11 (événements), §12 (jeu de démonstration).

## Architecture (hexagonale)

```
sn.cse.suivi
 ├─ domain          agrégats riches (Chantier, Materiel, Affectation, Maintenance, Panne, TypeEngin),
 │                  machines à états (EtatMateriel, StatutMaintenance, StatutPanne), PolitiqueAffectation
 ├─ application     ports in/out, commandes, vues de lecture, un service @Transactional par cas d'utilisation,
 │                  SuiviParametres (@ConfigurationProperties cse.suivi.*)
 ├─ infrastructure  persistance JPA (entités distinctes du domaine), outbox, consommateur Kafka,
 │                  planificateur des alertes, séquences PostgreSQL, DevDataSeeder
 └─ api             contrôleurs fins, DTO documentés (OpenAPI, exemples FR), mapper manuel
```

### Règles et machines à états

| Règle | Mise en œuvre |
|---|---|
| R04 — seul un matériel `DISPONIBLE` est affectable | `Materiel.affecter`, `PolitiqueAffectation` → 422 `MATERIEL_NON_DISPONIBLE` |
| Chevauchement de périodes | `PolitiqueAffectation` → 409 `CONFLIT_AFFECTATION` ; index unique partiel `ux_affectations_materiel_active` (une affectation active par matériel) + `@Version` : la seconde de deux demandes simultanées reçoit 409 `CONFLIT_AFFECTATION` |
| R12 — DG en lecture seule | `@PreAuthorize` : aucune écriture pour `DIRECTION_GENERALE` sauf la décision de réforme |
| R13 — réforme demandée par le RM, validée par la DG | `POST …/reforme/demande` puis `PATCH …/reforme/valider` ; `REFORME` est terminal, le matériel doit être libéré et hors maintenance |
| Chef de chantier limité à ses `chantier_ids` | `Perimetre` (listes filtrées, 403 `CHANTIER_NON_AUTORISE` sur un chantier hors périmètre) |

`EtatMateriel` : `DISPONIBLE → AFFECTE | EN_MAINTENANCE | REFORME` ; `AFFECTE → DISPONIBLE | EN_MAINTENANCE | REFORME` ;
`EN_MAINTENANCE → DISPONIBLE | AFFECTE | REFORME` ; `REFORME` terminal (toute autre transition → 409 `TRANSITION_INVALIDE`).
Maintenance : `PLANIFIEE → EN_COURS → CLOTUREE` ; la clôture remet le matériel `AFFECTE` si une affectation
est en cours (sinon `DISPONIBLE`), sauf si un autre ordre de travail est encore en cours.
Les numéros d'ordre de travail `OT-AAAA-NNNN` proviennent d'une séquence PostgreSQL annuelle (`seq_ot_<année>`).

## Endpoints

| Méthode | Route | Rôles |
|---|---|---|
| GET | `/api/chantiers`, `/api/chantiers/{id}` | internes (chef : ses chantiers) |
| POST / PUT | `/api/chantiers`, `/api/chantiers/{id}` | RM, ADMIN (PUT : `version` obligatoire) |
| PATCH | `/api/chantiers/{id}/terminer` | RM (422 si matériels encore affectés) |
| GET | `/api/materiels` (`etat, categorie, chantierId, q, maintenanceDue, page, size, sort`) | internes (chef : matériels de ses chantiers) |
| GET | `/api/materiels/{id}` (fiche + 20 dernières affectations/maintenances/pannes/mouvements) | internes |
| POST / PUT | `/api/materiels`, `/api/materiels/{id}` | RM |
| GET | `/api/materiels/disponibilite` (`categorie, designation, quantite, du, au`) | internes |
| GET | `/api/materiels/carte` (`categorie, etat`) → GeoJSON `FeatureCollection` | internes |
| POST | `/api/materiels/{id}/affecter` | RM |
| POST | `/api/materiels/{id}/confirmer-reception` | CHEF (son chantier), RM |
| POST | `/api/materiels/{id}/liberer` | RM |
| POST | `/api/materiels/{id}/heures` | CHEF (son chantier), RM |
| PATCH | `/api/materiels/{id}/etat` (`DISPONIBLE` ou `EN_MAINTENANCE`) | RM |
| POST | `/api/materiels/{id}/reforme/demande` | RM |
| PATCH | `/api/materiels/{id}/reforme/valider` | DG |
| GET | `/api/affectations`, `/api/affectations/planning` (≤ 366 jours) | internes (chef : ses chantiers) |
| GET / POST | `/api/maintenances` | internes / RM |
| PATCH | `/api/maintenances/{id}/demarrer`, `/api/maintenances/{id}/cloturer` | RM |
| GET / POST | `/api/pannes` | internes / CHEF (son chantier), RM |
| GET | `/api/referentiels/types-engins` | internes |
| POST / PUT | `/api/referentiels/types-engins[/{id}]` | ADMIN |

Erreurs au format RFC 7807 (`application/problem+json`, champs `code`, `traceId`, `errors`).
Swagger UI : `http://localhost:8085/swagger-ui.html` — contrat figé dans `src/test/resources/openapi.json`.

## Événements

Produits sur `cse.suivi.events` via l'outbox transactionnelle (chacun génère aussi une entrée `AUDIT` sur `cse.audit.events`) :

| Payload | Types |
|---|---|
| `ChantierPayload` | `CHANTIER_CREE`, `CHANTIER_MODIFIE` (modification et clôture) |
| `MaterielPayload` | `MATERIEL_ENREGISTRE`, `MATERIEL_ETAT_CHANGE`, `MATERIEL_REFORME_DEMANDEE`, `MATERIEL_REFORME`, `ALERTE_MAINTENANCE` |
| `AffectationPayload` | `MATERIEL_AFFECTE`, `RECEPTION_CONFIRMEE`, `AFFECTATION_TERMINEE`, `FIN_AFFECTATION_PROCHAINE` |
| `MaintenancePayload` | `MAINTENANCE_PLANIFIEE`, `MAINTENANCE_DEMARREE`, `MAINTENANCE_CLOTUREE` |
| `PannePayload` | `PANNE_SIGNALEE` |

Consommé (idempotent, rejeu + DLT) : `LIVRAISON_EFFECTUEE` sur `cse.achats.events` → création de `quantite`
matériels (codes `<préfixe catégorie>-AAAA-NNNN`, fournisseur/contrat/tarif du payload, valeur unitaire = montant / quantité)
et d'une affectation `EN_ATTENTE_RECEPTION` au chantier du besoin s'il est actif (sinon matériels laissés au parc).

## Jobs planifiés

| Job | Cron par défaut | Effet |
|---|---|---|
| Maintenance préventive | `0 0 6 * * *` | `ALERTE_MAINTENANCE` si heures depuis la dernière maintenance ≥ seuil horaire, ou date ≥ dernière maintenance + seuil en jours (seuils du type d'engin), sans ordre de travail ouvert ; rappel au plus tous les `rappel-maintenance-jours` |
| Fin d'affectation | `0 15 6 * * *` | `FIN_AFFECTATION_PROCHAINE` une seule fois par affectation dont la fin prévue est dans les `fin-affectation-jours-avant` jours (J-3) |

## Configuration (variables d'environnement)

| Variable | Défaut | Rôle |
|---|---|---|
| `SERVER_PORT` | `8085` | port HTTP |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | `jdbc:postgresql://localhost:5433/cse` / `ms_suivi` / `changeit-services` | base PostgreSQL (schéma `suivi`) |
| `DB_POOL_SIZE` | `10` | taille du pool Hikari |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | broker Kafka |
| `JWK_SET_URI` / `JWT_ISSUER_URI` | realm `cse` local | validation des JWT Keycloak |
| `SUIVI_ALERTES_ACTIVES` | `true` | active les jobs sur cette instance |
| `SUIVI_ALERTE_MAINTENANCE_CRON` / `SUIVI_ALERTE_FIN_AFFECTATION_CRON` | voir ci-dessus | crons des jobs |
| `SUIVI_FIN_AFFECTATION_JOURS_AVANT` | `3` | fenêtre d'alerte de fin d'affectation |
| `SUIVI_RAPPEL_MAINTENANCE_JOURS` | `7` | délai minimal entre deux alertes de maintenance |
| `SUIVI_LIVRAISON_DUREE_AFFECTATION_JOURS` | `180` | durée de l'affectation créée à la livraison |

Autres paramètres (`cse.suivi.*` dans `application.yml`) : taille des historiques, résultats de disponibilité,
amplitude du planning, ratio du tarif journalier par défaut, préfixes de numérotation.

## Données de démonstration (profil `dev`)

`DevDataSeeder` (idempotent) crée via le domaine et l'outbox : les 7 chantiers du contrat §12 (UUID et
coordonnées fixes, `CH-MBR-01` terminé), 14 types d'engins (`e0000000-0000-0000-0000-0000000000NN`),
36 matériels (pelles Caterpillar, bulldozers Komatsu, camions Volvo, niveleuses, compacteurs, grues,
malaxeurs, chargeuses, groupes électrogènes, informatique de chantier, consommables) dans tous les états,
et un historique daté sur 9 mois : affectations, réceptions, pannes, ordres de travail chiffrés, mouvements.

## Build et tests

```bash
cd backend && flock /tmp/cse-maven.lock env MAVEN_OPTS=-Xmx768m mvn -o -q -pl ms-suivi spotless:apply verify
```

Tests : domaine et cas d'utilisation (ports en mémoire), `@WebMvcTest` sécurité/ProblemDetail, ArchUnit
(`HexagonalArchitectureRules`), deux classes d'intégration Testcontainers partageant un contexte (parcours
complet, concurrence, consommateur Kafka, jobs, R12/R13, snapshot OpenAPI — régénération : `-Dopenapi.regenerer=true`).
JaCoCo impose 80 % de couverture des lignes sur `domain` + `application`.
