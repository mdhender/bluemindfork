#!/bin/bash

cat > ./mapping.props <<EOF
bm-master=$(hostname -i)
EOF
echo "Promoting to master active BM instance..."
cat ./mapping.props
rm -f /etc/bm/continuous.clone
bm-cli setup clone-promote --topo-map ./mapping.props bluemind-$(cat /etc/bm/mcast.id)
