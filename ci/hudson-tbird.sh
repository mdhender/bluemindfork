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


retval=0
artifacts=${WORKSPACE}/open/build_artifacts

BOCLIENT=/usr/share/bo-client/bo-client
COMMIT=`git rev-parse HEAD`

pushd $WORKSPACE/open

source ./ci/release

$BOCLIENT infos $BUILD_NUMBER > build.infos
source build.infos

echo "Sitting at $WORKSPACE/open, going to build"

pushd mozilla/bm-connector-tb
./make_extension.sh ${BUILD_NUMBER}
popd

# copy build artifacts
# hudson job is configured to record the build_artifacts directory content
rm -fr ${artifacts}
mkdir -p ${artifacts}

mv mozilla/bm-connector-tb/bm-connector-tb-*.xpi ${artifacts} || {
  echo "bm-connector-tb failed to build"
  retval=1
}

pushd mozilla/
. bm-connector-tb/version_major
version="${version}.${BUILD_NUMBER}"
sed -e "s/VERSION/$version/g" update.rdf > ${artifacts}/update.rdf
popd

$BOCLIENT step $BUILD_NUMBER $JOB_NAME $COMMIT

exit $retval
