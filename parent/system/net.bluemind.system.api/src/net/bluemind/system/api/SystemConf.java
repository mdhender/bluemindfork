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
package net.bluemind.system.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class SystemConf {

	public Map<String, String> values = Collections.emptyMap();
	public static final String systemConfSeparator = ",";

	public String stringValue(String prop) {
		return values.get(prop);
	}

	public Boolean booleanValue(String prop) {
		String value = values.get(prop);
		if (value == null) {
			return null;
		} else {
			return Boolean.valueOf(value);
		}
	}

	public List<String> stringList(String prop) {
		String value = values.get(prop);
		if (value == null || value.trim().length() == 0) {
			return Collections.emptyList();
		} else {
			return new ArrayList<>(Arrays.asList(value.split(systemConfSeparator)));
		}
	}

	public void setStringListValue(String prop, List<String> notifiedEmails) {
		values.put(prop, String.join(systemConfSeparator, notifiedEmails));
	}

	public <T> T convertedValue(String prop, Function<String, T> func, T defaultValue) {
		String value = values.get(prop);
		return null == value ? defaultValue : func.apply(value);
	}

	public static SystemConf create(Map<String, String> values) {
		SystemConf c = new SystemConf();
		c.values = values;
		return c;
	}

	/**
	 * Merge newValues to previous and remove key with null value
	 * 
	 * @param previous
	 * @param newValues
	 * @return
	 */
	public static Map<String, String> merge(SystemConf previous, Map<String, String> newValues) {
		Map<String, String> merged = new HashMap<>();
		merged.putAll(previous.values);
		merged.putAll(newValues);
		return merged.entrySet().stream().filter(entry -> entry.getValue() != null)
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
	}

	public Integer integerValue(String prop) {
		String valueAsString = values.get(prop);
		if (valueAsString != null && !"".equals(valueAsString.trim())) {
			return Integer.parseInt(valueAsString);
		} else {
			return null;
		}

	}
}
