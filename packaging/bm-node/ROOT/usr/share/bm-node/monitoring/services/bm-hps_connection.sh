#!/bin/sh
ip=$(curl --silent localhost:8079/location/host/mail/imap/admin0@global.virt)

if [[ -z "$ip" ]]
then
	echo " * [ERROR] No response from hps"
	exit 1
fi
exit 0
