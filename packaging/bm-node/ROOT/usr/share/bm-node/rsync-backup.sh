#!/bin/bash

# example grabbed from http://wiki.bash-hackers.org/howto/mutex
# grabbed from : "This code was taken from a script that controls PISG to 
#		  create statistical pages from my IRC logfiles."
 
# lock dirs/files
LOCKDIR="/tmp/rsyncbackup-lock"
PIDFILE="${LOCKDIR}/PID"
 
# exit codes and text for them - additional features nobody needs :-)
ENO_SUCCESS=0; ETXT[0]="ENO_SUCCESS"
ENO_GENERAL=1; ETXT[1]="ENO_GENERAL"
ENO_LOCKFAIL=2; ETXT[2]="ENO_LOCKFAIL"
ENO_RECVSIG=3; ETXT[3]="ENO_RECVSIG"
 
###
### start locking attempt
###
 
trap 'ECODE=$?; echo "[rsyncbackup] Exit: ${ETXT[ECODE]}($ECODE)" >&2' 0
echo -n "[rsyncbackup] Locking: " >&2
 
if mkdir "${LOCKDIR}" &>/dev/null; then
 
    # lock succeeded, install signal handlers before storing the PID just in case 
    # storing the PID fails
    trap 'ECODE=$?;
          echo "[rsyncbackup] Removing lock. Exit: ${ETXT[ECODE]}($ECODE)" >&2
          rm -rf "${LOCKDIR}"' 0
    echo "$$" >"${PIDFILE}" 
    # the following handler will exit the script on receiving these signals
    # the trap on "0" (EXIT) from above will be triggered by this trap's "exit" command!
    trap 'echo "[rsyncbackup] Killed by a signal." >&2
          exit ${ENO_RECVSIG}' 1 2 3 15
    echo "success, installed signal handlers"

    /usr/bin/rsync $@
else
 
    # lock failed, now check if the other PID is alive
    OTHERPID="$(cat "${PIDFILE}")"
 
    # if cat wasn't able to read the file anymore, another instance probably is
    # about to remove the lock -- exit, we're *still* locked
    #  Thanks to Grzegorz Wierzowiecki for pointing this race condition out on
    #  http://wiki.grzegorz.wierzowiecki.pl/code:mutex-in-bash
    if [ $? != 0 ]; then
      echo "lock failed, PID ${OTHERPID} is active" >&2
      exit ${ENO_LOCKFAIL}
    fi
 
    if ! kill -0 $OTHERPID &>/dev/null; then
        # lock is stale, remove it and restart
        echo "removing stale lock of nonexistant PID ${OTHERPID}" >&2
        rm -rf "${LOCKDIR}"
        echo "[rsyncbackup] restarting myself" >&2
        exec "$0" "$@"
    else
        # lock is valid and OTHERPID is active - exit, we're locked!
        echo "lock failed, PID ${OTHERPID} is active" >&2
        exit ${ENO_LOCKFAIL}
    fi
 
fi


