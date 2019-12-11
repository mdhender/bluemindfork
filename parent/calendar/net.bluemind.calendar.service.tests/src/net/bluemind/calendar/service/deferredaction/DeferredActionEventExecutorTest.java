/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.calendar.service.deferredaction;

import static net.bluemind.calendar.service.deferredaction.DeferredActionEventExecutor.getLocale;
import static net.bluemind.calendar.service.deferredaction.DeferredActionEventExecutor.getValue;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;

public class DeferredActionEventExecutorTest {

	@Test
	public void getLocaleFromSettingsOrDefaultTest() {
		Map<String, String> settingsWithLang = new HashMap<String, String>();
		String langValue = "fr";
		settingsWithLang.put("lang", langValue);
		Map<String, String> settingsWithoutLang = new HashMap<String, String>();

		assertEquals(Locale.FRENCH, getLocale(settingsWithLang));
		assertEquals(Locale.ENGLISH, getLocale(settingsWithoutLang));
	}

	@Test
	public void getOptionalValueTest() {
		Map<String, String> map = new HashMap<String, String>();
		List<String> keys = Arrays.asList("key1", "key2", "key3");
		keys.forEach(key -> map.put(key, key.replace("key", "value")));
		String defaultValue = "defaultValue";
		assertEquals(map.get("key2"), getValue(map, "key2").orElse(defaultValue));
		assertEquals(map.get("key3"), getValue(map, "key3", "key2").orElse(defaultValue));
		assertEquals(defaultValue, getValue(map, "unknownKey").orElse(defaultValue));
		assertEquals(defaultValue, getValue(map, new String[] { null }).orElse(defaultValue));
	}
}
