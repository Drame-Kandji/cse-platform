# ADR 0001 : Architecture microservices Clean/Hexagonale

Date : 2025-10-25
Statut : Accepté

## Contexte
Système de gestion des matériels et équipements CSE avec multiples domaines métier.

## Décision
Adopter une architecture microservices avec Clean Architecture/Hexagonale par service :
- domain (pur), application (cas d'usage), infrastructure (adaptateurs), api (REST)
- Database per Service
- Communication REST synchrone + Kafka asynchrone
- Transactional Outbox pour fiabilité

## Conséquences
- Découplage fort, testabilité accrue
- Nécessite gestion de la cohérence éventuelle
- Tests ArchUnit pour faire respecter les dépendances
