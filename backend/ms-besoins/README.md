# ms-besoins — Besoins en matériel

Microservice (port **8082**, schéma PostgreSQL `besoins`, utilisateur `ms_besoins`) qui gère l'expression
des besoins en matériel par les chantiers, leur validation par le Responsable Matériel (R01), puis leur
orientation automatique : **affectation directe** depuis le parc (ms-suivi) ou **transmission aux achats**.
Contrat : `docs/API_CONTRACT.md` §3 ; décisions : ADR-0009 (orientation), ADR-0010 (chaîne de validation).

## Cycle de vie

```
EN_ATTENTE ──valider (RM)──▶ VALIDE ──parc suffisant──▶ AFFECTE_DIRECT
     │                         └────parc insuffisant──▶ EN_ACHAT
     └──rejeter (RM, motif)──▶ REJETE
```

- Un besoin `VALIDE` a toujours `orientationEnAttente=true` jusqu'à son orientation. Si ms-suivi est
  indisponible (panne, délai, disjoncteur ouvert) il reste `VALIDE` ; le RM relance via
  `POST /api/besoins/{id}/orienter`. Les matériels déjà affectés avant une interruption sont conservés et
  la relance ne demande que la quantité restante.
- Chaque action ajoute une entrée d'historique (`SOUMISSION`, `MODIFICATION`, `APPROBATION_ETAPE`,
  `VALIDATION`, `REJET`, `ORIENTATION_EN_ATTENTE`, `AFFECTATION_DIRECTE`, `ORIENTATION_ACHAT`,
  `MATERIEL_AFFECTE`).

## Règles de gestion

| Règle | Implémentation | Erreur |
|---|---|---|
| R01 validation/rejet réservés au RM | `@PreAuthorize` + `ChaineValidation` (domaine) | 403 `VALIDATION_NON_AUTORISEE` / `ACCES_REFUSE` |
| R05 non modifiable dès `VALIDE` | `BesoinMateriel.modifier` | 409 `BESOIN_NON_MODIFIABLE` |
| R08 justification si `URGENT`/`CRITIQUE` | `DescriptionBesoin` | 422 `JUSTIFICATION_OBLIGATOIRE` |
| R08 motif de rejet obligatoire | `BesoinMateriel.rejeter` | 422 `MOTIF_REJET_OBLIGATOIRE` |
| Chef limité à ses `chantier_ids` | cas d'utilisation | 403 `CHANTIER_NON_ATTRIBUE` / `ACCES_CHANTIER_REFUSE` |
| Chantier existant et `EN_COURS` (ms-suivi) | `VerificationChantier` | 422 `CHANTIER_INTROUVABLE` / `CHANTIER_INACTIF`, 503 `SERVICE_SUIVI_INDISPONIBLE` |
| Modification réservée au demandeur | `BesoinMateriel.modifier` | 403 `MODIFICATION_RESERVEE_DEMANDEUR` |
| Verrou optimiste | `version` | 409 `CONFLIT_CONCURRENCE` |

## API (`/api/besoins`)

| Méthode | Route | Rôles |
|---|---|---|
| POST | `/api/besoins` | CHEF (ses chantiers), RM |
| PUT | `/api/besoins/{id}` | demandeur (CHEF ou RM) |
| GET | `/api/besoins/{id}`, `/api/besoins`, `/api/besoins/{id}/historique` | CHEF (filtré), RM, RA, DG |
| PATCH | `/api/besoins/{id}/valider`, `/api/besoins/{id}/rejeter` | RM |
| POST | `/api/besoins/{id}/orienter` | RM |
| GET | `/api/besoins/consolidation?statut=EN_ATTENTE\|VALIDE` | RM |
| GET | `/api/besoins/statistiques` | tous les rôles internes (CHEF filtré) |

Liste : `?chantierId&statut&priorite&categorie&q&du&au&page&size&sort` ; `du`/`au` portent sur la date
de soumission ; tri autorisé : `createdAt` (défaut, desc), `updatedAt`, `dateBesoin`, `priorite`, `statut`,
`reference`, `quantite`, `designation`. Swagger UI : `http://localhost:8082/swagger-ui.html`.

## Intégrations

