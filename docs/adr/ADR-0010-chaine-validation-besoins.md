# ADR 0010 : Chaîne de validation configurable des besoins

Date : 2026-10-30
Statut : Accepté

## Contexte
La règle R01 réserve la validation et le rejet des besoins au Responsable Matériel (RM). Le cahier des
charges évoque, pour les besoins importants, l'intervention d'un niveau hiérarchique supérieur
(« Directeur de projet ») au-delà d'un seuil. Ce rôle n'existe pas encore dans Keycloak et aucun montant
n'est porté par un besoin (le coût n'est connu qu'au moment de l'achat). Il faut donc respecter R01
aujourd'hui sans figer le circuit de validation dans le code.

## Décision
La validation est modélisée dans le domaine de ms-besoins par une **chaîne de responsabilité**
(`domain.validation`) :

- `EtapeValidation(code, roleRequis, CritereDeclenchement)` : une étape franchissable par un rôle ;
  le critère rend l'étape obligatoire sans condition, ou dès qu'**un** des seuils est atteint
  (`quantiteMinimale`, `prioriteMinimale` — combinaison « OU »).
- `MaillonValidation` : traite la demande si son étape est requise et pas encore franchie, sinon la
  délègue au maillon suivant ; le validateur doit détenir le rôle de l'étape en attente
  (`ValidationNonAutoriseeException`, HTTP 403 `VALIDATION_NON_AUTORISEE`).
- `ChaineValidation` : assemblée au démarrage à partir des étapes **actives** de
  `cse.besoins.validation.etapes` (ordre = ordre de franchissement). La première étape doit être
  inconditionnelle, de sorte que tout besoin a au moins un validateur.
- L'agrégat `BesoinMateriel.valider(...)` interroge la chaîne : étape intermédiaire → entrée
  d'historique `APPROBATION_ETAPE` (le besoin reste `EN_ATTENTE`, aucun événement) ; dernière étape →
  `VALIDE` + `BESOIN_VALIDE` puis orientation (ADR-0009). Les étapes franchies sont déduites de
  l'historique (colonne `etape_validation`), sans table supplémentaire.
- Le rejet appartient au détenteur du rôle de l'étape en attente.

Configuration par défaut : une seule étape active `RESPONSABLE_MATERIEL` (R01 strict). Une étape
`DIRECTEUR_PROJET` (quantité ≥ 10 ou priorité `CRITIQUE`) est déclarée mais **inactive**.

## Point d'extension « Directeur de projet »
Pour l'activer :
1. créer le rôle realm `DIRECTEUR_PROJET` dans Keycloak et l'ajouter à `Roles` (cse-common) ;
2. passer `actif: true` sur l'étape et ajuster les seuils (`quantite-minimale`, `priorite-minimale`) ;
3. élargir le `@PreAuthorize` de `PATCH /api/besoins/{id}/valider` et `/rejeter` à ce rôle (aujourd'hui
   RM seul, conformément au contrat d'API §3) ;
4. si un seuil en valeur est requis, ajouter un critère « montant estimé » quand le besoin portera une
   estimation (aucune donnée de coût n'existe à ce jour).

## Conséquences
- R01 est appliquée à deux niveaux : RBAC sur le contrôleur et contrôle métier dans le domaine (testé
  par `*_R01`).
- Ajouter un niveau de validation est une modification de configuration, pas de code métier.
- Tant qu'une étape supérieure est requise, le besoin reste `EN_ATTENTE` : il demeure modifiable par
  son demandeur (R05 ne s'applique qu'à partir de `VALIDE`). Si ce comportement devait changer, il
  faudrait un statut intermédiaire, ce qui modifierait la machine à états du contrat.
