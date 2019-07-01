#!/bin/bash

JAVA_APP="net.bluemind.locator.app"
APP_NAME="BlueMind Locator Server"
WORKSPACE=`dirname $0`

source ${WORKSPACE}"/check.lib"

function check_locator {
	coreip=$(curl --connect-timeout 30 --max-time 120 -X GET --silent http://localhost:8084/location/host/bm/core/admin0@global.virt)

	if [ -z ${coreip} ]
		then
			echo "[ERROR] Could not connect to ${APP_NAME}"
			exit 2
	fi
}


check_hprof
check_networkport "" 8084
check_locator

exit_ok
