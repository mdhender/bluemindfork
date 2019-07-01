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
import net.bluemind.backend.postfix.internal.maps.generators.MasterRelayTransportMap;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;

public class MasterRelayTransportMapTest {
	@Test
	public void generateMap_emptyDomainCollection() {
		String map = MasterRelayTransportMap.init(Collections.emptyMap()).generateMap();
		assertTrue(map.isEmpty());
	}

	@Test
	public void generateMap_oneDomain_noAlias_noSplit() {
		Domain domain = Domain.create("domain.tld", "domain.tld", "Test domain", Collections.emptySet());
		ItemValue<Domain> domainIv = ItemValue.create(domain.name, domain);

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domainIv.uid, DomainInfo.build(domainIv, Collections.emptyMap()));

		String map = MasterRelayTransportMap.init(domainInfoByUid).generateMap();
		assertTrue(map.isEmpty());
	}

	@Test
	public void generateMap_oneDomain_withAlias_noSplit() {
		Domain domain = Domain.create("domain.tld", "domain.tld", "Test domain",
				new HashSet<>(Arrays.asList("domain-alias.tld")));
		ItemValue<Domain> domainIv = ItemValue.create(domain.name, domain);

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domainIv.uid, DomainInfo.build(domainIv, Collections.emptyMap()));

		String map = MasterRelayTransportMap.init(domainInfoByUid).generateMap();
		assertTrue(map.isEmpty());
	}

	@Test
	public void generateMap_oneDomain_withAlias_noRelay_forwardUnknonwTrue() {
		Domain domain = Domain.create("domain.tld", "domain.tld", "Test domain",
				new HashSet<>(Arrays.asList("domain-alias.tld")));
		ItemValue<Domain> domainIv = ItemValue.create(domain.name, domain);

		Map<String, String> domainSettings = new HashMap<>();
		domainSettings.put(DomainSettingsKeys.mail_forward_unknown_to_relay.name(), "true");

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domainIv.uid, DomainInfo.build(domainIv, domainSettings));

		String map = MasterRelayTransportMap.init(domainInfoByUid).generateMap();
		assertTrue(map.isEmpty());
	}

	@Test
	public void generateMap_oneDomain_withAlias_withRelay_noForwardUnknonw() {
		Domain domain = Domain.create("domain.tld", "domain.tld", "Test domain",
				new HashSet<>(Arrays.asList("domain-alias.tld")));
		ItemValue<Domain> domainIv = ItemValue.create(domain.name, domain);

		Map<String, String> domainSettings = new HashMap<>();
		domainSettings.put(DomainSettingsKeys.mail_forward_unknown_to_relay.name(), "true");

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domainIv.uid, DomainInfo.build(domainIv, domainSettings));

		String map = MasterRelayTransportMap.init(domainInfoByUid).generateMap();
		assertTrue(map.isEmpty());
	}

	@Test
	public void generateMap_oneDomain_withAlias_withRelay_forwardUnknonwFalse() {
		Domain domain = Domain.create("domain.tld", "domain.tld", "Test domain",
				new HashSet<>(Arrays.asList("domain-alias.tld")));
		ItemValue<Domain> domainIv = ItemValue.create(domain.name, domain);

		Map<String, String> domainSettings = new HashMap<>();
		domainSettings.put(DomainSettingsKeys.mail_forward_unknown_to_relay.name(), "false");
		domainSettings.put(DomainSettingsKeys.mail_routing_relay.name(), "relay.split.tld");

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domainIv.uid, DomainInfo.build(domainIv, domainSettings));

		String map = MasterRelayTransportMap.init(domainInfoByUid).generateMap();
		System.out.println(map);
		assertTrue(map.isEmpty());
	}

	@Test
	public void generateMap_oneDomain_noAlias_withRelay_forwardUnknonwTrue() {
		Domain domain = Domain.create("domain.tld", "domain.tld", "Test domain", Collections.emptySet());
		ItemValue<Domain> domainIv = ItemValue.create(domain.name, domain);

		Map<String, String> domainSettings = new HashMap<>();
		domainSettings.put(DomainSettingsKeys.mail_forward_unknown_to_relay.name(), "true");
		domainSettings.put(DomainSettingsKeys.mail_routing_relay.name(), "relay.split.tld");

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domainIv.uid, DomainInfo.build(domainIv, domainSettings));

		String map = MasterRelayTransportMap.init(domainInfoByUid).generateMap();
		assertEquals("domain.tld smtp:relay.split.tld:25\n", map);
	}

	@Test
	public void generateMap_oneDomain_withAlias_withRelay_forwardUnknonwTrue() {
		Domain domain = Domain.create("domain.tld", "domain.tld", "Test domain",
				new HashSet<>(Arrays.asList("domain-alias.tld")));
		ItemValue<Domain> domainIv = ItemValue.create(domain.name, domain);

		Map<String, String> domainSettings = new HashMap<>();
		domainSettings.put(DomainSettingsKeys.mail_forward_unknown_to_relay.name(), "true");
		domainSettings.put(DomainSettingsKeys.mail_routing_relay.name(), "relay.split.tld");

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domainIv.uid, DomainInfo.build(domainIv, domainSettings));

		String map = MasterRelayTransportMap.init(domainInfoByUid).generateMap();

		assertTrue(map.contains("domain.tld smtp:relay.split.tld:25\n"));
		assertTrue(map.contains("domain-alias.tld smtp:relay.split.tld:25\n"));
		assertEquals(2, map.split("\n").length);
	}

	@Test
	public void generateMap_twoDomain_oneWithSplit() {
		Domain domain1 = Domain.create("domain1.tld", "domain1.tld", "Test domain 1",
				new HashSet<>(Arrays.asList("domain1-alias.tld")));
		ItemValue<Domain> domain1Iv = ItemValue.create(domain1.name, domain1);

		Map<String, String> domain1Settings = new HashMap<>();
		domain1Settings.put(DomainSettingsKeys.mail_forward_unknown_to_relay.name(), "true");
		domain1Settings.put(DomainSettingsKeys.mail_routing_relay.name(), "relay.split.tld");

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain1Iv.uid, DomainInfo.build(domain1Iv, domain1Settings));

		Domain domain2 = Domain.create("domain2.tld", "domain2.tld", "Test domain 2",
				new HashSet<>(Arrays.asList("domain2-alias.tld")));
		ItemValue<Domain> domain2Iv = ItemValue.create(domain2.name, domain2);
		domainInfoByUid.put(domain2Iv.uid, DomainInfo.build(domain2Iv, Collections.emptyMap()));

		String map = MasterRelayTransportMap.init(domainInfoByUid).generateMap();

		assertTrue(map.contains("domain1.tld smtp:relay.split.tld:25\n"));
		assertTrue(map.contains("domain1-alias.tld smtp:relay.split.tld:25\n"));
		assertEquals(2, map.split("\n").length);
	}
}
