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

echo "Sitting at $WORKSPACE/open, going to build"

./ci/validate_json.sh || {
    echo "You have an invalid .json in git"
    exit 1
}

# ensure a clean build with reusing existing core client
rm -f *.tar.bz2

./ci/update_changelog.sh
# binary build, the tgz is so big....
dpkg-buildpackage -us -uc -b
retval=$?

test $retval -eq 0 || exit 1

# debian build is successfull, copy build artifacts
# hudson job is configured to record the build_artifacts directory content
rm -fr ${artifacts}
mkdir -p ${artifacts}
mv ../*.changes ../*.deb ${artifacts}

# copy influx
cp grafana/prebuilt/deb/*.deb ${artifacts}

pushd ${artifacts}
dpkg-scanpackages . /dev/null | gzip -9c > Packages.gz
popd


###############
# Client APIs #
###############
pushd ${artifacts}
rm -fr bluemind-client-api
mkdir bluemind-client-api
pushd bluemind-client-api
tar xfj $WORKSPACE/open/core-client.tar.bz2
mkdir jars
mv core-client/plugins/net.bluemind.core*.jar jars
mv core-client/plugins/net.bluemind.locator*.jar jars
rm -fr core-client
cp ${WORKSPACE}/open/plugins/net.bluemind.lib.async-http/lib/async-http-client-*[0-9].jar jars
cp ${WORKSPACE}/open/plugins/net.bluemind.lib.netty/lib/netty-*.Final.jar jars
cp ${WORKSPACE}/open/plugins/net.bluemind.slf4j/lib/*.jar jars
popd
jar cvfM bluemind-client-api-${BUILD_NUMBER}.zip bluemind-client-api
rm -fr bluemind-client-api
popd



mkdir -p stats.tmp
echo "Running gitstats..."
# this crappy thing adds 8min to buid :/
#time gitstats . stats.tmp >/dev/null 2>&1 || true
cp -r stats.tmp ${artifacts}/stats

popd

# no auth on this setup wizard
ssh root@${junitVm} rm -f /etc/nginx/sw.htpasswd
ssh root@${junitVm} postsuper -d ALL

$BOCLIENT step $BUILD_NUMBER $JOB_NAME $COMMIT

exit $retval
