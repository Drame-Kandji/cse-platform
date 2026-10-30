# Connexion locale avec Keycloak

Keycloak est le serveur d'identité. Il héberge les utilisateurs, les mots de passe et les rôles. Le frontend redirige vers Keycloak, récupère un jeton OAuth2/OIDC, puis envoie ce jeton au gateway dans `Authorization: Bearer ...`. Le gateway vérifie le jeton avec le realm `cse`.

## Démarrage

Depuis `cse-platform` :

```bash
docker compose -f infra/docker-compose.yml up -d postgres keycloak
```

Ouvrir ensuite `http://localhost:8180/admin`.

Compte administrateur local défini par Docker :

- utilisateur : `admin`
- mot de passe : `changeit`

Dans Keycloak :

1. Sélectionner le realm `cse`.
2. Aller dans **Clients** et vérifier `cse-web-interne` et `cse-fournisseur`.
3. Pour le client interne, ajouter `http://localhost:5173/*` dans les URI de redirection et `http://localhost:5173` dans les origines Web.
4. Pour le client fournisseur, ajouter `http://localhost:5174/*` et `http://localhost:5174`.
5. Aller dans **Users**, créer un utilisateur réel, définir son mot de passe dans **Credentials**, puis désactiver **Temporary**.
6. Dans **Role mapping**, ajouter un rôle realm : `CHEF_CHANTIER`, `RESPONSABLE_MATERIEL`, `RESPONSABLE_ACHATS`, `DAF`, `DIRECTION_GENERALE`, `ADMIN_SYSTEME` ou `FOURNISSEUR`.

## Lancement des frontends

Copier les exemples en `.env.local` si nécessaire :

```bash
cp frontend/apps/web-interne/.env.local.example frontend/apps/web-interne/.env.local
cp frontend/apps/portail-fournisseur/.env.local.example frontend/apps/portail-fournisseur/.env.local
```

Puis lancer chaque application avec son script Vite. Ouvrir `http://localhost:5173` pour les équipes internes ou `http://localhost:5174` pour les fournisseurs. Le bouton de connexion ouvre Keycloak ; après connexion, Keycloak redirige vers le portail.

Les comptes créés dans Keycloak doivent avoir un rôle correspondant à leur portail. Le frontend ne contient aucun mot de passe ni compte fictif.
