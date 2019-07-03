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
package net.bluemind.addressbook.ldap.adapter.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.junit.Test;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Communications.Email;
import net.bluemind.addressbook.api.VCard.Communications.Tel;
import net.bluemind.addressbook.api.VCard.DeliveryAddressing;
import net.bluemind.addressbook.ldap.adapter.InetOrgPersonAdapter;
import net.bluemind.addressbook.ldap.adapter.LdapContact;
import net.bluemind.addressbook.ldap.adapter.LdapContact.ErrCode;
import net.bluemind.addressbook.ldap.api.LdapParameters;
import net.bluemind.utils.FileUtils;

public class InetOrgPersonAdapterTest {

	@Test
	public void testUid() throws Exception {
		LdifEntry ldif = new LdifEntry();
		ldif.addAttribute("uid", "yeahUid");
		LdapContact lc = InetOrgPersonAdapter.getVCard(ldif.getEntry(), LdapParameters.DirectoryType.ldap, "uid");
		assertEquals("yeahUid", lc.uid);

		ldif = new LdifEntry();
		ldif.addAttribute("entryUUID", "yeahUid");
		lc = InetOrgPersonAdapter.getVCard(ldif.getEntry(), LdapParameters.DirectoryType.ldap, "entryUUID");
		assertEquals("yeahUid", lc.uid);
	}

	@Test
	public void testIdentification() throws Exception {
		LdifEntry ldif = new LdifEntry();
		ldif.addAttribute("sn", "Jensen");
		ldif.addAttribute("givenName", "Barbara");

		LdapContact lc = InetOrgPersonAdapter.getVCard(ldif.getEntry(), LdapParameters.DirectoryType.ldap, "uid");
		VCard vcard = lc.vcard;
		assertNotNull(vcard);

		assertEquals("Barbara", vcard.identification.name.givenNames);
		assertEquals("Jensen", vcard.identification.name.familyNames);
	}

	@Test
	public void testPhoto() throws Exception {
		String data = null;

		// LDAP
		try (InputStream in = new FileInputStream(new File("data/testPhoto.ldif"))) {
			data = FileUtils.streamString(in, true);
		}
		LdifEntry ldif = new LdifEntry("uid=toto,dc=titi,dc=tata", data);
		LdapContact lc = InetOrgPersonAdapter.getVCard(ldif.getEntry(), LdapParameters.DirectoryType.ldap, "uid");
		VCard vcard = lc.vcard;
		assertNotNull(vcard);
		assertNotNull(lc.photo);

		// AD
		try (InputStream in = new FileInputStream(new File("data/testAdPhoto.ldif"))) {
			data = FileUtils.streamString(in, true);
		}
		ldif = new LdifEntry("uid=toto,dc=titi,dc=tata", data);
		lc = InetOrgPersonAdapter.getVCard(ldif.getEntry(), LdapParameters.DirectoryType.ad, "uid");
		vcard = lc.vcard;
		assertNotNull(vcard);
		assertNotNull(lc.photo);
	}

	@Test
	public void testInvalidPhoto() throws Exception {
		String data = null;
		try (InputStream in = new FileInputStream(new File("data/testInvalidPhoto.ldif"))) {
			data = FileUtils.streamString(in, true);
		}
		LdifEntry ldif = new LdifEntry("uid=toto,dc=titi,dc=tata", data);
		LdapContact lc = InetOrgPersonAdapter.getVCard(ldif.getEntry(), LdapParameters.DirectoryType.ldap, "uid");
		VCard vcard = lc.vcard;
		assertNotNull(vcard);
		assertNull(lc.photo);
		assertEquals(ErrCode.jpegPhoto, lc.err);
	}

	@Test
	public void testNonBase64Photo() throws Exception {
		String data = null;
		try (InputStream in = new FileInputStream(new File("data/testPhoto.ldif"))) {
			data = FileUtils.streamString(in, true);
		}
		LdifEntry ldif = new LdifEntry("uid=toto,dc=titi,dc=tata", data);
		ldif = new LdifEntry();
		ldif.addAttribute("jpegPhoto", "/path/to/photo.jpg");
		LdapContact lc = InetOrgPersonAdapter.getVCard(ldif.getEntry(), LdapParameters.DirectoryType.ldap, "uid");
		VCard vcard = lc.vcard;
		assertNotNull(vcard);
		assertNull(lc.photo);
		assertEquals(ErrCode.jpegPhoto, lc.err);
	}

