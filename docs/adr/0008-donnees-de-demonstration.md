# ADR 0008 : Données de démonstration produites par le code applicatif

Date : 2026-10-30
Statut : Accepté

## Contexte
Les anciens scripts SQL de seed contournaient le domaine : aucun événement n'était émis, donc le reporting, les notifications et l'audit restaient vides ; les identifiants n'étaient pas cohérents entre services.

## Décision
Chaque service possède un `DevDataSeeder` (profil `dev`, idempotent) qui passe par les agrégats et l'outbox. Les identifiants partagés (utilisateurs Keycloak, chantiers, fournisseurs) sont fixés dans `docs/API_CONTRACT.md` §12 et dans le realm Keycloak. Les dates métier sont relatives à l'horloge et réparties sur 9 mois.

## Conséquences
Le reporting est alimenté par de vrais événements dès le premier démarrage. Aucune donnée de démonstration en profil `prod`.
