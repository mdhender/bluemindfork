#!/bin/bash

echo "$(date) - Killing '${tikapid}'...." >> /var/log/bm-tika/tika.oom.log
# Kill is done by the JVM -XX:CrashOnOutOfMemory
