# ADR 0008 : Données de démonstration produites par le code applicatif

Date : 2026-10-30
Statut : Accepté

## Contexte
Les anciens scripts SQL de seed contournaient le domaine : aucun événement n'était émis, donc le reporting, les notifications et l'audit restaient vides ; les identifiants n'étaient pas cohérents entre services.

## Décision
ms-suivi et ms-fournisseurs possèdent un `DevDataSeeder` (profil `dev`, idempotent) qui passe par les agrégats et l'outbox. Les identifiants partagés (utilisateurs Keycloak, chantiers, matériels, fournisseurs) sont fixés dans `docs/API_CONTRACT.md` §12 et dans le realm Keycloak.

Révision de novembre 2026 : le jeu est réduit au référentiel (6 chantiers, 15 matériels dont 6 affectés aux chantiers, 5 fournisseurs actifs), daté du 1er du mois courant. L'ancien historique simulé sur 9 mois rendait la démonstration difficile à suivre ; besoins, appels d'offres, contrats et factures sont désormais créés en direct pendant la démonstration.

## Conséquences
Le reporting est alimenté par de vrais événements ; ses séries partent du mois courant et se remplissent pendant la démonstration. Aucune donnée de démonstration en profil `prod`.
