Summary:            BlueMind maintenance tools
Name:               bm-maintenance-tools
Version:            %{_bmrelease}
Release:            0
License:            GNU Affero General Public License v3
Group:              Applications/messaging
URL:                http://www.bluemind.net/
ExcludeArch:        s390 s390x
Requires:           bm-maintenance-snzip, vim-enhanced, emacs-nox, strace, lsof, telnet, openssh-clients, nfs-utils, sysstat, mutt, util-linux-ng, curl, screen, bind-utils, jq

%description
Install used BlueMind maintenance tools

%install
cp -a %{_rootdir}/* %{buildroot}

%files
/*
