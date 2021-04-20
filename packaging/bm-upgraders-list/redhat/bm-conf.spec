# Documentation
#   - http://fedoraproject.org/wiki/How_to_create_an_RPM_package
#   - http://fedoraproject.org/wiki/Packaging/ScriptletSnippets
Name:               bm-upgraders-list
Version:            %{_bmrelease}
Release:            0
License:            GNU Affero General Public License v3
Group:              Applications/messaging
URL:                http://www.blue-mind.net/
ExcludeArch:        s390 s390x
Summary:            BlueMind upgraders list

%define bluemindgid 841
%define _bluemindgroup bluemind

%description
BlueMind configuration

%prep
rm -rf %{buildroot}/*

%build

%install
# Install bm-upgraders-list
cp -r %{_rootdir}/usr/share %{buildroot}

%files
%attr(0755, root, root) /usr/share/bm-upgraders-list

%clean
# Clean RedHat build root
rm -rf %{buildroot}

