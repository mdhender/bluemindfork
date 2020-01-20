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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.IVCardService;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.DeliveryAddressing;
import net.bluemind.addressbook.api.VCard.Identification.FormatedName;
import net.bluemind.addressbook.api.VCard.Organizational.Member;
import net.bluemind.addressbook.api.VCard.Parameter;
import net.bluemind.addressbook.service.internal.AddressBookService;
import net.bluemind.addressbook.service.internal.VCardContainerStoreService;
import net.bluemind.addressbook.service.internal.VCardService;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.ical4j.vcard.Builder;
import net.bluemind.utils.FileUtils;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.vcard.Property;
import net.fortuna.ical4j.vcard.Property.Id;
import net.fortuna.ical4j.vcard.VCardBuilder;
import net.fortuna.ical4j.vcard.property.BDay;
import net.fortuna.ical4j.vcard.property.Gender;
import net.fortuna.ical4j.vcard.property.N;
import net.fortuna.ical4j.vcard.property.Nickname;

public class VCardServiceImportTests extends AbstractServiceTests {

	protected IAddressBook getService(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(defaultSecurityContext).instance(IAddressBook.class,
				container.uid);
	}

	@Test
	public void testImportFN() throws ServerFault {
		ItemValue<VCard> imported = importProperty(false, "FN:test 1");

		FormatedName fn = imported.value.identification.formatedName;
		assertNotNull(fn);
		assertEquals("test 1", fn.value);
	}

	@Test
	public void testImportN() throws ServerFault {
		ItemValue<VCard> imported = importProperty(true, "N:barrett;syd");

		VCard.Identification.Name name = imported.value.identification.name;
		assertNotNull(name);
		assertEquals("barrett", name.familyNames);
		assertEquals("syd", name.givenNames);

		// Family Names (also known as surnames),
		// Given Names,
		// Additional Names,
		// Honorific Prefixes,
		// and Honorific Suffixes.

		imported = importProperty(true, "N:barrett;syd;acid;mr;music");

		name = imported.value.identification.name;
		assertNotNull(name);
		assertEquals("barrett", name.familyNames);
		assertEquals("syd", name.givenNames);
		assertEquals("acid", name.additionalNames);
		assertEquals("mr", name.prefixes);
		assertEquals("music", name.suffixes);
	}

	@Test
	public void testImportNickname() throws ServerFault {
		ItemValue<VCard> imported = importProperty(true, "NICKNAME:acid");
		VCard.Identification.Nickname nn = imported.value.identification.nickname;
		assertNotNull(nn);
		assertEquals("acid", nn.value);
	}

	@Test
	public void testImportGender() throws ServerFault {
		ItemValue<VCard> imported = importProperty(true, "GENDER:M");
		VCard.Identification.Gender gender = imported.value.identification.gender;
		assertNotNull(gender);
		assertEquals("M", gender.value);
	}

	@Test
	public void testImportBDay() throws ServerFault {
		ItemValue<VCard> imported = importProperty(true, "BDAY:20010101");
		Date bday = imported.value.identification.birthday;
		assertNotNull(bday);
		try {
			importProperty(true, "BDAY:-1-1-1");
		} catch (ServerFault e) {
			fail();
		}

		try {
			imported = importProperty(true, "BDAY:2001-01-01");
		} catch (ServerFault e) {
			fail();
		}
		bday = imported.value.identification.birthday;
		assertNotNull(bday);
	}

	@Test
	public void testImportAddress() throws ServerFault {
		ItemValue<VCard> imported = importProperty(true, "ADR;TYPE=WORK:po;ext;123 Main Street;Any Town;CA;;U.S.A.;");

		assertEquals(1, imported.value.deliveryAddressing.size());
		VCard.DeliveryAddressing da = imported.value.deliveryAddressing.get(0);
		assertEquals("po", da.address.postOfficeBox);
		assertEquals("ext", da.address.extentedAddress);

		assertEquals("123 Main Street", da.address.streetAddress);
		assertEquals("Any Town", da.address.locality);
		assertEquals("CA", da.address.region);
		assertEquals("U.S.A.", da.address.countryName);
		assertEquals(Arrays.asList("work"), da.address.getParameterValues("TYPE"));

	}

