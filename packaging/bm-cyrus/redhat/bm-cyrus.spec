Name:               bm-cyrus
Version:            %{_bmrelease}
Release:            0
License:            GNU Affero General Public License v3
Group:              Applications/messaging
URL:                http://www.blue-mind.net/
ExcludeArch:        s390 s390x
Summary:            BlueMind Cyrus / SASL
Requires:           bm-conf = %{version}-%{release}, bm-cyrus-imapd = 1:3.0.13-bluemind334, cyrus-sasl, cyrus-sasl-plain, bm-ysnp = %{version}-%{release}, bm-sds-proxy = %{version}-%{release}
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
