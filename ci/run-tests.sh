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
BRANCH_NAME=`echo "$JOB_NAME" | cut -f2 -d-`

artifacts=build_artifacts

BOCLIENT=/usr/share/bo-client/bo-client
COMMIT=`git rev-parse HEAD`

pushd $WORKSPACE

source ./ci/release

$BOCLIENT infos $BUILD_NUMBER > build.infos
source build.infos


echo "Sitting at $WORKSPACE, going to build"

JAVA_HOME=/usr/lib/jvm/bm-jdk
export JAVA_HOME

test -f $JAVA_HOME/bin/java || {
    echo "JDK not found in JAVA_HOME: $JAVA_HOME"
    exit 1
}

./pde_build.sh test-launcher ./plugins/net.bluemind.tests.launcher/tests.product

rm -fr ${artifacts}
mkdir -p ${artifacts}

# run the test-launcher
rm -Rf /tmp/${BRANCH_NAME}
mkdir /tmp/${BRANCH_NAME}
pushd /tmp/${BRANCH_NAME}
rm -fr test-launcher
tar xfj ${WORKSPACE}/test-launcher.tar.bz2
pushd test-launcher
./test-launcher || true
cp junit-report.xml ${WORKSPACE} || true
popd
popd


pushd ${WORKSPACE}/ui/common/php/tests/bluemind
./ci_run.sh
mv *-report.xml ${WORKSPACE} || true
# rm the old filename I used for global reports
rm -f ${WORKSPACE}/phpunit-report.xml 
popd

# download bmcore logs for the tests run & publish them as artifacts
scp root@${junitVm}:/var/log/bm/core.log* ${artifacts}/ || true

ssh root@${junitVm} postsuper -d ALL

$BOCLIENT step $BUILD_NUMBER $JOB_NAME $COMMIT

$BOCLIENT finish $BUILD_NUMBER

exit $retval
