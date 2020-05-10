/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.server.service;

import static org.junit.Assert.fail;

import org.junit.Test;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.server.api.Server;
import net.bluemind.server.service.internal.ServerValidator;

public class ServerValidatorTest {

	private ServerValidator validator = new ServerValidator();

	@Test
	public void testNominal() {
		Server server = defaultServer();
		try {
			validator.validate(server);
		} catch (ServerFault e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testName() {
		Server server = defaultServer();
		server.name = "";
		try {
			validator.validate(server);
			fail();
		} catch (ServerFault e) {

		}

		server = defaultServer();
		server.name = null;
		try {
			validator.validate(server);
			fail();
		} catch (ServerFault e) {

		}
	}

	@Test
	public void testFqdnOrIp() {
		Server server = defaultServer();
		server.ip = "";
		server.fqdn = "";
		try {
			validator.validate(server);
			fail();
		} catch (ServerFault e) {
		}

		server = defaultServer();
		server.ip = "";
		try {
			validator.validate(server);
			fail();
		} catch (ServerFault e) {
		}

		server = defaultServer();
		server.fqdn = "";
		try {
			validator.validate(server);
		} catch (ServerFault e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private Server defaultServer() {
		Server ret = new Server();
		ret.ip = "127.0.0.1";
		ret.name = "test";
		ret.fqdn = "test.com";
		return ret;
	}
}
