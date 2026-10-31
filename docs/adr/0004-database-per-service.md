# ADR 0004 : Isolation PostgreSQL par service (schéma + utilisateur)

Date : 2026-10-30
Statut : Accepté

## Contexte
La spécification impose « Database per Service » et l'absence d'accès cross-schéma. L'ancienne configuration utilisait le superutilisateur `postgres` pour tous les services.

## Décision
Une seule instance PostgreSQL 15 (contrainte de ressources du projet) mais un utilisateur `ms_<service>` par service, propriétaire de son seul schéma, `search_path` fixé sur ce schéma et `connection-init-sql` côté Hikari. Les types ENUM natifs sont créés dans le schéma du service. Les tables techniques (`outbox_event`, `processed_event`) sont fournies par la migration commune `db/common`.

## Conséquences
Un service ne peut techniquement pas lire les données d'un autre (vérifié : `permission denied for schema`). Passage à une instance par service possible sans changement de code (URL et identifiants par variables d'environnement).
