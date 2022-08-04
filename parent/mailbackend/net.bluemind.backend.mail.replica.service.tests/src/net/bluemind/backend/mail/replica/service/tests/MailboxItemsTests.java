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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxRecord.InternalFlag;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.mime.MimeTree;

public class MailboxItemsTests extends AbstractRollingReplicationTests {

	private static final String POLO = "polo";
	private static final String MARCO = "marco";
	private static Logger logger = LoggerFactory.getLogger(MailboxItemsTests.class);
	public static final int MAIL_COUNT = 250;
	public static final int MARCO_POLO_CYCLES = 50;

	private IMailboxItems mailApi;

	@Before
	@Override
	public void before() throws Exception {
		super.before();

		this.partition = domainUid.replace('.', '_');
		this.mboxRoot = "user." + userUid.replace('.', '^');

		this.apiKey = "sid";
		Sessions.get().put(apiKey,
				new SecurityContext("sid", userUid, Collections.emptyList(), Collections.emptyList(), domainUid));

		imapAsUser(sc -> {
			assertTrue(sc.create(MARCO));
			assertTrue(sc.create(POLO));
			int added = sc.append(MARCO, testEml(), new FlagsList());
			assertTrue(added > 0);
			sc.select(MARCO);
			Collection<MimeTree> bs = sc.uidFetchBodyStructure(Arrays.asList(added));
			MimeTree tree = bs.iterator().next();
			logger.info(String.format("Mail %d added:%n %s", added, tree));
			return null;
		});

		IMailboxFolders foldersApi = foldersApi();
		ItemValue<MailboxFolder> marco;
		ItemValue<MailboxFolder> polo;
		marco = foldersApi.byName(MARCO);
		polo = foldersApi.byName(POLO);
		int retry = 100;
		while ((marco == null || polo == null) && retry-- > 0) {
			Thread.sleep(200);
			marco = foldersApi.byName(MARCO);
			polo = foldersApi.byName(POLO);
		}
		this.mailApi = mailItemsApi(marco);

		imapAsUser(sc -> {
			// start at index 1 since we have already created one mail before
			for (int i = 1; i < MAIL_COUNT; i++) {
				sc.append(MARCO, testEml(), new FlagsList());
			}
			return null;
		});

		ContainerChangeset<ItemVersion> notDeleted = mailApi.filteredChangesetById(0L,
				ItemFlagFilter.create().mustNot(ItemFlag.Deleted));
		retry = 100;
		while (notDeleted.created.size() < MAIL_COUNT && retry-- > 0) {
			Thread.sleep(500);
			notDeleted = mailApi.filteredChangesetById(0L, ItemFlagFilter.create().mustNot(ItemFlag.Deleted));
			String msg = String.format("Not deleted mail count in 'marco' is %d", notDeleted.created.size());
			logger.error(msg);
		}
		assertTrue(retry > 0);

		String msg = String.format("Got %d items after %d attempts.",
				notDeleted.created != null ? notDeleted.created.size() : -1, (100 - retry));
		logger.error(msg);
	}

	@Override
	protected InputStream testEml() {
		return EmlTemplates.withRandomMessageId("small_eml.ftl");
	}

	@Override
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
	public void deleteById() throws InterruptedException {
		List<Long> inMarco = mailApi.filteredChangesetById(0L,
				ItemFlagFilter.create().mustNot(ItemFlag.Deleted)).created.stream().map(iv -> iv.id).toList();
		long deletedId = inMarco.get(0);
		mailApi.deleteById(deletedId);
		List<Long> inMarcoAfterDelete = mailApi.filteredChangesetById(0L,
				ItemFlagFilter.create().mustNot(ItemFlag.Deleted)).created.stream().map(iv -> iv.id).toList();
		assertEquals(inMarco.size() - 1L, inMarcoAfterDelete.size());

		// wait for expunge to be done
		Thread.sleep(250);

		ItemValue<MailboxItem> item = mailApi.getCompleteById(deletedId);
		assertTrue("Should contain 'expunged' internal flag",
				((MailboxRecord) item.value).internalFlags.contains(InternalFlag.expunged));
	}

	@Test
	public void multipleDeleteById() throws InterruptedException {
		List<Long> inMarco = mailApi.filteredChangesetById(0L,
				ItemFlagFilter.create().mustNot(ItemFlag.Deleted)).created.stream().map(iv -> iv.id).toList();
		List<Long> deletedIds = Arrays.asList(inMarco.get(0), inMarco.get(1));
		mailApi.multipleDeleteById(deletedIds);
		List<Long> inMarcoAfterDelete = mailApi.filteredChangesetById(0L,
				ItemFlagFilter.create().mustNot(ItemFlag.Deleted)).created.stream().map(iv -> iv.id).toList();
		assertEquals((long) inMarco.size() - deletedIds.size(), inMarcoAfterDelete.size());

		// wait for expunge to be done
		Thread.sleep(500);

		List<ItemValue<MailboxItem>> records = mailApi.multipleGetById(deletedIds);
		assertEquals("Should contain 'expunged' internal flag", deletedIds.size(), records.stream()
				.filter(r -> ((MailboxRecord) r.value).internalFlags.contains(InternalFlag.expunged)).count());
	}
}
