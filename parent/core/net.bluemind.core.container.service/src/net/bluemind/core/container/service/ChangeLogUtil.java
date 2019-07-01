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
package net.bluemind.core.container.service;

import java.util.stream.Collectors;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;

public class ChangeLogUtil {

	public static ItemChangelog getItemChangeLog(String itemUid, Long since, BmContext bmContext,
			ContainerStoreService<?> storeService, String domainUid) throws ServerFault {
		IDirectory directoryService = bmContext.provider().instance(IDirectory.class, domainUid);

		ItemChangelog changelog = storeService.changelog(itemUid, since, Long.MAX_VALUE);
		changelog.entries = changelog.entries.stream().map((entry) -> {
			try {
				DirEntry dirEntry = directoryService.findByEntryUid(entry.author);
				if (null != dirEntry) {
					entry.authorDisplayName = dirEntry.displayName;
				}
			} catch (Exception e) {
			}
			return entry;
		}).collect(Collectors.toList());
		return changelog;
	}

}
