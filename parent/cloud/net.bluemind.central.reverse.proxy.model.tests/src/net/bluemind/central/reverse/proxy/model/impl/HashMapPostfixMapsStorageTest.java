/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.central.reverse.proxy.model.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import net.bluemind.central.reverse.proxy.model.common.DirInfo.DirEmail;

public class HashMapPostfixMapsStorageTest {
	private HashMapPostfixMapsStorage hashMapPostfixMapsStorage;

	@Before
	public void setup() {
		hashMapPostfixMapsStorage = new HashMapPostfixMapsStorage();
	}

	@Test
	public void domainAliases() {
		hashMapPostfixMapsStorage.updateDomain("domain-uid", Set.of("domain-alias1", "domain-alias2"));
		assertNull(hashMapPostfixMapsStorage.domainAliases("unknown-domain-uid"));
		assertNotNull(hashMapPostfixMapsStorage.domainAliases("domain-uid"));
		assertEquals(2, hashMapPostfixMapsStorage.domainAliases("domain-uid").size());
		assertTrue(hashMapPostfixMapsStorage.domainAliases("domain-uid").contains("domain-alias1"));
		assertTrue(hashMapPostfixMapsStorage.domainAliases("domain-uid").contains("domain-alias2"));
	}

	@Test
	public void updateDomain_domainManaged() {
		// Create domain1
		hashMapPostfixMapsStorage.updateDomain("domain1-uid", Set.of("domain1-alias1", "domain1-alias2"));

		updateDomain_domainManaged_assertDomain1Exists();

		assertFalse(hashMapPostfixMapsStorage.domainManaged("invalid"));

		// Create domain2
		hashMapPostfixMapsStorage.updateDomain("domain2-uid", Set.of("domain2-alias1"));

		assertTrue(hashMapPostfixMapsStorage.domainManaged("domain2-alias1"));
		assertFalse(hashMapPostfixMapsStorage.domainManaged("domain2-alias2"));
		assertTrue(hashMapPostfixMapsStorage.domainManaged("domain2-uid"));

		updateDomain_domainManaged_assertDomain1Exists();

		assertFalse(hashMapPostfixMapsStorage.domainManaged("invalid"));

		// Update domain2
		hashMapPostfixMapsStorage.updateDomain("domain2-uid", Set.of("domain2-alias2"));

		assertFalse(hashMapPostfixMapsStorage.domainManaged("domain2-alias1"));
		assertTrue(hashMapPostfixMapsStorage.domainManaged("domain2-alias2"));
		assertTrue(hashMapPostfixMapsStorage.domainManaged("domain2-uid"));

		// Delete domain2
		hashMapPostfixMapsStorage.removeDomain("domain2-uid");

		updateDomain_domainManaged_assertDomain1Exists();

		assertFalse(hashMapPostfixMapsStorage.domainManaged("domain2-alias1"));
		assertFalse(hashMapPostfixMapsStorage.domainManaged("domain2-alias2"));
		assertFalse(hashMapPostfixMapsStorage.domainManaged("domain2-uid"));
	}

	private void updateDomain_domainManaged_assertDomain1Exists() {
		assertTrue(hashMapPostfixMapsStorage.domainManaged("domain1-alias1"));
		assertTrue(hashMapPostfixMapsStorage.domainManaged("domain1-alias2"));
		assertTrue(hashMapPostfixMapsStorage.domainManaged("domain1-uid"));
	}

