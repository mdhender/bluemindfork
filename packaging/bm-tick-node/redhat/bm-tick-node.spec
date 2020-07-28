Name:               bm-tick-node
Version:            %{_bmrelease}
Release:            0
License:            GNU Affero General Public License v3
Group:              Applications/messaging
URL:                http://www.bluemind.net/
ExcludeArch:        s390 s390x
Summary:            BlueMind TICK stack
Requires:           bm-telegraf = 1.14.4.bm1-1, bm-metrics-agent

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
    chmod +x /usr/local/bin/unixget
    chmod +x /usr/local/bin/unixget-impl
    cp ${tick_conf}/bm-telegraf.conf /etc/telegraf/telegraf.d/
    service telegraf restart
fi

%files
%attr(0755, root, root) /usr/local/bin/unixget
%attr(0755, root, root) /usr/local/bin/unixget-impl
/*
