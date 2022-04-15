#!/bin/bash

JAVA_APP="net.bluemind.sds.proxy.launcher.sds-proxy"
APP_NAME="BlueMind SDS proxy server"
WORKSPACE=`dirname $0`

source ${WORKSPACE}"/check.lib"

function check_sds {
    existrequest=$(curl --connect-timeout 30 --max-time 120 -X POST -o /dev/null -w "%{http_code}" --silent -d'{"size":123456}' http://localhost:8091/msg_size)
    existrequestret=$?
    if [ ${existrequestret} -ne 0 ]
        then
            echo "[ERROR] Could not connect to "${APP_NAME}
            exit 2
    fi

    if [ ${existrequest} -ne 200 ]; then
            echo "[ERROR] HTTP error code "${existrequest}" on "${APP_NAME}
            exit 2
    fi
}

check_hprof
check_networkport "" 8091
check_sds

exit_ok