	@Test
	public void mailboxManaged_none() {
		// Create mailbox1-internal
		hashMapPostfixMapsStorage.updateMailbox(null, "mailbox1-internal-uid", "mailbox1-internal", "none",
				"datalocation1");

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("mailbox1-internal"));
	}

	@Test
	public void mailboxManaged_internal() {
		// Create mailbox1-internal
		hashMapPostfixMapsStorage.updateMailbox(null, "mailbox1-internal-uid", "mailbox1-internal", "internal",
				"datalocation1");

		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("mailbox1-internal"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("invalid"));

		// Create mailbox2-internal
		hashMapPostfixMapsStorage.updateMailbox(null, "mailbox2-internal-uid", "mailbox2-internal", "internal",
				"datalocation2");

		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("mailbox1-internal"));
		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("mailbox2-internal"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("invalid"));

		// Update mailbox2-internal
		hashMapPostfixMapsStorage.updateMailbox(null, "mailbox2-internal-uid", "mailbox2-internal-new", "internal",
				"datalocation2");

		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("mailbox1-internal"));
		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("mailbox2-internal-new"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("mailbox2-internal"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("invalid"));

		// Delete mailbox2-internal
		hashMapPostfixMapsStorage.removeMailbox("mailbox2-internal-uid");
		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("mailbox1-internal"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("mailbox2-internal-new"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("invalid"));
	}

	@Test
	public void mailboxManaged_split() {
		// Create mailbox-internal
		hashMapPostfixMapsStorage.updateMailbox("domain-uid", "mailbox-internal-uid", "mailbox-internal@domain-uid",
				"internal", "datalocation");
		hashMapPostfixMapsStorage.updateEmails("mailbox-internal-uid",
				Set.of(new DirEmail("email-internal@domain.tld", true)));

		// Create mailbox1-external - no email
		hashMapPostfixMapsStorage.updateMailbox("domain-uid", "mailbox-external-uid", "mailbox-external@domain-uid",
				"external", "datalocation");

		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("mailbox-internal@domain-uid"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-internal@domain-uid"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-internal@domain.tld"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-internal@domain-alias.tld"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("mailbox-external@domain-uid"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-external@domain-uid"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-external@domain.tld"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-external@domain-alias.tld"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("alias-external@domain-uid"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("alias-external@domain.tld"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("alias-external@domain-alias.tld"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("unknown@domain-uid"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("unknown@domain.tld"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("unknown@domain-alias.tld"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("invalid@unknown.tld"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("invalid"));

		// Add domain
		hashMapPostfixMapsStorage.updateDomain("domain-uid", Collections.emptySet());

		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("mailbox-internal@domain-uid"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-internal@domain-uid"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-internal@domain.tld"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-internal@domain-alias.tld"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("mailbox-external@domain-uid"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-external@domain-uid"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-external@domain.tld"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-external@domain-alias.tld"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("alias-external@domain-uid"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("alias-external@domain.tld"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("alias-external@domain-alias.tld"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("unknown@domain-uid"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("unknown@domain.tld"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("unknown@domain-alias.tld"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("invalid@unknown.tld"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("invalid"));

		// Add email to mailbox1-external
		hashMapPostfixMapsStorage.updateEmails("mailbox-external-uid", Set
				.of(new DirEmail("email-external@domain.tld", true), new DirEmail("alias-external@domain.tld", false)));

		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("mailbox-internal@domain-uid"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-internal@domain-uid"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-internal@domain.tld"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-internal@domain-alias.tld"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("mailbox-external@domain-uid"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-external@domain-uid"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-external@domain.tld"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-external@domain-alias.tld"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("alias-external@domain-uid"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("alias-external@domain.tld"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("alias-external@domain-alias.tld"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("unknown@domain-uid"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("unknown@domain.tld"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("unknown@domain-alias.tld"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("invalid@unknown.tld"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("invalid"));

		// Update Domain settings without forwardUnknown
		hashMapPostfixMapsStorage.updateDomainSettings("domain-uid", "smtp-relay", false);

		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("mailbox-internal@domain-uid"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-internal@domain-uid"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-internal@domain.tld"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-internal@domain-alias.tld"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("mailbox-external@domain-uid"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-external@domain-uid"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-external@domain.tld"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-external@domain-alias.tld"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("alias-external@domain-uid"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("alias-external@domain.tld"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("alias-external@domain-alias.tld"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("unknown@domain-uid"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("unknown@domain.tld"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("unknown@domain-alias.tld"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("invalid@unknown.tld"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("invalid"));

		// Update Domain settings with forwardUnknown
		hashMapPostfixMapsStorage.updateDomainSettings("domain-uid", "smtp-relay", true);

		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("mailbox-internal@domain-uid"));

		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("email-internal@domain-uid"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-internal@domain.tld"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-internal@domain-alias.tld"));

		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("mailbox-external@domain-uid"));

		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("email-external@domain-uid"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-external@domain.tld"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-external@domain-alias.tld"));

		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("alias-external@domain-uid"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("alias-external@domain.tld"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("alias-external@domain-alias.tld"));

		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("unknown@domain-uid"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("unknown@domain.tld"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("unknown@domain-alias.tld"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("invalid@unknown.tld"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("invalid"));

		// Update Domain settings without forwardUnknown
		// Add alias to domain
		hashMapPostfixMapsStorage.updateDomainSettings("domain-uid", "smtp-relay", false);
		hashMapPostfixMapsStorage.updateDomain("domain-uid", Set.of("domain.tld"));

		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("mailbox-internal@domain-uid"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-internal@domain-uid"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-internal@domain.tld"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-internal@domain-alias.tld"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("mailbox-external@domain-uid"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-external@domain-uid"));
		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("email-external@domain.tld"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-external@domain-alias.tld"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("alias-external@domain-uid"));
		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("alias-external@domain.tld"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("alias-external@domain-alias.tld"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("unknown@domain-uid"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("unknown@domain.tld"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("unknown@domain-alias.tld"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("invalid@unknown.tld"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("invalid"));

		// Add another alias to domain
		hashMapPostfixMapsStorage.updateDomain("domain-uid", Set.of("domain.tld", "domain-alias.tld"));

		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("mailbox-internal@domain-uid"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-internal@domain-uid"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-internal@domain.tld"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-internal@domain-alias.tld"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("mailbox-external@domain-uid"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("email-external@domain-uid"));
		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("email-external@domain.tld"));
		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("email-external@domain-alias.tld"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("alias-external@domain-uid"));
		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("alias-external@domain.tld"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("alias-external@domain-alias.tld"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("unknown@domain-uid"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("unknown@domain.tld"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("unknown@domain-alias.tld"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("invalid@unknown.tld"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("invalid"));

		// Update Domain settings with forwardUnknown
		hashMapPostfixMapsStorage.updateDomainSettings("domain-uid", "smtp-relay", true);

		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("mailbox-internal@domain-uid"));

		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("email-internal@domain-uid"));
		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("email-internal@domain.tld"));
		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("email-internal@domain-alias.tld"));

		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("mailbox-external@domain-uid"));

		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("email-external@domain-uid"));
		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("email-external@domain.tld"));
		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("email-external@domain-alias.tld"));

		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("alias-external@domain-uid"));
		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("alias-external@domain.tld"));
		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("alias-external@domain-alias.tld"));

		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("unknown@domain-uid"));
		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("unknown@domain.tld"));
		assertTrue(hashMapPostfixMapsStorage.mailboxManaged("unknown@domain-alias.tld"));

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("invalid@unknown.tld"));
		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("invalid"));
	}

	@Test
	public void mailboxRelay_internal() {
		// Create datalocation1
		hashMapPostfixMapsStorage.updateDataLocation("datalocation1", "ipdatalocation1");
		// Create mailbox1
		hashMapPostfixMapsStorage.updateMailbox(null, "mailbox1-uid", "mailbox1", "internal", "datalocation1");

		assertEquals("lmtp:ipdatalocation1:2400", hashMapPostfixMapsStorage.mailboxRelay("mailbox1"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("invalid"));

		// Create datalocation1
		hashMapPostfixMapsStorage.updateDataLocation("datalocation2", "ipdatalocation2");
		// Create mailbox2
		hashMapPostfixMapsStorage.updateMailbox(null, "mailbox2-uid", "mailbox2", "internal", "datalocation2");

		assertEquals("lmtp:ipdatalocation1:2400", hashMapPostfixMapsStorage.mailboxRelay("mailbox1"));
		assertEquals("lmtp:ipdatalocation2:2400", hashMapPostfixMapsStorage.mailboxRelay("mailbox2"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("invalid"));

		// Update mailbox2
		hashMapPostfixMapsStorage.updateMailbox(null, "mailbox2-uid", "mailbox2", "internal", "datalocation1");

		assertEquals("lmtp:ipdatalocation1:2400", hashMapPostfixMapsStorage.mailboxRelay("mailbox1"));
		assertEquals("lmtp:ipdatalocation1:2400", hashMapPostfixMapsStorage.mailboxRelay("mailbox2"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("invalid"));

		// Delete mailbox2
		hashMapPostfixMapsStorage.removeMailbox("mailbox2-uid");
		assertEquals("lmtp:ipdatalocation1:2400", hashMapPostfixMapsStorage.mailboxRelay("mailbox1"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("mailbox2"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("invalid"));

		// Update datalocation1
		hashMapPostfixMapsStorage.updateDataLocation("datalocation1", "ipdatalocation1-new");
		assertEquals("lmtp:ipdatalocation1-new:2400", hashMapPostfixMapsStorage.mailboxRelay("mailbox1"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("invalid"));
	}

	@Test
	public void mailboxRelay_external() {
		// Create datalocation1
		hashMapPostfixMapsStorage.updateDataLocation("datalocation1", "ipdatalocation1");

		// Create mailbox@domain-uid
		hashMapPostfixMapsStorage.updateMailbox("domain-uid", "mailbox-uid", "mailbox@domain-uid", "external",
				"datalocation1");
		hashMapPostfixMapsStorage.updateEmails("mailbox-uid",
				Set.of(new DirEmail("email@domain.tld", true), new DirEmail("alias@domain.tld", false)));

		// Create mailbox-internal@domain-uid
		hashMapPostfixMapsStorage.updateMailbox("domain-uid", "mailbox-internal-uid", "mailbox-internal@domain-uid",
				"internal", "datalocation1");
		hashMapPostfixMapsStorage.updateEmails("mailbox-internal-uid",
				Set.of(new DirEmail("email-internal@domain.tld", false)));

		// Email domain unknown
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("mailbox@domain-uid"));

		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email@domain-uid"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email@domain.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email@domain-alias.tld"));

		assertNull(hashMapPostfixMapsStorage.mailboxRelay("alias@domain-uid"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("alias@domain.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("alias@domain-alias.tld"));

		assertNull(hashMapPostfixMapsStorage.mailboxRelay("unknown@domain-uid"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("unknown@domain.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("unknown@unknown.tld"));

		assertEquals("lmtp:ipdatalocation1:2400",
				hashMapPostfixMapsStorage.mailboxRelay("mailbox-internal@domain-uid"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email-internal@domain.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email-internal@domain-alias.tld"));

		assertNull(hashMapPostfixMapsStorage.mailboxRelay("invalid@unknown.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("invalid"));

		// Add domain
		hashMapPostfixMapsStorage.updateDomain("domain-uid", Collections.emptySet());
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("mailbox@domain-uid"));

		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email@domain-uid"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email@domain.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email@domain-alias.tld"));

		assertNull(hashMapPostfixMapsStorage.mailboxRelay("alias@domain-uid"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("alias@domain.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("alias@domain-alias.tld"));

		assertNull(hashMapPostfixMapsStorage.mailboxRelay("unknown@domain-uid"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("unknown@domain.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("unknown@unknown.tld"));

		assertEquals("lmtp:ipdatalocation1:2400",
				hashMapPostfixMapsStorage.mailboxRelay("mailbox-internal@domain-uid"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email-internal@domain.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email-internal@domain-alias.tld"));

		assertNull(hashMapPostfixMapsStorage.mailboxRelay("invalid@unknown.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("invalid"));

		// Update Domain settings without forwardUnknown
		hashMapPostfixMapsStorage.updateDomainSettings("domain-uid", "smtp-relay", false);
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("mailbox@domain-uid"));

		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email@domain-uid"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email@domain.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email@domain-alias.tld"));

		assertNull(hashMapPostfixMapsStorage.mailboxRelay("alias@domain-uid"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("alias@domain.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("alias@domain-alias.tld"));

		assertNull(hashMapPostfixMapsStorage.mailboxRelay("unknown@domain-uid"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("unknown@domain.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("unknown@unknown.tld"));

		assertEquals("lmtp:ipdatalocation1:2400",
				hashMapPostfixMapsStorage.mailboxRelay("mailbox-internal@domain-uid"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email-internal@domain.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email-internal@domain-alias.tld"));

		assertNull(hashMapPostfixMapsStorage.mailboxRelay("invalid@unknown.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("invalid"));

		// Add Domain settings with forwardUnknown
		hashMapPostfixMapsStorage.updateDomainSettings("domain-uid", "smtp-relay", true);
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("mailbox@domain-uid"));

		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email@domain-uid"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email@domain.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email@domain-alias.tld"));

		assertNull(hashMapPostfixMapsStorage.mailboxRelay("alias@domain-uid"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("alias@domain.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("alias@domain-alias.tld"));

		assertNull(hashMapPostfixMapsStorage.mailboxRelay("unknown@domain-uid"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("unknown@domain.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("unknown@unknown.tld"));

		assertEquals("lmtp:ipdatalocation1:2400",
				hashMapPostfixMapsStorage.mailboxRelay("mailbox-internal@domain-uid"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email-internal@domain.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email-internal@domain-alias.tld"));

		assertNull(hashMapPostfixMapsStorage.mailboxRelay("invalid@unknown.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("invalid"));

		// Add domain alias
		hashMapPostfixMapsStorage.updateDomain("domain-uid", Set.of("domain.tld", "domain-alias.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("mailbox@domain-uid"));

		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email@domain-uid"));
		assertEquals("smtp:smtp-relay:25", hashMapPostfixMapsStorage.mailboxRelay("email@domain.tld"));
		assertEquals("smtp:smtp-relay:25", hashMapPostfixMapsStorage.mailboxRelay("email@domain-alias.tld"));

		assertNull(hashMapPostfixMapsStorage.mailboxRelay("alias@domain-uid"));
		assertEquals("smtp:smtp-relay:25", hashMapPostfixMapsStorage.mailboxRelay("alias@domain.tld"));
		assertEquals("smtp:smtp-relay:25", hashMapPostfixMapsStorage.mailboxRelay("alias@domain-alias.tld"));

		assertNull(hashMapPostfixMapsStorage.mailboxRelay("unknown@domain-uid"));
		assertEquals("smtp:smtp-relay:25", hashMapPostfixMapsStorage.mailboxRelay("unknown@domain.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("unknown@unknown.tld"));

		assertEquals("lmtp:ipdatalocation1:2400",
				hashMapPostfixMapsStorage.mailboxRelay("mailbox-internal@domain-uid"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email-internal@domain.tld"));

		assertEquals("lmtp:ipdatalocation1:2400",
				hashMapPostfixMapsStorage.mailboxRelay("mailbox-internal@domain-uid"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email-internal@domain.tld"));
		assertEquals("smtp:smtp-relay:25", hashMapPostfixMapsStorage.mailboxRelay("email-internal@domain-alias.tld"));

		assertNull(hashMapPostfixMapsStorage.mailboxRelay("invalid@unknown.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("invalid"));

		// Update Domain settings without forwardUnknown
		hashMapPostfixMapsStorage.updateDomainSettings("domain-uid", "smtp-relay", false);
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("mailbox@domain-uid"));

		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email@domain-uid"));
		assertEquals("smtp:smtp-relay:25", hashMapPostfixMapsStorage.mailboxRelay("email@domain.tld"));
		assertEquals("smtp:smtp-relay:25", hashMapPostfixMapsStorage.mailboxRelay("email@domain-alias.tld"));

		assertNull(hashMapPostfixMapsStorage.mailboxRelay("alias@domain-uid"));
		assertEquals("smtp:smtp-relay:25", hashMapPostfixMapsStorage.mailboxRelay("alias@domain.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("alias@domain-alias.tld"));

		assertNull(hashMapPostfixMapsStorage.mailboxRelay("unknown@domain-uid"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("unknown@domain.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("unknown@unknown.tld"));

		assertEquals("lmtp:ipdatalocation1:2400",
				hashMapPostfixMapsStorage.mailboxRelay("mailbox-internal@domain-uid"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email-internal@domain.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email-internal@domain-alias.tld"));

		assertNull(hashMapPostfixMapsStorage.mailboxRelay("invalid@unknown.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("invalid"));

		// Update domain alias
		hashMapPostfixMapsStorage.updateDomain("domain-uid", Set.of("domain.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("mailbox@domain-uid"));

		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email@domain-uid"));
		assertEquals("smtp:smtp-relay:25", hashMapPostfixMapsStorage.mailboxRelay("email@domain.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email@domain-alias.tld"));

		assertNull(hashMapPostfixMapsStorage.mailboxRelay("alias@domain-uid"));
		assertEquals("smtp:smtp-relay:25", hashMapPostfixMapsStorage.mailboxRelay("alias@domain.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("alias@domain-alias.tld"));

		assertNull(hashMapPostfixMapsStorage.mailboxRelay("unknown@domain-uid"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("unknown@domain.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("unknown@unknown.tld"));

		assertEquals("lmtp:ipdatalocation1:2400",
				hashMapPostfixMapsStorage.mailboxRelay("mailbox-internal@domain-uid"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email-internal@domain.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email-internal@domain-alias.tld"));

		assertNull(hashMapPostfixMapsStorage.mailboxRelay("invalid@unknown.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("invalid"));

		// Update Domain settings without null relay
		hashMapPostfixMapsStorage.updateDomainSettings("domain-uid", null, false);
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("mailbox@domain-uid"));

		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email@domain-uid"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email@domain.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email@domain-alias.tld"));

		assertNull(hashMapPostfixMapsStorage.mailboxRelay("alias@domain-uid"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("alias@domain.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("alias@domain-alias.tld"));

		assertNull(hashMapPostfixMapsStorage.mailboxRelay("unknown@domain-uid"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("unknown@domain.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("unknown@unknown.tld"));

		assertEquals("lmtp:ipdatalocation1:2400",
				hashMapPostfixMapsStorage.mailboxRelay("mailbox-internal@domain-uid"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email-internal@domain.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email-internal@domain-alias.tld"));

		assertNull(hashMapPostfixMapsStorage.mailboxRelay("invalid@unknown.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("invalid"));

		// Update Domain settings without empty relay
		hashMapPostfixMapsStorage.updateDomainSettings("domain-uid", "", false);
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("mailbox@domain-uid"));

		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email@domain-uid"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email@domain.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email@domain-alias.tld"));

		assertNull(hashMapPostfixMapsStorage.mailboxRelay("alias@domain-uid"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("alias@domain.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("alias@domain-alias.tld"));

		assertNull(hashMapPostfixMapsStorage.mailboxRelay("unknown@domain-uid"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("unknown@domain.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("unknown@unknown.tld"));

		assertEquals("lmtp:ipdatalocation1:2400",
				hashMapPostfixMapsStorage.mailboxRelay("mailbox-internal@domain-uid"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email-internal@domain.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("email-internal@domain-alias.tld"));

		assertNull(hashMapPostfixMapsStorage.mailboxRelay("invalid@unknown.tld"));
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("invalid"));
	}

	@Test
	public void mailboxRelay_noneOrNull() {
		// Create datalocation1
		hashMapPostfixMapsStorage.updateDataLocation("datalocation1", "ipdatalocation1");
		// Create mailbox1
		hashMapPostfixMapsStorage.updateMailbox("domain-uid", "mailbox1-uid", "mailbox1", null, "datalocation1");
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("mailbox1"));

		hashMapPostfixMapsStorage.updateMailbox("domain-uid", "mailbox1-uid", "mailbox1", "none", "datalocation1");
		assertNull(hashMapPostfixMapsStorage.mailboxRelay("mailbox1"));
	}

	@Test
	public void aliasToMailboxes_mailbox() {
		hashMapPostfixMapsStorage.updateMailbox(null, "entity-uid", "mailbox", "internal", "datalocation");
		hashMapPostfixMapsStorage.updateEmails("entity-uid",
				Set.of(new DirEmail("email1@domain.tld", false), new DirEmail("email2@domain.tld", true)));

		// Email domain not found
		Collection<String> mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("email1@domain.tld");
		assertEquals(0, mailboxes.size());

		// Add domain
		hashMapPostfixMapsStorage.updateDomain("domain-uid", Set.of("domain.tld"));

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("email1@domain.tld");
		assertEquals(1, mailboxes.size());
		assertEquals("mailbox", mailboxes.iterator().next());

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("email2@domain.tld");
		assertEquals(1, mailboxes.size());
		assertEquals("mailbox", mailboxes.iterator().next());

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("unkonwn@domain.tld");
		assertEquals(0, mailboxes.size());
	}

	@Test
	public void aliasToMailboxes_mailbox_splitNotFowardUnknown() {
		hashMapPostfixMapsStorage.updateDomain("domain-uid", Set.of("domain.tld", "domain-alias.tld"));
		// Add Domain settings without forwardUnknown
		hashMapPostfixMapsStorage.updateDomainSettings("domain-uid", "relais", false);

		hashMapPostfixMapsStorage.updateMailbox("domain-uid", "entity-uid", "mailbox@domain-uid", "external",
				"datalocation");
		hashMapPostfixMapsStorage.updateEmails("entity-uid",
				Set.of(new DirEmail("email1@domain.tld", false), new DirEmail("email2@domain.tld", true)));

		Collection<String> mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("mailbox@domain-uid");
		assertEquals(1, mailboxes.size());
		assertTrue(mailboxes.contains("email1@domain.tld") || mailboxes.contains("email2@domain.tld")
				|| mailboxes.contains("email2@domain-alias.tld"));

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("email1@domain.tld");
		assertEquals(1, mailboxes.size());
		assertEquals("email1@domain.tld", mailboxes.iterator().next());

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("email2@domain.tld");
		assertEquals(1, mailboxes.size());
		assertTrue(mailboxes.contains("email1@domain.tld") || mailboxes.contains("email2@domain.tld")
				|| mailboxes.contains("email2@domain-alias.tld"));

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("email2@domain-alias.tld");
		assertEquals(1, mailboxes.size());
		assertTrue(mailboxes.contains("email1@domain.tld") || mailboxes.contains("email2@domain.tld")
				|| mailboxes.contains("email2@domain-alias.tld"));

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("unknown@domain-uid");
		assertEquals(0, mailboxes.size());
		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("unknown@domain.tld");
		assertEquals(0, mailboxes.size());
		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("unknown@unknown.tld");
		assertEquals(0, mailboxes.size());
	}

	@Test
	public void aliasToMailboxes_mailbox_splitFowardUnknown() {
		hashMapPostfixMapsStorage.updateDomain("domain-uid", Set.of("domain.tld", "domain-alias.tld"));
		// Add Domain settings without forwardUnknown
		hashMapPostfixMapsStorage.updateDomainSettings("domain-uid", "relais", true);

		hashMapPostfixMapsStorage.updateMailbox("domain-uid", "entity-uid", "mailbox@domain-uid", "external",
				"datalocation");
		hashMapPostfixMapsStorage.updateEmails("entity-uid",
				Set.of(new DirEmail("email1@domain.tld", false), new DirEmail("email2@domain.tld", true)));

		// External user checks
		Collection<String> mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("mailbox@domain-uid");
		assertEquals(1, mailboxes.size());
		assertTrue(mailboxes.contains("email1@domain.tld") || mailboxes.contains("email2@domain.tld")
				|| mailboxes.contains("email2@domain-alias.tld"));

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("email1@domain.tld");
		assertEquals(1, mailboxes.size());
		assertEquals("email1@domain.tld", mailboxes.iterator().next());

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("email2@domain.tld");
		assertEquals(1, mailboxes.size());
		assertTrue(mailboxes.contains("email1@domain.tld") || mailboxes.contains("email2@domain.tld")
				|| mailboxes.contains("email2@domain-alias.tld"));

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("email2@domain-alias.tld");
		assertEquals(1, mailboxes.size());
		assertTrue(mailboxes.contains("email1@domain.tld") || mailboxes.contains("email2@domain.tld")
				|| mailboxes.contains("email2@domain-alias.tld"));

		// Unknown checks
		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("unknown@domain.tld");
		assertEquals(1, mailboxes.size());
		assertEquals("unknown@domain.tld", mailboxes.iterator().next());

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("unknown@domain-alias.tld");
		assertEquals(1, mailboxes.size());
		assertEquals("unknown@domain-alias.tld", mailboxes.iterator().next());

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("unknown@domain-uid");
		assertEquals(1, mailboxes.size());
		assertEquals("unknown@domain-uid", mailboxes.iterator().next());

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("invalid@domain-unknown.tld");
		assertEquals(0, mailboxes.size());
	}

	@Test
	public void aliasToMailboxes_mailbox_updateEmails() {
		hashMapPostfixMapsStorage.updateMailbox(null, "entity-uid", "mailbox", "internal", "datalocation");
		hashMapPostfixMapsStorage.updateEmails("entity-uid",
				Set.of(new DirEmail("email1@domain.tld", false), new DirEmail("email2@domain.tld", true)));

		// Email domain not found
		Collection<String> mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("email1@domain.tld");
		assertEquals(0, mailboxes.size());

		// Add domain
		hashMapPostfixMapsStorage.updateDomain("domain-uid", Set.of("domain.tld"));

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("email1@domain.tld");
		assertEquals(1, mailboxes.size());
		assertEquals("mailbox", mailboxes.iterator().next());

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("email2@domain.tld");
		assertEquals(1, mailboxes.size());
		assertEquals("mailbox", mailboxes.iterator().next());

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("email3@domain.tld");
		assertEquals(0, mailboxes.size());

		// Update emails - add 1, remove 1
		hashMapPostfixMapsStorage.updateEmails("entity-uid",
				Set.of(new DirEmail("email1@domain.tld", false), new DirEmail("email3@domain.tld", true)));

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("email1@domain.tld");
		assertEquals(1, mailboxes.size());
		assertEquals("mailbox", mailboxes.iterator().next());

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("email2@domain.tld");
		assertEquals(0, mailboxes.size());

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("email3@domain.tld");
		assertEquals(1, mailboxes.size());
		assertEquals("mailbox", mailboxes.iterator().next());
	}

	@Test
	public void aliasToMailboxes_group() {
		hashMapPostfixMapsStorage.updateMailbox(null, "entity1-uid", "mailbox1", "internal", "datalocation");
		hashMapPostfixMapsStorage.updateMailbox(null, "entity2-uid", "mailbox2", "internal", "datalocation");
		hashMapPostfixMapsStorage.updateEmails("group-uid",
				Set.of(new DirEmail("email1@domain.tld", false), new DirEmail("email2@domain.tld", true)));

		Collection<String> mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("email1");
		assertEquals(0, mailboxes.size());

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("email2");
		assertEquals(0, mailboxes.size());

		// entity1-uid is group member but email domain not found
		hashMapPostfixMapsStorage.addRecipient("group-uid", "user", "entity1-uid");
		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("email1@domain.tld");
		assertEquals(0, mailboxes.size());

		// Add domain
		hashMapPostfixMapsStorage.updateDomain("domain-uid", Set.of("domain.tld"));

		// entity1-uid is group member
		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("email1@domain.tld");
		assertEquals(1, mailboxes.size());
		assertEquals("mailbox1", mailboxes.iterator().next());

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("email2@domain.tld");
		assertEquals(1, mailboxes.size());
		assertFalse(mailboxes.contains("mailbox2"));
		assertTrue(mailboxes.contains("mailbox1"));

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("invalid@domain.tld");
		assertEquals(0, mailboxes.size());

		// entity1-uid no more a group member
		hashMapPostfixMapsStorage.removeRecipient("group-uid", "user", "entity1-uid");
		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("email1@domain.tld");
		assertEquals(0, mailboxes.size());

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("email2@domain.tld");
		assertEquals(0, mailboxes.size());

		// entity1-uid is group member
		hashMapPostfixMapsStorage.addRecipient("group-uid", "user", "entity1-uid");
		// group archive is enabled
		hashMapPostfixMapsStorage.updateMailbox(null, "group-uid", "group-mailbox", "internal", "datalocation");
		hashMapPostfixMapsStorage.addRecipient("group-uid", "group-achive", "group-uid");
		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("email1@domain.tld");
		assertEquals(2, mailboxes.size());
		assertTrue(mailboxes.contains("mailbox1"));
		assertTrue(mailboxes.contains("group-mailbox"));

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("email2@domain.tld");
		assertEquals(2, mailboxes.size());
		assertTrue(mailboxes.contains("mailbox1"));
		assertTrue(mailboxes.contains("group-mailbox"));

		// delete group archive is disabled
		hashMapPostfixMapsStorage.removeUid("group-uid");

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("email1@domain.tld");
		assertTrue(mailboxes.isEmpty());

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("email2@domain.tld");
		assertTrue(mailboxes.isEmpty());

		assertFalse(hashMapPostfixMapsStorage.mailboxManaged("group-uid"));
	}

	@Test
	public void aliasToMailboxes_groupOfGroup() {
		// Add domain
		hashMapPostfixMapsStorage.updateDomain("domain-uid", Set.of("domain.tld"));

		// Create users and groups
		hashMapPostfixMapsStorage.updateMailbox(null, "entity1-uid", "mailbox1", "internal", "datalocation");
		hashMapPostfixMapsStorage.updateMailbox(null, "entity2-uid", "mailbox2", "internal", "datalocation");
		hashMapPostfixMapsStorage.updateEmails("group1-uid",
				Set.of(new DirEmail("group1-email@domain.tld", false), new DirEmail("group1-alias@domain.tld", false)));
		hashMapPostfixMapsStorage.updateEmails("group2-uid",
				Set.of(new DirEmail("group2-email@domain.tld", false), new DirEmail("group2-alias@domain.tld", false)));

		// entity1-uid is group1 member
		hashMapPostfixMapsStorage.addRecipient("group1-uid", "user", "entity1-uid");

		// entity1-uid is group member
		Collection<String> mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group1-email@domain.tld");
		assertEquals(1, mailboxes.size());
		assertEquals("mailbox1", mailboxes.iterator().next());

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group1-alias@domain.tld");
		assertEquals(1, mailboxes.size());
		assertEquals("mailbox1", mailboxes.iterator().next());

		// entity2-uid is group2 member
		hashMapPostfixMapsStorage.addRecipient("group2-uid", "user", "entity2-uid");

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group2-email@domain.tld");
		assertEquals(1, mailboxes.size());
		assertEquals("mailbox2", mailboxes.iterator().next());

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group2-alias@domain.tld");
		assertEquals(1, mailboxes.size());
		assertEquals("mailbox2", mailboxes.iterator().next());

		// group2 is member of group1
		hashMapPostfixMapsStorage.addRecipient("group1-uid", "group", "group2-uid");

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group2-email@domain.tld");
		assertEquals(1, mailboxes.size());
		assertEquals("mailbox2", mailboxes.iterator().next());

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group2-alias@domain.tld");
		assertEquals(1, mailboxes.size());
		assertEquals("mailbox2", mailboxes.iterator().next());

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group1-email@domain.tld");
		assertEquals(2, mailboxes.size());
		assertTrue(mailboxes.contains("mailbox1"));
		assertTrue(mailboxes.contains("group2-email@domain.tld") || mailboxes.contains("group2-alias@domain.tld"));

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group1-alias@domain.tld");
		assertEquals(2, mailboxes.size());
		assertTrue(mailboxes.contains("mailbox1"));
		assertTrue(mailboxes.contains("group2-email@domain.tld") || mailboxes.contains("group2-alias@domain.tld"));

		// entity2-uid is no more group2 member
		hashMapPostfixMapsStorage.removeRecipient("group2-uid", "user", "entity2-uid");

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group2-email@domain.tld");
		assertEquals(0, mailboxes.size());

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group2-alias@domain.tld");
		assertEquals(0, mailboxes.size());

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group1-email@domain.tld");
		assertEquals(1, mailboxes.size());
		assertTrue(mailboxes.contains("mailbox1"));

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group1-alias@domain.tld");
		assertEquals(1, mailboxes.size());
		assertTrue(mailboxes.contains("mailbox1"));

		// entity2-uid is group2 member
		hashMapPostfixMapsStorage.addRecipient("group2-uid", "user", "entity2-uid");

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group2-email@domain.tld");
		assertEquals(1, mailboxes.size());
		assertEquals("mailbox2", mailboxes.iterator().next());

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group2-alias@domain.tld");
		assertEquals(1, mailboxes.size());
		assertEquals("mailbox2", mailboxes.iterator().next());

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group1-email@domain.tld");
		assertEquals(2, mailboxes.size());
		assertTrue(mailboxes.contains("mailbox1"));
		assertTrue(mailboxes.contains("group2-email@domain.tld") || mailboxes.contains("group2-alias@domain.tld"));

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group1-alias@domain.tld");
		assertEquals(2, mailboxes.size());
		assertTrue(mailboxes.contains("mailbox1"));
		assertTrue(mailboxes.contains("group2-email@domain.tld") || mailboxes.contains("group2-alias@domain.tld"));

		// group2 is no more a member of group1
		hashMapPostfixMapsStorage.removeRecipient("group1-uid", "group", "group2-uid");

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group2-email@domain.tld");
		assertEquals(1, mailboxes.size());
		assertEquals("mailbox2", mailboxes.iterator().next());

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group2-alias@domain.tld");
		assertEquals(1, mailboxes.size());
		assertEquals("mailbox2", mailboxes.iterator().next());

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group1-email@domain.tld");
		assertEquals(1, mailboxes.size());
		assertTrue(mailboxes.contains("mailbox1"));

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group1-alias@domain.tld");
		assertEquals(1, mailboxes.size());
		assertTrue(mailboxes.contains("mailbox1"));

		// group2 is member of group1
		hashMapPostfixMapsStorage.addRecipient("group1-uid", "group", "group2-uid");

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group2-email@domain.tld");
		assertEquals(1, mailboxes.size());
		assertEquals("mailbox2", mailboxes.iterator().next());

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group2-alias@domain.tld");
		assertEquals(1, mailboxes.size());
		assertEquals("mailbox2", mailboxes.iterator().next());

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group1-email@domain.tld");
		assertEquals(2, mailboxes.size());
		assertTrue(mailboxes.contains("mailbox1"));
		assertTrue(mailboxes.contains("group2-email@domain.tld") || mailboxes.contains("group2-alias@domain.tld"));

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group1-alias@domain.tld");
		assertEquals(2, mailboxes.size());
		assertTrue(mailboxes.contains("mailbox1"));
		assertTrue(mailboxes.contains("group2-email@domain.tld") || mailboxes.contains("group2-alias@domain.tld"));

		// entity2-uid is routing none
		hashMapPostfixMapsStorage.updateMailbox(null, "entity2-uid", "mailbox2", "none", "datalocation");

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group2-email@domain.tld");
		assertEquals(0, mailboxes.size());

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group2-alias@domain.tld");
		assertEquals(0, mailboxes.size());

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group1-email@domain.tld");
		assertEquals(2, mailboxes.size());
		assertTrue(mailboxes.contains("mailbox1"));
		assertTrue(mailboxes.contains("group2-email@domain.tld") || mailboxes.contains("group2-alias@domain.tld"));

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group1-alias@domain.tld");
		assertEquals(2, mailboxes.size());
		assertTrue(mailboxes.contains("mailbox1"));
		assertTrue(mailboxes.contains("group2-email@domain.tld") || mailboxes.contains("group2-alias@domain.tld"));

		// entity2-uid is routing internal
		hashMapPostfixMapsStorage.updateMailbox(null, "entity2-uid", "mailbox2", "internal", "datalocation");

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group2-email@domain.tld");
		assertEquals(1, mailboxes.size());
		assertEquals("mailbox2", mailboxes.iterator().next());

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group2-alias@domain.tld");
		assertEquals(1, mailboxes.size());
		assertEquals("mailbox2", mailboxes.iterator().next());

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group1-email@domain.tld");
		assertEquals(2, mailboxes.size());
		assertTrue(mailboxes.contains("mailbox1"));
		assertTrue(mailboxes.contains("group2-email@domain.tld") || mailboxes.contains("group2-alias@domain.tld"));

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group1-alias@domain.tld");
		assertEquals(2, mailboxes.size());
		assertTrue(mailboxes.contains("mailbox1"));
		assertTrue(mailboxes.contains("group2-email@domain.tld") || mailboxes.contains("group2-alias@domain.tld"));

		// delete entity2-uid
		hashMapPostfixMapsStorage.removeUid("group2-uid");

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group2-email@domain.tld");
		assertEquals(0, mailboxes.size());

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group2-alias@domain.tld");
		assertEquals(0, mailboxes.size());

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group1-email@domain.tld");
		assertEquals(1, mailboxes.size());
		assertTrue(mailboxes.contains("mailbox1"));

		mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group1-alias@domain.tld");
		assertEquals(1, mailboxes.size());
		assertTrue(mailboxes.contains("mailbox1"));
	}

	@Test
	public void aliasToMailboxes_external() {
		// Add domain
		hashMapPostfixMapsStorage.updateDomain("domain-uid", Set.of("domain.tld"));

		// Create users and groups
		hashMapPostfixMapsStorage.updateMailbox(null, "entity1-uid", "mailbox1", "external", "datalocation");
		hashMapPostfixMapsStorage.updateEmails("entity1-uid",
				Set.of(new DirEmail("entity1-external-email@domain.tld", false)));
		hashMapPostfixMapsStorage.updateEmails("group1-uid",
				Set.of(new DirEmail("group1-email@domain.tld", false), new DirEmail("group1-alias@domain.tld", false)));

		// entity1-uid is group1 member
		hashMapPostfixMapsStorage.addRecipient("group1-uid", "user", "entity1-uid");

		Collection<String> mailboxes = hashMapPostfixMapsStorage.aliasToMailboxes("group1-email@domain.tld");
		assertEquals(1, mailboxes.size());
		assertEquals("entity1-external-email@domain.tld", mailboxes.iterator().next());
	}
}
