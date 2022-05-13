#!/bin/bash

inst_id=bluemind-afc6e704-5e0f-4db4-9c3c-cf35e5209281

cat > /etc/bm/kafka.properties <<EOF
bootstrap.servers=192.168.231.88:9093
zookeeper.servers=192.168.231.88:2181
EOF

cat > /root/topo.mailreplica.props <<EOF
bm=$(hostname -i)
EOF


echo "installation will be ${inst_id}"

rm -f /root/clone-output.log

time bm-cli setup clone \
     --external-url $(hostname -f) --topo-map /root/topo.mailreplica.props \
     --workers=6 \
     --mode TAIL ${inst_id} 2>&1 | tee /root/clone-output.log
