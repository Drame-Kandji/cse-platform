# ADR 0002 : Authentification Keycloak OAuth2/OIDC

Date : 2025-10-25
Statut : Accepté

## Contexte
Besoin d'authentification centralisée avec rôles et 2FA.

## Décision
Utiliser Keycloak realm cse avec clients publics PKCE, Resource Server JWT côté services, ms-auth encapsule API Admin Keycloak. Mot de passe non stocké en ms-auth.

## Conséquences
Sécurité renforcée, SSO possible, 2FA configurable.