- **ms-suivi** (REST, JWT de l'utilisateur relayé par `ServiceRestClientFactory`) :
  `GET /api/chantiers/{id}` (soumission/modification), `GET /api/materiels/disponibilite` puis
  `POST /api/materiels/{id}/affecter` (`dateDebut = dateBesoin`, `dateFinPrevue = dateBesoin +
  dureeUtilisationJours`, matériels `materielIds` choisis par le RM en priorité). Résilience4j, instance
  `suivi` : délai (`cse.clients.read-timeout`), rejeu ×3 avec attente exponentielle sur erreurs réseau/5xx,
  disjoncteur ; les refus métier 4xx ne sont ni rejoués ni comptés comme pannes (409/422/404 sur une
  affectation → matériel suivant).
- **Kafka (outbox)** — produit sur `cse.besoins.events` (`BesoinPayload`) : `BESOIN_SOUMIS`,
  `BESOIN_MODIFIE`, `BESOIN_VALIDE`, `BESOIN_REJETE`, `BESOIN_AFFECTE_DIRECT`, `BESOIN_EN_ACHAT`
  (+ `AUDIT` automatique sur `cse.audit.events`).
- **Kafka (consommation)** — `cse.suivi.events` : `MATERIEL_AFFECTE` portant un `besoinId` → entrée
  d'historique `MATERIEL_AFFECTE` (idempotent via `processed_event`, rejeu + DLT communs).

## Configuration (`cse.besoins.*`)

| Clé | Défaut | Rôle |
|---|---|---|
| `reference.prefixe` / `reference.chiffres` | `BES` / `4` | format `BES-2026-0001` (séquence PostgreSQL par année) |
| `consolidation.fenetre-doublon-jours` | `30` (`CSE_BESOINS_FENETRE_DOUBLON_JOURS`) | doublon potentiel : même chantier, même désignation normalisée, dates de besoin à ≤ N jours |
| `consolidation.limite` | `2000` | besoins lus au maximum par consolidation |
| `validation.etapes[]` | RM actif ; `DIRECTEUR_PROJET` inactif | chaîne de validation (ADR-0010) |
| `suivi.base-url` | `http://localhost:8085` (`SUIVI_BASE_URL`) | URL de ms-suivi |
| `cse.clients.connect-timeout` / `read-timeout` | `2s` / `3s` | délais des appels inter-services |
| `resilience4j.retry/circuitbreaker.instances.suivi.*` | voir `application.yml` | rejeu et disjoncteur |

## Données de démonstration (profil `dev`)

`DevDataSeeder` crée 40 besoins (identifiants fixes `b0000000-0000-0000-0000-0000000000NN`) sur les
9 derniers mois, chantiers 1 à 6 du référentiel partagé : demandeurs Mamadou Diop (chantiers 1–2),
Awa Ndiaye (3–4) et Ibrahima Fall, RM (5–6, sans chef) ; décisions par le RM ; tous statuts et priorités ;
6 rejets motivés ; achats majoritairement `MATERIEL_ROULANT` ; doublon potentiel (bétonnières du
chantier 1) et regroupement multi-chantiers (pelles 20 t). Les événements passent par l'agrégat et
l'outbox, datés de l'action simulée et attribués à leur auteur. ms-suivi n'est jamais appelé : les
matériels affectés utilisent la convention d'identifiants `a1000000-…`. Idempotent.

## Développement

```bash
cd backend
flock /tmp/cse-maven.lock env MAVEN_OPTS=-Xmx768m mvn -o -q -pl ms-besoins spotless:apply verify
```

Tests : domaine et application (règles nommées `*_R0X`), `@WebMvcTest` (401/403/400/409/422 en
ProblemDetail), adaptateur ms-suivi contre WireMock avec les vrais aspects Resilience4j (rejeu, délai,
disjoncteur, repli), intégration Testcontainers de bout en bout (soumission → validation →
`AFFECTE_DIRECT` / `EN_ACHAT`, panne puis relance, outbox, consommateur idempotent, jeu de démonstration),
ArchUnit (`HexagonalArchitectureRules`) et instantané OpenAPI (`src/test/resources/openapi.json`,
régénérable avec `-Dopenapi.snapshot.update=true`). JaCoCo impose 80 % sur `domain` + `application`.

## Structure

```
sn.cse.besoins
 ├─ domain        BesoinMateriel (agrégat), StatutBesoin, DescriptionBesoin, HistoriqueBesoin,
 │                validation/ (chaîne de responsabilité), service/ (consolidation, sélection)
 ├─ application   port/in (cas d'utilisation), port/out (dépôt, requêtes, ms-suivi, événements),
 │                command/, query/, service/
 ├─ infrastructure persistence/ (JPA, séquences), client/ (ms-suivi + Resilience4j),
 │                messaging/ (outbox, listener Kafka), config/, seed/
 └─ api           BesoinController, dto/
```
