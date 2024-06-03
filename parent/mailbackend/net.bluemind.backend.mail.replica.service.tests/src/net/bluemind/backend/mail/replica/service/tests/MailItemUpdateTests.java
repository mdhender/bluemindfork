/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import io.vertx.core.json.JsonObject;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.backend.mail.api.MessageBody.Header;
import net.bluemind.backend.mail.api.MessageBody.Part;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.replica.api.MailApiHeaders;
import net.bluemind.core.container.api.Ack;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.mime.MimeTree;

public class MailItemUpdateTests extends AbstractRollingReplicationTests {

	private ItemValue<MailboxFolder> inbox;
	private IMailboxItems mailApi;
	private ItemValue<MailboxItem> mailObject;

	@BeforeEach
	@Override
	public void before(TestInfo testInfo) throws Exception {
		super.before(testInfo);

		imapAsUser(sc -> {
			int added = sc.append("INBOX", testEml(), new FlagsList());
			assertTrue(added > 0);
			sc.select("INBOX");
			Collection<MimeTree> bs = sc.uidFetchBodyStructure(Arrays.asList(added));
			MimeTree tree = bs.iterator().next();
			System.out.println("Mail " + added + " added:\n" + tree);
			return null;
		});

		IMailboxFolders foldersApi = foldersApi();
		this.inbox = foldersApi.byName("INBOX");
		int retry = 100;
		while (inbox == null && retry-- > 0) {
			Thread.sleep(200);
			inbox = foldersApi.byName("INBOX");
		}
		this.mailApi = mailItemsApi(inbox);

		ContainerChangeset<ItemVersion> notDeleted = mailApi.filteredChangesetById(0L,
				ItemFlagFilter.create().mustNot(ItemFlag.Deleted));
		retry = 100;
		while (notDeleted.created.isEmpty() && retry-- > 0) {
			Thread.sleep(200);
			notDeleted = mailApi.filteredChangesetById(0L, ItemFlagFilter.create().mustNot(ItemFlag.Deleted));
		}
		assertTrue(retry > 0);
		System.err.println("Got it after " + (100 - retry) + " attempts.");
		ItemVersion mailItem = notDeleted.created.get(0);
		this.mailObject = mailApi.getCompleteById(mailItem.id);
		assertNotNull(mailObject);
		System.err.println("Found email '" + mailObject.value.body.subject + "'");
	}

	public IServiceProvider provider() {
		SecurityContext userSec = new SecurityContext("sid", userUid, Collections.emptyList(), Collections.emptyList(),
				domainUid);
		return ServerSideServiceProvider.getProvider(userSec);
	}

	public IMailboxFolders foldersApi() {
		return provider().instance(IMailboxFolders.class, partition, mboxRoot);
	}

	public IMailboxItems mailItemsApi(ItemValue<MailboxFolder> mailContainer) {
		return provider().instance(IMailboxItems.class, mailContainer.uid);
	}

	@Test
	public void updateEmailSubject() {
		String newSubject = "new " + System.currentTimeMillis();
		mailObject.value.body.subject = newSubject;
		Ack ack = mailApi.updateById(mailObject.internalId, mailObject.value);
		assertTrue(ack.version > mailObject.version);
		ItemValue<MailboxItem> reloaded = mailApi.getCompleteById(mailObject.internalId);
		System.err.println("rel body: " + reloaded.value.body.guid);
		System.err.println("rel uid: " + reloaded.value.imapUid);
		assertEquals(newSubject, reloaded.value.body.subject);
		assertNotEquals(mailObject.value.imapUid, reloaded.value.imapUid);
	}

	@Test
	public void createExistingItemHavingSameVersionShouldNoop() {
		ItemValue<MailboxItem> existing = mailApi.getCompleteById(mailObject.internalId);
		mailObject.value.body.headers = new ArrayList<>();
		mailObject.value.body.headers.add(Header.create(MailApiHeaders.X_BM_DRAFT_REFRESH_DATE,
				Long.toString(existing.value.body.date.getTime())));
		Ack ack = mailApi.createById(mailObject.internalId, mailObject.value);
		assertTrue(ack.version == mailObject.version);
	}

	@Test
	public void createExistingItemHavingDifferentVersionShouldFail() {
		for (Header header : mailObject.value.body.headers) {
			if (header.name.equals(MailApiHeaders.X_BM_DRAFT_REFRESH_DATE)) {
				header.values.clear();
				header.values.add(Integer.MAX_VALUE + "");
			}
		}
		try {
			mailApi.createById(mailObject.internalId, mailObject.value);
			fail();
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("already exists"));
		}
	}

	@Test
	public void updateEmailAndMaintainExistingAttachments() {
		mailObject.value.body.headers.add(Header.create("John", "Bang" + System.currentTimeMillis()));
		Part tree = mailObject.value.body.structure;
		printMimeTree(tree);

		Ack ack = mailApi.updateById(mailObject.internalId, mailObject.value);
		assertTrue(ack.version > mailObject.version);
		ItemValue<MailboxItem> reloaded = mailApi.getCompleteById(mailObject.internalId);
		Part newTree = reloaded.value.body.structure;
		printMimeTree(newTree);

		// existing attachments should still have the same size
		assertEquals(tree.children.get(1).size, newTree.children.get(1).size);

	}

	private void printMimeTree(Part tree) {
		System.err.println(new JsonObject(JsonUtils.asString(tree)).encodePrettily());
	}

	@Test
	public void ensureDateRefreshes() {
		long now = System.currentTimeMillis();
		String newSubject = "new " + now;
		long ts = now + TimeUnit.DAYS.toMillis(8);
		mailObject.value.body.subject = newSubject;
		mailObject.value.body.headers = new ArrayList<>();
		mailObject.value.body.headers.add(Header.create(MailApiHeaders.X_BM_DRAFT_REFRESH_DATE, Long.toString(ts)));
		Ack ack = mailApi.updateById(mailObject.internalId, mailObject.value);
		assertTrue(ack.version > mailObject.version);
		ItemValue<MailboxItem> reloaded = mailApi.getCompleteById(mailObject.internalId);
		Date fetchedDate = reloaded.value.body.date;
		long hoursDiff = TimeUnit.MILLISECONDS.toHours(fetchedDate.getTime() - now);
		System.err.println("new date: " + fetchedDate + " diff: " + hoursDiff);
		// imap date precision is not that good, if we are 7 days away, we're good
		assertTrue(hoursDiff > 7 * 24);
		assertEquals(newSubject, reloaded.value.body.subject);
		assertNotEquals(mailObject.value.imapUid, reloaded.value.imapUid);
	}

	@Test
	public void simpleFlagsUpdateStillWorks() {
		String newFlag = "fresh" + System.currentTimeMillis();
		mailObject.value.flags = Arrays.asList(new MailboxItemFlag(newFlag));
		Ack ack = mailApi.updateById(mailObject.internalId, mailObject.value);
		assertEquals(mailObject.version + 1, ack.version, "version was not bumped");
	}

}
