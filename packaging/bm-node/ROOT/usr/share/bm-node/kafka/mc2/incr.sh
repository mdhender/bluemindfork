#!/bin/bash

prev_install=`bm-cli setup clone-capabilities --short 2>&1 | grep -v WARN | head -n1`

echo "Will clone ${prev_install}"

bm-cli setup clone --external-url meae-core-2.dev.bluemind.net --topo-map /root/mapping.props ${prev_install}