	@Test
	public void testImportPhone() throws ServerFault {
		ItemValue<VCard> imported = importProperty(true, "TEL;TYPE=WORK,Voice:+666");
		assertEquals(1, imported.value.communications.tels.size());
		assertEquals("+666", imported.value.communications.tels.get(0).value);
		assertEquals(Arrays.asList("work", "voice"),
				imported.value.communications.tels.get(0).getParameterValues("TYPE"));
	}

	@Test
	public void testImportEmails() throws ServerFault {
		ItemValue<VCard> imported = importProperty(true, "EMAIL;TYPE=WORK:test@test.com");
		assertEquals(1, imported.value.communications.emails.size());
		assertEquals("test@test.com", imported.value.communications.emails.get(0).value);
		assertEquals(Arrays.asList("work"), imported.value.communications.emails.get(0).getParameterValues("TYPE"));
	}

	@Test
	public void testImportImpps() throws ServerFault {
		ItemValue<VCard> imported = importProperty(true, "IMPP;TYPE=WORK:xmpp:test@test.com");
		assertEquals(1, imported.value.communications.impps.size());
		assertEquals("xmpp:test@test.com", imported.value.communications.impps.get(0).value);
		assertEquals(Arrays.asList("work"), imported.value.communications.impps.get(0).getParameterValues("TYPE"));
	}

	@Test
	public void testImportGroupMember() throws ServerFault {

		// complete values
		ItemValue<VCard> imported = importProperty(true, "X-ADDRESSBOOKSERVER-KIND:group",
				"X-ADDRESSBOOKSERVER-MEMBER;X-CN=testmember;X-MAILTO=testmember@bm.loc:urn:uuid:addressbook_bm.loc#0E47F82D-457D-4085-BDB7-D2655636B640");
		assertEquals(1, imported.value.organizational.member.size());
		Member member = imported.value.organizational.member.get(0);
		assertEquals("testmember", member.commonName);
		assertEquals("testmember@bm.loc", member.mailto);
		// FIXME item and container get deleted by VCardGroupSanitizer#cleanupMembersOf
		// assertEquals("0E47F82D-457D-4085-BDB7-D2655636B640", member.itemUid); //
		// assertEquals("addressbook_bm.loc", member.containerUid);

		// email but no cn, cn == uid
		imported = importProperty(true, "X-ADDRESSBOOKSERVER-KIND:group",
				"X-ADDRESSBOOKSERVER-MEMBER;X-MAILTO=testmember@bm.loc:urn:uuid:addressbook_bm.loc#0E47F82D-457D-4085-BDB7-D2655636B640");
		assertEquals(1, imported.value.organizational.member.size());
		member = imported.value.organizational.member.get(0);
		assertEquals("0E47F82D-457D-4085-BDB7-D2655636B640", member.commonName);
		assertEquals("testmember@bm.loc", member.mailto);

		// no email no cn, cn == uid
		imported = importProperty(true, "X-ADDRESSBOOKSERVER-KIND:group",
				"X-ADDRESSBOOKSERVER-MEMBER;X-MAILTO=testmember@bm.loc:urn:uuid:addressbook_bm.loc#0E47F82D-457D-4085-BDB7-D2655636B640");
		assertEquals(1, imported.value.organizational.member.size());
		member = imported.value.organizational.member.get(0);
		assertEquals("0E47F82D-457D-4085-BDB7-D2655636B640", member.commonName);
		assertEquals("testmember@bm.loc", member.mailto);

		// simple email
		imported = importProperty(true, "X-ADDRESSBOOKSERVER-KIND:group",
				"X-ADDRESSBOOKSERVER-MEMBER;:urn:uuid:testmember@bm.loc");
		assertEquals(1, imported.value.organizational.member.size());
		member = imported.value.organizational.member.get(0);
		assertEquals("testmember@bm.loc", member.commonName);
		assertEquals("testmember@bm.loc", member.mailto);
	}

	// TODO test import organization

