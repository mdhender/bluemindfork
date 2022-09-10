Summary:            BlueMind XMPP server
Name:               bm-xmpp
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
BlueMind XMPP server

%define __jar_repack 0

%install
cp -a %{_rootdir}/* %{buildroot}

mkdir -p %{buildroot}%{_unitdir}
install -m 644 /sources/stretch/bm-xmpp.service %{buildroot}%{_unitdir}

%files
%exclude %dir /usr
%exclude %dir /usr/lib
%exclude %dir /usr/lib/systemd
%exclude %dir %{_unitdir}
/*

%pre
if [ $1 -gt 1 ]; then
    # Upgrade
    [ -d /run/systemd/system ] && systemctl stop bm-xmpp
fi

%post -p /bin/bash
rm -f /usr/share/bm-xmpp/configuration/org.eclipse.equinox.simpleconfigurator/bundles.info.installed
rm -rf /usr/share/bm-xmpp/dropins
mkdir -p /usr/share/bm-xmpp/dropins
systemctl enable bm-xmpp
if [ -d /run/systemd/system ]; then
    systemctl daemon-reload

    if [ $1 -eq 1 ]; then
        # Installation
        systemctl start bm-xmpp
    fi
fi

%preun
if [ $1 -eq 0 ]; then
    # Uninstall
    [ -d /run/systemd/system ] && systemctl stop bm-xmpp
fi

%postun
if [ $1 -eq 1 ]; then
    # Upgrade
    [ -d /run/systemd/system ] && systemctl start bm-xmpp
fi
