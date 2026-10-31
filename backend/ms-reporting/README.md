# ms-reporting : Tableaux de bord CQRS

Port **8086** · schéma PostgreSQL `reporting` · utilisateur `ms_reporting` · contrat : `docs/API_CONTRACT.md` §6.

Le service calcule les indicateurs des tableaux de bord **uniquement à partir de vues locales** alimentées
par les événements Kafka (D3, D8) : il n'appelle jamais les autres services et n'a pas de données de
démonstration propres (les seeders des autres services publient de vrais événements via leur outbox).
Il ne publie aucun événement (`cse.outbox.relay-enabled=false`).

## Événements consommés

Consommateurs idempotents (`processed_event`, consommateur `ms-reporting.<topic>`), un cas d'utilisation
par topic, aiguillage par `EventEnvelope.type()` ; **les types inconnus sont ignorés** (et marqués traités).
Message illisible / valeur d'énumération inconnue → DLT sans rejeu ; erreur technique → rejeu puis DLT.

| Topic | Types | Payload | Vue(s) mise(s) à jour |
|---|---|---|---|
| `cse.suivi.events` | `CHANTIER_CREE`, `CHANTIER_MODIFIE` | `ChantierPayload` | `chantiers` |
| | `MATERIEL_ENREGISTRE`, `MATERIEL_ETAT_CHANGE`, `MATERIEL_REFORME_DEMANDEE`, `MATERIEL_REFORME`, `ALERTE_MAINTENANCE` | `MaterielPayload` | `materiels` (+ alerte en attente), instantané du jour |
| | `MATERIEL_AFFECTE`, `RECEPTION_CONFIRMEE`, `AFFECTATION_TERMINEE`, `FIN_AFFECTATION_PROCHAINE` | `AffectationPayload` | `affectations`, état / chantier du matériel, instantané |
| | `MAINTENANCE_PLANIFIEE`, `MAINTENANCE_DEMARREE`, `MAINTENANCE_CLOTUREE` | `MaintenancePayload` | `maintenances`, état du matériel, alertes, résolution des pannes, instantané |
| | `PANNE_SIGNALEE` | `PannePayload` | `pannes` |
| `cse.besoins.events` | `BESOIN_SOUMIS`, `BESOIN_MODIFIE`, `BESOIN_VALIDE`, `BESOIN_REJETE`, `BESOIN_AFFECTE_DIRECT`, `BESOIN_EN_ACHAT` | `BesoinPayload` | `besoins` |
| `cse.fournisseurs.events` | `FOURNISSEUR_INSCRIT`, `_VALIDE`, `_MODIFIE`, `_SUSPENDU`, `_REACTIVE`, `_BLACKLISTAGE_DEMANDE`, `_BLACKLISTAGE_REFUSE`, `_BLACKLISTE`, `SCORE_RECALCULE`, `DOCUMENT_EXPIRE` | `FournisseurPayload` | `fournisseurs` |
| | `LITIGE_OUVERT`, `LITIGE_RESOLU` (aussi émis pour un litige classé, `statut=CLASSE`) | `LitigePayload` | `litiges` |
| `cse.achats.events` | `AO_PUBLIE`, `OFFRE_DEPOSEE`, `AO_CLOTURE`, `ATTRIBUTION_PROPOSEE`, `ATTRIBUTION_REFUSEE`, `MARCHE_ATTRIBUE` | `AppelOffrePayload` | `appels_offres` |
| | `CONTRAT_GENERE`, `CONTRAT_SIGNE`, `ENGAGEMENT_DEMANDE`, `ENGAGEMENT_VALIDE`, `ENGAGEMENT_REJETE`, `LIVRAISON_DEMARREE`, `LIVRAISON_EFFECTUEE`, `ALERTE_RETARD_LIVRAISON`, `CONTRAT_CLOTURE` | `ContratPayload` | `contrats` |
| | `FACTURE_RECUE`, `FACTURE_MANDATEE`, `FACTURE_PAYEE`, `FACTURE_REJETEE` | `FacturePayload` | `factures` |

## Vues (read models) et ordre des événements

