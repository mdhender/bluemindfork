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
package net.bluemind.eas.api;

import java.util.Map;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class FolderSyncVersions {
	public Account account;
	public Map<String, String> versions;

	public static FolderSyncVersions create(Account account, Map<String, String> versions) {
		FolderSyncVersions x = new FolderSyncVersions();
		x.account = account;
		x.versions = versions;
		return x;
	}
}