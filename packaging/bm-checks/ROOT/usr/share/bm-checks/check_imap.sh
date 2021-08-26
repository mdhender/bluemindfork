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

function check_imap_children_number {
    processImap=`ps aux | grep imap | wc -l`
    processImapMax=`grep -Po "listen=\"1143.*maxchild=\K(\d+)" /etc/cyrus.conf`

    if [[ $processImap -gt `expr $processImapMax \* 3 / 2` ]]
    then
        echo "[WARNING] Maximum number of imap connexion may be too low. Recommended is 1.5 * number_of_users ($processImap/$processImapMax)"
        exit 1
    fi
}

check_networkport "" 1143
check_imap_connexion
check_imap_children_number

exit_ok