Une table par agrégat (`chantiers`, `materiels`, `affectations`, `maintenances`, `pannes`, `besoins`,
`appels_offres`, `contrats`, `factures`, `fournisseurs`, `litiges`) + `parc_instantanes` (jour × catégorie ×
chantier courant). ENUM PostgreSQL natifs, index sur les colonnes de filtre (période, chantier, matériel).

Kafka n'ordonne que les événements d'un même agrégat. Règles de fusion (domaine, `domain.model.vue`) :
- chaque vue garde `maj_le` (instant du dernier événement appliqué) : un instantané plus ancien n'écrase pas
  un plus récent (égalité : le reçu l'emporte) ;
- un événement d'un autre agrégat crée une **référence** (chantier ou fournisseur avec nom seul, matériel
  avec code/catégorie/tarif) ; l'instantané complet la remplace ensuite. L'insertion de référence est
  atomique (`ON CONFLICT … coalesce`) et ne remplace jamais une vue complète écrite par un autre consommateur ;
- l'état d'un matériel est aussi déduit des affectations (affecté / libéré) et maintenances (démarrée /
  clôturée), sous la même garde d'horodatage ; un matériel réformé reste réformé ;
- horodatages « premiers connus » conservés : soumission et décision d'un besoin, publication d'un AO,
  engagement d'un contrat, réception / mandatement / paiement d'une facture.

Rejouer un événement ne change rien (instantanés par identifiant) : l'idempotence est double
(`processed_event` + projection idempotente par construction), testée (« même événement deux fois → compté
une fois »).

Persistance en **JDBC** (`NamedParameterJdbcTemplate`) et non JPA : les vues sont des projections sans cycle
de vie d'entité, l'upsert `INSERT … ON CONFLICT DO UPDATE` exprime directement leur sémantique. Le schéma
est validé par la migration Flyway et le test d'intégration (pas de `ddl-auto` sans Hibernate).

### Dates retenues (date métier du payload, sinon date de l'événement)

| Fait | Date |
|---|---|
| Soumission d'un besoin | jour de `BESOIN_SOUMIS` (à défaut 1er événement), ramené à `dateBesoin` si celle-ci est antérieure (besoins historiques publiés a posteriori par les seeders) |
| Décision d'un besoin | 1er instant parmi `VALIDE`, `REJETE`, `AFFECTE_DIRECT`, `EN_ACHAT` |
| Publication d'un AO | jour du 1er événement de l'AO, borné par `dateLimite` |
| Engagement d'un contrat | `dateSignature` (l'engagement est demandé à la signature, R06), à défaut jour de `ENGAGEMENT_VALIDE` ; déduit aussi de tout statut ≥ `ENGAGEMENT_VALIDE` |
| Mandatement / paiement | `FacturePayload.dateMandatement` / `datePaiement` ; à défaut (événements antérieurs à ces champs) jour de `FACTURE_MANDATEE` / `FACTURE_PAYEE` |
| Réception d'une facture | jour du 1er événement, ramené à `dateEmission` si celle-ci est antérieure |
| Coût de maintenance | `dateFin`, à défaut `dateDebut`, puis `datePlanifiee` (maintenances clôturées avec coût) |
| Panne résolue | 1re clôture de maintenance du même matériel postérieure au signalement (le payload n'a pas de statut) |
| Signalement d'une panne, réforme d'un matériel | jour de l'événement (`PannePayload` / `MaterielPayload` sans date métier) |

## Formules (domaine : `CalculateurTableau`, `CalculateurUtilisation`, `CalculateurSeries`)

Période P = `[du, au]` (défaut : 90 derniers jours, jour courant inclus) ; « écoulée » = P ∩ ]−∞, aujourd'hui].
Montants FCFA entiers ; taux en % à une décimale ; un taux de dénominateur nul vaut 0 dans `/kpis`.

- **Parc** (état courant du périmètre) : `operationnels = total − enMaintenance − réformés` (disponibles ou
  affectés ; aussi par catégorie) ; **disponibilité opérationnelle**
  `tauxDisponibilite = (total − enMaintenance − réformés) / (total − réformés)`, un matériel affecté reste
  disponible pour l'exploitation (un chef dont les 12 engins sont affectés et aucun en panne voit 100 %).
