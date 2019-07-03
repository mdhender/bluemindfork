#!/bin/sh

cat /proc/meminfo | grep ^$1 | awk '{ print $2 }'

