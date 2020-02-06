Summary:            BlueMind core server
Name:               bm-core
Version:            %{_bmrelease}
Release:            0
License:            GNU Affero General Public License v3
Group:              Applications/messaging
URL:                http://www.bluemind.net/
ExcludeArch:        s390 s390x
Requires(post):     systemd systemd-sysv
Requires:           bm-jdk = 8u242-bluemind32, bm-conf = %{version}-%{release}, ghostscript-fonts, dejavu-sans-fonts, dejavu-sans-mono-fonts, dejavu-serif-fonts, openssl, ca-certificates, bm-tika = %{version}-%{release}, bm-plugin-core-subscription = %{version}-%{release}
Requires(post):     /bin/bash, initscripts
Conflicts:          bm-soap, bm-plugin-core-dataprotect-upgrade
Obsoletes:          bm-soap, bm-plugin-core-dataprotect-upgrade

%description
BlueMind core server

%define __jar_repack 0

%install
cp -a %{_rootdir}/* %{buildroot}

mkdir -p %{buildroot}%{_unitdir}
install -m 644 /sources/stretch/bm-core.service %{buildroot}%{_unitdir}


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
    systemctl stop bm-core
fi

%post -p /bin/bash
systemctl daemon-reload
systemctl enable bm-core

if [ $1 -eq 1 ]; then
    # Installation
    systemctl start bm-core
fi

%preun
if [ $1 -eq 0 ]; then
    # Uninstall
    systemctl stop bm-core
fi

%postun
if [ $1 -eq 1 ]; then
    # Upgrade
    systemctl start bm-core
fi
