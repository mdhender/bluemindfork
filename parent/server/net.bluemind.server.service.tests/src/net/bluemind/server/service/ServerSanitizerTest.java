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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.server.api.Server;

public class ServerSanitizerTest {

	private ServerSanitizer sanitizer = new ServerSanitizer();

	@Test
	public void emptyIp() {
		Server srv = new Server();
		srv.ip = " ";

		sanitizer.create(srv);

		assertNull(srv.ip);
	}

	@Test
	public void emptyFqdn() {
		Server srv = new Server();
		srv.fqdn = " ";

		sanitizer.create(srv);

		assertNull(srv.fqdn);
	}

	@Test
	public void tagMailPgData() {
		Server srv = new Server();
		srv.tags = Lists.newArrayList("mail/imap");
		sanitizer.create(srv);
		assertTrue(srv.tags.contains("bm/pgsql-data"));

		srv = new Server();
		srv.tags = Lists.newArrayList("bm/pgsql-data");
		sanitizer.create(srv);
		assertTrue(srv.tags.contains("mail/imap"));
	}

}
