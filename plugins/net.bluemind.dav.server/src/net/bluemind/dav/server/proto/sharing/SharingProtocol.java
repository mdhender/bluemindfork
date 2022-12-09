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
package net.bluemind.dav.server.proto.sharing;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.dav.server.proto.post.VEventStuffPostProtocol.VEventStuffContext;
import net.bluemind.dav.server.proto.sharing.Sharing.SharingAction;
import net.bluemind.dav.server.proto.sharing.Sharing.SharingType;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.store.ResType;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectory;

public class SharingProtocol {

	private static final Logger logger = LoggerFactory.getLogger(SharingProtocol.class);

	public static SharingResponse execute(LoggedCore lc, SharingQuery query, VEventStuffContext ctx) {
		SharingResponse sr = new SharingResponse();

		try {
			List<Sharing> sharings = query.getSharings();
			for (Sharing s : sharings) {
				IDirectory dirApi = lc.getCore().instance(IDirectory.class, lc.getDomain());
				ListResult<ItemValue<DirEntry>> dirEntries = dirApi.search(DirEntryQuery.filterEmail(s.href));
				if (dirEntries.total == 1) {
					ItemValue<DirEntry> dirEntry = dirEntries.values.get(0);
					Matcher m = ResType.VSTUFF_CONTAINER.matcher(query.getPath());
					m.find();
					String uid = m.group(2);
					IContainerManagement cmApi = lc.getCore().instance(IContainerManagement.class, uid);

					List<AccessControlEntry> entries = cmApi.getAccessControlList();

					Iterator<AccessControlEntry> it = entries.iterator();
					boolean found = false;
					while (it.hasNext()) {
						AccessControlEntry ace = it.next();
						if (ace.subject.equals(dirEntry.value.entryUid)) {
							found = true;
							if (s.action == SharingAction.Remove) {
								it.remove();
							} else {
								ace.verb = s.type == SharingType.ReadOnly ? Verb.Read : Verb.Write;
							}
						}
					}

					if (!found && s.action == SharingAction.Set) {
						entries.add(AccessControlEntry.create(dirEntry.value.entryUid,
								s.type == SharingType.ReadOnly ? Verb.Read : Verb.Write));
					}

					cmApi.setAccessControlList(entries);
				} else {
					logger.error("Cannot find user {}", s.href);
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return sr;
	}

}
