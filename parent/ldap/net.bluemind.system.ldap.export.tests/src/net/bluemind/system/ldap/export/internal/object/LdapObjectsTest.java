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
package net.bluemind.system.ldap.export.internal.object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.ModifyRequestImpl;
import org.junit.Test;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.ldap.export.internal.objects.LdapObjects;

public class LdapObjectsTest extends LdapObjects {
	@Test
	public void testLdapObjects_updateLdapAttributeNoneToNone() throws LdapException {
		Entry currentEntry = new DefaultEntry();
		Entry entry = new DefaultEntry();

		ModifyRequest modifyRequest = updateLdapAttribute(new ModifyRequestImpl(), currentEntry, entry, "cn");

		assertEquals(0, modifyRequest.getModifications().size());
	}

	@Test
	public void testLdapObjects_updateLdapAttributeNoneToAny() throws LdapException {
		Entry currentEntry = new DefaultEntry();
		Entry entry = new DefaultEntry();
		entry.add("cn", "val1");
		entry.add("cn", "val2");

		ModifyRequest modifyRequest = updateLdapAttribute(new ModifyRequestImpl(), currentEntry, entry, "cn");

		assertEquals(1, modifyRequest.getModifications().size());
		Modification mod = modifyRequest.getModifications().iterator().next();

		assertEquals(ModificationOperation.ADD_ATTRIBUTE, mod.getOperation());
		assertEquals("cn", mod.getAttribute().getId());
		assertTrue(mod.getAttribute().contains("val1"));
		assertTrue(mod.getAttribute().contains("val2"));
	}

	@Test
	public void testLdapObjects_updateLdapAttributeAnyToNone() throws LdapException {
		Entry currentEntry = new DefaultEntry();
		currentEntry.add("cn", "val1");
		currentEntry.add("cn", "val2");
		Entry entry = new DefaultEntry();

		ModifyRequest modifyRequest = updateLdapAttribute(new ModifyRequestImpl(), currentEntry, entry, "cn");

		assertEquals(1, modifyRequest.getModifications().size());
		Modification mod = modifyRequest.getModifications().iterator().next();

		assertEquals(ModificationOperation.REMOVE_ATTRIBUTE, mod.getOperation());
		assertEquals("cn", mod.getAttribute().getId());
		assertTrue(mod.getAttribute().contains("val1"));
		assertTrue(mod.getAttribute().contains("val2"));
	}

	@Test
	public void testLdapObjects_updateLdapAttributeDiffValueCount() throws LdapException {
		Entry currentEntry = new DefaultEntry();
		currentEntry.add("cn", "val1");
		currentEntry.add("cn", "val2");
		Entry entry = new DefaultEntry();
		entry.add("cn", "val1");
		entry.add("cn", "val2");
		entry.add("cn", "newVal3");

		ModifyRequest modifyRequest = updateLdapAttribute(new ModifyRequestImpl(), currentEntry, entry, "cn");

		assertEquals(1, modifyRequest.getModifications().size());
		Modification mod = modifyRequest.getModifications().iterator().next();

		assertEquals(ModificationOperation.REPLACE_ATTRIBUTE, mod.getOperation());
		assertEquals("cn", mod.getAttribute().getId());
		assertTrue(mod.getAttribute().contains("val1"));
		assertTrue(mod.getAttribute().contains("val2"));
		assertTrue(mod.getAttribute().contains("newVal3"));
	}

	@Test
	public void testLdapObjects_updateLdapAttributeSameValueCount() throws LdapException {
		Entry currentEntry = new DefaultEntry();
		currentEntry.add("cn", "val1");
		currentEntry.add("cn", "val2");
		Entry entry = new DefaultEntry();
		entry.add("cn", "newVal1");
		entry.add("cn", "newVal2");

		ModifyRequest modifyRequest = updateLdapAttribute(new ModifyRequestImpl(), currentEntry, entry, "cn");

		assertEquals(1, modifyRequest.getModifications().size());
		Modification mod = modifyRequest.getModifications().iterator().next();

		assertEquals(ModificationOperation.REPLACE_ATTRIBUTE, mod.getOperation());
		assertEquals("cn", mod.getAttribute().getId());
		assertTrue(mod.getAttribute().contains("newVal1"));
		assertTrue(mod.getAttribute().contains("newVal2"));
	}

	@Test
	public void testLdapObjects_updateLdapAttributeNoUpdate() throws LdapException {
		Entry currentEntry = new DefaultEntry();
		currentEntry.add("cn", "val1");
		currentEntry.add("cn", "val2");
		Entry entry = new DefaultEntry();
		entry.add("cn", "val1");
		entry.add("cn", "val2");

		ModifyRequest modifyRequest = updateLdapAttribute(new ModifyRequestImpl(), currentEntry, entry, "cn");

		assertEquals(0, modifyRequest.getModifications().size());
	}

	@Override
	public String getDn() {
		return null;
	}

	@Override
	public String getRDn() {
		return null;
	}

	@Override
	public Entry getLdapEntry() throws ServerFault {
		return null;
	}
}
