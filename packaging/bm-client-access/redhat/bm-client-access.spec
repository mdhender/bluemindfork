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
Requires:           bm-conf = %{version}-%{release}, bm-nginx = 1.14.0-bluemind79, epel-release >= 6, openssl
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
cp %{_rootdir}/usr/share/doc/bm-client-access/global.d/*.conf %{buildroot}/etc/nginx/global.d
cp %{_rootdir}/usr/share/doc/bm-client-access/bm-client-access %{buildroot}/etc/nginx/sites-available
cp %{_rootdir}/usr/share/doc/bm-client-access/bm-client-access-without-password %{buildroot}/etc/nginx/sites-available

mkdir -p %{buildroot}/usr/share/bm-client-access
mkdir -p %{buildroot}/usr/share/bm-client-access/bin
cp -r %{_rootdir}/usr/share/bm-client-access/errors-pages %{buildroot}/usr/share/bm-client-access
cp %{_rootdir}/usr/share/bm-client-access/bin/createcert.sh %{buildroot}/usr/share/bm-client-access/bin

mkdir -p %{buildroot}/etc/bm-webmail
mkdir -p %{buildroot}/etc/bm-eas
mkdir -p %{buildroot}/etc/bm-mapi

%files
/*

%post -p /bin/bash

generateDhParam() {
    if [ -e /etc/nginx/bm_dhparam.pem ]; then
        chmod 640 /etc/nginx/bm_dhparam.pem
        return
    fi
    
    char=("-" "\\" "|" "/")
    charCount=0;
    elapsedTime=0;
    
    openssl dhparam -out /etc/nginx/bm_dhparam.pem 2048 > /dev/null 2>&1 &
    while $(kill -0 $! > /dev/null 2>&1); do
        echo -en "\rGenerate 2048 dhparams. This may take some time: "${char[$charCount]}" ("$((elapsedTime/2))"s)"
        
        elapsedTime=$((elapsedTime+1))
        charCount=$(((charCount+1)%4))
        sleep 0.5
    done
    
    chmod 640 /etc/nginx/bm_dhparam.pem
    
    echo -e "\n"
}

if [ $1 -eq 1 ]; then
    # Installation
    BM_EXTERNALURL="configure.your.external.url"    
    /usr/share/bm-client-access/bin/createcert.sh ${BM_EXTERNALURL}
fi

if [ -f "/etc/bm/bm.ini" ]; then
  externalurl=`cat /etc/bm/bm.ini | grep external-url | sed -e 's/ //g' | cut -d'=' -f2`
else
  externalurl="configure.your.external.url"
fi

echo "server_name $externalurl;" > /etc/nginx/bm-servername.conf
echo "set \$bmexternalurl $externalurl;" > /etc/nginx/bm-externalurl.conf

pushd /etc/nginx/sites-enabled
rm -f bm-client-access*
if [ -f /etc/nginx/sw.htpasswd ]; then
    cp -f ../sites-available/bm-client-access .
else
    cp -f ../sites-available/bm-client-access-without-password .
fi
popd


grep -q "^include /etc/nginx/global.d" /etc/nginx/nginx.conf || {
    echo -e "\ninclude /etc/nginx/global.d/*.conf;\n" >> /etc/nginx/nginx.conf
    service bm-cyrus-imapd stop
}

generateDhParam

echo "Install bm-webmail nginx configuration"
if [ ! -e /etc/bm-webmail/nginx-webmail.conf ]; then
  cp -f /usr/share/doc/bm-client-access/bm-webmail/nginx-webmail.conf /etc/bm-webmail
fi
if [ ! -e /etc/bm-webmail/bm-filehosting.conf ]; then
  cp -f /usr/share/doc/bm-client-access/bm-webmail/bm-filehosting.conf /etc/bm-webmail
fi

if [ ! -e /etc/bm-eas/bm-eas-nginx.conf ]; then
  echo "Install bm-eas nginx configuration"
  cp -f /usr/share/doc/bm-client-access/bm-eas/bm-eas-nginx.conf /etc/bm-eas
fi

if [ ! -e /etc/bm-eas/bm-upstream-eas.conf ]; then
  echo "Install bm-eas nginx upstream configuration"
  cp -f /usr/share/doc/bm-client-access/bm-eas/bm-upstream-eas.conf /etc/bm-eas
fi

if [ ! -e /etc/bm-mapi/bm-upstream-mapi.conf ]; then
  echo "Install bm-mapi nginx upstream configuration"
  cp -f /usr/share/doc/bm-client-access/bm-mapi/bm-upstream-mapi.conf /etc/bm-mapi
fi

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
