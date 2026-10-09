#!/usr/bin/env bash
# Nightly PostgreSQL backup with retention. Install with cron, e.g.:
#   15 3 * * * /opt/ideaforge/infra/backup.sh >> /var/log/ideaforge-backup.log 2>&1
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/var/backups/ideaforge}"
RETAIN_DAYS="${RETAIN_DAYS:-14}"
COMPOSE_FILE="${COMPOSE_FILE:-/opt/ideaforge/infra/docker-compose.prod.yml}"
POSTGRES_DB="${POSTGRES_DB:-ideaforge}"
POSTGRES_USER="${POSTGRES_USER:-ideaforge}"

mkdir -p "$BACKUP_DIR"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
FILE="$BACKUP_DIR/$POSTGRES_DB-$STAMP.sql.gz"

docker compose -f "$COMPOSE_FILE" exec -T postgres \
  pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" | gzip > "$FILE"

# Fail loudly on empty/corrupt dumps.
if [ ! -s "$FILE" ] || ! gzip -t "$FILE"; then
  echo "backup failed: $FILE is empty or corrupt" >&2
  rm -f "$FILE"
  exit 1
fi

echo "backup written: $FILE ($(du -h "$FILE" | cut -f1))"

# Retention.
find "$BACKUP_DIR" -name "$POSTGRES_DB-*.sql.gz" -mtime "+$RETAIN_DAYS" -delete

# Restore reminder:
#   gunzip -c "$FILE" | docker compose -f "$COMPOSE_FILE" exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"
