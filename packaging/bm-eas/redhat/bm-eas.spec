Summary:            BlueMind Mobile Synchronization server
Name:               bm-eas
Version:            %{_bmrelease}
Release:            0
License:            GNU Affero General Public License v3
Group:              Applications/messaging
URL:                http://www.bluemind.net/
ExcludeArch:        s390 s390x
Requires(post):     systemd systemd-sysv
Requires:           bm-jdk = 8u222-bluemind30, bm-node = %{version}-%{release}
Requires(post):     /bin/bash, initscripts

%description
Mobile synchronization server for BlueMind

%define __jar_repack 0

%install
cp -a %{_rootdir}/* %{buildroot}

mkdir -p %{buildroot}%{_unitdir}
install -m 644 /sources/stretch/bm-eas.service %{buildroot}%{_unitdir}


%files
%exclude %dir /usr
%exclude %dir /usr/lib
%exclude %dir /usr/lib/systemd
%exclude %dir %{_unitdir}
/*

%pre
if [ $1 -gt 1 ]; then
    # Upgrade
    systemctl stop bm-eas
fi

%post -p /bin/bash
systemctl daemon-reload
systemctl enable bm-eas

if [ $1 -eq 1 ]; then
    # Installation
    systemctl start bm-eas
fi

%preun
if [ $1 -eq 0 ]; then
    # Uninstall
    systemctl stop bm-eas
fi

%postun
if [ $1 -eq 1 ]; then
    # Upgrade
    systemctl start bm-eas
fi
