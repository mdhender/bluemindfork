#!/bin/bash

JAVA_APP="net.bluemind.ysnp.ysnp"
APP_NAME="BlueMind ysnp Server"
WORKSPACE=`dirname $0`

source ${WORKSPACE}"/check.lib"

function check_ysnp(){
    token=`cat /etc/bm/bm-core.tok`
    /usr/sbin/testsaslauthd -u admin0@global.virt -p $token >/dev/null 2>&1
    ret=$?

    if [[ "$ret" > 0 ]]; then
        echo "[ERROR] SASL authentication fail!"
        exit 2
    fi
}

check_hprof
check_networkport "" 25250
check_ysnp

exit_ok
