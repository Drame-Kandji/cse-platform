# ADR 0011 : Seuil de délégation des achats (R11)

Date : 2026-10-30
Statut : Accepté

## Contexte
L'UML porte `seuilDelegation` sur ResponsableAchats. Le lire depuis le profil utilisateur imposerait un appel à ms-auth ou un claim supplémentaire.

## Décision
ms-achats applique un seuil configurable `cse.achats.seuil-delegation` (défaut 150 000 000 FCFA, égal à l'attribut du compte de démonstration). Au-delà, l'approbation de la Direction Générale est requise ; la co-validation du Responsable Matériel est toujours requise.

## Conséquences
Un seuil individuel par acheteur nécessiterait d'exposer l'attribut dans le JWT (mapper Keycloak) : évolution possible sans changement du domaine.
