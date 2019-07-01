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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Type;
import net.bluemind.mailbox.service.internal.MailboxValidator;

public class MailboxValidatorTests extends AbstractMailboxServiceTests {

	private MailboxValidator validator;

	@Before
	public void before() throws Exception {
		super.before();
		validator = new MailboxValidator(new BmTestContext(SecurityContext.SYSTEM), domainUid, mailboxStore, itemStore);
	}

	@Test
	public void validate() throws ServerFault {
		Mailbox mailshare = null;
		ErrorCode err = null;
		String uid = UUID.randomUUID().toString();

		try {
			validator.validate(mailshare, uid);
		} catch (ServerFault e) {
			err = e.getCode();
		}
		assertTrue(ErrorCode.INVALID_PARAMETER == err);

		mailshare = new Mailbox();
		mailshare.type = Type.mailshare;
		err = null;
		try {
			validator.validate(mailshare, uid);
		} catch (ServerFault e) {
			err = e.getCode();
		}
		assertTrue(ErrorCode.INVALID_PARAMETER == err);

		mailshare.name = "mailshare";
		mailshare.emails = new ArrayList<Email>();
		Email email = Email.create("mailshare", true);
		mailshare.emails.add(email);

		err = null;
		try {
			validator.validate(mailshare, uid);
		} catch (ServerFault e) {
			err = e.getCode();
		}
		assertTrue(ErrorCode.INVALID_PARAMETER == err);

		mailshare.emails = new ArrayList<Email>();
		email = Email.create("mailshare@bm.lan", true);
		mailshare.emails.add(email);

		err = null;
		try {
			validator.validate(mailshare, uid);
			fail();
		} catch (ServerFault e) {
		}

		mailshare.routing = Mailbox.Routing.internal;
		mailshare.dataLocation = imapServer.address();
		validator.validate(mailshare, uid);
	}

	@Test
	public void emailAlreadyUsed() throws ServerFault, SQLException {

		Email email = Email.create("mailshare@bm.lan", true);

		Mailbox mailshare = new Mailbox();
		mailshare.type = Type.mailshare;
		mailshare.routing = Mailbox.Routing.internal;
		mailshare.dataLocation = imapServer.address();
		mailshare.name = "mailshare";
		mailshare.emails = new ArrayList<Email>();
		mailshare.emails.add(email);

		itemStore.create(Item.create("mailshare@bm.lan", null));
		Item item = itemStore.get("mailshare@bm.lan");
		mailboxStore.create(item, mailshare);

		itemStore.create(Item.create("mailshare2@bm.lan", null));
		Item item2 = itemStore.get("mailshare2@bm.lan");
		mailshare.name = "mailshare2";

		ErrorCode err = null;
		try {
			validator.validate(mailshare, item2.uid);
			fail("Test must thrown an exception");
		} catch (ServerFault e) {
			err = e.getCode();
		}
		assertTrue(ErrorCode.ALREADY_EXISTS == err);
	}

	@Test
	public void duplicateName() throws ServerFault, SQLException {
		Mailbox mailshare = new Mailbox();
		mailshare.type = Type.mailshare;
		mailshare.routing = Mailbox.Routing.internal;
		mailshare.dataLocation = imapServer.address();

		ErrorCode err = null;
		String uid = UUID.randomUUID().toString();

		mailshare.name = "mailshare";
		mailshare.emails = new ArrayList<Email>();
		itemStore.create(Item.create("mailshare@bm.lan", null));
		Item item = itemStore.get("mailshare@bm.lan");
		mailboxStore.create(item, mailshare);
		mailshare.name = "mailshare";

		try {
			validator.validate(mailshare, uid);
		} catch (ServerFault e) {
			err = e.getCode();
		}
		assertTrue(ErrorCode.ALREADY_EXISTS == err);

	}

