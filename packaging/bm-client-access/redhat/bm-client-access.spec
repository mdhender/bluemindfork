# Documentation
#   - http://fedoraproject.org/wiki/How_to_create_an_RPM_package
#   - http://fedoraproject.org/wiki/Packaging/ScriptletSnippets
Name:               bm-client-access
Version:            %{_bmrelease}
Release:            0
License:            GNU Affero General Public License v3
Group:              Applications/messaging
URL:                http://www.blue-mind.net/
ExcludeArch:        s390 s390x
Summary:            BlueMind client access
Requires:           bm-conf = %{version}-%{release}, bm-nginx = 1.18.0-bluemind99, epel-release >= 7, openssl
Requires:           /bin/bash

%description
Proxy server for BlueMind client services

Need EPEL repository (http://fedoraproject.org/wiki/EPEL)

%build

%install
mkdir -p %{buildroot}/usr/share/doc
cp -r %{_rootdir}/usr/share/doc/bm-client-access %{buildroot}/usr/share/doc

mkdir -p %{buildroot}/etc/nginx/sites-available
mkdir -p %{buildroot}/etc/nginx/sites-enabled
mkdir -p %{buildroot}/etc/nginx/global.d
mkdir -p %{buildroot}/etc/nginx/bm-local.d

cp -r %{_rootdir}/* %{buildroot}

%files
/*

%pre -p /bin/bash

# SB-991: don't manage NGinx on edge already configured
[ $1 -eq 1 ] && touch /etc/nginx/BM-DONOTCONF

exit 0

%post -p /bin/bash

/usr/share/bm-client-access/bin/configure-nginx.sh

chkconfig bm-nginx on
service bm-nginx restart

%postun
if [ $1 -eq 0 ]; then
    # Uninstall
    if [ -e /etc/nginx/sw.htpasswd ]; then
        rm -f /etc/nginx/sw.htpasswd
    fi

    if [ -e /etc/nginx/sites-enabled ]; then
        rm -rf /etc/nginx/sites-enabled
    fi

    if [ -e /etc/nginx/sites-available ]; then
        rm -rf /etc/nginx/sites-available
    fi

    rm -f /etc/nginx/nginx.conf || true
    if [ -e /etc/nginx/nginx.conf.orig ]; then
        mv /etc/nginx/nginx.conf.orig /etc/nginx/nginx.conf
    fi
    
    if [ -e /etc/bm-webmail/nginx-webmail.conf ]; then
        rm -f /etc/bm-webmail/nginx-webmail.conf
    fi
    
    if [ -e /etc/bm-webmail/bm-filehosting.conf ]; then
        rm -f /etc/bm-webmail/bm-filehosting.conf
    fi
    
    if [ -e /etc/bm-eas/bm-eas-nginx.conf ]; then
        rm -f /etc/bm-eas/bm-eas-nginx.conf
    fi
fi

%triggerpostun -- nginx
cd /etc/nginx/sites-enabled
if ! [ "$(ls -A)" ]; then
    if [ -f /etc/nginx/sw.htpasswd ]; then
        ln -s ../sites-available/bm-client-access .
    else
        ln -s ../sites-available/bm-client-access-without-password .
    fi

    service bm-nginx restart
fi
