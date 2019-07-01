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
import java.util.HashSet;

import org.junit.Test;

import net.bluemind.backend.postfix.internal.maps.generators.VirtualDomainsMap;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;

public class VirtualDomainsMapTest {
	@Test
	public void generateMap_emptyDomainCollection() {
		String map = new VirtualDomainsMap(Collections.emptyList()).generateMap();
		assertTrue(map.isEmpty());
	}

	@Test
	public void generateMaps_oneDomain_noAlias() {
		Domain domain = Domain.create("domain.tld", "domain.tld", "Test domain", Collections.emptySet());
		ItemValue<Domain> domainIv = ItemValue.create(domain.name, domain);

		String map = new VirtualDomainsMap(Arrays.asList(domainIv)).generateMap();

		assertEquals("domain.tld OK\n", map);
		assertEquals(1, map.split("\n").length);
	}

	@Test
	public void generateMaps_oneDomain_withAlias() {
		Domain domain = Domain.create("domain.tld", "domain.tld", "Test domain",
				new HashSet<>(Arrays.asList("domain-alias1.tld", "domain-alias2.tld")));
		ItemValue<Domain> domainIv = ItemValue.create(domain.name, domain);

		String map = new VirtualDomainsMap(Arrays.asList(domainIv)).generateMap();

		assertTrue(map.contains("domain.tld OK\n"));
		assertTrue(map.contains("domain-alias1.tld OK\n"));
		assertTrue(map.contains("domain-alias2.tld OK\n"));
		assertEquals(3, map.split("\n").length);
	}

	@Test
	public void generateMaps_multipleDomain_noAlias() {
		Domain domain1 = Domain.create("domain1.tld", "domain1.tld", "Test domain1", Collections.emptySet());
		ItemValue<Domain> domain1Iv = ItemValue.create(domain1.name, domain1);

		Domain domain2 = Domain.create("domain2.tld", "domain2.tld", "Test domain2", Collections.emptySet());
		ItemValue<Domain> domain2Iv = ItemValue.create(domain2.name, domain2);

		String map = new VirtualDomainsMap(Arrays.asList(domain1Iv, domain2Iv)).generateMap();

		assertTrue(map.contains("domain1.tld OK\n"));
		assertTrue(map.contains("domain2.tld OK\n"));
		assertEquals(2, map.split("\n").length);
	}

	@Test
	public void generateMaps_multipleDomain_withAlias() {
		Domain domain1 = Domain.create("domain1.tld", "domain1.tld", "Test domain1",
				new HashSet<>(Arrays.asList("domain1-alias1.tld", "domain1-alias2.tld")));
		ItemValue<Domain> domain1Iv = ItemValue.create(domain1.name, domain1);

		Domain domain2 = Domain.create("domain2.tld", "domain2.tld", "Test domain2",
				new HashSet<>(Arrays.asList("domain2-alias1.tld", "domain2-alias2.tld", "domain2-alias3.tld")));
		ItemValue<Domain> domain2Iv = ItemValue.create(domain2.name, domain2);

		String map = new VirtualDomainsMap(Arrays.asList(domain1Iv, domain2Iv)).generateMap();

		assertTrue(map.contains("domain1.tld OK\n"));
		assertTrue(map.contains("domain1-alias1.tld OK\n"));
		assertTrue(map.contains("domain1-alias2.tld OK\n"));
		assertTrue(map.contains("domain2.tld OK\n"));
		assertTrue(map.contains("domain2-alias1.tld OK\n"));
		assertTrue(map.contains("domain2-alias2.tld OK\n"));
		assertTrue(map.contains("domain2-alias3.tld OK\n"));
		assertEquals(7, map.split("\n").length);
	}

	@Test
	public void generateMaps_duplicateDomain() {
		Domain domain = Domain.create("domain.tld", "domain.tld", "Test domain", Collections.emptySet());
		ItemValue<Domain> domainIv = ItemValue.create(domain.name, domain);

		String map = new VirtualDomainsMap(Arrays.asList(domainIv, domainIv)).generateMap();

		assertEquals("domain.tld OK\n", map);
		assertEquals(1, map.split("\n").length);
	}
}
