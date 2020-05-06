Name:               bm-webmail
Version:            %{_bmrelease}
Release:            0
License:            GNU Affero General Public License v3
Group:              Applications/messaging
URL:                http://www.bluemind.net/
ExcludeArch:        s390 s390x
Summary:            BlueMind webmail (Roundcube)
Requires:           bm-postgresql = 11.7-bluemind114, bm-nginx = 1.18.0-bluemind95, bm-php = 5.6.40-bluemind89, tzdata, epel-release >= 6, memcached
Conflicts:          bm-apache
Obsoletes:          bm-apache

%description
BlueMind webmail (Roundcube: http://roundcube.net/)

Need EPEL repository (http://fedoraproject.org/wiki/EPEL)

%install
cp -a %{_rootdir}/* %{buildroot}

mkdir -p %{buildroot}/var/log/bm-webmail
mkdir -p %{buildroot}/etc/bm-webmail

%files
/*

%post
chown -R www-data:www-data /usr/share/bm-webmail/temp
chown -R www-data:www-data /var/log/bm-webmail

if [ -d /usr/share/bm-webmail/logs ]; then
  rm -rf /usr/share/bm-webmail/logs
fi
if [ -L /usr/share/bm-webmail/logs ]; then
  rm -f /usr/share/bm-webmail/logs
fi

chkconfig memcached on
service memcached stop || true
cp -f /usr/share/doc/bm-webmail/sysconfig-memcached /etc/sysconfig/memcached
service memcached start

chkconfig httpd off || true
service httpd stop || true

if [ ! -e /etc/bm-webmail/bm-php5-fpm.conf ]; then
  cp -f /usr/share/doc/bm-webmail/bm-php5-fpm.conf /etc/bm-webmail
fi
cp -f /usr/share/doc/bm-webmail/bm-webmail /etc/nginx/sites-available/

pushd /etc/nginx/sites-enabled
rm -f bm-webmail
ln -s ../sites-available/bm-webmail .
popd

chkconfig bm-php-fpm on
service bm-php-fpm restart

service bm-nginx restart

%postun
if [ $1 -eq 0 ]; then
    # Uninstall
    if [ -e /etc/php-fpm.d/www.conf ]; then
      rm -rf /etc/php-fpm.d/www.conf
    fi
    
    if [ -e /etc/nginx/sites-available/bm-webmail ]; then
      rm -rf /etc/nginx/sites-available/bm-webmail
    fi
    
    if [ -e /etc/nginx/sites-enabled/bm-webmail ]; then
      rm -rf /etc/nginx/sites-enabled/bm-webmail
    fi

    if [ -e /var/log/bm-webmail ]; then
        rm -rf /var/log/bm-webmail
    fi
fi
