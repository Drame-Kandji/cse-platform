#!/usr/bin/env bash
# Sauvegarde immédiate des bases PostgreSQL CSE (cse + keycloak + rôles globaux), même procédure que la
# sauvegarde quotidienne du service Compose « backup » (infra/backup/backup.sh).
#   scripts/backup-now.sh                 écrit dans le volume Docker des sauvegardes (cse_backups)
#   scripts/backup-now.sh --dest <rép>    écrit dans un répertoire de l'hôte (copie hors site, test de restauration)
#   scripts/backup-now.sh --liste         liste les sauvegardes présentes dans le volume
# Lecture seule pour la base source (pg_dump / pg_dumpall). Aucun client PostgreSQL requis sur l'hôte.
set -euo pipefail

RACINE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
[[ -f "$RACINE/.env" ]] || { echo "Fichier .env absent : cp .env.example .env"; exit 1; }
COMPOSE=(docker compose --env-file "$RACINE/.env" -f "$RACINE/infra/docker-compose.yml" --profile backup)

case "${1:-}" in
  --dest)
    dest="${2:?répertoire de destination attendu}"
    mkdir -p "$dest"
    dest="$(cd "$dest" && pwd)"
    # Le conteneur écrit avec l'UID de l'utilisateur courant : fichiers lisibles sans sudo.
    "${COMPOSE[@]}" run --rm --no-deps --user "$(id -u):$(id -g)" -v "$dest:/backups" backup /bin/sh /opt/cse/backup.sh once
    echo "Sauvegarde écrite dans $dest"
    ;;
  --liste)
    "${COMPOSE[@]}" run --rm --no-deps --entrypoint sh backup -c 'ls -1 /backups | grep "^20" || echo "(aucune sauvegarde)"; tail -n 5 /backups/backup.log 2>/dev/null || true'
    ;;
  "")
    "${COMPOSE[@]}" run --rm --no-deps backup /bin/sh /opt/cse/backup.sh once
    ;;
  *)
    sed -n '2,7p' "$0"; exit 2 ;;
esac
