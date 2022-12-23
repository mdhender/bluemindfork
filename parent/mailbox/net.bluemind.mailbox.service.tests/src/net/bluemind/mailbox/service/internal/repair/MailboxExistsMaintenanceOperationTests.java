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

import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.directory.api.RepairConfig;
import net.bluemind.directory.service.RepairTaskMonitor;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.service.IMailboxesStorage;
import net.bluemind.mailbox.service.MailboxesStorageFactory;
import net.bluemind.mailshare.api.IMailshare;
import net.bluemind.mailshare.api.Mailshare;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class MailboxExistsMaintenanceOperationTests extends AbstractRepairTests {
	@Test
	public void noneUser() {
		String userUid = UUID.randomUUID().toString();

		User user = defaultUser("test-" + System.currentTimeMillis());
		user.routing = Routing.none;

		getProvider(SecurityContext.SYSTEM).instance(IUser.class, domainUid).create(userUid, user);

		IMailboxes apiMailboxes = getProvider(SecurityContext.SYSTEM).instance(IMailboxes.class, domainUid);
		var mbx = apiMailboxes.byEmail(user.login + "@" + domainUid);

		IMailboxesStorage mbxStorage = MailboxesStorageFactory.getMailStorage();
		assertTrue(mbxStorage.mailboxExist(testContext, domainUid, mbx));

		mbxStorage.delete(testContext, domainUid, mbx);

		assertFalse(mbxStorage.mailboxExist(testContext, domainUid, mbx));

		DirEntry dirEntry = getProvider(SecurityContext.SYSTEM).instance(IDirectory.class, domainUid)
				.findByEntryUid(userUid);
		assertNotNull(dirEntry);

		new MailboxExistsMaintenanceOperation(new BmTestContext(SecurityContext.SYSTEM)).repair(domainUid, dirEntry,
				new RepairTaskMonitor(new TestMonitor(), RepairConfig.create(null, false, false, false)));

		assertTrue(mbxStorage.mailboxExist(testContext, domainUid, mbx));
	}

	@Test
	public void internalUser() {
		String userUid = UUID.randomUUID().toString();

		User user = defaultUser("test-" + System.currentTimeMillis());
		user.routing = Routing.internal;

		getProvider(SecurityContext.SYSTEM).instance(IUser.class, domainUid).create(userUid, user);

		IMailboxes apiMailboxes = getProvider(SecurityContext.SYSTEM).instance(IMailboxes.class, domainUid);
		var mbx = apiMailboxes.byEmail(user.login + "@" + domainUid);

		IMailboxesStorage mbxStorage = MailboxesStorageFactory.getMailStorage();
		assertTrue(mbxStorage.mailboxExist(testContext, domainUid, mbx));
		mbxStorage.delete(testContext, domainUid, mbx);

		assertFalse(mbxStorage.mailboxExist(testContext, domainUid, mbx));

		DirEntry dirEntry = getProvider(SecurityContext.SYSTEM).instance(IDirectory.class, domainUid)
				.findByEntryUid(userUid);
		assertNotNull(dirEntry);

		new MailboxExistsMaintenanceOperation(new BmTestContext(SecurityContext.SYSTEM)).repair(domainUid, dirEntry,
				new RepairTaskMonitor(new TestMonitor(), RepairConfig.create(null, false, false, false)));
		assertTrue(mbxStorage.mailboxExist(testContext, domainUid, mbx));
	}

	@Test
	public void externalUser() {
		Map<String, String> domainSetting = getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid).get();
		domainSetting.put(DomainSettingsKeys.mail_routing_relay.name(), "split.domain.tld");
		getProvider(SecurityContext.SYSTEM).instance(IDomainSettings.class, domainUid).set(domainSetting);

		String userUid = UUID.randomUUID().toString();

		User user = defaultUser("test-" + System.currentTimeMillis());
		user.routing = Routing.external;

		getProvider(SecurityContext.SYSTEM).instance(IUser.class, domainUid).create(userUid, user);

		IMailboxes apiMailboxes = getProvider(SecurityContext.SYSTEM).instance(IMailboxes.class, domainUid);
		var mbx = apiMailboxes.byEmail(user.login + "@" + domainUid);
		IMailboxesStorage mbxStorage = MailboxesStorageFactory.getMailStorage();
		assertTrue(mbxStorage.mailboxExist(testContext, domainUid, mbx));

		mbxStorage.delete(testContext, domainUid, mbx);
		assertFalse(mbxStorage.mailboxExist(testContext, domainUid, mbx));

		DirEntry dirEntry = getProvider(SecurityContext.SYSTEM).instance(IDirectory.class, domainUid)
				.findByEntryUid(userUid);
		assertNotNull(dirEntry);

		new MailboxExistsMaintenanceOperation(new BmTestContext(SecurityContext.SYSTEM)).repair(domainUid, dirEntry,
				new RepairTaskMonitor(new TestMonitor(), RepairConfig.create(null, false, false, false)));

		assertTrue(mbxStorage.mailboxExist(testContext, domainUid, mbx));
	}

	@Test
	public void noneMailshare() {
		String mailshareUid = UUID.randomUUID().toString();

		Mailshare mailshare = defaultMailshare("test-" + System.currentTimeMillis());
		mailshare.routing = Routing.none;

		getProvider(SecurityContext.SYSTEM).instance(IMailshare.class, domainUid).create(mailshareUid, mailshare);

		IMailboxesStorage mbxStorage = MailboxesStorageFactory.getMailStorage();
		ItemValue<Mailbox> mbx = new ItemValue<>();
		mbx.uid = mailshareUid;
		mbx.value = mailshare.toMailbox();
		assertTrue(mbxStorage.mailboxExist(testContext, domainUid, mbx));

		// Remove mailshare
		mbxStorage.delete(testContext, domainUid, mbx);
		assertFalse(mbxStorage.mailboxExist(testContext, domainUid, mbx));

		// Try to repair removed mailshare
		DirEntry dirEntry = getProvider(SecurityContext.SYSTEM).instance(IDirectory.class, domainUid)
				.findByEntryUid(mailshareUid);
		assertNotNull(dirEntry);

		new MailboxExistsMaintenanceOperation(new BmTestContext(SecurityContext.SYSTEM)).repair(domainUid, dirEntry,
				new RepairTaskMonitor(new TestMonitor(), RepairConfig.create(null, false, false, false)));

		// Mailshare must exist after repair
		assertTrue(mbxStorage.mailboxExist(testContext, domainUid, mbx));
	}

	@Test
	public void internalMailshare() {
		String mailshareUid = UUID.randomUUID().toString();

		Mailshare mailshare = defaultMailshare("test-" + System.currentTimeMillis());
		mailshare.routing = Routing.internal;

		getProvider(SecurityContext.SYSTEM).instance(IMailshare.class, domainUid).create(mailshareUid, mailshare);

		IMailboxesStorage mbxStorage = MailboxesStorageFactory.getMailStorage();
		ItemValue<Mailbox> mbx = new ItemValue<>();
		mbx.uid = mailshareUid;
		mbx.value = mailshare.toMailbox();
		assertTrue(mbxStorage.mailboxExist(testContext, domainUid, mbx));

		mbxStorage.delete(testContext, domainUid, mbx);

		assertFalse(mbxStorage.mailboxExist(testContext, domainUid, mbx));

		DirEntry dirEntry = getProvider(SecurityContext.SYSTEM).instance(IDirectory.class, domainUid)
				.findByEntryUid(mailshareUid);
		assertNotNull(dirEntry);

		new MailboxExistsMaintenanceOperation(new BmTestContext(SecurityContext.SYSTEM)).repair(domainUid, dirEntry,
				new RepairTaskMonitor(new TestMonitor(), RepairConfig.create(null, false, false, false)));
		assertTrue(mbxStorage.mailboxExist(testContext, domainUid, mbx));
	}

	@Test
	public void externalMailshare() {
		Map<String, String> domainSetting = getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid).get();
		domainSetting.put(DomainSettingsKeys.mail_routing_relay.name(), "split.domain.tld");
		getProvider(SecurityContext.SYSTEM).instance(IDomainSettings.class, domainUid).set(domainSetting);

		String mailshareUid = UUID.randomUUID().toString();

		Mailshare mailshare = defaultMailshare("test-" + System.currentTimeMillis());
		mailshare.routing = Routing.internal;

		getProvider(SecurityContext.SYSTEM).instance(IMailshare.class, domainUid).create(mailshareUid, mailshare);

		IMailboxesStorage mbxStorage = MailboxesStorageFactory.getMailStorage();
		ItemValue<Mailbox> mbx = new ItemValue<>();
		mbx.uid = mailshareUid;
		mbx.value = mailshare.toMailbox();
		assertTrue(mbxStorage.mailboxExist(testContext, domainUid, mbx));

		mbxStorage.delete(testContext, domainUid, mbx);

		assertFalse(mbxStorage.mailboxExist(testContext, domainUid, mbx));

		DirEntry dirEntry = getProvider(SecurityContext.SYSTEM).instance(IDirectory.class, domainUid)
				.findByEntryUid(mailshareUid);
		assertNotNull(dirEntry);

		new MailboxExistsMaintenanceOperation(new BmTestContext(SecurityContext.SYSTEM)).repair(domainUid, dirEntry,
				new RepairTaskMonitor(new TestMonitor(), RepairConfig.create(null, false, false, false)));
		assertTrue(mbxStorage.mailboxExist(testContext, domainUid, mbx));

	}
}
