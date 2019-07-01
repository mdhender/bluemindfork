#!/bin/bash

echo -ne 'quit\r\n' > /tmp/logout.lmtp
token=`cat /etc/bm/bm-core.tok`
lmtptest -u admin0 -a admin0 -p 2400 -f /tmp/logout.lmtp localhost 2>&1 >/dev/null | grep -v "Authentication failed. no mechanism available"
ret=${PIPESTATUS[0]}
rm /tmp/logout.lmtp
exit $ret
