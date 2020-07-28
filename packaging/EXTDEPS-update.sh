#!/bin/bash

sed=/bin/sed
# OSX: sudo port install gsed
if [ -x /opt/local/bin/gsed ]; then
    sed=/opt/local/bin/gsed
fi

function getPackagesPath() {
    for file in $(find . -type f -name control); do
        parent=$(basename $(dirname ${file}))

        if [ ${parent} != "debian" ]; then
            continue
        fi

        pparent=$(dirname $(dirname ${file}))
        if [ ! -e ${pparent}"/redhat" ]; then
            continue
        fi

        uniqPath=${uniqPath}"\n"${pparent}
    done

    for file in $(find . -type f -name pom.xml -exec sh -c 'grep -q -m 1 'bm-package' {} && echo {}' \;); do
        uniqPath=${uniqPath}"\n"$(dirname ${file})
    done

    PACKAGES_PATH=$(echo -e ${uniqPath} | sort | uniq)
}

function updateControlFile() {
    fileName=$1
    depName=$2
    depVersion=$3

    ${sed} -i -e "s/${depName} (= [^)]\+)/${depName} (= ${depVersion})/" ${fileName}
}

function updateSpecFile() {
    fileName=$1
    depName=$2
    depVersion=$3

    ${sed} -i -e "s/${depName} = [^,]\+/${depName} = ${depVersion}/" ${fileName}
}

function updatePomFile() {
    fileName=$1
    depName=$2
    depVersion=$3

    ${sed} -i -e "s/${depName}=[^ <]\+/${depName}=${depVersion}/" ${fileName}
}

function updateDependencies() {
    echo "Update packages dependencies from "${PWD}

    file="pom.xml"
    if [ -e ${file} ]; then
        updatePomFile ${file} bm-nginx ${BMNGINX}
        updatePomFile ${file} bm-telegraf ${TELEGRAF}
        updatePomFile ${file} bm-chronograf ${CHRONOGRAF}
        updatePomFile ${file} bm-kapacitor ${KAPACITOR}
        updatePomFile ${file} bm-influxdb ${INFLUXDB}
        updatePomFile ${file} bm-kafka ${KAFKA}
        updatePomFile ${file} bm-sds-storage-s3-minio ${MINIO}
        updatePomFile ${file} bm-cyrus-imapd ${BMCYRUS}
        updatePomFile ${file} bm-postgresql ${BMPOSTGRESQL}
        updatePomFile ${file} bm-jdk ${BMJDK}
        updatePomFile ${file} bm-elasticsearch 1:${BMES}
        updatePomFile ${file} bm-php ${BMPHP}
    fi

    file="debian/control"
    if [ -e ${file} ]; then
        updateControlFile ${file} bm-nginx ${BMNGINX}
        updateControlFile ${file} bm-telegraf ${TELEGRAF}
        updateControlFile ${file} bm-chronograf ${CHRONOGRAF}
        updateControlFile ${file} bm-kapacitor ${KAPACITOR}
        updateControlFile ${file} bm-influxdb ${INFLUXDB}
        updateControlFile ${file} bm-kafka ${KAFKA}
        updateControlFile ${file} bm-sds-storage-s3-minio ${MINIO}
        updateControlFile ${file} bm-cyrus-imapd ${BMCYRUS}
        updateControlFile ${file} bm-postgresql ${BMPOSTGRESQL}
        updateControlFile ${file} bm-jdk ${BMJDK}
        updateControlFile ${file} bm-elasticsearch 1:${BMES}
        updateControlFile ${file} bm-php ${BMPHP}
    fi

    for file in redhat/*.spec; do
        if [ ! -e ${file} ]; then
            continue
        fi

        updateSpecFile ${file} bm-nginx ${BMNGINX}
        updateSpecFile ${file} bm-telegraf ${TELEGRAF}
        updateSpecFile ${file} bm-chronograf ${CHRONOGRAF}
        updateSpecFile ${file} bm-kapacitor ${KAPACITOR}
        updateSpecFile ${file} bm-influxdb ${INFLUXDB}
        updateSpecFile ${file} bm-kafka ${KAFKA}
        updateSpecFile ${file} bm-sds-storage-s3-minio ${MINIO}
        updateSpecFile ${file} bm-cyrus-imapd 1:${BMCYRUS}
        updateSpecFile ${file} bm-postgresql ${BMPOSTGRESQL}
        updateSpecFile ${file} bm-jdk ${BMJDK}
        updateSpecFile ${file} bm-elasticsearch 1:${BMES}
        updateSpecFile ${file} bm-php ${BMPHP}
    done
}

WORKSPACE=$(dirname $0)
pushd ${WORKSPACE} > /dev/null 2>&1

. ./EXTDEPS

pushd ${WORKSPACE}"/../.." > /dev/null 2>&1

getPackagesPath
for path in ${PACKAGES_PATH}; do
    pushd ${path} > /dev/null 2>&1
    updateDependencies
    popd > /dev/null 2>&1
done
