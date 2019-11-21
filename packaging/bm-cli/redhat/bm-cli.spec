Summary:            BlueMind Command Line Interface
Name:               bm-cli
Version:            %{_bmrelease}
Release:            0
License:            GNU Affero General Public License v3
Group:              Applications/messaging
URL:                http://www.bluemind.net/
ExcludeArch:        s390 s390x
Requires:           bm-jdk = 8u232-bluemind31, jq

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
rm -fr /var/lib/bm-cli/*


%preun
if [ $1 -eq 0 ]; then
    # Uninstall
    rm -f /usr/bin/bm-cli
fi
