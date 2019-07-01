#!/bin/bash
#BEGIN LICENSE
#
# Copyright © Blue Mind SAS, 2012
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
#
#END LICENSE

#build_hook!!!!
set -e

retval=0

CURDIR=$1
BUILD_DEB_DIR="${CURDIR}/debian"
BUILD_DEB_DIR_CORE="${BUILD_DEB_DIR}/bm-connector-thunderbird"
BRANCH_NAME=`echo "$JOB_NAME" | cut -f2 -d-`

mkdir -p ${BUILD_DEB_DIR_CORE}/usr/share/bm-settings/WEB-INF/
cp /tmp/${BRANCH_NAME}/tbird-deb/update.rdf ${BUILD_DEB_DIR_CORE}/usr/share/bm-settings/WEB-INF/
cp /tmp/${BRANCH_NAME}/tbird-deb/bm-connector-tb-*.xpi ${BUILD_DEB_DIR_CORE}/usr/share/bm-settings/WEB-INF/bm-connector-tb.xpi || {
  echo "bm-connector-tb failed to copy"
  retval=1
}

exit $retval

