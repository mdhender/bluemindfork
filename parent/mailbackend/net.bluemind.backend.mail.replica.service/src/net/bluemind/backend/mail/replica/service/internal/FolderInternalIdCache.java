/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.backend.mail.replica.service.internal;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.core.container.model.Container;

/**
 * When creating a folder with mail api
 * {@link IMailboxFolders#createById(long, net.bluemind.backend.mail.api.MailboxFolder)}
 * we want the replication to set the right itemId.
 * 
 * I did not find a way to pass the wanted id at creation time, annotations are
 * sent after the create. Only a cyrus patch to create would do the trick :/
 * 
 */
public class FolderInternalIdCache {

	private static final Logger logger = LoggerFactory.getLogger(FolderInternalIdCache.class);

	public static String key(Container substree, String folder) {
		Objects.requireNonNull(substree, "Container must not be null");
		Objects.requireNonNull(folder, "Folder name must not be null");
		String k = substree.uid + "." + folder;
		logger.debug("KEY: '{}'", k);
		return k;
	}

	private static final Cache<String, Long> folderKeyToExpectedInternalId = CacheBuilder.newBuilder().maximumSize(512)
			.build();

	public static Long expectedFolderId(Container subtree, String folder) {
		return folderKeyToExpectedInternalId.getIfPresent(key(subtree, folder));
	}

	public static void storeExpectedRecordId(Container subtree, String folder, long id) {
		folderKeyToExpectedInternalId.put(key(subtree, folder), id);
	}

}
