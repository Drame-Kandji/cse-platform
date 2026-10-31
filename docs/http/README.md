# Collections de requêtes HTTP

Fichiers `.http` compatibles **REST Client** (VS Code, extension `humao.rest-client`) et **HTTP Client**
d'IntelliJ IDEA. Toutes les requêtes passent par la passerelle `http://localhost:8080` (plateforme démarrée :
`make infra start`).

1. Obtenir un jeton de développement pour le rôle voulu (client `cse-tests-e2e`, absent en production) :
   ```bash
   scripts/token.sh resp.materiel@cse.sn      # mot de passe Cse@2026 par défaut
   ```
2. Coller la valeur dans la variable `@token` en tête du fichier (le jeton expire après 15 minutes).
3. Les identifiants d'exemple sont ceux du jeu de démonstration (`docs/API_CONTRACT.md` §12) ; les requêtes
   d'écriture **modifient les données** : `scripts/cse.sh reset` pour repartir d'un jeu propre.

| Fichier | Service | Compte conseillé |
|---|---|---|
| `gateway.http` | ms-gateway | tout rôle interne |
| `auth.http` | ms-auth | admin@cse.sn |
| `besoins.http` | ms-besoins | chef.chantier@cse.sn, resp.materiel@cse.sn |
| `suivi.http` | ms-suivi | resp.materiel@cse.sn |
| `fournisseurs.http` | ms-fournisseurs | resp.achats@cse.sn |
| `achats.http` | ms-achats | resp.achats@cse.sn, daf@cse.sn |
| `reporting.http` | ms-reporting | dg@cse.sn |
| `notification.http` | ms-notification | tout rôle interne |
| `chatbot.http` | ms-chatbot | tout rôle interne |
| `audit.http` | ms-audit | admin@cse.sn |
