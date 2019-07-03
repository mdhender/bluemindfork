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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.bluemind.imap.Acl;
import net.bluemind.imap.impl.IMAPResponse;

/**
 * Removes an ACL on a mailbox
 * 
 * Should be used like : <code>dam user/admin@willow.vmw admin0</code>
 * 
 * 
 */
public class ListAclCommand extends SimpleCommand<Map<String, Acl>> {

	private String mailboxName;

	/**
	 * @param mailboxName
	 *            user/admin@willow.vmw
	 * @param ownerId
	 *            admin0
	 */
	public ListAclCommand(String mailboxName) {
		super("GETACL " + toUtf7(mailboxName));

		// BM-11131
		// add quotes if mailboxName contains space
		if (mailboxName.contains(" ")) {
			this.mailboxName = toUtf7(mailboxName);
		} else {
			this.mailboxName = toUtf7(mailboxName, false);
		}
	}

	@Override
	public void responseReceived(List<IMAPResponse> rs) {
		data = new HashMap<String, Acl>();
		if (isOk(rs)) {
			Iterator<IMAPResponse> it = rs.iterator();
			String pf = "* ACL " + mailboxName + " ";
			int pfLen = pf.length();
			int len = rs.size() - 1;
			for (int i = 0; i < len; i++) {
				IMAPResponse r = it.next();
				String pl = r.getPayload();
				if (!pl.startsWith("* ACL ")) {
					logger.warn("skipped '{}'", pl);
					continue;
				}
				if (logger.isDebugEnabled()) {
					logger.debug("S: {}", pl);
				}
				if (pl.length() < pfLen) {
					// no acls present
					continue;
				}
				String[] splitted = pl.substring(pfLen).split(" ");
				for (int j = 0; j < splitted.length; j += 2) {
					String consumer = splitted[j];
					String aclString = splitted[j + 1];
					Acl a = new Acl(aclString);
					data.put(consumer, a);
				}
			}
		}
	}

}
