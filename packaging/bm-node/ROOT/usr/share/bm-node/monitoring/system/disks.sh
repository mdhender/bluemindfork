#!/bin/bash

function check_available_space {
        # directory like /var/spool/bm-hsm doesn't exist at the beginning
        if [ ! -d "$1" ]; then
                return 0
        fi
        usage=`df -h $1 | tail -1 | awk '{ print $5 }' | head -c -2 | tr -d '[[:space:]]'`
        echo $1 $usage%
        if [[ $usage -gt 80 ]]; then
                if [[ $usage -gt 90 ]]; then
                        return 2
                fi
                return 1
        fi
}

global_res=0
mounts=( "/" "/var/spool/cyrus" "/var/spool/bm-hsm" "/var/spool/bm-elasticsearch" "/var/spool/bm-filehosting" "/var/backups/bluemind" "/var/log" )    
for mount in "${mounts[@]}"        
do
        check_available_space $mount
	res=$?
	if (($res > $global_res)); then
		global_res=$res
	fi
done

exit $global_res

