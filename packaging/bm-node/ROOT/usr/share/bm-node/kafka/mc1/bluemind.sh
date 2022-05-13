#!/bin/bash

locale-gen en_US.UTF-8
update-locale LANG=en_US.UTF-8

mkdir -p /etc/bm

touch /etc/bm/no.workdir.purge
touch /etc/bm/mapi.decode.response
touch /etc/bm/bm-xmpp.disabled
touch /root/no.core.jobs
touch /root/core.debug


export DEBIAN_FRONTEND=noninteractive

echo "*** Add BM repository key for APT."
curl https://pkg.bluemind.net/bluemind-deb.asc | apt-key add -

cp /vagrant/bm.list /etc/apt/sources.list.d/bm.list

apt-get -qy update
apt-get -qy install aptitude

cat > /etc/apt/preferences.d/bm <<EOF
Package: *
Pin: origin ""
Pin-Priority: 990

Package: *
Pin: origin "pkg.blue-mind.net"
Pin-Priority: 997

Package: *
Pin: origin "bm.blue-mind.net"
Pin-Priority: 998

Package: *
Pin: origin "forge.bluemind.net"
Pin-Priority: 999
EOF

# ensure sane htop settings
pushd /root
mkdir -p .config/htop
cp /vagrant/htoprc .config/htop/
popd

aptitude -q -y --allow-untrusted update
aptitude -q -y --allow-untrusted clean

aptitude -q -y --allow-untrusted install bm-setup-wizard sudo bm-full wget \
	 bm-plugin-core-subscription \
	 mailutils locate \
	 bm-plugin-admin-console-monitoring bm-plugin-core-monitoring \
	 bm-plugin-node-monitoring bm-tick-full bm-metrics-agent \
	 bm-cli bm-plugin-cli-setup \
	 bm-plugin-core-mapi-support \
	 bm-plugin-admin-console-filehosting-settings bm-plugin-webserver-filehosting bm-plugin-core-filehosting-filesystem bm-chooser
aptitude -q -y --allow-untrusted clean

#echo "Backup mountpoint..."
#mkdir -p /var/backups/bluemind
#zfs create -o mountpoint=/var/backups/bluemind rpool/backup
#truncate -s 20G /backup.volume
#loop_dev=$(losetup -f --show /backup.volume)
#mkfs.ext4 ${loop_dev}
#mkdir -p /var/backups/bluemind
#mount ${loop_dev} /var/backups/bluemind

cat > /etc/bm/kafka.properties <<EOF
bootstrap.servers=meae-kafka1-paris.dev.bluemind.net:9093
zookeeper.servers=meae-kafka1-paris.dev.bluemind.net:2181
EOF

echo "Running SW from CLI..."
bm-cli setup install --domain devenv.blue --external-url focal.devenv.blue --set-contact admin@devenv.blue --admin0-pass admin --sw-pass admin --domain-admin-pass admin

echo "Switching to a valid certificate..."
#bm-cli setup cert --ca /vagrant/devenv.blue/chain.pem --cert /vagrant/devenv.blue/cert.pem --key /vagrant/devenv.blue/privkey.pem
bm-cli setup cert --ca /root/.config/sintls/certificates/$(hostname -f).issuer.crt --cert /root/.config/sintls/certificates/$(hostname -f).crt --key /root/.config/sintls/certificates/$(hostname -f).key

echo "Enable filehosting..."
bm-cli filehosting activate --domain devenv.blue --group user

echo "Switch to S3 storage"
bm-cli sysconf mset --format=json /vagrant/s3.json

updatedb

# install a fresh kernel
pushd /root
wget https://raw.githubusercontent.com/pimlie/ubuntu-mainline-kernel.sh/master/ubuntu-mainline-kernel.sh
chmod +x ubuntu-mainline-kernel.sh
./ubuntu-mainline-kernel.sh --yes -i
popd

touch /etc/bm/bm-xmpp.disabled


echo "Provision users..."
bm-cli /vagrant/users.cli

