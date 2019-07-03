#!/bin/bash

logFile="/tmp/"$(basename $0)".log"
root=$1
from=$2
to=$3

cd ${root}
echo "ROOT: "${root} >> ${logFile}
echo "FROM: "${from} >> ${logFile}
echo "TO: "${to} >> ${logFile}

if [[ "$from" != */ ]]; then
    from=${from}"/"
fi
rsync -aHS ${from} ${to} >> ${logFile} 2>&1
status=$?

echo "status was $status" >> ${logFile}

exit 0