	private ItemValue<VCard> importProperty(boolean appendFN, String... propertyValue) throws ServerFault {

		VCardService service = new VCardService(context, (AddressBookService) getService(defaultSecurityContext),
				container);

		StringBuilder sb = new StringBuilder();
		sb.append("BEGIN:VCARD\n");
		sb.append("VERSION:3.0\n");
		if (appendFN) {
			sb.append("FN:test 1\n");
		}
		for (String val : propertyValue) {
			sb.append(val + "\n");
		}
		sb.append("END:VCARD\n");

		List<String> uids = service.directImportCards(sb.toString()).uids;
		assertEquals(1, uids.size());
		String uid = uids.get(0);
		assertNotNull(uid);
		ItemValue<VCard> imported = getService(defaultSecurityContext).getComplete(uid);
		return imported;
	}

	@Test
	public void testExportName() throws ServerFault {
		VCard card = defaultVCard();
		card.identification.name = VCard.Identification.Name.create("familyNames", null, "additionalNames", "pr", "su",
				Arrays.<VCard.Parameter>asList());

		net.fortuna.ical4j.vcard.VCard export = export(card);
		N n = (N) export.getProperty(Id.N);
		assertNotNull(n);
		assertEquals("familyNames", n.getFamilyName());
		assertEquals("", n.getGivenName());
		assertEquals("additionalNames", n.getAdditionalNames()[0]);
		assertEquals("pr", n.getPrefixes()[0]);
		assertEquals("su", n.getSuffixes()[0]);
	}

	@Test
	public void testExportNickname() throws ServerFault {
		VCard card = defaultVCard();
		card.identification.nickname = VCard.Identification.Nickname.create("jojo");

		net.fortuna.ical4j.vcard.VCard export = export(card);
		Nickname nickname = (Nickname) export.getProperty(Id.NICKNAME);
		assertNotNull(nickname);
		assertEquals("jojo", nickname.getValue());
	}

	@Test
	public void testExportGender() throws ServerFault {
		VCard card = defaultVCard();
		card.identification.gender = VCard.Identification.Gender.create("M", "coucou");

		net.fortuna.ical4j.vcard.VCard export = export(card);
		Gender gender = (Gender) export.getProperty(Id.GENDER);
		assertNotNull(gender);
		assertEquals("M", gender.getValue());
	}

	@Test
	public void testExportBDay() throws ServerFault, ParseException {
		VCard card = defaultVCard();
		card.identification.birthday = new SimpleDateFormat("yyyyMMdd").parse("20010101");

		net.fortuna.ical4j.vcard.VCard export = export(card);
		BDay bday = (BDay) export.getProperty(Id.BDAY);
		assertNotNull(bday);
		// TODO test date value
	}

	@Test
	public void testExportAddress() throws ServerFault {
		VCard card = defaultVCard();
		card.deliveryAddressing = Arrays.asList(
				VCard.DeliveryAddressing.create(VCard.DeliveryAddressing.Address.create("coucou", "po", "ext",
						"123 Main Street", "Any Town", "CA", "zip", "U.S.A", Arrays.<Parameter>asList())),
				VCard.DeliveryAddressing.create(VCard.DeliveryAddressing.Address.create("coucou2", "po2", "ext",
						"123 Main Street", "Any Town", "CA", "zip2", "U.S.A", Arrays.<Parameter>asList())));

		net.fortuna.ical4j.vcard.VCard export = export(card);
		List<Property> props = export.getProperties(Id.ADR);
		assertNotNull(props);
		assertEquals(2, props.size());

		assertEquals("po;ext;123 Main Street;Any Town;CA;zip;U.S.A;", props.get(0).getValue());
		assertEquals("po2;ext;123 Main Street;Any Town;CA;zip2;U.S.A;", props.get(1).getValue());
	}

	@Test
	public void testExportPhone() throws ServerFault {
		VCard card = defaultVCard();
		card.communications.tels = Arrays.asList(
				VCard.Communications.Tel.create("+666", Arrays.asList(Parameter.create("TYPE", "WORK"))),
				VCard.Communications.Tel.create("+777", Arrays.asList(Parameter.create("TYPE", "HOME"))));

		net.fortuna.ical4j.vcard.VCard export = export(card);
		List<Property> props = export.getProperties(Id.TEL);
		assertEquals(2, props.size());
		assertEquals("+666", props.get(0).getValue());
		assertEquals("WORK", props.get(0).getParameter(net.fortuna.ical4j.vcard.Parameter.Id.TYPE).getValue());

		assertEquals("+777", props.get(1).getValue());
		assertEquals("HOME", props.get(1).getParameter(net.fortuna.ical4j.vcard.Parameter.Id.TYPE).getValue());
	}

