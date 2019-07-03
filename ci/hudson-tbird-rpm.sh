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

tmpBuildDir=/tmp/${BRANCH_NAME}/tbird-rpm
rm -rf ${tmpBuildDir}
mkdir -p ${tmpBuildDir}
XPI="${tmpBuildDir}/bm-connector-tb-${release}.${BUILD_NUMBER}.xpi"
artifacts=build_artifacts

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

echo "Cleanning RedHat tree..."
rm -f mozilla/redhat/SRPMS/*rpm
rm -f mozilla/redhat/RPMS/x86_64/*rpm

# Build RPMs
rpmbuild --define "_topdir ${WORKSPACE}/open/mozilla/redhat" --define "_hudson_release ${BUILD_NUMBER}" --define "_branch_name ${BRANCH_NAME}" -ba ${WORKSPACE}/open/mozilla/redhat/SPECS/*spec
retval=$?

test $retval -eq 0 || exit 1

# RPM build is successfull, copy build artifacts
# hudson job is configured to record the build_artifacts directory content
rm -fr ${artifacts}
mkdir -p ${artifacts}
mv mozilla/redhat/RPMS/x86_64/*rpm ${artifacts}
pushd ${artifacts}
# Do ${artifacts} a yum repo
createrepo ${WORKSPACE}/open/${artifacts}
popd

rm -f $XPI

$BOCLIENT step $BUILD_NUMBER $JOB_NAME $COMMIT

exit $retval
