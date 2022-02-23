#!/bin/bash

## WARNING: variables are "templates", replaced by BlueMind

echo "** Disconnect all users from database ${db}"
sql_disconnect="SELECT pg_terminate_backend(pg_stat_activity.pid)
    FROM pg_stat_activity
    WHERE pg_stat_activity.datname = '${db}'
    AND pid <> pg_backend_pid();"
sudo -n -u postgres -- psql -c "$sql_disconnect"

echo "** Drop database ${db}"
sudo -n -u postgres -- dropdb "${db}"
echo "** Drop database ${db} complete."
