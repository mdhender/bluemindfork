Name:               bm-plugin-webserver-dav
Version:            %{_bmrelease}
Release:            0
License:            GNU Affero General Public License v3
Group:              Applications/messaging
URL:                http://www.blue-mind.net/
ExcludeArch:        s390 s390x
Summary:            BlueMind CalDAV and CardDAV server
Requires:           bm-webserver = %{version}-%{release}
Conflicts:          bm-dav
Obsoletes:          bm-dav

%description
BlueMind CalDAV and CardDAV server

%prep
rm -rf %{buildroot}/*

%build

%install
cp -a %{_rootdir}/usr %{buildroot}

%files
/*
