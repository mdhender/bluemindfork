#!/bin/bash

logFile="/tmp/"$(basename $0)".log"
root=$1
shift

cd ${root}
echo "ROOT: "${root} >> ${logFile}
echo "PARAMS: "$@ >> ${logFile}
cp --parents -a $@ >> ${logFile} 2>&1 
status=$?

echo "status was $status" >> ${logFile}

exit 0
