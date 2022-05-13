#!/bin/bash
kaf=meae-kafka1-paris.dev.bluemind.net
echo "Reset kafka running on ${kaf}"
ssh ${kaf} /vagrant/reset_kafka.sh
