#!/bin/bash

generateDhParam() {
    if $(openssl dhparam -check -in /etc/nginx/bm_dhparam.pem -noout > /dev/null 2>&1); then
        chmod 640 /etc/nginx/bm_dhparam.pem
        return
    fi

    if [ -e /etc/nginx/bm_dhparam.pem ]; then
        rm -f /etc/nginx/bm_dhparam.pem
    fi

    char=("-" "\\" "|" "/")
    charCount=0;
    elapsedTime=0;
    
    openssl dhparam -out /etc/nginx/bm_dhparam.pem 2048 > /dev/null 2>&1 &
    opensslpid=$!
    while $(kill -0 ${opensslpid} > /dev/null 2>&1); do
        echo -en "\rGenerate 2048 dhparams. This may take some time: "${char[$charCount]}" ("$((elapsedTime/2))"s)"
        
        elapsedTime=$((elapsedTime+1))
        charCount=$(((charCount+1)%4))
        sleep 0.5
    done
    
    chmod 640 /etc/nginx/bm_dhparam.pem
    
    echo -e "\n"
}

setExternalUrl() {
    if [ -f /etc/bm/bm.ini ]; then
      externalurl=$(grep '^[^#]*external-url' /etc/bm/bm.ini | sed -e 's/ //g' | cut -d'=' -f2)
    else
      externalurl=$(hostname -f)
    fi

    if [ ! -e /etc/ssl/certs/bm_cert.pem ]; then
        # Installation
        /usr/share/bm-client-access/bin/createcert.sh configure.your.external.url ${externalurl} $(hostname -I)
    fi

    [ ! -e /etc/nginx/bm-servername.conf ] && \
        echo "server_name $externalurl;" > /etc/nginx/bm-servername.conf
    
    [ ! -e /etc/nginx/bm-externalurl.conf ] && \
        echo "set \$bmexternalurl $externalurl;" > /etc/nginx/bm-externalurl.conf
}

enableVhost() {
    pushd /etc/nginx/sites-enabled 2>&1 > /dev/null
    rm -f bm-client-access*
    if [ -f /etc/bm/bm.ini ]; then
        cp -f ../sites-available/bm-client-access .
    else
        cp -f ../sites-available/bm-client-access-without-password .
    fi
    popd  2>&1 > /dev/null
}

nginxConfiguration() {
    if [ -e $1 ]; then
        return
    fi
    
    forceNginxConfiguration $1 $2
}

forceNginxConfiguration() {
    dstDir=$(dirname $1)
    if [ ! -e ${dstDir} ]; then
        mkdir -p ${dstDir}
    fi

    echo "Installing "$1
    if [ -e $2".gz" ]; then
        zcat $2".gz" > $1
        return
    fi
    
    if [ -e $2 ]; then
        cp -f $2 $1
    fi
}

# SB-991: don't manage NGinx on edge already configured
# File /etc/nginx/BM-DONOTCONF exists only on bm-client-access install no upgrade
if [ -f /etc/bm/bm.ini ] && [ -e /etc/nginx/BM-DONOTCONF ]; then
    exit 0
fi

# If server not configured by BlueMind, ensure NGinx will be managed
rm -f /etc/nginx/BM-DONOTCONF

echo "Install/Upgrade BlueMind nginx virtual host"

[ -e /etc/nginx/bm-local.d/tick.conf ] && rm -f /etc/nginx/bm-local.d/tick.conf

forceNginxConfiguration /etc/nginx/sites-available/bm-client-access /usr/share/doc/bm-client-access/bm-client-access
forceNginxConfiguration /etc/nginx/sites-available/bm-client-access-without-password /usr/share/doc/bm-client-access/bm-client-access-without-password
forceNginxConfiguration /etc/nginx/global.d/bm-mail-proxy.conf /usr/share/doc/bm-client-access/global.d/bm-mail-proxy.conf

nginxConfiguration /etc/bm-webmail/nginx-webmail.conf /usr/share/doc/bm-client-access/bm-webmail/nginx-webmail.conf
nginxConfiguration /etc/bm-webmail/bm-filehosting.conf /usr/share/doc/bm-client-access/bm-webmail/bm-filehosting.conf

nginxConfiguration /etc/bm-eas/bm-eas-nginx.conf /usr/share/doc/bm-client-access/bm-eas/bm-eas-nginx.conf
nginxConfiguration /etc/bm-eas/bm-upstream-eas.conf /usr/share/doc/bm-client-access/bm-eas/bm-upstream-eas.conf

nginxConfiguration /etc/bm-mapi/bm-upstream-mapi.conf /usr/share/doc/bm-client-access/bm-mapi/bm-upstream-mapi.conf

nginxConfiguration /etc/bm-hps/bm-upstream-hps.conf /usr/share/doc/bm-client-access/bm-hps/bm-upstream-hps.conf

nginxConfiguration /etc/bm-webserver/bm-upstream-webserver.conf /usr/share/doc/bm-client-access/bm-webserver/bm-upstream-webserver.conf

nginxConfiguration /etc/bm-core/bm-upstream-core.conf /usr/share/doc/bm-client-access/bm-core/bm-upstream-core.conf

nginxConfiguration /etc/bm-tick/bm-upstream-tick.conf /usr/share/doc/bm-client-access/bm-tick/bm-upstream-tick.conf

nginxConfiguration /etc/nginx/bm-nginx-role.conf /usr/share/doc/bm-client-access/bm-nginx/bm-nginx-role.conf
nginxConfiguration /etc/nginx/bm-upstream-mainnginx.conf /usr/share/doc/bm-client-access/bm-nginx/bm-upstream-mainnginx.conf
nginxConfiguration /etc/nginx/bm-http-auth.conf /usr/share/doc/bm-client-access/bm-nginx/bm-http-auth.conf
nginxConfiguration /etc/nginx/bm-nginx-embed.conf /usr/share/doc/bm-client-access/bm-nginx/bm-nginx-embed.conf

generateDhParam
setExternalUrl
enableVhost
