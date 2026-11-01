# Contrat d'API et d'événements : Plateforme CSE (v2)

Ce document est **la référence unique** partagée par les microservices et le frontend `web-interne`.
Tout écart doit être corrigé ici d'abord. Notation des DTO : pseudo-TypeScript (`?` = optionnel / nullable).

## 0. Conventions globales

- Base : toutes les routes passent par la passerelle `http://localhost:8080` (préfixe `/api`).
- Identifiants : UUID (chaîne). Dates : `LocalDate` → `"2026-10-30"` ; instants → ISO-8601 UTC `"2026-10-30T09:15:00Z"`.
- Montants : nombre entier en **FCFA** (BigDecimal sans décimales côté Java).
- JSON en camelCase ; enums en `UPPER_SNAKE` (valeurs listées en §1).
- **Listes paginées** : `?page=0&size=20&sort=champ,asc|desc` (size max **50**, tri limité à une liste blanche par ressource) → `PageResponse<T>` :
  `{ content: T[], page, size, totalElements, totalPages, first, last }`.
- **Erreurs** : RFC 7807 `application/problem+json` :
  `{ type, title, status, detail (FR), code, traceId, timestamp, errors?: [{ field, message }] }`.
  Codes HTTP : 400 validation, 401 non authentifié, 403 rôle insuffisant, 404 introuvable, 409 conflit/concurrence/règle d'état, 422 règle métier (ex. R02, R04, R08).
- **Sécurité** : `Authorization: Bearer <JWT Keycloak>` sur toutes les routes `/api/**`. Rôles lus dans `realm_access.roles`.
  Claims supplémentaires : `chantier_ids` (string[], chefs de chantier), `fournisseur_id` (fournisseurs).
- **Corrélation** : en-tête `X-Correlation-Id` accepté et renvoyé ; propagé entre services et dans les événements.
- Concurrence : les DTO modifiables exposent `version` (verrou optimiste) ; conflit → 409 `CONFLIT_CONCURRENCE`.

### Rôles (`Roles`)
| Code | Libellé FR | Abréviation |
|---|---|---|
| `CHEF_CHANTIER` | Chef de chantier | CHEF |
| `RESPONSABLE_MATERIEL` | Responsable Matériel | RM |
| `RESPONSABLE_ACHATS` | Responsable des Achats | RA |
| `DAF` | Direction Administrative et Financière | DAF |
| `DIRECTION_GENERALE` | Direction Générale | DG |
| `ADMIN_SYSTEME` | Administrateur Système | ADMIN |
| `FOURNISSEUR` | Fournisseur (portail externe) | FOURN |

## 1. Énumérations

| Enum | Valeurs |
|---|---|
| `CategorieMateriel` | `MATERIEL_ROULANT`, `OUTILLAGE`, `EQUIPEMENT_INFORMATIQUE`, `CONSOMMABLE` |
| `EtatMateriel` | `DISPONIBLE`, `AFFECTE`, `EN_MAINTENANCE`, `REFORME` |
| `StatutChantier` | `EN_COURS`, `TERMINE` |
| `StatutBesoin` | `EN_ATTENTE`, `VALIDE`, `REJETE`, `AFFECTE_DIRECT`, `EN_ACHAT` |
| `Priorite` | `NORMAL`, `URGENT`, `CRITIQUE` |
| `StatutAffectation` | `EN_ATTENTE_RECEPTION`, `EN_COURS`, `TERMINEE` |
| `TypeMaintenance` | `PREVENTIVE`, `CORRECTIVE`, `LOURDE` |
| `StatutMaintenance` | `PLANIFIEE`, `EN_COURS`, `CLOTUREE` |
| `GravitePanne` | `MINEURE`, `MAJEURE`, `BLOQUANTE` |
| `StatutPanne` | `SIGNALEE`, `PRISE_EN_CHARGE`, `RESOLUE` |
| `StatutFournisseur` | `EN_ATTENTE_VALIDATION`, `ACTIF`, `SUSPENDU`, `BLACKLISTE` |
| `TypeDocument` | `NINEA`, `RCCM`, `QUITUS_FISCAL`, `ASSURANCE`, `CERTIFICATION`, `AUTRE` |
| `GraviteLitige` | `FAIBLE`, `MOYENNE`, `ELEVEE` |
| `StatutLitige` | `OUVERT`, `EN_COURS`, `RESOLU`, `CLASSE` |
| `TypeAppelOffre` | `APPEL_OFFRES`, `DEMANDE_PRIX` |
| `StatutAO` | `PUBLIE`, `CLOTURE`, `ATTRIBUE` |
| `CritereEvaluation` | `PRIX`, `DELAI`, `QUALITE_TECHNIQUE`, `GARANTIE`, `SAV`, `REFERENCES` |
| `StatutApprobation` | `EN_ATTENTE`, `APPROUVEE`, `REFUSEE` |
| `StatutContrat` | `BROUILLON`, `EN_ATTENTE_SIGNATURE`, `SIGNE`, `ENGAGEMENT_VALIDE`, `EN_LIVRAISON`, `LIVRE`, `CLOTURE` |
| `StatutJalon` | `A_VENIR`, `ATTEINT`, `EN_RETARD` |
| `StatutFacture` | `RECUE`, `MANDATEE`, `PAYEE`, `REJETEE` |
| `NiveauNotification` | `INFO`, `SUCCES`, `ALERTE`, `CRITIQUE` |
| `TypeProcesVerbal` | `OUVERTURE`, `EVALUATION` |

## 2. ms-suivi (port 8085) : chantiers, parc, affectations, maintenance

