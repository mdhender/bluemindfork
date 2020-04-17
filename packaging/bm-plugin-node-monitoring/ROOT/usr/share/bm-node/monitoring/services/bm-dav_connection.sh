#!/bin/bash

curdir=`dirname $0`
token=$(cat /etc/bm/bm-core.tok)
response=$(curl --connect-timeout 30 --max-time 120 --silent -XPROPFIND -H 'Content-Type: application/xml' --data-binary @$curdir/dav_body.xml http://admin0%40global.virt:${token}@localhost:8080/dav/)

user=$(echo $response | sed -n 's/.*<d:current-user-principal>\(.*\)<\/d:current-user-principal>.*/\1/p')

if [[ -z $user ]]
	then
		echo " * [ERROR] Incorrect answer from DAV"
		exit 1
fi

exit 0
