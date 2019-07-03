/*BEGIN LICENSE
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
package net.bluemind.mailbox.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;

/**
 * To unify the management of changelogs, ACLs, client synchronization,
 * permissions and sharing, Bluemind stores all elements in a generic structure
 * called a container. All containers are identified by a unique ID. Some
 * containers are named (UID) in a specific manner to express a certain meaning.
 * 
 * 
 * Returns specific mailbox ACL container UIDs. This container is used to share
 * access to a mailbox
 */
@BMApi(version = "3")
@Path("/mailbox/uids")
public interface IMailboxAclUids {
	public static final String TYPE = "mailboxacl";
	public static final String MAILBOX_ACL_PREFIX = "mailbox:acls-";

	/**
	 * Returns the mailbox ACL UID
	 * 
	 * @param uid
	 *                the {@link net.bluemind.mailbox.api.Mailbox} UID
	 * @return mailbox ACL UID
	 */
	@GET
	@Path("{uid}/_mailbox_acl")
	public default String getUidForMailbox(@PathParam("uid") String mailboxUid) {
		return MAILBOX_ACL_PREFIX + mailboxUid;
	}

	public static String uidForMailbox(String mailboxUid) {
		return MAILBOX_ACL_PREFIX + mailboxUid;
	}
}
