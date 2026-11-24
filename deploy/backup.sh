#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if [ ! -f .env ]; then
  echo "deploy/.env not found" >&2
  exit 1
fi

set -a
# shellcheck disable=SC1091
source .env
set +a

BACKUP_DIR="${BACKUP_DIR:-/data/shiguangji/backups}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"
DATE="$(date +%F-%H%M%S)"

mkdir -p "$BACKUP_DIR"

echo "[$(date '+%F %T')] dumping database..."
docker compose -f docker-compose.prod.yml exec -T mysql sh -c \
  'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers "$MYSQL_DATABASE"' \
  | gzip > "$BACKUP_DIR/db-$DATE.sql.gz"

echo "[$(date '+%F %T')] archiving uploads..."
tar -czf "$BACKUP_DIR/uploads-$DATE.tar.gz" -C "$DATA_DIR" uploads

find "$BACKUP_DIR" -type f -mtime +"$RETENTION_DAYS" -delete

echo "[$(date '+%F %T')] backup done: $BACKUP_DIR"
