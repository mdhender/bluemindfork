#!/bin/bash
#set -e
#set -x

## WARNING: variables are "templates", replaced by BlueMind

echo "** Cleanup old dataprotect databases"
export PGPASSWORD=bj

dp_dbs=`psql -U bj -h localhost -d bj -c "COPY (SELECT datname FROM pg_database WHERE datistemplate=false and datname like 'dp%') TO STDOUT"`

for i in $dp_dbs; do
    echo "Disconnect all users from database $i"
    sql_disconnect="SELECT pg_terminate_backend(pg_stat_activity.pid)
        FROM pg_stat_activity
        WHERE pg_stat_activity.datname = '$i'
        AND pid <> pg_backend_pid();"
    su postgres -c "psql -U bj -h localhost -d bj -c \"$sql_disconnect\""
    echo "Dropping database $i"
    su postgres -c "dropdb $i"
done

echo "** Restore from ${dumpPath} to ${db}"

# create db
echo " * Create new ${db} database"

su - postgres -c 'createdb --owner=${user} --encoding=UTF-8 ${db}'

su - postgres -c 'psql ${db} <<EOF
CREATE LANGUAGE plpgsql;
EOF'

su - postgres -c 'psql ${db} <<EOF
ALTER DATABASE ${db} SET TIMEZONE='GMT'
EOF'

echo " * Loading ${dumpPath}..."
PGPASSWORD=${pass} pg_restore -l  ${dumpPath} | grep -v -E '${excludeData}' > /tmp/restore.list
PGPASSWORD=${pass} pg_restore -U ${user} -h localhost -d ${db} -L /tmp/restore.list ${dumpPath} 2>&1

echo "** Restore complete. Proceed with upgrade..."
