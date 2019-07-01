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
package net.bluemind.resource.service.internal;

import net.bluemind.directory.service.DirValueStoreService.MailboxAdapter;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.resource.api.ResourceDescriptor;

public class ResourceMailboxAdapter implements MailboxAdapter<ResourceDescriptor> {

	@Override
	public Mailbox asMailbox(String domainUid, String uid, ResourceDescriptor rd) {
		Mailbox mb = new Mailbox();
		mb.archived = false;
		mb.system = true; // FIXME ??
		// FIXME cyrus is case sensitive and postfix (hook?) do a lowercase on
		// mailbox name
		mb.name = uid.toLowerCase();
		mb.emails = rd.emails;
		mb.routing = Mailbox.Routing.internal;
		mb.dataLocation = rd.dataLocation;
		mb.type = Mailbox.Type.resource;
		mb.hidden = true;
		return mb;
	}

}
