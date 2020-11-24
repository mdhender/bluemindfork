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
package net.bluemind.user.service.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import com.google.common.collect.ImmutableSet;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.FormatedName;
import net.bluemind.core.api.Email;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.user.api.User;

public class UserVCardAdapterTest {

	@Test
	public void testAliases() {
		Domain d = Domain.create("origin.net", "label", "description",
				ImmutableSet.<String>builder().add("a.net").add("b.net").add("c.net").build());
		ItemValue<Domain> domain = ItemValue.create("origin.net", d);

		User user = new User();
		user.contactInfos = new VCard();
		user.contactInfos.identification.formatedName = FormatedName.create("gg");
		user.emails = Arrays.asList(Email.create("1@origin.net", false, true),
				net.bluemind.core.api.Email.create("2@b.net", false, false));
		UserVCardAdapter adapter = new UserVCardAdapter();
		VCard card = adapter.asVCard(domain, "zz", user);
		assertNotNull(card);
		assertEquals(4 + 1, card.communications.emails.size());
		assertEquals("1@origin.net", card.communications.emails.get(0).value);
		// no ordered
		assertEquals(ImmutableSet.builder().add("1@a.net", "1@b.net", "1@c.net").build(),
				ImmutableSet.builder().add(card.communications.emails.get(1).value,
						card.communications.emails.get(2).value, card.communications.emails.get(3).value).build());

		assertEquals("2@b.net", card.communications.emails.get(4).value);

	}

	@Test
	public void tesAliasesAndDefault() {
		Domain d = Domain.create("origin.net", "label", "description",
				ImmutableSet.<String>builder().add("a.net").add("b.net").add("c.net").build());
		d.name = "origin.net";
		ItemValue<Domain> domain = ItemValue.create("origin.net", d);

		User user = new User();
		user.contactInfos = new VCard();
		user.contactInfos.identification.formatedName = FormatedName.create("gg");
		user.emails = Arrays.asList(Email.create("1@origin.net", false, false),
				net.bluemind.core.api.Email.create("2@b.net", true, true), Email.create("11@a.net", false, false));
		UserVCardAdapter adapter = new UserVCardAdapter();
		VCard card = adapter.asVCard(domain, "zz", user);
		assertNotNull(card);
		assertEquals(1 + 4 + 1, card.communications.emails.size());
		assertEquals("1@origin.net", card.communications.emails.get(0).value);
		assertEquals("2@b.net", card.communications.emails.get(1).value);
		// no ordered
		assertEquals(ImmutableSet.builder().add("2@origin.net", "2@a.net", "2@c.net").build(),
				ImmutableSet.builder().add(card.communications.emails.get(2).value,
						card.communications.emails.get(3).value, card.communications.emails.get(4).value).build());

		assertEquals("2@b.net", card.defaultMail());
	}

	@Test
	public void tesNoAliasesAndDefault() {
		Domain d = Domain.create("origin.net", "label", "description",
				ImmutableSet.<String>builder().add("a.net").add("b.net").add("c.net").build());
		ItemValue<Domain> domain = ItemValue.create("origin.net", d);

		User user = new User();
		user.contactInfos = new VCard();
		user.contactInfos.identification.formatedName = FormatedName.create("gg");
		user.emails = Arrays.asList(Email.create("1@origin.net", true, false),
				net.bluemind.core.api.Email.create("2@b.net", false, true), Email.create("11@a.net", false, false));
		UserVCardAdapter adapter = new UserVCardAdapter();
		VCard card = adapter.asVCard(domain, "zz", user);
		assertNotNull(card);
		assertEquals(1 + 4 + 1, card.communications.emails.size());
		assertEquals("1@origin.net", card.defaultMail());
	}

	@Test
	public void testUserWithRoutingNoneShouldHaveNoInternalEmailsInVCard() {
		Domain d = Domain.create("origin.net", "label", "description",
				ImmutableSet.<String>builder().add("a.net").add("b.net").add("c.net").build());
		ItemValue<Domain> domain = ItemValue.create("origin.net", d);

		User user = new User();
		user.routing = Routing.none;
		user.contactInfos = new VCard();
		user.contactInfos.communications.emails = Arrays.asList(VCard.Communications.Email.create("1@a.net"),
				VCard.Communications.Email.create("external@ext.net"), VCard.Communications.Email.create("1@b.net"),
				VCard.Communications.Email.create("1@c.net"), VCard.Communications.Email.create("1@origin.net"));
		user.contactInfos.identification.formatedName = FormatedName.create("gg");
		user.emails = new ArrayList<>();

		UserVCardAdapter adapter = new UserVCardAdapter();
		VCard card = adapter.asVCard(domain, "zz", user);
		assertNotNull(card);
		assertEquals(1, card.communications.emails.size());
		assertEquals("external@ext.net", card.communications.emails.get(0).value);
	}

	@Test
	public void test_BM_10296() {
		Domain d = Domain.create("origin.net", "label", "description",
				ImmutableSet.<String>builder().add("a.net").add("b.net").add("c.net").build());
		ItemValue<Domain> domain = ItemValue.create("origin.net", d);

		User user = new User();
		user.contactInfos = new VCard();
		user.contactInfos.identification.formatedName = FormatedName.create("gg");
		user.emails = Arrays.asList(Email.create("1@origin.net", false, true),
				net.bluemind.core.api.Email.create("2@b.net", true, true), Email.create("11@a.net", false, false));
		UserVCardAdapter adapter = new UserVCardAdapter();
		VCard card = adapter.asVCard(domain, "zz", user);
		assertNotNull(card);
		assertEquals("2@b.net", card.defaultMail());
	}
}
