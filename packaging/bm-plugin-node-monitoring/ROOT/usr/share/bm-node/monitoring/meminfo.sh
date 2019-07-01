#!/bin/bash

cat /proc/meminfo | grep MemTotal | awk '{val = $2 / 1024; print val "MB"}'

