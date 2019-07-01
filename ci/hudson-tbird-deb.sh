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

BOCLIENT=/usr/share/bo-client/bo-client
COMMIT=`git rev-parse HEAD`
BRANCH_NAME=`echo "$JOB_NAME" | cut -f2 -d-`

pushd $WORKSPACE/open

source ./ci/release

$BOCLIENT infos $BUILD_NUMBER > build.infos
source build.infos

popd


retval=0
artifacts=${WORKSPACE}/open/build_artifacts
tmpBuildDir=/tmp/${BRANCH_NAME}/tbird-deb
XPI="${tmpBuildDir}/bm-connector-tb-${release}.${BUILD_NUMBER}.xpi"

rm -rf ${tmpBuildDir}
mkdir -p ${tmpBuildDir}
pushd ${tmpBuildDir}

rm update.rdf
rm bm-connector*.xpi
wget http://hudson.blue-mind.loc:8080/hudson/job/${software}-${releaseName}-tbird-xpi/lastSuccessfulBuild/artifact/open/build_artifacts/bm-connector-tb-${release}.${BUILD_NUMBER}.xpi
retval=$?
test $retval -eq 0 || exit 1
wget http://hudson.blue-mind.loc:8080/hudson/job/${software}-${releaseName}-tbird-xpi/lastSuccessfulBuild/artifact/open/build_artifacts/update.rdf
retval=$?
test $retval -eq 0 || exit 1
popd

pushd $WORKSPACE/open

rm -fr ${artifacts}
mkdir -p ${artifacts}

./ci/update_changelog.sh
cp debian/changelog mozilla/debian/changelog
pushd mozilla/
dpkg-buildpackage -us -uc -b
mv ../*.changes ../*.deb ${artifacts}
pushd ${artifacts}
dpkg-scanpackages . /dev/null | gzip -9c > Packages.gz
popd

rm -f $XPI

$BOCLIENT step $BUILD_NUMBER $JOB_NAME $COMMIT

exit $retval
