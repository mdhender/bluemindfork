#!/bin/bash

JAVA_APP="net.bluemind.node.server.nodelauncher"
APP_NAME="BlueMind Node Server"
WORKSPACE=`dirname $0`

source ${WORKSPACE}"/check.lib"

check_hprof
check_networkport "" 8022

exit_ok
