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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.utils;

import java.util.Optional;

import com.google.common.base.Splitter;

import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTimeWrapper;

public class HeaderUtil {

	private final String header;

	public HeaderUtil(String header) {
		this.header = header;
	}

	public Optional<Value> getHeaderAttribute(String attribute) {
		Iterable<String> parts = Splitter.on(';').trimResults().omitEmptyStrings().split(header);
		for (String headerPart : parts) {
			if (headerPart.startsWith(attribute)) {
				String value = headerPart.substring((attribute + "=\"").length(), headerPart.length() - 1);
				return Optional.of(new Value(value));
			}
		}
		return Optional.empty();
	}

	public Optional<Value> getHeaderValue() {
		Iterable<String> parts = Splitter.on(';').trimResults().omitEmptyStrings().split(header);
		for (String headerPart : parts) {
			if (!headerPart.contains("=")) {
				return Optional.of(new Value(headerPart));
			}
		}
		return Optional.empty();
	}

	public static class Value {
		public final String value;

		public Value(String value) {
			this.value = value;
		}

		public BmDateTime toDate() {
			return BmDateTimeWrapper.create(value);
		}

		public boolean toBoolean() {
			return Boolean.valueOf(value);
		}

		@Override
		public String toString() {
			return value;
		}
	}

}
