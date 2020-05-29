#!/bin/bash

## WARNING: variables are "templates", replaced by BlueMind

export PGPASSWORD=bj
echo "** Disconnect all users from database ${db}"
sql_disconnect="SELECT pg_terminate_backend(pg_stat_activity.pid)
    FROM pg_stat_activity
    WHERE pg_stat_activity.datname = '${db}'
    AND pid <> pg_backend_pid();"
su postgres -c "psql -U bj -h localhost -d bj -c \"$sql_disconnect\""

echo "** Drop database ${db}"
su postgres -c "dropdb ${db}"
echo "** Drop database ${db} complete."
