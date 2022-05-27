/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.backend.mail.replica.service.tests;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.backend.mail.parsing.BodyStreamProcessor;
import net.bluemind.backend.mail.replica.service.internal.repair.MessageBodyRepair;
import net.bluemind.backend.mail.replica.service.internal.repair.MessageBodyRepair.MessageBodyMaintenance;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.task.service.NullTaskMonitor;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.directory.api.RepairConfig;
import net.bluemind.directory.service.RepairTaskMonitor;

/** @see MessageBodyRepair */
public class MessageBodyRepairTests {

	private static final Logger LOGGER = LoggerFactory.getLogger(MessageBodyRepairTests.class);

	private static int originalBodyStreamProcessorBodyVersion;

	private ReplicationStackTests replicationStackTests = new ReplicationStackTests();

	private IMailboxFolders mailboxFolderService;

	private ItemValue<MailboxFolder> mailboxFolder;

	private IMailboxItems mailboxItemService;

	@BeforeClass
	public static void oneShotBefore() {
		originalBodyStreamProcessorBodyVersion = BodyStreamProcessor.BODY_VERSION;
	}

	@Before
	public void before() throws Exception {
		replicationStackTests.before();
		this.mailboxFolderService = replicationStackTests.provider().instance(IMailboxFolders.class,
				replicationStackTests.partition, replicationStackTests.mboxRoot);
		this.mailboxFolder = this.mailboxFolderService.byName("INBOX");
		this.mailboxItemService = replicationStackTests.provider().instance(IMailboxItems.class,
				this.mailboxFolder.uid);
	}

	@After
	public void after() throws Exception {
		replicationStackTests.after();
		BodyStreamProcessor.BODY_VERSION = originalBodyStreamProcessorBodyVersion;
	}

	@Test
	public void testCheckNothingToDo() throws SQLException, IOException, InterruptedException, NoSuchFieldException,
			SecurityException, IllegalArgumentException, IllegalAccessException {
		// create mail
		this.replicationStackTests.addDraft(this.mailboxFolder);
		// changing BodyStreamProcessor#BODY_VERSION has impact on the "need update"
		// detection
		BodyStreamProcessor.BODY_VERSION = Integer.MIN_VALUE;
		this.check();
		// FIXME check something
	}

	@Test
	public void testCheckNeedUpdate() throws SQLException, IOException, InterruptedException, NoSuchFieldException,
			SecurityException, IllegalArgumentException, IllegalAccessException {
		// create mail
		this.replicationStackTests.addDraft(this.mailboxFolder);
		// changing BodyStreamProcessor#BODY_VERSION has impact on the "need update"
		// detection
		BodyStreamProcessor.BODY_VERSION = Integer.MAX_VALUE;
		this.check();
		// FIXME check something
	}

	@Test
	public void testRepairNothingToDo() throws SQLException, IOException, InterruptedException, NoSuchFieldException,
			SecurityException, IllegalArgumentException, IllegalAccessException {
		// create mail
		final ItemValue<MailboxItem> mailboxItem = this.replicationStackTests.addDraft(this.mailboxFolder);
		// changing BodyStreamProcessor#BODY_VERSION has impact on the "need update"
		// detection
		BodyStreamProcessor.BODY_VERSION = Integer.MIN_VALUE;
		this.repair();
		ItemValue<MailboxItem> mailboxItemAfterRepair = this.mailboxItemService.getCompleteById(mailboxItem.internalId);
		// body version should not have changed
		Assert.assertTrue(mailboxItemAfterRepair.value.body.bodyVersion == mailboxItem.value.body.bodyVersion);
	}

	@Test
	public void testRepairNeedUpdate() throws SQLException, IOException, InterruptedException, NoSuchFieldException,
			SecurityException, IllegalArgumentException, IllegalAccessException {
		// create mail
		final ItemValue<MailboxItem> mailboxItem = this.replicationStackTests.addDraft(this.mailboxFolder);
		// changing BodyStreamProcessor#BODY_VERSION has impact on the "need update"
		// detection
		BodyStreamProcessor.BODY_VERSION = Integer.MAX_VALUE;
		this.repair();
		final ItemValue<MailboxItem> mailboxItemAfterRepair = this.mailboxItemService
				.getCompleteById(mailboxItem.internalId);
		// body version should be the current one (in BodyStreamProcessor.BODY_VERSION)
		Assert.assertTrue(mailboxItemAfterRepair.value.body.bodyVersion == BodyStreamProcessor.BODY_VERSION);
	}

	@Test
	public void testMultipleRepairNeedUpdate() throws SQLException, IOException, InterruptedException,
			NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		// create mails
		final List<ItemValue<MailboxItem>> mailboxItems = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			mailboxItems.add(this.replicationStackTests.addDraft(this.mailboxFolder));
		}
		// changing BodyStreamProcessor#BODY_VERSION has impact on the "need update"
		// detection
		BodyStreamProcessor.BODY_VERSION = Integer.MAX_VALUE;
		this.repair();
		// body version should be the current one (in BodyStreamProcessor.BODY_VERSION)
		mailboxItems.forEach(mailboxItem -> {
			final ItemValue<MailboxItem> mailboxItemAfterRepair = this.mailboxItemService
					.getCompleteById(mailboxItem.internalId);
			Assert.assertTrue(mailboxItemAfterRepair.value.body.bodyVersion == BodyStreamProcessor.BODY_VERSION);
		});
	}

	private void checkOrRepair(boolean checkMode) {
		final BmTestContext bmTestContext = new BmTestContext(SecurityContext.SYSTEM);
		final MessageBodyMaintenance messageBodyMaintenance = new MessageBodyMaintenance(bmTestContext);
		final DirEntry dirEntry = replicationStackTests.provider()
				.instance(IDirectory.class, replicationStackTests.domainUid)
				.findByEntryUid(replicationStackTests.userUid);
		if (checkMode) {
			messageBodyMaintenance.check(replicationStackTests.domainUid, dirEntry,
					new RepairTaskMonitor(new NullTaskMonitor(), RepairConfig.create(null, false, false, false)));
		} else {
			messageBodyMaintenance.repair(replicationStackTests.domainUid, dirEntry,
					new RepairTaskMonitor(new NullTaskMonitor(), RepairConfig.create(null, false, false, false)));
		}

	}

	/** Call {@link MessageBodyRepair} check . */
	private void check() {
		this.checkOrRepair(true);
	}

	/** Call {@link MessageBodyRepair} repair . */
	private void repair() {
		this.checkOrRepair(false);
	}

}