```ts
Chantier { id, code, nom, localisation, region, latitude, longitude, statut: StatutChantier,
  budget, montantMarche, dateDebut, dateFinPrevue?, chefChantierId?, chefChantierNom?,
  nombreMateriels, version }
Materiel { id, code, immatriculation?, designation, categorie, typeEnginId?, typeEnginLibelle?,
  marque?, modele?, etat: EtatMateriel, heuresFonctionnement, heuresDepuisMaintenance,
  seuilMaintenanceHeures?, prochaineMaintenance?: date, maintenanceDue: boolean,
  tarifJournalier, valeurAcquisition?, dateAcquisition?, chantierActuelId?, chantierActuelNom?,
  latitude?, longitude?, fournisseurId?, contratId?, reformeDemandee: boolean, motifReforme?, version }
MaterielDetail = Materiel & { affectations: Affectation[], maintenances: Maintenance[],
  pannes: Panne[], mouvements: Mouvement[] }   // 20 derniers de chaque
Affectation { id, materielId, materielCode, materielDesignation, categorie, chantierId, chantierNom,
  besoinId?, dateDebut, dateFinPrevue, dateFinReelle?, statut: StatutAffectation,
  dateReception?, observationsReception?, fournisseurId?, contratId? }
Mouvement { id, materielId, type: 'AFFECTATION'|'RECEPTION'|'LIBERATION'|'MAINTENANCE'|'REFORME'|'ENREGISTREMENT',
  chantierOrigineNom?, chantierDestinationNom?, date, auteurNom?, commentaire? }
Maintenance { id, numeroOt, materielId, materielCode, materielDesignation, chantierId?, chantierNom?,
  type: TypeMaintenance, statut: StatutMaintenance, description, datePlanifiee, dateDebut?, dateFin?,
  technicien?, cout?, heuresCompteur?, observations?, panneId? }
Panne { id, materielId, materielCode, materielDesignation, chantierId?, chantierNom?, gravite,
  description, statut: StatutPanne, signaleParId, signaleParNom, dateSignalement, maintenanceId? }
TypeEngin { id, code, libelle, categorie, seuilMaintenanceHeures?, seuilMaintenanceJours?, actif }
Disponibilite { disponible: boolean, quantiteDemandee, quantiteDisponible, materiels: Materiel[] }
```

| Méthode | Route | Rôles | Corps / paramètres → réponse |
|---|---|---|---|
| GET | `/api/chantiers` | tous internes (CHEF : ses `chantier_ids`) | `?statut&q&page&size&sort` → `Page<Chantier>` |
| GET | `/api/chantiers/{id}` | idem | → `Chantier` |
| POST | `/api/chantiers` | RM, ADMIN | `{code,nom,localisation,region,latitude,longitude,budget,montantMarche,dateDebut,dateFinPrevue?,chefChantierId?,chefChantierNom?}` → 201 `Chantier` |
| PUT | `/api/chantiers/{id}` | RM, ADMIN | mêmes champs + `version` → `Chantier` |
| PATCH | `/api/chantiers/{id}/terminer` | RM | → `Chantier` (refus 422 si matériels encore affectés) |
| GET | `/api/materiels` | internes | `?etat&categorie&chantierId&q&maintenanceDue&page&size&sort` → `Page<Materiel>` |
| GET | `/api/materiels/{id}` | internes | → `MaterielDetail` |
| POST | `/api/materiels` | RM | `{code,immatriculation?,designation,categorie,typeEnginId?,marque?,modele?,tarifJournalier,valeurAcquisition?,dateAcquisition?,heuresFonctionnement?}` → 201 |
| PUT | `/api/materiels/{id}` | RM | champs descriptifs + `version` |
| GET | `/api/materiels/disponibilite` | internes | `?categorie&designation&quantite&du&au` → `Disponibilite` |
| POST | `/api/materiels/{id}/affecter` | RM | `{chantierId, besoinId?, dateDebut, dateFinPrevue}` → 201 `Affectation` (R04, chevauchement → 409 `CONFLIT_AFFECTATION`) |
| POST | `/api/materiels/{id}/confirmer-reception` | CHEF (son chantier), RM | `{affectationId?, observations?}` → `Affectation` (statut `EN_COURS`) |
| POST | `/api/materiels/{id}/liberer` | RM | `{dateFinReelle?, commentaire?}` → `Affectation` (`TERMINEE`, matériel `DISPONIBLE`) |
| POST | `/api/materiels/{id}/heures` | CHEF, RM | `{heuresFonctionnement}` relevé compteur → `Materiel` |
| PATCH | `/api/materiels/{id}/etat` | RM | `{etat: DISPONIBLE|EN_MAINTENANCE, motif}` (jamais REFORME ici) → `Materiel` |
| POST | `/api/materiels/{id}/reforme/demande` | RM | `{motif}` → `Materiel` |
| PATCH | `/api/materiels/{id}/reforme/valider` | DG | `{approuve, commentaire?}` → `Materiel` (R13) |
| GET | `/api/materiels/carte` | internes | `?categorie&etat` → GeoJSON `FeatureCollection` ; `properties: {kind:'CHANTIER'|'MATERIEL', id, code, libelle, categorie?, etat?, statut?, chantierId?, chantierNom?, nombreMateriels?}` |
| GET | `/api/affectations` | internes (CHEF : ses chantiers) | `?chantierId&materielId&statut&page&size` → `Page<Affectation>` |
| GET | `/api/affectations/planning` | internes | `?chantierId&categorie&du&au` (≤ 366 j) → `Affectation[]` |
| GET | `/api/maintenances` | internes | `?statut&type&materielId&page&size&sort` → `Page<Maintenance>` |
| POST | `/api/maintenances` | RM | `{materielId,type,description,datePlanifiee,technicien?,panneId?}` → 201 |
| PATCH | `/api/maintenances/{id}/demarrer` | RM | → `Maintenance` (matériel → `EN_MAINTENANCE`) |
| PATCH | `/api/maintenances/{id}/cloturer` | RM | `{cout, observations?, heuresCompteur?}` → `Maintenance` (matériel → `AFFECTE` si affectation en cours, sinon `DISPONIBLE`) |
| GET | `/api/pannes` | internes | `?statut&chantierId&materielId&page&size` → `Page<Panne>` |
| POST | `/api/pannes` | CHEF, RM | `{materielId, gravite, description}` → 201 |
| GET | `/api/referentiels/types-engins` | internes | → `TypeEngin[]` |
| POST/PUT | `/api/referentiels/types-engins[/{id}]` | ADMIN | `{code,libelle,categorie,seuilMaintenanceHeures?,seuilMaintenanceJours?,actif}` |

Jobs : alerte maintenance préventive (heures/jours), fin d'affectation à J-3, contrôle quotidien.
Consomme : `LIVRAISON_EFFECTUEE` (crée `quantite` matériels + affectation `EN_ATTENTE_RECEPTION` au chantier du besoin).

## 3. ms-besoins (port 8082)

```ts
Besoin { id, reference /* BES-2026-0001 */, designation, categorie, typeEnginId?, quantite, specifications?,
  priorite, statut, chantierId, chantierNom, demandeurId, demandeurNom, dureeUtilisationJours,
  dateBesoin, justification?, motifRejet?, validateurId?, validateurNom?, dateValidation?,
  materielIdsAffectes: string[], orientationEnAttente: boolean, createdAt, updatedAt, version }
HistoriqueBesoin { id, statut, action, auteurId, auteurNom, commentaire?, date }
Consolidation { categorie, designation, quantiteTotale, nombreDemandes, prioriteMax,
  chantiers: {id, nom}[], besoinIds: string[], doublonPotentiel: boolean }
StatistiquesBesoins { total, parStatut: Record<StatutBesoin, number>, parPriorite: Record<Priorite, number>,
  delaiMoyenValidationHeures? }
```

