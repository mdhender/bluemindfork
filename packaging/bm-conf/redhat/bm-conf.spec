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
Conflicts:          bm-ips, bm-cyrus, bm-imap-i18n, bm-cyrus-imapd, bm-lmtpd, bm-sds-proxy, bm-plugin-core-mailapp
Obsoletes:          bm-ips, bm-cyrus, bm-imap-i18n, bm-cyrus-imapd, bm-lmtpd, bm-sds-proxy, bm-plugin-core-mailapp

%define bluemindgid 841
%define _bluemindgroup bluemind

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

install -m 644 /sources/stretch/bm-conf.bluemind.target %{buildroot}/lib/systemd/system/bluemind.target

%files
%attr(0755, root, root) /etc/bm
%attr(0755, root, root) /usr/bin/bmctl
%attr(0755, root, root) /usr/bin/bmprofile
%attr(0755, root, root) /usr/bin/bmprofile-jfr
%attr(0644, root, root) /lib/systemd/system/bluemind.target
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

# create 'bluemind' group on target host
getent group %{_bluemindgroup} >/dev/null || /usr/sbin/groupadd -g %{bluemindgid} -r %{_bluemindgroup}
getent passwd cyrus >/dev/null && /usr/sbin/usermod -a -G %{_bluemindgroup} cyrus
getent passwd www-data >/dev/null && /usr/sbin/usermod -a -G %{_bluemindgroup} www-data
getent passwd telegraf >/dev/null && /usr/sbin/usermod -a -G %{_bluemindgroup} telegraf

# Needed to ensure pre script quit with 0 as return value
exit 0

%post

manageDeprecatedServices() {
    local deprecatedServices="bm-lmtpd \
        bm-sds-proxy \
        bm-xmpp" 

    for service in ${deprecatedServices}; do
        deprecatedServices ${service}
    done
}

removeCyrus() {
    local cyrusServices="bm-cyrus-imapd \
        bm-cyrus-syncclient@0 bm-cyrus-syncclient@1 bm-cyrus-syncclient@2 bm-cyrus-syncclient@3"

    for service in ${cyrusServices}; do
        deprecatedServices ${service}
    done

    [ -e /etc/cron.daily/bm-cyrus-imapd ] && rm -f /etc/cron.daily/bm-cyrus-imapd
    [ -e /etc/systemd/system/bm-cyrus-imapd.service.wants ] && rm -rf /etc/systemd/system/bm-cyrus-imapd.service.wants
}

deprecatedServices() {
    local service=${1}

    echo "Purging deprecated service: "${service}
    systemctl --no-reload disable --now ${service}.service
}

manageSystemUserGroup
manageDeprecatedServices
removeCyrus

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
    chown root:bluemind /etc/bm/bm-core.tok
fi

systemctl enable bluemind.target
if [ -d /run/systemd/system ]; then
    systemctl daemon-reload

    if [ $1 -eq 1 ]; then
        # Installation
        systemctl start bluemind.target
    fi
fi

for service in $(grep "enable" /lib/systemd/system-preset/10-bluemind.preset |cut -d ' ' -f 2); do
    if systemctl is-enabled ${service} > /dev/null 2>&1; then
        echo -n "Re-enabling "${service}": "
        systemctl reenable ${service} > /dev/null 2>&1 || true
        echo "done"
    fi
done


%preun
if [ $1 -eq 0 ]; then
    # Uninstall
    [ -d /run/systemd/system ] && systemctl stop bluemind.target
fi

%postun
if [ $1 -eq 0 ]; then
    # Uninstall
    if [ -e /etc/bm ]; then
        rm -rf /etc/bm
    fi
fi

if [ $1 -eq 1 ]; then
    # Upgrade
    [ -d /run/systemd/system ] && systemctl start bluemind.target
fi
