# Script de démonstration : circuit d'achat avec le portail fournisseur

Démonstration de bout en bout pour un jury, en **20 minutes environ** : le besoin exprimé par un chef de
chantier, sa validation par le Responsable Matériel, l'appel d'offres lancé par la CSE, l'offre déposée par
le fournisseur sur son portail, l'attribution, la double signature du contrat, l'engagement budgétaire,
l'expédition, la livraison sur le chantier et sa réception par le chef, la facture et son paiement. Une ouverture facultative
montre l'inscription d'un nouveau fournisseur. Le détail de chaque écran est dans le
[guide utilisateur](USER_GUIDE.md) (§2 à §5 et §8).

## 1. Avant la séance

**Démarrer la plateforme** (voir le [README](../README.md#démarrage-en-5-commandes)) :

```bash
make infra && make start      # infrastructure Docker + 10 microservices (profil dev, données de démo)
make front                    # application interne : http://localhost:5173
make portail                  # portail fournisseur : http://localhost:5174 (autre terminal)
scripts/cse.sh reset --yes    # repartir d'une base propre avant chaque séance
```

Vérifier avec `make status` que les 10 services sont UP.

**Données de départ.** La plateforme démarre avec le seul référentiel, daté du 1er du mois courant :
les comptes ci-dessous, 6 chantiers (dont CH-DKR-01 et CH-THS-01 pour le chef Mamadou Diop), 15 matériels
(6 déjà affectés aux chantiers, dont 3 sur ceux de Mamadou Diop, et 9 disponibles au parc central) et 5 fournisseurs actifs (dont **Sahel Engins SA**). Il n'y a encore aucun
besoin, appel d'offres, contrat ni facture : tout est créé pendant la démonstration, et les tableaux de bord
se remplissent au fil des étapes.

**Adresses et comptes** (mot de passe commun **`Cse@2026`**) :

| Qui | Application | Compte | Rôle dans la démo |
|---|---|---|---|
| Mamadou Diop | interne http://localhost:5173 | `chef.chantier@cse.sn` | Chef de chantier : besoin, réception du matériel sur son chantier |
| Fatou Sarr | interne http://localhost:5173 | `resp.achats@cse.sn` | Responsable des Achats (RA) : AO, attribution, contrat, réception |
| Ibrahima Fall | interne http://localhost:5173 | `resp.materiel@cse.sn` | Responsable Matériel (RM) : validation du besoin, co-validation de l'attribution |
| Ousmane Ba | interne http://localhost:5173 | `daf@cse.sn` | DAF : engagement, mandatement, paiement |
| Aminata Sow | interne http://localhost:5173 | `dg@cse.sn` | Direction Générale (seulement au-delà de 150 M FCFA) |
| Moussa Kane | portail http://localhost:5174 | `fournisseur@cse.sn` | fournisseur F1 **Sahel Engins SA** |
| - | MailHog http://localhost:8025 | - | e-mails envoyés (définition du mot de passe, notifications) |

**Préparer le navigateur.** Les deux applications partagent la même session Keycloak : un même navigateur
connecté en RA ouvrirait le portail en RA (« Accès réservé aux fournisseurs »). Utiliser donc **deux
navigateurs ou deux profils** : l'un pour la CSE (onglets RA, RM, DAF successivement, en se déconnectant
par le menu à initiales), l'autre, en navigation privée, pour le fournisseur. Côté CSE, l'idéal est une fenêtre
privée par rôle (chef, RM, RA, DAF) pour basculer sans se reconnecter.

Montant à utiliser : **95 000 000 FCFA**, sous le seuil de délégation de 150 000 000 FCFA, pour que la
co-validation du RM suffise (règle R11). Au-delà, la Direction Générale doit aussi approuver.

## 2. Ouverture facultative : inscription d'un nouveau fournisseur (3 min)

| Étape | Qui, où | Cliquer | Montrer |
|---|---|---|---|
| 2.1 | Visiteur, portail http://localhost:5174 | **Inscrire mon entreprise** ; remplir raison sociale, NINEA (ex. `001234567 2A1`), ville, e-mail (ex. `contact@nouvelle-entreprise.sn`), contact, spécialité ; **Envoyer la demande** | Formulaire public sans compte ; message « Demande envoyée » |
| 2.2 | RA, interne | **Fournisseurs** → sous-onglet **À valider** | La demande apparaît, statut « En attente de validation » |
| 2.3 | RA, interne | Ouvrir la fiche → **Valider l'inscription** → confirmer | Message : un e-mail a été envoyé au fournisseur pour définir son mot de passe ; statut **Actif** |
| 2.4 | MailHog http://localhost:8025 | Ouvrir le message adressé au nouveau fournisseur | E-mail Keycloak « définir votre mot de passe » ; le lien permet de choisir un mot de passe puis de se connecter au portail |

La suite de la démonstration utilise le fournisseur F1 Sahel Engins SA, déjà actif.

## 3. Circuit principal avec Sahel Engins SA (17 min)

| Étape | Qui, où | Cliquer | Montrer |
|---|---|---|---|
| 3.1 | Chef de chantier, interne | **Besoins** → **Nouveau besoin** ; catégorie Matériel roulant, désignation « Compacteur vibrant 15 t », quantité 1, durée 90 jours, chantier **CH-DKR-01** (Extension autoroute Dakar-AIBD), date souhaitée J+15, priorité Normale → **Envoyer le besoin** | Référence `BES-2026-NNNN`, statut **En attente** ; le chef ne voit que ses chantiers ; le RM est notifié (cloche) |
| 3.2 | RM, interne | **Validation** → le besoin → **Valider** (commentaire « Aucun compacteur vibrant au parc ») | L'écran indique qu'aucun engin disponible ne correspond (R01) : le besoin passe **En achat** |
| 3.3 | Chef de chantier, interne | **Besoins** → onglet **En achat** | Le chef suit son besoin : avancement Soumis → Validation → Orientation, statut **En achat** |
| 3.4 | RA, interne | **Appels d'offres** → onglet **Besoins à lancer** → **Lancer un AO** sur la ligne du besoin ; objet « Acquisition d'un compacteur vibrant », budget 110 000 000 ; critères par défaut ; cocher **Sahel Engins SA** ; date limite J+2 ; **Publier l'appel d'offres** | Assistant en 6 étapes prérempli depuis le besoin ; seuls les fournisseurs **actifs** de la spécialité sont invitables (R14) ; référence `AO-2026-NNN`, statut **Publié** |
| 3.5 | Fournisseur, portail | **Se connecter** (`fournisseur@cse.sn`) ; **Accueil** | Liste **À faire** : l'AO apparaît avec sa date limite |
| 3.6 | Fournisseur, portail | Ouvrir l'AO → **Déposer une offre** : 95 000 000 FCFA, 20 jours, garantie 12 mois → **Déposer l'offre** | Critères pondérés visibles ; onglet **Mes offres** : offre « En cours » |
| 3.7 | RA, interne | Fiche de l'AO, onglet **Offres** | L'offre est arrivée **anonymisée** (`OFR-…`, R10) |
| 3.8 | RA, interne | **Clôturer** ; onglet **Évaluation** → **Noter** (qualité technique, garantie, SAV, références) ; onglet **Comparatif** | L'anonymat est levé ; prix et délai notés automatiquement ; score pondéré et recommandation |
| 3.9 | RA, interne | Onglet **Attribution** : choisir l'offre, justifier, **Proposer l'attribution** | Mention « dans le seuil, sans approbation de la Direction Générale (R11) » |
| 3.10 | RM, interne | Même AO, onglet **Attribution** → **Co-valider** (commentaire) | AO **Attribué**, contrat **brouillon** généré (R03) ; lien **Voir le contrat** |
| 3.11 | RA, interne | **Voir le contrat** → **Modifier les clauses** (livraison franco sur le chantier Dakar-AIBD, maintenance, jalons) → **Signer et envoyer au fournisseur** | La confirmation rappelle que le fournisseur doit contresigner ; statut **En attente du fournisseur**, signature CSE (nom, date) dans la synthèse |
| 3.12 | Fournisseur, portail | **Contrats** → onglet **À signer** → le contrat → **Signer le contrat** → **Signer** | Avancement Signature → Engagement ; option **…** → **Refuser la signature** (motif obligatoire) à mentionner sans l'exécuter |
| 3.13 | RA, interne | Recharger le contrat | Statut **Signé**, deux signatures, « En attente de la validation de l'engagement par le DAF » |
| 3.14 | DAF, interne | **Finance** → onglet **Engagements à valider** → **Valider** (commentaire) | R06 : sans cet engagement, aucune expédition possible |
| 3.15 | Fournisseur, portail | Le contrat (onglet **En cours**, statut « À expédier ») → **Déclarer l'expédition** (date du jour, commentaire « Transport par porte-char ») | Statut **En livraison** ; les jalons peuvent être **marqués atteints** |
| 3.16 | RA, interne | Recharger le contrat ; **Confirmer la livraison** (date, bon de livraison `BL-DEMO-001`) | Date et commentaire d'expédition dans la synthèse ; statut **Livré** ; le compacteur est créé dans le **Parc matériel** et affecté au chantier CH-DKR-01, celui du besoin |
| 3.17 | Chef de chantier, interne | **Tableau de bord** → **À traiter** → **Réceptions à confirmer** → le compacteur → **Confirmer la réception** (observation « Livré en bon état ») | Le compacteur passe **Affecté** sur son chantier ; la fiche du besoin affiche le matériel reçu dans son historique : la boucle est bouclée |
| 3.18 | Fournisseur, portail | Le contrat → **Déposer la facture** : numéro `FA-DEMO-001`, montant HT 80 000 000, TVA 18 % → **Déposer la facture** | TVA et TTC (94 400 000) calculés en direct ; facture **Reçue** |
| 3.19 | DAF, interne | **Finance** → onglet **Factures** → **Mandater** (référence `MDT-DEMO-001`) puis **Payer** | Statuts Mandatée puis Payée ; synthèse financière mise à jour |
| 3.20 | Fournisseur, portail | **Factures** | La facture est **Payée**, avec sa date de paiement ; montants payés de l'accueil mis à jour |
| 3.21 | RA, interne | Le contrat → **Clôturer le contrat** ; puis **Fournisseurs** → Sahel Engins SA → **Évaluer** | Contrat **Clôturé** ; le score de performance du fournisseur est recalculé (R07) |

**Désignation du besoin.** À la validation, la plateforme cherche dans le parc un engin disponible de même
catégorie dont la désignation contient celle du besoin. Garder « Compacteur vibrant 15 t » : avec
« Compacteur » seul, le compacteur à pneus disponible serait affecté directement au chantier et le besoin
ne passerait pas par les achats (ce qui reste un bon exemple d'affectation directe à montrer en aparté).

Points à souligner pendant la démonstration :

- **Cloisonnement** : le fournisseur ne voit que ses appels d'offres, contrats et factures ; une ressource
  d'un autre fournisseur répond « introuvable ».
- **Événements** : chaque étape publie un événement Kafka ; les notifications (cloche) et le journal
  d'audit (compte `admin@cse.sn`, menu **Journal d'audit**) se mettent à jour.
- **Secours hors portail** : si un fournisseur ne peut pas déclarer son expédition, le RA dispose du menu
  **…** → **Démarrer la livraison (hors portail)** sur un contrat dont l'engagement est validé.

## 4. Après la séance

Revenir à la base de départ avec `scripts/cse.sh reset --yes` (les comptes Keycloak sont conservés). Si un
nouveau fournisseur a été inscrit à l'étape 2, son compte de portail reste dans Keycloak : le supprimer dans
la console d'administration (http://localhost:8180, realm `cse`, Utilisateurs) pour pouvoir rejouer
l'inscription avec la même adresse.
