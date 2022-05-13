#!/bin/bash

function kaf_props() {
    echo "Writing kafka properties..."
    cat > /etc/bm/kafka.properties <<EOF
bootstrap.servers=meae-kafka1-paris.dev.bluemind.net:9093
zookeeper.servers=meae-kafka1-paris.dev.bluemind.net:2181
EOF
}

kaf_props
prev_install=`bm-cli setup clone-capabilities --short 2>&1 | grep -v WARN | head -n1`

echo "Will clone ${prev_install}"

service telegraf stop
bmctl stop
echo "Services stopped."

service postgresql start
pushd /tmp
su postgres -c "dropdb bj"
su postgres -c "dropdb bj-data"
echo "Database dropped."
popd

rm -fr /etc/bm/*
kaf_props

rm -f /etc/cyrus-partitions && \
    touch /etc/cyrus-partitions && \
    chown cyrus:mail /etc/cyrus-partitions
/usr/share/bm-cyrus/resetCyrus.sh

rm -fr /var/log/bm-node/*
rm -fr /var/spool/bm-docs/*
service bm-node restart

rm -fr /var/spool/bm-elasticsearch/data/*
rm -fr /var/log/bm-elasticsearch/*

test -d /var/spool/bm-continuous-backup && rm -fr /var/spool/bm-continuous-backup/*

mkdir -p /etc/bm
touch /etc/bm/bm-xmpp.disabled
touch /etc/bm/bm-eas.disabled
rm -f /etc/bm/bm-core.disabled
touch /etc/bm/replication.probe.disabled

rm -fr /var/cache/bm-core/* /var/cache/bm-hps/*

rm -fr /var/log/bm/*
rm -fr /var/log/bm-mapi/*
rm -fr /var/spool/bm-mapi/*
rm -fr /var/spool/bm-hollowed/*

bmctl start

/usr/bin/htpasswd -bc /etc/nginx/sw.htpasswd admin 'admin'
