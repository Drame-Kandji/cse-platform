# ADR 0007 : Outbox transactionnelle JDBC et audit dérivé des événements

Date : 2026-10-30
Statut : Accepté

## Contexte
La spécification exige la publication fiable des événements (pas d'événement sans écriture en base, ni l'inverse) et la journalisation de toute création/modification d'entité métier, sans code dupliqué.

## Décision
- `OutboxWriter` écrit l'enveloppe dans `outbox_event` via JDBC dans la transaction métier (propagation MANDATORY) ; `OutboxRelay` publie vers Kafka (`FOR UPDATE SKIP LOCKED`, ordre préservé, rejeu).
- Chaque événement de domaine génère automatiquement une entrée `AUDIT` sur `cse.audit.events` (auteur, action, entité, résumé), consommée par ms-audit. Cela remplace l'annotation `@Audited` : toute modification métier émet déjà un événement de domaine, l'audit en découle sans aspect ni réflexion.
- Consommateurs idempotents (`processed_event`), rejeu puis Dead Letter Topic `<topic>.DLT`.

## Conséquences
Livraison « au moins une fois » + idempotence = effet « exactement une fois ». Une modification qui n'émettrait pas d'événement ne serait pas auditée : la revue de code doit le vérifier.
