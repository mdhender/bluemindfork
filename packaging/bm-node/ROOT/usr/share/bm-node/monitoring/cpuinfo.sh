#!/bin/bash

cat /proc/cpuinfo | grep 'model name' | uniq | cut -f 1 -d ':' --complement
