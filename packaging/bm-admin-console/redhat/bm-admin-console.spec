Summary:            Admin Console web interface
Name:               bm-admin-console
Version:            %{_bmrelease}
Release:            0
License:            GNU Affero General Public License v3
Group:              Applications/messaging
URL:                http://www.bluemind.net/
ExcludeArch:        s390 s390x
Requires(post):     systemd systemd-sysv
Requires:           bm-webserver = %{version}-%{release}
Conflicts:          bm-plugin-admin-console-monitoring
Obsoletes:          bm-plugin-admin-console-monitoring

%description
Admin Console web interface

%install
cp -a %{_rootdir}/* %{buildroot}

%files
/*
