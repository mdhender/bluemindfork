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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.eas.dto.find;

import net.bluemind.eas.dto.base.Range;

public class FindRequest {

	public static final class ExecuteSearch {
		public static final class MailBoxSearchCriterion {
			public Query query;
			public Options options;

		}

		public static final class GALSearchCriterion {
			public Query query;
			public Options options;
		}

		public MailBoxSearchCriterion mailBoxSearchCriterion;
		public GALSearchCriterion galSearchCriterion;
	}

	public static final class Query {
		public String freeText;

		/**
		 * MS-ASCMD 2.2.3.30.1 CollectionId (Find)
		 * 
		 * If no airsync:CollectionId element is present, the Find command request will
		 * apply to all folders returned in the FolderSync command response (section
		 * 2.2.1.5) and conduct a global search.
		 */
		public String collectionId;

		/**
		 * MS-ASCMD 2.2.3.27.1 Class (Find)
		 * 
		 * In Find command requests, the only supported value for the airsync:Class
		 * element is "Email".
		 */
		public String airsyncClass = "Email";

	}

	public static final class Options {
		public static final class Picture {
			public Integer maxSize;
			public Integer maxPictures;
		}

		public Range range;
		public boolean deepTraversal;
		public Picture picture;
	}

	public String searchId;
	public ExecuteSearch executeSearch;

}
