Summary:            BlueMind web server
Name:               bm-webserver
Version:            %{_bmrelease}
Release:            0
License:            GNU Affero General Public License v3
Group:              Applications/messaging
URL:                http://www.bluemind.net/
ExcludeArch:        s390 s390x
Requires(post):     systemd systemd-sysv
Requires:           bm-jdk = 8u242-bluemind32, bm-conf = %{version}-%{release}, bm-nginx = 1.16.1-bluemind84, bm-client-access = %{version}-%{release}
Requires(post):     /bin/bash, initscripts
Conflicts:          bm-tomcat
Obsoletes:          bm-tomcat

%description
BlueMind web server

%define __jar_repack 0

%install
cp -a %{_rootdir}/* %{buildroot}

mkdir -p %{buildroot}%{_unitdir}
install -m 644 /sources/stretch/bm-webserver.service %{buildroot}%{_unitdir}

%files
%exclude %dir /usr
%exclude %dir /usr/lib
%exclude %dir /usr/lib/systemd
%exclude %dir %{_unitdir}
/*

%pre
if [ $1 -gt 1 ]; then
    # Upgrade
    systemctl stop bm-webserver
fi

%post -p /bin/bash
systemctl daemon-reload
systemctl enable bm-webserver

if [ -d /var/lib/bm-webserver ]; then
    rm -fr /var/lib/bm-webserver/*
fi

if [ $1 -eq 1 ]; then
    # Installation
    systemctl start bm-webserver
fi

%preun
if [ $1 -eq 0 ]; then
    # Uninstall
    systemctl stop bm-webserver
fi

%postun
if [ $1 -eq 1 ]; then
    # Upgrade
    systemctl start bm-webserver
fi

%triggerin -p /bin/bash -- bm-setup-wizard, bm-installation-wizard, bm-admin-console, bm-calendar, bm-connector-thunderbird, bm-default-app, bm-plugin-admin-console-ldap-import, bm-plugin-admin-console-ad-import, bm-plugin-webserver-dav, bm-settings, bm-webmail, bm-autodiscover, bm-chooser, bm-contact, bm-im, bm-plugin-webserver-cti, bm-push, bm-todolist, bm-plugin-webserver-filehosting, bm-doc
[ $1 -ne 1 ] && exit 0
if [ $2 -eq 1 ]; then
    if [ -d /var/lib/bm-webserver ]; then
        rm -fr /var/lib/bm-webserver/*
    fi
    systemctl restart bm-webserver
fi

%triggerpostun -p /bin/bash -- bm-setup-wizard, bm-installation-wizard, bm-admin-console, bm-calendar, bm-connector-thunderbird, bm-default-app, bm-plugin-admin-console-ldap-import, bm-plugin-admin-console-ad-import, bm-plugin-webserver-dav, bm-settings, bm-webmail, bm-autodiscover, bm-chooser, bm-contact, bm-im, bm-plugin-webserver-cti, bm-push, bm-todolist, bm-plugin-webserver-filehosting, bm-doc
[ $1 -ne 1 ] && exit 0
[ $2 -lt 2 ] && systemctl restart bm-webserver
