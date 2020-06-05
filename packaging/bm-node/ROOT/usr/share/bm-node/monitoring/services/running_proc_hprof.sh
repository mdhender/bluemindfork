#!/bin/sh

file="java_pid$1.hprof"
find /var/log/ -name "$file"