| Méthode | Route | Rôles | Détail |
|---|---|---|---|
| POST | `/api/besoins` | CHEF (ses chantiers), RM | `{chantierId,designation,categorie,typeEnginId?,quantite,specifications?,priorite,dureeUtilisationJours,dateBesoin,justification?}` → 201. R08 : justification obligatoire si `URGENT`/`CRITIQUE` (422 `JUSTIFICATION_OBLIGATOIRE`). Chantier vérifié actif via ms-suivi. |
| PUT | `/api/besoins/{id}` | demandeur | mêmes champs + `version` ; uniquement `EN_ATTENTE` (R05 → 409 `BESOIN_NON_MODIFIABLE`) |
| GET | `/api/besoins/{id}` | CHEF (ses chantiers), RM, RA, DG | → `Besoin` |
| GET | `/api/besoins` | idem (CHEF filtré) | `?chantierId&statut&priorite&categorie&q&du&au&page&size&sort` → `Page<Besoin>` |
| PATCH | `/api/besoins/{id}/valider` | **RM uniquement (R01)** | `{commentaire?, materielIds?: string[]}` → `Besoin`. Interroge `GET /api/materiels/disponibilite` ; si suffisant → affecte via `POST /api/materiels/{id}/affecter` (JWT relayé) → `AFFECTE_DIRECT` ; sinon → `EN_ACHAT`. ms-suivi indisponible → reste `VALIDE` avec `orientationEnAttente=true`. |
| POST | `/api/besoins/{id}/orienter` | RM | relance l'orientation d'un besoin `VALIDE` dont `orientationEnAttente=true` (même logique ; pas de job système : l'affectation exige le JWT d'un RM) → `Besoin` |
| PATCH | `/api/besoins/{id}/rejeter` | RM | `{motif}` obligatoire (R08) → `Besoin` |
| GET | `/api/besoins/{id}/historique` | lecteurs du besoin | → `HistoriqueBesoin[]` (ordre chronologique) |
| GET | `/api/besoins/consolidation` | RM | `?statut=EN_ATTENTE|VALIDE` → `Consolidation[]` |
| GET | `/api/besoins/statistiques` | internes (CHEF filtré) | → `StatistiquesBesoins` |

## 4. ms-fournisseurs (port 8084)

```ts
Fournisseur { id, raisonSociale, ninea, rccm?, adresse?, ville?, pays, telephone?, email,
  contactNom?, contactFonction?, specialites: CategorieMateriel[], certifications: string[],
  statut: StatutFournisseur, motifStatut?, scorePerformance /*0..100*/, nombreEvaluations,
  tauxLivraisonALHeure? /*0..100*/, nombreLitigesOuverts, documentsExpires, dateInscription,
  blacklistageDemande?: { demandeurNom, motif, date }, version }
FournisseurDetail = Fournisseur & { documents: DocumentAdministratif[], coordonneesBancaires?: { banque, iban } /* RA et DAF seulement */ }
DocumentAdministratif { id, type: TypeDocument, libelle, dateExpiration?, expire: boolean }
Evaluation { id, contratId?, qualite, delais, conformite, sav, prix /* 0..10 */, noteGlobale /*0..100*/,
  commentaire?, evaluateurNom, date }
LigneCatalogue { id, reference, designation, categorie, prixUnitaire, unite, delaiLivraisonJours, dateMaj }
PrixHistorique { reference, designation, prixUnitaire, date }
Classement { rang, fournisseurId, raisonSociale, scorePerformance, nombreEvaluations,
  tauxLivraisonALHeure?, nombreLitiges, specialites }
ComparaisonPrix { designation, reference?, categorie, prixMin, prixMax, prixMoyen,
  offres: { fournisseurId, raisonSociale, prixUnitaire, delaiLivraisonJours, scorePerformance }[] }
Litige { id, reference, fournisseurId, raisonSociale, contratId?, objet, description, gravite, statut,
  dateOuverture, dateResolution?, actions: { date, auteurNom, type: 'OUVERTURE'|'ACTION'|'RESOLUTION'|'CLASSEMENT', commentaire }[] }
StatistiquesFournisseurs { total, actifs, enAttente, suspendus, blacklistes, scoreMoyen, litigesOuverts, documentsExpires }
```

| Méthode | Route | Rôles | Détail |
|---|---|---|---|
| GET | `/api/fournisseurs` | RA, RM, DAF, DG | `?q&specialite&statut&page&size&sort` → `Page<Fournisseur>` |
| GET | `/api/fournisseurs/{id}` | idem | → `FournisseurDetail` |
| POST | `/api/fournisseurs` | RA | `{raisonSociale,ninea,rccm?,adresse?,ville?,pays,telephone?,email,contactNom?,contactFonction?,specialites,certifications,coordonneesBancaires?}` → 201 (statut `ACTIF`) |
| PUT | `/api/fournisseurs/{id}` | RA | idem + `version` |
| PATCH | `/api/fournisseurs/{id}/valider-inscription` | RA | `EN_ATTENTE_VALIDATION → ACTIF` |
| PATCH | `/api/fournisseurs/{id}/suspendre` | RA | `{motif}` |
| PATCH | `/api/fournisseurs/{id}/reactiver` | RA | `SUSPENDU → ACTIF` |
| POST | `/api/fournisseurs/{id}/blacklistage/demande` | RA | `{motif}` (étape 1, R09), **uniquement sur un fournisseur `SUSPENDU`** (machine à états §B.3 ; sinon 409) ; refusée si une demande est déjà en cours |
| PATCH | `/api/fournisseurs/{id}/blacklister` | DG | `{approuve, commentaire?}` (étape 2, R09) |
| POST | `/api/fournisseurs/{id}/documents` | RA | `{type,libelle,dateExpiration?}` → `DocumentAdministratif` |
| POST | `/api/fournisseurs/{id}/evaluer` | RA, RM | `{contratId?,qualite,delais,conformite,sav,prix,commentaire?}` → 201 `Evaluation` (score recalculé, R07) |
| GET | `/api/fournisseurs/{id}/evaluations` | lecteurs | → `Evaluation[]` |
| GET | `/api/fournisseurs/classement` | lecteurs | `?categorie` → `Classement[]` (ACTIF uniquement) |
| GET | `/api/fournisseurs/{id}/catalogue` | lecteurs | → `LigneCatalogue[]` |
| POST | `/api/fournisseurs/{id}/catalogue` | RA | `{reference,designation,categorie,prixUnitaire,unite,delaiLivraisonJours}` upsert par référence (historise le prix) |
| GET | `/api/fournisseurs/{id}/prix/historique` | lecteurs | `?reference` → `PrixHistorique[]` |
| GET | `/api/fournisseurs/comparaison-prix` | lecteurs | `?categorie&q` → `ComparaisonPrix[]` |
| GET | `/api/fournisseurs/statistiques` | lecteurs | → `StatistiquesFournisseurs` |
| GET | `/api/litiges` | RA, RM, DG | `?statut&fournisseurId&page&size` → `Page<Litige>` |
| POST | `/api/fournisseurs/{id}/litiges` | RA, RM | `{contratId?,objet,description,gravite}` → 201 `Litige` |
| PATCH | `/api/litiges/{id}/action` | RA | `{type:'ACTION'|'RESOLUTION'|'CLASSEMENT', commentaire}` → `Litige` |

