#!/bin/sh

cat /proc/cpuinfo | grep "siblings" | head -1 | awk '{print $3}'

