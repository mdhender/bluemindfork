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

import java.util.Collections;
import java.util.List;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class DomainTemplate {

	@BMApi(version = "3")
	public static class Description {
		@BMApi(version = "3")
		public static class I18NDescription {
			public String lang;
			public String text;
			public String helpText;
		}

		public List<I18NDescription> i18n = Collections.emptyList();

		public String description() {
			for (I18NDescription desc : i18n) {
				if ("en".equals(desc.lang)) {
					return desc.text;
				}
			}

			return null;
		}
	}

	@BMApi(version = "3")
	public static class Kind {
		public Description description;
		public List<Tag> tags = Collections.emptyList();
		public String id;
	}

	@BMApi(version = "3")
	public static class Tag {
		public String value;
		public Description description;
		public boolean multival;
		public boolean mandatory;
		public boolean autoAssign;
	}

	public List<Kind> kinds = Collections.emptyList();
}
