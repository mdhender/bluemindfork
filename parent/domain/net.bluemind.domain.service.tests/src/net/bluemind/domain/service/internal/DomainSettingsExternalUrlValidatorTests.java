/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
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

import org.junit.Test;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.domain.api.DomainSettings;
import net.bluemind.domain.api.DomainSettingsKeys;

public class DomainSettingsExternalUrlValidatorTests {

	private static final String DOMAIN_SETTINGS_KEY = DomainSettingsKeys.external_url.name();
	String domainUid = "bm.lan";

	private DomainSettings emptySettings = new DomainSettings("bm.lan", new HashMap<>());

	private DomainSettingsValidator validator = new DomainSettingsValidator();

	@Test
	public void testNullDomainExternalUrl() {
		DomainSettings ds = new DomainSettings(domainUid, new HashMap<>());

		validator.create(ds.settings, domainUid);
		validator.update(emptySettings.settings, ds.settings, domainUid);

		ds.settings.put(DOMAIN_SETTINGS_KEY, null);
		validator.create(ds.settings, domainUid);
		validator.update(emptySettings.settings, ds.settings, domainUid);
	}

	@Test
	public void testEmptyDomainExternalUrl() {
		DomainSettings ds = new DomainSettings(domainUid, new HashMap<>());
		ds.settings.put(DOMAIN_SETTINGS_KEY, "");

		validator.create(ds.settings, domainUid);
		validator.update(emptySettings.settings, ds.settings, domainUid);
	}

	@Test
	public void testValidDomainExternalUrl() {
		DomainSettings ds = new DomainSettings(domainUid, new HashMap<>());
		ds.settings.put(DOMAIN_SETTINGS_KEY, "ext.bm.lan");

		validator.create(ds.settings, domainUid);
		validator.update(emptySettings.settings, ds.settings, domainUid);
	}

	@Test
	public void testInvalidDomainExternalUrl() {
		DomainSettings ds = new DomainSettings(domainUid, new HashMap<>());
		ds.settings.put(DOMAIN_SETTINGS_KEY, "invalid");

		try {
			validator.create(ds.settings, domainUid);
			fail("invalid " + DOMAIN_SETTINGS_KEY);
		} catch (ServerFault sf) {

		}
	}

}
