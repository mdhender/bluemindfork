#!/bin/bash
#BEGIN LICENSE
#
# Copyright © Blue Mind SAS, 2012
#
# This file is part of BlueMind. BlueMind is a messaging and collaborative
# solution.
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of either the GNU Affero General Public License as
# published by the Free Software Foundation (version 3 of the License).
#
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
#
#
#END LICENSE


test $# -eq 5 || {
    echo "usage: $0 db user password lang installationtype"
    exit 1
}

db=$1
user=$2
pw=$3
bm_lang=$4
host=localhost

export PGPASSWORD=$pw

curdir=`dirname $0`

echo " ** Update template databases"
sudo -n -u postgres -i -- psql template1 <<EOF
UPDATE pg_database SET datallowconn=TRUE WHERE datname='template0';
EOF

sudo -n -u postgres -i -- psql template0 <<EOF
UPDATE pg_database SET datistemplate=FALSE WHERE datname='template1';
DROP DATABASE template1;
CREATE DATABASE template1 WITH template=template0 ENCODING='UTF8' LC_CTYPE='en_US.utf8' LC_COLLATE='en_US.UTF8';
UPDATE pg_database SET datistemplate=TRUE WHERE datname='template1';
EOF

sudo -n -u postgres -i -- psql template1 <<EOF
UPDATE pg_database SET datallowconn=FALSE WHERE datname='template0';
DROP DATABASE postgres;
CREATE DATABASE postgres TEMPLATE=template1;
EOF

echo " ** Delete old database"
sudo -n -u postgres -i -- dropdb "${db}"

sudo -n -u postgres -i -- dropuser ${user}

echo " ** Creating role '${user}' (pw: ${pw}) & db '${db}' (lang: ${bm_lang})..."
sudo -n -u postgres -i -- createuser --createdb --superuser --no-createrole --login ${user}

sudo -n -u postgres -i -- psql template1 <<EOF
ALTER USER ${user} WITH PASSWORD '${pw}'
EOF

echo " ** Create new ${db} database"

sudo -n -u postgres -i -- createdb --owner=${user} --encoding=UTF-8 "${db}"

sudo -n -u postgres -i -- psql "${db}" <<EOF
CREATE OR REPLACE PROCEDURAL LANGUAGE plpgsql;
EOF

sudo -n -u postgres -i -- psql "${db}" <<EOF
ALTER DATABASE "${db}" SET TIMEZONE='GMT'
EOF

#psql -U ${user} -h ${host} ${db} -f \
#    ${curdir}/bm_iw_create.sql > /tmp/data_insert.log 2>&1
#grep -i error /tmp/data_insert.log && {
#    echo "error in pg script"
#    exit 1
#}

exit 0
