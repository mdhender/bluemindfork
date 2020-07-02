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
package net.bluemind.system.importation.commons.pool;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.bluemind.lib.ldap.LdapProtocol;
import net.bluemind.system.importation.commons.Parameters.Server;

public class TestServer extends Server {
	public List<Host> alternativeHosts = Collections.emptyList();

	public TestServer(Host host, String login, String password, LdapProtocol protocol, boolean acceptAllCertificates) {
		super(Optional.of(host), login, password, protocol, acceptAllCertificates);
	}

	@Override
	protected List<Host> getAlternativeHosts() {
		return alternativeHosts;
	}
}
