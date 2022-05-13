#!/bin/bash

clone_mode=PROMOTE
test $# -eq 1 && {
    clone_mode=$1
    echo "Clone mode will be '${clone_mode}'"
}

function kaf_props() {
    echo "Writing kafka properties..."
    cat > /etc/bm/kafka.properties <<EOF
bootstrap.servers=meae-kafka1-paris.dev.bluemind.net:9093
zookeeper.servers=meae-kafka1-paris.dev.bluemind.net:2181
EOF
}

kaf_props
prev_install=`bm-cli setup clone-capabilities --short 2>&1 | grep '^bluemind' | head -n1`

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
service bm-node restart

rm -fr /var/spool/bm-elasticsearch/data/*
rm -fr /var/log/bm-elasticsearch/*

test -d /var/spool/bm-continuous-backup && rm -fr /var/spool/bm-continuous-backup/*

mkdir -p /etc/bm
touch /etc/bm/bm-xmpp.disabled
touch /etc/bm/bm-eas.disabled
touch /etc/bm/replication.probe.disabled

rm -fr /var/cache/bm-core/* /var/cache/bm-hps/*

rm -fr /var/log/bm/*
rm -fr /var/spool/bm-mapi/*
rm -fr /var/spool/bm-hollowed/*

bmctl start

/usr/bin/htpasswd -bc /etc/nginx/sw.htpasswd admin 'admin'

ip_addr=$(hostname -i)
echo "bm-master=${ip_addr}" > ./mapping.props

bm-cli setup clone --external-url meae-proxy-1.dev.bluemind.net --topo-map ./mapping.props --mode ${clone_mode} ${prev_install}

bm-cli certificate file-cert --ca /root/.config/sintls/certificates/$(hostname -f).issuer.crt --cert /root/.config/sintls/certificates/$(hostname -f).crt --key /root/.config/sintls/certificates/$(hostname -f).key

#echo "Hollow repair..."
#bm-cli maintenance repair --ops=hollow.directory devenv.blue 

service bm-mapi restart

# custom logo to known which version we use
curl -k -X PUT -H 'Accept: application/json' -H "X-BM-ApiKey: $(cat /etc/bm/bm-core.tok)" -H "Content-Type: application/octet-stream" --data-binary "@/vagrant/logo_c2.png" 'http://127.0.0.1:8090/api/system/installation/logo'
