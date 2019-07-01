/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.backend.mail.api;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class SearchQuery {

	public String searchSessionId;
	public String query;
	public long maxResults;
	public long offset;
	public SearchScope scope;

	@BMApi(version = "3")
	public static class SearchScope {
		public FolderScope folderScope;
		public boolean isDeepTraversal;
	}

	@BMApi(version = "3")
	public static class FolderScope {
		public String folderUid;
	}

}
