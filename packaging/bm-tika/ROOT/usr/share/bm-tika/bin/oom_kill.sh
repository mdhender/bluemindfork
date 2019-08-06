#!/bin/bash
 
tikapid=$(systemctl show -p MainPID --value bm-tika.service)
echo "$(date) - Killing '${tikapid}'...." >> /var/log/bm-tika/tika.oom.log

kill -9 ${tikapid}

touch /var/run/bm-tika.oomkill
