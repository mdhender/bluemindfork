#!/bin/bash

test $# -eq 1 || {
    echo "usage: $0 version_minor"
    exit 1
}

. version_major
version=${version}"."$1

pushd src
rm -f ../bm-connector-tb-*.xpi
sed -i "s/\(<em:version>\).\+\(<.\+\)/\1${version}\2/" install.rdf
sed -i "s/\(dev\)/${versionName}/" install.rdf
zip -r ../bm-connector-tb-$version.xpi * -x "*~" "*git*"
popd
