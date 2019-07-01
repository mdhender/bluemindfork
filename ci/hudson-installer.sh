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

pushd ${WORKSPACE}/open

BOCLIENT=/usr/share/bo-client/bo-client
distribution=`echo ${JOB_NAME} | cut -d '-' -f 4`

if [ -z "${distribution}" ]; then
    exit 1
fi

artifacts=${WORKSPACE}"/open/build_artifacts"
installerBuildPath=${WORKSPACE}"/open/installer-build"
installerGitPath=${WORKSPACE}"/open/scripts/installer"
source ${installerGitPath}"/include/path"
installerPkgsPath=${installerBuildPath}"/"${ARCHIVE_PKGS_PATH}

source ./ci/release

installerLabel="BlueMind Groupware installer"
installerBinScript="bluemind-installer-"${release}"."${BUILD_NUMBER}"-"${distribution}".bin"
installerBinScriptGenName="bluemind-installer-"${distribution}".bin"
installerBin=${artifacts}"/"${installerBinScript}
installerPath="installer"
installerScript=${installerPath}"/bluemind-install.sh"

rm -fr ${artifacts}
mkdir -p ${artifacts}

removeInstallerPath() {
    echo "Cleaning..."
    pushd ${WORKSPACE}/open
    rm -rf ${installerBuildPath}
    popd
}

createInstallerPath() {
    pushd ${WORKSPACE}/open
    removeInstallerPath
    mkdir -p ${installerBuildPath}
    cp -a ${installerGitPath} ${installerBuildPath}
    sed -i -e "s/^BLUEMIND_INSTALLER=.\+$/BLUEMIND_INSTALLER=\"${installerBinScript}\"/" ${installerBuildPath}"/"${installerScript}

    touch ${installerBuildPath}"/"${installerPath}"/"${distribution}

    source ${installerGitPath}"/include/path"
    mkdir -p ${installerPkgsPath}

    popd
}

getPkgList() {
    pkgListFile=${WORKSPACE}"/open/pkgListFile"
    rm -f ${pkgListFile} || true
    echo "Getting open artifact list..."
    ${BOCLIENT} getopenartifacts ${software} ${releaseName} ${BUILD_NUMBER} | tail -n+3 > ${pkgListFile}

    if [ ! -e ${pkgListFile} ]; then
        exit 1
    fi

    echo "bo-client result:"
    cat ${pkgListFile}

    source ${pkgListFile}
    rm -f ${pkgListFile} || true
}

downloadPkgs() {
    pushd ${installerPkgsPath}

    nbPkg=0
    for i in ${!distribution}; do
        let nbPkg=nbPkg+1
        curl -O $i
        if [ $? -ne 0 ]; then
            echo "Fail to download artifact: "$i
            exit $?
        fi
    done

    if [ ${nbPkg} -eq 0 ]; then
        exit 1
    else
        echo ${nbPkg}" artifacts downloaded"
    fi

    rm -f *zip

    buildPkgsIndex
    popd
}

buildPkgsIndex() {
    ls *deb > /dev/null 2>&1
    if [ $? -eq 0 ]; then
        dpkg-scanpackages . /dev/null | gzip -9c > Packages.gz
    else
        createrepo .
    fi
}

buildInstaller() {
    pushd ${installerBuildPath}
    MKSELF="makeself"
    if [ -e /usr/bin/makeself.sh ]; then
        # RH
        MKSELF="makeself.sh"
    fi

    ${MKSELF} . "${installerBin}" "${installerLabel}" "${installerScript}"
    popd
}

createGenericName() {
    pushd ${artifacts}
    ln -s ${installerBinScript} ${installerBinScriptGenName}
    popd
}

ending() {
    removeInstallerPath
}
trap ending EXIT

createInstallerPath
getPkgList
downloadPkgs
buildInstaller
createGenericName

exit 0
