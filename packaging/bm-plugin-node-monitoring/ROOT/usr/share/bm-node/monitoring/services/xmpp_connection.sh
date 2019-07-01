#!/bin/sh
response=$(echo "quit" | telnet localhost 5222 2>/dev/null)
$(echo $response | grep -q "Connected to")
error=$?
if [[ "$error" > 0 ]]
then
	echo " * [ERROR] Could not connect to ${APP_NAME}"
	exit 1
fi
exit 0
