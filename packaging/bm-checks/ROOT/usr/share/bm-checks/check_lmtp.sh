#!/bin/bash

JAVA_APP="net.bluemind.lmtp.id1"
APP_NAME="BlueMind LMTP Server"
WORKSPACE=`dirname $0`

source ${WORKSPACE}"/check.lib"

check_hprof
check_networkport "" 2400

echo -ne 'quit\r\n' > /tmp/logout.lmtp
token=`cat /etc/bm/bm-core.tok`
lmtptest -u admin0 -a admin0 -p 2400 -f /tmp/logout.lmtp localhost >/dev/null 2>&1 | grep -v "Authentication failed. no mechanism available"
ret=${PIPESTATUS[0]}
rm /tmp/logout.lmtp

if [ ${ret} -ne 0 ]; then
    echo "[ERROR] "${APP_NAME}" fail"
    exit 2
fi

exit_ok
