#!/bin/sh

token=$(cat /etc/bm/bm-core.tok)

coreip=$(curl -X GET --silent http://localhost:8084/location/host/bm/core/admin0@global.virt)

echo $coreip
if [ -z $coreip ]
then
	echo " * [ERROR] Could not connect to ${APP_NAME}"
	exit 1
fi

exit 0
