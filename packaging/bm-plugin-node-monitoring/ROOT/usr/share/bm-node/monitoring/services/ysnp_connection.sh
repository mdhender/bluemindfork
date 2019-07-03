#!/bin/sh
token=`cat /etc/bm/bm-core.tok`
/usr/sbin/testsaslauthd -u admin0@global.virt -p $token &> /dev/null
ret=$?

exit $ret
