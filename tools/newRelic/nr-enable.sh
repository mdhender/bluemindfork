#!/bin/bash

set -e

products="
bm-core
bm-hps
bm-tomcat
bm
bm-mq
ysnp
bm-locator
bm-node
bm-lmtpd
bm-ips
bm-dav
bm-xmpp
bm-milter
"

nr_pg_version="1.0.1-1"

isRedhat() {
  if [ -e /etc/redhat-release ]; then
    return 0
  else
    return 1
  fi
}

for p in $products; do
    echo "Will try for "$p
done

if [ -f nr.lic ]; then
    echo "LIC found in nr.lic"
else
    echo "LIC not found, ./nr.lic not found."
    exit 1
fi

echo "Proceeding with agent install..."

lic=`cat nr.lic`
echo "LIC is '$lic'"

if isRedhat; then
  echo -n "Installing server agent using rpms..."
  sleep 2
  rpm -Uvh https://yum.newrelic.com/pub/newrelic/el5/x86_64/newrelic-repo-5-3.noarch.rpm || true
  yum install newrelic-sysmond || true
else
  echo -n "Installing server agent using debs..."
  sleep 2
  wget -O /etc/apt/sources.list.d/newrelic.list http://download.newrelic.com/debian/newrelic.list
  apt-key adv --keyserver hkp://subkeys.pgp.net --recv-keys 548C16BF
  apt-get update || true
  apt-get install newrelic-sysmond
fi

nrsysmond-config --set license_key=${lic}
/etc/init.d/newrelic-sysmond restart
echo "Server agent installed."
sleep 2


if [ -f newrelic.yml.tpl ]; then
    echo "NR template and license found."
    fqdn=`hostname -f`
    nr=nr.yml
    cat newrelic.yml.tpl > $nr
    nrt=nr.yml.tmp
    cat $nr | sed -e "s/BM_NR_LIC/${lic}/g" > $nrt
    mv $nrt $nr
    cat $nr | sed -e "s/BM_HOST/${fqdn}/g" > $nrt
    mv $nrt $nr
    
    # enabling for java products
    for p in $products; do
      if [ -d /etc/$p ]; then
        echo -n "Enabling for product $p..."
        cat $nr | sed -e "s/BM_PRODUCT/$p/g" > $nrt
        cp $nrt /etc/$p/newrelic.yml
        echo "Done."
        if [ -f $p.jmx.yml ]; then
          cp $p.jmx.yml /etc/$p/jmx.yml
        fi
      else
        echo "Skipping for $p"
      fi
    done
    echo "Finished enabling NR agent on java products."

    bmctl restart

    # enable for webmail
    echo "Enable for php apps..."
    nr=nr.ini.yml
    cat newrelic.ini.tpl > $nr
    nrt=nr.ini.tmp
    cat $nr | sed -e "s/BM_NR_LIC/${lic}/g" > $nrt
    mv $nrt $nr
    cat $nr | sed -e "s/BM_HOST/${fqdn}/g" > $nrt
    mv $nrt $nr
    
    echo -n "Moving conf for PHP stuff..."
    if isRedhat; then
      cp $nr /etc/php.d/newrelic.ini
    else
      cp $nr /etc/php5/conf.d/newrelic.ini 
    fi
    echo "DONE."

    if isRedhat; then
      yum install newrelic-php5
    else
      apt-get install newrelic-daemon newrelic-php5-common newrelic-php5
    fi

    # enable for PG
    #tar xfz newrelic-pg-${nr_pg_version}.tar.gz
    #pushd newrelic-pg-${nr_pg_version}
    #apt-get install ruby-bundler rubygems libpq-dev
    #
    #popd

fi
