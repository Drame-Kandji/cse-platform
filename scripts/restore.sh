#!/usr/bin/env bash
# Restauration d'une sauvegarde PostgreSQL CSE (format custom pg_dump) dans une base cible.
#   scripts/restore.sh <dump> [options]
# <dump> :
#   fichier .dump de l'hôte                     ex. ./sauvegardes/20261031-020000/cse.dump
#   répertoire de sauvegarde de l'hôte + --base  ex. ./sauvegardes/20261031-020000 --base cse
#   nom de sauvegarde du volume Docker + --base  ex. 20261031-020000 --base keycloak
# Options :
#   --base <nom>          base à restaurer et à créer si absente (défaut : nom du fichier sans .dump)
#   --hote <h>            serveur cible (défaut : localhost)        --port <p>  (défaut : 5433)
#   --utilisateur <u>     superutilisateur cible (défaut : POSTGRES_USER de .env)
#                         mot de passe : PGPASSWORD, sinon POSTGRES_PASSWORD de .env
#   --globals             rejoue d'abord globals.sql.gz (rôles ms_*, keycloak) — serveur neuf
#   --sans-proprietaires  ignore propriétaires et droits (--no-owner --no-acl) — base d'analyse
#   --yes                 pas de confirmation interactive (automatisation, test de restauration)
# La restauration est atomique (--single-transaction) : en cas d'erreur, la base cible reste inchangée.
set -euo pipefail

RACINE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
IMAGE="${CSE_RESTORE_IMAGE:-postgres:15-alpine}"
VOLUME="${CSE_BACKUP_VOLUME:-cse_backups}"
usage() { sed -n '2,17p' "$0"; exit 2; }

[[ $# -ge 1 ]] || usage
SOURCE="$1"; shift
BASE="" HOTE=localhost PORT=5433 UTILISATEUR="" GLOBALS=0 SANS_PROPRIO=0 OUI=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --base) BASE="$2"; shift 2 ;;
    --hote) HOTE="$2"; shift 2 ;;
    --port) PORT="$2"; shift 2 ;;
    --utilisateur) UTILISATEUR="$2"; shift 2 ;;
    --globals) GLOBALS=1; shift ;;
    --sans-proprietaires) SANS_PROPRIO=1; shift ;;
    --yes) OUI=1; shift ;;
    *) usage ;;
  esac
done

# Identifiants : variables d'environnement, à défaut le .env du dépôt.
if [[ -f "$RACINE/.env" ]]; then set -a; source "$RACINE/.env"; set +a; fi
UTILISATEUR="${UTILISATEUR:-${POSTGRES_USER:-postgres}}"
export PGPASSWORD="${PGPASSWORD:-${POSTGRES_PASSWORD:?mot de passe : définir PGPASSWORD ou POSTGRES_PASSWORD}}"

# Localisation de la sauvegarde -> montage Docker (répertoire) + nom du fichier dans ce répertoire.
if [[ -f "$SOURCE" ]]; then
  REP="$(cd "$(dirname "$SOURCE")" && pwd)"; FICHIER="$(basename "$SOURCE")"
  MONTAGE=(-v "$REP:/restore:ro")
elif [[ -d "$SOURCE" ]]; then
  [[ -n "$BASE" ]] || { echo "--base requis avec un répertoire de sauvegarde"; exit 2; }
  REP="$(cd "$SOURCE" && pwd)"; FICHIER="$BASE.dump"
  MONTAGE=(-v "$REP:/restore:ro")
else
  [[ -n "$BASE" ]] || { echo "--base requis avec un nom de sauvegarde du volume $VOLUME"; exit 2; }
  docker volume inspect "$VOLUME" >/dev/null 2>&1 || { echo "Sauvegarde introuvable : $SOURCE"; exit 1; }
  FICHIER="$BASE.dump"
  [[ "$SOURCE" =~ ^[0-9]{8}-[0-9]{6}$ ]] || { echo "Nom de sauvegarde invalide : $SOURCE (attendu AAAAMMJJ-HHMMSS)"; exit 2; }
  MONTAGE=(--mount "type=volume,source=$VOLUME,target=/restore,readonly,volume-subpath=$SOURCE")
fi
BASE="${BASE:-${FICHIER%.dump}}"
[[ "$BASE" =~ ^[a-z_][a-z0-9_]*$ ]] || { echo "Nom de base invalide : $BASE"; exit 2; }

pg() { docker run --rm --network host -e PGPASSWORD -e PGHOST="$HOTE" -e PGPORT="$PORT" -e PGUSER="$UTILISATEUR" \
         "${MONTAGE[@]}" "$IMAGE" "$@"; }

echo "Sauvegarde : $SOURCE ($FICHIER)"
echo "Cible      : base « $BASE » sur $HOTE:$PORT (utilisateur $UTILISATEUR)"
pg test -f "/restore/$FICHIER" || { echo "Fichier $FICHIER absent de la sauvegarde"; exit 1; }

# Intégrité : empreintes produites par backup.sh.
if pg test -f /restore/SHA256SUMS; then
  pg sh -c "cd /restore && grep ' $FICHIER\$' SHA256SUMS | sha256sum -c -" || { echo "Empreinte SHA-256 invalide : restauration refusée"; exit 1; }
fi
pg pg_restore --list "/restore/$FICHIER" >/dev/null || { echo "Archive illisible : $FICHIER"; exit 1; }

if [[ $OUI -ne 1 ]]; then
  echo "ATTENTION : les objets existants de la base « $BASE » sur $HOTE:$PORT seront SUPPRIMÉS puis recréés."
  read -r -p "Taper le nom de la base pour confirmer : " reponse
  [[ "$reponse" == "$BASE" ]] || { echo "Abandon."; exit 1; }
fi

debut=$(date +%s)
pg pg_isready --timeout=30 >/dev/null || { echo "Serveur cible injoignable"; exit 1; }
if [[ $GLOBALS -eq 1 ]]; then
  pg test -f /restore/globals.sql.gz || { echo "globals.sql.gz absent de la sauvegarde"; exit 1; }
  # Le rôle de connexion (superutilisateur cible) est exclu : sinon son mot de passe serait remplacé par celui
  # du serveur source en cours de session. Les autres rôles déjà présents donnent « already exists », sans effet.
  pg sh -c "gunzip -c /restore/globals.sql.gz | grep -vE '^(CREATE|ALTER) ROLE \"?$UTILISATEUR\"?( |;)' \
    | psql -q -d postgres -v ON_ERROR_STOP=0" 2>&1 | grep -v 'already exists' || true
  echo "Rôles globaux rejoués."
fi
existe=$(pg psql -tAq -d postgres -c "SELECT 1 FROM pg_database WHERE datname = '$BASE'")
if [[ "$existe" != "1" ]]; then
  pg psql -q -d postgres -c "CREATE DATABASE \"$BASE\""
  echo "Base « $BASE » créée."
fi
OPTIONS=(--clean --if-exists --single-transaction --no-password -d "$BASE")
[[ $SANS_PROPRIO -eq 1 ]] && OPTIONS+=(--no-owner --no-acl)
pg pg_restore "${OPTIONS[@]}" "/restore/$FICHIER"
pg psql -q -d "$BASE" -c 'ANALYZE' >/dev/null
echo "Restauration de « $BASE » terminée en $(( $(date +%s) - debut )) s."
