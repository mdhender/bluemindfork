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

package net.bluemind.backend.postfix;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.Email;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.tests.vertx.VertxEventChecker;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox;

/**
 * Test with mail/smtp assigned only
 * 
 * @author Anthony Prades <anthony.prades@blue-mind.net>
 *
 */
public class MailboxHookTests {
	@Before
	public void before() throws Exception {
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
	}

	@Test
	public void onMailboxCreated() {
		VertxEventChecker<JsonObject> dirtyMapChecker = new VertxEventChecker<>("postfix.map.dirty");

		Mailbox mailbox = new Mailbox();
		mailbox.name = "name";

		new MailboxHook().onMailboxCreated(null, null, ItemValue.create(mailbox.name, mailbox));

		assertNotNull(dirtyMapChecker.shouldSuccess());
	}

	@Test
	public void onMailboxUpdated_noUpdate() {
		VertxEventChecker<JsonObject> dirtyMapChecker = new VertxEventChecker<>("postfix.map.dirty");

		Mailbox mailbox = new Mailbox();
		mailbox.name = "name";
		mailbox.type = Mailbox.Type.group;
		mailbox.emails = new ArrayList<>(Arrays.asList(Email.create("email1@domain.tld", true)));

		new MailboxHook().onMailboxUpdated(null, null, ItemValue.create(mailbox.name, mailbox),
				ItemValue.create(mailbox.name, mailbox));

		dirtyMapChecker.shouldFail();
	}

	@Test
	public void onMailboxUpdated_update() {
		VertxEventChecker<JsonObject> dirtyMapChecker = new VertxEventChecker<>("postfix.map.dirty");

		Mailbox mailbox1 = new Mailbox();
		mailbox1.name = "name";
		mailbox1.type = Mailbox.Type.group;
		mailbox1.emails = new ArrayList<>(Arrays.asList(Email.create("email1@domain.tld", true)));

		Mailbox mailbox2 = new Mailbox();
		mailbox2.name = "name";
		mailbox2.type = Mailbox.Type.group;
		mailbox2.emails = new ArrayList<>(Arrays.asList(Email.create("email2@domain.tld", true)));

		new MailboxHook().onMailboxUpdated(null, null, ItemValue.create(mailbox1.name, mailbox1),
				ItemValue.create(mailbox2.name, mailbox2));

		assertNotNull(dirtyMapChecker.shouldSuccess());
	}

	@Test
	public void onMailboxDeleted() {
		VertxEventChecker<JsonObject> dirtyMapChecker = new VertxEventChecker<>("postfix.map.dirty");

		Mailbox mailbox = new Mailbox();
		mailbox.name = "name";
		mailbox.type = Mailbox.Type.group;

		new MailboxHook().onMailboxDeleted(null, null, ItemValue.create(mailbox.name, mailbox));

		dirtyMapChecker.shouldFail();
	}
}
