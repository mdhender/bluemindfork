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
package net.bluemind.system.ldap.export.object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.junit.Test;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Communications.Tel;
import net.bluemind.addressbook.api.VCard.DeliveryAddressing;
import net.bluemind.addressbook.api.VCard.DeliveryAddressing.Address;
import net.bluemind.addressbook.api.VCard.Parameter;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.system.ldap.export.objects.DomainDirectoryUser;
import net.bluemind.user.api.User;

public class DomainDirectoryUserTests {
	@Test
	public void testUser_getDn() {
		ItemValue<Domain> domain = getTestDomain();
		ItemValue<User> user = getTestUser();

		String dn = new DomainDirectoryUser(domain, user, null).getDn();
		assertEquals("uid=" + user.value.login + ",ou=users,dc=" + domain.value.name + ",dc=local", dn);
	}

	@Test
	public void testUser_getRdn() {
		ItemValue<Domain> domain = getTestDomain();
		ItemValue<User> user = getTestUser();

		String rdn = new DomainDirectoryUser(domain, user, null).getRDn();
		assertEquals("uid=" + user.value.login, rdn);
	}

	@Test
	public void testUser_ldapEntry() {
		ItemValue<Domain> domain = getTestDomain();
		ItemValue<User> user = getTestUser();
		byte[] photo = "photo".getBytes();

		Entry entry = new DomainDirectoryUser(domain, user, photo).getLdapEntry();

		List<String> attrs = getAttributeValues(entry, "objectclass");
		assertTrue(attrs.contains("inetOrgPerson"));
		assertTrue(attrs.contains("bmUser"));

		attrs = getAttributeValues(entry, "bmUid");
		assertEquals(1, attrs.size());
		assertEquals(user.uid, attrs.get(0));

		attrs = getAttributeValues(entry, "cn");
		assertEquals(1, attrs.size());
		assertEquals("Formated name", attrs.get(0));

		attrs = getAttributeValues(entry, "sn");
		assertEquals(1, attrs.size());
		assertEquals("familyName", attrs.get(0));

		attrs = getAttributeValues(entry, "givenname");
		assertEquals(1, attrs.size());
		assertEquals("givenName", attrs.get(0));

		attrs = getAttributeValues(entry, "description");
		assertEquals(1, attrs.size());
		assertEquals("description", attrs.get(0));

		attrs = getAttributeValues(entry, "ou");
		assertEquals(1, attrs.size());
		assertEquals("division", attrs.get(0));

		attrs = getAttributeValues(entry, "departmentNumber");
		assertEquals(1, attrs.size());
		assertEquals("department number", attrs.get(0));

		attrs = getAttributeValues(entry, "title");
		assertEquals(1, attrs.size());
		assertEquals("title", attrs.get(0));

		attrs = getAttributeValues(entry, "jpegPhoto");
		assertEquals(1, attrs.size());
		assertEquals("photo", attrs.get(0));

		attrs = getAttributeValues(entry, "mail");
		assertEquals(1, attrs.size());
		assertEquals("default@domain.tld", attrs.get(0));

		attrs = getAttributeValues(entry, "telephoneNumber");
		assertEquals(1, attrs.size());
		assertEquals("1111", attrs.get(0));

		attrs = getAttributeValues(entry, "homePhone");
		assertEquals(1, attrs.size());
		assertEquals("2222", attrs.get(0));

		attrs = getAttributeValues(entry, "mobile");
		assertEquals(1, attrs.size());
		assertEquals("3333", attrs.get(0));

		attrs = getAttributeValues(entry, "facsimileTelephoneNumber");
		assertEquals(1, attrs.size());
		assertEquals("4444", attrs.get(0));

		attrs = getAttributeValues(entry, "l");
		assertEquals(1, attrs.size());
		assertEquals("locality", attrs.get(0));

		attrs = getAttributeValues(entry, "postalCode");
		assertEquals(1, attrs.size());
		assertEquals("postal code", attrs.get(0));

		attrs = getAttributeValues(entry, "postOfficeBox");
		assertEquals(1, attrs.size());
		assertEquals("post office box", attrs.get(0));

		attrs = getAttributeValues(entry, "postalAddress");
		assertEquals(1, attrs.size());
		assertEquals("street address", attrs.get(0));

		attrs = getAttributeValues(entry, "street");
		assertEquals(1, attrs.size());
		assertEquals("street address", attrs.get(0));

		attrs = getAttributeValues(entry, "st");
		assertEquals(1, attrs.size());
		assertEquals("country name", attrs.get(0));

		attrs = getAttributeValues(entry, "registeredAddress");
		assertEquals(1, attrs.size());
		assertEquals("street address$post office box$locality, country name postal code$country name", attrs.get(0));

		attrs = getAttributeValues(entry, "userPassword");
		assertEquals(1, attrs.size());
		assertEquals("{SASL}login@domain.tld", attrs.get(0));

		attrs = getAttributeValues(entry, "audio");
		assertEquals(1, attrs.size());
		assertEquals("11119999", attrs.get(0));

		attrs = getAttributeValues(entry, "employeeType");
		assertEquals(0, attrs.size());

		user.value.archived = true;
		entry = new DomainDirectoryUser(domain, user, photo).getLdapEntry();

		attrs = getAttributeValues(entry, "employeeType");
		assertEquals(1, attrs.size());
		assertEquals("archived", attrs.get(0));
	}

