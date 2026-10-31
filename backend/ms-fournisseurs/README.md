# ms-fournisseurs : Référentiel fournisseurs

Port **8084**, schéma PostgreSQL **`fournisseurs`** (utilisateur `ms_fournisseurs`), topic produit
`cse.fournisseurs.events`. Contrat : `docs/API_CONTRACT.md` §4 (routes), §11 (événements), §12 (données).

## Périmètre

| Domaine | Contenu |
|---|---|
| Référentiel | informations légales (NINEA unique, RCCM), contacts, spécialités (`CategorieMateriel`), certifications |
| Cycle de vie | `EN_ATTENTE_VALIDATION → ACTIF ⇄ SUSPENDU → BLACKLISTE`, table explicite dans `StatutFournisseur`, transition invalide → **409** `TRANSITION_STATUT_INVALIDE` |
| Blacklistage (R09) | étape 1 : le RA demande (`POST …/blacklistage/demande`, fournisseur **SUSPENDU**), étape 2 : la DG approuve/refuse (`PATCH …/blacklister`) ; le demandeur ne peut pas statuer ; réactivation bloquée tant qu'une demande est en attente |
| Documents | documents administratifs avec date d'expiration ; `expire` et `documentsExpires` calculés à la date du jour |
| Coordonnées bancaires | chiffrées au repos AES-256-GCM (`EncryptedStringConverter`, clé `CSE_CRYPTO_KEY`) ; renvoyées dans `FournisseurDetail` **uniquement au RA et à la DAF** (`null` sinon) |
| Évaluations / score (R07) | 5 critères notés 0..10 → note globale 0..100 ; score 0..100 calculé par la stratégie `ScorePolicy` (`ScorePondere`) et recalculé après chaque évaluation, livraison (`LIVRAISON_EFFECTUEE`), réception (`RECEPTION_CONFIRMEE`), ouverture et clôture de litige |
| Classement | fournisseurs **ACTIF** (filtrables par catégorie) par score, ponctualité, nombre d'évaluations |
| Catalogue | upsert par référence ; prix initial et chaque changement de prix historisés (`PrixHistorique`) ; comparaison multi-fournisseurs (actifs) par catégorie + désignation normalisée |
| Litiges | référence `LIT-AAAA-NNN` (séquence PostgreSQL par année), chronologie d'actions, `OUVERT → EN_COURS → RESOLU/CLASSE` |
| Portail (SPEC §B.6) | `GET/PUT /api/portail/profil`, `GET/PUT /api/portail/catalogue`, rôle `FOURNISSEUR`, limité au claim `fournisseur_id` |

## Score de performance (R07)

```
note globale  = Σ(poids_i × note_i) / Σ poids × 10                       (0..100)
composante    = moyenne des notes globales (score-initial si aucune évaluation)
score         = composante × (100 − p)/100 + tauxLivraisonALHeure × p/100   (si livraisons suivies)
              − penalite-litige-ouvert × litiges OUVERT/EN_COURS
              − penalite-litige-resolu × litiges RESOLU                     (CLASSE : aucune pénalité)
borné à [0, 100], une décimale ; ponctualité = dateLivraisonReelle ≤ dateLivraisonPrevue (ContratPayload)
```

