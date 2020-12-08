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
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Communications.Email;
import net.bluemind.addressbook.api.VCard.Communications.Impp;
import net.bluemind.addressbook.api.VCard.Communications.Lang;
import net.bluemind.addressbook.api.VCard.Communications.Tel;
import net.bluemind.addressbook.api.VCard.DeliveryAddressing;
import net.bluemind.addressbook.api.VCard.DeliveryAddressing.Address;
import net.bluemind.addressbook.api.VCard.Explanatory.Url;
import net.bluemind.addressbook.api.VCard.Identification.FormatedName;
import net.bluemind.addressbook.api.VCard.Organizational.Member;
import net.bluemind.addressbook.api.VCard.Parameter;
import net.bluemind.addressbook.service.internal.VCardSanitizer;
import net.bluemind.core.api.fault.ServerFault;

public class BasicVCardSanitizerTest {

	@Test
	public void testSetNameAsFormattedName() {
		VCard card = defaultVCard();
		card.identification.name.givenNames = "Ferris";
		card.identification.name.familyNames = "Hilton";
		card.organizational.org.company = null;

		try {
			getSanitizer().sanitize(card, Optional.empty());
		} catch (ServerFault e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		assertEquals("default", card.identification.formatedName.value);

		card.identification.formatedName = null;

		try {
			getSanitizer().sanitize(card, Optional.empty());
		} catch (ServerFault e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		assertEquals("Ferris Hilton", card.identification.formatedName.value);

		card.identification.formatedName = FormatedName.create("  ");

		try {
			getSanitizer().sanitize(card, Optional.empty());
		} catch (ServerFault e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		assertEquals("Ferris Hilton", card.identification.formatedName.value);
	}

	@Test
	public void testSetCompanyNameAsFormattedNameIfNoNameIsPresent() {
		String company = "Deichkind";
		VCard card = defaultVCard();
		card.identification.name.givenNames = null;
		card.identification.name.familyNames = null;
		card.organizational.org.company = company;

		try {
			getSanitizer().sanitize(card, Optional.empty());
		} catch (ServerFault e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		assertEquals("default", card.identification.formatedName.value);

		card.identification.formatedName = null;

		try {
			getSanitizer().sanitize(card, Optional.empty());
		} catch (ServerFault e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		assertEquals(company, card.identification.formatedName.value);

		card.identification.formatedName = FormatedName.create("  ");

		try {
			getSanitizer().sanitize(card, Optional.empty());
		} catch (ServerFault e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		assertEquals(company, card.identification.formatedName.value);
	}

	@Test
	public void testSetEmailAsFormattedNameIfNoNameAndCompanyIsPresent() {
		String email = "test@test.com";
		VCard card = defaultVCard();
		card.identification.name.givenNames = null;
		card.identification.name.familyNames = null;
		card.organizational.org.company = null;

		List<Email> emails = new ArrayList<>();
		emails.add(Email.create(email));
		card.communications.emails = emails;

		try {
			getSanitizer().sanitize(card, Optional.empty());
		} catch (ServerFault e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		assertEquals("default", card.identification.formatedName.value);

		card.identification.formatedName = null;

		try {
			getSanitizer().sanitize(card, Optional.empty());
		} catch (ServerFault e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		assertEquals(email, card.identification.formatedName.value);

		card.identification.formatedName = FormatedName.create("   ");

		try {
			getSanitizer().sanitize(card, Optional.empty());
		} catch (ServerFault e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		assertEquals(email, card.identification.formatedName.value);
	}

	@Test
	public void testSanitizerShouldInsertMissingContainerUid() {
		String email = "test@test.com";
		VCard card = defaultVCard();
		card.identification.name.givenNames = null;
		card.identification.name.familyNames = null;
		card.organizational.org.company = null;
		card.organizational.member = new ArrayList<VCard.Organizational.Member>();
		VCard.Organizational.Member m = new VCard.Organizational.Member();
		m.commonName = "air conditioning";
		m.mailto = "killmy@planet.org";
		m.itemUid = "1234";
		VCard.Organizational.Member m2 = new VCard.Organizational.Member();
		m2.commonName = "wind";
		m2.mailto = "wind@ofchange.org";
		m2.itemUid = "4321";
		m2.containerUid = "alreadythere";
		VCard.Organizational.Member m3 = new VCard.Organizational.Member();
		m3.commonName = "ext";
		m3.mailto = "ext@ofchange.org";
		card.organizational.member.add(m);
		card.organizational.member.add(m2);
		card.organizational.member.add(m3);

		List<Email> emails = new ArrayList<>();
		emails.add(Email.create(email));
		card.communications.emails = emails;

		try {
			getSanitizer().sanitize(card, Optional.of("mycontainer"));
		} catch (ServerFault e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertEquals(3, card.organizational.member.size());

		for (Member member : card.organizational.member) {
			if (member.itemUid == null) {
				assertNull(member.containerUid);
			} else if (member.itemUid.equals("1234")) {
				assertEquals("mycontainer", member.containerUid);
			} else if (member.itemUid.equals("4321")) {
				assertEquals("alreadythere", member.containerUid);
			}
		}
	}

	@Test
	public void testSanitizerShouldRemoveEmptyBasicAttributes() {
		VCard card = defaultVCard();
		card.identification.formatedName.parameters = getEmptyAttributeTestList();
		card.identification.name.parameters = getEmptyAttributeTestList();
		card.identification.nickname.parameters = getEmptyAttributeTestList();
		card.identification.gender.parameters = getEmptyAttributeTestList();
		Address address = new Address();
		address.parameters = getEmptyAttributeTestList();
		DeliveryAddressing dAddressing1 = DeliveryAddressing.create(address);
		DeliveryAddressing dAddressing2 = DeliveryAddressing.create(address);
		card.deliveryAddressing = Arrays.asList(dAddressing1, dAddressing2);

		Url url1 = Url.create("blue-mind1.fr", getEmptyAttributeTestList());
		Url url2 = Url.create("blue-mind2.fr", getEmptyAttributeTestList());
		card.explanatory.urls = Arrays.asList(url1, url2);

		Email email1 = Email.create("1@blue-mind1.fr", getEmptyAttributeTestList());
		Email email2 = Email.create("2@blue-mind2.fr", getEmptyAttributeTestList());
		card.communications.emails = Arrays.asList(email1, email2);

		Tel tel1 = Tel.create("539858474", getEmptyAttributeTestList());
		Tel tel2 = Tel.create("343333333", getEmptyAttributeTestList());
		card.communications.tels = Arrays.asList(tel1, tel2);

		Impp impp1 = Impp.create("jabber1", getEmptyAttributeTestList());
		Impp impp2 = Impp.create("jabber2", getEmptyAttributeTestList());
		card.communications.impps = Arrays.asList(impp1, impp2);

		Lang lang1 = Lang.create("DE", getEmptyAttributeTestList());
		Lang lang2 = Lang.create("FR", getEmptyAttributeTestList());
		card.communications.langs = Arrays.asList(lang1, lang2);

		try {
			getSanitizer().sanitize(card, Optional.empty());
		} catch (ServerFault e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertEquals(1, card.identification.formatedName.parameters.size());
		assertEquals("good-key", card.identification.formatedName.parameters.get(0).label);
		assertEquals("good-value", card.identification.formatedName.parameters.get(0).value);

		assertEquals(1, card.identification.name.parameters.size());
		assertEquals("good-key", card.identification.name.parameters.get(0).label);
		assertEquals("good-value", card.identification.name.parameters.get(0).value);

		assertEquals(1, card.identification.nickname.parameters.size());
		assertEquals("good-key", card.identification.nickname.parameters.get(0).label);
		assertEquals("good-value", card.identification.nickname.parameters.get(0).value);

		assertEquals(1, card.identification.gender.parameters.size());
		assertEquals("good-key", card.identification.gender.parameters.get(0).label);
		assertEquals("good-value", card.identification.gender.parameters.get(0).value);

		assertEquals(2, card.deliveryAddressing.size());
		for (DeliveryAddressing da : card.deliveryAddressing) {
			assertEquals(1, da.address.parameters.size());
			assertEquals("good-key", da.address.parameters.get(0).label);
			assertEquals("good-value", da.address.parameters.get(0).value);
		}

		assertEquals(2, card.explanatory.urls.size());
		for (Url url : card.explanatory.urls) {
			assertEquals(1, url.parameters.size());
			assertEquals("good-key", url.parameters.get(0).label);
			assertEquals("good-value", url.parameters.get(0).value);
		}

		assertEquals(2, card.communications.emails.size());
		for (Email email : card.communications.emails) {
			assertEquals(1, email.parameters.size());
			assertEquals("good-key", email.parameters.get(0).label);
			assertEquals("good-value", email.parameters.get(0).value);
		}

		assertEquals(2, card.communications.tels.size());
		for (Tel tel : card.communications.tels) {
			assertEquals(1, tel.parameters.size());
			assertEquals("good-key", tel.parameters.get(0).label);
			assertEquals("good-value", tel.parameters.get(0).value);
		}

		assertEquals(2, card.communications.impps.size());
		for (Impp impp : card.communications.impps) {
			assertEquals(1, impp.parameters.size());
			assertEquals("good-key", impp.parameters.get(0).label);
			assertEquals("good-value", impp.parameters.get(0).value);
		}

		assertEquals(2, card.communications.langs.size());
		for (Lang lang : card.communications.langs) {
			assertEquals(1, lang.parameters.size());
			assertEquals("good-key", lang.parameters.get(0).label);
			assertEquals("good-value", lang.parameters.get(0).value);
		}
	}

	@Test
	public void testSanitizerShouldRemoveInvalidBasicAttributeCharacters() {
		VCard card = defaultVCard();
		card.identification.formatedName.parameters = getAttributeTestListContainingInvalidCharacters();
		card.identification.name.parameters = getAttributeTestListContainingInvalidCharacters();
		card.identification.nickname.parameters = getAttributeTestListContainingInvalidCharacters();
		card.identification.gender.parameters = getAttributeTestListContainingInvalidCharacters();
		Address address = new Address();
		address.parameters = getAttributeTestListContainingInvalidCharacters();
		DeliveryAddressing dAddressing1 = DeliveryAddressing.create(address);
		DeliveryAddressing dAddressing2 = DeliveryAddressing.create(address);
		card.deliveryAddressing = Arrays.asList(dAddressing1, dAddressing2);

		Url url1 = Url.create("blue-mind1.fr", getAttributeTestListContainingInvalidCharacters());
		Url url2 = Url.create("blue-mind2.fr", getAttributeTestListContainingInvalidCharacters());
		card.explanatory.urls = Arrays.asList(url1, url2);

		Email email1 = Email.create("1@blue-mind1.fr", getAttributeTestListContainingInvalidCharacters());
		Email email2 = Email.create("2@blue-mind2.fr", getAttributeTestListContainingInvalidCharacters());
		card.communications.emails = Arrays.asList(email1, email2);

		Tel tel1 = Tel.create("539858474", getAttributeTestListContainingInvalidCharacters());
		Tel tel2 = Tel.create("343333333", getAttributeTestListContainingInvalidCharacters());
		card.communications.tels = Arrays.asList(tel1, tel2);

		Impp impp1 = Impp.create("jabber1", getAttributeTestListContainingInvalidCharacters());
		Impp impp2 = Impp.create("jabber2", getAttributeTestListContainingInvalidCharacters());
		card.communications.impps = Arrays.asList(impp1, impp2);

		Lang lang1 = Lang.create("DE", getAttributeTestListContainingInvalidCharacters());
		Lang lang2 = Lang.create("FR", getAttributeTestListContainingInvalidCharacters());
		card.communications.langs = Arrays.asList(lang1, lang2);

		try {
			getSanitizer().sanitize(card, Optional.empty());
		} catch (ServerFault e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertEquals(1, card.identification.formatedName.parameters.size());
		assertEquals("good-key", card.identification.formatedName.parameters.get(0).label);
		assertEquals("good-value", card.identification.formatedName.parameters.get(0).value);

		assertEquals(1, card.identification.name.parameters.size());
		assertEquals("good-key", card.identification.name.parameters.get(0).label);
		assertEquals("good-value", card.identification.name.parameters.get(0).value);

		assertEquals(1, card.identification.nickname.parameters.size());
		assertEquals("good-key", card.identification.nickname.parameters.get(0).label);
		assertEquals("good-value", card.identification.nickname.parameters.get(0).value);

		assertEquals(1, card.identification.gender.parameters.size());
		assertEquals("good-key", card.identification.gender.parameters.get(0).label);
		assertEquals("good-value", card.identification.gender.parameters.get(0).value);

		assertEquals(2, card.deliveryAddressing.size());
		for (DeliveryAddressing da : card.deliveryAddressing) {
			assertEquals(1, da.address.parameters.size());
			assertEquals("good-key", da.address.parameters.get(0).label);
			assertEquals("good-value", da.address.parameters.get(0).value);
		}

		assertEquals(2, card.explanatory.urls.size());
		for (Url url : card.explanatory.urls) {
			assertEquals(1, url.parameters.size());
			assertEquals("good-key", url.parameters.get(0).label);
			assertEquals("good-value", url.parameters.get(0).value);
		}

		assertEquals(2, card.communications.emails.size());
		for (Email email : card.communications.emails) {
			assertEquals(1, email.parameters.size());
			assertEquals("good-key", email.parameters.get(0).label);
			assertEquals("good-value", email.parameters.get(0).value);
		}

		assertEquals(2, card.communications.tels.size());
		for (Tel tel : card.communications.tels) {
			assertEquals(1, tel.parameters.size());
			assertEquals("good-key", tel.parameters.get(0).label);
			assertEquals("good-value", tel.parameters.get(0).value);
		}

		assertEquals(2, card.communications.impps.size());
		for (Impp impp : card.communications.impps) {
			assertEquals(1, impp.parameters.size());
			assertEquals("good-key", impp.parameters.get(0).label);
			assertEquals("good-value", impp.parameters.get(0).value);
		}

		assertEquals(2, card.communications.langs.size());
		for (Lang lang : card.communications.langs) {
			assertEquals(1, lang.parameters.size());
			assertEquals("good-key", lang.parameters.get(0).label);
			assertEquals("good-value", lang.parameters.get(0).value);
		}
	}

	private List<Parameter> getEmptyAttributeTestList() {
		return Arrays.asList(new VCard.Parameter[] { Parameter.create("", "bad"), Parameter.create("bad", ""),
				Parameter.create("good-key", "good-value") });
	}

	private List<Parameter> getAttributeTestListContainingInvalidCharacters() {
		return Arrays.asList(new VCard.Parameter[] { Parameter.create("go;od-k;ey", ";good;;;;-;;value;") });
	}

	public VCardSanitizer getSanitizer() {
		return new TestVCardSanitizer(null);
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
