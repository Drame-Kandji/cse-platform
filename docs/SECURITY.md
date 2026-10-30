# Sécurité CSE Platform

## Authentification
- Keycloak realm `cse`, OAuth2/OIDC
- Clients publics avec PKCE
- 2FA obligatoire pour ADMIN_SYSTEME, DIRECTION_GENERALE, DAF, RESPONSABLE_ACHATS

## Autorisation
RBAC par rôles realm, vérification côté services avec @PreAuthorize.

## Données sensibles
Chiffrement AES-256-GCM pour montants contrats, coordonnées bancaires.

## Protection
- RFC 7807 Problem Details
- CORS strict
- Rate limiting au gateway
- Headers de sécurité
- Validation côté serveur
- Verrou optimiste @Version
