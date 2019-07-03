#!/bin/sh
PRODUCT="bm-$1"
GC_LOG_PATH="/var/log/garbage-collector"
GC_CURRENT_LOG=`ls /var/log/garbage-collector/${PRODUCT}/gc.pause.log.*.current | head -1 | tr -d '\n'`
if [ ! -f "${GC_CURRENT_LOG}" ]
then
	exit 1
fi
memory=`tac ${GC_CURRENT_LOG} | grep -m 1  "GC pause" | cut -d'>' -f2 | cut -d ',' -f1`
echo $memory
