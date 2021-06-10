#!/bin/bash
#BEGIN LICENSE
#
# Copyright © Blue Mind SAS, 2012-2018
#
# This file is part of BlueMind. BlueMind is a messaging and collaborative
# solution.
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of either the GNU Affero General Public License as
# published by the Free Software Foundation (version 3 of the License).
#
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
#
# See LICENSE.txt


if [ $EUID -ne 0 ]; then
    echo "Error: this script must be run as root !"
    exit 1
fi

CYRUS_LIB="/var/lib/cyrus"
CYRUS_SPOOL="/var/spool/cyrus"
CYRUS_SIEVE="/var/spool/sieve"

cvt_cyrusdb=/usr/sbin/cvt_cyrusdb

# Stopping Cyrus service
echo -n "Stopping Cyrus service: "
service bm-cyrus-imapd stop
echo "done."

killall -9 idled
killall -9 /usr/lib/cyrus/master
killall -9 imapd
killall -9 pop3d
killall -9 timsieved
killall -9 notifyd

# Purge partitions
echo -n "Purge Cyrus partitions: "
pushd ${CYRUS_SPOOL} > /dev/null 2>&1
for i in `find -maxdepth 1 -type d | grep -vE "mail|news|^\.$|^\./\."`; do
    rm -rf ${i}
done
popd > /dev/null 2>&1
echo "done."

# Purge domains tree
echo -n "Purge Cyrus lib domains tree: "
rm -rf ${CYRUS_LIB}/domain/*
echo "done"

# Purge users tree
echo -n "Purge Cyrus lib users tree: "
find ${CYRUS_LIB}/user -type f -exec rm -f {} \;
echo "done"

# Purge quota
echo -n "Purge Cyrus quota: "
find ${CYRUS_LIB}/quota -type f -exec rm -f {} \;
echo "done"

# Purge lock tree
echo -n "Purge Cyrus locks tree: "
rm -rf ${CYRUS_LIB}/lock/domain/*
find ${CYRUS_LIB}/lock -type f -exec rm {} \;
echo "done"

# this one is regenerated on start
rm -f ${CYRUS_LIB}/deliver.db

# Generate tls_sessions
echo -n "Generate new tls_sessions.db: "
rm -f ${CYRUS_LIB}/tls_sessions.db
touch ${CYRUS_LIB}/tls_sessions
chown cyrus:mail ${CYRUS_LIB}/tls_sessions
$cvt_cyrusdb ${CYRUS_LIB}/tls_sessions flat ${CYRUS_LIB}/tls_sessions.db twoskip
rm -f ${CYRUS_LIB}/tls_sessions
chown cyrus:mail ${CYRUS_LIB}/tls_sessions.db
echo "done"

# Generate new mailboxes.db
echo -n "Generate new Cyrus mailboxes.db: "
rm -f ${CYRUS_LIB}/mailboxes.db
touch ${CYRUS_LIB}/mailboxes
chown cyrus:mail ${CYRUS_LIB}/mailboxes
$cvt_cyrusdb ${CYRUS_LIB}/mailboxes flat ${CYRUS_LIB}/mailboxes.db twoskip
rm -f ${CYRUS_LIB}/mailboxes
chown cyrus:mail ${CYRUS_LIB}/mailboxes.db
echo "done"

# Re-create sieve tree
echo -n "Re-create empty sieve tree: "
pushd $CYRUS_SIEVE > /dev/null 2>&1
rm -fr [a-z] domain global

for i in `echo {a..z}`; do
    mkdir -p $i
    chown -R cyrus:mail $i
done

mkdir domain
for i in `echo {a..z}`; do
    mkdir -p domain/$i
done
chown -R cyrus:mail domain

mkdir global
chown -R cyrus:mail global
popd > /dev/null 2>&1
chown -R cyrus:mail $CYRUS_SIEVE
echo "done"

echo -n "Reset replication log: "
rm -rf ${CYRUS_LIB}/sync
echo "done"
