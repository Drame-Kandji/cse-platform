# Script de démonstration : circuit d'achat avec le portail fournisseur

Démonstration de bout en bout pour un jury, en **15 minutes environ** : un appel d'offres lancé par la CSE,
une offre déposée par le fournisseur sur son portail, l'attribution, la double signature du contrat,
l'engagement budgétaire, l'expédition, la réception, la facture et son paiement. Une ouverture facultative
montre l'inscription d'un nouveau fournisseur. Le détail de chaque écran est dans le
[guide utilisateur](USER_GUIDE.md) (§4, §5 et §8).

## 1. Avant la séance

**Démarrer la plateforme** (voir le [README](../README.md#démarrage-en-5-commandes)) :

```bash
make infra && make start      # infrastructure Docker + 10 microservices (profil dev, données de démo)
make front                    # application interne : http://localhost:5173
make portail                  # portail fournisseur : http://localhost:5174 (autre terminal)
scripts/cse.sh reset --yes    # facultatif : repartir des données de démonstration d'origine
```

Attendre 2 à 3 minutes après le démarrage que les tableaux de bord soient alimentés (`make status`).

**Adresses et comptes** (mot de passe commun **`Cse@2026`**) :

| Qui | Application | Compte | Rôle dans la démo |
|---|---|---|---|
| Fatou Sarr | interne http://localhost:5173 | `resp.achats@cse.sn` | Responsable des Achats (RA) : AO, attribution, contrat, réception |
| Ibrahima Fall | interne http://localhost:5173 | `resp.materiel@cse.sn` | Responsable Matériel (RM) : co-validation de l'attribution |
| Ousmane Ba | interne http://localhost:5173 | `daf@cse.sn` | DAF : engagement, mandatement, paiement |
| Aminata Sow | interne http://localhost:5173 | `dg@cse.sn` | Direction Générale (seulement au-delà de 150 M FCFA) |
| Moussa Kane | portail http://localhost:5174 | `fournisseur@cse.sn` | fournisseur F1 **Sahel Engins SA** |
| - | MailHog http://localhost:8025 | - | e-mails envoyés (définition du mot de passe, notifications) |

**Préparer le navigateur.** Les deux applications partagent la même session Keycloak : un même navigateur
connecté en RA ouvrirait le portail en RA (« Accès réservé aux fournisseurs »). Utiliser donc **deux
navigateurs ou deux profils** : l'un pour la CSE (onglets RA, RM, DAF successivement, en se déconnectant
par le menu à initiales), l'autre, en navigation privée, pour le fournisseur. Dans le premier, l'idéal est
une fenêtre privée par rôle CSE pour basculer sans se reconnecter.

Montant à utiliser : **95 000 000 FCFA**, sous le seuil de délégation de 150 000 000 FCFA, pour que la
co-validation du RM suffise (règle R11). Au-delà, la Direction Générale doit aussi approuver.

## 2. Ouverture facultative : inscription d'un nouveau fournisseur (3 min)

| Étape | Qui, où | Cliquer | Montrer |
|---|---|---|---|
| 2.1 | Visiteur, portail http://localhost:5174 | **Inscrire mon entreprise** ; remplir raison sociale, NINEA (ex. `001234567 2A1`), ville, e-mail (ex. `contact@nouvelle-entreprise.sn`), contact, spécialité ; **Envoyer la demande** | Formulaire public sans compte ; message « Demande envoyée » |
| 2.2 | RA, interne | **Fournisseurs** → sous-onglet **À valider** | La demande apparaît, statut « En attente de validation » (avec Baobab Logistique, déjà en attente dans les données de démo) |
| 2.3 | RA, interne | Ouvrir la fiche → **Valider l'inscription** → confirmer | Message : un e-mail a été envoyé au fournisseur pour définir son mot de passe ; statut **Actif** |
| 2.4 | MailHog http://localhost:8025 | Ouvrir le message adressé au nouveau fournisseur | E-mail Keycloak « définir votre mot de passe » ; le lien permet de choisir un mot de passe puis de se connecter au portail |

La suite de la démonstration utilise le fournisseur F1 Sahel Engins SA, déjà actif et doté d'un historique.

## 3. Circuit principal avec Sahel Engins SA (12 min)

| Étape | Qui, où | Cliquer | Montrer |
|---|---|---|---|
| 3.1 | RA, interne | **Appels d'offres** → **Lancer un appel d'offres** ; objet « Acquisition d'un compacteur », quantité 1, budget 110 000 000 ; critères par défaut ; cocher **Sahel Engins SA** ; date limite J+2 ; **Publier l'appel d'offres** | Assistant en 6 étapes ; seuls les fournisseurs **actifs** de la spécialité sont invitables (R14) ; référence `AO-2026-NNN`, statut **Publié** |
| 3.2 | Fournisseur, portail | **Se connecter** (`fournisseur@cse.sn`) ; **Accueil** | Liste **À faire** : l'AO apparaît avec sa date limite |
| 3.3 | Fournisseur, portail | Ouvrir l'AO → **Déposer une offre** : 95 000 000 FCFA, 20 jours, garantie 12 mois → **Déposer l'offre** | Critères pondérés visibles ; onglet **Mes offres** : offre « En cours » |
| 3.4 | RA, interne | Fiche de l'AO, onglet **Offres** | L'offre est arrivée **anonymisée** (`OFR-…`, R10) |
| 3.5 | RA, interne | **Clôturer** ; onglet **Évaluation** → **Noter** (qualité technique, garantie, SAV, références) ; onglet **Comparatif** | L'anonymat est levé ; prix et délai notés automatiquement ; score pondéré et recommandation |
| 3.6 | RA, interne | Onglet **Attribution** : choisir l'offre, justifier, **Proposer l'attribution** | Mention « dans le seuil, sans approbation de la Direction Générale (R11) » |
| 3.7 | RM, interne | Même AO, onglet **Attribution** → **Co-valider** (commentaire) | AO **Attribué**, contrat **brouillon** généré (R03) ; lien **Voir le contrat** |
| 3.8 | RA, interne | **Voir le contrat** → **Modifier les clauses** (conditions de livraison, maintenance, jalons) → **Signer et envoyer au fournisseur** | La confirmation rappelle que le fournisseur doit contresigner ; statut **En attente du fournisseur**, signature CSE (nom, date) dans la synthèse |
| 3.9 | Fournisseur, portail | **Contrats** → onglet **À signer** → le contrat → **Signer le contrat** → **Signer** | Avancement Signature → Engagement ; option **…** → **Refuser la signature** (motif obligatoire) à mentionner sans l'exécuter |
| 3.10 | RA, interne | Recharger le contrat | Statut **Signé**, deux signatures, « En attente de la validation de l'engagement par le DAF » |
| 3.11 | DAF, interne | **Finance** → onglet **Engagements à valider** → **Valider** (commentaire) | R06 : sans cet engagement, aucune expédition possible |
| 3.12 | Fournisseur, portail | Le contrat (onglet **En cours**, statut « À expédier ») → **Déclarer l'expédition** (date du jour, commentaire « Transport par porte-char ») | Statut **En livraison** ; les jalons peuvent être **marqués atteints** |
| 3.13 | RA, interne | Recharger le contrat ; **Confirmer la livraison** (date, bon de livraison `BL-DEMO-001`) | Date et commentaire d'expédition dans la synthèse ; statut **Livré** ; le matériel est créé dans le **Parc matériel**, disponible (affecté directement au chantier quand l'AO part d'un besoin) |
| 3.14 | Fournisseur, portail | Le contrat → **Déposer la facture** : numéro `FA-DEMO-001`, montant HT 80 000 000, TVA 18 % → **Déposer la facture** | TVA et TTC (94 400 000) calculés en direct ; facture **Reçue** |
| 3.15 | DAF, interne | **Finance** → onglet **Factures** → **Mandater** (référence `MDT-DEMO-001`) puis **Payer** | Statuts Mandatée puis Payée ; synthèse financière mise à jour |
| 3.16 | Fournisseur, portail | **Factures** | La facture est **Payée**, avec sa date de paiement ; montants payés de l'accueil mis à jour |
| 3.17 | RA, interne | Le contrat → **Clôturer le contrat** ; puis **Fournisseurs** → Sahel Engins SA → **Évaluer** | Contrat **Clôturé** ; le score de performance du fournisseur est recalculé (R07) |

Points à souligner pendant la démonstration :

- **Cloisonnement** : le fournisseur ne voit que ses appels d'offres, contrats et factures ; une ressource
  d'un autre fournisseur répond « introuvable ».
- **Événements** : chaque étape publie un événement Kafka ; les notifications (cloche) et le journal
  d'audit (compte `admin@cse.sn`, menu **Journal d'audit**) se mettent à jour.
