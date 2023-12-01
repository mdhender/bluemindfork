/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.index.mail.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map.Entry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import co.elastic.clients.elasticsearch.indices.get_alias.IndexAliases;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.elasticsearch.ElasticsearchClientConfig;

public class AliasRingMailIndexServiceTests extends MailIndexServiceTests {

	static String ACTIVATE_ALIAS_RING_MODE = "elasticsearch.indexation.alias_mode.ring";
	static String ALIAS_RING_MODE_ALIAS_COUNT_MULTIPLIER = "elasticsearch.indexation.alias_mode.mode_ring.alias_count_multiplier";

	@Override
	@Before
	public void before() throws Exception {

		System.setProperty(ACTIVATE_ALIAS_RING_MODE, "true");
		System.setProperty(ALIAS_RING_MODE_ALIAS_COUNT_MULTIPLIER, "4");
		ElasticsearchClientConfig.reload();
		super.before();
	}

	@After
	public void teardown() {
		System.setProperty(ACTIVATE_ALIAS_RING_MODE, "false");
		System.setProperty(ALIAS_RING_MODE_ALIAS_COUNT_MULTIPLIER, "0");
		ElasticsearchClientConfig.reload();
	}

	@Test
	public void testIndexAliasCount() throws Exception {
		List<Entry<String, IndexAliases>> indexAliases = ESearchActivator.getClient().indices().getAlias().result()
				.entrySet().stream().filter(e -> e.getKey().startsWith("mailspool_ring_")).toList();
		assertEquals(5, indexAliases.size());
		assertTrue(indexAliases.stream().anyMatch(entry -> entry.getKey().equals("mailspool_ring_2")));
		assertTrue(indexAliases.stream().anyMatch(entry -> entry.getKey().equals("mailspool_ring_6")));
		assertTrue(indexAliases.stream().anyMatch(entry -> entry.getKey().equals("mailspool_ring_10")));
		assertTrue(indexAliases.stream().anyMatch(entry -> entry.getKey().equals("mailspool_ring_14")));
		assertTrue(indexAliases.stream().anyMatch(entry -> entry.getKey().equals("mailspool_ring_18")));

		IndexAliases aliases = indexAliases.stream().filter(entry -> entry.getKey().equals("mailspool_ring_2")).toList()
				.get(0).getValue();
		assertTrue(aliases.aliases().containsKey("mailspool_ring_alias_read2"));
		assertTrue(aliases.aliases().containsKey("mailspool_ring_alias_read1"));
		assertTrue(aliases.aliases().containsKey("mailspool_ring_alias_read0"));
		assertTrue(aliases.aliases().containsKey("mailspool_ring_alias_read19"));

		aliases = indexAliases.stream().filter(entry -> entry.getKey().equals("mailspool_ring_6")).toList().get(0)
				.getValue();
		assertTrue(aliases.aliases().containsKey("mailspool_ring_alias_read6"));
		assertTrue(aliases.aliases().containsKey("mailspool_ring_alias_read5"));
		assertTrue(aliases.aliases().containsKey("mailspool_ring_alias_read4"));
		assertTrue(aliases.aliases().containsKey("mailspool_ring_alias_read3"));

		aliases = indexAliases.stream().filter(entry -> entry.getKey().equals("mailspool_ring_10")).toList().get(0)
				.getValue();
		assertTrue(aliases.aliases().containsKey("mailspool_ring_alias_read10"));
		assertTrue(aliases.aliases().containsKey("mailspool_ring_alias_read9"));
		assertTrue(aliases.aliases().containsKey("mailspool_ring_alias_read8"));
		assertTrue(aliases.aliases().containsKey("mailspool_ring_alias_read7"));

		aliases = indexAliases.stream().filter(entry -> entry.getKey().equals("mailspool_ring_14")).toList().get(0)
				.getValue();
		assertTrue(aliases.aliases().containsKey("mailspool_ring_alias_read14"));
		assertTrue(aliases.aliases().containsKey("mailspool_ring_alias_read13"));
		assertTrue(aliases.aliases().containsKey("mailspool_ring_alias_read12"));
		assertTrue(aliases.aliases().containsKey("mailspool_ring_alias_read11"));

		aliases = indexAliases.stream().filter(entry -> entry.getKey().equals("mailspool_ring_18")).toList().get(0)
				.getValue();
		assertTrue(aliases.aliases().containsKey("mailspool_ring_alias_read18"));
		assertTrue(aliases.aliases().containsKey("mailspool_ring_alias_read17"));
		assertTrue(aliases.aliases().containsKey("mailspool_ring_alias_read16"));
		assertTrue(aliases.aliases().containsKey("mailspool_ring_alias_read15"));
	}

}
