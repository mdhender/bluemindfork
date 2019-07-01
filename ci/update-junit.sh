#!/bin/bash
#BEGIN LICENSE
#
# Copyright © Blue Mind SAS, 2012-2016
#
# This file is part of BlueMind. BlueMind is a messaging and collaborative
# solution.
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of either the GNU Affero General Public License as
# published by the Free Software Foundation (version 3 of the License).
#
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
#
# See LICENSE.txt

scriptDir=`dirname $0`

artifacts=build_artifacts

BOCLIENT=/usr/share/bo-client/bo-client
COMMIT=`git rev-parse HEAD`

pushd $WORKSPACE/open

source ./ci/release

$BOCLIENT infos $BUILD_NUMBER > build.infos
source build.infos


echo "Sitting at $WORKSPACE/open, going to build"

export JAVA_HOME=/usr/lib/jvm/bm-jdk

rm -fr ${artifacts}
mkdir -p ${artifacts}

ending() {
    ssh root@${junitVm} /etc/init.d/bm-iptables restart
}
trap ending EXIT

ssh root@${junitVm} /etc/init.d/bm-iptables stop

ssh root@${junitVm} killall -9 7zr

ssh root@${junitVm} aptitude -q -y --allow-untrusted update
ssh root@${junitVm} aptitude -q -y --allow-untrusted full-upgrade
upgradeVm=$?
echo "Return status: "${upgradeVm}
if [ ${upgradeVm} -ne 0 ]; then
    exit ${upgradeVm}
fi
ssh root@${junitVm} aptitude -q -y --allow-untrusted clean
# Ensure that bm-full is installed
ssh root@${junitVm} aptitude -q -y --allow-untrusted install bm-full
upgradeVm=$?
echo "Return status: "${upgradeVm}
if [ ${upgradeVm} -ne 0 ]; then
    exit ${upgradeVm}
fi
# Ensure that some packages are installed
ssh root@${junitVm} aptitude -q -y --allow-untrusted install bm-plugin-core-ldap-samba-export bm-plugin-core-firewall
upgradeVm=$?
echo "Return status: "${upgradeVm}
if [ ${upgradeVm} -ne 0 ]; then
    exit ${upgradeVm}
fi

ssh root@${junitHostVm} aptitude -q -y --allow-untrusted update
ssh root@${junitHostVm} aptitude -q -y --allow-untrusted full-upgrade
upgradeVm=$?
echo "Return status: "${upgradeVm}
if [ ${upgradeVm} -ne 0 ]; then
    exit ${upgradeVm}
fi
ssh root@${junitHostVm} aptitude -q -y --allow-untrusted clean
# reopen bm-node "chakra"
ssh root@${junitHostVm} rm -f /etc/bm/bm.jks
ssh root@${junitHostVm} rm -f /etc/bm/nodeclient_truststore.jks
ssh root@${junitHostVm} /etc/init.d/bm-node restart

# clear vm bm core logs before running
ssh root@${junitVm} rm -f /var/log/bm/core.log* || true
ssh root@${junitVm} rm -f /var/log/bm-locator/locator.log* || true

# don't enable DEBUG log level
ssh root@${junitVm} sed -i -e 's/^LOGBACK_PROPS_FILE/#LOGBACK_PROPS_FILE/' /etc/default/bm-core

ssh root@${junitVm} /etc/init.d/bm-core restart

# clear backups & docs, speed up things
ssh root@${junitVm} rm -fr /var/spool/bm-docs/
ssh root@${junitVm} mkdir -p /var/spool/bm-docs/
ssh root@${junitVm} /root/reset_backup.sh


# Run BM setup wizard update

function sw_call() {
    echo "call to handler $1..."
    
    rm -f ./upd.output
    wget --quiet -O ./upd.output --header="X-GWT-Permutation: 37510BE1F768FEF3BE791A07F0541284" --header="Content-Type:text/x-gwt-rpc; charset=utf-8" --no-check-certificate --post-data $2 http://${junitVm}:8080/setup/setupwizard/$1
    
    #cat ./upd.output
    echo ""
    echo "= CALL COMPLETE ="
    echo ""
}

rpc_key=C95E8605BF7004332CEC247D175EA84C

sw_call dbm "7|0|4|https://${junitVm}/setup/setupwizard/|$rpc_key|net.bluemind.sw.client.rpc.DatabaseManager|updateForCore|1|2|3|4|0|"

uuid=`cat upd.output|sed -e 's/.*","\([0-9a-f\-]*\)".*/\1/g'`
taskref=`cat upd.output|sed -e 's/.*\/\([0-9]*\)".*/\1/g'`

echo "upgrade task uuid: $uuid, taskref: $taskref"

final_rpc="7|0|5|https://${junitVm}/setup/setupwizard/|A860A1D61C9E7A09B78429DCFD91181F|net.bluemind.sw.client.rpc.CoreTasks|doFinalTasks|java.lang.String/2004016611|1|2|3|4|1|5|0|"

function task_status() {
    rpc_key=C600D8805933FB2FC54D219D0E1BF445
    sw_call task "7|0|6|https://${junitVm}/setup/setupwizard/|$rpc_key|net.bluemind.sw.client.rpc.TaskMonitor|getStatus|net.bluemind.sw.shared.TaskRef/${taskref}|${uuid}|1|2|3|4|1|5|5|6|"

    grep -q '^//OK\[0,0,1,1,' ./upd.output && {
      echo "Calling final tasks...."
      sw_call core $final_rpc
      echo "Final task call complete"
      echo "Upgrade is COMPLETE"

      $BOCLIENT step $BUILD_NUMBER $JOB_NAME $COMMIT
      exit $?
    }

    sleep 1
}


for i in `seq 0 300`; do
    echo "task_status $i"
    task_status
done

exit $?
