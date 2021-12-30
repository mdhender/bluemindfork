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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.milter.srs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import net.bluemind.config.InstallationId;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.milter.action.DomainAliasCache;
import net.bluemind.milter.map.RecipientCanonical;
import net.bluemind.milter.srs.tools.SrsHash;

public class SrsRecipientTests {
	public static class DomainAliasCacheFiller extends DomainAliasCache {
		public static void addDomain(ItemValue<Domain> domain) {
			domain.value.aliases.forEach(alias -> domainCache.put(alias, domain));
		}
	}

	@Before
	public void before() {
		Domain domain = new Domain();
		domain.aliases = new HashSet<>(Arrays.asList("domain.tld", "alias.tld"));

		DomainAliasCacheFiller.addDomain(ItemValue.create(Item.create(UUID.randomUUID().toString(), null), domain));
	}

	@Test
	public void execute_invalidEmail() {
		assertFalse(srsRecipient().execute(null).isPresent());
		assertFalse(srsRecipient().execute("").isPresent());
		assertFalse(srsRecipient().execute("@domain.tld").isPresent());
	}

	@Test
	public void execute_nonLocalEmail() {
		assertFalse(srsRecipient().execute("john@ext-domain.tld").isPresent());
	}

	@Test
	public void execute_nonSrsEmail() {
		assertFalse(srsRecipient().execute("john@domain.tld").isPresent());
	}

	@Test
	public void execute_invalidSrsEmail() {
		// Hash do not match
		assertFalse(srsRecipient().execute("SRS0=Y5Eg=R6=ext-domain=john@domain.tld").isPresent());

		// Not from local domain
		SrsHash srsHash = SrsHash.build(InstallationId.getIdentifier()).orElse(null);
		assertNotNull(srsHash);

		String srsEmail = SrsData.forEmail(srsHash, "john@ext-domain.tld")
				.map(srsData -> srsData.srsEmail("ext-domain.tld")).orElse(null);
		assertNotNull(srsEmail);

		assertFalse(srsRecipient().execute(srsEmail).isPresent());
	}

	@Test
	public void execute_srsEmail() {
		SrsHash srsHash = SrsHash.build(InstallationId.getIdentifier()).orElse(null);
		assertNotNull(srsHash);

		String srsEmail = SrsData.forEmail(srsHash, "john@ext-domain.tld")
				.map(srsData -> srsData.srsEmail("domain.tld")).orElse(null);
		assertNotNull(srsEmail);

		Optional<String> unSrsEmail = srsRecipient().execute(srsEmail);
		assertTrue(unSrsEmail.isPresent());
		assertEquals("john@ext-domain.tld", unSrsEmail.get());
	}

	private RecipientCanonical srsRecipient() {
		return new SrsRecipient.SrsRecipientFactory().create();
	}
}
