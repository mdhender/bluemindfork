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


artifacts=build_artifacts
BOCLIENT=/usr/share/bo-client/bo-client
COMMIT=`git rev-parse HEAD`

pushd $WORKSPACE/open

source ./ci/release

$BOCLIENT infos $BUILD_NUMBER | tail -n+2 > build.infos
source build.infos

# ensure a clean build with reusing existing core client
rm -f *.tar.bz2


echo "Sitting at $WORKSPACE/open, going to build"

echo "Cleanning RedHat tree..."
rm -f redhat/SRPMS/*rpm
rm -f redhat/RPMS/x86_64/*rpm

./ci/update_changelog.sh redhat
# Build RPMs
rpmbuild --define "_topdir ${WORKSPACE}/open/redhat" --define "_hudson_release ${BUILD_NUMBER:-0}" -ba ${WORKSPACE}/open/redhat/SPECS/*spec
retval=$?

test $retval -eq 0 || exit 1

# RPM build is successfull, copy build artifacts
# hudson job is configured to record the build_artifacts directory content
rm -fr ${artifacts}
mkdir -p ${artifacts}
mv ./redhat/RPMS/x86_64/*rpm ${artifacts}

# influx
cp grafana/prebuilt/rpm/*.rpm ${artifacts}

pushd ${artifacts}
# Do ${artifacts} a yum repo
createrepo ${WORKSPACE}/open/${artifacts}
popd

# Run BM setup wizard update
#echo -n 'Mise a jour de Blue Mind: '
#wget --quiet -O /dev/null --header="X-GWT-Permutation: 37510BE1F768FEF3BE791A07F0541284" --header="Content-Type:text/x-gwt-rpc; charset=utf-8" --no-check-certificate --post-data "7|0|4|https://ju-master-rpm.blue-mind.loc/setup/setupwizard/|C95E8605BF7004332CEC247D175EA84C|net.bluemind.sw.client.rpc.DatabaseManager|updateForCore|1|2|3|4|0|" https://ju-master-rpm.blue-mind.loc/setup/setupwizard/dbm
#sleep 20
#echo 'done'

$BOCLIENT step $BUILD_NUMBER $JOB_NAME $COMMIT

exit $retval