	@Test
	public void testExportTitle() throws ServerFault {
		VCard card = defaultVCard();
		net.fortuna.ical4j.vcard.VCard export = export(card);
		Property title = export.getProperty(Id.TITLE);
		assertNotNull(title);
		assertEquals(card.organizational.title, title.getValue());
	}

	@Test
	public void testExportImportHtmlNote() throws ServerFault {
		VCard card = defaultVCard();
		card.explanatory.note = "<div><span>Coucou</span></div>";
		String uid = "junit-" + System.nanoTime();
		VCardContainerStoreService vStore = getVCardStore();
		vStore.create(uid, "blabla", card);

		IVCardService service = getVCardService();
		String vcard = service.exportCards(Arrays.asList(uid));
		assertTrue(vcard.contains("NOTE:Coucou"));
		assertTrue(vcard.contains("X-NOTE-HTML:<div><span>Coucou</span></div>"));

		String cleaned = String.join("\n", Arrays.asList(vcard.split("\n")).stream()
				.filter(line -> !line.startsWith("UID")).collect(Collectors.toList()).toArray(new String[0]));

		List<String> uids = getVCardService().directImportCards(cleaned).uids;

		ItemValue<VCard> imported = getVCardStore().get(uids.get(0), null);

		assertEquals("<div><span>Coucou</span></div>", imported.value.explanatory.note);
	}

	@Test
	public void testExportImportBrokenHtmlNote() throws ServerFault {
		VCard card = defaultVCard();
		card.explanatory.note = "<div><span>Coucou</div";
		String uid = "junit-" + System.nanoTime();
		VCardContainerStoreService vStore = getVCardStore();
		vStore.create(uid, "blabla", card);

		IVCardService service = getVCardService();
		String vcard = service.exportCards(Arrays.asList(uid));
		assertTrue(vcard.contains("NOTE:Coucou"));
		assertTrue(vcard.contains("X-NOTE-HTML:<div><span>Coucou</div"));

		String cleaned = String.join("\n", Arrays.asList(vcard.split("\n")).stream()
				.filter(line -> !line.startsWith("UID")).collect(Collectors.toList()).toArray(new String[0]));

		List<String> uids = getVCardService().directImportCards(cleaned).uids;

		ItemValue<VCard> imported = getVCardStore().get(uids.get(0), null);

		assertEquals("<div><span>Coucou</div", imported.value.explanatory.note);
	}

	@Test
	public void testExportPlainNote() throws ServerFault {
		VCard card = defaultVCard();
		card.explanatory.note = "Coucou";
		String uid = "junit-" + System.nanoTime();
		VCardContainerStoreService vStore = getVCardStore();
		vStore.create(uid, "blabla", card);

		IVCardService service = getVCardService();
		String vcard = service.exportCards(Arrays.asList(uid));
		assertTrue(vcard.contains("NOTE:Coucou"));
		assertFalse(vcard.contains("X-NOTE-HTML"));

		String cleaned = String.join("\n", Arrays.asList(vcard.split("\n")).stream()
				.filter(line -> !line.startsWith("UID")).collect(Collectors.toList()).toArray(new String[0]));

		List<String> uids = getVCardService().directImportCards(cleaned).uids;

		ItemValue<VCard> imported = getVCardStore().get(uids.get(0), null);

		assertEquals("Coucou", imported.value.explanatory.note);
	}

