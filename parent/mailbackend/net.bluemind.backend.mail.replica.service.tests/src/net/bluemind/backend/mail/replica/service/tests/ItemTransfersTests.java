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

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import net.bluemind.backend.mail.api.IItemsTransfer;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemIdentifier;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.mime.MimeTree;

public class ItemTransfersTests extends AbstractRollingReplicationTests {

	public static final int MAIL_COUNT = 250;
	public static final int MARCO_POLO_CYCLES = 50;

	private String partition;
	private String mboxRoot;
	private String apiKey;
	private IMailboxItems mailApi;
	private IMailboxItems mailApiPolo;
	private ItemValue<MailboxFolder> marco;
	private ItemValue<MailboxFolder> polo;
	private IItemsTransfer txApi;
	private IItemsTransfer txApiPolo;

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
			assertTrue(sc.create("marco"));
			assertTrue(sc.create("polo"));
			int added = sc.append("marco", testEml(), new FlagsList());
			assertTrue(added > 0);
			sc.select("marco");
			Collection<MimeTree> bs = sc.uidFetchBodyStructure(Arrays.asList(added));
			MimeTree tree = bs.iterator().next();
			System.out.println("Mail " + added + " added:\n" + tree);
			return null;
		});

		IMailboxFolders foldersApi = foldersApi();
		this.marco = foldersApi.byName("marco");
		this.polo = foldersApi.byName("polo");
		int retry = 100;
		while ((marco == null || polo == null) && retry-- > 0) {
			Thread.sleep(200);
			this.marco = foldersApi.byName("marco");
			this.polo = foldersApi.byName("polo");
		}
		this.mailApi = mailItemsApi(marco);
		this.mailApiPolo = mailItemsApi(polo);

		this.txApi = provider().instance(IItemsTransfer.class, marco.uid, polo.uid);
		this.txApiPolo = provider().instance(IItemsTransfer.class, polo.uid, marco.uid);

		imapAsUser(sc -> {
			for (int i = 0; i < MAIL_COUNT; i++) {
				sc.append("marco", testEml(), new FlagsList());
			}
			return null;
		});

		ContainerChangeset<ItemVersion> notDeleted = mailApi.filteredChangesetById(0L,
				ItemFlagFilter.create().mustNot(ItemFlag.Deleted));
		retry = 100;
		while (notDeleted.created.size() < MAIL_COUNT && retry-- > 0) {
			Thread.sleep(500);
			notDeleted = mailApi.filteredChangesetById(0L, ItemFlagFilter.create().mustNot(ItemFlag.Deleted));
			System.err.println("Not deleted mail count in 'marco' is " + notDeleted.created.size());
		}
		assertTrue(retry > 0);
		System.err.println("Got " + notDeleted.created.size() + " items after " + (100 - retry) + " attempts.");
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
	public void marcoPoloMoves() {
		List<Long> all = mailApi.filteredChangesetById(0L, ItemFlagFilter.create().mustNot(ItemFlag.Deleted)).created
				.stream().map(iv -> iv.id).collect(Collectors.toList());
		IItemsTransfer curApi = txApi;
		for (int i = 0; i < MARCO_POLO_CYCLES; i++) {
			long time = System.currentTimeMillis();
			System.err.println("Moving items... round " + (i + 1));
			List<ItemIdentifier> moved = curApi.move(all);
			assertTrue(moved.size() >= MAIL_COUNT);
			all = moved.stream().map(ii -> ii.id).collect(Collectors.toList());
			curApi = curApi == txApi ? txApiPolo : txApi;
			time = System.currentTimeMillis() - time;
			System.err.println("Cycle " + (i + 1) + " in " + time + "ms for " + all.size() + " messages.");
		}
	}

	@Test
	public void BM18771_deleteCopiedMessageOnly() {
		List<Long> inMarco = mailApi.filteredChangesetById(0L,
				ItemFlagFilter.create().mustNot(ItemFlag.Deleted)).created.stream().map(iv -> iv.id)
						.collect(Collectors.toList());
		assertTrue("Must move more than 100 messages", inMarco.size() > 100);

		IItemsTransfer curApi = txApi;
		List<ItemIdentifier> moved = curApi.move(inMarco);
		assertTrue("The returned message list size do not match the moved message list size",
				moved.size() == inMarco.size());
		List<Long> inPolo = mailApiPolo.filteredChangesetById(0L,
				ItemFlagFilter.create().mustNot(ItemFlag.Deleted)).created.stream().map(iv -> iv.id)
						.collect(Collectors.toList());
		assertTrue("All messages are not present in destination mailbox", inPolo.size() == inMarco.size());

		inMarco = mailApi.filteredChangesetById(0L, ItemFlagFilter.create().mustNot(ItemFlag.Deleted)).created.stream()
				.map(iv -> iv.id).collect(Collectors.toList());
		assertTrue("Some messages still present in source mailbox", inMarco.isEmpty());

	}
}
