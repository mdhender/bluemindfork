/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.pop3.endpoint.tests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.pop3.endpoint.MailboxConnection;
import net.bluemind.pop3.endpoint.PopDriver;

public class MockDriver implements PopDriver {

	private static final Logger logger = LoggerFactory.getLogger(MockDriver.class);

	@Override
	public MailboxConnection connect(String login, String password) {
		logger.info("Connect {} / {}", login, password);
		return new MockConnection(login);
	}

}
