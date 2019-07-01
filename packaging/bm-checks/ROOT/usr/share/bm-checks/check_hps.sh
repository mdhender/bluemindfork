#!/bin/bash

JAVA_APP="net.bluemind.proxy.http.launcher.hpslauncher"
APP_NAME="BlueMind HPS Server"
WORKSPACE=`dirname $0`

source ${WORKSPACE}"/check.lib"

function check_hps {
	ip=$(curl --connect-timeout 30 --max-time 120 --silent localhost:8079/location/host/mail/imap/admin0@global.virt)

	if [[ -z "$ip" ]]
		then
			echo "[ERROR] No response from ${APP_NAME}"
			exit 2
	fi
}

check_hprof
check_networkport "" 8079
check_hps

exit_ok
