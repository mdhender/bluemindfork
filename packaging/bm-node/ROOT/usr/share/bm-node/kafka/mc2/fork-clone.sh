#!/bin/bash

function kaf_props() {
    echo "Writing kafka properties..."
    cat > /etc/bm/kafka.properties <<EOF
bootstrap.servers=meae-kafka1-paris.dev.bluemind.net:9093
zookeeper.servers=meae-kafka1-paris.dev.bluemind.net:2181
EOF
}

kaf_props
prev_install=`bm-cli setup clone-capabilities --short 2>&1 | grep "^bluemind" | head -n1`

echo "Will clone ${prev_install}"

ip_addr=$(hostname -i)
echo "bm-master=${ip_addr}" > ./mapping.props

exec bm-cli setup clone --external-url meae-proxy-1.dev.bluemind.net --topo-map ./mapping.props --mode FORK ${prev_install}