	@Test
	public void nullDataLocation() {
		String uid = UUID.randomUUID().toString();
		Mailbox mailbox = defaultMailbox();

		try {
			validator.validate(mailbox, uid);
			fail("Test must trhown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage().endsWith("must be set"));
		}
	}

	private Mailbox defaultMailbox() {
		Mailbox mailbox = new Mailbox();
		mailbox.name = "mailshare";
		mailbox.type = Mailbox.Type.user;
		mailbox.dataLocation = null;
		mailbox.routing = Mailbox.Routing.internal;
		return mailbox;
	}

	@Test
	public void emptyDataLocation() {
		String uid = UUID.randomUUID().toString();
		Mailbox mailbox = defaultMailbox();
		mailbox.dataLocation = "";

		try {
			validator.validate(mailbox, uid);
			fail("Test must trhown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage().endsWith("must be set"));
		}
	}

	@Test
	public void blankDataLocation() {
		String uid = UUID.randomUUID().toString();
		Mailbox mailbox = defaultMailbox();
		mailbox.dataLocation = "   ";

		try {
			validator.validate(mailbox, uid);
			fail("Test must trhown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage().endsWith("must be set"));
		}
	}

	@Test
	public void inexistantDataLocation() {
		String uid = UUID.randomUUID().toString();
		Mailbox mailbox = defaultMailbox();
		mailbox.dataLocation = "inexistant.server.tld";

		try {
			validator.validate(mailbox, uid);
			fail("Test must trhown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage().endsWith(mailbox.dataLocation + " must exist"));
		}
	}

	@Test
	public void notAssignedDataLocation() {
		String uid = UUID.randomUUID().toString();
		Mailbox mailbox = defaultMailbox();
		mailbox.dataLocation = imapServerNotAssigned.address();

		try {
			validator.validate(mailbox, uid);
			fail("Test must trhown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage()
					.endsWith(mailbox.dataLocation + " not assigned to: " + domainUid + " as mail/imap"));
		}
	}

	@Test
	public void assignedButNotMailImapDataLocation() {
		String uid = UUID.randomUUID().toString();
		Mailbox mailbox = defaultMailbox();
		mailbox.dataLocation = smtpServer.address();

		try {
			validator.validate(mailbox, uid);
			fail("Test must trhown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage()
					.endsWith(mailbox.dataLocation + " not assigned to: " + domainUid + " as mail/imap"));
		}
	}

	@Override
	protected IMailboxes getService(SecurityContext context) throws ServerFault {
		return null;
	}

	@Test
	public void duplicateEmail() throws ServerFault {
		String uid = UUID.randomUUID().toString();
		Email email1 = Email.create("mailshare@bm.lan", true);
		Email email2 = Email.create("mailshare@bm.lan", false);

		Mailbox mailshare = new Mailbox();
		mailshare.type = Type.mailshare;
		mailshare.routing = Mailbox.Routing.internal;
		mailshare.dataLocation = imapServer.address();
		mailshare.name = "mailshare";
		mailshare.emails = new ArrayList<Email>();
		mailshare.emails.add(email1);
		mailshare.emails.add(email2);

		try {
			validator.validate(mailshare, uid);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("mailshare@bm.lan is duplicate", sf.getMessage());
			assertEquals(ErrorCode.ALREADY_EXISTS, sf.getCode());
		}
	}

	@Test
	public void moreThanOneDefaultEmail() throws ServerFault {
		String uid = UUID.randomUUID().toString();
		Email email1 = Email.create("mailshare1@bm.lan", true);
		Email email2 = Email.create("mailshare2@bm.lan", true);

		Mailbox mailshare = new Mailbox();
		mailshare.type = Type.mailshare;
		mailshare.routing = Mailbox.Routing.internal;
		mailshare.dataLocation = imapServer.address();
		mailshare.name = "mailshare";
		mailshare.emails = new ArrayList<Email>();
		mailshare.emails.add(email1);
		mailshare.emails.add(email2);

		try {
			validator.validate(mailshare, uid);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("There is more than one default address (at least mailshare2@bm.lan and mailshare1@bm.lan)",
					sf.getMessage());
			assertEquals(ErrorCode.ALREADY_EXISTS, sf.getCode());
		}
	}

	@Test
	public void duplicateLeftPart() throws ServerFault {
		String uid = UUID.randomUUID().toString();
		Email email1 = Email.create("mailshare1@bm.lan", true, false);
		Email email2 = Email.create("mailshare1@bmalias1.lan", false, false);
		Email email3 = Email.create("mailshare1@bmalias2.lan", false, true);

		Mailbox mailshare = new Mailbox();
		mailshare.type = Type.mailshare;
		mailshare.routing = Mailbox.Routing.internal;
		mailshare.dataLocation = imapServer.address();
		mailshare.name = "mailshare";
		mailshare.emails = new ArrayList<Email>();
		mailshare.emails.add(email1);
		mailshare.emails.add(email2);
		mailshare.emails.add(email3);

		try {
			validator.validate(mailshare, uid);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("mailshare1@bmalias2.lan is duplicate", sf.getMessage());
			assertEquals(ErrorCode.ALREADY_EXISTS, sf.getCode());
		}
	}

	@Test
	public void emptyEmailList() {

		Mailbox mailshare = new Mailbox();
		mailshare.type = Type.user;
		mailshare.routing = Mailbox.Routing.internal;
		mailshare.dataLocation = imapServer.address();
		mailshare.name = "mailshare";
		mailshare.emails = new ArrayList<Email>();
		try {
			validator.validate(mailshare, UUID.randomUUID().toString());
			fail("routing != none + empty email list should not be valid");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void emptyEmailListRoutingNone() {
		Mailbox mailshare = new Mailbox();
		mailshare.type = Type.user;
		mailshare.routing = Mailbox.Routing.none;
		mailshare.dataLocation = imapServer.address();
		mailshare.name = "mailshare";
		mailshare.emails = new ArrayList<Email>();

		try {
			validator.validate(mailshare, UUID.randomUUID().toString());
		} catch (ServerFault sf) {
			fail("routing none + empty email list should be valid");
		}
	}

	@Test
	public void mailshareEmptyEmailListIsValid() {

		Mailbox mailshare = new Mailbox();
		mailshare.type = Type.mailshare;
		mailshare.routing = Mailbox.Routing.internal;
		mailshare.dataLocation = imapServer.address();
		mailshare.name = "mailshare";
		mailshare.emails = new ArrayList<Email>();
		try {
			validator.validate(mailshare, UUID.randomUUID().toString());
		} catch (ServerFault sf) {
			fail("routing none + mailshare + empty email list should be valid");
		}
	}

	@Test
	public void validate_ExternalRouting_NoSplitDomain() {
		Mailbox mbox = new Mailbox();
		mbox.type = Type.mailshare;
		mbox.routing = Mailbox.Routing.external;
		mbox.dataLocation = imapServer.address();
		mbox.name = "mailshare";
		try {
			validator.validate(mbox, UUID.randomUUID().toString());
			fail("routing external + no split is not valid");
		} catch (ServerFault sf) {
		}
	}

	@Test
	public void validate_ExternalRouting_SplitDomain() {
		Mailbox mbox = new Mailbox();
		mbox.type = Type.mailshare;
		mbox.routing = Mailbox.Routing.external;
		mbox.dataLocation = imapServer.address();
		mbox.name = "mailshare";

		IDomainSettings domSettingsService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid);
		Map<String, String> settings = domSettingsService.get();
		settings.put(DomainSettingsKeys.mail_routing_relay.name(), "osef");
		domSettingsService.set(settings);

		try {
			validator.validate(mbox, UUID.randomUUID().toString());
		} catch (ServerFault sf) {
			fail("routing external + no split is be valid");
		}
	}

	@Test
	public void validate_Quota_NoUserMaxQuota() {
		Mailbox mbox = new Mailbox();
		mbox.type = Type.user;
		mbox.routing = Mailbox.Routing.none;
		mbox.dataLocation = imapServer.address();
		mbox.emails = new ArrayList<Email>();
		mbox.name = "bang";
		mbox.quota = 31;

		try {
			validator.validate(mbox, UUID.randomUUID().toString());
		} catch (ServerFault sf) {
			sf.printStackTrace();
			fail("quota is valid");
		}
	}

	@Test
	public void validate_Quota_UserMaxQuota() {
		IDomainSettings domSettingsService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid);
		Map<String, String> settings = domSettingsService.get();
		settings.put(DomainSettingsKeys.mailbox_max_user_quota.name(), "42");
		domSettingsService.set(settings);

		Mailbox mbox = new Mailbox();
		mbox.type = Type.user;
		mbox.routing = Mailbox.Routing.none;
		mbox.dataLocation = imapServer.address();
		mbox.emails = new ArrayList<Email>();
		mbox.name = "bang";
		mbox.quota = 31;

		try {
			validator.validate(mbox, UUID.randomUUID().toString());
		} catch (ServerFault sf) {
			fail("quota is valid");
		}
	}

	@Test
	public void validate_Quota_UserMaxQuota_Reached() {
		IDomainSettings domSettingsService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid);
		Map<String, String> settings = domSettingsService.get();
		settings.put(DomainSettingsKeys.mailbox_max_user_quota.name(), "31");
		domSettingsService.set(settings);

		Mailbox mbox = new Mailbox();
		mbox.type = Type.user;
		mbox.routing = Mailbox.Routing.none;
		mbox.dataLocation = imapServer.address();
		mbox.emails = new ArrayList<Email>();
		mbox.name = "bang";
		mbox.quota = 42;
		try {
			validator.validate(mbox, UUID.randomUUID().toString());
			fail("max quota is reached");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}
}
