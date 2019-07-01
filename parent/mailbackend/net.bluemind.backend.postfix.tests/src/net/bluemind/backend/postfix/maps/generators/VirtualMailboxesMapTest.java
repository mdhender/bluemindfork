/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.backend.postfix.maps.generators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.Test;

import net.bluemind.backend.postfix.internal.maps.DomainInfo;
import net.bluemind.backend.postfix.internal.maps.MapRow;
import net.bluemind.backend.postfix.internal.maps.generators.VirtualMailboxesMap;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.mailbox.api.Mailbox.Routing;

public class VirtualMailboxesMapTest {
	private MapRow getMapRow(ItemValue<Domain> domain, String mailboxName) {
		return new MapRow(domain, 0, null, null, null, null, mailboxName, null);
	}

	@Test
	public void generateMap_emptyDomainCollection_emptymapRowCollection() {
		assertTrue(VirtualMailboxesMap.init(Collections.emptyMap(), Collections.emptyList()).generateMap().isEmpty());
	}

	@Test
	public void generateMap_nullMailboxName() {
		Domain domain = Domain.create("domain.tld", "domain.tld", "Test domain", Collections.emptySet());
		ItemValue<Domain> domainIv = ItemValue.create(domain.name, domain);

		assertTrue(VirtualMailboxesMap.init(Collections.emptyMap(), Arrays.asList(getMapRow(domainIv, null)))
				.generateMap().isEmpty());
	}

	@Test
	public void generateMap_emptyMailboxName() {
		Domain domain = Domain.create("domain.tld", "domain.tld", "Test domain", Collections.emptySet());
		ItemValue<Domain> domainIv = ItemValue.create(domain.name, domain);

		assertTrue(VirtualMailboxesMap.init(Collections.emptyMap(), Arrays.asList(getMapRow(domainIv, "")))
				.generateMap().isEmpty());
	}

	@Test
	public void generateMap_validMailboxName() {
		Domain domain = Domain.create("domain.tld", "domain.tld", "Test domain", Collections.emptySet());
		ItemValue<Domain> domainIv = ItemValue.create(domain.name, domain);

		String map = VirtualMailboxesMap
				.init(Collections.emptyMap(), Arrays.asList(getMapRow(domainIv, "user@" + domain.name))).generateMap();

		assertEquals("user@domain.tld OK\n", map);
		assertEquals(1, map.split("\n").length);
	}

	@Test
	public void generateMap_splitEnabled_mailboxSameDomain() {
		Domain domain = Domain.create("domain.tld", "domain.tld", "Test domain", Collections.emptySet());
		ItemValue<Domain> domainIv = ItemValue.create(domain.name, domain);

		Map<String, String> domainSettings = new HashMap<>();
		domainSettings.put(DomainSettingsKeys.mail_forward_unknown_to_relay.name(), "true");

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domainIv.uid, DomainInfo.build(domainIv, domainSettings));

		String map = VirtualMailboxesMap
				.init(domainInfoByUid, Arrays.asList(getMapRow(domainIv, "user@" + domain.name))).generateMap();

		assertEquals("@domain.tld OK\n", map);
		assertEquals(1, map.split("\n").length);
	}

	@Test
	public void generateMap_splitEnabled_mailboxNotSameDomain() {
		Domain domain1 = Domain.create("domain1.tld", "domain1.tld", "Test domain 1",
				new HashSet<>(Arrays.asList("domain1-alias.tld")));
		ItemValue<Domain> domain1Iv = ItemValue.create(domain1.name, domain1);

		Map<String, String> domain1Settings = new HashMap<>();
		domain1Settings.put(DomainSettingsKeys.mail_forward_unknown_to_relay.name(), "true");

		Domain domain2 = Domain.create("domain2.tld", "domain2.tld", "Test domain 2", Collections.emptySet());
		ItemValue<Domain> domain2Iv = ItemValue.create(domain2.name, domain2);

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain1Iv.uid, DomainInfo.build(domain1Iv, domain1Settings));
		domainInfoByUid.put(domain2Iv.uid, DomainInfo.build(domain2Iv, Collections.emptyMap()));

		String map = VirtualMailboxesMap.init(domainInfoByUid, Arrays
				.asList(getMapRow(domain1Iv, "user@" + domain1.name), getMapRow(domain2Iv, "user@" + domain2.name)))
				.generateMap();

		assertTrue(map.contains("@domain1.tld OK\n"));
		assertTrue(map.contains("@domain1-alias.tld OK\n"));
		assertTrue(map.contains("user@domain2.tld OK\n"));
		assertEquals(3, map.split("\n").length);
	}

	@Test
	public void generateMap_splitEnable_defaultEmailAsMailboxName() {
		Domain domain1 = Domain.create("domain1.tld", "domain1.tld", "Test domain 1",
				new HashSet<>(Arrays.asList("alias-domain1.tld")));
		ItemValue<Domain> domain1Iv = ItemValue.create(domain1.name, domain1);

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain1Iv.uid, DomainInfo.build(domain1Iv, Collections.emptyMap()));

		MapRow mapRow = new MapRow(domain1Iv, 0, null, null, Routing.external, null, "user@" + domain1.name, null);
		mapRow.addEmail("user.mail", "alias-domain1.tld", true, true);

		String map = VirtualMailboxesMap.init(domainInfoByUid, Arrays.asList(mapRow)).generateMap();

		assertEquals("user.mail@alias-domain1.tld OK\n", map);
	}
}