- **Taux d'utilisation** = Σ jours affectés / Σ jours opérationnels, jour par jour et matériel par matériel sur
  la période écoulée : jours opérationnels = jours écoulés − jours d'immobilisation en maintenance (début → fin,
  ou aujourd'hui si en cours, quel que soit le chantier de l'intervention) ; jours affectés = jours couverts par
  une affectation (transit compris) non immobilisés. Dénominateur : sans périmètre, matériels non réformés ;
  périmètre de chantiers (chef, `chantierId`, ligne chantier) : matériels affectés à ces chantiers sur P.
- **Coût d'exploitation** = Σ jours affectés dans P (écoulée) × `tarifJournalier` de l'affectation.
- **Coût de maintenance** = Σ coût des maintenances clôturées imputées à P.
- **Finances** : `budgetTotal` = Σ budget des chantiers actifs sur P (commencés avant `au`, non terminés avant
  `du`) ; `montantEngage` = Σ contrats engagés dans P ; `montantMandate` / `montantPaye` = Σ TTC des factures
  mandatées (non rejetées) / payées dans P ; `tauxExecutionBudget = montantEngage / budgetTotal`.
- **Marge par chantier** = `montantMarche × (jours du marché ∩ P écoulée) / durée du marché − coutExploitation
  − coutMaintenance`, durée du marché = `dateDebut → dateFinPrevue` (sans date de fin : 365 jours,
  `cse.reporting.duree-chantier-defaut-jours`). Le montant engagé (achats d'équipements) est affiché à part et
  n'est pas déduit : l'usage des équipements est déjà valorisé par le coût d'exploitation.
- **Besoins** (soumis dans P, statut courant) : comptes par statut et priorité ;
  `delaiMoyenTraitementHeures` = moyenne (décision − soumission).
- **Achats** : `aoPublies` / `aoClotures` / `aoAttribues` = AO publiés dans P par statut courant ;
  `contratsEnCours` = BROUILLON..EN_LIVRAISON ; `livraisonsEnRetard` = non livrés, prévue < aujourd'hui ;
  `delaiMoyenApprovisionnementJours` = moyenne (livraison réelle − publication de l'AO) des contrats livrés
  dans P ; `tauxLivraisonALHeure` = livrés à la date prévue ou avant / livrés avec date prévue.
- **Fournisseurs** : actifs, score moyen des actifs, litiges ouverts (OUVERT, EN_COURS) ; `top` = 5 actifs
  de meilleur score (`cse.reporting.top-fournisseurs`) avec nombre de litiges et ponctualité de leurs livraisons.
- **Chantiers** : chantiers actifs sur P ; `materielsAffectes` et `besoinsEnAttente` = état courant.

## API

| Méthode | Route | Détail |
|---|---|---|
| GET | `/api/reporting/kpis` | `?profil&du&au&chantierId` → `Tableau` |
| GET | `/api/reporting/series` | `?indicateur&granularite=JOUR\|SEMAINE\|MOIS (défaut MOIS)&du&au&chantierId` → `Point[]` |
| GET | `/api/reporting/export` | `?format=pdf\|xlsx&profil&du&au&chantierId` → fichier `tableau-de-bord-<PROFIL>-<aaaa-mm-jj>.<ext>` |

Rôles : CHEF, RM, RA, DAF, DG (autres → 403). Profils (`profil`, insensible à la casse ; `DIRECTION_GENERALE`
accepté comme alias de `DG`) : DG → tous ses profils et toutes les sections ; RM → `RESPONSABLE_MATERIEL`
(parc, besoins, chantiers) ; RA → `RESPONSABLE_ACHATS` (achats, fournisseurs) ; DAF → `DAF` (finances, achats,
chantiers) ; CHEF → `CHEF_CHANTIER` (besoins, parc, chantiers limités à `chantier_ids`). Profil non autorisé →
403 `PROFIL_NON_AUTORISE` ; chantier hors périmètre du chef → 403 `CHANTIER_NON_AUTORISE` ; sans `profil`,
profil principal (DG > DAF > RA > RM > CHEF). Les sections non affichées sont absentes du JSON.

Séries : un indicateur est accessible si un profil autorisé affiche sa section (TAUX_* → parc ;
MONTANT_ENGAGE, COUT_MAINTENANCE → finances ou chantiers ; BESOINS_SOUMIS → besoins ;
DELAI_APPROVISIONNEMENT → achats), sinon 403 `INDICATEUR_NON_AUTORISE`. Libellés `2026-07-14`, `2026-W29`
(semaine ISO, année ISO), `2026-07`. Cumuls : un point par tranche (0 possible) ; taux / moyennes : pas de point
pour une tranche sans donnée. `TAUX_DISPONIBILITE` lit `parc_instantanes` (dernier instantané ≤ fin de tranche,
reporté) avec la même formule opérationnelle ; **avant le premier instantané enregistré**, l'instantané du jour
est **reconstitué à la demande** depuis l'historique des vues (`ReconstitutionParc`, calcul pur sans écriture,
donc idempotent) : pour chaque matériel, non compté avant son premier fait connu (début de sa 1re affectation ou
maintenance, à défaut jour de son dernier événement d'état), réformé à partir du jour de l'événement de réforme,
sinon en maintenance si une immobilisation couvre le jour, sinon affecté si une affectation le couvre (chantier de
l'affectation, à défaut de la maintenance), sinon disponible. Plus de `max-points-serie` points → 400
`SERIE_TROP_LONGUE`.

Exports : PDF (OpenPDF, A4 paysage : page de titre CSE avec profil, période et date de génération, puis une
page par section, pied de page numéroté) et Excel (Apache POI : une feuille par section, en-têtes stylés,
formats `# ##0 "FCFA"`, `0.0 "%"`). Montants et taux formatés à la française (`12 500 000 FCFA`, `85,3 %`).
Section parc : « Opérationnels » et « Disponibilité opérationnelle » (aussi par catégorie : « Opérationnels »).

## Configuration (`cse.reporting.*`)

| Clé | Défaut | Rôle |
|---|---|---|
| `periode-defaut-jours` | 90 | période par défaut |
| `periode-max-jours` | 1830 | période maximale (400 `PERIODE_TROP_LONGUE`) |
| `max-points-serie` | 400 | points maximum d'une série |
| `top-fournisseurs` | 5 | taille du classement |
| `duree-chantier-defaut-jours` | 365 | proratisation du marché sans date de fin |
| `consommateur` | `ms-reporting` | préfixe des consommateurs `processed_event` |
| `instantane-cron` | `0 5 0 * * *` (UTC) | instantané quotidien du parc (`-` désactive) |

## Tests

`cd backend && mvn -o -q -pl ms-reporting spotless:apply verify`, domaine (calculs sur un jeu construit à la
main, fusions, période, ISO, politique d'accès), application (séquences d'événements, rejeu, désordre, fausse
persistance en mémoire), `@WebMvcTest` (401, 403 rôle / profil / chantier / indicateur, 400 ProblemDetail,
exports), une intégration Testcontainers (listeners appelés avec des enveloppes JSON, SQL réel, API, instantané
OpenAPI `src/test/resources/openapi.json`, régénération : `-Dopenapi.regenerer=true`), architecture.

## Limites connues

- Les vues sont chargées en mémoire par requête (filtrées par période et périmètre en SQL pour les tables
  volumineuses) : adapté au volume de la CSE ; au-delà, pré-agréger par jour.
- L'instantané du parc est recalculé à partir de tous les matériels à chaque événement d'état.
- Le taux d'utilisation exclut les matériels réformés à la date du calcul (pas d'historique de réforme).
- `TAUX_DISPONIBILITE` antérieur à la mise en service est reconstitué : un matériel réformé est compté
  réformé à partir du jour de l'**événement** de réforme (pas de date métier), et un matériel sans aucune
  affectation ni maintenance n'apparaît qu'à partir de son dernier événement d'état (pas de date d'acquisition).
- Factures publiées avant l'ajout des dates métier au `FacturePayload` : mandatement / paiement datés du jour
  de l'événement (rejouer un seed ms-achats récent pour des dates exactes).
