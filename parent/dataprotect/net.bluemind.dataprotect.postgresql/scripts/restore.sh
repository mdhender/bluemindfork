#!/bin/bash
#set -e
#set -x

## WARNING: variables are "templates", replaced by BlueMind

echo " * Create new ${db} database"
sudo -Hu postgres createdb --owner="${user}" --encoding=UTF-8 "${db}"
sudo -Hu postgres psql -d "${db}" -c "CREATE OR REPLACE LANGUAGE plpgsql"
sudo -Hu postgres psql -d "${db}" -c "ALTER DATABASE \"${db}\" SET TIMEZONE='GMT'"

echo " * Loading ${dumpPath}..."
pg_restore -l ${dumpPath} | grep -v -E '${excludeData}' > /tmp/restore.list
PGPASSWORD=${pass} pg_restore -U ${user} -h localhost -d "${db}" -L /tmp/restore.list ${dumpPath} 2>&1

echo "** Restore complete. Proceed with upgrade..."
