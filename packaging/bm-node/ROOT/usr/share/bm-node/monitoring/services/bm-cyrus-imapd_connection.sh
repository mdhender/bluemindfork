#!/bin/bash

echo -ne '.logout\r\n' > /tmp/cyrus-logout.imap
token=`cat /etc/bm/bm-core.tok`
imtest -u admin0 -a admin0 -m login -w $token -p 1143 -f /tmp/cyrus-logout.imap localhost >/dev/null
ret=$?
rm /tmp/cyrus-logout.imap
exit $ret

