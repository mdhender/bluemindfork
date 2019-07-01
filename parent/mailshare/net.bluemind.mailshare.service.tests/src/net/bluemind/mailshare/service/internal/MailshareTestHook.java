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
package net.bluemind.mailshare.service.internal;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.mailshare.api.Mailshare;
import net.bluemind.mailshare.hook.IMailshareHook;
import net.bluemind.mailshare.service.MailshareTests;

public class MailshareTestHook implements IMailshareHook {

	@Override
	public void onCreate(BmContext context, String uid, Mailshare mailshare, String domainUid)
			throws ServerFault {
		MailshareTests.hook = true;
	}

	@Override
	public void onUpdate(BmContext context, String uid, Mailshare mailshare, String domainUid)
			throws ServerFault {
		System.err.println("MAILSHARE HOOK ON UPDATE");
	}

	@Override
	public void onDelete(BmContext context, String uid, String domainUid) throws ServerFault {
		System.err.println("MAILSHARE HOOK ON DELETE");
	}

}
