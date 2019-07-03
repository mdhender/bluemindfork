#!/bin/bash

for i in `find . -name "*.json" -type f`; do
    cat $i | python -mjson.tool 2>&1 >/dev/null
    validation=$?
    if [ ${validation} -eq 0 ]; then
	echo "validation of $i is ok: $validation"
    else
	echo "$i is not valid JSON"
	exit 1
    fi
done


exit 0
