#!/bin/bash

curdir=`dirname $0`

pushd ${curdir} >/dev/null 2>&1
for check in ch*.sh; do
    echo "On ${check}..."
    ./${check}
    status=$?
    if [ $status -gt 0 ]; then
	echo "${check} FAILED."
    else
	echo "${check} is OK."
    fi
done
popd >/dev/null 2>&1
