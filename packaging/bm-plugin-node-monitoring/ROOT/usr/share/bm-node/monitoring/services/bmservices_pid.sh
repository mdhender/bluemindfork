#!/bin/sh

awk '{ printf("%s\n", $1); }' $1
