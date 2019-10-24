#!/bin/bash

JAVA_APP="net.bluemind.application.launcher.coreLauncher"
APP_NAME="BlueMind Core Server"
WORKSPACE=`dirname $0`

source ${WORKSPACE}"/check.lib"

function check_core {
    token=$(cat /etc/bm/bm-core.tok)

    login=$(curl --connect-timeout 30 --max-time 120 -X POST --silent --data "\"${token}\"" http://localhost:8090/api/auth/login?login=admin0@global.virt&origin=check_core)
    loginret=$?
    if [ $loginret -ne 0 ]
        then
            echo "[ERROR] Could not connect to "${APP_NAME}
            exit 2
    fi

    $(echo $login | grep -qi 'bad')
    error=$?
    if [ $error -eq 0 ]; then
            echo "[ERROR] Login failed for "${APP_NAME}
            exit 2
    fi

    sid=$(echo $login | sed -e 's/^.\+\(authKey[^,]\+\).\+$/\1/i' | sed -e 's/"//g' | cut -d ':' -f 2)
    logout=$(curl --connect-timeout 30 --max-time 120 -X POST --silent --header 'Content-Type: application/json' --header 'Accept: application/json' --header "X-BM-ApiKey: ${sid}" 'http://localhost:8090/api/auth/logout')
    logoutRet=$?
    if [ ${logoutRet} -ne 0 ]; then
        echo " * [ERROR] Logout failed for "${APP_NAME}
        exit 1
    fi

    $(echo $logout | grep -qi 'forbidden')
    ok=$?
    if [ $ok -eq 0 ]
        then
            echo "[ERROR] Logout failed for "${APP_NAME}
            exit 2
    fi

}

check_hprof
check_networkport "" 8090
check_core

exit_ok
