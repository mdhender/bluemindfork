Name:               bm-cyrus
Version:            %{_bmrelease}
Release:            0
License:            GNU Affero General Public License v3
Group:              Applications/messaging
URL:                http://www.blue-mind.net/
ExcludeArch:        s390 s390x
Summary:            BlueMind Cyrus / SASL
Requires:           bm-conf = %{version}-%{release}, bm-cyrus-imapd = 1:3.0.8-bluemind172, cyrus-sasl, cyrus-sasl-plain, bm-ysnp = %{version}-%{release}
Conflicts:          bm-imap-i18n
Obsoletes:          bm-imap-i18n

%description
Configuration of Cyrus and SASL

%prep
rm -rf %{buildroot}/*

%build

%install
cp -a %{_rootdir}/usr %{buildroot}

%files
/*

%post
if [ $1 -eq 1 ]; then
    # Installation
    if [ ! -e /var/lib/cyrus ]; then
        ln -s /var/lib/imap /var/lib/cyrus
    fi

    if [ ! -e /var/spool/cyrus ]; then
        ln -s /var/spool/imap /var/spool/cyrus
    fi

    if [ ! -e /var/spool/cyrus/meta/mail ]; then
        mkdir -p /var/spool/cyrus/meta/mail
        chown -R cyrus:mail /var/spool/cyrus/meta
        chmod -R 700 /var/spool/cyrus/meta
    fi
    
    if [ ! -e /var/spool/cyrus/data/mail ]; then
        mkdir -p /var/spool/cyrus/data/mail
        chown -R cyrus:mail /var/spool/cyrus/data
        chmod -R 700 /var/spool/cyrus/data
    fi

    if [ ! -e /var/spool/cyrus/news ]; then
        mkdir -p /var/spool/cyrus/news
        chown cyrus:mail /var/spool/cyrus/news
        chmod 700 /var/spool/cyrus/news
    fi

    if [ ! -e /var/spool/news ]; then
        mkdir -p /var/spool/news
        chown cyrus:mail /var/spool/news
        chmod 700 /var/spool/news
    fi

    if [ ! -e /var/spool/sieve ]; then
        mkdir -p /var/spool/sieve
        chown cyrus:mail /var/spool/sieve
        chmod 700 /var/spool/sieve
    fi

    if [ ! -e /var/run/cyrus ]; then
        mkdir -p /var/run/cyrus
        chown cyrus:mail /var/run/cyrus
        ln -s /var/lib/imap/socket /var/run/cyrus/socket
    fi
fi

chkconfig bm-cyrus-imapd on
service bm-cyrus-imapd restart

%postun
if [ $1 -eq 0 ]; then
    # Uninstall
    if [ -e /var/lib/cyrus ]; then
        rm -f /var/lib/cyrus
    fi

    if [ -e /var/spool/cyrus/mail ]; then
        rm -rf /var/spool/cyrus/mail
    fi

    if [ -e /var/spool/cyrus/news ]; then
        rm -rf /var/spool/cyrus/news
    fi

    if [ -e /var/spool/cyrus ]; then
        rm -f /var/spool/cyrus
    fi

    if [ -e /var/spool/news ]; then
        rm -rf /var/spool/news
    fi

    if [ -e /var/spool/sieve ]; then
        rm -rf /var/spool/sieve
    fi

    if [ -e /var/run/cyrus ]; then
        rm -f /var/run/cyrus/*
        rm -rf /var/run/cyrus
    fi
fi
