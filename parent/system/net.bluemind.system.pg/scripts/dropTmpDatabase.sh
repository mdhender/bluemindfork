#!/bin/bash
#BEGIN LICENSE
#
# Copyright © Blue Mind SAS, 2012-2022
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


if [ $# -ne 1 ]
then
        echo "Usage : <tmpdbname>"
        exit 2
fi

tempdb=$1
if [ "$tempdb" = "bj" ] || [ "$tempdb" = "bj-data" ]
then
	echo "Forbidden : Database $tempdb cannot be deleted."
	exit 2
fi


sudo -u postgres -- psql -c "SELECT pg_terminate_backend(pg_stat_activity. pid) FROM pg_stat_activity WHERE datname = '${tempdb}'"

sudo -u postgres -- psql -c "drop database if exists ${tempdb}"

exit 0

