# ADR 0013 : ms-auth, Keycloak source de vérité, synchronisation et changement de rôle SINGLE_TABLE

Date : 2026-10-30
Statut : Accepté

## Contexte
D2 impose la hiérarchie `Utilisateur` en JPA SINGLE_TABLE et l'encapsulation de l'API Admin Keycloak
par ms-auth. Les comptes de démonstration sont importés directement dans Keycloak (realm) ; un
administrateur peut aussi modifier un compte depuis la console Keycloak. Le contrat autorise le
changement de rôle d'un utilisateur (`PUT /api/admin/utilisateurs/{id}`).

## Décision
1. **Keycloak fait foi** pour l'identité, le rôle, le statut et les attributs ; le profil local en est
   une projection enrichie (téléphone, date de création). Une synchronisation idempotente (au démarrage
   avec relances, puis périodique) importe les comptes portant un rôle CSE, met à jour ceux qui ont changé
   et désactive les profils dont le compte a disparu. `/api/me` provisionne le profil depuis le JWT s'il
   n'existe pas encore.
2. **Écritures administrateur** : création Keycloak d'abord (l'identifiant `sub` devient l'identifiant
   du profil), puis profil + outbox dans la transaction ; un rollback déclenche la suppression du compte
   Keycloak (compensation). Pour les modifications, la base est mise à jour puis Keycloak dans la même
   transaction : un échec Keycloak annule tout ; un échec au commit est corrigé par la synchronisation.
3. **Changement de rôle** : JPA ne sait pas changer la classe d'une entité gérée ; l'adaptateur réécrit
   le discriminateur `role` (et efface les attributs de l'ancien profil) par une requête native, vide le
   contexte de persistance puis relit l'entité, désormais de la nouvelle sous-classe. Le domaine modélise
   ce changement par `Utilisateur.convertirEn(role, attributs)`.
4. Le discriminateur `role` est un `VARCHAR` contraint par `CHECK` plutôt qu'un `ENUM` PostgreSQL natif :
   Hibernate lie les discriminateurs comme des chaînes, ce qui provoquerait des erreurs de type
   `enum = varchar`.
5. Client Keycloak : `RestClient` simple (pas de `keycloak-admin-client`, qui embarque RESTEasy et des
   versions de Jackson/Jakarta indépendantes de Spring Boot).

## Conséquences
L'annuaire de ms-notification est alimenté par `UTILISATEUR_SYNCHRONISE` dès le premier démarrage.
L'indicateur `otpConfigure` d'un profil reflète Keycloak au plus tard à la synchronisation suivante.
