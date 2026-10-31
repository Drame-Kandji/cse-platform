# ADR 0009 : Orientation d'un besoin validé et indisponibilité de ms-suivi

Date : 2026-10-30
Statut : Accepté

## Contexte
Après validation (R01), ms-besoins doit déterminer si le besoin est satisfait par affectation directe ou par achat. L'affectation dans ms-suivi est réservée au Responsable Matériel.

## Décision
L'orientation se fait de façon synchrone avec le JWT du Responsable Matériel relayé (ms-suivi applique ses propres règles R04). Si ms-suivi est indisponible (circuit ouvert), le besoin reste `VALIDE` avec `orientationEnAttente=true` et le Responsable Matériel relance via `POST /api/besoins/{id}/orienter`. Pas de bascule automatique vers l'achat ni de compte de service à privilèges élevés.

## Conséquences
On n'achète jamais un engin parce qu'un service est momentanément indisponible ; aucun compte technique ne détient le rôle d'affectation.
