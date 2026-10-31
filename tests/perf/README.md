# Tests de charge (k6)

`k6/scenario.js` simule des utilisateurs simultanés répartis par rôle (chef de chantier 35 %, responsable
matériel 25 %, responsable achats 20 %, DAF 10 %, direction générale 10 %). Chaque utilisateur virtuel
obtient un jeton Keycloak (client de développement `cse-tests-e2e`, *password grant*, absent en
production), puis enchaîne des « écrans » de 2 à 3 lectures parallèles sur les endpoints principaux,
avec un temps de réflexion de 1 à 3 s. Seuls des endpoints autorisés pour le rôle sont appelés : toute
réponse non 200 est une erreur.

Seuils (exigences non fonctionnelles) : **p95 < 2 s** et **taux d'erreur < 1 %** sur les requêtes API
(l'obtention des jetons est exclue des seuils). k6 renvoie un code de sortie non nul si un seuil est franchi.

## Lancer

Prérequis : plateforme démarrée (`make infra start`), aucune installation de k6 nécessaire.

```bash
# Profil cible du cahier des charges : 150 utilisateurs, montée 1 min, plateau 5 min
make perf
# Paramétré (variables documentées en tête de scenario.js)
make perf PERF_ARGS="-e VUS=200 -e PLATEAU=10m"
# Équivalent sans make
docker run --rm --network host -v "$PWD/tests/perf/k6:/scripts:ro" grafana/k6:0.54.0 \
  run -e VUS=50 -e MONTEE=10s -e PLATEAU=30s /scripts/scenario.js
# À travers le frontal HTTPS conteneurisé (certificat de développement)
make perf PERF_ARGS="-e BASE_URL=https://localhost:8443 -e INSECURE=true"
```

## Limitation de débit de la passerelle

La passerelle limite le débit **par utilisateur authentifié** (`sub` du jeton ; à défaut par IP) :
`GATEWAY_RATE_LIMIT_REPLENISH=20` jetons/s, rafale `GATEWAY_RATE_LIMIT_BURST=40`. Le scénario utilise les
5 comptes de démonstration : au-delà d'environ 50 utilisateurs virtuels, des dizaines d'utilisateurs
virtuels partagent le même compte et donc le même seau (ex. 200 VU → ~70 VU « chef de chantier » ≈ 85 req/s
sur un seul compte), ce qui déclenche des réponses **429** qui ne reflètent pas un usage réel (un humain ne
dépasse pas quelques requêtes par seconde). Le compteur `cse_reponses_429` les isole dans le résumé.

Pour une campagne à 100-200 utilisateurs, **ne pas désactiver la limitation** (`GATEWAY_RATE_LIMIT_ENABLED`
doit rester `true`, elle fait partie du système mesuré) ; deux options :

1. *Recommandé* : créer N comptes de test par rôle dans un realm de recette et répartir les VU dessus
   (le seau reste par utilisateur, comme en production) ;
2. sur l'environnement de performance uniquement, ajuster le seau au nombre de VU par compte, ex.
   `GATEWAY_RATE_LIMIT_REPLENISH=200 GATEWAY_RATE_LIMIT_BURST=400` au démarrage de `ms-gateway`
   (redémarrer la passerelle ; jamais en production).

## Résultats de référence

| Date | Environnement | Charge | p95 API | Erreurs | 429 |
|---|---|---|---|---|---|
| 2026-10-31 | Poste de dev 12 Go, 10 services sur l'hôte (JVM 256 Mo, profil dev), accès direct passerelle :8080 | 50 VU, 10 s + 30 s + 5 s, 2 404 requêtes API | **23 ms** (p99 33 ms, max 126 ms) | 0 % | 0 |

La campagne complète (150-200 VU, 5-10 min) est à exécuter sur l'environnement de recette, pas sur un poste
de développement partagé (mémoire limitée).
