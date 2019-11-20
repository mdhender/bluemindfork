# Documentation
#   - http://fedoraproject.org/wiki/How_to_create_an_RPM_package
#   - http://fedoraproject.org/wiki/Packaging/ScriptletSnippets
Name:               bm-checks
Version:            %{_bmrelease}
Release:            0
License:            GNU Affero General Public License v3
Group:              Applications/messaging
URL:                http://www.blue-mind.net/
ExcludeArch:        s390 s390x
Requires:           curl, expect, cyrus-sasl
Summary:            BlueMind services health check scripts

%description
BlueMind services health check scripts

%prep
rm -rf %{buildroot}/*

%build

%install
# Install bm-BlueMind services health check scripts
mkdir -p %{buildroot}/usr/share/bm-checks
cp %{_rootdir}/usr/share/bm-checks/* %{buildroot}/usr/share/bm-checks/

%files
%attr(0755, root, root) /usr/share/bm-checks/*sh

%clean
# Clean RedHat build root
rm -rf %{buildroot}
