/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import net.bluemind.domain.api.DomainSettingsKeys;

public class DomainSettingsSanitizerTest {

	private DomainSettingsSanitizer sanitizer = new DomainSettingsSanitizer();

	@Test
	public void nullCheckOfEmptyMap() {
		Map<String, String> settings = new HashMap<>();
		sanitizer.sanitize(settings);

		assertEquals(0, settings.size());
	}

	@Test
	public void nullCheckOfUnknownKey() {
		Map<String, String> settings = new HashMap<>();
		settings.put("blabla", null);
		sanitizer.sanitize(settings);

		assertEquals(1, settings.size());
	}

	@Test
	public void nullCheckOfNullableKey() {
		Map<String, String> settings = new HashMap<>();
		settings.put(DomainSettingsKeys.domain_max_basic_account.name(), null);
		sanitizer.sanitize(settings);

		assertEquals(1, settings.size());
	}

	@Test
	public void nullCheckOfNotNullableKey() {
		Map<String, String> settings = new HashMap<>();
		settings.put(DomainSettingsKeys.date.name(), null);
		sanitizer.sanitize(settings);

		assertEquals(0, settings.size());
	}

}
