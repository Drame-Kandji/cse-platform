# Notes API frontend

Le client partagé expose `unwrapList`, `toPage`, `stripEmoji` et la traduction des erreurs RFC 7807. Les contrats fournis par le backend sont la référence TypeScript.

La sécurité du gateway observée dans `backend/ms-gateway` est en bypass de développement par défaut. Les appels réels passent par `/api` et le proxy Vite vers `http://localhost:8080`. Les services n’étaient pas joignables pendant l’audit runtime car leur démarrage attendait PostgreSQL; aucun résultat de test réseau n’a donc été inventé.

Écart observé dans le code backend : le modèle suivi expose `heuresFonctionnement` dans certaines entités alors que le cahier des charges mentionne `heuresUtilisation`; le mapping doit privilégier la réponse réelle du endpoint.
