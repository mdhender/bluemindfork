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
package net.bluemind.mailbox.identity.service;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.http.HttpClientFactory;
import net.bluemind.mailbox.identity.api.IMailboxIdentity;

public class MailboxIdentityHttpTests extends MailboxIdentityTests {

	@Override
	protected IMailboxIdentity service(SecurityContext sc, String domainUid, String mboxUid) throws ServerFault {
		return HttpClientFactory.create(IMailboxIdentity.class, null, "http://localhost:8090")
				.syncClient(sc.getSessionId(), domainUid, mboxUid);
	}

}
