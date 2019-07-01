#!/bin/bash

JAVA_APP="org.elasticsearch.bootstrap.Elasticsearch"
APP_NAME="BlueMind ElasticSearch Server"
WORKSPACE=`dirname $0`

source ${WORKSPACE}"/check.lib"

function check_es {
    result=$(curl --connect-timeout 30 --max-time 120 -XGET --silent 'http://localhost:9200/_cluster/health')
    status=$(echo $result | sed -e 's/^.*"status":"\([^"]*\)".*$/\1/')

    if [[ -z $status ]] || [ "$status" = "red" ]
        then
            echo "[ERROR] Alert status for ${APP_NAME}"
            exit 2
    fi
}

check_hprof
check_networkport "" 9200 9300
check_es

exit_ok
