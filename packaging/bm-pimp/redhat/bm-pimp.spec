Summary:            BlueMind automated memory tuning
Name:               bm-pimp
Version:            %{_bmrelease}
Release:            0
License:            GNU Affero General Public License v3
Group:              Applications/messaging
URL:                http://www.bluemind.net/
ExcludeArch:        s390 s390x
Requires(post):     systemd systemd-sysv
Requires:           bm-jdk = 8u232-bluemind31
Requires(post):     /bin/bash, initscripts

%description
BlueMind automated memory tuning

%install
cp -a %{_rootdir}/* %{buildroot}

mkdir -p %{buildroot}%{_initrddir}
cp /sources/stretch/bm-pimp.init %{buildroot}%{_initrddir}/bm-pimp

mkdir -p %{buildroot}%{_unitdir}
install -m 644 /sources/stretch/bm-pimp.service %{buildroot}%{_unitdir}

%files
%attr(0755, root, root) %{_initrddir}/bm-pimp
%exclude %dir /usr
%exclude %dir /usr/lib
%exclude %dir /usr/lib/systemd
%exclude %dir %{_unitdir}
/*

%post
systemctl daemon-reload
systemctl enable bm-pimp

if [ $1 -eq 1 ]; then
    # Installation
    systemctl start bm-pimp
fi

%postun
if [ $1 -eq 1 ]; then
    # Upgrade
    systemctl start bm-pimp
fi
