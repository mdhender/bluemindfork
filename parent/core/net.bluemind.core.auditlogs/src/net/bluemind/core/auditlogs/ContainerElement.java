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

import com.fasterxml.jackson.annotation.JsonProperty;

public record ContainerElement(String name, String uid, @JsonProperty("owner") OwnerElement ownerElement) {

	public ContainerElement(ContainerElementBuilder builder) {
		this(builder.name, builder.uid, builder.ownerElement);
	}

	public static class ContainerElementBuilder {
		private String name;
		private String uid;
		private OwnerElement ownerElement;

		public ContainerElementBuilder name(String n) {
			name = n;
			return this;
		}

		public ContainerElementBuilder uid(String u) {
			uid = u;
			return this;
		}

		public ContainerElementBuilder ownerElement(OwnerElement o) {
			ownerElement = o;
			return this;
		}

		public ContainerElement build() {
			return new ContainerElement(this);
		}
	}
}
