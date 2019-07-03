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
package net.bluemind.eas.dto.search;

import net.bluemind.eas.dto.base.BodyOptions;

public class SearchRequest {

	public static final class Store {

		public static final class Options {

			public static final class Picture {
				public Integer maxSize;
				public Integer maxPictures;
			}

			// TODO BodyPartPreference
			// TODO RightsManagementSupport

			public BodyOptions bodyOptions;
			public Range range;
			public String userName;
			public String password;
			public boolean deepTraversal;
			public boolean rebuildResults;
			public Picture picture;
		}

		public static final class Query {

			public static final class And {
				public String freeText;
				public String clazz;
				public String collectionId;
				public String conversationId;
				public String greaterThan;
				public String lessThan;
			}

			public String value;
			public And and;
			public String equalsTo;
		}

		public StoreName name;
		public Query query;
		public Options options;

	}

	public Store store;

}
