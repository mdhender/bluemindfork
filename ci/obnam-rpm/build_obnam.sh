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
#
#END LICENSE


SPECS=~/rpmbuild/SPECS
RPMS=~/rpmbuild/RPMS
PREBUILT=~/prebuilt

root_url=http://kojipkgs.fedoraproject.org/packages

function build() {
  pkg=$1
  version=$2
  build=$3
  rpm -Uvh ${root_url}/${pkg}/${version}/${build}/src/${pkg}-${version}-${build}.src.rpm
  rpmbuild -ba ${SPECS}/${pkg}.spec || {
      echo "Failed to build ${pkg}"
      exit 1
  }
  echo "Installing..."
  rpm -Uvh ${RPMS}/*/${pkg}*.rpm
}

# dependencies
yum install python-coverage python-sphinx libattr-devel python-devel python-paramiko make rpm-build gcc

build python-coverage-test-runner 1.9 1.el6
build python-tracing 0.7 2.el6
build python-cliapp 1.20130313 1.el6
build python-ttystatus 0.22 1.el6
build cmdtest 0.6 1.el6
build python-larch 1.20130316 1.el6
build genbackupdata 1.7 1.el6
build summain 0.18 1.el6
build obnam 1.4 1.el6

rm -Rf ${PREBUILT}
mkdir ${PREBUILT}
pushd prebuilt
cp -a ${RPMS}/*/*.rpm .
popd


