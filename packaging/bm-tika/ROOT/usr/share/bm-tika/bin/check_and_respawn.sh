#!/bin/bash

pidFile=/var/run/bm-tika.pid

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
	    service bm-tika stop
	    mv /var/log/java_pid${tpid}.hprof /var/log/tika_death${tpid}.hprof
	    bzip2 -9 /var/log/tika_death${tpid}.hprof
	    service bm-tika start
	    exit_code=2
	fi
    fi
fi

if [ -f /var/run/bm-tika.oomkill ]; then
    rm -f /var/run/bm-tika.oomkill
    [ ${exit_code} -eq 0 ] && exit_code=1
    
    if [ ${exit_code} -eq 1 ]; then
	[ -e ${pidFile} ] && rm -f ${pidFile}
        service bm-tika start
        exit_code=1
    fi
fi

exit $exit_code
