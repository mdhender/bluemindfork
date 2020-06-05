#!/bin/sh

grep -Po "listen=\"1143.*maxchild=\K.*? " /etc/cyrus.conf
