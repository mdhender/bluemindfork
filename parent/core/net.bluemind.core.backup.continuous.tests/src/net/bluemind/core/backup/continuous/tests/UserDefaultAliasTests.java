/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.core.backup.continuous.tests;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.Name;
import net.bluemind.addressbook.api.VCard.Kind;
import net.bluemind.backend.mail.replica.api.IDbByContainerReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.core.api.Email;
import net.bluemind.core.container.api.IOfflineMgmt;
import net.bluemind.core.container.api.IdRange;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.BaseDirEntry.AccountType;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class UserDefaultAliasTests extends MailApiWithKafkaBaseTests {

	private String routingUid;

	private Optional<ItemValue<MailboxReplica>> userInbox(String uid) {
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IDbByContainerReplicatedMailboxes foldersApi = prov.instance(IDbByContainerReplicatedMailboxes.class,
				IMailReplicaUids.subtreeUid(domUid, Mailbox.Type.user, uid));
		return Optional.ofNullable(foldersApi.byReplicaName("INBOX"));
	}

	@Test
	public void defaultEmailOnAlias() throws Exception {

		this.routingUid = PopulateHelper.addUser("routing", domUid);
		await().atMost(20, TimeUnit.SECONDS).until(() -> userInbox(routingUid).isPresent());

		User user = new User();
		user.login = "dingo";
		user.password = "dingo";
		user.dataLocation = Topology.get().any(TagDescriptor.mail_imap.getTag()).uid;
		user.routing = Routing.internal;
		user.accountType = AccountType.FULL;
		VCard card = new VCard();
		card.kind = Kind.individual;
		card.identification.name = Name.create("wick", "john", null, null, null, null);
		user.contactInfos = card;

		List<Email> emails = Arrays.asList(Email.create("dingo@" + domUid, false, false),
				Email.create("john@" + alias, true, false));
		Collections.reverse(emails);
		user.emails = emails;

		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IUser userApi = prov.instance(IUser.class, domUid);
		IOfflineMgmt alloc = prov.instance(IOfflineMgmt.class, domUid, routingUid);
		IdRange fresh = alloc.allocateOfflineIds(1);
		ItemValue<User> asItem = ItemValue.create("dingo_uid", user);
		asItem.internalId = fresh.globalCounter;
		asItem.version = 1;
		System.err.println("Id should be " + asItem.internalId);

		userApi.restore(asItem, true);

		await().atMost(20, TimeUnit.SECONDS).until(() -> userInbox(asItem.uid).isPresent());

		ItemValue<User> fetched = userApi.getComplete("dingo_uid");
		System.err.println("email: " + fetched.value.defaultEmailAddress());
		assertEquals("john@" + alias, fetched.value.defaultEmailAddress());
		assertEquals(fresh.globalCounter, fetched.internalId);
		IMailboxes mboxApi = prov.instance(IMailboxes.class, domUid);
		ItemValue<Mailbox> mbox = mboxApi.getComplete("dingo_uid");
		System.err.println("mbox: " + mbox.value + " e: " + mbox.value.defaultEmail());

		IDirectory dirApi = prov.instance(IDirectory.class, domUid);
		DirEntry dirEntry = dirApi.findByEntryUid("dingo_uid");
		System.err.println("de: " + dirEntry);

	}

}
