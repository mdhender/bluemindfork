Summary:            BlueMind Postfix
Name:               bm-postfix
Version:            %{_bmrelease}
Release:            0
License:            GNU Affero General Public License v3
Group:              Applications/messaging
URL:                http://www.bluemind.net/
ExcludeArch:        s390 s390x
Requires:           postfix, bm-milter = %{version}-%{release}, bm-ysnp = %{version}-%{release}, cyrus-sasl, cyrus-sasl-plain

%global _initrddir /etc/rc.d/init.d

%description
Configuration of Postfix

%install
cp -a /sources/ROOT/* %buildroot

%files
%dir %attr(755, root, root)
/*

%post
[ -d /run/systemd/system ] && systemctl daemon-reload
systemctl enable postfix

if [ $1 -eq 1 ]; then
    # Install
    echo -n "Add user postfix to saslauth group... "
    usermod -a -G saslauth postfix
    service postfix restart
    echo "done"
fi

%postun
if [ $1 -eq 0 ]; then
    # Uninstall
    echo -n "Remove postfix user from saslauth group... "
    usermod -G `id -nG postfix|sed -e 's/saslauth//'|sed -e 's/^ *//'|sed -e 's/ *$//'|sed -e 's/ \+/ /g'|tr ' ' ','` postfix
    service postfix restart
    echo done
fi
