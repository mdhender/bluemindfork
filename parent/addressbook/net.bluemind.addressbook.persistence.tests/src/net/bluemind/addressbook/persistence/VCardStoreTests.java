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
package net.bluemind.addressbook.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.FormatedName;
import net.bluemind.addressbook.api.VCard.Parameter;
import net.bluemind.addressbook.api.VCard.Security.Key;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;

public class VCardStoreTests {
	private static Logger logger = LoggerFactory.getLogger(VCardStoreTests.class);
	private VCardStore vCardStore;
	private ItemStore itemStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		ContainerStore containerHome = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				securityContext);
		String containerId = "test_" + System.nanoTime();
		Container container = Container.create(containerId, "test", "test", "me", true);
		container = containerHome.create(container);

		assertNotNull(container);

		itemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), container, securityContext);

		vCardStore = new VCardStore(JdbcTestHelper.getInstance().getDataSource(), container);

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testSchemaIsWellRegsited() {
		assertNotNull(JdbcTestHelper.getInstance().getDbSchemaService().getSchemaDescriptor("vcard-schema"));
	}

	@Test
	public void testStoreAndRetrieveWithKind() throws SQLException {
		VCard card = defaultVCard();
		card.kind = VCard.Kind.group;
		VCard result = createAndGet(card);
		assertEquals(VCard.Kind.group, result.kind);

		card = defaultVCard();
		card.kind = VCard.Kind.individual;
		result = createAndGet(card);
		assertEquals(VCard.Kind.individual, result.kind);
	}

	@Test
	public void testGetMultiple() throws SQLException {
		VCard card = defaultVCard();
		card.source = "1";
		itemStore.create(Item.create("1", null));
		Item item = itemStore.get("1");
		vCardStore.create(item, card);

		VCard card2 = defaultVCard();
		card2.source = "2";
		itemStore.create(Item.create("2", null));
		Item item2 = itemStore.get("2");
		vCardStore.create(item2, card2);

		List<VCard> values = vCardStore.getMultiple(Arrays.asList(item, item2));
		assertEquals(2, values.size());
		assertEquals("1", values.get(0).source);
		assertEquals("2", values.get(1).source);

		values = vCardStore.getMultiple(Arrays.asList(item2, item));
		assertEquals(2, values.size());
		assertEquals("2", values.get(0).source);
		assertEquals("1", values.get(1).source);
	}

	@Test
	public void testStoreAndRetrieveWithSource() throws SQLException {
		VCard card = defaultVCard();
		card.source = "gg";
		VCard result = createAndGet(card);
		assertEquals("gg", result.source);
	}

	@Test
	public void testStoreAndRetrieveWithFN() throws SQLException {
		VCard card = defaultVCard();

		card.identification.formatedName = VCard.Identification.FormatedName.create("boris",
				Arrays.asList(Parameter.create("lang", "fr"), Parameter.create("fake", "value")));

		VCard result = createAndGet(card);

		assertEquals("boris", result.identification.formatedName.value);
		List<Parameter> parameters = result.identification.formatedName.parameters;
		assertEquals(2, parameters.size());
		assertEquals("lang", parameters.get(0).label);
		assertEquals("fr", parameters.get(0).value);

		assertEquals("fake", parameters.get(1).label);
		assertEquals("value", parameters.get(1).value);

		card = defaultVCard();

		card.identification.formatedName = VCard.Identification.FormatedName.create("umberto",
				Arrays.<VCard.Parameter>asList());

		result = createAndGet(card);

		assertEquals("umberto", result.identification.formatedName.value);
		assertEquals(0, result.identification.formatedName.parameters.size());
	}

	@Test
	public void testStoreAndRetrieveWithName() throws SQLException {
		VCard card = defaultVCard();

		card.identification.name = VCard.Identification.Name.create("Stevenson", "John", "Philip,Paul", "Jr.", "Mr",
				Arrays.asList(Parameter.create("lang", "fr"), Parameter.create("fake", "value")));

		VCard result = createAndGet(card);

		assertEquals("Stevenson", result.identification.name.familyNames);
		assertEquals("John", result.identification.name.givenNames);
		assertEquals("Philip,Paul", result.identification.name.additionalNames);
		assertEquals("Jr.", result.identification.name.prefixes);
		assertEquals("Mr", result.identification.name.suffixes);
		List<Parameter> parameters = result.identification.name.parameters;
		assertEquals(2, parameters.size());
		assertEquals("lang", parameters.get(0).label);
		assertEquals("fr", parameters.get(0).value);

		assertEquals("fake", parameters.get(1).label);
		assertEquals("value", parameters.get(1).value);

		card = defaultVCard();

		card.identification.name = VCard.Identification.Name.create("Stevenson", null, null, null, null,

				Arrays.asList(Parameter.create("lang", "fr"), Parameter.create("fake", "value")));

		result = createAndGet(card);

		assertEquals("Stevenson", result.identification.name.familyNames);
		assertNull(result.identification.name.givenNames);
		assertNull(result.identification.name.additionalNames);
		assertNull(result.identification.name.prefixes);
		assertNull(result.identification.name.suffixes);

	}

	@Test
	public void testStoreAndRetrieveWithNickname() throws SQLException {
		VCard card = defaultVCard();

		card.identification.nickname = VCard.Identification.Nickname.create("el barto");

		VCard result = createAndGet(card);
		assertEquals("el barto", result.identification.nickname.value);

	}

	@Test
	public void testStoreAndRetrieveWithBirthday() throws SQLException {
		VCard card = defaultVCard();

		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(0);
		c.set(Calendar.YEAR, 2001);
		c.set(Calendar.HOUR, 0);
		card.identification.birthday = c.getTime();

		VCard result = createAndGet(card);

		c = Calendar.getInstance();
		c.setTimeInMillis(0);
		c.set(Calendar.YEAR, 2001);
		c.set(Calendar.HOUR, 0);
		assertEquals(c.getTime(), result.identification.birthday);

	}

	@Test
	public void testStoreAndRetrieveWithAnniversary() throws SQLException {
		VCard card = defaultVCard();

		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(0);
		c.set(Calendar.YEAR, 2001);
		c.set(Calendar.HOUR, 0);
		card.identification.anniversary = c.getTime();

		VCard result = createAndGet(card);

		c = Calendar.getInstance();
		c.setTimeInMillis(0);
		c.set(Calendar.YEAR, 2001);
		c.set(Calendar.HOUR, 0);
		assertEquals(c.getTime(), result.identification.anniversary);

	}

	@Test
	public void testStoreAndRetrieveWithGender() throws SQLException {
		VCard card = defaultVCard();

		card.identification.gender = VCard.Identification.Gender.create("Foxy", "Because");

		VCard result = createAndGet(card);
		assertEquals("Foxy", result.identification.gender.value);
		assertEquals("Because", result.identification.gender.text);
	}

	@Test
	public void testStoreAndRetrieveWithDelivryAddresses() throws SQLException {
		VCard card = defaultVCard();

		VCard.DeliveryAddressing.Address addr = VCard.DeliveryAddressing.Address.create("test", "postOfficeBox",
				"extentedAddress", "streetAddress", "locality", "region", "zip", "countryName",
				Arrays.<VCard.Parameter>asList(VCard.Parameter.create("TYPE", "home"),
						VCard.Parameter.create("TYPE", "work")));
		VCard.DeliveryAddressing.Address addr2 = VCard.DeliveryAddressing.Address.create("test with empty",
				"postOfficeBox", null, "streetAddress", null, "region", "zip2", "countryName",
				Arrays.<VCard.Parameter>asList());

		card.deliveryAddressing = Arrays.asList(VCard.DeliveryAddressing.create(addr),
				VCard.DeliveryAddressing.create(addr2));

		VCard result = createAndGet(card);
		assertEquals(2, result.deliveryAddressing.size());
		VCard.DeliveryAddressing.Address a1 = result.deliveryAddressing.get(0).address;
		VCard.DeliveryAddressing.Address a2 = result.deliveryAddressing.get(1).address;
		assertEquals("test", a1.value);
		assertEquals("postOfficeBox", a1.postOfficeBox);
		assertEquals("extentedAddress", a1.extentedAddress);
		assertEquals("streetAddress", a1.streetAddress);
		assertEquals("locality", a1.locality);
		assertEquals("region", a1.region);
		assertEquals("zip", a1.postalCode);
		assertEquals("countryName", a1.countryName);
		assertEquals(2, a1.parameters.size());
		assertEquals("TYPE", a1.parameters.get(0).label);
		assertEquals("home", a1.parameters.get(0).value);
		assertEquals("TYPE", a1.parameters.get(1).label);
		assertEquals("work", a1.parameters.get(1).value);
		assertEquals("test with empty", a2.value);
		assertEquals("postOfficeBox", a2.postOfficeBox);
		assertNull(a2.extentedAddress);
		assertEquals("streetAddress", a2.streetAddress);
		assertNull(a2.locality);
		assertEquals("region", a2.region);
		assertEquals("zip2", a2.postalCode);
		assertEquals("countryName", a2.countryName);
	}

	@Test
	public void testStoreAndRetrieveWithTels() throws SQLException {
		VCard card = defaultVCard();

		VCard.Communications.Tel tel1 = VCard.Communications.Tel.create("666", Arrays.<VCard.Parameter>asList());
		VCard.Communications.Tel tel2 = VCard.Communications.Tel.create("55", Arrays.<VCard.Parameter>asList());
		card.communications.tels = Arrays.asList(tel1, tel2);

		VCard result = createAndGet(card);
		assertEquals(2, result.communications.tels.size());

		assertEquals("666", result.communications.tels.get(0).value);
		assertEquals("55", result.communications.tels.get(1).value);
	}

	@Test
	public void testStoreAndRetrieveWithEmails() throws SQLException {
		VCard card = defaultVCard();

		VCard.Communications.Email email1 = VCard.Communications.Email.create("boris.vian@herberouge.org",
				Arrays.<VCard.Parameter>asList(VCard.Parameter.create("PREF", "1")));
		VCard.Communications.Email email2 = VCard.Communications.Email.create("miles@davis.mus",
				Arrays.<VCard.Parameter>asList());
		card.communications.emails = Arrays.asList(email1, email2);

		VCard result = createAndGet(card);
		assertEquals(2, result.communications.emails.size());

		assertEquals("boris.vian@herberouge.org", result.communications.emails.get(0).value);
		assertEquals("1", result.communications.emails.get(0).getParameterValue("PREF"));
		assertEquals("miles@davis.mus", result.communications.emails.get(1).value);
	}

	@Test
	public void testStoreAndRetrieveWithImpp() throws SQLException {
		VCard card = defaultVCard();

		VCard.Communications.Impp impp1 = VCard.Communications.Impp.create("boris.vian@herberouge.org",
				Arrays.<VCard.Parameter>asList());
		VCard.Communications.Impp impp2 = VCard.Communications.Impp.create("miles@davis.mus",
				Arrays.<VCard.Parameter>asList());
		card.communications.impps = Arrays.asList(impp1, impp2);

		VCard result = createAndGet(card);
		assertEquals(2, result.communications.impps.size());

		assertEquals("boris.vian@herberouge.org", result.communications.impps.get(0).value);
		assertEquals("miles@davis.mus", result.communications.impps.get(1).value);
	}

	@Test
	public void testStoreAndRetrieveWithLang() throws SQLException {
		VCard card = defaultVCard();

		VCard.Communications.Lang lang1 = VCard.Communications.Lang.create("francais",
				Arrays.<VCard.Parameter>asList());
		VCard.Communications.Lang lang2 = VCard.Communications.Lang.create("catalan", Arrays.<VCard.Parameter>asList());
		card.communications.langs = Arrays.asList(lang1, lang2);

		VCard result = createAndGet(card);
		assertEquals(2, result.communications.langs.size());

		assertEquals("francais", result.communications.langs.get(0).value);
		assertEquals("catalan", result.communications.langs.get(1).value);
	}

	@Test
	public void testStoreAndRetrieveOrganizational() throws SQLException {
		VCard card = defaultVCard();

		VCard.Organizational organizational = VCard.Organizational.create("Loser", "Boss", //
				VCard.Organizational.Org.create("Blue-mind", "tlse", "Dev"), //
				Arrays.<VCard.Organizational.Member>asList());

		card.organizational = organizational;

		VCard result = createAndGet(card);
		assertEquals("Loser", result.organizational.title);
		assertEquals("Boss", result.organizational.role);

		assertEquals("Blue-mind", result.organizational.org.company);
		assertEquals("tlse", result.organizational.org.division);
		assertEquals("Dev", result.organizational.org.department);
	}

	@Test
	public void testStoreAndRetrieveMembers() throws SQLException {
		VCard card = defaultVCard();
		card.kind = VCard.Kind.group;
		card.identification.formatedName = FormatedName.create("testGroup");
		card.organizational.member = Arrays.asList(//
				VCard.Organizational.Member.create("testContainer", "test", "joe", "joe@do.com"), //
				VCard.Organizational.Member.create("testContainer2", "test2", "tina", "tina@turner.com"));
		VCard result = createAndGet(card);
		assertEquals(VCard.Kind.group, result.kind);
		List<VCard.Organizational.Member> members = result.organizational.member;
		assertEquals(2, members.size());

		assertEquals("testContainer", members.get(0).containerUid);
		assertEquals("test", members.get(0).itemUid);
		assertEquals("joe", members.get(0).commonName);
		assertEquals("joe@do.com", members.get(0).mailto);
		assertEquals("testContainer2", members.get(1).containerUid);
		assertEquals("test2", members.get(1).itemUid);
		assertEquals("tina", members.get(1).commonName);
		assertEquals("tina@turner.com", members.get(1).mailto);
	}

	@Test
	public void testStoreAndRetrieveWithUrl() throws SQLException {
		VCard card = defaultVCard();
		card.explanatory.urls = Arrays.asList(
				VCard.Explanatory.Url.create("http://test.com", Arrays.asList(VCard.Parameter.create("TYPE", "WORK"))),
				VCard.Explanatory.Url.create("http://home.com", Arrays.<VCard.Parameter>asList()));
		VCard result = createAndGet(card);
		assertEquals(2, result.explanatory.urls.size());
		VCard.Explanatory.Url url = card.explanatory.urls.get(0);
		assertEquals("http://test.com", url.value);
		assertEquals("WORK", url.getParameterValue("TYPE"));

		url = card.explanatory.urls.get(1);
		assertEquals("http://home.com", url.value);
	}

	@Test
	public void testStoreAndRetrieveWithKey() throws SQLException {
		VCard card = defaultVCard();
		List<Parameter> parameters = Arrays.asList(new Parameter[] { Parameter.create("data", "smime") });
		card.security.key = Key.create("MIICajCCAdOgAwIBAgICBE", parameters);

		VCard result = createAndGet(card);
		Key key = result.security.key;
		assertEquals("MIICajCCAdOgAwIBAgICBE", key.value);
		assertEquals("smime", key.getParameterValue("data"));
	}

	@Test
	public void testStoreAndRetrieveWithNote() throws SQLException {
		VCard card = defaultVCard();
		card.explanatory.note = "coucou";
		VCard result = createAndGet(card);
		assertEquals("coucou", result.explanatory.note);
	}

	@Test
	public void testStoreAndRetrieveWithSpouse() throws SQLException {
		VCard card = defaultVCard();

		card.related.spouse = "Clara Morgane";

		VCard result = createAndGet(card);
		assertEquals("Clara Morgane", result.related.spouse);
	}

	@Test
	public void testStoreAndRetrieveWithManager() throws SQLException {
		VCard card = defaultVCard();

		card.related.manager = "big boss";

		VCard result = createAndGet(card);
		assertEquals("big boss", result.related.manager);
	}

	@Test
	public void testStoreAndRetrieveWithAssistant() throws SQLException {
		VCard card = defaultVCard();

		card.related.assistant = "Sylvain Garcia";

		VCard result = createAndGet(card);
		assertEquals("Sylvain Garcia", result.related.assistant);
	}

	@Test
	public void testUpdate() throws SQLException {
		VCard card = defaultVCard();
		String uid = "test_" + System.nanoTime();

		VCard result = createAndGet(uid, card);
		Item item = itemStore.get(uid);

		result.identification.nickname = VCard.Identification.Nickname.create("John !!");
		vCardStore.update(item, result);

		result = vCardStore.get(item);
		assertEquals("John !!", result.identification.nickname.value);
	}

	@Test
	public void testDelete() throws SQLException {
		VCard card = defaultVCard();
		String uid = "test_" + System.nanoTime();

		createAndGet(uid, card);
		Item item = itemStore.get(uid);

		vCardStore.delete(item);
		assertNull(vCardStore.get(item));
	}

	private VCard createAndGet(String uid, VCard card) {
		try {
			itemStore.create(Item.create(uid, UUID.randomUUID().toString()));
			Item item = itemStore.get(uid);

			vCardStore.create(item, card);

			return vCardStore.get(item);

		} catch (SQLException e) {
			logger.error("error during vcard persistence call", e);
			fail(e.getMessage());
			return null;
		}

	}

	private VCard createAndGet(VCard card) {
		String uid = "test_" + System.nanoTime();
		return createAndGet(uid, card);
	}

	private VCard defaultVCard() {
		VCard card = new VCard();

		card.identification = new VCard.Identification();

		card.identification.formatedName = VCard.Identification.FormatedName.create("default",
				Arrays.<VCard.Parameter>asList());
		return card;
	}

}
