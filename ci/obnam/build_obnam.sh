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

set -e

function build() {
    apt-get build-dep $1 || echo "Failed to get dep for $1"
    apt-get source $1
    if [ -f ../$1.rules ]; then
	echo "rules patch exists"
	cp ../$1.rules $1-$2/debian/rules
    fi
    if [ -f ../$1.control ]; then
	echo "control patch exists"
	cp ../$1.control $1-$2/debian/control
    fi
    if [ -f ../$1.sh ]; then
	echo "shell patch exists"
	/bin/bash ../$1.sh $1 $2
    fi
    apt-get source -b $1
    dpkg -i $1_*.deb
}

rm -fr obnam
mkdir obnam

pushd obnam
aptitude install python-sphinx attr libattr1-dev

build python-coverage-test-runner 1.9
build python-tracing 0.7
build python-cliapp 1.20130313
build python-ttystatus 0.22
build cmdtest 0.6
build python-larch 1.20130316
build genbackupdata 1.7
build summain 0.18
build obnam 1.4

popd
