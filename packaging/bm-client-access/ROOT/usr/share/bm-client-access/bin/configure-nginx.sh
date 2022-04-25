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

setDefaultExternalUrl() {
    local externalUrl=$(hostname -f)

    [ -f /etc/bm/bm.ini ] && externalUrl=$(grep '^[^#]*external-url' /etc/bm/bm.ini | sed -e 's/ //g' | cut -d'=' -f2)

    echo ${externalUrl}
}

createDefaultVhost() {
    local vhostFile="/etc/nginx/bluemind/bluemind-vhosts.conf"
    local externalUrl=$(setDefaultExternalUrl)

    [ ! -e /etc/ssl/certs/bm_cert.pem ] && {
        # Installation
        /usr/share/bm-client-access/bin/createcert.sh configure.your.external.url ${externalurl} $(hostname -I)
    }

    forceNginxConfiguration ${vhostFile} /usr/share/bm-client-access/conf/bluemind-vhosts.conf
    setDefaultServer ${vhostFile}
    setUseProxyProtocol ${vhostFile}
    setExternalUrl ${vhostFile} ${externalUrl}
    setSslCertFile ${vhostFile}
    setVhostExtensionDir ${vhostFile}
}

createDomainsVhosts() {
    local domainSettings="/etc/bm/domains-settings"
    [ ! -e ${domainSettings} ] && return

    local externalUrls=( $(setDefaultExternalUrl) )

    while read -r line; do
        readarray -d : -t parts <<< "${line}"
        [ ${#parts[@]} -lt 2 ] && continue

        local domainUid=${parts[0]}
        local vhostFile="/etc/nginx/bluemind/"${domainUid}"/bluemind-vhosts.conf"

        local externalUrl=${parts[1]}
        ([ -z "${externalUrl}" ] || [[ " ${externalUrls[*]} " =~ " ${externalUrl} " ]]) && continue

        externalUrls+=(${externalUrl})

        forceNginxConfiguration ${vhostFile} /usr/share/bm-client-access/conf/bluemind-vhosts.conf
        unsetDefaultServer ${vhostFile}
        setUseProxyProtocol ${vhostFile}
        setExternalUrl ${vhostFile} ${externalUrl}
        setSslCertFile ${vhostFile} ${domainUid}
        setVhostExtensionDir ${vhostFile} ${domainUid}
    done < ${domainSettings}
}

setUseProxyProtocol() {
    [ -e /etc/nginx/bm-nginx-use-proxy-protocol ] && {
        local validIpFrom=$(head -n 1 /etc/nginx/bm-nginx-use-proxy-protocol)

        sed -i -e "s/###proxy-protocol###/proxy_protocol/g" ${1}

        if [ "${validIpFrom}" != "" ]; then
            sed -i -e "s/###proxy-protocol-conf###/real_ip_header proxy_protocol;\n  set_real_ip_from ${validIpFrom};/g" ${1}
        else
            sed -i -e "s/###proxy-protocol-conf###//g" ${1}
        fi
    	return
    }

    sed -i -e "s/###proxy-protocol###//g" ${1}
    sed -i -e "s/###proxy-protocol-conf###//g" ${1}
}

setVhostExtensionDir() {
    [ ${#} -ne 2 ] && {
        sed -i -e "s|###vhost-extension-dir###|/etc/nginx/bm-local.d/*.conf|g" ${1}
        return
    }

    sed -i -e "s|###vhost-extension-dir###|/etc/nginx/bm-local.d/${2}/*.conf|g" ${1}
}

setExternalUrl() {
    sed -i -e "s/###external-url###/${2}/g" ${1}
}

setSslCertFile() {
    local defaultSslCertFilePath="/etc/ssl/certs/bm_cert.pem"
    [ ${#} -ne 2 ] && {
        sed -i -e "s|###ssl-cert-file###|${defaultSslCertFilePath}|g" ${1}
        return
    }

    local sslCertFilePath="/etc/ssl/certs/bm_cert-"${2}".pem"
    [ ! -e ${sslCertFilePath} ] && {
        sed -i -e "s|###ssl-cert-file###|${defaultSslCertFilePath}|g" ${1}
        return
    }

    sed -i -e "s|###ssl-cert-file###|${sslCertFilePath}|g" ${1}
}

setDefaultServer() {
    sed -i -e "s/###default-server###/default_server/" ${1}
}

unsetDefaultServer() {
    sed -i -e "s/###default-server###//" ${1}
}

enableVhost() {
    pushd /etc/nginx/sites-enabled 2>&1 > /dev/null
    rm -f bm-client-access*
    if [ -f /etc/bm/bm.ini ]; then
        cp -f ../sites-available/bm-client-access .
    else
        local vhostFile="bm-client-access-without-password"
        local externalUrl=$(setDefaultExternalUrl)
        cp -f "../sites-available/"${vhostFile} .
        setExternalUrl ${vhostFile} ${externalUrl}
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
[ -e /etc/nginx/bm-servername.conf ] && rm -f /etc/nginx/bm-servername.conf
[ -e /etc/nginx/bm-externalurl.conf ] && rm -f /etc/nginx/bm-externalurl.conf
[ -e /etc/nginx/bluemind ] && rm -rf /etc/nginx/bluemind

forceNginxConfiguration /etc/nginx/sites-available/bm-client-access /usr/share/bm-client-access/conf/bm-client-access
forceNginxConfiguration /etc/nginx/sites-available/bm-client-access-without-password /usr/share/bm-client-access/conf/bm-client-access-without-password

forceNginxConfiguration /etc/nginx/global.d/bm-mail-proxy.conf /usr/share/bm-client-access/conf/global.d/bm-mail-proxy.conf

nginxConfiguration /etc/bm-webmail/nginx-webmail.conf /usr/share/bm-client-access/conf/bm-webmail/nginx-webmail.conf
nginxConfiguration /etc/bm-webmail/bm-filehosting.conf /usr/share/bm-client-access/conf/bm-webmail/bm-filehosting.conf

nginxConfiguration /etc/bm-eas/bm-eas-nginx.conf /usr/share/bm-client-access/conf/bm-eas/bm-eas-nginx.conf
nginxConfiguration /etc/bm-eas/bm-upstream-eas.conf /usr/share/bm-client-access/conf/bm-eas/bm-upstream-eas.conf

nginxConfiguration /etc/bm-mapi/bm-upstream-mapi.conf /usr/share/bm-client-access/conf/bm-mapi/bm-upstream-mapi.conf

nginxConfiguration /etc/bm-hps/bm-upstream-hps.conf /usr/share/bm-client-access/conf/bm-hps/bm-upstream-hps.conf

nginxConfiguration /etc/bm-webserver/bm-upstream-webserver.conf /usr/share/bm-client-access/conf/bm-webserver/bm-upstream-webserver.conf

nginxConfiguration /etc/bm-core/bm-upstream-core.conf /usr/share/bm-client-access/conf/bm-core/bm-upstream-core.conf

nginxConfiguration /etc/bm-tick/bm-upstream-tick.conf /usr/share/bm-client-access/conf/bm-tick/bm-upstream-tick.conf

nginxConfiguration /etc/nginx/bm-nginx-role.conf /usr/share/bm-client-access/conf/bm-nginx/bm-nginx-role.conf
nginxConfiguration /etc/nginx/bm-upstream-mainnginx.conf /usr/share/bm-client-access/conf/bm-nginx/bm-upstream-mainnginx.conf
nginxConfiguration /etc/nginx/bm-http-auth.conf /usr/share/bm-client-access/conf/bm-nginx/bm-http-auth.conf
nginxConfiguration /etc/nginx/bm-nginx-embed.conf /usr/share/bm-client-access/conf/bm-nginx/bm-nginx-embed.conf

# Sentry upstream and host
nginxConfiguration /etc/nginx/bm-sentry.conf /usr/share/bm-client-access/conf/bm-nginx/bm-sentry.conf
nginxConfiguration /etc/nginx/bm-upstream-sentry.conf /usr/share/bm-client-access/conf/bm-nginx/bm-upstream-sentry.conf


generateDhParam
createDefaultVhost
createDomainsVhosts
enableVhost
