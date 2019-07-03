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

public class DomainSettingsMaxBasicAccountValidatorTests {

	private DomainSettingsMaxBasicAccountValidator validator = new DomainSettingsMaxBasicAccountValidator(
			BmTestContext.context("boss", "bm.lan", BasicRoles.ROLE_DOMAIN_MAX_VALUES));

	private DomainSettings emptySettings = new DomainSettings("bm.lan", new HashMap<>());

	@Test
	public void testNullDomainMaxBasicAccount() {
		DomainSettings ds = new DomainSettings("bm.lan", new HashMap<>());

		validator.create(ds);
		validator.update(ds, ds);

		ds.settings.put(DomainSettingsKeys.domain_max_basic_account.name(), null);
		validator.create(ds);
		validator.update(emptySettings, ds);
	}

	@Test
	public void testEmptyDomainMaxBasicAccount() {
		Map<String, String> settings = new HashMap<>();
		settings.put(DomainSettingsKeys.domain_max_basic_account.name(), "");
		DomainSettings ds = new DomainSettings("bm.lan", settings);

		validator.create(ds);
		validator.update(emptySettings, ds);
	}

	@Test
	public void testValidDomainMaxBasicAccount() {
		Map<String, String> settings = new HashMap<>();
		settings.put(DomainSettingsKeys.domain_max_basic_account.name(), "10");
		DomainSettings ds = new DomainSettings("bm.lan", settings);

		validator.create(ds);
		validator.update(emptySettings, ds);
	}

	@Test
	public void testInvalidDomainMaxBasicAccount() {
		DomainSettings ds = new DomainSettings("bm.lan", new HashMap<>());
		ds.settings.put(DomainSettingsKeys.domain_max_basic_account.name(), "invalid");

		try {
			validator.create(ds);
			fail("invalid domain_max_basic_account");
		} catch (ServerFault sf) {

		}
	}

}
