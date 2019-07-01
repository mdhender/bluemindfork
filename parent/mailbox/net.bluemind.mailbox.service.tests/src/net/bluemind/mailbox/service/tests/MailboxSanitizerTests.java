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
package net.bluemind.mailbox.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.service.internal.MailboxSanitizer;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class MailboxSanitizerTests {

	private MailboxSanitizer sanitizer;
	private String domainUid;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		domainUid = "bm.lan";

		Item i = Item.create("bm.lan", null);
		Domain d = new Domain();
		d.name = domainUid;
		ItemValue<Domain> dom = ItemValue.create(i, d);
		sanitizer = new MailboxSanitizer(dom);

		PopulateHelper.initGlobalVirt();

		PopulateHelper.createTestDomain(domainUid);

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testSanitize() {
		try {
			Mailbox mailshare = new Mailbox();
			mailshare.name = "mailshare";
			mailshare.emails = new LinkedList<Email>();
			Email email = Email.create(" ADmIN@Bm.LAN ", true);
			mailshare.emails.add(email);
			email = Email.create(" ROOT@Bm.LAN ", true);
			mailshare.emails.add(email);
			sanitizer.sanitize(mailshare);

			Iterator<Email> iterator = mailshare.emails.iterator();
			email = iterator.next();
			assertEquals("admin@bm.lan", email.address);
			email = iterator.next();
			assertEquals("root@bm.lan", email.address);

			sanitizer.sanitize(mailshare);
		} catch (ServerFault e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testQuota_MailshareQuotaNotSet_DefaultQuotaNotSet() {
		Mailbox mbox = mailbox();

		sanitizer.sanitize(mbox);

		assertNull(mbox.quota);
	}

	@Test
	public void testQuota_MailshareSet_DefaultQuotaNotSet() {
		Mailbox mbox = mailbox();
		mbox.quota = 42;

		sanitizer.sanitize(mbox);

		assertEquals(42, mbox.quota.intValue());
	}

	@Test
	public void testQuota_MailshareNotSet_DefaultQuotaSet() {
		Mailbox mbox = mailbox();

		IDomainSettings domSettingsService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid);
		Map<String, String> settings = domSettingsService.get();
		settings.put(DomainSettingsKeys.mailbox_default_publicfolder_quota.name(), "31");
		domSettingsService.set(settings);

		sanitizer.sanitize(mbox);

		assertEquals(31, mbox.quota.intValue());
	}

	@Test
	public void testQuota_MailshareSet_DefaultQuotaSet() {
		Mailbox mbox = mailbox();
		mbox.quota = 42;

		IDomainSettings domSettingsService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid);
		Map<String, String> settings = domSettingsService.get();
		settings.put(DomainSettingsKeys.mailbox_default_publicfolder_quota.name(), "31");
		domSettingsService.set(settings);

		sanitizer.sanitize(mbox);

		assertEquals(42, mbox.quota.intValue());
	}

	private Mailbox mailbox() {
		Mailbox mbox = new Mailbox();
		mbox.type = Mailbox.Type.mailshare;
		mbox.name = "mbox";
		mbox.emails = new LinkedList<Email>();
		return mbox;
	}

}
