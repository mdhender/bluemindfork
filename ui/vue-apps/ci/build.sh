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
BM_ROOT=$BASEDIR/..

. ${BASEDIR}/java.sh

BM_VERSION=""
PUBLISH_NPM="false"
TRIGGER_SONAR="false"
SONAR_BRANCH_FLAG=""

while [[ $# -gt 0 ]]
do
    key="$1"
    case $key in
        --bm-version)
            BM_VERSION="$2"
            shift
            shift
            ;;
        --trigger-sonar)
            TRIGGER_SONAR="true"
            shift
            ;;
        --publish-npm)
            PUBLISH_NPM="true"
            shift
            ;;
        --branch-name)
            SONAR_BRANCH_FLAG="-Dsonar.branch.name=$2"
            shift
            shift
            ;;
        *)
            BM_VERSION="$1"
            shift
            ;;
    esac
done

pushd $BM_ROOT

if [ "$BM_VERSION" == "" ]; then
    # Test-only build if no version provided
    mvn clean install
else
    mvn -Dbm-runtime.url=https://forge.bluemind.net/staging/p2/bluemind/$BM_VERSION/ clean tycho-versions:set-version -DnewVersion=$BM_VERSION
    mvn -Dbm-runtime.url=https://forge.bluemind.net/staging/p2/bluemind/$BM_VERSION/ clean install
fi

yarn --version
node --version
yarn install
rm -f jest.json jest.xml
yarn test-ci || true
mv report.xml jest.xml

if [ "$PUBLISH_NPM" == "true" ]; then
    for path in $(node -pe 'Object.values(JSON.parse((JSON.parse(process.argv[1]).data))).forEach(({location}) => console.log(location))' "$(yarn --json workspaces info)"); do
        if [[ $path != "undefined" ]]; then
            pushd $path
            echo "Publishing $path"
            yarn publish --no-git-tag-version --no-commit-hooks --new-version $BM_VERSION
            popd
        fi
    done
fi

if [ "$TRIGGER_SONAR" == "true" ]; then
    yarn add sonarqube-scanner --dev -W
    ./node_modules/sonarqube-scanner/dist/bin/sonar-scanner -Dsonar.host.url=http://sonar.blue-mind.loc:9000/sonar/ -Dsonar.projectKey=vue-apps -Dsonar.login=1f83913160353db8f1dab30c05326c79ef5d8428 $SONAR_BRANCH_FLAG -Dsonar.exclusions="**/node/**,**/node_modules/**"
fi


popd
