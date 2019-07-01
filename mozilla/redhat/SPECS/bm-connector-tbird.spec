# Documentation
#   - http://fedoraproject.org/wiki/How_to_create_an_RPM_package
#   - http://fedoraproject.org/wiki/Packaging/ScriptletSnippets
Summary:            A powerfull collaborative software
Name:               bm-connector-thunderbird
Version:            3.1.%{_hudson_release}
Release:            0
License:            GPLv2+
Group:              Applications/messaging
URL:                http://www.blue-mind.net/
ExcludeArch:        s390 s390x
Summary:            Blue Mind Thunderbird connector

%description
Blue Mind Thunderbird connector

%prep
rm -rf %{buildroot}/*

%build

%install
mkdir -p %{buildroot}/usr/share/bm-settings/WEB-INF
cp /tmp/%{_branch_name}/tbird-rpm/bm-connector-tb-*.xpi %{buildroot}/usr/share/bm-settings/WEB-INF/bm-connector-tb.xpi
cp /tmp/%{_branch_name}/tbird-rpm/update.rdf %{buildroot}/usr/share/bm-settings/WEB-INF/update.rdf

%files
/usr/share/bm-settings/WEB-INF/bm-connector-tb.xpi
/usr/share/bm-settings/WEB-INF/update.rdf

%clean
rm -rf %{buildroot}

%changelog
* Mon Mar 19 2012 Blue Mind team <team@blue-mind.net> - 0.18
- Sprint 15.

%post 

%postun 

