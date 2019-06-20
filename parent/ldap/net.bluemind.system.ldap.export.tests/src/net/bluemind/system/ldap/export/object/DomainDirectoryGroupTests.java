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

import net.bluemind.core.api.Email;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.group.api.Group;
import net.bluemind.system.ldap.export.objects.DomainDirectoryGroup;
import net.bluemind.system.ldap.export.objects.DomainDirectoryGroup.MembersList;

public class DomainDirectoryGroupTests {
	@Test
	public void testGroup_getDn() {
		ItemValue<Domain> domain = getTestDomain();
		ItemValue<Group> group = getTestGroup();

		String dn = new DomainDirectoryGroup(domain, group).getDn();
		assertEquals("cn=" + group.value.name + ",ou=groups,dc=" + domain.value.name + ",dc=local", dn);
	}

	@Test
	public void testGroup_getRdn() {
		ItemValue<Domain> domain = getTestDomain();
		ItemValue<Group> group = getTestGroup();

		String rdn = new DomainDirectoryGroup(domain, group).getRDn();
		assertEquals("cn=" + group.value.name, rdn);
	}

	@Test
	public void testGroup_ldapEntry() {
		ItemValue<Domain> domain = getTestDomain();
		ItemValue<Group> group = getTestGroup();

		Entry entry = new DomainDirectoryGroup(domain, group).getLdapEntry();
		List<String> attrs = getAttributeValues(entry, "objectclass");
		assertTrue(attrs.contains("posixGroup"));
		assertTrue(attrs.contains("bmGroup"));

		attrs = getAttributeValues(entry, "bmUid");
		assertEquals(1, attrs.size());
		assertEquals(group.uid, attrs.get(0));

		attrs = getAttributeValues(entry, "gidNumber");
		assertEquals(1, attrs.size());
		assertEquals("-1", attrs.get(0));

		attrs = getAttributeValues(entry, "description");
		assertEquals(1, attrs.size());
		assertEquals("group description", attrs.get(0));

		attrs = getAttributeValues(entry, "mail");
		assertEquals(1, attrs.size());
		assertEquals("default@domain.tld", attrs.get(0));
	}

	@Test
	public void testGroup_modifyRequest() throws LdapInvalidDnException {
		ItemValue<Domain> domain = getTestDomain();
		ItemValue<Group> group = getTestGroup();

		MembersList memberList = new DomainDirectoryGroup.MembersList();
		memberList.member.add("uid=member,dc=local");
		memberList.memberUid.add("member");
		DomainDirectoryGroup ddg = new DomainDirectoryGroup(domain, group, memberList);

		Entry currentEntry = new DefaultEntry();
		currentEntry.setDn("cn=dntoupdate");

		ModifyRequest modificationRequest = ddg.getModifyRequest(currentEntry);
		assertEquals("cn=dntoupdate", modificationRequest.getName().getName());
		assertEquals(DomainDirectoryGroup.ldapAttrsStringsValues.size(), modificationRequest.getModifications().size());
	}

	private ItemValue<Group> getTestGroup() {
		Group group = new Group();
		group.name = "name";
		group.description = "group description";
		group.emails = Arrays.asList(Email.create("one@domain.tld", false), Email.create("default@domain.tld", true),
				Email.create("two@domain.tld", false));

		return ItemValue.create(Item.create(UUID.randomUUID().toString(), null), group);
	}

	private ItemValue<Domain> getTestDomain() {
		Domain domain = new Domain();
		domain.name = "domain.tld";

		return ItemValue.create(Item.create(UUID.randomUUID().toString(), null), domain);
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
