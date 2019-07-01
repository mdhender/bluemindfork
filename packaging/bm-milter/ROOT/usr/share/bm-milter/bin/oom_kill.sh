#!/bin/bash

milterpid=$(cat /var/run/bm-milter.pid)
echo "$(date) - Killing '${milterpid}'...." >> /var/log/bm-milter/milter.oom.log

kill -9 ${milterpid}
