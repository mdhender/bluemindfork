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
package net.bluemind.system.importation.commons;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.junit.Test;

import net.bluemind.system.importation.commons.Parameters.SplitDomain;

public class LdapParametersSplitDomainTest {
	@Test
	public void splitDomain_Instance() throws LdapInvalidDnException {
		SplitDomain splitDomain = new SplitDomain(true, "relayMailboxGroup");

		assertEquals(true, splitDomain.splitRelayEnabled);
		assertEquals("relayMailboxGroup", splitDomain.relayMailboxGroup);
	}

	@Test
	public void splitDomain_equals() throws LdapInvalidDnException {
		SplitDomain splitDomain1 = new SplitDomain(true, "relayMailboxGroup1");
		SplitDomain splitDomain2 = new SplitDomain(true, "relayMailboxGroup1");

		assertEquals(splitDomain1, splitDomain2);
	}

	@Test
	public void splitDomain_notEquals() throws LdapInvalidDnException {
		SplitDomain splitDomain1 = new SplitDomain(true, "relayMailboxGroup1");

		SplitDomain splitDomain2 = new SplitDomain(false, "relayMailboxGroup1");
		assertNotEquals(splitDomain1, splitDomain2);

		splitDomain2 = new SplitDomain(true, "relayMailboxGroup2");
		assertNotEquals(splitDomain1, splitDomain2);
	}
}
