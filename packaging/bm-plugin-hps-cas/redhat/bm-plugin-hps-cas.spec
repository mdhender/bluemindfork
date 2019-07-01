Summary:            BlueMind CAS support
Name:               bm-plugin-hps-cas
Version:            %{_bmrelease}
Release:            0
License:            GNU Affero General Public License v3
Group:              Applications/messaging
URL:                http://www.bluemind.net/
ExcludeArch:        s390 s390x
Requires:           bm-hps = %{version}-%{release}
Conflicts:          bm-cas-native, bm-cas-role
Obsoletes:          bm-cas-native, bm-cas-role

%description
BlueMind CAS support

%install
cp -a %{_rootdir}/* %{buildroot}

%files
/*
