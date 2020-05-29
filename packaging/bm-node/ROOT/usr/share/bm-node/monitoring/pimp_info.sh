#!/bin/bash

BASENAME=$1

if [ -e "/etc/bm/local/$BASENAME" ]; then
	echo "$BASENAME exists";
	cat "/etc/bm/local/$BASENAME" | grep "^MEM" | grep -Eo "[0-9]+"
	
else
	echo "$BASENAME does not exists, fetching info from /etc/bm/default/$BASENAME";
	cat "/etc/bm/default/$BASENAME" | grep "^MEM" | grep -Eo "[0-9]+"
fi