R14 : `GET /api/fournisseurs?statut=ACTIF&specialite=X` sert à ms-achats pour vérifier les invités.
Consomme : `LIVRAISON_EFFECTUEE` (ponctualité), `RECEPTION_CONFIRMEE` (déclenche recalcul R07).
Job quotidien : documents expirés, score < seuil (`cse.fournisseurs.seuil-alerte-score`).

## 5. ms-achats (port 8083) : appels d'offres, contrats, engagements, factures

```ts
CriterePondere { code: CritereEvaluation, poids /* somme = 100 */ }
AppelOffre { id, reference /* AO-2026-001 */, objet, description?, type: TypeAppelOffre, besoinId?,
  besoinReference?, chantierId?, chantierNom?, categorie, quantite, specifications?, budgetEstime?,
  datePublication, dateLimite /* instant */, statut: StatutAO, criteres: CriterePondere[],
  fournisseursInvites: { id, raisonSociale }[], nombreOffres, offreRetenueId?, montantAttribue?,
  attribution?: Attribution, contratId? /* contrat généré une fois ATTRIBUE */, creeParNom, version }
Attribution { offreId, codeAnonyme, fournisseurNom, montant, justification, proposeeParNom, dateProposition,
  coValidationMateriel: Approbation, approbationDg?: Approbation /* présent si montant > seuil (R11) */ }
Approbation { statut: StatutApprobation, parNom?, date?, commentaire? }
Offre { id, appelOffreId, codeAnonyme, fournisseurId? /* null tant que PUBLIE (R10) */, fournisseurNom?,
  montantPropose, delaiLivraisonJours, garantieMois?, description?, dateDepot,
  notes: Partial<Record<CritereEvaluation, number /*0..100*/>>, scoreGlobal?, rang?, retenue: boolean }
Comparatif { appelOffreId, criteres: CriterePondere[], offres: Offre[] /* triées par rang */, recommandationOffreId? }
Contrat { id, numero /* CTR-2026-001 */, appelOffreId, appelOffreReference, offreId, fournisseurId, fournisseurNom,
  besoinId?, chantierId?, chantierNom?, objet, designation, categorie, quantite, montant, statut: StatutContrat,
  dateSignature?, delaiLivraisonJours, dateLivraisonPrevue?, dateLivraisonReelle?, numeroBonLivraison?,
  clauses: { garantieMois?, conditionsMaintenance?, conditionsLivraison?, penaliteRetardPctParJour? },
  engagement?: { statut: StatutApprobation, montant, parNom?, dateDemande, dateDecision?, commentaire? },
  jalons: { id, libelle, datePrevue, dateReelle?, statut: StatutJalon }[], penalitesRetard,
  signatureCse?: { parNom, date }, signatureFournisseur?: { parNom, date }, motifRefusSignature?,
  dateExpedition?, commentaireExpedition?, version }
Engagement { contratId, contratNumero, fournisseurNom, chantierNom?, objet, montant, statut: StatutApprobation, dateDemande, dateDecision?, parNom? }
Facture { id, numero, contratId, contratNumero, fournisseurId, fournisseurNom, montantHt, tva, montantTtc,
  dateEmission, dateEcheance, statut: StatutFacture, referenceMandat?, dateMandatement?, datePaiement?,
  motifRejet?, enRetard: boolean }
BesoinAAcheter { besoinId, reference, designation, categorie, quantite, specifications?, priorite,
  chantierId, chantierNom, dateReception, appelOffreId? }
SyntheseFinance { montantEngage, montantEngagementsEnAttente, montantFacture, montantMandate, montantPaye,
  facturesEnRetard, engagementsEnAttente }
```

