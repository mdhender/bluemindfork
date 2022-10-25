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

package net.bluemind.backend.cyrus;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.cyrus.internal.MailboxOps;
import net.bluemind.core.container.hooks.IAclHook;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.imap.Acl;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.service.internal.DbAclToCyrusAcl;

public class CyrusBackendHook implements IAclHook {

	private static final Logger logger = LoggerFactory.getLogger(CyrusBackendHook.class);

	@Override
	public void onAclChanged(BmContext context, ContainerDescriptor container, List<AccessControlEntry> previous,
			List<AccessControlEntry> current) {
		// FIXME whot ?
		if (!container.uid.startsWith(IMailboxAclUids.MAILBOX_ACL_PREFIX)) {
			return;
		}

		IServiceProvider sp = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

		logger.info("Mailbox {} acls updated: {} => {}", container.uid, previous, current);
		try {
			ItemValue<Mailbox> box = sp.instance(IMailboxes.class, container.domainUid).getComplete(container.owner);

			Map<String, Acl> acls = new DbAclToCyrusAcl(container.domainUid, current, box).get();
			MailboxOps.setAcls(box, container.domainUid, acls);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

	}

}
