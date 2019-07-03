#!/bin/bash

JAVA_APP="net.bluemind.dav.server.dav"
APP_NAME="BlueMind dav Server"
WORKSPACE=`dirname $0`

source ${WORKSPACE}"/check.lib"

function check_dav {

	token=$(cat /etc/bm/bm-core.tok)
	response=$(curl --connect-timeout 30 --max-time 120 --silent -XPROPFIND -H 'Content-Type: application/xml' --data-binary @${WORKSPACE}/dav_body.xml http://admin0%40global.virt:${token}@localhost:8080/dav/)

	user=$(echo $response | sed -n 's/.*<d:current-user-principal>\(.*\)<\/d:current-user-principal>.*/\1/p')

	if [[ -z $user ]]
		then
			echo "[ERROR] Incorrect answer from ${APP_NAME}"
			exit 2
	fi

}

check_hprof
check_networkport "" 8080
check_dav

exit_ok
