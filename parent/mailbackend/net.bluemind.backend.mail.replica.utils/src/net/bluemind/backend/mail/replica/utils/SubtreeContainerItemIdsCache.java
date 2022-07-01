/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.mail.replica.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;

/**
 * When creating a folder with mail api
 * {@link IMailboxFolders#createById(long, net.bluemind.backend.mail.api.MailboxFolder)}
 * we want the replication to set the right itemId.
 * 
 * I did not find a way to pass the wanted id at creation time, annotations are
 * sent after the create. Only a cyrus patch to create would do the trick :/
 * 
 */
public class SubtreeContainerItemIdsCache {
	private static final Logger logger = LoggerFactory.getLogger(SubtreeContainerItemIdsCache.class);

	private SubtreeContainerItemIdsCache() {

	}

	private static final Cache<String, Long> folderKeyToSubtreeContainerItemId = //
			Caffeine.newBuilder().recordStats().maximumSize(512).build();

	public static class CacheRegistration implements ICacheRegistration {
		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.registerReadOnly(SubtreeContainerItemIdsCache.class, folderKeyToSubtreeContainerItemId);
		}
	}

	public static Long getFolderId(String subtreeUid, String folder) {
		return folderKeyToSubtreeContainerItemId.getIfPresent(key(subtreeUid, folder));
	}

	public static void putFolderId(String folderKey, long id) {
		folderKeyToSubtreeContainerItemId.put(folderKey, id);
	}

	public static Long putFolderIdIfMissing(String folderKey, long id) {
		return folderKeyToSubtreeContainerItemId.get(folderKey, key -> id);
	}

	public static void removeFolderId(String subtreeUid, String folder) {
		folderKeyToSubtreeContainerItemId.invalidate(key(subtreeUid, folder));
	}

	public static String key(String subtreeUid, String folder) {
		return subtreeUid + ":" + folder;
	}

}
