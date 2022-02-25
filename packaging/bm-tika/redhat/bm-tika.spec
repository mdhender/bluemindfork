Summary:            BlueMind tika-based text extractor
Name:               bm-tika
Version:            %{_bmrelease}
Release:            0
License:            GNU Affero General Public License v3
Group:              Applications/messaging
URL:                http://www.bluemind.net/
ExcludeArch:        s390 s390x
Requires(post):     systemd systemd-sysv
Requires:           bm-jdk = 8u322-bluemind48, bm-conf = %{version}-%{release}
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
    [ -d /run/systemd/system ] && systemctl stop bm-tika
fi

%post -p /bin/bash
systemctl enable bm-tika
if [ -d /run/systemd/system ]; then
    systemctl daemon-reload
    if [ $1 -eq 1 ]; then
        # Installation
        systemctl start bm-tika
    fi
fi

%preun
if [ $1 -eq 0 ]; then
    # Uninstall
    [ -d /run/systemd/system ] && systemctl stop bm-tika
fi

%postun
if [ $1 -eq 1 ]; then
    # Upgrade
    [ -d /run/systemd/system ] && systemctl start bm-tika
fi
