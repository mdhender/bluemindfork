Summary:            BlueMind locator service
Name:               bm-locator
Version:            %{_bmrelease}
Release:            0
License:            GNU Affero General Public License v3
Group:              Applications/messaging
URL:                http://www.bluemind.net/
ExcludeArch:        s390 s390x
Requires(post):     systemd systemd-sysv
Requires:           bm-jdk = 8u212-bluemind29, vim-enhanced, emacs-nox, strace, lsof, telnet, openssh-clients, tar, gzip, bzip2, rsync, bm-conf = %{version}-%{release}, nfs-utils, sysstat, bm-pimp = %{version}-%{release}, iptables, sudo, httpd-tools
Requires(post):     /bin/bash, initscripts

%description
Locator server for BlueMind

%define __jar_repack 0

%install
cp -a %{_rootdir}/* %{buildroot}

mkdir -p %{buildroot}%{_unitdir}
install -m 644 /sources/stretch/bm-locator.service %{buildroot}%{_unitdir}

%files
%exclude %dir /usr
%exclude %dir /usr/lib
%exclude %dir /usr/lib/systemd
%exclude %dir %{_unitdir}
/*

%pre
if [ $1 -gt 1 ]; then
    # Upgrade
    systemctl stop bm-locator
fi

%post -p /bin/bash
systemctl daemon-reload
systemctl enable bm-locator

if [ $1 -eq 1 ]; then
    # Installation
    systemctl start bm-locator
fi

%preun
if [ $1 -eq 0 ]; then
    # Uninstall
    systemctl stop bm-locator
fi

%postun
if [ $1 -eq 1 ]; then
    # Upgrade
    systemctl start bm-locator
fi
