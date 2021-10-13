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
package net.bluemind.domain.service.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.domain.api.DomainSettingsKeys;

public class DomainSettingsValidatorMaxUserTest {

	private DomainSettingsValidator validator = new DomainSettingsValidator();

	@Test
	public void testNullDomainMaxUser() throws ServerFault {
		Map<String, String> settings = new HashMap<>();

		validator.create(settings, "test.lan");
		validator.update(new HashMap<>(), settings, "test.lan");

		settings.put(DomainSettingsKeys.domain_max_users.name(), null);
		validator.create(settings, "test.lan");
		validator.update(new HashMap<>(), settings, "test.lan");
	}

	@Test
	public void testEmptyDomainMaxUser() throws ServerFault {
		Map<String, String> settings = new HashMap<>();
		settings.put(DomainSettingsKeys.domain_max_users.name(), "");

		validator.create(settings, "test.lan");
		validator.update(new HashMap<>(), settings, "test.lan");
	}

	@Test
	public void testValidDomainMaxUser() throws ServerFault {
		Map<String, String> settings = new HashMap<>();
		settings.put(DomainSettingsKeys.domain_max_users.name(), "10");

		validator.create(settings, "test.lan");
		validator.update(new HashMap<>(), settings, "test.lan");
	}

	@Test
	public void testInvalidDomainMaxUser() throws ServerFault {
		Map<String, String> settings = new HashMap<>();

		settings.put(DomainSettingsKeys.domain_max_users.name(), "invalid");
		checkInvalidDomainMaxUser(settings);

		settings.put(DomainSettingsKeys.domain_max_users.name(), "0");
		checkInvalidDomainMaxUser(settings);
	}

	private void checkInvalidDomainMaxUser(Map<String, String> settings) {
		try {
			validator.create(settings, "test.lan");
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Invalid maximum number of users. Must be an integer greater than 0.", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		try {
			validator.update(new HashMap<>(), settings, "test.lan");
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals("Invalid maximum number of users. Must be an integer greater than 0.", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}
}
