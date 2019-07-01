#!/bin/bash

host=$1

ssh root@$host mkdir -p /usr/share/grafana
ssh root@$host rm -fr /usr/share/grafana/*
pushd upstream
scp -r * root@$host:/usr/share/grafana/
popd
scp config.toml root@$host:/opt/influxdb/shared/
ssh root@$host /etc/init.d/influxdb restart
scp config.js root@$host:/usr/share/grafana/
scp bluemind.json root@$host:/usr/share/grafana/app/dashboards/
