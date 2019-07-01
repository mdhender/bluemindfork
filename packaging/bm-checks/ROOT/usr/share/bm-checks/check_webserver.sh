#!/bin/bash

JAVA_APP="net.bluemind.webmodules.launcher.webLauncher"
APP_NAME="BlueMind Webserver"
WORKSPACE=`dirname $0`

source ${WORKSPACE}"/check.lib"

function check_webserver {
    ret=$(curl --connect-timeout 30 --max-time 120 --silent -XOPTIONS http://localhost:8080/)
    result=$?
    
    if [[ "$result" > 0 ]]; then
        echo "[ERROR] Connection failed for ${APP_NAME}"
        exit 2
    fi
}


check_hprof
check_networkport "" 8080
check_webserver

exit_ok