- **Secours hors portail** : si un fournisseur ne peut pas déclarer son expédition, le RA dispose du menu
  **…** → **Démarrer la livraison (hors portail)** sur un contrat dont l'engagement est validé.

## 4. Démonstration rapide sur les états préparés

Le jeu de démonstration place le fournisseur F1 à chaque étape. Pour une démonstration courte (5 minutes),
se connecter au portail avec `fournisseur@cse.sn` et suivre la liste **À faire** de l'accueil :

| Élément | État préparé | Action à montrer | Côté CSE |
|---|---|---|---|
| **AO-2026-012** · porte-char 50 t | appel d'offres publié, F1 invité | **Déposer une offre** | RA : l'offre arrive anonymisée dans l'onglet **Offres** |
| **CTR-2026-007** · deux camions malaxeurs | signé par la CSE, en attente du fournisseur (après un premier refus motivé) | **Signer le contrat** ou **Refuser la signature** | RA : statut Signé et engagement chez la DAF, ou retour en brouillon avec le motif |
| **CTR-2026-005** · finisseur d'enrobés | engagement validé, « À expédier » | **Déclarer l'expédition** | RA : statut En livraison, date et commentaire d'expédition |
| **CTR-2026-009** · deux chargeuses sur pneus | livré, à facturer | **Déposer la facture** | DAF : facture Reçue, à mandater puis payer |

Côté CSE, **Baobab Logistique** attend la validation de son inscription (Fournisseurs → **À valider**).

Après la séance, revenir aux données d'origine avec `scripts/cse.sh reset --yes`.
