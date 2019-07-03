Summary:            BlueMind python client
Name:               python-bm-client
Version:            %{_bmrelease}
Release:            0
License:            GNU Affero General Public License v3
Group:              Applications/messaging
URL:                http://www.bluemind.net/
ExcludeArch:        s390 s390x
Requires:           python-requests, python-enum34

%global __python2 /usr/bin/python2
%global python2_sitelib %(%{__python2} -c "from distutils.sysconfig import get_python_lib; print(get_python_lib())")

%description
BlueMind python client

%prep
mkdir -p %buildroot
cp -a /sources/ROOT/* %{_builddir}
sed -i -e "s/version=.*$/version='%{_bmrelease}',/" %{_builddir}/setup.py

%build
%{__python2} setup.py bdist

%install
mkdir -p %{buildroot}%{python2_sitelib}
PYTHONPATH=%{buildroot}%{python2_sitelib} %{__python2} setup.py install --skip-build --root $RPM_BUILD_ROOT
#rm %{buildroot}%{python2_sitelib}/site*

%files
%{python2_sitelib}/*
