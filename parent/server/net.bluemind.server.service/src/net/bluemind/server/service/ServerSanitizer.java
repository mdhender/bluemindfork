/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.server.service;

import java.util.ArrayList;
import java.util.HashSet;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.sanitizer.ISanitizer;
import net.bluemind.server.api.Server;

public class ServerSanitizer implements ISanitizer<Server> {

	@Override
	public void create(Server obj) throws ServerFault {
		sanitize(obj);
	}

	@Override
	public void update(Server current, Server obj) throws ServerFault {
		sanitize(obj);
	}

	private void sanitize(Server srv) {
		if (srv.ip != null && srv.ip.trim().isEmpty()) {
			srv.ip = null;
		}
		if (srv.fqdn != null && srv.fqdn.trim().isEmpty()) {
			srv.fqdn = null;
		}

		if (srv.tags.contains("mail/imap")) {
			srv.tags.add("bm/pgsql-data");
		} else if (srv.tags.contains("bm/pgsql-data")) {
			srv.tags.add("mail/imap");
		}
		srv.tags = new ArrayList<String>(new HashSet<>(srv.tags)); 
	}

}
