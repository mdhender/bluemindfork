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

public record SecurityContextElement(String uid, String displayName, String email, String origin) {

	public SecurityContextElement(SecurityContextElementBuilder builder) {
		this(builder.uid, builder.displayName, builder.email, builder.origin);
	}

	public static class SecurityContextElementBuilder {
		public String uid;
		public String displayName;
		public String email;
		public String origin;

		public SecurityContextElementBuilder displayName(String d) {
			displayName = d;
			return this;
		}

		public SecurityContextElementBuilder email(String e) {
			email = e;
			return this;
		}

		public SecurityContextElementBuilder origin(String o) {
			origin = o;
			return this;
		}

		public SecurityContextElementBuilder uid(String u) {
			uid = u;
			return this;
		}

		public SecurityContextElement build() {
			return new SecurityContextElement(this);
		}
	}
}