	@Test
	public void testNote() throws Exception {
		LdifEntry ldif = new LdifEntry();
		ldif.addAttribute("description", "bang bang description");
		LdapContact lc = InetOrgPersonAdapter.getVCard(ldif.getEntry(), LdapParameters.DirectoryType.ldap, "uid");
		VCard vcard = lc.vcard;
		assertNotNull(vcard);
		assertEquals("bang bang description", vcard.explanatory.note);
	}

	@Test
	public void testOrganizational() throws Exception {
		LdifEntry ldif = new LdifEntry();
		ldif.addAttribute("title", "manager, product development");
		ldif.addAttribute("o", "Siroe");
		ldif.addAttribute("ou", "Product Development");

		LdapContact lc = InetOrgPersonAdapter.getVCard(ldif.getEntry(), LdapParameters.DirectoryType.ldap, "uid");
		VCard vcard = lc.vcard;
		assertNotNull(vcard);

		// ORG
		assertEquals("Siroe", vcard.organizational.org.company);
		assertEquals("Product Development", vcard.organizational.org.department);
		assertEquals("manager, product development", vcard.organizational.title);
	}

	@Test
	public void testPhones() throws Exception {
		LdifEntry ldif = new LdifEntry();
		ldif.addAttribute("telephoneNumber", "+1 408 555 1862");
		ldif.addAttribute("facsimileTelephoneNumber", "+1 408 555 1992");
		ldif.addAttribute("mobile", "+1 408 555 1941");
		ldif.addAttribute("mobile", "0600000000");
		ldif.addAttribute("homePhone", "0566666666");
		ldif.addAttribute("pager", "113");
		ldif.addAttribute("mobile", "");

		LdapContact lc = InetOrgPersonAdapter.getVCard(ldif.getEntry(), LdapParameters.DirectoryType.ldap, "uid");
		VCard vcard = lc.vcard;
		assertNotNull(vcard);

		// PHONES
		for (Tel tel : vcard.communications.tels) {
			if ("+1 408 555 1941".equals(tel.value)) {
				assertEquals(Arrays.asList("cell", "voice"), tel.getParameterValues("TYPE"));
			} else if ("0600000000".equals(tel.value)) {
				assertEquals(Arrays.asList("cell", "voice"), tel.getParameterValues("TYPE"));
			} else if ("+1 408 555 1992".equals(tel.value)) {
				assertEquals(Arrays.asList("fax", "work"), tel.getParameterValues("TYPE"));
			} else if ("+1 408 555 1862".equals(tel.value)) {
				assertEquals(Arrays.asList("voice", "work"), tel.getParameterValues("TYPE"));
			} else if ("0566666666".equals(tel.value)) {
				assertEquals(Arrays.asList("voice", "home"), tel.getParameterValues("TYPE"));
			} else if ("113".equals(tel.value)) {
				assertEquals(Arrays.asList("pager", "voice"), tel.getParameterValues("TYPE"));
			} else {
				fail("Unknown phone number " + tel.value);
			}
		}
		assertEquals(6, vcard.communications.tels.size());

	}

	@Test
	public void testEmails() throws Exception {
		LdifEntry ldif = new LdifEntry();
		ldif.addAttribute("mail", "bjensen@siroe.com");
		ldif.addAttribute("mail", "mail2@siroe.com");
		ldif.addAttribute("mail", "bang@siroe.com");
		ldif.addAttribute("mail", "");
		LdapContact lc = InetOrgPersonAdapter.getVCard(ldif.getEntry(), LdapParameters.DirectoryType.ldap, "uid");
		VCard vcard = lc.vcard;
		assertNotNull(vcard);

		int count = 0;
		for (Email email : vcard.communications.emails) {
			if ("bjensen@siroe.com".equals(email.value)) {
				count++;
			}
			if ("mail2@siroe.com".equals(email.value)) {
				count++;
			}
			if ("bang@siroe.com".equals(email.value)) {
				count++;
			}
			assertEquals(Arrays.asList("home"), email.getParameterValues("TYPE"));
		}
		assertEquals(3, count);
		assertEquals(3, vcard.communications.emails.size());
	}