	@Test
	public void testExportEMail() throws ServerFault {
		VCard card = defaultVCard();
		card.communications.emails = Arrays.asList(
				VCard.Communications.Email.create("test@1.com", Arrays.asList(Parameter.create("TYPE", "WORK"))),
				VCard.Communications.Email.create("test@2.com", Arrays.asList(Parameter.create("TYPE", "HOME"))));

		net.fortuna.ical4j.vcard.VCard export = export(card);
		List<Property> props = export.getProperties(Id.EMAIL);
		assertEquals(2, props.size());
		assertEquals("test@1.com", props.get(0).getValue());
		assertEquals("WORK", props.get(0).getParameter(net.fortuna.ical4j.vcard.Parameter.Id.TYPE).getValue());

		assertEquals("test@2.com", props.get(1).getValue());
		assertEquals("HOME", props.get(1).getParameter(net.fortuna.ical4j.vcard.Parameter.Id.TYPE).getValue());

	}

	@Test
	public void testExportIMPP() throws ServerFault {
		VCard card = defaultVCard();
		card.communications.impps = Arrays.asList(
				VCard.Communications.Impp.create("xmpp:test@1.com", Arrays.<Parameter>asList()),

				VCard.Communications.Impp.create("xmpp:test@2.com", Arrays.<Parameter>asList()));

		net.fortuna.ical4j.vcard.VCard export = export(card);
		List<Property> props = export.getProperties(Id.IMPP);
		assertEquals(2, props.size());
		assertEquals("xmpp:test@1.com", props.get(0).getValue());

		assertEquals("xmpp:test@2.com", props.get(1).getValue());
	}

	@Test
	public void testExportGroup() throws ServerFault {

		VCard card = new VCard();
		card.kind = VCard.Kind.group;
		card.identification.formatedName = VCard.Identification.FormatedName.create("dlist demo",
				Arrays.<VCard.Parameter>asList());

		card.organizational.member = Arrays.asList(VCard.Organizational.Member.create(null, null, "gg", "gg@test.com"),
				VCard.Organizational.Member.create("test", "uri", "jojo", "jojo@bm.lan"),
				VCard.Organizational.Member.create(container.uid, "uri2", "jojo2", "jojo2@bm.lan"));

		net.fortuna.ical4j.vcard.VCard export = export(card);
		System.out.println("-----\n" + export.toString() + "\n-----");
		List<Property> props = export.getProperties(Id.EXTENDED);
		Multimap<String, Property> extIdx = ArrayListMultimap.create();
		for (Property p : props) {
			String asString = p.toString();
			int idx = asString.indexOf(':');
			if (asString.startsWith("X-ADDRESSBOOKSERVER-MEMBER")) {
				asString = "X-ADDRESSBOOKSERVER-MEMBER";
			} else {
				asString = asString.substring(0, idx);
			}
			extIdx.put(asString, p);
		}
		Property serverKind = extIdx.get("X-ADDRESSBOOKSERVER-KIND").iterator().next();
		assertNotNull(serverKind);
		assertEquals("group", serverKind.getValue());

		Collection<Property> memberProps = extIdx.get("X-ADDRESSBOOKSERVER-MEMBER");
		assertEquals(3, memberProps.size());
		Iterator<Property> it = memberProps.iterator();
		assertEquals("urn:uuid:gg@test.com", it.next().getValue());
		Property jojo = it.next();
		assertEquals("jojo@bm.lan", jojo.getExtendedParameter("X-MAILTO").getValue());
		assertEquals("jojo", jojo.getExtendedParameter("X-CN").getValue());
		assertEquals("urn:uuid:test#uri", jojo.getValue());
		Property jojo2 = it.next();
		assertEquals("jojo2@bm.lan", jojo2.getExtendedParameter("X-MAILTO").getValue());
		assertEquals("jojo2", jojo2.getExtendedParameter("X-CN").getValue());
		assertEquals("urn:uuid:uri2", jojo2.getValue());
	}

	@Test
	public void testImportZimbraFileVcard() throws IOException, ServerFault {

		List<String> uids = importCards("vcfFile/zimbra.vcf");
		assertEquals(1, uids.size());

		ItemValue<VCard> cardItem = getService(defaultSecurityContext).getComplete(uids.get(0));
		VCard card = cardItem.value;
		assertEquals("John Doe", card.identification.formatedName.value);
		assertEquals("Doe", card.identification.name.familyNames);
		assertEquals("John", card.identification.name.givenNames);

		assertEquals(1, card.communications.emails.size());

		VCard.Communications.Email email = card.communications.emails.get(0);
		assertEquals("internet", email.getParameterValue("TYPE"));
		assertEquals("john.doe@domain.lan", email.value);

	}

