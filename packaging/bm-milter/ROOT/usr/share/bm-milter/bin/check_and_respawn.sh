#!/bin/bash

pidFile=/var/run/bm-milter.pid

exit_code=0

# no pid file, try to let the init system do the job
# or allow the user to stop the service
if [ -f ${pidFile} ]; then
    tpid=`cat ${pidFile}`
    kill -0 $tpid >/dev/null 2>&1
    status=$?
    if [ $status -eq 0 ]; then
	# it is running, but did it hprof ?
	if [ -f /var/log/java_pid${tpid}.hprof ]; then
	    service bm-milter stop
	    mv /var/log/java_pid${tpid}.hprof /var/log/milter_death${tpid}.hprof
	    bzip2 -9 /var/log/milter_death${tpid}.hprof

	    [ -e ${pidFile} ] && rm -f ${pidFile}
	    service bm-milter start
	    exit_code=2
	fi
    else
	# it is not running
	[ -e ${pidFile} ] && rm -f ${pidFile}
	service bm-milter start
	exit_code=1
    fi
fi

exit $exit_code
