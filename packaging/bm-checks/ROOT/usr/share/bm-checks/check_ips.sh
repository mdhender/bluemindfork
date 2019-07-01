#!/bin/bash

JAVA_APP="net.bluemind.ips.vertx.ips"
APP_NAME="BlueMind IPS Server"

WORKSPACE=`dirname $0`
source ${WORKSPACE}"/check.lib"

function check_ips {
    if [ ! -e ${WORKSPACE}/check_ips.expect ]; then
        echo "[ERROR] Missing expect script!"
        exit 2
    fi
    expect -f ${WORKSPACE}/check_ips.expect

        ret=$?
        if [ ${ret} -ne 0 ]; then
                exit ${ret}
        fi
}

check_hprof
check_networkport "" 144
check_ips

exit_ok
