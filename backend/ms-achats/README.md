# ms-achats — Achats et approvisionnements

Microservice (port **8083**, schéma PostgreSQL `achats`, utilisateur `ms_achats`) qui couvre la mise en
concurrence et l'exécution des achats : appels d'offres, offres, évaluation multicritère, attribution,
contrats, engagements DAF, livraisons, factures, synthèse financière et portail fournisseur.
Contrat d'API : `docs/API_CONTRACT.md` §5 ; événements : §11.

## Architecture (hexagonale)

```
sn.cse.achats
 ├─ domain          agrégats AppelOffre, ContratAchat, Facture ; machines à états (StatutAO, StatutContrat,
 │                  StatutFacture) ; MoteurEvaluation + StrategieNotation (Strategy) ; SyntheseFinance
 ├─ application     ports in (un cas d'utilisation par interface) / ports out ; services @Transactional
 ├─ infrastructure  JPA (entités, chiffrement AES-GCM des montants), séquences de références, outbox,
 │                  consommateurs Kafka idempotents, client ms-fournisseurs (Resilience4j), PDF OpenPDF,
 │                  jobs planifiés, seed dev
 └─ api             contrôleurs REST fins, DTO documentés (Swagger FR), mapper
```

## Règles de gestion

| Règle | Où | Comportement |
|---|---|---|
| R02 | `AppelOffre.deposerOffre` | aucune offre après `dateLimite` → 422 `DATE_LIMITE_DEPASSEE` |
| R03 | `ContratAchat.generer` | contrat uniquement depuis un AO `ATTRIBUE` → 422 `AO_NON_ATTRIBUE` ; un seul contrat par AO (409) |
| R06 | `ContratAchat.demarrerLivraison` | engagement DAF validé requis → 422 `ENGAGEMENT_NON_VALIDE` |
| R10 | `AppelOffre.estAnonyme`, mapper | offres anonymisées (`codeAnonyme`, fournisseur null, aucune note) tant que `PUBLIE` |
| R11 | `AppelOffre.proposer/coValider/approuverAttribution` | co-validation RM toujours ; DG si montant > `cse.achats.seuil-delegation` ; refus = proposition supprimée ; toutes approbations → `ATTRIBUE` + contrat `BROUILLON` |
| R14 | `FournisseurConnu.exigerEligible` | seul un fournisseur `ACTIF` est invité ou soumissionne → 422 `FOURNISSEUR_NON_ELIGIBLE` |

Évaluation : PRIX = 100 × meilleur prix / prix, DELAI = 100 × meilleur délai / délai, autres critères saisis
(0..100) ; score = Σ poids × note / 100 (poids sommés à 100) ; départage prix puis antériorité du dépôt.

Lien AO → contrat : `AppelOffre.contratId` expose le contrat généré une fois l'AO `ATTRIBUE` (une requête
groupée par page) ; `GET /api/contrats?appelOffreId=` filtre les contrats par AO d'origine.

