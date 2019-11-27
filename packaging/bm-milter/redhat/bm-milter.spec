Summary:            BlueMind Milter
Name:               bm-milter
Version:            %{_bmrelease}
Release:            0
License:            GNU Affero General Public License v3
Group:              Applications/messaging
URL:                http://www.bluemind.net/
ExcludeArch:        s390 s390x
Requires(post):     systemd systemd-sysv
Requires:           bm-jdk = 8u232-bluemind31, bm-conf = %{version}-%{release}
Requires(post):     /bin/bash, initscripts

%description
BlueMind Milter filters

%define __jar_repack 0

%install
cp -a %{_rootdir}/* %{buildroot}

mkdir -p %{buildroot}%{_unitdir}
install -m 644 /sources/stretch/bm-milter.service %{buildroot}%{_unitdir}

%files
%exclude %dir /usr
%exclude %dir /usr/lib
%exclude %dir /usr/lib/systemd
%exclude %dir %{_unitdir}
/*

%pre
if [ $1 -gt 1 ]; then
    # Upgrade
    systemctl stop bm-milter
fi

%post -p /bin/bash
systemctl daemon-reload
systemctl enable bm-milter

if [ $1 -eq 1 ]; then
    # Installation
    systemctl start bm-milter
fi

%preun
if [ $1 -eq 0 ]; then
    # Uninstall
    systemctl stop bm-milter
fi

%postun
if [ $1 -eq 1 ]; then
    # Upgrade
    systemctl start bm-milter
fi
