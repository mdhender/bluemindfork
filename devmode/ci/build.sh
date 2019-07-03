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
set -e

BASEDIR=$(dirname $0)
BM_VERSION=$1
BM_ROOT=$BASEDIR/..

pushd $BM_ROOT
mvn -Dbm-runtime.url=https://forge.bluemind.net/staging/p2/bluemind/$BM_VERSION/ clean tycho-versions:set-version -DnewVersion=$BM_VERSION
mvn -Dbm-runtime.url=https://forge.bluemind.net/staging/p2/bluemind/$BM_VERSION/ clean install

popd

