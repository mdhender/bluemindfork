#!/bin/bash

bm-cli sds s3cmd

service telegraf stop
bmctl stop
echo "Services stopped."

echo "empty the s3 bucket..."
s3cmd rm s3://meae/* >/dev/null

service postgresql start
pushd /tmp
su postgres -c "dropdb bj"
su postgres -c "dropdb bj-data"
echo "Database dropped."
popd

rm -fr /etc/bm/*

rm -f /etc/cyrus-partitions && \
    touch /etc/cyrus-partitions && \
    chown cyrus:mail /etc/cyrus-partitions
/usr/share/bm-cyrus/resetCyrus.sh

service bm-node restart

rm -fr /var/spool/bm-elasticsearch/data/*
rm -fr /var/log/bm-elasticsearch/*

test -d /var/spool/bm-continuous-backup && rm -fr /var/spool/bm-continuous-backup/*

mkdir -p /etc/bm
touch /etc/bm/bm-xmpp.disabled
touch /etc/bm/bm-eas.disabled
touch /etc/bm/replication.probe.disabled

echo "Writing kafka properties..."
echo "bootstrap.servers=meae-kafka1-paris.dev.bluemind.net:9093" > /etc/bm/kafka.properties
echo "zookeeper.servers=meae-kafka1-paris.dev.bluemind.net:2181" >> /etc/bm/kafka.properties

rm -fr /var/cache/bm-core/* /var/cache/bm-hps/*
rm -fr /var/log/bm/* /var/log/bm-hps/* /var/log/bm-sds-proxy/* /var/log/bm-kafka-broker/*

echo "Resetting kafka & zk..."
/vagrant/reset_kafka.sh

rm -fr /var/spool/bm-hollowed/* /var/spool/bm-mapi/*

bmctl start
/usr/bin/htpasswd -bc /etc/nginx/sw.htpasswd admin 'admin'

echo "Resetting kafka & zk..."
/vagrant/reset_kafka.sh

bm-cli setup install --domain devenv.blue --external-url meae-proxy-1.dev.bluemind.net --set-contact admin@devenv.blue --admin0-pass admin --sw-pass admin --domain-admin-pass admin

#bm-cli setup cert --ca /vagrant/devenv.blue/chain.pem --cert /vagrant/devenv.blue/cert.pem --key /vagrant/devenv.blue/privkey.pem
bm-cli certificate file-cert --ca /root/.config/sintls/certificates/$(hostname -f).issuer.crt --cert /root/.config/sintls/certificates/$(hostname -f).crt --key /root/.config/sintls/certificates/$(hostname -f).key

bm-cli filehosting activate --domain devenv.blue --group user

bm-cli sysconf mset --format=json /vagrant/s3.json

bm-cli /vagrant/users.cli

echo "Switch logo"
curl -k -X PUT -H 'Accept: application/json' -H "X-BM-ApiKey: $(cat /etc/bm/bm-core.tok)" -H "Content-Type: application/octet-stream" --data-binary "@/vagrant/logo_c1.png" 'http://127.0.0.1:8090/api/system/installation/logo'

echo "restart CRP..."
ssh meae-proxy-1.dev.bluemind.net service bm-crp restart
echo "done."

echo "Restart mapi"
service bm-mapi restart

echo "Inject messages & folders"
bm-cli inject imap --msg=400 --prod got128 --folders=6 --workers=4 devenv.blue

echo "Enable mail-app for everyone"
bm-cli setup enable-mailapp --group=user devenv.blue

echo "Setup tom's mapi profile"
bm-cli mapi inbox --latd=tom@devenv.blue

