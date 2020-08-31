Summary:            BlueMind Command Line Interface
Name:               bm-cli
Version:            %{_bmrelease}
Release:            0
License:            GNU Affero General Public License v3
Group:              Applications/messaging
URL:                http://www.bluemind.net/
ExcludeArch:        s390 s390x
Requires:           bm-jdk = 8u265-bluemind36, jq

%description
BlueMind CLI

%install
cp -a %{_rootdir}/* %{buildroot}

%files
/*

%post
if [ $1 -eq 1 ]; then
    # Installation
    rm -f /usr/bin/bm-cli
    ln -s /usr/share/bm-cli/bm-cli /usr/bin/bm-cli
    chmod +x /usr/share/bm-cli/bm-cli
    
fi

%posttrans
rm -fr /var/lib/bm-cli/*

rm -rf /usr/share/bm-cli/plugins
mkdir -p /usr/share/bm-cli/plugins

if [ -e /usr/share/bm-cli/extensions ]; then
    find /usr/share/bm-cli/extensions -name '*.jar' -exec ln -f {} /usr/share/bm-cli/plugins \;
fi
find /usr/share/bm-cli/main -name '*.jar' -exec ln -f {} /usr/share/bm-cli/plugins \;
/usr/lib/jvm/bm-jdk/bin/java -Xshare:dump

%preun
if [ $1 -eq 0 ]; then
    # Uninstall
    rm -f /usr/bin/bm-cli
fi

%triggerin -p /bin/bash -- bm-plugin-cli-setup, bm-plugin-cli-mapi
[ $1 -ne 1 ] && exit 0
if [ $2 -eq 1 ]; then
	rm -rf /usr/share/bm-cli/plugins
	mkdir -p /usr/share/bm-cli/plugins

	if [ -e /usr/share/bm-cli/extensions ]; then
		find /usr/share/bm-cli/extensions -name '*.jar' -exec ln -f {} /usr/share/bm-cli/plugins \;
	fi
	find /usr/share/bm-cli/main -name '*.jar' -exec ln -f {} /usr/share/bm-cli/plugins \;
fi

%triggerpostun -p /bin/bash --  bm-plugin-cli-setup, bm-plugin-cli-mapi
[ $1 -ne 1 ] && exit 0
[ $2 -lt 2 ] && {
    rm -rf /usr/share/bm-cli/plugins
	mkdir -p /usr/share/bm-cli/plugins

	if [ -e /usr/share/bm-cli/extensions ]; then
		find /usr/share/bm-cli/extensions -name '*.jar' -exec ln -f {} /usr/share/bm-cli/plugins \;
	fi
	find /usr/share/bm-cli/main -name '*.jar' -exec ln -f {} /usr/share/bm-cli/plugins \;
}
