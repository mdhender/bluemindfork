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


list_appli="/usr/share/bm-tomcat/applis"
tomcat_register_path="/usr/share/tomcat/conf/Catalina/localhost"

rm -f ${tomcat_register_path}/*
if [ -d ${list_appli} ]; then
    pushd ${tomcat_register_path} > /dev/null
    for i in `ls ${list_appli}/*xml`; do
      echo "publish $i into BM tomcat server..."
      ln -s $i .
    done
    popd > /dev/null
fi

service bm-tomcat restart
