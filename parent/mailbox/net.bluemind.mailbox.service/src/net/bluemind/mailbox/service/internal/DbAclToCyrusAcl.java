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
package net.bluemind.mailbox.service.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.imap.Acl;
import net.bluemind.mailbox.api.Mailbox;

public class DbAclToCyrusAcl {

	private static final Logger logger = LoggerFactory.getLogger(DbAclToCyrusAcl.class);

	private final String domainUid;
	private final List<AccessControlEntry> containerAcls;
	private final ItemValue<Mailbox> box;
	private final ServerSideServiceProvider sp;

	public DbAclToCyrusAcl(String domainUid, List<AccessControlEntry> acls, ItemValue<Mailbox> box) {
		this.domainUid = domainUid;
		this.containerAcls = acls;
		this.box = box;
		this.sp = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
	}

	public Map<String, Acl> get() {
		Map<String, Acl> acls = new HashMap<>();
		IDirectory directoryApi = sp.instance(IDirectory.class, domainUid);
		List<String> subjects = containerAcls.stream().map(a -> a.subject).collect(Collectors.toList());
		ListResult<ItemValue<DirEntry>> dirs = directoryApi.search(DirEntryQuery.entries(subjects));
		Map<String, DirEntry> entries = dirs.values.stream()
				.collect(Collectors.toMap(d -> d.value.entryUid, d -> d.value));
		for (AccessControlEntry ace : containerAcls) {
			DirEntry subject = entries.get(ace.subject);
			if (subject == null) {
				logger.warn("UID: {} not found, ignoring ACE", ace.subject);
				continue;
			}

			switch (subject.kind) {
			case DOMAIN:
				logger.info("  * ace: {}@{} {}", ace.subject, domainUid, ace.verb);
				acls.put("group:" + ace.subject + "@" + domainUid, cyrusAcl(ace.verb));
				break;
			case GROUP:
				logger.info("  * ace: group:{}@{} {}", ace.subject, domainUid, ace.verb);
				acls.put("group:" + ace.subject + "@" + domainUid, cyrusAcl(ace.verb));
				break;
			case USER:
				logger.info("  * ace: {}@{} {}", ace.subject, domainUid, ace.verb);
				acls.put(ace.subject + "@" + domainUid, cyrusAcl(ace.verb));
				break;
			default:
				logger.warn("UID: {} is {} kind, ignoring ACE", ace.subject, subject.kind.toString());
				break;
			}
		}

		switch (box.value.type) {
		case user:
			acls.put(box.uid + "@" + domainUid, cyrusAcl(Verb.All));
			break;
		default:
			if (!acls.containsKey("anyone") || acls.get("anyone") == Acl.NOTHING) {
				acls.put("anyone", Acl.POST);
			}
			break;
		}

		acls.put("admin0", Acl.ALL);
		return acls;
	}

	private static Acl cyrusAcl(Verb verb) {
		if (verb == Verb.All) {
			return Acl.ALL;
		} else if (verb == Verb.Write) {
			return Acl.RW;
		} else if (verb == Verb.Read) {
			return Acl.RO;
		}
		return Acl.NOTHING;
	}

}
