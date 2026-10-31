#!/bin/sh
# Sauvegarde PostgreSQL de la plateforme CSE (exécuté dans le conteneur « backup », image postgres:15-alpine).
#   backup.sh once     une sauvegarde immédiate puis purge de rétention
#   backup.sh daemon   boucle : une sauvegarde par jour à BACKUP_HEURE (UTC), purge après chaque sauvegarde
# Contenu d'une sauvegarde (répertoire $BACKUP_DIR/AAAAMMJJ-HHMMSS) :
#   globals.sql.gz   rôles et droits globaux (pg_dumpall --globals-only) : ms_* et keycloak
#   cse.dump         base « cse », tous les schémas (format custom pg_dump, compressé, restauration sélective)
#   keycloak.dump    base « keycloak » (realm, utilisateurs, OTP)
#   SHA256SUMS       empreintes pour vérifier l'intégrité avant restauration
# Variables : PGHOST, PGPORT, PGUSER, PGPASSWORD (libpq), BACKUP_DIR, BACKUP_BASES, BACKUP_RETENTION_JOURS,
#             BACKUP_HEURE, BACKUP_COMPRESSION.
set -eu

BACKUP_DIR="${BACKUP_DIR:-/backups}"
BACKUP_BASES="${BACKUP_BASES:-cse keycloak}"
BACKUP_RETENTION_JOURS="${BACKUP_RETENTION_JOURS:-90}"
BACKUP_HEURE="${BACKUP_HEURE:-02:00}"
BACKUP_COMPRESSION="${BACKUP_COMPRESSION:-6}"
JOURNAL="$BACKUP_DIR/backup.log"

log() {
  ligne="$(date -u +%Y-%m-%dT%H:%M:%SZ) [backup] $*"
  echo "$ligne"
  echo "$ligne" >>"$JOURNAL"
}

sauvegarder() {
  horodatage="$(date -u +%Y%m%d-%H%M%S)"
  cible="$BACKUP_DIR/$horodatage"
  temp="$BACKUP_DIR/.en-cours-$horodatage"
  mkdir -p "$temp"
  debut="$(date +%s)"
  log "début de la sauvegarde $horodatage (serveur $PGHOST:${PGPORT:-5432}, bases : $BACKUP_BASES)"

  if ! pg_isready --timeout=10 >/dev/null 2>&1; then
    log "ERREUR : serveur PostgreSQL injoignable"
    rm -rf "$temp"
    return 1
  fi
  # Les mots de passe des rôles sont inclus (hachés SCRAM) pour une restauration à l'identique.
  pg_dumpall --globals-only | gzip -"$BACKUP_COMPRESSION" >"$temp/globals.sql.gz"

  for base in $BACKUP_BASES; do
    if pg_dump --format=custom --compress="$BACKUP_COMPRESSION" --dbname="$base" --file="$temp/$base.dump"; then
      log "  $base : $(du -h "$temp/$base.dump" | cut -f1)"
    else
      log "ERREUR : échec du pg_dump de la base $base, sauvegarde $horodatage abandonnée"
      rm -rf "$temp"
      return 1
    fi
  done

  (cd "$temp" && sha256sum globals.sql.gz *.dump >SHA256SUMS)
  # Renommage atomique : un répertoire daté est toujours une sauvegarde complète.
  mv "$temp" "$cible"
  log "sauvegarde $horodatage terminée en $(( $(date +%s) - debut )) s ($(du -sh "$cible" | cut -f1))"
}

purger() {
  # Rétention : suppression des sauvegardes de plus de BACKUP_RETENTION_JOURS jours (et des restes d'échec).
  find "$BACKUP_DIR" -mindepth 1 -maxdepth 1 -type d -name '.en-cours-*' -mmin +720 -exec rm -rf {} +
  anciennes="$(find "$BACKUP_DIR" -mindepth 1 -maxdepth 1 -type d -name '20*' -mtime +"$BACKUP_RETENTION_JOURS")"
  for d in $anciennes; do
    rm -rf "$d"
    log "rétention : $(basename "$d") supprimée (> $BACKUP_RETENTION_JOURS jours)"
  done
}

# Secondes jusqu'à la prochaine occurrence de BACKUP_HEURE (UTC), en arithmétique POSIX.
sans_zero() { v="${1#0}"; echo "${v:-0}"; }
attente() {
  h="$(sans_zero "${BACKUP_HEURE%%:*}")"; m="$(sans_zero "${BACKUP_HEURE##*:}")"
  maintenant=$(( $(sans_zero "$(date -u +%H)") * 3600 + $(sans_zero "$(date -u +%M)") * 60 + $(sans_zero "$(date -u +%S)") ))
  cible=$(( h * 3600 + m * 60 ))
  [ "$cible" -le "$maintenant" ] && cible=$(( cible + 86400 ))
  echo $(( cible - maintenant ))
}

mkdir -p "$BACKUP_DIR"
case "${1:-daemon}" in
  once)
    sauvegarder
    purger
    ;;
  daemon)
    # PID 1 du conteneur : sans gestionnaire, SIGTERM (docker stop) serait ignoré pendant l'attente.
    trap 'log "arrêt du planificateur"; exit 0' TERM INT
    log "planificateur démarré : sauvegarde quotidienne à $BACKUP_HEURE UTC, rétention $BACKUP_RETENTION_JOURS jours"
    while true; do
      delai="$(attente)"
      log "prochaine sauvegarde dans $delai s"
      sleep "$delai" &
      wait $!
      sauvegarder || log "ERREUR : la sauvegarde a échoué, nouvel essai au prochain créneau"
      purger
    done
    ;;
  *)
    echo "Usage : backup.sh once|daemon" >&2
    exit 2
    ;;
esac
