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


openssl pkcs12 -password pass:bluemind -export \
-in /etc/ssl/certs/bm_cert.pem -inkey /etc/ssl/certs/bm_cert.pem \
-certfile /etc/ssl/certs/bm_cert.pem \
-name "bm certificate" -out /root/keystore.p12

rm -f /etc/bm-ips/bm.jks

/usr/lib/jvm/bm-jdk/bin/keytool -importkeystore -srckeystore /root/keystore.p12 -srcstoretype pkcs12 \
-destkeystore /etc/bm-ips/bm.jks \
-deststoretype JKS \
-srcstorepass bluemind -deststorepass bluemind

rm -f /root/keystore.p12
