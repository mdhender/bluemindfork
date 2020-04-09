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
package net.bluemind.domain.service.internal;

import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.domain.api.DomainSettings;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.role.api.BasicRoles;

public class DomainSettingsPasswordLifetimeValidatorTests {

	private DomainSettingsPasswordLifetimeValidator validator = new DomainSettingsPasswordLifetimeValidator(
			BmTestContext.context("boss", "bm.lan", BasicRoles.ROLE_MANAGE_DOMAIN));

	private DomainSettings emptySettings = new DomainSettings("bm.lan", new HashMap<>());

	@Test
	public void testNullDomainPasswordLifetime() {
		DomainSettings ds = new DomainSettings("bm.lan", new HashMap<>());

		validator.create(ds);
		validator.update(ds, ds);

		ds.settings.put(DomainSettingsKeys.password_lifetime.name(), null);
		validator.create(ds);
		validator.update(emptySettings, ds);
	}

	@Test
	public void testEmptyDomainPasswordLifetime() {
		Map<String, String> settings = new HashMap<>();
		settings.put(DomainSettingsKeys.password_lifetime.name(), "");
		DomainSettings ds = new DomainSettings("bm.lan", settings);

		validator.create(ds);
		validator.update(emptySettings, ds);
	}

	@Test
	public void testValidDomainPasswordLifetime() {
		Map<String, String> settings = new HashMap<>();
		settings.put(DomainSettingsKeys.password_lifetime.name(), "10");
		DomainSettings ds = new DomainSettings("bm.lan", settings);

		validator.create(ds);
		validator.update(emptySettings, ds);
	}

	@Test
	public void testInvalidDomainPasswordLifetime() {
		DomainSettings ds = new DomainSettings("bm.lan", new HashMap<>());
		ds.settings.put(DomainSettingsKeys.password_lifetime.name(), " ");

		try {
			validator.create(ds);
			fail("invalid password_lifetime");
		} catch (ServerFault sf) {

		}
	}

}
