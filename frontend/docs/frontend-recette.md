# Recette frontend

1. Ouvrir `http://localhost:5173/` sans session : redirection attendue vers `/connexion`.
2. Choisir chaque carte de profil et utiliser l’adresse correspondante avec `VITE_DEMO_PASSWORD`.
3. Vérifier que la sidebar ne contient que les entrées du profil connecté.
4. Ouvrir manuellement un autre espace : redirection attendue vers `/acces-refuse`.
5. Utiliser « Se déconnecter », confirmer, puis vérifier `/deconnexion` et la purge de session.
6. Ouvrir `http://localhost:5174/` sans session : redirection vers `/connexion` fournisseur.
