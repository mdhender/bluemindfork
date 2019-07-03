#!/bin/bash

JAVA_APP="net.bluemind.eas.push"
APP_NAME="BlueMind Exchange ActiveSync Server"
WORKSPACE=`dirname $0`

source ${WORKSPACE}"/check.lib"

function check_eas {
	$(curl --connect-timeout 30 --max-time 120 --silent -XOPTIONS http://localhost:8082/Microsoft-Server-ActiveSync/)
	result=$?
	
	if [[ "$result" > 0 ]]
		then
			echo "[ERROR] Connection failed for "${APP_NAME}
			exit 2
	fi
}

check_hprof
check_networkport "" 8082
check_eas

exit_ok
