#!/bin/bash

echo "** Cleanup old dataprotect databases"

dp_dbs=$(sudo -Hu postgres psql -c "COPY (SELECT datname FROM pg_database WHERE datistemplate=false and datname like 'dp%') TO STDOUT")

for i in $dp_dbs; do
    echo -n "[$i]: disconnect all users..."
    sql_disconnect="SELECT pg_terminate_backend(pg_stat_activity.pid)
        FROM pg_stat_activity
        WHERE pg_stat_activity.datname = '$i'
        AND pid <> pg_backend_pid();"
    echo " done"
    sudo -Hu postgres psql -q -c "$sql_disconnect"
    echo -n "[$i]: dropdb..."
    sudo -Hu postgres dropdb "$i"
    echo " done"
done

