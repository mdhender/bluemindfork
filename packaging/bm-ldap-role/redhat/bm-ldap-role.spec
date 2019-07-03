Summary:            BlueMind directory export
Name:               bm-ldap-role
Version:            %{_bmrelease}
Release:            0
License:            GNU Affero General Public License v3
Group:              Applications/messaging
URL:                http://www.bluemind.net/
ExcludeArch:        s390 s390x
Requires:           bm-conf = %{version}-%{release}, bm-node = %{version}-%{release}, openldap-servers, openldap-clients, bm-ysnp = %{version}-%{release}

%description
Install and configure needed LDAP services to BlueMind directory export plug-in

This package must be install on servers which will be LDAP master for BlueMind domains

%install
cp -a %{_rootdir}/* %{buildroot}

%files
/*

%post
echo -n "Add user ldap to saslauth group... "
usermod -a -G saslauth ldap
echo "done"

echo -n "RHEL7: Add slapd init wrapper... "
cp /usr/share/doc/bm-ldap-role/slapd.systemd.wrapper /etc/init.d/slapd
chmod +x /etc/init.d/slapd
echo "done"