	@Test
	public void testImportFileVcard() throws IOException, ServerFault {

		List<String> uids = importCards("vcfFile/john.vcf");
		assertEquals(1, uids.size());
		ItemValue<VCard> cardItem = getService(defaultSecurityContext).getComplete(uids.get(0));
		VCard card = cardItem.value;

		assertEquals("John", card.identification.name.givenNames);
		assertEquals("Bang", card.identification.name.familyNames);

		assertEquals(1, card.communications.emails.size());

		VCard.Communications.Email email = card.communications.emails.get(0);
		assertEquals("INTERNET", email.getParameterValue("TYPE"));
		assertEquals("john.john@aa.com", email.value);

		assertEquals("CEO", card.organizational.role);
		assertEquals("this is title", card.organizational.title);
		assertEquals("BlueMind", card.organizational.org.company);

		assertEquals(1, card.communications.tels.size());
		VCard.Communications.Tel tel = card.communications.tels.get(0);

		assertEquals("cell", tel.getParameterValue("TYPE"));
		assertEquals("0607080910", tel.value);
	}

	@Test
	public void testImportFileAllVcard() throws IOException, ServerFault {
		List<String> uids = importCards("vcfFile/contacts.vcf");
		assertFalse(uids.isEmpty());
	}

	@Test
	public void testGMailVcard() throws IOException, ServerFault {
		List<String> uids = importCards("vcfFile/tomGmailContacts.vcf");
		assertFalse(uids.isEmpty());
	}

	@Test
	public void testVCardBirthday() throws IOException, ServerFault {
		List<String> uids = importCards("vcfFile/vcardBirthday.vcf");

		assertEquals(3, uids.size());

		ItemValue<VCard> cardItem = getService(defaultSecurityContext).getComplete(uids.get(0));
		VCard card = cardItem.value;
		assertNotNull(card.identification.birthday);
		assertDate(1983, 1, 13, card.identification.birthday);

		cardItem = getService(defaultSecurityContext).getComplete(uids.get(1));
		card = cardItem.value;
		assertNotNull(card.identification.birthday);
		assertDate(1983, 1, 13, card.identification.birthday);

		cardItem = getService(defaultSecurityContext).getComplete(uids.get(2));
		card = cardItem.value;
		assertNotNull(card.identification.birthday);
		assertDate(1983, 1, 13, card.identification.birthday);
	}

	@Test
	public void testVCardUrl() throws IOException, ServerFault {
		List<String> uids = importCards("vcfFile/vcardUrl.vcf");

		assertEquals(1, uids.size());

		ItemValue<VCard> cardItem = getService(defaultSecurityContext).getComplete(uids.get(0));
		VCard card = cardItem.value;

		assertEquals(2, card.explanatory.urls.size());

		assertEquals("http://www.blue-mind.net", card.explanatory.urls.get(0).value);
		assertEquals("http://twitter.com/_bluemind", card.explanatory.urls.get(1).value);

	}

	@Test
	public void testVCardFolded() throws IOException, ServerFault {
		List<String> uids = importCards("vcfFile/folded.vcf");

		assertEquals(1, uids.size());

		ItemValue<VCard> cardItem = getService(defaultSecurityContext).getComplete(uids.get(0));
		VCard card = cardItem.value;

		assertEquals(1, card.communications.emails.size());

		VCard.Communications.Email email = card.communications.emails.get(0);
		assertEquals("aaaaaaa.bbbbbbb@cccc-dddddddd-eeeeeeeeeeeeee.fr", email.value);
		assertEquals(Arrays.asList("INTERNET", "home"), email.getParameterValues("TYPE"));
	}

	@Test
	public void testVCardDuplicateEntries() throws IOException, ServerFault {
		List<String> uids = importCards("vcfFile/vcardDuplicateEntries.vcf");
		assertEquals(2, uids.size());

		uids = importCards("vcfFile/vcardDuplicateEntries.vcf");
		assertEquals(2, uids.size());
	}

