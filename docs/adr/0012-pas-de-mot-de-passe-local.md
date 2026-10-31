# ADR 0012 : Aucun mot de passe stocké hors Keycloak

Date : 2026-10-30
Statut : Accepté

## Contexte
Le diagramme de classes définit `Utilisateur.motDePasseHash` ; la spécification technique délègue entièrement l'authentification à Keycloak.

## Décision
L'attribut `motDePasseHash` est omis. ms-auth stocke le profil métier (hiérarchie Utilisateur) et pilote Keycloak via son API d'administration (compte de service `cse-admin-api`). Mot de passe de démonstration : `Cse@2026` (politique de 8 caractères minimum).

## Conséquences
Un seul référentiel d'identité ; réinitialisation de mot de passe et 2FA gérés par Keycloak.
