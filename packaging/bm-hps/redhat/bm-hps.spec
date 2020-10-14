Summary:            BlueMind HTTP proxy server
Name:               bm-hps
Version:            %{_bmrelease}
Release:            0
License:            GNU Affero General Public License v3
Group:              Applications/messaging
URL:                http://www.bluemind.net/
ExcludeArch:        s390 s390x
Requires(post):     systemd systemd-sysv
Requires:           bm-jdk = 8u265-bluemind36, bm-conf = %{version}-%{release}
Requires(post):     /bin/bash, initscripts

%description
BlueMind HTTP proxy server with SSO capabilities

%define __jar_repack 0

%install
cp -a %{_rootdir}/* %{buildroot}

mkdir -p %{buildroot}%{_unitdir}
install -m 644 /sources/stretch/bm-hps.service %{buildroot}%{_unitdir}

%files
%exclude %dir /usr
%exclude %dir /usr/lib
%exclude %dir /usr/lib/systemd
%exclude %dir %{_unitdir}
/*

%pre
if [ $1 -gt 1 ]; then
    # Upgrade
    [ -d /run/systemd/system ] && systemctl stop bm-hps
fi

%post -p /bin/bash
systemctl enable bm-hps
if [ -d /run/systemd/system ]; then
    systemctl daemon-reload

    if [ $1 -eq 1 ]; then
        # Installation
        systemctl start bm-hps
    fi
fi

%preun
if [ $1 -eq 0 ]; then
    # Uninstall
    [ -d /run/systemd/system ] && systemctl stop bm-hps
fi

%postun
if [ $1 -eq 1 ]; then
    # Upgrade
    [ -d /run/systemd/system ] && systemctl start bm-hps
fi
