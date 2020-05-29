#!/bin/sh

ps -o pid,pcpu,pmem,start -p $1 | sed '1d'
