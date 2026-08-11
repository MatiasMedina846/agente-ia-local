#!/bin/bash
#
# Backup automatizado de PostgreSQL (contenedor Docker postgres_agente_ia)
#
# Ejemplo de cron (todas las noches a las 02:00):
#   0 2 * * * /bin/bash /ruta/al/proyecto/backup.sh >> /var/log/backup.log 2>&1
#
set -euo pipefail

CONTAINER=${DB_CONTAINER:-postgres_agente_ia}
DB_USER=${DB_USER:-postgres}
DB_NAME=${DB_NAME:-agente_ia_db}
BACKUP_DIR=${BACKUP_DIR:-/backups}
RETENTION_DAYS=${RETENTION_DAYS:-30}

mkdir -p "$BACKUP_DIR"

FILENAME="$BACKUP_DIR/db_backup_$(date +%Y%m%d_%H%M%S).sql.gz"

docker exec "$CONTAINER" pg_dump -U "$DB_USER" "$DB_NAME" | gzip > "$FILENAME"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Backup creado: $FILENAME"

find "$BACKUP_DIR" -type f -name "db_backup_*.sql.gz" -mtime +"$RETENTION_DAYS" -delete

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Backups anteriores a $RETENTION_DAYS días eliminados."
