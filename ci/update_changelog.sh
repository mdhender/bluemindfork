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

source ./ci/release

source build.infos

test -z "${BUILD_NUMBER}" && {
    echo "BUILD_NUMBER is not defined. Not runned by hudson ?!"
}

test -z "${BO_DISPLAY_NAME}" && {
    echo "BO_DISPLAY_NAME is not defined. Not runned by hudson ?!"
    BO_DISPLAY_NAME=`cat dn.properties`
}

if [ "$1" != "redhat" ]; then
    dch --newversion ${release}.${BUILD_NUMBER:-0} \
"Hudson automated build (build tag: ${BUILD_TAG})"
fi

echo "version.properties: version=${release}.${BUILD_NUMBER:-0}"
echo "version=${release}.${BUILD_NUMBER:-0}" > version.properties
echo "dn=${BO_DISPLAY_NAME}" >> version.properties

