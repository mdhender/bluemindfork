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
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.junit.Test;

import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.system.ldap.export.objects.DomainDirectoryRoot;

public class DomainDirectoryRootTests {
	@Test
	public void getDn() {
		DomainDirectoryRoot ddr = new DomainDirectoryRoot(getTestDomain());
		assertEquals("dc=domain.tld,dc=local", ddr.getDn());
	}

	@Test
	public void getRDn() {
		DomainDirectoryRoot ddr = new DomainDirectoryRoot(getTestDomain());
		assertEquals("dc=domain.tld", ddr.getRDn());
	}

	@Test
	public void getLdapEntry() {
		Entry entry = new DomainDirectoryRoot(getTestDomain()).getLdapEntry();

		assertEquals("dc=domain.tld,dc=local", entry.getDn().getName());

		assertEquals(3, entry.get("objectClass").size());
		List<String> expectedOC = Arrays.asList("organization", "dcobject", "bmdomain");
		entry.get("objectClass").forEach(v -> assertTrue(expectedOC.contains(v.getString().toLowerCase())));

		assertEquals(1, entry.get("dc").size());
		assertEquals("domain.tld", entry.get("dc").get().getString());

		assertEquals(1, entry.get("o").size());
		assertEquals("This is domain label", entry.get("o").get().getString());

		assertEquals(1, entry.get("description").size());
		assertEquals("This is domain description", entry.get("description").get().getString());

		assertEquals(1, entry.get("bmVersion").size());
		assertEquals("0", entry.get("bmVersion").get().getString());
	}

	@Test
	public void modifyRequest() throws LdapInvalidDnException, LdapException {
		ModifyRequest mr = new DomainDirectoryRoot(getTestDomain()).getModifyRequest(
				new DefaultEntry("dc=domain.tld,dc=local").add("dc", "dc=domain.tld").add("bmVersion", "154")
						.add("objectclass", "dcObject").add("o", "Old label").add("description", "Old description"));

		assertEquals("dc=domain.tld,dc=local", mr.getName().getName());
		mr.getModifications().stream().map(Modification::getAttribute).forEach(this::checkUpdatedAttribute);
	}

	private void checkUpdatedAttribute(Attribute attribute) {
		try {
			switch (attribute.getUpId().toLowerCase()) {
			case "objectclass":
				assertEquals(3, attribute.size());

				List<String> expectedOC = Arrays.asList("organization", "dcobject", "bmdomain");
				attribute.forEach(v -> assertTrue(expectedOC.contains(v.getString().toLowerCase())));
				break;
			case "o":
				assertEquals(1, attribute.size());
				assertEquals("This is domain label", attribute.get().getString());
				break;
			case "description":
				assertEquals(1, attribute.size());
				assertEquals("This is domain description", attribute.get().getString());
				break;
			default:
				fail(String.format("Unknow updated attribute: %s", attribute.getUpId()));
				break;
			}
		} catch (Exception e) {
			fail(String.format("Test thrown an exception: %s", e.getMessage()));
		}
	}

	private ItemValue<Domain> getTestDomain() {
		Domain domain = new Domain();
		domain.name = "domain.tld";
		domain.label = "This is domain label";
		domain.description = "This is domain description";

		return ItemValue.create(Item.create(UUID.randomUUID().toString(), null), domain);
	}
}
