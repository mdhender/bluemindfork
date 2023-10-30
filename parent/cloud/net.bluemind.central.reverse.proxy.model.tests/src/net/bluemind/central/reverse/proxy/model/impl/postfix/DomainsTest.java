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
package net.bluemind.central.reverse.proxy.model.impl.postfix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

import net.bluemind.central.reverse.proxy.model.impl.postfix.Domains.DomainAliases;

public class DomainsTest {
	@Test
	public void empty() {
		Domains domains = new Domains();
		assertNull(domains.getDomainAliases("unknown"));
		assertNull(domains.getDomainSettings("unknown"));

		assertTrue(domains.domainUidFromAlias("unknown-alias").isEmpty());
	}

	@Test
	public void addAliasesFirst() {
		Domains domains = new Domains();
		domains.updateDomainAliases("domain-uid", Set.of("alias"));

		assertNull(domains.getDomainAliases("unknown"));
		assertNull(domains.getDomainSettings("unknown"));

		assertEquals("domain-uid", domains.getDomainAliases("domain-uid").uid());
		assertEquals(1, domains.getDomainAliases("domain-uid").aliases().size());
		assertTrue(domains.getDomainAliases("domain-uid").aliases().contains("alias"));

		assertNull(domains.getDomainSettings("domain-uid"));

		domains.updateDomainSetting("domain-uid", "relay", false);

		assertEquals("domain-uid", domains.getDomainAliases("domain-uid").uid());
		assertEquals(1, domains.getDomainAliases("domain-uid").aliases().size());
		assertTrue(domains.getDomainAliases("domain-uid").aliases().contains("alias"));

		assertEquals("domain-uid", domains.getDomainSettings("domain-uid").uid());
		assertEquals("relay", domains.getDomainSettings("domain-uid").mailRoutingRelay());
		assertFalse(domains.getDomainSettings("domain-uid").mailForwardUnknown());
	}

	@Test
	public void addSettingsFirst() {
		Domains domains = new Domains();
		domains.updateDomainSetting("domain-uid", "relay", false);

		assertNull(domains.getDomainAliases("unknown"));
		assertNull(domains.getDomainSettings("unknown"));

		assertNull(domains.getDomainAliases("domain-uid"));

		assertEquals("domain-uid", domains.getDomainSettings("domain-uid").uid());
		assertEquals("relay", domains.getDomainSettings("domain-uid").mailRoutingRelay());
		assertFalse(domains.getDomainSettings("domain-uid").mailForwardUnknown());

		domains.updateDomainAliases("domain-uid", Set.of("alias"));

		assertEquals("domain-uid", domains.getDomainAliases("domain-uid").uid());
		assertEquals(1, domains.getDomainAliases("domain-uid").aliases().size());
		assertTrue(domains.getDomainAliases("domain-uid").aliases().contains("alias"));

		assertEquals("domain-uid", domains.getDomainSettings("domain-uid").uid());
		assertEquals("relay", domains.getDomainSettings("domain-uid").mailRoutingRelay());
		assertFalse(domains.getDomainSettings("domain-uid").mailForwardUnknown());
	}

	@Test
	public void updateAliases() {
		Domains domains = new Domains();
		domains.updateDomainAliases("domain-other", Set.of("other-alias"));
		domains.updateDomainSetting("domain-other", "relay-other", false);

		domains.updateDomainAliases("domain-uid", Set.of("alias"));
		domains.updateDomainSetting("domain-uid", "relay", false);

		checkOther(domains);

		assertEquals("domain-uid", domains.getDomainAliases("domain-uid").uid());
		assertEquals(1, domains.getDomainAliases("domain-uid").aliases().size());
		assertTrue(domains.getDomainAliases("domain-uid").aliases().contains("alias"));

		assertEquals("domain-uid", domains.getDomainSettings("domain-uid").uid());
		assertEquals("relay", domains.getDomainSettings("domain-uid").mailRoutingRelay());
		assertFalse(domains.getDomainSettings("domain-uid").mailForwardUnknown());

		domains.updateDomainAliases("domain-uid", Set.of("alias1", "alias2"));

		checkOther(domains);

		assertEquals("domain-uid", domains.getDomainAliases("domain-uid").uid());
		assertEquals(2, domains.getDomainAliases("domain-uid").aliases().size());
		assertTrue(domains.getDomainAliases("domain-uid").aliases().contains("alias1"));
		assertTrue(domains.getDomainAliases("domain-uid").aliases().contains("alias2"));

		assertEquals("domain-uid", domains.getDomainSettings("domain-uid").uid());
		assertEquals("relay", domains.getDomainSettings("domain-uid").mailRoutingRelay());
		assertFalse(domains.getDomainSettings("domain-uid").mailForwardUnknown());
	}

