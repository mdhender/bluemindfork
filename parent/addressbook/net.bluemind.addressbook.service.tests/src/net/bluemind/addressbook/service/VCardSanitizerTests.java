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
package net.bluemind.addressbook.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Kind;
import net.bluemind.addressbook.api.VCard.Organizational.Member;
import net.bluemind.addressbook.persistance.VCardStore;
import net.bluemind.addressbook.service.internal.AddressBookService;
import net.bluemind.addressbook.service.internal.VCardSanitizer;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistance.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.tests.BmTestContext;

public class VCardSanitizerTests extends AbstractServiceTests {

	private Container book1;
	private VCardStore vCardStoreBook1;
	private ItemStore itemStoreBook1;

	private Container book2;
	private VCardStore vCardStoreBook2;
	private ItemStore itemStoreBook2;
	private BmContext testContext;

	@Before
	public void before() throws Exception {
		super.before();

		book1 = createTestContainer(owner);

		itemStoreBook1 = new ItemStore(dataDataSource, book1, defaultSecurityContext);

		vCardStoreBook1 = new VCardStore(dataDataSource, book1);

		book2 = createTestContainer(owner);

		itemStoreBook2 = new ItemStore(dataDataSource, book2, defaultSecurityContext);

		vCardStoreBook2 = new VCardStore(dataDataSource, book2);

		testContext = new BmTestContext(SecurityContext.SYSTEM);
		// construct vcards containers
	}

	@Test
	public void testSanitize() throws SQLException, ServerFault {

		// create 2 cards into 2 addressbooks
		VCard card1 = defaultVCard();
		card1.identification.formatedName = VCard.Identification.FormatedName.create("user1");
		card1.communications.emails = Arrays.asList(VCard.Communications.Email.create("checked1@test.com"));
		String uid1 = "test1_" + System.nanoTime();

		Item item1 = Item.create(uid1, UUID.randomUUID().toString());
		item1.displayName = "user1";
		itemStoreBook1.create(item1);
		item1 = itemStoreBook1.get(uid1);
		vCardStoreBook1.create(item1, card1);

		VCard card2 = defaultVCard();
		card2.identification.formatedName = VCard.Identification.FormatedName.create("user2");
		card2.communications.emails = Arrays.asList(VCard.Communications.Email.create("checked2@test.com"));

		String uid2 = "test2_" + System.nanoTime();
		Item item2 = Item.create(uid2, UUID.randomUUID().toString());
		item2.displayName = "user2";
		itemStoreBook2.create(item2);
		item2 = itemStoreBook2.get(uid2);
		vCardStoreBook2.create(item2, card2);

		VCard card3 = defaultVCard();
		card3.identification.formatedName = VCard.Identification.FormatedName.create("user3");
		card3.communications.emails = Arrays.asList(VCard.Communications.Email.create("checked3@test.com"));

		String uid3 = "test3_" + System.nanoTime();
		Item item3 = Item.create(uid3, UUID.randomUUID().toString());
		item3.displayName = "user3";
		itemStoreBook2.create(item3);
		item3 = itemStoreBook2.get(uid3);
		vCardStoreBook2.create(item3, card3);

		VCard testCard = defaultVCard();
		testCard.kind = Kind.group;
		testCard.organizational.member = Arrays.asList(//
				VCard.Organizational.Member.create(book1.uid, uid1, "fakeName", "fakemailto@fake.com"),
				VCard.Organizational.Member.create(book2.uid, uid2, "fakeName", "fakemailto@fake.com"),
				VCard.Organizational.Member.create(book2.uid, uid3, "fakeName", "fakemailto@fake.com"),
				VCard.Organizational.Member.create("fakeUid", "fakeUid", "dontchange", "immutable@freeze.com"),
				VCard.Organizational.Member.create(null, "fakeUid2", "fakeuid2", "fakeuid2@freeze.com"));

		try {
			new VCardSanitizer(testContext).sanitize(testCard, Optional.of("bookUid"));
		} catch (ServerFault e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertEquals(5, testCard.organizational.member.size());
		VCard.Organizational.Member m = testCard.organizational.member.get(0);

		// check info of user1 was updated from container item
		assertEquals("user1", m.commonName);
		assertEquals("checked1@test.com", m.mailto);
		assertEquals(book1.uid, m.containerUid);

		// check info of user2 was updated from container item
		m = testCard.organizational.member.get(1);
		assertEquals("user2", m.commonName);
		assertEquals("checked2@test.com", m.mailto);
		assertEquals(book2.uid, m.containerUid);

		// check container uid of user2 was updated from container item
		m = testCard.organizational.member.get(2);
		assertEquals(book2.uid, m.containerUid);

		// member 4 is not an internal vcard so no update
		m = testCard.organizational.member.get(3);
		assertEquals("dontchange", m.commonName);
		assertEquals("immutable@freeze.com", m.mailto);
		assertNull(m.containerUid);

		m = testCard.organizational.member.get(4);
		assertEquals("fakeuid2", m.commonName);
		assertEquals("bookUid", m.containerUid);
	}

	@Test
	public void testTrimName() {
		VCard card = defaultVCard();
		card.identification.name = VCard.Identification.Name.create("familyNames  ", "givenNames  ",
				" additionnalNames  ", " prefix ", "suffix ", Arrays.<VCard.Parameter>asList());
		try {
			new VCardSanitizer(testContext).sanitize(card, Optional.empty());
		} catch (ServerFault e) {
			fail(e.getMessage());
		}

		assertEquals("givenNames additionnalNames familyNames", card.identification.formatedName.value);
		assertEquals("familyNames", card.identification.name.familyNames);
		assertEquals("givenNames", card.identification.name.givenNames);
		assertEquals("additionnalNames", card.identification.name.additionalNames);
		assertEquals("prefix", card.identification.name.prefixes);
		assertEquals("suffix", card.identification.name.suffixes);
	}

	@Test
	public void testTrimEmail() {
		VCard card = defaultVCard();
		card.communications.emails = Arrays.asList(VCard.Communications.Email.create("  this.is.calendar@bm.lan    "));
		try {
			new VCardSanitizer(testContext).sanitize(card, Optional.empty());
		} catch (ServerFault e) {
			fail(e.getMessage());
		}

		assertEquals("this.is.calendar@bm.lan", card.communications.emails.get(0).value);

	}

	@Test
	public void testDeleteDListMembersWithoutEmail() {
		VCard card = defaultVCard();

		Member member1 = Member.create("container", "item1", "hasMail1", "hasMail1@hasMail.org");
		Member member2 = Member.create("container", "item2", "hasNoMail2", null);
		Member member3 = Member.create("container", "item3", "hasNoMail3", null);
		Member member4 = Member.create("container", "item4", "hasMail4", "hasMail4@hasMail.org");
		Member member5 = Member.create("container", "item5", "hasNoMail5", null);

		card.kind = Kind.group;
		card.organizational.member = Arrays.asList(member1, member2, member3, member4, member5);

		try {
			new VCardSanitizer(testContext).sanitize(card, Optional.empty());
		} catch (ServerFault e) {
			fail(e.getMessage());
		}

		assertEquals(2, card.organizational.member.size());

		for (Member member : card.organizational.member) {
			assertTrue(member.commonName.startsWith("hasMail"));
		}

	}

	protected IAddressBook getService(SecurityContext context) {
		return new AddressBookService(dataDataSource, esearchClient, container, new BmTestContext(context));
	}
}
