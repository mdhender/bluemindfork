Summary:            A powerfull collaborative software
Name:               bm-plugin-webserver-devfilter
Version:            %{_bmrelease}
Release:            0
License:            GNU Affero General Public License v3
Group:              Applications/messaging
URL:                http://www.blue-mind.net/
ExcludeArch:        s390 s390x
Summary:            BlueMind devfilter
Requires:           bm-webserver

%description
BlueMind dev package

%prep
rm -rf %{buildroot}/*

%build

%install
cp -a %{_rootdir}/usr %{buildroot}

%files
/*
