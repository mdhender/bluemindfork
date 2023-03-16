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

public record ItemElement(String uid, long id, String displayName, long version) {

	public ItemElement(ItemElementBuilder builder) {
		this(builder.uid, builder.id, builder.displayName, builder.version);
	}

	public static class ItemElementBuilder {
		public String uid;
		public long id;
		public String displayName;
		public long version;

		public ItemElementBuilder uid(String u) {
			uid = u;
			return this;
		}

		public ItemElementBuilder id(long i) {
			id = i;
			return this;
		}

		public ItemElementBuilder displayName(String d) {
			displayName = d;
			return this;
		}

		public ItemElementBuilder version(long v) {
			version = v;
			return this;
		}

		public ItemElement build() {
			return new ItemElement(this);
		}
	}
}
