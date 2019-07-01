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


echo "Repairing quotas for domain $1..."

if [ -f /usr/bin/cyrquota ]; then
    su cyrus -c "/usr/bin/cyrquota -d $1 -f"
fi

if [ -f /usr/lib/cyrus/bin/quota ]; then
    su cyrus -c "/usr/lib/cyrus/bin/quota -d $1 -f"
fi

