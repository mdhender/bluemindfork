#!/bin/bash

APP_NAME="BlueMind Hazelcast cluster"

WORKSPACE=`dirname $0`
source ${WORKSPACE}"/check.lib"

installationId="72D26E8A-5BB1-48A4-BC71-EEE92E0CE4EE"

# Must listen on a least one network interface
for hostIp in $(hostname -I); do
    state=$(curl --silent --connect-timeout 5 --data "bluemind-${installationId}&dev-pass" http://${hostIp}:5701/hazelcast/rest/management/cluster/state)
    result=$?
    
    [[ "${result}" == 0 ]] && break
done

if [[ "$result" > 0 ]]
  then
    echo "[ERROR] Connection to Hazelcast cluster failed"
    exit 2
fi

if [[ "$state" != '{"status":"success","state":"active"}' ]]
  then
    echo "[ERROR] Hazelcast cluster state is: $state"
    exit 2
fi

exit_ok
