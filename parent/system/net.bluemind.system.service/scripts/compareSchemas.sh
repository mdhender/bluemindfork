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

db=$1
tempdb=$2
output=$3

if [ $# -eq 0 ]
then
	echo "Usage : <dbname> <tmpdbname> <output_comparison_path>"
	exit 2
fi

sudo -u postgres bash -c "psql -lqt | cut -d \\| -f 1 | grep -w ${tempdb}"

checkDbCreated=$(echo $?)

if [ $checkDbCreated -eq 0 ] 
then
	echo "launch migra comparison between ${tempdb} and ${db}"
	sudo -u postgres bash -c "migra --unsafe postgresql:///${db} postgresql:///${tempdb} > ${output}"

	if [ -f "${output}" ]
	then
		sudo -u postgres -- psql -c "drop database if exists ${tempdb}"
		exit 0
	fi
else
	echo "NOT launch migra"
fi

exit 2

