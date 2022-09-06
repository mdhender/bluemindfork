Summary:            BlueMind XiVO bridge server
Name:               bm-xivobridge
Version:            %{_bmrelease}
Release:            0
License:            GNU Affero General Public License v3
Group:              Applications/messaging
URL:                http://www.bluemind.net/
ExcludeArch:        s390 s390x
Requires(post):     systemd systemd-sysv
Requires:           bm-jdk = 17.0.4.1+1-bluemind12, bm-conf = %{version}-%{release}
Requires(post):     /bin/bash, initscripts

%description
BlueMind XiVO bridge sends cti events to BlueMind components

%define __jar_repack 0

%install
cp -a %{_rootdir}/* %{buildroot}

mkdir -p %{buildroot}%{_unitdir}
install -m 644 /sources/stretch/bm-xivobridge.service %{buildroot}%{_unitdir}

%files
%exclude %dir /usr
%exclude %dir /usr/lib
%exclude %dir /usr/lib/systemd
%exclude %dir %{_unitdir}
/*

%pre
if [ $1 -gt 1 ]; then
    # Upgrade
    [ -d /run/systemd/system ] && systemctl stop bm-xivobridge
fi

%post -p /bin/bash
rm -rf /usr/share/bm-xivodridge/extensions/eclipse/plugins
mkdir -p /usr/share/bm-xivobridge/extensions/eclipse/plugins
systemctl enable bm-xivobridge

if [ -d /run/systemd/system ]; then
    systemctl daemon-reload

    if [ $1 -eq 1 ]; then
        # Installation
        systemctl start bm-xivobridge
    fi
fi

%preun
if [ $1 -eq 0 ]; then
    # Uninstall
    [ -d /run/systemd/system ] && systemctl stop bm-xivobridge
fi

%postun
if [ $1 -eq 1 ]; then
    # Upgrade
    [ -d /run/systemd/system ] && systemctl start bm-xivobridge
fi