	@Test
	public void testAddresses() throws Exception {
		LdifEntry ldif = new LdifEntry();
		ldif.addAttribute("postalAddress", "1 place du Capitole $31000 Toulouse");
		LdapContact lc = InetOrgPersonAdapter.getVCard(ldif.getEntry(), LdapParameters.DirectoryType.ldap, "uid");
		VCard vcard = lc.vcard;
		assertNotNull(vcard);
		assertEquals(1, vcard.deliveryAddressing.size());
		DeliveryAddressing da = vcard.deliveryAddressing.get(0);
		assertEquals(Arrays.asList("work"), da.address.getParameterValues("TYPE"));
		assertEquals("1 place du Capitole", da.address.streetAddress);
		assertEquals("31000", da.address.postalCode);
		assertEquals("Toulouse", da.address.locality);

		//
		ldif = new LdifEntry();
		ldif.addAttribute("postalAddress", "1 place du Capitole $31000 Toulouse $FRANCE");
		lc = InetOrgPersonAdapter.getVCard(ldif.getEntry(), LdapParameters.DirectoryType.ldap, "uid");
		vcard = lc.vcard;
		assertNotNull(vcard);
		assertEquals(1, vcard.deliveryAddressing.size());
		da = vcard.deliveryAddressing.get(0);
		assertEquals(Arrays.asList("work"), da.address.getParameterValues("TYPE"));
		assertEquals("1 place du Capitole", da.address.streetAddress);
		assertEquals("31000", da.address.postalCode);
		assertEquals("Toulouse", da.address.locality);
		assertEquals("FRANCE", da.address.countryName);

		//
		ldif = new LdifEntry();
		ldif.addAttribute("postalAddress", "Hôtel des Télécoms $40, rue du Village d'Entreprises $31670 LABÈGE");
		lc = InetOrgPersonAdapter.getVCard(ldif.getEntry(), LdapParameters.DirectoryType.ldap, "uid");
		vcard = lc.vcard;
		assertNotNull(vcard);
		assertEquals(1, vcard.deliveryAddressing.size());
		da = vcard.deliveryAddressing.get(0);
		assertEquals(Arrays.asList("work"), da.address.getParameterValues("TYPE"));
		assertEquals("Hôtel des Télécoms", da.address.streetAddress);
		assertEquals("40, rue du Village d'Entreprises", da.address.extentedAddress);
		assertEquals("31670", da.address.postalCode);
		assertEquals("LABÈGE", da.address.locality);

		//
		ldif = new LdifEntry();
		ldif.addAttribute("postalAddress",
				"Hôtel des Télécoms $40, rue du Village d'Entreprises $31670 LABÈGE $FRANCE");
		lc = InetOrgPersonAdapter.getVCard(ldif.getEntry(), LdapParameters.DirectoryType.ldap, "uid");
		vcard = lc.vcard;
		assertNotNull(vcard);
		assertEquals(1, vcard.deliveryAddressing.size());
		da = vcard.deliveryAddressing.get(0);
		assertEquals(Arrays.asList("work"), da.address.getParameterValues("TYPE"));
		assertEquals("Hôtel des Télécoms", da.address.streetAddress);
		assertEquals("40, rue du Village d'Entreprises", da.address.extentedAddress);
		assertEquals("31670", da.address.postalCode);
		assertEquals("LABÈGE", da.address.locality);
		assertEquals("FRANCE", da.address.countryName);

		//
		ldif = new LdifEntry();
		ldif.addAttribute("postalAddress", "52 RUE DES JONQUILLES $BP 77 BELLEVILLE $99123 VILLENOUVELLE");
		lc = InetOrgPersonAdapter.getVCard(ldif.getEntry(), LdapParameters.DirectoryType.ldap, "uid");
		vcard = lc.vcard;
		assertNotNull(vcard);
		assertEquals(1, vcard.deliveryAddressing.size());
		da = vcard.deliveryAddressing.get(0);
		assertEquals(Arrays.asList("work"), da.address.getParameterValues("TYPE"));
		assertEquals("52 RUE DES JONQUILLES", da.address.streetAddress);
		assertEquals("BP 77 BELLEVILLE", da.address.postOfficeBox);
		assertEquals("99123", da.address.postalCode);
		assertEquals("VILLENOUVELLE", da.address.locality);

		//
		ldif = new LdifEntry();
		ldif.addAttribute("postalAddress",
				"52 RUE DES JONQUILLES $Bâtiment A $BP 77 BELLEVILLE $99123 VILLENOUVELLE DE OUF $FRANCE");
		lc = InetOrgPersonAdapter.getVCard(ldif.getEntry(), LdapParameters.DirectoryType.ldap, "uid");
		vcard = lc.vcard;
		assertNotNull(vcard);
		assertEquals(1, vcard.deliveryAddressing.size());
		da = vcard.deliveryAddressing.get(0);
		assertEquals(Arrays.asList("work"), da.address.getParameterValues("TYPE"));
		assertEquals("52 RUE DES JONQUILLES", da.address.streetAddress);
		assertEquals("Bâtiment A", da.address.extentedAddress);
		assertEquals("BP 77 BELLEVILLE", da.address.postOfficeBox);
		assertEquals("99123", da.address.postalCode);
		assertEquals("VILLENOUVELLE DE OUF", da.address.locality);
		assertEquals("FRANCE", da.address.countryName);

		// without $
		ldif = new LdifEntry();
		ldif.addAttribute("postalAddress", "Hôtel des Télécoms 40, rue du Village d'Entreprises 31670 LABÈGE FRANCE");
		lc = InetOrgPersonAdapter.getVCard(ldif.getEntry(), LdapParameters.DirectoryType.ldap, "uid");
		vcard = lc.vcard;
		assertNotNull(vcard);
		assertEquals(1, vcard.deliveryAddressing.size());
		da = vcard.deliveryAddressing.get(0);
		assertEquals(Arrays.asList("work"), da.address.getParameterValues("TYPE"));
		assertEquals("Hôtel des Télécoms 40, rue du Village d'Entreprises 31670 LABÈGE FRANCE",
				da.address.streetAddress);

		// invalid postal code
		ldif = new LdifEntry();
		ldif.addAttribute("postalAddress", "Hôtel des Télécoms 40, rue du Village d'Entreprises 31670 $LABÈGE FRANCE");
		lc = InetOrgPersonAdapter.getVCard(ldif.getEntry(), LdapParameters.DirectoryType.ldap, "uid");
		vcard = lc.vcard;
		assertNotNull(vcard);
		assertEquals(1, vcard.deliveryAddressing.size());
		da = vcard.deliveryAddressing.get(0);
		assertEquals(Arrays.asList("work"), da.address.getParameterValues("TYPE"));
		assertEquals("Hôtel des Télécoms 40, rue du Village d'Entreprises 31670 $LABÈGE FRANCE",
				da.address.streetAddress);

		// multiple addresses
		ldif = new LdifEntry();
		ldif.addAttribute("postalAddress", "1 place du Capitole $31000 Toulouse $FRANCE");
		ldif.addAttribute("postalAddress", "Hôtel des Télécoms $40, rue du Village d'Entreprises $31670 LABÈGE");
		ldif.addAttribute("postalAddress", "");
		lc = InetOrgPersonAdapter.getVCard(ldif.getEntry(), LdapParameters.DirectoryType.ldap, "uid");
		vcard = lc.vcard;
		assertNotNull(vcard);
		assertEquals(2, vcard.deliveryAddressing.size());
		da = vcard.deliveryAddressing.get(0);
		assertEquals(Arrays.asList("work"), da.address.getParameterValues("TYPE"));
		assertEquals("1 place du Capitole", da.address.streetAddress);
		assertEquals("31000", da.address.postalCode);
		assertEquals("Toulouse", da.address.locality);
		assertEquals("FRANCE", da.address.countryName);
		da = vcard.deliveryAddressing.get(1);
		assertEquals(Arrays.asList("work"), da.address.getParameterValues("TYPE"));
		assertEquals("Hôtel des Télécoms", da.address.streetAddress);
		assertEquals("40, rue du Village d'Entreprises", da.address.extentedAddress);
		assertEquals("31670", da.address.postalCode);
		assertEquals("LABÈGE", da.address.locality);

		//
		ldif = new LdifEntry();
		ldif.addAttribute("street", "1 place du Capitole");
		ldif.addAttribute("postalCode", "31000");
		ldif.addAttribute("l", "Toulouse");
		lc = InetOrgPersonAdapter.getVCard(ldif.getEntry(), LdapParameters.DirectoryType.ldap, "uid");
		vcard = lc.vcard;
		assertNotNull(vcard);

		assertEquals(1, vcard.deliveryAddressing.size());
		da = vcard.deliveryAddressing.get(0);
		assertEquals(Arrays.asList("work"), da.address.getParameterValues("TYPE"));
		assertEquals("1 place du Capitole", da.address.streetAddress);
		assertEquals("31000", da.address.postalCode);
		assertEquals("Toulouse", da.address.locality);

	}
}
