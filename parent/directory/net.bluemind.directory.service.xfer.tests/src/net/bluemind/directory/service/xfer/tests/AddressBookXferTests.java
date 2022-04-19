package net.bluemind.directory.service.xfer.tests;
/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.FormatedName;
import net.bluemind.addressbook.api.VCard.Kind;
import net.bluemind.addressbook.api.VCard.Organizational.Member;
import net.bluemind.addressbook.api.VCardChanges;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.directory.api.IDirectory;

public class AddressBookXferTests extends AbstractMultibackendTests {
	@Test
	public void testXferAB() {
		String containerUid = IAddressBookUids.defaultUserAddressbook(userUid);

		IAddressBook service = ServerSideServiceProvider.getProvider(context).instance(IAddressBook.class,
				containerUid);

		VCard card = defaultVCard();
		service.create("c1", card);
		service.create("c2", card);

		String uid3 = "testcreate_" + System.nanoTime();

		card.kind = Kind.group;
		card.identification.formatedName = FormatedName.create("test25");

		String uid4 = "testcreate_" + System.nanoTime();
		VCard group = defaultVCard();
		group.kind = Kind.group;
		String uid5 = "testcreate_" + System.nanoTime();
		group.organizational.member = Arrays.asList(Member.create(containerUid, uid5, "fakeName", "fake@email.la"));
		VCardChanges changes = VCardChanges.create(
				// add
				Arrays.asList(VCardChanges.ItemAdd.create(uid3, defaultVCard()),
						// Create group before member
						VCardChanges.ItemAdd.create(uid4, group), VCardChanges.ItemAdd.create(uid5, defaultVCard())

				),
				// modify
				Arrays.asList(VCardChanges.ItemModify.create("c1", card)),
				// delete
				Arrays.asList(VCardChanges.ItemDelete.create("c2")));

		service.updates(changes);

		// initial container state
		int nbItems = service.allUids().size();
		assertEquals(4, nbItems);
		long version = service.getVersion();
		assertEquals(7, version);

		TaskRef tr = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDirectory.class, domainUid)
				.xfer(userUid, shardIp);
		waitTaskEnd(tr);

		// current service should return nothing
		assertTrue(service.allUids().isEmpty());

		// new IAddressBook instance
		service = ServerSideServiceProvider.getProvider(context).instance(IAddressBook.class, containerUid);

		assertEquals(nbItems, service.allUids().size());
		assertEquals(4L, service.getVersion());

		service.create("new-one", card);

		ContainerChangeset<String> changeset = service.changeset(4L);
		assertEquals(1, changeset.created.size());
		assertEquals("new-one", changeset.created.get(0));
		assertTrue(changeset.updated.isEmpty());
		assertTrue(changeset.deleted.isEmpty());

	}

	protected VCard defaultVCard() {
		VCard card = new VCard();

		card.identification = new VCard.Identification();
		card.identification.formatedName = VCard.Identification.FormatedName.create("default",
				Arrays.<VCard.Parameter>asList());

		card.related.spouse = "Clara Morgane";
		card.related.assistant = "Sylvain Garcia";
		card.related.manager = "David Phan";

		VCard.Organizational organizational = VCard.Organizational.create("Loser", "Boss", //
				VCard.Organizational.Org.create("Blue-mind", "tlse", "Dev"), //
				Arrays.<VCard.Organizational.Member>asList());

		card.organizational = organizational;

		return card;
	}

}
