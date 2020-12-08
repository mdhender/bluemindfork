/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.mailbox.service.internal.repair;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.StoreClient;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailshare.api.IMailshare;
import net.bluemind.mailshare.api.Mailshare;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class MailboxExistsMaintenanceOperationTests extends AbstractRepairTests {
	@Test
	public void noneUser() throws IMAPException {
		String userUid = UUID.randomUUID().toString();

		User user = defaultUser("test-" + System.currentTimeMillis());
		user.routing = Routing.none;

		getProvider(SecurityContext.SYSTEM).instance(IUser.class, domainUid).create(userUid, user);

		try (StoreClient sc = new StoreClient(imapServer.address(), 1143, "admin0", "password")) {
			assertTrue(sc.login());
			assertTrue(sc.isExist("user/" + user.login + "@" + domainUid));
			sc.deleteMailbox("user/" + user.login + "@" + domainUid);
			assertFalse(sc.isExist("user/" + user.login + "@" + domainUid));
		}

		DirEntry dirEntry = getProvider(SecurityContext.SYSTEM).instance(IDirectory.class, domainUid)
				.findByEntryUid(userUid);
		assertNotNull(dirEntry);

		new MailboxExistsMaintenanceOperation(new BmTestContext(SecurityContext.SYSTEM)).repair(domainUid, dirEntry,
				DiagnosticReport.create(), new TestMonitor());
		try (StoreClient sc = new StoreClient(imapServer.address(), 1143, "admin0", "password")) {
			assertTrue(sc.login());
			assertTrue(sc.isExist("user/" + user.login + "@" + domainUid));
		}
	}

	@Test
	public void internalUser() throws IMAPException {
		String userUid = UUID.randomUUID().toString();

		User user = defaultUser("test-" + System.currentTimeMillis());
		user.routing = Routing.internal;

		getProvider(SecurityContext.SYSTEM).instance(IUser.class, domainUid).create(userUid, user);

		try (StoreClient sc = new StoreClient(imapServer.address(), 1143, "admin0", "password")) {
			assertTrue(sc.login());
			assertTrue(sc.isExist("user/" + user.login + "@" + domainUid));
			sc.deleteMailbox("user/" + user.login + "@" + domainUid);
			assertFalse(sc.isExist("user/" + user.login + "@" + domainUid));
		}

		DirEntry dirEntry = getProvider(SecurityContext.SYSTEM).instance(IDirectory.class, domainUid)
				.findByEntryUid(userUid);
		assertNotNull(dirEntry);

		new MailboxExistsMaintenanceOperation(new BmTestContext(SecurityContext.SYSTEM)).repair(domainUid, dirEntry,
				DiagnosticReport.create(), new TestMonitor());
		try (StoreClient sc = new StoreClient(imapServer.address(), 1143, "admin0", "password")) {
			assertTrue(sc.login());
			assertTrue(sc.isExist("user/" + user.login + "@" + domainUid));
		}
	}

	@Test
	public void externalUser() throws IMAPException {
		Map<String, String> domainSetting = getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid).get();
		domainSetting.put(DomainSettingsKeys.mail_routing_relay.name(), "split.domain.tld");
		getProvider(SecurityContext.SYSTEM).instance(IDomainSettings.class, domainUid).set(domainSetting);

		String userUid = UUID.randomUUID().toString();

		User user = defaultUser("test-" + System.currentTimeMillis());
		user.routing = Routing.external;

		getProvider(SecurityContext.SYSTEM).instance(IUser.class, domainUid).create(userUid, user);

		try (StoreClient sc = new StoreClient(imapServer.address(), 1143, "admin0", "password")) {
			assertTrue(sc.login());
			assertTrue(sc.isExist("user/" + user.login + "@" + domainUid));
			sc.deleteMailbox("user/" + user.login + "@" + domainUid);
			assertFalse(sc.isExist("user/" + user.login + "@" + domainUid));
		}

		DirEntry dirEntry = getProvider(SecurityContext.SYSTEM).instance(IDirectory.class, domainUid)
				.findByEntryUid(userUid);
		assertNotNull(dirEntry);

		new MailboxExistsMaintenanceOperation(new BmTestContext(SecurityContext.SYSTEM)).repair(domainUid, dirEntry,
				DiagnosticReport.create(), new TestMonitor());
		try (StoreClient sc = new StoreClient(imapServer.address(), 1143, "admin0", "password")) {
			assertTrue(sc.login());
			assertTrue(sc.isExist("user/" + user.login + "@" + domainUid));
		}
	}

	@Test
	public void noneMailshare() throws IMAPException, SQLException {
		String mailshareUid = UUID.randomUUID().toString();

		Mailshare mailshare = defaultMailshare("test-" + System.currentTimeMillis());
		mailshare.routing = Routing.none;

		getProvider(SecurityContext.SYSTEM).instance(IMailshare.class, domainUid).create(mailshareUid, mailshare);

		try (StoreClient sc = new StoreClient(imapServer.address(), 1143, "admin0", "password")) {
			assertTrue(sc.login());
			assertTrue(sc.isExist(mailshare.name + "@" + domainUid));
			sc.deleteMailbox(mailshare.name + "@" + domainUid);
			assertFalse(sc.isExist(mailshare.name + "@" + domainUid));
		}

		DirEntry dirEntry = getProvider(SecurityContext.SYSTEM).instance(IDirectory.class, domainUid)
				.findByEntryUid(mailshareUid);
		assertNotNull(dirEntry);

		new MailboxExistsMaintenanceOperation(new BmTestContext(SecurityContext.SYSTEM)).repair(domainUid, dirEntry,
				DiagnosticReport.create(), new TestMonitor());
		try (StoreClient sc = new StoreClient(imapServer.address(), 1143, "admin0", "password")) {
			assertTrue(sc.login());
			assertTrue(sc.isExist(mailshare.name + "@" + domainUid));
		}
	}

	@Test
	public void internalMailshare() throws IMAPException, SQLException {
		String mailshareUid = UUID.randomUUID().toString();

		Mailshare mailshare = defaultMailshare("test-" + System.currentTimeMillis());
		mailshare.routing = Routing.internal;

		getProvider(SecurityContext.SYSTEM).instance(IMailshare.class, domainUid).create(mailshareUid, mailshare);

		try (StoreClient sc = new StoreClient(imapServer.address(), 1143, "admin0", "password")) {
			assertTrue(sc.login());
			assertTrue(sc.isExist(mailshare.name + "@" + domainUid));
			sc.deleteMailbox(mailshare.name + "@" + domainUid);
			assertFalse(sc.isExist(mailshare.name + "@" + domainUid));
		}

		DirEntry dirEntry = getProvider(SecurityContext.SYSTEM).instance(IDirectory.class, domainUid)
				.findByEntryUid(mailshareUid);
		assertNotNull(dirEntry);

		new MailboxExistsMaintenanceOperation(new BmTestContext(SecurityContext.SYSTEM)).repair(domainUid, dirEntry,
				DiagnosticReport.create(), new TestMonitor());
		try (StoreClient sc = new StoreClient(imapServer.address(), 1143, "admin0", "password")) {
			assertTrue(sc.login());
			assertTrue(sc.isExist(mailshare.name + "@" + domainUid));
		}
	}

	@Test
	public void externalMailshare() throws IMAPException, SQLException {
		Map<String, String> domainSetting = getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid).get();
		domainSetting.put(DomainSettingsKeys.mail_routing_relay.name(), "split.domain.tld");
		getProvider(SecurityContext.SYSTEM).instance(IDomainSettings.class, domainUid).set(domainSetting);

		String mailshareUid = UUID.randomUUID().toString();

		Mailshare mailshare = defaultMailshare("test-" + System.currentTimeMillis());
		mailshare.routing = Routing.internal;

		getProvider(SecurityContext.SYSTEM).instance(IMailshare.class, domainUid).create(mailshareUid, mailshare);

		try (StoreClient sc = new StoreClient(imapServer.address(), 1143, "admin0", "password")) {
			assertTrue(sc.login());
			assertTrue(sc.isExist(mailshare.name + "@" + domainUid));
			sc.deleteMailbox(mailshare.name + "@" + domainUid);
			assertFalse(sc.isExist(mailshare.name + "@" + domainUid));
		}

		DirEntry dirEntry = getProvider(SecurityContext.SYSTEM).instance(IDirectory.class, domainUid)
				.findByEntryUid(mailshareUid);
		assertNotNull(dirEntry);

		new MailboxExistsMaintenanceOperation(new BmTestContext(SecurityContext.SYSTEM)).repair(domainUid, dirEntry,
				DiagnosticReport.create(), new TestMonitor());
		try (StoreClient sc = new StoreClient(imapServer.address(), 1143, "admin0", "password")) {
			assertTrue(sc.login());
			assertTrue(sc.isExist(mailshare.name + "@" + domainUid));
		}
	}
}
