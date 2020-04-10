#!/bin/bash

echo -ne '. logout\r\n' > /tmp/ips-logout.imap
token=`cat /etc/bm/bm-core.tok`
imtest -u admin0 -a admin0 -m login -w $token -p 144 -f /tmp/ips-logout.imap localhost >/dev/null
ret=$?
rm -f /tmp/ips-logout.imap
exit $ret

