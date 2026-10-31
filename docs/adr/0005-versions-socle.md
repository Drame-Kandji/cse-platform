# ADR 0005 : Spring Boot 3.5 / Spring Cloud 2025.0 / Spring AI 1.0

Date : 2026-10-30
Statut : Accepté

## Contexte
Spring AI 1.0 GA (remplaçant le jalon 1.0.0-M4) requiert Spring Boot 3.4+. Docker 29 n'accepte plus les API < 1.44, incompatible avec Testcontainers < 1.21.4.

## Décision
Spring Boot 3.5.5, Spring Cloud 2025.0.0, Spring AI 1.0.1, springdoc 2.8, Testcontainers 1.21.4 ; Java 17 conservé (exigence du cahier des charges).

## Conséquences
Plus de dépôt de jalons Spring ; versions centralisées dans `backend/pom.xml`.
