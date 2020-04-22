Summary:            BlueMind Forest node
Name:               bm-forest-node
Version:            %{_bmrelease}
Release:            0
License:            GNU Affero General Public License v3
Group:              Applications/messaging
URL:                http://www.bluemind.net/
ExcludeArch:        s390 s390x
Requires(post):     systemd systemd-sysv
Requires:           bm-jdk = 8u252-bluemind34, bm-kafka = 2.4.0-bluemind9
Requires(post):     /bin/bash, initscripts

%description
BlueMind Forest node (federate multiple)

%define __jar_repack 0

%install
cp -a %{_rootdir}/* %{buildroot}

mkdir -p %{buildroot}%{_unitdir}
install -m 644 /sources/stretch/bm-forest-node.service %{buildroot}%{_unitdir}

%files
%exclude %dir /usr
%exclude %dir /usr/lib
%exclude %dir /usr/lib/systemd
%exclude %dir %{_unitdir}
/*

%pre
if [ $1 -gt 1 ]; then
    # Upgrade
    systemctl stop bm-forest-node
fi

%post -p /bin/bash
systemctl daemon-reload
systemctl enable bm-forest-node

if [ $1 -eq 1 ]; then
    # Installation
    systemctl start bm-forest-node
fi

%preun
if [ $1 -eq 0 ]; then
    # Uninstall
    systemctl stop bm-forest-node
fi

%postun
if [ $1 -eq 1 ]; then
    # Upgrade
    systemctl start bm-forest-node
fi
