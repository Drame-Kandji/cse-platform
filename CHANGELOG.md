# Journal des modifications

Format inspiré de [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/) ; versions sémantiques.

## [2.1.0] - 2026-11-01 : Portail fournisseur

Le fournisseur devient acteur du circuit d'achat : il s'inscrit, dépose ses offres, signe ses contrats,
déclare ses expéditions et dépose ses factures sur un portail dédié.

### Ajouté
- **Portail fournisseur** (`apps/portail-fournisseur`, http://localhost:5174, client Keycloak
  `cse-portail-fournisseur`) : inscription publique, accueil avec liste « À faire », appels d'offres et dépôt
  d'offre, contrats (signature, refus motivé, expédition, jalons), factures, fiche entreprise et catalogue ;
  cibles `make portail` et `scripts/cse.sh portail`.
- **ms-achats** : signature en deux temps (RA puis fournisseur, statut `EN_ATTENTE_SIGNATURE`), refus motivé
  (`motifRefusSignature`), expédition (`dateExpedition`, `commentaireExpedition`) et jalons déclarés par le
  fournisseur, routes `/api/portail/contrats`, données de démonstration du fournisseur F1 à chaque étape.
- **ms-fournisseurs** et **ms-auth** : inscription publique `POST /api/portail/inscription` ; à la validation
  par le RA, création du compte Keycloak `FOURNISSEUR` et e-mail de définition du mot de passe (MailHog).
- **ms-gateway** : inscription sans jeton limitée par adresse IP ; **ms-reporting** et **ms-notification** :
  nouveau statut et règles de notification (signature, refus, expédition).
- **web-interne** : statut « En attente du fournisseur » (libellés, filtre, frise du contrat) ; fiche contrat
  avec les deux signatures, l'étape attendue (signature du fournisseur, engagement DAF, expédition), le motif
  de refus du fournisseur et l'expédition déclarée ; message de validation d'inscription indiquant l'envoi de
  l'e-mail au fournisseur.
- **Tests de bout en bout** : scénario Playwright 4 « Circuit complet avec le fournisseur » qui pilote les deux
  applications dans le navigateur ; test RestAssured du portail (cloisonnement 404, inscription 201 puis 409).
- **Documentation** : [docs/DEMO.md](docs/DEMO.md) (script de démonstration pour un jury), section « Portail
  fournisseur » du guide utilisateur et captures `docs/images/portail-*`.

### Modifié
- **web-interne** : l'action de signature devient « Signer et envoyer au fournisseur » ; l'engagement DAF naît
  de la contresignature du fournisseur ; « Démarrer la livraison » passe dans le menu « … » sous le nom
  « Démarrer la livraison (hors portail) », le cas nominal étant l'expédition déclarée par le fournisseur.
- Scénario 2 (Playwright et RestAssured) : contresignature du fournisseur avant l'engagement DAF, livraison
  démarrée hors portail.

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
