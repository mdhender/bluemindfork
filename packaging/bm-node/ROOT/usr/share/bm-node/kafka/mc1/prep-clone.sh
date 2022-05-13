#!/bin/bash

iid=bluemind-$(cat /etc/bm/mcast.id)
#iid='bluemind-7466a729-f3fd-4c3d-b9a4-5c5a693301e2'

rm -f ${iid}-*.json

echo "Dumping kafka content..."
bm-cli setup clone-dump ${iid}
rm -f clone-dump.tar*
tar cf clone-dump.tar ${iid}*json
bzip2 -9 clone-dump.tar

emldir=emls-${iid}
rm -fr ${emldir}
mkdir -p ${emldir}

echo "Ensure s3cmd is configured..."
bm-cli sds s3cmd

rm -f emls.tar*

pushd ${emldir}
s3cmd sync s3://meae .
tar cf ../emls.tar *
popd

bzip2 -9 emls.tar