| Méthode | Route | Rôles | Détail |
|---|---|---|---|
| GET | `/api/besoins-a-acheter` | RA, RM | `?page&size` → `Page<BesoinAAcheter>` (projection de `BESOIN_EN_ACHAT`) |
| GET | `/api/appels-offres` | RA, RM, DG, DAF | `?statut&type&q&page&size&sort` → `Page<AppelOffre>` |
| GET | `/api/appels-offres/{id}` | idem | → `AppelOffre` |
| POST | `/api/appels-offres` | RA | `{besoinId?,objet,description?,type,categorie,quantite,specifications?,budgetEstime?,dateLimite,criteres,fournisseurIdsInvites}` → 201 (statut `PUBLIE`, invités vérifiés `ACTIF` via ms-fournisseurs, R14) |
| POST | `/api/appels-offres/{id}/offres` | RA | saisie d'une offre reçue hors portail `{fournisseurId,montantPropose,delaiLivraisonJours,garantieMois?,description?}` → 201 (R02, R14) |
| GET | `/api/appels-offres/{id}/offres` | RA, RM, DG | → `Offre[]` (anonymisées tant que `PUBLIE`, R10) |
| POST | `/api/appels-offres/{id}/cloturer` | RA | → `AppelOffre` (`CLOTURE`, lève l'anonymat ; auto à `dateLimite`) |
| PUT | `/api/appels-offres/{id}/offres/{offreId}/evaluation` | RA, RM | `{QUALITE_TECHNIQUE?,GARANTIE?,SAV?,REFERENCES?}` notes 0..100 (PRIX et DELAI calculés) ; AO `CLOTURE` requis → `Offre` |
| GET | `/api/appels-offres/{id}/comparatif` | RA, RM, DG | → `Comparatif` |
| POST | `/api/appels-offres/{id}/attribuer` | RA | `{offreId, justification}` → `AppelOffre` (crée `attribution`) |
| POST | `/api/appels-offres/{id}/attribution/co-valider` | RM | `{approuve, commentaire?}` (R11) |
| POST | `/api/appels-offres/{id}/attribution/approuver` | DG | `{approuve, commentaire?}` (R11, si montant > `cse.achats.seuil-delegation`) |
|  |  |  | Toutes approbations OK → `ATTRIBUE` + contrat `BROUILLON` généré (R03). Refus → attribution supprimée. |
| GET | `/api/appels-offres/{id}/proces-verbaux/{type}` | RA, RM, DG | PDF (`application/pdf`) ; `OUVERTURE` après clôture, `EVALUATION` après attribution |
| GET | `/api/contrats` | RA, RM, DAF, DG | `?statut&fournisseurId&appelOffreId&q&page&size&sort` → `Page<Contrat>` |
| GET | `/api/contrats/{id}` | idem | → `Contrat` |
| POST | `/api/contrats` | RA | `{appelOffreId}` → 201 (si absent ; R03) |
| PUT | `/api/contrats/{id}/clauses` | RA | `{garantieMois?,conditionsMaintenance?,conditionsLivraison?,penaliteRetardPctParJour?,delaiLivraisonJours,jalons:{libelle,datePrevue}[]}` (BROUILLON) |
| POST | `/api/contrats/{id}/signer` | RA | signature CSE : `BROUILLON → EN_ATTENTE_SIGNATURE` (clauses figées, `CONTRAT_ENVOYE_SIGNATURE`) ; le contrat passe `SIGNE` + engagement `EN_ATTENTE` quand le fournisseur signe sur le portail (§13) |
| GET | `/api/engagements` | DAF, RA, DG | `?statut&page&size` → `Page<Engagement>` |
| POST | `/api/engagements/{contratId}/valider` | DAF | `{approuve, commentaire?}` → `Contrat` (R06) |
| POST | `/api/contrats/{id}/livraison/demarrer` | RA | exige `ENGAGEMENT_VALIDE` (R06, 422 `ENGAGEMENT_NON_VALIDE`) → `EN_LIVRAISON` ; secours « hors portail », le cas nominal est l'expédition déclarée par le fournisseur (§13) |
| PATCH | `/api/contrats/{id}/jalons/{jalonId}` | RA | `{dateReelle}` → `Contrat` |
| POST | `/api/contrats/{id}/livraison/confirmer` | RA | `{dateLivraisonReelle, numeroBonLivraison, observations?}` → `LIVRE` (+ `LIVRAISON_EFFECTUEE`) |
| POST | `/api/contrats/{id}/cloturer` | RA | → `CLOTURE` |
| GET | `/api/factures` | DAF, RA | `?statut&fournisseurId&enRetard&page&size` → `Page<Facture>` |
| POST | `/api/factures` | RA, DAF | `{contratId,numero,montantHt,tva,dateEmission,dateEcheance}` → 201 |
| POST | `/api/factures/{id}/mandater` | DAF | `{referenceMandat}` → `MANDATEE` |
| POST | `/api/factures/{id}/payer` | DAF | → `PAYEE` |
| POST | `/api/factures/{id}/rejeter` | DAF | `{motif}` → `REJETEE` |
| GET | `/api/finance/synthese` | DAF, DG | → `SyntheseFinance` |
| - | `/api/portail/**` | FOURN | portail fournisseur, voir §13 |

## 6. ms-reporting (port 8086) : CQRS alimenté par Kafka

Codes de profil : `DG` (alias accepté `DIRECTION_GENERALE`), `DAF`, `RESPONSABLE_ACHATS`, `RESPONSABLE_MATERIEL`, `CHEF_CHANTIER` ; profil par défaut selon la priorité DG > DAF > RA > RM > CHEF. Séries : indicateur autorisé si l'un des profils de l'appelant affiche la section correspondante (sinon 403 `INDICATEUR_NON_AUTORISE`). Profils autorisés : DG → tous ; RM → `RESPONSABLE_MATERIEL` ; RA → `RESPONSABLE_ACHATS` ; DAF → `DAF` ; CHEF → `CHEF_CHANTIER` (filtré sur ses chantiers). Profil non autorisé → 403.

```ts
Tableau { profil, du, au, genereLe,
  // tauxDisponibilite = disponibilité OPÉRATIONNELLE : (total − enMaintenance − réformés) / (total − réformés) ;
  // tauxUtilisation = jours affectés / jours opérationnels.
  parc?: { total, disponibles, affectes, enMaintenance, reformes, operationnels, tauxDisponibilite, tauxUtilisation,
           maintenancesEnCours, pannesOuvertes, alertesMaintenance,
           parCategorie: { categorie, total, disponibles, affectes, enMaintenance, tauxUtilisation }[] },
  besoins?: { total, enAttente, valides, rejetes, affectesDirect, enAchat, delaiMoyenTraitementHeures?,
              parPriorite: Record<Priorite, number> },
  achats?: { aoPublies, aoClotures, aoAttribues, contratsEnCours, livraisonsEnRetard,
             delaiMoyenApprovisionnementJours?, tauxLivraisonALHeure? },
  fournisseurs?: { actifs, scoreMoyen, litigesOuverts,
                   top: { fournisseurId, raisonSociale, scorePerformance, nombreLitiges, tauxLivraisonALHeure? }[] },
  finances?: { budgetTotal, montantEngage, montantMandate, montantPaye, tauxExecutionBudget,
               coutMaintenance, coutExploitation },
  chantiers?: { chantierId, chantierNom, region, budget, montantMarche, montantEngage, coutExploitation,
                coutMaintenance, marge, materielsAffectes, tauxUtilisation, besoinsEnAttente }[] }
Point { periode /* "2026-07" | "2026-07-14" | "2026-W29" */, valeur }
```

| Méthode | Route | Détail |
|---|---|---|
| GET | `/api/reporting/kpis` | `?profil&du&au&chantierId` (défaut : profil principal, 90 derniers jours) → `Tableau` |
| GET | `/api/reporting/series` | `?indicateur=TAUX_DISPONIBILITE|TAUX_UTILISATION|MONTANT_ENGAGE|COUT_MAINTENANCE|BESOINS_SOUMIS|DELAI_APPROVISIONNEMENT&granularite=JOUR|SEMAINE|MOIS&du&au&chantierId` → `Point[]` |
| GET | `/api/reporting/export` | `?format=pdf|xlsx&profil&du&au&chantierId` → fichier (`Content-Disposition: attachment`) |

## 7. ms-notification (port 8088)

```ts
Notification { id, type /* type d'événement */, titre, message, niveau: NiveauNotification,
  lien? /* route front, ex. "/besoins/<id>" */, entite?, entiteId?, lue: boolean, createdAt }
```
| Méthode | Route | Détail |
|---|---|---|
| GET | `/api/notifications` | `?lue&page&size` → `Page<Notification>` (destinataire = utilisateur ou un de ses rôles) |
| GET | `/api/notifications/non-lues/count` | → `{ count }` |
| PATCH | `/api/notifications/{id}/lue` | → `Notification` |
| POST | `/api/notifications/tout-lire` | → 204 |
| GET | `/api/notifications/stream` | SSE `text/event-stream` (événement `notification`, données = `Notification`, `ping` toutes les 25 s). Le front utilise `fetch` streaming avec l'en-tête `Authorization`. |

Règles de routage configurables (YAML) : type d'événement → destinataires (rôles et/ou champ utilisateur du payload), niveau, titre, lien, e-mail oui/non.

## 8. ms-audit (port 8089) : ADMIN uniquement

```ts
AuditLog { id, eventId, occurredAt, service, action, entite, entiteId, utilisateurId?, utilisateurNom?,
  roles: string[], resume?, correlationId? }
```
`GET /api/audit?utilisateurId&entite&action&service&du&au&q&page&size` → `Page<AuditLog>` ;
`GET /api/audit/statistiques` → `{ total, aujourdhui, septDerniersJours, parService: {service, total}[] }`.

## 9. ms-auth (port 8081)

```ts
Utilisateur { id /* = sub Keycloak */, email, prenom, nom, telephone?, role, actif,
  chantierIds?: string[], perimetreParc?, seuilDelegation?, budgetGere?, fournisseurId?,
  otpConfigure: boolean, createdAt }
RoleInfo { code, libelle, description, nombreUtilisateurs, otpObligatoire }
```
| Méthode | Route | Rôles | Détail |
|---|---|---|---|
| GET | `/api/me` | tout utilisateur | → `Utilisateur` (profil courant) |
| GET | `/api/admin/utilisateurs` | ADMIN | `?role&actif&q&page&size` → `Page<Utilisateur>` |
| GET | `/api/admin/utilisateurs/{id}` | ADMIN | → `Utilisateur` |
| POST | `/api/admin/utilisateurs` | ADMIN | `{email,prenom,nom,telephone?,role,motDePasseTemporaire,chantierIds?,perimetreParc?,seuilDelegation?,budgetGere?}` → 201 (créé dans Keycloak + profil) |
| PUT | `/api/admin/utilisateurs/{id}` | ADMIN | idem sans mot de passe |
| PATCH | `/api/admin/utilisateurs/{id}/statut` | ADMIN | `{actif}` |
| POST | `/api/admin/utilisateurs/{id}/reinitialiser-mot-de-passe` | ADMIN | `{motDePasseTemporaire}` → 204 |
| GET | `/api/admin/roles` | ADMIN | → `RoleInfo[]` |

## 10. ms-chatbot (port 8087)

```ts
Source { type: 'MATERIEL'|'BESOIN'|'CHANTIER'|'CONTRAT'|'FOURNISSEUR'|'MAINTENANCE'|'APPEL_OFFRE', id, libelle, lien }
Reponse { conversationId, messageId, reponse /* markdown simple */, sources: Source[], intention, modeDegrade: boolean, horodatage }
ConversationResume { id, titre, dernierMessage, nombreMessages, updatedAt }
Conversation { id, titre, messages: { id, role: 'UTILISATEUR'|'ASSISTANT', contenu, sources: Source[], horodatage }[] }
```
| Méthode | Route | Détail |
|---|---|---|
| POST | `/api/chatbot/query` | `{question (≤ 1000 car.), conversationId?}` → `Reponse` |
| GET | `/api/chatbot/historique` | `?page&size` → `Page<ConversationResume>` |
| GET | `/api/chatbot/historique/{id}` | → `Conversation` |
| DELETE | `/api/chatbot/historique/{id}` | → 204 |
| GET | `/api/chatbot/suggestions` | → `string[]` (questions types selon le rôle) |

## 11. Événements Kafka (module `cse-events`)

Enveloppe (JSON, clé Kafka = `aggregateId`) :
```ts
EventEnvelope { eventId, type, version /* 1 */, occurredAt, source /* ms-xxx */, aggregateType, aggregateId,
  correlationId?, actor?: { id, nom, roles: string[] }, payload: object }
```
Topics : `cse.besoins.events`, `cse.suivi.events`, `cse.fournisseurs.events`, `cse.achats.events`, `cse.auth.events`,
`cse.audit.events` (+ `<topic>.DLT`). Chaque événement de domaine écrit par l'outbox produit **aussi** une entrée
`AUDIT` sur `cse.audit.events` (payload `AuditPayload`).

Payloads « instantané » (un record par agrégat, le `type` distingue l'événement) :

| Payload | Types | Producteur | Consommateurs |
|---|---|---|---|
| `BesoinPayload {besoinId, reference, chantierId, chantierNom, demandeurId, demandeurNom, designation, categorie, quantite, specifications?, priorite, statut, dateBesoin, dureeUtilisationJours, motif?, materielIds[]}` | `BESOIN_SOUMIS`, `BESOIN_MODIFIE`, `BESOIN_VALIDE`, `BESOIN_REJETE`, `BESOIN_AFFECTE_DIRECT`, `BESOIN_EN_ACHAT` | ms-besoins | achats (EN_ACHAT), reporting, notification |
| `ChantierPayload {chantierId, code, nom, region, statut, budget, montantMarche, chefChantierId?, dateDebut, dateFinPrevue?}` | `CHANTIER_CREE`, `CHANTIER_MODIFIE` | ms-suivi | reporting |
| `MaterielPayload {materielId, code, designation, categorie, etat, chantierId?, chantierNom?, tarifJournalier, valeurAcquisition?, heuresFonctionnement, fournisseurId?, contratId?, motif?}` | `MATERIEL_ENREGISTRE`, `MATERIEL_ETAT_CHANGE`, `MATERIEL_REFORME_DEMANDEE`, `MATERIEL_REFORME`, `ALERTE_MAINTENANCE` | ms-suivi | reporting, notification |
| `AffectationPayload {affectationId, materielId, materielCode, materielDesignation, categorie, tarifJournalier, chantierId, chantierNom, chefChantierId?, besoinId?, dateDebut, dateFinPrevue, dateFinReelle?, statut, fournisseurId?, contratId?}` | `MATERIEL_AFFECTE`, `RECEPTION_CONFIRMEE`, `AFFECTATION_TERMINEE`, `FIN_AFFECTATION_PROCHAINE` | ms-suivi | besoins, fournisseurs (RECEPTION_CONFIRMEE), reporting, notification |
| `MaintenancePayload {maintenanceId, numeroOt, materielId, materielDesignation, chantierId?, type, statut, cout?, datePlanifiee, dateDebut?, dateFin?}` | `MAINTENANCE_PLANIFIEE`, `MAINTENANCE_DEMARREE`, `MAINTENANCE_CLOTUREE` | ms-suivi | reporting, notification |
| `PannePayload {panneId, materielId, materielDesignation, chantierId?, chantierNom?, gravite, description, signaleParId, signaleParNom}` | `PANNE_SIGNALEE` | ms-suivi | reporting, notification |
| `FournisseurPayload {fournisseurId, raisonSociale, email, specialites[], statut, scorePerformance, motif?, contactNom?}` | `FOURNISSEUR_INSCRIT`, `FOURNISSEUR_VALIDE`, `FOURNISSEUR_SUSPENDU`, `FOURNISSEUR_REACTIVE`, `FOURNISSEUR_BLACKLISTAGE_DEMANDE`, `FOURNISSEUR_BLACKLISTE`, `FOURNISSEUR_BLACKLISTAGE_REFUSE`, `FOURNISSEUR_MODIFIE`, `SCORE_RECALCULE` (motif « ALERTE : … » si score < seuil), `DOCUMENT_EXPIRE` | ms-fournisseurs | achats (statut, R14), reporting, notification |
| `LitigePayload {litigeId, reference, fournisseurId, raisonSociale, contratId?, objet, gravite, statut}` | `LITIGE_OUVERT`, `LITIGE_RESOLU` | ms-fournisseurs | reporting, notification |
| `AppelOffrePayload {appelOffreId, reference, objet, type, besoinId?, chantierId?, categorie, quantite, dateLimite, statut, nombreOffres, fournisseurIdsInvites[], offreRetenueId?, fournisseurRetenuId?, montantAttribue?, approbationDgRequise}` | `AO_PUBLIE`, `OFFRE_DEPOSEE`, `AO_CLOTURE`, `ATTRIBUTION_PROPOSEE`, `ATTRIBUTION_REFUSEE`, `MARCHE_ATTRIBUE` | ms-achats | reporting, notification |
| `ContratPayload {contratId, numero, appelOffreId, besoinId?, chantierId?, chantierNom?, fournisseurId, fournisseurNom, designation, categorie, quantite, montant, statut, dateSignature?, dateLivraisonPrevue?, dateLivraisonReelle?, tarifJournalierEstime?}` | `CONTRAT_GENERE`, `CONTRAT_ENVOYE_SIGNATURE`, `CONTRAT_SIGNATURE_REFUSEE`, `CONTRAT_SIGNE`, `ENGAGEMENT_DEMANDE`, `ENGAGEMENT_VALIDE`, `ENGAGEMENT_REJETE`, `LIVRAISON_DEMARREE`, `LIVRAISON_EFFECTUEE`, `ALERTE_RETARD_LIVRAISON`, `CONTRAT_CLOTURE` | ms-achats | suivi (LIVRAISON_EFFECTUEE), fournisseurs, reporting, notification |
| `FacturePayload {factureId, numero, contratId, fournisseurId, montantTtc, statut, dateEmission, dateEcheance, dateMandatement?, datePaiement?}` | `FACTURE_RECUE`, `FACTURE_MANDATEE`, `FACTURE_PAYEE`, `FACTURE_REJETEE` | ms-achats | reporting, notification |
| `UtilisateurPayload {utilisateurId, email, prenom, nom, role, actif, chantierIds[]}` | `UTILISATEUR_CREE`, `UTILISATEUR_MODIFIE`, `UTILISATEUR_SYNCHRONISE` | ms-auth | notification (annuaire e-mails) |
| `AuditPayload {service, action, entite, entiteId, resume?}` | `AUDIT` | tous (via outbox) | ms-audit |

Événements du portail : `CONTRAT_SIGNATURE_REFUSEE` ajoute `motif` au `ContratPayload`, `LIVRAISON_DEMARREE` déclarée par le
fournisseur ajoute `dateExpedition` et `commentaireExpedition` (champs JSON optionnels, ignorés par les consommateurs qui ne
les lisent pas).

## 12. Jeu de données de démonstration (profil `dev`, partagé entre services)

Les seeders Java (`DevDataSeeder`, profil `dev`, idempotents) passent par le domaine et l'outbox afin que
reporting / notification / audit soient alimentés par de vrais événements. Identifiants fixes :

Utilisateurs Keycloak (mot de passe dev `Cse@2026`, voir `infra/keycloak/realm-cse.json`) :
| id | e-mail | nom | rôle | attributs |
|---|---|---|---|---|
| `00000000-0000-0000-0000-000000000101` | chef.chantier@cse.sn | Mamadou Diop | CHEF | chantiers 1, 2 |
| `00000000-0000-0000-0000-000000000102` | awa.ndiaye@cse.sn | Awa Ndiaye | CHEF | chantiers 3, 4 |
| `00000000-0000-0000-0000-000000000201` | resp.materiel@cse.sn | Ibrahima Fall | RM | |
| `00000000-0000-0000-0000-000000000301` | resp.achats@cse.sn | Fatou Sarr | RA | seuilDelegation 150 000 000 |
| `00000000-0000-0000-0000-000000000401` | daf@cse.sn | Ousmane Ba | DAF | |
| `00000000-0000-0000-0000-000000000501` | dg@cse.sn | Aminata Sow | DG | |
| `00000000-0000-0000-0000-000000000601` | admin@cse.sn | Cheikh Ndiaye | ADMIN | |
| `00000000-0000-0000-0000-000000000701` | fournisseur@cse.sn | Moussa Kane | FOURN | fournisseur 1 |

Chantiers (`ms-suivi`), UUID `c0000000-0000-0000-0000-00000000000N` :
1 `CH-DKR-01` Extension autoroute Dakar-AIBD, Dakar (14.7167, -17.4677) · 2 `CH-THS-01` Contournement de Thiès RN2 (14.7910, -16.9256) ·
3 `CH-STL-01` Pont de la lagune, Saint-Louis (16.0326, -16.4818) · 4 `CH-KLK-01` Réhabilitation route Kaolack-Fatick (14.1652, -16.0758) ·
5 `CH-ZIG-01` Bâtiment administratif, Ziguinchor (12.5681, -16.2719) · 6 `CH-TBA-01` Aménagement hydraulique, Touba (14.8500, -15.8833) ·
7 `CH-MBR-01` Station d'épuration, Mbour (14.4199, -16.9658), `TERMINE`.

Fournisseurs (`ms-fournisseurs`), UUID `f0000000-0000-0000-0000-00000000000N` (noms fictifs) :
1 Sahel Engins SA (MATERIEL_ROULANT) · 2 Teranga Équipements BTP (MATERIEL_ROULANT, OUTILLAGE) · 3 Dakar Outillage Pro (OUTILLAGE) ·
4 Cap-Vert Informatique (EQUIPEMENT_INFORMATIQUE) · 5 Atlantique Consommables SARL (CONSOMMABLE) · 6 Ndiambour Matériaux (SUSPENDU) ·
7 Baobab Logistique (EN_ATTENTE_VALIDATION).

Chaque service seed ses propres agrégats avec des dates réparties sur les 9 derniers mois (séries de reporting non vides).

## 13. Portail fournisseur (`apps/portail-fournisseur`, port 5174, client Keycloak `cse-portail-fournisseur`)

Toutes les routes sont filtrées par le claim `fournisseur_id` du jeton : une ressource d'un autre fournisseur
renvoie **404** (jamais 403, pour ne pas révéler son existence). Rôle requis `FOURNISSEUR`, sauf l'inscription.

```ts
AppelOffrePortail { id, reference, objet, description?, type, categorie, quantite, specifications?,
  datePublication, dateLimite, statut, criteres: CriterePondere[], chantierNom?, invite: boolean,
  offreDeposee: boolean, monOffreId? }
OffrePortail { id, appelOffreId, appelOffreReference, appelOffreObjet, montantPropose, delaiLivraisonJours,
  garantieMois?, description?, dateDepot, statutAppelOffre: StatutAO,
  resultat: 'EN_COURS' | 'RETENUE' | 'NON_RETENUE' /* RETENUE/NON_RETENUE une fois l'AO ATTRIBUE */ }
Inscription { raisonSociale, ninea, rccm?, adresse?, ville?, pays, telephone?, email, contactNom,
  contactFonction?, specialites: CategorieMateriel[] }
ProfilFournisseur = Fournisseur (§4, sans coordonnées bancaires d'un autre) + documents: DocumentAdministratif[]
```

| Méthode | Route | Service | Détail |
|---|---|---|---|
| POST | `/api/portail/inscription` | fournisseurs | **public** (sans jeton, limité par IP à la passerelle) ; `Inscription` → 201 `{id, raisonSociale, statut: 'EN_ATTENTE_VALIDATION'}` ; NINEA ou e-mail déjà connu → 409 `FOURNISSEUR_EXISTANT` ; publie `FOURNISSEUR_INSCRIT` |
| GET | `/api/portail/profil` | fournisseurs | → `ProfilFournisseur` |
| PUT | `/api/portail/profil` | fournisseurs | coordonnées modifiables par le fournisseur (existant) |
| GET / PUT | `/api/portail/catalogue` | fournisseurs | catalogue du fournisseur (existant) |
| GET | `/api/portail/appels-offres` | achats | `?page&size&sort` → `Page<AppelOffrePortail>` (publiés, date limite non atteinte, procédure ouverte ou fournisseur invité) |
| GET | `/api/portail/appels-offres/{id}` | achats | → `AppelOffrePortail` (même filtre, sinon 404) |
| POST | `/api/portail/appels-offres/{id}/offres` | achats | `{montantPropose,delaiLivraisonJours,garantieMois?,description?}` → 201 `OffrePortail` (R02, R14, une offre par AO → 409) |
| GET | `/api/portail/offres/mes-offres` | achats | → `Page<OffrePortail>` |
| GET | `/api/portail/contrats` | achats | `?statut&page&size` → `Page<Contrat>` (§5, ceux du fournisseur) |
| GET | `/api/portail/contrats/{id}` | achats | → `Contrat` |
| POST | `/api/portail/contrats/{id}/signer` | achats | `EN_ATTENTE_SIGNATURE → SIGNE` + engagement `EN_ATTENTE` (DAF) ; publie `CONTRAT_SIGNE` puis `ENGAGEMENT_DEMANDE` ; autre statut → 409 `TRANSITION_INTERDITE` |
| POST | `/api/portail/contrats/{id}/refuser` | achats | `{motif}` (obligatoire) : `EN_ATTENTE_SIGNATURE → BROUILLON`, signature CSE annulée ; publie `CONTRAT_SIGNATURE_REFUSEE` |
| POST | `/api/portail/contrats/{id}/expedier` | achats | `{dateExpedition, commentaire?}` : exige `ENGAGEMENT_VALIDE` (R06, sinon 422 `ENGAGEMENT_NON_VALIDE`) → `EN_LIVRAISON` ; date antérieure à la signature → 422 `DATE_EXPEDITION_INVALIDE`, date future → 422 `DATE_FUTURE` ; publie `LIVRAISON_DEMARREE` |
| PATCH | `/api/portail/contrats/{id}/jalons/{jalonId}` | achats | `{dateReelle}` (contrat `EN_LIVRAISON`, sinon 409 `CONTRAT_NON_EN_LIVRAISON`) → `Contrat` |
| GET | `/api/portail/attributions` | achats | conservé (marchés attribués) |
| GET | `/api/portail/factures` | achats | → `Page<Facture>` |
| POST | `/api/portail/factures` | achats | `{contratId,numero,montantHt,tva,dateEmission,dateEcheance}` → 201 ; contrat `LIVRE` du fournisseur requis (sinon 422 `CONTRAT_NON_LIVRE` ; un contrat clôturé est soldé) |

Circuit complet (démonstration) :
1. **Inscription** publique → `EN_ATTENTE_VALIDATION` ; le RA valide (`PATCH /api/fournisseurs/{id}/valider-inscription`)
   → `FOURNISSEUR_VALIDE` ; ms-auth crée le compte Keycloak (identifiant = e-mail, rôle `FOURNISSEUR`, attribut
   `fournisseur_id`) et Keycloak envoie l'e-mail « définir votre mot de passe » (visible dans MailHog).
2. Le RA publie un appel d'offres en invitant le fournisseur ; le fournisseur **dépose son offre** sur le portail.
3. Clôture, évaluation, attribution, co-validation RM (et DG au-delà du seuil) → contrat `BROUILLON`.
4. Le RA renseigne les clauses puis **signe** → `EN_ATTENTE_SIGNATURE` ; le fournisseur **signe** → `SIGNE`
   (ou refuse avec un motif → `BROUILLON`).
5. La DAF valide l'engagement → `ENGAGEMENT_VALIDE` ; le fournisseur **déclare l'expédition** → `EN_LIVRAISON`
   et renseigne ses jalons.
6. Le RA confirme la livraison (bon de livraison) → `LIVRE` ; le fournisseur **dépose sa facture**.
7. La DAF mandate puis paie ; le fournisseur suit le statut ; le RA clôture le contrat et évalue le fournisseur.
