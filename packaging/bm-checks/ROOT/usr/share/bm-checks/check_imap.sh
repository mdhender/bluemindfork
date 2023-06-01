#!/bin/bash

APP_NAME="BlueMind Cyrus IMAPd"

WORKSPACE=`dirname $0`
source ${WORKSPACE}"/check.lib"

function check_imap_connexion {
    if [ ! -e ${WORKSPACE}/check_imap.expect ]; then
        echo "[ERROR] Missing expect script!"
        exit 2
    fi
    expect -f ${WORKSPACE}/check_imap.expect

    ret=$?
    if [ ${ret} -ne 0 ]; then
        exit ${ret}
    fi
}

check_networkport "" 1143
check_imap_connexion

exit_ok
