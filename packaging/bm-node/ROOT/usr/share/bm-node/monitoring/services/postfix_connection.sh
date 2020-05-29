#!/bin/bash

echo -ne 'quit\r\n' > /tmp/logout.smtp
token=`cat /etc/bm/bm-core.tok`
smtptest -u admin0 -a admin0 -w $token -p 25 -f /tmp/logout.smtp localhost >/dev/null
ret=$?
rm /tmp/logout.smtp
exit $ret
