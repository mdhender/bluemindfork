#!/bin/bash

set -e

rm -f ${file}

PGPASSWORD=${pass} pg_dump \
--format=${format} --file=${file} --clean \
--username=${user} --host=localhost \
"${db}"

echo "Dump done in ${file}"
