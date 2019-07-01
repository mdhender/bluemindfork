#!/bin/bash

JAVA_APP="net.bluemind.tika.server.tika"
APP_NAME="Tika Server"
WORKSPACE=`dirname $0`

source ${WORKSPACE}/check.lib

function check_tika {
	document=$(curl --connect-timeout 30 --max-time 120 -X POST --silent -H "Content-Type: binary/octet-stream" --data-binary @${WORKSPACE}/tika_test_file.pdf http://localhost:8087/tika)
	echo $document | grep -q "Test de TIKA"
	noerror=$?
	if [ $noerror -gt 0 ]; then
	    echo "[ERROR] "${APP_NAME}" fail!"
	    exit 2
	fi
}

check_hprof
check_networkport "" 8087
check_tika

exit_ok
