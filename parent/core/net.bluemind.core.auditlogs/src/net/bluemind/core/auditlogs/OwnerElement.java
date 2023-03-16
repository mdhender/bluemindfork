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

public record OwnerElement(String displayName, String email, String entryUid, String path) {

	public OwnerElement(OwnerElementBuilder builder) {
		this(builder.displayName, builder.email, builder.entryUid, builder.path);
	}

	public static class OwnerElementBuilder {
		public String displayName;
		public String email;
		public String entryUid;
		public String path;

		public OwnerElementBuilder displayName(String d) {
			displayName = d;
			return this;
		}

		public OwnerElementBuilder email(String e) {
			email = e;
			return this;
		}

		public OwnerElementBuilder entryUid(String e) {
			entryUid = e;
			return this;
		}

		public OwnerElementBuilder path(String p) {
			path = p;
			return this;
		}

		public OwnerElement build() {
			return new OwnerElement(this);
		}
	}

}
