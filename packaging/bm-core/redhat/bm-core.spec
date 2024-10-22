Summary:            BlueMind core server
Name:               bm-core
Version:            %{_bmrelease}
Release:            0
License:            GNU Affero General Public License v3
Group:              Applications/messaging
URL:                http://www.bluemind.net/
ExcludeArch:        s390 s390x
Requires(post):     systemd systemd-sysv
Requires:           bm-jdk = 21.0.2+13-bluemind1099, bm-conf = %{version}-%{release}, openssl, ca-certificates, bm-tika = %{version}-%{release}, bm-plugin-core-subscription = %{version}-%{release}, bm-upgraders-list = %{version}-%{release}
Requires(post):     /bin/bash, initscripts
Conflicts:          bm-soap, bm-plugin-core-dataprotect-upgrade, bm-plugin-core-hoster-report, bm-plugin-core-mapi-support, bm-plugin-core-monitoring, bm-locator, bm-plugin-core-videoconferencing-starleaf, bm-hps, bm-plugin-hps-cas, bm-plugin-hps-kerberos, bm-plugin-hps-signature, bm-xmpp, bm-im
Obsoletes:          bm-soap, bm-plugin-core-dataprotect-upgrade, bm-plugin-core-hoster-report, bm-plugin-core-mapi-support, bm-plugin-core-monitoring, bm-locator, bm-plugin-core-videoconferencing-starleaf, bm-hps, bm-plugin-hps-cas, bm-plugin-hps-kerberos, bm-plugin-hps-signature, bm-xmpp, bm-im

%description
BlueMind core server

%define __jar_repack 0

%install
cp -a %{_rootdir}/* %{buildroot}

mkdir -p %{buildroot}%{_unitdir}
install -m 644 /sources/buster/bm-core.service %{buildroot}%{_unitdir}


%files
%exclude /etc/ssl/certs
%exclude %dir /usr
%exclude %dir /usr/lib
%exclude %dir /usr/lib/systemd
%exclude %dir %{_unitdir}
/*

%pre
if [ $1 -gt 1 ]; then
    # Upgrade
    [ -d /run/systemd/system ] && systemctl stop bm-core
fi

%post -p /bin/bash
systemctl enable bm-core
if [ -d /run/systemd/system ]; then
    systemctl daemon-reload

    if [ $1 -eq 1 ]; then
        # Installation
        systemctl start bm-core
    fi
fi

%preun
if [ $1 -eq 0 ]; then
    # Uninstall
    [ -d /run/systemd/system ] && systemctl stop bm-core
fi

%postun
if [ $1 -eq 1 ]; then
    # Upgrade
    [ -d /run/systemd/system ] && systemctl start bm-core
fi
