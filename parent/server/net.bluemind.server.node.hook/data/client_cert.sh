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
#
#END LICENSE


# generates the 2 java keystores needed to setup SSL client auth on
# vert.x
# _keystore.jks is for the client
# _truststore.jks is for the server

keytool=/usr/lib/jvm/bm-jdk/bin/keytool

cn=nodeclient
odir=/etc/bm

cat >> ${odir}/${cn}_ssl.cnf <<EOF
[ req ]
default_bits = 2048
encrypt_key = yes
distinguished_name = req_dn
x509_extensions = user_cert
prompt = no

[ req_dn ]
O=blue-mind.net
OU=bm-node
CN=${cn}

[ user_cert ]
nsCertType = client
extendedKeyUsage = clientAuth
keyUsage = digitalSignature
subjectAltName=email:${cn}@blue-mind.net

EOF

certfile=${odir}/${cn}_cert.pem
keyfile=${odir}/${cn}_key.pem
p12file=${odir}/${cn}.p12
keystorefile=${odir}/${cn}_keystore.jks
truststorefile=${odir}/${cn}_truststore.jks
bmjksfile=${odir}/bm.jks

# generate x509 client cert & key
openssl req -new -x509 -days 3650 -passout pass:password -config ${odir}/${cn}_ssl.cnf \
-text -out ${certfile} -keyout ${keyfile}

rm -f ${odir}/${cn}_ssl.cnf


# put into a p12
rm -f ${p12file}
openssl pkcs12 -passin pass:password -password pass:password -export \
-in ${certfile} -inkey ${keyfile} \
-certfile ${certfile} \
-name "${cn}" -out ${p12file}

# generate what vertx wants
rm -f ${keystorefile} ${truststorefile}

$keytool -importkeystore -srckeystore ${p12file} -srcstoretype pkcs12 \
-destkeystore ${keystorefile} \
-deststoretype JKS \
-srcstorepass password -deststorepass password
echo "Generated ${cn}_keystore.jks"

$keytool -import -trustcacerts -alias ${cn} -file ${certfile} \
-noprompt -keystore ${truststorefile} -storepass password
echo "Generated ${cn}_truststore.jks"

# ensure we have a /etc/bm/bm.jks generated from official certs
openssl pkcs12 -password pass:bluemind -export \
-in /etc/ssl/certs/bm_cert.pem -inkey /etc/ssl/certs/bm_cert.pem \
-certfile /etc/ssl/certs/bm_cert.pem \
-name "bm certificate" -out /tmp/keystore.p12

rm -f ${bmjksfile}
$keytool -importkeystore -srckeystore /tmp/keystore.p12 -srcstoretype pkcs12 \
-destkeystore ${bmjksfile} \
-deststoretype JKS \
-srcstorepass bluemind -deststorepass bluemind

rm -f /tmp/keystore.p12

chmod 400 ${certfile} ${keyfile} ${p12file} ${keystorefile} ${truststorefile} ${bmjksfile} || true
