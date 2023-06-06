/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.backend.mail.replica.service.internal;

import java.util.ArrayList;
import java.util.List;

import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.MailboxFolderSearchQuery;
import net.bluemind.backend.mail.api.utils.FolderTree;
import net.bluemind.backend.mail.api.utils.MailIndexQuery;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.mailshare.api.IMailshare;
import net.bluemind.user.api.IUser;

public class SearchQueryAdapter {

	private SearchQueryAdapter() {

	}

	public static MailIndexQuery adapt(String domainUid, String dirEntryUid, MailboxFolderSearchQuery query) {
		if (!isRecursive(query)) {
			return MailIndexQuery.simpleQuery(query);
		} else {
			ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
			DirEntry entry = provider.instance(IDirectory.class, domainUid).findByEntryUid(dirEntryUid);
			String mboxRoot = null;
			String partition = domainUid.replace('.', '_');
			if (entry.kind == Kind.MAILSHARE) {
				mboxRoot = provider.instance(IMailshare.class, domainUid).getComplete(dirEntryUid).value.name
						.replace('.', '^');
			} else {
				mboxRoot = "user." + provider.instance(IUser.class, domainUid).getComplete(dirEntryUid).value.login
						.replace('.', '^');
			}
			List<String> folders = new ArrayList<>();
			IMailboxFolders service = provider.instance(IMailboxFolders.class, partition, mboxRoot);
			if (query.query.scope.folderScope != null && query.query.scope.folderScope.folderUid != null) {
				FolderTree fullTree = FolderTree.of(service.all());
				folders.add(query.query.scope.folderScope.folderUid);
				folders.addAll(fullTree.children(service.getComplete(query.query.scope.folderScope.folderUid)).stream()
						.map(f -> f.uid).toList());
			} else {
				folders.addAll(service.all().stream().map(f -> f.uid).toList());
			}
			return MailIndexQuery.folderQuery(query, folders);
		}

	}

	private static boolean isRecursive(MailboxFolderSearchQuery query) {
		return query.query.scope != null && query.query.scope.isDeepTraversal;
	}

}