	@Test
	public void testVCardInvalidEmail() throws IOException, ServerFault {
		List<String> uids = importCards("vcfFile/vcardInvalidEmail.vcf");
		assertEquals(1, uids.size());

		ItemValue<VCard> cardItem = getService(defaultSecurityContext).getComplete(uids.get(0));
		VCard card = cardItem.value;

		assertEquals("Phan", card.identification.name.familyNames);
		assertEquals("David", card.identification.name.givenNames);

	}

	@Test
	public void testVCardPhones() throws IOException, ServerFault {
		List<String> uids = importCards("vcfFile/vcardPhones.vcf");
		assertEquals(1, uids.size());

		ItemValue<VCard> cardItem = getService(defaultSecurityContext).getComplete(uids.get(0));
		VCard card = cardItem.value;

		assertEquals("Phan", card.identification.name.familyNames);
		assertEquals("David", card.identification.name.givenNames);

		assertEquals(5, card.communications.tels.size());
		VCard.Communications.Tel tel = card.communications.tels.get(0);
		assertEquals("+33 6 00 00 00 00", tel.value);
		assertEquals(Arrays.asList("cell"), tel.getParameterValues("TYPE"));

		tel = card.communications.tels.get(1);
		assertEquals("05 01 02 03 04", tel.value);
		assertEquals(Arrays.asList("work", "fax"), tel.getParameterValues("TYPE"));

		tel = card.communications.tels.get(2);
		assertEquals("05 91 92 93 94", tel.value);
		assertEquals(Arrays.asList("work"), tel.getParameterValues("TYPE"));

		tel = card.communications.tels.get(3);
		assertEquals("+1-213-555-1234", tel.value);
		assertEquals(Arrays.asList("work", "voice", "pref", "msg"), tel.getParameterValues("TYPE"));

		tel = card.communications.tels.get(4);
		assertEquals("+33 5 00 01 02 03", tel.value);
		assertEquals(Arrays.asList("dom", "voice"), tel.getParameterValues("TYPE"));

	}

	@Test
	public void testVCardAddresses() throws IOException, ServerFault {
		List<String> uids = importCards("vcfFile/vcardAddresses.vcf");
		assertEquals(1, uids.size());
		ItemValue<VCard> cardItem = getService(defaultSecurityContext).getComplete(uids.get(0));
		VCard card = cardItem.value;

		assertEquals("Phan", card.identification.name.familyNames);
		assertEquals("David", card.identification.name.givenNames);

		assertEquals(2, card.deliveryAddressing.size());
		DeliveryAddressing adrHome = card.deliveryAddressing.get(0);
		assertEquals("42 rue des plantes vertes", adrHome.address.streetAddress);
		assertEquals("31000", adrHome.address.postalCode);
		assertEquals("Toulouse", adrHome.address.locality);
		DeliveryAddressing adrWork = card.deliveryAddressing.get(1);

		assertEquals("40 rue du village d’entreprises", adrWork.address.streetAddress);
		assertEquals("31670", adrWork.address.postalCode);
		assertEquals("Labège", adrWork.address.locality);

	}

	@Test
	public void testImportYahooVcard() throws IOException, ServerFault {

		List<String> uids = importCards("vcfFile/yahoo.vcf");
		assertEquals(1, uids.size());

		ItemValue<VCard> cardItem = getService(defaultSecurityContext).getComplete(uids.get(0));
		VCard card = cardItem.value;

		assertEquals("MERIDIEN", card.identification.name.givenNames);
		assertEquals("Reservation", card.identification.name.familyNames);

		assertEquals(1, card.communications.emails.size());
		VCard.Communications.Email email = card.communications.emails.get(0);
		assertEquals("01810.reservation@lemeridien.com", email.value);

		// FIXME should we detect type=internet ?
		// assertEquals("INTERNET", email.getParameterValue("TYPE"));
	}

