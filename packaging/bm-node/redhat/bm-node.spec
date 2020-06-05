Summary:            BlueMind Node Daemon
Name:               bm-node
Version:            %{_bmrelease}
Release:            0
License:            GNU Affero General Public License v3
Group:              Applications/messaging
URL:                http://www.bluemind.net/
ExcludeArch:        s390 s390x
Requires(post):     systemd systemd-sysv
Requires:           bm-jdk = 8u252-bluemind34, tar, gzip, bzip2, rsync, bm-conf = %{version}-%{release}, bm-pimp = %{version}-%{release}, iptables, sudo, httpd-tools, bm-maintenance-tools = %{version}-%{release}, bm-cli = %{version}-%{release}
Requires(post):     /bin/bash, initscripts
Conflicts:          bm-mq, bm-plugin-node-monitoring
Obsoletes:          bm-mq, bm-plugin-node-monitoring

%description
BlueMind Node handles all the remote tasks for BlueMind Core

%install
cp -a %{_rootdir}/* %{buildroot}

mkdir -p %{buildroot}%{_unitdir}
install -m 644 /sources/stretch/bm-node.service %{buildroot}%{_unitdir}

mkdir -p %{buildroot}/var/lib/bm-ca

%files
%attr(0755, root, root) /usr/share/bm-node/bm-pra-restore.py
%exclude %dir /usr
%exclude %dir /usr/lib
%exclude %dir /usr/lib/systemd
%exclude %dir %{_unitdir}
/*

%pre
if [ $1 -gt 1 ]; then
    # Upgrade
    systemctl stop bm-node
fi

%post -p /bin/bash
systemctl daemon-reload
systemctl enable bm-node

if [ $1 -eq 1 ]; then
    # Installation
    systemctl start bm-node
fi

%preun
if [ $1 -eq 0 ]; then
    # Uninstall
    systemctl stop bm-node
fi

%postun
if [ $1 -eq 1 ]; then
    # Upgrade
    systemctl start bm-node
fi
