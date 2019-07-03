#!/bin/bash

JAVA_APP="net.bluemind.xmpp.server.launcher.tigase"
APP_NAME="BlueMind XMPP Server"
WORKSPACE=`dirname $0`

source ${WORKSPACE}"/check.lib"

function check_xmpp {
	response=$(echo "quit" | telnet localhost 5222 2>/dev/null)
	$(echo $response | grep -q "Connected to")
	error=$?
	if [[ "$error" > 0 ]]; then
			echo "[ERROR] Could not connect to "${APP_NAME}
			exit 2
	fi
}

check_hprof
check_networkport "" 5222 5223 5269 5280 5290
check_xmpp

exit_ok