	@Test
	public void testImportVcardWithoutLastname() throws IOException, ServerFault {
		List<String> uids = importCards("vcfFile/vcardWithoutLastname.vcf");
		assertEquals(1, uids.size());

		ItemValue<VCard> cardItem = getService(defaultSecurityContext).getComplete(uids.get(0));
		VCard card = cardItem.value;

		assertEquals(1, card.communications.emails.size());
		VCard.Communications.Email email = card.communications.emails.get(0);

		assertEquals("testvcard@bug3258.lan", email.value);
		// FIXME should we detect type=internet ?
		// assertEquals("INTERNET", email.getParameterValue("TYPE"));

	}

	@Test
	public void testImportOutlook2007Vcard() throws IOException, ServerFault {
		List<String> uids = importCards("vcfFile/outlook2007.vcf");
		assertEquals(1, uids.size());

		ItemValue<VCard> cardItem = getService(defaultSecurityContext).getComplete(uids.get(0));
		VCard card = cardItem.value;

		assertEquals("Alain", card.identification.name.givenNames);
		assertEquals("Legrand", card.identification.name.familyNames);

		assertEquals(1, card.communications.emails.size());
		VCard.Communications.Email email = card.communications.emails.get(0);

		// FIXME should we detect type=internet ?
		// assertEquals("INTERNET", email.getParameterValue("TYPE"));

		assertEquals("foo@bar.org", email.value);
		;

	}

	@Test
	public void testImportVCardTagRef() throws IOException, ServerFault {
		List<String> uids = importCards("vcfFile/tagRef.vcf");
		assertEquals(1, uids.size());

		ItemValue<VCard> cardItem = getService(defaultSecurityContext).getComplete(uids.get(0));
		VCard card = cardItem.value;
		assertEquals(1, card.explanatory.categories.size());
		assertEquals("Collègue", card.explanatory.categories.get(0).label);
		assertEquals("000000", card.explanatory.categories.get(0).color);
	}

	@Test
	public void testImportVCard_DlistShouldKeepTrackOfRefs() throws IOException, ServerFault {
		List<String> uids = importCards("vcfFile/dlist_dlist.vcf");
		assertEquals(4, uids.size());

		boolean checkOk = false;

		for (String uid : uids) {
			ItemValue<VCard> cardItem = getService(defaultSecurityContext).getComplete(uid);
			VCard card = cardItem.value;

			if (cardItem.displayName.equals("dlist2")) {
				for (Member m : card.organizational.member) {
					if (m.commonName.equals("dlist")) {
						ItemValue<VCard> dlist = getService(defaultSecurityContext).getComplete(m.itemUid);
						assertEquals(VCard.Kind.group.name(), dlist.value.kind.name());
						checkOk = true;
					}
				}
			}
		}

		assertTrue(checkOk);

	}

	private void assertDate(int yyyy, int mm, int dd, Date date) {
		System.out.println(date);
		Calendar bday = Calendar.getInstance();
		bday.setTimeInMillis(date.getTime());

		assertEquals(yyyy, bday.get(Calendar.YEAR));
		assertEquals(mm, bday.get(Calendar.MONTH));
		assertEquals(dd, bday.get(Calendar.DATE));

	}

	private List<String> importCards(String path) throws IOException, ServerFault {
		InputStream in = VCardServiceImportTests.class.getClassLoader().getResourceAsStream(path);
		String vCard = FileUtils.streamString(in, true);
		List<String> uids = getVCardService().directImportCards(vCard).uids;
		return uids;
	}

	private IVCardService getVCardService() throws ServerFault {
		return new VCardService(context, (AddressBookService) getService(defaultSecurityContext), container);

	}

	private net.fortuna.ical4j.vcard.VCard export(VCard card) throws ServerFault {
		String uid = "junit-" + System.nanoTime();
		VCardContainerStoreService vStore = getVCardStore();
		vStore.create(uid, "blabla", card);

		IVCardService service = getVCardService();
		String vcard = service.exportCards(Arrays.asList(uid));

		assertNotNull(vcard);
		VCardBuilder builder = Builder.from(new StringReader(vcard));
		net.fortuna.ical4j.vcard.VCard export = null;
		try {
			export = builder.build();
		} catch (IOException | ParserException e) {
			e.printStackTrace();
			fail();
		}
		return export;
	}

	private VCardContainerStoreService getVCardStore() {
		return new VCardContainerStoreService(context, dataDataSource, SecurityContext.SYSTEM, container);
	}
}