	private void checkOther(Domains domains) {
		assertEquals("domain-other", domains.getDomainAliases("domain-other").uid());
		assertEquals(1, domains.getDomainAliases("domain-other").aliases().size());
		assertTrue(domains.getDomainAliases("domain-other").aliases().contains("other-alias"));

		assertEquals("domain-other", domains.getDomainSettings("domain-other").uid());
		assertEquals("relay-other", domains.getDomainSettings("domain-other").mailRoutingRelay());
		assertFalse(domains.getDomainSettings("domain-other").mailForwardUnknown());
	}

	@Test
	public void updateSettings() {
		Domains domains = new Domains();
		domains.updateDomainAliases("domain-uid", Set.of("alias"));
		domains.updateDomainSetting("domain-uid", "relay", false);

		assertEquals("domain-uid", domains.getDomainAliases("domain-uid").uid());
		assertEquals(1, domains.getDomainAliases("domain-uid").aliases().size());
		assertTrue(domains.getDomainAliases("domain-uid").aliases().contains("alias"));

		assertEquals("domain-uid", domains.getDomainSettings("domain-uid").uid());
		assertEquals("relay", domains.getDomainSettings("domain-uid").mailRoutingRelay());
		assertFalse(domains.getDomainSettings("domain-uid").mailForwardUnknown());

		domains.updateDomainSetting("domain-uid", "new-relay", true);

		assertEquals("domain-uid", domains.getDomainAliases("domain-uid").uid());
		assertEquals(1, domains.getDomainAliases("domain-uid").aliases().size());
		assertTrue(domains.getDomainAliases("domain-uid").aliases().contains("alias"));

		assertEquals("domain-uid", domains.getDomainSettings("domain-uid").uid());
		assertEquals("new-relay", domains.getDomainSettings("domain-uid").mailRoutingRelay());
		assertTrue(domains.getDomainSettings("domain-uid").mailForwardUnknown());
	}

	@Test
	public void domainAliases() {
		DomainAliases domainAliases = new DomainAliases("domain-uid", Set.of("alias1", "alias2"));

		assertTrue(domainAliases.match("domain-uid"));
		assertTrue(domainAliases.match("alias1"));
		assertTrue(domainAliases.match("alias2"));
		assertFalse(domainAliases.match("unknown"));

		DomainAliases domainAliasesOnly = domainAliases.aliasOnly();
		assertNull(domainAliasesOnly.uid());
		assertEquals(2, domainAliasesOnly.aliases().size());
		assertTrue(domainAliasesOnly.aliases().contains("alias1"));
		assertTrue(domainAliasesOnly.aliases().contains("alias2"));
	}

	@Test
	public void domainUidFromAlias() {
		Domains domains = new Domains();
		domains.updateDomainAliases("domain-uid", Set.of("alias1", "alias2"));

		assertFalse(domains.domainUidFromAlias("unknown").isPresent());
		assertEquals("domain-uid", domains.domainUidFromAlias("alias1").get());
		assertEquals("domain-uid", domains.domainUidFromAlias("alias2").get());
	}

	@Test
	public void remove() {
		Domains domains = new Domains();
		domains.updateDomainAliases("domain1-uid", Set.of("domain1-alias1", "domain1-alias2"));
		domains.updateDomainSetting("domain1-uid", "relay1", false);
		domains.updateDomainAliases("domain2-uid", Set.of("domain2-alias1", "domain2-alias2"));
		domains.updateDomainSetting("domain2-uid", "relay2", false);

		domains.removeDomain("domain1-uid");

		assertNull(domains.getDomainAliases("domain1-uid"));
		assertNull(domains.getDomainSettings("domain1-uid"));

		assertEquals("domain2-uid", domains.getDomainAliases("domain2-uid").uid());
		assertEquals(2, domains.getDomainAliases("domain2-uid").aliases().size());
		assertTrue(domains.getDomainAliases("domain2-uid").aliases().contains("domain2-alias1"));
		assertTrue(domains.getDomainAliases("domain2-uid").aliases().contains("domain2-alias2"));
	}
}
