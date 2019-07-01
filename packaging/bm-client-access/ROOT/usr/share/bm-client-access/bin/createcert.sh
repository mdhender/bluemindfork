#!/bin/bash
#BEGIN LICENSE
#
# Copyright © Blue Mind SAS, 2012-2016
#
# This file is part of BlueMind. BlueMind is a messaging and collaborative
# solution.
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of either the GNU Affero General Public License as
# published by the Free Software Foundation (version 3 of the License).
#
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
#
# See LICENSE.txt

# CA management functions

ca_root=/tmp/bm-ca
certs_out=/etc/bm/certs

days="-days 9999"
cadays="-days 10000"
REQ="openssl req"
CA="openssl ca"
CERT="openssl x509"

cakey=cakey.pem
careq=careq.pem
cacert=cacert.pem
caconf=ca.cnf
certtemplate=cert.cnf.template
cert=bm_cert.pem

createCaConf() {
  cat > ${ca_root}"/"${caconf} <<EOF
[ req ]
default_bits = 2048
encrypt_key = no
distinguished_name = req_dn
prompt = no
x509_extensions = v3_ca # The extentions to add to the self signed cert
req_extensions  = v3_req

[ req_dn ]
C=FR
ST=France
O=BlueMind
OU=BlueMind
CN=ca-bm-${computed_ca_id}.blue-mind.net

[ ca ]
default_ca      = bluemind

[ bluemind ]
dir             = ${ca_root}
certs           = \$dir/certs            
crl_dir         = \$dir/crl              
database        = \$dir/index.txt        
new_certs_dir   = \$dir/newcerts         
certificate     = \$dir/cacert.pem       
serial          = \$dir/serial           
crlnumber       = \$dir/crlnumber        
crl             = \$dir/crl.pem          
private_key     = \$dir/private/cakey.pem
default_md      = sha256
policy          = policy_match

# For the CA policy
[ policy_match ]
countryName             = match
stateOrProvinceName     = match
organizationName        = optional
organizationalUnitName  = optional
commonName              = supplied
emailAddress            = optional

[ usr_cert ]
basicConstraints=CA:FALSE
nsCertType                      = client, server, email
keyUsage = nonRepudiation, digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth, clientAuth, codeSigning, emailProtection
nsComment                       = "OpenSSL Generated Certificate"
subjectKeyIdentifier=hash
authorityKeyIdentifier=keyid,issuer

[ v3_req ]
extendedKeyUsage = serverAuth, clientAuth
basicConstraints = CA:FALSE
keyUsage = nonRepudiation, digitalSignature, keyEncipherment

[ v3_ca ]
subjectKeyIdentifier=hash
authorityKeyIdentifier=keyid:always,issuer:always
basicConstraints = CA:true
nsCertType = sslCA
EOF
}

createCertTemplate() {
  cat > ${ca_root}"/"${certtemplate} <<EOF
[ req ]
default_bits = 2048
encrypt_key = no
distinguished_name = req_dn
prompt = no
x509_extensions = v3_ca # The extentions to add to the self signed cert
req_extensions  = v3_req
x509_extensions = usr_cert

[ req_dn ]
C=FR
ST=France
O=Blue Mind
OU=Blue Mind
CN=WILDCARDCN

[ ca ]
default_ca      = bluemind

[ bluemind ]
dir             = ${ca_root}
certs           = \$dir/certs            
crl_dir         = \$dir/crl              
database        = \$dir/index.txt        
new_certs_dir   = \$dir/newcerts         
certificate     = \$dir/cacert.pem       
serial          = \$dir/serial           
crlnumber       = \$dir/crlnumber        
crl             = \$dir/crl.pem          
private_key     = \$dir/private/cakey.pem
default_md      = sha256
policy          = policy_match

# For the CA policy
[ policy_match ]
countryName             = match
stateOrProvinceName     = match
organizationName        = optional
organizationalUnitName  = optional
commonName              = supplied
emailAddress            = optional

[ usr_cert ]
basicConstraints=CA:FALSE
nsCertType                      = client, server, email
keyUsage = nonRepudiation, digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth, clientAuth, codeSigning, emailProtection
nsComment                       = "OpenSSL Generated Certificate"
subjectKeyIdentifier=hash
authorityKeyIdentifier=keyid,issuer

[ v3_req ]
extendedKeyUsage = serverAuth, clientAuth
basicConstraints = CA:FALSE
keyUsage = nonRepudiation, digitalSignature, keyEncipherment
subjectAltName = @alt_names

[alt_names]
DNS.1 = WILDCARDCN
DNS.2 = DOMAINCN

[ v3_ca ]
subjectKeyIdentifier=hash
authorityKeyIdentifier=keyid:always,issuer:always
basicConstraints = CA:true
nsCertType = sslCA
EOF
}

function newCA() {
  rm -fr ${ca_root}
  mkdir -p ${ca_root} 
  mkdir -p ${ca_root}/certs 
  mkdir -p ${ca_root}/crl 
  mkdir -p ${ca_root}/newcerts
  mkdir -p ${ca_root}/private
  echo "00" > ${ca_root}/serial
  touch ${ca_root}/index.txt

  computed_ca_id=`date +%s`

  createCaConf

  echo "Making CA certificate ..."
  $REQ -config ${ca_root}"/"${caconf} -new -keyout ${ca_root}"/private/"${cakey} \
    -out ${ca_root}/${careq}
  echo "Request for CA cert creation done."
  
  $CA -config ${ca_root}"/"${caconf} \
    -extensions v3_ca -batch ${cadays} \
    -keyfile ${ca_root}"/private/"${cakey} -selfsign \
    -in ${ca_root}"/"${careq} \
    -out ${ca_root}"/"${cacert} ${cadays}
  echo "CA made"
}

function newSignedCert() {
  createCertTemplate
  
  dots=${cn//[^\.]}
  if (( ${#dots} > 1 )); then
    IFS='.' read -r head tails <<< "${cn}"
    wildcard_cn="*."${tails}
    domain_cn=${tails}
  else
    wildcard_cn="*.${cn}"
    domain_cn=${cn}
  fi

  mkdir -p ${certs_out}
  rm -f ${certs_out}"/"${cert}
  sed -i -e "s/WILDCARDCN/${wildcard_cn}/" ${ca_root}"/"${certtemplate}
  sed -i -e "s/DOMAINCN/${domain_cn}/" ${ca_root}"/"${certtemplate}

  $REQ -config ${ca_root}"/"${certtemplate} -new -nodes -keyout ${ca_root}"/"${cn}"_pk.pem" -out ${ca_root}"/"${cn}"_req.pem" $days

  $CA -config ${ca_root}"/"${certtemplate} \
    -extensions v3_req -batch $days \
    -in ${ca_root}"/"${cn}"_req.pem" \
    -out ${certs_out}"/"${cert}

  cat ${ca_root}"/"${cn}"_pk.pem" >> ${certs_out}"/"${cert}
  $CERT -in ${ca_root}"/"${cacert} >> ${certs_out}"/"${cert}
}

test $# -eq 1 || {
  echo "usage: createcert.sh <CN>"
  exit 1
}

cn=$1

newCA
newSignedCert

ca_path="/var/lib/bm-ca"
mkdir -p ${ca_path}
cp ${ca_root}/${cacert} ${ca_path}/${cacert}
cp ${certs_out}"/"${cert} /etc/ssl/certs/bm_cert.pem

rm -rf ${ca_root}
