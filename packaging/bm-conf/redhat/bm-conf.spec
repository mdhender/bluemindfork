# Documentation
#   - http://fedoraproject.org/wiki/How_to_create_an_RPM_package
#   - http://fedoraproject.org/wiki/Packaging/ScriptletSnippets
Name:               bm-conf
Version:            %{_bmrelease}
Release:            0
License:            GNU Affero General Public License v3
Group:              Applications/messaging
URL:                http://www.blue-mind.net/
ExcludeArch:        s390 s390x
Summary:            BlueMind configuration
Conflicts:          bm-ips
Obsoletes:          bm-ips

%description
BlueMind configuration

%prep
rm -rf %{buildroot}/*

%build

%install
# Install bm-conf
cp -r %{_rootdir}/etc %{buildroot}
mkdir -p %{buildroot}/usr/bin
cp %{_rootdir}/usr/bin/* %{buildroot}/usr/bin
mkdir -p %{buildroot}/lib/systemd
cp -a %{_rootdir}/lib/systemd/* %{buildroot}/lib/systemd

%files
%attr(0755, root, root) /etc/bm
%attr(0755, root, root) /usr/bin/bm_java_home
%attr(0755, root, root) /usr/bin/bmctl
%attr(0644, root, root) /lib/systemd/system/bm-iptables.service
%attr(0644, root, root) /lib/systemd/system-preset/10-bluemind.preset

%clean
# Clean RedHat build root
rm -rf %{buildroot}

%pre
# Create 'www-data' user on target host
getent group www-data >/dev/null || /usr/sbin/groupadd -r www-data
getent passwd www-data >/dev/null || /usr/sbin/useradd -c "Nginx web server" -d /usr/share/nginx/html -g www-data \
  -s /sbin/nologin -r www-data

%post
for file in /etc/bm/nodeclient_cert.pem \
  /etc/bm/nodeclient_key.pem \
  /etc/bm/nodeclient.p12 \
  /etc/bm/nodeclient_keystore.jks \
  /etc/bm/nodeclient_truststore.jks \
  /etc/bm/bm.jks; do
    if [ -e ${file} ]; then
        chmod 400 ${file}
    fi
done

if [ -e /etc/bm/bm-core.tok ]; then
    chmod 440 /etc/bm/bm-core.tok
    chown root:www-data /etc/bm/bm-core.tok
fi

%postun
if [ $1 -eq 0 ]; then
    # Uninstall
    if [ -e /etc/bm ]; then
        rm -rf /etc/bm
    fi
fi

