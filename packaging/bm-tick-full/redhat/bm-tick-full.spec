Name:               bm-tick-full
Version:            %{_bmrelease}
Release:            0
License:            GNU Affero General Public License v3
Group:              Applications/messaging
URL:                http://www.bluemind.net/
ExcludeArch:        s390 s390x
Summary:            BlueMind TICK stack
Requires:           bm-node = %{version}-%{release}, bm-tick-node = %{version}-%{release}, bm-chronograf = 1.6.2.bm2~224d2b158, bm-kapacitor = 1.5.2.bm1~b76a788b, bm-influxdb = 1.6.6.bm1~5610cb9d4

%description
Installs TICK stack for BlueMind

%global _curdir %_topdir/..
%global _initrddir /etc/rc.d/init.d

%prep
rm -rf %{buildroot}/*

%install
cp -a %{_rootdir}/* %{buildroot}

%post
[ -d /run/systemd/system ] && systemctl daemon-reload

if [ $1 -eq 1 ]; then
    # Installation
    tick_conf=/usr/share/bm-tick-config

    cp ${tick_conf}/chronograf.default /etc/default/chronograf
    service bm-nginx restart
    service influxdb restart
    service kapacitor restart
    service chronograf restart
fi

%files
/*
