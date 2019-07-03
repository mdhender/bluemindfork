#!/bin/bash

tikapid=$(cat /var/run/bm-tika.pid)
echo "$(date) - Killing '${tikapid}'...." >> /var/log/bm-tika/tika.oom.log

kill -9 ${tikapid}
