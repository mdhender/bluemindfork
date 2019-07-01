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
package net.bluemind.imap.command;

import java.util.List;

import net.bluemind.imap.Acl;
import net.bluemind.imap.impl.IMAPResponse;

/**
 * Set an ACL on a mailbox
 * 
 * Should be used like : <code>cm user/admin@willow.vmw willow_vmw</code>
 * 
 * 
 */
public class SetAclCommand extends SimpleCommand<Boolean> {

	/**
	 * @param mailboxName
	 *            user/admin@willow.vmw
	 * @param partition
	 *            willow_vmw
	 */
	public SetAclCommand(String mailboxName, String consumer, Acl acl) {
		super("SETACL " + toUtf7(mailboxName) + " " + consumer + " " + acl.toString());
	}

	@Override
	public void responseReceived(List<IMAPResponse> rs) {
		IMAPResponse last = rs.get(rs.size() - 1);
		data = last.isOk();
		if (!last.isOk()) {
			logger.error("setacl failed: " + last.getPayload());
		}
	}
}
