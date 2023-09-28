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

package net.bluemind.core.auditlogs;

import java.util.ArrayList;
import java.util.List;

public record ContentElement(String key, String description, List<String> with, List<String> author, List<String> is,
		List<String> has, String newValue) {

	public ContentElement(ContentElementBuilder builder) {
		this(builder.key, builder.description, builder.with, builder.author, builder.is, builder.has, builder.newValue);
	}

	public static class ContentElementBuilder {
		public String description;
		public String key;
		public List<String> with = new ArrayList<>();
		public List<String> author = new ArrayList<>();
		public List<String> is = new ArrayList<>();
		public List<String> has = new ArrayList<>();
		public String newValue;

		public ContentElementBuilder description(String d) {
			description = d;
			return this;
		}

		public ContentElementBuilder key(String k) {
			key = k;
			return this;
		}

		public ContentElementBuilder with(List<String> w) {
			with = w;
			return this;
		}

		public ContentElementBuilder addWith(String w) {
			with.add(w);
			return this;
		}

		public ContentElementBuilder author(List<String> a) {
			author = a;
			return this;
		}

		public ContentElementBuilder newValue(String v) {
			newValue = v;
			return this;
		}

		public ContentElementBuilder is(List<String> i) {
			is = i;
			return this;
		}

		public ContentElementBuilder has(List<String> h) {
			has = h;
			return this;
		}

		public ContentElement build() {
			return new ContentElement(this);
		}
	}

	@Override
	public String toString() {
		return "key=" + key + " ,description=" + description + " ,with=" + with + " ,author=" + author;
	}
}
