#!/bin/bash

exit_code=0

# no pid file, try to let the init system do the job
# or allow the user to stop the service
if [ -f /var/run/bm-lmtpd.pid ]; then
    tpid=`cat /var/run/bm-lmtpd.pid`
    kill -0 $tpid >/dev/null 2>&1
    status=$?
    if [ $status -eq 0 ]; then
	# it is running, but did it hprof ?
	if [ -f /var/log/java_pid${tpid}.hprof ]; then
	    service bm-lmtpd stop
	    mv /var/log/java_pid${tpid}.hprof /var/log/lmtpd_death${tpid}.hprof
	    bzip2 -9 /var/log/lmtpd_death${tpid}.hprof
	    service bm-lmtpd start
	    exit_code=2
	fi
    else
	# it is not running
	service bm-lmtpd start
	exit_code=1
    fi
fi

exit $exit_code
