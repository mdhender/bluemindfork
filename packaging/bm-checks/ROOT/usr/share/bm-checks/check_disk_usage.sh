#!/bin/bash


function check_available_space {
        usage=`df -h $1 | tail -1 | awk '{ print $5 }' | head -c -2 | tr -d '[[:space:]]'`

	if [[ $usage -gt 90 ]]
	then
		echo " * [ERROR] Disk containing $1 is almost full : $usage % used"
		exit 1	
	fi
}

check_available_space "/"
check_available_space "/var/spool/cyrus"
check_available_space "/var/spool/bm-hsm"
check_available_space "/var/backups/bluemind"
check_available_space "/var/spool/bm-elasticsearch"
check_available_space "/var/spool/bm-docs"
check_available_space "/var/log"

exit 0
