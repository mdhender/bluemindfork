#!/bin/sh

cat /proc/loadavg | awk '{ printf("%s\t%s\n",$2,$3) }'
