/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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

package net.bluemind.backend.postfix;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.core.tests.vertx.VertxEventChecker;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.lib.vertx.VertxPlatform;

public class DomainHookTests {
	@Before
	public void before() throws Exception {
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
	}

	@Test
	public void onSettingsUpdated_nullPrevious_nullCurrent() {
		VertxEventChecker<JsonObject> dirtyMapChecker = new VertxEventChecker<>("postfix.map.dirty");

		new DomainHook().onSettingsUpdated(null, null, null, null);

		dirtyMapChecker.shouldFail();
	}

	@Test
	public void onSettingsUpdated_nullPrevious_emptyCurrent() {
		VertxEventChecker<JsonObject> dirtyMapChecker = new VertxEventChecker<>("postfix.map.dirty");

		new DomainHook().onSettingsUpdated(null, null, null, Collections.emptyMap());

		dirtyMapChecker.shouldFail();
	}

	@Test
	public void onSettingsUpdated_emptyPrevious_nullCurrent() {
		VertxEventChecker<JsonObject> dirtyMapChecker = new VertxEventChecker<>("postfix.map.dirty");

		new DomainHook().onSettingsUpdated(null, null, Collections.emptyMap(), null);

		dirtyMapChecker.shouldFail();
	}

	@Test
	public void onSettingsUpdated_sameValues() {
		VertxEventChecker<JsonObject> dirtyMapChecker = new VertxEventChecker<>("postfix.map.dirty");

		Map<String, String> current = new HashMap<>();
		current.put(DomainSettingsKeys.mail_routing_relay.name(), "relay");
		current.put(DomainSettingsKeys.mail_forward_unknown_to_relay.name(), Boolean.toString(true));

		Map<String, String> previous = new HashMap<>();
		previous.put(DomainSettingsKeys.mail_routing_relay.name(), "relay");
		previous.put(DomainSettingsKeys.mail_forward_unknown_to_relay.name(), Boolean.toString(true));

		new DomainHook().onSettingsUpdated(null, null, previous, current);

		dirtyMapChecker.shouldFail();
	}

	@Test
	public void onSettingsUpdated_updateRoutingRelay() {
		VertxEventChecker<JsonObject> dirtyMapChecker = new VertxEventChecker<>("postfix.map.dirty");

		Map<String, String> current = new HashMap<>();
		current.put(DomainSettingsKeys.mail_routing_relay.name(), "relay");
		current.put(DomainSettingsKeys.mail_forward_unknown_to_relay.name(), Boolean.toString(true));

		Map<String, String> previous = new HashMap<>();
		previous.put(DomainSettingsKeys.mail_routing_relay.name(), "updatedrelay");
		previous.put(DomainSettingsKeys.mail_forward_unknown_to_relay.name(), Boolean.toString(true));

		new DomainHook().onSettingsUpdated(null, null, previous, current);

		assertNotNull(dirtyMapChecker.shouldSuccess());
	}

	@Test
	public void onSettingsUpdated_updateForwardToUnknoww() {
		VertxEventChecker<JsonObject> dirtyMapChecker = new VertxEventChecker<>("postfix.map.dirty");

		Map<String, String> current = new HashMap<>();
		current.put(DomainSettingsKeys.mail_routing_relay.name(), "relay");
		current.put(DomainSettingsKeys.mail_forward_unknown_to_relay.name(), Boolean.toString(true));

		Map<String, String> previous = new HashMap<>();
		previous.put(DomainSettingsKeys.mail_routing_relay.name(), "relay");
		previous.put(DomainSettingsKeys.mail_forward_unknown_to_relay.name(), Boolean.toString(false));

		new DomainHook().onSettingsUpdated(null, null, previous, current);

		assertNotNull(dirtyMapChecker.shouldSuccess());
	}

	@Test
	public void onAliasesUpdated_nullPrevious_nullCurrent() {
		VertxEventChecker<JsonObject> dirtyMapChecker = new VertxEventChecker<>("postfix.map.dirty");

		Domain domain = Domain.create("domain.tld", "domain.tld", "domain.tld", null);
		assertNull(domain.aliases);

		new DomainHook().onAliasesUpdated(null, ItemValue.create(domain.name, domain), null);

		dirtyMapChecker.shouldFail();
	}

	@Test
	public void onAliasesUpdated_nullPrevious_emptyCurrent() {
		VertxEventChecker<JsonObject> dirtyMapChecker = new VertxEventChecker<>("postfix.map.dirty");

		Domain domain = Domain.create("domain.tld", "domain.tld", "domain.tld", Collections.emptySet());

		new DomainHook().onAliasesUpdated(null, ItemValue.create(domain.name, domain), null);

		dirtyMapChecker.shouldFail();
	}

	@Test
	public void onAliasesUpdated_emptyPrevious_nullCurrent() {
		VertxEventChecker<JsonObject> dirtyMapChecker = new VertxEventChecker<>("postfix.map.dirty");

		Domain domain = Domain.create("domain.tld", "domain.tld", "domain.tld", null);
		assertNull(domain.aliases);

		new DomainHook().onAliasesUpdated(null, ItemValue.create(domain.name, domain), Collections.emptySet());

		dirtyMapChecker.shouldFail();
	}

	@Test
	public void onAliasesUpdated_sameAliases() {
		VertxEventChecker<JsonObject> dirtyMapChecker = new VertxEventChecker<>("postfix.map.dirty");

		Domain domain = Domain.create("domain.tld", "domain.tld", "domain.tld",
				new HashSet<>(Arrays.asList("domain-alias.tld")));

		new DomainHook().onAliasesUpdated(null, ItemValue.create(domain.name, domain),
				new HashSet<>(Arrays.asList("domain-alias.tld")));

		dirtyMapChecker.shouldFail();
	}

	@Test
	public void onAliasesUpdated_updatedAliases() {
		VertxEventChecker<JsonObject> dirtyMapChecker = new VertxEventChecker<>("postfix.map.dirty");

		Domain domain = Domain.create("domain.tld", "domain.tld", "domain.tld",
				new HashSet<>(Arrays.asList("domain-alias1.tld")));

		new DomainHook().onAliasesUpdated(new BmTestContext(SecurityContext.SYSTEM),
				ItemValue.create(domain.name, domain), new HashSet<>(Arrays.asList("domain-alias2.tld")));

		assertNotNull(dirtyMapChecker.shouldSuccess());
	}
}
