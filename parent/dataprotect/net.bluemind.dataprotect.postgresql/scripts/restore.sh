#!/bin/bash
#set -e
#set -x

echo "** Cleanup old dataprotect databases"
export PGPASSWORD=bj

dp_dbs=`psql -U bj -h localhost -d bj -c "COPY (SELECT datname FROM pg_database WHERE datistemplate=false and datname like 'dp%') TO STDOUT"`

for i in $dp_dbs; do
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
