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
import java.util.Map;

import org.junit.Test;

import net.bluemind.backend.postfix.internal.maps.MapRow;
import net.bluemind.backend.postfix.internal.maps.generators.TransportMap;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.api.Mailbox.Type;
import net.bluemind.server.api.Server;

public class TransportMapTest {
	@Test
	public void generateMap_emptyMapRowList() {
		String map = new TransportMap(Collections.emptyMap(), Collections.emptyList()).generateMap();
		assertEquals("", map);
	}

	@Test
	public void generateMap_noneRouting() {
		Domain domain = Domain.create("domain.tld", "domain.tld", "Test domain", Collections.emptySet());
		ItemValue<Domain> domainIv = ItemValue.create(domain.name, domain);

		MapRow mapRow = new MapRow(domainIv, 0, null, null, Routing.none, null, null, null);
		String map = new TransportMap(Collections.emptyMap(), Arrays.asList(mapRow)).generateMap();

		assertTrue(map.isEmpty());
	}

	@Test
	public void generateMap_internalRouting_nullMailboxName() {
		Domain domain = Domain.create("domain.tld", "domain.tld", "Test domain", Collections.emptySet());
		ItemValue<Domain> domainIv = ItemValue.create(domain.name, domain);

		MapRow mapRow = new MapRow(domainIv, 0, "", Type.user, Routing.internal, "", null, null);
		String map = new TransportMap(Collections.emptyMap(), Arrays.asList(mapRow)).generateMap();

		assertTrue(map.isEmpty());
	}

	@Test
	public void generateMap_mapRowEmptyDataLocation() {
		Domain domain = Domain.create("domain.tld", "domain.tld", "Test domain", Collections.emptySet());
		ItemValue<Domain> domainIv = ItemValue.create(domain.name, domain);

		MapRow mapRow = new MapRow(domainIv, 0, null, null, null, "", null, null);
		String map = new TransportMap(Collections.emptyMap(), Arrays.asList(mapRow)).generateMap();

		assertTrue(map.isEmpty());
	}

	@Test
	public void generateMap_externalRouting() {
		Domain domain = Domain.create("domain.tld", "domain.tld", "Test domain", Collections.emptySet());
		ItemValue<Domain> domainIv = ItemValue.create(domain.name, domain);

		MapRow mapRow = new MapRow(domainIv, 0, null, null, Routing.external, "relay.domain.tld",
				"mailboxname@domain.tld", null);
		String map = new TransportMap(Collections.emptyMap(), Arrays.asList(mapRow)).generateMap();

		assertEquals("mailboxname@domain.tld smtp:relay.domain.tld:25\n", map);
		assertEquals(1, map.split("\n").length);
	}

	@Test
	public void generateMap_externalRouting_nullDataLocation() {
		Domain domain = Domain.create("domain.tld", "domain.tld", "Test domain", Collections.emptySet());
		ItemValue<Domain> domainIv = ItemValue.create(domain.name, domain);

		MapRow mapRow = new MapRow(domainIv, 0, null, null, Routing.external, null, "mailboxname@domain.tld", null);
		String map = new TransportMap(Collections.emptyMap(), Arrays.asList(mapRow)).generateMap();

		assertTrue(map.isEmpty());
	}

	@Test
	public void generateMap_notExternalRouting() {
		Domain domain = Domain.create("domain.tld", "domain.tld", "Test domain", Collections.emptySet());
		ItemValue<Domain> domainIv = ItemValue.create(domain.name, domain);

		MapRow mapRow1 = new MapRow(domainIv, 0, null, null, Routing.internal, "imap1.domain.tld",
				"mailboxname1@domain.tld", null);
		MapRow mapRow2 = new MapRow(domainIv, 0, null, null, Routing.internal, "imap2.domain.tld",
				"mailboxname2@domain.tld", null);

		String map = new TransportMap(Collections.emptyMap(), Arrays.asList(mapRow1, mapRow2)).generateMap();

		assertTrue(map.contains("mailboxname1@domain.tld lmtp:imap1.domain.tld:2400\n"));
		assertTrue(map.contains("mailboxname2@domain.tld lmtp:imap2.domain.tld:2400\n"));
		assertEquals(2, map.split("\n").length);
	}

	@Test
	public void generateMap_onEdge() {
		Domain domain = Domain.create("domain.tld", "domain.tld", "Test domain", Collections.emptySet());
		ItemValue<Domain> domainIv = ItemValue.create(domain.name, domain);

		Server server = new Server();
		server.ip = "10.0.0.1";
		ItemValue<Server> serverIv = ItemValue.create("smtpserver", server);

		Map<String, ItemValue<Server>> serverByDomainUid = new HashMap<>();
		serverByDomainUid.put(domainIv.uid, serverIv);

		MapRow mapRow = new MapRow(domainIv, 0, null, null, null, null, "mailboxname@domain.tld", null);
		String map = new TransportMap(serverByDomainUid, Arrays.asList(mapRow)).generateMap();

		assertEquals("mailboxname@domain.tld smtp:10.0.0.1:25\n", map);
		assertEquals(1, map.split("\n").length);
	}

	@Test
	public void generateMap_onEdge_noneRouting() {
		Domain domain = Domain.create("domain.tld", "domain.tld", "Test domain", Collections.emptySet());
		ItemValue<Domain> domainIv = ItemValue.create(domain.name, domain);

		Server server = new Server();
		server.ip = "10.0.0.1";
		ItemValue<Server> serverIv = ItemValue.create("smtpserver", server);

		Map<String, ItemValue<Server>> serverByDomainUid = new HashMap<>();
		serverByDomainUid.put(domainIv.uid, serverIv);

		MapRow mapRow = new MapRow(domainIv, 0, null, null, Routing.none, null, "mailboxname@domain.tld", null);
		String map = new TransportMap(serverByDomainUid, Arrays.asList(mapRow)).generateMap();

		assertTrue(map.isEmpty());
	}

	@Test
	public void generateMap_onEdge_internalRouting_nullMailboxName() {
		Domain domain = Domain.create("domain.tld", "domain.tld", "Test domain", Collections.emptySet());
		ItemValue<Domain> domainIv = ItemValue.create(domain.name, domain);

		Server server = new Server();
		server.ip = "10.0.0.1";
		ItemValue<Server> serverIv = ItemValue.create("smtpserver", server);

		Map<String, ItemValue<Server>> serverByDomainUid = new HashMap<>();
		serverByDomainUid.put(domainIv.uid, serverIv);

		MapRow mapRow = new MapRow(domainIv, 0, null, null, Routing.none, null, null, null);
		String map = new TransportMap(serverByDomainUid, Arrays.asList(mapRow)).generateMap();

		assertTrue(map.isEmpty());
	}
}
