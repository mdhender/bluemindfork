Summary:            BlueMind Command Line Interface
Name:               bm-cli
Version:            %{_bmrelease}
Release:            0
License:            GNU Affero General Public License v3
Group:              Applications/messaging
URL:                http://www.bluemind.net/
ExcludeArch:        s390 s390x
Requires:           bm-jdk = 21.0.2+13-bluemind1099, jq

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

/usr/lib/jvm/bm-jdk/bin/java -Xshare:dump

# Generate autocompletion map
if [ -d /etc/bash_completion.d/ ]; then
/usr/bin/bm-cli generate-completion >/usr/share/bm-cli/bm-cli.bash-completion 2>/dev/null
cat >/etc/bash_completion.d/bm-cli <<EOF
if [[ -e /usr/share/bm-cli/bm-cli.bash-completion ]]; then
    . /usr/share/bm-cli/bm-cli.bash-completion
fi
EOF
fi

%preun
if [ $1 -eq 0 ]; then
    # Uninstall
    rm -f /usr/bin/bm-cli
fi

%triggerin -p /bin/bash -- bm-plugin-cli-setup, bm-plugin-cli-mapi
[ $1 -ne 1 ] && exit 0

%triggerpostun -p /bin/bash --  bm-plugin-cli-setup, bm-plugin-cli-mapi
[ $1 -ne 1 ] && exit 0

