Name:               bm-full
Version:            %{_bmrelease}
Release:            0
License:            GNU Affero General Public License v3
Group:              Applications/messaging
URL:                http://www.bluemind.net/
ExcludeArch:        s390 s390x
Summary:            Install a full BlueMind
Requires:           bm-core = %{version}-%{release}, bm-postgresql = 14.5-bluemind199, bm-admin-console = %{version}-%{release}, bm-eas = %{version}-%{release}, bm-conf = %{version}-%{release}, bm-postfix = %{version}-%{release}, bm-cyrus = %{version}-%{release}, bm-webserver = %{version}-%{release}, bm-wizard = %{version}-%{release}, bm-lmtpd = %{version}-%{release}, bm-calendar = %{version}-%{release}, bm-node = %{version}-%{release}, bm-contact = %{version}-%{release}, bm-settings = %{version}-%{release}, bm-im = %{version}-%{release}, bm-xmpp = %{version}-%{release}, bm-hps = %{version}-%{release}, bm-default-app = %{version}-%{release}, bm-client-access = %{version}-%{release}, bm-webmail = %{version}-%{release}, bm-elasticsearch = 1:7.17.5-bluemind103, bm-autodiscover = %{version}-%{release}, bm-push = %{version}-%{release}, bm-todolist = %{version}-%{release}, bm-chooser = %{version}-%{release}, bm-plugin-core-cti = %{version}-%{release}, bm-plugin-webserver-cti = %{version}-%{release}, bm-plugin-webserver-dav = %{version}-%{release}, bm-tick-full = %{version}-%{release}, bm-tick-node = %{version}-%{release}, bm-videoconferencing-saas-app = %{version}-%{release}, bm-plugin-core-videoconferencing-bluemind = %{version}-%{release}, bm-plugin-admin-console-videoconferencing-bluemind = %{version}-%{release}, bm-mail-app = %{version}-%{release}, bm-plugin-core-mailapp = %{version}-%{release}
Conflicts:          bm-cas-role
Obsoletes:          bm-cas-role

%description
BlueMind brings a new vision for mail, calendar and contact sharing by adding collaborative
tools using new technologies:
    * web 2.0
    * offline mode
    * web services
    * plugins
    * ...

For a great Blue-Mind experience

%global _curdir %_topdir/..
%global _initrddir /etc/rc.d/init.d

%prep
rm -rf %{buildroot}/*

%files


%package -n bm-autodiscover
Summary: Virtual package for autodiscover
Requires: bm-node

%description -n bm-autodiscover
Empty package. Autodiscover was moved to bm-mapi server

%files -n bm-autodiscover
