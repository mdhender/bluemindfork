#!/bin/bash

lmtpdpid=$(cat /var/run/bm-lmtpd.pid)
echo "$(date) - Killing '${lmtpdpid}'...." >> /var/log/bm-lmtpd/lmtpd.oom.log

kill -9 ${lmtpdpid}