Contrat : `BROUILLON → SIGNE → ENGAGEMENT_VALIDE → EN_LIVRAISON → LIVRE → CLOTURE` (+ `SIGNE → BROUILLON`
quand la DAF refuse l'engagement). Pénalités = montant × `penaliteRetardPctParJour` × jours de retard,
plafonnées à `plafond-penalites-pct`. Clôture refusée si des factures restent à payer (409 `FACTURES_EN_COURS`).

## Vérification des fournisseurs

`POST /api/appels-offres` et la saisie d'offre interne appellent `GET /api/fournisseurs/{id}` (JWT relayé,
retry + circuit breaker `fournisseurs`). En cas d'échec : repli sur la projection locale
`fournisseur_projection` (alimentée par `FOURNISSEUR_*`) ; si elle ne connaît pas le fournisseur → 503
`FOURNISSEURS_INDISPONIBLE`. Le portail lit directement la projection (le jeton fournisseur n'ouvre pas ms-fournisseurs).

## Événements

- Produits (outbox, `cse.achats.events`, + entrée `AUDIT`) : `AO_PUBLIE`, `OFFRE_DEPOSEE`, `AO_CLOTURE`,
  `ATTRIBUTION_PROPOSEE`, `ATTRIBUTION_REFUSEE`, `MARCHE_ATTRIBUE` (`AppelOffrePayload`) ; `CONTRAT_GENERE`,
  `CONTRAT_SIGNE`, `ENGAGEMENT_DEMANDE`, `ENGAGEMENT_VALIDE`, `ENGAGEMENT_REJETE`, `LIVRAISON_DEMARREE`,
  `LIVRAISON_EFFECTUEE`, `ALERTE_RETARD_LIVRAISON`, `CONTRAT_CLOTURE` (`ContratPayload`, avec
  `tarifJournalierEstime` = prix unitaire × `taux-tarif-journalier`) ; `FACTURE_*` (`FacturePayload`, avec les dates métier
  `dateEmission`, `dateMandatement`, `datePaiement` pour dater mandats et paiements côté reporting).
- Consommés (idempotents, rejeu + DLT) : `BESOIN_EN_ACHAT` (`cse.besoins.events`) → `besoin_a_acheter` ;
  `FOURNISSEUR_*`, `SCORE_RECALCULE`, `DOCUMENT_EXPIRE` (`cse.fournisseurs.events`) → statut local (ignorés si plus anciens).

## Configuration (`cse.achats.*`)

| Clé | Variable | Défaut |
|---|---|---|
| `seuil-delegation` | `CSE_ACHATS_SEUIL_DELEGATION` | 150000000 |
| `penalite-retard-pct-par-jour-defaut` | `CSE_ACHATS_PENALITE_PCT_JOUR` | 0.1 |
| `plafond-penalites-pct` | `CSE_ACHATS_PLAFOND_PENALITES_PCT` | 10 |
| `taux-tarif-journalier` | `CSE_ACHATS_TAUX_TARIF_JOURNALIER` | 0.0025 |
| `intervalle-rappel-retard-jours` | `CSE_ACHATS_RAPPEL_RETARD_JOURS` | 7 |
| `fournisseurs.base-url` | `MS_FOURNISSEURS_URL` | http://localhost:8084 |
| `jobs.cloture-cron` | `CSE_ACHATS_CLOTURE_CRON` | `0 */5 * * * *` (`-` désactive) |
| `jobs.retard-livraison-cron` | `CSE_ACHATS_RETARD_CRON` | `0 0 6 * * *` |

`CSE_CRYPTO_KEY` (AES-256 Base64) est obligatoire : montants des offres, contrats, engagements, factures
et budgets chiffrés au repos (sommes calculées en Java, jamais en SQL).

## Références

`AO-2026-001`, `CTR-2026-001` via une séquence PostgreSQL par année (`seq_ref_ao_<année>`,
`seq_ref_contrat_<année>`, créées à la demande sous verrou consultatif). Les numéros de facture sont saisis.

## Données de démonstration (profil `dev`)

`DevDataSeeder` (idempotent, via agrégats + outbox, dates relatives à l'horloge sur 9 mois) : 12 AO
(PUBLIE/CLOTURE/ATTRIBUE, dont un en attente DG et un en attente de co-validation RM), 8 contrats couvrant
tous les statuts (livraisons à l'heure, en retard avec pénalités, retard en cours alerté), 12 factures
(RECUE/MANDATEE/PAYEE/REJETEE, 2 en retard), fournisseurs et chantiers §12.

Cohérence inter-services : les 11 projections `BesoinAAcheter` sont les besoins **EN_ACHAT 13 à 23** du seed
de ms-besoins (mêmes identifiants, désignation, catégorie, quantité, priorité, chantier, spécifications ; la
référence `BES-AAAA-NNNN` est recalculée selon l'ordre de soumission de ms-besoins). 9 AO en sont issus
(catégorie, quantité et spécifications reprises du besoin, désignation du contrat = celle du besoin) ;
3 AO d'outillage/topographie n'ont pas de besoin. Le `BESOIN_EN_ACHAT` publié ensuite par ms-besoins pour ces
besoins est absorbé sans doublon et conserve le lien `appelOffreId`.

## Build et tests

```bash
cd backend && flock /tmp/cse-maven.lock env MAVEN_OPTS=-Xmx768m mvn -o -q -pl ms-achats spotless:apply verify
# régénérer volontairement l'instantané OpenAPI :  ... verify -Dcse.openapi.regenerer=true
```

Tests : domaine (règles `_R02`…`_R14`, stratégies), cas d'utilisation sur ports en mémoire, `@WebMvcTest`
sécurité/ProblemDetail, WireMock de l'adaptateur ms-fournisseurs, ArchUnit, 2 classes Testcontainers
(cycle complet, instantané OpenAPI + seed). Configuration de test : `application-test.yml` (profil `test`).
