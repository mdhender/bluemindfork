Name:               bm-edge-role
Version:            %{_bmrelease}
Release:            0
License:            GNU Affero General Public License v3
Group:              Applications/messaging
URL:                http://www.bluemind.net/
ExcludeArch:        s390 s390x
Summary:            BlueMind edge role
Requires:           bm-node = %{version}-%{release}, bm-conf = %{version}-%{release}, bm-postfix = %{version}-%{release}, bm-nginx = 1.20.1-bluemind101, bm-ysnp = %{version}-%{release}, bm-tick-node = %{version}-%{release}, bm-client-access = %{version}-%{release}

%description
Install and configure BlueMind server edge role to proxy services to BlueMind core

%global _curdir %_topdir/..
%global _initrddir /etc/rc.d/init.d

%prep
rm -rf %{buildroot}/*

%files