Paramètres (`cse.fournisseurs.score.*`, surchargés par variables d'environnement) :

| Propriété | Défaut | Variable |
|---|---|---|
| `poids-qualite` / `poids-delais` / `poids-conformite` / `poids-sav` / `poids-prix` | 30 / 25 / 20 / 10 / 15 | `CSE_SCORE_POIDS_*` |
| `poids-livraison-a-l-heure` (%) | 20 | `CSE_SCORE_POIDS_LIVRAISON` |
| `penalite-litige-ouvert` / `penalite-litige-resolu` | 5 / 2 | `CSE_SCORE_PENALITE_LITIGE_*` |
| `score-initial` | 50 | `CSE_SCORE_INITIAL` |
| `seuil-alerte` | 40 | `CSE_SCORE_SEUIL_ALERTE` |

Job de surveillance (`cse.fournisseurs.surveillance.*`, défaut `0 0 6 * * *`, zone `Africa/Dakar`) :
documents expirés non signalés → `DOCUMENT_EXPIRE` (une fois par document) ; fournisseurs actifs sous le
seuil → `SCORE_RECALCULE` avec motif `ALERTE : …`.

## Routes (contrat §4)

| Méthode | Route | Rôles |
|---|---|---|
| GET | `/api/fournisseurs?q&specialite&statut&page&size&sort` | RA, RM, DAF, DG (tri : raisonSociale, scorePerformance, dateInscription, statut, ville) |
| GET | `/api/fournisseurs/{id}` | RA, RM, DAF, DG |
| POST / PUT | `/api/fournisseurs[/{id}]` | RA |
| PATCH | `/api/fournisseurs/{id}/valider-inscription` · `/suspendre` · `/reactiver` | RA |
| POST | `/api/fournisseurs/{id}/blacklistage/demande` | RA |
| PATCH | `/api/fournisseurs/{id}/blacklister` | DG |
| POST | `/api/fournisseurs/{id}/documents` | RA |
| POST | `/api/fournisseurs/{id}/evaluer` | RA, RM |
| GET | `/api/fournisseurs/{id}/evaluations` · `/catalogue` · `/prix/historique?reference` | lecteurs |
| POST | `/api/fournisseurs/{id}/catalogue` | RA |
| GET | `/api/fournisseurs/classement?categorie` · `/comparaison-prix?categorie&q` · `/statistiques` | lecteurs |
| GET | `/api/litiges?statut&fournisseurId&page&size&sort` | RA, RM, DG |
| POST | `/api/fournisseurs/{id}/litiges` | RA, RM |
| PATCH | `/api/litiges/{id}/action` | RA |
| GET / PUT | `/api/portail/profil` · `/api/portail/catalogue` | FOURNISSEUR |

Swagger UI : `http://localhost:8084/swagger-ui.html` ; contrat figé : `src/test/resources/openapi.json`.

## Événements

Produits via l'outbox (`OutboxWriter`, une entrée `AUDIT` est ajoutée automatiquement) :
`FOURNISSEUR_INSCRIT`, `FOURNISSEUR_VALIDE`, `FOURNISSEUR_SUSPENDU`, `FOURNISSEUR_REACTIVE`,
`FOURNISSEUR_BLACKLISTAGE_DEMANDE`, `FOURNISSEUR_BLACKLISTE`, `SCORE_RECALCULE`, `DOCUMENT_EXPIRE`
(`FournisseurPayload`) ; `LITIGE_OUVERT`, `LITIGE_RESOLU` (`LitigePayload`, aussi émis au classement avec
`statut=CLASSE`). Types locaux hors §11 : `FOURNISSEUR_MODIFIE`, `FOURNISSEUR_BLACKLISTAGE_REFUSE`.

Consommés (idempotents, `processed_event`) : `cse.achats.events` / `LIVRAISON_EFFECTUEE` (ponctualité +
recalcul) ; `cse.suivi.events` / `RECEPTION_CONFIRMEE` avec `fournisseurId` (recalcul).

## Architecture

```
sn.cse.fournisseurs
 ├─ domain        Fournisseur, Litige, LigneCatalogue, Evaluation… ; ScorePolicy/ScorePondere,
 │                ComparateurPrix, ClassementFournisseurs (Java pur)
 ├─ application   ports in (cas d'utilisation) / out (dépôts, outbox, références) ; services @Transactional
 ├─ infrastructure persistance JPA (entités distinctes, ENUM natifs, @Version, audit), outbox, Kafka,
 │                configuration validée, job planifié, DevDataSeeder
 └─ api           contrôleurs fins, DTO records documentés (exemples FR), erreurs ProblemDetail
```

## Données de démonstration (profil `dev`)

`DevDataSeeder` (idempotent, passe par le domaine et l'outbox) : les 7 fournisseurs du contrat §12
(UUID `f0000000-…-00000000000N`, noms fictifs), documents dont 4 expirés, évaluations sur 9 mois,
livraisons, catalogues avec historique (pelles, camions-bennes, groupes électrogènes, outillage,
informatique, ciment/bitume), 5 litiges (OUVERT, EN_COURS, RESOLU, CLASSE) et une demande de
blacklistage en attente sur le fournisseur 6 (SUSPENDU).

## Lancer / tester

```bash
cd backend
flock /tmp/cse-maven.lock env MAVEN_OPTS=-Xmx768m mvn -o -q -pl ms-fournisseurs spotless:apply verify
# régénérer volontairement le contrat OpenAPI figé :
... verify -Dopenapi.regenerer=true
CSE_CRYPTO_KEY=<base64 32 octets> SPRING_PROFILES_ACTIVE=dev java -jar ms-fournisseurs/target/ms-fournisseurs-1.0.0-SNAPSHOT.jar
```

Tests : domaine et application (adaptateurs en mémoire, règles nommées `_R07`, `_R09`, `_R14`),
`@WebMvcTest` sécurité (401/403/ProblemDetail, coordonnées bancaires masquées pour le RM),
2 classes d'intégration Testcontainers (chiffrement au repos, parcours complet, outbox, consommateurs
idempotents, snapshot OpenAPI ; jeu de démonstration), ArchUnit (`HexagonalArchitectureRules`).
JaCoCo ≥ 80 % imposé sur `domain` + `application`.

## Limites connues

- Le job planifié n'a pas de verrou distribué : en multi-instance, prévoir ShedLock ou une instance
  dédiée.
- Les documents administratifs sont des métadonnées (pas de stockage de fichier).
- La raison sociale d'un litige est un instantané pris à l'ouverture.
