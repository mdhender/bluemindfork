Summary:            BlueMind tika-based text extractor
Name:               bm-tika
Version:            %{_bmrelease}
Release:            0
License:            GNU Affero General Public License v3
Group:              Applications/messaging
URL:                http://www.bluemind.net/
ExcludeArch:        s390 s390x
Requires(post):     systemd systemd-sysv
Requires:           bm-jdk = 8u222-bluemind30, bm-conf = %{version}-%{release}
Requires(post):     /bin/bash, initscripts

%description
BlueMind tika-based text extractor

%define __jar_repack 0

%install
cp -a %{_rootdir}/* %{buildroot}

mkdir -p %{buildroot}%{_unitdir}
install -m 644 /sources/stretch/bm-tika.service %{buildroot}%{_unitdir}

%files
%attr(0755, root, root) %{_datadir}/bm-tika/bin/oom_kill.sh
%attr(0755, root, root) %{_datadir}/bm-tika/bin/check_and_respawn.sh
%exclude %dir /usr
%exclude %dir /usr/lib
%exclude %dir /usr/lib/systemd
%exclude %dir %{_unitdir}
/*

%pre
if [ $1 -gt 1 ]; then
    # Upgrade
    systemctl stop bm-tika
fi

%post -p /bin/bash
systemctl daemon-reload
systemctl enable bm-tika

if [ $1 -eq 1 ]; then
    # Installation
    systemctl start bm-tika
fi

%preun
if [ $1 -eq 0 ]; then
    # Uninstall
    systemctl stop bm-tika
fi

%postun
if [ $1 -eq 1 ]; then
    # Upgrade
    systemctl start bm-tika
fi