	@Test
	public void testUser_modifyRequest() throws LdapInvalidDnException {
		ItemValue<Domain> domain = getTestDomain();
		ItemValue<User> user = getTestUser();
		user.value.archived = true;
		byte[] photo = "photo".getBytes();

		DomainDirectoryUser ddu = new DomainDirectoryUser(domain, user, photo);

		Entry currentEntry = new DefaultEntry();
		currentEntry.setDn("cn=dntoupdate");

		ModifyRequest modificationRequest = ddu.getModifyRequest(currentEntry);
		assertEquals("cn=dntoupdate", modificationRequest.getName().getName());
		assertEquals(26, modificationRequest.getModifications().size());
	}

	private ItemValue<Domain> getTestDomain() {
		Domain domain = new Domain();
		domain.name = "domain.tld";

		return ItemValue.create(Item.create(UUID.randomUUID().toString(), null), domain);
	}

	private ItemValue<User> getTestUser() {
		User user = new User();
		user.login = "login";

		VCard vcard = new VCard();
		user.contactInfos = vcard;

		vcard.identification.formatedName.value = "Formated name";
		vcard.identification.name.familyNames = "familyName";
		vcard.identification.name.givenNames = "givenName";
		vcard.explanatory.note = "description";
		vcard.organizational.org.division = "division";
		vcard.organizational.org.department = "department number";
		vcard.organizational.title = "title";

		net.bluemind.addressbook.api.VCard.Communications.Email email1 = new VCard.Communications.Email();
		email1.value = "toto@yahoo.fr";

		net.bluemind.addressbook.api.VCard.Communications.Email email2 = new VCard.Communications.Email();
		email2.value = "default@domain.tld";
		Parameter parameter = new VCard.Parameter();
		parameter.label = "default";
		parameter.value = "true";
		email2.parameters = Arrays.asList(parameter);
		vcard.communications.emails = Arrays.asList(email1, email2);

		vcard.communications.tels = Arrays.asList(
				Tel.create("1111", Arrays.asList(Parameter.create("TYPE", "voice"), Parameter.create("TYPE", "work"))),
				Tel.create("2222", Arrays.asList(Parameter.create("TYPE", "voice"), Parameter.create("TYPE", "home"))),
				Tel.create("3333", Arrays.asList(Parameter.create("TYPE", "voice"), Parameter.create("TYPE", "cell"))),
				Tel.create("4444", Arrays.asList(Parameter.create("TYPE", "fax"), Parameter.create("TYPE", "work"))),
				Tel.create("5555", Arrays.asList(Parameter.create("TYPE", "fax"), Parameter.create("TYPE", "home"))));

		Address address = new VCard.DeliveryAddressing.Address();
		address.locality = "locality";
		address.postalCode = "postal code";
		address.postOfficeBox = "post office box";
		address.streetAddress = "street address";
		address.countryName = "country name";
		DeliveryAddressing deliveryAddress = new VCard.DeliveryAddressing();
		deliveryAddress.address = address;
		vcard.deliveryAddressing = Arrays.asList(deliveryAddress);

		return ItemValue.create(Item.create(UUID.randomUUID().toString(), null), user);
	}

	private static List<String> getAttributeValues(Entry entry, String attr) {
		List<String> attrsValues = new ArrayList<>();

		Attribute mailAttr = entry.get(attr);
		if (mailAttr == null) {
			return attrsValues;
		}

		Iterator<Value<?>> adIterator = mailAttr.iterator();
		while (adIterator.hasNext()) {
			String value = adIterator.next().getString().trim();
			if (value.isEmpty()) {
				continue;
			}

			attrsValues.add(value);
		}

		return attrsValues;
	}
}
