# Authentification frontend

Le gateway est actuellement configuré avec `cse.security.dev-bypass=true` et accepte les requêtes sans JWT en développement. Le frontend utilise donc le mode `demo` par défaut, choisi via `VITE_AUTH_MODE`.

La session utilisateur est conservée uniquement dans `sessionStorage` sous `cse-session`; aucun mot de passe n’est stocké. Les routes métier sont protégées par rôle et un espace différent redirige vers `/acces-refuse`. La déconnexion vide la session et remplace l’URL par `/deconnexion`.

Comptes de démonstration : `chef.chantier@cse.sn`, `resp.materiel@cse.sn`, `resp.achats@cse.sn`, `daf@cse.sn`, `dg@cse.sn`, `admin@cse.sn`. Le mot de passe est fourni par `VITE_DEMO_PASSWORD`.
