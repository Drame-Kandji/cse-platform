# Architecture CSE Platform

## Vue d'ensemble
Architecture microservices en Clean/Hexagonale. Communication REST synchrone + Kafka asynchrone. Keycloak pour l'identité.

## Diagramme C4 (niveau contexte)
```mermaid
graph TD
    A[Chef de Chantier] --> B[Web Interne]
    C[Fournisseur] --> D[Portail Fournisseur]
    B --> E[API Gateway]
    D --> E
    E --> F[Microservices]
    F --> G[(PostgreSQL)]
    F --> H[(Kafka)]
    E --> I[Keycloak]
```
