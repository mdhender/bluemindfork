/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.system.ldap.export.internal.conf;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.server.api.Server;

public class RhSlapdConfig extends SlapdConfig {
	public RhSlapdConfig(ItemValue<Server> server) {
		super(server);

		confPath = "/etc/openldap/slapd.d";
		schemaPath = "/etc/openldap/schema";
		varRunPath = "/var/run/openldap";
		usrLibPath = "/usr/lib64/openldap";

		sasl2Path = "/usr/lib64/sasl2";

		slapdDefaultPath = "/etc/sysconfig/slapd";
		slapdDefaultTemplate = "slapd.default.redhat";

		owner = "ldap";
		group = "ldap";
	}

}
