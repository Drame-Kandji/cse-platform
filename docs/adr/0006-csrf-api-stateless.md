# ADR 0006 : CSRF désactivé sur les API

Date : 2026-10-30
Statut : Accepté

## Contexte
Spring Security active la protection CSRF par défaut, conçue pour l'authentification par cookie de session.

## Décision
Les API sont sans état (`SessionCreationPolicy.STATELESS`) et authentifiées uniquement par l'en-tête `Authorization: Bearer`, qu'un site tiers ne peut ni lire ni forger ; la protection CSRF est donc désactivée. Aucun cookie d'authentification n'est utilisé par le frontend (jeton en mémoire via keycloak-js).

## Conséquences
Toute introduction future d'une authentification par cookie imposerait de réactiver CSRF.
