Summary:            BlueMind Postfix
Name:               bm-postfix
Version:            %{_bmrelease}
Release:            0
License:            GNU Affero General Public License v3
Group:              Applications/messaging
URL:                http://www.bluemind.net/
ExcludeArch:        s390 s390x
Requires:           postfix, bm-milter, cyrus-sasl, cyrus-sasl-plain

%global _initrddir /etc/rc.d/init.d

%description
Configuration of Postfix

%install
cp -a /sources/ROOT/* %buildroot
mkdir -p %{buildroot}%{_initrddir}
cp /sources/rhel7/postfix.systemd.wrapper %{buildroot}%{_initrddir}/postfix
chmod +x %{buildroot}%{_initrddir}/postfix

%files
%dir %attr(755, root, root)
/*

%post
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
